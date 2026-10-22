package com.a3shield.core.model;

import java.util.Map;
import java.util.Set;
import lombok.*;

/**
 * <p><b>Subject</b> is the authenticated principal the evaluation is about.
 * Identity is established by the host application (ADR-0001); the subject is
 * only ever consumed here.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    // we can use an abstract class here and have UserSubject and ServiceSubject if we want to add more specific fields
    // later
    // also for some complicated systems, roles are not just strings but objects with more attributes. So we can use
    // generics to allow
    // each subjectType to define the role.
    // we can also add policies as subjects can have option for direct policy binding
    // for attributes, rather than having a map we can have an abstract method getAttribute(String key) and let each
    // subject type implement how they store attributes. This allows more flexibility in how attributes are stored and
    // accessed.

    private String id;
    private SubjectType type; // USER, SERVICE
    private Set<String> roles;
    private Set<String> permissions;
    private Map<String, Object> attributes;
}
