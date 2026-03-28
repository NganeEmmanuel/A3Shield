package com.a3shield.enforcer.decision;

import java.util.List;

/**
 * <p><b>DecisionCombiner</b> defines the contract for combining multiple
 * {@link Decision} results into a single, consolidated outcome.</p>
 *
 * <p>Implementations of this interface are responsible for applying a
 * combining algorithm (such as permit-overrides, deny-overrides, or
 * first-applicable) to merge individual policy evaluation results into
 * one final decision.</p>
 *
 * <p>Typical responsibilities may include:</p>
 * <ul>
 *   <li>Resolving conflicts between multiple decisions.</li>
 *   <li>Applying a specific combining strategy consistently.</li>
 *   <li>Preserving obligations, advice, or diagnostic information
 *       from the contributing decisions.</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> Implementers should document whether their
 * combiner implementation is safe for concurrent use.</p>
 */
public interface DecisionCombiner {

    /**
     * <p>Combine a list of individual {@link Decision} objects into a
     * single consolidated decision.</p>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>The input list must not be {@code null}; use an empty list
     *       if no decisions are available.</li>
     *   <li>The returned {@link Decision} must not be {@code null}.</li>
     *   <li>Implementations should clearly document the combining
     *       algorithm used (e.g., permit-overrides, deny-overrides).</li>
     * </ul>
     *
     * @param decisions the list of individual decisions to combine; must not be null
     * @return a single {@link Decision} representing the combined outcome
     */
    Decision combine(List<Decision> decisions);
}
