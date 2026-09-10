package com.studioos.service;

import com.studioos.dto.StudentBatchDTO;
import java.util.List;

public interface StudentBatchService {
    StudentBatchDTO assign(StudentBatchDTO dto);
    void remove(Long studentId, Long batchId, String tenantId);
    List<StudentBatchDTO> byBatch(Long batchId, String tenantId);
    List<StudentBatchDTO> byStudent(Long studentId, String tenantId);
}