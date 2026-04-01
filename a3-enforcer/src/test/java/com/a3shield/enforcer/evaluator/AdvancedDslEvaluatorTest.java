package com.a3shield.enforcer.evaluator;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedDslEvaluatorTest {

    private final DslConditionEvaluator evaluator = new DslConditionEvaluator();

    @Test
    void shouldHandleAndCondition() {

        RequestContext ctx = RequestContext.builder()
                .subject(Subject.builder().id("1").type(SubjectType.USER).build())
                .resource(Resource.builder()
                        .attributes(Map.of("ownerId", "1"))
                        .build())
                .build();

        boolean result = evaluator.evaluate(
                "subject.type == USER AND resource.ownerId == subject.id",
                ctx
        );

        assertThat(result).isTrue();
    }

    @Test
    void shouldHandleOrCondition() {

        RequestContext ctx = RequestContext.builder()
                .subject(Subject.builder().id("2").type(SubjectType.USER).build())
                .resource(Resource.builder()
                        .attributes(Map.of("ownerId", "1"))
                        .build())
                .build();

        boolean result = evaluator.evaluate(
                "resource.ownerId == subject.id OR subject.type == USER",
                ctx
        );

        assertThat(result).isTrue();
    }

    @Test
    void shouldHandleGreaterThan() {

        RequestContext ctx = RequestContext.builder()
                .resource(Resource.builder()
                        .attributes(Map.of("price", 200))
                        .build())
                .build();

        boolean result = evaluator.evaluate(
                "resource.price > 100",
                ctx
        );

        assertThat(result).isTrue();
    }

    @Test
    void shouldHandleNotEquals() {

        RequestContext ctx = RequestContext.builder()
                .subject(Subject.builder().id("1").build())
                .build();

        boolean result = evaluator.evaluate(
                "subject.id != 2",
                ctx
        );

        assertThat(result).isTrue();
    }
}