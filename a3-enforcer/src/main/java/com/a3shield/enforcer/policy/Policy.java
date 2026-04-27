package com.a3shield.enforcer.policy;

import com.a3shield.enforcer.effect.Effect;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    private String id;                     // unique identifier for the policy
    private Effect effect;                 // ALLOW / DENY

    private Map<String, Object> subject;   // static match conditions e.g {"role":"Admin", "permission":{"user:read", "post:edit"}}

    // resource and action our our composite keys used to query every policy that matches the action
    private String resource;               // resource type
    private String action;                 // action name


    private String condition;              // DSL expression (Domain-Specific Language) for complex conditions

    private String version;                // for policy versioning and cache invalidation
    private LocalDateTime updatedAt;       // for policy versioning and cache invalidation
}