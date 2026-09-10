# Role Management

## Roles

- `OWNER`: full access to all modules and branches. Existing `ADMIN` accounts are accepted as owner-equivalent for administration access.
- `BRANCH_HEAD`: intended for one assigned branch.
- `INSTRUCTOR`: intended for one linked Instructor record and assigned batches/students.

## Permissions

- Owner users can list, create, edit, disable, assign roles, assign branches, link instructors, and reset passwords through `/api/admin/users`.
- Branch heads and instructors do not receive administration access.
- Existing authentication, JWT cookies, and Spring Security user loading remain unchanged.

## Branch Restrictions

User records now store nullable `branchId` and `instructorId` values. These assignments are available to the dashboard and administration workflow.

The current application controllers still accept tenant/query identifiers directly, so complete branch-level enforcement across every existing module remains a follow-up gap. The user-management API itself is owner-protected.

## User Creation Workflow

1. An owner opens `Administration / Users`.
2. The owner enters full name, email, and password.
3. The owner assigns `OWNER`, `BRANCH_HEAD`, or `INSTRUCTOR`.
4. The owner optionally assigns a branch and instructor link.
5. The backend hashes the password and replaces the user role assignment.
6. Existing users can be edited or disabled.

## Files Added or Modified

- `backend/src/main/java/com/studioos/model/Role.java`
- `backend/src/main/java/com/studioos/model/User.java`
- `backend/src/main/java/com/studioos/repository/UserRepository.java`
- `backend/src/main/java/com/studioos/service/UserService.java`
- `backend/src/main/java/com/studioos/dto/UserManagementDTO.java`
- `backend/src/main/java/com/studioos/controller/UserManagementController.java`
- `lib/admin-users.ts`
- `components/sidebar.tsx`
- `components/auth-guard.tsx`
- `app/admin/users/page.tsx`
