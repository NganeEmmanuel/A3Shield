package com.a3shield.spring.config;

import com.a3shield.policymgmt.admin.DefaultPolicyAdminService;
import com.a3shield.policymgmt.admin.PolicyAdminApi;
import com.a3shield.policymgmt.audit.PolicyAuditStore;
import com.a3shield.policymgmt.discovery.PolicyMigrationDiscoverer;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Opt-in runtime admin API (ADR-0003): enable with
 * {@code a3shield.admin-api.enabled=true}. Exposes tenant-scoped policy CRUD
 * (validate-before-commit, audited) over REST.
 *
 * <p><b>Security warning:</b> the endpoint itself performs no
 * authentication/authorization — protect {@code /a3/admin/**} with Spring
 * Security in the host application. Enabling this API without protecting the
 * route lets anyone change policies.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "a3shield.admin-api.enabled", havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class A3AdminApiConfiguration {

    @Bean
    @ConditionalOnMissingBean(PolicyAdminApi.class)
    public PolicyAdminApi policyAdminApi(EffectivePolicyStore store, PolicyAuditStore auditStore) {
        return new DefaultPolicyAdminService(store, auditStore, () -> new PolicyMigrationDiscoverer()
                .discoverFromClasspath(A3AdminApiConfiguration.class.getClassLoader()));
    }

    @Bean
    @ConditionalOnMissingBean
    public A3PolicyAdminController a3PolicyAdminController(PolicyAdminApi policyAdminApi) {
        return new A3PolicyAdminController(policyAdminApi);
    }
}
