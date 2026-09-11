package com.studioos.ai.repository;

import com.studioos.ai.model.AiClassSchedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiClassScheduleRepository extends JpaRepository<AiClassSchedule, Long> {
    List<AiClassSchedule> findByTenantIdAndBranchIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(String tenantId, Long branchId);
    List<AiClassSchedule> findByTenantIdAndBranchIdOrderByDayOfWeekAscStartTimeAsc(String tenantId, Long branchId);
    List<AiClassSchedule> findByTenantIdAndBranchIdAndAiClassIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(String tenantId, Long branchId, Long aiClassId);
    Optional<AiClassSchedule> findByIdAndTenantIdAndBranchId(Long id, String tenantId, Long branchId);
}
