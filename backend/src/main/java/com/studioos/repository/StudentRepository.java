package com.studioos.repository;

import com.studioos.model.Student;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Page<Student> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);

    Page<Student> findByTenantIdAndBranchIdAndActiveTrue(String tenantId, Long branchId, Pageable pageable);

    Optional<Student> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);

    @Query("""
        select s from Student s
        where s.tenantId = :tenantId
          and s.active = true
          and (
            lower(s.firstName) like lower(concat('%', :search, '%'))
            or lower(s.lastName) like lower(concat('%', :search, '%'))
            or lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :search, '%'))
            or lower(coalesce(s.email, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(s.phone, '')) like lower(concat('%', :search, '%'))
          )
        """)
    Page<Student> searchActiveStudents(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);

    @Query("""
        select s from Student s
        where s.tenantId = :tenantId
          and s.active = true
          and s.branchId = :branchId
          and (
            lower(s.firstName) like lower(concat('%', :search, '%'))
            or lower(s.lastName) like lower(concat('%', :search, '%'))
            or lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :search, '%'))
            or lower(coalesce(s.email, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(s.phone, '')) like lower(concat('%', :search, '%'))
          )
        """)
    Page<Student> searchActiveStudentsByBranch(@Param("tenantId") String tenantId, @Param("branchId") Long branchId, @Param("search") String search, Pageable pageable);
}
