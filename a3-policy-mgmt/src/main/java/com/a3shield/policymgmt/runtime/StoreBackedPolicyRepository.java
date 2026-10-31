package com.a3shield.policymgmt.runtime;

import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.enforcer.repository.PolicyRepository;
import com.a3shield.policymgmt.store.EffectivePolicyRecord;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link PolicyRepository} reading the durable {@link EffectivePolicyStore}
 * with poll-based cache invalidation (ADR-0011): the store's state version is
 * checked at most once per poll interval (default 5s); a version change
 * reloads the snapshot. This makes runtime admin edits visible to other
 * instances sharing the same database without any external pub-sub.
 *
 * <p>Thread-safety: the snapshot swap is atomic; reads are wait-free.</p>
 */
public class StoreBackedPolicyRepository implements PolicyRepository {

    private final EffectivePolicyStore store;
    private final long pollIntervalMillis;
    private final AtomicLong lastPollNanos = new AtomicLong(Long.MIN_VALUE);

    private volatile List<Policy> snapshot = List.of();
    private volatile long observedVersion = -1;

    public StoreBackedPolicyRepository(EffectivePolicyStore store) {
        this(store, 5000);
    }

    public StoreBackedPolicyRepository(EffectivePolicyStore store, long pollIntervalMillis) {
        this.store = store;
        this.pollIntervalMillis = pollIntervalMillis;
    }

    @Override
    public List<Policy> findAll(String tenantId, Action action, String resourceType) {
        refreshIfStale();
        String scope = tenantId == null ? Tenants.DEFAULT_TENANT : tenantId;

        List<Policy> result = new ArrayList<>();
        for (Policy policy : snapshot) {
            boolean tenantMatch = Tenants.GLOBAL.equals(policy.effectiveTenantId())
                    || policy.effectiveTenantId().equals(scope);
            if (tenantMatch && matches(policy, action, resourceType)) {
                result.add(policy);
            }
        }
        return result;
    }

    /** Force a snapshot reload on the next read (used after local mutations). */
    public void invalidate() {
        observedVersion = -1;
    }

    private void refreshIfStale() {
        long now = System.nanoTime();
        long last = lastPollNanos.get();
        if (now - last < pollIntervalMillis * 1_000_000L && last != Long.MIN_VALUE) {
            return;
        }
        if (!lastPollNanos.compareAndSet(last, now)) {
            return; // another thread is refreshing
        }

        long version = store.stateVersion();
        if (version != observedVersion) {
            List<Policy> reloaded = new ArrayList<>();
            for (String tenant : store.tenants()) {
                for (EffectivePolicyRecord record : store.findByTenant(tenant)) {
                    reloaded.add(record.getPolicy());
                }
            }
            snapshot = List.copyOf(reloaded);
            observedVersion = version;
        }
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
