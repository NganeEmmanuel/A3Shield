package com.a3shield.policymgmt.migration;

import com.a3shield.enforcer.policy.Policy;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * A parsed policy migration file: versioned policies plus embedded test
 * cases, applied atomically at startup (ADR-0003, ADR-0010).
 *
 * @param source a human-readable origin (file name / URL) for error messages
 */
@Getter
@Builder
public class PolicyMigration {

    private final int version;
    private final String name;
    private final String tenantId;
    private final OnConflict onConflict;
    private final List<Policy> policies;
    private final List<PolicyTestCase> tests;
    private final String checksum;
    private final String source;

    public String description() {
        return "V" + version + "__" + name + " (" + source + ")";
    }
}
