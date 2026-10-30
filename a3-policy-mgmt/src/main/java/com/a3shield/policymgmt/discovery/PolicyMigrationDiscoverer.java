package com.a3shield.policymgmt.discovery;

import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.parse.PolicyMigrationParser;
import com.a3shield.policymgmt.parse.YamlPolicyMigrationParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers policy migration files named {@code V<n>__<name>.yaml|.yml} on
 * the classpath (directory {@code a3/policies/}) or in an explicit directory
 * (used by tests and filesystem-based deployments), parses them and returns
 * them in version order. Duplicate versions are fatal.
 */
public class PolicyMigrationDiscoverer {

    public static final String DEFAULT_LOCATION = "a3/policies";
    static final Pattern FILE_NAME = Pattern.compile("V(\\d+)__(.+)\\.(ya?ml)$");

    private final PolicyMigrationParser parser;

    public PolicyMigrationDiscoverer() {
        this(new YamlPolicyMigrationParser());
    }

    public PolicyMigrationDiscoverer(PolicyMigrationParser parser) {
        this.parser = parser;
    }

    /** Discover on the classpath under {@link #DEFAULT_LOCATION} using the given classloader. */
    public List<com.a3shield.policymgmt.migration.PolicyMigration> discoverFromClasspath(ClassLoader classLoader) {
        List<NamedContent> files = new ArrayList<>();
        try {
            Enumeration<URL> roots = classLoader.getResources(DEFAULT_LOCATION);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                if ("file".equals(root.getProtocol())) {
                    collectDirectory(Path.of(root.toURI()), files);
                } else if ("jar".equals(root.getProtocol())) {
                    collectJar(root, files);
                }
            }
        } catch (Exception e) {
            throw new PolicyMigrationException("Failed to scan classpath for policy migrations", e);
        }
        return parseAll(files);
    }

    /** Discover in a filesystem directory (tests, filesystem deployments). */
    public List<com.a3shield.policymgmt.migration.PolicyMigration> discoverFromDirectory(Path directory) {
        List<NamedContent> files = new ArrayList<>();
        collectDirectory(directory, files);
        return parseAll(files);
    }

    private void collectDirectory(Path directory, List<NamedContent> files) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        List<Path> entries;
        try (Stream<Path> stream = Files.list(directory)) {
            entries = stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new PolicyMigrationException("Failed to list policy directory " + directory, e);
        }
        for (Path path : entries) {
            try {
                files.add(new NamedContent(
                        path.getFileName().toString(), Files.readString(path, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new PolicyMigrationException("Failed to read policy migration " + path, e);
            }
        }
    }

    private void collectJar(URL root, List<NamedContent> files) throws IOException {
        URLConnection connection = root.openConnection();
        if (!(connection instanceof JarURLConnection jarConnection)) {
            return;
        }
        try (JarFile jar = jarConnection.getJarFile()) {
            String prefix = DEFAULT_LOCATION + "/";
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix)) {
                    continue;
                }
                String fileName = name.substring(prefix.length());
                if (!FILE_NAME.matcher(fileName).matches()) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    files.add(new NamedContent(fileName, new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private List<com.a3shield.policymgmt.migration.PolicyMigration> parseAll(List<NamedContent> files) {
        List<com.a3shield.policymgmt.migration.PolicyMigration> migrations = new ArrayList<>();
        for (NamedContent file : files) {
            Matcher matcher = FILE_NAME.matcher(file.name());
            if (!matcher.matches()) {
                logger.debug("Ignoring non-migration file in policy directory: {}", file.name());
                continue;
            }
            migrations.add(
                    parser.parse(file.name(), Integer.parseInt(matcher.group(1)), matcher.group(2), file.content()));
        }
        migrations.sort(Comparator.comparingInt(com.a3shield.policymgmt.migration.PolicyMigration::getVersion));
        for (int i = 1; i < migrations.size(); i++) {
            if (migrations.get(i).getVersion() == migrations.get(i - 1).getVersion()) {
                throw new PolicyMigrationException("Duplicate policy migration version V"
                        + migrations.get(i).getVersion() + ": "
                        + migrations.get(i).description() + " and "
                        + migrations.get(i - 1).description());
            }
        }
        return migrations;
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PolicyMigrationDiscoverer.class);

    private record NamedContent(String name, String content) {}
}
