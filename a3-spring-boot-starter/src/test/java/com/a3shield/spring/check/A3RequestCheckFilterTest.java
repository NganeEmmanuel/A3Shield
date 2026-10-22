package com.a3shield.spring.check;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.core.tenant.Tenants;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.tenant.DefaultTenantResolver;
import com.a3shield.spring.tenant.TenantContextHolder;
import com.a3shield.spring.tenant.TenantResolver;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class A3RequestCheckFilterTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

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

    private A3RequestCheckFilter filter(RequestCheck... checks) {
        return new A3RequestCheckFilter(new RequestCheckChain(List.of(checks)), defaultTenant());
    }

    private static TenantResolver defaultTenant() {
        return req -> Tenants.DEFAULT_TENANT;
    }

    @Test
    void shouldProceedWhenAllChecksAllow() throws ServletException, IOException {
        MockFilterChain servletChain = new MockFilterChain();

        filter(req -> CheckResult.allow()).doFilter(request, response, servletChain);

        assertThat(servletChain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAnswer403WithReasonAndNotProceedOnDenial() throws ServletException, IOException {
        MockFilterChain servletChain = new MockFilterChain();

        filter(req -> CheckResult.deny("blocked")).doFilter(request, response, servletChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("blocked");
        assertThat(servletChain.getRequest()).isNull();
    }

    @Test
    void shouldClearHoldersAfterRequestEvenWhenDenied() throws ServletException, IOException {
        SubjectContext.setCurrentSubject(Subject.builder().id("9").build());

        filter(req -> CheckResult.deny("blocked")).doFilter(request, response, new MockFilterChain());

        // cleared holder re-extracts from the SubjectExtractor, so id 9 must be gone
        assertThat(SubjectContext.getCurrentSubject().getId()).isEqualTo("1");
    }

    //
    // ADR-0009: tenant resolution and scoping
    //

    @Test
    void shouldResolveTenantOncePerRequestAndClearItAfter() throws ServletException, IOException {
        AtomicReference<String> tenantSeenByCheck = new AtomicReference<>();

        A3RequestCheckFilter tenantFilter = new A3RequestCheckFilter(
                new RequestCheckChain(List.of(req -> {
                    tenantSeenByCheck.set(TenantContextHolder.getTenantId());
                    return CheckResult.allow();
                })),
                req -> "acme");

        tenantFilter.doFilter(request, response, new MockFilterChain());

        assertThat(tenantSeenByCheck.get()).isEqualTo("acme");
        // after the request, the holder falls back to the default tenant
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(Tenants.DEFAULT_TENANT);
    }

    @Test
    void defaultResolverShouldUseDefaultTenant() throws ServletException, IOException {
        AtomicReference<String> tenantSeenByCheck = new AtomicReference<>();

        A3RequestCheckFilter defaultFilter = new A3RequestCheckFilter(
                new RequestCheckChain(List.of(req -> {
                    tenantSeenByCheck.set(TenantContextHolder.getTenantId());
                    return CheckResult.allow();
                })),
                new DefaultTenantResolver());

        defaultFilter.doFilter(request, response, new MockFilterChain());

        assertThat(tenantSeenByCheck.get()).isEqualTo(Tenants.DEFAULT_TENANT);
    }
}
