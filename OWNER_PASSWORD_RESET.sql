-- OWNER password reset: owner@dance7.com / Test@123
-- Hash: BCrypt, cost 10, $2a$ format (matches BCryptPasswordEncoder default).
-- Run in the Neon SQL editor against the production database.

UPDATE users
SET password = '$2a$10$SiyIVh2X3yXEfYjhwG.idO5wUFI9Pj0/Kn/SvND1oUGScoCsg70J6',
    enabled = true
WHERE lower(email) = 'owner@dance7.com';

-- Verify: exactly one row, enabled = true.
SELECT id, email, full_name, enabled FROM users WHERE lower(email) = 'owner@dance7.com';
