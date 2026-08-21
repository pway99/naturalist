# `Plant` Read Model — Chunk 2: Ancestry Features + Controller Switch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the `Plant` read model its ancestry-inherited field marks — a `PlantFeatureView` (lineage-composite, ancestor-first) composed into `Plant`, produced by upgrading the feature query from direct-rank to an ancestry walk — and switch the four rank-page handlers to read the rank record + features off the resolved `Plant`, rendering a `plants/features.jte` section.

**Architecture:** Faithful mirror of the insects feature path. `PlantFeatureView(subject, List<RankGroup>)` mirrors `InsectFeatureView`; `PlantAncestryResolver` mirrors `InsectAncestryResolver` (4-permit `parentOf`); `PlantFeatureQueryImpl.findByRankName` mirrors `InsectFeatureQueryImpl.findByRankName` (two batched calls over the ancestry set, grouped ancestor-first, ordinal-ordered). `Plant` gains a `@Nullable PlantFeatureView features` component composed on every rank by `PlantFactory` (a `base(name)` step). The controller resolves one `Plant` per rank via `getByName` and reads `plant.features()`.

**Tech Stack:** Java 21 records + sealed types, JUnit 5 + AssertJ, JTE templates, the Observer framework, `NaturalistDatabase` + repository mocks seeded from JSON. Build: scoped `mvn -pl <module> -am test`; the user runs full `mvn verify`.

**Design of record:** [2026-08-20-plant-read-model-design.md](2026-08-20-plant-read-model-design.md) (Chunk 2 of 6). Prior chunk shipped: commit `73cc06ed` (the `Plant`/`PlantTaxonView`/`getByName` skeleton).

## Global Constraints

- **Insects is the reference; plants moves, insects holds still** (`domains/plants/CLAUDE.md` rule 1). Mirror `InsectFeatureView`, `InsectAncestryResolver`, `InsectFeatureQueryImpl.findByRankName`, `insects/features.jte`, `RankLabel`, adapted to the **four** `PlantRankName` permits (`PlantOrderName`/`PlantFamilyName`/`PlantGenusName`/`PlantSpeciesName` — no subspecies; `parentOf` is a 4-arm exhaustive switch).
- **Server-side (application-layer) join, batched.** `findByRankName` makes exactly two batched repository calls — `assignmentRepository.getByRankNames(ancestry)` (1) and `featureRepository.getByEntityNameSet(allIds)` (1) — never a per-rank or per-assignment fetch (`domains/CLAUDE.md` "fan-out must batch"). The batched substrate already exists: `PlantRepository.FeatureAssignmentRepository.getByRankNames(Set<PlantRankName>)` and the inherited `FeatureRepository.getByEntityNameSet(Set<PlantFeatureId>)`.
- **`findByRankName` REPLACES `forRankName`.** The Chunk-1-era direct-rank `FeatureQuery.forRankName(PlantRankName) → FeatureCollection` has zero consumers (grep-confirmed) — it is renamed+upgraded to `findByRankName(PlantRankName) → PlantFeatureView`, not kept alongside. Remove the now-unused `FeatureCollection` import from `PlantQuery.java`. (`FeatureCollection` the type stays in `PlantEntityCollections` for Chunk 6 cleanup to assess.)
- **Strip `@DomainService` from `PlantFeatureQueryImpl`.** It is manually constructed in `PlantsTestContext`, and once it depends on `PlantAncestryResolver` (which depends on three queries) the annotation is exactly the factory-backed-query shape that reds every `@SpringBootTest` in management-console (see the project memory on `@DomainService` + factory queries). Insects' `InsectFeatureQueryImpl` carries no such annotation — match it.
- **Plants features render as PLAIN TEXT.** No glossary linker (plants has none; the insects template's `GlossaryLinker` is insects-only). The plants `features.jte` takes only `PlantFeatureView`.
- **Record-arity ripple.** `Plant` grows from 4 to 5 components. Every `new Plant(...)` / `Plant.empty()` / `with*` site updates — grep `grep -rn "new Plant(" --include=*.java` (expected: only `Plant.java` itself; confirm). ([[feedback_record_arity_ripple]])
- **CSRF / `page.jte` classpath trap.** `features.jte` must reference only its own `PlantFeatureView` param — no Spring Security or servlet types (the [[project_shared_layout_jte_classpath]] / insects CSRF-as-request-attribute rule). It is a pure display fragment; it takes no `CsrfToken`.
- **Build gotchas (this effort):** `InvariantViolationException` is in `com.naturalist.exception`; `Description` is a plain 4-arg record (no `of()`); `mvn -pl … -am -Dtest=X` needs `-Dsurefire.failIfNoSpecifiedTests=false`.

---

### Task 1: `PlantFeatureView` (+ nested `RankGroup`)

**Files:**
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantFeatureView.java`
- Test: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantFeatureViewTest.java`

**Interfaces:**
- Consumes: `PlantRankName`, `PlantFeature` (plants-api).
- Produces: `record PlantFeatureView(PlantRankName subject, List<RankGroup> groups) implements ReadModel` with nested `record RankGroup(PlantRankName rank, List<PlantFeature> features) implements ValueObject`.

- [ ] **Step 1: Write the failing test** `PlantFeatureViewTest.java`:

```java
package com.naturalist.plants;

import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlantFeatureViewTest {

    private final Observer observer = Observer.forClass(PlantFeatureViewTest.class);

    @Test
    void validView_hasNoViolations() {
        PlantFeature ray = PlantFeature.of(PlantFeatureId.create(), "ray florets");
        PlantFeatureView view = new PlantFeatureView(
                PlantGenusName.of("helianthus"),
                List.of(new PlantFeatureView.RankGroup(PlantFamilyName.of("asteraceae"), List.of(ray))));
        assertThat(observer.forMethod("valid").observable(view, "view").violations()).isEmpty();
    }

    @Test
    void nullSubject_reportsViolation() {
        var mo = observer.forMethod("null");
        PlantFeatureView view = new PlantFeatureView(null, List.of());
        assertThat(mo.observable(view, "view").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".view.subject");
    }
}
```

(Confirm `PlantFeature.of(PlantFeatureId, String)` and `PlantFeatureId.create()` signatures against the shipped S3 `PlantFeature`/`PlantFeatureId`.)

- [ ] **Step 2: Run to verify failure.** `mvn -pl domains/plants/plants-api -am test -Dtest=PlantFeatureViewTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL (`PlantFeatureView` undefined).

- [ ] **Step 3: Implement `PlantFeatureView`** (mirror `InsectFeatureView`):

```java
package com.naturalist.plants;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lineage-composite, display-ready view of identification features at a given rank.
 * Composites the subject rank's own field marks with those inherited from its ancestors,
 * pre-grouped by contributing rank so the console can render directly without reshaping.
 * <p>
 * Ordering contract: ancestor groups first (most general), descendant groups last (most
 * specific). Within a group, features are ordered by assignment ordinal. Mirrors
 * {@code InsectFeatureView}.
 */
public record PlantFeatureView(
        PlantRankName subject,
        List<RankGroup> groups
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(groups, "groups");
    }

    /** The field marks contributed at one rank in the lineage, ordinal-ordered. */
    public record RankGroup(
            PlantRankName rank,
            List<PlantFeature> features
    ) implements ValueObject {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .identifier(rank, "rank")
                    .notNull(features, "features");
        }
    }
}
```

- [ ] **Step 4: Run to verify pass.** Same command → PASS.

- [ ] **Step 5: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantFeatureView.java domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantFeatureViewTest.java`.

---

### Task 2: `PlantAncestryResolver`

**Files:**
- Create: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantAncestryResolver.java`
- Test: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantAncestryResolverTest.java`

**Interfaces:**
- Consumes: `PlantQuery.SpeciesQuery.getByName`, `GenusQuery.getByName`, `FamilyQuery.getByName`; `PlantSpecies.genusName()`, `PlantGenus.familyName()`, `PlantFamily.orderName()`; `RankAncestry.ancestry` (`kernels/taxonomy`).
- Produces: package-private `PlantAncestryResolver(SpeciesQuery, GenusQuery, FamilyQuery)` with `Set<PlantRankName> ancestry(PlantRankName)` (ancestor-first, ordered).

- [ ] **Step 1: Write the failing test** `PlantAncestryResolverTest.java` (wire the rank query impls directly against seeded mocks, mirroring `PlantFactoryTest`):

```java
package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantAncestryResolverTest {

    private PlantAncestryResolver resolver() {
        NaturalistDatabase db = NaturalistDatabase.create();
        PlantQuery.GenusQuery genusQuery = new PlantGenusQueryImpl(new PlantGenusRepositoryMock(db));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(db), genusQuery);
        PlantQuery.FamilyQuery familyQuery = new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(db));
        return new PlantAncestryResolver(speciesQuery, genusQuery, familyQuery);
    }

    @Test
    void ancestry_ofGenus_isAncestorFirst() {
        // helianthus → asteraceae → asterales; returned ancestor-first (order → … → subject).
        assertThat(resolver().ancestry(PlantGenusName.of("helianthus")))
                .containsExactly(
                        PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"),
                        PlantGenusName.of("helianthus"));
    }

    @Test
    void ancestry_ofOrder_isSelfOnly() {
        assertThat(resolver().ancestry(PlantOrderName.of("asterales")))
                .containsExactly(PlantOrderName.of("asterales"));
    }
}
```

(Confirm the seeded chain `helianthus → asteraceae → asterales` against `plant-genera.json`/`plant-families.json`; adjust to the real parents if different. `containsExactly` on the returned ordered `Set` asserts iteration order — the resolver must return a `LinkedHashSet`.)

- [ ] **Step 2: Run to verify failure.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantAncestryResolverTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL.

- [ ] **Step 3: Implement `PlantAncestryResolver`** (mirror `InsectAncestryResolver`, 4-arm `parentOf`, no subspecies):

```java
package com.naturalist.plants;

import com.naturalist.taxonomy.RankAncestry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The plants wrapper over {@link RankAncestry}: the single place the plant FK chain
 * (species→genus→family→order) is encoded, as {@link #parentOf}. Mirrors
 * {@code InsectAncestryResolver}.
 */
class PlantAncestryResolver {

    private final PlantQuery.SpeciesQuery speciesQuery;
    private final PlantQuery.GenusQuery genusQuery;
    private final PlantQuery.FamilyQuery familyQuery;

    PlantAncestryResolver(PlantQuery.SpeciesQuery speciesQuery,
                          PlantQuery.GenusQuery genusQuery,
                          PlantQuery.FamilyQuery familyQuery) {
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    /** The rank's Linnaean ancestry as an ancestor-first ordered set (order → … → subject). */
    Set<PlantRankName> ancestry(PlantRankName rankName) {
        List<PlantRankName> subjectFirst = RankAncestry.ancestry(rankName, this::parentOf);
        LinkedHashSet<PlantRankName> ancestorFirst = new LinkedHashSet<>();
        for (int i = subjectFirst.size() - 1; i >= 0; i--) {
            ancestorFirst.add(subjectFirst.get(i));
        }
        return ancestorFirst;
    }

    /** The plant FK chain: the parent rank of a given rank, empty at the order (or a gap). */
    private Optional<PlantRankName> parentOf(PlantRankName rankName) {
        return switch (rankName) {
            case PlantSpeciesName s -> speciesQuery.getByName(s).map(PlantSpecies::genusName);
            case PlantGenusName g -> genusQuery.getByName(g).map(PlantGenus::familyName);
            case PlantFamilyName f -> familyQuery.getByName(f).map(PlantFamily::orderName);
            case PlantOrderName o -> Optional.empty();
        };
    }
}
```

(Confirm `RankAncestry.ancestry(R subject, Function<R,Optional<R>> parentOf)` signature in `kernels/taxonomy/.../RankAncestry.java` — it returns subject-first `List<R>`. The switch is exhaustive over the four permits; no `default`.)

- [ ] **Step 4: Run to verify pass.** Same command → PASS.

- [ ] **Step 5: Stage (do NOT commit).** `git add domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantAncestryResolver.java domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantAncestryResolverTest.java`.

---

### Task 3: Upgrade `PlantFeatureQueryImpl` to ancestry `findByRankName`

**Files:**
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantQuery.java` (replace `forRankName` with `findByRankName`; drop the `FeatureCollection` import)
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFeatureQueryImpl.java` (strip `@DomainService`; add `PlantAncestryResolver` dep; replace the body)
- Modify: `domains/plants/plants-test-context/src/main/java/com/naturalist/plants/PlantsTestContext.java` (`createPlantQuery`: build the resolver, pass it to the 3-arg `PlantFeatureQueryImpl`)
- Test: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFeatureQueryImplTest.java` (rewrite for `findByRankName`)

**Interfaces:**
- Consumes: `PlantAncestryResolver.ancestry` (Task 2); `PlantFeatureView` (Task 1); `FeatureAssignmentRepository.getByRankNames(Set)`, `FeatureRepository.getByEntityNameSet(Set)`.
- Produces: `PlantQuery.FeatureQuery.findByRankName(PlantRankName) → PlantFeatureView` (replacing `forRankName`).

- [ ] **Step 1: Change the port.** In `PlantQuery.java`, replace the `FeatureQuery` interface body and remove the now-unused `import com.naturalist.plants.PlantEntityCollections.FeatureCollection;` line:

```java
    interface FeatureQuery {

        /**
         * The lineage-composite field marks for a taxon at the given rank — the rank's own
         * assignments plus those inherited from its ancestors, grouped ancestor-first,
         * ordinal-ordered within a group. Mirrors {@code InsectQuery.FeatureQuery.findByRankName}.
         */
        PlantFeatureView findByRankName(PlantRankName subject);
    }
```

- [ ] **Step 2: Rewrite the test** `PlantFeatureQueryImplTest.java` — the impl now needs a resolver, so wire the rank query impls (mirror `PlantAncestryResolverTest`), and assert the ancestry composite for the seeded helianthus chain:

```java
package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantFeatureQueryImplTest {

    private PlantFeatureQueryImpl featureQuery() {
        NaturalistDatabase db = NaturalistDatabase.create();
        PlantQuery.GenusQuery genusQuery = new PlantGenusQueryImpl(new PlantGenusRepositoryMock(db));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(db), genusQuery);
        PlantQuery.FamilyQuery familyQuery = new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(db));
        PlantAncestryResolver resolver = new PlantAncestryResolver(speciesQuery, genusQuery, familyQuery);
        return new PlantFeatureQueryImpl(
                new PlantFeatureRepositoryMock(db), new PlantFeatureAssignmentRepositoryMock(db), resolver);
    }

    @Test
    void findByRankName_composesAncestryFirst_ordinalOrderedWithinGroup() {
        // Seeded: ORDER asterales=[composite inflorescence]; FAMILY asteraceae=[ray florets(0),
        // composite inflorescence(1)]; GENUS helianthus=[ray florets]. Ancestor-first.
        PlantFeatureView view = featureQuery().findByRankName(PlantGenusName.of("helianthus"));

        assertThat(view.subject()).isEqualTo(PlantGenusName.of("helianthus"));
        assertThat(view.groups().stream().map(g -> g.rank()))
                .containsExactly(
                        PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"),
                        PlantGenusName.of("helianthus"));
        assertThat(view.groups().get(1).features().stream().map(PlantFeature::value))
                .containsExactly("ray florets", "composite inflorescence"); // ordinal 0 then 1
        assertThat(view.groups().get(2).features().stream().map(PlantFeature::value))
                .containsExactly("ray florets");
    }

    @Test
    void findByRankName_noAssignmentsInLineage_returnsEmptyGroups() {
        // piperales carries no feature assignments anywhere in its lineage.
        PlantFeatureView view = featureQuery().findByRankName(PlantOrderName.of("piperales"));
        assertThat(view.groups()).isEmpty();
    }

    @Test
    void findByRankName_rejectsNull() {
        assertThat(catchThrowable(() -> featureQuery().findByRankName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }
}
```

(Confirm the seeded values against `plant-feature-assignments.json`/`plant-features.json` — they are: features `ray florets`/`opposite leaves`/`composite inflorescence`/`pinnately compound leaves`; asteraceae FAMILY = ray florets(0)+composite inflorescence(1); asterales ORDER = composite inflorescence; helianthus GENUS = ray florets.)

- [ ] **Step 3: Run to verify failure.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFeatureQueryImplTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL to compile (3-arg ctor / `findByRankName` absent).

- [ ] **Step 4: Rewrite `PlantFeatureQueryImpl`** (strip `@DomainService`, add resolver, mirror `InsectFeatureQueryImpl`):

```java
package com.naturalist.plants;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lineage-composite feature resolution — walks the ancestry chain from the subject rank up
 * to the order, gathers feature assignments at each rank in one batched call, resolves the
 * {@link PlantFeature} entities in one batched call, and composes a display-ready
 * {@link PlantFeatureView} — one {@link PlantFeatureView.RankGroup} per contributing rank,
 * ancestor-first, ordinal-ordered within a group. Mirrors {@code InsectFeatureQueryImpl}.
 */
class PlantFeatureQueryImpl implements PlantQuery.FeatureQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantRepository.FeatureRepository featureRepository;
    private final PlantRepository.FeatureAssignmentRepository assignmentRepository;
    private final PlantAncestryResolver ancestryResolver;

    PlantFeatureQueryImpl(PlantRepository.FeatureRepository featureRepository,
                          PlantRepository.FeatureAssignmentRepository assignmentRepository,
                          PlantAncestryResolver ancestryResolver) {
        observer.arguments("constructor", i -> i
                        .notNull(featureRepository, "featureRepository")
                        .notNull(assignmentRepository, "assignmentRepository")
                        .notNull(ancestryResolver, "ancestryResolver"))
                .throwWhenInvalid();
        this.featureRepository = featureRepository;
        this.assignmentRepository = assignmentRepository;
        this.ancestryResolver = ancestryResolver;
    }

    @Override
    public PlantFeatureView findByRankName(PlantRankName subject) {
        observer.arguments("findByRankName", i -> i.identifier(subject, "subject")).throwWhenInvalid();

        Set<PlantRankName> ancestry = ancestryResolver.ancestry(subject); // ancestor-first, ordered

        Map<PlantRankName, List<PlantFeatureAssignment>> byRank =
                assignmentRepository.getByRankNames(ancestry).stream()
                        .collect(Collectors.groupingBy(PlantFeatureAssignment::rankName));

        Set<PlantFeatureId> allIds = byRank.values().stream().flatMap(List::stream)
                .map(PlantFeatureAssignment::featureId).collect(Collectors.toSet());
        Map<PlantFeatureId, PlantFeature> resolved = new HashMap<>();
        if (!allIds.isEmpty()) {
            for (PlantFeature f : featureRepository.getByEntityNameSet(allIds)) {
                resolved.put(f.id(), f);
            }
        }

        List<PlantFeatureView.RankGroup> groups = new ArrayList<>();
        for (PlantRankName rank : ancestry) {
            List<PlantFeatureAssignment> atRank = byRank.getOrDefault(rank, List.of());
            if (atRank.isEmpty()) continue;
            List<PlantFeature> features = atRank.stream()
                    .sorted(Comparator.comparingInt(PlantFeatureAssignment::ordinal))
                    .map(x -> resolved.get(x.featureId()))
                    .filter(Objects::nonNull)
                    .toList();
            if (!features.isEmpty()) {
                groups.add(new PlantFeatureView.RankGroup(rank, features));
            }
        }

        PlantFeatureView view = new PlantFeatureView(subject, List.copyOf(groups));
        observer.observable(view, "featureView").observe(Level.WARN);
        return view;
    }
}
```

(Confirm `PlantFeature.id()` returns `PlantFeatureId` and `PlantFeatureAssignment.rankName()/featureId()/ordinal()` accessor names against the shipped S3 records.)

- [ ] **Step 5: Rewire `PlantsTestContext.createPlantQuery`.** Where it currently builds `featureQuery` (the 2-arg `new PlantFeatureQueryImpl(featureRepo, assignmentRepo)`), build the resolver first and pass it. The rank queries `entityQuery`(species), `genusQuery`, `familyQuery` are already local variables above that line:

```java
        PlantAncestryResolver ancestryResolver =
                new PlantAncestryResolver(entityQuery, genusQuery, familyQuery);
        PlantQuery.FeatureQuery featureQuery =
                new PlantFeatureQueryImpl(new PlantFeatureRepositoryMock(db),
                        new PlantFeatureAssignmentRepositoryMock(db), ancestryResolver);
```

(`PlantsTestContext` is split-package `com.naturalist.plants` and already reaches the package-private `PlantFeatureQueryImpl`/mocks — it reaches package-private `PlantAncestryResolver` the same way. Confirm the species-query local is named `entityQuery` per the current file.)

- [ ] **Step 6: Run to verify pass + module green.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFeatureQueryImplTest -Dsurefire.failIfNoSpecifiedTests=false` → PASS. Then `mvn -pl domains/plants/plants-test-context -am test` → green (proves the rewired context compiles + the whole plants read side still passes).

- [ ] **Step 7: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantQuery.java domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFeatureQueryImpl.java domains/plants/plants-test-context/src/main/java/com/naturalist/plants/PlantsTestContext.java domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFeatureQueryImplTest.java`.

---

### Task 4: Compose `features` into `Plant` via `PlantFactory`

**Files:**
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java` (add `@Nullable PlantFeatureView features` — 5th component)
- Modify: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java` (arity + a features-descent test)
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java` (add `FeatureQuery` dep; `base(name)` composes features)
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java` (pass `featureQuery` to `new PlantFactory(...)`)
- Modify: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java` (wire the featureQuery; assert `plant.features()` present)

**Interfaces:**
- Consumes: `PlantQuery.FeatureQuery.findByRankName` (Task 3); `PlantFeatureView` (Task 1).
- Produces: `Plant.features() → @Nullable PlantFeatureView`, `Plant.withFeatures(@Nullable PlantFeatureView)`; `PlantFactory(SpeciesQuery, GenusQuery, FamilyQuery, OrderQuery, FeatureQuery)`.

- [ ] **Step 1: Grep the arity sites.** `grep -rn "new Plant(" --include=*.java domains` — confirm the only site is `Plant.java` (its `empty()` + four `with*`). If any other site exists, it updates in this task.

- [ ] **Step 2: Write the failing tests.** Append to `PlantTest.java`:

```java
    @Test
    void withFeatures_carriesFeatureView() {
        PlantFeatureView fv = new PlantFeatureView(PlantOrderName.of("asterales"), java.util.List.of());
        Plant plant = Plant.empty().withFeatures(fv);
        assertThat(plant.features()).isEqualTo(fv);
    }

    @Test
    void emptyPlant_hasNullFeatures_andNoViolations() {
        Plant plant = Plant.empty();
        assertThat(plant.features()).isNull();
        assertThat(observer.forMethod("emptyFeatures").observable(plant, "plant").violations()).isEmpty();
    }
```

And in `PlantFactoryTest.java`, extend the factory wiring to pass a real `FeatureQuery` and assert features compose (replace the `factory()` helper's `new PlantFactory(...)` call):

```java
        PlantAncestryResolver resolver = new PlantAncestryResolver(speciesQuery, genusQuery, familyQuery);
        PlantQuery.FeatureQuery featureQuery = new PlantFeatureQueryImpl(
                new PlantFeatureRepositoryMock(db), new PlantFeatureAssignmentRepositoryMock(db), resolver);
        return new PlantFactory(speciesQuery, genusQuery, familyQuery, orderQuery, featureQuery);
```

add a test:

```java
    @Test
    void buildByName_composesAncestryFeatures() {
        Plant plant = factory().buildByName(PlantGenusName.of("helianthus")).orElseThrow();
        assertThat(plant.features()).isNotNull();
        assertThat(plant.features().groups().stream().map(g -> g.rank()))
                .containsExactly(PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"), PlantGenusName.of("helianthus"));
    }
```

- [ ] **Step 3: Run to verify failure.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL (compile: `withFeatures`/5-arg factory absent).

- [ ] **Step 4: Add `features` to `Plant`.** Add `@Nullable PlantFeatureView features` as the 5th record component (after `species`). Update `empty()` to pass a 5th `null`; update each of the four existing `with*` to thread `features` through; add `withFeatures`:

```java
    public Plant withFeatures(@Nullable PlantFeatureView features) {
        return new Plant(order, family, genus, species, features);
    }
```

Each existing `with*` becomes `new Plant(order, family, genus, species, features)` with its one component swapped. In `invariants()`, add after the `species` block:

```java
                .whenNotNull(features, f -> f.readModel(features, "features"))
```

(`PlantFeatureView` is a `ReadModel`, so `.readModel(...)` is the right descent — mirror `Insect.invariants()`'s `features` line.)

- [ ] **Step 5: Compose features in `PlantFactory`.** Add `private final PlantQuery.FeatureQuery featureQuery;` as the 5th constructor param (null-checked in the `observer.arguments` block + assigned). Add a `base` helper and route every arm through it:

```java
    private Plant base(PlantRankName name) {
        return Plant.empty().withFeatures(featureQuery.findByRankName(name));
    }
```

and change each `switch` arm's initial `Plant.empty()` to `base(<name>)`, e.g.:

```java
            case PlantSpeciesName sn -> speciesQuery.getByName(sn).map(s -> observe(
                    resolveGenus(base(sn).withSpecies(PlantSpeciesView.of(s)), s.genusName())));
            case PlantGenusName gn -> genusQuery.getByName(gn).map(g -> observe(
                    resolveFamily(base(gn).withGenus(PlantGenusView.of(g)), g.familyName())));
            case PlantFamilyName fn -> familyQuery.getByName(fn).map(f -> observe(
                    resolveOrder(base(fn).withFamily(PlantFamilyView.of(f)), f.orderName())));
            case PlantOrderName on -> orderQuery.getByName(on).map(o -> observe(
                    base(on).withOrder(PlantOrderView.of(o))));
```

(Features are keyed to the **subject** rank `name`, not the ancestors — `findByRankName(subject)` already walks the lineage. `resolveGenus/Family/Order` are unchanged; they only fill the rank-chain views.)

- [ ] **Step 6: Pass `featureQuery` in `PlantQueryImpl`.** Update the `new PlantFactory(...)` construction (added in Chunk 1) to append `this.featureQuery` (already a field on `PlantQueryImpl`):

```java
        this.plantFactory = new PlantFactory(
                plantEntityQuery, plantGenusEntityQuery, plantFamilyEntityQuery, plantOrderEntityQuery, featureQuery);
```

- [ ] **Step 7: Run to verify pass + module green.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest,PlantTest -Dsurefire.failIfNoSpecifiedTests=false` → PASS. Then `mvn -pl domains/plants/plants-core -am test` → green.

- [ ] **Step 8: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java`.

---

### Task 5: `PlantRankLabel` + `plants/features.jte` fragment + template includes

**Files:**
- Create: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantRankLabel.java`
- Create: `domains/plants/plants-console/src/main/jte/plants/features.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/orders/detail.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/families/detail.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/genera/detail.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/detail.jte` (species)

**Interfaces:**
- Consumes: `PlantFeatureView` (Task 1), `PlantRankName`.
- Produces: `PlantRankLabel.of(PlantRankName) → String`; a `plants.features` JTE fragment taking `@param PlantFeatureView features = null`.

- [ ] **Step 1: Create `PlantRankLabel`** (mirror insects `RankLabel`):

```java
package com.naturalist.plants.console;

import com.naturalist.plants.PlantRankName;

import java.util.Locale;

/** Presentation helper: the human label for a rank ("Order", "Family"). Mirrors the insects RankLabel. */
public final class PlantRankLabel {
    private PlantRankLabel() {}

    public static String of(PlantRankName rankName) {
        String name = rankName.rank().name();               // e.g. "ORDER"
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
```

(Confirm `PlantRankName.rank()` returns a `LinealRank` whose `.name()` is the enum constant, matching insects.)

- [ ] **Step 2: Create `plants/features.jte`** (mirror `insects/features.jte`, plain text — NO glossary linker, NO Spring types):

```jte
@import com.naturalist.plants.PlantFeatureView
@import com.naturalist.plants.console.PlantRankLabel

@param PlantFeatureView features = null

@if(features != null && !features.groups().isEmpty())
    <section class="field-marks">
        <h2>Field Marks</h2>
        <p class="field-marks-lead">What the identification was based on, from most conspicuous to most diagnostic. Check these against your photo.</p>
        @for(var group : features.groups())
            <div class="field-mark-group">
                <h3>${PlantRankLabel.of(group.rank())} — ${group.rank().value()}</h3>
                <ul>
                    @for(var feature : group.features())
                        <li>${feature.value()}</li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
```

- [ ] **Step 3: Add the param + include to each of the four rank templates.** In `orders/detail.jte`, `families/detail.jte`, `genera/detail.jte`, and `detail.jte`, add near the other `@param` lines:

```jte
@param com.naturalist.plants.PlantFeatureView features = null
```

and place the include where the field-marks section should render (after the description block, matching where insects places it):

```jte
@template.plants.features(features = features)
```

The `= null` default means these templates still render correctly before the controller passes `features` (Task 6) — they simply render no field-marks section.

- [ ] **Step 4: Build the console.** `mvn -pl domains/plants/plants-console -am test` → green (JTE templates compile; `features.jte` + the four includes + `PlantRankLabel` all resolve). If plants-console has template tests that construct these four models, they still pass because `features` defaults to `null`.

- [ ] **Step 5: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantRankLabel.java domains/plants/plants-console/src/main/jte/plants/features.jte domains/plants/plants-console/src/main/jte/plants/orders/detail.jte domains/plants/plants-console/src/main/jte/plants/families/detail.jte domains/plants/plants-console/src/main/jte/plants/genera/detail.jte domains/plants/plants-console/src/main/jte/plants/detail.jte`.

---

### Task 6: Switch the four rank handlers to `getByName` + pass `features`

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java` (`orderDetail`, `familyDetail`, `genusDetail`, species `detail`)

**Interfaces:**
- Consumes: `PlantQuery.getByName(PlantRankName) → Optional<Plant>` (Chunk 1); `Plant.order()/family()/genus()/species()` (returning the `*View` permits) and `Plant.features()`.

- [ ] **Step 1: Switch `orderDetail`.** Replace the entity fetch with the composed `Plant`, extract the rank record from the view for the existing `order` attribute, and add `features`. Everything else (`families`, breadcrumb, cladeTrail, description) stays as-is:

```java
    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, Model model) {
        var orderName = PlantOrderName.of(name);
        var plant = plantQuery.getByName(orderName);
        if (plant.isEmpty() || plant.get().order() == null) {
            return "redirect:/plants";
        }
        PlantOrder order = plant.get().order().order();
        var families = plantQuery.families().forOrderName(orderName).stream()
                .sorted(Comparator.comparing((PlantFamily f) -> f.name().value()))
                .toList();
        model.addAttribute("order", order);
        model.addAttribute("families", families);
        model.addAttribute("features", plant.get().features());
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("cladeTrail", cladeTrailFor(order));
        addDescription(model, order.description());
        return "plants/orders/detail";
    }
```

- [ ] **Step 2: Switch `familyDetail`, `genusDetail`, species `detail` the same way.** Each: `var plant = plantQuery.getByName(<rankName>);` guard `plant.isEmpty() || plant.get().<rank>() == null`; extract the rank entity via `plant.get().family().family()` / `.genus().genus()` / `.species().species()`; add `model.addAttribute("features", plant.get().features());`. Keep every other existing attribute (genera/species children, ecologicalRole, cultivars, programs, constituents, breadcrumb, cladeTrail, description) exactly as they are — those migrate in later chunks. For the species `detail`, the rank record is `PlantSpecies species = plant.get().species().species();` and it feeds `ecologicalRoles().forPlantName(plantName)`, `breadcrumbToSpecies(species)`, `cladeTrailForSpecies(species)`, `addDescription(model, species.description())`, and the cultivar/program/constituent lookups keyed by `plantName` (unchanged — `plantName` is still `PlantSpeciesName.of(name)`).

- [ ] **Step 3: Confirm no stale references.** `grep -n "plantQuery.orders().getByName\|plantQuery.families().getByName\|plantQuery.genera().getByName\|plantQuery.species().getByName" domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java` — the four rank *detail* handlers no longer fetch the rank entity directly (other handlers like `orderList`/`species` list and the breadcrumb helpers `orderOf`/`familyOf`/`genusOf` legitimately still use the rank queries — leave those).

- [ ] **Step 4: Build + verify render.** `mvn -pl domains/plants/plants-console -am test` → green. Then verify the field-marks actually render: start the console preview and open a rank page with seeded features — e.g. `/plants/genera/helianthus` should show three field-mark groups (Order — asterales, Family — asteraceae, Genus — helianthus). (Use the browser preview tools; the controller bootstrap builds `PlantsTestContext` in-process, so no DB is needed. If console preview needs `mvn install` of the domain jars, see [[project_console_run_needs_install]].)

- [ ] **Step 5: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java`.

---

## Self-Review

**Spec coverage (design Chunk 2):**
- `PlantFeatureView` (+ `RankGroup`) → Task 1. ✓
- `PlantAncestryResolver` (4-permit `parentOf`, ancestor-first) → Task 2. ✓
- `PlantFeatureQueryImpl.findByRankName` ancestry walk, two batched calls, replacing `forRankName`; `@DomainService` stripped → Task 3. ✓
- `Plant.features()` composed on every rank by `PlantFactory` → Task 4. ✓
- `plants/features.jte` (plain text) + `PlantRankLabel` + includes → Task 5. ✓
- Controller switches four handlers to `getByName`, reads `plant.features()` → Task 6. ✓

**Placeholder scan:** The "confirm seeded chain / accessor names / `RankAncestry` signature / species-query local name" notes are verification instructions with concrete anchors (the JSON contents are quoted; the chain is `helianthus→asteraceae→asterales`), not placeholders. All code blocks are complete.

**Type consistency:** `PlantFeatureView(PlantRankName subject, List<RankGroup> groups)` + `RankGroup(PlantRankName, List<PlantFeature>)` are used identically in the query, the factory, `Plant`, and the template. `findByRankName(PlantRankName) → PlantFeatureView` matches across the port, impl, factory `base(...)`, and both tests. `PlantFactory(species, genus, family, order, feature)` matches the `PlantQueryImpl` call and `PlantFactoryTest`. `Plant`'s 5th component `features` threads through `empty()`/all five `with*`/`invariants()`.

**Batching invariant honored:** `findByRankName` = `getByRankNames(ancestry)` (1) + `getByEntityNameSet(allIds)` (1); the factory composes one subject `Plant` with one `findByRankName` call per page. No per-element repository access.

**Notes for the executor:** `@DomainService` removal is load-bearing (avoids the management-console `@SpringBootTest` red — project memory); the ancestry `Set` must be a `LinkedHashSet` for ancestor-first ordering; features key to the subject rank only (the walk is inside `findByRankName`); the four rank templates render safely before Task 6 because `features` defaults to `null`.
