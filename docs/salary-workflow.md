# Salary workflow

HR can view, establish and change an employee's current gross annual base salary
from employee details, with paginated, read-only history. Archived employees are
read-only. The supported currencies are USD, GBP, EUR, INR, BRL, AUD, CAD and SGD;
all have two fractional digits. Currency is explicit, independent of country.
Amounts are positive decimal strings from 0.01 to 999999999.99; no exponents,
grouping separators or rounding of input. Changes take effect immediately in UTC.
A nonblank reason (at most 500 characters) is mandatory. Unchanged salary writes
are rejected rather than creating misleading history.

GET `/api/employees/{code}/salary` returns current amount/currency, version,
effective date and update timestamp, or 204 when the employee has no salary.
PUT at the same path accepts amount, currency, changeReason and expectedVersion.
An explicit null/absent expectedVersion establishes salary only when none exists;
existing salary requires its current version. Concurrent/stale writes return 409.
History GET `/api/employees/{code}/salary/history?page=0&size=20` returns the standard
page DTO, newest timestamp first with descending ID as a deterministic tie-break.
Page must be nonnegative and size 1–100. History exposes before/after values,
actor, reason and UTC timestamp; no update/delete endpoints exist.

The backend locks the employee row during writes to serialize initialization and
coordinate with lifecycle changes, checks the supplied salary version, and uses
JPA optimistic versioning. Current salary and immutable audit append commit in
one transaction. New migration columns preserve older audit rows as-is: absent
previous values and actor are displayed as unknown, never backfilled with guesses.
A database trigger rejects history updates/deletes. Existing seeding inserts stay
compatible. API authentication, HR permission and CSRF protection remain enabled.

The UI preserves entered values on errors, disables duplicate submits, and shows
field errors and success. A conflict blocks resubmission until the user explicitly
refreshes current salary and reviews their preserved draft. History has independent
loading, empty, error/retry and paging states. Money stays a string in forms/wire
DTOs; only display formatting may convert it.

Acceptance checks cover validation, string serialization, authorization/CSRF,
initial salary, successful updates, before/after audit data, stale/competing writes,
audit-failure rollback, history immutability/paging, archived/missing employees,
and UI conflict/retry/validation behavior. PostgreSQL infrastructure checks use
isolated fixtures; Docker limitations and actual results will be recorded.
