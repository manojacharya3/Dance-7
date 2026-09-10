-- Run after the Spring Boot application creates the students table.
-- Safe to rerun: each sample row is identified by tenant and email.
INSERT INTO students (
    tenant_id, first_name, last_name, date_of_birth, gender, email, phone,
    address, emergency_contact, parent_name, parent_phone, dance_style,
    skill_level, medical_notes, student_photo_url, active, created_at, updated_at
)
SELECT 'default', 'Aanya', 'Sharma', DATE '2012-04-18', 'Female', 'aanya.sharma@example.com', '+1 555 010 1001',
       '12 Maple Street', 'Rohan Sharma - +1 555 010 2001', 'Rohan Sharma', '+1 555 010 2001', 'Ballet',
       'Intermediate', 'Mild asthma. Keep inhaler with front desk.', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM students WHERE tenant_id = 'default' AND email = 'aanya.sharma@example.com');

INSERT INTO students (
    tenant_id, first_name, last_name, date_of_birth, gender, email, phone,
    address, emergency_contact, parent_name, parent_phone, dance_style,
    skill_level, medical_notes, student_photo_url, active, created_at, updated_at
)
SELECT 'default', 'Maya', 'Patel', DATE '2010-09-02', 'Female', 'maya.patel@example.com', '+1 555 010 1002',
       '44 Oak Avenue', 'Priya Patel - +1 555 010 2002', 'Priya Patel', '+1 555 010 2002', 'Contemporary',
       'Advanced', 'No known conditions.', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM students WHERE tenant_id = 'default' AND email = 'maya.patel@example.com');

INSERT INTO students (
    tenant_id, first_name, last_name, date_of_birth, gender, email, phone,
    address, emergency_contact, parent_name, parent_phone, dance_style,
    skill_level, medical_notes, student_photo_url, active, created_at, updated_at
)
SELECT 'default', 'Leo', 'Williams', DATE '2014-01-27', 'Male', 'leo.williams@example.com', '+1 555 010 1003',
       '8 Cedar Lane', 'Jordan Williams - +1 555 010 2003', 'Jordan Williams', '+1 555 010 2003', 'Hip-hop',
       'Beginner', NULL, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM students WHERE tenant_id = 'default' AND email = 'leo.williams@example.com');

INSERT INTO students (
    tenant_id, first_name, last_name, date_of_birth, gender, email, phone,
    address, emergency_contact, parent_name, parent_phone, dance_style,
    skill_level, medical_notes, student_photo_url, active, created_at, updated_at
)
SELECT 'default', 'Sofia', 'Garcia', DATE '2011-06-11', 'Female', 'sofia.garcia@example.com', '+1 555 010 1004',
       '71 Birch Road', 'Elena Garcia - +1 555 010 2004', 'Elena Garcia', '+1 555 010 2004', 'Jazz',
       'Intermediate', 'Allergy to latex.', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM students WHERE tenant_id = 'default' AND email = 'sofia.garcia@example.com');