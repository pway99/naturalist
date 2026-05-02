# Adapters

Ports-and-adapters implementations carrying heavy or vendor-specific
third-party dependencies. The `adapters/` tree exists so that core code
(`kernels/`, `domains/<d>/<d>-api`, `domains/<d>/<d>-core`) can stay free
of those dependencies, and so that swapping a vendor means changing one
adapter module instead of every call site.

## Placement rule

A module belongs in `adapters/` when **all** of the following are true:

1. It implements a port defined by a kernel or by `*-api` (a facade
   interface, a contribution interface, a runtime bridge contract).
2. It pulls in a third-party dependency that should not appear on the
   classpath of any module that depends only on the kernels or on a
   domain api.
3. It is not owned by a single domain's data — adapters here serve the
   composition root, not one domain's persistence.

## What does NOT belong here

- **Per-domain repository adapters.** `domains/<d>/<d>-repository-rdms/`
  stays under its domain. The adapter is owned by the domain whose data
  it persists, not by the app's composition root. The DAG rule
  "repository modules depend only on their own api" still applies.
- **Pure kernel implementations with light deps.** Reference adapters
  that depend only on the framework's existing third-party set
  (Jackson, Commons, Micrometer, JSpecify) belong in `kernels/`. The
  in-memory catalog (`kernels/catalog-inmem/`) is the canonical
  example.
- **Composition roots.** Executable apps with a `main` belong in
  `apps/`. Adapters never have a `main`.

## Members

- `resilience-resilience4j/` — bridges the kernel `Resilience` facade
  (in `kernels/framework`) to `io.github.resilience4j.*`. Domain
  `*-core` imports the kernel facade only; only this adapter imports
  Resilience4j.
- `spring-runtime/` — DI bridge. Ships
  `DomainServiceScan`, an `ImportBeanDefinitionRegistrar` that scans the
  classpath for classes carrying the
  `com.naturalist.infrastructure.@DomainService` marker (defined in
  `kernels/framework`) and registers them as Spring beans. Apps activate
  the scan with `@Import(DomainServiceScan.class)` on a configuration
  class. The marker carries no Spring meta-annotation; domain `*-core`
  modules import the marker only.

  Post-M7 the scan covers the full `com.naturalist` root: every domain
  shipping a `DomainId` subtype, an `EntityRefLinker`, a
  `CatalogContribution`, or an `EntityReferences` provider with the
  `@DomainService` marker is auto-discovered. A new domain adding the
  marker requires no change to either the scan or the assembly factory.

- `spring-test-data/` — Spring-side composition adapter that publishes
  the `framework-test` `NaturalistDatabase` as a singleton bean. Holds
  Spring out of `kernels/framework-test` and the `<domain>-repository-test`
  modules. The adapter is on the classpath only while the app is
  pre-RDBMS; once the production data adapter lands, this module is
  excluded from the deployment artifact, the class disappears from the
  classpath, and the bean simply does not exist — no profile checks
  needed at consumer sites.

Future members anticipated by the runtime architecture refactor plan
(`docs/plans/runtime-architecture-refactor.md`):
`catalog-solr/` (production Solr-backed catalog).

## Dependency rules

- An adapter depends on `kernels/framework` (and possibly other
  kernels) plus its single vendor's library. It does **not** depend on
  another adapter, on any domain `*-api`/`*-core`, or on `apps/*`.
- `apps/*` modules depend on the adapters they compose. Adapters never
  depend on apps.
- Adapters never import Spring unless the adapter is itself the Spring
  runtime bridge. The boundary discipline that protects core from
  Spring also protects each non-Spring adapter from Spring.
