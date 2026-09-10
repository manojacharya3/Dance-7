# Dance7 Missing Features and Repair Report

## Final Working URL Map

| Route | Status | Behavior |
|---|---|---|
| `/` | Working | Redirects to `/dashboard`; authentication guard then requires a session. |
| `/login` | Working | Calls `POST /api/auth/login`, stores HttpOnly cookies through the backend, and redirects to dashboard. |
| `/register` | Working | Calls `POST /api/auth/register` and redirects to dashboard. |
| `/dashboard` | Working | Loads live student data from `GET /api/students` and links to working student flows. |
| `/students` | Working | Authenticated list, search, pagination, detail/edit links, and soft delete. |
| `/students/new` | Working | Authenticated create form backed by `POST /api/students`. |
| `/students/:id` | Working | Authenticated detail page backed by `GET /api/students/:id`. |
| `/students/:id/edit` | Working | Authenticated edit form backed by `PUT /api/students/:id`. |

Unimplemented product areas are no longer exposed as navigation links: Projects, Calendar, Team, Reports, and Settings.

## Issues Found and Fixes Applied

| Issue found | Root cause | Fix applied | Files modified |
|---|---|---|---|
| Sidebar Settings link was a dead `#` anchor | No Settings page existed, but navigation exposed the link | Replaced the dead link with non-interactive workspace context | `components/sidebar.tsx` |
| Navbar Search button did nothing | Button had no handler or route | Converted it to a link to `/students` | `components/navbar.tsx` |
| Navbar Notifications button was placeholder UI | No notification API or page existed | Removed the non-functional control | `components/navbar.tsx` |
| Dashboard showed mock projects, fake activity, and fake metrics | Dashboard used hardcoded arrays and static values | Replaced with live student count, API status, recent students, and working links | `app/dashboard/page.tsx` |
| Dashboard “New project” was a dead action | Project module is not implemented | Replaced it with working “Add student” navigation | `app/dashboard/page.tsx` |
| Student delete failures became unhandled promise rejections | Delete handler had no catch path | Reused the visible list error state for delete failures | `app/students/page.tsx` |
| Documentation described old static dashboard/public student behavior | Auth and dashboard implementation had changed after the original guide | Audit artifacts document the current authenticated behavior and remaining risks | `REVIEW_REPORT.md`, `BUILD_VALIDATION.md`, `API_TESTING_GUIDE.md`, `DATABASE_REVIEW.md`, `MANUAL_TEST_PLAN.md`, `TESTING_GUIDE.md` |

## Audit Results

- Broken active links: none found after repair.
- Exposed routes returning guaranteed 404: none in the final URL map.
- Dead menu entries: removed.
- Mock dashboard data: removed from the rendered dashboard.
- Frontend API calls without backend support: none found for auth/health/students.
- Backend endpoints without a corresponding frontend flow: `/api/health` and `/api/auth/refresh` are infrastructure/session endpoints; all UI CRUD/auth endpoints are connected.
- TODO/FIXME markers: none found in active application source.
- TypeScript errors: none; `tsc --noEmit` passed.
- Backend compile/test: previously passed with `mvn clean test`; repeat validation should remain a CI gate.

## Remaining Incomplete or High-Risk Functionality

These were intentionally not implemented as new product features, but remain release blockers or future work:

1. Tenant identity is still accepted from client request data rather than derived from JWT.
2. Tenant and dance-class entities are not part of the Java model.
3. Authentication needs automated integration tests, especially refresh rotation and 403/401 behavior.
4. CSRF is disabled while authentication uses cookies; define a production cookie/CSRF policy.
5. The JWT fallback secret and database fallback credentials must not be used in production.
6. `ddl-auto=update` must be replaced by migrations.
7. Dashboard metrics are now live student metrics, but project/team/report modules remain outside the implemented scope.
8. Mobile sidebar navigation is not available below the `lg` breakpoint.

## Validation Evidence

- Frontend TypeScript: `npm.cmd exec -- tsc --noEmit` returned exit code 0.
- Changed frontend files: no editor diagnostics.
- Active source grep: no `href="#"`, TODO/FIXME, mock dashboard labels, or placeholder notification/settings controls.
- Backend endpoint inventory matches the API documentation in `API_TESTING_GUIDE.md`.
