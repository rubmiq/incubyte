# Design — ACME Salary Management

## Architecture overview

Same-origin modular monolith: Angular SPA calls `/api/**` on the same host
as the Spring Boot app (reverse-proxied together in Docker Compose), so
session cookies and CSRF cookies work without CORS or bearer tokens in
browser storage.

```
                        ┌─────────────────────────────┐
                        │        Browser (HR)         │
                        │   Angular 21 SPA (Material)  │
                        └──────────────┬───────────────┘
                                       │ same-origin, cookies
                                       ▼
                        ┌─────────────────────────────┐
                        │   Spring Boot 4 (Tomcat)     │
                        │  Spring Security 7 (session, │
                        │   CSRF, ROLE_HR)             │
                        │ ┌─────────┬─────────┬───────┐│
                        │ │ auth    │employee │compen- ││
                        │ │         │         │sation  ││
                        │ ├─────────┼─────────┼───────┤│
                        │ │       analytics            ││
                        │ ├─────────────────────────────┤│
                        │ │        common (errors,     ││
                        │ │  pagination, Clock, config) ││
                        │ └─────────────────────────────┘│
                        └──────────────┬───────────────┘
                                       │ JDBC (Spring Data JPA)
                                       ▼
                        ┌─────────────────────────────┐
                        │      PostgreSQL 18           │
                        │ employee / compensation /    │
                        │ compensation_history /        │
                        │ fx_rate_snapshot / hr_user    │
                        └─────────────────────────────┘
```

## Backend package layout

`com.acme.hr`
- `auth` — login/logout controller, Spring Security config, session/CSRF behavior.
- `employee` — `api` (controller, request/response DTOs), `application`
  (directory search service, lifecycle service), `persistence` (JPA entity,
  repository with spec/criteria-based filtering).
- `compensation` — current-salary read/update service (transactional,
  optimistic-locked), history repository (insert-only), currency metadata
  (scale/bounds allowlist).
- `analytics` — aggregate queries (headcount, payroll sum, mean, median,
  breakdowns) sharing the employee module's filter semantics.
- `common` — `ApiError`/`ProblemDetail` mapping, pagination/sort-allowlist
  helpers, `Clock` bean, Flyway/db config, seed command.

Controllers depend on application services; application services depend on
repositories; no controller touches a repository directly, and no JPA
entity is returned from a controller.

## Frontend feature layout

`src/app`
- `auth/` — login page, session state (signal-based), auth HTTP interceptor.
- `employees/` — directory (table + filters + paginator), employee
  detail/edit forms, create flow.
- `compensation/` — salary view/edit within employee detail, history list,
  conflict dialog.
- `analytics/` — dashboard page, chart + table equivalents.
- `shared/` — generic Material wrappers, money/date pipes.
- `core/` — HTTP error interceptor, CSRF/XSRF wiring, route guards.

Each feature is a lazy-loaded standalone route group; API access goes
through a typed `*ApiService` per feature returning explicit DTOs.

## Data model

- `employee(id, employee_code UNIQUE, first_name, last_name, email UNIQUE,
  country, department, status, created_at, updated_at)`
- `compensation(employee_id PK/FK, local_amount NUMERIC, currency CHAR(3),
  version BIGINT, updated_at, updated_by)` — one row per employee, current
  state only.
- `compensation_history(id, employee_id FK, previous_amount,
  previous_currency, new_amount, new_currency, reason, changed_by,
  changed_at TIMESTAMPTZ)` — append-only, no update/delete endpoint.
- `fx_rate_snapshot(currency CHAR(3) PK, rate_to_usd NUMERIC, snapshot_date,
  label)` — seeded by migration, read-only at runtime.
- `hr_user(id, username UNIQUE, password_hash, created_at)` — single role,
  so no separate roles table in the MVP.

## Concurrency and audit

A salary update request carries the client's last-seen `version`. The
service loads the `compensation` row, compares/applies via JPA optimistic
locking (`@Version`), and in the same transaction inserts a
`compensation_history` row. If the version doesn't match, JPA raises an
optimistic-lock failure, the transaction rolls back (no history row is
written), and the controller maps it to `409` with the current server
state so the UI can show a conflict and let HR re-review before retrying.

## Analytics semantics

Aggregates run as SQL over `employee ⋈ compensation` restricted to
`status = ACTIVE` and the same country/department filters as the
directory, converting each local amount to USD using the single
`fx_rate_snapshot` row for that currency. The API response always includes
the reporting currency (USD), the FX snapshot date, and the matching
result count, so the UI can state the basis instead of implying a live
figure. Median is computed in SQL (`percentile_cont(0.5)`), which correctly
averages the two middle values for an even count.
