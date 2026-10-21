# ADR-0009: Multi-tenancy is a day-one constraint

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

A3Shield's natural customers are SaaS applications where many tenants share one deployment. Tenant isolation for authorization means each tenant has its own policy set (and later its own roles/permissions data). Retrofitting a tenant dimension into the policy format, repository contracts, matcher, and caches after policies exist is a breaking change across every stored artifact — it must be present in the contracts from the start, even if single-tenant deployments never notice it.

## Decision

1. **Tenant is a first-class dimension of the evaluation model.** `RequestContext` carries a tenant identifier (part of the request's ambient context). Every evaluation is implicitly tenant-scoped.
2. **Policies are tenant-scoped artifacts:** each policy carries a `tenantId`, or a wildcard/global scope (`*`) for platform-wide policies (e.g. "platform admins"). Tenant-scoped policies override global policies when both match (stricter-locality precedence, consistent with DENY-overrides in the combiner — exact combination rules verified by tests in M2).
3. **Repository contract is tenant-aware from day one:** `findAll(tenantId, action, resourceType)` (supersedes the current two-arg form). Caches are keyed including tenant. The matcher checks tenant before any other dimension.
4. **Zero-config single-tenant UX is preserved:** with no tenant configuration, everything lands in a `default` tenant — a single-tenant app never sees the concept.
5. **Migrations declare tenant scope** in their header (ADR-0003): apply to `default`, to a named tenant, or to all tenants. **Admin API operations are always tenant-scoped.**

## Consequences

- Every contract evolution in M2 includes the tenant parameter — slightly more ceremony now, no storage-format migration later.
- Policy files and the future store schema bake in tenant from the first released format version.
- Host applications must supply the tenant id (via the subject extractor or an explicit resolver SPI) — the starter needs a `TenantResolver` hook (simple: header/subdomain/claim extraction).
