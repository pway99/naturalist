# ADR-024: `apps/` and `adapters/` Trees — Placement Rule

**Status:** Accepted

**Date:** 2026-05-02

## Context

For most of the codebase's life there were two top-level module groups:
`kernels/` (cross-cutting infrastructure) and `domains/` (the bounded
contexts). Deployment artifacts lived under `naturalist-web/console/` — a
parent that suggested "web is the only app shape" and conflated a
deployable executable with the per-domain `*-console` contribution
libraries that share the suffix.

Two pressures forced a structural answer.

**Production approach.** A management console is the first deployable, but
the codebase needs a place for further executables — a sync daemon, an
importer, a scheduled-task runner, a future field-guide API. None of those
is "web."

**Heavy adapters do not belong in `kernels/`.** `kernels/framework`
deliberately depends only on Jackson, Commons, Micrometer, and JSpecify
(ADR-018). The first production-grade catalog adapter (Solr) would drag
SolrJ, the Zookeeper client, and Lucene transitives onto the classpath of
every consumer of the catalog kernel. Spring and Resilience4j present the
same problem at smaller scale. Adapters with that weight need a home
outside `kernels/`.

## Decision

Two new top-level module groups, sibling to `kernels/` and `domains/`.

### `apps/`

Hosts composition-root deployment artifacts — modules with a `main` and a
deployable jar. Every executable in the system lives here.

- `apps/management-console/` — Spring Boot fat jar, the human-facing
  domain catalog UI. First member.
- Future members anticipated: a sync daemon, an importer, a scheduled-task
  runner, a field-guide API.

The placement rule for an app:

1. It has a `main` (or a Spring Boot equivalent that the build packages as
   an executable).
2. It composes the system. It is the only kind of module allowed to know
   about every domain it deploys, and the only kind of module allowed to
   import an `adapters/` member.
3. Apps never depend on apps. Each app is a standalone deployment.

### `adapters/`

Hosts ports-and-adapters implementations whose third-party dependencies
are too heavy or too vendor-specific to live in `kernels/`. The boundary
discipline that protects core from Spring also protects core from
Resilience4j, SolrJ, and any future infrastructure library.

- `adapters/resilience-resilience4j/` — bridges the kernel `Resilience`
  facade to `io.github.resilience4j.*` (ADR-026).
- `adapters/spring-runtime/` — DI bridge that scans for
  `@DomainService` and registers candidates as Spring beans (ADR-025).
- `adapters/spring-test-data/` — Spring composition adapter that
  publishes the `framework-test` `NaturalistDatabase` as a singleton
  bean. Pre-RDBMS scaffolding; excluded from the deployment artifact
  once the production data adapter lands.
- Future members anticipated: `adapters/catalog-solr/` (production
  catalog backend), `adapters/runtime-<x>/` (alternative DI runtimes if
  they ever land).

The placement rule for an adapter (all three must hold):

1. It implements a port defined by a kernel or by a `*-api` module.
2. It pulls in a third-party dependency that should not appear on the
   classpath of any module depending only on the kernels or on a domain
   api.
3. It is not owned by a single domain's data — adapters here serve the
   composition root, not one domain's persistence.

### What does NOT move into `adapters/`

**Per-domain repository adapters stay with their domain.**
`domains/<d>/<d>-repository-rdms/` is owned by the domain whose data it
persists, not by an app's composition root. The DAG rule "repository
modules depend only on their own api" still applies. A domain's RDBMS
schema decisions are part of the domain's evolution; a centralized
`adapters/repositories-rdms/` would invert that.

**Light reference adapters stay in `kernels/`.** A reference adapter that
depends only on the framework's existing third-party set is a kernel
module, not an adapter-tree member. `kernels/catalog-inmem/` is the
canonical example: it implements a kernel port, ships no extra
dependencies, and serves the same role for kernel tests as a fixture.

## Consequences

- The `naturalist-web/` parent disappears. The path
  `naturalist-web/console/` migrates to `apps/management-console/`. A
  domain's per-domain `*-console` contribution library remains in its
  domain — those modules are libraries, not apps, and their suffix now
  unambiguously names a contribution surface rather than a deployment
  artifact.
- Apps depend on the adapters they compose. Adapters never depend on
  apps. Adapters do not depend on each other except where the
  dependency is also a kernel-level port (e.g. an adapter wiring two
  facades the kernel defines).
- A new adapter introducing a new third-party dependency lands in
  `adapters/` automatically by the placement rule. Reviewers do not
  need to relitigate where it goes.
- A new app — the sync daemon, the importer — lands in `apps/`
  automatically by the same rule.
- The kernel's third-party surface is preserved. `kernels/framework`
  still depends only on Jackson, Commons, Micrometer, and JSpecify.
  Nothing in the catalog kernel forces a SolrJ transitive on consumers.
- A future runtime swap (Guice, Helidon, or plain `main`-based
  composition) is a single new adapter under `adapters/runtime-<x>/`,
  not a re-architecture of every domain.

## Applicability Signals

Flag an ADR-024 violation in review when any of the following appears:

- A new module with a `main` lands outside `apps/`.
- A new module wrapping a heavy third-party dependency lands inside
  `kernels/` or inside a domain's tree.
- A `*-repository-rdms` module migrates out of its domain into
  `adapters/`.
- A kernel module gains a third-party dependency that is not Jackson,
  Commons, Micrometer, or JSpecify (ADR-018 + ADR-024).
- An adapter under `adapters/` declares a `main` or is packaged as a
  deployment artifact.
- An app declares a dependency on another app.

## Related ADRs

- ADR-018 — Third-Party Dependency Policy (the rule the new tree
  preserves at the kernel boundary).
- ADR-025 — DI via Marker Annotations (`adapters/spring-runtime/` is
  the canonical adapter shape).
- ADR-026 — Resilience as a First-Order Concern
  (`adapters/resilience-resilience4j/` is its production implementation).

## Reference Implementation

Post-M4 and M5 of the runtime architecture refactor:

- `apps/pom.xml` — `naturalist-apps` parent, packaging `pom`.
- `apps/management-console/` — Spring Boot fat jar; sole app today.
- `adapters/pom.xml` — `naturalist-adapters` parent, packaging `pom`;
  owns shared `resilience4j-version`, `junit-version`, and
  `assertj-version` properties so adapters do not lean on the kernels
  parent.
- `adapters/resilience-resilience4j/`, `adapters/spring-runtime/`,
  `adapters/spring-test-data/` — current members.
- `adapters/CLAUDE.md` — documents the placement rule, the three-condition
  check, and the dependency rules.
- `apps/CLAUDE.md` — documents the composition-root rule and what does
  not belong in `apps/`.
