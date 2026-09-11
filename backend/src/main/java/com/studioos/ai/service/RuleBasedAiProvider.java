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
            case "ADMISSION_FEE" -> "The one-time admission fee at Dance7 " + f.branchName() + " is "
                + f.admissionFee().getOrDefault("admission_fee", "not listed")
                + ". Want me to walk you through the packages too?";
            case "SCHEDULE" -> scheduleReply(f);
            case "CLASSES" -> classesReply(f);
            case "RECOMMEND" -> recommendReply(f);
            case "CONTACT" -> contactReply(f);
            case "TRIAL" -> trialReply(f);
            case "POLICY" -> knowledgeReply(f, "policies", "Here is our policy for Dance7 " + f.branchName() + ":");
            case "OFFER" -> knowledgeReply(f, "offers", "Current offers at Dance7 " + f.branchName() + ":");
            case "LEAD" ->
                "Wonderful! I can have the " + f.branchName() + " team call you to confirm your batch. "
                    + "Just share your details using the form — name, phone, and the class you are interested in.";
            case "THANKS" -> "You are most welcome! Anything else I can help with — classes, fees, or timings?";
            default -> unknownReply(f);
        };
        boolean leadPrompt = f.leadSignal() || "LEAD".equals(f.intent()) || "FEES".equals(f.intent());
        return new AiReply(reply, leadPrompt);
    }

    private String feeReply(FactPack f) {
        if (f.packages().isEmpty()) return unknownReply(f);
        StringBuilder sb = new StringBuilder("Here are the current packages at Dance7 " + f.branchName() + ": ");
        for (Map<String, String> p : f.packages()) {
            sb.append(p.getOrDefault("name", "Package"));
            if (p.containsKey("fee")) sb.append(" at ").append(p.get("fee"));
            if (p.containsKey("duration")) sb.append(" (").append(p.get("duration")).append(")");
            sb.append("; ");
        }
        sb.append("One-time admission fee: ").append(f.admissionFee().getOrDefault("admission_fee", "not listed")).append(". ");
        sb.append("Shall I arrange a callback to confirm your batch?");
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
        StringBuilder sb = new StringBuilder("Based on what you told me, I recommend: ");
        for (RecommendedClassDto r : recs) {
            sb.append(r.name()).append(" — ").append(r.reason()).append(" ");
        }
        sb.append("Want the ").append(f.branchName()).append(" team to call you to confirm?");
        return sb.toString().trim();
    }

    private String contactReply(FactPack f) {
        Map<String, String> d = f.branchDetails();
        StringBuilder sb = new StringBuilder("You can reach Dance7 " + f.branchName());
        if (d.containsKey("phone")) sb.append(" at ").append(d.get("phone"));
        if (d.containsKey("address")) sb.append(". Studio: ").append(d.get("address"));
        sb.append(". Or share your details here and we will call you back.");
        return sb.toString();
    }

    private String trialReply(FactPack f) {
        Optional<Map<String, String>> trial = f.searchHits().stream().findFirst();
        if (trial.isPresent()) return trial.get().getOrDefault("text", "") + " Shall I book one for you?";
        return "Yes — new students can usually book a trial class before enrolling at Dance7 "
            + f.branchName() + ". Share your details and the team will schedule it.";
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
