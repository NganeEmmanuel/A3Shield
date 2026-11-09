package com.a3shield.spring.web;

import com.a3shield.core.audit.EnforcementAuditEvent;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.model.Intent;
import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.audit.A3EnforcementAuditor;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Route-level enforcement (ADR-0004, enforcement point 1): checks the
 * request's route against policy before the handler runs — whole-endpoint
 * rules without touching controller code.
 *
 * <p>The intent resource type is the matched route pattern (e.g.
 * {@code /api/documents/{id}} — attribute {@code HandlerMapping
 * .BEST_MATCHING_PATTERN_ATTRIBUTE}), the action is the HTTP method, and
 * query parameters become intent attributes. Example policy:
 * {@code resource: "/api/admin/*", action: "DELETE", effect: DENY}.</p>
 *
 * <p><b>Opt-in and fail-closed:</b> enabled with
 * {@code a3shield.route-enforcement.enabled=true} — once enabled, a route
 * with no applicable ALLOW policy is denied. Use
 * {@code a3shield.route-enforcement.exclude-paths} for endpoints that must
 * stay outside policy control ({@code /error} is always excluded).</p>
 */
public class A3RouteInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(A3RouteInterceptor.class);

    private final AccessEnforcer enforcer;
    private final A3EnforcementAuditor auditor;

    public A3RouteInterceptor(AccessEnforcer enforcer, A3EnforcementAuditor auditor) {
        this.enforcer = enforcer;
        this.auditor = auditor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String route = resolveRoute(request);
        String method = request.getMethod() == null ? "GET" : request.getMethod();

        Map<String, Object> attributes = new HashMap<>();
        request.getParameterMap()
                .forEach((name, values) -> attributes.put(name, values.length == 1 ? values[0] : List.of(values)));
        // path variables (e.g. {id}) take precedence over same-named query params
        if (request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> pathVars) {
            pathVars.forEach((name, value) -> attributes.put(String.valueOf(name), value));
        }
        Object id = attributes.get("id");

        Intent intent = Intent.builder()
                .id(id == null ? null : String.valueOf(id))
                .type(route)
                .attributes(attributes)
                .build();
        RequestContext context = RequestContext.builder()
                .subject(SubjectContext.getCurrentSubject())
                .resources(List.of(intent))
                .action(new Action(method))
                .tenantId(TenantContextHolder.getTenantId())
                .build();

        DecisionResult result = enforcer.enforce(context);
        auditor.publish(
                EnforcementAuditEvent.POINT_ROUTE,
                context.getTenantId(),
                context.getSubject() == null ? null : context.getSubject().getId(),
                method,
                List.of(intent),
                result);

        if (!result.isAllowed()) {
            logger.debug("Route check denied {} {} ({})", method, route, result.reason());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getOutputStream().write(result.reason().getBytes(StandardCharsets.UTF_8));
            return false;
        }
        return true;
    }

    private String resolveRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern.toString() : request.getRequestURI();
    }
}
