package com.a3shield.enforcer.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.core.model.Target;
import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.context.RequestContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CelConditionEvaluatorTest {

    private final CelConditionEvaluator evaluator = new CelConditionEvaluator();

    private RequestContext context(Subject subject, Target target) {
        return RequestContext.builder()
                .subject(subject)
                .resources(target == null ? List.of() : List.of(target))
                .action(new Action("read"))
                .context(Map.of("ip", "10.0.0.1", "hour", 14))
                .tenantId("acme")
                .build();
    }

    private Subject user(String id) {
        return Subject.builder()
                .id(id)
                .type(SubjectType.USER)
                .roles(Set.of("editor"))
                .attributes(Map.of("department", "finance"))
                .build();
    }

    @Test
    void shouldEvaluateSimpleEquality() {
        var ctx = context(
                user("1"), Target.builder().attributes(Map.of("ownerId", "1")).build());

        assertThat(evaluator.evaluate("resource.ownerId == subject.id", ctx)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenValuesDiffer() {
        var ctx = context(
                user("2"), Target.builder().attributes(Map.of("ownerId", "1")).build());

        assertThat(evaluator.evaluate("resource.ownerId == subject.id", ctx)).isFalse();
    }

    @Test
    void shouldTranslateLegacyAndOrWordOperators() {
        var ctx = context(
                user("1"), Target.builder().attributes(Map.of("ownerId", "1")).build());

        assertThat(evaluator.evaluate("subject.type == USER AND resource.ownerId == subject.id", ctx))
                .isTrue();
        assertThat(evaluator.evaluate("resource.ownerId == subject.id OR subject.type == SERVICE", ctx))
                .isTrue();
        assertThat(evaluator.evaluate("subject.type == SERVICE AND resource.ownerId == subject.id", ctx))
                .isFalse();
    }

    @Test
    void shouldEvaluateNumericComparisons() {
        var ctx = context(
                user("1"), Target.builder().attributes(Map.of("price", 200)).build());

        assertThat(evaluator.evaluate("resource.price > 100", ctx)).isTrue();
        assertThat(evaluator.evaluate("resource.price < 100", ctx)).isFalse();
        assertThat(evaluator.evaluate("context.hour >= 14", ctx)).isTrue();
    }

    @Test
    void shouldEvaluateStringLiteralsAndRoles() {
        var ctx = context(user("1"), Target.builder().attributes(Map.of()).build());

        assertThat(evaluator.evaluate("subject.id != admin", ctx)).isTrue();
        assertThat(evaluator.evaluate("'editor' in subject.roles", ctx)).isTrue();
        assertThat(evaluator.evaluate("subject.department == 'finance'", ctx)).isTrue();
    }

    @Test
    void shouldEvaluateParenthesesAndTenantAndContext() {
        var ctx = context(
                user("1"), Target.builder().attributes(Map.of("ownerId", "1")).build());

        assertThat(evaluator.evaluate("tenant == 'acme' && context.ip == '10.0.0.1'", ctx))
                .isTrue();
        // native CEL (parentheses + quoted literals) passes through untouched
        assertThat(evaluator.evaluate(
                        "(subject.type == 'USER' || subject.type == 'SERVICE') && resource.ownerId == subject.id", ctx))
                .isTrue();
    }

    @Test
    void shouldEvaluateExistsMacroAcrossResourceList() {
        // ANY-match semantics across multiple resources via the CEL macro
        var ctx = RequestContext.builder()
                .subject(user("1"))
                .resources(List.of(
                        Target.builder().attributes(Map.of("ownerId", "2")).build(),
                        Target.builder().attributes(Map.of("ownerId", "1")).build()))
                .build();

        assertThat(evaluator.evaluate("resources.exists(r, r.ownerId == subject.id)", ctx))
                .isTrue();
    }

    @Test
    void blankAndNullConditionsAllowByDefault() {
        var ctx = context(user("1"), null);

        assertThat(evaluator.evaluate(null, ctx)).isTrue();
        assertThat(evaluator.evaluate("   ", ctx)).isTrue();
    }

    @Test
    void brokenExpressionsFailClosed() {
        var ctx = context(user("1"), Target.builder().attributes(Map.of()).build());

        assertThat(evaluator.evaluate("this is not ((( valid", ctx)).isFalse();
        assertThat(evaluator.evaluate("subject.id", ctx)).isFalse(); // not a boolean expression
    }

    @Test
    void missingFieldsFailClosed() {
        var ctx = context(user("1"), Target.builder().attributes(Map.of()).build());

        assertThat(evaluator.evaluate("resource.ownerId == subject.id", ctx)).isFalse();
    }

    @Test
    void heterogeneousEqualityFollowsCelSemantics() {
        // string vs int: different types are never equal, so != is true —
        // this matches the legacy DSL's coercion behaviour, so no parity break
        var ctx = context(user("1"), Target.builder().attributes(Map.of()).build());

        assertThat(evaluator.evaluate("subject.id != 2", ctx)).isTrue();
        assertThat(evaluator.evaluate("subject.id == 2", ctx)).isFalse();
        assertThat(evaluator.evaluate("subject.id != '2'", ctx)).isTrue();
    }

    @Test
    void numericComparisonAgainstStringAttributeFailsClosed() {
        // documented difference from the legacy DSL (ADR-0002): legacy coerced
        // numeric strings ("200" > 100 was true); CEL is strictly typed and the
        // comparison fails closed. Store numbers as numbers.
        var ctx = context(
                user("1"), Target.builder().attributes(Map.of("price", "200")).build());

        assertThat(evaluator.evaluate("resource.price > 100", ctx)).isFalse();
    }

    @Test
    void oversizedExpressionsFailClosed() {
        var ctx = context(user("1"), Target.builder().attributes(Map.of()).build());
        String oversized = "context.hour == 1 && " + "x".repeat(CelConditionEvaluator.MAX_EXPRESSION_LENGTH);

        assertThat(evaluator.evaluate(oversized, ctx)).isFalse();
    }

    @Test
    void facadeTranslatesLegacySyntaxToCel() {
        assertThat(evaluator.toCel("subject.type == USER AND resource.ownerId == subject.id"))
                .isEqualTo("subject.type == 'USER' && resource.ownerId == subject.id");
        assertThat(evaluator.toCel("subject.id != admin OR context.ip == 10.0.0.1"))
                .isEqualTo("subject.id != 'admin' || context.ip == 10.0.0.1");
        assertThat(evaluator.toCel("resource.price > 100")).isEqualTo("resource.price > 100");
        assertThat(evaluator.toCel("tenant == 'acme' && resources.exists(r, r.x == 1)"))
                .isEqualTo("tenant == 'acme' && resources.exists(r, r.x == 1)");
    }
}
