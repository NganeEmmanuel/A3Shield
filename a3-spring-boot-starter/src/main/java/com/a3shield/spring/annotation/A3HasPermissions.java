package com.a3shield.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * todo proper docs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface A3HasPermissions {
    String[] preAuthorize() default {};

    String[] postAuthorize() default {};
}
