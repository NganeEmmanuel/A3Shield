package com.a3shield.enforcer.relation;

import java.util.Set;

/**
 * <p><b>RelationResolver</b> answers relationship questions for ReBAC-style
 * checks: "is subject S in relation R to resource X?" (ADR-0008).</p>
 *
 * <p><b>Status: preview contract</b> — reserved in M2 so a later ReBAC
 * implementation (graph store or a delegate such as OpenFGA/SpiceDB) is
 * additive, not breaking. Resources are addressed by the {@code type:id}
 * URN convention on {@code Resource#urn()}.</p>
 *
 * <p>The engine does not call this SPI yet; when it does (M7), it will be
 * exposed to policy conditions as a CEL function such as
 * {@code related(subject, 'owner', resource)}.</p>
 */
public interface RelationResolver {

    /**
     * @param subjectId   the subject's id
     * @param relation    the relation name (e.g. {@code "owner"}, {@code "editor"})
     * @param resourceUrn the resource identity in {@code type:id} form
     * @return true when the subject holds the relation on the resource
     */
    boolean isRelated(String subjectId, String relation, String resourceUrn);

    /**
     * @param subjectId    the subject's id
     * @param relation     the relation name
     * @param resourceType the resource type prefix (e.g. {@code "folder"})
     * @return all resource URNs of the given type the subject holds the relation on
     */
    Set<String> relatedResources(String subjectId, String relation, String resourceType);
}
