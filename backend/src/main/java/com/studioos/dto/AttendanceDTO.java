package com.studioos.dto;

import com.studioos.model.AttendanceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceDTO(
    Long id,
    String tenantId,
    @NotNull(message = "Student is required") Long studentId,
    Long batchId,
    @NotNull(message = "Attendance date is required") LocalDate attendanceDate,
    @NotNull(message = "Attendance status is required") AttendanceStatus status,
    @Min(value = 0, message = "Performance score must be at least 0")
    @Max(value = 10, message = "Performance score must be at most 10") Integer performanceScore,
    String remarks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}