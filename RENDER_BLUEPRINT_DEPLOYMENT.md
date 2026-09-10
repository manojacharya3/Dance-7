# Render Blueprint Deployment — dance7-api

## Decision: Blueprint (not Docker, not manual Web Service)

- Repo root contains `render.yaml` defining the service as code; no `Dockerfile`
  exists, so Docker is out.
- Blueprint and manual Web Service would converge on the same settings, but
  Blueprint is reviewable, repeatable, and already committed — use it.

## render.yaml verification

| Field | Value | Verdict |
|-------|-------|---------|
| `type` / `runtime` | `web` / `java` | Correct — Spring Boot jar on Render's native Java runtime |
| `rootDir` | `backend` | Correct — `pom.xml` lives in `backend/` |
| `buildCommand` | `mvn -DskipTests package` | Correct — Render Java images ship Maven; skips tests for deploy speed |
| `startCommand` | `java -jar target/*.jar` | Correct (single `dance7-api` artifact); runs relative to `rootDir` |
| `healthCheckPath` | `/actuator/health` | Correct — actuator exposed (`health,info`) and `/actuator/health` is permitAll in `SecurityConfiguration` |
| `JAVA_VERSION: 21` | env | Correct — matches `pom.xml` (`java.version 21`) |
| `DATABASE_URL/USERNAME/PASSWORD` | `sync: false` | Correct — pasted manually from Neon (JDBC URL with `?sslmode=require`) |
| `JWT_SECRET` | `generateValue: true` | ⚠️ See warning below |
| `COOKIE_SECURE: true` | env | Correct — required for cross-site Vercel → Render cookies (`Secure + SameSite=None`) |
| `CORS_ALLOWED_ORIGINS` | `sync: false` | Correct — must exactly match the Vercel URL (scheme + host, no trailing slash) |
| `PORT` | (not listed) | Fine — injected by Render; `server.port: ${PORT:8080}` binds it |

⚠️ JWT warning: `JwtService` does `Decoders.BASE64.decode(secret)` and needs
≥256-bit key material. Render's generated value is not guaranteed to be valid
Base64 — prefer setting `JWT_SECRET` manually to `openssl rand -base64 48`.
Rotating it later invalidates all sessions (users re-login).

## Exact deployment steps

1. Push is done (`render.yaml` on `origin/main`).
2. Render Dashboard → New → **Blueprint** → select `manojacharya3/Dance-7` →
   branch `main` → Apply.
3. When prompted for `sync: false` values, enter:
   - `DATABASE_URL`: Neon pooled JDBC URL, e.g.
     `jdbc:postgresql://<host>-pooler.<region>.aws.neon.tech/<db>?sslmode=require`
   - `DATABASE_USERNAME` / `DATABASE_PASSWORD`: Neon credentials.
   - `CORS_ALLOWED_ORIGINS`: `https://<your-app>.vercel.app`.
4. Replace the generated `JWT_SECRET` with `openssl rand -base64 48` output.
5. Create the Blueprint → build starts (`mvn -DskipTests package`).
6. Watch Logs: expect Spring startup, `BranchSchemaMigration`, account
   initializers, `Tomcat started on port(s)`.
7. Verify: `GET https://dance7-api.onrender.com/actuator/health` → `200`.
8. Verify: log in on the Vercel app as OWNER; confirm `Set-Cookie ... Secure;
   SameSite=None` and dashboard data loads (no CORS errors).
9. Keep the service on Starter or higher in production (Free sleeps and will
   cold-start; free PostgreSQL on Render is not used — Neon is the database).

## If the deploy fails

- Build errors → check Java version is 21 and `backend/pom.xml` resolves.
- Crash loop on start with JWT/decode error → fix `JWT_SECRET` (Base64, ≥32 bytes).
- `Invalid CORS request` in browser → `CORS_ALLOWED_ORIGINS` mismatch.
- Login succeeds but immediate logout → `COOKIE_SECURE` must be `true`.
- Rollback: Render → Service → Deploys → Redeploy a previous commit; database
  changes are additive (`ddl-auto: update`), Neon point-in-time restore if needed.
