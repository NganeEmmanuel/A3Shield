package com.a3shield.spring.aspect;

import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.Resource;
import com.a3shield.spring.annotation.A3SecurePolicy;
import com.a3shield.spring.context.RequestContextHolder;
import com.a3shield.spring.context.TargetContextHolder;
import com.a3shield.spring.exceptions.AccessDeniedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
public class A3SecurePolicyAspect {
    private static final Logger logger = LoggerFactory.getLogger(A3SecurePolicyAspect.class);
    private final AccessEnforcer enforcer;

    public A3SecurePolicyAspect(AccessEnforcer accessEnforcer){
        enforcer = accessEnforcer;    }

    @Around("@annotation(a3SecurePolicy)")
    public List<?> securePolicy(ProceedingJoinPoint joinPoint, A3SecurePolicy a3SecurePolicy){
        // run the method and get the return value(target)
        try {
            Object result = joinPoint.proceed();
            List<Resource> targets = TargetContextHolder.getTargetContext(result);
            RequestContext context = RequestContextHolder.getRequestContext(targets);

            boolean allowed = enforcer.enforce(context);

            if (!allowed) {
                //            throw new RuntimeException("Access Denied");
                throw new AccessDeniedException("Access Denied");
            }

            return targets;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
