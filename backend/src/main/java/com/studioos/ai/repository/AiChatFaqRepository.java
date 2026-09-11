package com.studioos.ai.repository;

import com.studioos.ai.model.AiChatFaq;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatFaqRepository extends JpaRepository<AiChatFaq, Long> {
    List<AiChatFaq> findByTenantIdAndBranchIdAndActiveTrueOrderBySortOrderAscIdAsc(String tenantId, Long branchId);
    List<AiChatFaq> findByTenantIdAndBranchIdOrderBySortOrderAscIdAsc(String tenantId, Long branchId);
    Optional<AiChatFaq> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
}
