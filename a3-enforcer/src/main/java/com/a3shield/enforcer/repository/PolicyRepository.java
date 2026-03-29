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

//    /**
//     * <p>Add a list of policies to the repository.</p>
//     * <p>Notes:</p>
//     * <ul>
//     *     <li>The input list of policies must not be {@code null}.</li>
//     *     <li>Implementations should handle duplicates according to their specific logic (e.g., overwrite, ignore, etc.).</li>
//     *     <li>Implementations should ensure that the policies are persisted according to the underlying storage mechanism.</li>
//     *     <li>Implementations should handle any exceptions that may occur during the addition process and provide appropriate feedback.</li>
//     *     <li>The method should return the list of policies that were successfully added to the repository.</li>
//     *     <li>Implementations should consider thread-safety when adding policies, especially if the repository is accessed concurrently.</li>
//     *     <li>Implementations may choose to return the added policies with any additional metadata (e.g., assigned IDs) if applicable.</li>
//     *     <li>Implementations should validate the input policies before adding them to ensure they meet any required criteria (e.g., non-null fields, valid formats, etc.).</li>
//     *     <li>Implementations should consider the performance implications of adding policies, especially if the repository is large or if the addition process involves complex operations (e.g., indexing, caching, etc.).</li>
//     *     <li>Implementations should document any specific behavior or constraints related to adding policies, such as maximum allowed policies, rate limits, etc.</li>
//     *     <li>Implementations should ensure that the addition of policies does not negatively impact the performance or availability of the repository for other operations (e.g., retrieval).</li>
//     *     <li>Implementations should consider providing feedback on the success or failure of the addition process, such as returning a boolean status or throwing exceptions for invalid input.</li>
//     *     <li>Implementations should consider the security implications of adding policies, such as ensuring that only authorized users can add policies and that the added policies do not introduce vulnerabilities.</li>
//     *     <li>Implementations should consider providing a mechanism for rolling back or undoing the addition of policies in case of errors or unintended consequences.</li>
//     *     <li>Implementations should consider providing a mechanism for validating the added policies against existing policies to prevent conflicts or inconsistencies.</li>
//     *     <li>Implementations should consider providing a mechanism for notifying other components (e.g., policy engine, cache) about the addition of new policies to ensure they are aware of the changes.</li>
//     *     <li>Implementations should consider providing a mechanism for logging the addition of policies for auditing and troubleshooting purposes.</li>
//     *     <li>Implementations should consider providing a mechanism for handling concurrent additions of policies to ensure data integrity and consistency.</li>
//     *     <li>Implementations should consider providing a mechanism for validating the input policies against a schema or set of rules to ensure they are well-formed and adhere to expected standards.</li>
//     *     <li>Implementations should consider providing a mechanism for handling conflicts that may arise when adding policies, such as duplicate entries or conflicting rules.</li>
//     *     <li>Implementations should consider providing a mechanism for handling the addition of policies in a transactional manner to ensure that either all policies are added successfully or none are added in case of errors.</li>
//     * @param policies
//     * @return
//     */
//    List<Policy> addPolicies(List<Policy> policies);
}
