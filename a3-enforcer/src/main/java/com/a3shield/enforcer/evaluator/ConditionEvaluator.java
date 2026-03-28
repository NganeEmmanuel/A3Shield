package com.a3shield.enforcer.evaluator;

import com.a3shield.enforcer.context.RequestContext;

/**
 * <p><b>ConditionEvaluator</b> defines the contract for evaluating
 * conditional expressions against a given {@link RequestContext}.</p>
 *
 * <p>Implementations of this interface are responsible for interpreting
 * an expression (such as a logical or attribute-based condition) and
 * determining whether it evaluates to {@code true} or {@code false}
 * in the context of the request.</p>
 *
 * <p>Typical responsibilities may include:</p>
 * <ul>
 *   <li>Parsing and interpreting the provided expression string.</li>
 *   <li>Fetching attributes from the {@link RequestContext} to resolve
 *       variables or placeholders in the expression.</li>
 *   <li>Applying logical operators and evaluation rules consistently.</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> Implementers should document whether their
 * evaluator implementation is safe for concurrent use.</p>
 */
public interface ConditionEvaluator {

    /**
     * <p>Evaluate the given expression against the provided request context.</p>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>The expression should be a valid string understood by the implementation.</li>
     *   <li>The context must not be {@code null}; attributes within the context
     *       are used to resolve variables in the expression.</li>
     *   <li>Implementations may throw runtime exceptions if the expression
     *       cannot be parsed or evaluated.</li>
     * </ul>
     *
     * @param expression the condition expression to evaluate; must not be null
     * @param context the request context providing attributes for evaluation; must not be null
     * @return {@code true} if the expression evaluates successfully and is satisfied,
     *         {@code false} otherwise
     */
    boolean evaluate(String expression, RequestContext context);
}
