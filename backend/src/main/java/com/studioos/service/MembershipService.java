package com.studioos.service;

import com.studioos.dto.MembershipDTO;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MembershipService {
    MembershipDTO create(MembershipDTO dto);
    MembershipDTO update(Long id, MembershipDTO dto);
    MembershipDTO get(Long id, String tenantId);
    Page<MembershipDTO> list(String tenantId, String search, Pageable pageable);
    void delete(Long id, String tenantId);
    long countActive(String tenantId);
    long countExpiring(String tenantId, LocalDate today, LocalDate deadline);
    long countExpired(String tenantId);
}
