package com.a3shield.policymgmt.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.a3shield.enforcer.action.Action;
import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.migration.OnConflict;
import com.a3shield.policymgmt.migration.PolicyMigration;
import com.a3shield.policymgmt.parse.YamlPolicyMigrationParser;
import org.junit.jupiter.api.Test;

class PolicyMigrationRunnerTest {

    private final YamlPolicyMigrationParser parser = new YamlPolicyMigrationParser();

    private static final String OWNER_ONLY =
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
              - name: stranger denied
                expect: DENY
                subject: {id: "2", type: USER}
                resources: [{type: document, attributes: {ownerId: "1"}}]
                action: read
            """;

    private PolicyMigration parse(int version, String name, String yaml) {
        return parser.parse("V" + version + "__" + name + ".yaml", version, name, yaml);
    }

    @Test
    void shouldApplyMigrationsAndRunEmbeddedTests() {
        var store = new InMemoryAppliedMigrationsStore();
        var repository = new PolicyMigrationRunner(store).apply(java.util.List.of(parse(1, "baseline", OWNER_ONLY)));

        assertThat(store.isApplied(1)).isTrue();
        assertThat(repository.policyCount()).isEqualTo(1);
        assertThat(repository.findAll("acme", new Action("read"), "document")).hasSize(1);
    }

    @Test
    void sameChecksumSecondRunSkipsTestsButKeepsEffectivePolicies() {
        var store = new InMemoryAppliedMigrationsStore();
        var runner = new PolicyMigrationRunner(store);

        runner.apply(java.util.List.of(parse(1, "baseline", OWNER_ONLY)));
        // second run (e.g. in-memory store reset semantics elsewhere): same checksum
        var repository = runner.apply(java.util.List.of(parse(1, "baseline", OWNER_ONLY)));

        assertThat(repository.policyCount()).isEqualTo(1);
    }

    @Test
    void editedFileWithFailConflictMustAbort() {
        var store = new InMemoryAppliedMigrationsStore();
        var runner = new PolicyMigrationRunner(store);
        runner.apply(java.util.List.of(parse(1, "baseline", OWNER_ONLY)));

        var edited = parse(1, "baseline", OWNER_ONLY + "# drift\n");
        assertThatThrownBy(() -> runner.apply(java.util.List.of(edited)))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("different checksum");
    }

    @Test
    void editedFileWithOverwriteConflictMustReapply() {
        var store = new InMemoryAppliedMigrationsStore();
        var runner = new PolicyMigrationRunner(store);
        runner.apply(java.util.List.of(parse(1, "baseline", OWNER_ONLY)));

        var edited = parse(1, "baseline", "onConflict: OVERWRITE\n" + OWNER_ONLY);
        var repository = runner.apply(java.util.List.of(edited));

        assertThat(store.checksumOf(1)).contains(edited.getChecksum());
        assertThat(repository.policyCount()).isEqualTo(1);
    }

    @Test
    void editedFileWithSkipConflictKeepsDriftUnrecorded() {
        var store = new InMemoryAppliedMigrationsStore();
        var runner = new PolicyMigrationRunner(store);
        var original = parse(1, "baseline", OWNER_ONLY);
        runner.apply(java.util.List.of(original));

        var edited = parse(1, "baseline", "onConflict: SKIP\n" + OWNER_ONLY);
        var repository = runner.apply(java.util.List.of(edited));

        // skipped: old checksum stays recorded (drift), policies remain effective
        assertThat(store.checksumOf(1)).contains(original.getChecksum());
        assertThat(repository.policyCount()).isEqualTo(1);
    }

    @Test
    void failingPolicyTestMustAbort() {
        var broken = parse(
                1,
                "broken",
                """
                tenant: acme
                policies:
                  - id: deny-all
                    effect: DENY
                    resource: document
                    action: read
                tests:
                  - name: wrongly expects allow
                    expect: ALLOW
                    subject: {id: "1", type: USER}
                    resources: [{type: document, attributes: {}}]
                    action: read
                """);

        var runner = new PolicyMigrationRunner(new InMemoryAppliedMigrationsStore());
        assertThatThrownBy(() -> runner.apply(java.util.List.of(broken)))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("test 'wrongly expects allow' FAILED")
                .hasMessageContaining("expected ALLOW but got DENY");
    }

    @Test
    void laterMigrationCanReplacePolicyById() {
        var store = new InMemoryAppliedMigrationsStore();
        var runner = new PolicyMigrationRunner(store);

        var v2 = parse(
                2,
                "tighten",
                """
                tenant: acme
                policies:
                  - id: doc-read-owner
                    effect: DENY
                    resource: document
                    action: read
                """);

        var repository = runner.apply(java.util.List.of(parse(1, "baseline", OWNER_ONLY), v2));

        var policies = repository.findAll("acme", new Action("read"), "document");
        assertThat(policies).hasSize(1);
        assertThat(policies.getFirst().getEffect().name()).isEqualTo("DENY");
        assertThat(store.isApplied(2)).isTrue();
    }

    @Test
    void conflictDefaultsToFailWhenUnspecified() {
        // default onConflict is FAIL - covered by editedFileWithFailConflictMustAbort
        assertThat(parse(1, "x", "policies: []\n").getOnConflict()).isEqualTo(OnConflict.FAIL);
    }
}
