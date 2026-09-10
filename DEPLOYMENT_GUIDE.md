# Dance7 Deployment Guide

Stack: Next.js 15 frontend (Vercel) + Spring Boot 3 / Java 21 API (Render) +
PostgreSQL (Neon). Auth: JWT access cookie (15 min) + rotating refresh cookie
(7 days), stateless sessions. No business logic was changed for deployment —
only CORS origins, cookie flags, and env bindings.

## 1. Neon PostgreSQL setup

1. Create a project at https://neon.tech (region closest to Render region).
2. Create database `studioos` (or keep `neondb` and use its name in the URL).
3. Copy the pooled connection string; convert to JDBC form:
   `jdbc:postgresql://<host>-pooler.<region>.aws.neon.tech/<db>?sslmode=require`
4. Schema is created automatically (`ddl-auto: update`); seed branches are
   inserted by `BranchSchemaMigration`; OWNER/DEVELOPER accounts self-heal via
   startup initializers.
5. Store as Render env vars (see §4). Never commit credentials.

## 2. Render backend setup (`dance7-api`)

Option A — Blueprint: repo contains `render.yaml` (rootDir `backend`,
`mvn -DskipTests package`, `java -jar target/*.jar`,
health check `/actuator/health`, `JAVA_VERSION=21`). New → Blueprint → select
repo → fill the `sync: false` vars below.

Option B — Manual Web Service: Runtime Java, Root Directory `backend`,
Build Command `mvn -DskipTests package`, Start Command `java -jar target/*.jar`,
Health Check Path `/actuator/health`, instance Starter or higher (512 MB JVM).

## 3. Vercel frontend setup

1. Import repo `manojacharya3/Dance-7`, Framework Next.js, root `./`.
2. Build `npm run build`. No `next.config.ts` change needed: with
   `NEXT_PUBLIC_API_URL` set, all API calls go directly to Render; local
   `/api → localhost:8080` rewrites only apply to relative calls (none in prod).
3. Set env var (Production + Preview as needed):
   `NEXT_PUBLIC_API_URL=https://dance7-api.onrender.com/api`
4. Redeploy after changing env vars (they are baked at build time).

## 4. Environment variables

Backend (Render):

| Var | Required | Value |
|-----|----------|-------|
| `DATABASE_URL` | yes | Neon JDBC URL with `?sslmode=require` |
| `DATABASE_USERNAME` | yes | Neon user |
| `DATABASE_PASSWORD` | yes (secret) | Neon password |
| `JWT_SECRET` | yes (secret) | Base64 ≥256-bit key; generate fresh (`openssl rand -base64 48`). Dev default in yml must NOT be used in prod |
| `CORS_ALLOWED_ORIGINS` | yes | `https://<your-app>.vercel.app` (comma-separated if more) |
| `COOKIE_SECURE` | yes (prod) | `true` → `Secure + SameSite=None` so cross-site cookies work; keep `false` locally |
| `PORT` | auto | Injected by Render; `server.port` already binds `${PORT:8080}` |
| `ACCESS_TOKEN_MINUTES` / `REFRESH_TOKEN_DAYS` | no | Defaults 15 / 7 |
| Recovery passwords | no | Change after first login |

Frontend (Vercel):

| Var | Required | Value |
|-----|----------|-------|
| `NEXT_PUBLIC_API_URL` | yes | `https://dance7-api.onrender.com/api` |

## 5. Production verification

- `GET https://dance7-api.onrender.com/actuator/health` → `200 {"status":"UP"}`.
- Login as OWNER on the Vercel URL; open DevTools → login `POST` returns
  `Set-Cookie ... Secure; SameSite=None`; dashboard loads (CORS `200`, no
  `Invalid CORS request`).
- JWT rotation: `JWT_SECRET` must differ from the dev default; rotating it
  invalidates existing sessions (users re-login).
- First-run: OWNER (`owner@dance7.com`) and DEVELOPER accounts are created by
  initializers — set new passwords immediately.

## 6. Production URLs (fill after deploy)

- Frontend: `https://<your-app>.vercel.app`
- Backend: `https://dance7-api.onrender.com`
- Database: Neon project dashboard (no public URL; use pooled connection string)

## 7. Rollback steps

- Vercel: Deployments → previous production deployment → Promote to Production
  (instant; env vars unchanged).
- Render: Deploys → Redeploy last known-good commit, or
  `git revert <sha> && git push origin main` (auto-redeploy). Keep `ddl-auto:
  update` (non-destructive); DB rollback = Neon point-in-time restore / branch.
- If auth breaks after a deploy: confirm `CORS_ALLOWED_ORIGINS` exactly matches
  the Vercel URL (scheme + host, no trailing slash) and `COOKIE_SECURE=true`.
