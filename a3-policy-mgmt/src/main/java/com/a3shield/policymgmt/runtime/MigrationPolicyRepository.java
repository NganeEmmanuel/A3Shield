package com.a3shield.policymgmt.runtime;

import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.enforcer.repository.PolicyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The effective policy set built from applied migrations: policies keyed by
 * (tenant, id). A later migration reusing a policy id replaces the earlier
 * definition — that is the migration versioning model. Tenant-aware lookup
 * follows the {@link PolicyRepository} contract (tenant + global scope).
 *
 * <p>M4: rebuilt from migrations at every startup. Runtime admin edits (M5)
 * will mutate this set and persist through a PolicyStore.</p>
 */
public class MigrationPolicyRepository implements PolicyRepository {

    private final Map<String, Map<String, Policy>> byTenant = new ConcurrentHashMap<>();

    /** Replace (or add) policies of one tenant scope; empty list removes the scope. */
    public void put(String tenantId, List<Policy> policies) {
        Map<String, Policy> scope = byTenant.computeIfAbsent(tenantId, key -> new ConcurrentHashMap<>());
        for (Policy policy : policies) {
            scope.put(policy.getId(), policy);
        }
    }

    public int policyCount() {
        return byTenant.values().stream().mapToInt(Map::size).sum();
    }

    @Override
    public List<Policy> findAll(String tenantId, Action action, String resourceType) {
        String scope = tenantId == null ? Tenants.DEFAULT_TENANT : tenantId;
        List<Policy> result = new ArrayList<>();
        for (String candidate : List.of(scope, Tenants.GLOBAL)) {
            Map<String, Policy> tenantPolicies = byTenant.get(candidate);
            if (tenantPolicies == null) {
                continue;
            }
            for (Policy policy : tenantPolicies.values()) {
                if (matches(policy, action, resourceType)) {
                    result.add(policy);
                }
            }
        }
        return result;
    }

    private boolean matches(Policy policy, Action action, String resourceType) {
        if (policy.getAction() != null) {
            if (action == null
                    || action.getName() == null
                    || !policy.getAction().equalsIgnoreCase(action.getName())) {
                return false;
            }
        }
        return policy.getResource() == null || Objects.equals(policy.getResource(), resourceType);
    }
}
