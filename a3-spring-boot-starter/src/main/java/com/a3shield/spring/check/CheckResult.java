package com.a3shield.spring.check;

/**
 * Outcome of a single {@link RequestCheck}. The reason is included in the
 * 403 response body, so never put sensitive information in it.
 */
public record CheckResult(boolean allowed, String reason) {

    public static CheckResult allow() {
        return new CheckResult(true, null);
    }

    public static CheckResult deny(String reason) {
        return new CheckResult(false, reason == null || reason.isBlank() ? "Request denied" : reason);
    }
}
