package com.a3shield.policymgmt.store;

import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.parse.PolicySerde;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * JDBC-backed {@link EffectivePolicyStore}: policy bodies persist as YAML,
 * so a restart (or another instance) sees runtime admin edits (ADR-0003,
 * ADR-0011). Plain JDBC, portable upserts.
 */
public class JdbcEffectivePolicyStore implements EffectivePolicyStore {

    private final DataSource dataSource;
    private final PolicySerde serde = new PolicySerde();

    public JdbcEffectivePolicyStore(DataSource dataSource) {
        this.dataSource = dataSource;
        createTablesIfMissing();
    }

    private void createTablesIfMissing() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS a3_effective_policies ("
                    + "tenant_id VARCHAR(255) NOT NULL, "
                    + "policy_id VARCHAR(255) NOT NULL, "
                    + "body_yaml LONGVARCHAR NOT NULL, "
                    + "origin VARCHAR(16) NOT NULL, "
                    + "migration_version VARCHAR(32), "
                    + "tests_yaml LONGVARCHAR, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY (tenant_id, policy_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS a3_policy_state ("
                    + "id INT NOT NULL PRIMARY KEY, version BIGINT NOT NULL)");
            try (ResultSet existing = statement.executeQuery("SELECT version FROM a3_policy_state WHERE id = 1")) {
                if (!existing.next()) {
                    statement.execute("INSERT INTO a3_policy_state (id, version) VALUES (1, 0)");
                }
            }
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to create effective-policy tables", e);
        }
    }

    @Override
    public Optional<EffectivePolicyRecord> find(String tenantId, String policyId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT body_yaml, origin, migration_version, tests_yaml FROM a3_effective_policies "
                                + "WHERE tenant_id = ? AND policy_id = ?")) {
            statement.setString(1, tenantId);
            statement.setString(2, policyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet, tenantId, policyId)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to load policy " + tenantId + "/" + policyId, e);
        }
    }

    @Override
    public List<EffectivePolicyRecord> findByTenant(String tenantId) {
        List<EffectivePolicyRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT policy_id, body_yaml, origin, migration_version, tests_yaml "
                                + "FROM a3_effective_policies WHERE tenant_id = ?")) {
            statement.setString(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(map(resultSet, tenantId, resultSet.getString("policy_id")));
                }
            }
            return records;
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to list policies of tenant " + tenantId, e);
        }
    }

    @Override
    public void upsert(EffectivePolicyRecord record) {
        try (Connection connection = dataSource.getConnection()) {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE a3_effective_policies SET body_yaml = ?, origin = ?, migration_version = ?, "
                            + "tests_yaml = ?, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND policy_id = ?")) {
                bind(statement, record);
                statement.setString(5, record.getTenantId());
                statement.setString(6, record.getPolicy().getId());
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO a3_effective_policies "
                        + "(tenant_id, policy_id, body_yaml, origin, migration_version, tests_yaml) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    statement.setString(1, record.getTenantId());
                    statement.setString(2, record.getPolicy().getId());
                    statement.setString(3, serde.toYaml(record.getPolicy()));
                    statement.setString(4, record.getOrigin().name());
                    statement.setString(5, record.getMigrationVersion());
                    statement.setString(
                            6,
                            record.getTests() == null || record.getTests().isEmpty()
                                    ? null
                                    : serde.testsToYaml(record.getTests()));
                    statement.executeUpdate();
                }
            }
            bumpState(connection);
        } catch (SQLException e) {
            throw new PolicyMigrationException(
                    "Failed to persist policy " + record.getTenantId() + "/"
                            + record.getPolicy().getId(),
                    e);
        }
    }

    private void bind(PreparedStatement statement, EffectivePolicyRecord record) throws SQLException {
        statement.setString(1, serde.toYaml(record.getPolicy()));
        statement.setString(2, record.getOrigin().name());
        statement.setString(3, record.getMigrationVersion());
        statement.setString(
                4,
                record.getTests() == null || record.getTests().isEmpty() ? null : serde.testsToYaml(record.getTests()));
    }

    @Override
    public boolean delete(String tenantId, String policyId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM a3_effective_policies WHERE tenant_id = ? AND policy_id = ?")) {
            statement.setString(1, tenantId);
            statement.setString(2, policyId);
            boolean deleted = statement.executeUpdate() > 0;
            if (deleted) {
                try (Statement bump = connection.createStatement()) {
                    bump.execute("UPDATE a3_policy_state SET version = version + 1 WHERE id = 1");
                }
            }
            return deleted;
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to delete policy " + tenantId + "/" + policyId, e);
        }
    }

    @Override
    public java.util.Set<String> tenants() {
        java.util.Set<String> tenants = new java.util.HashSet<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT DISTINCT tenant_id FROM a3_effective_policies")) {
            while (resultSet.next()) {
                tenants.add(resultSet.getString(1));
            }
            return tenants;
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to list policy tenants", e);
        }
    }

    @Override
    public long stateVersion() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT version FROM a3_policy_state WHERE id = 1")) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to read policy state version", e);
        }
    }

    private void bumpState(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("UPDATE a3_policy_state SET version = version + 1 WHERE id = 1");
        }
    }

    private EffectivePolicyRecord map(ResultSet resultSet, String tenantId, String policyId) throws SQLException {
        return EffectivePolicyRecord.builder()
                .tenantId(tenantId)
                .policy(serde.fromYaml(resultSet.getString("body_yaml")))
                .bodyYaml(resultSet.getString("body_yaml"))
                .origin(Origin.valueOf(resultSet.getString("origin")))
                .migrationVersion(resultSet.getString("migration_version"))
                .tests(serde.testsFromYaml(resultSet.getString("tests_yaml")))
                .build();
    }
}
