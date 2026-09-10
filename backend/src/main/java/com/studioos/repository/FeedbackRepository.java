package com.studioos.repository;

import com.studioos.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByTenantId(String tenantId, Pageable pageable);

    Page<Feedback> findByTenantIdAndBranchId(String tenantId, Long branchId, Pageable pageable);

    Page<Feedback> findByTenantIdAndStatus(String tenantId, Feedback.Status status, Pageable pageable);

    Page<Feedback> findByTenantIdAndBranchIdAndStatus(String tenantId, Long branchId, Feedback.Status status, Pageable pageable);

    Optional<Feedback> findByIdAndTenantId(Long id, String tenantId);

    @Query("""
        select f from Feedback f
        where f.tenantId = :tenantId
          and (:branchId is null or f.branchId = :branchId)
          and (:status is null or f.status = :status)
          and (:category is null or f.category = :category)
          and (
            :search is null or :search = ''
            or lower(f.title) like lower(concat('%', :search, '%'))
            or lower(f.description) like lower(concat('%', :search, '%'))
            or lower(f.createdBy) like lower(concat('%', :search, '%'))
          )
        """)
    Page<Feedback> search(
        @Param("tenantId") String tenantId,
        @Param("branchId") Long branchId,
        @Param("status") Feedback.Status status,
        @Param("category") Feedback.Category category,
        @Param("search") String search,
        Pageable pageable);
}
