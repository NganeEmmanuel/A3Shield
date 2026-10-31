package com.a3shield.policymgmt.store;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link EffectivePolicyStore}: single-instance deployments and
 * tests. Every mutation bumps the state version.
 */
public class InMemoryEffectivePolicyStore implements EffectivePolicyStore {

    private final Map<String, EffectivePolicyRecord> records = new ConcurrentHashMap<>();
    private final AtomicLong stateVersion = new AtomicLong();

    private static String key(String tenantId, String policyId) {
        return tenantId + "|" + policyId;
    }

    @Override
    public Optional<EffectivePolicyRecord> find(String tenantId, String policyId) {
        return Optional.ofNullable(records.get(key(tenantId, policyId)));
    }

    @Override
    public List<EffectivePolicyRecord> findByTenant(String tenantId) {
        return records.values().stream()
                .filter(record -> record.getTenantId().equals(tenantId))
                .toList();
    }

    @Override
    public java.util.Set<String> tenants() {
        java.util.Set<String> tenants = new java.util.HashSet<>();
        records.values().forEach(record -> tenants.add(record.getTenantId()));
        return tenants;
    }

    @Override
    public void upsert(EffectivePolicyRecord record) {
        records.put(key(record.getTenantId(), record.getPolicy().getId()), record);
        stateVersion.incrementAndGet();
    }

    @Override
    public boolean delete(String tenantId, String policyId) {
        boolean removed = records.remove(key(tenantId, policyId)) != null;
        if (removed) {
            stateVersion.incrementAndGet();
        }
        return removed;
    }

    @Override
    public long stateVersion() {
        return stateVersion.get();
    }
}
