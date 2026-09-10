# Dance7 Database Review

## Current Schema Model

| Entity | Table | Relationships |
|---|---|---|
| User | `users` | Many-to-many with Role through `user_roles` |
| Role | `roles` | Many-to-many with User |
| RefreshToken | `refresh_tokens` | Many-to-one User |
| Student | `students` | No entity relationships |

There is no Tenant entity/table and no DanceClass entity/table in the application model.

## Student Mapping

- Identity: generated `Long` with PostgreSQL identity strategy.
- Required: `tenant_id`, `first_name`, `last_name`, `active`, `created_at`, `updated_at`.
- Soft delete: `active=false`; repository queries only active rows.
- Search index: tenant/active composite index and email index exist.
- Timestamps: JPA `@PrePersist` and `@PreUpdate`.
- Medical notes: PostgreSQL `TEXT`.

## Auth Mapping

- Users have unique email, BCrypt-encoded password, full name, tenant ID, enabled flag, created timestamp.
- Roles use enum string values `ADMIN` and `STAFF`.
- Refresh tokens are unique opaque UUID strings, linked to users, expiry and revoked state.

## Findings

1. Tenant ID is a scalar copied from client input, not a foreign key to a tenant table. This prevents referential integrity and enables tenant spoofing.
2. Refresh tokens are stored plaintext; hash them at rest.
3. `ddl-auto=update` is not a production migration strategy.
4. There is no unique student email constraint, so duplicate records are possible.
5. There is no audit history for create/update/delete or who performed a soft delete.
6. No foreign-key relationship connects students to a tenant entity.
7. No class/enrollment tables exist, so dance-class seed data cannot be consumed by current application code.
8. The `roles`/`user_roles` schema has no explicit seed migration and role creation occurs during registration only for STAFF.

## Index Recommendations

- Unique or tenant-scoped index on `(tenant_id, email)` if email uniqueness is a business rule.
- Index `(tenant_id, active, updated_at DESC)` for the list query.
- PostgreSQL trigram indexes for large-scale name/email search instead of leading-wildcard LIKE.
- Index `refresh_tokens(user_id, revoked, expires_at)` for cleanup/validation.
- Unique index on normalized lowercase user email is already approximated by the case-insensitive lookup but database uniqueness is currently case-sensitive.

## Migration Recommendations

Introduce `tenants`, `dance_classes`, and enrollment tables; migrate scalar tenant IDs to foreign keys; add Flyway/Liquibase migrations; backfill constraints and indexes; then disable Hibernate schema mutation in production.
