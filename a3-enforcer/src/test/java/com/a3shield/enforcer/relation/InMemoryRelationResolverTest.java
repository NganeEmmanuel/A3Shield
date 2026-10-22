package com.a3shield.enforcer.relation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryRelationResolverTest {

    private final InMemoryRelationResolver resolver = new InMemoryRelationResolver(Map.of(
            "user-1|owner", Set.of("document:7", "folder:3"),
            "user-1|editor", Set.of("folder:9")));

    @Test
    void shouldAnswerRelationChecksByUrn() {
        assertThat(resolver.isRelated("user-1", "owner", "document:7")).isTrue();
        assertThat(resolver.isRelated("user-1", "owner", "document:8")).isFalse();
        assertThat(resolver.isRelated("user-1", "editor", "folder:9")).isTrue();
        assertThat(resolver.isRelated("user-2", "owner", "document:7")).isFalse();
    }

    @Test
    void shouldListRelatedResourcesOfType() {
        assertThat(resolver.relatedResources("user-1", "owner", "folder")).containsExactly("folder:3");
        assertThat(resolver.relatedResources("user-1", "owner", "document")).containsExactly("document:7");
        assertThat(resolver.relatedResources("user-1", "editor", "document")).isEmpty();
    }
}
