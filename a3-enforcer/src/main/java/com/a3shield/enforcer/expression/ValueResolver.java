package com.a3shield.enforcer.expression;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.*;

import java.util.Map;

/**
 * <p><b>ValueResolver</b> resolves values from a {@link RequestContext}
 * using dot notation paths.</p>
 *
 * <p>Supported path prefixes:</p>
 * <ul>
 *   <li><b>subject.</b> – resolves fields from the {@link Subject} (e.g., {@code subject.id}).</li>
 *   <li><b>resource.</b> – resolves fields from the {@link Resource} (e.g., {@code resource.type}).</li>
 *   <li><b>context.</b> – resolves fields from the contextual attributes map (e.g., {@code context.time}).</li>
 *   <li>Literals – values without a prefix are treated as string literals (quotes are stripped).</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ValueResolver resolver = new ValueResolver();
 * Object value = resolver.resolve("subject.id", requestContext);
 * }</pre>
 *
 * <p><b>Thread-safety:</b> This implementation is stateless and safe
 * for concurrent use.</p>
 */
public class ValueResolver {

    /**
     * <p>Resolve a value from the given {@link RequestContext} based on the provided path.</p>
     *
     * @param path the dot-notation path (e.g., {@code subject.id}, {@code resource.type}, {@code context.time})
     * @param context the request context containing subject, resource, and contextual attributes
     * @return the resolved value, or a literal string if no prefix matches
     */
    public Object resolve(String path, RequestContext context) {

        if (path.startsWith("subject.")) {
            return resolveSubject(path.substring(8), context.getSubject());
        }

        if (path.startsWith("resource.")) {
            return resolveResource(path.substring(9), context.getResource());
        }

        if (path.startsWith("context.")) {
            return resolveContext(path.substring(8), context.getContext());
        }

        // literal (e.g., "USER")
        return stripQuotes(path);
    }

    /**
     * <p>Resolve a field from the {@link Subject} object.</p>
     *
     * @param field the subject field name (e.g., {@code id}, {@code type}, or custom attribute key)
     * @param subject the subject object
     * @return the resolved value, or {@code null} if not found
     */
    private Object resolveSubject(String field, Subject subject) {
        if (subject == null) return null;

        return switch (field) {
            case "id" -> subject.getId();
            case "type" -> subject.getType().name();
            default -> subject.getAttributes() != null
                    ? subject.getAttributes().get(field)
                    : null;
        };
    }

    /**
     * <p>Resolve a field from the {@link Resource} object.</p>
     *
     * @param field the resource field name (e.g., {@code id}, {@code type}, or custom attribute key)
     * @param resource the resource object
     * @return the resolved value, or {@code null} if not found
     */
    private Object resolveResource(String field, Resource resource) {
        if (resource == null) return null;

        if ("id".equals(field)) return resource.getId();
        if ("type".equals(field)) return resource.getType();

        return resource.getAttributes() != null
                ? resource.getAttributes().get(field)
                : null;
    }

    /**
     * <p>Resolve a field from the contextual attributes map.</p>
     *
     * @param field the context attribute key
     * @param ctx the context map
     * @return the resolved value, or {@code null} if not found
     */
    private Object resolveContext(String field, Map<String, Object> ctx) {
        return ctx != null ? ctx.get(field) : null;
    }

    /**
     * <p>Strip quotes from a literal string value.</p>
     *
     * @param value the string value
     * @return the unquoted and trimmed string
     */
    private String stripQuotes(String value) {
        return value.replace("\"", "").trim();
    }
}
