package com.a3shield.enforcer.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    // this can also be an abstract class
    // for attributes, rather than using maps we can use an abstract method allowing each resource to implement the way they store attributes

    private String type;
    private String id;
    private Map<String, Object> attributes;
}
