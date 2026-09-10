package com.studioos.repository;

import com.studioos.model.Payment;
import com.studioos.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);
    Page<Payment> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);
    Page<Payment> findByTenantIdAndStudentIdInAndActiveTrue(String tenantId, Collection<Long> studentIds, Pageable pageable);
    Page<Payment> findByTenantIdAndBranchIdAndActiveTrue(String tenantId, Long branchId, Pageable pageable);
    Page<Payment> findByTenantIdAndStudentIdAndActiveTrue(String tenantId, Long studentId, Pageable pageable);
    Page<Payment> findByTenantIdAndBranchIdAndStudentIdAndActiveTrue(String tenantId, Long branchId, Long studentId, Pageable pageable);
    Page<Payment> findByTenantIdAndMembershipIdAndActiveTrue(String tenantId, Long membershipId, Pageable pageable);
    Page<Payment> findByTenantIdAndBranchIdAndMembershipIdAndActiveTrue(String tenantId, Long branchId, Long membershipId, Pageable pageable);
    @Query("select p from Payment p where p.tenantId = :tenantId and p.active = true and (str(p.studentId) like concat('%', :search, '%') or str(p.membershipId) like concat('%', :search, '%') or lower(p.paymentMethod) like lower(concat('%', :search, '%')) or lower(p.paymentStatus) like lower(concat('%', :search, '%')))")
    Page<Payment> search(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);
    @Query("select p from Payment p where p.tenantId = :tenantId and p.active = true and p.branchId = :branchId and (str(p.studentId) like concat('%', :search, '%') or str(p.membershipId) like concat('%', :search, '%') or lower(p.paymentMethod) like lower(concat('%', :search, '%')) or lower(p.paymentStatus) like lower(concat('%', :search, '%')))")
    Page<Payment> searchByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("search") String search, Pageable pageable);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.tenantId = :tenantId and p.active = true and p.paymentStatus = com.studioos.model.PaymentStatus.PAID")
    BigDecimal totalRevenue(@Param("tenantId") String tenantId);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.tenantId = :tenantId and p.active = true and p.paymentStatus = com.studioos.model.PaymentStatus.PENDING")
    BigDecimal pendingPayments(@Param("tenantId") String tenantId);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.tenantId = :tenantId and p.active = true and p.paymentStatus = com.studioos.model.PaymentStatus.PAID and p.paymentDate between :startDate and :endDate")
    BigDecimal monthlyCollections(@Param("tenantId") String tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.tenantId = :tenantId and p.active = true and p.branchId = :branchId and p.paymentStatus = com.studioos.model.PaymentStatus.PAID")
    BigDecimal totalRevenueByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.tenantId = :tenantId and p.active = true and p.branchId = :branchId and p.paymentStatus = com.studioos.model.PaymentStatus.PENDING")
    BigDecimal pendingPaymentsByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.tenantId = :tenantId and p.active = true and p.branchId = :branchId and p.paymentStatus = com.studioos.model.PaymentStatus.PAID and p.paymentDate between :startDate and :endDate")
    BigDecimal monthlyCollectionsByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
