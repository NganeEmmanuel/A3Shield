package com.a3shield.policymgmt.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-persistent {@link AppliedMigrationsStore}: applied-state lives for a
 * single run. Every startup re-applies all migrations (idempotent rebuild);
 * cross-restart conflict detection requires the JDBC store.
 */
public class InMemoryAppliedMigrationsStore implements AppliedMigrationsStore {

    private final Map<Integer, Recorded> applied = new ConcurrentHashMap<>();

    private record Recorded(String name, String checksum) {}

    @Override
    public boolean isApplied(int version) {
        return applied.containsKey(version);
    }

    @Override
    public Optional<String> checksumOf(int version) {
        return Optional.ofNullable(applied.get(version)).map(Recorded::checksum);
    }

    @Override
    public void record(int version, String name, String checksum) {
        applied.put(version, new Recorded(name, checksum));
    }

    @Override
    public void clear() {
        applied.clear();
    }
}
