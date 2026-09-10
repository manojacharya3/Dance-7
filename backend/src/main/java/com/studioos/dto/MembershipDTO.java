package com.studioos.dto;

import com.studioos.model.MembershipStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MembershipDTO(
    Long id,
    String tenantId,
    @NotNull(message = "Branch is required") Long branchId,
    @NotNull(message = "Student is required") Long studentId,
    @NotBlank(message = "Plan name is required") String planName,
    @NotNull @Min(value = 1, message = "Duration must be at least one month") Integer durationMonths,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal feeAmount,
    @NotNull MembershipStatus status,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
