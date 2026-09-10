# DEVELOPER Role

## Permissions

Enforced server-side in `RoleScopeFilter` (read-only + feedback triage) and
service-layer checks; UI additionally hides unavailable actions.

Allow:
- Dashboard (dedicated developer view), Feedback Center, Reminders page.
- Read-only `GET` (list + by-id) on: Students, Instructors, Batches, Memberships,
  Payments, Invoices, Branches, Assignments (`/api/student-batches`).
- Administration: `GET /api/admin/users` (view users, roles, assignments). Writes
  (`POST/PUT/DELETE`) on admin users stay OWNER-only (`requireOwner`); only the
  list endpoint accepts `ROLE_DEVELOPER` (`requireOwnerOrDeveloper`).
- Feedback: view all (no branch restriction, like OWNER), filter, search,
  `POST /api/feedback`, `PATCH /{id}/status`, `PATCH /{id}/notes`.

Role plumbing: `Role.Name.DEVELOPER` (`model/Role.java`), DB check constraint
extended (`BranchSchemaMigration`), `ScopeService.developer()` helper, JWT/`/me`
roles flow through automatically.

## Restrictions (return 403)

- Create / Edit / Delete User, Change Roles (admin UI hides Create + row actions
  for developers; role shown read-only; backend rejects writes).
- Record Payments, Delete Memberships, Delete Invoices (no UI hiding added —
  backend rejects; error surfaces in existing banners).
- Attendance APIs, revenue/count/summary aggregates, invoice writes, admin writes.

## Dashboard access (`app/dashboard/page.tsx`)

Role check order: INSTRUCTOR → BRANCH_HEAD → DEVELOPER → OWNER (prefix-tolerant).
Developers fetch only branches + feedback list (no student/financial calls) and see:
- Open Tickets, In Progress Tickets, Resolved Tickets, Total Tickets.
- Recent Feedback (title, category, priority, branch, reporter, status) + link to
  Feedback Center. No revenue, branch, membership, or payment widgets.

## Feedback workflow

Statuses `OPEN → IN_PROGRESS → RESOLVED` (existing module, reused).
- `FeedbackService.updateStatus`: now allowed for OWNER, DEVELOPER, or submitter
  (was OWNER/submitter only).
- Internal notes: new nullable `internal_notes` column (`Feedback` entity, auto-added
  by `ddl-auto: update`), exposed in `FeedbackDTO`, writable via
  `PATCH /api/feedback/{id}/notes` (OWNER or DEVELOPER only).
- UI (`app/feedback/page.tsx`, existing search/filter/status buttons reused):
  notes shown under each ticket; OWNER/DEVELOPER get textarea + Save.
- Sidebar: developers see Dashboard, Students, Instructors, Batches, Memberships,
  Payments, Invoices, Reminders, Feedback, Administration/Users (attendance hidden).

## Default account (`DeveloperAccountInitializer`, order 3)

Created on startup if missing: `Dance7 Developer / developer@dance7.com /
Test@123` (override via `app.security.developer-password`), `DEVELOPER` role,
`enabled=true`, tenant `default`, no branch. Existing account without the role is
repaired (role added, re-enabled).

## Verification

- `npx tsc --noEmit` → no errors in touched frontend modules.
- Restart backend → login `developer@dance7.com / Test@123` → developer dashboard
  with ticket counts; feedback status/notes save; `POST /api/payments` → 403;
  `GET /api/admin/users` → 200 without write buttons in UI.
- Java changes follow existing conventions but need `./mvnw compile` (no JDK here).
