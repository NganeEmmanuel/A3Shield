package com.a3shield.spring.aspect;

import com.a3shield.spring.annotation.A3HasPermissions;
import com.a3shield.spring.context.SubjectContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;

@Aspect
public class A3PermissionValidation {

    @Before("@annotation(a3HasPermissions)")
    public void preAuthorized(JoinPoint joinPoint, A3HasPermissions a3HasPermissions) {
        List<String> requiredPermissions = List.of(a3HasPermissions.preAuthorize());
        if(!requiredPermissions.isEmpty()) {
            var hasPermission = hasPermission(SubjectContext.getCurrentSubject().getPermissions(), requiredPermissions);
            if (!hasPermission) {
                throw new AccessDeniedException("You are not authorized to perform this action.");
            }
        }
    }

    @After("@annotation(a3HasPermissions)")
    public void postAuthorize(JoinPoint joinPoint, A3HasPermissions a3HasPermissions) {
        List<String> requiredPermissions = List.of(a3HasPermissions.postAuthorize());
        if(!requiredPermissions.isEmpty()) {
            var hasPermission = hasPermission(SubjectContext.getCurrentSubject().getPermissions(), requiredPermissions);
            if (!hasPermission) {
                throw new AccessDeniedException("You are not authorized to perform this action.");
            }
        }
    }

    private boolean hasPermission(Set<String> subjectPermissions, List<String> requiredPermissions) {
        return subjectPermissions.containsAll(requiredPermissions);
    }
}
