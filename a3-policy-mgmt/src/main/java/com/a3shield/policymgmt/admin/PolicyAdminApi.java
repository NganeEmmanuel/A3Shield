package com.a3shield.policymgmt.admin;

import com.a3shield.policymgmt.migration.PolicyTestCase;
import com.a3shield.policymgmt.store.EffectivePolicyRecord;
import java.util.List;

/**
 * Runtime policy administration (ADR-0003): tenant-scoped CRUD on the
 * effective policy store, always validated before commit and always
 * audited. This is the library SPI; the starter exposes it over REST
 * (opt-in) — protecting that endpoint is the host application's job.
 */
public interface PolicyAdminApi {

    EffectivePolicyRecord create(
            String tenantId, com.a3shield.enforcer.policy.Policy policy, List<PolicyTestCase> tests, String actor);

    /**
     * @param tests new validation tests; null keeps the policy's stored tests
     *              (e.g. the suite from the migration file that introduced it)
     */
    EffectivePolicyRecord update(
            String tenantId,
            String policyId,
            com.a3shield.enforcer.policy.Policy policy,
            List<PolicyTestCase> tests,
            String actor);

    boolean delete(String tenantId, String policyId, String actor);

    List<EffectivePolicyRecord> list(String tenantId);

    List<com.a3shield.policymgmt.audit.PolicyAuditRecord> audit(String tenantId, int limit);

    List<DriftEntry> drift(String tenantId);

    /** One difference between the migration files (source of truth for baselines) and the effective store. */
    record DriftEntry(String tenantId, String policyId, String fileYaml, String storeYaml, String kind) {}
}
