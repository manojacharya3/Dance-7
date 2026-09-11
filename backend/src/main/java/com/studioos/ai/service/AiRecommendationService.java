package com.studioos.ai.service;

import com.studioos.ai.dto.AiDtos.RecommendedClassDto;
import com.studioos.ai.model.AiClass;
import com.studioos.ai.model.AiClassSchedule;
import com.studioos.ai.repository.AiClassRepository;
import com.studioos.ai.repository.AiClassScheduleRepository;
import com.studioos.model.Branch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic class-recommendation engine. Scores active branch classes on
 * age fit, experience-level fit and interest-keyword fit — no LLM inference.
 * The assistant presents these results instead of guessing from raw text.
 */
@Service
public class AiRecommendationService {
    private final AiBranchContext ctx;
    private final AiClassRepository classes;
    private final AiClassScheduleRepository schedules;

    public AiRecommendationService(AiBranchContext ctx, AiClassRepository classes, AiClassScheduleRepository schedules) {
        this.ctx = ctx; this.classes = classes; this.schedules = schedules;
    }

    @Transactional(readOnly = true)
    public List<RecommendedClassDto> recommend(String tenant, String branchRef, Integer age, String experienceLevel, String interest) {
        Branch b = ctx.resolve(tenant, branchRef);
        List<AiClass> all = classes.findByTenantIdAndBranchIdAndActiveTrueOrderByNameAsc(b.getTenantId(), b.getId());
        if (all.isEmpty()) return List.of();
        String level = norm(experienceLevel);
        List<String> interests = AiRetrievalService.tokens(interest == null ? "" : interest);
        List<Scored> scored = new ArrayList<>();
        for (AiClass c : all) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            if (age != null && age >= 0) {
                boolean minOk = c.getMinAge() == null || age >= c.getMinAge();
                boolean maxOk = c.getMaxAge() == null || age <= c.getMaxAge();
                if (minOk && maxOk) {
                    score += 5;
                    reasons.add("fits age " + age);
                } else {
                    score -= 4;
                }
            }
            if (level != null && c.getExperienceLevel() != null) {
                if (c.getExperienceLevel().equalsIgnoreCase("ALL") || c.getExperienceLevel().equalsIgnoreCase(level)) {
                    score += 3;
                    reasons.add(level.toLowerCase() + "-friendly");
                }
            }
            String hay = ((c.getName() == null ? "" : c.getName()) + " " + (c.getCategory() == null ? "" : c.getCategory())
                + " " + (c.getDescription() == null ? "" : c.getDescription())).toLowerCase();
            int hits = 0;
            for (String t : interests) if (hay.contains(t)) hits++;
            if (hits > 0) {
                score += Math.min(hits, 3) * 2;
                reasons.add("matches your interest");
            }
            if (age != null && age < 13 && "KIDS".equalsIgnoreCase(c.getCategory())) {
                score += 2;
                reasons.add("designed for kids");
            }
            scored.add(new Scored(score, c, reasons));
        }
        Map<Long, List<AiClassSchedule>> byClass = schedules
            .findByTenantIdAndBranchIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(b.getTenantId(), b.getId())
            .stream().collect(Collectors.groupingBy(AiClassSchedule::getAiClassId));
        return scored.stream().sorted(Comparator.comparingInt(Scored::score).reversed()).limit(3)
            .filter(s -> s.score() > 0).map(s -> {
                AiClass c = s.cls();
                String sched = byClass.getOrDefault(c.getId(), List.of()).stream()
                    .map(slot -> slot.getDayOfWeek() + " " + slot.getStartTime() + "–" + slot.getEndTime())
                    .collect(Collectors.joining(", "));
                String ages = (c.getMinAge() == null && c.getMaxAge() == null) ? "All age groups"
                    : "Age: " + (c.getMinAge() == null ? "" : c.getMinAge()) + "–" + (c.getMaxAge() == null ? "+" : c.getMaxAge()) + " years";
                String fee = c.getFeeAmount() == null ? "" : "₹" + c.getFeeAmount().stripTrailingZeros().toPlainString();
                return new RecommendedClassDto(c.getId(), c.getName(), c.getCategory(),
                    s.reasons().isEmpty() ? "Popular at this branch." : String.join(", ", s.reasons()) + ".",
                    ages, sched, fee);
            }).toList();
    }

    private String norm(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim().toUpperCase();
        if (v.startsWith("BEGIN")) return "BEGINNER";
        if (v.startsWith("INTER")) return "INTERMEDIATE";
        if (v.startsWith("ADV")) return "ADVANCED";
        return v;
    }

    private record Scored(int score, AiClass cls, List<String> reasons) {}
}
