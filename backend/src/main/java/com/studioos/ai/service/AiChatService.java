package com.studioos.ai.service;

import com.studioos.ai.dto.AiDtos.ChatMessageRequest;
import com.studioos.ai.dto.AiDtos.ChatMessageResponse;
import com.studioos.ai.dto.AiDtos.RecommendedClassDto;
import com.studioos.ai.model.AiChatConversation;
import com.studioos.ai.model.AiChatMessage;
import com.studioos.ai.repository.AiChatConversationRepository;
import com.studioos.ai.repository.AiChatMessageRepository;
import com.studioos.ai.service.Dance7AiPort.AiReply;
import com.studioos.ai.service.Dance7AiPort.ChatTurn;
import com.studioos.ai.service.Dance7AiPort.FactPack;
import com.studioos.model.Branch;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat orchestrator: sanitize → injection screen → branch resolve → rate limit →
 * conversation load/create (ownership-checked) → intent detect → tool retrieval →
 * provider verbalization → persist + audit → respond. Branch id threads through
 * every step; no cross-branch data ever enters the FactPack.
 */
@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final Pattern AGE = Pattern.compile("(\\d{1,2})\\s*(?:years? old|year old|yrs? old|y\\.o\\.|years?)");
    private static final Pattern AGE_PLAIN = Pattern.compile("\\bage\\s*(\\d{1,2})\\b");

    private final AiBranchContext ctx;
    private final AiRetrievalService retrieval;
    private final AiRecommendationService recommender;
    private final AiChatConversationRepository conversations;
    private final AiChatMessageRepository messages;
    private final AiGuard guard;
    private final Dance7AiPort ruleBased;
    private final ObjectProvider<HttpLlmAiProvider> llm;

    public AiChatService(AiBranchContext ctx, AiRetrievalService retrieval, AiRecommendationService recommender,
        AiChatConversationRepository conversations, AiChatMessageRepository messages, AiGuard guard,
        RuleBasedAiProvider ruleBased, ObjectProvider<HttpLlmAiProvider> llm) {
        this.ctx = ctx; this.retrieval = retrieval; this.recommender = recommender;
        this.conversations = conversations; this.messages = messages; this.guard = guard;
        this.ruleBased = ruleBased; this.llm = llm;
    }

    @Transactional
    public ChatMessageResponse message(String tenant, ChatMessageRequest req, HttpServletRequest http) {
        String clean = guard.sanitize(req.message());
        if (clean.isEmpty()) throw new IllegalArgumentException("Please type a message.");
        String ipHash = guard.ipHash(http);
        if (!guard.allow("chat:" + ipHash)) {
            guard.audit(tenant, null, null, "RATE_LIMITED", "public chat", ipHash);
            throw new RateLimitedException();
        }
        Branch branch = ctx.resolve(tenant, req.branch());
        if (guard.looksLikeInjection(clean)) {
            log.warn("Dance7 chat injection blocked (branch={}, convo={}).", branch.getName(), req.conversationId());
            guard.audit(tenant, branch.getId(), req.conversationId(), "PROMPT_INJECTION_BLOCKED", "marker matched", ipHash);
            AiChatConversation convo = loadOrCreate(tenant, branch, req.conversationId(), req.visitorId());
            String refusal = "I can help with classes, timings, fees and admissions at Dance7 "
                + branch.getName() + ". What would you like to know?";
            save(convo.getId(), "user", clean, "BLOCKED", null);
            save(convo.getId(), "assistant", refusal, "BLOCKED", null);
            return new ChatMessageResponse(convo.getId(), convo.getVisitorId(), refusal, "BLOCKED", false, defaults(), List.of());
        }

        long started = System.currentTimeMillis();
        AiChatConversation convo;
        try {
            convo = loadOrCreate(tenant, branch, req.conversationId(), req.visitorId());
        } catch (EntityNotFoundException | SecurityException e) {
            throw e;
        } catch (RuntimeException e) {
            // Storage unavailable but branch is known: degrade instead of failing the request.
            log.error("Dance7 chat conversation unavailable (branch={}).", branch.getName(), e);
            guard.audit(tenant, branch.getId(), null, "CHAT_STORAGE_UNAVAILABLE", e.getClass().getSimpleName(), ipHash);
            return fallbackResponse(tenant, branch, null,
                req.visitorId() != null && !req.visitorId().isBlank() ? req.visitorId() : UUID.randomUUID().toString());
        }

        try {
            String intent = detectIntent(clean);
            // Bare join interest ("I want to join") carries no details yet: ask the
            // child/adult + age clarification first instead of jumping to lead capture.
            intent = refineIntent(intent, clean);
            boolean leadSignal = isLeadSignal(clean, intent);

            List<RecommendedClassDto> recs = List.of();
            if ("RECOMMEND".equals(intent)) {
                Integer age = extractAge(clean);
                String level = extractLevel(clean);
                if (age == null && level == null && !hasCategorySignal(clean)) {
                    // Missing inputs: ask a short clarification instead of guessing.
                    intent = "CLARIFY";
                } else {
                    recs = recommender.recommend(tenant, String.valueOf(branch.getId()), age, level, clean);
                }
            }
            List<Map<String, String>> hits = retrieval.searchKnowledgeBase(tenant, branch.getId(), clean, 3);
            List<ChatTurn> history = messages.findByConversationIdOrderByCreatedAtAscIdAsc(convo.getId()).stream()
                .skip(Math.max(0, messages.countByConversationId(convo.getId()) - 6))
                .map(m -> new ChatTurn(m.getRole(), m.getContent())).toList();

            FactPack facts = new FactPack(tenant, branch.getId(), branch.getName(), clean, intent,
                retrieval.getBranchDetails(tenant, String.valueOf(branch.getId())),
                needsPackages(intent) ? retrieval.getPackages(tenant, branch.getId()) : List.of(),
                needsPackages(intent) ? retrieval.getAdmissionFee(tenant, branch.getId()) : Map.of(),
                needsSchedules(intent) ? retrieval.getClassSchedules(tenant, branch.getId(), null) : List.of(),
                needsClasses(intent) ? retrieval.getClasses(tenant, branch.getId()) : List.of(),
                List.of(),
                needsPolicies(intent) ? retrieval.getPolicies(tenant, branch.getId(), null) : List.of(),
            needsOffers(intent) ? retrieval.getOffers(tenant, branch.getId()) : List.of(),
            hits, recs, history, leadSignal,
            "TRIAL".equals(intent) ? retrieval.setting(tenant, branch.getId(), "trial_info").orElse(null) : null);

            Dance7AiPort provider = llm.getIfAvailable() != null ? llm.getIfAvailable() : ruleBased;
            AiReply reply = provider.generate(facts);
            // Last-resort rule-based safety net: if the LLM path ever returns unusable output,
            // re-verbalize the same facts deterministically instead of failing.
            if ((reply == null || reply.text() == null || reply.text().isBlank()) && provider != ruleBased) {
                log.warn("Dance7 LLM provider returned empty output (branch={}); falling back to rule-based.",
                    branch.getName());
                reply = ruleBased.generate(facts);
            }

            save(convo.getId(), "user", clean, intent, null);
            save(convo.getId(), "assistant", reply.text(), intent, provider.getClass().getSimpleName());
            guard.audit(tenant, branch.getId(), convo.getId(), "CHAT_TURN", intent, ipHash);
            log.info("Dance7 chat turn ok (branch={}, intent={}, provider={}, {}ms).",
                branch.getName(), intent, provider.getClass().getSimpleName(), System.currentTimeMillis() - started);
            return new ChatMessageResponse(convo.getId(), convo.getVisitorId(), reply.text(), intent,
                reply.leadPrompt(), defaults(), recs);
        } catch (IllegalArgumentException | EntityNotFoundException | SecurityException | RateLimitedException e) {
            // Client/contract errors keep their status codes (400/404/403/429).
            throw e;
        } catch (Throwable t) {
            // Recoverable failure (retrieval/provider/persist): never 502 — return the
            // graceful fallback as a normal 200 reply and keep the widget usable.
            log.error("Dance7 chat turn failed (branch={}).", branch.getName(), t);
            guard.audit(tenant, branch.getId(), convo.getId(), "CHAT_TURN_FAILED", t.getClass().getSimpleName(), ipHash);
            try {
                save(convo.getId(), "user", clean, "FALLBACK", null);
                save(convo.getId(), "assistant", fallbackText(tenant, branch), "FALLBACK", "fallback");
            } catch (Throwable ignored) {
                log.warn("Dance7 fallback persist also failed (branch={}).", branch.getName());
            }
            return fallbackResponse(tenant, branch, convo.getId(), convo.getVisitorId());
        }
    }

    /** Graceful-degradation reply. Phone comes from branch settings — never hardcoded —
     *  so Whitefield renders the exact approved sentence while other branches stay correct. */
    static String fallbackReply(String branchName, String phone) {
        String base = "I'm having trouble retrieving information right now. Please try again shortly or contact the "
            + branchName + " branch";
        return phone == null || phone.isBlank() ? base + " — our team will help you right away."
            : base + " at " + phone + ".";
    }

    private String fallbackText(String tenant, Branch branch) {
        String phone = null;
        try {
            phone = retrieval.contactPhone(tenant, branch.getId()).orElse(null);
        } catch (Throwable ignored) {
            // Settings unreadable — fall back to the branch name only.
        }
        return fallbackReply(branch.getName(), phone);
    }

    private ChatMessageResponse fallbackResponse(String tenant, Branch branch, Long conversationId, String visitorId) {
        return new ChatMessageResponse(conversationId, visitorId, fallbackText(tenant, branch),
            "FALLBACK", false, defaults(), List.of());
    }

    private AiChatConversation loadOrCreate(String tenant, Branch branch, Long conversationId, String visitorId) {
        if (conversationId != null) {
            AiChatConversation existing = conversations.findByIdAndTenantId(conversationId, branch.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found."));
            if (!existing.getBranchId().equals(branch.getId()))
                throw new SecurityException("Conversation belongs to another branch.");
            if (visitorId == null || !visitorId.equals(existing.getVisitorId()))
                throw new SecurityException("Conversation ownership mismatch.");
            return existing;
        }
        AiChatConversation convo = new AiChatConversation();
        convo.setTenantId(branch.getTenantId());
        convo.setBranchId(branch.getId());
        convo.setVisitorId(visitorId != null && !visitorId.isBlank() ? visitorId : UUID.randomUUID().toString());
        convo.setChannel("WIDGET");
        convo.setStatus("OPEN");
        return conversations.save(convo);
    }

    private void save(Long conversationId, String role, String content, String intent, String tool) {
        AiChatMessage m = new AiChatMessage();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content.length() > 4000 ? content.substring(0, 4000) : content);
        m.setIntent(intent);
        m.setToolName(tool);
        messages.save(m);
    }

    static String detectIntent(String text) {
        String t = " " + text.toLowerCase() + " ";
        if (t.matches(".*\\b(hi|hii+|hello|hey|namaste|good morning|good afternoon|good evening)\\b.*") && text.length() < 40) return "GREETING";
        if (t.matches(".*\\b(thank|thanks|shukriya|dhanyavad)\\b.*")) return "THANKS";
        if (t.matches(".*\\b(admission fee|admission fees)\\b.*")) return "ADMISSION_FEE";
        boolean feeWords = t.matches(".*\\b(fee|fees|price|pricing|cost|charges|package|packages)\\b.*");
        if (!feeWords && t.matches(".*\\b(discount|discounts|promo|deal|deals|any offers|special offer)\\b.*")) return "OFFER";
        if (feeWords || t.matches(".*\\btrial class cost\\b.*")) return "FEES";
        if (t.matches(".*\\b(timing|timings|schedule|when|batch time|class time|days|slot|morning batch|evening batch|weekend)\\b.*")) return "SCHEDULE";
        if (t.matches(".*\\b(trial|demo|free class)\\b.*")) return "TRIAL";
        if (t.matches(".*\\b(recommend|suggest|which class|best class|my \\d|age \\d|years old|kid|child|beginner|intermediate|advanced|bharatanatyam|hip.?hop|contemporary|bollywood|zumba|freestyle)\\b.*")) return "RECOMMEND";
        if (t.matches(".*\\b(class|classes|course|courses|batches|kathak|ballet|salsa)\\b.*")) return "CLASSES";
        if (t.matches(".*\\b(enroll|enrol|join|joining|register|registration|admission|admit|sign ?up|interested|book|callback|call me)\\b.*")) return "LEAD";
        if (t.matches(".*\\b(contact|phone|number|call|address|where|location|reach)\\b.*")) return "CONTACT";
        if (t.matches(".*\\b(policy|policies|refund|cancel|attendance rule|terms)\\b.*")) return "POLICY";
        return "OTHER";
    }

    static boolean isLeadSignal(String text, String intent) {
        if ("LEAD".equals(intent)) return true;
        String t = text.toLowerCase();
        return ("FEES".equals(intent) || "TRIAL".equals(intent) || "RECOMMEND".equals(intent))
            && t.matches(".*\\b(interested|join|enroll|book|yes|please|call)\\b.*");
    }

    static Integer extractAge(String text) {
        Matcher m = AGE.matcher(text.toLowerCase());
        if (m.find()) return Integer.parseInt(m.group(1));
        Matcher p = AGE_PLAIN.matcher(text.toLowerCase());
        if (p.find()) return Integer.parseInt(p.group(1));
        return null;
    }

    static String refineIntent(String intent, String text) {
        if ("LEAD".equals(intent) && extractAge(text) == null && !text.matches(".*\\d{7,}.*")) return "JOIN";
        return intent;
    }

    static boolean hasCategorySignal(String text) {
        String t = " " + text.toLowerCase() + " ";
        return t.matches(".*\\b(kid|kids|child|children|toddler|teen|adult|adults|classical|bharatanatyam|hip.?hop|contemporary|bollywood|freestyle|zumba)\\b.*");
    }

    static String extractLevel(String text) {        String t = text.toLowerCase();
        if (t.contains("beginner") || t.contains("no experience") || t.contains("just start") || t.contains("new to")) return "BEGINNER";
        if (t.contains("intermediate")) return "INTERMEDIATE";
        if (t.contains("advanced") || t.contains("experienced")) return "ADVANCED";
        return null;
    }

    private boolean needsPackages(String intent) { return List.of("FEES", "ADMISSION_FEE", "LEAD", "OTHER").contains(intent); }
    private boolean needsSchedules(String intent) { return List.of("SCHEDULE", "CLASSES", "RECOMMEND", "OTHER").contains(intent); }
    private boolean needsClasses(String intent) { return List.of("CLASSES", "RECOMMEND", "SCHEDULE", "OTHER").contains(intent); }
    private boolean needsPolicies(String intent) { return List.of("POLICY", "OTHER").contains(intent); }
    private boolean needsOffers(String intent) { return List.of("OFFER", "FEES", "OTHER").contains(intent); }

    private List<String> defaults() {
        return List.of("View Classes", "Kids Classes", "Bharatanatyam", "Fees & Packages", "Contact Studio");
    }

    public static class RateLimitedException extends RuntimeException {
        public RateLimitedException() { super("Too many messages right now — please wait a few seconds and try again."); }
    }
}
