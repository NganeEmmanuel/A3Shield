package com.a3shield.spring.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.core.model.Target;
import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.spring.tenant.TenantContextHolder;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoldersTest {

    @BeforeEach
    void setUp() {
        // static wiring the auto-configuration normally performs
        new TargetContextHolder(new TargetContextBuilder());
        new SubjectContext(
                () -> Subject.builder().id("1").type(SubjectType.USER).build());
    }

    @AfterEach
    void tearDown() {
        SubjectContext.clearCurrentSubject();
        RequestContextHolder.clearRequestContext();
        TargetContextHolder.clearTargetContext();
        TenantContextHolder.clear();
    }

    @Test
    void requestContextShouldCarryTenantFromHolder() {
        TenantContextHolder.setTenantId("acme");

        RequestContext ctx = RequestContextHolder.getRequestContext(
                List.of(Target.builder().id("1").type("Document").build()));

        assertThat(ctx.getTenantId()).isEqualTo("acme");

        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(Tenants.DEFAULT_TENANT);
    }

    @Test
    void subjectContextShouldExtractOnceAndCacheUntilCleared() {
        AtomicInteger extractions = new AtomicInteger();
        new SubjectContext(() -> {
            extractions.incrementAndGet();
            return Subject.builder().id("2").build();
        });

        SubjectContext.getCurrentSubject();
        SubjectContext.getCurrentSubject();

        assertThat(extractions.get()).isEqualTo(1);
        assertThat(SubjectContext.getCurrentSubject().getId()).isEqualTo("2");

        SubjectContext.clearCurrentSubject();
        SubjectContext.getCurrentSubject();

        assertThat(extractions.get()).isEqualTo(2);
    }

    @Test
    void requestContextShouldBeBuiltOnceAndReuseResourcesSwapped() {
        Target first = Target.builder().id("1").type("Document").build();
        Target second = Target.builder().id("2").type("Document").build();

        RequestContext built = RequestContextHolder.getRequestContext(List.of(first));
        RequestContext same = RequestContextHolder.getRequestContext(List.of(second));

        assertThat(same).isSameAs(built);
        assertThat(same.getResources()).containsExactly(second);
        assertThat(same.getSubject().getId()).isEqualTo("1");

        RequestContextHolder.clearRequestContext();
        assertThat(RequestContextHolder.getRequestContext()).isNull();
    }

    @Test
    void targetContextShouldRoundTripAndClear() {
        List<com.a3shield.core.model.Resource> targets =
                TargetContextHolder.getTargetContext(List.of(new TargetContextBuilderTest.Document("1", "a")));

        assertThat(targets).isNotEmpty();

        TargetContextHolder.clearTargetContext();
        // cleared holder rebuilds on next access
        assertThat(TargetContextHolder.getTargetContext(List.of(new TargetContextBuilderTest.Document("2", "b"))))
                .isNotEmpty();
    }
}
