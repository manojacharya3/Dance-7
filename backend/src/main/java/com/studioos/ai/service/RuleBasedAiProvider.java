package com.studioos.ai.service;

import com.studioos.ai.dto.AiDtos.RecommendedClassDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Default provider: deterministic, template-based verbalization over retrieved
 * branch facts. Needs no API keys and never invents content — every number,
 * name and time in a reply comes from a tool result. An external LLM can
 * replace this behind the same port (see HttpLlmAiProvider).
 */
@Service @Primary
public class RuleBasedAiProvider implements Dance7AiPort {
    @Override
    public AiReply generate(FactPack f) {
        String reply = switch (f.intent()) {
            case "GREETING" -> "Hi! Welcome to Dance7 " + f.branchName()
                + ". Ask me about classes, timings, fees — or tap an option below to explore.";
            case "FEES" -> feeReply(f);
            case "ADMISSION_FEE" -> "The one-time admission fee is "
                + f.admissionFee().getOrDefault("admission_fee", "not listed") + ".";
            case "SCHEDULE" -> scheduleReply(f);
            case "CLASSES" -> classesReply(f);
            case "RECOMMEND" -> recommendReply(f);
            case "JOIN", "CLARIFY" ->
                "Absolutely! \uD83D\uDC83 I'd love to help you find the right batch.\n\nIs the class for:\n1. A child\n2. An adult\n\nAnd if it's for a child, what is their age?";
            case "CONTACT" -> contactReply(f);
            case "TRIAL" -> trialReply(f);
            case "POLICY" -> knowledgeReply(f, "policies", "Here is our policy for Dance7 " + f.branchName() + ":");
            case "OFFER" -> knowledgeReply(f, "offers", "Current offers at Dance7 " + f.branchName() + ":");
            case "LEAD" ->
                "Great! I can help you get started. Would you like me to collect your details so the studio team can contact you? "
                    + "Tap the button below and share the student's name and phone number — nothing is enrolled until the studio team confirms it with you.";
            case "THANKS" -> "You are most welcome! Anything else I can help with — classes, fees, or timings?";
            default -> unknownReply(f);
        };
        boolean leadPrompt = f.leadSignal() || List.of("LEAD", "FEES", "RECOMMEND", "JOIN").contains(f.intent());
        return new AiReply(reply, leadPrompt);
    }

    private String feeReply(FactPack f) {
        if (f.packages().isEmpty()) return unknownReply(f);
        StringBuilder sb = new StringBuilder("Here are the package options at Dance7 " + f.branchName() + ":\n");
        for (Map<String, String> p : f.packages()) {
            sb.append("\n").append(p.getOrDefault("name", "Package")).append(" – ").append(p.getOrDefault("fee", "not listed"));
            if (p.containsKey("details")) sb.append(" (").append(p.get("details")).append(")");
        }
        sb.append("\n\nOne-time admission fee: ").append(f.admissionFee().getOrDefault("admission_fee", "not listed")).append(".");
        sb.append("\nShall I arrange a callback to confirm your batch?");
        return sb.toString();
    }

    private String scheduleReply(FactPack f) {
        if (f.schedules().isEmpty()) return unknownReply(f);
        StringBuilder sb = new StringBuilder("Class timings at Dance7 " + f.branchName() + ": ");
        for (Map<String, String> s : f.schedules()) {
            sb.append(s.getOrDefault("class", "Class")).append(" — ").append(s.getOrDefault("day", ""))
                .append(" ").append(s.getOrDefault("time", ""));
            if (s.containsKey("batch")) sb.append(" (").append(s.get("batch")).append(")");
            sb.append("; ");
        }
        return sb.toString().trim();
    }

    private String classesReply(FactPack f) {
        if (f.classes().isEmpty()) return unknownReply(f);
        StringBuilder sb = new StringBuilder("We offer these at Dance7 " + f.branchName() + ": ");
        for (Map<String, String> c : f.classes()) {
            sb.append(c.getOrDefault("name", "Class"));
            if (c.containsKey("age_range")) sb.append(" (ages ").append(c.get("age_range")).append(")");
            sb.append("; ");
        }
        sb.append("Tell me the student's age and interest and I will recommend the best fit.");
        return sb.toString();
    }

    private String recommendReply(FactPack f) {
        List<RecommendedClassDto> recs = f.recommendations();
        if (recs.isEmpty()) return classesReply(f);
        RecommendedClassDto top = recs.get(0);
        StringBuilder sb = new StringBuilder("Based on what you told me, I'd recommend:\n");
        sb.append("\n🕺 ").append(top.name());
        if (top.ageRange() != null && !top.ageRange().isBlank()) sb.append("\n").append(top.ageRange());
        if (top.schedule() != null && !top.schedule().isBlank()) sb.append("\n").append(top.schedule());
        if (top.fee() != null && !top.fee().isBlank()) sb.append("\n").append(top.fee()).append("/month");
        if (recs.size() > 1) {
            sb.append("\n\nAlso worth a look: ");
            sb.append(recs.subList(1, recs.size()).stream().map(RecommendedClassDto::name).reduce((a, b) -> a + ", " + b).orElse(""));
            sb.append(".");
        }
        sb.append("\n\nWould you like to know about the package options?");
        return sb.toString();
    }

    private String contactReply(FactPack f) {
        Map<String, String> d = f.branchDetails();
        if (d.containsKey("phone"))
            return "Sure! You can contact the Dance7 " + f.branchName() + " branch at:\n\n📞 " + d.get("phone");
        return unknownReply(f);
    }

    private String trialReply(FactPack f) {
        Optional<Map<String, String>> trial = f.searchHits().stream().findFirst();
        if (trial.isPresent()) {
            String text = trial.get().getOrDefault("text", "");
            if (!text.isBlank()) return text;
        }
        // Never invent trial availability: the branch setting is the only other source.
        if (f.trialInfo() != null && !f.trialInfo().isBlank()) return f.trialInfo();
        return unknownReply(f);
    }

    private String knowledgeReply(FactPack f, String kind, String prefix) {
        List<Map<String, String>> hits = f.searchHits();
        if (hits.isEmpty()) {
            List<Map<String, String>> all = "policies".equals(kind) ? f.policies() : f.offers();
            if (all.isEmpty()) return unknownReply(f);
            hits = all.subList(0, Math.min(2, all.size()));
        }
        StringBuilder sb = new StringBuilder(prefix + " ");
        for (Map<String, String> h : hits.subList(0, Math.min(2, hits.size()))) {
            sb.append(h.getOrDefault("title", "")).append(h.getOrDefault("title", "").isEmpty() ? "" : ": ")
                .append(h.getOrDefault("text", h.getOrDefault("body", ""))).append(" ");
        }
        return sb.toString().trim();
    }

    private String unknownReply(FactPack f) {
        String phone = f.branchDetails().get("phone");
        String base = "I don't have that information for the " + f.branchName() + " branch yet.";
        return phone == null ? base + " Please contact the studio and the team will help you right away."
            : base + " Please contact the studio at " + phone + ".";
    }
}
