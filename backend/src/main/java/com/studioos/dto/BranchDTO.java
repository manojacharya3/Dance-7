package com.studioos.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record BranchDTO(Long id, String tenantId, @NotBlank String name, String address, String phone, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {}
