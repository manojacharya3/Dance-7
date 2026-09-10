package com.studioos.controller;

import com.studioos.dto.AttendanceDTO;
import com.studioos.dto.AttendanceMonthlySummaryDTO;
import com.studioos.dto.BatchAttendanceRequest;
import com.studioos.service.AttendanceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;
    public AttendanceController(AttendanceService attendanceService) { this.attendanceService = attendanceService; }

    @PostMapping
    public ResponseEntity<AttendanceDTO> createAttendance(@Valid @RequestBody AttendanceDTO attendanceDTO) { AttendanceDTO created = attendanceService.createAttendance(attendanceDTO); return ResponseEntity.created(URI.create("/api/attendance/" + created.id())).body(created); }
    @PostMapping("/batch")
    public List<AttendanceDTO> saveBatchAttendance(@Valid @RequestBody BatchAttendanceRequest request) { return attendanceService.saveBatchAttendance(request); }
    @PutMapping("/{id}")
    public AttendanceDTO updateAttendance(@PathVariable Long id, @Valid @RequestBody AttendanceDTO attendanceDTO) { return attendanceService.updateAttendance(id, attendanceDTO); }
    @GetMapping("/{id}")
    public AttendanceDTO getAttendance(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { return attendanceService.getAttendanceById(id, tenantId); }
    @GetMapping
    public Page<AttendanceDTO> listAttendance(@RequestParam(defaultValue = "default") String tenantId, @RequestParam(required = false) LocalDate date, @RequestParam(required = false) Long studentId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return attendanceService.listAttendance(tenantId, date, studentId, pageable(page, size)); }
    @GetMapping("/student/{studentId}")
    public Page<AttendanceDTO> getByStudent(@PathVariable Long studentId, @RequestParam(defaultValue = "default") String tenantId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return attendanceService.getAttendanceByStudent(studentId, tenantId, pageable(page, size)); }
    @GetMapping("/student/{studentId}/monthly-summary")
    public AttendanceMonthlySummaryDTO getMonthlySummary(@PathVariable Long studentId, @RequestParam(defaultValue = "default") String tenantId) { return attendanceService.getMonthlySummary(studentId, tenantId); }
    @GetMapping("/date/{attendanceDate}")
    public Page<AttendanceDTO> getByDate(@PathVariable LocalDate attendanceDate, @RequestParam(defaultValue = "default") String tenantId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return attendanceService.getAttendanceByDate(attendanceDate, tenantId, pageable(page, size)); }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attendanceDate", "updatedAt"));
    }
}