package com.a3shield.enforcer.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    private String type;
    private String id;
    private Map<String, Object> attributes;
}
