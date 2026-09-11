package com.studioos.controller;

import com.studioos.dto.InvoiceDTO;
import com.studioos.model.Invoice;
import com.studioos.model.Membership;
import com.studioos.model.Payment;
import com.studioos.repository.InvoiceRepository;
import com.studioos.repository.MembershipRepository;
import com.studioos.repository.PaymentRepository;
import com.studioos.service.RazorpayPaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Invoice PDF download + receipt resend. Read/download never regenerate numbers. */
@RestController @RequestMapping("/api/invoices")
public class InvoiceDeliveryController {
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final MembershipRepository memberships;
    private final RazorpayPaymentService razorpay;

    public InvoiceDeliveryController(InvoiceRepository invoices, PaymentRepository payments,
        MembershipRepository memberships, RazorpayPaymentService razorpay) {
        this.invoices = invoices; this.payments = payments;
        this.memberships = memberships; this.razorpay = razorpay;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id,
        @RequestParam(defaultValue = "default") String tenantId) {
        Invoice invoice = invoices.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        if (invoice.getPdfData() == null) {
            Payment payment = payments.findByIdAndTenantIdAndActiveTrue(invoice.getPaymentId(), tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found."));
            Membership membership = memberships.findByIdAndTenantIdAndActiveTrue(payment.getMembershipId(), tenantId)
                .orElse(null);
            InvoiceDTO dto = new InvoiceDTO(invoice.getId(), invoice.getTenantId(), invoice.getInvoiceNumber(),
                invoice.getPaymentId(), invoice.getStudentId(), invoice.getBranchId(), invoice.getAmount(),
                invoice.getPaymentMethod(), invoice.getTransactionReference(), invoice.getInvoiceDate(),
                invoice.getStatus(), invoice.getCreatedAt(), invoice.getUpdatedAt());
            razorpay.completeInvoiceDelivery(tenantId, payment, membership, dto);
            invoice = invoices.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(invoice.getInvoiceNumber() + ".pdf").build().toString())
            .contentType(MediaType.APPLICATION_PDF)
            .body(invoice.getPdfData());
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<InvoiceDTO> resend(@PathVariable Long id,
        @RequestParam(defaultValue = "default") String tenantId) {
        Invoice invoice = invoices.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        Payment payment = payments.findByIdAndTenantIdAndActiveTrue(invoice.getPaymentId(), tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Payment not found."));
        Membership membership = memberships.findByIdAndTenantIdAndActiveTrue(payment.getMembershipId(), tenantId)
            .orElse(null);
        invoice.setSentAt(null);
        invoices.save(invoice);
        InvoiceDTO dto = new InvoiceDTO(invoice.getId(), invoice.getTenantId(), invoice.getInvoiceNumber(),
            invoice.getPaymentId(), invoice.getStudentId(), invoice.getBranchId(), invoice.getAmount(),
            invoice.getPaymentMethod(), invoice.getTransactionReference(), invoice.getInvoiceDate(),
            invoice.getStatus(), invoice.getCreatedAt(), invoice.getUpdatedAt());
        razorpay.completeInvoiceDelivery(tenantId, payment, membership, dto);
        return ResponseEntity.ok(dto);
    }
}
