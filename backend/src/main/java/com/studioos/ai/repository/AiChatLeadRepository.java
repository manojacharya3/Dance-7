package com.studioos.ai.repository;

import com.studioos.ai.model.AiChatLead;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatLeadRepository extends JpaRepository<AiChatLead, Long> {
    List<AiChatLead> findByTenantIdAndBranchIdOrderByCreatedAtDesc(String tenantId, Long branchId);
    List<AiChatLead> findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(String tenantId, Long branchId, String status);
    Optional<AiChatLead> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
    long countByTenantIdAndBranchId(String tenantId, Long branchId);
}
