package com.studioos.controller;

import com.studioos.dto.StudentBatchDTO;
import com.studioos.service.StudentBatchService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/student-batches") @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class StudentBatchController {
    private final StudentBatchService service; public StudentBatchController(StudentBatchService service) { this.service = service; }
    @PostMapping public StudentBatchDTO assign(@Valid @RequestBody StudentBatchDTO dto) { return service.assign(dto); }
    @DeleteMapping("/{studentId}/{batchId}") public ResponseEntity<Void> remove(@PathVariable Long studentId, @PathVariable Long batchId, @RequestParam(defaultValue = "default") String tenantId) { service.remove(studentId, batchId, tenantId); return ResponseEntity.noContent().build(); }
    @GetMapping("/batch/{batchId}") public List<StudentBatchDTO> byBatch(@PathVariable Long batchId, @RequestParam(defaultValue = "default") String tenantId) { return service.byBatch(batchId, tenantId); }
    @GetMapping("/student/{studentId}") public List<StudentBatchDTO> byStudent(@PathVariable Long studentId, @RequestParam(defaultValue = "default") String tenantId) { return service.byStudent(studentId, tenantId); }
}