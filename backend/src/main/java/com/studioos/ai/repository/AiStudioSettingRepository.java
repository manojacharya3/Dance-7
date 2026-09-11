package com.studioos.ai.repository;

import com.studioos.ai.model.AiStudioSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiStudioSettingRepository extends JpaRepository<AiStudioSetting, Long> {
    List<AiStudioSetting> findByTenantIdAndBranchIdOrderBySettingKeyAsc(String tenantId, Long branchId);
    Optional<AiStudioSetting> findByTenantIdAndBranchIdAndSettingKey(String tenantId, Long branchId, String settingKey);
    Optional<AiStudioSetting> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
}
