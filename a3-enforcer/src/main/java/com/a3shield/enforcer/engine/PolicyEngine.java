/**
 * Package: com.a3shield.enforcer.engine
 *
 * <p><b>PolicyEngine</b> defines the contract for evaluating access control policies
 * against a request context and producing a Decision.</p>
 *
 * <p>Implementations of this interface are responsible for applying one or more policies,
 * resolving obligations or advice, and producing a Decision that represents the outcome
 * (for example, ALLOW, DENY).</p>
 *
 * <p>Typical implementations may:</p>
 * <ul>
 *   <li>Load policies from a repository (files, database, remote service).</li>
 *   <li>Combine multiple policy evaluation results using a combining algorithm.</li>
 *   <li>Evaluate expressions and fetch attributes from the RequestContext.</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> implementers should document whether their implementation is
 * thread-safe. Many engines are safe for concurrent use; if stateful behavior is required,
 * use appropriate synchronization or provide one engine instance per thread/request.</p>
 */
package com.a3shield.enforcer.engine;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.Decision;

/**
 * <p><b>PolicyEngine</b> is the primary entry point for evaluating policies against a request.
 * Implementations evaluate the provided RequestContext and return a Decision describing
 * whether the request is allowed, denied, not applicable, or indeterminate.</p>
 */
public interface PolicyEngine {

    /**
     * <p>Evaluate the set of policies against the given request context and return a Decision.</p>
     *
     * <p>Implementations should:</p>
     * <ul>
     *   <li>Interpret the RequestContext to obtain attributes (subject, resource, action, environment).</li>
     *   <li>Apply the policy or policy set and combine results according to the configured combining algorithm.</li>
     *   <li>Populate the returned Decision with the effect (e.g., PERMIT/DENY), any obligations,
     *       and diagnostic information useful for auditing or debugging.</li>
     * </ul>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>The method should not mutate the provided RequestContext unless explicitly documented.</li>
     *   <li>Implementations may throw runtime exceptions for unrecoverable errors (for example,
     *       policy repository failures). Consider returning an INDETERMINATE Decision for recoverable
     *       policy-evaluation errors if you need to represent the error in the policy model.</li>
     * </ul>
     *
     * @param context the request context containing attributes required for policy evaluation;
     *                must not be null
     * @return a Decision representing the outcome of the evaluation. Never null; if an engine
     *         cannot produce a decision, return a Decision with an INDETERMINATE effect and
     *         diagnostic details.
     */
    Decision evaluate(RequestContext context);
}
