package com.a3shield.enforcer.model;

/**
 * <p><b>Resource</b> is the base model for all resolvable runtime entities
 * used by the enforcer.</p>
 *
 * <p>Concrete subclasses (e.g., {@code Intent}, {@code Target}) implement
 * the {@link #resolve(String)} method to provide field/attribute values.</p>
 *
 * <p><b>Resolution behavior:</b></p>
 * <ul>
 *   <li>The supplied {@code field} represents an attribute/key to resolve.</li>
 *   <li>If the attribute is present, implementations return its value.</li>
 *   <li>If the attribute is missing, implementations returns null</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> depends on the concrete subclass implementation.
 * This base class itself is stateless.</p>
 */
public abstract class Resource {

    /**
     * <p>Resolve the given attribute/key from this resource.</p>
     *
     * @param field the attribute/key to resolve (e.g., {@code "id"}, {@code "type"}, or a custom key)
     * @return the resolved value
     */
    public abstract Object resolve(String field);
}