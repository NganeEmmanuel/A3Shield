package com.a3shield.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.policymgmt.admin.PolicyAdminApi;
import com.a3shield.policymgmt.audit.PolicyAuditStore;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/** The admin API is opt-in: beans exist only with a3shield.admin-api.enabled=true. */
class A3AdminApiConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A3AutoConfiguration.class, A3AdminApiConfiguration.class));

    @Test
    void adminBeansAbsentByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(PolicyAdminApi.class);
            assertThat(context).doesNotHaveBean(A3PolicyAdminController.class);
            // infrastructure beans exist regardless (harmless when unused)
            assertThat(context).hasSingleBean(EffectivePolicyStore.class);
            assertThat(context).hasSingleBean(PolicyAuditStore.class);
        });
    }

    @Test
    void adminBeansPresentWhenEnabled() {
        runner.withPropertyValues("a3shield.admin-api.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(PolicyAdminApi.class);
            assertThat(context).hasSingleBean(A3PolicyAdminController.class);
        });
    }
}
