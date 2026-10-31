package com.a3shield.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.enforcer.repository.InMemoryPolicyRepository;
import com.a3shield.enforcer.repository.PolicyRepository;
import com.a3shield.policymgmt.runtime.StoreBackedPolicyRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifies the auto-configured {@link PolicyRepository}: classpath policy
 * migrations are applied at startup (tests executed, fail-closed) and host
 * applications can still override the repository bean entirely.
 */
class A3AutoConfigurationMigrationsTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(A3AutoConfiguration.class));

    @Test
    void classpathMigrationsShouldBecomeThePolicyRepository() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(PolicyRepository.class);
            assertThat(context.getBean(PolicyRepository.class)).isInstanceOf(StoreBackedPolicyRepository.class);

            // classpath migration content landed in the effective store
            var policies = context.getBean(PolicyRepository.class).findAll("default", new Action("read"), "document");
            assertThat(policies).extracting(Policy::getId).containsExactly("starter-doc-read");
        });
    }

    @Test
    void hostDefinedRepositoryShouldBackOffMigrations() {
        runner.withBean("customPolicyRepository", PolicyRepository.class, () -> new InMemoryPolicyRepository(List.of()))
                .run(context -> assertThat(context.getBean(PolicyRepository.class))
                        .isInstanceOf(InMemoryPolicyRepository.class));
    }
}
