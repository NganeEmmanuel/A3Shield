package com.a3shield.enforcer.evaluator;

import com.a3shield.enforcer.context.RequestContext;

/**
 * <p><b>SimpleConditionEvaluator</b> is a basic implementation of
 * {@link ConditionEvaluator} that evaluates condition expressions
 * against a given {@link RequestContext}.</p>
 *
 * <p>This evaluator is intended for straightforward use cases where
 * expressions are simple and can be directly interpreted without
 * complex parsing or external libraries.</p>
 *
 * <p>Typical responsibilities may include:</p>
 * <ul>
 *   <li>Interpreting basic string expressions (e.g., equality checks).</li>
 *   <li>Resolving attributes from the {@link RequestContext} to evaluate conditions.</li>
 *   <li>Returning a boolean result indicating whether the condition is satisfied.</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and safe
 * for concurrent use.</p>
 */
public class SimpleConditionEvaluator implements ConditionEvaluator {

    @Override
    public boolean evaluate(String expression, RequestContext context) {
        // V1: If no condition → allow
        if (expression == null || expression.isBlank()) {
            return true;
        }

        // TODO: Replace with real DSL parser
        // For now, always return true (we improve next step)
        return true;
    }
}
