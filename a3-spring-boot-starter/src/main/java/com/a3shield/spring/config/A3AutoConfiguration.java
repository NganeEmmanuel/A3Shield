package com.a3shield.spring.config;

import com.a3shield.enforcer.*;
import com.a3shield.enforcer.decision.*;
import com.a3shield.enforcer.engine.*;
import com.a3shield.enforcer.evaluator.DslConditionEvaluator;
import com.a3shield.enforcer.matcher.DefaultPolicyMatcher;

import com.a3shield.enforcer.repository.InMemoryPolicyRepository;
import com.a3shield.enforcer.repository.PolicyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Auto-configures A3Shield components for Spring Boot.
 */
@Configuration
public class A3AutoConfiguration {

    @Bean
    public PolicyRepository policyRepository() {
        // For now: empty (user can override)
        return new InMemoryPolicyRepository(List.of());
    }

    @Bean
    public PolicyEngine policyEngine(PolicyRepository repo) {
        return new DefaultPolicyEngine(
                repo,
                new DefaultPolicyMatcher(),
                new DslConditionEvaluator(),
                new DefaultDecisionCombiner()
        );
    }

    @Bean
    public AccessEnforcer accessEnforcer(PolicyEngine engine) {
        return new AccessEnforcer(engine);
    }
}