package com.studioos.service;

import com.studioos.dto.BatchDTO;
import com.studioos.model.Batch;
import com.studioos.repository.BatchRepository;
import com.studioos.repository.InstructorRepository;
import com.studioos.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class BatchServiceImpl implements BatchService {
    private final BatchRepository repository;
    private final InstructorRepository instructorRepository;
    private final BranchRepository branchRepository;
    public BatchServiceImpl(BatchRepository repository, InstructorRepository instructorRepository, BranchRepository branchRepository) { this.repository = repository; this.instructorRepository = instructorRepository; this.branchRepository = branchRepository; }
    public BatchDTO create(BatchDTO dto) { String t = tenant(dto.tenantId()); Long branchId = resolveBranch(dto.branchId(), t); validateBranch(branchId, t); validateInstructor(dto.instructorId(), t); return toDto(repository.save(toEntity(dto, branchId))); }
    public BatchDTO update(Long id, BatchDTO dto) { String t = tenant(dto.tenantId()); Long branchId = resolveBranch(dto.branchId(), t); validateBranch(branchId, t); validateInstructor(dto.instructorId(), t); Batch entity = find(id, t); updateEntity(entity, dto, branchId); return toDto(repository.save(entity)); }
    @Transactional(readOnly = true) public BatchDTO get(Long id, String tenantId) { return toDto(find(id, tenant(tenantId))); }
    @Transactional(readOnly = true) public Page<BatchDTO> list(String tenantId, String search, Pageable pageable) { String t = tenant(tenantId); return (search == null || search.isBlank() ? repository.findByTenantIdAndActiveTrue(t, pageable) : repository.search(t, search.trim(), pageable)).map(this::toDto); }
    public void delete(Long id, String tenantId) { Batch entity = find(id, tenant(tenantId)); entity.setActive(false); repository.save(entity); }
    @Transactional(readOnly = true) public long count(String tenantId) { return repository.countByTenantIdAndActiveTrue(tenant(tenantId)); }
    private void validateInstructor(Long id, String tenantId) { if (instructorRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId).isEmpty()) throw new EntityNotFoundException("Instructor not found: " + id); }
    private Batch find(Long id, String tenantId) { return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).orElseThrow(() -> new EntityNotFoundException("Batch not found: " + id)); }
    private Batch toEntity(BatchDTO dto, Long branchId) { Batch entity = new Batch(); updateEntity(entity, dto, branchId); return entity; }
    private void updateEntity(Batch e, BatchDTO d, Long branchId) { e.setTenantId(tenant(d.tenantId())); e.setBranchId(branchId); e.setBatchName(d.batchName().trim()); e.setInstructorId(d.instructorId()); e.setStartTime(d.startTime()); e.setEndTime(d.endTime()); e.setCapacity(d.capacity()); if (d.active() != null) e.setActive(d.active()); }
    private BatchDTO toDto(Batch e) { return new BatchDTO(e.getId(), e.getTenantId(), e.getBranchId(), e.getBatchName(), e.getInstructorId(), e.getStartTime(), e.getEndTime(), e.getCapacity(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt()); }
    private void validateBranch(Long id, String tenantId) { if (branchRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId).isEmpty()) throw new EntityNotFoundException("Branch not found: " + id); }
    private Long resolveBranch(Long id, String tenantId) { return id != null ? id : branchRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId).stream().findFirst().map(branch -> branch.getId()).orElseThrow(() -> new EntityNotFoundException("No active branch found")); }
    private String tenant(String value) { return value == null || value.isBlank() ? "default" : value.trim(); }
}