package com.studioos.service;

import com.studioos.dto.InstructorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InstructorService {
    InstructorDTO create(InstructorDTO dto);
    InstructorDTO update(Long id, InstructorDTO dto);
    InstructorDTO get(Long id, String tenantId);
    Page<InstructorDTO> list(String tenantId, String search, Pageable pageable);
    void delete(Long id, String tenantId);
    long count(String tenantId);
}