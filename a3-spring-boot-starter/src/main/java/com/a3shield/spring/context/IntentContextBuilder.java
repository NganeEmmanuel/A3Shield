package com.a3shield.spring.context;

import com.a3shield.enforcer.model.Intent;
import org.aspectj.lang.JoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Component
public class IntentContextBuilder {
    private final Logger logger = LoggerFactory.getLogger(IntentContextBuilder.class);
    public Intent build(Object object) {
        Map<String, Object> attributes = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                attributes.put(field.getName(), field.get(object));
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage());
            }
        }

        return Intent.builder()
                .type(object.getClass().getSimpleName())
                .attributes(attributes)
                .build();
    }

}
