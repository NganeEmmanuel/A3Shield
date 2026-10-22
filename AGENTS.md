# AGENTS.md — Working Guide for AI Agents (and humans)

Read this before making any change. It reflects the **actual** state of the codebase, not the aspirational README.

**Strategic direction (decided 2026-10-21, see `docs/adr/`):** embedded library first (no external PDP); authorization-only core, identity consumed via `SubjectExtractor`; CEL as expression engine; multi-tenant policies are a day-one contract constraint; ReBAC contracts reserved now, implemented later; policies defined as versioned migration files + runtime admin editing with guardrails. Architecture decisions live in `docs/adr/` — **accepted ADRs are immutable** (supersede, never edit); never implement against a superseded/rejected ADR.

## What A3Shield Is

A Java 23 / Maven multi-module library for **policy-driven authorization** (RBAC, PBAC, ABAC) with a Spring Boot starter integration. Think "framework-agnostic policy engine + Spring glue". Authentication is a possible future module — **all work so far is authorization**.

## Module Map (actual)

```
a3shield (parent pom, v0.1.0)
├── a3-core                # Shared models, decisions, errors — ZERO runtime deps (ADR-0007)
│   ├── model/               # Subject, SubjectType, Resource (abstract, urn() = type:id), Target, Intent (has id)
│   ├── decision/            # Decision enum, DecisionResult record (reason, matchedPolicyIds, obligations)
│   ├── exception/           # AccessDeniedException (unchecked, carries DecisionResult)
│   └── tenant/              # Tenants constants (DEFAULT_TENANT="default", GLOBAL="*")
├── a3-enforcer             # Framework-agnostic policy engine (depends only on a3-core)
│   ├── expression/          # Expression record, parser (==, !=, >, <), ValueResolver
│   ├── evaluator/           # SimpleConditionEvaluator, DslConditionEvaluator (AND/OR, ANY-match list semantics)
│   ├── cache/               # ExpressionCache (thread-safe, parsed-expression cache)
│   ├── matcher/             # DefaultPolicyMatcher (tenant first, then subject/resource/action)
│   ├── decision/            # DecisionCombiner + DefaultDecisionCombiner (deny-overrides)
│   ├── engine/              # PolicyEngine → DecisionResult; DefaultPolicyEngine (tenant-scope precedence)
│   ├── repository/          # PolicyRepository (tenant-scoped findAll), InMemoryPolicyRepository (real filtering)
│   ├── relation/            # RelationResolver SPI (preview, ADR-0008) + InMemoryRelationResolver
│   ├── action/, effect/, policy/, permission/  # Action, Effect, Policy (tenantId+relations), Permission
│   └── context/             # RequestContext (subject + resources + action + context + tenantId)
└── a3-spring-boot-starter   # Spring Boot integration
    ├── annotation/          # @A3Authorize, @A3HasRoles, @A3HasPermissions, @A3Intent (PARAMETER), @A3SecurePolicy
    ├── aspect/              # A3AuthorizationAspect (pre-check), A3SecurePolicyAspect (post-execution check)
    ├── check/               # RequestCheck SPI + CheckResult + RequestCheckChain + A3RequestCheckFilter
    ├── context/             # ThreadLocal holders + builders: Request, Subject, Target, Intent
    ├── tenant/              # TenantResolver SPI + DefaultTenantResolver + TenantContextHolder
    ├── security/            # SubjectExtractor interface + SpringSecuritySubjectContextExtractor
    └── config/              # A3AutoConfiguration + A3WebConfiguration (registered via AutoConfiguration.imports)
```

**Key design points:**
- `RequestContext.resources` is a `List<Resource>` — homogeneous types enforced at construction (mixed types throw).
- `Target` = the resource being acted on (post-execution, e.g. method return value); `Intent` = the intended action/resource (pre-execution). Both extend `Resource`; every resource has a `type:id` URN (`Resource.urn()`).
- DSL conditions like `resource.ownerId == subject.id`, with `AND`/`OR` and `==`, `!=`, `>`, `<`. List values use ANY-match semantics. (CEL migration is M3.)
- Decision strategy: **DENY overrides ALLOW, default DENY**. `NOT_APPLICABLE` = policy matched but condition was false (does not force DENY). `PolicyEngine.evaluate` returns a rich `DecisionResult` (reason, fired policy ids, obligations).
- **Tenancy (ADR-0009):** every evaluation carries a tenant (`default` when unset). Policies are tenant-scoped; `tenantId == "*"` = global. Precedence: **most specific applicable scope wins** — tenant policies override global ones when both match; global applies only when no tenant policy is applicable. Starter resolves tenants via `TenantResolver` (default → `default`).
- `PolicyRepository.findAll(tenantId, action, resourceType)` is the scoped-lookup contract; null policy action/resource means "matches anything".

## Build & Test Commands

```bash
mvn test              # full build + all tests (run before every commit)
mvn -pl a3-enforcer test        # enforcer only
mvn -pl a3-spring-boot-starter test   # starter only (currently has NO tests)
```

- Java 23 required (Oracle JDK 23.0.2 in use). Maven 3.9.x.
- Lombok 1.18.44 via annotationProcessorPaths in the parent pom.
- Spring Boot 3.2.5 dependency management (starter module).

## Conventions

- `master` branch, direct commits (no PR flow yet for solo work). Commit style: short lowercase summary lines, descriptive body is fine.
- **Never commit `target/`** (build output). It is gitignored now; keep it that way.
- **Starter classes carry no `@Component`/stereotype annotations** — everything is registered via `A3AutoConfiguration`/`A3WebConfiguration` with `@ConditionalOnMissingBean` back-off. Adding a stereotype re-registers the bean twice when host apps scan the package.
- Javadoc-style `<p>` docs on public contracts in `a3-enforcer`; the starter is less documented.
- Tests: JUnit 5 + AssertJ + Mockito (mockito 5.14/byte-buddy 1.15 pinned in the parent POM for JDK 23). Aspect tests use `AspectJProxyFactory`; holder/filter tests must clear ThreadLocals in `@AfterEach`.
- Builders (Lombok `@Builder`) are the standard construction style.

## Known Issues / Gotchas (as of 2026-10-22, post-M2)

1. `@A3Authorize`'s expression value is only a trigger flag — it is not evaluated against the intent; enforcement is entirely policy-driven. Deciding the expression's role is open (M3 CEL may absorb it).
2. `TargetContextBuilder` uses raw reflection (`setAccessible`) — breaks on JDK internals (e.g. String fields) and is flagged "too slow" by its author; fine for app POJOs for now.
3. README + `docs/architecture/*` describe modules that **don't exist** (`a3-auth`, `a3-access`, `a3-gateway`, `a3-sdk`). Treat them as vision docs, not reality. (README's Intent/Target and module sections partially updated.)
4. `A3HasRoles`/`A3HasPermissions` + `A3RoleValidation`/`A3PermissionValidation` are unreferenced WIP — no aspect uses them yet.
5. No CI, no publish setup (local install only).
6. Obligations are plumbed through `DecisionResult` but policies can't declare them yet.
7. M2 moved packages (`com.a3shield.enforcer.model.*` → `com.a3shield.core.model.*`, `AccessDeniedException` → `com.a3shield.core.exception`): any host-app code from pre-M2 needs import updates.

## Continuity Workflow (how sessions stay coherent)

1. **Start of session:** read this file + `docs/ROADMAP.md` + `docs/SESSION_HANDOFF.md` (latest entry at top) + any ADRs relevant to the work at hand (`docs/adr/README.md` index).
2. **During work:** keep commits small; run `mvn test` before each commit.
3. **End of session:** prepend a dated entry to `docs/SESSION_HANDOFF.md` (what changed / decisions / next steps), update `docs/ROADMAP.md` checkboxes, commit + push.

See `docs/ROADMAP.md` for milestones and `docs/SESSION_HANDOFF.md` for the latest state.
