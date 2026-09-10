package com.studioos.config;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class BranchSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public BranchSchemaMigration(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check");
        jdbcTemplate.execute("ALTER TABLE roles ADD CONSTRAINT roles_name_check CHECK (name IN ('ADMIN', 'STAFF', 'OWNER', 'BRANCH_HEAD', 'INSTRUCTOR', 'DEVELOPER'))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS branches (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL, name VARCHAR(120) NOT NULL, address VARCHAR(500), phone VARCHAR(40), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_branches_tenant_name ON branches (tenant_id, name)");
        for (String name : List.of("Kasturi Nagar", "Whitefield", "NRI Layout", "Mahadevpura")) {
            jdbcTemplate.update("INSERT INTO branches (tenant_id, name, active, created_at, updated_at) VALUES ('default', ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON CONFLICT (tenant_id, name) DO NOTHING", name);
        }
        Long defaultBranchId = jdbcTemplate.queryForObject("SELECT id FROM branches WHERE tenant_id = 'default' ORDER BY id LIMIT 1", Long.class);
        for (String table : List.of("students", "instructors", "batches", "memberships", "payments")) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS branch_id BIGINT");
            jdbcTemplate.update("UPDATE " + table + " SET branch_id = ? WHERE branch_id IS NULL", defaultBranchId);
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN branch_id SET NOT NULL");
        }
    }
}
