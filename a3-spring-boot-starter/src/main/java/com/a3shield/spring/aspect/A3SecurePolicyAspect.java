package com.a3shield.spring.aspect;

import com.a3shield.core.audit.EnforcementAuditEvent;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.exception.AccessDeniedException;
import com.a3shield.core.model.Resource;
import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.annotation.A3SecurePolicy;
import com.a3shield.spring.audit.A3EnforcementAuditor;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.context.TargetContextBuilder;
import com.a3shield.spring.tenant.TenantContextHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-execution enforcement with row-level filtering (ADR-0004, enforcement
 * point 3): intercepts methods annotated with {@link A3SecurePolicy}, runs
 * the method, converts its return value into target resources and asks the
 * enforcer whether that data may leave.
 *
 * <ul>
 *   <li><b>Collection returns:</b> each element is evaluated separately.
 *     Default mode silently filters denied rows out of the response;
 *     {@code @A3SecurePolicy(strict = true)} denies the whole response when
 *     any row is denied.</li>
 *   <li><b>Single-object returns:</b> deny-or-allow on the whole value.</li>
 * </ul>
 *
 * <p>Every row decision is published as an {@link EnforcementAuditEvent} so
 * hosts can audit filtered rows (subscribe with {@code @EventListener}).</p>
 */
@Aspect
public class A3SecurePolicyAspect {

    private static final Logger logger = LoggerFactory.getLogger(A3SecurePolicyAspect.class);

    private final AccessEnforcer enforcer;
    private final TargetContextBuilder targetBuilder;
    private final A3EnforcementAuditor auditor;

    public A3SecurePolicyAspect(
            AccessEnforcer enforcer, TargetContextBuilder targetBuilder, A3EnforcementAuditor auditor) {
        this.enforcer = enforcer;
        this.targetBuilder = targetBuilder;
        this.auditor = auditor;
    }

    @Around("@annotation(a3SecurePolicy)")
    public Object securePolicy(ProceedingJoinPoint joinPoint, A3SecurePolicy a3SecurePolicy) throws Throwable {
        Object result = joinPoint.proceed();
        String action = a3SecurePolicy.action().isEmpty() ? null : a3SecurePolicy.action();

        if (result instanceof List<?> rows) {
            return secureCollection(rows, action, a3SecurePolicy.strict());
        }

        Resource target = targetBuilder.build(result).getFirst();
        DecisionResult decision = enforce(target, action);
        if (!decision.isAllowed()) {
            throw new AccessDeniedException(
                    "Access Denied by policy for returned target " + target.urn() + ": " + decision.reason(), decision);
        }
        return result;
    }

    private Object secureCollection(List<?> rows, String action, boolean strict) {
        List<Object> allowed = new ArrayList<>(rows.size());
        DecisionResult worstDenial = null;
        int deniedCount = 0;

        for (Object row : rows) {
            Resource target = targetBuilder.build(row).getFirst();
            DecisionResult decision = enforce(target, action);
            if (decision.isAllowed()) {
                allowed.add(row);
            } else {
                deniedCount++;
                worstDenial = decision;
            }
        }

        if (deniedCount > 0 && strict) {
            throw new AccessDeniedException(
                    String.format(
                            "Access Denied: %d of %d rows failed policy (strict mode): %s",
                            deniedCount, rows.size(), worstDenial.reason()),
                    worstDenial);
        }

        if (deniedCount > 0) {
            logger.debug("Filtered {} of {} rows by policy", deniedCount, rows.size());
        }
        return allowed;
    }

    private DecisionResult enforce(Resource target, String action) {
        RequestContext context = RequestContext.builder()
                .subject(SubjectContext.getCurrentSubject())
                .resources(List.of(target))
                .action(action == null ? null : new Action(action))
                .context(Map.of("phase", "post-execution"))
                .tenantId(TenantContextHolder.getTenantId())
                .build();

        DecisionResult result = enforcer.enforce(context);
        auditor.publish(
                EnforcementAuditEvent.POINT_SECURE_POLICY,
                context.getTenantId(),
                context.getSubject() == null ? null : context.getSubject().getId(),
                action,
                List.of(target),
                result);
        return result;
    }
}
