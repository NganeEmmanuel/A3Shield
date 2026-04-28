package com.a3shield.enforcer.context;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.model.*;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
//we can use an annotation like A3ValidateResources to validate all list of resources. that we it is done on creation of a request context object
public class RequestContext {

    private Subject subject; // this info can be gotten from both the request and DB depending on the usage (based on checks performed)
    // builder pattern recommended
    @Setter
    private List<Resource> resources;
    private Action action;
    private Map<String, Object> context; // time, location, ip, metadata, etc.

    private void validateResources(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) return;

        Object type = resources.getFirst().resolve("type");

        for (Resource r : resources) {
            if (!Objects.equals(type, r.resolve("type"))) {
                throw new IllegalArgumentException(
                        "Mixed resource types detected in RequestContext"
                );
            }
        }
    }

}