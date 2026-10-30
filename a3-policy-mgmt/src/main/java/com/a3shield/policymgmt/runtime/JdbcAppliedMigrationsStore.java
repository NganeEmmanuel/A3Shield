package com.a3shield.policymgmt.runtime;

import com.a3shield.policymgmt.PolicyMigrationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * JDBC-backed {@link AppliedMigrationsStore} — makes conflict detection
 * work across restarts (table {@code a3_applied_migrations}). Uses plain
 * JDBC, no framework dependency (ADR-0007).
 */
public class JdbcAppliedMigrationsStore implements AppliedMigrationsStore {

    private final DataSource dataSource;

    public JdbcAppliedMigrationsStore(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfMissing();
    }

    private void createTableIfMissing() {
        String ddl = "CREATE TABLE IF NOT EXISTS a3_applied_migrations ("
                + "version INT NOT NULL, "
                + "name VARCHAR(255) NOT NULL, "
                + "checksum VARCHAR(64) NOT NULL, "
                + "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (version))";
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to create a3_applied_migrations table", e);
        }
    }

    @Override
    public boolean isApplied(int version) {
        return checksumOf(version).isPresent();
    }

    @Override
    public Optional<String> checksumOf(int version) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT checksum FROM a3_applied_migrations WHERE version = ?")) {
            statement.setInt(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to read applied migration V" + version, e);
        }
    }

    @Override
    public void record(int version, String name, String checksum) {
        // portable upsert: UPDATE first, INSERT when absent (MERGE/ON CONFLICT
        // syntax differs across databases)
        try (Connection connection = dataSource.getConnection()) {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE a3_applied_migrations SET name = ?, checksum = ?, applied_at = CURRENT_TIMESTAMP "
                            + "WHERE version = ?")) {
                statement.setString(1, name);
                statement.setString(2, checksum);
                statement.setInt(3, version);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO a3_applied_migrations (version, name, checksum) VALUES (?, ?, ?)")) {
                    statement.setInt(1, version);
                    statement.setString(2, name);
                    statement.setString(3, checksum);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to record applied migration V" + version, e);
        }
    }

    @Override
    public void clear() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM a3_applied_migrations");
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to clear applied migrations", e);
        }
    }
}
