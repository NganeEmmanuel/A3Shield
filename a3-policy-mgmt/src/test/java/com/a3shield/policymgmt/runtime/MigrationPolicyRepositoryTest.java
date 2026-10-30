package com.a3shield.policymgmt.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationPolicyRepositoryTest {

    private MigrationPolicyRepository repositoryWith(Policy... policies) {
        var repository = new MigrationPolicyRepository();
        for (Policy policy : policies) {
            repository.put(policy.effectiveTenantId(), List.of(policy));
        }
        return repository;
    }

    private Policy policy(String id, String tenantId, String action, String resource, Effect effect) {
        return Policy.builder()
                .id(id)
                .tenantId(tenantId)
                .action(action)
                .resource(resource)
                .effect(effect)
                .build();
    }

    @Test
    void shouldReturnTenantPoliciesPlusGlobal() {
        var repository = repositoryWith(
                policy("acme-read", "acme", "read", "document", Effect.ALLOW),
                policy("global-read", "*", "read", "document", Effect.ALLOW),
                policy("other-read", "globex", "read", "document", Effect.ALLOW),
                policy("acme-write", "acme", "write", "document", Effect.ALLOW));

        var found = repository.findAll("acme", new Action("read"), "document");

        assertThat(found).extracting(Policy::getId).containsExactlyInAnyOrder("acme-read", "global-read");
    }

    @Test
    void sameIdInLaterPutReplacesEarlierDefinition() {
        var repository = new MigrationPolicyRepository();
        repository.put("acme", List.of(policy("p1", "acme", "read", "document", Effect.ALLOW)));
        repository.put("acme", List.of(policy("p1", "acme", "read", "document", Effect.DENY)));

        var found = repository.findAll("acme", new Action("read"), "document");
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getEffect()).isEqualTo(Effect.DENY);
    }

    @Test
    void nullActionOrResourceOnlyMatchesUnconstrainedPolicies() {
        var repository = repositoryWith(
                policy("any-action", "acme", null, "document", Effect.ALLOW),
                policy("read-only", "acme", "read", "document", Effect.ALLOW));

        assertThat(repository.findAll("acme", new Action("delete"), "document"))
                .extracting(Policy::getId)
                .containsExactly("any-action");
        assertThat(repository.findAll("acme", null, null)).isEmpty();
    }
}
