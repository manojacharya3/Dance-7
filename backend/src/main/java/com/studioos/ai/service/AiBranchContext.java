package com.studioos.ai.service;

import com.studioos.model.Branch;
import com.studioos.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the active branch for every chat/admin request. All downstream
 * retrieval is keyed by the resolved branch id — this is the single
 * server-side branch-isolation choke point.
 */
@Service
public class AiBranchContext {
    private final BranchRepository branches;

    public AiBranchContext(BranchRepository branches) { this.branches = branches; }

    @Transactional(readOnly = true)
    public Branch resolve(String tenantId, String branchRef) {
        String tenant = tenantOf(tenantId);
        if (branchRef == null || branchRef.isBlank()) throw new EntityNotFoundException("Branch is required.");
        String ref = branchRef.trim();
        try {
            Long id = Long.parseLong(ref);
            return branches.findByIdAndTenantIdAndActiveTrue(id, tenant)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found."));
        } catch (NumberFormatException notAnId) {
            String slug = slugify(ref);
            List<Branch> all = branches.findByTenantIdAndActiveTrueOrderByNameAsc(tenant);
            return all.stream().filter(b -> slugify(b.getName()).equals(slug)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Branch not found."));
        }
    }

    public static String slugify(String name) {
        return name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public static String tenantOf(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim();
    }
}
