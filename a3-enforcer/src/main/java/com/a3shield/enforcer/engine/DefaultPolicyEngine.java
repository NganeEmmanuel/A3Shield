package com.a3shield.enforcer.engine;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.decision.*;
import com.a3shield.enforcer.evaluator.ConditionEvaluator;
import com.a3shield.enforcer.matcher.PolicyMatcher;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.enforcer.repository.PolicyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * <p><b>DefaultPolicyEngine</b> is the standard implementation of
 * {@link PolicyEngine} that orchestrates the evaluation of policies
 * against a given {@link RequestContext}.</p>
 *
 * <p>This engine coordinates the following components:</p>
 * <ul>
 *   <li>{@link PolicyRepository} – retrieves all available policies.</li>
 *   <li>{@link PolicyMatcher} – filters policies applicable to the request context.</li>
 *   <li>{@link ConditionEvaluator} – evaluates conditional expressions within policies.</li>
 *   <li>{@link DecisionCombiner} – merges individual policy decisions into a final outcome.</li>
 * </ul>
 *
 * <p>The evaluation process typically involves:</p>
 * <ol>
 *   <li>Loading all policies from the repository.</li>
 *   <li>Matching applicable policies against the request context.</li>
 *   <li>Evaluating conditions within each matched policy.</li>
 *   <li>Combining the resulting decisions into a single {@link Decision}.</li>
 * </ol>
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

    /**
     * <p>Create a new {@code DefaultPolicyEngine} with the required components.</p>
     *
     * @param policyRepository the repository used to load policies; must not be null
     * @param policyMatcher the matcher used to filter applicable policies; must not be null
     * @param conditionEvaluator the evaluator used to process policy conditions; must not be null
     * @param decisionCombiner the combiner used to merge individual decisions; must not be null
     */
    public DefaultPolicyEngine(
            PolicyRepository policyRepository,
            PolicyMatcher policyMatcher,
            ConditionEvaluator conditionEvaluator,
            DecisionCombiner decisionCombiner
    ) {
        this.policyRepository = policyRepository;
        this.policyMatcher = policyMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.decisionCombiner = decisionCombiner;
    }

    @Override
    public Decision evaluate(RequestContext context) {

        List<Policy> allPolicies = policyRepository.findAll();

        List<Policy> matchedPolicies = policyMatcher.match(context, allPolicies);

        List<Decision> decisions = new ArrayList<>();

        for (Policy policy : matchedPolicies) {

            boolean conditionResult = conditionEvaluator.evaluate(
                    policy.getCondition(), context
            );

            if (conditionResult) {
                decisions.add(mapEffect(policy));
            }
        }

        return decisionCombiner.combine(decisions);
    }

    private Decision mapEffect(Policy policy) {
        return policy.getEffect() == null
                ? Decision.DENY
                : Decision.valueOf(policy.getEffect().name());
    }
}
