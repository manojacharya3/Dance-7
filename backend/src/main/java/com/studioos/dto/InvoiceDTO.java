package com.studioos.dto;

import com.studioos.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceDTO(
    Long id,
    String tenantId,
    String invoiceNumber,
    @NotNull Long paymentId,
    @NotNull Long studentId,
    @NotNull Long branchId,
    @NotNull BigDecimal amount,
    @NotNull PaymentMethod paymentMethod,
    String transactionReference,
    @NotNull LocalDate invoiceDate,
    @NotBlank String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
