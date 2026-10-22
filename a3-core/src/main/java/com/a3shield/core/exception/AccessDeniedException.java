package com.a3shield.core.exception;

import com.a3shield.core.decision.DecisionResult;

/**
 * <p>Thrown when the A3Shield enforcer denies access (ADR-0005).</p>
 *
 * <p>Unchecked on purpose: it must propagate through proxies, aspects and
 * interceptors untouched so callers and exception handlers can match on the
 * type. Carries the {@link DecisionResult} that caused the denial, when one
 * exists.</p>
 */
public class AccessDeniedException extends RuntimeException {

    private final transient DecisionResult result;

    public AccessDeniedException(String message) {
        this(message, null);
    }

    public AccessDeniedException(String message, DecisionResult result) {
        super(message);
        this.result = result;
    }

    /** The evaluation outcome behind this denial; may be null when the exception is thrown without an evaluation. */
    public DecisionResult getResult() {
        return result;
    }
}
