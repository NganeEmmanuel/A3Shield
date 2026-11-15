package com.a3shield.spring.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.a3shield.core.decision.DecisionResult;
import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.audit.A3EnforcementAuditor;
import com.a3shield.spring.context.SubjectContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class A3RouteInterceptorTest {

    private final AccessEnforcer enforcer = mock(AccessEnforcer.class);
    private final List<Object> events = new ArrayList<>();
    private final A3RouteInterceptor interceptor =
            new A3RouteInterceptor(enforcer, new A3EnforcementAuditor((ApplicationEventPublisher) events::add));

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        new SubjectContext(() -> com.a3shield.core.model.Subject.builder()
                .id("1")
                .type(com.a3shield.core.model.SubjectType.USER)
                .build());
        request.setMethod("DELETE");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/documents/{id}");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, java.util.Map.of("id", "42"));
        request.addParameter("reason", "cleanup");
    }

    @AfterEach
    void tearDown() {
        SubjectContext.clearCurrentSubject();
        com.a3shield.spring.tenant.TenantContextHolder.clear();
    }

    @Test
    void allowedRouteShouldProceed() throws Exception {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.allow("route ok"));

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void deniedRouteShouldAnswer403WithoutProceeding() throws Exception {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.deny("route denied"));

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("route denied");
    }

    @Test
    void evaluationShouldUseRoutePatternHttpMethodAndParams() throws Exception {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.allow("ok"));

        interceptor.preHandle(request, response, new Object());

        ArgumentCaptor<RequestContext> captor = ArgumentCaptor.forClass(RequestContext.class);
        org.mockito.Mockito.verify(enforcer).enforce(captor.capture());
        RequestContext ctx = captor.getValue();
        assertThat(ctx.getAction().getName()).isEqualTo("DELETE");
        assertThat(ctx.getResources().getFirst().resolve("type")).isEqualTo("/api/documents/{id}");
        // path variable {id} becomes the intent identity -> urn "route:42"-style addressing
        assertThat(ctx.getResources().getFirst().resolve("id")).isEqualTo("42");
        assertThat(ctx.getResources().getFirst().resolve("reason")).isEqualTo("cleanup");
    }

    @Test
    void decisionShouldBePublishedAsRouteAuditEvent() throws Exception {
        when(enforcer.enforce(any(RequestContext.class))).thenReturn(DecisionResult.allow("ok"));

        interceptor.preHandle(request, response, new Object());

        assertThat(events).hasSize(1);
        var event = (com.a3shield.core.audit.EnforcementAuditEvent) events.getFirst();
        assertThat(event.point()).isEqualTo(com.a3shield.core.audit.EnforcementAuditEvent.POINT_ROUTE);
        assertThat(event.action()).isEqualTo("DELETE");
    }
}
