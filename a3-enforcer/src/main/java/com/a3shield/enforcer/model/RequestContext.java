package com.a3shield.enforcer.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    private Subject subject;
    private Resource resource;
    private Action action;
    private Map<String, Object> context;
}