package com.a3shield.core.audit;

import com.a3shield.core.decision.DecisionResult;
import java.time.Instant;
import java.util.List;

/**
 * One enforcement decision, published at every enforcement point (method
 * annotations, post-execution checks, route checks) so host applications
 * can audit-log, metric or alert on authorization activity (ADR-0004).
 */
public record EnforcementAuditEvent(
        Instant at,
        String point,
        String tenantId,
        String subjectId,
        String action,
        List<String> resourceUrns,
        DecisionResult result) {

    /** Where the decision was made. */
    public static final String POINT_ANNOTATION = "ANNOTATION";

    public static final String POINT_SECURE_POLICY = "SECURE_POLICY";
    public static final String POINT_ROUTE = "ROUTE";
}
