# Railway Healthcheck Fix (`/actuator/health` → 401)

## Root cause

`spring-boot-starter-actuator` was missing from `backend/pom.xml`, so Spring
Boot never registered the `/actuator/health` endpoint. The `permitAll` line in
`SecurityConfiguration` alone cannot create an endpoint — unauthenticated
requests to the unmapped path fell through the security chain and were rejected
by the authentication entry point with `401`. (Separately, only `/actuator/health`
was permit-listed; `/actuator/info` was not.)

## Files modified

- `backend/pom.xml` — added `spring-boot-starter-actuator` (same
  `spring-boot-starter-parent` 3.4.1 line, no version pin needed).
- `backend/src/main/java/com/studioos/config/SecurityConfiguration.java` —
  permit list extended to `/actuator/health` + `/actuator/info`
  (`/api/health`, `/api/auth/**` unchanged).
- `application.yml` — no change needed: `management.endpoints.web.exposure`
  already includes `health,info`.

## Security changes

- Anonymous access: `GET /actuator/health`, `GET /actuator/info` only.
- Everything else unchanged: `anyRequest().authenticated()`, JWT filter,
  `RoleScopeFilter`, CSRF disabled as before. No business logic touched.

## Validation results

- `pom.xml` hunk verified in place; yml exposure confirmed (`health,info`).
- No local JVM/Maven build available in this environment, so compilation is
  validated by Railway's Docker build (`backend/Dockerfile`, Temurin 21).
- Frontend untouched (last `npm run build` green); no `tsc` re-run required.
- Post-deploy expectation: `GET /actuator/health` → `200 {"status":"UP"}`,
  Railway healthcheck turns healthy; all `/api/**` still require auth.

## Commit / push

- Commit: `Allow Railway health check endpoint` (hash below after push).
- Push: `origin/main`.
