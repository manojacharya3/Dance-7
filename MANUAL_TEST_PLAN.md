# Dance7 Manual Test Plan

## Environment

- PostgreSQL running on 5432 with `studioos` database.
- Backend on 8080.
- Frontend on 3000.
- A registered Dance7 account with valid cookies.

## Dashboard

- Open `/dashboard`; expect summary cards, project list, and activity feed without runtime errors.
- Verify Dance7 branding and sidebar links.
- Verify Students navigation opens `/students`.
- Note: widgets/charts are static sample data; there are no chart APIs.

## Student List

- Load `/students`; expect active rows or empty state.
- Search by first name, last name, full name, email, and phone.
- Move forward/back across pages; confirm first/last button states.
- Verify empty search results and backend error state.
- Verify unauthenticated access redirects to login and API access is denied.

## Student Details

- Open a valid `/students/{id}`; verify profile, contact, emergency, and care sections.
- Open an unknown ID; expect visible error and no infinite spinner.
- Verify edit link opens populated form.

## Student Create

- Submit valid required fields; expect `201` and redirect to details.
- Submit missing first name; browser/API validation must reject.
- Submit missing last name; browser/API validation must reject.
- Submit malformed email; expect validation failure.
- Stop backend and submit; expect actionable API-unavailable error and retained form values.

## Student Edit

- Edit name/contact/style; expect `200`, new values, and updated timestamp.
- Submit invalid email; expect rejection.
- Edit with wrong tenant; current implementation is a security test failure because tenant comes from request data.

## Student Delete

- Cancel confirmation; row remains.
- Confirm deletion; expect row removed from active list.
- Query database; expect row remains with `active=false`.
- Get/search deleted ID; expect not found.
- Delete unknown ID; expect `404`.

## Authentication

- Register unique email; expect 201 and HttpOnly cookies.
- Login valid credentials; expect 200 and rotated cookies.
- Login invalid credentials; expect denial without session.
- Open protected route without cookie; expect redirect to login.
- Call `/auth/me` without cookie; expect 401.
- Refresh valid cookie; expect 200 and token rotation.
- Reuse old refresh token; expect 401.
- Logout; expect 204 and protected API denial afterward.
- Inspect cookies: HttpOnly must be true; Secure must be true in HTTPS production.

