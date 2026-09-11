package com.studioos.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** A weekly schedule slot for a class at a branch. Branch-scoped. */
@Entity
@Table(name = "ai_class_schedules")
public class AiClassSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(name = "ai_class_id", nullable = false) private Long aiClassId;
    @Column(name = "day_of_week", nullable = false, length = 20) private String dayOfWeek;
    @Column(name = "start_time", nullable = false, length = 10) private String startTime;
    @Column(name = "end_time", nullable = false, length = 10) private String endTime;
    @Column(name = "batch_label", length = 160) private String batchLabel;
    @Column(name = "instructor_name", length = 160) private String instructorName;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getAiClassId() { return aiClassId; }
    public void setAiClassId(Long aiClassId) { this.aiClassId = aiClassId; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getBatchLabel() { return batchLabel; }
    public void setBatchLabel(String batchLabel) { this.batchLabel = batchLabel; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
