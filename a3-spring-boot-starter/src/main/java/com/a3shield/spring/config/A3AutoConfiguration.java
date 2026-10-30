package com.a3shield.spring.config;

import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.decision.DefaultDecisionCombiner;
import com.a3shield.enforcer.engine.DefaultPolicyEngine;
import com.a3shield.enforcer.engine.PolicyEngine;
import com.a3shield.enforcer.evaluator.CelConditionEvaluator;
import com.a3shield.enforcer.evaluator.ConditionEvaluator;
import com.a3shield.enforcer.matcher.DefaultPolicyMatcher;
import com.a3shield.enforcer.permission.PermissionExtractor;
import com.a3shield.enforcer.repository.InMemoryPolicyRepository;
import com.a3shield.enforcer.repository.PolicyRepository;
import com.a3shield.policymgmt.discovery.PolicyMigrationDiscoverer;
import com.a3shield.policymgmt.runtime.AppliedMigrationsStore;
import com.a3shield.policymgmt.runtime.InMemoryAppliedMigrationsStore;
import com.a3shield.policymgmt.runtime.JdbcAppliedMigrationsStore;
import com.a3shield.policymgmt.runtime.PolicyMigrationRunner;
import com.a3shield.spring.aspect.A3AuthorizationAspect;
import com.a3shield.spring.aspect.A3SecurePolicyAspect;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.context.TargetContextBuilder;
import com.a3shield.spring.context.TargetContextHolder;
import com.a3shield.spring.security.SpringSecuritySubjectContextExtractor;
import com.a3shield.spring.security.SubjectExtractor;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Core auto-configuration for A3Shield: engine beans, subject extraction,
 * context holders and enforcement aspects. Web-specific wiring lives in
 * {@link A3WebConfiguration}. Every bean backs off if the host application
 * defines its own.
 */
@AutoConfiguration
public class A3AutoConfiguration {

    /**
     * Policy repository: when migration files exist under
     * {@code a3/policies/} on the classpath they are applied at startup
     * (tests first, fail-closed — ADR-0003) and become the effective policy
     * set. Without files the repository is empty; hosts can also define
     * their own {@link PolicyRepository} bean to bypass migrations entirely.
     *
     * <p>Applied-versions tracking uses a JDBC store when a
     * {@link javax.sql.DataSource} bean is present (conflict detection
     * across restarts), otherwise in-memory (single-run).</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public PolicyRepository policyRepository(
            ObjectProvider<AppliedMigrationsStore> appliedMigrationsStore,
            ObjectProvider<javax.sql.DataSource> dataSource,
            ObjectProvider<ConditionEvaluator> conditionEvaluator) {

        var discoverer = new PolicyMigrationDiscoverer();
        var migrations = discoverer.discoverFromClasspath(A3AutoConfiguration.class.getClassLoader());

        if (migrations.isEmpty()) {
            return new InMemoryPolicyRepository(List.of());
        }

        AppliedMigrationsStore store = appliedMigrationsStore.getIfAvailable(() -> {
            javax.sql.DataSource ds = dataSource.getIfAvailable();
            return ds != null ? new JdbcAppliedMigrationsStore(ds) : new InMemoryAppliedMigrationsStore();
        });

        var runner = new PolicyMigrationRunner(store, conditionEvaluator.getIfAvailable(CelConditionEvaluator::new));
        return runner.apply(migrations);
    }

    /**
     * CEL is the canonical condition engine (ADR-0002); the legacy DSL
     * evaluator remains available for hosts that prefer it — define a
     * {@link ConditionEvaluator} bean to override.
     */
    @Bean
    @ConditionalOnMissingBean(ConditionEvaluator.class)
    public ConditionEvaluator conditionEvaluator() {
        return new CelConditionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyEngine policyEngine(PolicyRepository repo, ConditionEvaluator conditionEvaluator) {
        return new DefaultPolicyEngine(
                repo, new DefaultPolicyMatcher(), conditionEvaluator, new DefaultDecisionCombiner());
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessEnforcer accessEnforcer(PolicyEngine engine) {
        return new AccessEnforcer(engine);
    }

    @Bean
    @ConditionalOnMissingBean(SubjectExtractor.class)
    public SubjectExtractor subjectExtractor(ObjectProvider<PermissionExtractor> permissionExtractors) {
        return new SpringSecuritySubjectContextExtractor(
                permissionExtractors.orderedStream().toList());
    }

    @Bean
    public SubjectContext subjectContext(SubjectExtractor subjectExtractor) {
        return new SubjectContext(subjectExtractor);
    }

    @Bean
    @ConditionalOnMissingBean
    public TargetContextBuilder targetContextBuilder() {
        return new TargetContextBuilder();
    }

    @Bean
    public TargetContextHolder targetContextHolder(TargetContextBuilder targetContextBuilder) {
        return new TargetContextHolder(targetContextBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public A3AuthorizationAspect a3AuthorizationAspect(AccessEnforcer accessEnforcer) {
        return new A3AuthorizationAspect(accessEnforcer);
    }

    @Bean
    @ConditionalOnMissingBean
    public A3SecurePolicyAspect a3SecurePolicyAspect(AccessEnforcer accessEnforcer) {
        return new A3SecurePolicyAspect(accessEnforcer);
    }
}
