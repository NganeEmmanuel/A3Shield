# ADR-0010: Policy migration files use YAML

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

ADR-0003 left the policy file format open (JSON vs YAML), to be decided at M4 kickoff. Constraints: the files are hand-authored by developers (reviewed in PRs), must carry embedded policy tests, and must be parsed by a Spring-free module (ADR-0007 engine boundary). JSON has no comments — a real cost for policy files, which benefit from explaining *why* a rule exists. YAML supports comments and is the established convention for human-authored policy/config artifacts (Kubernetes, CI, OPA data files). SnakeYAML is a single small dependency with no transitive deps and safe by default in 2.x.

Options considered:
1. **YAML** via SnakeYAML — human-friendly, comments, one small dep.
2. **JSON** via Jackson — machine-friendly, but Jackson databind is heavy for a Spring-free module and the no-comments problem is permanent.
3. Support both — doubles the parser surface and the docs; YAGNI until a consumer asks.

## Decision

Policy migration files are **YAML** (`V<n>__<name>.yaml` or `.yml` under `a3/policies/`), parsed with SnakeYAML (safe loading only — `Map`-based construction, never target-class instantiation). YAML documents in policy files are loaded as plain maps and bound manually; arbitrary object deserialization is disabled.

## Consequences

- Policies are commentable and human-reviewable; structure mirrors K8s-style config conventions.
- One new dependency (org.yaml:snakeyaml, version managed by the imported BOM) in `a3-policy-mgmt` only — the enforcer and core stay clean.
- A JSON loader can be added later behind the same `PolicyMigrationParser` interface if tooling ever emits JSON migrations; not planned now.
