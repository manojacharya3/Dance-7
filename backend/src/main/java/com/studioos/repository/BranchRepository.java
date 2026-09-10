package com.studioos.repository;

import com.studioos.model.Branch;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);
    List<Branch> findByTenantIdAndActiveTrueOrderByNameAsc(String tenantId);
    Optional<Branch> findByTenantIdAndNameIgnoreCase(String tenantId, String name);
}
