package com.a3shield.enforcer.permission;

import java.util.Set;

public interface PermissionExtractor {
    boolean supports(String subjectType);
    /**
     * Extract policies attached the subject(user or service) from the database (try cache first) to be able to build out the subject context
     * @return set of permissions attached
     */
    Set<String> extractPermissions();
}
