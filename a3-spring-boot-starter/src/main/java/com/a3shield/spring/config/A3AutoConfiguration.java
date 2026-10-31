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
import com.a3shield.policymgmt.audit.InMemoryPolicyAuditStore;
import com.a3shield.policymgmt.audit.PolicyAuditStore;
import com.a3shield.policymgmt.discovery.PolicyMigrationDiscoverer;
import com.a3shield.policymgmt.runtime.AppliedMigrationsStore;
import com.a3shield.policymgmt.runtime.InMemoryAppliedMigrationsStore;
import com.a3shield.policymgmt.runtime.JdbcAppliedMigrationsStore;
import com.a3shield.policymgmt.runtime.PolicyStoreSynchronizer;
import com.a3shield.policymgmt.runtime.StoreBackedPolicyRepository;
import com.a3shield.policymgmt.store.EffectivePolicyStore;
import com.a3shield.policymgmt.store.InMemoryEffectivePolicyStore;
import com.a3shield.policymgmt.store.JdbcEffectivePolicyStore;
import com.a3shield.spring.aspect.A3AuthorizationAspect;
import com.a3shield.spring.aspect.A3SecurePolicyAspect;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.context.TargetContextBuilder;
import com.a3shield.spring.context.TargetContextHolder;
import com.a3shield.spring.security.SpringSecuritySubjectContextExtractor;
import com.a3shield.spring.security.SubjectExtractor;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Core auto-configuration for A3Shield: engine beans, subject extraction,
 * context holders, enforcement aspects and the policy-migration lifecycle.
 * Web-specific wiring lives in {@link A3WebConfiguration}; the opt-in admin
 * API in {@link A3AdminApiConfiguration}. Every bean backs off if the host
 * application defines its own.
 */
@AutoConfiguration
public class A3AutoConfiguration {

    /**
     * Durable effective-policy store (ADR-0003): JDBC-backed when a
     * {@link javax.sql.DataSource} exists (survives restarts, shared across
     * instances), in-memory otherwise.
     */
    @Bean
    @ConditionalOnMissingBean
    public EffectivePolicyStore effectivePolicyStore(ObjectProvider<javax.sql.DataSource> dataSource) {
        javax.sql.DataSource ds = dataSource.getIfAvailable();
        return ds != null ? new JdbcEffectivePolicyStore(ds) : new InMemoryEffectivePolicyStore();
    }

    /** Append-only audit trail of policy mutations; JDBC when a DataSource exists. */
    @Bean
    @ConditionalOnMissingBean
    public PolicyAuditStore policyAuditStore(ObjectProvider<javax.sql.DataSource> dataSource) {
        javax.sql.DataSource ds = dataSource.getIfAvailable();
        return ds != null ? new com.a3shield.policymgmt.audit.JdbcPolicyAuditStore(ds) : new InMemoryPolicyAuditStore();
    }

    /**
     * Policy repository: migration files under {@code a3/policies/} are
     * synchronized into the effective store at startup (tests first,
     * fail-closed, admin edits protected per ADR-0003) and evaluation reads
     * the store with poll-based invalidation (ADR-0011). With no files and
     * the admin API disabled the repository is empty; hosts can always
     * define their own {@link PolicyRepository} bean to bypass all of this.
     */
    @Bean
    @ConditionalOnMissingBean
    public PolicyRepository policyRepository(
            EffectivePolicyStore effectivePolicyStore,
            PolicyAuditStore policyAuditStore,
            ObjectProvider<AppliedMigrationsStore> appliedMigrationsStore,
            ObjectProvider<javax.sql.DataSource> dataSource,
            ObjectProvider<ConditionEvaluator> conditionEvaluator,
            @Value("${a3shield.admin-api.enabled:false}") boolean adminApiEnabled) {

        var discoverer = new PolicyMigrationDiscoverer();
        var migrations = discoverer.discoverFromClasspath(A3AutoConfiguration.class.getClassLoader());

        if (migrations.isEmpty() && !adminApiEnabled) {
            return new InMemoryPolicyRepository(List.of());
        }

        AppliedMigrationsStore applied = appliedMigrationsStore.getIfAvailable(() -> {
            javax.sql.DataSource ds = dataSource.getIfAvailable();
            return ds != null ? new JdbcAppliedMigrationsStore(ds) : new InMemoryAppliedMigrationsStore();
        });

        new PolicyStoreSynchronizer(effectivePolicyStore, applied, policyAuditStore).sync(migrations);

        return new StoreBackedPolicyRepository(effectivePolicyStore);
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
