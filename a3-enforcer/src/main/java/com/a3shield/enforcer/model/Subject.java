package com.a3shield.enforcer.model;

import lombok.*;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {

    private String id;
    private SubjectType type; // USER, SERVICE
    private Set<String> roles;
    private Map<String, Object> attributes;
}