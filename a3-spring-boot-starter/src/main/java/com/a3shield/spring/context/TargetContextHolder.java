package com.a3shield.spring.context;

import com.a3shield.enforcer.model.Target;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;

public class TargetContextHolder {
    private static final ThreadLocal<List<Target>> TARGET_CONTEXT = new ThreadLocal<>();
    private static TargetContextBuilder contextBuilder;

    public TargetContextHolder(TargetContextBuilder contextBuilder){
        TargetContextHolder.contextBuilder = contextBuilder;
    }
    public static List<Target> getTargetContext(Object object){
        if(TARGET_CONTEXT.get() == null){
            setTargetContext(contextBuilder.build(object));
        }

        return TARGET_CONTEXT.get();
    }

    public static void setTargetContext(List<Target> target){
        TARGET_CONTEXT.set(target);
    }

    public static void clearTargetContext(){
        TARGET_CONTEXT.remove();
    }
}
