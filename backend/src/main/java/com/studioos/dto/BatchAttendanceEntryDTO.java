package com.studioos.dto;

import com.studioos.model.AttendanceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BatchAttendanceEntryDTO(Long id, @NotNull Long studentId, @NotNull AttendanceStatus status, @Min(0) @Max(10) Integer performanceScore, String remarks) {}