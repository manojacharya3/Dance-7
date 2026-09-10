package com.studioos.controller;

import com.studioos.dto.BatchDTO;
import com.studioos.service.BatchService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/batches") @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class BatchController {
    private final BatchService service; public BatchController(BatchService service) { this.service = service; }
    @PostMapping public ResponseEntity<BatchDTO> create(@Valid @RequestBody BatchDTO dto) { BatchDTO created = service.create(dto); return ResponseEntity.created(URI.create("/api/batches/" + created.id())).body(created); }
    @PutMapping("/{id}") public BatchDTO update(@PathVariable Long id, @Valid @RequestBody BatchDTO dto) { return service.update(id, dto); }
    @GetMapping("/{id}") public BatchDTO get(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { return service.get(id, tenantId); }
    @GetMapping public Page<BatchDTO> list(@RequestParam(defaultValue = "default") String tenantId, @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return service.list(tenantId, search, pageable(page, size)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { service.delete(id, tenantId); return ResponseEntity.noContent().build(); }
    @GetMapping("/count") public long count(@RequestParam(defaultValue = "default") String tenantId) { return service.count(tenantId); }
    private Pageable pageable(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values"); return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "batchName")); }
}