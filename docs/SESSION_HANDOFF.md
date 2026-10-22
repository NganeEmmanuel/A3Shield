# Session Handoff Log

> **Protocol:** newest entry at the top. At the end of every working session, prepend an entry using the template at the bottom. Commit and push it together with your work. This file is how the next session (human or agent) picks up exactly where you left off.

---

## 2026-10-22 (session 5) — Workflow & CI established (first PR-based work)

**Goal of session:** establish branching model + CI per owner requirements (external contributors, progressive commits, standard practices, docs-only changes skip builds, deterministic local==CI parity).

**What was done:**
1. **Branches:** created `develop` and `staging` from master (all three synced at `47bbf92`), pushed. Model: `master` (production) ← `staging` (pre-release) ← `develop` (integration); all work on `feature|fix|chore|security|docs/*` branches via PR; merge commits (no squash); Conventional Commits.
2. **This session's work** on `chore/ci-pipeline` (owner will open the PR and merge): Maven wrapper pinned to 3.9.11 (`./mvnw` everywhere now — CI parity); Spotless 2.44.5 with palantir-java-format + import order, baseline applied (58 files, tests still green); CI workflows (`.github/workflows/ci-build.yml` = build+test+spotless+CodeQL with docs path filters; `ci-security.yml` = gitleaks full-history scan always + dependency-review on PRs); `dependabot.yml` (maven + actions, weekly); `.github/PULL_REQUEST_TEMPLATE.md`.
3. **Docs:** CONTRIBUTING.md fully rewritten (branch model, commit format, local-verification parity section, CI gate table); AGENTS.md workflow section + agent rule recorded (**at end of every work unit the agent provides a PR description in the template format**); ROADMAP M8 updated.

**State:** work committed progressively on `chore/ci-pipeline` (5 commits), pushed for owner PR. All 52 tests green via `./mvnw clean verify` after the format baseline. `develop`/`staging`/`master` untouched by this work (equal at `47bbf92`).

**Decisions made:** branch names `develop` + `staging` (owner asked for best practice — gitflow-standard); merge-commit strategy preserving progressive commits; format = palantir (4-space, closest to existing style); CI builds skip docs-only paths but secret scan always runs; Temurin 23 in CI.

**Pending (owner actions):** open PR `chore/ci-pipeline` → `develop` using the provided description; merge; **enable branch protection manually** (Settings → Branches; steps in CONTRIBUTING) — requires PR + status checks on master/staging/develop.

**Suggested next steps:** after merge, cut future work branches from `develop` (not master); M3 CEL engine on `feature/cel-evaluator`.

---

## 2026-10-22 (session 4) — M2 complete: contract evolution (tenant day-1, DecisionResult, a3-core)

**Goal of session:** implement M2 per ROADMAP — the coordinated breaking contract release.

**What was done:**
1. **`a3-core` extracted** (ADR-0007): models (Subject/SubjectType/Resource/Target/Intent), Decision + **DecisionResult**, AccessDeniedException, Tenants — zero runtime deps (Lombok compile-time only). Packages moved: `enforcer.model.*` → `core.model.*`; `AccessDeniedException` now `core.exception` (unchecked, carries DecisionResult). Enforcer depends on core; starter depends on both.
2. **DecisionResult (ADR-0005):** `PolicyEngine.evaluate` and `AccessEnforcer.enforce` return `DecisionResult` (decision, matchedPolicyIds, reason, obligations); `AccessEnforcer.allow()` boolean helper; aspects throw `AccessDeniedException(msg, result)` — callers/exception handlers can now explain denials.
3. **Tenant day-one (ADR-0009):** `RequestContext.tenantId` (never null; `default` fallback), `Policy.tenantId` (`*` = global), `PolicyRepository.findAll(tenantId, action, resourceType)` — **real filtering implemented** (was a no-op returning everything), matcher checks tenant first, engine applies **most-specific-scope-wins**: tenant policies override global when both apply; global only when no tenant policy is applicable. Starter: `TenantResolver` SPI (`DefaultTenantResolver` → `default`), `TenantContextHolder` set by `A3RequestCheckFilter` and cleared in its finally.
4. **ReBAC readiness (ADR-0008):** `Resource.urn()` = `type:id` convention (+ tests), `Intent.id` populated by the builder, `RelationResolver` SPI + `InMemoryRelationResolver` (map-based; engine does not call it yet — M7), `Policy.relations` reserved section.
5. Null-safety: `getResourceType()` returns null on empty (was NPE); matcher tolerates null request action.
6. **52 tests green** (3 core / 27 enforcer / 22 starter) — new: tenant precedence (tenant-over-global, global fallback, cross-tenant isolation, default-tenant), repository filtering, URN convention, relation resolver, tenant propagation through holders/filter.

**State:** clean tree after commit, local == remote. BUILD SUCCESS, all three modules.

**Decisions made:** tenant precedence = most-specific-APPLICABLE-scope wins (partition happens after matching, so a tenant policy that doesn't match the subject falls back to global — tested); repository returns tenant + global candidates, engine picks the scope; `DecisionResult` keeps record-style accessors (`decision()`, `reason()`, `matchedPolicyIds()`, `isAllowed()`).

**Known issues carried forward:** `@A3Authorize` expression still a trigger flag (decide role in M3); TargetContextBuilder reflection; obligations plumbed but not declarable; A3HasRoles/A3HasPermissions still unwired WIP; package moves break pre-M2 host code.

**Suggested next steps:** M3 — CEL evaluation engine (add cel-java, `CelConditionEvaluator` behind the SPI, deadline budget, parity suite vs the legacy DSL; decide fate of `@A3Authorize`'s expression value there).

---

## 2026-10-21 (session 3) — M1 complete: starter stabilized

**Goal of session:** implement M1 (stabilize the starter) per ROADMAP.

**What was done:**
1. **Aspect fixes:** `A3SecurePolicyAspect` no longer wraps everything in `RuntimeException` — typed `AccessDeniedException` reaches callers; on allow it returns the *original* method result (M6 will add per-row filtering). `A3AuthorizationAspect` intent bug fixed: it now builds the intent from the `@A3Intent` parameter (it was passing the join point object). Found & fixed a latent library bug: `@A3Intent` was `@Target(TYPE)` — unusable on parameters, so the `@A3Authorize` expression path could never compile for users; now `@Target(PARAMETER)`.
2. **`AccessDeniedException` is now unchecked** (was checked `Exception`) so it propagates through proxies.
3. **RequestCheck SPI (ADR-0001):** `RequestCheck`/`CheckResult`/`RequestCheckChain` (ordered, short-circuit) + `A3RequestCheckFilter` — runs host-app checks (403 + reason on denial) and clears ALL ThreadLocal holders in `finally` (subject/request/intent/target), fixing cross-request state bleed on pooled threads.
4. **Real auto-configuration:** `@AutoConfiguration` + `META-INF/spring/...AutoConfiguration.imports` registration, `@ConditionalOnMissingBean` back-off on every bean, aspects/builders/holders wired as beans, default `SubjectExtractor` = Spring Security JWT extractor. All `@Component` stereotypes removed from starter classes (double-registration hazard). **Breaking for host apps that relied on component-scanning the package** — the starter now self-registers like a real Boot starter.
5. **19 starter tests** (builders, holders, both aspects incl. regression tests for both fixed bugs, check chain, filter) + **JDK-23 toolchain fix** (mockito 5.14.2 / byte-buddy 1.15.10 pinned in parent POM — Boot 3.2.5's mockito 5.7 cannot instrument on JDK 23).
6. README: Intent/Target model section.

**State:** clean tree after commit, local == remote. **34 tests green (15 enforcer + 19 starter), BUILD SUCCESS.**

**Decisions made:** filter order = `HIGHEST_PRECEDENCE + 50` (runs ahead of Spring Security); denial response = 403 text/plain with check reason; M1 post-execution semantics = verify-only, original result returned (filtering deferred to M6).

**Known issues carried forward:** repo filtering not implemented (M2); `getResourceType()` NPE on empty resources (M2); `@A3Authorize` expression value is a trigger flag, not evaluated — its role must be decided; `TargetContextBuilder` reflection is slow & JDK-fragile (its own todo).

**Suggested next steps:** M2 contract evolution (tenant day-1 + DecisionResult + `type:id` + RelationResolver SPI + `a3-core` extraction) — the time-critical milestone; then M3 CEL.

---

## 2026-10-21 (session 2) — Architecture decisions & ADRs

**Goal of session:** discuss the product vision (full IAM library, policy migrations, runtime admin editing), get an architectural review, and decide direction.

**Decisions made** (owner + review consensus; all recorded as accepted ADRs in `docs/adr/`):
1. **ADR-0007 Embedded-first** — embedded Java library, no external PDP service; engine boundary stays clean so an `a3-server` wrapper remains possible later.
2. **ADR-0008 ReBAC planned now** — `type:id` resource identity convention, `RelationResolver` SPI, reserved `relations` policy-format section, CEL `related()` bridge; graph implementation deferred to M7.
3. **ADR-0009 Multi-tenant day-one** — tenant is a first-class contract dimension: tenant-aware `Policy`, repository (`findAll(tenantId, action, resourceType)`), caches, matcher; `default`-tenant zero-config fallback; migrations and admin API are tenant-scoped.
Plus accepted review recommendations: **ADR-0001** authz-only core (authn via `SubjectExtractor`, never rebuilt), **ADR-0002** CEL replaces the hand-rolled DSL behind the `ConditionEvaluator` SPI (simple syntax stays as facade), **ADR-0003** Flyway-style policy migrations + guardrailed runtime admin editing (`onConflict` FAIL/OVERWRITE/SKIP, drift report, policy tests), **ADR-0004** three enforcement points incl. per-row post-execution filtering, **ADR-0005** rich `DecisionResult`, **ADR-0006** servlet/ThreadLocal-at-edge concurrency model.

**What was done:** created `docs/adr/` (README process + template + ADR-0001..0009); restructured `docs/ROADMAP.md` into M0–M8 sequenced around the decisions (M2 = coordinated breaking contract evolution: tenant + DecisionResult + `type:id` + RelationResolver SPI + `a3-core` extraction); updated `AGENTS.md` with the strategic direction and ADR rules.

**State:** clean tree, docs only — no code changes this session. Build/test state unchanged from session 1 (15 tests green).

**Suggested next steps (in order):**
1. M1: fix `A3SecurePolicyAspect` exception passthrough + starter unit tests (small, unblocks credibility).
2. M2: the contract-evolution release — do it as one coordinated change while the codebase is small; tenant day-1 makes this the time-critical item.
3. M4 kickoff decision: policy file format (JSON vs YAML) — last open format question.

---

## 2026-10-21 — Context re-establishment & sync

**Goal of session:** establish project context, sync local with remote, set up continuity infrastructure.

**What was done:**
1. Synced with remote (`origin/master` was 2 commits behind local → pushed; nothing to pull).
2. Found the working tree mid-feature and **not compiling** (`RequestContext` duplicate constructor). Fixed: validation moved into the all-args constructor the Lombok builder uses.
3. Migrated 4 stale test classes to the new `resources`-list API (`DslConditionEvaluatorTest`, `AdvancedDslEvaluatorTest`, `DefaultPolicyMatcherTest`, `DefaultPolicyEngineTest`). **All 15 tests green, both modules build.**
4. Committed the in-flight feature (commit `9f0192e`): `@A3SecurePolicy` + aspect + `AccessDeniedException`, `Decision.NOT_APPLICABLE`, `PolicyRepository.findAll(action, resourceType)`.
5. Hygiene: `.gitignore` now excludes `target/` everywhere; ~20 compiled `.class` files untracked from git.
6. Created continuity docs: `AGENTS.md`, `docs/ROADMAP.md`, this file.

**State:** clean tree, local == remote == `9f0192e`, build green.

**Known issues carried forward** (details in `AGENTS.md` §Known Issues):
- `A3SecurePolicyAspect` wraps `AccessDeniedException` in `RuntimeException` (typed denial never reaches callers) and returns transformed `targets`, not the raw result.
- `InMemoryPolicyRepository.findAll(action, resourceType)` ignores its parameters.
- `RequestContext.getResourceType()` throws on empty resources — engine calls it every evaluate.
- Starter module has no tests.

**Suggested next steps (in order):**
1. Fix the `A3SecurePolicyAspect` exception handling (small, high value).
2. Implement repository filtering; add empty-resources behavior test.
3. Starter unit tests (context builders first, then aspects with mocked enforcer).
4. Owner to provide detailed product breakdown → merge into `docs/ROADMAP.md` and re-prioritize M2+.

---

### Entry template (copy for each new session)

```markdown
## YYYY-MM-DD — <short session title>

**Goal of session:** ...

**What was done:**
1. ...

**State:** branch/commit, build & test status, clean or dirty tree.

**Decisions made:** ... (and why)

**Known issues carried forward:** ...

**Suggested next steps:** ...
```
