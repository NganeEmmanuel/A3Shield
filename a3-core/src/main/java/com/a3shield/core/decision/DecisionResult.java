package com.a3shield.core.decision;

import java.util.List;
import java.util.Map;

/**
 * <p><b>DecisionResult</b> is the structured outcome of one evaluation
 * (ADR-0005). It carries not just the verdict but <i>why</i>: which policies
 * fired, a human-readable reason and any obligations the caller must honor
 * on allow. This is what audit logs record and what the admin interface
 * shows when explaining a denial.</p>
 *
 * @param decision          final combined decision (never NOT_APPLICABLE)
 * @param matchedPolicyIds  ids of policies that actually fired (ALLOW or DENY)
 * @param reason            human-readable explanation of the outcome
 * @param obligations       post-allow requirements; empty until policies
 *                          declare them
 */
public record DecisionResult(
        Decision decision, List<String> matchedPolicyIds, String reason, Map<String, Object> obligations) {

    public DecisionResult {
        matchedPolicyIds = matchedPolicyIds == null ? List.of() : List.copyOf(matchedPolicyIds);
        obligations = obligations == null ? Map.of() : Map.copyOf(obligations);
    }

    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }

    public boolean isDenied() {
        return decision == Decision.DENY;
    }

    public static DecisionResult allow(String reason) {
        return new DecisionResult(Decision.ALLOW, List.of(), reason, Map.of());
    }

    public static DecisionResult deny(String reason) {
        return new DecisionResult(Decision.DENY, List.of(), reason, Map.of());
    }
}
