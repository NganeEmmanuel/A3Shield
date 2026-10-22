package com.a3shield.spring.config;

import com.a3shield.enforcer.AccessEnforcer;
import com.a3shield.enforcer.decision.DefaultDecisionCombiner;
import com.a3shield.enforcer.engine.DefaultPolicyEngine;
import com.a3shield.enforcer.engine.PolicyEngine;
import com.a3shield.enforcer.evaluator.DslConditionEvaluator;
import com.a3shield.enforcer.matcher.DefaultPolicyMatcher;
import com.a3shield.enforcer.permission.PermissionExtractor;
import com.a3shield.enforcer.repository.InMemoryPolicyRepository;
import com.a3shield.enforcer.repository.PolicyRepository;
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

    @Bean
    @ConditionalOnMissingBean
    public PolicyRepository policyRepository() {
        // Empty by default: host apps provide policies (file store lands in M4)
        return new InMemoryPolicyRepository(List.of());
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyEngine policyEngine(PolicyRepository repo) {
        return new DefaultPolicyEngine(
                repo, new DefaultPolicyMatcher(), new DslConditionEvaluator(), new DefaultDecisionCombiner());
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
