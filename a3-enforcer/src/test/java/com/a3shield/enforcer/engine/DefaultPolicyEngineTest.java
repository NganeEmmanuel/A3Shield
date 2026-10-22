package com.a3shield.enforcer.engine;

import com.a3shield.core.decision.Decision;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.model.Intent;
import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.DefaultDecisionCombiner;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.evaluator.SimpleConditionEvaluator;
import com.a3shield.enforcer.matcher.DefaultPolicyMatcher;
import com.a3shield.enforcer.policy.Policy;
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

    private static PolicyEngine engineFor(Policy... policies) {
        PolicyRepository repo = new InMemoryPolicyRepository(List.of(policies));
        return new DefaultPolicyEngine(
                repo,
                new DefaultPolicyMatcher(),
                new SimpleConditionEvaluator(),
                new DefaultDecisionCombiner()
        );
    }

    @Test
    void shouldAllowWhenPolicyMatches() {

        PolicyEngine engine = engineFor(Policy.builder()
                .id("p-allow")
                .resource("document")
                .action("read")
                .subject(Map.of("type", "USER"))
                .effect(Effect.ALLOW)
                .build());

        RequestContext context = RequestContext.builder()
                .subject(Subject.builder()
                        .id("1")
                        .type(SubjectType.USER)
                        .build())
                .resources(List.of(Intent.builder()
                        .type("document")
                        .build()))
                .action(new Action("read"))
                .build();

        DecisionResult result = engine.evaluate(context);

        assertThat(result.decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.matchedPolicyIds()).containsExactly("p-allow");
        assertThat(result.reason()).contains("p-allow");
    }

    @Test
    void shouldDenyWhenNoPoliciesMatch() {

        PolicyEngine engine = engineFor();

        RequestContext context = RequestContext.builder()
                .resources(List.of(Intent.builder().type("document").build()))
                .action(new Action("read"))
                .build();

        DecisionResult result = engine.evaluate(context);

        assertThat(result.decision()).isEqualTo(Decision.DENY);
        assertThat(result.matchedPolicyIds()).isEmpty();
        assertThat(result.reason()).contains("no applicable policy");
    }

    @Test
    void shouldDenyWhenPolicyEffectIsDeny() {

        PolicyEngine engine = engineFor(Policy.builder()
                .id("p-deny")
                .resource("document")
                .action("read")
                .effect(Effect.DENY)
                .build());

        RequestContext context = RequestContext.builder()
                .resources(List.of(Intent.builder().type("document").build()))
                .action(new Action("read"))
                .build();

        DecisionResult result = engine.evaluate(context);

        assertThat(result.decision()).isEqualTo(Decision.DENY);
        assertThat(result.matchedPolicyIds()).containsExactly("p-deny");
    }

    //
    // ADR-0009: tenant precedence — most specific applicable scope wins
    //

    @Test
    void tenantPolicyShouldOverrideGlobalPolicy() {

        PolicyEngine engine = engineFor(
                Policy.builder()
                        .id("global-deny")
                        .tenantId(Tenants.GLOBAL)
                        .resource("document")
                        .action("read")
                        .effect(Effect.DENY)
                        .build(),
                Policy.builder()
                        .id("acme-allow")
                        .tenantId("acme")
                        .resource("document")
                        .action("read")
                        .effect(Effect.ALLOW)
                        .build());

        RequestContext context = RequestContext.builder()
                .tenantId("acme")
                .resources(List.of(Intent.builder().type("document").build()))
                .action(new Action("read"))
                .build();

        DecisionResult result = engine.evaluate(context);

        assertThat(result.decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.matchedPolicyIds()).containsExactly("acme-allow");
    }

    @Test
    void globalPolicyShouldApplyWhenTenantHasNoApplicablePolicy() {

        PolicyEngine engine = engineFor(
                Policy.builder()
                        .id("global-allow")
                        .tenantId(Tenants.GLOBAL)
                        .resource("document")
                        .action("read")
                        .effect(Effect.ALLOW)
                        .build(),
                // tenant policy exists but does not match this subject
                Policy.builder()
                        .id("acme-service-only")
                        .tenantId("acme")
                        .resource("document")
                        .action("read")
                        .subject(Map.of("type", "SERVICE"))
                        .effect(Effect.DENY)
                        .build());

        RequestContext context = RequestContext.builder()
                .tenantId("acme")
                .subject(Subject.builder().id("1").type(SubjectType.USER).build())
                .resources(List.of(Intent.builder().type("document").build()))
                .action(new Action("read"))
                .build();

        DecisionResult result = engine.evaluate(context);

        assertThat(result.decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.matchedPolicyIds()).containsExactly("global-allow");
    }

    @Test
    void otherTenantsPolicyShouldNeverApply() {

        PolicyEngine engine = engineFor(Policy.builder()
                .id("acme-allow")
                .tenantId("acme")
                .resource("document")
                .action("read")
                .effect(Effect.ALLOW)
                .build());

        RequestContext context = RequestContext.builder()
                .tenantId("globex")
                .resources(List.of(Intent.builder().type("document").build()))
                .action(new Action("read"))
                .build();

        DecisionResult result = engine.evaluate(context);

        assertThat(result.decision()).isEqualTo(Decision.DENY);
    }

    @Test
    void missingTenantShouldDefaultToDefaultTenant() {

        PolicyEngine engine = engineFor(Policy.builder()
                .id("default-allow")
                .tenantId(Tenants.DEFAULT_TENANT)
                .resource("document")
                .action("read")
                .effect(Effect.ALLOW)
                .build());

        RequestContext context = RequestContext.builder()
                .resources(List.of(Intent.builder().type("document").build()))
                .action(new Action("read"))
                .build();

        assertThat(context.getTenantId()).isEqualTo(Tenants.DEFAULT_TENANT);

        DecisionResult result = engine.evaluate(context);

        assertThat(result.decision()).isEqualTo(Decision.ALLOW);
    }
}
