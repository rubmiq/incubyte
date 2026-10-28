# Requirements — ACME Salary Management

## Goal

HR at ACME currently manages salaries for 10,000 employees across multiple
countries in spreadsheets. This system replaces that spreadsheet with a
reliable, auditable web application so HR can find any employee, see and
change their compensation with confidence, and answer basic payroll
questions for leadership without manual reconciliation.

## Persona

**HR administrator** (single role, `ROLE_HR`): searches/filters the
directory, edits employee profiles, records salary changes with a reason,
and reads the compensation dashboard. No employee self-service and no
manager/approver role exist in this MVP.

## Scope

- Secure HR login/logout (server-side sessions, CSRF-protected).
- Employee directory: server-side search (name/email/employee code),
  filters (country, department, status), allowlisted sorting, pagination;
  filters persist in the URL.
- Employee lifecycle: create, view, edit profile, archive, reactivate.
  Employees are never hard-deleted.
- Compensation: view/update current gross **annual base** salary in the
  employee's local currency, with a required change reason; full,
  immutable, chronological audit history per employee.
- Dashboard: active headcount, total annual base payroll, mean and median
  salary (reported in one fixed reporting currency, USD, via a labeled demo
  FX snapshot), and breakdowns by country and department — computed over
  the full filtered population, not just the visible page.
- Deterministic seed of exactly 10,000 synthetic employees across
  countries, departments, statuses and currencies, each with at least one
  salary-history entry.

## Acceptance criteria

- An unauthenticated request to any employee/compensation/analytics
  endpoint returns 401; an authenticated non-HR request (none exist yet,
  but the check must exist) returns 403.
- Directory queries never load the full table; every list response is
  paged, server-sorted on an allowlisted field, and server-filtered.
- A salary update is atomic: the current value and a new history row
  change together or not at all; a stale update (wrong expected version)
  is rejected with 409 and no history is written.
- Dashboard numbers match hand-computed values on small fixed fixtures,
  including even-count median and empty-result cases.
- `docker compose up` on a clean checkout produces a working app with
  migrations applied and a seed command available, with no manual SQL.

## Assumptions

- **"AngularJS for Java"** in the assessment brief is interpreted as
  **modern Angular**, not legacy AngularJS (1.x). AngularJS reached end of
  life in January 2022 and is not viable for new work; this interpretation
  is flagged here for evaluator confirmation rather than silently assumed.
- **Display timezone is UTC.** No organization-specific timezone was given,
  so all audit timestamps are stored and shown in UTC; this is a visible,
  documented placeholder, not a hidden guess.
- One fixed, clearly labeled **demo FX snapshot** (local currency → USD) is
  used for reporting-currency aggregates; it is not live and not a
  real-time payroll figure.
- Demo HR credentials are environment-provided, not hardcoded.

## Explicit exclusions (and why)

- **Payroll execution, taxes, benefits, bonuses/equity, approvals** — these
  are separate systems with their own compliance rules; including them
  would dilute a small, reliably correct submission.
- **Employee self-service, multi-tenancy** — out of scope for a single HR
  admin workflow; adds auth/isolation complexity with no MVP value.
- **Scheduled/backdated compensation, real-time FX** — both require
  temporal/versioned effective-dating and external rate feeds that the
  brief does not ask for; MVP salary changes are effective immediately.
- **Statistical pay-equity claims, natural-language AI chat** — both imply
  guarantees (fairness analysis, conversational correctness) this system
  does not and should not claim.
- **CSV import/export** — optional, only after the core is tested; if not
  delivered, the assumption is that employee onboarding is a staged
  migration performed once via the seed/admin tooling, not a recurring
  bulk-upload feature.
