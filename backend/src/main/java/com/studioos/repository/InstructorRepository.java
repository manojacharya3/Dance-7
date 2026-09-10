package com.studioos.repository;

import com.studioos.model.Instructor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Optional<Instructor> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);
    Page<Instructor> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);
    Page<Instructor> findByTenantIdAndBranchIdAndActiveTrue(String tenantId, Long branchId, Pageable pageable);
    @Query("select i from Instructor i where i.tenantId = :tenantId and i.active = true and (lower(i.firstName) like lower(concat('%', :search, '%')) or lower(i.lastName) like lower(concat('%', :search, '%')) or lower(i.email) like lower(concat('%', :search, '%')))" )
    Page<Instructor> search(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);
    @Query("select i from Instructor i where i.tenantId = :tenantId and i.active = true and i.branchId = :branchId and (lower(i.firstName) like lower(concat('%', :search, '%')) or lower(i.lastName) like lower(concat('%', :search, '%')) or lower(i.email) like lower(concat('%', :search, '%')))")
    Page<Instructor> searchByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("search") String search, Pageable pageable);
    long countByTenantIdAndActiveTrue(String tenantId);
    long countByTenantIdAndBranchIdAndActiveTrue(String tenantId, Long branchId);
}