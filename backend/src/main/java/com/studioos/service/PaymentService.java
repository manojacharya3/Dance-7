package com.studioos.service;

import com.studioos.dto.PaymentDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentDTO create(PaymentDTO dto);
    PaymentDTO update(Long id, PaymentDTO dto);
    PaymentDTO get(Long id, String tenantId);
    Page<PaymentDTO> list(String tenantId, String search, Long studentId, Long membershipId, Pageable pageable);
    void delete(Long id, String tenantId);
    BigDecimal totalRevenue(String tenantId);
    BigDecimal pendingPayments(String tenantId);
    BigDecimal monthlyCollections(String tenantId, LocalDate startDate, LocalDate endDate);
}
