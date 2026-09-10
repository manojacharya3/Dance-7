# Dance7 Authentication QA Results

Date: 2026-09-06

## Test User

- Email: `test.user@dance7.com`
- Password: supplied QA password
- Full name: `Test User`
- Database ID: `15`
- Enabled: `true`
- Password storage: 60-character BCrypt hash
- Roles: `STAFF, ADMIN`

Registration assigns `STAFF` by application logic. `ADMIN` was added directly to this QA account for the requested test data; the public registration UI does not accept a role field.

## Results

| Check | Result | Evidence |
|---|---|---|
| Registration API | PASS | HTTP `201` for fresh QA users |
| Registration UI route | PASS | `/register` returns HTTP `200` |
| User saved in PostgreSQL | PASS | User ID 15 exists in `studioos.users` |
| Email | PASS | `test.user@dance7.com` |
| Password hash | PASS | 60-character BCrypt hash beginning `$2a$10$` |
| Enabled flag | PASS | `true` |
| Role | PASS | `STAFF, ADMIN` for QA account |
| Login API | PASS | HTTP `200` after transaction fix |
| Access cookie | PASS | `dance7_access_token` received |
| Refresh cookie | PASS | `dance7_refresh_token` received |
| Protected student endpoint | PASS | HTTP `200` with authenticated cookies |
| Logout | PASS | HTTP `204` |
| Protected access after logout | PASS | HTTP `401` |
| Login again | PASS | HTTP `200` |
| Login UI route | PASS | `/login` returns HTTP `200` |

## Failure Found and Fixed

Login reached the user lookup and BCrypt comparison successfully:

```text
userFound=true
enabled=true
passwordMatches=true
```

It then failed while rotating refresh tokens:

```text
TransactionRequiredException:
No EntityManager with actual transaction available for current thread
```

Root cause: `RefreshTokenRepository.deleteByUserId()` was a modifying delete method without `@Modifying` and `@Transactional`.

Fix applied in `backend/src/main/java/com/studioos/repository/RefreshTokenRepository.java`:

```java
@Modifying
@Transactional
void deleteByUserId(Long userId);
```

## Validation

- Backend `mvn clean test-compile`: passed.
- Frontend TypeScript validation: previously passed.
- Registration and login UI routes: HTTP `200`.
- Fresh registration: HTTP `201`.
- Fresh login: HTTP `200`.
