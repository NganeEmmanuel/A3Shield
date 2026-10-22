package com.a3shield.spring.tenant;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>Resolves the tenant a request belongs to (ADR-0009). Host applications
 * provide a bean; typical strategies read a header ({@code X-Tenant-Id}),
 * a subdomain, or a JWT claim. The resolved value is stored for the request
 * and used to scope every evaluation.</p>
 *
 * <p>With no custom bean, everything lands in the {@code default} tenant —
 * single-tenant deployments never see the concept.</p>
 */
@FunctionalInterface
public interface TenantResolver {

    /**
     * @param request the incoming servlet request
     * @return the tenant id; never null (return the default tenant when unknown)
     */
    String resolveTenant(HttpServletRequest request);
}
