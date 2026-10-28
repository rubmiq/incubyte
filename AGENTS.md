# Shared engineering rules

## Product and scope
- Read requirements and decisions before editing. Write and commit the one-page requirements before application code.
- Optimize for a small complete HR workflow. Keep optional work out until the core is tested.
- Modern Angular + Java/Spring Boot + PostgreSQL is the chosen stack. Document the assessment's ambiguous AngularJS wording.
- Do not add microservices, event buses, generic plugin systems, CQRS frameworks, or speculative abstractions.

## Working process
- Inspect code and git status first. Preserve unrelated edits. Plan small vertical slices.
- Use official version-matched documentation. Pin compatible dependencies; never invent APIs.
- Prefer descriptive domain names, small cohesive functions, explicit types and clear boundaries. Comments explain reasons or invariants, not syntax.
- Extract abstractions only for a demonstrated need. Avoid both giant services and one-interface-per-class ceremony.
- Handle loading, empty, error, conflict and unauthorized cases as product behavior.
- Each meaningful behavior needs a focused test. Assert outcomes and invariants, not private implementation calls.
- Keep unit tests fast and deterministic; isolate infrastructure tests. Never fix a test by weakening an assertion without a reason.
- Inspect diffs for secrets, dead code, unused dependencies, accidental formatting churn and sensitive logs.
- Commit real progress with relevant tests. Never fabricate history, force-push, or silently skip failed checks.

## Cross-stack contract
- Backend owns authorization, validation, money rules, FX calculations, analytics and concurrency.
- Document API endpoints, DTO shapes, enums, decimal strings, timestamp formats, pagination and error responses in a checked-in OpenAPI contract. Keep contract and code consistent; avoid a redundant second source of truth where generation is practical.
- Use a stable error format with machine code, safe message and field errors. Distinguish 400/422 validation, 401 authentication, 403 authorization, 404 absence and 409 conflict consistently.
- Monetary values use exact decimals on the server and decimal strings over the wire. The browser may format display values but must not produce authoritative totals.
- Use server-side filtering, sorting, pagination and aggregates. Never fetch all 10,000 employees to render a table.
- Limit pagination and use deterministic secondary ordering. Parameterize values; allowlist dynamic field names.
- Version salary writes; current state and immutable history change in the same transaction.

## Security and delivery
- Synthetic data only. No secrets in code, URLs, screenshots, logs or AI usage records.
- Same-origin session authentication with CSRF protection is the default. Never disable protection to make development easier.
- No persistent bearer tokens in browser storage. No wildcard credentialed CORS.
- Reproducible setup, explicit seed command, schema migrations, CI and documented deployment are part of done.
- State what was tested and what remains unverified. Never invent test results, benchmarks, deployment URLs or video links.

## Definition of done
Acceptance criteria met; relevant tests and builds pass; errors and accessibility reviewed; API and documentation updated; focused commit made; limitations honestly recorded.
