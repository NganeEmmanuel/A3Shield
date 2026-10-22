package com.a3shield.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * <p><b>Intent</b> describes what the caller is trying to do — typically
 * built from an {@code @A3Intent}-marked request parameter and checked
 * pre-execution by {@code @A3Authorize}.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intent extends Resource {
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
