# `Plant` Read Model — Chunk 5: Species Extras (cultivars / programs / constituents) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fold the species page's three cross-sub-context attachments — cultivars, plant programs, phytochemical constituents — into the `Plant` read model, composed by `PlantFactory` on the species-rank `Plant`, so the species handler reads them off `plant.get()` instead of firing three separate sub-context queries.

**Architecture:** `Plant` gains three `BehavioralCollection` components (`CultivarCollection cultivars`, `PlantProgramCollection programs`, `PhytochemicalConstituentCollection constituents`), empty by default and for all ranks above species. `PlantFactory` gains the three sub-context query deps (`CultivarQuery`, `PlantProgramQuery`, `PhytochemicalConstituentQuery`) and composes them only on the species arm. `PlantQueryImpl` gains the three deps and passes them through; `PlantsTestContext.createPlantQuery` wires them from the sub-context test-contexts. The species `detail` handler sources the three off `plant.get()`.

**Tech Stack:** Java 21 records + sealed types, JUnit 5 + AssertJ, the Observer framework, `NaturalistDatabase` + repository mocks, `PlantsTestContext`. Build: scoped `mvn -pl <module> -am test`; the user runs full `mvn verify`.

**Design of record:** [2026-08-20-plant-read-model-design.md](2026-08-20-plant-read-model-design.md) (Chunk 5 of 6). Prior chunks shipped: `73cc06ed` skeleton, `57d2e269` features, `fbf36888` children, `25bdf510` role+gallery.

## Global Constraints

- **User decision (brainstorming):** fold the species extras into `Plant` — `PlantFactory` reaches across the `cultivar/`, `management/`, `phytochemistry/` sub-contexts. This is the deliberate cross-sub-context coupling the user approved; the three query interfaces are public in `plants-api`, so `PlantFactory`/`PlantQueryImpl` (plants-core) may depend on them.
- **Species-only.** All three are composed **only** on the species arm of `PlantFactory.buildByName`; order/family/genus `Plant`s carry the empty collections. This preserves current behavior (only the species `detail` page shows these). `PlantProgram`/`PhytochemicalConstituent` attach at any rank in the data model, but no page above species renders them today — do not add rank-level program/constituent display in this chunk.
- **Signatures:** `CultivarQuery.forPlantName(PlantSpeciesName) → CultivarCollection`; `PlantProgramQuery.forPlantName(PlantRankName) → PlantProgramCollection`; `PhytochemicalConstituentQuery.forPlantName(PlantRankName) → PhytochemicalConstituentCollection`. On the species arm the species name `sn` (a `PlantSpeciesName`) is passed to all three (it widens to `PlantRankName` for programs/constituents). Each collection has `of(Collection)` + `empty()`.
- **Record-arity ripple.** `Plant` grows from 8 to 11 components. Only `Plant.java` constructs `new Plant(...)` — confirm with `grep -rn "new Plant(" --include='*.java' domains`. `Plant` imports the three collection types from their sub-context packages (`com.naturalist.plants.cultivar.CultivarCollection`, `...management.PlantProgramCollection`, `...phytochemistry.PhytochemicalConstituentCollection` — all public).
- **`plants-core` test cannot reach the sub-context queries.** `CultivarTestContext` etc. live in `plants-test-context` (which depends on plants-core — no cycle allowed the other way), and the sub-context `*QueryImpl`/mocks are package-private in `com.naturalist.plants.cultivar` etc. So `PlantFactoryTest` (plants-core) uses **inline stub implementations** of the three query interfaces (each interface = the 3 inherited `EntityQuery` methods `getByName`/`findByNameSet`/`findPage` + `forPlantName`); the stubs return `.empty()` from `forPlantName` and throw `UnsupportedOperationException` from the inherited three. Real-data composition is verified by a **plants-console integration test** using `PlantsTestContext` (Task 2).
- **Behavior-preserving.** The species template (`plants/detail.jte`) is unchanged — it still takes `List<Cultivar>`/`List<PlantProgram>`/`List<PhytochemicalConstituent>` params; the handler still sorts + `.toList()`, only the source changes (`plant.get().cultivars()` instead of the query). The controller keeps its `cultivarQuery`/`plantProgramQuery`/`phytochemicalConstituentQuery` fields — other handlers (`cultivarDetail`, `programDetail`, `constituentDetail`, the phytochemistry list) still use them.
- **Build gotcha:** `mvn -pl … -am -Dtest=X` needs `-Dsurefire.failIfNoSpecifiedTests=false`.

---

### Task 1: `Plant` gains the three extras; `PlantFactory` composes them species-only

**Files:**
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java`
- Modify: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java`
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java`
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java`
- Modify: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java`
- Modify: `domains/plants/plants-test-context/src/main/java/com/naturalist/plants/PlantsTestContext.java`

**Interfaces:**
- Consumes: the three `forPlantName` signatures above.
- Produces: `Plant.cultivars() → CultivarCollection`, `Plant.programs() → PlantProgramCollection`, `Plant.constituents() → PhytochemicalConstituentCollection`; `Plant.withCultivars/withPrograms/withConstituents`; `PlantFactory(SpeciesQuery, GenusQuery, FamilyQuery, OrderQuery, FeatureQuery, EcologicalRoleQuery, ImageQuery, CultivarQuery, PlantProgramQuery, PhytochemicalConstituentQuery)` (10-arg).

- [ ] **Step 1: Grep arity sites.** `grep -rn "new Plant(" --include='*.java' domains` — confirm only `Plant.java`.

- [ ] **Step 2: Write the failing tests.** Append to `PlantTest.java`:

```java
    @Test
    void withCultivars_carries_andEmptyDefaultsAreNonNull() {
        Plant plant = Plant.empty();
        assertThat(plant.cultivars().isEmpty()).isTrue();
        assertThat(plant.programs().isEmpty()).isTrue();
        assertThat(plant.constituents().isEmpty()).isTrue();
        assertThat(observer.forMethod("extras").observable(plant, "plant").violations()).isEmpty();
    }

    @Test
    void nullCultivars_reportsViolation() {
        var mo = observer.forMethod("nullCultivars");
        Plant plant = Plant.empty().withCultivars(null);
        assertThat(mo.observable(plant, "plant").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".plant.cultivars");
    }
```

In `PlantFactoryTest.java`, extend the `factory()` helper to pass three inline stubs and add a non-species-empty assertion. Add the stubs (imports: `com.naturalist.plants.cultivar.*`, `com.naturalist.plants.management.*`, `com.naturalist.plants.phytochemistry.*`, `com.naturalist.data.Page`, `com.naturalist.data.PageRequest`, `java.util.Set`):

```java
    private static PlantQueryStubs stubs() { /* see below */ }

    // In factory():
        CultivarQuery cultivarStub = new CultivarQuery() {
            @Override public CultivarCollection forPlantName(PlantSpeciesName n) { return CultivarCollection.empty(); }
            @Override public Optional<Cultivar> getByName(CultivarName name) { throw new UnsupportedOperationException(); }
            @Override public CultivarCollection findByNameSet(Set<CultivarName> s) { throw new UnsupportedOperationException(); }
            @Override public Page<Cultivar> findPage(PageRequest p) { throw new UnsupportedOperationException(); }
        };
        PlantProgramQuery programStub = new PlantProgramQuery() {
            @Override public PlantProgramCollection forPlantName(PlantRankName n) { return PlantProgramCollection.empty(); }
            @Override public Optional<PlantProgram> getByName(PlantProgramName name) { throw new UnsupportedOperationException(); }
            @Override public PlantProgramCollection findByNameSet(Set<PlantProgramName> s) { throw new UnsupportedOperationException(); }
            @Override public Page<PlantProgram> findPage(PageRequest p) { throw new UnsupportedOperationException(); }
        };
        PhytochemicalConstituentQuery constituentStub = new PhytochemicalConstituentQuery() {
            @Override public PhytochemicalConstituentCollection forPlantName(PlantRankName n) { return PhytochemicalConstituentCollection.empty(); }
            @Override public PhytochemicalConstituentCollection forCompoundName(com.naturalist.chemistry.CompoundName c) { throw new UnsupportedOperationException(); }
            @Override public Optional<PhytochemicalConstituent> getByName(PhytochemicalConstituentName name) { throw new UnsupportedOperationException(); }
            @Override public PhytochemicalConstituentCollection findByNameSet(Set<PhytochemicalConstituentName> s) { throw new UnsupportedOperationException(); }
            @Override public Page<PhytochemicalConstituent> findPage(PageRequest p) { throw new UnsupportedOperationException(); }
        };
        return new PlantFactory(speciesQuery, genusQuery, familyQuery, orderQuery,
                featureQuery, roleQuery, imageQuery, cultivarStub, programStub, constituentStub);
```

and:

```java
    @Test
    void buildByName_nonSpeciesRank_hasEmptyExtras() {
        Plant plant = factory().buildByName(PlantOrderName.of("asterales")).orElseThrow();
        assertThat(plant.cultivars().isEmpty()).isTrue();
        assertThat(plant.programs().isEmpty()).isTrue();
        assertThat(plant.constituents().isEmpty()).isTrue();
    }
```

(Confirm the exact method set of each query interface + the `CompoundName` package (`com.naturalist.chemistry.CompoundName` or `com.naturalist.identifiers...`) by reading `PhytochemicalConstituentQuery.java`. Confirm the `*Name` id types — `CultivarName`, `PlantProgramName`, `PhytochemicalConstituentName` — and their imports. Adjust the stubs to match the real interface signatures exactly; the `@Override`s will fail to compile if a signature is wrong, which is the safety net.)

- [ ] **Step 3: Run to verify failure.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest,PlantTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL.

- [ ] **Step 4: Add the three components to `Plant`.** Add imports for the three collection types. Add `CultivarCollection cultivars`, `PlantProgramCollection programs`, `PhytochemicalConstituentCollection constituents` as the 9th/10th/11th components (append after `children`). Update `empty()` (append `CultivarCollection.empty(), PlantProgramCollection.empty(), PhytochemicalConstituentCollection.empty()`), thread all three through every existing `with*`, add `withCultivars`/`withPrograms`/`withConstituents`. In `invariants()` add:

```java
                .behavioralCollection(cultivars, "cultivars")
                .behavioralCollection(programs, "programs")
                .behavioralCollection(constituents, "constituents")
```

- [ ] **Step 5: Compose on the species arm of `PlantFactory`.** Add `CultivarQuery cultivarQuery`, `PlantProgramQuery programQuery`, `PhytochemicalConstituentQuery constituentQuery` as the 8th/9th/10th ctor params (null-checked + assigned; import the three query interfaces). In the species arm, apply the three extras to the species `Plant` (they are species-only, so NOT in the shared `base()`):

```java
            case PlantSpeciesName sn -> speciesQuery.getByName(sn).map(s -> observe(
                    resolveGenus(base(sn)
                            .withSpecies(PlantSpeciesView.of(s))
                            .withChildren(java.util.List.of())
                            .withCultivars(cultivarQuery.forPlantName(sn))
                            .withPrograms(programQuery.forPlantName(sn))
                            .withConstituents(constituentQuery.forPlantName(sn)),
                            s.genusName())));
```

The order/family/genus arms are unchanged (extras default to empty via `Plant.empty()`).

- [ ] **Step 6: Pass the three queries in `PlantQueryImpl`.** Add the three sub-context query interfaces as constructor params (append after `featureQuery`), null-check + assign to fields, and append them to the `new PlantFactory(...)` construction. Import `com.naturalist.plants.cultivar.CultivarQuery`, `...management.PlantProgramQuery`, `...phytochemistry.PhytochemicalConstituentQuery`.

- [ ] **Step 7: Wire `PlantsTestContext.createPlantQuery`.** The `PlantsTestContext` constructor already builds `cultivarQuery`/`plantProgramQuery`/`phytochemicalConstituentQuery` (via `CultivarTestContext.createQuery(db)` etc.). Reorder so those three are built **before** `this.plantQuery`, then change `createPlantQuery(db)` to `createPlantQuery(db, cultivarQuery, plantProgramQuery, phytochemicalConstituentQuery)` and give the static method three new params it forwards to `new PlantQueryImpl(...)`. (Do not double-build the queries — pass the ones the constructor already builds.)

- [ ] **Step 8: Run + module green.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest,PlantTest -Dsurefire.failIfNoSpecifiedTests=false` → PASS; then `mvn -pl domains/plants/plants-test-context -am test` → green (proves the rewired context + PlantQueryImpl compile).

- [ ] **Step 9: Stage (do NOT commit).** `git add` the six files above.

---

### Task 2: Species handler reads the extras off `plant.get()`; integration test

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java` (the species `detail` handler)
- Create: `domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantDetailGraphTest.java` (integration test via `PlantsTestContext`)

**Interfaces:**
- Consumes: `Plant.cultivars()/programs()/constituents()` (Task 1); `PlantsTestContext.create(db).plantQuery().getByName(...)`.

- [ ] **Step 1: Source the extras from the read model.** In the species `detail` handler, replace the three query-backed locals:

```java
        var cultivars = cultivarQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((Cultivar c) -> c.name().value())).toList();
        var programs = plantProgramQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((PlantProgram p) -> p.name().value())).toList();
        var constituents = phytochemicalConstituentQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((PhytochemicalConstituent c) -> c.name().value())).toList();
```

with the same three sorted lists sourced from the resolved `Plant`:

```java
        var cultivars = plant.get().cultivars().stream()
                .sorted(Comparator.comparing((Cultivar c) -> c.name().value())).toList();
        var programs = plant.get().programs().stream()
                .sorted(Comparator.comparing((PlantProgram p) -> p.name().value())).toList();
        var constituents = plant.get().constituents().stream()
                .sorted(Comparator.comparing((PhytochemicalConstituent c) -> c.name().value())).toList();
```

Leave every `model.addAttribute(...)` line and the template unchanged. (`cultivarQuery`/`plantProgramQuery`/`phytochemicalConstituentQuery` stay as controller fields — the cultivar/program/constituent *detail* handlers + phytochemistry list still use them; do not remove the fields.)

- [ ] **Step 2: Write the integration test** `PlantDetailGraphTest.java` — the real-data composition check the plants-core stubs can't do:

```java
package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.PlantsTestContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The species-rank Plant read model composes its cross-sub-context extras. */
class PlantDetailGraphTest {

    private final PlantsTestContext context = PlantsTestContext.create(NaturalistDatabase.create());

    @Test
    void speciesPlant_composesProgramsAndConstituents() {
        var plant = context.plantQuery().getByName(
                PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.programs().isEmpty()).isFalse();      // pipevine has seeded programs
        assertThat(plant.constituents().isEmpty()).isFalse();  // and seeded constituents
    }

    @Test
    void speciesPlant_composesCultivars_whenSeeded() {
        // Pick a species with seeded cultivars (a tomato — cultivars.json). Confirm the slug.
        var plant = context.plantQuery().getByName(
                PlantSpeciesName.of("solanum-lycopersicum")).orElseThrow();
        assertThat(plant.cultivars().isEmpty()).isFalse();
    }
```

(Confirm `aristolochia-californica` has seeded programs + constituents in `plant-programs.json`/`phytochemical-constituents.json` — per `TestPlantsIdentifiers.Plants.CaliforniaPipevine.Programs`/`.Constituents` it does. Confirm the cultivar-bearing species slug against `cultivar/cultivars.json` — if `solanum-lycopersicum` is not the seeded tomato slug, use whichever species the cultivar catalog references. If no species has cultivars, drop the second test and note it.)

- [ ] **Step 3: Build + module green.** `mvn -pl domains/plants/plants-console -am test` → green (the integration test passes; the species `detail.jte` template test is unaffected — it still passes its own `cultivars`/`programs`/`constituents` maps).

- [ ] **Step 4: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantDetailGraphTest.java`.

---

## Self-Review

**Spec coverage (design Chunk 5 + user decision to fold extras in):**
- `Plant.cultivars()/programs()/constituents()` composed by `PlantFactory` species-only → Task 1. ✓
- `PlantQueryImpl` + `PlantsTestContext` gain the three sub-context deps → Task 1 Steps 6–7. ✓
- Species handler reads them off `plant.get()` → Task 2 Step 1. ✓
- Real-data composition verified via `PlantsTestContext` integration test → Task 2 Step 2. ✓

**Placeholder scan:** the stub method sets + the seeded-species slugs carry concrete anchors (the interface `@Override`s fail to compile if wrong; `aristolochia-californica` is the known programs+constituents anchor). Not placeholders.

**Type consistency:** the three collections are `CultivarCollection`/`PlantProgramCollection`/`PhytochemicalConstituentCollection` on `Plant`, its `with*`, and the factory; `PlantFactory` 10-arg ctor matches `PlantQueryImpl`'s construction call + `PlantFactoryTest`'s stub-wired call; `PlantQueryImpl` 11-arg ctor matches `PlantsTestContext.createPlantQuery`'s call. Arity threads through `empty()` + all `with*`.

**Scope discipline:** species-only composition (order/family/genus arms untouched, verified empty by `PlantFactoryTest`); template + controller query fields preserved; nothing from Chunk 6 (cleanup) pulled forward.

**Notes for the executor:** the plants-core→plants-test-context cycle forces the stub approach in `PlantFactoryTest`; the real wiring lives in `PlantsTestContext` and is exercised by the plants-console integration test; `Plant`'s three new imports cross into the cultivar/management/phytochemistry sub-context packages (public collection types) — that cross-sub-context reference is the coupling the user approved.
