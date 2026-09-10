# Dance7 QA and Testing Guide

This guide covers the current Dance7 - The Art Factory application as implemented in the workspace.

## 1. Application Overview

Dance7 is a Next.js frontend backed by a Spring Boot REST API and PostgreSQL. The implemented functional module is Student Management.

- Frontend: Next.js 15, React 19, TypeScript, Tailwind CSS
- Backend: Spring Boot 3.4.1, Java 21 target, Spring Data JPA, Spring Security
- Database: PostgreSQL
- Default tenant: `default`

## 2. Application URL Map

| URL | Method | Expected result |
|---|---|---|
| `http://localhost:3000/` | GET | Redirects to `/dashboard` |
| `http://localhost:3000/login` | GET | Dance7 sign-in screen |
| `http://localhost:3000/dashboard` | GET | Dashboard overview with summary cards, projects, and activity |
| `http://localhost:3000/students` | GET | Student table with search and pagination |
| `http://localhost:3000/students/new` | GET | Add student form |
| `http://localhost:3000/students/{id}` | GET | Student detail view |
| `http://localhost:3000/students/{id}/edit` | GET | Edit student form |

The sidebar currently exposes only implemented routes: Overview and Students. Projects, Calendar, Team, Reports, and Settings are not implemented modules.

## 3. API Endpoint List

Base URL: `http://localhost:8080/api`

| Method | Endpoint | Purpose | Success |
|---|---|---|---|
| GET | `/health` | Service health check | `200` with `{ "status": "ok", "service": "dance7-api" }` |
| POST | `/students` | Create a student | `201` with created student |
| GET | `/students` | List active students | `200` paged response |
| GET | `/students?search=maya&page=0&size=10` | Search active students | `200` paged response |
| GET | `/students/{id}?tenantId=default` | Get one active student | `200` student or `404` |
| PUT | `/students/{id}` | Update a student | `200` updated student or `404` |
| DELETE | `/students/{id}?tenantId=default` | Soft-delete a student | `204` |

List query parameters:

- `tenantId`: defaults to `default`
- `search`: optional search across first name, last name, full name, email, and phone
- `page`: zero-based page number, defaults to `0`
- `size`: page size from `1` to `100`, defaults to `10`

The list response follows Spring Data's page shape:

```json
{
  "content": [],
  "number": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## 4. Startup Instructions

Run commands from PowerShell. Use separate terminals for the backend and frontend.

### 4.1 Prerequisites

- Java 21 or newer installed. The project compiles with Java 21 release settings.
- Maven 3.9 or newer.
- Node.js and npm.
- PostgreSQL running on port `5432`.

The current local toolchain used during QA was JDK 25 as the host JDK, Maven 3.9.16, and Java 21 compiler source/target/release settings.

### 4.2 Database Setup

Create the database and user if they do not already exist:

```sql
CREATE USER postgres WITH PASSWORD 'postgres';
CREATE DATABASE studioos OWNER postgres;
```

If the user already exists, run only:

```sql
CREATE DATABASE studioos OWNER postgres;
```

The application reads these environment variables:

- `DATABASE_URL`, default `jdbc:postgresql://localhost:5432/studioos`
- `DATABASE_USERNAME`, default `postgres`
- `DATABASE_PASSWORD`, default `postgres`
- `PORT`, default `8080`

Hibernate uses `spring.jpa.hibernate.ddl-auto=update`, so it creates or updates the `students` table during startup. After the backend has started once, load sample records:

```powershell
psql -U postgres -d studioos -f backend\src\main\resources\db\seed_students.sql
```

The seed script is repeatable and inserts four active sample students only when their tenant/email combination does not already exist.

### 4.3 Backend Startup

```powershell
cd backend
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
& 'C:\Users\manojkumar.nandachar\.maven\maven-3.9.16\bin\mvn.cmd' spring-boot:run
```

Verify the backend:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Expected result:

```json
{
  "status": "ok",
  "service": "dance7-api"
}
```

### 4.4 Frontend Startup

```powershell
cd C:\Users\manojkumar.nandachar\Documents\Project-1
npm.cmd install
npm.cmd run dev
```

Open:

```text
http://localhost:3000/students
```

To use a different backend URL, set `NEXT_PUBLIC_API_URL` before starting Next.js. It should include the `/api` suffix, for example:

```powershell
$env:NEXT_PUBLIC_API_URL = 'http://localhost:8080/api'
```

## 5. PostgreSQL Configuration Verification

Current configuration is in `backend/src/main/resources/application.yml`:

- Driver dependency: PostgreSQL JDBC driver managed by Spring Boot.
- Default JDBC URL: `jdbc:postgresql://localhost:5432/studioos`.
- Default user: `postgres`.
- Default password: `postgres`.
- Schema mode: `update`.
- JPA open-in-view: disabled.
- SQL formatting: enabled.

Verification checklist:

- PostgreSQL service is running.
- Port `5432` accepts TCP connections.
- Database `studioos` exists.
- Credentials match the environment variables.
- The backend creates the `students` table after startup.
- The seed script inserts four rows.

## 6. Spring Security Verification

Current security behavior:

- Stateless session policy.
- CSRF disabled for this REST API.
- HTTP Basic and form login are disabled; JWT cookies protect authenticated routes.
- `OPTIONS /**` is permitted for browser preflight.
- `/api/health` and `/api/auth/**` are public; `/api/students/**` requires authentication.
- Other routes require authentication.
- StudentController allows origins `http://localhost:3000` and `http://localhost:3001`.

Security QA checks:

- A browser request from port 3000 can call the student API.
- An `OPTIONS` request receives a non-401 response.
- Student routes return an authentication failure without a valid JWT cookie.
- A future production release should derive tenant authorization from the authenticated JWT rather than request parameters.

## 7. Manual Testing Checklist

### Frontend navigation

- [ ] `/` redirects to `/dashboard`.
- [ ] `/login` renders Dance7 branding.
- [ ] `/dashboard` renders without console errors.
- [ ] Sidebar opens `/students`.
- [ ] `/students` renders without a backend error when PostgreSQL and backend are running.

### Student list

- [ ] Student rows show name, gender, dance style, level, email, and phone.
- [ ] Search matches first name.
- [ ] Search matches last name and full name.
- [ ] Search matches email and phone.
- [ ] Clearing search returns the first page.
- [ ] Next and Previous pagination buttons enable and disable correctly.
- [ ] Empty results show the empty state.
- [ ] API errors show an inline error message.

### Create student

- [ ] Add Student opens `/students/new`.
- [ ] First name and last name are required.
- [ ] Invalid email is rejected by browser/API validation.
- [ ] All profile, contact, and medical fields can be entered.
- [ ] Successful submit creates a row and redirects to its detail page.
- [ ] A failed API request displays an error without losing the form.

### View and edit student

- [ ] Selecting a student opens `/students/{id}`.
- [ ] Detail page shows profile, contact, emergency, and care information.
- [ ] Edit opens `/students/{id}/edit` with existing values populated.
- [ ] Saving edits updates the detail page and list data.
- [ ] An unknown ID displays an error rather than an infinite loading state.

### Soft delete

- [ ] Delete asks for confirmation.
- [ ] Confirming delete removes the row from the active list.
- [ ] Deleted records are not returned by list, search, or get-by-id.
- [ ] The database row remains with `active=false`.
- [ ] Cancelling delete leaves the row unchanged.

## 8. API Test Scenarios and Sample Requests

Set a PowerShell variable:

```powershell
$base = 'http://localhost:8080/api'
```

Health:

```powershell
Invoke-RestMethod "$base/health"
```

List and search:

```powershell
Invoke-RestMethod "$base/students?tenantId=default&page=0&size=10"
Invoke-RestMethod "$base/students?tenantId=default&search=maya&page=0&size=10"
```

Create:

```powershell
$student = @{
  tenantId = 'default'
  firstName = 'Nora'
  lastName = 'Kim'
  dateOfBirth = '2013-08-22'
  gender = 'Female'
  email = 'nora.kim@example.com'
  phone = '+1 555 010 1099'
  parentName = 'Daniel Kim'
  parentPhone = '+1 555 010 2099'
  danceStyle = 'Ballet'
  skillLevel = 'Beginner'
  medicalNotes = 'No known conditions.'
  active = $true
} | ConvertTo-Json

$created = Invoke-RestMethod "$base/students" -Method Post -ContentType 'application/json' -Body $student
$created
```

Get and update:

```powershell
Invoke-RestMethod "$base/students/$($created.id)?tenantId=default"

$update = $created | ConvertTo-Json
Invoke-RestMethod "$base/students/$($created.id)" -Method Put -ContentType 'application/json' -Body $update
```

Soft delete:

```powershell
Invoke-WebRequest "$base/students/$($created.id)?tenantId=default" -Method Delete
```

Negative scenarios:

- `GET /api/students/999999?tenantId=default` returns `404`.
- `DELETE /api/students/999999?tenantId=default` returns `404`.
- `GET /api/students?page=-1` returns `400`.
- `GET /api/students?size=101` returns `400`.
- `POST /api/students` without `firstName` or `lastName` returns validation failure.
- `POST /api/students` with malformed email returns validation failure.
- A different tenant ID cannot see `default` tenant students.

## 9. Expected Results by Page

| Page | Expected result |
|---|---|
| Root | Redirects to dashboard. |
| Login | Calls the backend login API, sets HttpOnly cookies, and navigates to dashboard after authentication. |
| Dashboard | Displays live active-student data and API status. Project/team/report modules are not implemented. |
| Students | API-backed active student list, search, page navigation, and soft-delete controls render. |
| Add Student | Form submits to `POST /api/students`, then navigates to the new profile. |
| Student Details | API-backed profile view renders; missing IDs show an error. |
| Edit Student | Existing profile loads and submits to `PUT /api/students/{id}`. |

## 10. Known Issues and Recommended Fixes

### Known issues

1. PostgreSQL must be running locally; there is no Docker Compose or embedded test database yet.
2. Login and registration call the backend JWT endpoints and receive HttpOnly access/refresh cookies.
3. Dashboard and student routes are guarded by `/api/auth/me`; Student API endpoints require authentication. `tenantId` is still supplied by the client and is not yet derived from an authenticated identity.
4. The dashboard project cards, `New project`, `View all`, and Settings controls are presentation-only.
5. There are no committed automated controller/service integration tests yet.
6. The application starts with JDK 25 locally while compiling Java 21-compatible bytecode. A Java 21 runtime should be used in CI and production for LTS consistency.
7. `spring.jpa.hibernate.ddl-auto=update` is convenient for development but should be replaced with migrations before production.

### Fixes made during this QA review

- Added a 404 JSON response for missing students instead of exposing a generic server error.
- Removed sidebar links to unimplemented routes so navigation no longer leads to guaranteed 404 pages.
- Added repeatable PostgreSQL seed data at `backend/src/main/resources/db/seed_students.sql`.

### Recommended next fixes

1. Add authentication and derive tenant identity from the authenticated principal.
2. Add Flyway or Liquibase migrations and change production schema management from `update`.
3. Add `@WebMvcTest` controller tests and `@DataJpaTest` repository tests for Student CRUD, tenant isolation, search, paging, and soft delete.
4. Add a Docker Compose PostgreSQL service for repeatable local setup.
5. Implement or remove the dashboard's project/settings presentation controls.
6. Add frontend Playwright tests for create, edit, search, pagination, detail, and delete flows.
7. Add a global CORS configuration with environment-specific allowed origins rather than fixed localhost values.
8. Add structured validation-error mapping so frontend forms display field-level backend errors.

## 11. Validation Results from This Review

- Backend `mvn clean test`: passed.
- Frontend `tsc --noEmit`: passed.
- Frontend calls and backend Student route paths match.
- Student controller, service, mapper, repository, and entity diagnostics report no errors.
- PostgreSQL runtime connectivity was not available during the earlier local startup check; full CRUD runtime testing requires PostgreSQL to be running.
