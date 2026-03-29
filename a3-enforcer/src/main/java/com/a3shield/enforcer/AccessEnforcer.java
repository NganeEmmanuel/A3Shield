package com.a3shield.enforcer;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.Decision;
import com.a3shield.enforcer.engine.PolicyEngine;

public class AccessEnforcer {

    private final PolicyEngine policyEngine;

    public AccessEnforcer(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    public boolean enforce(RequestContext context) {
        return policyEngine.evaluate(context) == Decision.ALLOW;
    }
}