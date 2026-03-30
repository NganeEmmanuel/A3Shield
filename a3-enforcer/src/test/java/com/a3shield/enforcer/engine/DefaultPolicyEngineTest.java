package com.a3shield.enforcer.engine;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.*;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.evaluator.SimpleConditionEvaluator;
import com.a3shield.enforcer.matcher.DefaultPolicyMatcher;
import com.a3shield.enforcer.model.*;
import com.a3shield.enforcer.policy.*;
import com.a3shield.enforcer.repository.InMemoryPolicyRepository;
import com.a3shield.enforcer.repository.PolicyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style test for PolicyEngine.
 * <p>
 * Tests full evaluation flow:
 * repository → matcher → evaluator → combiner
 */
class DefaultPolicyEngineTest {

    @Test
    void shouldAllowWhenPolicyMatches() {

        Policy policy = Policy.builder()
                .resource("document")
                .action("read")
                .subject(Map.of("type", "USER"))
                .effect(Effect.ALLOW)
                .build();

        PolicyRepository repo = new InMemoryPolicyRepository(List.of(policy));

        PolicyEngine engine = new DefaultPolicyEngine(
                repo,
                new DefaultPolicyMatcher(),
                new SimpleConditionEvaluator(),
                new DefaultDecisionCombiner()
        );

        RequestContext context = RequestContext.builder()
                .subject(Subject.builder()
                        .id("1")
                        .type(SubjectType.USER)
                        .build())
                .resource(Resource.builder()
                        .type("document")
                        .build())
                .action(new Action("read"))
                .build();

        Decision decision = engine.evaluate(context);

        assertThat(decision).isEqualTo(Decision.ALLOW);
    }

    @Test
    void shouldDenyWhenNoPoliciesMatch() {

        PolicyRepository repo = new InMemoryPolicyRepository(List.of());

        PolicyEngine engine = new DefaultPolicyEngine(
                repo,
                new DefaultPolicyMatcher(),
                new SimpleConditionEvaluator(),
                new DefaultDecisionCombiner()
        );

        RequestContext context = RequestContext.builder()
                .resource(Resource.builder().type("document").build())
                .action(new Action("read"))
                .build();

        Decision decision = engine.evaluate(context);

        assertThat(decision).isEqualTo(Decision.DENY);
    }

    @Test
    void shouldDenyWhenPolicyEffectIsDeny() {

        Policy policy = Policy.builder()
                .resource("document")
                .action("read")
                .effect(Effect.DENY)
                .build();

        PolicyRepository repo = new InMemoryPolicyRepository(List.of(policy));

        PolicyEngine engine = new DefaultPolicyEngine(
                repo,
                new DefaultPolicyMatcher(),
                new SimpleConditionEvaluator(),
                new DefaultDecisionCombiner()
        );

        RequestContext context = RequestContext.builder()
                .resource(Resource.builder().type("document").build())
                .action(new Action("read"))
                .build();

        Decision decision = engine.evaluate(context);

        assertThat(decision).isEqualTo(Decision.DENY);
    }
}