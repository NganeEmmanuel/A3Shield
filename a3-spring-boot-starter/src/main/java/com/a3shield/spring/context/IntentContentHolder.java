package com.a3shield.spring.context;

import com.a3shield.core.model.Resource;

public class IntentContentHolder {
    private static final ThreadLocal<Resource>  CURRENT_INTENT = new ThreadLocal<>();
    private static final IntentContextBuilder contextBuilder = new IntentContextBuilder(); // todo not sure how to handle autowires

    public static Resource getCurrentIntent(Object object) {
        if (CURRENT_INTENT.get() == null) {
            setCurrentIntent(contextBuilder.build(object));
        }
        return CURRENT_INTENT.get();
    }

    public static void setCurrentIntent(Resource intent) {
        CURRENT_INTENT.set(intent);
    }

    public static void clearCurrentIntent() {
        CURRENT_INTENT.remove();
    }

}
