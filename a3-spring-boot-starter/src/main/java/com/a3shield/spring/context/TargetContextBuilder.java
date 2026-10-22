package com.a3shield.spring.context;

import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetContextBuilder {
    private final Logger logger = LoggerFactory.getLogger(TargetContextBuilder.class);

    // todo implement method
    public List<Resource> build(Object objects) {
        List<Resource> targetList = new ArrayList<>();
        if (objects instanceof List<?>) {
            targetList.addAll(buildTargetList((List<?>) objects));
        } else {
            targetList.add(buildTarget(objects));
        }
        return targetList;
    }

    private Target buildTarget(Object object) {
        Map<String, Object> attributes = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                field.setAccessible(true);
                attributes.put(field.getName(), field.get(object));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Target.builder()
                .id(String.valueOf(attributes.get("id")))
                .type(object.getClass().getSimpleName())
                .attributes(attributes)
                .build();
    }

    private List<Target> buildTargetList(List<?> objects) {
        // todo i think this is too slow we need to fix this
        List<Target> targets = new ArrayList<>();
        objects.forEach(o -> targets.add(buildTarget(o)));
        return targets;
    }
}
