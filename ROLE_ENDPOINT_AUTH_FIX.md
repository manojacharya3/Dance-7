# Role Endpoint Authorization Fix (DEVELOPER read access)

## Endpoints failing

For `DEVELOPER`, collection calls returned `200` but the read calls issued by the
detail / reminder views were rejected by `RoleScopeFilter` (`403`; surfaced in the
UI as an auth failure with an empty-body fallback):

- `GET /api/student-batches/batch/{id}` (batch detail → assigned students)
- `GET /api/student-batches/student/{id}` (student 360 → assignments; reminders)
- `GET /api/attendance/student/{id}` (+ `/monthly-summary`, `/date/*` variants
  used by student detail / performance views)

Note on status codes: the role rules emit `403` (`sendError`). A bare `401` comes
only from the global entry point (`SecurityConfiguration`) when the request is
unauthenticated (missing/expired token) — e.g. a stale session after the account
was created. Both cases previously rendered the generic
`The request could not be completed` because the error body was empty.

## Role restrictions found

`RoleScopeFilter` developer rule allowed only `GET` on
`/api/(students|instructors|batches|memberships|payments|invoices|branches|student-batches|feedback)(/\d+)?`:
- `student-batches` real endpoints (`/batch/{id}`, `/student/{id}`) never matched
  the single-`/\d+` shape → blocked.
- `attendance` reads were absent entirely → student/batch detail pages (which call
  `getAttendanceByStudent`) failed even though every other call on those pages
  was permitted.

Collection vs detail were therefore inconsistent: `GET /batches` allowed while the
reads composing `GET /batches/{id}` views were denied.

## Files modified

- `backend/.../config/RoleScopeFilter.java` — extended developer `readOnly`:
  `GET /api/student-batches/(batch|student)/{id}` and `GET /api/attendance/**`
  (list, by-id, by-student, monthly-summary, by-date). Writes (`POST/PUT/PATCH/
  DELETE`) on attendance/assignments stay blocked, as do counts/summaries,
  invoices writes, and admin writes.
- `lib/students.ts`, `lib/batches.ts`, `lib/payments.ts`, `lib/invoices.ts`,
  `lib/instructors.ts`, `lib/branches.ts`, `lib/attendance.ts`,
  `lib/memberships.ts` — error fallback changed from the generic
  `The request could not be completed` to the actual response body, else
  `Request failed with status {status}.` (memberships keeps its JSON
  `{error,message}` parsing with the same status-aware fallback).

## Exact authorization changes

Before (developer): `GET` allowlist = collection + `/{id}` on 9 modules only.
After (developer): same + assignment subpath reads + attendance `GET` reads.
Everything else unchanged: still no create/update/delete, no revenue summaries,
no invoice/admin writes — DEVELOPER remains READ ONLY (plus feedback triage).

## Verification

- `npx tsc --noEmit` → no errors in touched libs.
- As DEVELOPER: open student, batch, membership, payment, invoice detail pages and
  Reminders — Network shows `200` on the previously rejected reads; write attempts
  still `403`.
- Trigger a failure (e.g. stop backend / bad id) → banner now shows the API body
  or `Request failed with status 403/404/...` instead of the generic message.
- Java edit mirrors existing regex conventions; run `./mvnw compile` (no JDK here).
