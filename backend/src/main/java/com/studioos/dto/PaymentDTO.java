package com.studioos.dto;

import com.studioos.model.PaymentMethod;
import com.studioos.model.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentDTO(
    Long id,
    String tenantId,
    Long branchId,
    @NotNull(message = "Membership is required") Long membershipId,
    @NotNull(message = "Student is required") Long studentId,
    @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero") BigDecimal amount,
    @NotNull LocalDate paymentDate,
    @NotNull PaymentMethod paymentMethod,
    @NotNull PaymentStatus paymentStatus,
    String remarks,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
