package com.a3shield.policymgmt.parse;

import com.a3shield.policymgmt.migration.PolicyMigration;

/**
 * Parses one policy migration file into a {@link PolicyMigration},
 * including its SHA-256 checksum.
 */
public interface PolicyMigrationParser {

    /**
     * @param sourceName origin description for error messages (e.g. file name)
     * @param version    migration version parsed from the file name
     * @param name       migration name parsed from the file name
     * @param content    raw file content; the checksum is computed from these exact bytes
     */
    PolicyMigration parse(String sourceName, int version, String name, String content);
}
