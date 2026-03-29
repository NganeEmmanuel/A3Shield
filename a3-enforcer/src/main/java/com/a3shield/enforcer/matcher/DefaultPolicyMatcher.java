package com.a3shield.enforcer.matcher;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.Subject;
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
 *   <li>Resource type matching between policy and request.</li>
 *   <li>Action name matching between policy and request.</li>
 *   <li>Subject attribute matching between policy and request subject.</li>
 * </ul>
 *
 * <p>Policies that satisfy all applicable conditions are returned
 * as a list of matching policies.</p>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and
 * safe for concurrent use.</p>
 */
public class DefaultPolicyMatcher implements PolicyMatcher {

    /**
     * <p>Filter the provided list of policies to those that match
     * the given request context.</p>
     *
     * @param context the request context containing subject, resource,
     *                and action attributes; must not be null
     * @param policies the list of candidate policies to evaluate; must not be null
     * @return a non-null list of {@link Policy} objects that match the request context
     */
    @Override
    public List<Policy> match(RequestContext context, List<Policy> policies) {
        return policies.stream()
                .filter(policy -> matches(policy, context))
                .collect(Collectors.toList());
    }

    /**
     * <p>Determine whether a single policy matches the given request context.</p>
     *
     * @param policy the policy to evaluate
     * @param context the request context
     * @return {@code true} if the policy matches the context, {@code false} otherwise
     */
    private boolean matches(Policy policy, RequestContext context) {

        // Match resource
        if (policy.getResource() != null &&
                !policy.getResource().equals(context.getResource().getType())) {
            return false;
        }

        // Match action
        if (policy.getAction() != null &&
                !policy.getAction().equalsIgnoreCase(context.getAction().getName())) {
            return false;
        }

        // Match subject attributes
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
            case "type" -> subject.getType().name();
            default -> subject.getAttributes() != null
                    ? subject.getAttributes().get(key)
                    : null;
        };
    }
}
