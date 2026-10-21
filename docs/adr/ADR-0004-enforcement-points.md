# ADR-0004: Three enforcement points, one engine

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

Method annotations alone miss two real enforcement moments: HTTP routes (before controller selection is even relevant) and response data (the *data* leaving the system, not just the *request* entering it). The current starter has annotation aspects only, and the post-execution check (`@A3SecurePolicy`) is all-or-nothing — the whole result is denied or returned, with no per-row filtering.

## Decision

One evaluation engine, three enforcement points in the starter:

1. **Route-level** — a `HandlerInterceptor`/filter builds an Intent context (route, HTTP method, parameters) and evaluates before the handler runs. Covers whole-endpoint rules without touching controller code.
2. **Method-level annotations** — `@A3Authorize`, `@A3HasRoles`, `@A3HasPermissions`, `@A3Intent` (existing), hardened. Pre-execution, intent-based.
3. **Post-execution data check** — `@A3SecurePolicy` evolves from all-or-nothing to **per-row filtering**: for collection returns, each element is evaluated and non-permitted rows are filtered out; for single-object returns it remains deny-or-allow. Filtering vs partial-deny semantics (silent filtering must be documented; an option to deny when any row fails can be configured).

All three feed the same engine, produce a `DecisionResult` (ADR-0005), and emit audit events.

## Consequences

- Per-row filtering is the row-level-security story and a genuine differentiator; it has a performance cost (N evaluations — mitigated by expression caching, and later by batched/bulk checks).
- Silent filtering can surprise developers; the default must be clearly documented, with the strict mode available.
- Known bug to fix in M1: `A3SecurePolicyAspect` currently wraps `AccessDeniedException` in `RuntimeException` (typed denials never reach callers) and returns the transformed list rather than the raw result.
- Intent (pre-execution) vs Target (post-execution) becomes the documented mental model of the library.
