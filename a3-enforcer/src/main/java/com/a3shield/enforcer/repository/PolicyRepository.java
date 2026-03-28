package com.a3shield.enforcer.repository;

import com.a3shield.enforcer.policy.Policy;

import java.util.List;

/**
 * <p><b>PolicyRepository</b> defines the contract for retrieving policies
 * from a storage mechanism (such as files, databases, or remote services).</p>
 *
 * <p>Implementations of this interface are responsible for providing access
 * to all available policies that can be evaluated by a {@code PolicyEngine}.</p>
 *
 * <p>Typical responsibilities may include:</p>
 * <ul>
 *   <li>Loading policies from persistent storage.</li>
 *   <li>Refreshing or caching policies for performance optimization.</li>
 *   <li>Handling errors gracefully when the repository is unavailable.</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> Implementers should document whether their
 * repository implementation is safe for concurrent use.</p>
 */
public interface PolicyRepository {

    /**
     * <p>Retrieve all policies available in the repository.</p>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>The returned list should contain all policies currently stored.</li>
     *   <li>Implementations may return an empty list if no policies are present.</li>
     *   <li>The list itself must not be {@code null}.</li>
     * </ul>
     *
     * @return a non-null list of {@link Policy} objects representing all
     *         policies available in the repository.
     */
    List<Policy> findAll();
}
