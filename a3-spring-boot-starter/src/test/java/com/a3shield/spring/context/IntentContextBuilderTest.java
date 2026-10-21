package com.a3shield.spring.context;

import com.a3shield.enforcer.model.Intent;
import com.a3shield.enforcer.model.Resource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentContextBuilderTest {

    public static class Order {
        private final String id;
        private final double amount;

        Order(String id, double amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    private final IntentContextBuilder builder = new IntentContextBuilder();

    @Test
    void shouldBuildIntentWithAllFieldsAsAttributes() {
        Resource intent = builder.build(new Order("o-1", 99.0));

        assertThat(intent).isInstanceOf(Intent.class);
        assertThat(intent.resolve("type")).isEqualTo("Order");
        assertThat(intent.resolve("id")).isEqualTo("o-1");
        assertThat(intent.resolve("amount")).isEqualTo(99.0);
    }
}
