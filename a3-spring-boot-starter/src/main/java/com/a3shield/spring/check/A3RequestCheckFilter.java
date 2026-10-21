package com.a3shield.spring.check;

import com.a3shield.spring.context.IntentContentHolder;
import com.a3shield.spring.context.RequestContextHolder;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.context.TargetContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet filter with two jobs (ADR-0001, ADR-0006):
 *
 * <ol>
 *   <li>Run the pluggable {@link RequestCheck} chain before anything else;
 *       the first denial answers 403 with its reason.</li>
 *   <li>Own the ThreadLocal lifecycle: every A3 holder is cleared in a
 *       {@code finally} so state never bleeds across pooled threads — even
 *       when an exception escapes downstream.</li>
 * </ol>
 */
public class A3RequestCheckFilter extends OncePerRequestFilter {

    private final RequestCheckChain chain;

    public A3RequestCheckFilter(RequestCheckChain chain) {
        this.chain = chain;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            var denial = chain.firstDenial(request);
            if (denial.isPresent()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getOutputStream().write(denial.get().reason().getBytes(StandardCharsets.UTF_8));
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            IntentContentHolder.clearCurrentIntent();
            TargetContextHolder.clearTargetContext();
            RequestContextHolder.clearRequestContext();
            SubjectContext.clearCurrentSubject();
        }
    }
}
