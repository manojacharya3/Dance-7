# Mobile Login Redirect Loop Analysis

## Symptom

Desktop login works. On mobile, login appears to succeed, then the app returns
to `/login` (loop). Page refresh / redirect to `/dashboard` does not stick.

## Cookie configuration (audited, `AuthController.addCookie`)

| Attribute | Value |
|-----------|-------|
| Names | `dance7_access_token` (15 min), `dance7_refresh_token` (7 days) |
| `HttpOnly` | `true` |
| `Secure` | from `COOKIE_SECURE` (prod must be `true`) |
| `SameSite` | `None` when secure, else `Lax` |
| `Domain` | unset (host-only, bound to the Railway backend host) |
| `Path` | `/` |
| Transport | `fetch(..., { credentials: "include" })` to `NEXT_PUBLIC_API_URL` |

## Root cause

The deployment is **cross-site**: frontend `https://dance7.vercel.app` vs API
`https://dance7-api-production.up.railway.app` (different registrable domains),
so both cookies are **third-party** from the browser's perspective.

- Desktop Chrome still accepts third-party cookies in most configurations →
  session persists → login works.
- Mobile Safari (default ON "Prevent Cross-Site Tracking", all iOS browsers use
  WebKit) blocks third-party `Set-Cookie` / sends nothing back → `POST
  /api/auth/login` returns 200 but the cookies never persist → the next call
  (`GET /api/auth/me`) is unauthenticated → 401 → app redirects to `/login`.
  Loop. Chrome Mobile (third-party-cookie phase-out/partitioning) behaves the
  same on recent versions.

So: login "succeeds" but the session cannot be stored on mobile. No backend
logic bug — pure cookie-partitioning behavior.

## Mobile compatibility assessment

| Browser | Third-party cookie auth | Same-site (recommended) |
|---------|------------------------|--------------------------|
| Mobile Safari / any iOS browser | ❌ blocked by ITP by default | ✅ first-party, unaffected |
| Chrome Mobile (recent) | ⚠️ deprecated/partitioned, unreliable | ✅ unaffected |
| Desktop Chrome | ✅ (for now) | ✅ |

## Minimum required change (recommended)

Eliminate third-party cookies: serve the API same-site through the frontend
origin via Vercel rewrites, so cookies become first-party and ITP-proof:

1. Vercel → this project → Settings → Rewrites (or `next.config.ts`,
   replacing the dev-only `localhost:8080` rule with an env-driven destination):
   source `/api/:path*` → destination
   `https://dance7-api-production.up.railway.app/api/:path*`.
2. Set frontend `NEXT_PUBLIC_API_URL` to empty/relative (`/api`) and redeploy
   (baked at build time) — `lib/auth.ts` already falls back to `"/api"`.
3. Keep backend `CORS_ALLOWED_ORIGINS=https://dance7.vercel.app`,
   `COOKIE_SECURE=true` (harmless same-site; `SameSite=None` still valid).
4. Alternative (larger, not recommended): return the JWT in the login response
   body and store it in app memory/`localStorage` — removes cookie dependence
   but adds XSS token-theft surface and a bigger auth rework.

## Verification (mobile)

1. iPhone Safari (default settings, no private mode): log in → lands on
   `/dashboard` and stays after refresh.
2. DevTools/Remote inspect: `POST /api/auth/login` → 200 with `Set-Cookie`
   (no yellow "blocked" warning); `GET /api/auth/me` → 200 on refresh.
3. Repeat on Chrome Mobile. Desktop behavior must remain unchanged.

No code was modified in this analysis.
