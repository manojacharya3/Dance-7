package com.studioos.service;

import com.studioos.dto.BranchDTO;
import com.studioos.model.Branch;
import com.studioos.repository.BranchRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class BranchServiceImpl implements BranchService {
    private final BranchRepository repository;
    public BranchServiceImpl(BranchRepository repository) { this.repository = repository; }
    @PostConstruct
    public void seedDefaultBranches() { for (String name : List.of("Kasturi Nagar", "Whitefield", "NRI Layout", "Mahadevpura")) { if (repository.findByTenantIdAndNameIgnoreCase("default", name).isEmpty()) { Branch branch = new Branch(); branch.setTenantId("default"); branch.setName(name); branch.setActive(true); repository.save(branch); } } }
    public BranchDTO create(BranchDTO dto) { return toDto(repository.save(toEntity(dto))); }
    public BranchDTO update(Long id, BranchDTO dto) { Branch branch = find(id, tenant(dto.tenantId())); updateEntity(branch, dto); return toDto(repository.save(branch)); }
    @Transactional(readOnly = true) public List<BranchDTO> list(String tenantId) { return repository.findByTenantIdAndActiveTrueOrderByNameAsc(tenant(tenantId)).stream().map(this::toDto).toList(); }
    @Transactional(readOnly = true) public BranchDTO get(Long id, String tenantId) { return toDto(find(id, tenant(tenantId))); }
    public void delete(Long id, String tenantId) { Branch branch = find(id, tenant(tenantId)); branch.setActive(false); repository.save(branch); }
    private Branch find(Long id, String tenantId) { return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId).orElseThrow(() -> new EntityNotFoundException("Branch not found: " + id)); }
    private Branch toEntity(BranchDTO dto) { Branch branch = new Branch(); updateEntity(branch, dto); return branch; }
    private void updateEntity(Branch branch, BranchDTO dto) { branch.setTenantId(tenant(dto.tenantId())); branch.setName(dto.name().trim()); branch.setAddress(dto.address()); branch.setPhone(dto.phone()); if (dto.active() != null) branch.setActive(dto.active()); }
    private BranchDTO toDto(Branch branch) { return new BranchDTO(branch.getId(), branch.getTenantId(), branch.getName(), branch.getAddress(), branch.getPhone(), branch.isActive(), branch.getCreatedAt(), branch.getUpdatedAt()); }
    private String tenant(String value) { return value == null || value.isBlank() ? "default" : value.trim(); }
}
