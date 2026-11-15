package com.a3shield.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Post-execution enforcement (ADR-0004, enforcement point 3): the method's
 * return value is converted into target resources and re-checked against
 * policy — so a response can be filtered even when the request itself was
 * allowed.</p>
 *
 * <p>For collection returns each element is evaluated separately
 * (row-level security): the default silently filters denied rows;
 * {@code strict = true} denies the whole response if any row is denied.
 * Single-object returns are deny-or-allow.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface A3SecurePolicy {

    /**
     * The action this data access represents (e.g. {@code "read"}); matched
     * against policy actions. Empty means "any action" — policies without an
     * action constraint still apply.
     */
    String action() default "";

    /**
     * Strict mode: deny the entire response when any row of a collection
     * return is denied, instead of silently filtering it out.
     */
    boolean strict() default false;
}
