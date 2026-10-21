# ADR-0007: Embedded library first; service mode later via clean engine boundary

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

A3Shield could ship as (a) an embedded Java library linked into the application, or (b) an external policy decision point (PDP) service queried over the network (the OPA/Cerbos model). Embedded means no extra infrastructure, no network hop, trivial local dev; service means polyglot access and central control but real operational burden (deploy, scale, monitor, another failure mode in the hot path). A3Shield's differentiators — annotation DX, migration-based lifecycle, in-process enforcement — are all embedded-native. A future polyglot consumer (a Python service wanting decisions) would need a service mode.

## Decision

- **Embedded library is the primary and only v1 delivery model.** No sidecar, no service dependency, no network hop in the evaluation path.
- The **engine boundary stays clean to enable a later service mode**: `a3-enforcer` (and future `a3-core`, `a3-policy-mgmt`) remain Spring-free and dependency-light; all contracts are serializable-friendly (plain models, no ThreadLocal, no ambient state — see ADR-0006).
- A hypothetical future `a3-server` module would wrap the engine in a REST/gRPC PDP without changing engine contracts. It is **not** on the near roadmap; this ADR only guarantees we don't foreclose it.
- Consequence for horizontal scaling: embedded means every app instance evaluates locally against a shared policy store; runtime admin edits (ADR-0003) therefore require cache invalidation across instances (poll/TTL/pub-sub) — designed when the persistent store lands, not retrofitted.

## Consequences

- Strongest differentiation vs OPA/Cerbos (zero-infra, in-process, Spring-native DX); simplest possible local development.
- Polyglot consumers must wait for service mode.
- Multi-instance deployments need a shared `PolicyStore` + invalidation story before runtime admin editing is safe in clustered prod (tracked in ROADMAP M5).
