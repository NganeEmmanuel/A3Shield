package com.a3shield.enforcer.expression;

/**
 * <p><b>ExpressionParser</b> parses DSL condition expressions into
 * structured {@link Expression} objects.</p>
 *
 * <p>This parser currently supports operators such as {@code ==}, {@code !=}, {@code >},
 * and {@code <}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ExpressionParser parser = new ExpressionParser();
 * Expression expr = parser.parse("resource.ownerId == subject.id");
 * }</pre>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and safe
 * for concurrent use.</p>
 */
public class ExpressionParser {

    /**
     * <p>Parse a condition expression string into an {@link Expression} object.</p>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>If the expression is {@code null} or blank, {@code null} is returned.</li>
     *   <li>If the expression does not contain exactly two parts separated by a {@code Comparison operators}, or if the operator is not supported, then
     *       an {@link IllegalArgumentException} is thrown.</li>
     * </ul>
     *
     * <p>Supported operators:</p>
     * <ul>
     *   <li>Comparison operators: {@code ==}, {@code !=}, {@code >}, and {@code <}.</p>
     * </ul>

     * @param expression the condition expression string (e.g., {@code resource.id == subject.id})
     * @return a structured {@link Expression} object, or {@code null} if the input is blank
     * @throws IllegalArgumentException if the expression format is invalid
     */
    public Expression parse(String expression) {

        if (expression == null || expression.isBlank()) {
            return null;
        }

        if (expression.contains("==")) return split(expression, "==");
        if (expression.contains("!=")) return split(expression, "!=");
        if (expression.contains(">")) return split(expression, ">");
        if (expression.contains("<")) return split(expression, "<");

        throw new IllegalArgumentException("Invalid expression: " + expression);
    }

    private Expression split(String expr, String operator) {
        String[] parts = expr.split(operator);
        return new Expression(parts[0], operator, parts[1]);
    }
}
