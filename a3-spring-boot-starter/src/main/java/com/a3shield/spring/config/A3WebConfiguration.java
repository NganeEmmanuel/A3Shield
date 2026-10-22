package com.a3shield.spring.config;

import com.a3shield.spring.check.A3RequestCheckFilter;
import com.a3shield.spring.check.RequestCheck;
import com.a3shield.spring.check.RequestCheckChain;
import com.a3shield.spring.tenant.DefaultTenantResolver;
import com.a3shield.spring.tenant.TenantResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Servlet-web wiring: registers the {@link A3RequestCheckFilter}, which
 * resolves the tenant (ADR-0009), runs the host application's
 * {@link RequestCheck}s and owns the ThreadLocal context lifecycle (clears
 * all A3 holders after every request).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class A3WebConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantResolver tenantResolver() {
        return new DefaultTenantResolver();
    }

    /**
     * Early in the chain (ahead of Spring Security's default -100) so
     * perimeter checks run first and holder cleanup always wraps the
     * downstream chain. Host apps can override the registration entirely.
     */
    @Bean
    @ConditionalOnMissingBean(name = "a3RequestCheckFilter")
    public FilterRegistrationBean<A3RequestCheckFilter> a3RequestCheckFilter(ObjectProvider<RequestCheck> requestChecks,
                                                                             TenantResolver tenantResolver) {
        A3RequestCheckFilter filter = new A3RequestCheckFilter(
                new RequestCheckChain(requestChecks.orderedStream().toList()),
                tenantResolver
        );
        FilterRegistrationBean<A3RequestCheckFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return registration;
    }
}
