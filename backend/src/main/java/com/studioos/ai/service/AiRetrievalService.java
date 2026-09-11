package com.studioos.ai.service;

import com.studioos.ai.model.AiChatFaq;
import com.studioos.ai.model.AiChatOffer;
import com.studioos.ai.model.AiChatPolicy;
import com.studioos.ai.model.AiClass;
import com.studioos.ai.model.AiClassSchedule;
import com.studioos.ai.model.AiPackage;
import com.studioos.ai.repository.AiChatFaqRepository;
import com.studioos.ai.repository.AiChatOfferRepository;
import com.studioos.ai.repository.AiChatPolicyRepository;
import com.studioos.ai.repository.AiClassRepository;
import com.studioos.ai.repository.AiClassScheduleRepository;
import com.studioos.ai.repository.AiPackageRepository;
import com.studioos.ai.repository.AiStudioSettingRepository;
import com.studioos.model.Branch;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal retrieval tools. Every tool takes the resolved branch id and only
 * reads active rows for that branch — the model calls these instead of relying
 * on prompt memory, so admin edits are reflected immediately.
 */
@Service
public class AiRetrievalService {
    private static final Logger log = LoggerFactory.getLogger(AiRetrievalService.class);
    private final AiBranchContext ctx;
    private final AiClassRepository classes;
    private final AiClassScheduleRepository schedules;
    private final AiPackageRepository packages;
    private final AiStudioSettingRepository settings;
    private final AiChatFaqRepository faqs;
    private final AiChatPolicyRepository policies;
    private final AiChatOfferRepository offers;

    public AiRetrievalService(AiBranchContext ctx, AiClassRepository classes, AiClassScheduleRepository schedules,
        AiPackageRepository packages, AiStudioSettingRepository settings, AiChatFaqRepository faqs,
        AiChatPolicyRepository policies, AiChatOfferRepository offers) {
        this.ctx = ctx; this.classes = classes; this.schedules = schedules; this.packages = packages;
        this.settings = settings; this.faqs = faqs; this.policies = policies; this.offers = offers;
    }

    /** Tool: getBranchDetails(branchId) */
    @Transactional(readOnly = true)
    public Map<String, String> getBranchDetails(String tenant, String branchRef) {
        Branch b = ctx.resolve(tenant, branchRef);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("name", b.getName());
        if (b.getAddress() != null) out.put("address", b.getAddress());
        contactPhone(tenant, b.getId()).ifPresent(phone -> out.put("phone", phone));
        return out;
    }

    /** Tool: getClasses(branchId) — active classes only. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getClasses(String tenant, Long branchId) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        return classes.findByTenantIdAndBranchIdAndActiveTrueOrderByNameAsc(b.getTenantId(), b.getId())
            .stream().map(this::classFacts).toList();
    }

    /** Tool: getClassSchedules(branchId[, classId]) — active slots only. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getClassSchedules(String tenant, Long branchId, Long classId) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        List<AiClassSchedule> rows = classId == null
            ? schedules.findByTenantIdAndBranchIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(b.getTenantId(), b.getId())
            : schedules.findByTenantIdAndBranchIdAndAiClassIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(b.getTenantId(), b.getId(), classId);
        Map<Long, String> names = classes.findByTenantIdAndBranchIdAndActiveTrueOrderByNameAsc(b.getTenantId(), b.getId())
            .stream().collect(Collectors.toMap(AiClass::getId, AiClass::getName, (a, c) -> a));
        return rows.stream().map(s -> {
            Map<String, String> out = new LinkedHashMap<>();
            out.put("class", names.getOrDefault(s.getAiClassId(), "Class"));
            out.put("day", s.getDayOfWeek());
            out.put("time", s.getStartTime() + "–" + s.getEndTime());
            if (s.getBatchLabel() != null) out.put("batch", s.getBatchLabel());
            if (s.getInstructorName() != null && !s.getInstructorName().isBlank()) out.put("instructor", s.getInstructorName());
            return out;
        }).toList();
    }

    /** Tool: getPackages(branchId) — active packages only, cheapest first. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getPackages(String tenant, Long branchId) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        return packages.findByTenantIdAndBranchIdAndActiveTrueOrderByFeeAmountAsc(b.getTenantId(), b.getId())
            .stream().map(p -> {
                Map<String, String> out = new LinkedHashMap<>();
                out.put("name", p.getName());
                if (p.getFeeAmount() != null) out.put("fee", "₹" + p.getFeeAmount().stripTrailingZeros().toPlainString());
                if (p.getDurationMonths() != null) out.put("duration", p.getDurationMonths() + " month(s)");
                if (p.getDescription() != null) out.put("details", p.getDescription());
                return out;
            }).toList();
    }

    /** Tool: getAdmissionFee(branchId) — packages first, studio setting fallback. */
    @Transactional(readOnly = true)
    public Map<String, String> getAdmissionFee(String tenant, Long branchId) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        Map<String, String> out = new LinkedHashMap<>();
        packages.findByTenantIdAndBranchIdAndActiveTrueOrderByFeeAmountAsc(b.getTenantId(), b.getId()).stream()
            .filter(p -> p.getAdmissionFee() != null).findFirst()
            .ifPresent(p -> out.put("admission_fee", "₹" + p.getAdmissionFee().stripTrailingZeros().toPlainString()));
        out.putIfAbsent("admission_fee", settingValue(tenant, b.getId(), "admission_fee")
            .map(v -> v.matches("[0-9.]+") ? "₹" + v : v).orElse("not listed"));
        return out;
    }

    /** Tool: getFaqs(branchId) */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getFaqs(String tenant, Long branchId) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        return faqs.findByTenantIdAndBranchIdAndActiveTrueOrderBySortOrderAscIdAsc(b.getTenantId(), b.getId())
            .stream().map(f -> Map.of("question", f.getQuestion(), "answer", f.getAnswer())).toList();
    }

    /** Tool: getPolicies(branchId[, category]) */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getPolicies(String tenant, Long branchId, String category) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        return policies.findByTenantIdAndBranchIdAndActiveTrueOrderByTitleAsc(b.getTenantId(), b.getId()).stream()
            .filter(p -> category == null || category.isBlank()
                || (p.getCategory() != null && p.getCategory().equalsIgnoreCase(category.trim())))
            .map(p -> Map.of("title", p.getTitle(), "body", p.getBody())).toList();
    }

    /** Tool: getOffers(branchId) — currently-valid only. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getOffers(String tenant, Long branchId) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        LocalDate today = LocalDate.now();
        return offers.findByTenantIdAndBranchIdAndActiveTrueOrderByValidUntilAsc(b.getTenantId(), b.getId()).stream()
            .filter(o -> (o.getValidFrom() == null || !o.getValidFrom().isAfter(today))
                && (o.getValidUntil() == null || !o.getValidUntil().isBefore(today)))
            .map(o -> Map.of("title", o.getTitle(), "body", o.getBody())).toList();
    }

    /** Tool: searchKnowledgeBase(branchId, query) — keyword-ranked snippets across FAQs, policies, offers, classes. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> searchKnowledgeBase(String tenant, Long branchId, String query, int limit) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) return List.of();
        List<Scored> scored = new ArrayList<>();        for (AiChatFaq f : faqs.findByTenantIdAndBranchIdAndActiveTrueOrderBySortOrderAscIdAsc(b.getTenantId(), b.getId())) {
            int score = score(tokens, f.getQuestion(), 3) + score(tokens, f.getKeywords(), 2) + score(tokens, f.getAnswer(), 1);
            if (score > 0) scored.add(new Scored(score, "FAQ", f.getQuestion(), f.getAnswer()));
        }
        for (AiChatPolicy p : policies.findByTenantIdAndBranchIdAndActiveTrueOrderByTitleAsc(b.getTenantId(), b.getId())) {
            int score = score(tokens, p.getTitle(), 3) + score(tokens, p.getBody(), 1);
            if (score > 0) scored.add(new Scored(score, "POLICY", p.getTitle(), p.getBody()));
        }
        for (AiChatOffer o : offers.findByTenantIdAndBranchIdAndActiveTrueOrderByValidUntilAsc(b.getTenantId(), b.getId())) {
            int score = score(tokens, o.getTitle(), 3) + score(tokens, o.getBody(), 1);
            if (score > 0) scored.add(new Scored(score, "OFFER", o.getTitle(), o.getBody()));
        }
        for (AiClass c : classes.findByTenantIdAndBranchIdAndActiveTrueOrderByNameAsc(b.getTenantId(), b.getId())) {
            int score = score(tokens, c.getName(), 3) + score(tokens, c.getDescription(), 1);
            if (score > 0) scored.add(new Scored(score, "CLASS", c.getName(), c.getDescription() == null ? "" : c.getDescription()));
        }
        List<Map<String, String>> hits = scored.stream().sorted(Comparator.comparingInt(Scored::score).reversed()).limit(Math.max(1, Math.min(limit, 5)))
            .map(s -> Map.of("type", s.type(), "title", s.title(), "text", s.text())).toList();
        if (hits.isEmpty()) log.debug("Dance7 knowledge search returned no hits (branch={}).", branchId);
        return hits;
    }

    /** Tool helper: contact phone for the unknown-information fallback (DB-driven, never hardcoded). */
    @Transactional(readOnly = true)
    public java.util.Optional<String> contactPhone(String tenant, Long branchId) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        return settingValue(tenant, b.getId(), "contact_phone");
    }

    @Transactional(readOnly = true)
    public java.util.Optional<String> setting(String tenant, Long branchId, String key) {
        Branch b = ctx.resolve(tenant, String.valueOf(branchId));
        return settingValue(tenant, b.getId(), key);
    }

    private java.util.Optional<String> settingValue(String tenant, Long branchId, String key) {
        return settings.findByTenantIdAndBranchIdAndSettingKey(tenant, branchId, key).map(s -> s.getSettingValue());
    }

    private Map<String, String> classFacts(AiClass c) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("id", String.valueOf(c.getId()));
        out.put("name", c.getName());
        if (c.getCategory() != null) out.put("category", c.getCategory());
        if (c.getMinAge() != null || c.getMaxAge() != null)
            out.put("age_range", (c.getMinAge() == null ? "0" : c.getMinAge()) + "–" + (c.getMaxAge() == null ? "+" : c.getMaxAge()));
        if (c.getExperienceLevel() != null) out.put("level", c.getExperienceLevel());
        if (c.getDescription() != null) out.put("about", c.getDescription());
        return out;
    }

    static List<String> tokens(String query) {
        if (query == null) return List.of();
        return java.util.Arrays.stream(query.toLowerCase().split("[^a-z0-9₹]+")).filter(t -> t.length() > 2)
            .filter(t -> !STOP.contains(t)).distinct().toList();
    }

    static int score(List<String> tokens, String text, int weight) {
        if (text == null || text.isBlank()) return 0;
        String hay = text.toLowerCase();
        int hits = 0;
        for (String t : tokens) if (hay.contains(t)) hits++;
        return hits * weight;
    }

    private record Scored(int score, String type, String title, String text) {}

    private static final java.util.Set<String> STOP = java.util.Set.of(
        "the", "and", "for", "are", "you", "your", "with", "have", "has", "what", "when",
        "where", "which", "how", "much", "does", "class", "classes", "dance", "about", "there",
        "their", "them", "from", "that", "this", "please", "tell", "know", "branch", "studio");
}
