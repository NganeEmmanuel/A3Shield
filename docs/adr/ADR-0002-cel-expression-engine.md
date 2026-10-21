# ADR-0002: CEL as the condition expression engine

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

The current engine uses a hand-rolled DSL (`ExpressionParser` + `DslConditionEvaluator`) supporting `==`, `!=`, `>`, `<`, `AND`/`OR` with ANY-match semantics for list values. It is well-tested, but owning an expression language is an open-ended liability: operator precedence and parentheses are unsupported, string literals and type coercion semantics are undefined, and tooling/linting/IDE support would all be homemade. Runtime-admin-edited policies (ADR-0003) make expressions effectively *user input*, raising the bar for sandboxing, limits, and evaluation deadlines.

Options considered:
1. Keep growing the custom DSL — full ownership, zero deps, unbounded maintenance and security surface.
2. SpEL — Spring-native, but Spring-bound (conflicts with the framework-agnostic enforcer) and historically prone to injection footguns.
3. **CEL** (Common Expression Language; `org.projectnessie.cel:cel-java) — non-Turing-complete, sandboxed, deadline-capable, battle-tested in Kubernetes and Envoy, portable across the industry.
4. Delegate to an external PDP (OPA/Cerbos) — rejected here, see ADR-0007.

## Decision

Adopt **CEL** as the evaluation engine, hidden behind the existing `ConditionEvaluator` SPI:

- `a3-enforcer` gets a `CelConditionEvaluator`; the enforcer module stays Spring-free but may depend on `cel-java`.
- The current DSL syntax remains available as a thin "simple syntax" facade for trivial conditions (`resource.ownerId == subject.id` — simple equality expressions are shape-identical in CEL, so the facade is mostly a passthrough). The facade is optional sugar, not the primary language; new capabilities (parentheses, `in`, string ops, macros like `.exists()`) are CEL-only.
- Custom A3 functions are registered into the CEL environment (e.g. `related()` for ReBAC, ADR-0008), so the expression language is the extension point.
- Evaluation gets a deadline/complexity budget so admin-authored policies cannot hang a request.

## Consequences

- We stop owning a parser; precedence, coercion, and tooling come from CEL.
- New dependency (`cel-java`) in the enforcer; DSL parser tests either become facade tests or are retired once the facade's coverage is proven.
- Policies written in the simple syntax keep working; documentation must teach CEL as the canonical language.
- ANY-match list semantics must be re-expressed with CEL macros (`.exists()`) — the facade can translate the old behavior for compat.
