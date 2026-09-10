package com.studioos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BatchDTO(Long id, String tenantId, Long branchId, @NotBlank String batchName, @NotNull Long instructorId, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @NotNull @Min(1) Integer capacity, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {}