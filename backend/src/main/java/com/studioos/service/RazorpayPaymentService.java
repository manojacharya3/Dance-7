package com.studioos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.dto.InvoiceDTO;
import com.studioos.model.Branch;
import com.studioos.model.Invoice;
import com.studioos.model.Membership;
import com.studioos.model.MembershipStatus;
import com.studioos.model.Payment;
import com.studioos.model.PaymentEvent;
import com.studioos.model.PaymentMethod;
import com.studioos.model.PaymentStatus;
import com.studioos.model.Student;
import com.studioos.repository.BranchRepository;
import com.studioos.repository.InvoiceRepository;
import com.studioos.repository.MembershipRepository;
import com.studioos.repository.PaymentEventRepository;
import com.studioos.repository.PaymentRepository;
import com.studioos.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-side payment truth: nothing is marked PAID without a verified
 * Razorpay signature (checkout verify or webhook). All capture paths funnel
 * through {@link #capture} exactly once (duplicate-safe), which then activates
 * the membership, generates the invoice + PDF, emails the receipt, and audits.
 */
@Service
public class RazorpayPaymentService {
    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

    private final RazorpayGateway gateway;
    private final PaymentRepository payments;
    private final MembershipRepository memberships;
    private final InvoiceRepository invoices;
    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;
    private final InvoiceEmailService emailService;
    private final StudentRepository students;
    private final BranchRepository branches;
    private final PaymentEventRepository events;
    private final ObjectMapper mapper = new ObjectMapper();

    public RazorpayPaymentService(RazorpayGateway gateway, PaymentRepository payments,
        MembershipRepository memberships, InvoiceRepository invoices, InvoiceService invoiceService,
        InvoicePdfService pdfService, InvoiceEmailService emailService, StudentRepository students,
        BranchRepository branches, PaymentEventRepository events) {
        this.gateway = gateway; this.payments = payments; this.memberships = memberships;
        this.invoices = invoices; this.invoiceService = invoiceService; this.pdfService = pdfService;
        this.emailService = emailService; this.students = students; this.branches = branches;
        this.events = events;
    }

    public record OrderResult(String orderId, long amountPaise, String currency, String keyId, Long paymentId) {}

    @Transactional
    public OrderResult createOrder(String tenant, Long paymentId) {
        gateway.requireEnabled();
        Payment payment = activePayment(tenant, paymentId);
        if (payment.getPaymentStatus() == PaymentStatus.PAID)
            throw new IllegalArgumentException("Payment is already completed.");
        if (payment.getRazorpayOrderId() != null && payment.getPaymentStatus() == PaymentStatus.PROCESSING) {
            return new OrderResult(payment.getRazorpayOrderId(), RazorpayGateway.toPaise(payment.getAmount()),
                gateway.getCurrency(), gateway.getKeyId(), payment.getId());
        }
        String receipt = "d7pay-" + payment.getId() + "-" + System.currentTimeMillis() / 1000;
        String orderId = gateway.createOrder(RazorpayGateway.toPaise(payment.getAmount()), receipt);
        payment.setRazorpayOrderId(orderId);
        payment.setReceiptNumber(receipt);
        payment.setPaymentStatus(PaymentStatus.PROCESSING);
        payments.save(payment);
        audit(tenant, payment.getId(), "ORDER_CREATED", "order " + orderId);
        log.info("Razorpay order {} created for payment {}.", orderId, payment.getId());
        return new OrderResult(orderId, RazorpayGateway.toPaise(payment.getAmount()),
            gateway.getCurrency(), gateway.getKeyId(), payment.getId());
    }

    @Transactional
    public InvoiceDTO verifyAndCapture(String tenant, String orderId, String razorpayPaymentId, String signature) {
        gateway.requireEnabled();
        Payment payment = payments.findByRazorpayOrderIdAndTenantIdAndActiveTrue(orderId, tenant)
            .orElseThrow(() -> new EntityNotFoundException("Payment not found for this order."));
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            audit(tenant, payment.getId(), "VERIFY_DUPLICATE", "already paid");
            return invoiceService.generateFromPayment(payment.getId(), tenant);
        }
        if (!gateway.verifyPaymentSignature(orderId, razorpayPaymentId, signature)) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payments.save(payment);
            audit(tenant, payment.getId(), "VERIFY_SIGNATURE_MISMATCH", "order " + orderId);
            log.warn("Razorpay signature mismatch for payment {}.", payment.getId());
            throw new SecurityException("Payment verification failed. If money was debited it will be refunded by Razorpay.");
        }
        return capture(tenant, payment, razorpayPaymentId, signature, null, "checkout");
    }

    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        boolean ok = gateway.verifyWebhookSignature(rawBody, signature);
        audit("default", null, "WEBHOOK_RECEIVED", ok ? "signature ok" : "SIGNATURE MISMATCH");
        if (!ok) throw new SecurityException("Invalid webhook signature.");
        try {
            JsonNode root = mapper.readTree(rawBody);
            String event = root.path("event").asText("");
            JsonNode entity = root.path("payload").path("payment").path("entity");
            if (entity.isMissingNode()) entity = root.path("payload").path("order").path("entity");
            String orderId = entity.path("order_id").asText(null);
            String paymentId = entity.path("id").asText(null);
            String method = entity.path("method").asText(null);
            if (orderId == null) {
                log.warn("Razorpay webhook without order reference ignored (event={}).", event);
                return;
            }
            // Webhooks carry no tenant: Razorpay order ids are globally unique, so the
            // stored payment itself is the tenant authority.
            Payment payment = payments.findByRazorpayOrderIdAndActiveTrue(orderId).orElse(null);
            if (payment == null) {
                log.warn("Razorpay webhook for unknown order ignored (event={}).", event);
                return; // ack to stop Razorpay retries
            }
            String tenant = payment.getTenantId();
            if (event.contains("payment.failed")) {
                if (payment.getPaymentStatus() != PaymentStatus.PAID) {
                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    payments.save(payment);
                    audit(tenant, payment.getId(), "WEBHOOK_FAILED", "event " + event);
                }
                return;
            }
            if (event.contains("payment.captured") || event.contains("order.paid")) {
                if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                    audit(tenant, payment.getId(), "WEBHOOK_DUPLICATE", "event " + event);
                    return;
                }
                capture(tenant, payment, paymentId, null, method, "webhook:" + event);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Razorpay webhook handling failed: {}", e.getClass().getSimpleName());
            throw new IllegalStateException("Webhook processing failed.");
        }
    }

    /** Single capture path: payment → membership → invoice → PDF → email → audit. */
    @Transactional
    public InvoiceDTO capture(String tenant, Payment payment, String razorpayPaymentId,
        String signature, String razorpayMethod, String source) {
        payment.setRazorpayPaymentId(razorpayPaymentId);
        if (signature != null) payment.setRazorpaySignature(signature);
        payment.setPaymentMethod(mapMethod(razorpayMethod, payment.getPaymentMethod()));
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payments.save(payment);
        audit(tenant, payment.getId(), "CAPTURED", source);

        Membership membership = memberships.findByIdAndTenantIdAndActiveTrue(payment.getMembershipId(), tenant)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + payment.getMembershipId()));
        LocalDate start = LocalDate.now();
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setStartDate(start);
        membership.setEndDate(start.plusMonths(Math.max(1, membership.getDurationMonths())));
        memberships.save(membership);
        audit(tenant, payment.getId(), "MEMBERSHIP_ACTIVATED", "membership " + membership.getId());
        log.info("Membership {} activated for payment {}.", membership.getId(), payment.getId());

        InvoiceDTO invoice = invoiceService.generateFromPayment(payment.getId(), tenant);
        completeInvoiceDelivery(tenant, payment, membership, invoice);
        return invoice;
    }

    /** Renders + stores the PDF and emails it. Safe to retry (idempotent). */
    @Transactional
    public InvoiceDTO completeInvoiceDelivery(String tenant, Payment payment, Membership membership, InvoiceDTO invoice) {
        Invoice entity = invoices.findByIdAndTenantId(invoice.id(), tenant)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoice.id()));
        Student student = students.findByIdAndTenantIdAndActiveTrue(payment.getStudentId(), tenant).orElse(null);
        Branch branch = branches.findByIdAndTenantIdAndActiveTrue(payment.getBranchId(), tenant).orElse(null);
        String reference = payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId()
            : payment.getRazorpayOrderId();
        if (entity.getPdfData() == null) {
            entity.setPdfData(pdfService.render(entity, student, branch, membership, reference));
            invoices.save(entity);
            audit(tenant, payment.getId(), "INVOICE_PDF", "invoice " + entity.getInvoiceNumber());
        }
        if (entity.getSentAt() == null && student != null && student.getEmail() != null && !student.getEmail().isBlank()) {
            boolean sent = emailService.sendInvoice(student.getEmail(), studentName(student),
                entity.getInvoiceNumber(), entity.getAmount().stripTrailingZeros().toPlainString(), entity.getPdfData());
            if (sent) {
                entity.setSentAt(LocalDateTime.now());
                invoices.save(entity);
                audit(tenant, payment.getId(), "INVOICE_EMAILED", student.getEmail());
            }
        }
        return invoice;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status() {
        return Map.of("enabled", gateway.isEnabled(),
            "keyId", gateway.isEnabled() ? gateway.getKeyId() : "",
            "currency", gateway.getCurrency(),
            "emailEnabled", emailService.isEnabled());
    }

    private Payment activePayment(String tenant, Long paymentId) {
        return payments.findByIdAndTenantIdAndActiveTrue(paymentId, tenant)
            .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
    }

    private PaymentMethod mapMethod(String razorpayMethod, PaymentMethod current) {
        if (razorpayMethod == null) return current;
        return switch (razorpayMethod.toLowerCase(Locale.ROOT)) {
            case "upi" -> PaymentMethod.UPI;
            case "card" -> PaymentMethod.CARD;
            case "netbanking" -> PaymentMethod.BANK_TRANSFER;
            default -> current;
        };
    }

    private String studentName(Student student) {
        String name = ((student.getFirstName() == null ? "" : student.getFirstName()) + " "
            + (student.getLastName() == null ? "" : student.getLastName())).trim();
        return name.isEmpty() ? ("Student #" + student.getId()) : name;
    }

    private void audit(String tenant, Long paymentId, String event, String detail) {
        try {
            PaymentEvent audit = new PaymentEvent();
            audit.setTenantId(tenant == null ? "default" : tenant);
            audit.setPaymentId(paymentId);
            audit.setEvent(event);
            audit.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
            events.save(audit);
        } catch (Exception ignored) {
            // Audit must never break payments.
        }
    }
}
