package com.studioos.ai.repository;

import com.studioos.ai.model.AiChatOffer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatOfferRepository extends JpaRepository<AiChatOffer, Long> {
    List<AiChatOffer> findByTenantIdAndBranchIdAndActiveTrueOrderByValidUntilAsc(String tenantId, Long branchId);
    List<AiChatOffer> findByTenantIdAndBranchIdOrderByValidUntilAsc(String tenantId, Long branchId);
    Optional<AiChatOffer> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
}
