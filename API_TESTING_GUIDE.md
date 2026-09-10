# Dance7 API Testing Guide

Base URL: `http://localhost:8080/api`

Authentication uses HttpOnly cookies. Use a client that preserves cookies between requests.

## Health

### GET `/health`

Request:

```http
GET /api/health
```

Expected: `200`

```json
{"status":"ok","service":"dance7-api"}
```

## Authentication

### POST `/auth/register`

```json
{"fullName":"Dance7 Admin","email":"admin@dance7.test","password":"Dance7Admin123!","tenantId":"default"}
```

Expected: `201`, user response, `dance7_access_token` and `dance7_refresh_token` HttpOnly cookies.

### POST `/auth/login`

```json
{"email":"admin@dance7.test","password":"Dance7Admin123!"}
```

Expected: `200`, user response, rotated cookies. Bad credentials should be rejected with a 401-class response.

### GET `/auth/me`

Request with the access cookie. Expected: `200` current user. Without a valid access cookie: `401`.

### POST `/auth/refresh`

Request with the refresh cookie and no body. Expected: `200`, old refresh token revoked, new access/refresh cookies issued. Missing, expired, or revoked token: `401`.

### POST `/auth/logout`

Request with the refresh cookie. Expected: `204`, both auth cookies cleared, refresh token revoked.

## Students

All student endpoints require a valid access token.

### GET `/students?tenantId=default&page=0&size=10`

Expected: `200` Spring Page response containing only active tenant students. Current implementation accepts tenant ID from the request; this is a security finding and must be replaced with JWT-derived tenant identity.

### GET `/students?tenantId=default&search=maya&page=0&size=10`

Expected: `200`, case-insensitive match across first name, last name, full name, email, and phone.

### GET `/students/{id}?tenantId=default`

Expected: `200` for an active record, `404` for missing/inactive/wrong-tenant lookup.

### POST `/students`

```json
{
  "tenantId":"default",
  "firstName":"Nora",
  "lastName":"Kim",
  "dateOfBirth":"2013-08-22",
  "gender":"Female",
  "email":"nora.kim@example.com",
  "phone":"+1 555 010 1099",
  "parentName":"Daniel Kim",
  "parentPhone":"+1 555 010 2099",
  "danceStyle":"Ballet",
  "skillLevel":"Beginner",
  "medicalNotes":"No known conditions.",
  "active":true
}
```

Expected: `201` and a response containing generated ID and timestamps. Missing first/last name or malformed email should produce validation failure; add a global validation handler to guarantee a stable 400 envelope.

### PUT `/students/{id}`

Use the same JSON with edited fields. Expected: `200`, updated values, updated timestamp. Missing student: `404`.

### DELETE `/students/{id}?tenantId=default`

Expected: `204`; row remains in the database with `active=false` and disappears from active list/search/get.

## Negative API Tests

- Unauthenticated `GET /students`: expected `401` or configured authentication entry-point response.
- Invalid JWT on `GET /students`: expected `401`.
- Other tenant lookup/list: must not expose records; current client-controlled tenant parameter is a critical gap.
- `GET /students?page=-1`: expected `400`.
- `GET /students?size=101`: expected `400`.
- Missing refresh cookie: expected `401`, not `403`.
- Reusing a rotated refresh token: expected `401`.
