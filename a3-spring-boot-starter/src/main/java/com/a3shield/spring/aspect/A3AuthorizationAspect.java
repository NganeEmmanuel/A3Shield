package com.a3shield.spring.aspect;

import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.Resource;
import com.a3shield.spring.annotation.A3Authorize;

import com.a3shield.spring.annotation.A3Intent;
import com.a3shield.spring.context.IntentContentHolder;
import com.a3shield.spring.context.RequestContextHolder;
import com.a3shield.spring.exceptions.AccessDeniedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Intercepts methods annotated with @A3Authorize
 * and enforces access control.
 */
@Aspect
@Component
public class A3AuthorizationAspect {
    private static final Logger logger = LoggerFactory.getLogger(A3AuthorizationAspect.class);
    private final AccessEnforcer enforcer;

    public A3AuthorizationAspect(AccessEnforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Around("@annotation(a3Authorize)")
    public Object authorize(ProceedingJoinPoint joinPoint, A3Authorize a3Authorize) throws Throwable {

        String expression = a3Authorize.value();

        if(!expression.isEmpty()){
            Object intentObject = getIntentObject(joinPoint);
            if(intentObject != null){
                // perform preAuthorization checks with intent
                preAuthorize(joinPoint);
            }else{
                logger.debug("static use of an expression directly in @A3Authorize() annotation requires the use of @A3Intent in the method parameter to mark the resource to be used in the comparison");
            }
        }

        return joinPoint.proceed();
    }

    private Object getIntentObject(ProceedingJoinPoint joinPoint){
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

    private void preAuthorize(Object intentObject) throws AccessDeniedException {
        Resource intent = IntentContentHolder.getCurrentIntent(intentObject);
        RequestContext context = RequestContextHolder.getRequestContext(List.of(intent));

        boolean allowed = enforcer.enforce(context);

        if (!allowed) {
            throw new AccessDeniedException("Access Denied"); // use A3AccessDeniedException
        }
    }

}