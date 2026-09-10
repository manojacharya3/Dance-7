package com.studioos.controller;

import com.studioos.dto.BranchDTO;
import com.studioos.service.BranchService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/branches") @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class BranchController {
    private final BranchService service;
    public BranchController(BranchService service) { this.service = service; }
    @PostMapping public ResponseEntity<BranchDTO> create(@Valid @RequestBody BranchDTO dto) { BranchDTO created = service.create(dto); return ResponseEntity.created(URI.create("/api/branches/" + created.id())).body(created); }
    @PutMapping("/{id}") public BranchDTO update(@PathVariable Long id, @Valid @RequestBody BranchDTO dto) { return service.update(id, dto); }
    @GetMapping public List<BranchDTO> list(@RequestParam(defaultValue = "default") String tenantId) { return service.list(tenantId); }
    @GetMapping("/{id}") public BranchDTO get(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { return service.get(id, tenantId); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { service.delete(id, tenantId); return ResponseEntity.noContent().build(); }
}
