# Authentication 401 Fix

## Scope

Review limited to frontend authentication for API requests.

## Root Cause

The frontend clients called the backend directly at `http://localhost:8080/api`. The browser request trace showed the first failing request was:

```text
GET http://localhost:8080/api/memberships?tenantId=default&page=0&size=100
```

It contained no `Cookie` header. The same boundary affected `/api/auth/me`, `/api/batches`, and `/api/instructors`.

The frontend did include `credentials: "include"`, and the backend/frontend cookie names matched:

- `dance7_access_token`
- `dance7_refresh_token`

The problem was cross-origin cookie forwarding, not a missing `credentials` option or a module-specific API client.

## Exact Request Losing Authentication

`/api/auth/me` is the first authentication check used by `AuthGuard` on protected routes. The browser trace also confirmed the direct module request to:

```text
http://localhost:8080/api/memberships
```

was sent without a cookie.

Additionally, `/memberships` was missing from the AuthGuard protected-path list, so it could issue API requests without first checking the session.

## Fix Applied

- Added a Next.js same-origin rewrite from `/api/:path*` to `http://localhost:8080/api/:path*`.
- Changed frontend API clients to default to relative `/api` URLs.
- Preserved `credentials: "include"` in all clients.
- Added `/memberships` to `AuthGuard` protected paths.

## Affected Files

- `next.config.ts`
- `components/auth-guard.tsx`
- `lib/auth.ts`
- `lib/attendance.ts`
- `lib/batches.ts`
- `lib/instructors.ts`
- `lib/memberships.ts`
- `lib/students.ts`

## Verification

- Backend cookie names are `dance7_access_token` and `dance7_refresh_token`.
- Frontend login and `currentUser()` use `credentials: "include"`.
- Protected API clients use `credentials: "include"`.
- TypeScript/editor diagnostics pass for the changed authentication files.

Restart the Next.js development server after this change so it reloads `next.config.ts` and activates the rewrite.
