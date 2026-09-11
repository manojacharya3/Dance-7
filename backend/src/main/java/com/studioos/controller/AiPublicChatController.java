package com.studioos.controller;

import com.studioos.ai.dto.AiDtos.BranchOptionDto;
import com.studioos.ai.dto.AiDtos.ChatMessageRequest;
import com.studioos.ai.dto.AiDtos.ChatMessageResponse;
import com.studioos.ai.dto.AiDtos.LeadRequest;
import com.studioos.ai.dto.AiDtos.LeadResponse;
import com.studioos.ai.dto.AiDtos.RecommendRequest;
import com.studioos.ai.dto.AiDtos.RecommendedClassDto;
import com.studioos.ai.service.AiBranchContext;
import com.studioos.ai.service.AiChatService;
import com.studioos.ai.service.AiGuard;
import com.studioos.ai.service.AiLeadService;
import com.studioos.ai.service.AiRecommendationService;
import com.studioos.model.Branch;
import com.studioos.repository.BranchRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (unauthenticated) front-desk API. Every endpoint resolves an active
 * branch first; all facts returned are scoped to that branch only.
 */
@RestController @RequestMapping("/api/chat/public")
public class AiPublicChatController {
    private final AiBranchContext ctx;
    private final BranchRepository branches;
    private final AiChatService chat;
    private final AiRecommendationService recommender;
    private final AiLeadService leads;
    private final AiGuard guard;

    public AiPublicChatController(AiBranchContext ctx, BranchRepository branches, AiChatService chat,
        AiRecommendationService recommender, AiLeadService leads, AiGuard guard) {
        this.ctx = ctx; this.branches = branches; this.chat = chat;
        this.recommender = recommender; this.leads = leads; this.guard = guard;
    }

    @GetMapping("/branches")
    public List<BranchOptionDto> branches(@RequestParam(defaultValue = "default") String tenantId) {
        return branches.findByTenantIdAndActiveTrueOrderByNameAsc(AiBranchContext.tenantOf(tenantId)).stream()
            .map(b -> new BranchOptionDto(b.getId(), b.getName(), AiBranchContext.slugify(b.getName()))).toList();
    }

    @PostMapping("/message")
    public ChatMessageResponse message(@Valid @RequestBody ChatMessageRequest req, HttpServletRequest http) {
        return chat.message("default", req, http);
    }

    @GetMapping("/recommend")
    public List<RecommendedClassDto> recommend(@RequestParam String branch,
        @RequestParam(required = false) Integer age, @RequestParam(required = false) String experienceLevel,
        @RequestParam(required = false) String interest, HttpServletRequest http) {
        if (!guard.allow("rec:" + guard.ipHash(http))) throw new AiChatService.RateLimitedException();
        Branch b = ctx.resolve("default", branch);
        return recommender.recommend("default", String.valueOf(b.getId()), age, experienceLevel, interest);
    }

    @PostMapping("/leads")
    public ResponseEntity<LeadResponse> lead(@Valid @RequestBody LeadRequest req, HttpServletRequest http) {
        String ipHash = guard.ipHash(http);
        if (!guard.allow("lead:" + ipHash)) throw new AiChatService.RateLimitedException();
        Branch b = ctx.resolve("default", req.branch());
        LeadResponse saved = leads.capture("default", req);
        guard.audit("default", b.getId(), req.conversationId(), "LEAD_CAPTURED", "lead " + saved.leadId(), ipHash);
        return ResponseEntity.ok(saved);
    }
}
