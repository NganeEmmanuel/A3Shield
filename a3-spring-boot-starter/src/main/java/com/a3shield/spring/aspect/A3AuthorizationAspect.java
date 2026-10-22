package com.a3shield.spring.aspect;

import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.exception.AccessDeniedException;
import com.a3shield.core.model.Resource;
import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.annotation.A3Authorize;
import com.a3shield.spring.annotation.A3Intent;
import com.a3shield.spring.context.IntentContentHolder;
import com.a3shield.spring.context.RequestContextHolder;
import java.lang.annotation.Annotation;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-execution enforcement: intercepts methods annotated with {@link A3Authorize},
 * builds an Intent resource from the {@link A3Intent}-annotated parameter and asks
 * the enforcer whether the request may proceed (ADR-0004, enforcement point 2).
 */
@Aspect
public class A3AuthorizationAspect {
    private static final Logger logger = LoggerFactory.getLogger(A3AuthorizationAspect.class);
    private final AccessEnforcer enforcer;

    public A3AuthorizationAspect(AccessEnforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Around("@annotation(a3Authorize)")
    public Object authorize(ProceedingJoinPoint joinPoint, A3Authorize a3Authorize) throws Throwable {

        String expression = a3Authorize.value();

        if (!expression.isEmpty()) {
            Object intentObject = getIntentObject(joinPoint);
            if (intentObject != null) {
                preAuthorize(intentObject);
            } else {
                logger.debug(
                        "@A3Authorize(\"{}\") has no @A3Intent-annotated parameter to build an intent from; no pre-check performed",
                        expression);
            }
        }

        return joinPoint.proceed();
    }

    private Object getIntentObject(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] parameterAnnotations = methodSignature.getMethod().getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof A3Intent) {
                    return args[i];
                }
            }
        }

        return null;
    }

    private void preAuthorize(Object intentObject) {
        Resource intent = IntentContentHolder.getCurrentIntent(intentObject);
        RequestContext context = RequestContextHolder.getRequestContext(List.of(intent));

        DecisionResult result = enforcer.enforce(context);
        if (!result.isAllowed()) {
            throw new AccessDeniedException("Access Denied: " + result.reason(), result);
        }
    }
}
