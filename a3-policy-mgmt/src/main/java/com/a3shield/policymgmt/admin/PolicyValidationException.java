package com.a3shield.policymgmt.admin;

/** A policy mutation was rejected by validate-before-commit (ADR-0003). Nothing was written. */
public class PolicyValidationException extends RuntimeException {

    public PolicyValidationException(String message) {
        super(message);
    }
}
