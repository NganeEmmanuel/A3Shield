package com.a3shield.spring.context;

import com.a3shield.enforcer.model.Intent;
import org.aspectj.lang.JoinPoint;

public class IntentContentHolder {
    private static final ThreadLocal<Intent>  CURRENT_INTENT = new ThreadLocal<>();
    private static final IntentContextBuilder contextBuilder = new IntentContextBuilder(); // todo not sure how to handle autowires

    public static Intent getCurrentIntent(Object object) {
        if (CURRENT_INTENT.get() == null) {
            setCurrentIntent(contextBuilder.build(object));
        }
        return CURRENT_INTENT.get();
    }

    public static void setCurrentIntent(Intent intent) {
        CURRENT_INTENT.set(intent);
    }

    public static void clearCurrentIntent() {
        CURRENT_INTENT.remove();
    }

}
