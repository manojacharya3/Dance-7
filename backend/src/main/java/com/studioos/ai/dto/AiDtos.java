package com.studioos.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AiDtos {
    private AiDtos() {}

    public record AiClassDto(Long id, String tenantId, @NotNull Long branchId, @NotBlank String name,
        String category, Integer minAge, Integer maxAge, String experienceLevel,
        BigDecimal feeAmount, String description, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record AiClassScheduleDto(Long id, String tenantId, @NotNull Long branchId, @NotNull Long aiClassId,
        @NotBlank String dayOfWeek, @NotBlank String startTime, @NotBlank String endTime,
        String batchLabel, String instructorName, Boolean active) {}

    public record AiPackageDto(Long id, String tenantId, @NotNull Long branchId, @NotBlank String name,
        Integer durationMonths, BigDecimal feeAmount, BigDecimal admissionFee,
        String description, Boolean active) {}

    public record AiStudioSettingDto(Long id, String tenantId, @NotNull Long branchId,
        @NotBlank String settingKey, @NotBlank String settingValue) {}

    public record AiChatFaqDto(Long id, String tenantId, @NotNull Long branchId, @NotBlank String question,
        @NotBlank String answer, String keywords, Integer sortOrder, Boolean active) {}

    public record AiChatPolicyDto(Long id, String tenantId, @NotNull Long branchId, @NotBlank String title,
        @NotBlank String body, String category, Boolean active) {}

    public record AiChatOfferDto(Long id, String tenantId, @NotNull Long branchId, @NotBlank String title,
        @NotBlank String body, LocalDate validFrom, LocalDate validUntil, Boolean active) {}

    public record BranchOptionDto(Long id, String name, String slug) {}

    public record ChatMessageRequest(String branch, Long conversationId, String visitorId, String message) {}

    public record ChatMessageResponse(Long conversationId, String visitorId, String reply, String intent,
        boolean leadPrompt, List<String> quickActions, List<RecommendedClassDto> recommendations) {}

    public record RecommendedClassDto(Long classId, String name, String category, String reason,
        String ageRange, String schedule, String fee) {}

    public record RecommendRequest(String branch, Integer age, String experienceLevel, String interest) {}

    public record LeadRequest(String branch, Long conversationId, String visitorId, String studentName,
        String parentName, Integer age, String phone, String email, String interestedClass,
        String preferredBatch, String preferredStartDate, String message) {}

    public record LeadResponse(Long leadId, String status, String confirmation) {}

    public record AiChatLeadDto(Long id, Long branchId, Long conversationId, String studentName,
        String parentName, Integer age, String phone, String email, String interestedClass,
        String preferredBatch, String preferredStartDate, String message, String status,
        LocalDateTime createdAt) {}

    public record AnalyticsDto(long conversations, long leads, long leadsNew, long messages,
        List<IntentCountDto> topIntents, List<LeadStatusCountDto> leadsByStatus) {}
    public record IntentCountDto(String intent, long count) {}
    public record LeadStatusCountDto(String status, long count) {}
}
