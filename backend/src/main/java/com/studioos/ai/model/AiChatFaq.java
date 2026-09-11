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

/** Branch-owned FAQ entry. keywords drive retrieval scoring. */
@Entity
@Table(name = "ai_chat_faqs")
public class AiChatFaq {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId = "default";
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(nullable = false, length = 500) private String question;
    @Column(nullable = false, length = 4000) private String answer;
    @Column(length = 500) private String keywords;
    @Column(name = "sort_order", nullable = false) private int sortOrder = 0;
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
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
