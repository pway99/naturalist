# `Plant` Read Model — Chunk 1: Skeleton (views + `Plant` + `getByName`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the composed `Plant` read model seam — the `PlantTaxonView` rank-chain views, the `Plant` `ReadModel` carrying the ancestry spine, and a `PlantQuery.getByName(PlantRankName) → Optional<Plant>` that resolves the subject rank and walks its ancestry — with **no console changes** (the controller switch is Chunk 2).

**Architecture:** Faithful mirror of the insects rank-page read model at api/core, restricted to the rank chain. `PlantTaxonView` (sealed, 4 permits) mirrors `InsectTaxonView`; `Plant` mirrors `Insect` cut down to the four `@Nullable` rank slots; `PlantFactory` mirrors `InsectFactory.buildByName` cut down to `resolveGenus/resolveFamily/resolveOrder` (no base attributes, no children — those are later chunks). `PlantQueryImpl` builds the factory from the four rank sub-queries it already holds and delegates `getByName`.

**Tech Stack:** Java 21 records + sealed types, JUnit 5 + AssertJ, the Observer framework, `NaturalistDatabase` + repository mocks seeded from JSON. Build: scoped `mvn -pl <module> -am test`; the user runs full `mvn verify`.

**Design of record:** [2026-08-20-plant-read-model-design.md](2026-08-20-plant-read-model-design.md) (this is Chunk 1 of 6).

## Global Constraints

- **Insects is the reference; plants moves, insects holds still** (`domains/plants/CLAUDE.md` rule 1). Mirror `InsectTaxonView`/`InsectFamilyView`/`InsectSpeciesView`, `Insect`, `InsectFactory`, `InsectQueryImpl` exactly, adapted to the **four** `PlantRankName` permits (`PlantOrderName`/`PlantFamilyName`/`PlantGenusName`/`PlantSpeciesName` — **no subspecies**, so the factory `switch` is exhaustive with no `default` and no subspecies arm).
- **Views are 2-arg from birth.** `PlantTaxonView` permits carry `(entity, ImageCollection images)` only — the insects `features()` permit slot was already retired in insects Stage 3; do **not** add it here.
- **Static factories are the instantiation API** (`domains/CLAUDE.md` ADR-012): every view is built via `of(...)`, never `new` at a call site outside its own class. `Plant` uses `empty()` + `with*`.
- **`ImageCollection`** is `com.naturalist.plants.PlantEntityCollections.ImageCollection` (`final class extends BehavioralCollection<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>>`) with `empty()`.
- **Rank entity accessors (confirmed):** `PlantOrder.name()`→`PlantOrderName`, `PlantOrder.order()`→`TaxonomicOrder`; `PlantFamily.name()`→`PlantFamilyName`, `.family()`, `.orderName()`→`PlantOrderName`; `PlantGenus.name()`→`PlantGenusName`, `.genus()`, `.familyName()`→`PlantFamilyName`; `PlantSpecies.name()`→`PlantSpeciesName`, `.epithet()`, `.genusName()`→`PlantGenusName`.
- **No `belongsTo*` predicates exist on the plant rank entities** (unlike insects). The FK-consistency check lives **in the view**, comparing typed FK names directly (e.g. `family.orderName().equals(order.name())`). Do not add predicates to the entity records in this chunk.
- **Package:** all api types live in `com.naturalist.plants` (plants-api); `Plant`, `PlantRankName`, the views, and `PlantQuery` share that package — no cross-imports needed between them. `PlantRankName` and its permits come from the `identifiers` module but sit in the same `com.naturalist.plants` package.
- **No console changes in this chunk.** `PlantsController` and all `.jte` templates are untouched.

---

### Task 1: `PlantTaxonView` sealed interface + four permit records

**Files:**
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantTaxonView.java`
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantOrderView.java`
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantFamilyView.java`
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantGenusView.java`
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantSpeciesView.java`
- Test: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTaxonViewTest.java`

**Interfaces:**
- Consumes: `PlantOrder`/`PlantFamily`/`PlantGenus`/`PlantSpecies` (plants-api); `PlantEntityCollections.ImageCollection`.
- Produces: `sealed interface PlantTaxonView extends ReadModel permits PlantOrderView, PlantFamilyView, PlantGenusView, PlantSpeciesView` with `PlantRankName name()` + `ImageCollection images()`. Permits: `PlantOrderView(PlantOrder order, ImageCollection images)` with `of(PlantOrder, ImageCollection)` + `of(PlantOrder)`, `name()`→`PlantOrderName`, `order()`; `PlantFamilyView(PlantFamily family, ImageCollection images)` with the two `of`s, `name()`→`PlantFamilyName`, `family()`, `orderName()`→`PlantOrderName`, `belongsToOrder(@Nullable PlantOrderView)`; `PlantGenusView(PlantGenus genus, ImageCollection images)` with the two `of`s, `name()`→`PlantGenusName`, `genus()`, `familyName()`, `belongsToFamily(@Nullable PlantFamilyView)`; `PlantSpeciesView(PlantSpecies species, ImageCollection images)` with the two `of`s, `name()`→`PlantSpeciesName`, `species()`, `genusName()`, `belongsToGenus(@Nullable PlantGenusView)`.

- [ ] **Step 1: Write the failing test** `PlantTaxonViewTest.java`:

```java
package com.naturalist.plants;

import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantTaxonViewTest {

    private final Observer observer = Observer.forClass(PlantTaxonViewTest.class);

    private static PlantFamily asteraceae() {
        return new PlantFamily(
                PlantFamilyName.of("asteraceae"),
                PlantOrderName.of("asterales"),
                com.naturalist.taxonomy.TaxonomicFamily.of("Asteraceae"),
                com.naturalist.fieldnotes.Description.of("a", "b", "c", "d"),
                java.util.Set.of());
    }

    @Test
    void familyView_of_exposesNameAndFk_andHasNoViolations() {
        PlantFamilyView view = PlantFamilyView.of(asteraceae());
        assertThat(view.name()).isEqualTo(PlantFamilyName.of("asteraceae"));
        assertThat(view.orderName()).isEqualTo(PlantOrderName.of("asterales"));
        assertThat(view.images().isEmpty()).isTrue();
        assertThat(observer.forMethod("valid").observable(view, "view").violations()).isEmpty();
    }

    @Test
    void familyView_belongsToOrder_matchesFk_andTolueratesNull() {
        PlantFamilyView family = PlantFamilyView.of(asteraceae());
        PlantOrderView asterales = PlantOrderView.of(new PlantOrder(
                PlantOrderName.of("asterales"),
                com.naturalist.taxonomy.TaxonomicOrder.of("Asterales"),
                com.naturalist.fieldnotes.Description.of("a", "b", "c", "d"),
                java.util.Set.of(),
                null));
        assertThat(family.belongsToOrder(asterales)).isTrue();
        assertThat(family.belongsToOrder(null)).isTrue();
    }

    @Test
    void dispatchesAcrossSealedPermits() {
        PlantTaxonView view = PlantFamilyView.of(asteraceae());
        String tag = switch (view) {
            case PlantOrderView v -> "order:" + v.order().name().value();
            case PlantFamilyView v -> "family:" + v.family().name().value();
            case PlantGenusView v -> "genus:" + v.genus().name().value();
            case PlantSpeciesView v -> "species:" + v.species().name().value();
        };
        assertThat(tag).isEqualTo("family:asteraceae");
    }
}
```

(Confirm the exact `PlantOrder`/`PlantFamily` canonical-constructor component order and the `TaxonomicOrder`/`TaxonomicFamily`/`Description` factory signatures against the record sources before running — the test uses `new` only inside the test, which is permitted; adjust arg lists to match. `Description.of(...)` may differ — mirror an existing plants-api test's `Description` construction, e.g. `PlantFamilyTest`.)

- [ ] **Step 2: Run to verify failure.** `mvn -pl domains/plants/plants-api -am test -Dtest=PlantTaxonViewTest` → FAIL (types undefined).

- [ ] **Step 3: Create `PlantTaxonView`** (mirror `InsectTaxonView`):

```java
package com.naturalist.plants;

import com.naturalist.ddd.ReadModel;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;

/**
 * The catalog-view read model for a plant at whatever rank identification reached —
 * its rank record composed with the photographic field record. Sealed across the four
 * botanical ranks that carry catalog entities. Identity is the root rank's typed
 * {@link PlantRankName}, returned polymorphically by {@link #name()}. Permits carry no
 * {@code features()} slot; features live on {@link Plant} as a {@code PlantFeatureView}
 * (later chunk). Mirrors {@code InsectTaxonView}.
 */
public sealed interface PlantTaxonView extends ReadModel
        permits PlantOrderView, PlantFamilyView, PlantGenusView, PlantSpeciesView {

    /** The typed slug of the root rank record — polymorphic across the sealed permits. */
    PlantRankName name();

    /** Photographs of this organism at the root rank — non-null, possibly empty. */
    ImageCollection images();
}
```

- [ ] **Step 4: Create the four permits.** Each mirrors `InsectFamilyView`/`InsectSpeciesView`. `PlantOrderView` (no FK, top of chain):

```java
package com.naturalist.plants;

import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import java.util.function.Consumer;

/** Order-rank {@link PlantTaxonView}. Root identity is the order's {@link PlantOrderName}. */
public record PlantOrderView(PlantOrder order, ImageCollection images) implements PlantTaxonView {

    public static PlantOrderView of(PlantOrder order, ImageCollection images) {
        return new PlantOrderView(order, images);
    }

    public static PlantOrderView of(PlantOrder order) {
        return new PlantOrderView(order, ImageCollection.empty());
    }

    @Override public PlantOrderName name() { return order.name(); }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(order, "order").behavioralCollection(images, "images");
    }
}
```

`PlantFamilyView` (adds `orderName()` + `belongsToOrder`):

```java
package com.naturalist.plants;

import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/** Family-rank {@link PlantTaxonView}. Root identity is the family's {@link PlantFamilyName}. */
public record PlantFamilyView(PlantFamily family, ImageCollection images) implements PlantTaxonView {

    public static PlantFamilyView of(PlantFamily family, ImageCollection images) {
        return new PlantFamilyView(family, images);
    }

    public static PlantFamilyView of(PlantFamily family) {
        return new PlantFamilyView(family, ImageCollection.empty());
    }

    @Override public PlantFamilyName name() { return family.name(); }

    /** The order this family belongs to, exposed as a typed FK delegate. */
    public PlantOrderName orderName() { return family.orderName(); }

    /** True iff this family's order FK equals the given order's name, OR the order is null. */
    public boolean belongsToOrder(@Nullable PlantOrderView order) {
        return order == null || family.orderName().equals(order.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(family, "family").behavioralCollection(images, "images");
    }
}
```

`PlantGenusView` (adds `familyName()` + `belongsToFamily`, delegate `genus()`), `PlantSpeciesView` (adds `genusName()` + `belongsToGenus`, delegate `species()`) — the identical transformation on their own entity type and FK:

- `PlantGenusView`: `name()`→`genus.name()`; `familyName()`→`genus.familyName()`; `belongsToFamily(@Nullable PlantFamilyView f)` → `f == null || genus.familyName().equals(f.name())`; invariants `namedEntity(genus, "genus")` + `behavioralCollection(images, "images")`.
- `PlantSpeciesView`: `name()`→`species.name()`; `genusName()`→`species.genusName()`; `belongsToGenus(@Nullable PlantGenusView g)` → `g == null || species.genusName().equals(g.name())`; invariants `namedEntity(species, "species")` + `behavioralCollection(images, "images")`.

- [ ] **Step 5: Run to verify pass.** `mvn -pl domains/plants/plants-api -am test -Dtest=PlantTaxonViewTest` → PASS.

- [ ] **Step 6: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantTaxonView.java domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant*View.java domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTaxonViewTest.java`. (Controller stops for review + commit.)

---

### Task 2: `Plant` read model (rank chain only)

**Files:**
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java`
- Test: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java`

**Interfaces:**
- Consumes: `PlantOrderView`/`PlantFamilyView`/`PlantGenusView`/`PlantSpeciesView` (Task 1).
- Produces: `record Plant(@Nullable PlantOrderView order, @Nullable PlantFamilyView family, @Nullable PlantGenusView genus, @Nullable PlantSpeciesView species) implements ReadModel` with `empty()`, `identifiedTo()→Optional<PlantRankName>`, `orderName()/familyName()/genusName()/speciesName()→Optional<...>`, `withOrder/withFamily/withGenus/withSpecies`, and `invariants()` (per-rank descent + ancestor-presence + FK consistency).

- [ ] **Step 1: Write the failing test** `PlantTest.java`:

```java
package com.naturalist.plants;

import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantTest {

    private final Observer observer = Observer.forClass(PlantTest.class);

    @Test
    void empty_hasNoRanks_andNoViolations() {
        Plant plant = Plant.empty();
        assertThat(plant.identifiedTo()).isEmpty();
        assertThat(observer.forMethod("empty").observable(plant, "plant").violations()).isEmpty();
    }

    @Test
    void childRankWithoutAncestor_reportsAncestorPresenceViolation() {
        // A genus set with no family is a broken ancestry spine.
        PlantGenus thymus = new PlantGenus(
                PlantGenusName.of("thymus"), PlantFamilyName.of("lamiaceae"),
                com.naturalist.taxonomy.TaxonomicFamily.of("Lamiaceae"),
                com.naturalist.taxonomy.TaxonomicGenus.of("Thymus"),
                com.naturalist.fieldnotes.Description.of("a", "b", "c", "d"),
                java.util.Set.of());
        Plant plant = Plant.empty().withGenus(PlantGenusView.of(thymus));
        MethodObserver mo = observer.forMethod("childRankWithoutAncestor");
        assertThat(mo.observable(plant, "plant").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".plant.genus:family");
    }

    @Test
    void identifiedTo_returnsMostSpecificRank() {
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("lamiales"),
                com.naturalist.taxonomy.TaxonomicOrder.of("Lamiales"),
                com.naturalist.fieldnotes.Description.of("a", "b", "c", "d"),
                java.util.Set.of(), null);
        Plant plant = Plant.empty().withOrder(PlantOrderView.of(order));
        assertThat(plant.identifiedTo()).contains(PlantOrderName.of("lamiales"));
    }
}
```

(Confirm the `PlantGenus`/`PlantOrder` constructor arg order + the `TaxonomicGenus`/`TaxonomicFamily`/`TaxonomicOrder`/`Description` factory signatures against the record sources; mirror an existing `PlantGenusTest`/`PlantOrderTest` for exact construction. Confirm `violationNamesRemovingPrefix` + `MethodObserver.observationPoint()` are the same helpers `InsectTest` uses.)

- [ ] **Step 2: Run to verify failure.** `mvn -pl domains/plants/plants-api -am test -Dtest=PlantTest` → FAIL (`Plant` undefined).

- [ ] **Step 3: Implement `Plant`** (mirror `Insect` cut to the rank chain):

```java
package com.naturalist.plants;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Plant read model — in-memory composition of everything known about a plant at
 * whatever identification depth was reached. Chunk 1 carries only the rank chain
 * ({@code @Nullable} {@link PlantOrderView}/{@link PlantFamilyView}/{@link PlantGenusView}/
 * {@link PlantSpeciesView}), populated top-down to the resolved depth. Features, children,
 * role, images, and species extras fold in over later chunks (see the design of record).
 * Mirrors {@code Insect}.
 *
 * <p>Construction never throws; invalid states are reported by {@link #invariants()} when a
 * consumer asks an {@link com.naturalist.observability.Observer} to walk them.
 */
public record Plant(
        @Nullable PlantOrderView order,
        @Nullable PlantFamilyView family,
        @Nullable PlantGenusView genus,
        @Nullable PlantSpeciesView species
) implements ReadModel {

    /** Zero-state read model — no rank identified. Starting point for {@code with*} refinement. */
    public static Plant empty() {
        return new Plant(null, null, null, null);
    }

    /** Most-specific identified rank's typed name, if any. */
    public Optional<PlantRankName> identifiedTo() {
        if (species != null) return Optional.of(species.name());
        if (genus != null) return Optional.of(genus.name());
        if (family != null) return Optional.of(family.name());
        if (order != null) return Optional.of(order.name());
        return Optional.empty();
    }

    public Optional<PlantOrderName> orderName() {
        return order == null ? Optional.empty() : Optional.of(order.name());
    }

    public Optional<PlantFamilyName> familyName() {
        return family == null ? Optional.empty() : Optional.of(family.name());
    }

    public Optional<PlantGenusName> genusName() {
        return genus == null ? Optional.empty() : Optional.of(genus.name());
    }

    public Optional<PlantSpeciesName> speciesName() {
        return species == null ? Optional.empty() : Optional.of(species.name());
    }

    public Plant withOrder(@Nullable PlantOrderView order) {
        return new Plant(order, family, genus, species);
    }

    public Plant withFamily(@Nullable PlantFamilyView family) {
        return new Plant(order, family, genus, species);
    }

    public Plant withGenus(@Nullable PlantGenusView genus) {
        return new Plant(order, family, genus, species);
    }

    public Plant withSpecies(@Nullable PlantSpeciesView species) {
        return new Plant(order, family, genus, species);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .whenNotNull(order, o -> o.readModel(order, "order"))
                .whenNotNull(family, f -> f
                        .readModel(family, "family")
                        .notNull(order, "family:order")
                        .isTrue(family.belongsToOrder(order), "familyBelongsToOrder"))
                .whenNotNull(genus, g -> g
                        .readModel(genus, "genus")
                        .notNull(family, "genus:family")
                        .isTrue(genus.belongsToFamily(family), "genusBelongsToFamily"))
                .whenNotNull(species, s -> s
                        .readModel(species, "species")
                        .notNull(genus, "species:genus")
                        .isTrue(species.belongsToGenus(genus), "speciesBelongsToGenus"));
    }
}
```

- [ ] **Step 4: Run to verify pass.** `mvn -pl domains/plants/plants-api -am test -Dtest=PlantTest` → PASS.

- [ ] **Step 5: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java`.

---

### Task 3: `PlantQuery.getByName` + `PlantFactory` + `PlantQueryImpl` wiring

**Files:**
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantQuery.java` (add `Optional<Plant> getByName(PlantRankName rankName);`)
- Create: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java`
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java` (add `PlantFactory` field built in ctor from the four rank queries; implement `getByName`)
- Test: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java`

**Interfaces:**
- Consumes: `PlantQuery.SpeciesQuery.getByName`, `GenusQuery.getByName`, `FamilyQuery.getByName`, `OrderQuery.getByName` (all inherited `EntityQuery.getByName`); `Plant.empty()` + `with*`; the four `*View.of(entity)`; `PlantSpecies.genusName()`, `PlantGenus.familyName()`, `PlantFamily.orderName()`.
- Produces: `PlantQuery.getByName(PlantRankName) → Optional<Plant>`; package-private `PlantFactory` with `buildByName(PlantRankName) → Optional<Plant>`.

- [ ] **Step 1: Add the port.** In `PlantQuery.java` add (after `genera()`), keeping the existing `import java.util.Optional;`:

```java
    /**
     * The composed {@link Plant} read model for a taxon at the given rank — the rank
     * record plus its resolved ancestry spine (later chunks fold in features, children,
     * role, images, and species extras). Empty when no entity exists at that rank name.
     */
    Optional<Plant> getByName(PlantRankName rankName);
```

- [ ] **Step 2: Write the failing factory test** `PlantFactoryTest.java` (wire the four rank query impls directly against seeded mocks — mirror `PlantsTestContext.createPlantQuery`; do **not** use `PlantsTestContext`, which lives in another module and would create a Maven cycle for a core test):

```java
package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantFactoryTest {

    private PlantFactory factory() {
        NaturalistDatabase db = NaturalistDatabase.create();
        PlantQuery.GenusQuery genusQuery =
                new PlantGenusQueryImpl(new PlantGenusRepositoryMock(db));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(db), genusQuery);
        PlantQuery.FamilyQuery familyQuery =
                new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(db));
        PlantQuery.OrderQuery orderQuery =
                new PlantOrderQueryImpl(new PlantOrderRepositoryMock(db));
        return new PlantFactory(speciesQuery, genusQuery, familyQuery, orderQuery);
    }

    @Test
    void buildByName_speciesName_composesFullAncestrySpine() {
        Plant plant = factory().buildByName(
                com.naturalist.plants.PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.speciesName()).contains(PlantSpeciesName.of("aristolochia-californica"));
        assertThat(plant.genusName()).contains(PlantGenusName.of("aristolochia"));
        assertThat(plant.familyName()).contains(PlantFamilyName.of("aristolochiaceae"));
        assertThat(plant.orderName()).contains(PlantOrderName.of("piperales"));
    }

    @Test
    void buildByName_orderName_composesOrderOnly() {
        Plant plant = factory().buildByName(PlantOrderName.of("lamiales")).orElseThrow();
        assertThat(plant.orderName()).contains(PlantOrderName.of("lamiales"));
        assertThat(plant.familyName()).isEmpty();
        assertThat(plant.genusName()).isEmpty();
        assertThat(plant.speciesName()).isEmpty();
    }

    @Test
    void buildByName_unknownName_isEmpty() {
        assertThat(factory().buildByName(PlantOrderName.of("unobtainium-ales"))).isEmpty();
    }

    @Test
    void buildByName_nullName_throws() {
        assertThat(catchThrowable(() -> factory().buildByName(null)))
                .isInstanceOf(com.naturalist.observability.InvariantViolationException.class);
    }
}
```

(Confirm the seeded chain `aristolochia-californica → aristolochia → aristolochiaceae → piperales` still holds in the catalogs — it is the `CaliforniaPipevine` fixture; if the genus slug differs, read `plant-species.json`/`plant-genera.json` and adjust. Confirm the mock/impl class names `PlantOrderQueryImpl`/`PlantOrderRepositoryMock` etc. against `PlantsTestContext.createPlantQuery` — they are package-private in `com.naturalist.plants`, visible to this test. Confirm `InvariantViolationException`'s package.)

- [ ] **Step 3: Run to verify failure.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest` → FAIL (`PlantFactory` undefined).

- [ ] **Step 4: Implement `PlantFactory`** (mirror `InsectFactory.buildByName` cut to the rank chain — exhaustive 4-permit switch, no `default`):

```java
package com.naturalist.plants;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Name-keyed, rank-polymorphic assembly of the {@link Plant} read model. Resolves the rank
 * chain from the given {@link PlantRankName} upward to the order. Chunk 1 composes only the
 * ancestry spine; base attributes (images, features, role) and children fold in over later
 * chunks. Mirrors {@code InsectFactory}.
 */
class PlantFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantQuery.SpeciesQuery speciesQuery;
    private final PlantQuery.GenusQuery genusQuery;
    private final PlantQuery.FamilyQuery familyQuery;
    private final PlantQuery.OrderQuery orderQuery;

    PlantFactory(PlantQuery.SpeciesQuery speciesQuery,
                 PlantQuery.GenusQuery genusQuery,
                 PlantQuery.FamilyQuery familyQuery,
                 PlantQuery.OrderQuery orderQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(orderQuery, "orderQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
        this.orderQuery = orderQuery;
    }

    Optional<Plant> buildByName(PlantRankName name) {
        observer.arguments("buildByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return switch (name) {
            case PlantSpeciesName sn -> speciesQuery.getByName(sn).map(s -> observe(
                    resolveGenus(Plant.empty().withSpecies(PlantSpeciesView.of(s)), s.genusName())));
            case PlantGenusName gn -> genusQuery.getByName(gn).map(g -> observe(
                    resolveFamily(Plant.empty().withGenus(PlantGenusView.of(g)), g.familyName())));
            case PlantFamilyName fn -> familyQuery.getByName(fn).map(f -> observe(
                    resolveOrder(Plant.empty().withFamily(PlantFamilyView.of(f)), f.orderName())));
            case PlantOrderName on -> orderQuery.getByName(on).map(o -> observe(
                    Plant.empty().withOrder(PlantOrderView.of(o))));
        };
    }

    private Plant resolveGenus(Plant plant, PlantGenusName genusName) {
        return genusQuery.getByName(genusName)
                .map(genus -> resolveFamily(plant.withGenus(PlantGenusView.of(genus)), genus.familyName()))
                .orElse(plant);
    }

    private Plant resolveFamily(Plant plant, PlantFamilyName familyName) {
        return familyQuery.getByName(familyName)
                .map(family -> resolveOrder(plant.withFamily(PlantFamilyView.of(family)), family.orderName()))
                .orElse(plant);
    }

    private Plant resolveOrder(Plant plant, PlantOrderName orderName) {
        return orderQuery.getByName(orderName)
                .map(order -> plant.withOrder(PlantOrderView.of(order)))
                .orElse(plant);
    }

    private Plant observe(Plant plant) {
        observer.observable(plant, "plant").observe(Level.WARN);
        return plant;
    }
}
```

(Confirm `PlantRankName`'s `permits` clause is exactly these four — if the sealed switch is not exhaustive the compiler will say so; do not add a `default`. Confirm `EntityQuery.getByName` returns `Optional<Entity>` on each rank query — it is the inherited select the controller already uses via `plantQuery.orders().getByName(...)`.)

- [ ] **Step 5: Wire `getByName` in `PlantQueryImpl`.** Add a `private final PlantFactory plantFactory;` field; at the end of the constructor (after the field assignments) add:

```java
        this.plantFactory = new PlantFactory(
                plantEntityQuery, plantGenusEntityQuery, plantFamilyEntityQuery, plantOrderEntityQuery);
```

and add the accessor:

```java
    @Override
    public Optional<Plant> getByName(PlantRankName name) {
        return plantFactory.buildByName(name);
    }
```

Add `import java.util.Optional;` to `PlantQueryImpl.java` if absent. `PlantsTestContext` needs **no change** — `PlantQueryImpl`'s constructor signature is unchanged (the factory is built internally from the queries it already receives).

- [ ] **Step 6: Run to verify pass.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest` → PASS. Then the whole plants build: `mvn -pl domains/plants/plants-core -am test` → green (confirms `PlantQueryImpl` still compiles + no other plants-core test broke).

- [ ] **Step 7: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantQuery.java domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java`.

---

## Self-Review

**Spec coverage (design Chunk 1):**
- `PlantTaxonView` sealed + 4 permits (2-arg `of`, no `features()` slot) → Task 1. ✓
- `Plant` ReadModel carrying only the rank chain + `empty()`/`with*`/`invariants()` → Task 2. ✓
- `PlantQuery.getByName` + `PlantFactory` composing the ancestry spine + `PlantQueryImpl` wiring → Task 3. ✓
- No console changes; features/children/role/images/extras deferred → stated in Global Constraints + `Plant` javadoc. ✓

**Placeholder scan:** The "confirm constructor arg order / seeded chain / class names / `PlantRankName` permits" notes are verification instructions with concrete anchors (the `CaliforniaPipevine` fixture, `PlantsTestContext.createPlantQuery`, the sealed-switch compiler check), not placeholders. All code blocks are complete.

**Type consistency:** `PlantTaxonView.name()→PlantRankName` and `images()→ImageCollection` across all four permits; `Plant` uses `PlantOrderView/PlantFamilyView/PlantGenusView/PlantSpeciesView` consistently in components, `with*`, and `invariants()`; `PlantFactory(speciesQuery, genusQuery, familyQuery, orderQuery)` matches the `PlantQueryImpl` construction call and the `PlantFactoryTest` wiring; `getByName(PlantRankName)→Optional<Plant>` matches in the port, the impl, and the factory `buildByName`.

**Notes for the executor:** the FK-consistency check lives in the views (plants entities have no `belongsTo*` predicates); the switch in `PlantFactory` is exhaustive over the four `PlantRankName` permits with no subspecies arm and no `default`; `PlantsTestContext` is deliberately left untouched.
