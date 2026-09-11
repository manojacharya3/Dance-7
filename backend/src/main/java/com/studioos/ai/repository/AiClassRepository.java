package com.studioos.ai.repository;

import com.studioos.ai.model.AiClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiClassRepository extends JpaRepository<AiClass, Long> {
    List<AiClass> findByTenantIdAndBranchIdAndActiveTrueOrderByNameAsc(String tenantId, Long branchId);
    List<AiClass> findByTenantIdAndBranchIdOrderByNameAsc(String tenantId, Long branchId);
    Optional<AiClass> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
}
