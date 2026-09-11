package com.studioos.repository;

import com.studioos.model.FeedbackAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, Long> {
    List<FeedbackAttachment> findByFeedbackIdAndTenantIdOrderByCreatedAtAsc(Long feedbackId, String tenantId);
    Optional<FeedbackAttachment> findByIdAndTenantId(Long id, String tenantId);
    long countByFeedbackIdAndTenantId(Long feedbackId, String tenantId);
}
