package com.studioos.repository;

import com.studioos.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    org.springframework.data.domain.Page<User> findByTenantId(String tenantId, org.springframework.data.domain.Pageable pageable);
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'OWNER'")
    long countOwners();
}
