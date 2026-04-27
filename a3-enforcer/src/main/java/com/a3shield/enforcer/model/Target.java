package com.a3shield.enforcer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Target extends Resource{
    private String id;
    private String type;
    private Map<String, Object> attributes;
    private static final Logger logger = LoggerFactory.getLogger(Intent.class);

    @Override
    public Object resolve(String field) {
        if ("id".equals(field)) return id;
        if ("type".equals(field)) return type;

        if (attributes == null || !attributes.containsKey(field)) {
            logger.debug("Missing attribute '{}' on resource {}", field, this);
            return null;
        }

        return attributes.get(field);
    }
}
