package com.studioos.service;

import com.studioos.dto.StudentBatchDTO;
import com.studioos.model.StudentBatch;
import com.studioos.repository.BatchRepository;
import com.studioos.repository.StudentBatchRepository;
import com.studioos.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class StudentBatchServiceImpl implements StudentBatchService {
    private final StudentBatchRepository repository; private final StudentRepository studentRepository; private final BatchRepository batchRepository;
    public StudentBatchServiceImpl(StudentBatchRepository repository, StudentRepository studentRepository, BatchRepository batchRepository) { this.repository = repository; this.studentRepository = studentRepository; this.batchRepository = batchRepository; }
    public StudentBatchDTO assign(StudentBatchDTO dto) { String t = tenant(dto.tenantId()); if (studentRepository.findByIdAndTenantIdAndActiveTrue(dto.studentId(), t).isEmpty()) throw new EntityNotFoundException("Student not found: " + dto.studentId()); if (batchRepository.findByIdAndTenantIdAndActiveTrue(dto.batchId(), t).isEmpty()) throw new EntityNotFoundException("Batch not found: " + dto.batchId()); StudentBatch existing = repository.findByTenantIdAndStudentIdAndBatchIdAndActiveTrue(t, dto.studentId(), dto.batchId()).orElse(null); if (existing != null) return toDto(existing); StudentBatch entity = new StudentBatch(); entity.setTenantId(t); entity.setStudentId(dto.studentId()); entity.setBatchId(dto.batchId()); return toDto(repository.save(entity)); }
    public void remove(Long studentId, Long batchId, String tenantId) { repository.findByTenantIdAndStudentIdAndBatchIdAndActiveTrue(tenant(tenantId), studentId, batchId).ifPresent(entity -> { entity.setActive(false); repository.save(entity); }); }
    @Transactional(readOnly = true) public List<StudentBatchDTO> byBatch(Long batchId, String tenantId) { return repository.findByTenantIdAndBatchIdAndActiveTrue(tenant(tenantId), batchId).stream().map(this::toDto).toList(); }
    @Transactional(readOnly = true) public List<StudentBatchDTO> byStudent(Long studentId, String tenantId) { return repository.findByTenantIdAndStudentIdAndActiveTrue(tenant(tenantId), studentId).stream().map(this::toDto).toList(); }
    private StudentBatchDTO toDto(StudentBatch e) { return new StudentBatchDTO(e.getId(), e.getTenantId(), e.getStudentId(), e.getBatchId(), e.getAssignedAt()); }
    private String tenant(String value) { return value == null || value.isBlank() ? "default" : value.trim(); }
}