package com.a3shield.spring.tenant;

import com.a3shield.core.tenant.Tenants;

/**
 * ThreadLocal holder for the current request's tenant. Populated once per
 * request by {@code A3RequestCheckFilter} and cleared at request end;
 * enforcement code reads it through {@link #getTenantId()} which never
 * returns null (ADR-0009, ADR-0006).
 */
public class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        return tenant == null ? Tenants.DEFAULT_TENANT : tenant;
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
