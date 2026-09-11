package com.studioos.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** feedback_attachments table. Additive only; failures are logged, never fatal. */
@Component
@Order(4)
public class FeedbackAttachmentMigration implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(FeedbackAttachmentMigration.class);
    private final JdbcTemplate jdbc;

    public FeedbackAttachmentMigration(DataSource dataSource) { this.jdbc = new JdbcTemplate(dataSource); }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS feedback_attachments (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL, feedback_id BIGINT NOT NULL, file_name VARCHAR(255) NOT NULL, content_type VARCHAR(100) NOT NULL, file_size BIGINT NOT NULL, data BYTEA, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_feedback_attachments_feedback ON feedback_attachments (feedback_id)");
            log.info("Dance7 feedback_attachments schema verified.");
        } catch (Exception e) {
            log.error("Dance7 feedback_attachments migration failed; continuing boot.", e);
        }
    }
}
