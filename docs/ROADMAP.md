# A3Shield Roadmap & Task Tracker

> Living document. Check items off, add new ones. Strategic decisions are recorded in `docs/adr/` — this file sequences the work that implements them.

## Target module structure (direction)

```
a3-core             models, DecisionResult, errors (zero deps) — extracted from a3-enforcer in M2
a3-enforcer         evaluation engine (CEL-backed), PolicySource SPI, tenant-aware — stays Spring-free (ADR-0007)
a3-policy-mgmt      policy migrations, runner, drift detection, admin API, audit, policy tests (ADR-0003)
a3-spring-boot-starter  annotations, aspects, interceptor, RequestCheck chain, SubjectExtractor, TenantResolver
(a3-auth-adapter)   later, optional, never core (ADR-0001)
(a3-server)         possible future PDP wrapper — not on near roadmap (ADR-0007)
```

## Milestones

### M0 — Core policy engine (a3-enforcer) ✅ DONE
- [x] Contracts: PolicyRepository, PolicyMatcher, ConditionEvaluator, DecisionCombiner, PolicyEngine
- [x] Default implementations; expression parser (`==`, `!=`, `>`, `<`); DSL evaluator with AND/OR, ANY-match list semantics
- [x] Thread-safe ExpressionCache; in-memory repository; 15 passing tests
- [x] Decision model incl. NOT_APPLICABLE; resources-as-list with homogeneity validation

### M1 — Stabilize the starter ✅ DONE (2026-10-21)
- [x] Fix `A3SecurePolicyAspect`: typed `AccessDeniedException` reaches callers un-wrapped; original return value passed through on allow (per-row filtering is M6)
- [x] Fix `A3AuthorizationAspect`: intent now built from the `@A3Intent` parameter (was passing the join point); `@A3Intent` retargeted to PARAMETER (was TYPE — the path could never compile for users)
- [x] `AccessDeniedException` is now unchecked so it survives proxies/interceptors
- [x] Starter unit tests: builders, holders, both aspects (mocked enforcer), check chain, filter — 19 tests
- [x] RequestCheck chain SPI (ADR-0001): `RequestCheck`/`CheckResult`/`RequestCheckChain` + `A3RequestCheckFilter` (403 on denial, clears all ThreadLocal holders per request)
- [x] Real auto-configuration: `@AutoConfiguration` + `META-INF/spring/...imports` registration, `@ConditionalOnMissingBean` back-off, aspects/builders as beans (stereotype annotations removed)
- [x] Document Intent-vs-Target mental model (README + aspect javadocs)
- [x] Parent POM: mockito 5.14.2 / byte-buddy 1.15.10 pinned for JDK 23

### M2 — Contract evolution (breaking, do while small) 🔜 NEXT
*Implements ADR-0005, ADR-0008 §1–2, ADR-0009. One coordinated breaking release, before anything is published.*
- [ ] Extract `a3-core` (models, DecisionResult, errors — zero deps)
- [ ] `DecisionResult` replaces boolean across PolicyEngine/AccessEnforcer; `AccessDeniedException` carries it (ADR-0005)
- [ ] Tenant dimension: `RequestContext` carries tenant; `Policy.tenantId` (+ `*` global scope); repo → `findAll(tenantId, action, resourceType)`; tenant-aware cache keys; matcher checks tenant first (ADR-0009)
- [ ] `TenantResolver` hook in starter (header/subdomain/claim); `default` tenant zero-config fallback
- [ ] Resource identity convention `type:id` everywhere; `Intent` gains `id` (ADR-0008)
- [ ] `RelationResolver` SPI added (map-based test impl only) + `relations` reserved section in policy format (ADR-0008)
- [ ] Tenant-scope precedence tests (tenant policy vs global policy vs DENY-overrides)

### M3 — CEL evaluation engine 📋 PLANNED (ADR-0002)
- [ ] Add `cel-java` to enforcer; `CelConditionEvaluator` behind the SPI with deadline/complexity budget
- [ ] Register custom functions bridge (prep for `related()`); simple-syntax facade for legacy conditions (`.exists()` for ANY-match)
- [ ] Evaluation parity test suite: every legacy DSL expression verified against CEL result

### M4 — Policy lifecycle: migrations 📋 PLANNED (ADR-0003)
- [ ] Decide policy file format: JSON vs YAML (last open format question)
- [ ] `a3-policy-mgmt` module: file layout `a3/policies/V<n>__<name>`, tenant-scope header, checksums
- [ ] `PolicyMigrationRunner`: apply-on-startup, applied-versions tracking, `onConflict` (FAIL default / OVERWRITE / SKIP)
- [ ] Policy tests embedded in files; apply aborts on test failure
- [ ] Persistent `PolicyStore` (JDBC impl) alongside in-memory; `PolicySource` SPI

### M5 — Runtime admin layer 📋 PLANNED (ADR-0003, ADR-0007)
- [ ] `PolicyAdminApi` SPI + REST exposure in starter (always tenant-scoped)
- [ ] Validate-before-commit (run policy tests on every mutation); audit log (who/when/diff)
- [ ] Drift report endpoint (file vs store); cache invalidation strategy for multi-instance deployments

### M6 — Post-execution per-row filtering 📋 PLANNED (ADR-0004)
- [ ] `@A3SecurePolicy` per-row filtering for collections; single-object deny-or-allow; documented default + strict mode
- [ ] Route-level interceptor enforcement (intent check from route/method/params)
- [ ] Audit events at all three enforcement points

### M7 — ReBAC implementation 🔮 FUTURE (ADR-0008)
- [ ] Graph-backed `RelationResolver` (or delegate to OpenFGA/SpiceDB behind the SPI — separate ADR when reached)
- [ ] `related()` CEL function end-to-end; freeze preview contracts

### M8 — Distribution & credibility 🔮 FUTURE
- [ ] Example app using the starter end-to-end; README rewrite to match reality
- [ ] CI (GitHub Actions: build + test on push/PR); publish to GitHub Packages/Maven Central

### Vision backlog (unprioritized, from README)
- [ ] a3-auth-adapter (login-flow conveniences — adapter, never core)
- [ ] a3-server PDP mode; admin dashboard; JS/Python SDKs; distributed evaluation

## Open Design Questions
1. Policy file format: JSON vs YAML (decide at M4 kickoff).
2. Per-row filtering default: silent filter vs deny-on-any-failure (ADR-0004 leans silent-with-strict-option — confirm with example app in M6).
3. Cache invalidation mechanism for clustered deployments: TTL vs poll vs pub-sub (decide in M5).
4. What exactly goes into v0.1.0 for publishing (M2+M3 minimum? include M4?) — owner call.

## Changelog (recent)
- 2026-10-21 (session 3): M1 complete — aspect fixes (exception passthrough, intent-object bug, `@A3Intent` target), unchecked `AccessDeniedException`, RequestCheck SPI + filter with ThreadLocal cleanup, real auto-configuration registration, 19 starter tests, JDK-23 test toolchain pins. 34 tests green total.
- 2026-10-21 (session 2): Architecture decided; ADRs 0001–0009 accepted; roadmap restructured into M0–M8 around embedded-first, ReBAC-ready, multi-tenant-day-one direction.
- 2026-10-21 (session 1): Synced remote; fixed RequestContext compile error; tests migrated to resources-list API (15 green); `NOT_APPLICABLE` decision; `@A3SecurePolicy` WIP committed; build output untracked from git; continuity docs added.
