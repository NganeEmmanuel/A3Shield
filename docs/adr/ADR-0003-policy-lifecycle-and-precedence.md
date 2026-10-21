# ADR-0003: Policy lifecycle — file migrations + runtime admin, precedence & drift

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

Two authoring modes are core to the product vision:
1. **Developer-authored policies, versioned in the repo** — reviewed, promoted through environments with the code.
2. **Admin-edited policies at runtime** — business rules change after go-live without redeployment.

Together they create two sources of truth. The failure mode is drift: an admin edits a policy in prod; the next deploy's migration re-applies the old definition, silently reverting the business rule — or the migration is skipped and prod diverges from what's in git forever. Retrofitting precedence after both features exist is miserable; this must be designed first.

## Decision

**Policy migrations, Flyway-style, as the baseline mechanism:**

- Policy files live in `src/main/resources/a3/policies/`, named `V<n>__<name>.<ext>` (format: JSON or YAML — open question, decided at M4 kickoff).
- Each file is ordered, versioned, and checksummed. A `PolicyMigrationRunner` applies pending migrations at startup into the effective `PolicyStore`, recording applied versions + checksums (JDBC table or equivalent in the store).
- Each file declares its tenant scope in its header (ADR-0009) and may carry **policy tests** (`given subject X, resource Y, action Z → ALLOW`) executed at apply time; a migration whose tests fail aborts startup.

**Runtime admin edits, with guardrails, on the effective store:**

- A `PolicyAdminApi` (library SPI; REST exposure in the starter) allows create/update/delete/enable-disable on live policies.
- Every mutation is validated by running the policy test suite **before** commit, and written to an **audit log** (who, when, full before/after diff).

**Precedence and drift:**

- The effective store (DB) is the truth at evaluation time. Migrations define baselines and introduce *new* versions.
- On startup, if a pending migration conflicts with an admin-modified policy, the migration's declared `onConflict` applies: `FAIL` (default — startup error, human resolves), `OVERWRITE` (dev explicitly resets the admin edit), or `SKIP` (admin edit wins, drift recorded).
- A drift report (file vs store differences) is exposed via an endpoint/actuator so operators can see where git and prod disagree.

## Consequences

- Startup ordering and failure semantics become explicit and testable; a bad migration can (by default) stop the deploy, which is the safe default.
- Admin editing is credible for production use because it's tested-before-applied and fully audited.
- Policy tests become a first-class artifact — the feature that makes both migrations and runtime editing safe.
- We need a real persistent `PolicyStore` (JDBC at minimum) before runtime editing ships; the in-memory repository remains for tests/embedded-simple usage.
- Flyway analogy is intentional but scope is narrower: forward-only, no down-migrations; rollback happens via new migrations or admin revert (audited).
