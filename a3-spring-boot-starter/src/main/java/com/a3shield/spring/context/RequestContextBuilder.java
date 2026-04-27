package com.a3shield.spring.context;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.enforcer.model.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestContextBuilder {

    public RequestContext buildRequestContext(List<Resource> resource) {
        // get the resource from the joinPoint
        return RequestContext.builder()
                .subject(SubjectContext.getCurrentSubject())
                .resource(resource)
//                .action() // todo implement action extractor/determinant
//                .context() // todo implement contect aditionalContext builder (like ip, country, region, time, etc)
                .build();
    }
}
