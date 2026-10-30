package com.a3shield.enforcer.matcher;

import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Subject;
import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.policy.Policy;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p><b>DefaultPolicyMatcher</b> is the default implementation of
 * {@link PolicyMatcher} that filters policies based on attributes
 * in the provided {@link RequestContext}.</p>
 *
 * <p>This matcher evaluates:</p>
 * <ul>
 *   <li>Tenant scope — policy must be global or scoped to the request's tenant (ADR-0009).</li>
 *   <li>Resource type matching between policy and request.</li>
 *   <li>Action name matching between policy and request.</li>
 *   <li>Subject attribute matching between policy and request subject.</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and
 * safe for concurrent use.</p>
 */
public class DefaultPolicyMatcher implements PolicyMatcher {

    @Override
    public List<Policy> match(RequestContext context, List<Policy> policies) {
        return policies.stream().filter(policy -> matches(policy, context)).collect(Collectors.toList());
    }

    /**
     * <p>Determine whether a single policy matches the given request context.</p>
     *
     * @param policy the policy to evaluate
     * @param context the request context
     * @return {@code true} if the policy matches the context, {@code false} otherwise
     */
    private boolean matches(Policy policy, RequestContext context) {

        // ADR-0009: tenant scope first — global or exact tenant match
        if (!policy.effectiveTenantId().equals(context.getTenantId()) && !"*".equals(policy.effectiveTenantId())) {
            return false;
        }

        List<Resource> resources = context.getResources();

        // Match resource type
        if (policy.getResource() != null) {
            boolean match = resources != null
                    && resources.stream().map(r -> r.resolve("type")).anyMatch(type -> policy.getResource()
                            .equals(type));

            if (!match) return false;
        }

        // Match action (null request action never satisfies a constrained policy)
        if (policy.getAction() != null) {
            if (context.getAction() == null
                    || context.getAction().getName() == null
                    || !policy.getAction().equalsIgnoreCase(context.getAction().getName())) {
                return false;
            }
        }

        // Match subject
        return matchSubject(policy.getSubject(), context.getSubject());
    }

    /**
     * <p>Evaluate whether the subject attributes in the policy
     * match those in the request subject.</p>
     *
     * @param policySubject the subject attributes defined in the policy
     * @param subject the request subject
     * @return {@code true} if all attributes match, {@code false} otherwise
     */
    private boolean matchSubject(Map<String, Object> policySubject, Subject subject) {
        if (policySubject == null) return true;
        // a policy with subject constraints never matches a request without a subject (fail closed)
        if (subject == null) return false;

        for (Map.Entry<String, Object> entry : policySubject.entrySet()) {
            Object value = getSubjectField(subject, entry.getKey());

            if (value == null || !value.equals(entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>Retrieve the value of a subject field by key.</p>
     *
     * @param subject the subject object
     * @param key the attribute key (e.g., "id", "type", or custom attribute name)
     * @return the value of the subject field, or {@code null} if not found
     */
    private Object getSubjectField(Subject subject, String key) {
        return switch (key) {
            case "id" -> subject.getId();
            case "type" -> subject.getType() == null ? null : subject.getType().name();
            default -> subject.getAttributes() != null ? subject.getAttributes().get(key) : null;
        };
    }
}
