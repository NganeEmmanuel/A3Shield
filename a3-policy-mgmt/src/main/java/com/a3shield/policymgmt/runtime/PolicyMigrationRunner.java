package com.a3shield.policymgmt.runtime;

import com.a3shield.core.decision.Decision;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.DefaultDecisionCombiner;
import com.a3shield.enforcer.engine.DefaultPolicyEngine;
import com.a3shield.enforcer.evaluator.CelConditionEvaluator;
import com.a3shield.enforcer.evaluator.ConditionEvaluator;
import com.a3shield.enforcer.matcher.DefaultPolicyMatcher;
import com.a3shield.enforcer.repository.InMemoryPolicyRepository;
import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.migration.OnConflict;
import com.a3shield.policymgmt.migration.PolicyMigration;
import com.a3shield.policymgmt.migration.PolicyTestCase;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Applies policy migrations at startup (ADR-0003): for each migration in
 * version order — skip when already applied with the same checksum; resolve
 * checksum conflicts via the file's {@link OnConflict} (FAIL aborts startup);
 * otherwise run the migration's embedded policy tests and, when they pass,
 * fold its policies into the effective repository and record the version.</p>
 *
 * <p>Fail-closed throughout: parse errors, duplicate versions, failing
 * policy tests and FAIL-conflicts all abort startup.</p>
 */
public class PolicyMigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PolicyMigrationRunner.class);

    private final AppliedMigrationsStore appliedMigrations;
    private final ConditionEvaluator conditionEvaluator;

    public PolicyMigrationRunner(AppliedMigrationsStore appliedMigrations) {
        this(appliedMigrations, new CelConditionEvaluator());
    }

    public PolicyMigrationRunner(AppliedMigrationsStore appliedMigrations, ConditionEvaluator conditionEvaluator) {
        this.appliedMigrations = appliedMigrations;
        this.conditionEvaluator = conditionEvaluator;
    }

    /**
     * Apply all migrations (in version order) and return the effective policy
     * repository. The discovered files are always the baseline of the
     * effective set; the applied-versions store governs whether tests
     * (re-)run and how checksum conflicts resolve.
     */
    public MigrationPolicyRepository apply(List<PolicyMigration> migrations) {
        MigrationPolicyRepository repository = new MigrationPolicyRepository();
        int applied = 0;
        int skipped = 0;

        for (PolicyMigration migration : migrations) {
            Optional<String> recorded = appliedMigrations.checksumOf(migration.getVersion());

            if (recorded.isEmpty()) {
                applyMigration(migration, repository);
                applied++;
            } else if (recorded.get().equals(migration.getChecksum())) {
                logger.info(
                        "Policy migration {} already applied, skipping (tests not re-run)", migration.description());
                skipped++;
            } else {
                switch (migration.getOnConflict()) {
                    case FAIL ->
                        throw new PolicyMigrationException("Policy migration " + migration.description()
                                + " was already applied with a different checksum "
                                + "(applied=" + recorded.get().substring(0, 8) + "…, file="
                                + migration.getChecksum().substring(0, 8)
                                + "…). Resolve the drift or set onConflict: OVERWRITE/SKIP.");
                    case OVERWRITE -> {
                        logger.warn(
                                "Policy migration {} was edited after being applied; OVERWRITE: re-applying",
                                migration.description());
                        applyMigration(migration, repository);
                        applied++;
                    }
                    case SKIP -> {
                        // M4: applied policy bodies are not persisted (only checksums),
                        // so the current file stays in the effective set and the drift
                        // is logged; the new checksum is NOT recorded.
                        logger.warn(
                                "Policy migration {} was edited after being applied; SKIP: drift recorded",
                                migration.description());
                        skipped++;
                    }
                }
            }

            // the file is the baseline: its policies are always in the effective set
            repository.put(migration.getTenantId(), migration.getPolicies());
        }

        logger.info(
                "Policy migrations complete: {} applied, {} skipped, {} effective policies",
                applied,
                skipped,
                repository.policyCount());
        return repository;
    }

    private void applyMigration(PolicyMigration migration, MigrationPolicyRepository repository) {
        runPolicyTests(migration);
        repository.put(migration.getTenantId(), migration.getPolicies());
        appliedMigrations.record(migration.getVersion(), migration.getName(), migration.getChecksum());
        logger.info(
                "Applied policy migration {}: {} policies, {} tests",
                migration.description(),
                migration.getPolicies().size(),
                migration.getTests().size());
    }

    /** Policy tests run against the migration's own policies in isolation. */
    private void runPolicyTests(PolicyMigration migration) {
        if (migration.getTests().isEmpty()) {
            return;
        }
        DefaultPolicyEngine engine = new DefaultPolicyEngine(
                new InMemoryPolicyRepository(migration.getPolicies()),
                new DefaultPolicyMatcher(),
                conditionEvaluator,
                new DefaultDecisionCombiner());

        for (PolicyTestCase test : migration.getTests()) {
            RequestContext context = RequestContext.builder()
                    .subject(test.getSubject())
                    .resources(test.getResources())
                    .action(test.getAction() == null ? null : new Action(test.getAction()))
                    .tenantId(test.getTenantId() == null ? migration.getTenantId() : test.getTenantId())
                    .build();

            DecisionResult result = engine.evaluate(context);
            if (result.decision() != test.getExpect()) {
                throw new PolicyMigrationException(String.format(
                        "Policy migration %s: test '%s' FAILED - expected %s but got %s (%s)",
                        migration.description(), test.getName(), test.getExpect(), result.decision(), result.reason()));
            }
            logger.debug(
                    "Policy migration {} test '{}' passed ({})",
                    migration.description(),
                    test.getName(),
                    Decision.ALLOW == test.getExpect() ? "allowed" : "denied");
        }
    }
}
