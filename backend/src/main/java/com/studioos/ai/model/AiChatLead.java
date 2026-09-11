package com.studioos.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** A captured front-desk lead. PII lives here only — never in audit logs. */
@Entity
@Table(name = "ai_chat_leads")
public class AiChatLead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(name = "conversation_id") private Long conversationId;
    @Column(name = "student_name", length = 160) private String studentName;
    @Column(name = "parent_name", length = 160) private String parentName;
    private Integer age;
    @Column(length = 30) private String phone;
    @Column(length = 160) private String email;
    @Column(name = "interested_class", length = 160) private String interestedClass;
    @Column(name = "preferred_batch", length = 160) private String preferredBatch;
    @Column(name = "preferred_start_date", length = 40) private String preferredStartDate;
    @Column(length = 2000) private String message;
    @Column(nullable = false, length = 30) private String status = "NEW";
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); if (status == null) status = "NEW"; }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getInterestedClass() { return interestedClass; }
    public void setInterestedClass(String interestedClass) { this.interestedClass = interestedClass; }
    public String getPreferredBatch() { return preferredBatch; }
    public void setPreferredBatch(String preferredBatch) { this.preferredBatch = preferredBatch; }
    public String getPreferredStartDate() { return preferredStartDate; }
    public void setPreferredStartDate(String preferredStartDate) { this.preferredStartDate = preferredStartDate; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
