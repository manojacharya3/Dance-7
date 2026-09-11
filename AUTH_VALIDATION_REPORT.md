# Authentication Validation Report — Dance7 (post-redesign)

Date: 2026-09-11 · Method: static review of `lib/auth.ts`,
`components/auth-guard.tsx`, `app/login|register/page.tsx`, `next.config.ts`,
plus repo history (`MOBILE_LOGIN_LOOP_ANALYSIS.md`, commits `496eae2`, `6ca1202`).
No auth code was changed by the redesign except auth-page presentation.

## Endpoint contract (unchanged)
| Endpoint | Use | Result |
|----------|-----|--------|
| `POST /api/auth/login` | `login(email, password)` | ✅ contract unchanged |
| `POST /api/auth/register` | `register(fullName, email, password, tenantId:"default")` | ✅ unchanged |
| `GET /api/auth/me` | `currentUser()`, `cache: "no-store"`, `credentials: "include"` | ✅ unchanged |
| `POST /api/auth/logout` | `logout()` → `router.replace("/login")` | ✅ unchanged |

## Findings
1. **Session persistence (refresh / navigation)** — ✅ PASS (static). Cookie-based
   session (`credentials: "include"`); `AuthGuard` revalidates via `/auth/me` on every
   protected pathname change; `?next=` redirect preserved on login. No token handling
   in JS, no `localStorage` session state to desync.
2. **Logout** — ✅ PASS (static). `POST /auth/logout` (failures swallowed) then
   hard redirect to `/login`; drawer + topbar both expose logout.
3. **Mobile login persistence** — ⚠️ CONDITIONAL. Known root cause documented in
   `MOBILE_LOGIN_LOOP_ANALYSIS.md`: cross-site frontend/API hosts made auth cookies
   third-party, blocked by iOS ITP / partitioned on Chrome Mobile (login 200 → `/me`
   401 → redirect loop). **Mitigation is already deployed in code**: `next.config.ts`
   rewrites `/api/:path*` to `BACKEND_INTERNAL_URL` (commit `496eae2`), making cookies
   first-party; `lib/auth.ts` defaults to relative `/api`. Residual deploy-time
   prerequisites (verify, do not code-change):
   - `BACKEND_INTERNAL_URL` set on the hosting platform (no trailing slash).
   - `NEXT_PUBLIC_API_URL` unset/relative at build time so the rewrite is used.
   - Backend `COOKIE_SECURE=true` + CORS allow-list incl. the frontend origin.
4. **Validation rules** — ✅ unchanged: email required, password `minLength 8`,
   server error messages surfaced in `role="alert"` banners.

## Required live verification (pilot, per `MOBILE_LOGIN_LOOP_ANALYSIS.md` §Verification)
- [ ] iPhone Safari (default settings): login → `/dashboard` persists after refresh;
      `Set-Cookie` not flagged blocked; `/auth/me` → 200 on refresh.
- [ ] Chrome Mobile repeat. Desktop regression check.
