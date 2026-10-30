package com.a3shield.policymgmt.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.a3shield.policymgmt.PolicyMigrationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PolicyMigrationDiscovererTest {

    @TempDir
    Path tempDir;

    private final PolicyMigrationDiscoverer discoverer = new PolicyMigrationDiscoverer();

    @Test
    void shouldDiscoverAndOrderByVersion() throws IOException {
        write("V2__documents.yaml", "policies: []\n");
        write("V10__late.yml", "policies: []\n");
        write("V1__baseline.yaml", "policies: []\n");
        write("readme.txt", "not a migration");

        var migrations = discoverer.discoverFromDirectory(tempDir);

        assertThat(migrations).extracting(m -> m.getVersion()).containsExactly(1, 2, 10);
        assertThat(migrations.get(1).getName()).isEqualTo("documents");
        assertThat(migrations.get(2).getSource()).isEqualTo("V10__late.yml");
    }

    @Test
    void duplicateVersionsMustBeFatal() throws IOException {
        write("V1__one.yaml", "policies: []\n");
        write("V1__two.yaml", "policies: []\n");

        assertThatThrownBy(() -> discoverer.discoverFromDirectory(tempDir))
                .isInstanceOf(PolicyMigrationException.class)
                .hasMessageContaining("Duplicate policy migration version V1");
    }

    @Test
    void emptyDirectoryYieldsNoMigrations() {
        assertThat(discoverer.discoverFromDirectory(tempDir)).isEmpty();
        assertThat(discoverer.discoverFromClasspath(PolicyMigrationDiscovererTest.class.getClassLoader()))
                .isEmpty(); // module test resources carry no a3/policies
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(tempDir.resolve(name), content);
    }
}
