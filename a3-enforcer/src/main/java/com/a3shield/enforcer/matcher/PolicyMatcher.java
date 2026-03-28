package com.a3shield.enforcer.matcher;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.policy.Policy;

import java.util.List;

/**
 * <p><b>PolicyMatcher</b> defines the contract for selecting policies
 * that are applicable to a given {@link RequestContext}.</p>
 *
 * <p>Implementations of this interface are responsible for filtering
 * or matching policies from a provided list based on attributes in
 * the request (such as subject, resource, action, or environment).</p>
 *
 * <p>Typical responsibilities may include:</p>
 * <ul>
 *   <li>Evaluating policy conditions against the request context.</li>
 *   <li>Returning only those policies that are applicable for evaluation.</li>
 *   <li>Handling cases where no policies match (returning an empty list).</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> Implementers should document whether their
 * matcher implementation is safe for concurrent use.</p>
 */
public interface PolicyMatcher {

    /**
     * <p>Match the given request context against a list of policies
     * and return those that are applicable.</p>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>The returned list must not be {@code null}; use an empty list
     *       if no policies match.</li>
     *   <li>Implementations may apply complex matching logic, such as
     *       attribute comparison or expression evaluation.</li>
     *   <li>Performance considerations should be documented if matching
     *       involves expensive operations.</li>
     * </ul>
     *
     * @param context the request context containing attributes used
     *                to determine applicable policies; must not be null
     * @param policies the list of candidate policies to evaluate; must not be null
     * @return a non-null list of {@link Policy} objects that match the request context
     */
    List<Policy> match(RequestContext context, List<Policy> policies);
}
