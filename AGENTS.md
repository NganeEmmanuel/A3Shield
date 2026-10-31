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
├── a3-policy-mgmt          # Versioned policy migrations (ADR-0003, ADR-0010) — Spring-free, depends on a3-enforcer
│   ├── migration/            # PolicyMigration, OnConflict, PolicyTestCase models
│   ├── parse/                # PolicyMigrationParser SPI + YamlPolicyMigrationParser (safe map loading)
│   ├── discovery/            # PolicyMigrationDiscoverer (classpath a3/policies/ or directory, V<n>__name.yaml)
│   └── runtime/              # PolicyMigrationRunner (tests-first, fail-closed, checksum conflicts),
│                             # AppliedMigrationsStore (in-memory + JDBC), MigrationPolicyRepository
├── a3-enforcer             # Framework-agnostic policy engine (depends only on a3-core)
│   ├── expression/          # Expression record, parser (==, !=, >, <), ValueResolver
│   ├── evaluator/           # CelConditionEvaluator (canonical, ADR-0002), DslConditionEvaluator (legacy compat), SimpleConditionEvaluator
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
- **Conditions are CEL (ADR-0002)**: `CelConditionEvaluator` is the canonical engine (`cel-standalone` 0.5.1, non-Turing-complete, fail-closed, 512-char cap). Legacy simple syntax (`AND`/`OR`, bareword literals like `USER`) is auto-translated to CEL by the facade; `DslConditionEvaluator` remains as compat. Activation exposes `subject`, `resource` (first), `resources` (all, for `.exists()` ANY-match), `context`, `tenant`. One semantic difference vs legacy: ordered comparisons (`>` `<`) against string-typed numbers fail closed — store numbers as numbers.
- Decision strategy: **DENY overrides ALLOW, default DENY**. `NOT_APPLICABLE` = policy matched but condition was false (does not force DENY). `PolicyEngine.evaluate` returns a rich `DecisionResult` (reason, fired policy ids, obligations).
- **Tenancy (ADR-0009):** every evaluation carries a tenant (`default` when unset). Policies are tenant-scoped; `tenantId == "*"` = global. Precedence: **most specific applicable scope wins** — tenant policies override global ones when both match; global applies only when no tenant policy is applicable. Starter resolves tenants via `TenantResolver` (default → `default`).
- `PolicyRepository.findAll(tenantId, action, resourceType)` is the scoped-lookup contract; null policy action/resource means "matches anything".

## Build & Test Commands

```bash
./mvnw -B -ntp clean verify      # full build + all tests (exactly what CI runs)
./mvnw -B -ntp spotless:check    # format gate (CI fails if not clean)
./mvnw -B -ntp spotless:apply    # auto-fix formatting before committing
./mvnw -B -ntp -pl a3-enforcer test              # enforcer only
./mvnw -B -ntp -pl a3-spring-boot-starter test   # starter only
```

- Use `./mvnw` (wrapper, pinned Maven 3.9.11) — NOT bare `mvn` — so local matches CI.
- Java 23 required (Temurin 23 in CI). Lombok 1.18.44 via annotationProcessorPaths.
- Spring Boot 3.2.5 dependency management (starter module).
- Tests are pure unit tests (no DB/network/secrets) — deterministic by design.

## Workflow (branches, commits, PRs — see CONTRIBUTING.md for full detail)

- **Permanent protected branches:** `master` (production releases) ← `staging` (pre-release hardening) ← `develop` (integration). **Never commit directly to them.**
- **All work happens on branches from `develop`:** `feature/xxxx`, `fix/xxxx`, `chore/xxxx`, `security/xxxx`, `docs/xxxx`.
- **Commits follow Conventional Commits** (`feat:`, `fix:`, `chore:`, `docs:`, `ci:`, `build:`, `security:`, `refactor:`, `test:`). Progressive, small commits within a milestone are the norm.
- **PRs** may contain multiple related commits; merge with a **merge commit** (no squash). CI must be green (build+tests, spotless, CodeQL, gitleaks, dependency review).
- **Agent rule (recorded by owner decision, 2026-10-22):** at the end of every unit of work, the agent must provide a PR description following `.github/PULL_REQUEST_TEMPLATE.md` (What/Why/Type/Changes/Testing/Checklist/Breaking changes/Related), ready to paste into the PR.
- Docs-only changes skip the build workflows (path filters) but always run the secret scan.

## Conventions

- **Never commit `target/`** (build output). It is gitignored; keep it that way.
- Format with Spotless (palantir-java-format): run `./mvnw spotless:apply` before committing; CI enforces `spotless:check`.
- **Starter classes carry no `@Component`/stereotype annotations** — everything is registered via `A3AutoConfiguration`/`A3WebConfiguration` with `@ConditionalOnMissingBean` back-off. Adding a stereotype re-registers the bean twice when host apps scan the package.
- Javadoc-style `<p>` docs on public contracts in `a3-enforcer`; the starter is less documented.
- Tests: JUnit 5 + AssertJ + Mockito (mockito 5.14/byte-buddy 1.15 pinned in the parent POM for JDK 23). Aspect tests use `AspectJProxyFactory`; holder/filter tests must clear ThreadLocals in `@AfterEach`.
- Builders (Lombok `@Builder`) are the standard construction style.

## Known Issues / Gotchas (as of 2026-10-22, post-M2)

1. `@A3Authorize`'s expression value is only a trigger flag — it is not evaluated against the intent; enforcement is entirely policy-driven. Deciding the expression's role is open (M3 CEL may absorb it).
2. `TargetContextBuilder` uses raw reflection (`setAccessible`) — breaks on JDK internals (e.g. String fields) and is flagged "too slow" by its author; fine for app POJOs for now.
3. README + `docs/architecture/*` describe modules that **don't exist** (`a3-auth`, `a3-access`, `a3-gateway`, `a3-sdk`). Treat them as vision docs, not reality. (README's Intent/Target and module sections partially updated.)
4. `A3HasRoles`/`A3HasPermissions` + `A3RoleValidation`/`A3PermissionValidation` are unreferenced WIP — no aspect uses them yet.
5. CI added 2026-10-22 (build/tests/spotless/CodeQL/gitleaks/dependency-review + Dependabot); branch protection still to be enabled manually (see CONTRIBUTING); no publish setup yet (local install only).
6. Obligations are plumbed through `DecisionResult` but policies can't declare them yet.
7. M2 moved packages (`com.a3shield.enforcer.model.*` → `com.a3shield.core.model.*`, `AccessDeniedException` → `com.a3shield.core.exception`): any host-app code from pre-M2 needs import updates.

## Continuity Workflow (how sessions stay coherent)

1. **Start of session:** read this file + `docs/ROADMAP.md` + `docs/SESSION_HANDOFF.md` (latest entry at top) + any ADRs relevant to the work at hand (`docs/adr/README.md` index).
2. **During work:** branch off `develop` (`feature/*` etc.), keep commits small and Conventional-Commits-formatted; run `./mvnw -B -ntp clean verify` before each push.
3. **End of session:** prepend a dated entry to `docs/SESSION_HANDOFF.md` (what changed / decisions / next steps), update `docs/ROADMAP.md` checkboxes, push the branch, and **provide the PR description** in the template format for the owner to open the PR.

See `docs/ROADMAP.md` for milestones and `docs/SESSION_HANDOFF.md` for the latest state.
