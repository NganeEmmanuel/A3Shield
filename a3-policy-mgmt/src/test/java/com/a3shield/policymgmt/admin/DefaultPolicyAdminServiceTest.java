package com.a3shield.policymgmt.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.a3shield.core.model.SubjectType;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.audit.InMemoryPolicyAuditStore;
import com.a3shield.policymgmt.audit.PolicyAuditRecord;
import com.a3shield.policymgmt.migration.PolicyTestCase;
import com.a3shield.policymgmt.parse.PolicySerde;
import com.a3shield.policymgmt.store.EffectivePolicyRecord;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import com.a3shield.policymgmt.store.InMemoryEffectivePolicyStore;
import com.a3shield.policymgmt.store.Origin;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultPolicyAdminServiceTest {

    private final EffectivePolicyStore store = new InMemoryEffectivePolicyStore();
    private final InMemoryPolicyAuditStore audit = new InMemoryPolicyAuditStore();
    private final PolicySerde serde = new PolicySerde();

    private final DefaultPolicyAdminService service = new DefaultPolicyAdminService(store, audit, List::of);

    private static Policy policy(String id, Effect effect, String condition) {
        return Policy.builder()
                .id(id)
                .effect(effect)
                .tenantId("acme")
                .resource("document")
                .action("read")
                .condition(condition)
                .build();
    }

    private static PolicyTestCase ownerReads(String name, com.a3shield.core.decision.Decision expect) {
        return PolicyTestCase.builder()
                .name(name)
                .expect(expect)
                .subject(com.a3shield.core.model.Subject.builder()
                        .id("1")
                        .type(SubjectType.USER)
                        .build())
                .resources(List.of(com.a3shield.core.model.Target.builder()
                        .type("document")
                        .attributes(java.util.Map.of("ownerId", "1"))
                        .build()))
                .action("read")
                .build();
    }

    @Test
    void createShouldValidateAndAudit() {
        var record = service.create(
                "acme",
                policy("doc-read", Effect.ALLOW, "resource.ownerId == subject.id"),
                List.of(ownerReads("owner", com.a3shield.core.decision.Decision.ALLOW)),
                "alice");

        assertThat(record.getOrigin()).isEqualTo(Origin.ADMIN);
        assertThat(audit.findByTenant("acme", 10))
                .anyMatch(entry -> entry.operation() == PolicyAuditRecord.Operation.CREATE
                        && entry.actor().equals("alice"));
    }

    @Test
    void brokenConditionMustBeRejected() {
        assertThatThrownBy(() ->
                        service.create("acme", policy("bad", Effect.ALLOW, "this is ((( not valid"), null, "alice"))
                .isInstanceOf(PolicyValidationException.class)
                .hasMessageContaining("does not compile");

        assertThat(store.find("acme", "bad")).isEmpty(); // nothing written
    }

    @Test
    void failingValidationTestMustRejectMutation() {
        // the test expects ALLOW but a DENY policy denies
        assertThatThrownBy(() -> service.create(
                        "acme",
                        policy("deny-doc", Effect.DENY, null),
                        List.of(ownerReads("wrong", com.a3shield.core.decision.Decision.ALLOW)),
                        "alice"))
                .isInstanceOf(PolicyValidationException.class)
                .hasMessageContaining("test 'wrong' failed")
                .hasMessageContaining("nothing was written");

        assertThat(store.find("acme", "deny-doc")).isEmpty();
    }

    @Test
    void updateShouldKeepStoredTestsWhenTestsOmitted() {
        service.create(
                "acme",
                policy("doc-read", Effect.ALLOW, "resource.ownerId == subject.id"),
                List.of(ownerReads("owner", com.a3shield.core.decision.Decision.ALLOW)),
                "alice");

        // tighten the policy to DENY without providing tests: the stored suite
        // (owner expects ALLOW) must reject the change
        assertThatThrownBy(() -> service.update("acme", "doc-read", policy("doc-read", Effect.DENY, null), null, "bob"))
                .isInstanceOf(PolicyValidationException.class)
                .hasMessageContaining("test 'owner' failed");

        assertThat(store.find("acme", "doc-read").get().getPolicy().getEffect()).isEqualTo(Effect.ALLOW);
    }

    @Test
    void deleteShouldAuditAndRemove() {
        service.create("acme", policy("gone", Effect.ALLOW, null), null, "alice");

        assertThat(service.delete("acme", "gone", "bob")).isTrue();
        assertThat(store.find("acme", "gone")).isEmpty();
        assertThat(audit.findByTenant("acme", 10))
                .anyMatch(entry -> entry.operation() == PolicyAuditRecord.Operation.DELETE
                        && entry.actor().equals("bob"));
        assertThat(service.delete("acme", "gone", "bob")).isFalse();
    }

    @Test
    void driftShouldCompareFilesAgainstStore() {
        // store has: one file policy edited by admin, one admin-only policy
        Policy filePolicy = policy("file-p", Effect.ALLOW, null);
        store.upsert(EffectivePolicyRecord.builder()
                .tenantId("acme")
                .policy(filePolicy)
                .bodyYaml(serde.toYaml(filePolicy))
                .origin(Origin.ADMIN) // admin edited the file policy
                .build());
        Policy adminOnly = policy("admin-p", Effect.ALLOW, null);
        store.upsert(EffectivePolicyRecord.builder()
                .tenantId("acme")
                .policy(adminOnly)
                .bodyYaml(serde.toYaml(adminOnly))
                .origin(Origin.ADMIN)
                .build());

        var drift = service.drift("acme");

        assertThat(drift).extracting(PolicyAdminApi.DriftEntry::kind).contains("ADMIN_ONLY");
    }

    @Test
    void updateMustNotAllowChangingPolicyId() {
        service.create("acme", policy("p1", Effect.ALLOW, null), null, "alice");

        assertThatThrownBy(() -> service.update("acme", "p1", policy("p2", Effect.ALLOW, null), null, "alice"))
                .isInstanceOf(PolicyValidationException.class)
                .hasMessageContaining("id cannot change");
    }
}
