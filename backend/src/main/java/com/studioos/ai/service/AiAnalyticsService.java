package com.studioos.ai.service;

import com.studioos.ai.dto.AiDtos.AnalyticsDto;
import com.studioos.ai.dto.AiDtos.IntentCountDto;
import com.studioos.ai.dto.AiDtos.LeadStatusCountDto;
import com.studioos.ai.repository.AiChatConversationRepository;
import com.studioos.ai.repository.AiChatLeadRepository;
import com.studioos.ai.repository.AiChatMessageRepository;
import com.studioos.model.Branch;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Branch-scoped chat analytics: volume, intents, and lead funnel. */
@Service
public class AiAnalyticsService {
    private final AiBranchContext ctx;
    private final AiChatConversationRepository conversations;
    private final AiChatMessageRepository messages;
    private final AiChatLeadRepository leads;

    public AiAnalyticsService(AiBranchContext ctx, AiChatConversationRepository conversations,
        AiChatMessageRepository messages, AiChatLeadRepository leads) {
        this.ctx = ctx; this.conversations = conversations; this.messages = messages; this.leads = leads;
    }

    @Transactional(readOnly = true)
    public AnalyticsDto summary(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        long convos = conversations.countByTenantIdAndBranchId(b.getTenantId(), b.getId());
        long leadCount = leads.countByTenantIdAndBranchId(b.getTenantId(), b.getId());
        List<IntentCountDto> intents = messages.countIntentsByBranch(b.getTenantId(), b.getId()).stream()
            .map(r -> new IntentCountDto(String.valueOf(r[0]), (Long) r[1])).toList();
        long messageCount = conversations.findByTenantIdAndBranchIdOrderByUpdatedAtDesc(b.getTenantId(), b.getId())
            .stream().mapToLong(c -> messages.countByConversationId(c.getId())).sum();
        Map<String, Long> byStatus = leads.findByTenantIdAndBranchIdOrderByCreatedAtDesc(b.getTenantId(), b.getId())
            .stream().collect(Collectors.groupingBy(l -> l.getStatus() == null ? "NEW" : l.getStatus(), Collectors.counting()));
        List<LeadStatusCountDto> leadsByStatus = byStatus.entrySet().stream()
            .map(e -> new LeadStatusCountDto(e.getKey(), e.getValue())).toList();
        long leadsNew = byStatus.getOrDefault("NEW", 0L);
        return new AnalyticsDto(convos, leadCount, leadsNew, messageCount, intents, leadsByStatus);
    }
}
