package com.studioos.ai.repository;

import com.studioos.ai.model.AiChatConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatConversationRepository extends JpaRepository<AiChatConversation, Long> {
    Optional<AiChatConversation> findByIdAndTenantId(Long id, String tenantId);
    List<AiChatConversation> findByTenantIdAndBranchIdOrderByUpdatedAtDesc(String tenantId, Long branchId);
    long countByTenantIdAndBranchId(String tenantId, Long branchId);
}
