package com.studioos.ai.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the AI-assistant tables and seeds the initial Whitefield branch dataset.
 * All business facts live in the database (editable via admin CRUD) — Java code
 * only contains table/column names and retrieval logic, never branch content.
 */
@Component
@Order(2)
public class AiChatSchemaMigration implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AiChatSchemaMigration.class);
    private final JdbcTemplate jdbc;

    public AiChatSchemaMigration(DataSource dataSource) { this.jdbc = new JdbcTemplate(dataSource); }

    @Override
    public void run(String... args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_classes (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, name VARCHAR(160) NOT NULL, category VARCHAR(60), min_age INT, max_age INT, experience_level VARCHAR(40), description VARCHAR(2000), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_class_schedules (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, ai_class_id BIGINT NOT NULL, day_of_week VARCHAR(20) NOT NULL, start_time VARCHAR(10) NOT NULL, end_time VARCHAR(10) NOT NULL, batch_label VARCHAR(160), instructor_name VARCHAR(160), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_packages (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, name VARCHAR(160) NOT NULL, duration_months INT, fee_amount NUMERIC(12,2), admission_fee NUMERIC(12,2), description VARCHAR(2000), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_studio_settings (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, setting_key VARCHAR(120) NOT NULL, setting_value VARCHAR(2000) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE (tenant_id, branch_id, setting_key))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_chat_leads (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, conversation_id BIGINT, student_name VARCHAR(160), parent_name VARCHAR(160), age INT, phone VARCHAR(30), email VARCHAR(160), interested_class VARCHAR(160), preferred_batch VARCHAR(160), preferred_start_date VARCHAR(40), message VARCHAR(2000), status VARCHAR(30) NOT NULL DEFAULT 'NEW', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_chat_conversations (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, visitor_id VARCHAR(80) NOT NULL, channel VARCHAR(30) NOT NULL DEFAULT 'WIDGET', status VARCHAR(30) NOT NULL DEFAULT 'OPEN', lead_captured BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_chat_messages (id BIGSERIAL PRIMARY KEY, conversation_id BIGINT NOT NULL, role VARCHAR(20) NOT NULL, content VARCHAR(4000) NOT NULL, intent VARCHAR(60), tool_name VARCHAR(80), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_conv ON ai_chat_messages (conversation_id)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_chat_faqs (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, question VARCHAR(500) NOT NULL, answer VARCHAR(4000) NOT NULL, keywords VARCHAR(500), sort_order INT NOT NULL DEFAULT 0, active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_chat_policies (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, title VARCHAR(200) NOT NULL, body VARCHAR(4000) NOT NULL, category VARCHAR(80), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_chat_offers (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, title VARCHAR(200) NOT NULL, body VARCHAR(4000) NOT NULL, valid_from DATE, valid_until DATE, active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_chat_audit (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT, conversation_id BIGINT, event VARCHAR(60) NOT NULL, detail VARCHAR(500), ip_hash VARCHAR(128), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        // The ON CONFLICT seed below needs this constraint even when Hibernate
        // (ddl-auto:update) created the table first without it — otherwise Postgres
        // raises 42P10, the runner throws, Spring Boot fails to start, and the
        // platform proxy returns 502 "Application failed to respond" for everything.
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_studio_settings_tenant_branch_key ON ai_studio_settings (tenant_id, branch_id, setting_key)");

        try {
            seedWhitefield();
        } catch (Exception e) {
            // Seed data must never prevent boot: the chat degrades gracefully and
            // knowledge can be entered via admin CRUD.
            log.error("Dance7 AI seed failed; continuing boot without seed data.", e);
        }
    }

    private void seedWhitefield() {
        Long branchId;
        try {
            branchId = jdbc.queryForObject(
                "SELECT id FROM branches WHERE tenant_id = 'default' AND LOWER(name) = 'whitefield' AND active = TRUE ORDER BY id LIMIT 1",
                Long.class);
        } catch (Exception e) {
            return;
        }
        if (branchId == null || jdbc.queryForObject("SELECT COUNT(*) FROM ai_classes WHERE branch_id = ?", Long.class, branchId) > 0) return;

        jdbc.update("INSERT INTO ai_classes (branch_id, name, category, min_age, max_age, experience_level, description, active) VALUES (?, 'Kids Dance Batch', 'KIDS', 4, 12, 'BEGINNER', 'Fun foundational batches for children covering Bollywood, hip-hop basics and stage confidence.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_classes (branch_id, name, category, min_age, max_age, experience_level, description, active) VALUES (?, 'Adult Dance Batch', 'ADULT', 13, NULL, 'ALL', 'Evening batches for teens and adults across Bollywood, hip-hop and contemporary styles.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_classes (branch_id, name, category, min_age, max_age, experience_level, description, active) VALUES (?, 'Bharatanatyam', 'BHARATANATYAM', 5, NULL, 'ALL', 'Classical Bharatanatyam training from adavus to stage-ready margam pieces.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_classes (branch_id, name, category, min_age, max_age, experience_level, description, active) VALUES (?, 'Contemporary & Freestyle', 'ADULT', 10, NULL, 'INTERMEDIATE', 'Contemporary technique and freestyle expression for dancers with some prior training.', TRUE)", branchId);

        seedSchedule(branchId, "Kids Dance Batch", "Saturday", "10:00", "11:00", "Kids Morning Batch");
        seedSchedule(branchId, "Kids Dance Batch", "Sunday", "10:00", "11:00", "Kids Morning Batch");
        seedSchedule(branchId, "Adult Dance Batch", "Monday", "19:00", "20:00", "Adults Evening Batch");
        seedSchedule(branchId, "Adult Dance Batch", "Wednesday", "19:00", "20:00", "Adults Evening Batch");
        seedSchedule(branchId, "Bharatanatyam", "Tuesday", "17:00", "18:00", "Classical Evening Batch");
        seedSchedule(branchId, "Bharatanatyam", "Thursday", "17:00", "18:00", "Classical Evening Batch");
        seedSchedule(branchId, "Contemporary & Freestyle", "Friday", "19:00", "20:00", "Contemporary Batch");

        jdbc.update("INSERT INTO ai_packages (branch_id, name, duration_months, fee_amount, admission_fee, description, active) VALUES (?, 'Kids Monthly', 1, 2500, 1000, 'One month of weekend Kids Dance Batch sessions.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_packages (branch_id, name, duration_months, fee_amount, admission_fee, description, active) VALUES (?, 'Adult Monthly', 1, 2700, 1000, 'One month of evening Adult Dance Batch sessions.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_packages (branch_id, name, duration_months, fee_amount, admission_fee, description, active) VALUES (?, 'Bharatanatyam Quarterly', 3, 7000, 1000, 'Three months of classical Bharatanatyam training.', TRUE)", branchId);

        setting(branchId, "admission_fee", "1000");
        setting(branchId, "contact_phone", "9731067867");
        setting(branchId, "trial_info", "New students can book one trial class before enrolling. Ask the studio team to schedule it.");
        setting(branchId, "branch_address", "Dance7 Whitefield — please confirm the exact studio address with the front desk when you call.");
        setting(branchId, "unknown_info_template", "I don''t have that information for the {branch} branch yet. Please contact the studio at {phone}.");

        faq(branchId, "What are the fees and packages?", "Our current Whitefield packages: Kids Monthly ₹2500, Adult Monthly ₹2700, Bharatanatyam Quarterly ₹7000, plus a one-time admission fee of ₹1000. The front-desk team can confirm the latest offers.", "fees,fee,price,cost,package,charges,admission fee", 1);
        faq(branchId, "What are the class timings?", "Kids Dance Batch runs Sat–Sun 10:00–11:00, Adult Dance Batch Mon & Wed 19:00–20:00, Bharatanatyam Tue & Thu 17:00–18:00, Contemporary Friday 19:00–20:00. Timings can change, so confirm your batch with the studio.", "timing,timings,schedule,when,batch time,class time", 2);
        faq(branchId, "Is there a trial class?", "Yes — new students can book one trial class before enrolling. Share your details and we will arrange it with the studio team.", "trial,demo,free class,try", 3);
        faq(branchId, "How do I contact the Whitefield studio?", "You can reach the Whitefield studio at 9731067867. Share your details here and our team will also call you back.", "contact,phone,number,call,address,where,location", 4);
        faq(branchId, "Do you have classes for kids?", "Yes — our Kids Dance Batch (ages 4–12, beginner friendly) runs every Saturday and Sunday 10:00–11:00.", "kids,children,child,age 5,age 6,junior", 5);
        faq(branchId, "Do you teach Bharatanatyam?", "Yes — classical Bharatanatyam training from adavus to stage pieces, every Tuesday and Thursday 17:00–18:00, open to ages 5+.", "bharatanatyam,classical", 6);

        jdbc.update("INSERT INTO ai_chat_policies (branch_id, title, body, category, active) VALUES (?, 'Admission policy', 'Admission is confirmed after the admission fee and first package fee are paid. A free trial class can be taken before enrolling.', 'ADMISSION', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_chat_policies (branch_id, title, body, category, active) VALUES (?, 'Fee and refund policy', 'Fees are charged per package cycle. Refunds are handled case-by-case by the branch manager; please contact the studio for assistance.', 'FEES', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_chat_policies (branch_id, title, body, category, active) VALUES (?, 'Attendance policy', 'Students should attend their assigned batch slots. Missed classes are not automatically adjusted; speak to your instructor about catch-up options.', 'ATTENDANCE', TRUE)", branchId);

        jdbc.update("INSERT INTO ai_chat_offers (branch_id, title, body, valid_from, valid_until, active) VALUES (?, 'Welcome offer', 'New Whitefield admissions this season get priority batch choice plus a free trial class. Ask the front desk to apply it at enrollment.', DATE '2026-01-01', DATE '2027-12-31', TRUE)", branchId);
    }

    private void seedSchedule(Long branchId, String className, String day, String start, String end, String batch) {
        jdbc.update("INSERT INTO ai_class_schedules (branch_id, ai_class_id, day_of_week, start_time, end_time, batch_label, active) "
            + "SELECT ?, id, ?, ?, ?, ?, TRUE FROM ai_classes WHERE branch_id = ? AND name = ?",
            branchId, day, start, end, batch, branchId, className);
    }

    private void setting(Long branchId, String key, String value) {
        jdbc.update("INSERT INTO ai_studio_settings (branch_id, setting_key, setting_value) VALUES (?, ?, ?) "
            + "ON CONFLICT (tenant_id, branch_id, setting_key) DO NOTHING", branchId, key, value);
    }

    private void faq(Long branchId, String q, String a, String keywords, int order) {
        jdbc.update("INSERT INTO ai_chat_faqs (branch_id, question, answer, keywords, sort_order, active) VALUES (?, ?, ?, ?, ?, TRUE)",
            branchId, q, a, keywords, order);
    }
}
