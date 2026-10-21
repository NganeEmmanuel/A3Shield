package com.a3shield.spring.exceptions;

/**
 * <p>Thrown when the A3Shield enforcer denies access.</p>
 *
 * <p>Unchecked on purpose: it must propagate through proxies, aspects and
 * interceptors untouched so callers and exception handlers can match on the
 * type. A richer payload (full DecisionResult) is attached in M2 per
 * ADR-0005.</p>
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
