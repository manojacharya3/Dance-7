package com.studioos.ai.repository;

import com.studioos.ai.model.AiPackage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPackageRepository extends JpaRepository<AiPackage, Long> {
    List<AiPackage> findByTenantIdAndBranchIdAndActiveTrueOrderByFeeAmountAsc(String tenantId, Long branchId);
    List<AiPackage> findByTenantIdAndBranchIdOrderByFeeAmountAsc(String tenantId, Long branchId);
    Optional<AiPackage> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
}
