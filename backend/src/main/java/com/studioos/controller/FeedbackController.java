package com.studioos.controller;

import com.studioos.dto.FeedbackDTO;
import com.studioos.model.Feedback;
import com.studioos.service.FeedbackService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<FeedbackDTO> create(@Valid @RequestBody FeedbackDTO dto) {
        FeedbackDTO created = feedbackService.create(dto);
        return ResponseEntity.created(URI.create("/api/feedback/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public FeedbackDTO get(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) {
        return feedbackService.get(id, tenantId);
    }

    @GetMapping
    public Page<FeedbackDTO> list(
        @RequestParam(defaultValue = "default") String tenantId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Feedback.Status status,
        @RequestParam(required = false) Feedback.Category category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return feedbackService.list(tenantId, search, status, category, pageable);
    }

    @PatchMapping("/{id}/status")
    public FeedbackDTO updateStatus(@PathVariable Long id,
                                    @RequestParam(defaultValue = "default") String tenantId,
                                    @RequestBody Map<String, String> body) {
        String raw = body == null ? null : body.get("status");
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        Feedback.Status status;
        try {
            status = Feedback.Status.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + raw);
        }
        return feedbackService.updateStatus(id, tenantId, status);
    }

    @PatchMapping("/{id}/notes")
    public FeedbackDTO updateNotes(@PathVariable Long id,
                                   @RequestParam(defaultValue = "default") String tenantId,
                                   @RequestBody Map<String, String> body) {
        String notes = body == null ? null : body.get("notes");
        return feedbackService.updateNotes(id, tenantId, notes);
    }
}
