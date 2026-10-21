# ADR-0005: Rich DecisionResult instead of boolean

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

`AccessEnforcer.enforce(RequestContext)` currently returns a boolean. That discards everything the engine knows: which policies matched, why access was denied, what obligations apply. Without it, the admin UX can't show "why was this denied?", audit logs are meaningless ("denied" with no cause), and debugging policies in production is guesswork. Real PDPs (XACML, OPA) return structured decisions.

## Decision

The engine returns a rich result object:

```
DecisionResult {
  decision: ALLOW | DENY | NOT_APPLICABLE
  matchedPolicyIds: list of policies that matched and evaluated
  reason: human-readable explanation (which policy, which condition failed)
  obligations: optional post-allow requirements the caller must honor
  diagnostics: evaluation metadata (for actuator/debug endpoints)
}
```

- `AccessEnforcer` exposes convenience `boolean allow(...)` on top; the raw result is always available.
- `AccessDeniedException` carries the full `DecisionResult`.
- Audit events record the result, not just the outcome.

## Consequences

- Breaking API change across `PolicyEngine`/`AccessEnforcer` — done early (M2) while the codebase is small, per the "evolve contracts now" principle.
- Enables the admin interface's "explain this denial" feature and meaningful audit trails.
- `Decision` enum gains precision: engine-level result is ALLOW/DENY/NOT_APPLICABLE per policy; combine step already handles DENY-overrides-ALLOW with default DENY — semantics re-verified in M2 with tests.
