package com.studioos.ai.service;

import com.studioos.ai.dto.AiDtos.RecommendedClassDto;
import java.util.List;
import java.util.Map;

/** Port for response verbalization. Retrieval always happens first in AiChatService;
 *  implementations only turn branch facts into conversational replies. */
public interface Dance7AiPort {
    AiReply generate(FactPack facts);

    /** Facts gathered by tools for exactly one branch. No other branch data is present. */
    record FactPack(String tenantId, Long branchId, String branchName, String userText,
        String intent, Map<String, String> branchDetails, List<Map<String, String>> packages,
        Map<String, String> admissionFee, List<Map<String, String>> schedules,
        List<Map<String, String>> classes, List<Map<String, String>> faqs,
        List<Map<String, String>> policies, List<Map<String, String>> offers,
        List<Map<String, String>> searchHits, List<RecommendedClassDto> recommendations,
        List<ChatTurn> history, boolean leadSignal, String trialInfo) {}

    record ChatTurn(String role, String content) {}
    record AiReply(String text, boolean leadPrompt) {}
}
