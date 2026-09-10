package com.studioos.service;

import com.studioos.dto.BranchDTO;
import java.util.List;

public interface BranchService {
    BranchDTO create(BranchDTO dto);
    BranchDTO update(Long id, BranchDTO dto);
    List<BranchDTO> list(String tenantId);
    BranchDTO get(Long id, String tenantId);
    void delete(Long id, String tenantId);
}
