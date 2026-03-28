package com.a3shield.enforcer.context;

import com.a3shield.enforcer.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestContext {

    private Subject subject;
    private Resource resource;
    private Action action;
    private Map<String, Object> context; // time, location, ip, metadata, etc.

    // builder pattern recommended
}