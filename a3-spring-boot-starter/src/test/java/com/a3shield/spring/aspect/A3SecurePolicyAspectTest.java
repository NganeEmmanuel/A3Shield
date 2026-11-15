package com.a3shield.spring.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.a3shield.core.audit.EnforcementAuditEvent;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.exception.AccessDeniedException;
import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.annotation.A3SecurePolicy;
import com.a3shield.spring.audit.A3EnforcementAuditor;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.context.TargetContextBuilder;
import com.a3shield.spring.tenant.TenantContextHolder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.ApplicationEventPublisher;

class A3SecurePolicyAspectTest {

    public static class Document {
        private final String id;

        Document(String id) {
            this.id = id;
        }
    }

    public static class Service {
        @A3SecurePolicy(action = "read")
        public List<Document> findAll() {
            return List.of(new Document("1"), new Document("2"), new Document("3"));
        }

        @A3SecurePolicy(action = "read", strict = true)
        public List<Document> findAllStrict() {
            return List.of(new Document("1"), new Document("2"));
        }

        @A3SecurePolicy(action = "read")
        public Document findOne() {
            return new Document("1");
        }
    }

    private final AccessEnforcer enforcer = mock(AccessEnforcer.class);
    private final List<Object> publishedEvents = new ArrayList<>();
    private final A3EnforcementAuditor auditor =
            new A3EnforcementAuditor((ApplicationEventPublisher) publishedEvents::add);

    @BeforeEach
    void setUp() {
        new SubjectContext(
                () -> Subject.builder().id("1").type(SubjectType.USER).build());
    }

    @AfterEach
    void tearDown() {
        SubjectContext.clearCurrentSubject();
        TenantContextHolder.clear();
    }

    /** Allow rows whose document id is "1" or "3"; deny "2". */
    private void allowOddIds() {
        when(enforcer.enforce(any(RequestContext.class))).thenAnswer(invocation -> {
            RequestContext ctx = invocation.getArgument(0);
            String rowId = (String) ctx.getResources().getFirst().resolve("id");
            return "2".equals(rowId)
                    ? DecisionResult.deny("row denied: " + rowId)
                    : DecisionResult.allow("row allowed: " + rowId);
        });
    }

    private Service proxied(Service service) {
        AspectJProxyFactory factory = new AspectJProxyFactory(service);
        factory.addAspect(new A3SecurePolicyAspect(enforcer, new TargetContextBuilder(), auditor));
        return factory.getProxy();
    }

    @Test
    void shouldFilterDeniedRowsByDefault() {
        allowOddIds();

        List<Document> result = proxied(new Service()).findAll();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(doc -> !"2".equals(doc.id));
    }

    @Test
    void strictModeShouldDenyWhenAnyRowFails() {
        allowOddIds();

        assertThatThrownBy(() -> proxied(new Service()).findAllStrict())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("1 of 2 rows failed policy")
                .isInstanceOfSatisfying(AccessDeniedException.class, ex -> assertThat(ex.getResult())
                        .isNotNull());
    }

    @Test
    void singleObjectDenialShouldThrowTypedException() {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.deny("test deny"));

        // regression: the aspect used to wrap everything in RuntimeException,
        // making the typed exception unmatchable by exception handlers
        assertThatThrownBy(() -> proxied(new Service()).findOne())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("test deny")
                .isInstanceOfSatisfying(AccessDeniedException.class, ex -> {
                    assertThat(ex.getResult()).isNotNull();
                    assertThat(ex.getResult().isDenied()).isTrue();
                });
    }

    @Test
    void actionAttributeShouldFlowIntoEvaluation() {
        allowOddIds();

        proxied(new Service()).findAll();

        ArgumentCaptor<RequestContext> captor = ArgumentCaptor.forClass(RequestContext.class);
        verify(enforcer, atLeastOnce()).enforce(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(ctx -> assertThat(ctx.getAction().getName()).isEqualTo("read"));
    }

    @Test
    void rowDecisionsShouldBePublishedAsAuditEvents() {
        allowOddIds();

        proxied(new Service()).findAll();

        assertThat(publishedEvents).hasSize(3);
        assertThat(publishedEvents).allSatisfy(event -> assertThat(event)
                .isInstanceOfSatisfying(EnforcementAuditEvent.class, audit -> assertThat(audit.point())
                        .isEqualTo(EnforcementAuditEvent.POINT_SECURE_POLICY)));
        assertThat(publishedEvents.stream()
                        .map(event -> ((EnforcementAuditEvent) event).result().isAllowed()))
                .containsExactly(true, false, true);
    }
}
