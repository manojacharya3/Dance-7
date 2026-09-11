package com.studioos.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A fee package at a branch. Single source of truth for all fee answers. Branch-scoped. */
@Entity
@Table(name = "ai_packages")
public class AiPackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "duration_months") private Integer durationMonths;
    @Column(name = "fee_amount", precision = 12, scale = 2) private BigDecimal feeAmount;
    @Column(name = "admission_fee", precision = 12, scale = 2) private BigDecimal admissionFee;
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
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getAdmissionFee() { return admissionFee; }
    public void setAdmissionFee(BigDecimal admissionFee) { this.admissionFee = admissionFee; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
