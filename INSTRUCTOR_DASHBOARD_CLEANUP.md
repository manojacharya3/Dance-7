# Instructor Dashboard Cleanup

Scope: `app/dashboard/page.tsx` only. No backend changes. No new APIs.

## Why financial cards still appeared

The page already had an INSTRUCTOR branch, but `scope` defaulted to `OWNER`
while `user` was `null` (before `GET /api/auth/me` resolved — or forever if
that call failed). First paint (and any auth failure) therefore rendered the
OWNER stat row: Students / Active Memberships / Revenue / Attendance.

## Role checks used

- `normalizedRoles()` strips any `ROLE_` prefix, then:
  `INSTRUCTOR` → `INSTRUCTOR`, else `BRANCH_HEAD` → `BRANCH_HEAD`, else `OWNER`.
- Data loading is role-aware: `instructorScope` (from the `/me` response) skips
  blocked calls before they are made.
- Render is auth-gated: while `authLoading` is true the page shows
  `Loading dashboard...` instead of the OWNER layout, so instructors never see
  a flash of financial widgets.

## Widgets removed (INSTRUCTOR)

- Revenue card (and every `formatCurrency` financial metric)
- Active Memberships card
- Payments summaries / pending counts
- Invoices links
- Branch Analytics section
- Branch Revenue Analytics / branch financial KPIs
- Membership Analytics / membership summaries
- Cross-instructor Instructor Analytics

## Widgets retained / added (INSTRUCTOR)

- My Students
- My Batches
- Attendance Percentage
- Average Performance Score (`x.x / 10`)
- Attendance Pending (students with zero attendance records)
- Performance Reviews Pending (records with no performance score)
- Upcoming Classes (my batches with schedule times; empty-state message if none)
- Instructor Reminders (pending counts + batch list + Open reminders / Take attendance)
- My Batches analytics (students / capacity / attendance / average, no revenue)

## API calls removed (INSTRUCTOR)

- `GET /api/memberships?...` — never requested (403 for instructors)
- `GET /api/payments?...` — never requested (403 for instructors)
- Invoice API — never requested (no invoice call existed; none added)
- Only used: `auth/me`, `branches`, `instructors`, `batches`, `students`,
  `attendance`, `student-batches/batch/{id}` (performance derived from attendance)

## Verification

- `npx tsc --noEmit` → no errors in dashboard.
- Hard-refresh as INSTRUCTOR: brief `Loading dashboard...`, then instructor-only
  widgets; Network shows no membership/payment/invoice requests.
- OWNER / BRANCH_HEAD layouts unchanged (after the loading gate resolves).
