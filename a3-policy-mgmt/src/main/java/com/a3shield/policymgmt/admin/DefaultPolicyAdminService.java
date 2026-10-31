package com.a3shield.policymgmt.admin;

import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.model.Resource;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.DefaultDecisionCombiner;
import com.a3shield.enforcer.engine.DefaultPolicyEngine;
import com.a3shield.enforcer.evaluator.CelConditionEvaluator;
import com.a3shield.enforcer.evaluator.ConditionEvaluator;
import com.a3shield.enforcer.matcher.DefaultPolicyMatcher;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.enforcer.repository.InMemoryPolicyRepository;
import com.a3shield.policymgmt.audit.PolicyAuditRecord;
import com.a3shield.policymgmt.audit.PolicyAuditStore;
import com.a3shield.policymgmt.migration.PolicyMigration;
import com.a3shield.policymgmt.migration.PolicyTestCase;
import com.a3shield.policymgmt.parse.PolicySerde;
import com.a3shield.policymgmt.store.EffectivePolicyRecord;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import com.a3shield.policymgmt.store.Origin;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default {@link PolicyAdminApi}: tenant-scoped CRUD with
 * validate-before-commit and a full audit trail (ADR-0003).
 *
 * <p>Validation before every commit: the condition must compile (CEL), and
 * the policy's validation tests — the caller's, else the stored suite from
 * the migration file that introduced the policy — must pass against the
 * candidate effective set (current tenant policies with the mutation
 * applied). A failing validation rejects the mutation; nothing is written.</p>
 */
public class DefaultPolicyAdminService implements PolicyAdminApi {

    private final EffectivePolicyStore store;
    private final PolicyAuditStore auditStore;
    private final Supplier<List<PolicyMigration>> migrationsSupplier;
    private final ConditionEvaluator conditionEvaluator;
    private final PolicySerde serde = new PolicySerde();

    public DefaultPolicyAdminService(
            EffectivePolicyStore store,
            PolicyAuditStore auditStore,
            Supplier<List<PolicyMigration>> migrationsSupplier) {
        this(store, auditStore, migrationsSupplier, new CelConditionEvaluator());
    }

    public DefaultPolicyAdminService(
            EffectivePolicyStore store,
            PolicyAuditStore auditStore,
            Supplier<List<PolicyMigration>> migrationsSupplier,
            ConditionEvaluator conditionEvaluator) {
        this.store = store;
        this.auditStore = auditStore;
        this.migrationsSupplier = migrationsSupplier;
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public EffectivePolicyRecord create(String tenantId, Policy policy, List<PolicyTestCase> tests, String actor) {
        if (store.find(tenantId, policy.getId()).isPresent()) {
            throw new PolicyValidationException(
                    "Policy " + tenantId + "/" + policy.getId() + " already exists; use update");
        }
        validate(tenantId, policy, null, tests == null ? List.of() : tests);
        EffectivePolicyRecord record = EffectivePolicyRecord.builder()
                .tenantId(tenantId)
                .policy(policy)
                .bodyYaml(serde.toYaml(policy))
                .origin(Origin.ADMIN)
                .tests(tests)
                .build();
        store.upsert(record);
        audit(tenantId, policy.getId(), PolicyAuditRecord.Operation.CREATE, null, record.getBodyYaml(), actor);
        return record;
    }

    @Override
    public EffectivePolicyRecord update(
            String tenantId, String policyId, Policy policy, List<PolicyTestCase> tests, String actor) {
        EffectivePolicyRecord existing = store.find(tenantId, policyId)
                .orElseThrow(
                        () -> new PolicyValidationException("Policy " + tenantId + "/" + policyId + " does not exist"));

        List<PolicyTestCase> effectiveTests = tests != null ? tests : existing.getTests();
        validate(tenantId, policy, policyId, effectiveTests);

        EffectivePolicyRecord record = EffectivePolicyRecord.builder()
                .tenantId(tenantId)
                .policy(policy)
                .bodyYaml(serde.toYaml(policy))
                .origin(Origin.ADMIN)
                .migrationVersion(existing.getMigrationVersion())
                .tests(effectiveTests)
                .build();
        store.upsert(record);
        audit(
                tenantId,
                policyId,
                PolicyAuditRecord.Operation.UPDATE,
                existing.getBodyYaml(),
                record.getBodyYaml(),
                actor);
        return record;
    }

    @Override
    public boolean delete(String tenantId, String policyId, String actor) {
        Optional<EffectivePolicyRecord> existing = store.find(tenantId, policyId);
        if (existing.isEmpty()) {
            return false;
        }
        boolean deleted = store.delete(tenantId, policyId);
        if (deleted) {
            audit(
                    tenantId,
                    policyId,
                    PolicyAuditRecord.Operation.DELETE,
                    existing.get().getBodyYaml(),
                    null,
                    actor);
        }
        return deleted;
    }

    @Override
    public List<EffectivePolicyRecord> list(String tenantId) {
        return store.findByTenant(tenantId);
    }

    @Override
    public List<PolicyAuditRecord> audit(String tenantId, int limit) {
        return auditStore.findByTenant(tenantId, limit);
    }

    @Override
    public List<DriftEntry> drift(String tenantId) {
        List<DriftEntry> result = new ArrayList<>();
        List<Policy> filePolicies = new ArrayList<>();
        for (PolicyMigration migration : migrationsSupplier.get()) {
            if (migration.getTenantId().equals(tenantId)) {
                filePolicies.addAll(migration.getPolicies());
            }
        }

        for (Policy filePolicy : filePolicies) {
            Optional<EffectivePolicyRecord> stored = store.find(tenantId, filePolicy.getId());
            String fileYaml = serde.toYaml(filePolicy);
            if (stored.isEmpty()) {
                result.add(new DriftEntry(tenantId, filePolicy.getId(), fileYaml, null, "MISSING_IN_STORE"));
            } else if (!stored.get().getBodyYaml().equals(fileYaml)) {
                result.add(new DriftEntry(
                        tenantId,
                        filePolicy.getId(),
                        fileYaml,
                        stored.get().getBodyYaml(),
                        stored.get().getOrigin() == Origin.ADMIN ? "ADMIN_EDITED" : "DIVERGED"));
            }
        }

        for (EffectivePolicyRecord stored : store.findByTenant(tenantId)) {
            boolean fromFile = filePolicies.stream()
                    .anyMatch(p -> p.getId().equals(stored.getPolicy().getId()));
            if (!fromFile) {
                result.add(new DriftEntry(
                        tenantId,
                        stored.getPolicy().getId(),
                        null,
                        stored.getBodyYaml(),
                        stored.getOrigin() == Origin.ADMIN ? "ADMIN_ONLY" : "REMOVED_FROM_FILE"));
            }
        }
        return result;
    }

    /** Validate-before-commit: CEL syntax + policy tests against the candidate effective set. */
    private void validate(String tenantId, Policy policy, String replacedId, List<PolicyTestCase> tests) {
        if (policy.getId() == null || policy.getId().isBlank()) {
            throw new PolicyValidationException("Policy id is required");
        }
        if (replacedId != null && !Objects.equals(replacedId, policy.getId())) {
            throw new PolicyValidationException("Policy id cannot change on update (" + replacedId + " -> "
                    + policy.getId() + "); delete and create instead");
        }
        if (conditionEvaluator instanceof CelConditionEvaluator cel && !cel.compiles(policy.getCondition())) {
            throw new PolicyValidationException("Condition does not compile: '" + policy.getCondition() + "'");
        }

        if (tests == null || tests.isEmpty()) {
            return; // no suite to validate against - condition syntax check only
        }

        List<Policy> candidate = new ArrayList<>();
        for (EffectivePolicyRecord record : store.findByTenant(tenantId)) {
            if (!record.getPolicy().getId().equals(replacedId)) {
                candidate.add(record.getPolicy());
            }
        }
        candidate.add(policy);
        candidate.addAll(store.findByTenant(com.a3shield.core.tenant.Tenants.GLOBAL).stream()
                .map(EffectivePolicyRecord::getPolicy)
                .toList());

        DefaultPolicyEngine engine = new DefaultPolicyEngine(
                new InMemoryPolicyRepository(candidate),
                new DefaultPolicyMatcher(),
                conditionEvaluator,
                new DefaultDecisionCombiner());

        for (PolicyTestCase test : tests) {
            List<Resource> resources = test.getResources() == null ? List.of() : test.getResources();
            RequestContext context = RequestContext.builder()
                    .subject(test.getSubject())
                    .resources(resources)
                    .action(test.getAction() == null ? null : new Action(test.getAction()))
                    .tenantId(test.getTenantId() == null ? tenantId : test.getTenantId())
                    .build();

            DecisionResult result = engine.evaluate(context);
            if (result.decision() != test.getExpect()) {
                throw new PolicyValidationException(String.format(
                        "Validation test '%s' failed against the candidate policy set: expected %s but got %s (%s). "
                                + "Mutation rejected, nothing was written.",
                        test.getName(), test.getExpect(), result.decision(), result.reason()));
            }
        }
    }

    private void audit(
            String tenantId,
            String policyId,
            PolicyAuditRecord.Operation operation,
            String before,
            String after,
            String actor) {
        auditStore.append(new PolicyAuditRecord(
                0, actor == null ? "unknown" : actor, Instant.now(), tenantId, policyId, operation, before, after));
    }
}
