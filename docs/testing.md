# Verification

## Salary workflow (2026-10-29)

- Frontend unit tests: `cd frontend; npm.cmd test -- --watch=false` — 19 passed.
- Frontend production build: `npm.cmd run build` — passed. Existing unused
  confirmation-dialog import and initial bundle budget warnings remain.
- Backend focused suite: 44 passed, including 11 real PostgreSQL salary service
  tests, 6 salary MVC/security tests, and existing authentication, employee,
  analytics-controller and API-contract tests.
- Playwright: one complete salary browser journey passed in headless Microsoft
  Edge. It covers session login, employee search, initial salary, decimal input
  validation, salary update, before/after history, payroll dashboard and two-tab
  stale-write recovery with the draft preserved. No HTTP mocks are used.
- OpenAPI was regenerated from the contract test; successful and error responses,
  decimal strings, nullable legacy audit fields and supported currencies are included.
- After restarting the main app, read-only salary/history requests through the
  Angular proxy passed against existing demo records after the additive migration.

Salary service fixtures prove exact bounds, currency changes, actor/time capture,
stale versions, competing writes/initialization, per-employee history, deterministic
paging, archived/missing employees, no-op rejection, database history immutability,
and rollback of both amount and version when the audit insert fails. Clocks are
fixed and concurrency uses barriers rather than sleeps.

## Backend commands

With Docker running (Testcontainers PostgreSQL 18):

```sh
cd backend
./mvnw verify
```

On Windows use `mvnw.cmd`. Docker Desktop remains unavailable locally (engine
returns 500), so the full container suite is not claimed as passing. Salary
transaction tests instead passed on local PostgreSQL 17 in a dedicated schema.
The remote CI jobs, PostgreSQL 18 container run and Docker image build are not
verified locally.

For the explicitly isolated salary fallback, supply these environment variables
using your own local database credentials (never put credentials in the URL):

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/<database>?currentSchema=salary_test_workflow
SPRING_DATASOURCE_USERNAME=<local database user>
SPRING_DATASOURCE_PASSWORD=<local database password>
SPRING_FLYWAY_SCHEMAS=salary_test_workflow
```

```sh
./mvnw test -Dsalary.test.external-postgres=true -Dtest=SalaryServiceTest,SalaryControllerTest,ApiContractTest,AuthSecurityTest,EmployeeControllerTest,EmployeeSortAllowlistTest,AnalyticsControllerTest -Dopenapi.output=../docs/openapi.json
```

The salary test class truncates **only its test schema** between fixtures. It
refuses an external schema whose name does not start with `salary_test_`.
Never redirect fixtures at application tables. This fallback does not bypass
Docker for unrelated Testcontainers test classes.

## Browser command

From `frontend`, with Java 21, Node 22 and a reachable PostgreSQL database:

1. `npm ci`
2. Provide `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
   `DEMO_HR_USERNAME`, `DEMO_HR_PASSWORD` through the process environment.
3. `npx playwright install chromium` (or set `PLAYWRIGHT_CHANNEL=msedge` to use
   installed Microsoft Edge).
4. `npm run test:e2e`

The command type-checks the test/configuration and starts Angular on 4202 and
Spring Boot on 8082. It uses the separate `salary_test_e2e` schema and does not
seed the 10,000-employee demo. Previously running local test servers can be reused;
do not run an unrelated backend on 8082. Each run creates one synthetic employee
and archives it afterwards; active payroll remains isolated between runs.
Tracing, screenshots, video and persisted session files are disabled so login
credentials and session tokens do not enter artifacts. The CI browser job uses
an ephemeral PostgreSQL 18 service and a generated HR password.
