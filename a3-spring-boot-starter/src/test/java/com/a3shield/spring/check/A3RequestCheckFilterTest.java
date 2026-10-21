package com.a3shield.spring.check;

import com.a3shield.enforcer.model.Subject;
import com.a3shield.enforcer.model.SubjectType;
import com.a3shield.spring.context.SubjectContext;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class A3RequestCheckFilterTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        new SubjectContext(() -> Subject.builder().id("1").type(SubjectType.USER).build());
    }

    @AfterEach
    void tearDown() {
        SubjectContext.clearCurrentSubject();
    }

    @Test
    void shouldProceedWhenAllChecksAllow() throws ServletException, IOException {
        A3RequestCheckFilter filter = new A3RequestCheckFilter(
                new RequestCheckChain(List.of(req -> CheckResult.allow()))
        );
        MockFilterChain servletChain = new MockFilterChain();

        filter.doFilter(request, response, servletChain);

        assertThat(servletChain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAnswer403WithReasonAndNotProceedOnDenial() throws ServletException, IOException {
        A3RequestCheckFilter filter = new A3RequestCheckFilter(
                new RequestCheckChain(List.of(req -> CheckResult.deny("blocked")))
        );
        MockFilterChain servletChain = new MockFilterChain();

        filter.doFilter(request, response, servletChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("blocked");
        assertThat(servletChain.getRequest()).isNull();
    }

    @Test
    void shouldClearHoldersAfterRequestEvenWhenDenied() throws ServletException, IOException {
        SubjectContext.setCurrentSubject(Subject.builder().id("9").build());

        A3RequestCheckFilter filter = new A3RequestCheckFilter(
                new RequestCheckChain(List.of(req -> CheckResult.deny("blocked")))
        );

        filter.doFilter(request, response, new MockFilterChain());

        // cleared holder re-extracts from the SubjectExtractor, so id 9 must be gone
        assertThat(SubjectContext.getCurrentSubject().getId()).isEqualTo("1");
    }
}
