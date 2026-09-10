package com.studioos.controller;

import com.studioos.dto.InvoiceDTO;
import com.studioos.service.InvoiceService;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService service;
    public InvoiceController(InvoiceService service) { this.service = service; }
    @PostMapping("/from-payment/{paymentId}") public ResponseEntity<InvoiceDTO> generate(@PathVariable Long paymentId, @RequestParam(defaultValue = "default") String tenantId) { InvoiceDTO invoice = service.generateFromPayment(paymentId, tenantId); return ResponseEntity.created(URI.create("/api/invoices/" + invoice.id())).body(invoice); }
    @GetMapping("/{id}") public InvoiceDTO get(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { return service.get(id, tenantId); }
    @GetMapping public Page<InvoiceDTO> list(@RequestParam(defaultValue = "default") String tenantId, @RequestParam(required = false) Long branchId, @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return service.list(tenantId, branchId, search, pageable(page, size)); }
    private Pageable pageable(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values"); return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceDate", "createdAt")); }
}
