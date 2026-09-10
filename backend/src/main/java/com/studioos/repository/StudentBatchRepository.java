package com.studioos.repository;

import com.studioos.model.StudentBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentBatchRepository extends JpaRepository<StudentBatch, Long> {
    Optional<StudentBatch> findByTenantIdAndStudentIdAndBatchIdAndActiveTrue(String tenantId, Long studentId, Long batchId);
    List<StudentBatch> findByTenantIdAndBatchIdAndActiveTrue(String tenantId, Long batchId);
    List<StudentBatch> findByTenantIdAndStudentIdAndActiveTrue(String tenantId, Long studentId);
}