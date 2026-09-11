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
        try {
            createTables();
        } catch (Exception e) {
            // DDL must never prevent boot: the chat degrades gracefully.
            log.error("Dance7 AI table creation failed; continuing boot without AI tables.", e);
            return;
        }
        try {
            seedWhitefield();
        } catch (Exception e) {
            // Seed data must never prevent boot: the chat degrades gracefully and
            // knowledge can be entered via admin CRUD.
            log.error("Dance7 AI seed failed; continuing boot without seed data.", e);
        }
    }

    private void createTables() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_classes (id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', branch_id BIGINT NOT NULL, name VARCHAR(160) NOT NULL, category VARCHAR(60), min_age INT, max_age INT, experience_level VARCHAR(40), fee_amount NUMERIC(12,2), description VARCHAR(2000), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
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
        // Class-level fee (spec: each class carries its own fee).
        jdbc.execute("ALTER TABLE ai_classes ADD COLUMN IF NOT EXISTS fee_amount NUMERIC(12,2)");
        // Deduplicate settings first: earlier seed attempts may have left duplicate
        // (tenant, branch, key) rows, which would make the unique index below fail.
        jdbc.execute("DELETE FROM ai_studio_settings a USING ai_studio_settings b "
            + "WHERE a.id < b.id AND a.tenant_id = b.tenant_id AND a.branch_id = b.branch_id AND a.setting_key = b.setting_key");
        // The ON CONFLICT seed below needs this constraint even when Hibernate
        // (ddl-auto:update) created the table first without it — otherwise Postgres
        // raises 42P10, the runner throws, Spring Boot fails to start, and the
        // platform proxy returns 502 "Application failed to respond" for everything.
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_studio_settings_tenant_branch_key ON ai_studio_settings (tenant_id, branch_id, setting_key)");
    }

    /**
     * Seeds the verified Whitefield dataset (spec v2). Versioned via the
     * dataset_version setting: re-runs (replacing only AI knowledge, never leads or
     * conversations) whenever the bundled dataset is newer than the database.
     * Every fact below is branch data — Java code contains none of it.
     */
    private static final String DATASET_VERSION = "whitefield-v2";

    private void seedWhitefield() {
        Long branchId;
        try {
            branchId = jdbc.queryForObject(
                "SELECT id FROM branches WHERE tenant_id = 'default' AND LOWER(name) = 'whitefield' AND active = TRUE ORDER BY id LIMIT 1",
                Long.class);
        } catch (Exception e) {
            return;
        }
        if (branchId == null) return;
        String current = null;
        try {
            current = jdbc.queryForObject(
                "SELECT setting_value FROM ai_studio_settings WHERE tenant_id = 'default' AND branch_id = ? AND setting_key = 'dataset_version'",
                String.class, branchId);
        } catch (Exception e) {
            current = null;
        }
        if (DATASET_VERSION.equals(current)) {
            // Version matches but knowledge may still be empty (partial seed or manual
            // wipe): reseed to self-heal instead of leaving the chatbot with no data.
            Long classCount = jdbc.queryForObject("SELECT COUNT(*) FROM ai_classes WHERE branch_id = ?",
                Long.class, branchId);
            if (classCount != null && classCount > 0) {
                log.info("Dance7 AI dataset {} already present for Whitefield branch {}.", DATASET_VERSION, branchId);
                return;
            }
            log.warn("Dance7 AI dataset {} marked but knowledge empty for Whitefield branch {}; reseeding.",
                DATASET_VERSION, branchId);
        }

        // Replace AI knowledge for this branch only. Leads, conversations, messages
        // and audit rows are user data and are never touched.
        jdbc.update("DELETE FROM ai_class_schedules WHERE branch_id = ?", branchId);
        jdbc.update("DELETE FROM ai_classes WHERE branch_id = ?", branchId);
        jdbc.update("DELETE FROM ai_packages WHERE branch_id = ?", branchId);
        jdbc.update("DELETE FROM ai_chat_faqs WHERE branch_id = ?", branchId);
        jdbc.update("DELETE FROM ai_chat_policies WHERE branch_id = ?", branchId);
        jdbc.update("DELETE FROM ai_chat_offers WHERE branch_id = ?", branchId);
        jdbc.update("DELETE FROM ai_studio_settings WHERE branch_id = ?", branchId);

        seedClass(branchId, "Sub Juniors", "KIDS", 3, 5, "BEGINNER", 2500,
            "For tiny stars taking their first dance steps. Fun-filled sessions focused on rhythm, coordination and stage confidence.");
        seedClass(branchId, "Juniors", "KIDS", 6, 8, "BEGINNER", 2500,
            "For young movers ready to level up their skills, focusing on technique, musicality and stage performance.");
        seedClass(branchId, "Level 1 Kids", "KIDS", 9, 12, "BEGINNER", 2500,
            "A specially designed batch where kids are introduced to various dance styles and basic techniques. The class focuses on improving skills, rhythm and confidence in a fun and supportive environment.");
        seedClass(branchId, "Level 2 Kids", "KIDS", 13, 15, "INTERMEDIATE", 2500,
            "A high-energy batch for kids ready to take their dancing to the next level, with faster movements, choreography, sharper techniques, rhythm, confidence and stage presence.");
        seedClass(branchId, "Freestyle Adults Beginners", "ADULT", 16, null, "BEGINNER", 2500,
            "Discover different dance styles and progress from beginner to confident dancer. Build confidence, improve fitness and express yourself freely. No prior experience is required.");
        seedClass(branchId, "Level 1 Adults", "ADULT", 16, null, "INTERMEDIATE", 2500,
            "A progression from freestyle for dancers ready to step up their skills, focusing on sharper techniques, complex routines and stage-ready confidence.");
        seedClass(branchId, "Bharatanatyam", "BHARATANATYAM", null, null, "ALL", 2500,
            "A graceful classical dance form that combines rhythm, expression and tradition. Suitable for people looking to connect with culture and learn storytelling through movement.");

        seedSchedule(branchId, "Sub Juniors", "Monday", "4:30 PM", "5:30 PM", "Sub Juniors");
        seedSchedule(branchId, "Sub Juniors", "Wednesday", "4:30 PM", "5:30 PM", "Sub Juniors");
        seedSchedule(branchId, "Sub Juniors", "Friday", "4:30 PM", "5:30 PM", "Sub Juniors");
        seedSchedule(branchId, "Juniors", "Monday", "5:30 PM", "6:30 PM", "Juniors");
        seedSchedule(branchId, "Juniors", "Wednesday", "5:30 PM", "6:30 PM", "Juniors");
        seedSchedule(branchId, "Juniors", "Friday", "5:30 PM", "6:30 PM", "Juniors");
        seedSchedule(branchId, "Level 1 Kids", "Monday", "6:30 PM", "7:30 PM", "Level 1 Kids");
        seedSchedule(branchId, "Level 1 Kids", "Wednesday", "6:30 PM", "7:30 PM", "Level 1 Kids");
        seedSchedule(branchId, "Level 1 Kids", "Friday", "6:30 PM", "7:30 PM", "Level 1 Kids");
        seedSchedule(branchId, "Level 2 Kids", "Tuesday", "5:00 PM", "6:30 PM", "Level 2 Kids");
        seedSchedule(branchId, "Level 2 Kids", "Thursday", "5:00 PM", "6:30 PM", "Level 2 Kids");
        seedSchedule(branchId, "Freestyle Adults Beginners", "Monday", "7:30 PM", "8:30 PM", "Freestyle Adults Beginners");
        seedSchedule(branchId, "Freestyle Adults Beginners", "Wednesday", "7:30 PM", "8:30 PM", "Freestyle Adults Beginners");
        seedSchedule(branchId, "Level 1 Adults", "Thursday", "7:30 PM", "8:30 PM", "Level 1 Adults");
        seedSchedule(branchId, "Level 1 Adults", "Friday", "7:30 PM", "8:30 PM", "Level 1 Adults");
        seedSchedule(branchId, "Bharatanatyam", "Saturday", "2:30 PM", "3:30 PM", "Bharatanatyam");
        seedSchedule(branchId, "Bharatanatyam", "Sunday", "9:00 AM", "10:00 AM", "Bharatanatyam");

        jdbc.update("INSERT INTO ai_packages (branch_id, name, duration_months, fee_amount, description, active) VALUES (?, '1 Month', 1, 2500, 'Monthly package.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_packages (branch_id, name, duration_months, fee_amount, description, active) VALUES (?, '3 Months', 3, 6750, '10% OFF.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_packages (branch_id, name, duration_months, fee_amount, description, active) VALUES (?, '6 Months', 6, 13005, '15% OFF.', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_packages (branch_id, name, duration_months, fee_amount, description, active) VALUES (?, '12 Months', 12, 23040, '20% OFF.', TRUE)", branchId);

        setting(branchId, "admission_fee", "500");
        setting(branchId, "currency", "INR");
        setting(branchId, "contact_phone", "9731067867");
        setting(branchId, "trial_info", "I don''t have the current trial-class policy in my available Whitefield branch information. Please contact the branch at 9731067867 for the latest details.");
        setting(branchId, "enrollment_info", "To complete your enrollment, please contact the Whitefield branch at 9731067867.");
        setting(branchId, "unknown_info_template", "I don''t have that information for the {branch} branch yet.");
        setting(branchId, "dataset_version", DATASET_VERSION);

        faq(branchId, "What are the fees and packages?", "Whitefield packages: 1 Month \u20B92,500; 3 Months \u20B96,750 (10% OFF); 6 Months \u20B913,005 (15% OFF); 12 Months \u20B923,040 (20% OFF). One-time admission fee: \u20B9500.", "fees,fee,price,cost,package,charges,admission fee", 1);
        faq(branchId, "What are the class timings?", "Sub Juniors (3-5 yrs): Mon, Wed, Fri 4:30-5:30 PM. Juniors (6-8 yrs): Mon, Wed, Fri 5:30-6:30 PM. Level 1 Kids (9-12 yrs): Mon, Wed, Fri 6:30-7:30 PM. Level 2 Kids (13-15 yrs): Tue, Thu 5:00-6:30 PM. Freestyle Adults Beginners: Mon, Wed 7:30-8:30 PM. Level 1 Adults: Thu, Fri 7:30-8:30 PM. Bharatanatyam: Sat 2:30-3:30 PM, Sun 9:00-10:00 AM.", "timing,timings,schedule,when,batch time,class time", 2);
        faq(branchId, "How much is admission?", "The one-time admission fee is \u20B9500.", "admission,admission fee,joining fee,enrollment fee", 3);
        faq(branchId, "Do you have classes for kids?", "Yes - Sub Juniors (3-5 yrs), Juniors (6-8 yrs), Level 1 Kids (9-12 yrs) and Level 2 Kids (13-15 yrs), each \u20B92,500. Tell me the child''s age and I will point you to the right batch.", "kids,children,child,junior", 4);
        faq(branchId, "Do you teach Bharatanatyam?", "Yes - Bharatanatyam for all age groups, every Saturday 2:30-3:30 PM and Sunday 9:00-10:00 AM, \u20B92,500.", "bharatanatyam,classical", 5);
        faq(branchId, "Do you have a trial class?", "I don''t have the current trial-class policy in my available Whitefield branch information. Please contact the branch at 9731067867 for the latest details.", "trial,demo,free class,try", 6);
        faq(branchId, "How do I contact the Whitefield studio?", "You can reach the Dance7 Whitefield branch at 9731067867. Share your details here and our team will also call you back.", "contact,phone,number,call,where,location", 7);
        faq(branchId, "How do I join or enroll?", "Pick your batch, then share your details so the studio team can confirm your enrollment. To complete your enrollment, please contact the Whitefield branch at 9731067867.", "join,enroll,enrol,register,registration,admission,sign up", 8);

        jdbc.update("INSERT INTO ai_chat_policies (branch_id, title, body, category, active) VALUES (?, 'Admission policy', 'Admission needs a one-time admission fee of \u20B9500 plus the chosen package fee. Enrollment is confirmed by the studio team - the assistant itself never enrolls students.', 'ADMISSION', TRUE)", branchId);
        jdbc.update("INSERT INTO ai_chat_policies (branch_id, title, body, category, active) VALUES (?, 'Unlisted policies', 'Refund, makeup-class, holiday, capacity, payment-method, tax, document, uniform and parking policies are not published here. Please contact the Whitefield branch at 9731067867 for the latest details.', 'GENERAL', TRUE)", branchId);
    }

    private void seedClass(Long branchId, String name, String category, Integer minAge, Integer maxAge,
        String level, int fee, String description) {
        jdbc.update("INSERT INTO ai_classes (branch_id, name, category, min_age, max_age, experience_level, fee_amount, description, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
            branchId, name, category, minAge, maxAge, level, fee, description);
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
