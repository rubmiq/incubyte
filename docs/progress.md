# Implementation checkpoint — 2026-10-29

Implemented:

- HR session login/logout with CSRF renewal and session-expiry navigation.
- Server-filtered/sorted/paginated employee directory, URL state restoration,
  profile creation/editing, archive/reactivate and employee details.
- Initial salary setup and updates in employee details, explicit local currency,
  exact decimal-string salary API, validation and mandatory reasons.
- Version-protected writes, atomic current salary/audit updates, immutable
  before/after history with actor/time and server pagination. Conflicts preserve
  drafts and require explicit refresh/review; archived employees are read-only.
- Filtered payroll analytics and responsive filter spacing; USD handling,
  breakdown rendering and stale-request cancellation fixed.
- Explicit synthetic employee/compensation seed, Flyway migrations, generated
  OpenAPI contract, Docker packaging and CI jobs.
- Browser salary journey with real backend/database and two-tab conflict recovery.

See [testing](testing.md) for actual results and commands: 44 focused backend
checks, 19 frontend unit tests, production build and a Microsoft Edge browser
journey passed. PostgreSQL integration checks used isolated local PostgreSQL 17
schemas because Docker Desktop's engine remains unavailable. The full
PostgreSQL 18 Testcontainers suite, Docker image and remote CI are unverified.

Remaining assessment work:

1. Analytics decimal-string wire format, exact numeric median and explicit
   missing-FX handling; consistent population semantics for employees without salary.
2. Seed count/rerun/status-distribution tests and bounded batch insertion;
   measured list/analytics query plans and performance notes.
3. Broader accessibility/responsive review and deployment smoke test.
4. AI-usage record, performance report, demo script, submission checklist,
   authorized deployment and recorded video. No deployment/video is claimed.

API contract export (from `backend`; Windows uses `mvnw.cmd`):

```sh
./mvnw test -Dtest=ApiContractTest -Dopenapi.output=../docs/openapi.json
```

The contract test mocks application boundaries only; real transaction tests
exercise PostgreSQL, including failed audit writes and competing salary changes.
