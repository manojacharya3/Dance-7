package com.studioos.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record StudentBatchDTO(Long id, String tenantId, @NotNull Long studentId, @NotNull Long batchId, LocalDateTime assignedAt) {}