package com.a3shield.spring.config;

import com.a3shield.spring.audit.A3EnforcementAuditor;
import com.a3shield.spring.web.A3RouteInterceptor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Route-level enforcement registration (ADR-0004): opt-in via
 * {@code a3shield.route-enforcement.enabled=true}. Once enabled the check is
 * fail-closed — every route needs an applicable ALLOW policy (or an entry in
 * {@code a3shield.route-enforcement.exclude-paths}; {@code /error} is always
 * excluded).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "a3shield.route-enforcement.enabled", havingValue = "true")
@EnableConfigurationProperties(A3RouteEnforcementConfiguration.Properties.class)
public class A3RouteEnforcementConfiguration {

    @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "a3shield.route-enforcement")
    public record Properties(boolean enabled, List<String> excludePaths) {

        public List<String> effectiveExcludes() {
            List<String> excludes = new ArrayList<>();
            excludes.add("/error");
            if (excludePaths != null) {
                excludes.addAll(excludePaths);
            }
            return excludes;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public A3RouteInterceptor a3RouteInterceptor(
            com.a3shield.enforcer.AccessEnforcer enforcer, A3EnforcementAuditor auditor) {
        return new A3RouteInterceptor(enforcer, auditor);
    }

    @Bean
    public WebMvcConfigurer a3RouteInterceptorRegistration(
            A3RouteInterceptor interceptor, Properties properties, ObjectProvider<A3EnforcementAuditor> auditor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor).excludePathPatterns(properties.effectiveExcludes());
            }
        };
    }
}
