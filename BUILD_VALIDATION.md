# Dance7 Build Validation

## Project Configuration

| Area | Result | Evidence |
|---|---|---|
| Frontend package | Pass | `package.json` uses Next.js 15, React 19, TypeScript 5.7, Tailwind 3.4 |
| Frontend scripts | Partial | `dev`, `build`, `start` exist; `lint` points to deprecated `next lint` behavior in newer Next versions |
| Next configuration | Pass | `reactStrictMode: true`; no custom rewrites/proxy |
| Frontend routes | Pass | `/`, `/login`, `/register`, `/dashboard`, `/students`, `/students/new`, `/students/{id}`, `/students/{id}/edit` |
| Backend parent | Pass | Spring Boot 3.4.1 |
| Java target | Pass | Maven source/target/release 21 |
| Compiler plugin | Pass | maven-compiler-plugin 3.13.0 |
| JWT dependencies | Pass | JJWT 0.12.6 API, implementation, Jackson runtime |
| PostgreSQL driver | Pass | Spring Boot-managed PostgreSQL runtime dependency |
| Datasource | Development-ready | Environment overrides exist; source contains fallback credentials |
| Security | Functional but high risk | JWT filter chain is configured; tenant and cookie/CSRF hardening are incomplete |

## Commands and Results

Backend command:

```powershell
cd backend
mvn clean test
```

Result: **Passed** during this audit. There are currently no backend test source files, so this primarily validates compilation and application test lifecycle.

Frontend command:

```powershell
npm.cmd exec -- tsc --noEmit
npm.cmd run build
```

TypeScript validation should be run in the local environment. The production build was started during this audit; confirm the final exit code in CI because the current workspace has generated `.next` artifacts.

## Route/Integration Validation

- `lib/auth.ts` calls `/api/auth/login`, `/api/auth/register`, `/api/auth/me`, and `/api/auth/logout`.
- `lib/students.ts` calls `/api/students` with `credentials: include`.
- Backend mappings match those paths.
- `NEXT_PUBLIC_API_URL` defaults to `http://localhost:8080/api`.
- CORS allows localhost ports 3000 and 3001 with credentials.

## Configuration Risks

- `JWT_SECRET` has a committed fallback.
- Database username/password have committed fallbacks.
- TRACE security logging is active in the default profile.
- No production profile, migration plugin, container configuration, health/readiness configuration, or CI workflow is present.
