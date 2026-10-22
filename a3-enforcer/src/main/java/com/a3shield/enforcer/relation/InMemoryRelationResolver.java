package com.a3shield.enforcer.relation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p><b>InMemoryRelationResolver</b> is a map-backed {@link RelationResolver}
 * for tests and demos. Relations are stored as
 * {@code subjectId|relation -> Set<resourceUrn>}.</p>
 */
public class InMemoryRelationResolver implements RelationResolver {

    private final Map<String, Set<String>> relations;

    public InMemoryRelationResolver(Map<String, Set<String>> relations) {
        this.relations = Map.copyOf(relations);
    }

    @Override
    public boolean isRelated(String subjectId, String relation, String resourceUrn) {
        return relations.getOrDefault(key(subjectId, relation), Set.of()).contains(resourceUrn);
    }

    @Override
    public Set<String> relatedResources(String subjectId, String relation, String resourceType) {
        String prefix = resourceType + ":";
        return relations.getOrDefault(key(subjectId, relation), Set.of()).stream()
                .filter(urn -> urn.startsWith(prefix))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String key(String subjectId, String relation) {
        return subjectId + "|" + relation;
    }
}
