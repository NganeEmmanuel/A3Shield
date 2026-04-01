package com.a3shield.enforcer.evaluator;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.expression.Expression;
import com.a3shield.enforcer.expression.ExpressionParser;
import com.a3shield.enforcer.expression.ValueResolver;

/**
 * Evaluates DSL expressions against a RequestContext.
 */
public class DslConditionEvaluator implements ConditionEvaluator {

    private final ExpressionParser parser = new ExpressionParser();
    private final ValueResolver resolver = new ValueResolver();

    @Override
    public boolean evaluate(String expression, RequestContext context) {

        // No condition → allow
        if (expression == null || expression.isBlank()) {
            return true;
        }

        Expression exp = parser.parse(expression);

        Object leftValue = resolver.resolve(exp.left(), context);
        Object rightValue = resolver.resolve(exp.right(), context);

        if ("==".equals(exp.operator())) {
            return compare(leftValue, rightValue);
        }

        return false;
    }

    private boolean compare(Object left, Object right) {
        if (left == null || right == null) return false;
        return left.toString().equals(right.toString());
    }
}