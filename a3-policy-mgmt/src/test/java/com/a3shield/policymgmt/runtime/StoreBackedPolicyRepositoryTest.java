package com.a3shield.policymgmt.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.parse.PolicySerde;
import com.a3shield.policymgmt.store.EffectivePolicyRecord;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import com.a3shield.policymgmt.store.InMemoryEffectivePolicyStore;
import com.a3shield.policymgmt.store.Origin;
import org.junit.jupiter.api.Test;

class StoreBackedPolicyRepositoryTest {

    private final PolicySerde serde = new PolicySerde();
    private final EffectivePolicyStore store = new InMemoryEffectivePolicyStore();

    private void put(String tenant, String id, String action, Effect effect) {
        Policy policy = Policy.builder()
                .id(id)
                .tenantId(tenant)
                .resource("document")
                .action(action)
                .effect(effect)
                .build();
        store.upsert(EffectivePolicyRecord.builder()
                .tenantId(tenant)
                .policy(policy)
                .bodyYaml(serde.toYaml(policy))
                .origin(Origin.ADMIN)
                .build());
    }

    @Test
    void shouldSeeStoreMutationsAcrossInstances() {
        // instance A reads
        var repoA = new StoreBackedPolicyRepository(store, 0);
        put("acme", "p1", "read", Effect.ALLOW);
        assertThat(repoA.findAll("acme", new Action("read"), "document")).hasSize(1);

        // a mutation happens through the store (e.g. another instance's admin API)
        put("acme", "p2", "read", Effect.ALLOW);

        // instance A sees it (poll interval 0 -> always re-check the version)
        assertThat(repoA.findAll("acme", new Action("read"), "document")).hasSize(2);
    }

    @Test
    void globalPoliciesApplyToAnyTenant() {
        put("*", "global-write", "write", Effect.DENY);

        var repository = new StoreBackedPolicyRepository(store, 0);
        assertThat(repository.findAll("acme", new Action("write"), "document")).hasSize(1);
        assertThat(repository.findAll("other", new Action("write"), "document")).hasSize(1);
        assertThat(repository.findAll("acme", new Action("read"), "document")).isEmpty();
    }
}
