# A3Shield Roadmap & Task Tracker

> Living document. Check items off, add new ones. Detailed product breakdown from the owner is pending and will be merged in when provided.

## Milestone Status

### M0 — Core policy engine (a3-enforcer) ✅ WORKING
- [x] Contracts: PolicyRepository, PolicyMatcher, ConditionEvaluator, DecisionCombiner, PolicyEngine
- [x] Default implementations for all of the above
- [x] Expression parser (`==`, `!=`, `>`, `<`) + ValueResolver
- [x] DSL evaluator with `AND`/`OR`, ANY-match semantics for list values
- [x] Thread-safe ExpressionCache
- [x] Decision model: ALLOW / DENY / **NOT_APPLICABLE** (added 2026-10-21; condition-false ≠ DENY)
- [x] In-memory repository + 15 passing tests

### M1 — Spring Boot integration ✅ COMPILES, PARTIALLY WIRED
- [x] Auto-configuration (A3AutoConfiguration, InterceptorConfig)
- [x] Annotations: `@A3Authorize`, `@A3HasRoles`, `@A3HasPermissions`, `@A3Intent`, `@A3SecurePolicy`
- [x] Aspects: A3AuthorizationAspect (pre), A3SecurePolicyAspect (post-execution)
- [x] ThreadLocal context holders/builders: Request, Subject, Target, Intent
- [x] SubjectExtractor decoupled; Spring Security default impl (JWT-based)
- [x] AccessDeniedException
- [ ] **Fix:** A3SecurePolicyAspect wraps AccessDeniedException in RuntimeException (denials lose their type) and returns transformed targets instead of raw result
- [ ] Temporal `hasPermissions`/`hasRoles` implementations need hardening
- [ ] Starter module has **zero tests** — needs at least context-builder + aspect unit tests

### M2 — Enforcer hardening 🔜 NEXT
- [ ] Implement filtering in `InMemoryPolicyRepository.findAll(action, resourceType)` (args currently ignored)
- [ ] Decide & handle empty-resources behavior (`getResourceType()` currently throws NPE)
- [ ] Verify DefaultDecisionCombiner semantics with NOT_APPLICABLE (empty decision list → default DENY)
- [ ] DSL: parentheses/grouping, `>=`, `<=`, string literals beyond bare words
- [ ] PermissionRepository is a stub — wire permission-based checks

### M3 — Real-world validation 🔜
- [ ] Example app (separate module or repo) using the starter end-to-end
- [ ] JDBC- or file-backed PolicyRepository
- [ ] README rewrite to match reality (current README lists modules that don't exist)
- [ ] CI: GitHub Actions running `mvn test` on push/PR

### M4 — Vision items (from README, unprioritized)
- [ ] Authentication module (a3-auth: JWT, MFA) — not started
- [ ] Distributed policy engine / multi-tenant
- [ ] Admin dashboard, JS/Python SDKs, OAuth integrations

## Open Design Questions (owner input wanted)
1. Should `@A3SecurePolicy` support per-annotation policy overrides (effect, condition)?
2. Target/Intent duality: is the long-term plan *both* pre-execution (intent) and post-execution (target) checks on the same endpoint?
3. Ownership model for `Permission` vs `Policy` — how do PBAC checks relate to policy evaluation?
4. What does v0.1.0 need to include before publishing to Maven Central / GitHub Packages?

## Changelog (recent)
- 2026-10-21: Synced remote; fixed RequestContext compile error; tests migrated to resources-list API (15 green); `NOT_APPLICABLE` decision; `@A3SecurePolicy` WIP committed; build output untracked from git; continuity docs added.
