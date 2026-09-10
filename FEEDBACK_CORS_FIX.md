# Feedback Status Update CORS Fix

## Failing endpoint

`PATCH /api/feedback/{id}/status` (and likewise `PATCH /api/feedback/{id}/notes`).

- HTTP method: **PATCH** (`lib/feedback.ts` → `updateFeedbackStatus` / `updateFeedbackNotes`).
- List (`GET /api/feedback`) worked; only the status transition
  `OPEN → IN_PROGRESS` / `OPEN → RESOLVED` failed with `403 Invalid CORS request`.

## Current CORS configuration

`backend/.../config/CorsConfig.java` (global `CorsConfigurationSource`, `/**`):
- Origins: `http://localhost:3000`, `http://localhost:3001` ✓
- Methods (before fix): `GET, POST, PUT, DELETE, OPTIONS` — **no PATCH** ✗
- Headers: `Content-Type, Authorization` ✓; credentials allowed ✓.

`SecurityConfiguration`: `cors(withDefaults())`, `OPTIONS /**` permitAll,
everything else authenticated — preflight itself was reachable.

## Root cause

PATCH is not a CORS-safelisted method, so the browser sends a preflight
`OPTIONS` request. Spring's `CorsProcessor` validates the *actual* method
against `allowedMethods`; PATCH was absent, so preflight was rejected with
`403 Invalid CORS request` and the browser never sent the PATCH. GET/POST/PUT
callers were unaffected, which is why only status/notes updates failed.

Reachability otherwise confirmed: `FeedbackController` is
`@CrossOrigin(origins = {3000, 3001})`, `PATCH /{id}/status|notes` requires only
authentication, and `RoleScopeFilter` permits OWNER (unrestricted) and DEVELOPER
(feedback-triage rule).

## Files modified

- `backend/.../config/CorsConfig.java` (1 line): added `PATCH` to
  `setAllowedMethods` → `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

## Verification steps

1. Restart backend.
2. As OWNER or DEVELOPER: Feedback Center → change a ticket
   `OPEN → IN_PROGRESS` → `RESOLVED`; internal-notes save.
3. DevTools Network: `OPTIONS /api/feedback/{id}/status` → `200` with
   `Access-Control-Allow-Methods` containing `PATCH`; `PATCH` → `200`.
4. No other CORS settings changed; unrelated modules untouched.
