package com.studioos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserManagementDTO(Long id, String tenantId, @NotBlank String fullName, @Email @NotBlank String email, String password, @NotBlank String role, Long branchId, Long instructorId, Boolean enabled) {}
