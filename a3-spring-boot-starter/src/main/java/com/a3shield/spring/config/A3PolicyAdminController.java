package com.a3shield.spring.config;

import com.a3shield.enforcer.policy.Policy;
import com.a3shield.policymgmt.admin.PolicyAdminApi;
import com.a3shield.policymgmt.admin.PolicyValidationException;
import com.a3shield.policymgmt.migration.PolicyTestCase;
import com.a3shield.policymgmt.parse.PolicySerde;
import com.a3shield.spring.context.SubjectContext;
import com.a3shield.spring.tenant.TenantContextHolder;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface of the {@link PolicyAdminApi} (opt-in, ADR-0003). Bodies are
 * JSON in the same shape as YAML policy entries:
 *
 * <pre>{@code
 * {
 *   "policy": { "id": "...", "effect": "ALLOW", "resource": "...", "action": "...", "condition": "..." },
 *   "tests": [ { "name": "...", "expect": "ALLOW", ... } ]
 * }
 * }</pre>
 *
 * <p>Tenant scoping: {@code ?tenant=} parameter, falling back to the request
 * tenant (resolved via the {@code TenantResolver}), else {@code default}.
 * <b>Protect {@code /a3/admin/**} with Spring Security — this controller
 * performs no authentication or authorization itself.</b></p>
 */
@RestController
@RequestMapping("/a3/admin")
public class A3PolicyAdminController {

    private final PolicyAdminApi adminApi;
    private final PolicySerde serde = new PolicySerde();

    public A3PolicyAdminController(PolicyAdminApi adminApi) {
        this.adminApi = adminApi;
    }

    @GetMapping("/policies")
    public List<Map<String, Object>> list(@RequestParam(required = false) String tenant) {
        return adminApi.list(scope(tenant)).stream()
                .map(record -> Map.<String, Object>of(
                        "id", record.getPolicy().getId(),
                        "origin", record.getOrigin().name(),
                        "policy", serde.toMap(record.getPolicy())))
                .toList();
    }

    @PostMapping("/policies")
    public ResponseEntity<Map<String, Object>> create(
            @RequestParam(required = false) String tenant, @RequestBody Map<String, Object> body) {
        Policy policy = serde.fromMap(requirePolicy(body));
        try {
            var record = adminApi.create(scope(tenant), policy, parseTests(body), actor());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "id",
                            record.getPolicy().getId(),
                            "origin",
                            record.getOrigin().name()));
        } catch (PolicyValidationException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }

    @PutMapping("/policies/{id}")
    public Map<String, Object> update(
            @PathVariable String id,
            @RequestParam(required = false) String tenant,
            @RequestBody Map<String, Object> body) {
        Policy policy = serde.fromMap(requirePolicy(body));
        try {
            var record = adminApi.update(scope(tenant), id, policy, parseTests(body), actor());
            return Map.of(
                    "id",
                    record.getPolicy().getId(),
                    "origin",
                    record.getOrigin().name());
        } catch (PolicyValidationException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }

    @DeleteMapping("/policies/{id}")
    public Map<String, Boolean> delete(@PathVariable String id, @RequestParam(required = false) String tenant) {
        return Map.of("deleted", adminApi.delete(scope(tenant), id, actor()));
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> audit(
            @RequestParam(required = false) String tenant, @RequestParam(defaultValue = "50") int limit) {
        return adminApi.audit(scope(tenant), Math.min(limit, 500)).stream()
                .map(record -> Map.<String, Object>of(
                        "id", record.id(),
                        "actor", record.actor(),
                        "at", record.at().toString(),
                        "policyId", record.policyId(),
                        "operation", record.operation().name()))
                .toList();
    }

    @GetMapping("/drift")
    public List<PolicyAdminApi.DriftEntry> drift(@RequestParam(required = false) String tenant) {
        return adminApi.drift(scope(tenant));
    }

    private String scope(String tenantParam) {
        if (tenantParam != null && !tenantParam.isBlank()) {
            return tenantParam;
        }
        String current = TenantContextHolder.getTenantId();
        return current == null ? com.a3shield.core.tenant.Tenants.DEFAULT_TENANT : current;
    }

    private String actor() {
        try {
            var subject = SubjectContext.getCurrentSubject();
            return subject == null || subject.getId() == null ? "admin-api" : subject.getId();
        } catch (RuntimeException e) {
            return "admin-api"; // no subject wiring (e.g. standalone tests)
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requirePolicy(Map<String, Object> body) {
        Object policy = body.get("policy");
        if (!(policy instanceof Map)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body must contain a 'policy' object");
        }
        return (Map<String, Object>) policy;
    }

    private List<PolicyTestCase> parseTests(Map<String, Object> body) {
        Object tests = body.get("tests");
        if (!(tests instanceof List<?> list)) {
            return null; // keep the stored validation suite on update
        }
        String yaml = new org.yaml.snakeyaml.Yaml().dump(list);
        return serde.testsFromYaml(yaml);
    }
}
