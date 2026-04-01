package com.a3shield.enforcer.evaluator;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DslConditionEvaluatorTest {

    private final DslConditionEvaluator evaluator = new DslConditionEvaluator();

    @Test
    void shouldEvaluateSimpleEquality() {

        RequestContext context = RequestContext.builder()
                .subject(Subject.builder()
                        .id("1")
                        .type(SubjectType.USER)
                        .build())
                .resource(Resource.builder()
                        .attributes(Map.of("ownerId", "1"))
                        .build())
                .build();

        boolean result = evaluator.evaluate(
                "resource.ownerId == subject.id",
                context
        );

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenValuesDiffer() {

        RequestContext context = RequestContext.builder()
                .subject(Subject.builder().id("2").build())
                .resource(Resource.builder()
                        .attributes(Map.of("ownerId", "1"))
                        .build())
                .build();

        boolean result = evaluator.evaluate(
                "resource.ownerId == subject.id",
                context
        );

        assertThat(result).isFalse();
    }
}