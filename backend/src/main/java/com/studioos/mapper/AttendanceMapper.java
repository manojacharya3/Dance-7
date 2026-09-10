package com.studioos.mapper;

import com.studioos.dto.AttendanceDTO;
import com.studioos.model.Attendance;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {
    public AttendanceDTO toDto(Attendance attendance) {
        return new AttendanceDTO(attendance.getId(), attendance.getTenantId(), attendance.getStudentId(), attendance.getBatchId(), attendance.getAttendanceDate(), attendance.getStatus(), attendance.getPerformanceScore(), attendance.getRemarks(), attendance.getCreatedAt(), attendance.getUpdatedAt());
    }

    public Attendance toEntity(AttendanceDTO dto) {
        Attendance attendance = new Attendance();
        updateEntity(attendance, dto);
        return attendance;
    }

    public void updateEntity(Attendance attendance, AttendanceDTO dto) {
        attendance.setTenantId(dto.tenantId() == null || dto.tenantId().isBlank() ? "default" : dto.tenantId().trim());
        attendance.setStudentId(dto.studentId());
        attendance.setBatchId(dto.batchId());
        attendance.setAttendanceDate(dto.attendanceDate());
        attendance.setStatus(dto.status());
        attendance.setPerformanceScore(dto.performanceScore());
        attendance.setRemarks(dto.remarks());
    }
}