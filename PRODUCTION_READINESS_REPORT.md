# Dance7 Production Readiness Report

## Verdict

**Not production ready.** The application is suitable for local development and a controlled prototype, but authentication, tenant isolation, secret management, migrations, observability, and test coverage require work before production deployment.

## Security

- JWT signing key has a committed fallback: critical.
- Database credentials have committed fallbacks: critical.
- Tenant ID is client-controlled: critical.
- Student APIs are protected by the current security chain, but authorization is not tenant-safe.
- CSRF is disabled while auth is cookie-based: high risk; establish a deliberate mitigation.
- Refresh tokens are stored plaintext: high risk.
- Cookies hardcode `Secure=false` and omit SameSite: high risk for production.
- TRACE security logs are enabled in default config: high risk for production.
- Passwords use BCrypt: positive.
- Access tokens are short-lived and refresh tokens rotate/revoke: positive, pending tests.

## Authentication and Authorization

Registration assigns STAFF by default. ADMIN exists as an enum but there is no admin-management workflow or authorization policy. No endpoint uses role restrictions. Tenant authorization is not derived from JWT claims at the controller/service boundary.

## Error Handling

Only EntityNotFoundException has a dedicated JSON handler. Validation, duplicate registration, bad credentials, malformed JWT, database failures, and access-denied errors need stable response envelopes and correlation IDs.

## Logging and Monitoring

Security TRACE is useful for local diagnosis but must be profile-scoped. There is no structured logging, request correlation, metrics, tracing, readiness probe, or production alerting configuration. Actuator health exposure exists but actuator dependency/health strategy should be explicitly validated.

## Database and Deployment

- PostgreSQL integration works locally when configured.
- `ddl-auto=update` must be replaced by migrations.
- No Dockerfile, Docker Compose, CI workflow, or deployment manifest is present.
- No backup/restore, connection pool, TLS, or secret rotation runbook is present.

## Readiness Gates

Before release: remove source secrets, add tenant/role authorization, harden cookies/CSRF/refresh storage, add migrations, add automated coverage, configure production logging/metrics, add container/CI assets, and run dependency/security scans.
