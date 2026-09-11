package com.studioos.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Security/usage audit trail. Never stores message bodies or PII — hashes and flags only. */
@Entity
@Table(name = "ai_chat_audit")
public class AiChatAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id") private Long branchId;
    @Column(name = "conversation_id") private Long conversationId;
    @Column(nullable = false, length = 60) private String event;
    @Column(length = 500) private String detail;
    @Column(name = "ip_hash", length = 128) private String ipHash;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
