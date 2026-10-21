package com.a3shield.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the method parameter that describes what the caller intends to act
 * on; {@link com.a3shield.spring.aspect.A3AuthorizationAspect} builds the
 * Intent resource from it for pre-execution checks with {@link A3Authorize}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface A3Intent {
}
