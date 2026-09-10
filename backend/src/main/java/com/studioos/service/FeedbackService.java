package com.studioos.service;

import com.studioos.config.ScopeService;
import com.studioos.dto.FeedbackDTO;
import com.studioos.model.Feedback;
import com.studioos.model.User;
import com.studioos.repository.BranchRepository;
import com.studioos.repository.FeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final BranchRepository branchRepository;
    private final ScopeService scope;

    public FeedbackService(FeedbackRepository feedbackRepository, BranchRepository branchRepository, ScopeService scope) {
        this.feedbackRepository = feedbackRepository;
        this.branchRepository = branchRepository;
        this.scope = scope;
    }

    public FeedbackDTO create(FeedbackDTO dto) {
        User user = requireAuthenticatedUser();
        String tenantId = tenant(dto.tenantId(), user);
        Long branchId = resolveBranch(dto.branchId(), user);
        validateBranch(branchId, tenantId);
        assertCanSubmit();

        Feedback entity = new Feedback();
        entity.setTenantId(tenantId);
        entity.setBranchId(branchId);
        entity.setTitle(dto.title().trim());
        entity.setDescription(dto.description().trim());
        entity.setCategory(dto.category());
        entity.setPriority(dto.priority() == null ? Feedback.Priority.MEDIUM : dto.priority());
        entity.setStatus(Feedback.Status.OPEN);
        entity.setCreatedBy(user.getEmail());
        return toDto(feedbackRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public FeedbackDTO get(Long id, String tenantId) {
        User user = requireAuthenticatedUser();
        Feedback entity = find(id, tenant(tenantId, user));
        assertVisible(entity, user);
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDTO> list(String tenantId, String search, Feedback.Status status,
                                  Feedback.Category category, Pageable pageable) {
        User user = requireAuthenticatedUser();
        String normalizedTenant = tenant(tenantId, user);
        Long scopedBranch = scopedBranchId(user);
        return feedbackRepository
            .search(normalizedTenant, scopedBranch, status, category, search == null ? null : search.trim(), pageable)
            .map(this::toDto);
    }

    public FeedbackDTO updateStatus(Long id, String tenantId, Feedback.Status status) {
        User user = requireAuthenticatedUser();
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        Feedback entity = find(id, tenant(tenantId, user));
        assertVisible(entity, user);
        if (!scope.owner() && !scope.developer() && !entity.getCreatedBy().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the OWNER, a DEVELOPER, or the submitter can update feedback status");
        }
        entity.setStatus(status);
        return toDto(feedbackRepository.save(entity));
    }

    public FeedbackDTO updateNotes(Long id, String tenantId, String notes) {
        User user = requireAuthenticatedUser();
        Feedback entity = find(id, tenant(tenantId, user));
        assertVisible(entity, user);
        if (!scope.owner() && !scope.developer()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the OWNER or a DEVELOPER can add internal notes");
        }
        entity.setInternalNotes(notes == null || notes.isBlank() ? null : notes.trim());
        return toDto(feedbackRepository.save(entity));
    }

    private Feedback find(Long id, String tenantId) {
        return feedbackRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Feedback not found: " + id));
    }

    private void assertCanSubmit() {
        if (scope.owner() || scope.branchHead() || scope.instructor() || scope.developer()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Branch Heads and Instructors can submit feedback");
    }

    private void assertVisible(Feedback entity, User user) {
        if (scope.owner() || scope.developer()) {
            return;
        }
        Long scopedBranch = scopedBranchId(user);
        if (scopedBranch != null && !scopedBranch.equals(entity.getBranchId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this branch");
        }
        if (scope.instructor() && !entity.getCreatedBy().equalsIgnoreCase(user.getEmail())
                && (user.getBranchId() == null || !user.getBranchId().equals(entity.getBranchId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Instructors can only view feedback from their assigned branch");
        }
    }

    private Long scopedBranchId(User user) {
        if (scope.owner() || scope.developer()) {
            return null;
        }
        return user.getBranchId();
    }

    private Long resolveBranch(Long requested, User user) {
        if (scope.owner() || scope.developer()) {
            if (requested == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is required");
            }
            return requested;
        }
        if (user.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your user is not assigned to a branch");
        }
        if (requested != null && !requested.equals(user.getBranchId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this branch");
        }
        return user.getBranchId();
    }

    private void validateBranch(Long branchId, String tenantId) {
        if (branchRepository.findByIdAndTenantIdAndActiveTrue(branchId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("Branch not found: " + branchId);
        }
    }

    private User requireAuthenticatedUser() {
        User user = scope.currentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return user;
    }

    private String tenant(String value, User user) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return user.getTenantId() == null || user.getTenantId().isBlank() ? "default" : user.getTenantId().trim();
    }

    private FeedbackDTO toDto(Feedback entity) {
        return new FeedbackDTO(
            entity.getId(), entity.getTenantId(), entity.getTitle(), entity.getDescription(),
            entity.getCategory(), entity.getPriority(), entity.getStatus(),
            entity.getCreatedBy(), entity.getBranchId(), entity.getInternalNotes(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
