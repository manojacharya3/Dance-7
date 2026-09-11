package com.studioos.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Invoice email delivery. Best-effort by design: failures are logged and never
 * fail the payment itself. Disabled unless dance7.email.enabled=true with SMTP
 * configured (spring.mail.*).
 */
@Service
public class InvoiceEmailService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public InvoiceEmailService(JavaMailSender mailSender,
        @Value("${dance7.email.enabled:false}") boolean enabled,
        @Value("${dance7.email.from:Dance7 <no-reply@dance7.studio>}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public boolean isEnabled() { return enabled; }

    /** @return true if the email was accepted for delivery. */
    public boolean sendInvoice(String to, String studentName, String invoiceNumber, String amount, byte[] pdf) {
        if (!enabled) {
            log.info("Invoice email skipped (email disabled) for invoice {}.", invoiceNumber);
            return false;
        }
        if (to == null || to.isBlank() || pdf == null) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to.trim());
            helper.setSubject("Payment Confirmation - Dance7");
            helper.setText("Hi " + (studentName == null || studentName.isBlank() ? "there" : studentName) + ",\n\n"
                + "Thank you for your payment of Rs. " + amount + ".\n"
                + "Invoice number: " + invoiceNumber + "\n\n"
                + "Your receipt is attached.\n\n— Dance7, The Art Factory", false);
            helper.addAttachment(invoiceNumber + ".pdf", new ByteArrayResource(pdf));
            mailSender.send(message);
            log.info("Invoice email sent for {}.", invoiceNumber);
            return true;
        } catch (Exception e) {
            log.warn("Invoice email failed for {}: {}", invoiceNumber, e.getClass().getSimpleName());
            return false;
        }
    }
}
