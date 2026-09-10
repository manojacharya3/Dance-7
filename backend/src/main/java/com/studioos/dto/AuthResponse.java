package com.studioos.dto;

import java.util.Set;

public record AuthResponse(Long id, String email, String fullName, String tenantId, Set<String> roles, Long branchId, Long instructorId) {}
