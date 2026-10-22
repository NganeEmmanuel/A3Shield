package com.a3shield.spring.aspect;

import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.spring.annotation.A3SecurePolicy;
import com.a3shield.spring.context.RequestContextHolder;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.context.TargetContextHolder;
import com.a3shield.spring.context.TargetContextBuilder;
import com.a3shield.core.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class A3SecurePolicyAspectTest {

    public static class Document {
        private final String id;

        Document(String id) {
            this.id = id;
        }
    }

    public static class Service {
        @A3SecurePolicy
        public List<Document> findAll() {
            return List.of(new Document("1"), new Document("2"));
        }
    }

    private final AccessEnforcer enforcer = mock(AccessEnforcer.class);

    @BeforeEach
    void setUp() {
        new TargetContextHolder(new TargetContextBuilder());
        new SubjectContext(() -> Subject.builder().id("1").type(SubjectType.USER).build());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clearRequestContext();
        TargetContextHolder.clearTargetContext();
        SubjectContext.clearCurrentSubject();
    }

    private Service proxiedService() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Service());
        factory.addAspect(new A3SecurePolicyAspect(enforcer));
        return factory.getProxy();
    }

    @Test
    void shouldReturnOriginalResultWhenAllowed() {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.allow("test allow"));

        List<Document> result = proxiedService().findAll();

        assertThat(result).hasSize(2);
        assertThat(enforcer).isNotNull();
    }

    @Test
    void shouldEvaluateAgainstTargetsOfTheReturnValue() {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.allow("test allow"));

        proxiedService().findAll();

        RequestContext evaluated = RequestContextHolder.getRequestContext();
        assertThat(evaluated).isNotNull();
        assertThat(evaluated.getResources()).hasSize(2);
        assertThat(evaluated.getResources().getFirst().resolve("type")).isEqualTo("Document");
    }

    @Test
    void denialMustReachCallerAsTypedAccessDeniedException() {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.deny("test deny"));

        // regression: the aspect used to wrap everything in RuntimeException,
        // making the typed exception unmatchable by exception handlers
        assertThatThrownBy(() -> proxiedService().findAll())
                .isInstanceOf(AccessDeniedException.class)
                .isInstanceOfSatisfying(AccessDeniedException.class, ex -> {
                    assertThat(ex.getResult()).isNotNull();
                    assertThat(ex.getResult().isDenied()).isTrue();
                    assertThat(ex.getResult().reason()).isEqualTo("test deny");
                });
    }
}
