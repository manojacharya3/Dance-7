# Dance7 Test Coverage Recommendations

Current automated test inventory: no files were found under `backend/src/test`, and no frontend unit/E2E test framework is configured.

## Backend Priority

1. `@WebMvcTest(AuthController.class)`:
   - register success/duplicate/validation
   - login success/bad credentials
   - refresh success/missing/expired/revoked cookie
   - refresh token rotation
   - logout revocation and cookie clearing
   - `/auth/me` authenticated/unauthenticated
2. `@WebMvcTest(StudentController.class)`:
   - authentication required
   - CRUD status codes
   - invalid page/size
   - validation response shape
   - missing student 404
3. `@DataJpaTest`:
   - active-only list/search
   - soft-delete exclusion
   - tenant isolation
   - index/query behavior
4. Service tests:
   - BCrypt registration
   - duplicate email normalization
   - role assignment
   - tenant derivation policy
5. Security integration tests:
   - JWT cookie accepted
   - invalid/expired JWT rejected
   - Bearer token accepted
   - CORS credentials/preflight
   - CSRF policy explicitly verified

## Frontend Priority

- Add Playwright tests for login, register, redirect guard, logout, student CRUD, search, pagination, empty/error states.
- Add component tests for StudentForm validation and retained values after failed submit.
- Add tests for API-unavailable behavior and 401 handling.
- Add accessibility checks for labels, keyboard operation, table actions, and focus states.

## Quality Gates

Require `mvn clean test`, TypeScript check, Next production build, API integration tests against disposable PostgreSQL, and Playwright smoke tests in CI.
