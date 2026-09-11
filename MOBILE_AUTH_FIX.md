# Mobile Auth Fix — Vercel Rewrite for First-Party Cookies

## What changed

`next.config.ts` only — the rewrite destination is now env-driven. No
`vercel.json` existed and none was added (a second rewrite source would
conflict; `next.config.ts` rewrites deploy to Vercel automatically). No backend,
auth-flow, cookie, or API-contract changes.

## Rewrite configuration

```ts
// next.config.ts
const backendOrigin = (process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080").replace(/\/$/, "");
rewrites: [{ source: "/api/:path*", destination: `${backendOrigin}/api/:path*` }]
```

- Vercel: set `BACKEND_INTERNAL_URL=https://dance7-api-production.up.railway.app`
  (no trailing slash) and **remove** `NEXT_PUBLIC_API_URL` so the browser uses
  relative `/api` (all 11 `lib/*.ts` clients already fall back to `"/api"`).
- Local dev: variable unset → `http://localhost:8080`, behavior unchanged.

⚠️ Correction to the requested destination: it must be
`https://<backend>/api/:path*`, **not** `https://<backend>/:path*`. The
`:path*` capture excludes the `/api` prefix, and every backend controller is
mapped under `/api/*` — dropping it would turn every call into a 401 on an
unmapped path (the exact failure being fixed).

## API URL changes

- Code: none needed — no hardcoded Railway URLs exist; all clients use
  `NEXT_PUBLIC_API_URL ?? "/api"`.
- Config: delete `NEXT_PUBLIC_API_URL` on Vercel (or set it to `/api`);
  add `BACKEND_INTERNAL_URL` (build-time, so redeploy after setting).
- Result: zero browser requests hit Railway directly; all go through `/api` on
  the Vercel origin → cookies are first-party → mobile ITP/partitioning no
  longer applies. Auth flow, cookie flags, and contracts untouched.

## Mobile compatibility verification

- Logic: same-site `Set-Cookie` cannot be blocked as third-party on Mobile
  Safari, Chrome Mobile, or desktop — the loop condition is removed by
  construction. No physical-device run available in this environment.
- After Vercel redeploy, confirm on each browser: login → `/dashboard` sticks
  across refresh; Network shows `POST /api/auth/login` → 200 (same origin) and
  `GET /api/auth/me` → 200; no `up.railway.app` requests from the page.

## Build validation result

- `npm run build` → success, all routes compiled, no type errors.

## Commit / push

- Commit: `Use Vercel rewrite for first-party auth cookies` (hash below after push).
- Push: `origin/main`.
