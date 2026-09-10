package com.studioos.config;

import com.studioos.model.Batch;
import com.studioos.model.User;
import com.studioos.repository.BatchRepository;
import com.studioos.repository.StudentBatchRepository;
import com.studioos.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ScopeService {
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final StudentBatchRepository studentBatchRepository;
    public ScopeService(UserRepository userRepository, BatchRepository batchRepository, StudentBatchRepository studentBatchRepository) { this.userRepository = userRepository; this.batchRepository = batchRepository; this.studentBatchRepository = studentBatchRepository; }
    public User currentUser() { Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); if (authentication == null) return null; return userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null); }
    public boolean owner() { User user = currentUser(); return user != null && user.getRoles().stream().anyMatch(role -> Set.of("ADMIN", "OWNER").contains(role.getName().name())); }
    public boolean instructor() { User user = currentUser(); return user != null && user.getRoles().stream().anyMatch(role -> role.getName().name().equals("INSTRUCTOR")); }
    public boolean branchHead() { User user = currentUser(); return user != null && user.getRoles().stream().anyMatch(role -> role.getName().name().equals("BRANCH_HEAD")); }
    public boolean developer() { User user = currentUser(); return user != null && user.getRoles().stream().anyMatch(role -> role.getName().name().equals("DEVELOPER")); }
    public Long branchId() { User user = currentUser(); return user == null ? null : user.getBranchId(); }
    public Long instructorId() { User user = currentUser(); return user == null ? null : user.getInstructorId(); }

    /**
     * Student IDs visible to the current INSTRUCTOR (assigned batches).
     * Returns null for non-instructors (no restriction), empty set when nothing assigned.
     */
    public Set<Long> assignedStudentIds(String tenantId) {
        if (!instructor()) return null;
        Long instructorId = instructorId();
        if (instructorId == null) return Set.of();
        List<Batch> batches = batchRepository.findByTenantIdAndInstructorIdAndActiveTrue(tenantId, instructorId);
        Set<Long> studentIds = new HashSet<>();
        batches.forEach(batch -> studentBatchRepository.findByTenantIdAndBatchIdAndActiveTrue(tenantId, batch.getId())
            .forEach(assignment -> studentIds.add(assignment.getStudentId())));
        return studentIds;
    }

    /** Null for OWNER/ADMIN (unrestricted), otherwise the user's assigned branch. */
    public Long effectiveBranchId() {
        if (owner()) {
            return null;
        }
        User user = currentUser();
        return user == null ? null : user.getBranchId();
    }

    /** Throws 403 when a non-owner accesses a row outside the assigned branch. */
    public void enforceBranch(Long entityBranchId) {
        Long scoped = effectiveBranchId();
        if (scoped != null && entityBranchId != null && !scoped.equals(entityBranchId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this branch");
        }
    }

    /**
     * Resolves the branch to persist for write requests.
     * Non-owners are pinned to their assigned branch; mismatched requests are rejected.
     */
    public Long resolveBranchForWrite(Long requested) {
        Long scoped = effectiveBranchId();
        if (scoped == null) {
            if (requested == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is required");
            }
            return requested;
        }
        if (requested != null && !requested.equals(scoped)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this branch");
        }
        return scoped;
    }
}
