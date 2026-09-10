-- Dance7 review seed data.
-- Run after the backend has started once so Hibernate creates users/roles/user_roles/refresh_tokens/students.
-- The tenants and dance_classes tables are currently not mapped by the application; they are included here
-- as reference data for future Tenant/Class entities and are not consumed by current APIs.

CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS dance_classes (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL REFERENCES tenants(id),
    name VARCHAR(150) NOT NULL,
    dance_style VARCHAR(100) NOT NULL,
    skill_level VARCHAR(60) NOT NULL,
    schedule_text VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO tenants (id, name, active)
VALUES ('default', 'Dance7 - The Art Factory', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO roles (name)
VALUES ('ADMIN')
ON CONFLICT (name) DO NOTHING;

-- Password: password
-- Replace this development-only hash immediately outside local testing.
INSERT INTO users (email, password, full_name, tenant_id, enabled, created_at)
SELECT 'admin@dance7.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Dance7 Administrator', 'default', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE lower(email) = 'admin@dance7.test');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE lower(u.email) = 'admin@dance7.test' AND r.name = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO dance_classes (tenant_id, name, dance_style, skill_level, schedule_text)
SELECT 'default', v.name, v.style, v.level, v.schedule
FROM (VALUES
    ('Tiny Tots Ballet', 'Ballet', 'Beginner', 'Saturday 09:00'),
    ('Contemporary Lab', 'Contemporary', 'Advanced', 'Tuesday 18:00'),
    ('Street Foundations', 'Hip-hop', 'Intermediate', 'Thursday 17:30')
) AS v(name, style, level, schedule)
WHERE NOT EXISTS (SELECT 1 FROM dance_classes c WHERE c.tenant_id = 'default' AND c.name = v.name);

INSERT INTO students (
    tenant_id, first_name, last_name, date_of_birth, gender, email, phone,
    address, emergency_contact, parent_name, parent_phone, dance_style,
    skill_level, medical_notes, active, created_at, updated_at
)
SELECT 'default', v.first_name, v.last_name, v.dob, v.gender, v.email, v.phone,
       v.address, v.emergency, v.parent_name, v.parent_phone, v.style,
       v.level, v.notes, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('Aanya','Sharma',DATE '2012-04-18','Female','aanya.seed@dance7.test','+1 555 010 1001','12 Maple Street','Rohan Sharma - +1 555 010 2001','Rohan Sharma','+1 555 010 2001','Ballet','Intermediate','Mild asthma'),
    ('Maya','Patel',DATE '2010-09-02','Female','maya.seed@dance7.test','+1 555 010 1002','44 Oak Avenue','Priya Patel - +1 555 010 2002','Priya Patel','+1 555 010 2002','Contemporary','Advanced','None'),
    ('Leo','Williams',DATE '2014-01-27','Male','leo.seed@dance7.test','+1 555 010 1003','8 Cedar Lane','Jordan Williams - +1 555 010 2003','Jordan Williams','+1 555 010 2003','Hip-hop','Beginner','None'),
    ('Sofia','Garcia',DATE '2011-06-11','Female','sofia.seed@dance7.test','+1 555 010 1004','71 Birch Road','Elena Garcia - +1 555 010 2004','Elena Garcia','+1 555 010 2004','Jazz','Intermediate','Latex allergy'),
    ('Ethan','Brown',DATE '2013-02-08','Male','ethan.seed@dance7.test','+1 555 010 1005','19 Pine Road','Grace Brown - +1 555 010 2005','Grace Brown','+1 555 010 2005','Tap','Beginner','None'),
    ('Isla','Martin',DATE '2009-12-14','Female','isla.seed@dance7.test','+1 555 010 1006','33 Willow Drive','Nina Martin - +1 555 010 2006','Nina Martin','+1 555 010 2006','Ballet','Advanced','None'),
    ('Noah','Wilson',DATE '2012-08-30','Male','noah.seed@dance7.test','+1 555 010 1007','5 Lake View','Sam Wilson - +1 555 010 2007','Sam Wilson','+1 555 010 2007','Jazz','Intermediate','None'),
    ('Zoe','Taylor',DATE '2015-05-19','Female','zoe.seed@dance7.test','+1 555 010 1008','62 Hill Street','Alex Taylor - +1 555 010 2008','Alex Taylor','+1 555 010 2008','Acro','Beginner','None'),
    ('Arjun','Mehta',DATE '2011-10-05','Male','arjun.seed@dance7.test','+1 555 010 1009','27 Garden Lane','Neha Mehta - +1 555 010 2009','Neha Mehta','+1 555 010 2009','Hip-hop','Advanced','None'),
    ('Lina','Chen',DATE '2013-07-23','Female','lina.seed@dance7.test','+1 555 010 1010','90 River Road','Wei Chen - +1 555 010 2010','Wei Chen','+1 555 010 2010','Contemporary','Intermediate','None')
) AS v(first_name,last_name,dob,gender,email,phone,address,emergency,parent_name,parent_phone,style,level,notes)
WHERE NOT EXISTS (SELECT 1 FROM students s WHERE s.tenant_id = 'default' AND s.email = v.email);
