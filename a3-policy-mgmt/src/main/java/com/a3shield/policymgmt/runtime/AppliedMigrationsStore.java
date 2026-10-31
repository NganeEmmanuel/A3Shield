package com.a3shield.policymgmt.runtime;

import java.util.Optional;

/**
 * Records which policy migrations have been applied, with their checksums —
 * the "applied-versions table" of ADR-0003. Persistent implementations
 * (JDBC) make conflict detection work across restarts; in-memory ones only
 * cover a single run.
 */
public interface AppliedMigrationsStore {

    boolean isApplied(int version);

    /** Checksum recorded when the version was applied; empty if not applied. */
    Optional<String> checksumOf(int version);

    void record(int version, String name, String checksum);

    void clear();
}
