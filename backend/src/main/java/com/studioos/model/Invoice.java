package com.studioos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoices_tenant_date", columnList = "tenant_id,invoice_date"),
    @Index(name = "idx_invoices_tenant_branch", columnList = "tenant_id,branch_id"),
    @Index(name = "idx_invoices_tenant_payment", columnList = "tenant_id,payment_id")
})
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, length = 100) private String tenantId;
    @Column(name = "invoice_number", nullable = false, unique = true, length = 40) private String invoiceNumber;
    @Column(name = "payment_id", nullable = false, unique = true) private Long paymentId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", nullable = false, length = 30) private PaymentMethod paymentMethod;
    @Column(name = "transaction_reference", length = 120) private String transactionReference;
    @Column(name = "invoice_date", nullable = false) private LocalDate invoiceDate;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getInvoiceNumber() { return invoiceNumber; } public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Long getPaymentId() { return paymentId; } public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getStudentId() { return studentId; } public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getBranchId() { return branchId; } public void setBranchId(Long branchId) { this.branchId = branchId; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionReference() { return transactionReference; } public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public LocalDate getInvoiceDate() { return invoiceDate; } public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getUpdatedAt() { return updatedAt; }
}
