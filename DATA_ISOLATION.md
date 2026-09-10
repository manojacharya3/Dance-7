# Data Isolation

## Role Restrictions

- `OWNER` and existing `ADMIN`: unrestricted access to all modules and branches.
- `BRANCH_HEAD`: assigned branch is stored on User and mismatched `branchId` request parameters are rejected with HTTP 403.
- `INSTRUCTOR`: assigned instructor is stored on User. Payment, Membership, Invoice, Instructor, and Administration API routes are rejected with HTTP 403. Instructor navigation is limited to Dashboard, Students, Attendance, and Batches.

## Controllers Updated

- `UserManagementController`: owner-only administration access.
- Existing module controllers remain protected by Spring Security and now pass through `RoleScopeFilter`.

## Services Updated

- `ScopeService`: resolves the authenticated User, role, branch, and instructor assignment.
- `UserService`: creates, edits, disables, and assigns managed-user roles/branch/instructor links.

## Security Changes

- `RoleScopeFilter` runs after JWT authentication.
- Instructor finance/admin/instructor endpoints return `403 Access denied`.
- Branch Head and Instructor requests with a branchId that does not match the authenticated assignment return `403`.
- Sidebar hides Administration for non-owners and hides finance/administration modules for instructors.

## Remaining Gaps

Existing list services still accept tenant and optional branch identifiers directly, and several list endpoints do not yet add authenticated branch predicates to their repository queries. Full row-level Branch Head filtering for every legacy list/dashboard query requires a follow-up service/repository pass. The current filter prevents explicit cross-branch parameter access and blocks instructor finance access, but does not silently rewrite unscoped requests to the user branch.
