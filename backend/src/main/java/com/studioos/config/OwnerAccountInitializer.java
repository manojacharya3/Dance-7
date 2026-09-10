package com.studioos.config;

import com.studioos.model.Role;
import com.studioos.model.User;
import com.studioos.repository.RoleRepository;
import com.studioos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class OwnerAccountInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String recoveryPassword;

    public OwnerAccountInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, @Value("${app.security.owner-recovery-password:Test@123}") String recoveryPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.recoveryPassword = recoveryPassword;
    }

    @Transactional
    public void run(String... args) {
        Role ownerRole = roleRepository.findByName(Role.Name.OWNER)
            .orElseGet(() -> roleRepository.save(new Role(Role.Name.OWNER)));
        userRepository.findByEmailIgnoreCase("owner@dance7.com").ifPresentOrElse(user -> {
            boolean changed = false;
            if (user.getRoles().stream().noneMatch(role -> role.getName() == Role.Name.OWNER || role.getName() == Role.Name.ADMIN)) {
                user.addRole(ownerRole);
                changed = true;
            }
            if (!user.isEnabled()) {
                user.setEnabled(true);
                changed = true;
            }
            if (changed) userRepository.save(user);
        }, () -> {
            User owner = new User("owner@dance7.com", passwordEncoder.encode(recoveryPassword), "Dance7 Owner", "default");
            owner.setEnabled(true);
            owner.addRole(ownerRole);
            userRepository.save(owner);
        });
    }
}
