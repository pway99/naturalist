# Runtime Architecture Refactor — Effort Plan

A coordinated structural refactor that renames the atlas kernel to *catalog*,
opens `DomainId` so each domain owns its own identity, hoists a top-level
`apps/` tree (with `naturalist-web/console/` becoming `apps/management-console/`),
introduces a top-level `adapters/` tree for production infrastructure that
should not pollute the kernel's dependency surface, and — as the priority
foundation that gates everything that follows — ships a kernel-level
`Resilience` facade with a Resilience4j-backed adapter.

This document is the durable plan for executing all of it. Like the atlas
plan, it is structured so any one milestone can be picked up in a fresh
session without reading prior chat history. The architectural decisions are
stated declaratively at the top; each milestone lists the exact files to read
first before doing the work.

---

## Why this exists

Three concrete pains motivate the refactor.

**The kernel knows the names of every domain.** `kernels/atlas/DomainId` is a
sealed interface with nested records for `Plants`, `Chemistry`, `Insects`,
plus an `of(String)` factory enumerating every slug. Adding a domain — and
the codebase already has eight more scaffolded (apiary, arachnids, climate,
fungi, microbes, molluscs, naturalists, plus active work on others) — is a
kernel edit that violates the project's own "no domain knowledge in the
kernel" rule. The closed set was originally chosen to bound the
`source_domain` metric tag's cardinality; that property can be preserved by
assembly-time validation without the sealed type carrying it.

**`naturalist-web/console/` is the only deployment artifact the codebase
admits, and its placement implies that.** As production approaches, the
codebase needs a first-class place to host additional executables — a sync
daemon, an importer, a scheduled-task runner, a future field-guide API. A
top-level `apps/` tree (sibling to `domains/` and `kernels/`) is the natural
slot. While renaming, the existing `console/` artifact is also worth
disambiguating from the per-domain `chemistry-console`, `plants-console`
contribution libraries — its real name is `management-console`.

**The first production-grade catalog adapter (Solr) does not belong in
`kernels/`.** Kernels are intentionally light on third-party deps —
`framework` depends only on Jackson, Commons, Micrometer, and JSpecify.
Adding `kernels/catalog-solr/` would drag SolrJ, Zookeeper client, and
Lucene transitives into a place reserved for the framework. The cleaner
home is a new top-level `adapters/` tree explicitly designated for
ports-and-adapters implementations carrying heavy infrastructure
dependencies. The same neighborhood houses the future Spring runtime
bridge (`adapters/spring-runtime/`) and the resilience adapter
(`adapters/resilience-resilience4j/`) so that core code never imports them
directly.

A fourth motivator is forward-looking rather than corrective: **resilience
must become a first-order concern from this point onward.** Production
deployment is closer than it felt a quarter ago. Every cross-boundary call
— repository, catalog fan-out, future Solr query, every external service —
needs an explicit resilience strategy (timeout, retry, circuit breaker,
bulkhead, or a deliberate "no protection needed" exemption). Wiring
resilience into the kernel's vocabulary first means subsequent milestones,
PRs, and feature work can be evaluated against it. A PR that introduces a
new external collaborator without considering its resilience boundary
should be rejected.

---

## Architectural decisions (declarative, do not re-derive)

**Resilience facade lives in the kernel; Resilience4j is an adapter.** The
`Resilience` facade ships in `kernels/framework` as a small set of
interfaces (`Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead`) and a marker
annotation `@Resilient` for declarative use. The default in-process
implementation is a no-op suitable for unit tests. The production
implementation lives in `adapters/resilience-resilience4j/` and bridges
the facade to `io.github.resilience4j.*`. Domain `*-core` code references
the kernel facade only; it never imports Resilience4j directly. The same
boundary discipline that protects core from Spring also protects it from
Resilience4j — only one adapter knows the vendor.

**`DomainId` becomes an open interface.** The kernel ships `interface
DomainId extends ValueObject { String value(); }` with no `permits`
clause, no nested records, and no `of(String)` factory. Each domain's
`*-api` module ships its own subtype: `domains/plants/plants-api`
contributes `PlantsDomain`, `domains/chemistry/chemistry-api` contributes
`ChemistryDomain`, etc. The catalog's assembly validates uniqueness of
`value()` across registered DomainIds at startup and fails fast on
collision; that preserves the bounded-cardinality property that
originally motivated the sealed type, by a different mechanism.

**Atlas renames to *catalog*.** The kernel's javadoc already speaks of
itself as a catalog ("the catalog's growth signal"); "atlas" was a
package-name accident. The rename is a one-shot mechanical change:
`kernels/atlas/` → `kernels/catalog/`, `kernels/atlas-inmem/` →
`kernels/catalog-inmem/`, `com.naturalist.atlas.*` →
`com.naturalist.catalog.*`, `Atlas` → `Catalog`, `AtlasContribution` →
`CatalogContribution`, `AtlasAssembly` → `CatalogAssembly`. No semantic
changes; only naming.

**Top-level `apps/` tree.** New sibling to `domains/`, `kernels/`. Hosts
composition-root deployment artifacts (executables with a `main`).
First member: `apps/management-console/` (renamed from
`naturalist-web/console/`). Future members: a sync daemon, an importer,
the scheduled-task runner, a field-guide API. The `naturalist-web`
parent disappears.

**Top-level `adapters/` tree.** New sibling to `domains/`, `kernels/`,
`apps/`. Hosts ports-and-adapters implementations whose third-party
dependencies are too heavy or too vendor-specific to live in `kernels/`.
First members: `adapters/spring-runtime/` (DI bridge),
`adapters/resilience-resilience4j/` (resilience bridge), and eventually
`adapters/catalog-solr/` (production catalog backend). Per-domain
repository adapters (e.g., `domains/soil/soil-repository-rdms/`) stay
where they are — they are owned by the domain whose data they persist,
not by an app's composition root.

**Marker annotations gate Spring out of core.** `kernels/framework`
ships a runtime-retention `@DomainService` marker (and possibly a small
family — `@CatalogContribution`, `@DomainReference`, `@DomainLinker` —
deferred until the constructor signature alone proves insufficient for
disambiguation at scan time). The marker has no third-party
meta-annotations. `adapters/spring-runtime/` ships an
`ImportBeanDefinitionRegistrar` that scans for `@DomainService` and
registers each match with Spring's `BeanDefinitionRegistry`. Domain
`*-core` classes carry the marker; Spring instantiates them; core never
imports Spring. If a non-Spring runtime ever lands (Guice, Helidon,
plain `main()`), `adapters/runtime-<x>/` is the only module that
changes.

**Resilience compliance is a PR-level gate.** Every PR introducing or
modifying a cross-boundary call must declare its resilience strategy.
Acceptable declarations: `@Resilient(...)` annotation on the call site
or its enclosing service; an explicit code review note recording an
exemption ("read-only kernel call, no remote I/O, exempt"); or a
documented compensation pattern. The gate is enforced by reviewer
discipline first, and by an ArchUnit test in M9 that requires services
with external collaborators to declare a strategy or annotate as
`@ResilienceExempt`. PRs without this consideration should be rejected.

---

## Module layout (target end state)

```
naturalist/
  kernels/
    framework/                    — + @DomainService, + Resilience facade
    framework-test/
    field-notes/
    taxonomy/
    catalog/                      ← was atlas
    catalog-inmem/                ← was atlas-inmem (reference adapter, light deps)
  domains/
    identifiers/
    identifiers-test/
    <domain>/
      <domain>-api/               — + <D>Domain record (DomainId subtype)
      <domain>-core/              — @DomainService on services/contributions/providers
      <domain>-repository-test/
      <domain>-repository-rdms/   (where applicable; per-domain stays per-domain)
      <domain>-console/           (where applicable; library, not app)
  apps/                           ← new top-level
    management-console/           ← was naturalist-web/console/
    ...future apps...
  adapters/                       ← new top-level
    spring-runtime/               — DI bridge (scans @DomainService)
    resilience-resilience4j/      — Resilience facade → r4j
    catalog-solr/                 (planned, not in this plan's scope)
    ...future runtime/infra adapters...
```

The `naturalist-web/` parent pom is deleted.

---

## DAG impact

```
kernels/framework             →  Jackson, Commons, Micrometer, JSpecify   (unchanged)
                                  + @DomainService, Resilience facade     (additive)
kernels/catalog               →  framework, identifiers                   (unchanged shape)
kernels/catalog-inmem         →  catalog                                  (unchanged shape)
adapters/spring-runtime       →  framework + spring-context               (new isolation)
adapters/resilience-r4j       →  framework + resilience4j                 (new isolation)
domains/<d>/<d>-api           →  framework, identifiers, field-notes      (unchanged)
                                  + DomainId subtype                      (additive)
domains/<d>/<d>-core          →  its own api + framework                  (unchanged — no Spring, no r4j)
                                  + @DomainService on classes             (additive marker)
apps/management-console       →  everything it composes + spring-boot     (the only place spring-boot lives)
                                  + adapters/spring-runtime               (new dep)
                                  + adapters/resilience-r4j               (new dep)
```

Rules preserved:
1. `*-api` modules depend only on `framework`, `identifiers`, `field-notes`,
   (organism only) `taxonomy`. Nothing else.
2. `*-core` may import another domain's **api** only.
3. No domain or kernel module imports Spring or Resilience4j.
4. Apps depend on adapters; adapters never depend on apps.
5. `kernels/framework` keeps its small third-party set; markers and
   facade interfaces add no transitive deps.

---

## Milestones

Each milestone is sized to fit a focused session. The "Read first" lists
are exhaustive — a fresh session should not need to grep before starting.

### M0 — Plan ratification

**Goal.** This file lands; the team agrees on the architectural
decisions before any code moves.

**Build.** Commit this PLAN. Open a brief discussion thread linking to
it. Resolve any architectural disagreements as edits to the
"Architectural decisions" section before M1 starts.

**Acceptance.** Plan merged on `main`. No code changes.

### M1 — Resilience kernel facade + Resilience4j adapter (PRIORITY)

**Goal.** Establish the resilience vocabulary and its production
implementation before anything else moves. Subsequent milestones — and
all subsequent feature work — can then evaluate themselves against it.

**Read first.**
- `kernels/framework/src/main/java/com/naturalist/observability/Observable.java`
  (the existing facade-with-adapter pattern)
- `docs/adr/rationale/ADR-018-third-party-dependency-policy.md`
- `docs/adr/ADR-017-observability-monitoring-and-validation.md`
- this PLAN.md ("Architectural decisions")

**Build.**
- `kernels/framework/src/main/java/com/naturalist/resilience/`:
  - `Resilience` — facade interface aggregating the four primitives.
  - `Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead` — small per-primitive
    interfaces. Each takes a `Supplier<T>` (or `Runnable`) and returns the
    protected execution.
  - `ResilienceConfig` — value-object configuration record per primitive
    (max attempts, backoff, slow-call thresholds, queue depth).
  - `@Resilient` — runtime-retention annotation taking a config name.
    Pure marker, no Spring meta-annotation.
  - `NoOpResilience` — package-private default that runs the supplier
    unprotected; used in tests and by any composition root that has not
    yet wired the production adapter.
- `adapters/resilience-resilience4j/`:
  - New module under the new `adapters/` tree (creating the tree itself
    is part of M5; for M1, the module sits temporarily at
    `kernels/resilience-resilience4j/` and moves in M5 — or, if M5 ships
    first, it lands directly in `adapters/`. Sequencing decision deferred
    to the executor.)
  - `Resilience4jResilience` — bridges the facade to
    `io.github.resilience4j.retry.*`, `.circuitbreaker.*`, `.timelimiter.*`,
    `.bulkhead.*`. Reads `ResilienceConfig` records and constructs the
    corresponding r4j primitives.
  - Unit tests covering: retry executes N attempts on transient failure;
    timeout fires after configured duration; circuit breaker opens after
    threshold; bulkhead rejects beyond capacity. All tests use the
    adapter directly — no Spring.
- `kernels/framework`'s `pom.xml` does NOT add Resilience4j. The facade
  is implementation-free at the kernel level.
- A short CONTRIBUTING-style note in `docs/` that captures the PR-level
  expectation: "every cross-boundary call must declare resilience or
  carry an explicit exemption."

**Acceptance.** The facade compiles in `kernels/framework` with no new
third-party deps. The adapter passes its own tests. A worked example in
the adapter's tests demonstrates wrapping a synthetic flaky `Supplier`
with retry+timeout+circuit-breaker via the facade.

**Status: Shipped.** Facade in `kernels/framework/src/main/java/com/naturalist/resilience/`
(`Resilience`, `Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead`,
`ResilienceConfig` sealed interface with four nested config records,
`@Resilient`, `@ResilienceExempt`, package-private `NoOpResilience`).
Adapter at `kernels/resilience-resilience4j/` (will move to
`adapters/resilience-resilience4j/` in M5 per the executor sequencing
note above). Tests cover each primitive plus a composed
retry+timeout+circuit-breaker worked example. Policy note at
`docs/resilience-policy.md`.

### M2 — Atlas → Catalog rename

**Goal.** Mechanical rename. Zero semantic change. Done in one
session because half-completed renames are toxic.

**Read first.**
- `kernels/atlas/PLAN.md` (the entire atlas plan — this rename is its
  natural conclusion)
- `kernels/atlas/src/main/java/com/naturalist/atlas/` (every file)
- `kernels/atlas-inmem/src/main/java/com/naturalist/atlas/inmem/` (every file)
- All grep hits for `atlas` (case-insensitive) across the repo.

**Build.**
- Directory rename: `kernels/atlas/` → `kernels/catalog/`,
  `kernels/atlas-inmem/` → `kernels/catalog-inmem/`.
- Package rename: `com.naturalist.atlas` → `com.naturalist.catalog`,
  `com.naturalist.atlas.inmem` → `com.naturalist.catalog.inmem`.
- Class rename: `Atlas` → `Catalog`, `AtlasContribution` →
  `CatalogContribution`, `AtlasAssembly` → `CatalogAssembly`,
  `InMemoryAtlas` → `InMemoryCatalog`, `AtlasConfiguration` →
  `CatalogConfiguration`, `UnresolvedSearchObservation` (no rename;
  already domain-neutral), `UnresolvedReferenceObservation` (no rename).
- File rename for every type. Sub-package renames (`plants/atlas/` →
  `plants/catalog/` in domain core modules; `console/atlas/` →
  `console/catalog/` in console modules).
- artifactId rename: `atlas` → `catalog`, `atlas-inmem` → `catalog-inmem`.
- Update `kernels/pom.xml`, root `pom.xml` `dependencyManagement`, every
  consumer's `pom.xml`.
- Update all CLAUDE.md and PLAN.md references. Move
  `kernels/atlas/PLAN.md` → `kernels/catalog/PLAN.md`; add a one-line
  redirect at the old path (already a `PLAN-redirect.md` precedent).

**Acceptance.** `mvn clean verify` passes. `grep -rin atlas` returns
only intentional references (changelog, ADR history). The dev console
boots; the worked example in `kernels/catalog/PLAN.md` (the resolution
of `Aristolochia californica`) still works end to end.

**Status: Shipped.** Directories renamed (`kernels/catalog/`,
`kernels/catalog-inmem/`); packages renamed (`com.naturalist.catalog`,
`com.naturalist.catalog.inmem`); types renamed (`Atlas` → `Catalog`,
`AtlasContribution` → `CatalogContribution`, `AtlasAssembly` →
`CatalogAssembly`, `InMemoryAtlas` → `InMemoryCatalog`,
`AtlasConfiguration` → `CatalogConfiguration`,
`PlantAtlasContribution` → `PlantCatalogContribution`); per-domain
sub-packages renamed (`plants/catalog/`, `console/catalog/`);
artifactIds updated (`catalog`, `catalog-inmem`); jte templates,
javadoc, and consumer wiring follow. Remaining `atlas` references
appear only in this plan's narrative history and in
`kernels/catalog/PLAN.md` / `PLAN-redirect.md`'s historical record.

### M3 — Open `DomainId`

**Goal.** Move domain identity from a sealed kernel enum to per-domain
subtypes. Preserve the bounded-cardinality property via assembly-time
validation.

**Read first.**
- `kernels/catalog/src/main/java/com/naturalist/catalog/DomainId.java`
  (post-M2)
- `kernels/catalog-inmem/src/main/java/com/naturalist/catalog/inmem/InMemoryCatalog.java`
- Every per-domain file referencing `DomainId.Plants`,
  `DomainId.Chemistry`, `DomainId.Insects` (grep before starting).
- This PLAN.md ("Open DomainId" decision).

**Build.**
- `kernels/catalog/DomainId.java` becomes a non-sealed interface:
  `String value()` and the default no-op `invariants()`. Nested records
  removed. `of(String)` factory removed.
- Each `domains/<d>/<d>-api/` ships a record:
  - `domains/plants/plants-api/.../PlantsDomain.java` —
    `record PlantsDomain() implements DomainId { value() = "plants"; }`
  - `domains/chemistry/chemistry-api/.../ChemistryDomain.java`
  - `domains/insects/insects-api/.../InsectsDomain.java`
  - (Subtypes for the other scaffolded domains are added when those
    domains land their first catalog contribution; do not preemptively
    create empty domain subtypes for modules that do not yet contribute.)
- `CatalogAssembly.from(...)` validates uniqueness of `domain.value()`
  across all registered `CatalogContribution`s and `EntityReferences`.
  Duplicate slug throws `IllegalArgumentException` at assembly time.
- Every consumer site that referenced `new DomainId.Plants()` updates to
  the new per-domain record import.
- Removed: the `DomainIdTest` suite's exhaustive-switch assertions.
  Added: an assembly test asserting that two contributions with the same
  slug fail fast.

**Acceptance.** `mvn clean verify` passes. The catalog assembly rejects
duplicate slugs. No file under `kernels/` mentions a domain by name.
ADR follow-up: append a note to ADR-022 (or open ADR-023) recording the
shift from sealed to open `DomainId` and the slug-uniqueness invariant.

**Status: Shipped.** `kernels/catalog/.../DomainId.java` is now a non-sealed
interface — nested `Plants`/`Chemistry`/`Insects` records and the
`of(String)` factory are gone, javadoc rewritten to describe the
assembly-time slug-uniqueness invariant. Per-domain subtypes contributed by
their api modules: `domains/plants/plants-api/.../PlantsDomain`,
`domains/chemistry/chemistry-api/.../ChemistryDomain`,
`domains/insects/insects-api/.../InsectsDomain` (each
`*-api` pom now depends on `kernels/catalog`).
`CatalogAssembly.from(...)` walks every contribution's and provider's
`domain()` and throws `IllegalArgumentException` when two distinct
`DomainId` instances share a `value()`; same-instance reuse across
multiple contributions/providers is the normal case and is allowed.
Consumer call sites (`PlantCatalogContribution`,
`PlantCompoundReferences`, `BackReferencesViewModel`, `SearchController`,
all four affected tests) use the per-domain records. Kernel-level
`DomainIdTest`'s exhaustive-switch suite is removed; the new behaviour is
covered by three slug-uniqueness tests in `InMemoryCatalogTest` plus
test-local `DomainId` records in the surviving kernel tests so the
catalog kernel still compiles and tests against no production domain api.
Formal ADR (slug uniqueness via assembly validation) lands in M10.

### M4 — `apps/` tree introduction; `naturalist-web/console/` moves

**Goal.** Hoist the management console into a first-class apps tree.
Disambiguate its name from per-domain console contribution libraries.

**Read first.**
- `naturalist-web/pom.xml`
- `naturalist-web/console/pom.xml`
- `naturalist-web/console/src/main/java/com/naturalist/console/`
  (sample of the tree structure)
- Root `pom.xml` modules section.

**Build.**
- Create `apps/` directory at the repo root with a parent `pom.xml`
  (artifactId `naturalist-apps`, packaging `pom`).
- Move `naturalist-web/console/` → `apps/management-console/`. Update
  the moved module's artifactId from `console` to `management-console`.
  The produced fat jar becomes `management-console-1.0.0-SNAPSHOT.jar`.
- Delete `naturalist-web/` (empty parent after the move).
- Update root `pom.xml` modules: remove `naturalist-web`, add `apps`.
- Update root `CLAUDE.md` module-layout diagram.
- Sanity sweep: `find . -name pom.xml -exec grep -H '<artifactId>' {} \;`
  to confirm no artifactId collisions; `unzip -l` on the first
  repackaged fat jar to confirm no `application*.yml` is shipped from a
  library module (only `apps/management-console/` should ship it).

**Acceptance.** `mvn clean verify` passes. The console boots from
`apps/management-console/`. The fat jar's `BOOT-INF/classes/` contains
only the app's own resources; library jars sit independently in
`BOOT-INF/lib/` with unique filenames.

**Status: Shipped.** `apps/` parent pom landed (`naturalist-apps`,
packaging `pom`, sole child `management-console`). `naturalist-web/console/`
moved via `git mv` to `apps/management-console/` (history preserved); the
moved pom now declares parent `naturalist-apps` and artifactId
`management-console`, name `apps :: management-console`. The empty
`naturalist-web/` parent pom was deleted. Root `pom.xml` modules now list
`apps`, `domains`, `kernels` (alphabetical); the `WEB :: console`
dependencyManagement entry became `APPS :: management-console`.
`JteConfiguration` scans `apps/*/src/main/jte` instead of
`naturalist-web/*`; the three per-domain `TestTemplateEngine`s
(chemistry, insects, plants consoles) follow suit. README.md and the
moved `apps/management-console/README.md` reflect the new path; the root
`CLAUDE.md` module-layout block lists `apps/management-console/`. Build
verification deferred to the user (per local convention). Remaining
`naturalist-web` references appear only in this plan's narrative history
and in `kernels/catalog/PLAN.md` / `PLAN-redirect.md`'s historical record.

### M5 — `adapters/` tree formalized

**Goal.** Stand up the new top-level neighborhood for ports-and-adapters
implementations carrying heavy or vendor-specific dependencies.

**Read first.**
- This PLAN.md ("Architectural decisions" — adapters tree).
- M1 output (the resilience adapter, wherever it temporarily landed).
- M4 output (the apps tree, which is the symmetric sibling).

**Build.**
- Create `adapters/` directory at the repo root with a parent `pom.xml`
  (artifactId `naturalist-adapters`, packaging `pom`).
- If M1's `resilience-resilience4j` landed in `kernels/`, move it now to
  `adapters/resilience-resilience4j/`. Update its `pom.xml`'s parent
  reference and any consumer (only `apps/management-console/`).
- Update root `pom.xml` modules: add `adapters`.
- Update root `CLAUDE.md`.
- Document the placement rule in the new `adapters/CLAUDE.md`: heavy
  third-party deps live here; per-domain repository adapters
  (`domains/<d>/<d>-repository-rdms/`) stay with their domain because
  they are owned by the domain's data, not the app's composition.

**Acceptance.** `mvn clean verify` passes. `adapters/` is registered;
`adapters/resilience-resilience4j/` lives there; subsequent adapters
have a documented home.

**Status: Shipped.** New `adapters/` parent pom landed
(`naturalist-adapters`, packaging `pom`, sole child
`resilience-resilience4j`); the parent owns the shared
`resilience4j-version`, `junit-version`, and `assertj-version`
properties so adapter modules inherit them without leaning on the
kernels parent. `kernels/resilience-resilience4j/` moved via `git mv`
to `adapters/resilience-resilience4j/` (history preserved); the moved
pom now declares parent `naturalist-adapters`, artifactId
`resilience-resilience4j`, name `adapters :: resilience-resilience4j`,
and a refreshed description that drops the temporary-placement note.
`kernels/pom.xml` no longer lists the module and no longer carries
`resilience4j-version`. Root `pom.xml` modules now list `adapters`,
`apps`, `domains`, `kernels` (alphabetical); the
`resilience-resilience4j` dependencyManagement entry moved out of the
KERNELS block into a new `<!-- ADAPTERS -->` block. `apps/management-console`
does not yet depend on the adapter (that wiring lands in M8). New
`adapters/CLAUDE.md` documents the placement rule (heavy/vendor
dependencies only; per-domain repository adapters stay with their
domain; no `main`). Root `CLAUDE.md` module-layout block now lists
`adapters/` alongside `kernels/`, `domains/`, `apps/`. Build
verification deferred to the user (per local convention).

### M6 — Marker annotations + `adapters/spring-runtime/` (pilot)

**Goal.** Establish the annotation-driven DI bridge with one pilot
domain wired through it. Prove the boundary holds before rolling
across all domains.

**Read first.**
- `kernels/framework/src/main/java/com/naturalist/` (sample
  to confirm the marker annotation has a natural home alongside other
  framework types).
- `apps/management-console/src/main/java/com/naturalist/console/catalog/CatalogConfiguration.java`
  (post-M2-M4 — the manual `@Bean` wiring this milestone replaces).
- One domain's contribution and provider wiring as the pilot:
  `domains/plants/plants-core/src/main/java/com/naturalist/plants/catalog/`
  (post-M2 path).
- Spring's `ClassPathScanningCandidateComponentProvider` and
  `ImportBeanDefinitionRegistrar` javadoc.

**Build.**
- `kernels/framework/src/main/java/com/naturalist/framework/DomainService.java`:
  ```java
  @Retention(RUNTIME) @Target(TYPE)
  public @interface DomainService { }
  ```
  No meta-annotations. No third-party imports.
- `adapters/spring-runtime/`:
  - New module. Depends on `kernels/framework` and `spring-context`
    (NOT `spring-boot`).
  - `DomainServiceScan` — `@Configuration` implementing
    `ImportBeanDefinitionRegistrar`. Runs
    `ClassPathScanningCandidateComponentProvider` filtered by
    `AnnotationTypeFilter(DomainService.class)`, base package
    `com.naturalist`. Registers each candidate with the
    `BeanDefinitionRegistry`.
- Pilot wiring: annotate `PlantCatalogContribution` and
  `PlantCompoundReferences` with `@DomainService`. The
  `PlantsDomain` record (from M3) gets `@DomainService` too.
- `apps/management-console/`'s `CatalogConfiguration` becomes:
  ```java
  @Configuration
  @Import(DomainServiceScan.class)
  public class CatalogConfiguration {
      @Bean Catalog catalog(List<DomainId> domains,
                            List<CatalogContribution> contributions,
                            List<EntityReferences<?>> providers) {
          return CatalogAssembly.from(domains, contributions, providers);
      }
  }
  ```
- Integration test: boot the Spring context, assert `Catalog` bean is
  present, assert it resolves an Aristolochia californica alias.

**Acceptance.** Pilot domain (plants) is wired entirely through
`@DomainService`. The plants `*-core` module imports nothing from
Spring. The console boots with the same behaviour as before. Other
domains' wiring remains manual until M7.

### M7 — Roll `@DomainService` across all domains

**Goal.** Replace every remaining manual `@Bean` declaration of a
catalog contribution / provider / linker / DomainId with the marker.

**Read first.**
- `apps/management-console/src/main/java/com/naturalist/console/catalog/CatalogConfiguration.java`
  (post-M6) and any other configuration class that hand-wires
  domain-owned beans.
- Each domain's `*-core/src/main/java/com/naturalist/<d>/catalog/`
  package.
- M6 output (the established pattern).

**Build.**
- Annotate each domain's `CatalogContribution`, `EntityReferences`,
  `EntityRefLinker` (where present), and `DomainId` subtype with
  `@DomainService`.
- Strip the now-unnecessary `@Bean` declarations from
  `CatalogConfiguration` and any sibling per-domain configurations.
- Verify each domain `*-core` module's `pom.xml` does NOT depend on
  Spring. If any does, that's a leftover from earlier wiring and gets
  removed.
- Per-domain integration test boots a minimal Spring context with only
  that domain's `*-core` on the classpath plus
  `adapters/spring-runtime`, asserting the domain's beans are
  discovered.

**Acceptance.** No domain `*-core` module depends on Spring. The
console boots and operates identically. `grep -r '@Bean.*Domain\|@Bean.*Contribution\|@Bean.*References'`
in `apps/management-console/` returns only the assembly bean
(`@Bean Catalog catalog(...)`).

### M8 — Apply `@Resilient` to existing cross-boundary calls

**Goal.** Backfill the resilience facade onto the cross-boundary calls
that exist today. This is the bridge from "facade exists" (M1) to
"resilience is enforced" (M9).

**Read first.**
- M1 output (`Resilience` facade and adapter).
- An audit pass: every call site in `*-core` that crosses a boundary
  the facade should protect. Initial candidates:
  - Catalog fan-out to `EntityReferences.referencesTo(...)` — each
    provider call is a candidate for individual timeout + retry.
  - Repository reads in `*-repository-rdms/` (currently only
    `soil-repository-rdms/`).
  - The console's HTTP controllers, where downstream domain calls form
    a request boundary.

**Build.**
- For each identified call site, either:
  - Wrap with `@Resilient(name = "...")` (or programmatic `Resilience`
    facade call) and document the chosen primitive set, or
  - Annotate with `@ResilienceExempt(reason = "...")` if the call is
    in-process, side-effect-free, and not subject to timeout (the
    annotation, also in `kernels/framework`, exists precisely so M9's
    enforcement test has a deliberate exemption path).
- Configuration for each named resilience strategy lives in
  `apps/management-console/`'s `application.yml` (or a sibling
  `resilience.yml`), keyed by the names referenced in `@Resilient`.

**Acceptance.** Every cross-boundary call site in the codebase carries
either `@Resilient` or `@ResilienceExempt`. The console's startup logs
list every registered resilience strategy by name. A deliberate
synthetic failure injected into the catalog fan-out triggers the
expected retry-then-circuit-breaker behaviour observable in the
console's metrics.

### M9 — Resilience compliance gate

**Goal.** Make resilience consideration a build-time requirement, not
a reviewer-discipline-only requirement.

**Read first.**
- M1, M8 outputs.
- Existing ArchUnit tests in the project (search
  `com.tngtech.archunit`) — pattern to follow.

**Build.**
- New ArchUnit test in a test-scope module the build runs:
  `ResilienceComplianceTest`. Discovers every concrete class in `*-core`
  and `apps/*` modules that has a constructor parameter implementing a
  port interface (heuristic: parameter type is an interface defined in
  another module, OR the class is annotated with `@DomainService` and
  takes any constructor parameter that crosses a domain boundary).
  Asserts that the class — or each of its public methods that uses such
  a collaborator — is annotated with `@Resilient` or `@ResilienceExempt`.
- The test's failure message names the offending class and points to
  this PLAN's "Resilience compliance" section for guidance.
- A short CONTRIBUTING checklist update: every PR description should
  answer the question "What resilience strategy does this introduce or
  rely on?" (or check the `@ResilienceExempt` box with reason).
- The PR template (if one exists) gets a resilience checkbox added.

**Acceptance.** The compliance test passes on the current codebase
post-M8. Adding a new service class with an external collaborator and
no resilience annotation fails the build with a message identifying the
class. Annotating with `@ResilienceExempt(reason = "...")` makes the
build pass; the rationale is captured in the source.

### M10 — ADR sweep

**Goal.** Record the architectural decisions in permanent ADRs so
future contributors find them in `docs/adr/` rather than re-deriving
them from this plan.

**Read first.**
- `docs/adr/README.md`
- `docs/adr/ADR-018-third-party-dependency-policy.md` (closest
  philosophical neighbour for the resilience and DI boundary
  decisions).
- This PLAN.md (the "Architectural decisions" section is the source
  material).

**Build.**
- ADR-023: "Open DomainId — slug uniqueness via assembly validation."
  Records the shift from sealed kernel enum to per-domain open subtypes
  and the mechanism that preserves bounded metric tag cardinality.
- ADR-024: "Apps and adapters trees." Records the placement rule —
  composition roots in `apps/`, heavy-dep adapter implementations in
  `adapters/`, per-domain repository adapters stay with their domain.
- ADR-025: "DI via marker annotations." Records `@DomainService` and
  the `adapters/spring-runtime/` boundary; explains why core stays
  Spring-free and how a future runtime swap would work.
- ADR-026: "Resilience as a first-order concern." Records the
  `Resilience` facade, the `@Resilient` / `@ResilienceExempt`
  annotations, the PR-level expectation, and the M9 compliance test.
- Update `kernels/CLAUDE.md` to mention the renamed `catalog` kernel
  and the new resilience facade alongside the existing kernels.
- Update root `CLAUDE.md` module-layout section to include `apps/` and
  `adapters/`.
- Add `apps/CLAUDE.md` and `adapters/CLAUDE.md` documenting the
  placement and dependency rules for each tree.

**Acceptance.** ADRs merged. Root and per-tree CLAUDE.md files
reflect the new structure. A fresh contributor reading
`docs/adr/README.md` can locate the decisions captured in this plan.

---

## How to resume across sessions

A fresh session picking up this work should:

1. Read this PLAN.md in full (it is sized to fit easily).
2. Run `git log --oneline docs/plans/runtime-architecture-refactor.md`
   and the relevant per-tree paths (`kernels/catalog/`, `apps/`,
   `adapters/`) to see which milestones have shipped.
3. Look at the `Read first` list of the next pending milestone and
   read only those files.
4. Do the milestone. Update the milestone's status in this file as
   part of the same commit. Do not edit milestones beyond the one
   being executed — if a discovery during work changes a later
   milestone, append a note under the relevant milestone, do not
   rewrite it.

If a milestone proves bigger than expected mid-session, split it:
leave the original milestone partially done, add a new milestone
immediately after for the leftover, and surface the split in the
commit message.

---

## Open questions

Worth deciding before M6, but not blocking earlier work:

- **Marker annotation granularity.** Does the kernel ship one
  `@DomainService` marker, or a small family
  (`@CatalogContribution`, `@DomainReference`, `@DomainLinker`, plus
  a generic `@DomainService`)? **Tentative pick:** start with one;
  split only when the scanner needs to filter (e.g., a stripped-down
  test composition that wants contributions but not providers).

- **Scan scope.** `com.naturalist.**` is the obvious base package.
  When/if third-party plugins ship, switch to a `META-INF`-listed
  registration model (Spring Boot's `AutoConfiguration.imports`
  pattern, or a custom `ServiceLoader`-style file). **Tentative
  pick:** broad-scan now; narrow when plugins land.

- **Resilience config location.** Per-app YAML, or per-domain
  default config in `*-core` overridden by app YAML? The latter
  gives domains opinionated defaults ("a catalog fan-out should
  time out at 200ms") that apps inherit. **Tentative pick:**
  per-app YAML in M1/M8; revisit if multiple apps duplicate the
  same per-domain config.

- **Sequencing of M1 vs M5.** M1 ships the resilience adapter; M5
  creates the `adapters/` tree. Strictly, M1 needs M5. In practice,
  the adapter can land temporarily in `kernels/` and move in M5, or
  M5 can ship before M1 (creating the tree empty). Either path is
  fine; the executor of M1 picks based on what feels less awkward in
  the moment.

---

## Out of scope

These are explicitly *not* part of this effort. Each is a worthy
follow-up:

- `adapters/catalog-solr/` — the production Solr-backed catalog
  adapter. Its placement is decided by this plan; its
  implementation is not.
- A second runtime adapter (`adapters/runtime-guice/`,
  `adapters/runtime-helidon/`). The boundary that makes this
  possible lands in M6/M7; actually building one is future work.
- Resilience strategy tuning. M1/M8 ship the facade and apply it;
  picking the right thresholds for each named strategy is an
  operational concern that follows production deployment.
- Resilience observability. Resilience4j ships micrometer
  integration; wiring it through the kernel's `Observable`
  pipeline is adjacent work that ADR-026 should mention as a
  follow-up.
- Per-domain repository rdms adapter rollout. The decision
  preserves them per-domain; growing them to more domains is
  domain work, not refactor work.
