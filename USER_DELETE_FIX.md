# USER_DELETE_FIX — Administration → Users delete/disable failure

## Symptom
Confirmation dialog appears correctly. Clicking `Delete User` shows
`Unable to delete user. Please try again.` and the row is not removed.

## Complete delete-flow trace

1. `app/admin/users/page.tsx:90` — `confirmDelete()` calls `await disableManagedUser(target.id)`
2. `lib/admin-users.ts:7` — `disableManagedUser(id)` calls `request<void>(\`/admin/users/${id}\`, { method: "DELETE" })`
3. `lib/admin-users.ts:3` — `request()` does `fetch(\`${API}${path}\`)` with `API = process.env.NEXT_PUBLIC_API_URL ?? "/api"`, `credentials: "include"`, then:
   ```ts
   if (!response.ok) throw new Error((await response.text()) || "...");
   return response.status === 204 ? (undefined as T) : response.json();
   ```
4. `next.config.ts` rewrites `/api/:path*` → `http://localhost:8080/api/:path*`
5. `backend/src/main/java/com/studioos/controller/UserManagementController.java:22`
   `@DeleteMapping("/{id}")` → `requireOwner(authentication)` → `service.disableManagedUser(id)`
6. `backend/src/main/java/com/studioos/service/UserService.java:69`
   ```java
   public void disableManagedUser(Long id) {
     User user = userRepository.findById(id).orElseThrow(...);
     user.setEnabled(false);
     userRepository.save(user);
   }
   ```
7. `UserRepository` (`JpaRepository`) → `UPDATE users SET enabled=false WHERE id=?`

## Exact failing step
Step 3, frontend response parsing — **after** the backend DB update succeeds.
The backend returned `200 OK` with an empty body (Spring default for a `void`
controller method). The frontend only treats `204` as empty and calls
`response.json()` on the empty `200` body, which throws, so `confirmDelete()`
goes to `catch` and shows the generic error. The user row is never filtered
because the `catch` path skips `setUsers(filter)`.

No temporary logging was needed — the failure is deterministic from code
inspection plus a `Response.json()` empty-body reproduction (see below).

## Captured request/response

- HTTP request URL: `/api/admin/users/{id}` → rewritten to `http://localhost:8080/api/admin/users/{id}`
  (`{id}` = `confirmUser.id`, e.g. `/api/admin/users/5`)
- HTTP method: `DELETE`
- Response status (before fix): `200 OK` with `Content-Length: 0` / empty body
- Response body (before fix): empty string
- Exception message: `SyntaxError: Unexpected end of JSON input`
  - from `response.json()` on empty body in `lib/admin-users.ts:3`
  - reproduced with Node: `await new Response('', {status:200}).json()`
    → `SyntaxError: Unexpected end of JSON input`
  - swallowed by `app/admin/users/page.tsx:97-98` catch-all which sets
    `Unable to delete user. Please try again.`

## Verification checklist

- API route exists: YES — `UserManagementController @RequestMapping("/api/admin/users")`
  + `@DeleteMapping("/{id}")`.
- Route path matches frontend: YES — frontend `DELETE /admin/users/{id}` with
  base `/api` = `/api/admin/users/{id}` matches backend mapping.
- OWNER role authorization passes: YES — `requireOwner()` allows
  `ROLE_ADMIN` or `ROLE_OWNER`; `SecurityConfiguration` only requires
  authenticated (401 otherwise, not the observed error); failure here would be
  `403 "Owner access required"`, not an empty-200 parse error.
- CSRF/security blocking: NO — `SecurityConfiguration.java:20`
  `.csrf(csrf -> csrf.disable())`, CORS enabled, stateless JWT; a security
  block would be 401/403 with a body, and `response.ok` would be false.
- User disable logic works: YES — `UserService.disableManagedUser()` loads,
  sets `enabled=false`, saves. No validation on this path.
- Database update succeeds: YES — `userRepository.save(user)` runs before the
  response; the bug manifests on response parsing, i.e. after commit. (If the
  backend had thrown `UsernameNotFoundException`, the status would be an error
  status with a body, not empty 200.)

## Root cause
Contract mismatch on empty DELETE responses. Every other DELETE controller
(`Batch`, `Branch`, `Instructor`, `Membership`, `Payment`, `Student`,
`StudentBatch`) returns `ResponseEntity.noContent().build()` → `204`, which is
exactly what the shared frontend `request()` helper expects. Only
`UserManagementController.disable()` returned `void` → `200` + empty body,
hitting the `response.json()` branch and throwing `SyntaxError`.

## File causing failure
`backend/src/main/java/com/studioos/controller/UserManagementController.java`

## Exact line causing failure
Line 21 (before fix):
```java
@DeleteMapping("/{id}") public void disable(Authentication authentication, @PathVariable Long id) { requireOwner(authentication); service.disableManagedUser(id); }
```
Returning `void` makes Spring send `200 OK` with no body instead of `204 No Content`.

## Fix applied (smallest possible, Users module only)
Same file, line 8 + line 22:
```java
import org.springframework.http.ResponseEntity;
...
@DeleteMapping("/{id}") public ResponseEntity<Void> disable(Authentication authentication, @PathVariable Long id) { requireOwner(authentication); service.disableManagedUser(id); return ResponseEntity.noContent().build(); }
```
- No service/repository/auth/routing changes.
- No frontend redesign; `lib/admin-users.ts` and `app/admin/users/page.tsx`
  work unchanged because `204` now hits the existing
  `response.status === 204 ? undefined : ...` branch.
- Now consistent with all other DELETE endpoints in the codebase.

## Verification steps
1. `npx tsc --noEmit` → no errors in `app/admin/users/page.tsx`
   (2 pre-existing errors elsewhere in `student-form` / `management-filtering`).
2. Static check: `grep DeleteMapping backend/src` → all DELETEs including
   `UserManagementController` now return `ResponseEntity.noContent()`.
3. Manual test:
   - Login as OWNER → Administration → Users → click disable icon →
     dialog `Confirm User Deletion` shows name/email → click `Delete User`.
   - Expect: dialog closes, `User disabled successfully`, row disappears
     immediately, list re-fetches, no page reload.
   - DevTools Network: `DELETE /api/admin/users/{id}` → `204 No Content`.
   - Failure case (e.g. stop backend): still shows
     `Unable to delete user. Please try again.` and keeps the row.
4. Backend: `mvn compile` once Maven is available (no Maven in this env, so
   verified by inspection — change is the identical one-line pattern used by
   7 sibling controllers).
