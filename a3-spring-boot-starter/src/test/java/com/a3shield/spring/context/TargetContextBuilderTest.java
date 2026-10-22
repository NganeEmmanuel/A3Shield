package com.a3shield.spring.context;

import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Target;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TargetContextBuilderTest {

    public static class Document {
        private final String id;
        private final String name;

        Document(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final TargetContextBuilder builder = new TargetContextBuilder();

    @Test
    void shouldBuildSingleTargetFromObject() {
        List<Resource> targets = builder.build(new Document("7", "invoice"));

        assertThat(targets).hasSize(1);
        Resource target = targets.getFirst();
        assertThat(target).isInstanceOf(Target.class);
        assertThat(target.resolve("type")).isEqualTo("Document");
        assertThat(target.resolve("id")).isEqualTo("7");
        assertThat(target.resolve("name")).isEqualTo("invoice");
    }

    @Test
    void shouldBuildTargetListFromCollection() {
        List<Resource> targets = builder.build(List.of(
                new Document("1", "a"),
                new Document("2", "b")
        ));

        assertThat(targets).hasSize(2);
        assertThat(targets).allMatch(t -> "Document".equals(t.resolve("type")));
    }
}
