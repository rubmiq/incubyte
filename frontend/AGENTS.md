# Angular engineering guide

Read ../AGENTS.md first. Applies to frontend/.

## Structure and types
- Use a supported stable Angular release compatible with Node, TypeScript and Angular Material. Follow that release's CLI conventions; record versions.
- Use standalone components, strict TypeScript and strict template checks. No unreasoned any, unsafe casts, non-null assertions or ignored compiler errors.
- Organize by feature: auth/, employees/, compensation/, analytics/. Keep generic UI in shared/ and app-wide infrastructure in core/ only when truly shared.
- Co-locate tests. Lazy-load feature routes. Keep components focused on presentation and orchestration; API adapters own HTTP details.
- Prefer readonly data and explicit DTO/view-model boundaries. Avoid a generic BaseComponent or giant shared service.

## Reactivity and state
- Prefer signals/computed for synchronous local and derived UI state. Use RxJS for asynchronous composition and cancellation. Do not duplicate derived state or use effects to copy values unnecessarily.
- Use OnPush change detection where appropriate for the selected version and maintain immutable updates.
- Search uses debounce, distinct values and switchMap so stale results cannot replace newer results. Reset pagination when filters change and preserve URL state.
- Prefer async pipe or supported signal interop; use takeUntilDestroyed for necessary long-lived subscriptions. Never nest subscriptions or leak manual subscriptions.
- Keep state at the narrowest useful scope. Do not add NgRx unless concrete cross-feature complexity justifies it in a decision note.

## Forms and HTTP
- Use typed reactive forms with explicit defaults and domain-aware validation. Server validation remains authoritative.
- Show field errors near controls and global failures near the action. Preserve user input after failures; disable duplicate submits.
- Represent monetary inputs/DTO amounts as decimal strings. Validate syntax, precision and bounds without lossy parseFloat round-trips. Format display with supported currency metadata.
- Typed HttpClient services return explicit DTOs. Centralize session/error infrastructure without hiding feature-specific error handling.
- Use relative /api URLs and local development proxy. Align Angular XSRF cookie/header behavior with Spring Security; test authentication, logout and token renewal end-to-end.
- A 409 must show a clear conflict and a refresh/review path. Never auto-resubmit stale salary edits.
- Route guards improve navigation; the backend enforces access. Clear user-specific state on logout and handle expired sessions.

## UI quality and safety
- Use Angular Material consistently with a restrained visual system, responsive layouts and readable tables. Prioritize usable HR workflows over decorative dashboards.
- Semantic HTML, labeled fields, keyboard access, visible focus, accessible dialogs, meaningful table headers and errors, sufficient contrast. Never rely only on color.
- Include loading, empty, no-filter-results, retryable error, success and conflict states. Announce relevant async status accessibly.
- Use stable identity in template tracking and the modern control-flow syntax supported by the version. Avoid expensive template methods and uncontrolled DOM access.
- Never bypass Angular sanitization for convenience or interpolate user data into executable templates. No secrets in frontend configuration.
- Charts include text/table equivalents and visible definitions for currency, population and FX date.

## Tests and checks
- Use the CLI-supported test runner, Angular TestBed and HTTP testing utilities. Mock HTTP boundaries; test user-visible behavior.
- Cover form errors, API error mapping, cancellation/latest-result behavior, filter/page changes, monetary string handling and conflict recovery.
- Use fake timers/test schedulers where needed; no real sleeps. Keep most tests focused rather than bootstrapping the whole app.
- Add a small Playwright suite for complete HR journeys with stable accessible selectors and isolated deterministic data.
- Run formatting/linting, unit tests and strict production build. Record actual commands in README/CI rather than assuming scripts exist.

References: https://angular.dev/style-guide ; https://angular.dev/best-practices/security ; use version-matched Angular documentation for forms, signals and testing.
