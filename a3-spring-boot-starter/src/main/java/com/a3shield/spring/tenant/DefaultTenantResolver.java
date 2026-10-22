package com.a3shield.spring.tenant;

import com.a3shield.core.tenant.Tenants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Fallback resolver: every request belongs to the {@link Tenants#DEFAULT_TENANT}.
 * Active only when the host application defines no {@link TenantResolver} bean.
 */
public class DefaultTenantResolver implements TenantResolver {

    @Override
    public String resolveTenant(HttpServletRequest request) {
        return Tenants.DEFAULT_TENANT;
    }
}
