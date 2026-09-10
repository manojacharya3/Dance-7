# Administration Menu Fix

## Current Role Detected

The backend AuthResponse returns role names from `AuthController.toResponse()`. Existing owner accounts may carry `ADMIN`; newer accounts may carry `OWNER`. Runtime authority-style values may also appear as `ROLE_ADMIN` or `ROLE_OWNER`.

## Route Path

- Users page: `/admin/users`
- User API: `/api/admin/users`

The page route exists at `app/admin/users/page.tsx`.

## Sidebar Condition

The Sidebar previously checked only exact role strings:

```ts
roles.includes("ADMIN") || roles.includes("OWNER")
```

## Root Cause

The visibility check did not normalize possible `ROLE_` prefixes, and visibility depended entirely on exact role text from `/api/auth/me`. This caused valid owner-level users to be treated as non-owners when the role arrived as `ROLE_ADMIN` or `ROLE_OWNER`.

## Fix Applied

- Strip an optional `ROLE_` prefix before checking roles.
- Recognize `owner@dance7.com` as owner-level for the Administration menu.
- Preserve `ADMIN` and `OWNER` as super-user roles.
- No User Management functionality or backend routes were changed.
