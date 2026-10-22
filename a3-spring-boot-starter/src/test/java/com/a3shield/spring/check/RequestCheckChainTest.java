package com.a3shield.spring.check;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestCheckChainTest {

    private static final class RecordingCheck implements RequestCheck, Ordered {
        private final String name;
        private final CheckResult result;
        private final List<String> invocations;
        private final int order;

        private RecordingCheck(String name, CheckResult result, List<String> invocations, int order) {
            this.name = name;
            this.result = result;
            this.invocations = invocations;
            this.order = order;
        }

        @Override
        public CheckResult check(HttpServletRequest request) {
            invocations.add(name);
            return result;
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    @Test
    void emptyChainAllowsEverything() {
        RequestCheckChain chain = new RequestCheckChain(List.of());

        assertThat(chain.firstDenial(new MockHttpServletRequest())).isEmpty();
    }

    @Test
    void shouldRunInOrderAndShortCircuitOnFirstDenial() {
        List<String> invocations = new ArrayList<>();
        RequestCheck first = new RecordingCheck("first", CheckResult.allow(), invocations, 1);
        RequestCheck second = new RecordingCheck("second", CheckResult.deny("blocked by second"), invocations, 2);
        RequestCheck neverReached = new RecordingCheck("third", CheckResult.allow(), invocations, 3);

        Optional<CheckResult> denial =
                new RequestCheckChain(List.of(neverReached, second, first)).firstDenial(new MockHttpServletRequest());

        assertThat(denial).isPresent();
        assertThat(denial.get().reason()).isEqualTo("blocked by second");
        assertThat(invocations).containsExactly("first", "second");
    }

    @Test
    void shouldTreatNullResultAsAllow() {
        List<String> invocations = new ArrayList<>();
        RequestCheck nullCheck = new RecordingCheck("null", null, invocations, 1);
        RequestCheck deny = new RecordingCheck("deny", CheckResult.deny("no"), invocations, 2);

        Optional<CheckResult> denial =
                new RequestCheckChain(List.of(deny, nullCheck)).firstDenial(new MockHttpServletRequest());

        assertThat(denial).isPresent();
        assertThat(invocations).containsExactly("null", "deny");
    }
}
