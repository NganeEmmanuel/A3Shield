package com.a3shield.spring.context;

import com.a3shield.enforcer.context.RequestContext;
import com.a3shield.core.model.Resource;

import java.util.List;

public class RequestContextHolder {
    private static final ThreadLocal<RequestContext> requestContextHolder = new ThreadLocal<>();
    private static  final RequestContextBuilder requestContextBuilder = new RequestContextBuilder();

    public static RequestContext getRequestContext(List<Resource> resource) {
        if (requestContextHolder.get() == null) {
            setRequestContext(requestContextBuilder.buildRequestContext(resource));
        }else{
            requestContextHolder.get().setResources(resource);
        }

        return requestContextHolder.get();
    }

    public static RequestContext getRequestContext() {
        return requestContextHolder.get();
    }

    public static void setRequestContext(RequestContext requestContext) {
        requestContextHolder.set(requestContext);
    }
    public static void clearRequestContext() {
        requestContextHolder.remove();
    }
}
