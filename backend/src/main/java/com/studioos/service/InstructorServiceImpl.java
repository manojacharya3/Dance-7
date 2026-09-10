package com.studioos.service;

import com.studioos.dto.InstructorDTO;
import com.studioos.model.Instructor;
import com.studioos.repository.InstructorRepository;
import com.studioos.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class InstructorServiceImpl implements InstructorService {
    private final InstructorRepository repository;
    private final BranchRepository branchRepository;
    public InstructorServiceImpl(InstructorRepository repository, BranchRepository branchRepository) { this.repository = repository; this.branchRepository = branchRepository; }
    public InstructorDTO create(InstructorDTO dto) { validateBranch(dto.branchId(), tenant(dto.tenantId())); return toDto(repository.save(toEntity(dto))); }
    public InstructorDTO update(Long id, InstructorDTO dto) { String t = tenant(dto.tenantId()); validateBranch(dto.branchId(), t); Instructor entity = find(id, t); updateEntity(entity, dto); return toDto(repository.save(entity)); }
    @Transactional(readOnly = true) public InstructorDTO get(Long id, String tenantId) { return toDto(find(id, tenant(tenantId))); }
    @Transactional(readOnly = true) public Page<InstructorDTO> list(String tenantId, String search, Pageable pageable) { String t = tenant(tenantId); return (search == null || search.isBlank() ? repository.findByTenantIdAndActiveTrue(t, pageable) : repository.search(t, search.trim(), pageable)).map(this::toDto); }
    public void delete(Long id, String tenantId) { Instructor instructor = find(id, tenant(tenantId)); instructor.setActive(false); repository.save(instructor); }
    @Transactional(readOnly = true) public long count(String tenantId) { return repository.countByTenantIdAndActiveTrue(tenant(tenantId)); }
    private Instructor find(Long id, String tenantId) { return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).orElseThrow(() -> new EntityNotFoundException("Instructor not found: " + id)); }
    private Instructor toEntity(InstructorDTO dto) { Instructor entity = new Instructor(); updateEntity(entity, dto); return entity; }
    private void updateEntity(Instructor e, InstructorDTO d) { e.setTenantId(tenant(d.tenantId())); e.setBranchId(d.branchId()); e.setFirstName(d.firstName().trim()); e.setLastName(d.lastName().trim()); e.setEmail(d.email().trim().toLowerCase()); e.setPhone(d.phone()); e.setSpecialization(d.specialization()); if (d.active() != null) e.setActive(d.active()); }
    private InstructorDTO toDto(Instructor e) { return new InstructorDTO(e.getId(), e.getTenantId(), e.getBranchId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(), e.getSpecialization(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt()); }
    private void validateBranch(Long id, String tenantId) { if (branchRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId).isEmpty()) throw new EntityNotFoundException("Branch not found: " + id); }
    private String tenant(String value) { return value == null || value.isBlank() ? "default" : value.trim(); }
}