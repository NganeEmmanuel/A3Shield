package com.a3shield.spring.audit;

import com.a3shield.core.audit.EnforcementAuditEvent;
import com.a3shield.core.decision.DecisionResult;
import com.a3shield.core.model.Resource;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publishes {@link EnforcementAuditEvent}s through Spring's event bus when a
 * publisher is available. Host applications subscribe
 * ({@code @EventListener}) to audit-log, metric or alert on authorization
 * decisions; nothing is published in plain unit contexts without a
 * publisher.
 */
public class A3EnforcementAuditor {

    private final ObjectProvider<ApplicationEventPublisher> publisher;

    public A3EnforcementAuditor(ObjectProvider<ApplicationEventPublisher> publisher) {
        this.publisher = publisher;
    }

    /** Convenience for direct wiring/tests: publish to the given publisher. */
    @SuppressWarnings("deprecation")
    public A3EnforcementAuditor(ApplicationEventPublisher directPublisher) {
        this.publisher = new ObjectProvider<>() {
            @Override
            public ApplicationEventPublisher getObject() {
                return directPublisher;
            }

            @Override
            public ApplicationEventPublisher getIfAvailable() {
                return directPublisher;
            }

            @Override
            public ApplicationEventPublisher getIfUnique() {
                return directPublisher;
            }
        };
    }

    public void publish(
            String point,
            String tenantId,
            String subjectId,
            String action,
            List<Resource> resources,
            DecisionResult result) {
        ApplicationEventPublisher available = publisher.getIfAvailable();
        if (available == null) {
            return;
        }
        available.publishEvent(new EnforcementAuditEvent(
                Instant.now(),
                point,
                tenantId,
                subjectId,
                action,
                resources == null
                        ? List.of()
                        : resources.stream().map(Resource::urn).toList(),
                result));
    }
}
