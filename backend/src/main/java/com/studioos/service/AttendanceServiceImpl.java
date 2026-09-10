package com.studioos.service;

import com.studioos.dto.AttendanceDTO;
import com.studioos.dto.AttendanceMonthlySummaryDTO;
import com.studioos.dto.BatchAttendanceEntryDTO;
import com.studioos.dto.BatchAttendanceRequest;
import com.studioos.mapper.AttendanceMapper;
import com.studioos.model.Attendance;
import com.studioos.model.AttendanceStatus;
import com.studioos.model.Student;
import com.studioos.repository.AttendanceRepository;
import com.studioos.repository.StudentRepository;
import com.studioos.repository.BatchRepository;
import com.studioos.repository.StudentBatchRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;
    private final StudentRepository studentRepository;
    private final BatchRepository batchRepository;
    private final StudentBatchRepository studentBatchRepository;

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository, AttendanceMapper attendanceMapper, StudentRepository studentRepository, BatchRepository batchRepository, StudentBatchRepository studentBatchRepository) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
        this.studentRepository = studentRepository;
        this.batchRepository = batchRepository;
        this.studentBatchRepository = studentBatchRepository;
    }

    @Override
    public AttendanceDTO createAttendance(AttendanceDTO attendanceDTO) {
        String tenantId = tenantIdFrom(attendanceDTO.tenantId());
        requireActiveStudent(attendanceDTO.studentId(), tenantId);
        Attendance attendance = attendanceMapper.toEntity(attendanceDTO);
        attendance.setTenantId(tenantId);
        return attendanceMapper.toDto(attendanceRepository.save(attendance));
    }

    @Override
    public AttendanceDTO updateAttendance(Long id, AttendanceDTO attendanceDTO) {
        String tenantId = tenantIdFrom(attendanceDTO.tenantId());
        requireActiveStudent(attendanceDTO.studentId(), tenantId);
        Attendance attendance = findAttendance(id, tenantId);
        attendanceMapper.updateEntity(attendance, attendanceDTO);
        return attendanceMapper.toDto(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDTO getAttendanceById(Long id, String tenantId) { return attendanceMapper.toDto(findAttendance(id, tenantIdFrom(tenantId))); }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDTO> listAttendance(String tenantId, LocalDate attendanceDate, Long studentId, Pageable pageable) {
        String normalizedTenant = tenantIdFrom(tenantId);
        Page<Attendance> records = attendanceDate != null
            ? attendanceRepository.findByTenantIdAndAttendanceDate(normalizedTenant, attendanceDate, pageable)
            : studentId != null ? attendanceRepository.findByTenantIdAndStudentId(normalizedTenant, studentId, pageable) : attendanceRepository.findByTenantId(normalizedTenant, pageable);
        return records.map(attendanceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDTO> getAttendanceByStudent(Long studentId, String tenantId, Pageable pageable) { return attendanceRepository.findByTenantIdAndStudentId(tenantIdFrom(tenantId), studentId, pageable).map(attendanceMapper::toDto); }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDTO> getAttendanceByDate(LocalDate attendanceDate, String tenantId, Pageable pageable) { return attendanceRepository.findByTenantIdAndAttendanceDate(tenantIdFrom(tenantId), attendanceDate, pageable).map(attendanceMapper::toDto); }

    @Override
    @Transactional(readOnly = true)
    public AttendanceMonthlySummaryDTO getMonthlySummary(Long studentId, String tenantId) {
        String normalizedTenant = tenantIdFrom(tenantId);
        Student student = studentRepository.findByIdAndTenantIdAndActiveTrue(studentId, normalizedTenant)
            .orElseThrow(() -> new EntityNotFoundException("Active student not found: " + studentId));
        YearMonth month = YearMonth.now();
        List<Attendance> records = attendanceRepository.findByTenantIdAndStudentIdAndAttendanceDateBetween(
            normalizedTenant, studentId, month.atDay(1), month.atEndOfMonth());
        long presentCount = records.stream().filter(record -> record.getStatus() == AttendanceStatus.PRESENT).count();
        double averagePerformanceScore = records.stream()
            .map(Attendance::getPerformanceScore)
            .filter(score -> score != null)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
        double attendancePercentage = records.isEmpty() ? 0.0 : presentCount * 100.0 / records.size();
        return new AttendanceMonthlySummaryDTO(
            student.getFirstName() + " " + student.getLastName(),
            records.size(),
            presentCount,
            attendancePercentage,
            averagePerformanceScore);
    }

    @Override
    public List<AttendanceDTO> saveBatchAttendance(BatchAttendanceRequest request) {
        String tenantId = tenantIdFrom(request.tenantId());
        if (batchRepository.findByIdAndTenantIdAndActiveTrue(request.batchId(), tenantId).isEmpty()) throw new EntityNotFoundException("Batch not found: " + request.batchId());
        List<Attendance> records = new ArrayList<>();
        for (BatchAttendanceEntryDTO entry : request.entries()) {
            if (studentBatchRepository.findByTenantIdAndStudentIdAndBatchIdAndActiveTrue(tenantId, entry.studentId(), request.batchId()).isEmpty()) throw new EntityNotFoundException("Student is not assigned to batch: " + entry.studentId());
            Attendance attendance = entry.id() == null
                ? attendanceRepository.findByTenantIdAndStudentIdAndBatchIdAndAttendanceDate(tenantId, entry.studentId(), request.batchId(), request.attendanceDate()).orElseGet(Attendance::new)
                : findAttendance(entry.id(), tenantId);
            AttendanceDTO dto = new AttendanceDTO(attendance.getId(), tenantId, entry.studentId(), request.batchId(), request.attendanceDate(), entry.status(), entry.performanceScore(), entry.remarks(), null, null);
            attendanceMapper.updateEntity(attendance, dto);
            records.add(attendance);
        }
        return attendanceRepository.saveAll(records).stream().map(attendanceMapper::toDto).toList();
    }

    private Attendance findAttendance(Long id, String tenantId) { return attendanceRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> new EntityNotFoundException("Attendance not found: " + id)); }
    private void requireActiveStudent(Long studentId, String tenantId) { if (studentRepository.findByIdAndTenantIdAndActiveTrue(studentId, tenantId).isEmpty()) throw new EntityNotFoundException("Active student not found: " + studentId); }
    private String tenantIdFrom(String tenantId) { return tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim(); }
}