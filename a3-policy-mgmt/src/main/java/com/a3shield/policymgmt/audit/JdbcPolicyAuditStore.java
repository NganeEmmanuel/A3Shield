package com.a3shield.policymgmt.audit;

import com.a3shield.policymgmt.PolicyMigrationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * JDBC-backed {@link PolicyAuditStore}: durable, append-only audit trail
 * (table {@code a3_policy_audit}).
 */
public class JdbcPolicyAuditStore implements PolicyAuditStore {

    private final DataSource dataSource;

    public JdbcPolicyAuditStore(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfMissing();
    }

    private void createTableIfMissing() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS a3_policy_audit ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "actor VARCHAR(255) NOT NULL, "
                    + "at TIMESTAMP NOT NULL, "
                    + "tenant_id VARCHAR(255) NOT NULL, "
                    + "policy_id VARCHAR(255) NOT NULL, "
                    + "operation VARCHAR(16) NOT NULL, "
                    + "before_yaml LONGVARCHAR, "
                    + "after_yaml LONGVARCHAR)");
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to create a3_policy_audit table", e);
        }
    }

    @Override
    public void append(PolicyAuditRecord record) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO a3_policy_audit "
                        + "(actor, at, tenant_id, policy_id, operation, before_yaml, after_yaml) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, record.actor());
            statement.setTimestamp(2, Timestamp.from(record.at()));
            statement.setString(3, record.tenantId());
            statement.setString(4, record.policyId());
            statement.setString(5, record.operation().name());
            statement.setString(6, record.beforeYaml());
            statement.setString(7, record.afterYaml());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to append policy audit record", e);
        }
    }

    @Override
    public List<PolicyAuditRecord> findByTenant(String tenantId, int limit) {
        List<PolicyAuditRecord> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, actor, at, policy_id, operation, before_yaml, after_yaml "
                                + "FROM a3_policy_audit WHERE tenant_id = ? ORDER BY id DESC LIMIT ?")) {
            statement.setString(1, tenantId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new PolicyAuditRecord(
                            resultSet.getLong("id"),
                            resultSet.getString("actor"),
                            resultSet.getTimestamp("at").toInstant(),
                            tenantId,
                            resultSet.getString("policy_id"),
                            PolicyAuditRecord.Operation.valueOf(resultSet.getString("operation")),
                            resultSet.getString("before_yaml"),
                            resultSet.getString("after_yaml")));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new PolicyMigrationException("Failed to read policy audit for tenant " + tenantId, e);
        }
    }
}
