package com.studioos.dto;

import com.studioos.model.Feedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record FeedbackDTO(
    Long id,
    String tenantId,
    @NotBlank(message = "Title is required") @Size(max = 200, message = "Title must be at most 200 characters") String title,
    @NotBlank(message = "Description is required") String description,
    @NotNull(message = "Category is required") Feedback.Category category,
    Feedback.Priority priority,
    Feedback.Status status,
    String createdBy,
    @NotNull(message = "Branch is required") Long branchId,
    String internalNotes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
