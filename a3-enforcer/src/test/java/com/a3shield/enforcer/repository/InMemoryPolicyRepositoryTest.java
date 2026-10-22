package com.a3shield.enforcer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryPolicyRepositoryTest {

    private final Policy acmeReadDocument = Policy.builder()
            .id("acme-read")
            .tenantId("acme")
            .resource("document")
            .action("read")
            .effect(Effect.ALLOW)
            .build();

    private final Policy acmeAnyAction = Policy.builder()
            .id("acme-any")
            .tenantId("acme")
            .resource("document")
            .effect(Effect.ALLOW)
            .build();

    private final Policy globalWriteDocument = Policy.builder()
            .id("global-write")
            .tenantId(Tenants.GLOBAL)
            .resource("document")
            .action("write")
            .effect(Effect.DENY)
            .build();

    private final Policy otherTenant = Policy.builder()
            .id("globex-read")
            .tenantId("globex")
            .resource("document")
            .action("read")
            .effect(Effect.ALLOW)
            .build();

    private final InMemoryPolicyRepository repo =
            new InMemoryPolicyRepository(List.of(acmeReadDocument, acmeAnyAction, globalWriteDocument, otherTenant));

    @Test
    void shouldReturnTenantPoliciesPlusGlobalForMatchingActionAndResource() {
        List<Policy> found = repo.findAll("acme", new Action("read"), "document");

        assertThat(found).extracting(Policy::getId).containsExactlyInAnyOrder("acme-read", "acme-any");
    }

    @Test
    void shouldFilterByAction() {
        List<Policy> found = repo.findAll("acme", new Action("delete"), "document");

        assertThat(found).extracting(Policy::getId).containsExactly("acme-any");
    }

    @Test
    void shouldIncludeGlobalPoliciesForAnyTenant() {
        List<Policy> found = repo.findAll("acme", new Action("write"), "document");

        // acme-any has no action constraint, so it matches any action
        assertThat(found).extracting(Policy::getId).containsExactlyInAnyOrder("acme-any", "global-write");
    }

    @Test
    void shouldNeverReturnOtherTenantsPolicies() {
        List<Policy> found = repo.findAll("acme", new Action("read"), "document");

        assertThat(found).extracting(Policy::getId).doesNotContain("globex-read");
    }

    @Test
    void nullTenantShouldMeanDefaultTenant() {
        List<Policy> found = repo.findAll(null, new Action("read"), "document");

        // only global policies apply to the implicit default tenant here
        assertThat(found).isEmpty();
    }

    @Test
    void nullResourceTypeShouldOnlyMatchUnconstrainedPolicies() {
        List<Policy> found = repo.findAll("acme", new Action("read"), null);

        assertThat(found).isEmpty();
    }
}
