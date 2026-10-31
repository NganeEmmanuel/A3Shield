package com.a3shield.policymgmt.audit;

import java.util.List;

/**
 * Append-only audit trail of policy mutations (ADR-0003). Every admin API
 * change and every startup conflict resolution writes a record with full
 * before/after bodies.
 */
public interface PolicyAuditStore {

    void append(PolicyAuditRecord record);

    /** Recent records for a tenant, newest first. */
    List<PolicyAuditRecord> findByTenant(String tenantId, int limit);
}
