package com.a3shield.spring.context;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.core.model.Resource;
import com.a3shield.spring.tenant.TenantContextHolder;

import java.util.List;

public class RequestContextBuilder {

    public RequestContext buildRequestContext(List<Resource> resource) {
        // get the resource from the joinPoint
        return RequestContext.builder()
                .subject(SubjectContext.getCurrentSubject())
                .resources(resource)
                .tenantId(TenantContextHolder.getTenantId())
//                .action() // todo implement action extractor/determinant
//                .context() // todo implement contect aditionalContext builder (like ip, country, region, time, etc)
                .build();
    }
}
