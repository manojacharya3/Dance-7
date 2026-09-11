package com.studioos.ai.service;

import com.studioos.ai.model.AiChatAudit;
import com.studioos.ai.repository.AiChatAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;

/**
 * Chat guardrails: input sanitization, XSS stripping, prompt-injection screening,
 * in-memory per-IP rate limiting, IP hashing, and PII-free audit logging.
 */
@Service
public class AiGuard {
    private static final int MAX_MESSAGE_CHARS = 1000;
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final List<String> INJECTION_MARKERS = List.of(
        "ignore previous instructions", "ignore all previous", "disregard previous",
        "system prompt", "reveal your instructions", "show your prompt", "jailbreak",
        "developer mode", "do anything now", "bypass", "override your rules");

    private final AiChatAuditRepository audits;
    private final Map<String, List<Long>> hits = new ConcurrentHashMap<>();

    public AiGuard(AiChatAuditRepository audits) { this.audits = audits; }

    /** Strip HTML, trim, enforce length. Returns sanitized text. */
    public String sanitize(String raw) {
        if (raw == null) return "";
        String noTags = raw.replaceAll("<[^>]*>", " ");
        String collapsed = noTags.replaceAll("\\s+", " ").trim();
        return collapsed.length() > MAX_MESSAGE_CHARS ? collapsed.substring(0, MAX_MESSAGE_CHARS) : collapsed;
    }

    public boolean looksLikeInjection(String text) {
        String lower = text.toLowerCase();
        return INJECTION_MARKERS.stream().anyMatch(lower::contains);
    }

    /** Sliding-window rate limit per key (IP + endpoint). */
    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        List<Long> times = hits.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        times.removeIf(t -> now - t > 60_000);
        if (times.size() >= MAX_REQUESTS_PER_MINUTE) return false;
        times.add(now);
        return true;
    }

    public String ipHash(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(("dance7-ai|" + ip).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "unavailable";
        }
    }

    public void audit(String tenant, Long branchId, Long conversationId, String event, String detail, String ipHash) {
        try {
            AiChatAudit audit = new AiChatAudit();
            audit.setTenantId(tenant == null ? "default" : tenant);
            audit.setBranchId(branchId);
            audit.setConversationId(conversationId);
            audit.setEvent(event);
            audit.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
            audit.setIpHash(ipHash);
            audits.save(audit);
        } catch (Exception ignored) {
            // Audit must never break the chat flow.
        }
    }

    public String timestamp() { return LocalDateTime.now().toString(); }
}
