package com.a3shield.core.decision;

/**
 * Outcome of a single policy's evaluation, before combining:
 * {@link #ALLOW} and {@link #DENY} mean a policy fired;
 * {@link #NOT_APPLICABLE} means the policy matched but its condition
 * did not hold (which is not a denial).
 */
public enum Decision {
    ALLOW,
    DENY,
    NOT_APPLICABLE
}
