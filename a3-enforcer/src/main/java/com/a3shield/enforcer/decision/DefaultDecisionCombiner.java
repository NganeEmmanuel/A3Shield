package com.a3shield.enforcer.decision;

import java.util.List;

/**
 * <p><b>DefaultDecisionCombiner</b> is the default implementation of
 * {@link DecisionCombiner} that applies a simple combining strategy
 * to merge multiple {@link Decision} results into a single outcome.</p>
 *
 * <p>The combining algorithm used here is:</p>
 * <ul>
 *   <li>If the list of decisions is {@code null} or empty, return {@link Decision#DENY}.</li>
 *   <li>If any decision is {@link Decision#DENY}, return {@link Decision#DENY}.</li>
 *   <li>If any decision is {@link Decision#ALLOW}, return {@link Decision#ALLOW}.</li>
 *   <li>Otherwise, return {@link Decision#DENY}.</li>
 * </ul>
 *
 * <p>This strategy prioritizes denial over allowance, ensuring that
 * a single denial overrides multiple allowances.</p>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and safe
 * for concurrent use.</p>
 */
public class DefaultDecisionCombiner implements DecisionCombiner {

    /**
     * <p>Combine a list of individual {@link Decision} objects into a
     * single consolidated decision using the default deny-overrides strategy.</p>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>The input list may be {@code null} or empty; in such cases,
     *       {@link Decision#DENY} is returned.</li>
     *   <li>If the list contains {@link Decision#DENY}, the result is {@link Decision#DENY}.</li>
     *   <li>If the list contains {@link Decision#ALLOW}, the result is {@link Decision#ALLOW}.</li>
     *   <li>If neither condition is met, {@link Decision#DENY} is returned.</li>
     * </ul>
     *
     * @param decisions the list of individual decisions to combine; may be null or empty
     * @return a single {@link Decision} representing the combined outcome
     */
    @Override
    public Decision combine(List<Decision> decisions) {

        if (decisions == null || decisions.isEmpty()) {
            return Decision.DENY;
        }

        if (decisions.contains(Decision.DENY)) {
            return Decision.DENY;
        }

        if (decisions.contains(Decision.ALLOW)) {
            return Decision.ALLOW;
        }

        return Decision.DENY;
    }
}
