# Instructor Membership / Payment Access

Scope: instructor permissions + `app/dashboard/page.tsx`. No other modules changed.

## Security model

Instructor → Assigned Batches → Assigned Students. Enforced server-side:

- `ScopeService.assignedStudentIds(tenantId)` (`backend/.../config/ScopeService.java`)
  resolves the caller's `users.instructor_id` → active batches
  (`BatchRepository.findByTenantIdAndInstructorIdAndActiveTrue`) → active
  student assignments (`StudentBatchRepository`). Returns `null` for non-instructors
  (no restriction), empty set when unlinked/nothing assigned.
- `MembershipServiceImpl.list` / `PaymentServiceImpl.list` return `Page.empty()`
  for unassigned instructors, otherwise query only
  `findByTenantIdAndStudentIdInAndActiveTrue` (new repository methods) — records
  of other instructors' students are never returned.
- `get(id)` in both services throws `403 Access denied for this student` when the
  record's `studentId` is outside the assigned set.
- `RoleScopeFilter` allows instructors only `GET /api/memberships`,
  `GET /api/payments`, and `GET /{id}` on those paths. Writes, `/count/*`,
  `/summary/*` (revenue aggregates), `/api/invoices/*`, and `/api/admin/*` stay
  `403`. So revenue, branch revenue, invoice management, and financial analytics
  remain unreachable for instructors.

## Allowed visibility (assigned students only)

- Memberships: status, expiry (`endDate`), active/inactive state, plan.
- Payments: per-student status, pending payments, outstanding dues amounts.
- Reminders inputs: expiring ≤30d, pending, overdue — the reminders page calls the
  same scoped list endpoints, so it now loads for instructors with assigned-only data.

## Kept hidden

Revenue card, branch revenue, invoice management, financial analytics
(`/summary/*` blocked), branch analytics section, cross-instructor data.

## Instructor dashboard (`app/dashboard/page.tsx`)

- Now always fetches memberships/payments (previously skipped); responses are
  server-scoped, so no 403 failures.
- Row 1: My Students, My Batches, Attendance %, Average Performance Score.
- Row 2 (new): Active Memberships, Pending Payments, Memberships Expiring (30d),
  Overdue Payments.
- Row 3 (retained): Attendance Pending, Performance Reviews Pending.
- Reminders section (extended via `instructorFollowUpItems`): expiring memberships
  with student + plan + expiry, pending dues with student + amount and outstanding
  total, overdue with days count — plus the existing attendance/review follow-ups.
- Retained: Upcoming Classes, My Batches (no revenue figures anywhere).

## Verification

- `npx tsc --noEmit` → no errors in dashboard.
- As INSTRUCTOR with assignments: dashboard shows follow-up cards; Network:
  `GET /api/memberships` + `GET /api/payments` → `200` with only assigned
  students' records; `POST` → `403`; `/summary/total-revenue` → `403`.
- As INSTRUCTOR with no link: scoped APIs return empty pages; banner keeps
  `No instructor profile has been linked to this user.`
- OWNER / BRANCH_HEAD flows unchanged (unrestricted lists).
