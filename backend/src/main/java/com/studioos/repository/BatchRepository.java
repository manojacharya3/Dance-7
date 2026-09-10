package com.studioos.repository;

import com.studioos.model.Batch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    Optional<Batch> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);
    Page<Batch> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);
    Page<Batch> findByTenantIdAndBranchIdAndActiveTrue(String tenantId, Long branchId, Pageable pageable);
    Page<Batch> findByTenantIdAndBranchIdAndInstructorIdAndActiveTrue(String tenantId, Long branchId, Long instructorId, Pageable pageable);
    Page<Batch> findByTenantIdAndInstructorIdAndActiveTrue(String tenantId, Long instructorId, Pageable pageable);
    List<Batch> findByTenantIdAndInstructorIdAndActiveTrue(String tenantId, Long instructorId);
    @Query("select b from Batch b where b.tenantId = :tenantId and b.active = true and lower(b.batchName) like lower(concat('%', :search, '%'))")
    Page<Batch> search(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);
    @Query("select b from Batch b where b.tenantId = :tenantId and b.active = true and b.branchId = :branchId and lower(b.batchName) like lower(concat('%', :search, '%'))")
    Page<Batch> searchByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("search") String search, Pageable pageable);
    @Query("select b from Batch b where b.tenantId = :tenantId and b.active = true and b.branchId = :branchId and b.instructorId = :instructorId and lower(b.batchName) like lower(concat('%', :search, '%'))")
    Page<Batch> searchByBranchAndInstructor(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("instructorId") Long instructorId, @Param("search") String search, Pageable pageable);
    long countByTenantIdAndActiveTrue(String tenantId);
    long countByTenantIdAndBranchIdAndActiveTrue(String tenantId, Long branchId);
}