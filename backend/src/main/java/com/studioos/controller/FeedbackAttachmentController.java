package com.studioos.controller;

import com.studioos.model.FeedbackAttachment;
import com.studioos.service.FeedbackAttachmentService;
import com.studioos.service.FeedbackAttachmentService.AttachmentMeta;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Screenshot attachments for feedback reports. All routes require staff auth. */
@RestController
public class FeedbackAttachmentController {

    private final FeedbackAttachmentService service;

    public FeedbackAttachmentController(FeedbackAttachmentService service) { this.service = service; }

    @PostMapping("/api/feedback/{id}/attachments")
    public ResponseEntity<AttachmentMeta> upload(@PathVariable Long id,
        @RequestParam(defaultValue = "default") String tenantId,
        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.upload(tenantId, id, file));
    }

    @GetMapping("/api/feedback/{id}/attachments")
    public List<AttachmentMeta> list(@PathVariable Long id,
        @RequestParam(defaultValue = "default") String tenantId) {
        return service.list(tenantId, id);
    }

    @GetMapping("/api/feedback/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long attachmentId,
        @RequestParam(defaultValue = "default") String tenantId) {
        FeedbackAttachment attachment = service.content(tenantId, attachmentId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(attachment.getFileName()).build().toString())
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .contentLength(attachment.getData() == null ? 0 : attachment.getData().length)
            .body(attachment.getData());
    }

    @DeleteMapping("/api/feedback/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long attachmentId,
        @RequestParam(defaultValue = "default") String tenantId) {
        service.delete(tenantId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    // Back-compat alias used by older clients: /api/feedback/attachments?feedbackId=
    @GetMapping(value = "/api/feedback/attachments", params = "feedbackId")
    public List<AttachmentMeta> listByQuery(@RequestParam Long feedbackId,
        @RequestParam(defaultValue = "default") String tenantId) {
        return service.list(tenantId, feedbackId);
    }
}
