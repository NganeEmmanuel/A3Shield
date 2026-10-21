# Session Handoff Log

> **Protocol:** newest entry at the top. At the end of every working session, prepend an entry using the template at the bottom. Commit and push it together with your work. This file is how the next session (human or agent) picks up exactly where you left off.

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
