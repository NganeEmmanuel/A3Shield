# ADR-0001: Authorization-first scope; authentication via integration

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

The original vision was a full IAM platform: authentication (JWT, login, MFA) *and* authorization (RBAC/PBAC/ABAC). Building authentication means reimplementing JWT issuance, refresh rotation, revocation, password storage, MFA — a solved space owned by Spring Security, Keycloak, Auth0, and SuperTokens. Every hour spent there is an hour not spent on A3Shield's actual differentiators: the policy engine, the migration-based policy lifecycle, and runtime admin editing. The repo already contains the escape hatch: `SubjectExtractor` is decoupled from Spring Security.

Options considered:
1. Build both authn and authz (original vision) — huge surface, crowded market, slow to v0.1.
2. Authorization-only core; authn consumed via SPI (chosen).
3. Authorization core + thin authn convenience wrappers later.

## Decision

A3Shield's core is **authorization-only**. Identity is *consumed*, never issued:

- `SubjectExtractor` is a first-class SPI; the shipped default reads Spring Security's context (JWT/OIDC whatever the host app already uses).
- Perimeter concerns (CORS, rate limiting, IP blacklists) are exposed as an extensible `RequestCheck` chain in the starter. We ship only security-relevant checks ourselves; rate limiting and CORS are delegated/integrated, not built.
- An `a3-auth-adapter` module (third-party login flows, token issuance convenience) may exist **later** as an adapter, never as core, and never a precondition for the authorization story.

## Consequences

- Smaller surface, faster path to a usable v0.1 and to publishing.
- Users must bring their own authentication — documented as a feature ("bring your own identity"), not a gap.
- We cannot market "complete IAM" initially; positioning is "sophisticated access control, plug-and-play".
- The `README`'s authn feature list is aspirational and must not drive near-term milestones (see ROADMAP M4/vision).
