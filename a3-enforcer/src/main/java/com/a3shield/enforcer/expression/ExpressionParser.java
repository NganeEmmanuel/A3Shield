package com.a3shield.enforcer.expression;

/**
 * <p><b>ExpressionParser</b> parses DSL condition expressions into
 * structured {@link Expression} objects.</p>
 *
 * <p>This parser currently supports simple equality expressions
 * using the {@code ==} operator. Future enhancements may extend
 * support to additional operators such as {@code !=}, {@code >},
 * or {@code <}.</p>
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
     *   <li>Only the {@code ==} operator is supported at present.</li>
     *   <li>If the expression does not contain exactly two parts separated by {@code ==},
     *       an {@link IllegalArgumentException} is thrown.</li>
     * </ul>
     *
     * @param expression the condition expression string (e.g., {@code resource.id == subject.id})
     * @return a structured {@link Expression} object, or {@code null} if the input is blank
     * @throws IllegalArgumentException if the expression format is invalid
     */
    public Expression parse(String expression) {

        if (expression == null || expression.isBlank()) {
            return null;
        }

        // Only support == for now
        String[] parts = expression.split("==");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }

        return new Expression(parts[0], "==", parts[1]);
    }
}
