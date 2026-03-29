package com.a3shield.enforcer.repository;

import com.a3shield.enforcer.policy.Policy;

import java.util.List;

public class InMemoryPolicyRepository implements PolicyRepository {

    private final List<Policy> policies;

    public InMemoryPolicyRepository(List<Policy> policies) {
        this.policies = policies;
    }

    @Override
    public List<Policy> findAll() {
        return policies;
    }
}