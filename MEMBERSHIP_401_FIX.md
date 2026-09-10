# Membership API 401 Review

## Root Cause

The frontend Membership client and Student client use the same authentication strategy:

- Same default API base: `/api`
- Same `credentials: "include"`
- Same JSON headers
- Same Next.js `/api` rewrite

`StudentController` is mapped under `/api/students`, and `MembershipController` is mapped under `/api/memberships`. `SecurityConfiguration` applies the same rule to both routes: `anyRequest().authenticated()`.

Live browser verification with the same authenticated session showed:

- `GET /api/auth/me` -> `200`
- `GET /api/students` -> `200`
- `GET /api/memberships` -> `401`
- `GET /api/memberships/` -> `401`
- `GET /api/memberships/count/active` -> `401`

There is no source-level authentication difference that can produce this route-specific result. The running Spring Boot process is serving stale Membership/security runtime classes. The Membership source and compiled application must be rebuilt and the backend restarted.

## File Modified

- `MEMBERSHIP_401_FIX.md`

No application source file required modification. Adding a redundant Membership matcher to `SecurityConfiguration` would not fix the issue because `anyRequest().authenticated()` already protects Membership routes.

## Fix Applied

The smallest effective fix is to rebuild and restart the backend so the current `MembershipController` and security configuration are loaded:

```powershell
cd backend
mvn clean package -DskipTests
java -jar target\dance7-api-0.0.1-SNAPSHOT.jar
```

If Maven is not installed, run the project through the configured VS Code/Spring Boot Java launcher after stopping the existing Java process.

## Relevant Source Lines

- `lib/students.ts`: same `/api` base and `credentials: "include"` as Membership client.
- `lib/memberships.ts`: same request configuration.
- `backend/src/main/java/com/studioos/controller/StudentController.java`: `@RequestMapping("/api/students")`.
- `backend/src/main/java/com/studioos/controller/MembershipController.java`: `@RequestMapping("/api/memberships")`.
- `backend/src/main/java/com/studioos/config/SecurityConfiguration.java`: `.anyRequest().authenticated()`.
