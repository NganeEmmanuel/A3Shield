package com.a3shield.enforcer.expression;

/**
 * <p><b>Expression</b> represents a parsed condition expression
 * used in policy evaluation.</p>
 *
 * <p>An expression consists of three parts:</p>
 * <ul>
 *   <li><b>Left operand</b> – typically an attribute reference (e.g., {@code resource.ownerId}).</li>
 *   <li><b>Operator</b> – a comparison operator (e.g., {@code ==}, {@code !=}, {@code >}).</li>
 *   <li><b>Right operand</b> – typically another attribute reference or a literal value (e.g., {@code subject.id}).</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * resource.ownerId == subject.id
 * }</pre>
 *
 * <p><b>Thread-safety:</b> This record is immutable and therefore safe
 * for concurrent use.</p>
 *
 * @param left the left-hand side of the expression; trimmed of whitespace
 * @param operator the comparison operator; trimmed of whitespace
 * @param right the right-hand side of the expression; trimmed of whitespace
 */
public record Expression(String left, String operator, String right) {

    /**
     * <p>Create a new {@code Expression} with trimmed operands and operator.</p>
     *
     * @param left the left-hand side of the expression; must not be null
     * @param operator the comparison operator; must not be null
     * @param right the right-hand side of the expression; must not be null
     */
    public Expression(String left, String operator, String right) {
        this.left = left.trim();
        this.operator = operator.trim();
        this.right = right.trim();
    }
}
