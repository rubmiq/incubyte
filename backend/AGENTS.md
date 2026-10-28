# Java / Spring Boot engineering guide

Read ../AGENTS.md first. Applies to backend/.

## Architecture and Java
- Choose a supported Java LTS and compatible stable Spring Boot release; verify official compatibility. Use Maven Wrapper and the Boot dependency management BOM.
- Build a Spring MVC modular monolith, packaged by feature (employee, compensation, analytics, auth) with explicit API/application/persistence boundaries where useful. No WebFlux for blocking JPA.
- Constructor injection only. Prefer immutable DTOs, including Java records when appropriate. Do not serialize JPA entities or let request binding mutate them.
- Use clear application services for use cases. Controllers translate HTTP and delegate; repositories query persistence. Avoid unnecessary service interfaces and generic CRUD frameworks.
- Prefer explicit mapping for this small application. Add mapping libraries only with a documented benefit.
- Keep nullability and Optional usage deliberate. Use typed domain errors; never catch Exception and return success or null.

## Persistence and transactions
- Flyway owns versioned migrations. Validate schema on startup; do not use production ddl-auto=update/create. Never edit already-applied migrations to change deployed schema.
- Use PostgreSQL consistently in runtime and integration tests. Define foreign keys, uniqueness, NOT NULL and check constraints for key invariants.
- Define transaction boundaries on application services. Salary mutation plus audit append is one transaction. History has no ordinary update/delete API.
- Use @Version or an equivalent atomic conditional update and require the client's expected version. Map optimistic locking failures to 409. Explain the chosen concurrency behavior and test competing/stale writes.
- Use BigDecimal and NUMERIC precision/scale with explicit currency scale and RoundingMode. No double/float arithmetic for salaries, FX or authoritative aggregate values.
- Inject Clock for auditable timestamps and deterministic tests. UTC Instant for audit timestamps; distinguish date-only business values.
- Avoid lazy serialization, N+1 queries and broad eager fetching. Use projections/targeted fetches; do not paginate a collection fetch join. Disable open-session-in-view and materialize DTOs within appropriate boundaries.
- Analytics run in SQL or bounded database queries over the full filtered population. Share filter semantics, not a giant universal query framework.
- Add indexes based on actual filter/order/query patterns and inspect query plans. Do not promise indexed substring search without verifying its plan.

## API and security
- Bean Validation on request DTOs plus domain validation in services. Enforce lengths, enums, currency precision, bounds, pagination caps and sort allowlists.
- Provide consistent ProblemDetail-style errors with safe messages, code and field details. Never return stack traces, SQL or sensitive data.
- Use Spring Security with server-side sessions and encoded passwords. Protect every employee, salary, history and analytics endpoint with the HR permission.
- Configure CSRF for Angular SPA integration using the selected Spring Security version's official guidance, including obtaining/renewing tokens after login/logout. Do not disable CSRF for cookie-authenticated APIs.
- Require POST logout, invalidate sessions, use secure HttpOnly session cookies in HTTPS production. Distinguish the readable CSRF token cookie from the session cookie.
- Return API 401/403 responses rather than HTML login redirects. Keep CORS absent for same-origin deployment or narrowly configured if necessary.
- Never log passwords, session IDs, full salary payloads or personal records. Expose only necessary health information; secure other management endpoints.

## Seeding and operations
- Explicit demo-only seed profile/command, seeded RNG, fixed clock and stable employee codes. Exactly 10,000 employees, bounded/batched inserts and meaningful distributions.
- Define safe rerun behavior: a fresh demo database produces exactly 10,000; a repeated seed either makes no changes or exits clearly. Never delete application data on normal startup.
- Demo credentials are environment-provided; seed only synthetic records. Document counts, sample filters, salary ranges and FX snapshot provenance as synthetic.
- Keep secrets out of application.yml and Docker images. Use external environment configuration, persistent PostgreSQL storage and graceful health checks.

## Testing and checks
- JUnit Jupiter and AssertJ for pure domain tests; Mockito only at genuine boundaries. Avoid loading Spring for arithmetic or simple business rules.
- Focused MVC tests for validation, serialization, auth/authorization and CSRF, with security filters enabled. Use selected Boot version's test packages/annotations.
- Testcontainers PostgreSQL for migrations, constraints, repository filtering, exact aggregate queries, concurrency and transactional rollback. No H2 approximation for these behaviors.
- Explicitly verify that a failed audit write rolls back the salary mutation and a stale update creates no history. Do not rely only on mocked service tests.
- Small fixed fixtures for money/FX/median results. Cover odd/even and empty populations, excluded inactive employees, missing FX, unsupported currencies and rounding boundaries.
- Keep database fixtures isolated; never seed 10,000 records for every unit test. A dedicated seed/performance check verifies the large dataset.
- Run Maven verification and the chosen formatter/static analysis in CI; configure tools before documenting commands. Report failures honestly and resolve meaningful warnings.

References: https://docs.spring.io/spring-boot/reference/ ; https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html ; consult version-matched Spring Data JPA, Flyway and PostgreSQL documentation.
