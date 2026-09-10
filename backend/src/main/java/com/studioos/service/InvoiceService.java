package com.studioos.service;

import com.studioos.dto.InvoiceDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
    InvoiceDTO generateFromPayment(Long paymentId, String tenantId);
    InvoiceDTO get(Long id, String tenantId);
    Page<InvoiceDTO> list(String tenantId, Long branchId, String search, Pageable pageable);
}
