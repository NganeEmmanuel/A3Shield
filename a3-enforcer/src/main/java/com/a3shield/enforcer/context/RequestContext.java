package com.a3shield.enforcer.context;

import com.a3shield.enforcer.action.Action;
import com.a3shield.enforcer.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
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