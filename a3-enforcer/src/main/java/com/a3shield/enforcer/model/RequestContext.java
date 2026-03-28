package com.a3shield.enforcer.model;

import com.a3shield.enforcer.action.Action;
import lombok.*;

import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    private Subject subject;
    private Resource resource;
    private Action action;
    private Map<String, Object> context;
}