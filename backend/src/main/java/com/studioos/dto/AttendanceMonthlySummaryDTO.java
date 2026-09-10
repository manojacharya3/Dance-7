package com.studioos.dto;

public record AttendanceMonthlySummaryDTO(
    String studentName,
    long totalClasses,
    long presentCount,
    double attendancePercentage,
    double averagePerformanceScore
) {}