package com.studioos.service;

import com.studioos.dto.BatchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BatchService {
    BatchDTO create(BatchDTO dto);
    BatchDTO update(Long id, BatchDTO dto);
    BatchDTO get(Long id, String tenantId);
    Page<BatchDTO> list(String tenantId, String search, Pageable pageable);
    void delete(Long id, String tenantId);
    long count(String tenantId);
}