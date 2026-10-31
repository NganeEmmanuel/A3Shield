package com.a3shield.policymgmt.store;

import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.migration.PolicyTestCase;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * A policy in the effective store, with its provenance: the origin
 * (file migration vs runtime admin edit), the migration version that last
 * wrote it, its YAML body and its attached validation tests (ADR-0003).
 */
@Getter
@Builder
public class EffectivePolicyRecord {

    private final Policy policy;
    private final String tenantId;
    private final String bodyYaml;
    private final Origin origin;
    private final String migrationVersion;
    private final List<PolicyTestCase> tests;
}
