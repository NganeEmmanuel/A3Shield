package com.a3shield.enforcer.context;

import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Subject;
import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.action.Action;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.*;

@Getter
@NoArgsConstructor(force = true)
@Builder
// we can use an annotation like A3ValidateResources to validate all list of resources. that we it is done on creation
// of a request context object
public class RequestContext {

    private Subject
            subject; // this info can be gotten from both the request and DB depending on the usage (based on checks
    // performed)
    // builder pattern recommended
    @Setter
    private List<Resource> resources;

    private Action action;
    private Map<String, Object> context; // time, location, ip, metadata, etc.

    @Getter(AccessLevel.NONE)
    private String tenantId; // ADR-0009: every evaluation is tenant-scoped

    public RequestContext(
            Subject subject, List<Resource> resources, Action action, Map<String, Object> context, String tenantId) {
        this.subject = subject;
        this.resources = resources;
        this.action = action;
        this.context = context;
        this.tenantId = tenantId;
        validateResources(resources);
    }

    /** The tenant this evaluation is scoped to; never null (defaults to {@link Tenants#DEFAULT_TENANT}). */
    public String getTenantId() {
        return tenantId == null ? Tenants.DEFAULT_TENANT : tenantId;
    }

    private void validateResources(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) return;

        Object type = resources.getFirst().resolve("type");

        for (Resource r : resources) {
            if (!Objects.equals(type, r.resolve("type"))) {
                throw new IllegalArgumentException("Mixed resource types detected in RequestContext");
            }
        }
    }

    /** The (homogeneous) resource type of this request, or null when the request carries no resources. */
    public String getResourceType() {
        if (resources == null || resources.isEmpty()) return null;
        Object type = resources.getFirst().resolve("type");
        return type != null ? type.toString() : null;
    }
}
