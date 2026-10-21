# AGENTS.md — Working Guide for AI Agents (and humans)

Read this before making any change. It reflects the **actual** state of the codebase, not the aspirational README.

**Strategic direction (decided 2026-10-21, see `docs/adr/`):** embedded library first (no external PDP); authorization-only core, identity consumed via `SubjectExtractor`; CEL as expression engine; multi-tenant policies are a day-one contract constraint; ReBAC contracts reserved now, implemented later; policies defined as versioned migration files + runtime admin editing with guardrails. Architecture decisions live in `docs/adr/` — **accepted ADRs are immutable** (supersede, never edit); never implement against a superseded/rejected ADR.

## What A3Shield Is

A Java 23 / Maven multi-module library for **policy-driven authorization** (RBAC, PBAC, ABAC) with a Spring Boot starter integration. Think "framework-agnostic policy engine + Spring glue". Authentication is a possible future module — **all work so far is authorization**.

## Module Map (actual)

```
a3shield (parent pom, v0.1.0)
├── a3-enforcer              # Framework-agnostic policy engine (no Spring deps)
│   ├── expression/          # Expression record, parser (==, !=, >, <), ValueResolver
│   ├── evaluator/           # SimpleConditionEvaluator, DslConditionEvaluator (AND/OR, ANY-match list semantics)
│   ├── cache/               # ExpressionCache (thread-safe, parsed-expression cache)
│   ├── matcher/             # DefaultPolicyMatcher (subject/resource/action filtering)
│   ├── decision/            # Decision enum (ALLOW, DENY, NOT_APPLICABLE), DecisionCombiner
│   ├── engine/              # PolicyEngine interface + DefaultPolicyEngine (orchestrates repo→matcher→evaluator→combiner)
│   ├── repository/          # PolicyRepository, InMemoryPolicyRepository, PermissionRepository
│   ├── model/               # Subject, Resource (abstract), Target, Intent, SubjectType
│   ├── action/, effect/, policy/, permission/  # Action, Effect, Policy, Permission models
│   └── context/             # RequestContext (subject + resources list + action + context map)
└── a3-spring-boot-starter   # Spring Boot integration
    ├── annotation/          # @A3Authorize, @A3HasRoles, @A3HasPermissions, @A3Intent, @A3SecurePolicy
    ├── aspect/              # A3AuthorizationAspect (pre-check), A3SecurePolicyAspect (post-execution check)
    ├── context/             # ThreadLocal holders + builders: Request, Subject, Target, Intent
    ├── security/            # SubjectExtractor interface + SpringSecuritySubjectContextExtractor
    ├── config/              # A3AutoConfiguration, InterceptorConfig
    └── exceptions/          # AccessDeniedException
```

**Key design points:**
- `RequestContext.resources` is a `List<Resource>` — homogeneous types enforced at construction (mixed types throw).
- `Target` = the resource being acted on (post-execution, e.g. method return value); `Intent` = the intended action/resource (pre-execution). Both extend `Resource`.
- DSL conditions like `resource.ownerId == subject.id`, with `AND`/`OR` and `==`, `!=`, `>`, `<`. List values use ANY-match semantics.
- Decision strategy: **DENY overrides ALLOW, default DENY**. `NOT_APPLICABLE` = policy matched but condition was false (does not force DENY).
- `PolicyRepository.findAll(action, resourceType)` is the (new) scoped-lookup contract — in-memory impl does NOT yet filter.

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
- Javadoc-style `<p>` docs on public contracts in `a3-enforcer`; the starter is less documented.
- Tests: JUnit 5 + AssertJ, live in `a3-enforcer/src/test`. Builders (Lombok `@Builder`) are the standard construction style.

## Known Issues / Gotchas (as of 2026-10-21)

1. **`A3SecurePolicyAspect` swallows its own denial** — the `catch (Throwable)` wraps `AccessDeniedException` in a plain `RuntimeException`, so callers can't catch the typed exception. Also it returns the *transformed* `targets` list, not the raw method result.
2. **`InMemoryPolicyRepository.findAll(action, resourceType)` ignores its args** — no filtering yet.
3. `RequestContext.getResourceType()` throws NPE when resources are empty — the engine calls it on every evaluate; contexts without resources will blow up.
4. README + `docs/architecture/*` describe modules that **don't exist** (`a3-core`, `a3-auth`, `a3-access`, `a3-gateway`, `a3-sdk`). Treat them as vision docs, not reality.
5. No CI, no starter tests, no publish setup (local install only).
6. Minor: `Target`'s logger references `Intent.class`; typo "must nut be" in `RequestContext` error message.

## Continuity Workflow (how sessions stay coherent)

1. **Start of session:** read this file + `docs/ROADMAP.md` + `docs/SESSION_HANDOFF.md` (latest entry at top) + any ADRs relevant to the work at hand (`docs/adr/README.md` index).
2. **During work:** keep commits small; run `mvn test` before each commit.
3. **End of session:** prepend a dated entry to `docs/SESSION_HANDOFF.md` (what changed / decisions / next steps), update `docs/ROADMAP.md` checkboxes, commit + push.

See `docs/ROADMAP.md` for milestones and `docs/SESSION_HANDOFF.md` for the latest state.
