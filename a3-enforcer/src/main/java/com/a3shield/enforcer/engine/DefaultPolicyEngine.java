package com.a3shield.enforcer.engine;

import com.a3shield.core.decision.Decision;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.DecisionCombiner;
import com.a3shield.enforcer.evaluator.ConditionEvaluator;
import com.a3shield.enforcer.matcher.PolicyMatcher;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.enforcer.repository.PolicyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p><b>DefaultPolicyEngine</b> is the standard implementation of
 * {@link PolicyEngine} that orchestrates the evaluation of policies
 * against a given {@link RequestContext}.</p>
 *
 * <p>This engine coordinates the following components:</p>
 * <ul>
 *   <li>{@link PolicyRepository} – retrieves candidate policies for the tenant/action/resource.</li>
 *   <li>{@link PolicyMatcher} – filters policies applicable to the request context.</li>
 *   <li>{@link ConditionEvaluator} – evaluates conditional expressions within policies.</li>
 *   <li>{@link DecisionCombiner} – merges individual policy decisions into a final outcome.</li>
 * </ul>
 *
 * <p>Tenant precedence (ADR-0009): when both tenant-scoped and globally-scoped
 * policies apply, only the tenant-scoped set is evaluated — the most specific
 * scope wins. Within one scope, DENY overrides ALLOW and the default is DENY.</p>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and safe
 * for concurrent use, provided the injected components are themselves
 * thread-safe.</p>
 */
public class DefaultPolicyEngine implements PolicyEngine {

    private final PolicyRepository policyRepository;
    private final PolicyMatcher policyMatcher;
    private final ConditionEvaluator conditionEvaluator;
    private final DecisionCombiner decisionCombiner;

    public DefaultPolicyEngine(
            PolicyRepository policyRepository,
            PolicyMatcher policyMatcher,
            ConditionEvaluator conditionEvaluator,
            DecisionCombiner decisionCombiner) {
        this.policyRepository = policyRepository;
        this.policyMatcher = policyMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.decisionCombiner = decisionCombiner;
    }

    @Override
    public DecisionResult evaluate(RequestContext context) {

        List<Policy> candidates =
                policyRepository.findAll(context.getTenantId(), context.getAction(), context.getResourceType());

        List<Policy> matchedPolicies = policyMatcher.match(context, candidates);
        List<Policy> applicablePolicies = chooseByScopePrecedence(matchedPolicies, context.getTenantId());

        List<Decision> decisions = new ArrayList<>();
        List<String> firedPolicyIds = new ArrayList<>();

        for (Policy policy : applicablePolicies) {
            boolean conditionResult = conditionEvaluator.evaluate(policy.getCondition(), context);

            if (conditionResult) {
                decisions.add(mapEffect(policy));
                firedPolicyIds.add(policy.getId());
            } else {
                decisions.add(Decision.NOT_APPLICABLE);
            }
        }

        Decision combined = decisionCombiner.combine(decisions);
        return new DecisionResult(combined, firedPolicyIds, describe(combined, firedPolicyIds), Map.of());
    }

    /**
     * Most-specific-scope-wins (ADR-0009): if any tenant-scoped policy is
     * applicable, global policies are ignored for this request.
     */
    private List<Policy> chooseByScopePrecedence(List<Policy> matched, String tenantId) {
        List<Policy> tenantScoped = matched.stream()
                .filter(p -> !Tenants.GLOBAL.equals(p.effectiveTenantId()))
                .toList();

        if (!tenantScoped.isEmpty()) {
            return tenantScoped;
        }

        return matched.stream()
                .filter(p -> Tenants.GLOBAL.equals(p.effectiveTenantId()))
                .toList();
    }

    private String describe(Decision combined, List<String> firedPolicyIds) {
        if (firedPolicyIds.isEmpty()) {
            return "Denied: no applicable policy (default deny)";
        }
        return (combined == Decision.ALLOW ? "Allowed by policy: " : "Denied by policy: ") + firedPolicyIds;
    }

    private Decision mapEffect(Policy policy) {
        return policy.getEffect() == null
                ? Decision.DENY
                : Decision.valueOf(policy.getEffect().name());
    }
}
