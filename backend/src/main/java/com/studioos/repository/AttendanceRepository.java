package com.studioos.repository;

import com.studioos.model.Attendance;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Page<Attendance> findByTenantId(String tenantId, Pageable pageable);
    Page<Attendance> findByTenantIdAndAttendanceDate(String tenantId, LocalDate attendanceDate, Pageable pageable);
    Page<Attendance> findByTenantIdAndStudentId(String tenantId, Long studentId, Pageable pageable);
    Optional<Attendance> findByIdAndTenantId(Long id, String tenantId);
    List<Attendance> findByTenantIdAndStudentIdAndAttendanceDateBetween(String tenantId, Long studentId, LocalDate startDate, LocalDate endDate);
    Optional<Attendance> findByTenantIdAndStudentIdAndBatchIdAndAttendanceDate(String tenantId, Long studentId, Long batchId, LocalDate attendanceDate);
}