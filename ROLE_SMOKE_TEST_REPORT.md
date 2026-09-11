# Role-Based Smoke Test Report — Dance7 (post-redesign)

Date: 2026-09-11 · Method: **static code review** (no live backend reachable from this
environment). All flows below were traced through `components/sidebar.tsx`
(`visibleNavItems`), `components/auth-guard.tsx`, `app/dashboard/page.tsx` scoping,
`app/admin/users/page.tsx` gating, and `app/feedback/page.tsx` triage permissions.
Dynamic (live-browser) confirmation per role is still required during pilot — see §5.

Nav visibility matrix (`visibleNavItems`, preserved verbatim by the redesign):

| Page | OWNER¹ | DEVELOPER | BRANCH_HEAD | INSTRUCTOR |
|------|--------|-----------|-------------|------------|
| Dashboard | ✅ | ✅ | ✅ | ✅ |
| Students | ✅ | ✅ | ✅ | ✅ |
| Attendance | ✅ | ❌ (hidden) | ✅ | ✅ |
| Instructors | ✅ | ✅ | ✅ | ❌ (hidden) |
| Batches | ✅ | ✅ | ✅ | ✅ |
| Memberships | ✅ | ✅ | ✅ | ❌ (hidden) |
| Payments | ✅ | ✅ | ✅ | ❌ (hidden) |
| Invoices | ✅ | ✅ | ✅ | ❌ (hidden) |
| Reminders | ✅ | ✅ | ✅ | ❌ (hidden) |
| Feedback | ✅ | ✅ | ✅ | ✅ |
| Users (`/admin/users`) | ✅ | ✅ (read-only) | ❌ (hidden) | ❌ (hidden) |

¹ OWNER = `email == owner@dance7.com` OR role `ADMIN`/`OWNER`.

## Results by role

### OWNER — PASS (static)
Dashboard owner scope (all branches/batches/students, revenue = Σ PAID). Full nav.
Admin/Users create/edit/disable/remove available. No 401/403 expected with valid session;
backend is authoritative for data isolation.

### DEVELOPER — PASS (static)
Dashboard shows developer summary (branches count, open feedback, role, system health)
instead of studio analytics. Attendance hidden from nav; Users page forced
`readOnly` (no create button, row actions replaced with read-only indicator).
No write path available in UI.

### BRANCH_HEAD — PASS (static)
Dashboard scopes batches/students/payments/memberships to `user.branchId`.
Full nav except `/admin/users`. No cross-branch leakage in frontend filtering;
enforcement depends on backend (unchanged).

### INSTRUCTOR — PASS (static)
Dashboard resolves linked instructor profile (`user.instructorId`, fallback by
email/name match); shows "No instructor profile has been linked" error banner when
unlinked rather than silently empty data. Students/batches scoped to assigned batches.
Nav limited to Dashboard/Students/Attendance/Batches/Feedback. Payments, memberships,
invoices, reminders, users hidden.

## Issues found

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | No dedicated `/profile` or `/settings` routes exist in `app/` — "Profile/Settings" validation items have no target. Only student/instructor detail pages use the word "profile". | P2 (info) | Open — confirm whether these routes are planned or out of scope |
| 2 | Frontend has **no per-route role guards** — `AuthGuard` enforces authentication only. A logged-in INSTRUCTOR can manually visit hidden URLs (e.g. `/reminders`); the mobile bottom-nav "More" tab links there for every role. Data protection relies entirely on backend 401/403. | P1 | Open — pre-existing (not introduced by redesign); recommend backend verifies 403s during pilot, or add client-side role redirects as a follow-up |
| 3 | Bottom-nav "More" tab icon falls back to the Home icon for roles without `/reminders` in nav (e.g. INSTRUCTOR). Cosmetic only. | P2 | Open |

No broken links found (all nav `href`s resolve to existing `app/` routes).
No redesign-introduced 401/403 paths: all fetch helpers and endpoints unchanged.
No workspace visibility regression: branch/batch filters and workspace switcher
derive from the same `getBranches()` source as before.
