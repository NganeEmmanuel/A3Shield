package com.a3shield.enforcer.evaluator;

import com.a3shield.enforcer.cache.ExpressionCache;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.expression.Expression;
import com.a3shield.enforcer.expression.ExpressionParser;
import com.a3shield.enforcer.expression.ValueResolver;
import java.util.List;
import java.util.Objects;

/**
 * <p><b>DslConditionEvaluator</b> is an advanced implementation of
 * {@link ConditionEvaluator} that supports evaluation of DSL expressions
 * with logical operators and comparison operators.</p>
 *
 * <p>Supported features:</p>
 * <ul>
 *   <li>Logical operators: <b>AND</b>, <b>OR</b></li>
 *   <li>Comparison operators: <b>==</b>, <b>!=</b>, <b>&gt;</b>, <b>&lt;</b></li>
 * </ul>
 *
 * <p>Evaluation process:</p>
 * <ol>
 *   <li>If the expression is {@code null} or blank, return {@code true} (allow by default).</li>
 *   <li>Split the expression by <b>OR</b> blocks and evaluate each block.</li>
 *   <li>Within each block, split by <b>AND</b> and evaluate all conditions.</li>
 *   <li>Parse each condition into an {@link Expression} and resolve values using {@link ValueResolver}.</li>
 *   <li>Apply the comparison operator to determine the result.</li>
 * </ol>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and safe
 * for concurrent use.</p>
 */
public class DslConditionEvaluator implements ConditionEvaluator {

    private final ExpressionParser parser = new ExpressionParser();
    private final ValueResolver resolver = new ValueResolver();
    private final ExpressionCache cache = new ExpressionCache();

    /**
     * <p>Evaluate a DSL expression against the provided request context.</p>
     *
     * @param expression the DSL condition string (supports AND/OR and ==, !=, &gt;, &lt;)
     * @param context the request context providing attributes for evaluation
     * @return {@code true} if the expression evaluates successfully and is satisfied,
     *         {@code false} otherwise
     */
    @Override
    public boolean evaluate(String expression, RequestContext context) {

        // if no condition return true (allow by default)
        if (expression == null || expression.isBlank()) {
            return true;
        }

        // OR has lowest priority → evaluate first
        String[] orParts = expression.split("(?i)\\s+OR\\s+");

        for (String orPart : orParts) {
            if (evaluateAndBlock(orPart, context)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>Evaluate an AND block of conditions.</p>
     *
     * @param expression the AND block expression
     * @param context the request context
     * @return {@code true} if all conditions in the block are satisfied
     */
    private boolean evaluateAndBlock(String expression, RequestContext context) {

        String[] andParts = expression.split("(?i)\\s+AND\\s+");

        for (String part : andParts) {
            if (!evaluateSingle(part.trim(), context)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>Evaluate a single condition expression.</p>
     *
     * @param expr the condition string
     * @param context the request context
     * @return {@code true} if the condition is satisfied, {@code false} otherwise
     */

    // update idea is that since the we compare each field, we have to compare both in the case of single item resource
    // or multiple items resources
    // so for each item we run the compare for each field
    private boolean evaluateSingle(String expr, RequestContext context) {

        Expression parsed = getOrParse(expr);

        Object leftRaw = resolver.resolve(parsed.left(), context);
        Object rightRaw = resolver.resolve(parsed.right(), context);

        List<?> leftList = toList(leftRaw);
        List<?> rightList = toList(rightRaw);

        // ANY match semantics (default)
        return leftList.stream()
                .anyMatch(l -> rightList.stream().anyMatch(r -> evaluateComparison(l, r, parsed.operator())));
    }

    /**
     * Fetch expression from cache or parse and store it.
     * @param expr the condition string
     * @return a structured {@link Expression} object
     */
    private Expression getOrParse(String expr) {

        if (cache.contains(expr)) {
            return cache.get(expr);
        }

        Expression parsed = parser.parse(expr);
        cache.put(expr, parsed);

        return parsed;
    }

    private boolean equals(Object left, Object right) {
        if (left == null || right == null) return false;
        return Objects.equals(left, right);
    }

    private int compare(Object left, Object right) {
        if (left == null || right == null) return 0;

        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }

        try {
            double l = Double.parseDouble(left.toString());
            double r = Double.parseDouble(right.toString());
            return Double.compare(l, r);
        } catch (Exception e) {
            return 0;
        }
    }

    private List<?> toList(Object obj) {
        if (obj == null) return List.of((Object) null);
        if (obj instanceof List<?>) return (List<?>) obj;
        return List.of(obj);
    }

    private boolean evaluateComparison(Object left, Object right, String operator) {
        return switch (operator) {
            case "==" -> equals(left, right);
            case "!=" -> !equals(left, right);
            case ">" -> compare(left, right) > 0;
            case "<" -> compare(left, right) < 0;
            default -> false;
        };
    }

    boolean compareAnyMatch(Object left, Object right) {
        if (right instanceof List<?>) {
            return ((List<?>) right).stream().anyMatch(r -> Objects.equals(left, r));
        }
        return Objects.equals(left, right);
    }
}
