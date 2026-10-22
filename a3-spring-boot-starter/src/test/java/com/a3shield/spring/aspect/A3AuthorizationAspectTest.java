package com.a3shield.spring.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.exception.AccessDeniedException;
import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.annotation.A3Authorize;
import com.a3shield.spring.annotation.A3Intent;
import com.a3shield.spring.context.IntentContentHolder;
import com.a3shield.spring.context.RequestContextHolder;
import com.a3shield.spring.context.SubjectContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class A3AuthorizationAspectTest {

    public static class Order {
        private final String id;

        Order(String id) {
            this.id = id;
        }
    }

    public static class Service {
        @A3Authorize("intent")
        public String submit(@A3Intent Order order) {
            return "done-" + order.id;
        }

        @A3Authorize
        public String noExpression(@A3Intent Order order) {
            return "unchecked";
        }
    }

    private final AccessEnforcer enforcer = mock(AccessEnforcer.class);

    @BeforeEach
    void setUp() {
        new SubjectContext(
                () -> Subject.builder().id("1").type(SubjectType.USER).build());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clearRequestContext();
        SubjectContext.clearCurrentSubject();
        IntentContentHolder.clearCurrentIntent();
    }

    private Service proxiedService() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Service());
        factory.addAspect(new A3AuthorizationAspect(enforcer));
        return factory.getProxy();
    }

    @Test
    void shouldProceedWhenEnforcerAllows() {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.allow("test allow"));

        String result = proxiedService().submit(new Order("o-1"));

        assertThat(result).isEqualTo("done-o-1");
    }

    @Test
    void shouldThrowTypedAccessDeniedExceptionWhenDenied() {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.deny("test deny"));

        assertThatThrownBy(() -> proxiedService().submit(new Order("o-1"))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldBuildIntentFromAnnotatedParameterNotJoinPoint() {
        // regression: preAuthorize used to receive the JoinPoint instead of the
        // @A3Intent parameter, so the evaluated "intent" was the join point object
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.allow("test allow"));
        ArgumentCaptor<RequestContext> captor = ArgumentCaptor.forClass(RequestContext.class);

        proxiedService().submit(new Order("o-42"));

        verify(enforcer).enforce(captor.capture());
        RequestContext evaluated = captor.getValue();
        assertThat(evaluated.getResources()).hasSize(1);
        assertThat(evaluated.getResources().getFirst().resolve("type")).isEqualTo("Order");
        assertThat(evaluated.getResources().getFirst().resolve("id")).isEqualTo("o-42");
    }

    @Test
    void shouldSkipEnforcementWhenExpressionIsEmpty() {
        String result = proxiedService().noExpression(new Order("o-1"));

        assertThat(result).isEqualTo("unchecked");
        verify(enforcer, never()).enforce(any(RequestContext.class));
    }
}
