package com.a3shield.enforcer.decision;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DefaultDecisionCombiner
 * <p>
 * Ensures correct conflict resolution:
 * DENY overrides ALLOW.
 */
class DefaultDecisionCombinerTest {

    private final DecisionCombiner combiner = new DefaultDecisionCombiner();

    @Test
    void shouldReturnDenyIfAnyDenyExists() {
        Decision result = combiner.combine(List.of(
                Decision.ALLOW,
                Decision.DENY
        ));

        assertThat(result).isEqualTo(Decision.DENY);
    }

    @Test
    void shouldReturnAllowIfOnlyAllowExists() {
        Decision result = combiner.combine(List.of(
                Decision.ALLOW
        ));

        assertThat(result).isEqualTo(Decision.ALLOW);
    }

    @Test
    void shouldReturnDenyIfEmpty() {
        Decision result = combiner.combine(List.of());

        assertThat(result).isEqualTo(Decision.DENY);
    }
}