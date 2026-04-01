package com.a3shield.enforcer.cache;

import com.a3shield.enforcer.expression.Expression;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe cache for parsed expressions.
 * <p>
 * Key: raw expression string
 * Value: parsed Expression object
 *
 */
public class ExpressionCache {
    // todo upgrade to:
    //
    //LRU cache (limit memory)
    //TTL expiration
    //Precompiled policies
    //Distributed cache (Redis)

    private final ConcurrentMap<String, Expression> cache = new ConcurrentHashMap<>();

    public Expression get(String expression) {
        return cache.get(expression);
    }

    public void put(String expression, Expression parsed) {
        cache.put(expression, parsed);
    }

    public boolean contains(String expression) {
        return cache.containsKey(expression);
    }
}