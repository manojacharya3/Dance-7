package com.studioos.service;

import com.studioos.model.Role;
import com.studioos.model.User;
import com.studioos.repository.RoleRepository;
import com.studioos.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studioos.dto.UserManagementDTO;
import java.util.List;
import com.studioos.repository.BranchRepository;
import com.studioos.repository.InstructorRepository;
import com.studioos.repository.RefreshTokenRepository;

@Service
@Transactional
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;
    private final InstructorRepository instructorRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, BranchRepository branchRepository, InstructorRepository instructorRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository;
        this.instructorRepository = instructorRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public User register(String email, String password, String fullName, String tenantId) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        Role role = roleRepository.findByName(Role.Name.STAFF)
            .orElseGet(() -> roleRepository.save(new Role(Role.Name.STAFF)));
        User user = new User(normalizedEmail, passwordEncoder.encode(password), fullName.trim(), tenantIdOrDefault(tenantId));
        user.addRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
            .password(user.getPassword())
            .disabled(!user.isEnabled())
            .authorities(user.getRoles().stream().map(role -> "ROLE_" + role.getName().name()).toArray(String[]::new))
            .build();
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public List<UserManagementDTO> listManagedUsers(String tenantId) { return userRepository.findByTenantId(tenantIdOrDefault(tenantId), org.springframework.data.domain.PageRequest.of(0, 200)).map(this::toManagementDto).toList(); }
    public UserManagementDTO createManagedUser(UserManagementDTO dto) { String email = dto.email().trim().toLowerCase(Locale.ROOT); if (userRepository.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("An account with this email already exists."); User user = new User(email, passwordEncoder.encode(dto.password()), dto.fullName().trim(), tenantIdOrDefault(dto.tenantId())); apply(user, dto); return toManagementDto(userRepository.save(user)); }
    public UserManagementDTO updateManagedUser(Long id, UserManagementDTO dto) { User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found")); user.setFullName(dto.fullName().trim()); if (dto.password() != null && !dto.password().isBlank()) user.setPassword(passwordEncoder.encode(dto.password())); apply(user, dto); return toManagementDto(userRepository.save(user)); }
    public void disableManagedUser(Long id) { User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found")); user.setEnabled(false); userRepository.save(user); }
    public void removeManagedUser(Long id, String currentUserEmail) { User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found")); if (user.isEnabled()) throw new IllegalArgumentException("Active users cannot be permanently removed. Disable the user first."); boolean isOwner = user.getRoles().stream().anyMatch(role -> role.getName() == Role.Name.OWNER); if (isOwner && currentUserEmail != null && user.getEmail().equalsIgnoreCase(currentUserEmail.trim())) throw new IllegalArgumentException("You cannot remove your own account."); if (isOwner && userRepository.countOwners() <= 1) throw new IllegalArgumentException("The last OWNER account cannot be removed."); refreshTokenRepository.deleteByUserId(id); user.getRoles().clear(); userRepository.delete(user); }
    private void apply(User user, UserManagementDTO dto) { Role.Name roleName = Role.Name.valueOf(dto.role().toUpperCase(Locale.ROOT)); if (roleName == Role.Name.BRANCH_HEAD && dto.branchId() == null) throw new IllegalArgumentException("Branch Head must be assigned a branch"); if (roleName == Role.Name.INSTRUCTOR && (dto.branchId() == null || dto.instructorId() == null)) throw new IllegalArgumentException("Instructor must be assigned a branch and instructor record"); if (dto.branchId() != null && branchRepository.findByIdAndTenantIdAndActiveTrue(dto.branchId(), user.getTenantId()).isEmpty()) throw new IllegalArgumentException("Branch is not valid for this tenant"); if (dto.instructorId() != null && instructorRepository.findByIdAndTenantIdAndActiveTrue(dto.instructorId(), user.getTenantId()).isEmpty()) throw new IllegalArgumentException("Instructor is not valid for this tenant"); user.setBranchId(dto.branchId()); user.setInstructorId(dto.instructorId()); user.getRoles().clear(); Role role = roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName))); user.addRole(role); }
    private UserManagementDTO toManagementDto(User user) { return new UserManagementDTO(user.getId(), user.getTenantId(), user.getFullName(), user.getEmail(), null, user.getRoles().stream().findFirst().map(role -> role.getName().name()).orElse("STAFF"), user.getBranchId(), user.getInstructorId(), user.isEnabled()); }

    private String tenantIdOrDefault(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim();
    }
}
