package com.studioos.repository;

import com.studioos.model.PaymentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByTenantIdAndPaymentIdOrderByCreatedAtDesc(String tenantId, Long paymentId);
}
