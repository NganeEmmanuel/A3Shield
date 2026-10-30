# Architecture Decision Records

ADRs capture **why** A3Shield is built the way it is. Each ADR is one decision, numbered, immutable once accepted.

## Process

1. Copy `_template.md` → `ADR-00NN-short-name.md` (next free number).
2. Write it while the decision is being discussed, status `Proposed`.
3. When the owner decides, set status `Accepted` (or `Rejected` — rejected ADRs are kept; the reasoning is valuable).
4. **Accepted ADRs are never edited.** If a decision changes, write a new ADR that references and supersedes the old one; flip the old one's status to `Superseded by ADR-00XX`.

## Reading order for new sessions

`ADR-0001` (scope) → `ADR-0007` (embedded-first) → `ADR-0009` (multi-tenancy) → `ADR-0003` (policy lifecycle) → the rest as relevant.

Never implement against a superseded or rejected ADR.

| ADR | Title | Status |
|-----|-------|--------|
| [0001](ADR-0001-authorization-first-scope.md) | Authorization-first scope; authn via integration | Accepted |
| [0002](ADR-0002-cel-expression-engine.md) | CEL as the condition expression engine | Accepted |
| [0003](ADR-0003-policy-lifecycle-and-precedence.md) | Policy lifecycle: file migrations + runtime admin, precedence & drift | Accepted |
| [0004](ADR-0004-enforcement-points.md) | Three enforcement points, one engine (route / annotation / post-execution) | Accepted |
| [0005](ADR-0005-decision-result-model.md) | Rich DecisionResult instead of boolean | Accepted |
| [0006](ADR-0006-concurrency-model.md) | Servlet thread-per-request now; ThreadLocal only at the starter edge | Accepted |
| [0007](ADR-0007-embedded-first-engine-boundary.md) | Embedded library first; service mode later via clean engine boundary | Accepted |
| [0008](ADR-0008-rebac-readiness.md) | ReBAC: reserve contracts now, implement later | Accepted |
| [0009](ADR-0009-multi-tenant-day-one.md) | Multi-tenancy is a day-one constraint | Accepted |
| [0010](ADR-0010-yaml-policy-format.md) | Policy migration files use YAML | Accepted |
