package com.studioos.repository;

import com.studioos.model.Invoice;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByIdAndTenantId(Long id, String tenantId);
    Optional<Invoice> findByPaymentIdAndTenantId(Long paymentId, String tenantId);
    Page<Invoice> findByTenantId(String tenantId, Pageable pageable);
    Page<Invoice> findByTenantIdAndBranchId(String tenantId, Long branchId, Pageable pageable);
    @Query("select i from Invoice i where i.tenantId = :tenantId and (lower(i.invoiceNumber) like lower(concat('%', :search, '%')) or str(i.paymentId) like concat('%', :search, '%') or str(i.studentId) like concat('%', :search, '%') or lower(i.status) like lower(concat('%', :search, '%')))")
    Page<Invoice> search(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);
    @Query("select i from Invoice i where i.tenantId = :tenantId and i.branchId = :branchId and (lower(i.invoiceNumber) like lower(concat('%', :search, '%')) or str(i.paymentId) like concat('%', :search, '%') or str(i.studentId) like concat('%', :search, '%') or lower(i.status) like lower(concat('%', :search, '%')))")
    Page<Invoice> searchByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("search") String search, Pageable pageable);
}
