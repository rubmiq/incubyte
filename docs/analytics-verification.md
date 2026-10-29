# Analytics filtering verification — 2026-10-29

Country and department filters combine with AND and apply to the full active
population, summary metrics and both breakdowns. Clearing filters makes one
unfiltered request; new selections cancel previous requests. Invalid enum values
return the standard 400 field error; authentication and HR authorization remain
required. The generated OpenAPI contract includes both filter enums.

Fixed an undefined SQL alias in filtered headcounts, row callbacks that skipped
the aggregate row, missing USD payroll when the seed omits USD/USD, and missing
Material table header templates. Analytics filter controls now use the employee
list's wrapping flex layout, 0.75rem gap, flex-start alignment and 1rem bottom margin.
The FX label uses the stored snapshot date when available.

Verification performed:

- Frontend: `npm.cmd test -- --watch=false` — 10 tests passed, including combined
  controls, cancellation, clear, retry and rendered summary/breakdown tables.
- Frontend: `npm.cmd run build` — passed. Existing unused confirmation-dialog
  import and initial bundle budget warnings remain.
- Backend: focused `AnalyticsControllerTest,ApiContractTest,AuthSecurityTest,
  EmployeeControllerTest,EmployeeSortAllowlistTest` — 27 tests passed. Contract
  regenerated with `-Dopenapi.output=../docs/openapi.json`.
- Backend: `AnalyticsFilteringTest` — 5 fixture cases passed against local
  PostgreSQL 17 in the separate `analytics_filter_verification` schema. These
  cover country, department, combined/empty filters, archived exclusion,
  odd/even medians, USD without a rate, EUR conversion, snapshot date and
  summary/breakdown payroll and headcount consistency. Fixture writes roll back.
- Live requests through Angular's `/api` proxy: headcounts matched the paged
  employee endpoint and both breakdown sums: all active 9,999; US 1,002;
  Engineering 1,011; US + Engineering 112. Payroll was positive in every case.
  Invalid country/department returned 400; unauthenticated analytics returned 401.

Applications were left running at http://127.0.0.1:4201 (Angular) and
http://localhost:8080 (Spring Boot), using the existing local PostgreSQL server.
No credentials are recorded here.

Full `mvnw.cmd verify` was attempted and failed: Docker Desktop's engine returned
500, blocking Testcontainers; the contract test also needed its analytics and
seeder boundaries mocked, which was fixed and rechecked successfully. The
PostgreSQL 18 container suite and a real-browser visual check remain unverified.
Frontend component checks used Angular Material harnesses in the unit runner.

The analytics fixture test defaults to Testcontainers PostgreSQL 18. For the
external database fallback, set `analytics.test.external-postgres=true`, provide
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and
`SPRING_DATASOURCE_PASSWORD`, and isolate it with a dedicated URL `currentSchema`
and matching `SPRING_FLYWAY_SCHEMAS`. Run only `-Dtest=AnalyticsFilteringTest`;
never point fixture tests at application tables.
