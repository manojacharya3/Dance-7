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

/** A class/product offered at a branch (e.g. Kids Batch, Adult Batch, Bharatanatyam). Branch-scoped. */
@Entity
@Table(name = "ai_classes")
public class AiClass {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 60) private String category;
    @Column(name = "min_age") private Integer minAge;
    @Column(name = "max_age") private Integer maxAge;
    @Column(name = "experience_level", length = 40) private String experienceLevel;
    @Column(length = 2000) private String description;
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }
    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
