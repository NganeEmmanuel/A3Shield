package com.a3shield.policymgmt.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.migration.OnConflict;
import org.junit.jupiter.api.Test;

class YamlPolicyMigrationParserTest {

    private final YamlPolicyMigrationParser parser = new YamlPolicyMigrationParser();

    private static final String VALID =
            """
            # baseline document policies for the acme tenant
            tenant: acme
            onConflict: FAIL
            policies:
              - id: doc-read-owner
                effect: ALLOW
                resource: document
                action: read
                subject:
                  type: USER
                condition: "resource.ownerId == subject.id"
                version: "1"
            tests:
              - name: owner can read
                expect: ALLOW
                subject:
                  id: "1"
                  type: USER
                resources:
                  - type: document
                    attributes:
                      ownerId: "1"
                action: read
              - name: stranger denied
                expect: DENY
                subject:
                  id: "2"
                  type: USER
                resources:
                  - type: document
                    attributes:
                      ownerId: "1"
                action: read
            """;

    @Test
    void shouldParsePoliciesTestsAndMetadata() {
        var migration = parser.parse("V1__baseline.yaml", 1, "baseline", VALID);

        assertThat(migration.getVersion()).isEqualTo(1);
        assertThat(migration.getTenantId()).isEqualTo("acme");
        assertThat(migration.getOnConflict()).isEqualTo(OnConflict.FAIL);
        assertThat(migration.getPolicies()).hasSize(1);

        var policy = migration.getPolicies().getFirst();
        assertThat(policy.getId()).isEqualTo("doc-read-owner");
        assertThat(policy.getEffect()).isEqualTo(Effect.ALLOW);
        assertThat(policy.getTenantId()).isEqualTo("acme");
        assertThat(policy.getCondition()).isEqualTo("resource.ownerId == subject.id");

        assertThat(migration.getTests()).hasSize(2);
        var first = migration.getTests().getFirst();
        assertThat(first.getName()).isEqualTo("owner can read");
        assertThat(first.getExpect().name()).isEqualTo("ALLOW");
        assertThat(first.getSubject().getId()).isEqualTo("1");
        assertThat(first.getResources()).hasSize(1);
    }

    @Test
    void defaultsShouldApply() {
        var migration = parser.parse("V1__minimal.yaml", 1, "minimal", "policies: []\n");

        assertThat(migration.getTenantId()).isEqualTo(Tenants.DEFAULT_TENANT);
        assertThat(migration.getOnConflict()).isEqualTo(OnConflict.FAIL);
        assertThat(migration.getTests()).isEmpty();
    }

    @Test
    void checksumMustBeStableAndContentSensitive() {
        var a = parser.parse("V1__a.yaml", 1, "a", VALID);
        var b = parser.parse("V1__a.yaml", 1, "a", VALID);
        var c = parser.parse("V1__a.yaml", 1, "a", VALID + "# edited\n");

        assertThat(a.getChecksum()).isEqualTo(b.getChecksum()).hasSize(64);
        assertThat(a.getChecksum()).isNotEqualTo(c.getChecksum());
    }

    @Test
    void brokenFilesMustFailWithClearErrors() {
        assertThatThrownBy(() -> parser.parse("V1__x.yaml", 1, "x", "policies:\n  - effect: ALLOW\n"))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("missing 'id'");

        assertThatThrownBy(() -> parser.parse("V1__x.yaml", 1, "x", "policies:\n  - id: p1\n"))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("missing 'effect'");

        assertThatThrownBy(() -> parser.parse("V1__x.yaml", 1, "x", "onConflict: NOPE\npolicies: []\n"))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("invalid onConflict");

        assertThatThrownBy(() -> parser.parse("V1__x.yaml", 1, "x", "just a string"))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("expected a YAML mapping");
    }
}
