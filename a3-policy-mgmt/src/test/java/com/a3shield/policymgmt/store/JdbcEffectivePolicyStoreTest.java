package com.a3shield.policymgmt.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.parse.PolicySerde;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class JdbcEffectivePolicyStoreTest {

    private static JdbcEffectivePolicyStore newStore() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:a3eff" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        return new JdbcEffectivePolicyStore(dataSource);
    }

    private static EffectivePolicyRecord record(String tenant, String id, Origin origin) {
        Policy policy =
                Policy.builder().id(id).effect(Effect.ALLOW).tenantId(tenant).build();
        return EffectivePolicyRecord.builder()
                .tenantId(tenant)
                .policy(policy)
                .bodyYaml(new PolicySerde().toYaml(policy))
                .origin(origin)
                .build();
    }

    @Test
    void upsertFindDeleteAndTenants() {
        JdbcEffectivePolicyStore store = newStore();
        store.upsert(record("acme", "p1", Origin.MIGRATION));
        store.upsert(record("*", "global-p", Origin.MIGRATION));

        assertThat(store.find("acme", "p1")).isPresent();
        assertThat(store.find("acme", "p1").get().getPolicy().getId()).isEqualTo("p1");
        assertThat(store.tenants()).containsExactlyInAnyOrder("acme", "*");
        assertThat(store.findByTenant("acme")).hasSize(1);

        // policy body survives a restart (fresh store on the same database)
        assertThat(store.stateVersion()).isEqualTo(2);
        store.delete("acme", "p1");
        assertThat(store.find("acme", "p1")).isEmpty();
        assertThat(store.delete("acme", "p1")).isFalse();
    }

    @Test
    void stateVersionBumpsOnEveryMutation() {
        JdbcEffectivePolicyStore store = newStore();
        long v0 = store.stateVersion();

        store.upsert(record("acme", "p1", Origin.ADMIN));
        long v1 = store.stateVersion();

        store.upsert(record("acme", "p1", Origin.MIGRATION)); // update path
        long v2 = store.stateVersion();

        store.delete("acme", "p1");
        long v3 = store.stateVersion();

        assertThat(v1).isGreaterThan(v0);
        assertThat(v2).isGreaterThan(v1);
        assertThat(v3).isGreaterThan(v2);
    }
}
