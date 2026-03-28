package com.a3shield.enforcer.policy;

import com.a3shield.enforcer.effect.Effect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    private String id;                     // unique identifier for the policy
    private Effect effect;                 // ALLOW / DENY

    private Map<String, Object> subject;   // match conditions
    private String resource;               // resource type
    private String action;                 // action name
    private String condition;              // DSL expression (Domain-Specific Language) for complex conditions

    private String version;                // for policy versioning and cache invalidation
    private LocalDateTime updatedAt;       // for policy versioning and cache invalidation
}