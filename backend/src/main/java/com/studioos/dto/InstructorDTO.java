package com.studioos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InstructorDTO(Long id, String tenantId, @NotNull Long branchId, @NotBlank String firstName, @NotBlank String lastName, @Email @NotBlank String email, String phone, String specialization, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {}