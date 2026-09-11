package com.studioos.ai.service;

import com.studioos.ai.dto.AiDtos.AiChatLeadDto;
import com.studioos.ai.dto.AiDtos.LeadRequest;
import com.studioos.ai.dto.AiDtos.LeadResponse;
import com.studioos.ai.model.AiChatConversation;
import com.studioos.ai.model.AiChatLead;
import com.studioos.ai.repository.AiChatConversationRepository;
import com.studioos.ai.repository.AiChatLeadRepository;
import com.studioos.model.Branch;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lead capture and inbox. Validates, stores, and confirms front-desk leads. */
@Service @Transactional
public class AiLeadService {
    private static final Pattern PHONE = Pattern.compile("^[+0-9][0-9\\s-]{6,17}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AiBranchContext ctx;
    private final AiChatLeadRepository leads;
    private final AiChatConversationRepository conversations;

    public AiLeadService(AiBranchContext ctx, AiChatLeadRepository leads, AiChatConversationRepository conversations) {
        this.ctx = ctx; this.leads = leads; this.conversations = conversations;
    }

    public LeadResponse capture(String tenant, LeadRequest req) {
        Branch b = ctx.resolve(tenant, req.branch());
        String phone = req.phone() == null ? "" : req.phone().trim();
        if (phone.isEmpty() || !PHONE.matcher(phone).matches())
            throw new IllegalArgumentException("Please share a valid phone number so the studio can reach you.");
        if (req.email() != null && !req.email().isBlank() && !EMAIL.matcher(req.email().trim()).matches())
            throw new IllegalArgumentException("That email address does not look valid.");
        if ((req.studentName() == null || req.studentName().isBlank()) && (req.parentName() == null || req.parentName().isBlank()))
            throw new IllegalArgumentException("Please share the student name or parent name.");

        AiChatLead lead = new AiChatLead();
        lead.setTenantId(b.getTenantId());
        lead.setBranchId(b.getId());
        lead.setConversationId(req.conversationId());
        lead.setStudentName(trim(req.studentName()));
        lead.setParentName(trim(req.parentName()));
        lead.setAge(req.age());
        lead.setPhone(phone);
        lead.setEmail(trim(req.email()));
        lead.setInterestedClass(trim(req.interestedClass()));
        lead.setPreferredBatch(trim(req.preferredBatch()));
        lead.setPreferredStartDate(trim(req.preferredStartDate()));
        lead.setMessage(trim(req.message()));
        lead.setStatus("NEW");
        AiChatLead saved = leads.save(lead);

        if (req.conversationId() != null) {
            conversations.findByIdAndTenantId(req.conversationId(), b.getTenantId()).ifPresent(c -> {
                if (c.getBranchId().equals(b.getId())) { c.setLeadCaptured(true); conversations.save(c); }
            });
        }
        String who = saved.getStudentName() != null && !saved.getStudentName().isBlank() ? saved.getStudentName() : "there";
        return new LeadResponse(saved.getId(), saved.getStatus(),
            "Thanks " + who + "! Your details are with the " + b.getName() + " team — they will call you shortly to confirm your batch.");
    }

    @Transactional(readOnly = true)
    public List<AiChatLeadDto> inbox(String tenant, String branch, String status) {
        Branch b = ctx.resolve(tenant, branch);
        List<AiChatLead> rows = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
            ? leads.findByTenantIdAndBranchIdOrderByCreatedAtDesc(b.getTenantId(), b.getId())
            : leads.findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(b.getTenantId(), b.getId(), status.trim().toUpperCase());
        return rows.stream().map(this::toDto).toList();
    }

    public AiChatLeadDto setStatus(String tenant, String branch, Long id, String status) {
        Branch b = ctx.resolve(tenant, branch);
        AiChatLead lead = leads.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Lead not found: " + id));
        String next = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("NEW", "CONTACTED", "TRIAL", "ENROLLED", "LOST", "CLOSED").contains(next))
            throw new IllegalArgumentException("Unknown lead status: " + status);
        lead.setStatus(next);
        return toDto(leads.save(lead));
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private AiChatLeadDto toDto(AiChatLead l) {
        return new AiChatLeadDto(l.getId(), l.getBranchId(), l.getConversationId(), l.getStudentName(),
            l.getParentName(), l.getAge(), l.getPhone(), l.getEmail(), l.getInterestedClass(),
            l.getPreferredBatch(), l.getPreferredStartDate(), l.getMessage(), l.getStatus(), l.getCreatedAt());
    }

    /** Conversation ownership helper used by the chat flow. */
    public AiChatConversation ownedConversation(String tenant, Long conversationId, String visitorId) {
        AiChatConversation c = conversations.findByIdAndTenantId(conversationId, AiBranchContext.tenantOf(tenant))
            .orElseThrow(() -> new EntityNotFoundException("Conversation not found."));
        if (visitorId == null || !visitorId.equals(c.getVisitorId()))
            throw new SecurityException("Conversation ownership mismatch.");
        return c;
    }
}
