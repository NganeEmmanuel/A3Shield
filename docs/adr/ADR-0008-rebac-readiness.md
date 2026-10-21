# ADR-0008: ReBAC — reserve contracts now, implement later

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

Relationship-based access control (Zanzibar/OpenFGA/SpiceDB style: "subject is `editor` of the *parent folder* of this document") is the fourth access model. It is architecturally invasive: relationship checks need a graph store, a `Check(subject, relation, resource)` evaluation path, and cyclic-expansion semantics. Retrofitting resource identity and relation hooks after ABAC is entrenched forces breaking redesigns of the policy format and matcher. Decision: we do not implement ReBAC now, but we **reserve the contracts** so a later implementation is additive, not breaking.

## Decision

1. **Resource identity convention, effective immediately:** every authorizable resource is addressable as `type:id` (e.g. `folder:42`, `document:7`). `Target` already carries `id` + `type`; `Intent` gains `id` where applicable. The convention applies to policy files, admin API payloads, and logs — one identity grammar everywhere.
2. **`RelationResolver` SPI, added now, unused by the engine by default:** `check(subjectId, relation, resourceType:id) → boolean` (plus a related-ids variant). No graph-backed implementation ships yet; a trivial map-based impl exists for tests.
3. **CEL bridge (ADR-0002):** the SPI is exposed to expressions as a custom function, e.g. `related(subject, 'owner', resource)` — so ABAC policies can express relationship conditions the moment a resolver is provided, with zero engine changes.
4. **Policy format reserves an optional `relations` schema section** (declared relationships, e.g. `document → parent → folder`); ignored by the engine until ReBAC ships.
5. Everything in this ADR is documented as **preview contract, may evolve** until the ReBAC milestone (ROADMAP M7) — evolve it before v1.0, freeze it after.

## Consequences

- Small API surface cost now (one SPI, one convention, one optional format section) in exchange for no breaking redesign later.
- `type:id` discipline must be kept from day one — it touches migration files, the admin API, and audit logs, which is exactly why it cannot wait.
- Full ReBAC (graph store, check API, expansion semantics) remains deliberately out of scope; a future ADR will decide the implementation (embed a graph library vs delegate to OpenFGA/SpiceDB as a `RelationResolver` implementation — the SPI makes delegation possible without core changes).
