package com.studioos.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** One chat message. intent labels the assistant turn for analytics. */
@Entity
@Table(name = "ai_chat_messages")
public class AiChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "conversation_id", nullable = false) private Long conversationId;
    @Column(nullable = false, length = 20) private String role;
    @Column(nullable = false, length = 4000) private String content;
    @Column(length = 60) private String intent;
    @Column(name = "tool_name", length = 80) private String toolName;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
