# Implementation checkpoint — 2026-10-29

## Implemented

- HR session API and Angular login/logout, with CSRF cookie renewal.
- Employee directory with server-side filtering, bounded pagination and
  allowlisted sorting with deterministic employee-code ordering.
- Create, view, edit, archive and reactivate employee workflows.
- URL query state restoration, cancelled stale list requests, loading,
  empty, retryable list errors and visible lifecycle-action errors.
- Profile length validation, generated OpenAPI snapshot and contract test.
- Session-expiry handling redirects to login without replaying writes.
- Windows Maven wrapper handles a normal non-symlink dependency directory.
- CI workflow for backend verification and frontend tests/build.

## Verification and limits

Frontend unit tests passed (7 tests), and the production build passed. Focused
backend security, controller, pagination and OpenAPI tests passed (24 tests)
without a database.
The full `mvnw verify` was attempted and failed because Testcontainers could
not connect to a Docker engine. Docker Desktop was started, but its backend
remained unavailable. PostgreSQL migration/repository checks and complete
browser journeys remain unverified. CI has been configured, not run remotely.

## Remaining core slices

1. Compensation migration, exact-decimal validation, atomic current/history
   writes, expected-version conflicts and database rollback/concurrency tests.
2. Salary editor and paginated history with explicit conflict recovery.
3. Fixed labeled demo FX snapshot and SQL analytics with fixture-based tests.
4. Explicit deterministic 10,000-employee seed command and rerun verification.
5. Dashboard, browser journeys, query-plan review and Docker deployment smoke test.

The app is an employee-management checkpoint, not a completed salary-management
assessment. No deployed application or demo video is claimed.

## API contract

`docs/openapi.json` is exported from Springdoc, not maintained independently.
Regenerate from `backend` (PowerShell uses `mvnw.cmd`):

```sh
./mvnw test -Dtest=ApiContractTest -Dopenapi.output=../docs/openapi.json
```

The contract test replaces persistence boundaries with mocks and excludes
database auto-configuration solely in that test. Runtime and persistence tests
still use PostgreSQL. Directory status defaults to all statuses when absent.
Only the currently implemented endpoints are included; salary/analytics are
not yet available. Server-side Bean Validation errors return 400.
