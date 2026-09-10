package com.studioos.controller;

import com.studioos.dto.InstructorDTO;
import com.studioos.service.InstructorService;
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

@RestController @RequestMapping("/api/instructors") @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class InstructorController {
    private final InstructorService service; public InstructorController(InstructorService service) { this.service = service; }
    @PostMapping public ResponseEntity<InstructorDTO> create(@Valid @RequestBody InstructorDTO dto) { InstructorDTO created = service.create(dto); return ResponseEntity.created(URI.create("/api/instructors/" + created.id())).body(created); }
    @PutMapping("/{id}") public InstructorDTO update(@PathVariable Long id, @Valid @RequestBody InstructorDTO dto) { return service.update(id, dto); }
    @GetMapping("/{id}") public InstructorDTO get(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { return service.get(id, tenantId); }
    @GetMapping public Page<InstructorDTO> list(@RequestParam(defaultValue = "default") String tenantId, @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return service.list(tenantId, search, pageable(page, size)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "default") String tenantId) { service.delete(id, tenantId); return ResponseEntity.noContent().build(); }
    @GetMapping("/count") public long count(@RequestParam(defaultValue = "default") String tenantId) { return service.count(tenantId); }
    private Pageable pageable(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values"); return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")); }
}