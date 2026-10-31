package com.a3shield.policymgmt.parse;

import com.a3shield.core.decision.Decision;
import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.core.model.Target;
import com.a3shield.core.tenant.Tenants;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.migration.OnConflict;
import com.a3shield.policymgmt.migration.PolicyMigration;
import com.a3shield.policymgmt.migration.PolicyTestCase;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Parses YAML policy migration files (ADR-0010). Loading is safe by
 * construction: documents are loaded as plain maps and bound manually —
 * SnakeYAML never instantiates arbitrary classes.
 */
public class YamlPolicyMigrationParser implements PolicyMigrationParser {

    private static final Logger logger = LoggerFactory.getLogger(YamlPolicyMigrationParser.class);

    private final Yaml yaml = new Yaml(new LoaderOptions());

    @Override
    public PolicyMigration parse(String sourceName, int version, String name, String content) {
        Map<String, Object> doc;
        try {
            Object loaded = yaml.load(content);
            if (!(loaded instanceof Map)) {
                throw new PolicyMigrationException(sourceName + ": expected a YAML mapping at document root, got "
                        + (loaded == null ? "null" : loaded.getClass().getSimpleName()));
            }
            doc = asMap(loaded);
        } catch (PolicyMigrationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PolicyMigrationException(sourceName + ": invalid YAML: " + e.getMessage(), e);
        }

        String tenantId = str(doc.get("tenant"), Tenants.DEFAULT_TENANT);
        OnConflict onConflict = parseOnConflict(str(doc.get("onConflict"), "FAIL"), sourceName);

        List<Policy> policies = parsePolicies(asMapList(doc.get("policies"), "policies"), tenantId, sourceName);
        List<PolicyTestCase> tests = parseTests(asMapList(doc.get("tests"), "tests"), sourceName);

        return PolicyMigration.builder()
                .version(version)
                .name(name)
                .tenantId(tenantId)
                .onConflict(onConflict)
                .policies(policies)
                .tests(tests)
                .checksum(sha256(content))
                .source(sourceName)
                .build();
    }

    private List<Policy> parsePolicies(List<Map<String, Object>> entries, String tenantId, String source) {
        List<Policy> policies = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            String id = requireString(entry, "id", source + ": policy entry missing 'id'");
            Effect effect =
                    Effect.valueOf(requireString(entry, "effect", source + ": policy '" + id + "' missing 'effect'")
                            .toUpperCase());

            Policy policy = Policy.builder()
                    .id(id)
                    .effect(effect)
                    .tenantId(tenantId)
                    .subject(asMapOrEmpty(entry.get("subject"), source + ": policy '" + id + "'"))
                    .resource(str(entry.get("resource"), null))
                    .action(str(entry.get("action"), null))
                    .condition(str(entry.get("condition"), null))
                    .relations(asMapOrEmpty(entry.get("relations"), source + ": policy '" + id + "'"))
                    .version(str(entry.get("version"), null))
                    .build();
            policies.add(policy);
        }
        return policies;
    }

    private List<PolicyTestCase> parseTests(List<Map<String, Object>> entries, String source) {
        List<PolicyTestCase> tests = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            String testName = str(entry.get("name"), "unnamed");
            Decision expect;
            try {
                expect = Decision.valueOf(
                        requireString(entry, "expect", source + ": test '" + testName + "' missing 'expect'")
                                .toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PolicyMigrationException(
                        source + ": test '" + testName + "' has invalid expect value (ALLOW/DENY/NOT_APPLICABLE)");
            }

            Subject subject = parseSubject(asMapOrEmpty(entry.get("subject"), source + ": test '" + testName + "'"));
            List<Resource> resources = new ArrayList<>();
            for (Map<String, Object> resourceEntry :
                    asMapList(entry.get("resources"), source + ": test '" + testName + "' resources")) {
                Target target = Target.builder()
                        .id(str(resourceEntry.get("id"), null))
                        .type(str(resourceEntry.get("type"), null))
                        .attributes(asMapOrEmpty(resourceEntry.get("attributes"), source + ": test resource"))
                        .build();
                resources.add(target);
            }

            tests.add(PolicyTestCase.builder()
                    .name(testName)
                    .subject(subject)
                    .resources(resources)
                    .action(str(entry.get("action"), null))
                    .tenantId(str(entry.get("tenant"), null))
                    .expect(expect)
                    .build());
        }
        return tests;
    }

    private Subject parseSubject(Map<String, Object> entry) {
        String type = str(entry.get("type"), null);
        return Subject.builder()
                .id(str(entry.get("id"), null))
                .type(type == null ? null : SubjectType.valueOf(type.toUpperCase()))
                .roles(entry.get("roles") instanceof List<?> roles ? java.util.Set.copyOf((List<String>) roles) : null)
                .build();
    }

    private OnConflict parseOnConflict(String value, String source) {
        try {
            return OnConflict.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PolicyMigrationException(source + ": invalid onConflict '" + value + "' (FAIL/OVERWRITE/SKIP)");
        }
    }

    static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String requireString(Map<String, Object> map, String key, String error) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new PolicyMigrationException(error);
        }
        return value.toString();
    }

    private static String str(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> asMapOrEmpty(Object value, String error) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map)) {
            throw new PolicyMigrationException(
                    error + ": expected a mapping, got " + value.getClass().getSimpleName());
        }
        return (Map<String, Object>) value;
    }

    private static List<Map<String, Object>> asMapList(Object value, String error) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new PolicyMigrationException(error + ": expected a list");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map)) {
                throw new PolicyMigrationException(error + ": expected list entries to be mappings");
            }
            result.add((Map<String, Object>) item);
        }
        return result;
    }
}
