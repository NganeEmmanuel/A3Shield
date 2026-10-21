# ADR-0006: Concurrency model — servlet thread-per-request now; ThreadLocal only at the starter edge

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

The starter uses ThreadLocal holders (`RequestContextHolder`, `TargetContextHolder`, …) to move context between filters, interceptors, and aspects. This works for servlet thread-per-request and virtual threads, but breaks under reactive stacks (WebFlux) or explicit async hand-offs, where work hops threads. Supporting reactive properly means Reactor `Context` propagation throughout — significant complexity for an audience we haven't validated yet.

## Decision

- **Target for v0.x: servlet, thread-per-request** (virtual threads fully supported). WebFlux is explicitly out of scope and documented as unsupported.
- The **engine** (`a3-enforcer`) never touches ThreadLocal: it receives an explicit `RequestContext` argument everywhere. ThreadLocal exists only in the starter as the *edge* mechanism that assembles the context during the request.
- Holders are populated/cleared at the request boundary (filter/interceptor) with try/finally guarantees.
- If reactive support is ever added, it will be a new ADR and likely a parallel context-propagation strategy in the starter; the engine needs no change because it is context-transport-agnostic.

## Consequences

- Simpler code today; no premature reactive complexity.
- Documented limitation; WebFlux users cannot adopt the starter yet.
- Discipline required: no new engine-side code may read ThreadLocal (enforced by the enforcer having no Spring/servlet deps at all).
