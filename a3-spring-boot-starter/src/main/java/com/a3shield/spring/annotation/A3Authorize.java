package com.a3shield.spring.annotation;

import java.lang.annotation.*;

/**
 * Annotation for method-level authorization using A3Shield.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface A3Authorize {

    /**
     * DSL condition expression.
     */
    String value();
}