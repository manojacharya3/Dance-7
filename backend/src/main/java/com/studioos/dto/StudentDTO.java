package com.studioos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentDTO(
    Long id,
    String tenantId,
    @NotNull(message = "Branch is required") Long branchId,
    @NotBlank(message = "First name is required") String firstName,
    @NotBlank(message = "Last name is required") String lastName,
    LocalDate dateOfBirth,
    String gender,
    @Email(message = "Email must be valid") String email,
    String phone,
    String address,
    String emergencyContact,
    String parentName,
    String parentPhone,
    String danceStyle,
    String skillLevel,
    String medicalNotes,
    String studentPhotoUrl,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
