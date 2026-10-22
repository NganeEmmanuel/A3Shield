package com.a3shield.enforcer.policy;

import com.a3shield.core.tenant.Tenants;
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

    // ADR-0009: tenant this policy belongs to; null or Tenants.GLOBAL ("*") = applies across tenants
    private String tenantId;

    private String condition;              // DSL expression (Domain-Specific Language) for complex conditions

    // ADR-0008: reserved relations schema (e.g. declared resource relationships for
    // ReBAC). Ignored by the engine until the ReBAC milestone (M7).
    private Map<String, Object> relations;

    private String version;                // for policy versioning and cache invalidation
    private LocalDateTime updatedAt;       // for policy versioning and cache invalidation

    /** The effective tenant scope: {@link Tenants#GLOBAL} when unset. */
    public String effectiveTenantId() {
        return tenantId == null || tenantId.isBlank() ? Tenants.GLOBAL : tenantId;
    }
}
