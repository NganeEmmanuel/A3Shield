package com.a3shield.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourceUrnTest {

    @Test
    void targetUrnShouldFollowTypeIdConvention() {
        Target target =
                Target.builder().id("42").type("folder").attributes(Map.of()).build();

        assertThat(target.urn()).isEqualTo("folder:42");
    }

    @Test
    void intentUrnShouldFollowTypeIdConvention() {
        Intent intent =
                Intent.builder().id("o-1").type("Order").attributes(Map.of()).build();

        assertThat(intent.urn()).isEqualTo("Order:o-1");
    }

    @Test
    void missingPartsShouldRenderAsQuestionMark() {
        Target anonymous = Target.builder().attributes(Map.of()).build();

        assertThat(anonymous.urn()).isEqualTo("?:?");
    }
}
