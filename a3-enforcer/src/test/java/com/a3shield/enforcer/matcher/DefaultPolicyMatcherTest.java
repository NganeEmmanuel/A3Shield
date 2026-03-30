package com.a3shield.enforcer.matcher;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.*;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.enforcer.effect.Effect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DefaultPolicyMatcher
 *
 * Validates that policies are correctly filtered
 * based on subject, resource, and action.
 */
class DefaultPolicyMatcherTest {

    private final PolicyMatcher matcher = new DefaultPolicyMatcher();

    @Test
    void shouldMatchPolicyWhenAllFieldsMatch() {

        Policy policy = Policy.builder()
                .resource("document")
                .action("read")
                .subject(Map.of("type", "USER"))
                .effect(Effect.ALLOW)
                .build();

        RequestContext context = RequestContext.builder()
                .subject(Subject.builder()
                        .id("1")
                        .type(SubjectType.USER)
                        .roles(Set.of("user"))
                        .build())
                .resource(Resource.builder()
                        .type("document")
                        .build())
                .action(new Action("read"))
                .build();

        List<Policy> result = matcher.match(context, List.of(policy));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldNotMatchWhenResourceDiffers() {

        Policy policy = Policy.builder()
                .resource("order")
                .action("read")
                .effect(Effect.ALLOW)
                .build();

        RequestContext context = RequestContext.builder()
                .resource(Resource.builder().type("document").build())
                .action(new Action("read"))
                .build();

        List<Policy> result = matcher.match(context, List.of(policy));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotMatchWhenActionDiffers() {

        Policy policy = Policy.builder()
                .resource("document")
                .action("write")
                .effect(Effect.ALLOW)
                .build();

        RequestContext context = RequestContext.builder()
                .resource(Resource.builder().type("document").build())
                .action(new Action("read"))
                .build();

        List<Policy> result = matcher.match(context, List.of(policy));

        assertThat(result).isEmpty();
    }
}