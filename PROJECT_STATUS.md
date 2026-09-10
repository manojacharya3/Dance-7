# Dance7 Project Status

## Assessment Scope

This report reflects the implementation currently present in the repository. No application code was changed while preparing it.

## Completed Features

- Authentication foundation with registration, login, logout, refresh-token rotation, JWT access tokens, HttpOnly cookies, and `/auth/me`.
- Student Management with tenant-aware CRUD, soft delete, search, and pagination.
- Attendance Management with attendance entry, update, lookup, student history, date filtering, remarks, and `performanceScore` validation from 0 to 10.
- Current-month attendance summary per student with attendance percentage and average performance score.
- Student Performance page showing current-month attendance and performance history without charts.
- Dashboard with student data and live attendance-today, present, and absent widgets.
- Protected frontend route handling for dashboard, students, and attendance routes.

## Partially Implemented Features

- **Dashboard:** Functional summary view, but the student metrics are based on the first page of loaded students rather than a dedicated aggregate/count API. The “Records shown” widget duplicates the loaded student count.
- **Student Performance:** The page exists at `/students/{id}/performance`, but it is not linked from the visible student details page and calculates the current-month view from the paginated attendance-history response.
- **Attendance Reporting:** Current-month per-student summary is implemented. There are no selectable reporting periods, cross-student reports, export formats, instructor reports, or historical report views beyond attendance history.
- **Multi-tenancy:** Student and attendance queries accept tenant IDs and apply tenant filters, but tenant identity is also accepted from request data. It is not consistently derived from the authenticated JWT/user context.
- **Production security configuration:** Cookies are HttpOnly, but the current implementation sets `Secure=false`; HTTPS production configuration is not complete.
- **Automated verification:** Build validation has been performed, but the repository does not contain a broad automated test suite covering the listed workflows.

## Frontend Status

### Pages Implemented

- `/` redirects to `/dashboard`.
- `/login`
- `/register`
- `/dashboard`
- `/students`
- `/students/new`
- `/students/{id}`
- `/students/{id}/edit`
- `/students/{id}/performance`
- `/attendance`
- `/attendance/new`

### Navigation Items

- Overview: `/dashboard`
- Students: `/students`
- Attendance: `/attendance`

Student performance is implemented as a direct route but is not currently a sidebar item.

### Protected Routes

The client `AuthGuard` protects:

- `/dashboard`
- `/students` and nested student routes
- `/attendance` and nested attendance routes

Public routes are `/`, `/login`, and `/register`.

### Dashboard Widgets

- Active students
- Records shown
- Attendance today
- Present students
- Absent students
- Recent students list

The student widgets use the first loaded page of students. Attendance widgets use the date-filtered attendance API.

## Backend Status

### Controllers

- `AuthController`: registration, login, refresh, logout, and current-user lookup.
- `AuthDebugController`: debug-user endpoint.
- `HealthController`: health endpoint.
- `StudentController`: student CRUD, search, and pagination.
- `AttendanceController`: attendance CRUD, filtering, history, and monthly summary.
- `ApiExceptionHandler`: validation, not-found, authentication, and conflict responses.

### Services

- `UserService`: registration, password encoding, user lookup, and Spring Security user details.
- `StudentService` / `StudentServiceImpl`: student lifecycle, search, tenant filtering, and soft delete.
- `AttendanceService` / `AttendanceServiceImpl`: attendance lifecycle, active-student validation, filtering, and monthly calculations.

### Repositories

- `UserRepository`
- `RoleRepository`
- `RefreshTokenRepository`
- `StudentRepository`
- `AttendanceRepository`

### DTOs

- `AuthResponse`
- `LoginRequest`
- `RegisterRequest`
- `RefreshRequest`
- `StudentDTO`
- `AttendanceDTO`
- `AttendanceMonthlySummaryDTO`

### Security

- Stateless Spring Security configuration.
- JWT access-token validation through `JwtAuthenticationFilter`.
- Access tokens accepted from bearer headers or the `dance7_access_token` cookie.
- Refresh tokens persisted and revoked during rotation/logout.
- BCrypt password hashing.
- Role model includes `ADMIN` and `STAFF`.
- All non-auth API routes require authentication.
- CSRF is disabled for the stateless API.

Known security gap: tenant IDs are supplied through request parameters/body values instead of being enforced exclusively from the authenticated user context. Production cookies also need `Secure=true` when served over HTTPS.

## Database Status

### Entities

- `User`
- `Role`
- `RefreshToken`
- `Student`
- `Attendance`
- `AttendanceStatus` enum

### Relationships

- `User` has a many-to-many relationship with `Role` through `user_roles`.
- `RefreshToken` has a many-to-one relationship with `User`.
- `Attendance` stores `studentId` as a scalar field; there is no JPA relationship to `Student`.
- `Student` and `Attendance` are associated logically by `studentId` and tenant ID.

### Tables

The JPA entities map to:

- `users`
- `roles`
- `user_roles`
- `refresh_tokens`
- `students`
- `attendance`

The configured local schema strategy is `spring.jpa.hibernate.ddl-auto=update`, so schema changes are managed by Hibernate in the configured environment. PostgreSQL is the configured database.

## API Status

All listed application endpoints are implemented in controllers. Protected endpoints require authentication unless noted otherwise.

| Endpoint | Purpose | Status |
|---|---|---|
| `POST /api/auth/register` | Register a user and issue access/refresh cookies | Implemented |
| `POST /api/auth/login` | Authenticate credentials and issue rotated cookies | Implemented |
| `POST /api/auth/refresh` | Validate, revoke, and rotate a refresh token | Implemented; permitted without access token |
| `POST /api/auth/logout` | Revoke refresh token and clear cookies | Implemented |
| `GET /api/auth/me` | Return the authenticated user | Implemented |
| `GET /api/auth/debug-user` | Authentication diagnostics | Implemented; debug surface should be reviewed before production |
| `GET /api/health` | API health check | Implemented |
| `POST /api/students` | Create a student | Implemented |
| `PUT /api/students/{id}` | Update an active student | Implemented |
| `GET /api/students/{id}` | Get an active student | Implemented |
| `GET /api/students` | List, search, and paginate active students | Implemented |
| `DELETE /api/students/{id}` | Soft-delete a student | Implemented |
| `POST /api/attendance` | Mark attendance | Implemented |
| `PUT /api/attendance/{id}` | Update attendance and performance score | Implemented |
| `GET /api/attendance/{id}` | Get attendance by ID | Implemented |
| `GET /api/attendance` | List attendance, optionally by date or student | Implemented |
| `GET /api/attendance/student/{studentId}` | Get attendance history for a student | Implemented |
| `GET /api/attendance/student/{studentId}/monthly-summary` | Return current-month student attendance and performance summary | Implemented |
| `GET /api/attendance/date/{attendanceDate}` | Get attendance for a date | Implemented |

## Authentication Status

- **Register:** Implemented. Validated registration creates a user with a default tenant and STAFF role, then issues cookies.
- **Login:** Implemented. Credentials are checked using BCrypt and access/refresh cookies are issued.
- **Logout:** Implemented. The current refresh token is revoked and both cookies are cleared.
- **Refresh Token:** Implemented. Refresh tokens are persisted, checked for expiry/revocation, revoked on use, and rotated.
- **Protected Routes:** Implemented at both API and frontend route-guard levels.
- **Production readiness gap:** Cookie `Secure` is currently false, and authentication-derived tenant enforcement is incomplete.

## Student Management Status

- **Create:** Implemented with validation and `201 Created` response.
- **Read:** Implemented for individual active students and lists.
- **Update:** Implemented for active students with audit timestamps.
- **Delete:** Implemented as tenant-scoped soft delete using `active=false`.
- **Search:** Implemented by first name, last name, full name, email, and phone.
- **Pagination:** Implemented with page and size parameters, validation, and sorted results.
- **Related performance view:** Implemented separately at `/students/{id}/performance`, but not linked from the student details UI.

## Attendance Status

- **Attendance Entry:** Implemented through the attendance form and `POST /api/attendance`.
- **Performance Score:** Implemented as an optional integer from 0 to 10, persisted with attendance and displayed in attendance/performance views.
- **Monthly Summary:** Implemented for the current calendar month through `GET /api/attendance/student/{studentId}/monthly-summary`.
- **Reports:** Partially implemented. Attendance list, date filtering, student history, and the current-month summary exist. Advanced reporting, date-range selection, exports, and aggregate studio/instructor reports are absent.

## Missing Modules

The following business modules have no corresponding implementation in the current application:

- Branch Management
- Instructor Management
- Batch Management
- Memberships
- Payments
- Invoices
- Parent Portal
- Notifications

These missing modules also mean there are no corresponding domain entities, repositories, services, controllers, frontend pages, or workflows for them.

## MVP Completion Percentage

**Estimated completion: 45%.**

This estimate reflects a functional foundation with authentication, student management, attendance, performance scoring, and dashboard visibility, but most studio operations are still absent. The percentage is reduced by the missing business modules, limited reporting, incomplete production security settings, tenant-context gaps, and limited automated test coverage.

## Recommended Next Module

### Instructor Management

Instructor Management is the recommended next module because it is a prerequisite for assigning ownership and accountability to attendance, batches, classes, and performance records. It establishes the people model needed before implementing Batch Management, instructor-specific reporting, scheduling, and richer attendance workflows. After instructors, Batch Management should follow to connect students and instructors to recurring classes.