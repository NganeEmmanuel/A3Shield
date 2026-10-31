package com.a3shield.policymgmt.parse;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import org.junit.jupiter.api.Test;

class PolicySerdeTest {

    private final PolicySerde serde = new PolicySerde();

    @Test
    void policyShouldRoundTripThroughYaml() {
        Policy original = Policy.builder()
                .id("doc-read")
                .effect(Effect.ALLOW)
                .tenantId("acme")
                .resource("document")
                .action("read")
                .subject(java.util.Map.of("type", "USER"))
                .condition("resource.ownerId == subject.id")
                .version("2")
                .build();

        Policy parsed = serde.fromYaml(serde.toYaml(original));

        assertThat(parsed.getId()).isEqualTo("doc-read");
        assertThat(parsed.getEffect()).isEqualTo(Effect.ALLOW);
        assertThat(parsed.getResource()).isEqualTo("document");
        assertThat(parsed.getAction()).isEqualTo("read");
        assertThat(parsed.getSubject()).containsEntry("type", "USER");
        assertThat(parsed.getCondition()).isEqualTo("resource.ownerId == subject.id");
        assertThat(parsed.getVersion()).isEqualTo("2");
    }

    @Test
    void validationTestsShouldRoundTrip() {
        var tests = java.util.List.of(com.a3shield.policymgmt.migration.PolicyTestCase.builder()
                .name("owner reads")
                .expect(com.a3shield.core.decision.Decision.ALLOW)
                .subject(com.a3shield.core.model.Subject.builder()
                        .id("1")
                        .type(com.a3shield.core.model.SubjectType.USER)
                        .build())
                .resources(java.util.List.of(com.a3shield.core.model.Target.builder()
                        .type("document")
                        .attributes(java.util.Map.of("ownerId", "1"))
                        .build()))
                .action("read")
                .build());

        var parsed = serde.testsFromYaml(serde.testsToYaml(tests));

        assertThat(parsed).hasSize(1);
        assertThat(parsed.getFirst().getName()).isEqualTo("owner reads");
        assertThat(parsed.getFirst().getExpect()).isEqualTo(com.a3shield.core.decision.Decision.ALLOW);
        assertThat(parsed.getFirst().getSubject().getId()).isEqualTo("1");
        assertThat(parsed.getFirst().getResources().getFirst().resolve("ownerId"))
                .isEqualTo("1");
        assertThat(parsed.getFirst().getAction()).isEqualTo("read");
    }

    @Test
    void blankTestsYamlIsEmpty() {
        assertThat(serde.testsFromYaml(null)).isEmpty();
        assertThat(serde.testsFromYaml("")).isEmpty();
    }
}
