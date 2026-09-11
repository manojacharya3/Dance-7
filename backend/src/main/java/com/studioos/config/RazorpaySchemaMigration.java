package com.studioos.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Razorpay + invoice-delivery columns. Additive only (IF NOT EXISTS);
 * failures are logged and never prevent boot.
 */
@Component
@Order(3)
public class RazorpaySchemaMigration implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(RazorpaySchemaMigration.class);
    private final JdbcTemplate jdbc;

    public RazorpaySchemaMigration(DataSource dataSource) { this.jdbc = new JdbcTemplate(dataSource); }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(80)");
            jdbc.execute("ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(80)");
            jdbc.execute("ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_signature VARCHAR(255)");
            jdbc.execute("ALTER TABLE payments ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP");
            jdbc.execute("ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(80)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_payments_razorpay_order ON payments (razorpay_order_id)");
            jdbc.execute("ALTER TABLE invoices ADD COLUMN IF NOT EXISTS pdf_data BYTEA");
            jdbc.execute("ALTER TABLE invoices ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP");
            jdbc.execute("CREATE TABLE IF NOT EXISTS payment_events (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL, payment_id BIGINT, event VARCHAR(60) NOT NULL, detail VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_payment_events_payment ON payment_events (payment_id)");
            log.info("Dance7 Razorpay schema verified.");
        } catch (Exception e) {
            log.error("Dance7 Razorpay schema migration failed; continuing boot.", e);
        }
    }
}
