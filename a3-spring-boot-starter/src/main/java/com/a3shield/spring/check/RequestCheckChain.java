package com.a3shield.spring.check;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.OrderComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Runs {@link RequestCheck}s in order; the first denial short-circuits the
 * chain. An empty chain allows everything.
 */
public class RequestCheckChain {

    private final List<RequestCheck> checks;

    public RequestCheckChain(List<RequestCheck> checks) {
        List<RequestCheck> ordered = new ArrayList<>(checks);
        OrderComparator.sort(ordered);
        this.checks = List.copyOf(ordered);
    }

    /**
     * @param request the incoming request
     * @return the first denial, or empty when every check allows
     */
    public Optional<CheckResult> firstDenial(HttpServletRequest request) {
        for (RequestCheck check : checks) {
            CheckResult result = check.check(request);
            if (result != null && !result.allowed()) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    public List<RequestCheck> getChecks() {
        return checks;
    }
}
