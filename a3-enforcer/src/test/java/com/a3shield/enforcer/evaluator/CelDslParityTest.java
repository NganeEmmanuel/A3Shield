package com.a3shield.enforcer.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.core.model.Target;
import com.a3shield.enforcer.context.RequestContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Parity suite (ADR-0002): every legacy simple-DSL expression shape must
 * produce the same result under CEL. The legacy {@link DslConditionEvaluator}
 * stays as the compatibility engine; this suite is the contract that the
 * facade keeps them equivalent.
 */
class CelDslParityTest {

    private final DslConditionEvaluator legacy = new DslConditionEvaluator();
    private final CelConditionEvaluator cel = new CelConditionEvaluator();

    private RequestContext context(String subjectId, String subjectType, Map<String, Object> targetAttrs) {
        return RequestContext.builder()
                .subject(Subject.builder()
                        .id(subjectId)
                        .type(SubjectType.valueOf(subjectType))
                        .build())
                .resources(List.of(Target.builder().attributes(targetAttrs).build()))
                .build();
    }

    @ParameterizedTest
    @CsvSource({
        "1, USER, resource.ownerId == subject.id, ownerId=1, true",
        "2, USER, resource.ownerId == subject.id, ownerId=1, false",
        "1, USER, subject.type == USER AND resource.ownerId == subject.id, ownerId=1, true",
        "1, SERVICE, subject.type == USER AND resource.ownerId == subject.id, ownerId=1, false",
        "2, USER, resource.ownerId == subject.id OR subject.type == USER, ownerId=1, true",
        "2, SERVICE, resource.ownerId == subject.id OR subject.type == USER, ownerId=1, false",
        "1, USER, resource.price > 100, price=200, true",
        "1, USER, resource.price > 100, price=50, false",
        "1, USER, resource.price < 100, price=50, true",
        "1, USER, subject.id != admin, none, true",
        "1, USER, subject.id != 9, none, true"
    })
    void legacyExpressionsMustEvaluateIdentically(
            String subjectId, String subjectType, String expression, String attrs, boolean expected) {
        Map<String, Object> targetAttrs;
        if ("none".equals(attrs)) {
            targetAttrs = Map.of();
        } else {
            String[] pair = attrs.split("=");
            // price is the one numeric attribute in this corpus; ids stay strings
            targetAttrs =
                    pair[0].equals("price") ? Map.of(pair[0], Integer.valueOf(pair[1])) : Map.of(pair[0], pair[1]);
        }
        RequestContext ctx = context(subjectId, subjectType, targetAttrs);

        // every row in this corpus evaluates identically under both engines.
        // ('subject.id != 9' is mixed-type string/int: CEL's heterogeneous
        // equality says not-equal, matching the legacy coercion — see
        // CelConditionEvaluatorTest for the one real difference: ordered
        // comparisons against string-typed numbers fail closed in CEL.)
        assertThat(legacy.evaluate(expression, ctx)).as("legacy %s", expression).isEqualTo(expected);
        assertThat(cel.evaluate(expression, ctx)).as("cel %s", expression).isEqualTo(expected);
    }

    @Test
    void blankAndNullConditionsMustStayAllowByDefault() {
        RequestContext ctx = context("1", "USER", Map.of("ownerId", "1"));

        assertThat(legacy.evaluate(null, ctx)).isTrue();
        assertThat(cel.evaluate(null, ctx)).isTrue();
        assertThat(legacy.evaluate("", ctx)).isTrue();
        assertThat(cel.evaluate("", ctx)).isTrue();
    }
}
