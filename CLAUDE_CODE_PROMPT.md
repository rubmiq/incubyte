# Claude Code implementation prompt

You are my senior full-stack engineering partner. Build a complete assessment submission for ACME salary management using modern Angular and Java/Spring Boot. Implement and verify working software; do not stop at a plan or mock UI.

## Context and constraints

The HR team currently manages salaries for 10,000 employees across multiple countries in spreadsheets. They need reliable editing and clear answers about compensation. Evaluation emphasizes product framing, engineering judgment, meaningful tests, deliberate AI use, deployed functionality, and authentic incremental commits.

Use Java with Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Maven Wrapper; Angular with strict TypeScript, standalone components, typed reactive forms, Angular Material, and RxJS. Choose mutually compatible supported stable versions after checking official documentation and the environment. Record exact versions and commit lockfiles. Use a modular monolith and one repository with backend/ and frontend/. PostgreSQL is an intentional relational database choice; do not add SQLite compatibility. Prefer a same-origin deployment for UI and API.

The assessment says “AngularJS for Java.” We are choosing modern Angular, not legacy AngularJS. Record this interpretation in requirements and flag it for evaluator confirmation without silently changing frameworks.

Read CLAUDE.md, AGENTS.md, frontend/AGENTS.md, and backend/AGENTS.md first. These files govern implementation. Inspect existing repository state and preserve unrelated work. Ask only questions that block correctness or access; document reasonable assumptions and continue.

## Before application code

Write docs/requirements.md, approximately one page (500–650 words maximum), with goal, HR persona, scope, acceptance criteria, assumptions, explicit exclusions, and reasons. Commit it BEFORE scaffolding application code. Keep this document concise; place detail in docs/design.md and docs/decisions.md.

Default MVP:
- HR login/logout using secure server sessions and one HR role.
- Employee directory: server-side pagination, search by name/email/employee code, country/department/status filters, and allowlisted sorting. Persist filters in URL parameters.
- Create employees, view details, edit profile, archive/reactivate. Preserve compensation history; do not hard-delete employees.
- View and update current gross annual base salary, local currency, change reason, and read-only chronological audit history.
- Dashboard with active headcount, annual base payroll, mean and median, and breakdowns by country and department. Make the questions answered obvious. All aggregations cover the full filtered dataset, not the current page.
- Seed exactly 10,000 synthetic employees with realistic distributions across countries, departments, statuses, and currencies; include representative salary history.

Exclude payroll execution, taxes, benefits, bonuses/equity, approvals, employee self-service, multi-tenancy, scheduled/backdated compensation, real-time FX, statistical pay-equity claims, and natural-language AI chat. Document why these would dilute a small reliable submission. CSV import/export is optional only after the core is complete; if omitted, explain the staged migration assumption. Do not build extra features just to look sophisticated.

## Domain decisions to implement explicitly

- Compensation is gross ANNUAL base salary; do not conflate salary with net pay or total compensation. MVP salary changes take effect immediately. Define the organization's display timezone; store audit instants in UTC.
- Store local amount as an exact decimal and ISO currency code. Use Java BigDecimal and database NUMERIC; never binary floating point for authoritative money. Validate scale per supported currency and safe upper/lower bounds. Accept/return monetary JSON values as decimal strings; display formatted values in Angular.
- Define one reporting currency (USD) and a dated, fixed, clearly labeled DEMO FX snapshot. Define rate direction, conversion precision, rounding stage, even-count median, missing-rate behavior, and zero-result behavior. Never silently sum mixed currencies or imply live FX. Preserve local salary as the source of truth. Employee country and currency are separate fields with validated supported values.
- Analytics use active employees' current salaries and the same normalized filters. Show basis, currency, FX date, and result count. Do not imply historical payroll analytics from current FX data.
- Salary updates atomically change the current compensation and append an immutable history entry with before/after amount and currency, actor, reason, and timestamp. Use optimistic concurrency with an expected version; return 409 on stale updates and give the UI a recoverable conflict flow. No silent last-write-wins. Cover profile/status updates as appropriate.
- Enforce authentication and authorization on the API, not only in Angular guards. Use validated DTOs, not exposed JPA entities. Reject invalid or unknown query values consistently.

## Build in reviewable vertical slices

1. Product framing and architecture decisions.
2. Reproducible project setup, migrations, local database, deterministic seeding, health check.
3. Authentication and security tests.
4. Searchable employee directory and details through real API/UI.
5. Salary updates, transactional audit history, concurrency handling.
6. Accurate filtered analytics and accessible dashboard.
7. Integration/E2E tests, deployment, measured performance notes, demo artifacts.

For each slice: implement the smallest coherent behavior, test success and meaningful failures, inspect the diff, and make an honest descriptive commit. Commit tests with the behavior they protect. Never fabricate commit history or rewrite it to simulate development. Do not push over unrelated branches or commit credentials, generated build output, database dumps, or real personal data.

## Verification

Use fast JUnit domain tests; focused MVC/security tests; repository and transaction integration tests against PostgreSQL using Testcontainers; focused Angular component/service tests using the selected CLI's supported runner; and a small Playwright E2E suite. Do not substitute H2 for database-sensitive tests or mock away the behavior under test.

Prioritize exact conversion/rounding and median examples, empty results, combined filters, deterministic paging, invalid input, unauthenticated/forbidden requests, rejected CSRF, stale updates, atomic audit rollback, immutable history, seed count and rerun behavior, UI validation, latest-search-wins behavior, loading/error/empty states, and conflict recovery. E2E: login → find employee → update salary → verify history and dashboard; include one conflict or validation failure. Use tiny explicit fixtures for arithmetic assertions. No sleeps, live FX, or network-dependent unit tests. Keep clocks and random seeds controlled.

Run backend tests, frontend tests, formatting/linting, strict compilation, production builds, integration tests and E2E where the environment supports them. Report commands and actual outcomes. Never label unrun checks as passed. Set documented, realistic performance targets and measure key list/analytics queries on 10,000 seeded records; record environment and limitations, not invented timings.

## Deployment and handoff

Provide Dockerfiles, docker-compose.yml, .env.example without secrets, CI, and exact fresh-clone instructions. Persist PostgreSQL data outside ephemeral application storage. Use migrations for schema changes; seeding is an explicit demo operation, never a destructive startup step. Production debug disabled, HTTPS and secure cookies configured, narrow health endpoint, no publicly exposed database.

Deploy when a suitable authorized environment is available. If credentials or hosting access are missing, complete a reproducible deployment package and state the exact remaining blocker; do not claim deployment is finished. Do not reset a shared database. Demo accounts must be environment-controlled and limited to synthetic data.

Commit README.md, docs/requirements.md, docs/design.md (including a small architecture diagram), docs/decisions.md, docs/testing.md, docs/performance.md, docs/ai-usage.md, docs/demo-script.md, and docs/submission-checklist.md. AI usage should record representative prompts, decisions accepted/rejected, human review needs, and checks actually run; exclude private chain-of-thought and secrets. Provide a 3–5 minute demo script, and record a video if recording tools are available. Otherwise explicitly leave video as a submission blocker, not completed by the script.

Before finishing, perform a reviewer-style pass for correctness, security, money semantics, test quality, maintainability, and deployment gaps. Fix concrete issues. Return implemented features, meaningful decisions, verified checks, remaining limitations, commit summary, repository/deployment/video links only when real, and exact remaining submission actions. Do not email the evaluator.
