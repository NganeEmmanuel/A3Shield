# Contributing to A3Shield

Thank you for contributing! This document describes the branch model, commit
conventions, CI gates and the exact local commands that mirror CI.

## Branch Model

Three permanent, protected branches:

| Branch     | Purpose                                    | Merged from        |
|------------|--------------------------------------------|--------------------|
| `master`   | Production releases                        | `staging`          |
| `staging`  | Pre-release hardening / release candidates | `develop`          |
| `develop`  | Day-to-day integration                     | work branches      |

All work — yours, the maintainer's, and external contributors' — happens on
short-lived branches cut from `develop` and merged back via pull request:

```
feature/<short-name>   new capabilities
fix/<short-name>       bug fixes
chore/<short-name>     tooling, CI, deps, refactors without behaviour change
security/<short-name>  vulnerability-related changes
docs/<short-name>      documentation-only changes
```

Flow: `develop` → (release hardening) → `staging` → (release) → `master`.
Hotfixes may branch from `master` (`fix/*`), then merge back to `master` and
`staging`/`develop`.

**Never commit directly to `master`, `staging` or `develop`.**

## Commits — Conventional Commits

Every commit message follows [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <imperative summary, lowercase, <= 72 chars>

<optional body: what and why>
```

Types: `feat`, `fix`, `chore`, `docs`, `ci`, `build`, `security`,
`refactor`, `test`, `perf`.

Progressive commits within a piece of work are encouraged — small, meaningful
commits are preferred over one large one. A PR may contain multiple related
commits; merges use a **merge commit** (not squash) so that history is
preserved.

## Pull Requests

- PRs target `develop` by default (`staging` only for release prep, `master`
  only for hotfixes).
- The PR description must follow `.github/PULL_REQUEST_TEMPLATE.md`
  (What / Why / Type / Changes / Testing / Checklist / Breaking changes /
  Related).
- CI must be green before merge.

## Local Verification — must match CI

CI (`.github/workflows/`) runs exactly these commands. If they pass locally,
they pass in CI — same pinned Maven (wrapper), same JDK line, no hidden state:

```bash
./mvnw -B -ntp clean verify        # build + all tests (what CI runs)
./mvnw -B -ntp spotless:check      # format check (CI gate)
./mvnw -B -ntp spotless:apply      # auto-fix formatting before committing
```

Requirements: **JDK 23** (Temurin in CI; any 23.x distribution locally) and
nothing else — the Maven wrapper downloads the pinned Maven 3.9.11 itself.
Tests are pure unit tests: no database, network, or secrets required.

## What CI Checks

| Gate                     | Tool                   | When                          |
|--------------------------|------------------------|-------------------------------|
| Build + tests            | `./mvnw clean verify`  | push/PR touching code         |
| Format                   | Spotless (palantir)    | push/PR touching code         |
| Static analysis / injection | CodeQL (Java)       | push/PR touching code         |
| Secret / leak scan       | gitleaks (full history)| every push & PR               |
| Dependency vulnerabilities | dependency-review     | every PR                      |
| Ongoing dep updates      | Dependabot (maven + actions) | weekly, opens PRs        |

Docs-only / metadata-only changes (`.md`, `docs/`, `LICENSE`, `.gitignore`)
skip the build/test/CodeQL workflows — the secret scan still runs everywhere.

## Code Standards

* Follow clean architecture principles; keep `a3-core` dependency-free and
  `a3-enforcer` Spring-free (see `AGENTS.md` and `docs/adr/`).
* All new features and bug fixes include tests.
* Read `AGENTS.md` before changing the codebase — it reflects the actual
  state and known issues.
* Architecture-relevant decisions get an ADR in `docs/adr/`.

## Issues & Discussions

Open an issue before large changes so we can align on the approach first.
