# ACME Salary Management

A small, auditable HR salary-management system: searchable employee
directory, versioned compensation history, and a filtered payroll
dashboard — built for ~10,000 synthetic employees across countries and
currencies.

Stack: Java 21 + Spring Boot 4.1.2 (Spring MVC, Spring Security, Spring
Data JPA, Flyway) · Angular 21 (standalone, strict TypeScript, Angular
Material) · PostgreSQL 18. See `docs/requirements.md` for product scope,
`docs/design.md` for architecture, and `docs/decisions.md` for the
version/rationale log.

## Run it (Docker, fresh clone)

Requires Docker with Compose v2.

```bash
cp .env.example .env     # edit values if you want non-default local credentials
docker compose up --build
```

This builds the Angular production bundle, embeds it into the Spring Boot
image as static resources (same-origin deployment — one process, one
origin, no CORS), starts PostgreSQL with a persistent named volume, and
applies Flyway migrations on boot. The app is served at
`http://localhost:8080`.

Seeding 10,000 synthetic employees is an explicit, opt-in operation (never
a side effect of a normal boot) — see `docs/design.md` / `docs/decisions.md`
for the seed command once the seeding slice lands.

## Local development (without Docker)

Backend (Java 21, no system Maven required — uses the Maven Wrapper):

```bash
cd backend
./mvnw spring-boot:run
```

Point it at a local PostgreSQL 18 (e.g. the `postgres` service from
`docker compose up postgres`) via the `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, `DB_PASSWORD` environment variables, plus `DEMO_HR_USERNAME`
and `DEMO_HR_PASSWORD` for the seeded demo account.

Frontend (Node 22, Angular CLI 21):

```bash
cd frontend
npm install
npm start   # serves on http://localhost:4200, proxies /api to :8080
```

## Tests

```bash
cd backend && ./mvnw verify     # JUnit/AssertJ + Testcontainers PostgreSQL integration tests
cd frontend && npm test         # Vitest-based unit tests
cd frontend && npm run build    # strict production build
```

## Status

This repository is being built in reviewable vertical slices, each ending
in a commit; see `docs/decisions.md` and commit history for what has
landed so far and what remains.
