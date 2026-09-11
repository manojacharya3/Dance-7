package com.studioos.service;

import com.studioos.model.FeedbackAttachment;
import com.studioos.repository.FeedbackAttachmentRepository;
import com.studioos.repository.FeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Screenshot storage for feedback reports. Defense in depth: extension +
 * declared content-type allowlists, 10 MB cap, 5-per-feedback cap, and magic-
 * byte sniffing (PNG/JPEG/WEBP) so renamed executables/scripts are rejected
 * even when headers lie.
 */
@Service
public class FeedbackAttachmentService {
    private static final Logger log = LoggerFactory.getLogger(FeedbackAttachmentService.class);
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    public static final int MAX_PER_FEEDBACK = 5;
    static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg", "image/webp");
    static final List<String> ALLOWED_EXTENSIONS = List.of("png", "jpg", "jpeg", "webp");

    public record AttachmentMeta(Long id, String fileName, String fileUrl, String contentType, Long fileSize, String uploadedAt) {}

    private final FeedbackAttachmentRepository attachments;
    private final FeedbackRepository feedback;

    public FeedbackAttachmentService(FeedbackAttachmentRepository attachments, FeedbackRepository feedback) {
        this.attachments = attachments;
        this.feedback = feedback;
    }

    @Transactional
    public AttachmentMeta upload(String tenant, Long feedbackId, MultipartFile file) {
        feedback.findByIdAndTenantId(feedbackId, tenant)
            .orElseThrow(() -> new EntityNotFoundException("Feedback not found: " + feedbackId));
        if (attachments.countByFeedbackIdAndTenantId(feedbackId, tenant) >= MAX_PER_FEEDBACK)
            throw new IllegalArgumentException("Maximum " + MAX_PER_FEEDBACK + " screenshots per feedback.");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file received.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File exceeds the 10 MB limit.");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!ALLOWED_TYPES.contains(contentType))
            throw new IllegalArgumentException("Only PNG, JPG and WEBP images are accepted.");
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension))
            throw new IllegalArgumentException("Only .png, .jpg, .jpeg and .webp files are accepted.");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read the uploaded file.");
        }
        if (!matchesMagicBytes(bytes, contentType))
            throw new IllegalArgumentException("File content does not match its image type.");
        FeedbackAttachment attachment = new FeedbackAttachment();
        attachment.setTenantId(tenant);
        attachment.setFeedbackId(feedbackId);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setFileSize((long) bytes.length);
        attachment.setData(bytes);
        FeedbackAttachment saved = attachments.save(attachment);
        log.info("Feedback {} screenshot stored ({} bytes).", feedbackId, bytes.length);
        return toMeta(tenant, saved);
    }

    @Transactional(readOnly = true)
    public List<AttachmentMeta> list(String tenant, Long feedbackId) {
        feedback.findByIdAndTenantId(feedbackId, tenant)
            .orElseThrow(() -> new EntityNotFoundException("Feedback not found: " + feedbackId));
        return attachments.findByFeedbackIdAndTenantIdOrderByCreatedAtAsc(feedbackId, tenant)
            .stream().map(a -> toMeta(tenant, a)).toList();
    }

    @Transactional(readOnly = true)
    public FeedbackAttachment content(String tenant, Long attachmentId) {
        return attachments.findByIdAndTenantId(attachmentId, tenant)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));
    }

    @Transactional
    public void delete(String tenant, Long attachmentId) {
        FeedbackAttachment attachment = content(tenant, attachmentId);
        attachments.delete(attachment);
    }

    static String sanitizeFileName(String raw) {
        String name = raw == null ? "screenshot" : raw.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..")) name = "screenshot";
        if (name.length() > 200) name = name.substring(name.length() - 200);
        return name;
    }

    static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Sniffs PNG / JPEG / WEBP magic bytes; rejects everything else (incl. scripts). */
    static boolean matchesMagicBytes(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length < 12) return false;
        int b0 = bytes[0] & 0xFF, b1 = bytes[1] & 0xFF, b2 = bytes[2] & 0xFF, b3 = bytes[3] & 0xFF;
        return switch (contentType) {
            case "image/png" -> b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47;
            case "image/jpeg" -> b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF;
            case "image/webp" -> b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
            default -> false;
        };
    }

    private AttachmentMeta toMeta(String tenant, FeedbackAttachment attachment) {
        return new AttachmentMeta(attachment.getId(), attachment.getFileName(),
            "/api/feedback/attachments/" + attachment.getId() + "/content?tenantId=" + tenant,
            attachment.getContentType(), attachment.getFileSize(),
            attachment.getCreatedAt() == null ? null : attachment.getCreatedAt().toString());
    }
}
