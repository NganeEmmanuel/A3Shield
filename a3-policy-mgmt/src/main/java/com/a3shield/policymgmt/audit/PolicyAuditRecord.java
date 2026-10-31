package com.a3shield.policymgmt.audit;

import java.time.Instant;

/**
 * One immutable audit entry for a policy mutation (ADR-0003): who changed
 * what, when, with the full before/after bodies for forensics and rollback.
 */
public record PolicyAuditRecord(
        long id,
        String actor,
        Instant at,
        String tenantId,
        String policyId,
        Operation operation,
        String beforeYaml,
        String afterYaml) {

    public enum Operation {
        CREATE,
        UPDATE,
        DELETE,

        /** Startup synchronization replaced an admin-edited policy (onConflict: OVERWRITE). */
        OVERWRITE,

        /** Startup synchronization detected and recorded drift (onConflict: SKIP). */
        DRIFT
    }
}
