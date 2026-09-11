package com.studioos.controller;

import com.studioos.dto.InvoiceDTO;
import com.studioos.model.Invoice;
import com.studioos.repository.InvoiceRepository;
import com.studioos.service.RazorpayPaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Online-payment endpoints. Checkout success is never trusted: capture happens
 * only after HMAC signature verification (or a verified webhook for the same
 * order, which is duplicate-safe).
 */
@RestController @RequestMapping("/api/payments")
public class RazorpayController {
    private final RazorpayPaymentService service;
    private final InvoiceRepository invoices;

    public RazorpayController(RazorpayPaymentService service, InvoiceRepository invoices) {
        this.service = service;
        this.invoices = invoices;
    }

    public record CreateOrderRequest(@NotNull Long paymentId) {}
    public record VerifyRequest(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature) {}

    @GetMapping("/razorpay/status")
    public Map<String, Object> status(@RequestParam(defaultValue = "default") String tenantId) {
        return service.status();
    }

    @PostMapping("/create-order")
    public RazorpayPaymentService.OrderResult createOrder(@RequestBody CreateOrderRequest req,
        @RequestParam(defaultValue = "default") String tenantId) {
        return service.createOrder(tenantId, req.paymentId());
    }

    @PostMapping("/verify")
    public InvoiceDTO verify(@RequestBody VerifyRequest req,
        @RequestParam(defaultValue = "default") String tenantId) {
        return service.verifyAndCapture(tenantId, req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody(required = false) String rawBody,
        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        service.handleWebhook(rawBody == null ? "" : rawBody, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/{paymentId}/invoice")
    public InvoiceDTO invoiceForPayment(@PathVariable Long paymentId,
        @RequestParam(defaultValue = "default") String tenantId) {
        Invoice invoice = invoices.findByPaymentIdAndTenantId(paymentId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("No invoice for payment: " + paymentId));
        return new InvoiceDTO(invoice.getId(), invoice.getTenantId(), invoice.getInvoiceNumber(),
            invoice.getPaymentId(), invoice.getStudentId(), invoice.getBranchId(), invoice.getAmount(),
            invoice.getPaymentMethod(), invoice.getTransactionReference(), invoice.getInvoiceDate(),
            invoice.getStatus(), invoice.getCreatedAt(), invoice.getUpdatedAt());
    }
}
