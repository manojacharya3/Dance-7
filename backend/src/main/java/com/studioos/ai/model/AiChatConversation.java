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

/** A visitor chat session. Ownership enforced via visitor_id token. */
@Entity
@Table(name = "ai_chat_conversations")
public class AiChatConversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(name = "visitor_id", nullable = false, length = 80) private String visitorId;
    @Column(length = 30) private String channel = "WIDGET";
    @Column(nullable = false, length = 30) private String status = "OPEN";
    @Column(name = "lead_captured", nullable = false) private boolean leadCaptured = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isLeadCaptured() { return leadCaptured; }
    public void setLeadCaptured(boolean leadCaptured) { this.leadCaptured = leadCaptured; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
