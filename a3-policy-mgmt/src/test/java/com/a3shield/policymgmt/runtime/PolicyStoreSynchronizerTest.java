package com.a3shield.policymgmt.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.audit.InMemoryPolicyAuditStore;
import com.a3shield.policymgmt.audit.PolicyAuditRecord;
import com.a3shield.policymgmt.migration.PolicyMigration;
import com.a3shield.policymgmt.parse.YamlPolicyMigrationParser;
import com.a3shield.policymgmt.store.EffectivePolicyRecord;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import com.a3shield.policymgmt.store.InMemoryEffectivePolicyStore;
import com.a3shield.policymgmt.store.Origin;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyStoreSynchronizerTest {

    private final YamlPolicyMigrationParser parser = new YamlPolicyMigrationParser();

    private static final String FILE =
            """
            tenant: acme
            policies:
              - id: doc-read-owner
                effect: ALLOW
                resource: document
                action: read
                condition: "resource.ownerId == subject.id"
            tests:
              - name: owner can read
                expect: ALLOW
                subject: {id: "1", type: USER}
                resources: [{type: document, attributes: {ownerId: "1"}}]
                action: read
            """;

    private PolicyMigration migration(String yaml) {
        return parser.parse("V1__baseline.yaml", 1, "baseline", yaml);
    }

    private PolicyStoreSynchronizer sync(
            EffectivePolicyStore store, AppliedMigrationsStore applied, InMemoryPolicyAuditStore audit) {
        return new PolicyStoreSynchronizer(store, applied, audit);
    }

    @Test
    void initialSyncPopulatesStoreAndRecordsVersion() {
        var store = new InMemoryEffectivePolicyStore();
        var audit = new InMemoryPolicyAuditStore();
        var applied = new InMemoryAppliedMigrationsStore();

        sync(store, applied, audit).sync(List.of(migration(FILE)));

        assertThat(store.find("acme", "doc-read-owner")).isPresent();
        assertThat(store.find("acme", "doc-read-owner").get().getOrigin()).isEqualTo(Origin.MIGRATION);
        assertThat(applied.isApplied(1)).isTrue();
    }

    @Test
    void reSyncWithSameChecksumIsNoOp() {
        var store = new InMemoryEffectivePolicyStore();
        var applied = new InMemoryAppliedMigrationsStore();
        var audit = new InMemoryPolicyAuditStore();
        sync(store, applied, audit).sync(List.of(migration(FILE)));
        long version = store.stateVersion();

        sync(store, applied, audit).sync(List.of(migration(FILE)));

        assertThat(store.stateVersion()).isEqualTo(version); // untouched
        assertThat(audit.findByTenant("acme", 10)).isEmpty();
    }

    @Test
    void fileEditedAfterApplyWithFailMustAbort() {
        var store = new InMemoryEffectivePolicyStore();
        var applied = new InMemoryAppliedMigrationsStore();
        var audit = new InMemoryPolicyAuditStore();
        sync(store, applied, audit).sync(List.of(migration(FILE)));

        var edited = migration(FILE + "# drift\n");
        assertThatThrownBy(() -> sync(store, applied, audit).sync(List.of(edited)))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("checksum");
    }

    @Test
    void adminEditConflictRespectsOnConflict() {
        var store = new InMemoryEffectivePolicyStore();
        var applied = new InMemoryAppliedMigrationsStore();
        var audit = new InMemoryPolicyAuditStore();
        sync(store, applied, audit).sync(List.of(migration(FILE)));

        // simulate a runtime admin edit of the file-managed policy
        Policy adminVersion = Policy.builder()
                .id("doc-read-owner")
                .effect(Effect.DENY)
                .tenantId("acme")
                .resource("document")
                .action("read")
                .build();
        store.upsert(EffectivePolicyRecord.builder()
                .tenantId("acme")
                .policy(adminVersion)
                .bodyYaml(new com.a3shield.policymgmt.parse.PolicySerde().toYaml(adminVersion))
                .origin(Origin.ADMIN)
                .build());

        // FAIL (default): startup aborts
        assertThatThrownBy(() -> sync(store, applied, audit).sync(List.of(migration(FILE))))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("modified at runtime");

        // SKIP: admin edit wins, drift audited
        sync(store, applied, audit).sync(List.of(migration("onConflict: SKIP\n" + FILE)));
        assertThat(store.find("acme", "doc-read-owner").get().getOrigin()).isEqualTo(Origin.ADMIN);
        assertThat(audit.findByTenant("acme", 10))
                .anyMatch(record -> record.operation() == PolicyAuditRecord.Operation.DRIFT);

        // OVERWRITE: file wins, admin edit reset (audited)
        sync(store, applied, audit).sync(List.of(migration("onConflict: OVERWRITE\n" + FILE)));
        assertThat(store.find("acme", "doc-read-owner").get().getOrigin()).isEqualTo(Origin.MIGRATION);
        assertThat(audit.findByTenant("acme", 10))
                .anyMatch(record -> record.operation() == PolicyAuditRecord.Operation.OVERWRITE);
    }

    @Test
    void policyRemovedFromFileIsCleanedFromStore() {
        var store = new InMemoryEffectivePolicyStore();
        var applied = new InMemoryAppliedMigrationsStore();
        var audit = new InMemoryPolicyAuditStore();
        sync(store, applied, audit).sync(List.of(migration(FILE)));

        // next version drops the policy and adds another; admin-only policy survives
        Policy adminOnly = Policy.builder()
                .id("admin-only")
                .effect(Effect.ALLOW)
                .tenantId("acme")
                .build();
        store.upsert(EffectivePolicyRecord.builder()
                .tenantId("acme")
                .policy(adminOnly)
                .bodyYaml(new com.a3shield.policymgmt.parse.PolicySerde().toYaml(adminOnly))
                .origin(Origin.ADMIN)
                .build());

        var v2 = parser.parse(
                "V2__drop.yaml",
                2,
                "drop",
                """
                tenant: acme
                policies:
                  - id: other-policy
                    effect: ALLOW
                    resource: invoice
                    action: read
                """);
        // next release: V1 is replaced by V2 (doc-read-owner no longer in any file)
        sync(store, applied, audit).sync(List.of(v2));

        assertThat(store.find("acme", "doc-read-owner")).isEmpty(); // removed from files
        assertThat(store.find("acme", "admin-only")).isPresent(); // admin edits survive
        assertThat(store.find("acme", "other-policy")).isPresent();
    }
}
