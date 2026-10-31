package com.a3shield.policymgmt.parse;

import com.a3shield.core.model.Resource;
import com.a3shield.core.model.Subject;
import com.a3shield.core.model.SubjectType;
import com.a3shield.core.model.Target;
import com.a3shield.enforcer.effect.Effect;
import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.PolicyMigrationException;
import com.a3shield.policymgmt.migration.PolicyTestCase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Round-trips a single {@link Policy} (and its validation tests) through the
 * YAML policy format (ADR-0010). Used by the effective store (policy bodies
 * are persisted as YAML) and the admin API (mutations arrive as YAML or
 * maps and are validated before commit).
 */
public class PolicySerde {

    private final Yaml yaml = new Yaml(defaultDumperOptions());

    private static DumperOptions defaultDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(false);
        return options;
    }

    /** Serialize one policy to its YAML mapping (no document-level keys). */
    public String toYaml(Policy policy) {
        return yaml.dump(toMap(policy));
    }

    public Map<String, Object> toMap(Policy policy) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", policy.getId());
        map.put("effect", policy.getEffect() == null ? null : policy.getEffect().name());
        if (policy.getSubject() != null && !policy.getSubject().isEmpty()) {
            map.put("subject", policy.getSubject());
        }
        if (policy.getResource() != null) {
            map.put("resource", policy.getResource());
        }
        if (policy.getAction() != null) {
            map.put("action", policy.getAction());
        }
        if (policy.getCondition() != null) {
            map.put("condition", policy.getCondition());
        }
        if (policy.getRelations() != null && !policy.getRelations().isEmpty()) {
            map.put("relations", policy.getRelations());
        }
        if (policy.getVersion() != null) {
            map.put("version", policy.getVersion());
        }
        return map;
    }

    /** Parse one policy from a YAML mapping (as loaded from a document or JSON-like structure). */
    public Policy fromMap(Map<String, Object> entry) {
        String id = require(entry.get("id"), "policy entry missing 'id'");
        String effect = require(entry.get("effect"), "policy '" + id + "' missing 'effect'");
        return Policy.builder()
                .id(id)
                .effect(parseEffect(effect, id))
                .subject(asMap(entry.get("subject")))
                .resource(str(entry.get("resource")))
                .action(str(entry.get("action")))
                .condition(str(entry.get("condition")))
                .relations(asMap(entry.get("relations")))
                .version(str(entry.get("version")))
                .build();
    }

    public Policy fromYaml(String body) {
        Object loaded = yaml.load(body);
        if (!(loaded instanceof Map)) {
            throw new PolicyMigrationException("policy body must be a YAML mapping");
        }
        return fromMap((Map<String, Object>) loaded);
    }

    /** Serialize validation tests to YAML (stored alongside the policy). */
    public String testsToYaml(List<PolicyTestCase> tests) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (PolicyTestCase test : tests) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", test.getName());
            entry.put("expect", test.getExpect().name());
            if (test.getSubject() != null) {
                entry.put("subject", subjectToMap(test.getSubject()));
            }
            if (test.getResources() != null && !test.getResources().isEmpty()) {
                entry.put("resources", resourcesToMaps(test.getResources()));
            }
            if (test.getAction() != null) {
                entry.put("action", test.getAction());
            }
            if (test.getTenantId() != null) {
                entry.put("tenant", test.getTenantId());
            }
            entries.add(entry);
        }
        return yaml.dump(entries);
    }

    /** Parse stored validation tests back from YAML (blank → empty list). */
    @SuppressWarnings("unchecked")
    public List<PolicyTestCase> testsFromYaml(String testsYaml) {
        if (testsYaml == null || testsYaml.isBlank()) {
            return List.of();
        }
        Object loaded = yaml.load(testsYaml);
        if (!(loaded instanceof List<?> list)) {
            throw new PolicyMigrationException("stored tests must be a YAML list");
        }
        List<PolicyTestCase> tests = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map)) {
                throw new PolicyMigrationException("stored test entries must be mappings");
            }
            Map<String, Object> entry = (Map<String, Object>) item;
            String name = str(entry.get("name")) == null
                    ? "unnamed"
                    : entry.get("name").toString();
            java.util.Set<String> roles = entry.get("subject") instanceof Map
                            && ((Map<String, Object>) entry.get("subject")).get("roles") instanceof List<?> roleList
                    ? java.util.Set.copyOf((List<String>) roleList)
                    : null;
            Subject.SubjectBuilder subjectBuilder = Subject.builder();
            if (entry.get("subject") instanceof Map<?, ?> subjectMap) {
                Map<String, Object> sm = (Map<String, Object>) subjectMap;
                if (sm.get("id") != null) {
                    subjectBuilder.id(sm.get("id").toString());
                }
                if (sm.get("type") != null) {
                    subjectBuilder.type(
                            SubjectType.valueOf(sm.get("type").toString().toUpperCase()));
                }
            }
            List<Resource> resources = new ArrayList<>();
            if (entry.get("resources") instanceof List<?> resourceList) {
                for (Object resourceItem : resourceList) {
                    if (!(resourceItem instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> rm = (Map<String, Object>) resourceItem;
                    resources.add(Target.builder()
                            .id(str(rm.get("id")))
                            .type(str(rm.get("type")))
                            .attributes(asMap(rm.get("attributes")) == null ? Map.of() : asMap(rm.get("attributes")))
                            .build());
                }
            }
            tests.add(PolicyTestCase.builder()
                    .name(name)
                    .expect(com.a3shield.core.decision.Decision.valueOf(
                            entry.get("expect").toString().toUpperCase()))
                    .subject(subjectBuilder.roles(roles).build())
                    .resources(resources)
                    .action(str(entry.get("action")))
                    .tenantId(str(entry.get("tenant")))
                    .build());
        }
        return tests;
    }

    private Map<String, Object> subjectToMap(Subject subject) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (subject.getId() != null) {
            map.put("id", subject.getId());
        }
        if (subject.getType() != null) {
            map.put("type", subject.getType().name());
        }
        if (subject.getRoles() != null && !subject.getRoles().isEmpty()) {
            map.put("roles", subject.getRoles());
        }
        return map;
    }

    private List<Map<String, Object>> resourcesToMaps(List<Resource> resources) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Resource resource : resources) {
            if (!(resource instanceof Target target)) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            if (target.getId() != null) {
                entry.put("id", target.getId());
            }
            if (target.getType() != null) {
                entry.put("type", target.getType());
            }
            if (target.getAttributes() != null && !target.getAttributes().isEmpty()) {
                entry.put("attributes", target.getAttributes());
            }
            entries.add(entry);
        }
        return entries;
    }

    private Effect parseEffect(String value, String id) {
        try {
            return Effect.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PolicyMigrationException("policy '" + id + "' has invalid effect '" + value + "'");
        }
    }

    private static String require(Object value, String error) {
        if (value == null || value.toString().isBlank()) {
            throw new PolicyMigrationException(error);
        }
        return value.toString();
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}
