package com.studioos.service;

import com.studioos.config.ScopeService;
import com.studioos.dto.MembershipDTO;
import com.studioos.model.Membership;
import com.studioos.model.MembershipStatus;
import com.studioos.repository.MembershipRepository;
import com.studioos.repository.StudentRepository;
import com.studioos.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MembershipServiceImpl implements MembershipService {
    private final MembershipRepository repository;
    private final StudentRepository studentRepository;
    private final BranchRepository branchRepository;
    private final ScopeService scope;

    public MembershipServiceImpl(MembershipRepository repository, StudentRepository studentRepository, BranchRepository branchRepository, ScopeService scope) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.branchRepository = branchRepository;
        this.scope = scope;
    }

    @Override
    public MembershipDTO create(MembershipDTO dto) {
        String tenantId = tenant(dto.tenantId());
        validateBranch(dto.branchId(), tenantId);
        validateStudent(dto.studentId(), tenantId);
        validateDates(dto.startDate(), dto.endDate());
        return toDto(repository.save(toEntity(dto, tenantId)));
    }

    @Override
    public MembershipDTO update(Long id, MembershipDTO dto) {
        String tenantId = tenant(dto.tenantId());
        validateBranch(dto.branchId(), tenantId);
        validateStudent(dto.studentId(), tenantId);
        validateDates(dto.startDate(), dto.endDate());
        Membership membership = find(id, tenantId);
        updateEntity(membership, dto, tenantId);
        refreshStatus(membership, LocalDate.now());
        return toDto(repository.save(membership));
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipDTO get(Long id, String tenantId) {
        Membership membership = find(id, tenant(tenantId));
        enforceInstructorScope(membership);
        refreshStatus(membership, LocalDate.now());
        return toDto(membership);
    }

    @Override
    public Page<MembershipDTO> list(String tenantId, String search, Pageable pageable) {
        String normalizedTenant = tenant(tenantId);
        Set<Long> assigned = scope.assignedStudentIds(normalizedTenant);
        if (assigned != null) {
            if (assigned.isEmpty()) return Page.empty(pageable);
            return repository.findByTenantIdAndStudentIdInAndActiveTrue(normalizedTenant, assigned, pageable).map(this::toDto);
        }
        Page<Membership> memberships = search == null || search.isBlank()
            ? repository.findByTenantIdAndActiveTrue(normalizedTenant, pageable)
            : repository.search(normalizedTenant, search.trim(), pageable);
        memberships.forEach(membership -> refreshStatus(membership, LocalDate.now()));
        return memberships.map(this::toDto);
    }

    @Override
    public void delete(Long id, String tenantId) {
        Membership membership = find(id, tenant(tenantId));
        membership.setActive(false);
        repository.save(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive(String tenantId) {
        return repository.countActive(tenant(tenantId), LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countExpiring(String tenantId, LocalDate today, LocalDate deadline) {
        return repository.countExpiring(tenant(tenantId), today, deadline);
    }

    @Override
    @Transactional(readOnly = true)
    public long countExpired(String tenantId) {
        return repository.countExpired(tenant(tenantId), LocalDate.now());
    }

    private Membership find(Long id, String tenantId) {
        return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + id));
    }

    private void enforceInstructorScope(Membership membership) {
        Set<Long> assigned = scope.assignedStudentIds(membership.getTenantId());
        if (assigned != null && !assigned.contains(membership.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this student");
        }
    }

    private void validateStudent(Long studentId, String tenantId) {
        if (studentRepository.findByIdAndTenantIdAndActiveTrue(studentId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("Student not found: " + studentId);
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("End date must be on or after start date");
    }

    private Membership toEntity(MembershipDTO dto, String tenantId) {
        Membership membership = new Membership();
        updateEntity(membership, dto, tenantId);
        return membership;
    }

    private void updateEntity(Membership membership, MembershipDTO dto, String tenantId) {
        membership.setTenantId(tenantId);
        membership.setBranchId(dto.branchId());
        membership.setStudentId(dto.studentId());
        membership.setPlanName(dto.planName().trim());
        membership.setDurationMonths(dto.durationMonths());
        membership.setStartDate(dto.startDate());
        membership.setEndDate(dto.endDate());
        membership.setFeeAmount(dto.feeAmount());
        membership.setStatus(dto.status());
        if (dto.active() != null) membership.setActive(dto.active());
        refreshStatus(membership, LocalDate.now());
    }

    private void refreshStatus(Membership membership, LocalDate today) {
        if (membership.getStatus() == MembershipStatus.CANCELLED) return;
        membership.setStatus(membership.getEndDate().isBefore(today) ? MembershipStatus.EXPIRED : MembershipStatus.ACTIVE);
    }

    private MembershipDTO toDto(Membership membership) {
        return new MembershipDTO(membership.getId(), membership.getTenantId(), membership.getBranchId(), membership.getStudentId(), membership.getPlanName(), membership.getDurationMonths(), membership.getStartDate(), membership.getEndDate(), membership.getFeeAmount(), membership.getStatus(), membership.isActive(), membership.getCreatedAt(), membership.getUpdatedAt());
    }
    private void validateBranch(Long branchId, String tenantId) { if (branchRepository.findByIdAndTenantIdAndActiveTrue(branchId, tenantId).isEmpty()) throw new EntityNotFoundException("Branch not found: " + branchId); }

    private String tenant(String value) { return value == null || value.isBlank() ? "default" : value.trim(); }
}
