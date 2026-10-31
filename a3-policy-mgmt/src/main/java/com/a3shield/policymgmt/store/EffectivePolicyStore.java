package com.a3shield.policymgmt.store;

import java.util.List;
import java.util.Optional;

/**
 * The durable effective policy set (ADR-0003). Runtime admin edits and file
 * migrations both write here; evaluation reads from it (directly or via a
 * caching repository). Implementations must be thread-safe and bump
 * {@link #stateVersion()} on every mutation so cached views can detect
 * changes (ADR-0011).
 */
public interface EffectivePolicyStore {

    Optional<EffectivePolicyRecord> find(String tenantId, String policyId);

    /** All policies of one tenant scope (exact scope, no global fallback). */
    List<EffectivePolicyRecord> findByTenant(String tenantId);

    /** All tenant scopes that currently hold policies (includes {@code "*"} when global policies exist). */
    java.util.Set<String> tenants();

    void upsert(EffectivePolicyRecord record);

    /** @return true when a record existed and was removed */
    boolean delete(String tenantId, String policyId);

    /**
     * Monotonic change counter — bumped on every mutation. Cheap to poll:
     * cached repositories compare it to detect invalidation (ADR-0011).
     */
    long stateVersion();
}
