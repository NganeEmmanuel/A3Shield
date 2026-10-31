# ADR-0011: Policy cache invalidation — poll-based state version

- **Status:** Accepted
- **Date:** 2026-10-31

## Context

Embedded deployments (ADR-0007) mean every app instance evaluates against a shared policy store. A runtime admin edit on one instance must become visible on the others, or different instances enforce different policy sets — unacceptable. Options considered:

1. **TTL-only refresh** — every instance reloads every N seconds regardless of changes; simple but wasteful and gives stale windows even when nothing changed.
2. **Poll-based state version (chosen)** — the store maintains a monotonic `stateVersion` bumped on every mutation; cached repositories cheaply poll the version (throttled, default every 5s) and reload only when it changed.
3. **Pub/sub invalidation** (Redis, Kafka, Postgres LISTEN/NOTIFY) — instant propagation but adds infrastructure and another failure mode; premature before multi-instance deployments exist.

## Decision

`EffectivePolicyStore.stateVersion()` is part of the SPI contract; every implementation must bump it on every mutation. `StoreBackedPolicyRepository` polls the version at most once per poll interval (configurable, default 5s) and atomically swaps its snapshot when the version changed. Local mutations can call `invalidate()` for immediate refresh.

## Consequences

- No external infrastructure needed; works for the in-memory store (single instance) and the JDBC store (shared database) alike.
- Worst-case staleness equals the poll interval; the version check is a single cheap query.
- Instant cross-instance invalidation remains open: a pub/sub escalation path can be added later behind the same SPI (e.g., a bump triggered by a message listener) without changing call sites. Revisit when real multi-instance latency requirements exist.
