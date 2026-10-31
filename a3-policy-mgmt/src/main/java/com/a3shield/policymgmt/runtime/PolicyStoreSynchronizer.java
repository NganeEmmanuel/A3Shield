package com.a3shield.policymgmt.runtime;

import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.audit.PolicyAuditRecord;
import com.a3shield.policymgmt.audit.PolicyAuditStore;
import com.a3shield.policymgmt.migration.OnConflict;
import com.a3shield.policymgmt.migration.PolicyMigration;
import com.a3shield.policymgmt.parse.PolicySerde;
import com.a3shield.policymgmt.store.EffectivePolicyRecord;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import com.a3shield.policymgmt.store.Origin;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Startup synchronization between migration files and the durable
 * {@link EffectivePolicyStore} (ADR-0003, superseding the M4 in-memory
 * baseline): files populate the store; runtime admin edits live in the
 * store with {@link Origin#ADMIN} and are never silently overwritten.</p>
 *
 * <p>Conflict rules (per the file's {@code onConflict}): when a file policy
 * collides with an admin-edited policy — FAIL aborts startup, OVERWRITE
 * resets to the file version (audited), SKIP keeps the admin edit (audited
 * as drift). File-edited-after-apply (checksum drift) follows the same
 * setting. Policies removed from files are cleaned from the store unless
 * admin-edited.</p>
 */
public class PolicyStoreSynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(PolicyStoreSynchronizer.class);

    private final EffectivePolicyStore store;
    private final AppliedMigrationsStore appliedMigrations;
    private final PolicyAuditStore auditStore;
    private final PolicySerde serde = new PolicySerde();

    public PolicyStoreSynchronizer(
            EffectivePolicyStore store, AppliedMigrationsStore appliedMigrations, PolicyAuditStore auditStore) {
        this.store = store;
        this.appliedMigrations = appliedMigrations;
        this.auditStore = auditStore;
    }

    public void sync(List<PolicyMigration> migrations) {
        Set<String> expectedFromFiles = new HashSet<>();

        for (PolicyMigration migration : migrations) {
            Optional<String> recorded = appliedMigrations.checksumOf(migration.getVersion());
            boolean fileEdited = recorded.isPresent() && !recorded.get().equals(migration.getChecksum());

            if (fileEdited && migration.getOnConflict() == OnConflict.FAIL) {
                throw new PolicyMigrationException(
                        "Policy migration " + migration.description() + " was modified after being applied "
                                + "(checksum mismatch). Resolve the drift or set onConflict: OVERWRITE/SKIP.");
            }

            boolean pending = recorded.isEmpty() || (fileEdited && migration.getOnConflict() == OnConflict.OVERWRITE);
            if (pending) {
                new PolicyMigrationRunner(appliedMigrations).runTestsOnly(migration);
            }

            for (com.a3shield.enforcer.policy.Policy policy : migration.getPolicies()) {
                expectedFromFiles.add(key(migration.getTenantId(), policy.getId()));
                String desiredBody = serde.toYaml(policy);
                Optional<EffectivePolicyRecord> existing = store.find(migration.getTenantId(), policy.getId());

                if (existing.isPresent() && existing.get().getOrigin() == Origin.ADMIN) {
                    switch (migration.getOnConflict()) {
                        case FAIL ->
                            throw new PolicyMigrationException(
                                    "Policy " + migration.getTenantId() + "/" + policy.getId()
                                            + " was modified at runtime (admin edit) but migration "
                                            + migration.description() + " also defines it. "
                                            + "Resolve manually or set onConflict: OVERWRITE/SKIP.");
                        case OVERWRITE -> {
                            store.upsert(record(migration, policy));
                            audit(
                                    migration.getTenantId(),
                                    policy.getId(),
                                    PolicyAuditRecord.Operation.OVERWRITE,
                                    existing.get().getBodyYaml(),
                                    desiredBody,
                                    "migration-sync");
                        }
                        case SKIP ->
                            audit(
                                    migration.getTenantId(),
                                    policy.getId(),
                                    PolicyAuditRecord.Operation.DRIFT,
                                    desiredBody,
                                    existing.get().getBodyYaml(),
                                    "migration-sync");
                    }
                    continue;
                }

                boolean unchanged = existing.isPresent()
                        && desiredBody.equals(existing.get().getBodyYaml());
                if (unchanged && !fileEdited) {
                    continue;
                }

                if (fileEdited && migration.getOnConflict() == OnConflict.SKIP && existing.isPresent()) {
                    audit(
                            migration.getTenantId(),
                            policy.getId(),
                            PolicyAuditRecord.Operation.DRIFT,
                            desiredBody,
                            existing.get().getBodyYaml(),
                            "migration-sync");
                    continue;
                }

                store.upsert(record(migration, policy));
            }

            if (pending) {
                appliedMigrations.record(migration.getVersion(), migration.getName(), migration.getChecksum());
            }
        }

        removePoliciesDeletedFromFiles(expectedFromFiles);
    }

    /** MIGRATION-origin policies no longer present in any file are removed; ADMIN edits are kept. */
    private void removePoliciesDeletedFromFiles(Set<String> expectedFromFiles) {
        for (String tenantId : store.tenants()) {
            for (EffectivePolicyRecord record : store.findByTenant(tenantId)) {
                String key = key(tenantId, record.getPolicy().getId());
                if (record.getOrigin() == Origin.MIGRATION && !expectedFromFiles.contains(key)) {
                    logger.info(
                            "Removing policy {}/{} - deleted from migration files",
                            tenantId,
                            record.getPolicy().getId());
                    store.delete(tenantId, record.getPolicy().getId());
                }
            }
        }
    }

    private EffectivePolicyRecord record(PolicyMigration migration, com.a3shield.enforcer.policy.Policy policy) {
        return EffectivePolicyRecord.builder()
                .tenantId(migration.getTenantId())
                .policy(policy)
                .bodyYaml(serde.toYaml(policy))
                .origin(Origin.MIGRATION)
                .migrationVersion(String.valueOf(migration.getVersion()))
                .tests(migration.getTests())
                .build();
    }

    private void audit(
            String tenantId,
            String policyId,
            PolicyAuditRecord.Operation operation,
            String before,
            String after,
            String actor) {
        auditStore.append(new PolicyAuditRecord(0, actor, Instant.now(), tenantId, policyId, operation, before, after));
    }

    private static String key(String tenantId, String policyId) {
        return tenantId + "|" + policyId;
    }
}
