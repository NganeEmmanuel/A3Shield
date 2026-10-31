package com.a3shield.policymgmt.migration;

import com.a3shield.core.decision.Decision;
import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Subject;
import lombok.Builder;
import lombok.Getter;

/**
 * One embedded policy test case (ADR-0003): build a RequestContext from the
 * case, evaluate it against the migration's own policies, and require the
 * expected decision. Executed at apply time; a failing test aborts startup.
 */
@Getter
@Builder
public class PolicyTestCase {

    private final String name;
    private final Subject subject;
    private final java.util.List<Resource> resources;
    private final String action;
    private final String tenantId;
    private final Decision expect;
}
