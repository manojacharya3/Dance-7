package com.studioos.ai.repository;

import com.studioos.ai.model.AiChatPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatPolicyRepository extends JpaRepository<AiChatPolicy, Long> {
    List<AiChatPolicy> findByTenantIdAndBranchIdAndActiveTrueOrderByTitleAsc(String tenantId, Long branchId);
    List<AiChatPolicy> findByTenantIdAndBranchIdOrderByTitleAsc(String tenantId, Long branchId);
    Optional<AiChatPolicy> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
}
