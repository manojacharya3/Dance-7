# User Management Completion

## Routes Added

- `GET /api/admin/users`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`
- `/admin/users`

The edit flow supports password reset by submitting a new password.

## Sidebar Changes

- Added `Administration / Users` navigation.
- Visibility accepts both `OWNER` and existing `ADMIN` roles, so `owner@dance7.com` remains recognized as an owner-level account.
- Instructor navigation is limited to dashboard, students, attendance, and batches.

## Role Checks

- User management endpoints require `ROLE_ADMIN` or `ROLE_OWNER`.
- `BRANCH_HEAD` and `INSTRUCTOR` receive HTTP 403 for administration endpoints.
- The user form supports `OWNER`, `BRANCH_HEAD`, and `INSTRUCTOR`.

## User Creation Flow

Owner enters:

- Full name
- Email
- Password
- Role
- Branch
- Optional Instructor link

The backend hashes passwords, assigns the selected role, stores branch/instructor assignments, validates Branch Head branch requirements, and validates Instructor branch/instructor requirements. Existing users can be edited or disabled.

## Test Accounts Created

No test accounts were created automatically. The current account `owner@dance7.com` is accepted as an owner-level user when it has the existing `ADMIN` role.

## Remaining Scope Gap

Full row-level branch filtering for every legacy module controller still requires a broader service/repository pass. The administration workflow, role checks, assignments, navigation visibility, and owner access are complete.
