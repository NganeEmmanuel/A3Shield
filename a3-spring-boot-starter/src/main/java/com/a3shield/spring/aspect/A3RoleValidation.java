package com.a3shield.spring.aspect;

import com.a3shield.spring.annotation.A3HasRoles;
import com.a3shield.spring.context.SubjectContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.expression.AccessException;

import java.util.List;
import java.util.Set;

@Aspect
public class A3RoleValidation {

    @Before("@annotation(a3HasRoles)")
    public void authorize(JoinPoint joinPoint, A3HasRoles a3HasRoles) throws Throwable {
        // validate roles
        var hasRoles = validate(SubjectContext.getCurrentSubject().getRoles(), List.of(a3HasRoles.value()));

        if(!hasRoles){
            throw new AccessException("Access Denied");
        }

    }

    private boolean validate(Set<String> subjectRoles, List<String> requiredRoles){
        return subjectRoles.containsAll(requiredRoles);
    }
}
