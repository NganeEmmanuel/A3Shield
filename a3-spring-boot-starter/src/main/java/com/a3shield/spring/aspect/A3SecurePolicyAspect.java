package com.a3shield.spring.aspect;

import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.exception.AccessDeniedException;
import com.a3shield.core.model.Resource;
import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.annotation.A3SecurePolicy;
import com.a3shield.spring.context.RequestContextHolder;
import com.a3shield.spring.context.TargetContextHolder;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-execution enforcement: intercepts methods annotated with
 * {@link A3SecurePolicy}, runs the method, converts its return value into
 * target resources and asks the enforcer whether that data may leave
 * (ADR-0004, enforcement point 3).
 *
 * <p>M1 semantics: the original return value is passed through unchanged when
 * access is allowed; denial throws {@link AccessDeniedException} as-is.
 * Per-row filtering of collection results replaces the all-or-nothing check
 * in M6.</p>
 */
@Aspect
public class A3SecurePolicyAspect {
    private static final Logger logger = LoggerFactory.getLogger(A3SecurePolicyAspect.class);
    private final AccessEnforcer enforcer;

    public A3SecurePolicyAspect(AccessEnforcer accessEnforcer) {
        this.enforcer = accessEnforcer;
    }

    @Around("@annotation(a3SecurePolicy)")
    public Object securePolicy(ProceedingJoinPoint joinPoint, A3SecurePolicy a3SecurePolicy) throws Throwable {
        Object result = joinPoint.proceed();

        List<Resource> targets = TargetContextHolder.getTargetContext(result);
        RequestContext context = RequestContextHolder.getRequestContext(targets);

        DecisionResult decision = enforcer.enforce(context);
        if (!decision.isAllowed()) {
            throw new AccessDeniedException(
                    "Access Denied by policy for returned target(s) of type "
                            + (targets.isEmpty() ? "<none>" : targets.getFirst().resolve("type"))
                            + ": " + decision.reason(),
                    decision);
        }

        return result;
    }
}
