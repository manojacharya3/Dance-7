package com.studioos.repository;

import com.studioos.model.Membership;
import com.studioos.model.MembershipStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);
    Page<Membership> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);
    Page<Membership> findByTenantIdAndStudentIdInAndActiveTrue(String tenantId, Collection<Long> studentIds, Pageable pageable);
    Page<Membership> findByTenantIdAndBranchIdAndActiveTrue(String tenantId, Long branchId, Pageable pageable);
        @Query("""
                select m from Membership m, Student s
                where m.tenantId = :tenantId
                    and m.active = true
                    and s.id = m.studentId
                    and s.tenantId = :tenantId
                    and s.active = true
                    and (
                        str(m.studentId) like concat('%', :search, '%')
                        or lower(s.firstName) like lower(concat('%', :search, '%'))
                        or lower(s.lastName) like lower(concat('%', :search, '%'))
                        or lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :search, '%'))
                        or lower(coalesce(s.email, '')) like lower(concat('%', :search, '%'))
                        or lower(m.planName) like lower(concat('%', :search, '%'))
                    )
                """)
    Page<Membership> search(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);
    @Query("""
            select m from Membership m, Student s
            where m.tenantId = :tenantId
                and m.active = true
                and m.branchId = :branchId
                and s.id = m.studentId
                and s.tenantId = :tenantId
                and s.active = true
                and (
                    str(m.studentId) like concat('%', :search, '%')
                    or lower(s.firstName) like lower(concat('%', :search, '%'))
                    or lower(s.lastName) like lower(concat('%', :search, '%'))
                    or lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :search, '%'))
                    or lower(coalesce(s.email, '')) like lower(concat('%', :search, '%'))
                    or lower(m.planName) like lower(concat('%', :search, '%'))
                )
            """)
    Page<Membership> searchByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("search") String search, Pageable pageable);
    @Query("select count(m) from Membership m where m.tenantId = :tenantId and m.active = true and m.status <> com.studioos.model.MembershipStatus.CANCELLED and m.startDate <= :today and m.endDate >= :today")
    long countActive(@Param("tenantId") String tenantId, @Param("today") LocalDate today);
    @Query("select count(m) from Membership m where m.tenantId = :tenantId and m.active = true and m.status <> com.studioos.model.MembershipStatus.CANCELLED and m.endDate between :startDate and :endDate")
    long countExpiring(@Param("tenantId") String tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    @Query("select count(m) from Membership m where m.tenantId = :tenantId and m.active = true and m.status <> com.studioos.model.MembershipStatus.CANCELLED and m.endDate < :today")
    long countExpired(@Param("tenantId") String tenantId, @Param("today") LocalDate today);
    @Query("select count(m) from Membership m where m.tenantId = :tenantId and m.active = true and m.branchId = :branchId and m.status <> com.studioos.model.MembershipStatus.CANCELLED and m.startDate <= :today and m.endDate >= :today")
    long countActiveByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("today") LocalDate today);
    @Query("select count(m) from Membership m where m.tenantId = :tenantId and m.active = true and m.branchId = :branchId and m.status <> com.studioos.model.MembershipStatus.CANCELLED and m.endDate between :startDate and :endDate")
    long countExpiringByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    @Query("select count(m) from Membership m where m.tenantId = :tenantId and m.active = true and m.branchId = :branchId and m.status <> com.studioos.model.MembershipStatus.CANCELLED and m.endDate < :today")
    long countExpiredByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("today") LocalDate today);
}
