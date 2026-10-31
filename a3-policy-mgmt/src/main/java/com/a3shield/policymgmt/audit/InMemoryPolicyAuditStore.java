package com.a3shield.policymgmt.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link PolicyAuditStore} with bounded retention (default 1000
 * entries) for single-instance deployments.
 */
public class InMemoryPolicyAuditStore implements PolicyAuditStore {

    private final ConcurrentLinkedDeque<PolicyAuditRecord> records = new ConcurrentLinkedDeque<>();
    private final AtomicLong ids = new AtomicLong();
    private final int capacity;

    public InMemoryPolicyAuditStore() {
        this(1000);
    }

    public InMemoryPolicyAuditStore(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void append(PolicyAuditRecord record) {
        records.addFirst(new PolicyAuditRecord(
                ids.incrementAndGet(),
                record.actor(),
                record.at(),
                record.tenantId(),
                record.policyId(),
                record.operation(),
                record.beforeYaml(),
                record.afterYaml()));
        while (records.size() > capacity) {
            records.removeLast();
        }
    }

    @Override
    public List<PolicyAuditRecord> findByTenant(String tenantId, int limit) {
        List<PolicyAuditRecord> result = new ArrayList<>();
        for (PolicyAuditRecord record : records) {
            if (record.tenantId().equals(tenantId)) {
                result.add(record);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }
}
