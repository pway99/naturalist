# Controller De-Fork + ArchUnit Enforcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Put all 9 console controllers on the shared Spring graph by constructor-injecting the already-discoverable query/command beans, drop the private `XTestContext.create(NaturalistDatabase.create())` forks, and lock the result with ArchUnit so it cannot regress.

**Architecture:** The app already discovers `@DomainService` beans via `DomainServiceScan` and publishes one shared `NaturalistDatabase` bean (`adapters/spring-test-data/TestDataConfiguration`). Each controller today forks a *private* in-memory graph in its constructor. This plan (1) annotates the small transitive set of namespace query/command impls that are not yet beans so every controller's ctor dependency resolves, (2) rewrites each controller ctor to inject those beans and assemble only its own presentation helpers locally, (3) de-forks the ~40 `new *TestEntitySource(NaturalistDatabase.create())` test sites onto `NaturalistTestExtension` + `getNamed`, and (4) lands ArchUnit rules — central (main-code, in `apps/management-console`) plus per-module (test-code, in the 4 affected modules) — that go green only once every fork is gone.

**Tech Stack:** Java 21, Spring Boot (management-console), custom `@DomainService` marker + `DomainServiceScan`, JUnit 5, ArchUnit (`com.tngtech.archunit:archunit-junit5`), Maven multi-module.

## Global Constraints

- **Trunk-based, no push.** Stay on `main`. The user pushes. Never `git push`.
- **Scoped commits only.** Never `git add -A`. Stage only the files a task changed. Leave the untracked `docs/plans/2026-08-21-n-plus-one-*` files alone.
- **Commit footer** (every commit):
  `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`
- **Subagents stage only; the controller session stops for user review before each commit.** Core/kernel changes are reviewed before commit (this plan's core edits are marker-only — see below).
- **Core edits are `@DomainService` markers only.** No ctor signature or behavior changes to any `<domain>-core` impl. If a task appears to need more than a marker, STOP and surface it.
- **No kernel edits.** `EolClientMock` (external-authorities) and `NoOpTextGenerationService` (kernels) are wired via a composition-root `@Bean` / kept `new`ed locally — never annotated.
- **Mocks stay unannotated.** Each `<Entity>RepositoryRdms` is the sole repository bean; annotating a mock whose rdms is also annotated causes `NoUniqueBeanDefinitionException`.
- **Do not remove `@Primary` from `CompositeEntityRefLinker`** — it is what lets 4 `EntityRefLinker` impls coexist.
- **PR/commit size:** one domain per commit (ADR-019, ≤400 lines meaningful diff). The central ArchUnit lock is its own final commit.
- **Build:** scoped `mvn -pl <module> -am test` during a task; full `mvn verify` from repo root at the final gate. If a kernel signature ever changes, a clean `mvn install` is required (not applicable here — no kernel changes planned).

---

## Reference facts (verified — do not re-derive)

**How discovery works:** `@DomainService` (`kernels/framework/.../infrastructure/DomainService.java`) is a bare `TYPE` marker. `DomainServiceScan` (`adapters/spring-runtime/.../DomainServiceScan.java`) classpath-scans `com.naturalist`, registers each annotated class by fully-qualified bean name, injects by type. Package-private impl classes/ctors are fine. The marker is inert outside a Spring scan, so `TestContext` classes that `new` these impls keep working unchanged.

**The shared DB bean:** `TestDataConfiguration.naturalistDatabase()` returns `NaturalistDatabase.create()`. Every `<Entity>RepositoryRdms` takes `NaturalistDatabase` in its ctor and resolves its source via `getNamed` — so the whole bean graph bottoms out on this one shared registry.

**Reference DI'd controller:** `apps/management-console/.../search/SearchController.java` — `SearchController(Catalog, EntityRefLinker)`, forks nothing.

**Collision facts:** No query/command interface has two impls across the migrating domains. Four `EntityRefLinker` impls coexist only because `CompositeEntityRefLinker` is `@Primary` over `List<EntityRefLinker>`. There is currently **no** `ExternalAuthority` bean anywhere — adding one EOL bean is safe and sole.

**The annotation worklist (all `<domain>-core`, marker-only):**

| Domain | Classes to add `@DomainService` |
|---|---|
| insects | `InsectQueryImpl`, `InsectImageQueryImpl`, `InsectFunctionalRoleQueryImpl`, `InsectObservationQueryImpl`, `InsectCommandImpl`, `InsectObservationCommandImpl`, `InsectFeatureCommandImpl`, `InsectFeatureAssignmentCommandImpl` (8 — `InsectImageCommandImpl` is ALREADY a bean, do not touch) |
| library (for insects' injected `LibraryCommand`) | `LibraryCommandImpl`, `CitationCommandImpl`, `CitationAssociationCommandImpl`, `CitationAttributionTransaction` |
| plants | `PlantQueryImpl`, `PlantFeatureQueryImpl`, `PlantAncestryResolver` |
| garden | `PlantedZoneFactory`, **`PlantedZoneQueryImpl`** (+ update its class javadoc — it currently states "no `@DomainService`… factory is not a bean", which no longer holds) |
| soil | `SoilProfileFactory`, **`SoilProfileQueryImpl`** (+ update its class javadoc, same reason) |
| chemistry | none |

> **Audit correction (2026-08-22, during execution):** `PlantedZoneQueryImpl` and `SoilProfileQueryImpl` are factory-backed and were deliberately left unannotated ("wired manually in the context") — so `PlantedZoneQuery` / `SoilProfileQuery` are NOT beans yet. Annotating only the factory is insufficient; the query impl must be annotated too. `InsectImageCommandImpl` is already `@DomainService`. Verify real (line-start) `@DomainService`, not javadoc mentions of it.

**Not annotated (built locally by the insects controller, per "assemble locally"):** `InsectCatalogIdentificationTransaction`, `InsectAddPhotoTransaction`, `InsectAddPhotoCommand`, `InsectIdentificationCommand`. The controller `new`s these from injected `InsectCommand`/`InsectQuery`/`LibraryCommand`/`ExternalAuthority`/`VisionService`. `NoOpTextGenerationService` stays `new`ed inline.

**Test-site de-fork footprint (`new *TestEntitySource(NaturalistDatabase.create())`) — 4 modules, 14 files:**
- chemistry-console: `ChemistryDetailTemplateTest`, `ChemistryListTemplateTest`
- chemistry-repository-test: `ElementCatalogDataTest`
- insects-console: `InsectsDetailTemplateTest`, `InsectsFamiliesTemplateTest`, `InsectsGeneraTemplateTest`, `InsectsGuildTemplateTest`, `InsectsListTemplateTest`, `InsectsOrdersTemplateTest`
- plants-console: `PhytochemistryDetailTemplateTest`, `PhytochemistryListTemplateTest`, `PlantsDetailTemplateTest`, `PlantsListTemplateTest`, `render/DescriptionRendererTest`

The de-fork transform in each: replace the `NaturalistDatabase database = NaturalistDatabase.create();` local + `new <X>TestEntitySource(database)` with a static extension and `getNamed`:
```java
@org.junit.jupiter.api.extension.RegisterExtension
static com.naturalist.data.NaturalistTestExtension db =
        com.naturalist.data.NaturalistTestExtension.create();
// ...
var source = db.getNamed(<X>TestEntitySource.class);   // was: new <X>TestEntitySource(database)
```
(`getNamed` constructs the source reflectively, so ArchUnit's `new *TestEntitySource` rule stays satisfied.)

**ArchUnit whitelist (rule A, main-code `NaturalistDatabase.create()` callers that remain legitimate after de-fork):** `com.naturalist.data.TestEntitySourceTest` (framework-test main — reusable contract base) and `com.naturalist.spring.data.TestDataConfiguration`. These are the ONLY legitimate main-code callers.

**App-level safety net:** `apps/management-console/src/test/.../*WebMvcTest.java` are `@SpringBootTest` — booting any one of them starts the whole context, so a controller whose ctor cannot be satisfied fails ALL of them. That is the wiring gate for every controller (not just insects). After de-fork these run on the shared graph; data content is identical (same JSON via `getNamed`).

---

## File Structure

**Controllers (main — de-fork):**
- `domains/chemistry/chemistry-console/.../ChemistryController.java`
- `domains/library/library-console/.../citation/CitationsController.java`, `.../clade/CladesController.java`, `.../concept/ConceptsController.java`, `.../glossary/GlossaryController.java`
- `domains/garden/garden-console/.../GardenController.java`
- `domains/soil/soil-console/.../SoilsController.java`
- `domains/plants/plants-console/.../PlantsController.java`
- `domains/insects/insects-console/.../InsectsController.java`

**Core impls (main — add `@DomainService` marker):** per worklist above.

**Composition root (management-console — new):**
- `apps/management-console/src/main/java/com/naturalist/console/IdentificationConfiguration.java` (the `ExternalAuthority` EOL `@Bean`)
- `apps/management-console/pom.xml` (add `eol-client-mock` dependency)

**ArchUnit (test):**
- `apps/management-console/src/test/java/com/naturalist/console/architecture/DataForkComplianceTest.java` (central, main-code, 2 rules)
- Per-module test in each of the 4 affected modules: `.../DataForkArchTest.java` + `archunit-junit5` test-scoped dependency in that module's `pom.xml`.

**Test sites (test — de-fork):** the 14 files listed above.

**Poms:**
- `domains/insects/insects-console/pom.xml` — add `insects-core` dep, remove `insects-test-context` dep.
- `apps/management-console/pom.xml` — add `eol-client-mock`.
- 4 module poms — add `archunit-junit5` (test scope).

---

## Task 1: Chemistry controller de-fork (template — zero annotations)

Establishes the controller-de-fork + test-site de-fork + per-module ArchUnit pattern on the simplest domain.

**Files:**
- Modify: `domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/ChemistryController.java`
- Modify (test de-fork): `.../chemistry/console/ChemistryDetailTemplateTest.java`, `.../ChemistryListTemplateTest.java`, `domains/chemistry/chemistry-repository-test/.../element/ElementCatalogDataTest.java`
- Create: `domains/chemistry/chemistry-console/src/test/java/com/naturalist/chemistry/console/DataForkArchTest.java`
- Modify: `domains/chemistry/chemistry-console/pom.xml`, `domains/chemistry/chemistry-repository-test/pom.xml` (add `archunit-junit5` test scope)
- Create: `domains/chemistry/chemistry-repository-test/src/test/java/com/naturalist/chemistry/DataForkArchTest.java`

**Interfaces:**
- Consumes: `CompoundQuery`, `ProductQuery`, `ElementQuery` beans (impls already `@DomainService`); `Catalog`, `EntityRefLinker`, `DepictionRenderer` beans (already injected today).
- Produces: the pattern (extension field + `getNamed`; per-module `DataForkArchTest`) reused by later tasks.

- [ ] **Step 1: Read the current controller and confirm the forked fields**

Read `ChemistryController.java`. Confirm the ctor takes `(DepictionRenderer, Catalog, EntityRefLinker)` and forks `ChemistryTestContext.create(NaturalistDatabase.create())` to obtain `compoundQuery()`, `productQuery()`, `elementQuery()`. Note the exact field names.

- [ ] **Step 2: Rewrite the ctor to inject the three query beans**

Replace the fork with constructor parameters. Remove the `NaturalistDatabase`/`ChemistryTestContext` imports and the fork lines. Result shape:
```java
ChemistryController(DepictionRenderer depictionRenderer,
                    Catalog catalog,
                    EntityRefLinker linker,
                    CompoundQuery compoundQuery,
                    ProductQuery productQuery,
                    ElementQuery elementQuery) {
    this.depictionRenderer = depictionRenderer;
    this.catalog = catalog;
    this.linker = linker;
    this.compoundQuery = compoundQuery;
    this.productQuery = productQuery;
    this.elementQuery = elementQuery;
}
```
Keep all field types exactly as they are today (interface types). Do not change any handler method.

- [ ] **Step 3: Build the module (main)**

Run: `mvn -q -pl domains/chemistry/chemistry-console -am -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: De-fork the chemistry test sites**

In `ChemistryDetailTemplateTest`, `ChemistryListTemplateTest` (chemistry-console) and `ElementCatalogDataTest` (chemistry-repository-test), apply the transform from Reference facts: add a `@RegisterExtension static NaturalistTestExtension db = NaturalistTestExtension.create();` field and replace each `new <X>TestEntitySource(<database>)` with `db.getNamed(<X>TestEntitySource.class)`. Remove now-unused `NaturalistDatabase database = ...` locals and imports.

- [ ] **Step 5: Add the per-module ArchUnit test (chemistry-console)**

Add `archunit-junit5` (test scope) to `chemistry-console/pom.xml` if absent, then create `DataForkArchTest.java`:
```java
package com.naturalist.chemistry.console;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Locks this module off the private-graph fork: a TestEntitySource is only ever
 * acquired through NaturalistDatabase#getNamed, never constructed directly.
 */
@AnalyzeClasses(packagesOf = DataForkArchTest.class)
class DataForkArchTest {
    @ArchTest
    static final ArchRule noDirectTestEntitySourceConstruction =
            noClasses().should().callConstructorWhere(
                    target -> target.getConstructor().getOwner().getSimpleName().endsWith("TestEntitySource"))
                    .because("acquire a TestEntitySource via NaturalistDatabase#getNamed, "
                            + "never `new` — see ADR-001");
}
```
> **Note on `@AnalyzeClasses(packagesOf = ...)`:** by default ArchUnit analyzes the class's package **including test classes** — which is what we want here (the forks are in test code). Do NOT add `ImportOption.DoNotIncludeTests`.

- [ ] **Step 6: Add the per-module ArchUnit test (chemistry-repository-test)**

Add `archunit-junit5` (test scope) to `chemistry-repository-test/pom.xml`, and create `domains/chemistry/chemistry-repository-test/src/test/java/com/naturalist/chemistry/DataForkArchTest.java` with the same rule (package `com.naturalist.chemistry`).

- [ ] **Step 7: Run the chemistry module tests**

Run: `mvn -q -pl domains/chemistry/chemistry-console,domains/chemistry/chemistry-repository-test -am test`
Expected: BUILD SUCCESS, including both `DataForkArchTest`s green.

- [ ] **Step 8: Stage and stop for review, then commit**

Stage only the files changed in this task. Print the commit for the user:
```bash
git add domains/chemistry/chemistry-console domains/chemistry/chemistry-repository-test
git commit -m "$(cat <<'EOF'
refactor(chemistry): inject query beans into ChemistryController; de-fork test sites; lock with ArchUnit

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Library controllers de-fork (4 controllers, zero annotations)

**Files:**
- Modify: `.../library/console/citation/CitationsController.java`, `.../clade/CladesController.java`, `.../concept/ConceptsController.java`, `.../glossary/GlossaryController.java`

**Interfaces:**
- Consumes: `CitationQuery`, `CladeQuery`, `ConceptQuery`, `GlossaryTermQuery` beans (impls already `@DomainService`); `Catalog`, `EntityRefLinker` (CladesController) already injected.

- [ ] **Step 1: Read each of the 4 controllers**

Note each ctor and which `LibraryTestContext.create(NaturalistDatabase.create())` accessor it forks (one query per controller; `CladesController` also injects `Catalog`+`EntityRefLinker` and builds a local `DescriptionRenderer(List.of())`).

- [ ] **Step 2: De-fork `CitationsController`**

Replace the fork with `CitationsController(CitationQuery citationQuery)`; assign the field. Remove `NaturalistDatabase`/`LibraryTestContext` imports.

- [ ] **Step 3: De-fork `ConceptsController`**

`ConceptsController(ConceptQuery conceptQuery)`; keep the local `DescriptionRenderer` field init as-is.

- [ ] **Step 4: De-fork `GlossaryController`**

`GlossaryController(GlossaryTermQuery glossaryTermQuery)`.

- [ ] **Step 5: De-fork `CladesController`**

`CladesController(Catalog catalog, EntityRefLinker linker, CladeQuery cladeQuery)`; keep the local `descriptionRenderer = new DescriptionRenderer(List.of())`.

- [ ] **Step 6: Build the module**

Run: `mvn -q -pl domains/library/library-console -am -DskipTests compile`
Expected: BUILD SUCCESS. (No `new *TestEntitySource` test sites in library-console → no test de-fork, no per-module ArchUnit here.)

- [ ] **Step 7: Run library module tests**

Run: `mvn -q -pl domains/library/library-console -am test`
Expected: BUILD SUCCESS.

- [ ] **Step 8: Stage and stop for review, then commit**

```bash
git add domains/library/library-console
git commit -m "$(cat <<'EOF'
refactor(library): inject query beans into the 4 library console controllers; drop private-graph forks

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Garden controller de-fork (annotate `PlantedZoneFactory`)

**Files:**
- Modify: `domains/garden/garden-core/src/main/java/com/naturalist/garden/PlantedZoneFactory.java` (add marker)
- Modify: `domains/garden/garden-core/src/main/java/com/naturalist/garden/PlantedZoneQueryImpl.java` (add marker + fix javadoc)
- Modify: `domains/garden/garden-console/.../GardenController.java`

**Interfaces:**
- Consumes: `PlantingQuery` (already a bean), `PlantedZoneQuery` (becomes a bean in this task). `PlantedZoneQueryImpl` ctor requires `PlantedZoneFactory`, whose ctor is `(PlantingQuery)` → both resolvable once annotated.

- [ ] **Step 1: Confirm the two ctors resolve**

Read `PlantedZoneFactory.java` and `PlantedZoneQueryImpl.java`. Confirm `PlantedZoneFactory` ctor is `(PlantingQuery)` with `PlantingQueryImpl` a real (line-start) `@DomainService`, and `PlantedZoneQueryImpl` ctor is `(PlantedZoneFactory)`. If either ctor needs anything not already a bean, STOP and surface it.

- [ ] **Step 2: Annotate `PlantedZoneFactory` and `PlantedZoneQueryImpl`**

Add `import com.naturalist.infrastructure.DomainService;` and `@DomainService` on BOTH classes (marker only). On `PlantedZoneQueryImpl`, also update the class javadoc that currently explains why it carries no `@DomainService` (it now does, because `PlantedZoneFactory` is now a bean) — replace that paragraph with a one-line note that it is a factory-backed query bean whose `PlantedZoneFactory` is injected.

- [ ] **Step 3: De-fork `GardenController`**

Replace the `GardenTestContext.create(NaturalistDatabase.create())` fork with `GardenController(PlantingQuery plantingQuery, PlantedZoneQuery plantedZoneQuery)`; assign fields. Remove fork imports.

- [ ] **Step 4: Build**

Run: `mvn -q -pl domains/garden/garden-console -am -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Run garden module tests**

Run: `mvn -q -pl domains/garden/garden-console,domains/garden/garden-core -am test`
Expected: BUILD SUCCESS. (`garden-console`'s test uses `NaturalistDatabase.create()` but not `new *TestEntitySource` → out of scope; leave it.)

- [ ] **Step 6: Stage and stop for review (core marker!), then commit**

Core change is a single `@DomainService` marker — surface it in review.
```bash
git add domains/garden/garden-core domains/garden/garden-console
git commit -m "$(cat <<'EOF'
refactor(garden): inject query beans into GardenController; mark PlantedZoneFactory @DomainService

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Soil controller de-fork (annotate `SoilProfileFactory`)

**Files:**
- Modify: `domains/soil/soil-core/src/main/java/com/naturalist/soil/SoilProfileFactory.java` (add marker)
- Modify: `domains/soil/soil-core/src/main/java/com/naturalist/soil/SoilProfileQueryImpl.java` (add marker + fix javadoc)
- Modify: `domains/soil/soil-console/.../SoilsController.java`

**Interfaces:**
- Consumes: `SoilProfileInfoQuery` (already a bean), `SoilProfileQuery` (becomes a bean in this task); `Catalog`, `EntityRefLinker` (already injected); `GlossaryTermQuery` (library, already a bean). `SoilProfileQueryImpl` ctor requires `SoilProfileFactory` whose ctor takes 6 already-`@DomainService` queries.

- [ ] **Step 1: Confirm the ctors resolve**

Read `SoilProfileFactory.java` and `SoilProfileQueryImpl.java`; confirm all 6 factory ctor args are real (line-start) `@DomainService` query impls (they are: `SoilProfileInfoQueryImpl`, `LabAnalysisInfoQueryImpl`, `NutrientReadingQueryImpl`, `SoilPhysicalCharacteristicsQueryImpl`, `ReportedOptimumQueryImpl`, `ReportedRecommendationQueryImpl`), and `SoilProfileQueryImpl` ctor is `(SoilProfileFactory)`. If not, STOP.

- [ ] **Step 2: Annotate `SoilProfileFactory` and `SoilProfileQueryImpl`**

Add `@DomainService` (+ import) on BOTH classes (marker only). On `SoilProfileQueryImpl`, also update the class javadoc that explains why it carries no `@DomainService` — replace with a one-line note that it is a factory-backed query bean whose `SoilProfileFactory` is injected.

- [ ] **Step 3: De-fork `SoilsController`**

Current ctor `(Catalog, EntityRefLinker)`; it forks `SoilTestContext.create(db)` for `soilProfileInfoQuery()`/`soilProfileQuery()` and `LibraryTestContext.create(db)` for `glossaryTermQuery()` (used to build `GlossaryLinker`), plus `NutrientChemistryLinks.of(catalog, linker)`. New ctor:
```java
SoilsController(Catalog catalog,
                EntityRefLinker linker,
                SoilProfileInfoQuery soilProfileInfoQuery,
                SoilProfileQuery soilProfileQuery,
                GlossaryTermQuery glossaryTermQuery) {
    this.catalog = catalog;
    this.linker = linker;
    this.soilProfileInfoQuery = soilProfileInfoQuery;
    this.soilProfileQuery = soilProfileQuery;
    this.glossaryLinker = GlossaryLinker.of(glossaryTermQuery
            .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content());
    this.chemistryLinks = NutrientChemistryLinks.of(catalog, linker);
}
```
Keep the exact `findPage(...)` / `NutrientChemistryLinks.of(...)` calls the current code uses (copy them verbatim from the forked version). Remove `NaturalistDatabase`/`SoilTestContext`/`LibraryTestContext` imports.

- [ ] **Step 4: Build**

Run: `mvn -q -pl domains/soil/soil-console -am -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Run soil module tests**

Run: `mvn -q -pl domains/soil/soil-console,domains/soil/soil-core -am test`
Expected: BUILD SUCCESS. (`soil-console` test uses `create()` but not `new *TestEntitySource` → out of scope.)

- [ ] **Step 6: Stage and stop for review (core marker!), then commit**

```bash
git add domains/soil/soil-core domains/soil/soil-console
git commit -m "$(cat <<'EOF'
refactor(soil): inject query beans into SoilsController; mark SoilProfileFactory @DomainService

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Plants controller de-fork (annotate `PlantQueryImpl`, `PlantFeatureQueryImpl`, `PlantAncestryResolver`)

**Files:**
- Modify (markers): `domains/plants/plants-core/.../PlantQueryImpl.java`, `.../PlantFeatureQueryImpl.java`, `.../PlantAncestryResolver.java`
- Modify: `domains/plants/plants-console/.../PlantsController.java`
- Modify (test de-fork): `PhytochemistryDetailTemplateTest`, `PhytochemistryListTemplateTest`, `PlantsDetailTemplateTest`, `PlantsListTemplateTest`, `render/DescriptionRendererTest` (plants-console)
- Create: `domains/plants/plants-console/src/test/java/com/naturalist/plants/console/DataForkArchTest.java`
- Modify: `domains/plants/plants-console/pom.xml` (add `archunit-junit5` test scope)

**Interfaces:**
- Consumes: `PlantQuery`, `CultivarQuery`, `SeedLineageQuery`, `PlantProgramQuery`, `PhytochemicalConstituentQuery` beans. `PlantQueryImpl` ctor args are all already beans except the feature branch; `PlantFeatureQueryImpl` ctor needs feature repos (rdms beans) + `PlantAncestryResolver`; `PlantAncestryResolver` ctor is `(SpeciesQuery, GenusQuery, FamilyQuery)` — all beans.

- [ ] **Step 1: Confirm the three ctor graphs resolve**

Read `PlantQueryImpl.java`, `PlantFeatureQueryImpl.java`, `PlantAncestryResolver.java`. Confirm every ctor arg is either an already-`@DomainService` query, a `*RepositoryRdms` bean, or one of these three classes. If any arg is a hand-built object (not a bean and not one of these three), STOP and surface it.

- [ ] **Step 2: Annotate the three classes**

Add `@DomainService` (+ import) to `PlantQueryImpl`, `PlantFeatureQueryImpl`, `PlantAncestryResolver`. No other change.

- [ ] **Step 3: De-fork `PlantsController`**

Current ctor `(Resilience)` forks `PlantsTestContext.create(db)` for `plantQuery()`, `cultivarQuery()`, `seedLineageQuery()`, `plantProgramQuery()`, `phytochemicalConstituentQuery()`. New ctor injects those five interface types + `Resilience`; keep the local `DescriptionRenderer` and `PlantImageStorageService(Path.of(...))` field inits as-is. Remove fork imports.

- [ ] **Step 4: Build**

Run: `mvn -q -pl domains/plants/plants-console -am -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: De-fork the 5 plants test sites**

Apply the extension + `getNamed` transform to the 5 files listed.

- [ ] **Step 6: Add the per-module ArchUnit test (plants-console)**

Add `archunit-junit5` (test scope) to `plants-console/pom.xml`; create `DataForkArchTest.java` (package `com.naturalist.plants.console`) with the same rule as Task 1 Step 5.

- [ ] **Step 7: Run plants module tests**

Run: `mvn -q -pl domains/plants/plants-console,domains/plants/plants-core -am test`
Expected: BUILD SUCCESS, `DataForkArchTest` green.

- [ ] **Step 8: Stage and stop for review (core markers!), then commit**

```bash
git add domains/plants/plants-core domains/plants/plants-console
git commit -m "$(cat <<'EOF'
refactor(plants): inject query beans into PlantsController; mark plant query/factory impls @DomainService; de-fork test sites; lock with ArchUnit

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Insects controller de-fork (the hard one — 9 insects + 4 library markers, EOL `@Bean`)

**Files:**
- Modify (insects markers, 8): `InsectQueryImpl`, `InsectImageQueryImpl`, `InsectFunctionalRoleQueryImpl`, `InsectObservationQueryImpl`, `InsectCommandImpl`, `InsectObservationCommandImpl`, `InsectFeatureCommandImpl`, `InsectFeatureAssignmentCommandImpl` (all `domains/insects/insects-core/.../`; `InsectImageCommandImpl` is already a bean — not touched)
- Modify (library markers): `LibraryCommandImpl`, `CitationCommandImpl`, `CitationAssociationCommandImpl`, `CitationAttributionTransaction` (all `domains/library/library-core/.../`)
- Create: `apps/management-console/src/main/java/com/naturalist/console/IdentificationConfiguration.java`
- Modify: `apps/management-console/pom.xml` (add `eol-client-mock` dep)
- Modify: `domains/insects/insects-console/.../InsectsController.java`
- Modify: `domains/insects/insects-console/pom.xml` (add `insects-core`, remove `insects-test-context`)
- Modify (test de-fork): `InsectsDetailTemplateTest`, `InsectsFamiliesTemplateTest`, `InsectsGeneraTemplateTest`, `InsectsGuildTemplateTest`, `InsectsListTemplateTest`, `InsectsOrdersTemplateTest` (insects-console)
- Create: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/DataForkArchTest.java`
- Modify: `domains/insects/insects-console/pom.xml` (add `archunit-junit5` test scope)

**Interfaces:**
- Consumes (controller ctor): `Resilience`, `VisionService`, `InsectQuery`, `InsectCommand`, `InsectLifeStageQuery` (bean), `CladeQuery` (library bean), `GlossaryTermQuery` (library bean), `LibraryCommand`, `ExternalAuthority` (new EOL bean).
- Produces: the fully Spring-wired insects console; the EOL `@Bean` for any future identification consumer.

- [ ] **Step 1: Verify the `CitationAttributionTransaction` ctor resolves**

Read `CitationAttributionTransaction.java`. Confirm its ctor args are all beans after the library markers are added (its citation commands / repositories). If it needs a non-bean, STOP and surface it.

- [ ] **Step 2: Annotate the 8 insects-core impls**

Add `@DomainService` (+ import) to each of the 8 insects-core classes: `InsectQueryImpl`, `InsectImageQueryImpl`, `InsectFunctionalRoleQueryImpl`, `InsectObservationQueryImpl`, `InsectCommandImpl`, `InsectObservationCommandImpl`, `InsectFeatureCommandImpl`, `InsectFeatureAssignmentCommandImpl`. Markers only. **Do NOT touch `InsectImageCommandImpl` — it is already a bean** (verify each target has NO existing line-start `@DomainService` before adding).

- [ ] **Step 3: Annotate the 4 library-core impls**

Add `@DomainService` (+ import) to `LibraryCommandImpl`, `CitationCommandImpl`, `CitationAssociationCommandImpl`, `CitationAttributionTransaction`. Markers only.

- [ ] **Step 4: Add the EOL `@Bean` to the composition root**

Add `eol-client-mock` dependency to `apps/management-console/pom.xml` (no version — inherited from dependencyManagement). Create `IdentificationConfiguration.java`:
```java
package com.naturalist.console;

import com.naturalist.authority.ExternalAuthority;
import com.naturalist.authority.eol.EolClientMock;
import com.naturalist.data.NaturalistDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition-root wiring for the pre-real-EOL identification path. Publishes the
 * mock external authority as the sole {@link ExternalAuthority} bean, backed by the
 * shared {@link NaturalistDatabase}. Replaced by the real EOL client adapter when
 * external-authority Phase 4 lands.
 */
@Configuration
class IdentificationConfiguration {
    @Bean
    ExternalAuthority eolAuthority(NaturalistDatabase naturalistDatabase) {
        return new EolClientMock(naturalistDatabase);
    }
}
```

- [ ] **Step 5: Swap insects-console module deps**

In `domains/insects/insects-console/pom.xml`: add a dependency on `insects-core` (needed for the concrete `InsectCatalogIdentificationTransaction`, `InsectAddPhotoTransaction`, `InsectIdentificationCommand`, `InsectAddPhotoCommand` types the controller `new`s) and remove the `insects-test-context` dependency.

- [ ] **Step 6: Rewrite `InsectsController` ctor**

Replace the forked ctor (lines ~104-128) with injection + local assembly. The transactions and command wrappers are built locally from injected ports (they are NOT beans):
```java
InsectsController(Resilience resilience,
                  com.naturalist.vision.VisionService visionService,
                  InsectQuery insectQuery,
                  InsectCommand insectCommand,
                  InsectLifeStageQuery insectLifeStageQuery,
                  CladeQuery cladeQuery,
                  com.naturalist.library.GlossaryTermQuery glossaryTermQuery,
                  com.naturalist.library.LibraryCommand libraryCommand,
                  com.naturalist.authority.ExternalAuthority eolAuthority) {
    this.insectQuery = insectQuery;
    this.insectCommand = insectCommand;
    this.insectLifeStageQuery = insectLifeStageQuery;
    this.cladeQuery = cladeQuery;
    this.resilience = resilience;
    this.descriptionRenderer = new DescriptionRenderer(InsectsParagraphCues.CUES);
    this.imageStorageService = new ImageStorageService(Path.of("data/images/insects"));
    this.glossaryLinker = GlossaryLinker.of(glossaryTermQuery
            .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content());
    var catalogIdentificationTransaction =
            new InsectCatalogIdentificationTransaction(insectCommand, insectQuery);
    this.identificationCommand = new InsectIdentificationCommand(
            visionService, new NoOpTextGenerationService(), eolAuthority,
            libraryCommand, insectQuery, catalogIdentificationTransaction);
    this.addPhotoCommand = new InsectAddPhotoCommand(
            new InsectAddPhotoTransaction(insectCommand));
}
```
Remove imports for `NaturalistDatabase`, `InsectsTestContext`, `LibraryTestContext`, `EolClientMock` (EOL now injected as `ExternalAuthority`). Keep the `NoOpTextGenerationService` import (still `new`ed). Confirm `InsectIdentificationCommand`'s EOL param type is `ExternalAuthority` (it is) so `eolAuthority` fits directly.

- [ ] **Step 7: Build main across the affected modules**

Run: `mvn -q -pl apps/management-console,domains/insects/insects-console,domains/insects/insects-core,domains/library/library-core -am -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 8: Boot the full context (wiring gate for ALL controllers)**

Run one app-level `@SpringBootTest`:
Run: `mvn -q -pl apps/management-console -am -Dtest=InsectsControllerWebMvcTest test`
Expected: PASS — proves the whole context (every de-forked controller + the EOL bean + the insects/library bean graph) wires with no `NoUniqueBeanDefinitionException` / `UnsatisfiedDependency`. If it fails on an unresolved bean, the missing marker is named in the error — add it (marker only) and re-run.

- [ ] **Step 9: De-fork the 6 insects test sites**

Apply the extension + `getNamed` transform to the 6 insects-console template tests listed.

- [ ] **Step 10: Add the per-module ArchUnit test (insects-console)**

Add `archunit-junit5` (test scope) to `insects-console/pom.xml`; create `DataForkArchTest.java` (package `com.naturalist.insects.console`) with the Task 1 Step 5 rule.

- [ ] **Step 11: Run the full insects + library + app WebMvc suite**

Run: `mvn -q -pl apps/management-console,domains/insects/insects-console,domains/insects/insects-core,domains/library/library-core -am test`
Expected: BUILD SUCCESS. The app-level `console/insects/*WebMvcTest` now run on the shared graph. Investigate any failure that stems from shared-graph semantics (write-then-read across tests); data content is unchanged (same JSON), so genuine breaks should be rare — fix the test to the shared-graph reality, do not re-introduce a fork.

- [ ] **Step 12: Stage and stop for review (core markers + app config!), then commit**

```bash
git add domains/insects/insects-core domains/library/library-core domains/insects/insects-console apps/management-console
git commit -m "$(cat <<'EOF'
refactor(insects): inject bean graph into InsectsController; mark insects/library command+query impls @DomainService; wire EOL mock as composition-root bean; de-fork test sites; lock with ArchUnit

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Central ArchUnit lock + full verify

Lands the two central main-code rules the effort promised. Rule A goes green only because Tasks 1-6 removed every controller fork.

**Files:**
- Create: `apps/management-console/src/test/java/com/naturalist/console/architecture/DataForkComplianceTest.java`

**Interfaces:**
- Consumes: all de-forked controllers (main code). `archunit-junit5` is already a management-console test dep.

- [ ] **Step 1: Write `DataForkComplianceTest`**

```java
package com.naturalist.console.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Main-code enforcement of the pre-RDBMS data-graph discipline. The app runs on the
 * single shared NaturalistDatabase bean (adapters/spring-test-data). No main-code class
 * may fork a private graph via NaturalistDatabase.create(), and none may construct a
 * TestEntitySource directly — getNamed is the only sanctioned path (ADR-001). Test-code
 * enforcement of the same rules lives per-module (DataForkArchTest in the affected modules).
 */
@AnalyzeClasses(packages = "com.naturalist", importOptions = ImportOption.DoNotIncludeTests.class)
class DataForkComplianceTest {

    /** The only legitimate main-code callers of NaturalistDatabase.create(). */
    private static final Set<String> CREATE_ALLOWED = Set.of(
            "com.naturalist.data.TestEntitySourceTest",
            "com.naturalist.spring.data.TestDataConfiguration");

    @ArchTest
    static final ArchRule noPrivateGraphFork =
            noClasses()
                    .that(new com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass>(
                            "are not sanctioned NaturalistDatabase.create() callers") {
                        @Override
                        public boolean test(com.tngtech.archunit.core.domain.JavaClass c) {
                            return !CREATE_ALLOWED.contains(c.getFullName());
                        }
                    })
                    .should().callMethodWhere(target ->
                            target.getTarget().getOwner().getName().equals("com.naturalist.data.NaturalistDatabase")
                                    && target.getTarget().getName().equals("create"))
                    .because("the app runs on the shared NaturalistDatabase bean; "
                            + "forking a private graph bypasses Spring — see "
                            + "docs/plans/2026-08-22-repository-rdms-intermediate-design.md");

    @ArchTest
    static final ArchRule noDirectTestEntitySourceConstruction =
            noClasses().should().callConstructorWhere(target ->
                            target.getConstructor().getOwner().getSimpleName().endsWith("TestEntitySource"))
                    .because("acquire a TestEntitySource via NaturalistDatabase#getNamed, never `new` — see ADR-001");
}
```
> If the exact ArchUnit predicate API differs in this version, adjust to the idioms already used in `ResilienceComplianceTest` (same module, same ArchUnit version) — e.g. its `DescribedPredicate`/`ArchCondition` patterns. Keep the two rules and the whitelist semantics identical.

- [ ] **Step 2: Run the ArchUnit test**

Run: `mvn -q -pl apps/management-console -Dtest=DataForkComplianceTest test`
Expected: BOTH rules PASS. If `noPrivateGraphFork` reports a violation, it names a class still calling `NaturalistDatabase.create()` in main code — that is a controller (or new site) not yet de-forked; fix it, don't widen the whitelist.

- [ ] **Step 3: Full verify from repo root**

Run: `mvn verify`
Expected: BUILD SUCCESS across all modules — every per-module `DataForkArchTest`, the central `DataForkComplianceTest`, and the full app WebMvc suite green.

- [ ] **Step 4: Stage and stop for review, then commit**

```bash
git add apps/management-console/src/test/java/com/naturalist/console/architecture/DataForkComplianceTest.java
git commit -m "$(cat <<'EOF'
test(console): lock controllers off the private-graph fork with ArchUnit (main-code rules)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

**Spec coverage:**
- Part 1 (controller de-fork, all 9): Tasks 1 (chemistry), 2 (4 library), 3 (garden), 4 (soil), 5 (plants), 6 (insects). ✓
- Namespace-impl annotation prerequisite: Tasks 3/4/5/6 add the exact verified worklist; chemistry/library controllers need none (Tasks 1/2). ✓
- InsectsController injects library `CladeQuery` + `GlossaryTermQuery` and builds `GlossaryLinker` off the injected query: Task 6 Step 6. ✓
- Part 2 (~40 test-site de-fork): Tasks 1 (chemistry, 3 files), 5 (plants, 5 files), 6 (insects, 6 files) = the 14 `new *TestEntitySource` files. ✓
- Part 3 (two ArchUnit rules, together, green only when forks gone): central rules in Task 7; per-module (D2) in Tasks 1/5/6. Rule A stays red until every controller de-forks, so it is added last. ✓
- `*TestContext` stays a test-only helper: untouched (markers are inert; TestContexts keep `new`ing impls). ✓
- Working rules (main, scoped commits, footer, core sign-off, no push): Global Constraints + per-task review stops. ✓

**Placeholder scan:** no TBD/TODO/"handle edge cases"; every code step has concrete code; ArchUnit predicate has a fallback note pointing at the in-repo `ResilienceComplianceTest` idioms. ✓

**Type consistency:** controller ctor param types are the api interfaces (`InsectQuery`, `CompoundQuery`, …) + `ExternalAuthority`/`LibraryCommand`/`GlossaryTermQuery` (library-api) + concrete insects-core transactions built locally; `getNamed(<X>TestEntitySource.class)` returns the source type used in each de-forked test. Worklist classes match the verified audit. ✓

**Open verification the executor must not skip:** Task 6 Step 1 (`CitationAttributionTransaction` ctor resolves) and Task 5 Step 1 / Task 3-4 factory ctor checks — if any needs more than a marker, STOP per Global Constraints.
