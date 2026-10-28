# Decisions — ACME Salary Management

Each entry: decision, alternatives considered, reason.

## Framework interpretation: "AngularJS for Java"

- **Decision**: build with modern Angular (v21), not AngularJS.
- **Alternatives**: implement literal AngularJS 1.x per the brief's wording.
- **Reason**: AngularJS reached end-of-life in January 2022 and has no
  supported tooling, security patches, or ecosystem left; shipping it for
  a 2026 assessment would be actively worse engineering judgment than
  choosing the framework the brief's author almost certainly meant.
  Recorded here and in `docs/requirements.md` for evaluator confirmation
  rather than silently assumed.

## Version matrix (verified against official sources on 2026-10-28)

| Component | Version | Verification source |
|---|---|---|
| Java | 21 LTS | Installed locally (`openjdk 21.0.13`); Spring Boot 4.1.2 requires Java 17+. |
| Spring Boot | 4.1.2 | spring.io/projects/spring-boot (current GA); endoflife.date/spring-boot confirms 3.3/3.4/3.5 are past OSS support as of Oct 2026, so 4.1 is the only "supported stable" line. (Spring Initializr labels this version "4.1.2.RELEASE" in its API; the real Maven Central artifact version is `4.1.2` with no suffix — confirmed via the artifact's `maven-metadata.xml` after the Initializr-labeled version failed to resolve.) |
| Maven | Wrapper, Maven 3.9.x | No system `mvn` installed; Boot 4.1.2 requires Maven 3.6.3+. |
| PostgreSQL | 18 | postgresql.org/support/versioning (current stable major; 19 is RC-only as of Oct 2026). |
| Angular | 21.1.x | Matches globally-installed Angular CLI (21.1.5) and Node 22.18.0. Angular 22 (current latest) requires Node `^22.22.3`, which the installed Node does not satisfy; Angular 21 is confirmed in active/LTS support (not EOL) via angular.dev/reference/releases. |
| Node | 22.18.0 | Installed locally. |
| Spring Security, Spring Data JPA, Flyway | Managed by the Spring Boot 4.1.2 BOM | Will record exact resolved versions once `pom.xml` is generated, to avoid hand-pinning versions that drift from Boot's tested combination. |

Spring Boot 4 / Spring Security 7 is new enough that implementation details
(CSRF token repository wiring in particular) will be checked against the
official Spring Security 7 servlet CSRF reference at the point that code
is written, rather than relied on from memory.

## Reporting currency and FX

- **Decision**: single reporting currency (USD), one fixed, dated, labeled
  DEMO FX snapshot table seeded by migration; conversion direction is
  "local currency amount × rate_to_usd = USD amount"; rounding to 2 decimal
  places with `RoundingMode.HALF_UP` at the point of conversion, before
  aggregation; median uses SQL `percentile_cont(0.5)` (interpolated/averaged
  for even counts); a filter combination with zero active employees returns
  zero headcount and null mean/median (not zero), with `resultCount: 0`
  explicit in the response so the UI can show an empty state rather than a
  misleading "$0 average."
- **Reason**: avoids ever silently summing mixed currencies, makes the FX
  basis auditable, and makes the empty-result behavior a visible product
  decision rather than an accidental divide-by-zero or a misleading zero.

## Display timezone

- **Decision**: UTC, stored as `Instant`/`TIMESTAMPTZ`, displayed as UTC in
  the Angular UI with an explicit "UTC" label on timestamps.
- **Reason**: no organization timezone was specified in the brief; UTC is
  the only choice that doesn't silently assume a specific HR office
  location. Documented as an assumption, not hidden.

## Concurrency strategy

- **Decision**: JPA `@Version` optimistic locking on `compensation`,
  client must submit the last-seen version, mismatch → `409` with current
  server state attached.
- **Alternatives**: pessimistic row locks; last-write-wins.
- **Reason**: HR edits are infrequent and interactive (not high-contention
  writers), so optimistic locking avoids held locks across a user think-time
  gap, while still giving a correct, recoverable conflict instead of
  silent last-write-wins, which the brief explicitly forbids.

## Deployment shape

- **Decision**: same-origin deployment (Angular static build served by/with
  the Spring app or a shared reverse proxy in Docker Compose), server-side
  sessions with HttpOnly cookies, no bearer tokens in browser storage.
- **Reason**: eliminates an entire class of CORS/CSRF/token-storage
  mistakes the brief explicitly warns against, and matches
  `backend/AGENTS.md`'s same-origin session requirement.

## Spring Boot 4 package relocations (verified against resolved jars)

- **Finding**: Spring Boot 4.1.2 defaults to **Jackson 3** (`tools.jackson.databind.ObjectMapper`,
  artifact `tools.jackson.core:jackson-databind:3.1.7`) as the autoconfigured
  `ObjectMapper` bean, not the legacy `com.fasterxml.jackson.databind.ObjectMapper`
  (`com.fasterxml.jackson.core:jackson-databind:2.21.7`, also present on the
  classpath but not the wired bean). `@WebMvcTest` also moved package, to
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (artifact
  `spring-boot-webmvc-test`).
- **Verification**: both were confirmed by inspecting the actual resolved
  `.m2` jars (`javap`/`unzip -l`) after compile/test failures pointed at the
  old package names, rather than assumed from memory, per this project's
  "never invent APIs" rule.
- **Why it matters**: any future Jackson-dependent code or test in this
  repo (DTO (de)serialization tests, custom `ObjectMapper` customizers) must
  import from `tools.jackson.*`, not `com.fasterxml.jackson.*`, or it will
  fail to find the autoconfigured bean.

## SPA fallback routing implementation

- **Decision**: serve the Angular build via a `WebMvcConfigurer.addResourceHandlers()`
  override (`/**` → `classpath:/static/`) with a custom `PathResourceResolver`
  that returns the real file when present, `null` (→ 404) for unmatched
  `api/`/`actuator/`-prefixed paths, and `index.html` otherwise.
- **Alternatives tried and rejected**: a `ViewControllerRegistry` regex-capture
  pattern (e.g. `/{path:^(?!api|actuator).*}/**`) — rejected after hitting two
  separate `PathPattern` syntax errors: a capture-group name cannot be reused
  twice in one pattern, and `**`/`{*...}` elements are only legal at the very
  start or end of a pattern, never spanning a middle segment. Neither
  restriction has a regex workaround within `PathPattern`.
- **Reason**: resource resolution isn't subject to `PathPattern`'s
  path-variable/wildcard placement rules, so it's the only approach that
  correctly forwards arbitrary-depth deep links (e.g. `/employees/42`) to
  the SPA shell while still serving real static assets and returning genuine
  404s for unmatched API routes.

## CSV import/export

- **Decision**: omitted from the MVP.
- **Reason**: the brief marks it optional and "only after the core is
  complete"; the 10,000-employee seed already demonstrates bulk data
  presence, and a real onboarding migration would be a one-time, reviewed,
  staged operation rather than a recurring self-service upload — building
  that endpoint would add surface area (validation, partial-failure
  semantics, injection-safe parsing) without a corresponding MVP
  requirement.
