package com.a3shield.policymgmt;

/**
 * Thrown when policy migration discovery, parsing or application fails.
 * Deliberately fatal: a broken migration aborts startup (fail-closed,
 * ADR-0003).
 */
public class PolicyMigrationException extends RuntimeException {

    public PolicyMigrationException(String message) {
        super(message);
    }

    public PolicyMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
