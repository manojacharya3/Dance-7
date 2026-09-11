# Auth Endpoint Security Fix (`POST /api/auth/login`, `/api/auth/register` → 401)

## Root cause

Live probes of `dance7-api-production.up.railway.app` showed:
- `GET /actuator/health` → `200 {"status":"UP"}` (backend up, actuator present).
- `GET /auth/login` (no `/api` prefix) → `401` — expected: unmapped path +
  unauthenticated request falls into `anyRequest().authenticated()` and the
  entry point answers `401`. Any client whose base URL lacks the `/api` suffix
  (e.g. `NEXT_PUBLIC_API_URL=https://<host>` instead of `https://<host>/api`)
  sees exactly the reported symptom: health `200`, login/register `401`.
- The reviewed rules already contained `permitAll` for `/api/auth/**`, so a
  second contributor is a stale deployment (pre-fix image still serving).

No controller bug: `AuthController` maps `POST /api/auth/login` and
`POST /api/auth/register`, both under the permitted `/api/auth/**` space, and
neither `JwtAuthenticationFilter` (passive) nor `RoleScopeFilter` (skips
`/api/auth`) blocks them.

## Files modified

- `backend/.../config/SecurityConfiguration.java` — explicit method matchers:
  `POST /api/auth/login`, `/api/auth/register` (plus existing `/api/auth/refresh`)
  `permitAll`, ahead of the general `/api/auth/**` rule. Defense in depth; behavior
  for all other endpoints unchanged.

## Security changes

- Public: `POST /api/auth/login`, `POST /api/auth/register`
  (plus pre-existing public: `OPTIONS /**`, `POST /api/auth/refresh`,
  `/actuator/health`, `/actuator/info`, `/api/health`, `/api/auth/**`).
- Protected: everything else still requires authentication
  (`anyRequest().authenticated()`); no filter, role, or business-logic change.

## Validation results

- Reviewed `SecurityConfiguration`, `AuthController` mappings,
  `JwtAuthenticationFilter`, `RoleScopeFilter`, `SecurityConfig` (no second
  filter chain), `AuthDebugController` (no mapping clash).
- No local JVM build available; compilation is covered by Railway's Docker build.
- Post-deploy verification (must be done against a fresh deploy of this commit):
  `POST https://<backend>/api/auth/login` with bad credentials → `401` with
  controller JSON (proves the controller answers, not the entry point);
  with valid credentials → `200` + cookies. Same for `/api/auth/register`.

## Commit / push

- Commit: `Allow public authentication endpoints` (hash below after push).
- Push: `origin/main`.
- If 401s persist after redeploy, check: (1) deployed commit SHA equals this
  fix, (2) client base URL ends with `/api`.
