package com.studioos.service;

import com.studioos.config.ScopeService;
import com.studioos.dto.PaymentDTO;
import com.studioos.model.Membership;
import com.studioos.model.Payment;
import com.studioos.repository.MembershipRepository;
import com.studioos.repository.PaymentRepository;
import com.studioos.repository.StudentRepository;
import com.studioos.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository repository;
    private final MembershipRepository membershipRepository;
    private final StudentRepository studentRepository;
    private final BranchRepository branchRepository;
    private final ScopeService scope;

    public PaymentServiceImpl(PaymentRepository repository, MembershipRepository membershipRepository, StudentRepository studentRepository, BranchRepository branchRepository, ScopeService scope) {
        this.repository = repository;
        this.membershipRepository = membershipRepository;
        this.studentRepository = studentRepository;
        this.branchRepository = branchRepository;
        this.scope = scope;
    }

    @Override
    public PaymentDTO create(PaymentDTO dto) {
        String tenantId = tenant(dto.tenantId());
        validateBranch(resolveBranch(dto.branchId(), tenantId), tenantId);
        validateReferences(dto.membershipId(), dto.studentId(), tenantId);
        return toDto(repository.save(toEntity(dto, tenantId)));
    }

    @Override
    public PaymentDTO update(Long id, PaymentDTO dto) {
        String tenantId = tenant(dto.tenantId());
        validateBranch(resolveBranch(dto.branchId(), tenantId), tenantId);
        validateReferences(dto.membershipId(), dto.studentId(), tenantId);
        Payment payment = find(id, tenantId);
        updateEntity(payment, dto, tenantId);
        return toDto(repository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO get(Long id, String tenantId) { Payment payment = find(id, tenant(tenantId)); enforceInstructorScope(payment); return toDto(payment); }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> list(String tenantId, String search, Long studentId, Long membershipId, Pageable pageable) {
        String normalizedTenant = tenant(tenantId);
        Set<Long> assigned = scope.assignedStudentIds(normalizedTenant);
        if (assigned != null) {
            if (assigned.isEmpty()) return Page.empty(pageable);
            return repository.findByTenantIdAndStudentIdInAndActiveTrue(normalizedTenant, assigned, pageable).map(this::toDto);
        }
        Page<Payment> payments;
        if (search != null && !search.isBlank()) payments = repository.search(normalizedTenant, search.trim(), pageable);
        else if (studentId != null) payments = repository.findByTenantIdAndStudentIdAndActiveTrue(normalizedTenant, studentId, pageable);
        else if (membershipId != null) payments = repository.findByTenantIdAndMembershipIdAndActiveTrue(normalizedTenant, membershipId, pageable);
        else payments = repository.findByTenantIdAndActiveTrue(normalizedTenant, pageable);
        return payments.map(this::toDto);
    }

    @Override
    public void delete(Long id, String tenantId) { Payment payment = find(id, tenant(tenantId)); payment.setActive(false); repository.save(payment); }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalRevenue(String tenantId) { return repository.totalRevenue(tenant(tenantId)); }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal pendingPayments(String tenantId) { return repository.pendingPayments(tenant(tenantId)); }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal monthlyCollections(String tenantId, LocalDate startDate, LocalDate endDate) { return repository.monthlyCollections(tenant(tenantId), startDate, endDate); }

    private Payment find(Long id, String tenantId) { return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).orElseThrow(() -> new EntityNotFoundException("Payment not found: " + id)); }

    private void enforceInstructorScope(Payment payment) {
        Set<Long> assigned = scope.assignedStudentIds(payment.getTenantId());
        if (assigned != null && !assigned.contains(payment.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this student");
        }
    }

    private void validateReferences(Long membershipId, Long studentId, String tenantId) {
        Membership membership = membershipRepository.findByIdAndTenantIdAndActiveTrue(membershipId, tenantId).orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        if (!membership.getStudentId().equals(studentId)) throw new IllegalArgumentException("Payment student does not match membership student");
        if (studentRepository.findByIdAndTenantIdAndActiveTrue(studentId, tenantId).isEmpty()) throw new EntityNotFoundException("Student not found: " + studentId);
    }

    private Payment toEntity(PaymentDTO dto, String tenantId) { Payment payment = new Payment(); updateEntity(payment, dto, tenantId); return payment; }
    private void updateEntity(Payment payment, PaymentDTO dto, String tenantId) { payment.setTenantId(tenantId); payment.setBranchId(dto.branchId()); payment.setMembershipId(dto.membershipId()); payment.setStudentId(dto.studentId()); payment.setAmount(dto.amount()); payment.setPaymentDate(dto.paymentDate()); payment.setPaymentMethod(dto.paymentMethod()); payment.setPaymentStatus(dto.paymentStatus()); payment.setRemarks(dto.remarks()); if (dto.active() != null) payment.setActive(dto.active()); }
    private PaymentDTO toDto(Payment payment) { return new PaymentDTO(payment.getId(), payment.getTenantId(), payment.getBranchId(), payment.getMembershipId(), payment.getStudentId(), payment.getAmount(), payment.getPaymentDate(), payment.getPaymentMethod(), payment.getPaymentStatus(), payment.getRemarks(), payment.isActive(), payment.getCreatedAt(), payment.getUpdatedAt()); }
    private void validateBranch(Long branchId, String tenantId) { if (branchRepository.findByIdAndTenantIdAndActiveTrue(branchId, tenantId).isEmpty()) throw new EntityNotFoundException("Branch not found: " + branchId); }
    private Long resolveBranch(Long id, String tenantId) { return id != null ? id : branchRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId).stream().findFirst().map(branch -> branch.getId()).orElseThrow(() -> new EntityNotFoundException("No active branch found")); }
    private String tenant(String value) { return value == null || value.isBlank() ? "default" : value.trim(); }
}
