package com.a3shield.enforcer.repository;

import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.policy.Policy;

import java.util.List;
import java.util.Objects;

/**
 * <p><b>InMemoryPolicyRepository</b> holds policies in a list and performs
 * real tenant/action/resource filtering on lookup. Suitable for tests and
 * simple embedded deployments; persistent stores arrive with M4.</p>
 */
public class InMemoryPolicyRepository implements PolicyRepository {

    private final List<Policy> policies;

    public InMemoryPolicyRepository(List<Policy> policies) {
        this.policies = List.copyOf(policies);
    }

    @Override
    public List<Policy> findAll(String tenantId, Action action, String resourceType) {
        String scope = tenantId == null ? Tenants.DEFAULT_TENANT : tenantId;

        return policies.stream()
                .filter(p -> isTenantInScope(p, scope))
                .filter(p -> matchesAction(p, action))
                .filter(p -> matchesResource(p, resourceType))
                .toList();
    }

    private boolean isTenantInScope(Policy policy, String tenantId) {
        return Tenants.GLOBAL.equals(policy.effectiveTenantId())
                || policy.effectiveTenantId().equals(tenantId);
    }

    private boolean matchesAction(Policy policy, Action action) {
        if (policy.getAction() == null) return true;
        if (action == null || action.getName() == null) return false;
        return policy.getAction().equalsIgnoreCase(action.getName());
    }

    private boolean matchesResource(Policy policy, String resourceType) {
        if (policy.getResource() == null) return true;
        return Objects.equals(policy.getResource(), resourceType);
    }
}
