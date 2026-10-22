package com.a3shield.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * <p><b>Target</b> is the resource being acted on — typically built from a
 * method's return value and checked post-execution by
 * {@code @A3SecurePolicy}.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Target extends Resource {
    private String id;
    private String type;
    private Map<String, Object> attributes;

    @Override
    public Object resolve(String field) {
        if ("id".equals(field)) return id;
        if ("type".equals(field)) return type;

        if (attributes == null || !attributes.containsKey(field)) {
            return null;
        }

        return attributes.get(field);
    }
}
