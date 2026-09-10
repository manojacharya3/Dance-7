# Dance7 Codebase Review Report

**Scope:** Frontend, Spring Boot backend, PostgreSQL/JPA, authentication, Student Management, build configuration, and API integration.

## Executive Summary

Dance7 is a small Next.js 15 frontend with a Spring Boot 3.4.1/Java 21 backend and PostgreSQL persistence. Student CRUD is implemented and the backend clean test build passes. The highest risks are authentication hardening, tenant isolation, secrets/configuration, schema migration strategy, and missing automated tests.

## Architecture

- `app/`: Next.js App Router pages for login, registration, dashboard, and students.
- `components/`: shared sidebar, navbar, authentication guard, and student form.
- `lib/`: browser API clients for auth and students.
- `backend/src/main/java/com/studioos/controller`: HTTP endpoints and exception advice.
- `backend/src/main/java/com/studioos/service`: business services.
- `backend/src/main/java/com/studioos/repository`: Spring Data repositories.
- `backend/src/main/java/com/studioos/model`: JPA entities.
- `backend/src/main/java/com/studioos/dto`: request/response records.
- `backend/src/main/java/com/studioos/config`: security, JWT, CORS, and application configuration.

The layering is recognizable, but controllers accept tenant identity from request parameters/bodies and the frontend hardcodes the `default` tenant.

## Findings

### Critical

1. **Tenant isolation is not enforced by authenticated identity.** `StudentController` accepts `tenantId` from query parameters, and `StudentDTO` accepts it from request bodies. A logged-in user can request another tenant's data or update/delete records by supplying another tenant ID. Fix by deriving tenant ID from the authenticated principal and removing tenant ID from client-controlled student operations.

2. **JWT fallback secret is committed in `application.yml`.** The property `${JWT_SECRET:<base64 value>}` provides a known signing key when the environment variable is absent. Anyone with the source can forge tokens. Make `JWT_SECRET` mandatory outside a local profile and fail startup if missing; use a secret manager.

3. **Database credentials are committed as defaults.** `application.yml` contains `postgres` and `postgres123` fallbacks. Remove production credentials from source and use environment/secret-manager values.

### High

4. **Refresh-token flow is not fully verified and has an observed 403 history.** Current source permits `POST /api/auth/refresh`, disables CSRF, and controller code returns 401 for missing/invalid refresh tokens. A 403 therefore indicates stale runtime/configuration or an external filter; add integration tests that assert valid refresh=200, missing/invalid=401, and no CSRF denial.

5. **CSRF is globally disabled while authentication uses cookies.** HttpOnly cookies are sent automatically by browsers, so cookie-authenticated state-changing requests need CSRF protection or a robust same-site/token strategy. Re-enable CSRF with a cookie/token pattern, or document and enforce a strict same-site deployment model.

6. **Security TRACE logging is enabled in the committed default configuration.** `FilterChainProxy`, authentication, authorization, and CSRF TRACE logs can expose request/security details and create high-volume logs. Restrict this to a local profile and set production levels to INFO/WARN.

7. **No authentication/authorization automated tests exist.** The backend has no `src/test` files. This leaves refresh rotation, logout revocation, cookie flags, protected routes, and tenant boundaries unverified.

### Medium

8. **`ddl-auto: update` is used as schema management.** It is unsafe for controlled production deployments. Use Flyway/Liquibase migrations and validate schema changes in CI.

9. **Password/login error handling is not normalized.** `IllegalArgumentException`, authentication failures, validation failures, and data errors do not share a documented error envelope. Add a global handler for validation, bad credentials, duplicate email, malformed JWT, and database exceptions.

10. **Cookie security is development-only.** `Cookie#setSecure(false)` is hardcoded and cookies do not set SameSite explicitly. Use `Secure=true` under HTTPS and configure SameSite/Domain/Path by environment.

11. **Refresh token is stored as a raw database value.** A database read leak permits session replay. Store a hash of the refresh token and compare hashes, or use a rotating opaque-token strategy with hashed persistence.

12. **Frontend auth guard is client-side only.** A protected page initially renders a loading shell and redirects after `/api/auth/me`; it is not a server-side route boundary. Add Next middleware/server checks if SSR protection is required.

13. **Student list delete has no local error state.** `handleDelete` awaits `deleteStudent` without a catch; a failed delete can produce an unhandled rejection and stale UI.

14. **Frontend search requests are un-debounced.** Every keystroke calls the API. Add debounce/cancellation for production-scale data.

### Low

15. **Dashboard is mostly static sample data.** Project metrics, activity, and controls are not API-backed.

16. **`RefreshRequest` is unused.** The DTO exists but current refresh reads only the cookie. Remove it or support a documented non-cookie client flow.

17. **The old `@CrossOrigin` annotation duplicates global CORS configuration.** Keep one source of truth and make allowed origins environment-driven.

18. **Date and greeting text are hardcoded.** The dashboard and navbar show a fixed September 5, 2026 date.

## Code Smells and Incomplete Areas

- Long single-line JSX components reduce maintainability and reviewability.
- No error boundary, toast system, or field-level server validation display.
- No tenant entity, class entity, audit trail, or role administration UI.
- No pagination test for empty/last-page behavior.
- No API contract/OpenAPI document.

## Recommended Priority Order

1. Remove committed secrets/default credentials and require environment configuration.
2. Enforce tenant identity from JWT, not request parameters.
3. Add auth/refresh/student integration tests and resolve the observed refresh 403 with a reproducible test.
4. Establish migrations and production logging profiles.
5. Harden cookie/CSRF/refresh-token storage.
6. Improve frontend error handling, debounce, and server-side protection.
