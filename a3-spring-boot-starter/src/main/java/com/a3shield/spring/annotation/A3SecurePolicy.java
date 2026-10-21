package com.a3shield.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * used to secure a method. the return value of that methode will be validated with against business policies.
 * policies are retrieved based on resource type and actions specified in the RequestContext for that request
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface A3SecurePolicy {
}
