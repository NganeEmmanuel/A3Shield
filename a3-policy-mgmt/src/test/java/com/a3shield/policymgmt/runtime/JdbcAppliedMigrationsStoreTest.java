package com.a3shield.policymgmt.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class JdbcAppliedMigrationsStoreTest {

    private static final String URL = "jdbc:h2:mem:a3migrations" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

    private static JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(URL);
        return dataSource;
    }

    @Test
    void shouldRecordAndLookupAcrossInstances() {
        // first "startup" records the applied migration
        new JdbcAppliedMigrationsStore(dataSource()).record(1, "baseline", "abc123");

        // second "startup" on the same database (restart semantics) sees it
        JdbcAppliedMigrationsStore restarted = new JdbcAppliedMigrationsStore(dataSource());

        assertThat(restarted.isApplied(1)).isTrue();
        assertThat(restarted.checksumOf(1)).contains("abc123");
        assertThat(restarted.isApplied(2)).isFalse();
    }

    @Test
    void recordShouldUpdateChecksumOnReApply() {
        JdbcAppliedMigrationsStore store = new JdbcAppliedMigrationsStore(dataSource());
        store.record(1, "baseline", "abc123");
        store.record(1, "baseline", "def456");

        assertThat(store.checksumOf(1)).contains("def456");
    }

    @Test
    void clearShouldRemoveAllRecords() {
        JdbcAppliedMigrationsStore store = new JdbcAppliedMigrationsStore(dataSource());
        store.record(1, "baseline", "abc123");
        store.clear();

        assertThat(store.isApplied(1)).isFalse();
    }
}
