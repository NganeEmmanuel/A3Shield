# Session Handoff Log

> **Protocol:** newest entry at the top. At the end of every working session, prepend an entry using the template at the bottom. Commit and push it together with your work. This file is how the next session (human or agent) picks up exactly where you left off.

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
