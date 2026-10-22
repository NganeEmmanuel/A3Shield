package com.a3shield.spring.check;

import com.a3shield.spring.context.IntentContentHolder;
import com.a3shield.spring.context.RequestContextHolder;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.context.TargetContextHolder;
import com.a3shield.spring.tenant.TenantContextHolder;
import com.a3shield.spring.tenant.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter with three jobs (ADR-0001, ADR-0006, ADR-0009):
 *
 * <ol>
 *   <li>Resolve the request's tenant (via {@link TenantResolver}) so every
 *       downstream evaluation is tenant-scoped.</li>
 *   <li>Run the pluggable {@link RequestCheck} chain; the first denial
 *       answers 403 with its reason.</li>
 *   <li>Own the ThreadLocal lifecycle: every A3 holder (tenant, subject,
 *       request, intent, target) is cleared in a {@code finally} so state
 *       never bleeds across pooled threads.</li>
 * </ol>
 */
public class A3RequestCheckFilter extends OncePerRequestFilter {

    private final RequestCheckChain chain;
    private final TenantResolver tenantResolver;

    public A3RequestCheckFilter(RequestCheckChain chain, TenantResolver tenantResolver) {
        this.chain = chain;
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TenantContextHolder.setTenantId(tenantResolver.resolveTenant(request));

            var denial = chain.firstDenial(request);
            if (denial.isPresent()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getOutputStream().write(denial.get().reason().getBytes(StandardCharsets.UTF_8));
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            IntentContentHolder.clearCurrentIntent();
            TargetContextHolder.clearTargetContext();
            RequestContextHolder.clearRequestContext();
            SubjectContext.clearCurrentSubject();
        }
    }
}
