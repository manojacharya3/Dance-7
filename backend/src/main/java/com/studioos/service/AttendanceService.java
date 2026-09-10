package com.studioos.service;

import com.studioos.dto.AttendanceDTO;
import com.studioos.dto.AttendanceMonthlySummaryDTO;
import com.studioos.dto.BatchAttendanceRequest;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AttendanceService {
    AttendanceDTO createAttendance(AttendanceDTO attendanceDTO);
    AttendanceDTO updateAttendance(Long id, AttendanceDTO attendanceDTO);
    AttendanceDTO getAttendanceById(Long id, String tenantId);
    Page<AttendanceDTO> listAttendance(String tenantId, LocalDate attendanceDate, Long studentId, Pageable pageable);
    Page<AttendanceDTO> getAttendanceByStudent(Long studentId, String tenantId, Pageable pageable);
    Page<AttendanceDTO> getAttendanceByDate(LocalDate attendanceDate, String tenantId, Pageable pageable);
    AttendanceMonthlySummaryDTO getMonthlySummary(Long studentId, String tenantId);
    List<AttendanceDTO> saveBatchAttendance(BatchAttendanceRequest request);
}