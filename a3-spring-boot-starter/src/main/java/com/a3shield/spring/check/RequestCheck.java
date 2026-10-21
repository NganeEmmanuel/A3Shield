package com.a3shield.spring.check;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>One pluggable perimeter check executed before the request enters the
 * enforcement pipeline (ADR-0001). Implementations are provided by the host
 * application — A3Shield deliberately ships no built-in CORS/rate-limit
 * checks; those belong to infrastructure the app already has.</p>
 *
 * <p>Checks run in order (Spring's {@code Ordered}/{@code @Order} respected)
 * and the first denial short-circuits the chain.</p>
 */
@FunctionalInterface
public interface RequestCheck {

    /**
     * Evaluate the incoming request.
     *
     * @param request the incoming servlet request
     * @return an allowed result to pass, or a denial with a reason
     */
    CheckResult check(HttpServletRequest request);
}
