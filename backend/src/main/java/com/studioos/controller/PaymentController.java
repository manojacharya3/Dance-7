package com.studioos.controller;

import com.studioos.dto.PaymentDTO;
import com.studioos.service.PaymentService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PaymentDTO> create(@Valid @RequestBody PaymentDTO dto) { PaymentDTO created = service.create(dto); return ResponseEntity.created(URI.create("/api/payments/" + created.id())).body(created); }
    @PutMapping("/{id}")
    public PaymentDTO update(@PathVariable Long id, @Valid @RequestBody PaymentDTO dto) { return service.update(id, dto); }
    @GetMapping("/{id}")
    public PaymentDTO get(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { return service.get(id, tenantId); }
    @GetMapping
    public Page<PaymentDTO> list(@RequestParam(defaultValue = "default") String tenantId, @RequestParam(required = false) String search, @RequestParam(required = false) Long studentId, @RequestParam(required = false) Long membershipId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return service.list(tenantId, search, studentId, membershipId, pageable(page, size)); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { service.delete(id, tenantId); return ResponseEntity.noContent().build(); }
    @GetMapping("/summary/total-revenue")
    public BigDecimal totalRevenue(@RequestParam(defaultValue = "default") String tenantId) { return service.totalRevenue(tenantId); }
    @GetMapping("/summary/pending")
    public BigDecimal pending(@RequestParam(defaultValue = "default") String tenantId) { return service.pendingPayments(tenantId); }
    @GetMapping("/summary/monthly-collections")
    public BigDecimal monthlyCollections(@RequestParam(defaultValue = "default") String tenantId) { YearMonth month = YearMonth.now(); return service.monthlyCollections(tenantId, month.atDay(1), month.atEndOfMonth()); }

    private Pageable pageable(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values"); return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentDate", "updatedAt")); }
}
