package com.a3shield.core.tenant;

/**
 * Tenant-scope constants (ADR-0009). Every evaluation is tenant-scoped;
 * deployments that never configure tenancy live in {@link #DEFAULT_TENANT}.
 */
public final class Tenants {

    /** The implicit tenant used when none is configured. */
    public static final String DEFAULT_TENANT = "default";

    /** Wildcard scope: a policy that applies across all tenants. */
    public static final String GLOBAL = "*";

    private Tenants() {}
}
