package com.studioos.controller;

import com.studioos.dto.MembershipDTO;
import com.studioos.service.MembershipService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@RequestMapping("/api/memberships")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class MembershipController {
    private final MembershipService service;

    public MembershipController(MembershipService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<MembershipDTO> create(@Valid @RequestBody MembershipDTO dto) {
        MembershipDTO created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/memberships/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MembershipDTO update(@PathVariable Long id, @Valid @RequestBody MembershipDTO dto) { return service.update(id, dto); }

    @GetMapping("/{id}")
    public MembershipDTO get(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { return service.get(id, tenantId); }

    @GetMapping
    public Page<MembershipDTO> list(@RequestParam(defaultValue = "default") String tenantId, @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return service.list(tenantId, search, pageable(page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { service.delete(id, tenantId); return ResponseEntity.noContent().build(); }

    @GetMapping("/count/active")
    public long countActive(@RequestParam(defaultValue = "default") String tenantId) { return service.countActive(tenantId); }

    @GetMapping("/count/expiring")
    public long countExpiring(@RequestParam(defaultValue = "default") String tenantId) { LocalDate today = LocalDate.now(); return service.countExpiring(tenantId, today, today.plusDays(30)); }

    @GetMapping("/count/expired")
    public long countExpired(@RequestParam(defaultValue = "default") String tenantId) { return service.countExpired(tenantId); }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "endDate", "planName"));
    }
}
