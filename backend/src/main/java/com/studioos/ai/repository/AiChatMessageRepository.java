package com.studioos.ai.repository;

import com.studioos.ai.model.AiChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
    long countByConversationId(Long conversationId);

    @Query("select coalesce(m.intent, 'OTHER'), count(m) from AiChatMessage m where m.role = 'assistant' and m.conversationId in "
        + "(select c.id from AiChatConversation c where c.tenantId = :tenantId and c.branchId = :branchId) "
        + "group by coalesce(m.intent, 'OTHER') order by count(m) desc")
    List<Object[]> countIntentsByBranch(String tenantId, Long branchId);
}
