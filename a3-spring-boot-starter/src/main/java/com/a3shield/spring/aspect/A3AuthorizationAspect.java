package com.a3shield.spring.aspect;

import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.Subject;
import com.a3shield.spring.annotation.A3Authorize;

import com.a3shield.spring.security.SecurityContextExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Intercepts methods annotated with @A3Authorize
 * and enforces access control.
 */
@Aspect
@Component
public class A3AuthorizationAspect {

    private final AccessEnforcer enforcer;
    private final SecurityContextExtractor extractor = new SecurityContextExtractor();

    public A3AuthorizationAspect(AccessEnforcer enforcer) {
        this.enforcer = enforcer;
    }

    // todo we can have two aspects - one (a3Authorize) for direct string expressions and another (a3CheckPermission) that takes in a permission(DOCUMENT_READ, USER_WRITE) name and looks up attached expression/policies from the policy repo. This way we can support both inline expressions and system permissions.
    @Around("@annotation(a3Authorize)")
    public Object authorize(ProceedingJoinPoint joinPoint, A3Authorize a3Authorize) throws Throwable {

        String expression = a3Authorize.value();

        // TODO: Replace with real context extraction (JWT, request, etc.)/ Better still allow devs to pass context in the annotation
        RequestContext context = buildContext();

        boolean allowed = enforcer.enforce(context);

        if (!allowed) {
//            throw new RuntimeException("Access Denied");
            throw new AccessDeniedException("Access Denied");
        }

        return joinPoint.proceed();
    }

    private RequestContext buildContext() {

        Subject subject = extractor.extractSubject();

        return RequestContext.builder()
                .subject(subject)
                .context(Map.of()) // optional metadata
                .build();
    }
}