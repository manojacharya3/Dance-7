# Role-Based Dashboard Refinement

Scope: `app/dashboard/page.tsx` only. No backend changes. No new APIs.

## Role mapping

Role is taken from the authenticated user (`GET /api/auth/me` → `roles`):

| Role | Scope | Data scoping |
|------|-------|--------------|
| OWNER (or fallback) | `OWNER` | All branches, batches, students |
| BRANCH_HEAD | `BRANCH_HEAD` | Filtered to `user.branchId` (batches, students, branches); falls back to all when no branch assigned |
| INSTRUCTOR | `INSTRUCTOR` | Batches filtered to linked instructor profile (`user.instructorId` preferred, email/name fallback); students filtered to assigned batches |

## INSTRUCTOR dashboard

### Widgets removed
- Revenue stat and all financial metrics (`formatCurrency` revenue)
- Payments summary
- Invoices links
- Branch Analytics section
- Instructor Analytics section (cross-instructor + revenue)
- Membership Analytics (stat + sections)

### Widgets added
- My Students (count of students assigned to my batches)
- My Batches (count of my batches)
- Attendance Percentage (present % over my scoped records)
- Average Performance Score (`x.x / 10`)
- Attendance Pending (students with zero attendance records)
- Performance Reviews Pending (attendance records with no performance score)
- Instructor Reminders (pending counts + next batches + links to Open reminders / Take attendance)
- My Batches section (per-batch students/capacity/attendance/average, no revenue)

### API calls removed (INSTRUCTOR)
- `GET /api/memberships?...` — skipped (403 for instructors)
- `GET /api/payments?...` — skipped (403 for instructors)
- Still called: `branches`, `instructors`, `batches`, `students`, `attendance`, `student-batches/batch/{id}`, `auth/me`

## BRANCH_HEAD dashboard

Shows: Branch Revenue (paid sum, branch-scoped), Students, Batches, Memberships
(active count), Attendance %, Reminders (pending workload count), Branch Analytics
(own branch only + Open reminders link), Batch Analytics (branch batches, no revenue).
No invoices section. Memberships/payments APIs still called (allowed for branch heads).

## OWNER dashboard

Unchanged widgets plus two new sections built from already-fetched data:
- Membership Analytics (active / expired / other status breakdown + manage link)
- Payments & Invoices (revenue collected vs pending + links to payments/invoices)

## Failure prevention

- Role-aware fetch: instructor scope never requests memberships/payments, so the
  previous `403` → `Promise.all` → generic error path cannot trigger.
- Unlinked instructor still shows `No instructor profile has been linked to this user.`
- Branch head with no `branchId` falls back to unscoped data instead of an empty page.

## Verification

- `npx tsc --noEmit` → no errors in dashboard (2 pre-existing errors elsewhere).
- INSTRUCTOR login: no Revenue/Branch/Membership widgets, no membership/payment
  network calls, pending widgets + reminders visible.
- BRANCH_HEAD login: branch-scoped revenue/analytics, no invoices.
- OWNER login: full analytics + new membership/payments sections.
