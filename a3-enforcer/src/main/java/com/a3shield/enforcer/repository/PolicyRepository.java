package com.a3shield.enforcer.repository;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.policy.Policy;

import java.util.List;

/**
 * <p><b>PolicyRepository</b> defines the contract for retrieving policies
 * from a storage mechanism (such as files, databases, or remote services).</p>
 *
 * <p>ADR-0009: retrieval is always tenant-scoped — implementations return
 * policies of the given tenant <i>plus</i> globally-scoped policies
 * ({@code tenantId == "*"}).</p>
 *
 * <p><b>Thread-safety:</b> Implementers should document whether their
 * repository implementation is safe for concurrent use.</p>
 */
public interface PolicyRepository {

    /**
     * <p>Retrieve candidate policies for a request: those scoped to
     * {@code tenantId} (or global) whose action and resource type are
     * compatible with the request. A null policy action or resource means
     * "matches anything" and must be returned.</p>
     *
     * @param tenantId     the tenant scope of the request (never null)
     * @param action       the requested action, or null when unknown
     * @param resourceType the requested resource type, or null when the request carries no resources
     * @return a non-null list of candidate {@link Policy} objects
     */
    List<Policy> findAll(String tenantId, Action action, String resourceType);
}
