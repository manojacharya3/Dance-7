package com.studioos.service;

import com.studioos.dto.InvoiceDTO;
import com.studioos.model.Invoice;
import com.studioos.model.Payment;
import com.studioos.model.PaymentStatus;
import com.studioos.repository.InvoiceRepository;
import com.studioos.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository repository;
    private final PaymentRepository paymentRepository;
    public InvoiceServiceImpl(InvoiceRepository repository, PaymentRepository paymentRepository) { this.repository = repository; this.paymentRepository = paymentRepository; }
    public InvoiceDTO generateFromPayment(Long paymentId, String tenantId) { String tenant = tenant(tenantId); Payment payment = paymentRepository.findByIdAndTenantIdAndActiveTrue(paymentId, tenant).orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId)); if (payment.getPaymentStatus() != PaymentStatus.PAID) throw new IllegalArgumentException("Invoice can only be generated from a paid payment"); return repository.findByPaymentIdAndTenantId(paymentId, tenant).map(this::toDto).orElseGet(() -> toDto(repository.save(createInvoice(payment, tenant)))); }
    @Transactional(readOnly = true) public InvoiceDTO get(Long id, String tenantId) { return toDto(repository.findByIdAndTenantId(id, tenant(tenantId)).orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id))); }
    @Transactional(readOnly = true) public Page<InvoiceDTO> list(String tenantId, Long branchId, String search, Pageable pageable) { String tenant = tenant(tenantId); Page<Invoice> invoices = search != null && !search.isBlank() ? branchId == null ? repository.search(tenant, search.trim(), pageable) : repository.searchByBranch(tenant, branchId, search.trim(), pageable) : branchId == null ? repository.findByTenantId(tenant, pageable) : repository.findByTenantIdAndBranchId(tenant, branchId, pageable); return invoices.map(this::toDto); }
    private Invoice createInvoice(Payment payment, String tenant) { Invoice invoice = new Invoice(); invoice.setTenantId(tenant); invoice.setInvoiceNumber("INV-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()); invoice.setPaymentId(payment.getId()); invoice.setStudentId(payment.getStudentId()); invoice.setBranchId(payment.getBranchId()); invoice.setAmount(payment.getAmount()); invoice.setPaymentMethod(payment.getPaymentMethod()); invoice.setInvoiceDate(payment.getPaymentDate()); invoice.setTransactionReference(payment.getRemarks()); invoice.setStatus("PAID"); return invoice; }
    private InvoiceDTO toDto(Invoice invoice) { return new InvoiceDTO(invoice.getId(), invoice.getTenantId(), invoice.getInvoiceNumber(), invoice.getPaymentId(), invoice.getStudentId(), invoice.getBranchId(), invoice.getAmount(), invoice.getPaymentMethod(), invoice.getTransactionReference(), invoice.getInvoiceDate(), invoice.getStatus(), invoice.getCreatedAt(), invoice.getUpdatedAt()); }
    private String tenant(String value) { return value == null || value.isBlank() ? "default" : value.trim(); }
}
