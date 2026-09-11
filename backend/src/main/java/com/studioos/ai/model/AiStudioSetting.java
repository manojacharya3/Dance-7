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

/** Per-branch key/value settings (admission_fee, contact_phone, trial_info, ...). Branch-scoped. */
@Entity
@Table(name = "ai_studio_settings")
public class AiStudioSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(name = "setting_key", nullable = false, length = 120) private String settingKey;
    @Column(name = "setting_value", nullable = false, length = 2000) private String settingValue;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
