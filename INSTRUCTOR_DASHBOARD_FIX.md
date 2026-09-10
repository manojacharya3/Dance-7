# Instructor Dashboard Fix

## Dashboard initialization trace (`app/dashboard/page.tsx`)

1. `currentUser()` → `GET /api/auth/me` (role, email, name)
2. In parallel via `Promise.all`:
   - `getBranches()` → `GET /api/branches?tenantId=default`
   - `getInstructors()` → `GET /api/instructors?tenantId=default&page=0&size=100`
   - `getBatches("", 0, 100)` → `GET /api/batches?...`
   - `getStudents("", 0, 100)` → `GET /api/students?...`
   - `getAttendance()` → `GET /api/attendance?tenantId=default&page=0&size=100`
   - `getMemberships("", 0, 100)` → `GET /api/memberships?...`
   - `getPayments("", 0, 100)` → `GET /api/payments?...`
3. Then `getBatchStudents(batchId)` per batch → `GET /api/student-batches/batch/{id}`
4. Any single rejection → `catch` shows one banner; before the fix the banner was
   the generic `The request could not be completed.`

## Authenticated user verification

- Role: from `AuthResponse.roles` (`INSTRUCTOR` drives `scope === "INSTRUCTOR"`).
- `branchId`: stored on `users.branch_id`, was **not** exposed via `/api/auth/me`.
- `instructorId`: stored on `users.instructor_id` (set in Administration → Users),
  was **not** exposed via `/api/auth/me` either — the dashboard could only guess
  the profile by email/name match against the instructors list.

## Failing API

`GET /api/instructors?tenantId=default&page=0&size=100` (and also
`GET /api/memberships?...` and `GET /api/payments?...` for the same reason):

- Status code: `403 Forbidden`
- Exception: `RoleScopeFilter.doFilterInternal` → `response.sendError(403,
  "Instructor access is limited to ...")` (`backend/.../config/RoleScopeFilter.java:17`)
- The old dashboard fired all 7 requests unconditionally in one `Promise.all`,
  so one `403` failed the whole dashboard. Depending on the error body, the
  `request()` helpers (`lib/*.ts`) fell back to `The request could not be completed.`
- Secondary defect: instructor resolution used only an email/name heuristic, so a
  user with no linked instructor record silently fell back to **all** batches
  (`scopedBatches = ... && instructor ? filter : batches`) with no explanation.

## Root cause

Two compounding defects, both in the Instructor Dashboard scope only:

1. `RoleScopeFilter` blocked `INSTRUCTOR` from `GET /api/instructors`, the very
   list the dashboard needs to resolve the instructor profile — plus blocked
   `/api/memberships` and `/api/payments`, which the dashboard also fetched
   unconditionally.
2. `/api/auth/me` (`AuthResponse`) omitted `branchId`/`instructorId`, so the
   frontend could not verify the `users.instructor_id` mapping and had no
   graceful path for a missing link.

## Fix applied (smallest possible)

Backend (3 tiny additive changes, no behavior change for other roles):

- `backend/.../config/RoleScopeFilter.java:17` — removed `/api/instructors`
  from the instructor-blocked paths (payments, memberships, invoices, admin stay
  blocked). Instructors can now read the instructors list for dashboard scoping.
- `backend/.../dto/AuthResponse.java` — added `branchId`, `instructorId`.
- `backend/.../controller/AuthController.java:toResponse` — populates them from
  `User.getBranchId()/getInstructorId()` (covers login, refresh, `/me`).

Frontend (dashboard only + `AuthUser` type):

- `lib/auth.ts` — `AuthUser` gains optional `branchId`/`instructorId`.
- `app/dashboard/page.tsx` — loads `currentUser()` first, then role-aware fetch:
  `INSTRUCTOR` scope skips the still-blocked memberships/payments calls (empty
  defaults) instead of failing the whole page.
- Instructor resolution prefers `user.instructorId` (exact link), falls back to
  email/name match.
- Missing mapping is handled gracefully: sets
  `No instructor profile has been linked to this user.` instead of the generic
  `The request could not be completed.`

## Verification

- `npx tsc --noEmit` → no errors in dashboard/auth module (only 2 pre-existing
  errors elsewhere: `student-form`, `management-filtering`).
- Manual: log in as INSTRUCTOR with linked `instructorId` → dashboard loads,
  Network shows `GET /api/instructors` → `200`, no memberships/payments calls,
  stats scoped to assigned batches.
- Manual: log in as INSTRUCTOR with `instructorId = null` and no email/name
  match → banner shows `No instructor profile has been linked to this user.`
- OWNER/BRANCH_HEAD flows unchanged (still fetch all 7 APIs).
