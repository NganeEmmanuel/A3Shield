package com.a3shield.enforcer;

import com.a3shield.core.decision.DecisionResult;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.engine.PolicyEngine;

/**
 * <p><b>AccessEnforcer</b> is the facade enforcement points call. It returns
 * the full {@link DecisionResult} so callers (aspects, interceptors, filters)
 * can log or surface the reason; {@link #allow} is the boolean convenience.</p>
 */
public class AccessEnforcer {

    private final PolicyEngine policyEngine;

    public AccessEnforcer(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    public DecisionResult enforce(RequestContext context) {
        return policyEngine.evaluate(context);
    }

    public boolean allow(RequestContext context) {
        return enforce(context).isAllowed();
    }
}
