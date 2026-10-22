package com.a3shield.enforcer.engine;

import com.a3shield.core.decision.DecisionResult;
import com.a3shield.enforcer.context.RequestContext;

/**
 * Package: com.a3shield.enforcer.engine
 *
 * <p><b>PolicyEngine</b> is the primary entry point for evaluating policies
 * against a request. Implementations evaluate the provided RequestContext
 * and return a structured {@link DecisionResult} describing the outcome and
 * why (ADR-0005).</p>
 *
 * <p><b>Thread-safety:</b> implementers should document whether their
 * implementation is thread-safe.</p>
 */
public interface PolicyEngine {

    /**
     * <p>Evaluate the set of policies against the given request context.</p>
     *
     * @param context the request context containing attributes required for
     *                policy evaluation; must not be null
     * @return a {@link DecisionResult}; never null
     */
    DecisionResult evaluate(RequestContext context);
}
