package com.studioos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_batches", indexes = {
    @Index(name = "idx_student_batches_tenant_batch", columnList = "tenant_id,batch_id"),
    @Index(name = "idx_student_batches_tenant_student", columnList = "tenant_id,student_id")
})
public class StudentBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "batch_id", nullable = false) private Long batchId;
    @Column(nullable = false, updatable = false) private LocalDateTime assignedAt;
    @Column(nullable = false) private boolean active = true;

    @jakarta.persistence.PrePersist void onCreate() { assignedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}