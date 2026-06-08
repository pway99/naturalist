# Insect rank-FK normalization — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Normalize the insect rank entities so each references only its immediate parent (`InsectGenus` loses `orderName`; `InsectSpecies` loses `familyName`), reimplement the two skip-level queries as in-core fan-outs, collapse the redundant re-descents in `Insect.invariants()`, and add a typed `parentName : InsectRankName` to `LifeStage`.

**Architecture:** Two independent PRs under one design doc (`docs/plans/2026-06-07-insect-rank-fk-normalization-design.md`). PR-A (Tasks A1–A2) is the rank-FK normalization plus the `Insect.invariants()` rewrite. PR-B (Task B1) is the `LifeStage` typed parent FK. The fan-out follows the existing cross-rank composition precedent (`InsectAggregateFactory` holds sibling *queries*, never another entity's repository): `SpeciesQueryImpl` gains a `GenusQuery`, `GenusQueryImpl` gains a `FamilyQuery`. Dependency order is Species→Genus→Family→∅ (no cycle).

**Tech Stack:** Java 21 records, Maven multi-module, Jackson 2.19 (record deserialization + `@JsonTypeInfo`/`@JsonSubTypes` external-property dispatch), JUnit 5, AssertJ, the kernel Observer/Constraints framework.

---

## Conventions for this plan (project-specific — read first)

- **The user runs Maven.** Do **not** invoke `mvn`. Where a step says "Verify build", surface the exact command for the user/controller to run and the expected result; wait for their confirmation before proceeding.
- **Subagents stage only — never commit.** Each task ends by staging (`git add`) the listed files and reporting. The controller stops, the user reviews the diff, and the user commits. The `git add` lists below are the staging set, not authorization to commit.
- **Trunk-based.** Work happens on `main`; the user pushes.
- Run targeted module builds during a task; the task's final "Verify build" is the authoritative gate. The fastest useful command per module is `mvn -q -pl domains/insects/<module> -am test` (from repo root). The final gate per PR is `mvn -q verify` from the repo root.

---

# PR-A — Rank-FK normalization + `Insect.invariants()` rewrite (R8 + R1)

PR-A is two tasks. **Task A1** installs the fan-out while the grandparent FKs still exist (everything stays green). **Task A2** removes the grandparent FKs and ripples the change through entities, JSON, repository methods, console, aggregates, `Insect.invariants()`, and tests — one atomic commit (a record-component removal cannot be split and stay compilable).

---

## Task A1: Reimplement the two skip-level queries as in-core fan-outs

The skip-level queries `species().forFamilyName(...)` and `genera().forOrderName(...)` today delegate to a repository index keyed on the *grandparent* FK. Reimplement them as fan-outs over the *parent*-level queries — which do **not** depend on the grandparent FK — so this task is behavior-preserving and leaves the build green. After this task, `SpeciesRepository.getByFamilyName` and `GenusRepository.getByOrderName` are dead code (removed in A2).

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/SpeciesQueryImpl.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/GenusQueryImpl.java`
- Modify: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/SpeciesQueryImplTest.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/GenusQueryImplTest.java`

- [ ] **Step 1: Add the `FamilyQuery` dependency to `GenusQueryImpl` and fan out `forOrderName`**

Replace the whole body of `GenusQueryImpl.java` with:

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@DomainService
class GenusQueryImpl
        extends AbstractEntityQuery<
        InsectGenusName,
        InsectGenus,
        GenusCollection,
        InsectRepository.GenusRepository>
        implements InsectQuery.GenusQuery {

    private final InsectQuery.FamilyQuery familyQuery;

    GenusQueryImpl(InsectRepository.GenusRepository repository,
                   InsectQuery.FamilyQuery familyQuery) {
        super(repository);
        this.familyQuery = Objects.requireNonNull(familyQuery, "familyQuery");
    }

    @Override
    public GenusCollection findByNameSet(Set<InsectGenusName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public GenusCollection forFamilyName(InsectFamilyName familyName) {
        observer().arguments("forFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByFamilyName(familyName));
    }

    @Override
    public GenusCollection forOrderName(InsectOrderName orderName) {
        observer().arguments("forOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        List<InsectGenus> genera = familyQuery.forOrderName(orderName).stream()
                .flatMap(family -> forFamilyName(family.name()).stream())
                .toList();
        return GenusCollection.of(genera);
    }
}
```

Note: `forOrderName` walks `families in order → genera in each family`, using only the parent-level `genus.familyName` / `family.orderName` FKs. The `observer()` accessor is inherited from `AbstractEntityQuery` (already used elsewhere in this class).

- [ ] **Step 2: Add the `GenusQuery` dependency to `SpeciesQueryImpl` and fan out `forFamilyName`**

Replace the whole body of `SpeciesQueryImpl.java` with:

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@DomainService
class SpeciesQueryImpl
        extends AbstractEntityQuery<
        InsectSpeciesName,
        InsectSpecies,
        InsectEntityCollections.SpeciesCollection,
        InsectRepository.SpeciesRepository>
        implements InsectQuery.SpeciesQuery {

    private final InsectQuery.GenusQuery genusQuery;

    SpeciesQueryImpl(InsectRepository.SpeciesRepository repository,
                     InsectQuery.GenusQuery genusQuery) {
        super(repository);
        this.genusQuery = Objects.requireNonNull(genusQuery, "genusQuery");
    }

    @Override
    public InsectEntityCollections.SpeciesCollection findByNameSet(Set<InsectSpeciesName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public InsectEntityCollections.SpeciesCollection forGenusName(InsectGenusName genusName) {
        observer().arguments("forGenusName",
                        i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(
                repository().getByGenusName(genusName));
    }

    @Override
    public InsectEntityCollections.SpeciesCollection forFamilyName(InsectFamilyName familyName) {
        observer().arguments("forFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        List<InsectSpecies> species = genusQuery.forFamilyName(familyName).stream()
                .flatMap(genus -> forGenusName(genus.name()).stream())
                .toList();
        return InsectEntityCollections.SpeciesCollection.of(species);
    }
}
```

- [ ] **Step 3: Rewire `InsectsTestContext` in dependency order**

In `InsectsTestContext.java`, replace the query-construction block (currently lines 44–50) with the following. The order changes: family first, then genus(family), then species(genus).

```java
        InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(repository.familyRepository);
        InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(repository.genusRepository, familyQuery);
        InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(repository.speciesRepository, genusQuery);
        InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(repository.imageRepository);
        InsectQuery.FunctionalRoleQuery functionalRoleQuery =
                new FunctionalRoleQueryImpl(repository.functionalRoleRepository);
        InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(repository.orderRepository);
```

Leave the subsequent `new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery, orderQuery)` line unchanged.

- [ ] **Step 4: Fix the two query-impl test wirings**

In `GenusQueryImplTest.java`, replace line 23:

```java
    InsectQuery.GenusQuery query = new GenusQueryImpl(repository);
```

with:

```java
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery query = new GenusQueryImpl(repository, familyQuery);
```

In `SpeciesQueryImplTest.java`, replace line 23:

```java
    InsectQuery.SpeciesQuery query = new SpeciesQueryImpl(repository);
```

with:

```java
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, new FamilyQueryImpl(familyRepository));
    InsectQuery.SpeciesQuery query = new SpeciesQueryImpl(repository, genusQuery);
```

(`FamilyRepositoryMock`, `GenusRepositoryMock`, `FamilyQueryImpl`, `GenusQueryImpl` are all in package `com.naturalist.insects`, same package as the test — no imports needed. The `db` field is the existing `NaturalistDatabaseExtension` in each test class, which seeds every mock from the shared JSON catalogs.)

- [ ] **Step 5: Verify build (user runs)**

Ask the user to run, from the repo root:

```
mvn -q -pl domains/insects/insects-core -am test
mvn -q -pl domains/insects/insects-test-context -am test
```

Expected: PASS. In particular `SpeciesQueryImplTest.forFamilyName_returnsSpeciesWithMatchingFamilyName` (asserts the fan-out finds `battus-philenor` under `papilionidae`) and `GenusQueryImplTest.forFamilyName_returnsGeneraInThatFamily` still pass, and no test references a removed constructor. Do not proceed until the user confirms PASS.

- [ ] **Step 6: Stage (do NOT commit — controller checkpoints for user review)**

```bash
git add \
  domains/insects/insects-core/src/main/java/com/naturalist/insects/SpeciesQueryImpl.java \
  domains/insects/insects-core/src/main/java/com/naturalist/insects/GenusQueryImpl.java \
  domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java \
  domains/insects/insects-core/src/test/java/com/naturalist/insects/SpeciesQueryImplTest.java \
  domains/insects/insects-core/src/test/java/com/naturalist/insects/GenusQueryImplTest.java
```

Report to the controller: "A1 staged — fan-out installed, build green. Awaiting review before commit." Suggested commit message (the user commits): `Fan out species-by-family and genera-by-order queries in core`.

---

## Task A2: Remove the grandparent FKs and ripple the change

Remove `InsectGenus.orderName` and `InsectSpecies.familyName` and everything that depends on them. This is one atomic commit (the record-component removal breaks every constructor call at once). Work through the steps; the build is red until the last code step.

**Files:**
- Modify (entities): `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java`, `.../InsectSpecies.java`
- Modify (aggregates): `.../InsectGenusAggregate.java`, `.../InsectSpeciesAggregate.java`, `.../Insect.java`
- Modify (repository contract surface): `.../InsectRepository.java`
- Modify (mocks): `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryMock.java`, `.../GenusRepositoryMock.java`
- Modify (repository contract tests): `.../SpeciesRepositoryTest.java`, `.../GenusRepositoryTest.java`
- Modify (JSON catalogs): `domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`, `.../insect-genera.json`
- Modify (console): `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify (aggregate test): `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectTest.java`

- [ ] **Step 1: Remove `orderName` from `InsectGenus`**

In `InsectGenus.java`:
1. Delete the `InsectOrderName orderName,` component (record header line 39).
2. Delete the `import com.naturalist.taxonomy.LinnaeanGenus;`? **No** — keep it; `InsectGenus` still `implements ... LinnaeanGenus<InsectFamilyName>`. Keep the `InsectOrderName` import **only if** still referenced; after removal it is unused → delete `import` of `InsectOrderName` if present (there is no explicit import; `InsectOrderName` is same-package — nothing to delete).
3. Update `withPlacedIn` to drop `orderName` from the constructor call:
   ```java
   public InsectGenus withPlacedIn(@Nullable Clade value) {
       return new InsectGenus(name, familyName, genus, description, commonNames, value);
   }
   ```
4. Delete the `belongsToOrder` method (lines 56–59) entirely.
5. In `invariants()`, delete the line `.entityName(orderName, "orderName")`.
6. Fix the class javadoc: delete the sentence "The {@link #orderName} component is the grandparent typed reference to the parent {@link InsectOrder}." and adjust the following sentence so it reads naturally (the genus carries only the direct family FK).

The resulting record header is:

```java
public record InsectGenus(
        InsectGenusName name,
        InsectFamilyName familyName,
        TaxonomicGenus genus,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn
) implements NamedEntity<InsectGenusName>, LinnaeanGenus<InsectFamilyName> {
```

- [ ] **Step 2: Remove `familyName` from `InsectSpecies`**

In `InsectSpecies.java`:
1. Delete the `InsectFamilyName familyName,` component (record header line 86).
2. Update `withPlacedIn` to drop `familyName`:
   ```java
   public InsectSpecies withPlacedIn(@Nullable Clade value) {
       return new InsectSpecies(
               name, genusName, epithet, description, commonNames,
               sightingNotes, identificationFeatures,
               value,
               chemicalDefense, voltinism, habitatProfile, habitatRequirements,
               gardenConnections, beneficialProfile, ecologicalSignificance);
   }
   ```
3. Delete the `belongsToFamily` method (lines 116–119) entirely. **Keep** `belongsToGenus`.
4. In `invariants()`, delete the line `.entityName(familyName, "familyName")`.
5. Fix the class javadoc: in the first paragraph, change "typed upward FKs to its parent {@link InsectGenus} and {@link InsectFamily} (both required ...)" to describe only the genus FK; delete "Order and family epithets are derivable from the parent family record" → replace with "Family and order are reached by resolving the parent genus's family FK." (Keep it brief.)

The resulting record header is:

```java
public record InsectSpecies(
        InsectSpeciesName name,
        InsectGenusName genusName,
        TaxonomicSpecies epithet,
        Description description,
        Set<CommonName> commonNames,
        @Nullable String sightingNotes,
        @Nullable IdentificationFeatures identificationFeatures,
        @Nullable Clade placedIn,
        @Nullable ChemicalDefense chemicalDefense,
        @Nullable Voltinism voltinism,
        @Nullable HabitatProfile habitatProfile,
        @Nullable HabitatRequirements habitatRequirements,
        @Nullable GardenConnections gardenConnections,
        @Nullable BeneficialProfile beneficialProfile,
        @Nullable EcologicalSignificance ecologicalSignificance
) implements NamedEntity<InsectSpeciesName> {
```

- [ ] **Step 3: Remove the dead aggregate delegates**

In `InsectGenusAggregate.java`: delete `orderName()` (lines 38–41) and `belongsToOrder(@Nullable InsectOrderAggregate order)` (lines 55–66, including its javadoc). **Keep** `familyName()` and `belongsToFamily(...)`. Delete the now-unused `import org.jspecify.annotations.Nullable;` only if no `@Nullable` remains — `belongsToFamily` still uses `@Nullable`, so keep the import.

In `InsectSpeciesAggregate.java`: delete `familyName()` (lines 37–40) and `belongsToFamily(@Nullable InsectFamilyAggregate family)` (lines 52–59, including its javadoc). **Keep** `genusName()` and `belongsToGenus(...)`. Keep the `@Nullable` import (still used by `belongsToGenus`).

- [ ] **Step 4: Rewrite `Insect.invariants()` (R8 drop skip-level FK checks + R1 collapse re-descents)**

In `Insect.java`, replace the entire `invariants()` method (lines 161–198) with:

```java
    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
            .behavioralCollection(observations, "observations")
            .behavioralCollection(lifeStages, "lifeStages")
            // One descent per present rank (R1): a rank's own invariants are
            // walked exactly once, in its own block. Ancestor *presence* is a
            // notNull check (not a re-descent), and the single remaining FK
            // check per rank validates the immediate-parent typed FK. With the
            // grandparent FKs removed there is exactly one path up the tree, so
            // the former skip-level checks (speciesBelongsToFamily,
            // genusBelongsToOrder) are structurally impossible and gone.
            .whenNotNull(order, o -> o
                .aggregate(order, "order")
            )
            .whenNotNull(family, f -> f
                .aggregate(family, "family")
                .notNull(order, "family:order")
                .isTrue(family.belongsToOrder(order), "familyBelongsToOrder")
            )
            .whenNotNull(genus, g -> g
                .aggregate(genus, "genus")
                .notNull(family, "genus:family")
                .isTrue(genus.belongsToFamily(family), "genusBelongsToFamily")
            )
            .whenNotNull(species, s -> s
                .aggregate(species, "species")
                .notNull(genus, "species:genus")
                .isTrue(species.belongsToGenus(genus), "speciesBelongsToGenus")
            )
        ;
    }
```

Notes for the implementer: `family.belongsToOrder(order)`, `genus.belongsToFamily(family)`, `species.belongsToGenus(genus)` are the **kept** aggregate-level null-tolerant predicates (each returns `true` when its argument is null, so the FK check never double-fires the missing-ancestor case — that is what the adjacent `notNull(...)` reports). The `notNull(ancestor, "child:ancestor")` presence checks preserve the violation names the tests assert (`family:order`, `genus:family`, `species:genus`). The class javadoc's "Cross-rank FK consistency" bullet (lines 61–70) lists five `isTrue` checks — update it to list the three that remain (`familyBelongsToOrder`, `genusBelongsToFamily`, `speciesBelongsToGenus`) and delete the sentence about denormalized-FK drift.

- [ ] **Step 5: Remove the dead repository methods + mocks**

In `InsectRepository.java`:
- In `SpeciesRepository` (lines 86–92), delete `List<InsectSpecies> getByFamilyName(InsectFamilyName familyName);`. **Keep** `getByGenusName`.
- In `GenusRepository` (lines 106–112), delete `List<InsectGenus> getByOrderName(InsectOrderName orderName);`. **Keep** `getByFamilyName`.
- Leave `FamilyRepository.getByOrderName` untouched (that uses `family.orderName`, the parent FK).

In `SpeciesRepositoryMock.java`: delete the `getByFamilyName` override (lines 28–36). **Keep** `getByGenusName`.

In `GenusRepositoryMock.java`: delete the `getByOrderName` override (lines 28–36). **Keep** `getByFamilyName`.

- [ ] **Step 6: Remove the moved contract-test cases from the repository tests**

The skip-level read behavior is now covered by the query-impl tests (`SpeciesQueryImplTest.forFamilyName_*`, `GenusQueryImplTest.forOrderName_*` — note `forOrderName` query tests do not yet exist; add them in Step 7). Remove the repository-level cases for the deleted methods:

In `SpeciesRepositoryTest.java`: delete the four `getByFamilyName_*` test methods (lines 151–177): `getByFamilyName_rejectsNull` and `getByFamilyName_returnsSpeciesWithMatchingFamilyName` and `getByFamilyName_returnsEmptyForUnknownFamily`. **Keep** all `getByGenusName_*` cases.

In `GenusRepositoryTest.java`: delete the four `getByOrderName_*` test methods (lines 120–142): `getByOrderName_rejectsNull`, `getByOrderName_returnsGeneraWithMatchingOrderName`, `getByOrderName_returnsEmptyForUnknownOrder`. **Keep** all `getByFamilyName_*` cases.

- [ ] **Step 7: Add `forOrderName` fan-out coverage to `GenusQueryImplTest`**

`genera().forOrderName(...)` lost its repository-level test in Step 6; cover the fan-out at the query layer. Append these two methods to `GenusQueryImplTest.java` (before the closing brace). They rely on the `db`-seeded catalogs (`halictus`→`halictidae`→`hymenoptera`):

```java
    @Test
    void forOrderName_rejectsNull() {
        assertThatThrownBy(() -> query.forOrderName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("orderName");
    }

    @Test
    void forOrderName_returnsGeneraInThatOrderViaFanOut() {
        GenusCollection collection = query.forOrderName(InsectOrderName.of("hymenoptera"));

        assertThat(collection.stream())
                .extracting(g -> g.name().value())
                .contains("halictus");
    }
```

`SpeciesQueryImplTest` already has `forFamilyName_rejectsNull` and `forFamilyName_returnsSpeciesWithMatchingFamilyName` (now exercising the fan-out) — leave them.

- [ ] **Step 8: Update the repository-test entity builders to the new arity**

In `SpeciesRepositoryTest.java`, three `new InsectSpecies(...)` sites drop the `familyName` argument (the 3rd positional, after `genusName`):

`newEntity()` (lines 48–60) becomes:
```java
    @Override
    default InsectSpecies newEntity() {
        return new InsectSpecies(
                InsectSpeciesName.of("test-species-xx"),
                InsectGenusName.of("carabus"),
                TaxonomicSpecies.of("nemoralis"),
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null,
                null, null, null);
    }
```

`ghostEntity()` (lines 63–75) — drop the `InsectFamilyName.of("carabidae"),` line; same shape as above but with `TaxonomicSpecies.of("ghost")`.

`modifiedEntity(...)` (lines 79–103) — drop the `InsectFamilyName.of("carabidae"),` line (3rd arg). All other args unchanged.

`getByGenusName_returnsSpeciesWithMatchingGenusName` (lines 122–131) — the inline `new InsectSpecies(...)` drops the `TestInsectsIdentifiers.InsectFamily.Halictidae.name,` line (3rd arg):
```java
        InsectSpecies underHalictus = new InsectSpecies(
                seeded.name(),
                halictus,
                seeded.epithet(),
                seeded.description(),
                seeded.commonNames(),
                null, null, null,
                null, null, null, null, null, null, null);
```
Remove the now-unused `import com.naturalist.taxonomy.*;`? It is still used (`TaxonomicSpecies`); keep it.

In `GenusRepositoryTest.java`, three `new InsectGenus(...)` sites drop the `orderName` argument (the 3rd positional, after `familyName`):

`newEntity()` (lines 49–57):
```java
    @Override
    default InsectGenus newEntity() {
        return new InsectGenus(
                InsectGenusName.of("test-genus-xx"),
                InsectFamilyName.of("tachinidae"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }
```

`ghostEntity()` (lines 61–72) — drop the `InsectOrderName.of("test-ghost-order-xx"),` line; keep the comment.

`modifiedEntity(...)` (lines 75–84) — drop the `InsectOrderName.of("hymenoptera"),` line (3rd arg).

- [ ] **Step 9: Migrate the JSON catalogs**

In `insect-species.json`: remove the `"familyName": "...",` line from **every** entry (it sits between `"genusName"` and `"epithet"`).

In `insect-genera.json`: remove the `"orderName": "...",` line from **every** entry (it sits between `"familyName"` and `"genus"`).

Do this with an edit that preserves all other formatting and UTF-8 content (descriptions contain em-dashes — if scripting the edit in Python, pass `ensure_ascii=False` and read/write UTF-8). A line-oriented delete (drop lines matching `^\s*"familyName":` in the species file and `^\s*"orderName":` in the genera file) is safest. After editing, confirm both files are still valid JSON (e.g. `python3 -m json.tool <file> >/dev/null`).

- [ ] **Step 10: Fix the three console species→family hops**

In `InsectsController.java`:

Species list (around lines 220–223) — resolve genus first, then family from the genus. Replace:
```java
            familyByName.computeIfAbsent(species.familyName(),
                    n -> insectQuery.families().getByName(n).orElseThrow());
            genusByName.computeIfAbsent(species.genusName(),
                    n -> insectQuery.genera().getByName(n).orElseThrow());
```
with:
```java
            InsectGenus genus = genusByName.computeIfAbsent(species.genusName(),
                    n -> insectQuery.genera().getByName(n).orElseThrow());
            familyByName.computeIfAbsent(genus.familyName(),
                    n -> insectQuery.families().getByName(n).orElseThrow());
```

`detail` (line 417) — the genus is already resolved on line 416. Replace:
```java
        InsectFamily family = insectQuery.families().getByName(s.familyName()).orElseThrow();
```
with:
```java
        InsectFamily family = insectQuery.families().getByName(genus.familyName()).orElseThrow();
```

`lifeStages` (line 466) — the genus is already resolved on line 465. Make the identical replacement as in `detail`:
```java
        InsectFamily family = insectQuery.families().getByName(genus.familyName()).orElseThrow();
```

(No `genus.orderName()` sites exist — order is reached via `family.orderName()` throughout, which is unchanged.)

- [ ] **Step 11: Update `InsectTest` — entity builders, dropped FK tests, ancestor assertion**

In `InsectTest.java`:

(a) **Helper builders** — drop the removed arg:
- `genusAggregate()` (lines 447–456): the `new InsectGenus(...)` drops `orderName(),` (3rd arg) →
  ```java
      private static InsectGenusAggregate genusAggregate() {
          return InsectGenusAggregate.of(new InsectGenus(
                  genusName(),
                  familyName(),
                  TaxonomicGenus.of("Battus"),
                  description(),
                  Set.of(),
                  null));
      }
  ```
- `speciesAggregate()` (lines 458–468): the `new InsectSpecies(...)` drops `familyName(),` (3rd arg) →
  ```java
      private static InsectSpeciesAggregate speciesAggregate() {
          return InsectSpeciesAggregate.of(new InsectSpecies(
                  speciesName(),
                  genusName(),
                  TaxonomicSpecies.of("philenor"),
                  description(),
                  Set.of(),
                  null, null,
                  null,
                  null, null, null, null, null, null, null));
      }
  ```

(b) **`genusWithMismatchedFamilyFkReportsGenusBelongsToFamilyViolation`** (lines 316–336): the inline `new InsectGenus(...)` drops the `orderName(),` argument (3rd). The mismatched family stays `InsectFamilyName.of("syrphidae")`:
```java
        InsectGenusAggregate mismatched = InsectGenusAggregate.of(new InsectGenus(
                genusName(),
                InsectFamilyName.of("syrphidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null));
```

(c) **`speciesWithMismatchedGenusFkReportsSpeciesBelongsToGenusViolation`** (lines 385–408): the inline `new InsectSpecies(...)` drops the `familyName(),` argument (3rd). The mismatched genus stays `InsectGenusName.of("empoasca")`:
```java
        InsectSpeciesAggregate mismatched = InsectSpeciesAggregate.of(new InsectSpecies(
                speciesName(),
                InsectGenusName.of("empoasca"),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null, null, null, null));
```

(d) **Delete two now-impossible tests entirely:**
- `genusWithMismatchedOrderFkReportsGenusBelongsToOrderViolation` (lines 338–358).
- `speciesWithMismatchedFamilyFkReportsSpeciesBelongsToFamilyViolation` (lines 360–383).

(e) **Update one ancestor-presence assertion.** `withSpeciesOnOrderOnlyAggregateConstructsButReportsAncestorViolations` (lines 282–292) today asserts both `.attempted.species:genus` and `.attempted.species:family`. Under the R1 rewrite, the species block reports only its immediate-parent presence (`species:genus`); the family-presence gap would surface as `genus:family` only when a genus is present (here it is not). Change the assertion to:
```java
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".attempted.species:genus");
```

(f) Leave untouched: `familyWithMismatchedOrderFkReportsFamilyBelongsToOrderViolation` (parent FK, still valid — `familyAggregate()`/`orderAggregate()` builders are unchanged), all `identifiedTo*`, `with*`, `*IsValid`, and the three presence-violation tests (`speciesPresentWithoutGenus...` → `.species:genus`, `genusPresentWithoutFamily...` → `.genus:family`, `familyPresentWithoutOrder...` → `.family:order`).

- [ ] **Step 12: Verify build (user runs)**

Ask the user to run, from the repo root:

```
mvn -q verify
```

Expected: PASS across all modules. Spot-checks to call out to the user:
- `InsectTest` compiles and passes with two fewer tests and the updated ancestor assertion.
- `SpeciesQueryImplTest` / `GenusQueryImplTest` fan-out tests pass against the migrated JSON.
- The console module compiles (the three `genus.familyName()` hops).
- No file references `InsectSpecies::familyName`, `InsectGenus::orderName`, `SpeciesRepository.getByFamilyName`, or `GenusRepository.getByOrderName`. (A quick `grep -rn` the implementer can run: `grep -rn "\.familyName()" domains/insects | grep -i species` should return nothing, and `grep -rn "getByOrderName" domains/insects/insects-*/src/main` should show only `FamilyRepository`.)

Do not proceed until the user confirms PASS.

- [ ] **Step 13: Stage (do NOT commit — controller checkpoints for user review)**

```bash
git add \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpecies.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenusAggregate.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpeciesAggregate.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java \
  domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryMock.java \
  domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryMock.java \
  domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryTest.java \
  domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java \
  domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json \
  domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json \
  domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
  domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectTest.java \
  domains/insects/insects-core/src/test/java/com/naturalist/insects/GenusQueryImplTest.java
```

Report to the controller: "A2 staged — grandparent FKs removed, invariants rewritten, build green. Awaiting review before commit." Suggested commit message: `Normalize insect ranks to parent-only FKs; trim Insect invariants (R8+R1)`.

---

# PR-B — Typed parent FK on `LifeStage` (R6)

Independent of PR-A; may land before or after. Adds `InsectRankName parentName` to `LifeStage` and its four permits, mirroring the proven `InsectImage.parentName` Jackson dispatch, and migrates the 60-entry catalog.

---

## Task B1: Add `parentName : InsectRankName` to `LifeStage`

**Files:**
- Modify (interface): `domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/LifeStage.java`
- Modify (permits): `.../lifestage/EggStage.java`, `.../LarvaStage.java`, `.../PupaStage.java`, `.../AdultStage.java`
- Modify (mock): `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/lifestage/LifeStageEntityRepositoryMock.java`
- Modify (contract test): `.../lifestage/LifeStageEntityRepositoryTest.java`
- Modify (catalog): `domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json`

- [ ] **Step 1: Add the accessor to the `LifeStage` interface**

In `LifeStage.java`, add an `InsectRankName parentName();` accessor and its import. After the change the interface body is:

```java
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.LifeStageKind;
import com.naturalist.insects.LifeStageName;
import org.jspecify.annotations.Nullable;

// ... existing class javadoc ...
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EggStage.class, name = "EGG"),
        @JsonSubTypes.Type(value = LarvaStage.class, name = "LARVA"),
        @JsonSubTypes.Type(value = PupaStage.class, name = "PUPA"),
        @JsonSubTypes.Type(value = AdultStage.class, name = "ADULT")
})
public sealed interface LifeStage extends NamedEntity<LifeStageName>
        permits EggStage, LarvaStage, PupaStage, AdultStage {

    LifeStageKind kind();

    /**
     * The typed name of the catalogued rank (family, genus, or species) this
     * stage is attached to. Mirrors {@code InsectImage.parentName}: the sealed
     * {@link InsectRankName} marker statically constrains the slot to insect-side
     * rank names. Jackson dispatch is declared on each permit's component, not
     * here, to keep the discriminator off direct leaf-class serialization.
     */
    InsectRankName parentName();

    StagePhenology phenology();

    StageHabitat habitat();

    @Nullable StageChemistryRole chemistryRole();

    Description description();
}
```

- [ ] **Step 2: Add the `parentName` component to each of the four permits**

For **each** of `EggStage`, `LarvaStage`, `PupaStage`, `AdultStage`: insert `parentName` as the **second** record component (right after `LifeStageName name`), carrying the same Jackson annotations `InsectImage` uses, and add the matching imports + an `.identifier(parentName, "parentName")` invariant line.

Add these imports to each permit (if not already present):
```java
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.insects.InsectRankName;
```

The component to insert (identical in all four permits):
```java
        @JsonTypeInfo(use = Id.NAME, property = "parentRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectOrderName.class, name = "ORDER"),
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName parentName,
```
This also needs imports for the four rank-name types used in the `@Type` list. They live in package `com.naturalist.insects` (e.g. `InsectFamilyName`); add explicit imports since the permits are in `com.naturalist.insects.lifestage`:
```java
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.InsectSubspeciesName;
```

In each permit's `invariants()`, add `.identifier(parentName, "parentName")` immediately after the `.entityName(name, "name")` line.

Worked example — `EggStage.java` header + invariants after the change:
```java
public record EggStage(
        LifeStageName name,
        @JsonTypeInfo(use = Id.NAME, property = "parentRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectOrderName.class, name = "ORDER"),
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName parentName,
        StagePhenology phenology,
        StageHabitat habitat,
        @Nullable StageChemistryRole chemistryRole,
        Description description,
        @Nullable String colorProgression,
        @Nullable String layingPattern,
        @Nullable String adaptiveSignificance
) implements LifeStage {

    @Override
    public LifeStageKind kind() {
        return LifeStageKind.EGG;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .identifier(parentName, "parentName")
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(chemistryRole, "chemistryRole")
                .valueObject(description, "description");
    }
}
```
Apply the same insertion (component position 2 + import set + invariant line) to `LarvaStage`, `PupaStage`, `AdultStage`, keeping each permit's other components in their existing order.

- [ ] **Step 3: Switch the mock to typed parent matching + add argument validation**

In `LifeStageEntityRepositoryMock.java`, replace `getByParentName` so it (a) validates its argument (per the repository-mock-validation convention) and (b) matches on the typed `parentName` field (class-qualified equality), not the rank-ambiguous slug substring:

```java
    @Override
    public List<LifeStage> getByParentName(InsectRankName parentName) {
        observer().arguments("getByParentName",
                        i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(stage -> parentName.equals(stage.parentName()))
                .toList();
    }
```

(`observer()` is inherited from `AbstractTestEntityRepository`. `identifier(...)` is the same primitive `InsectImage`'s invariants use for an `InsectRankName`.)

- [ ] **Step 4: Update the contract-test entity builders + add a null-rejection case**

In `LifeStageEntityRepositoryTest.java`, the three `new EggStage(...)` builders gain a `parentName` as the second argument. Use a species rank name for the synthetic ones and the real parent for `modifiedEntity`:

`newEntity()`:
```java
    @Override
    default LifeStage newEntity() {
        return new EggStage(
                LifeStageName.of(InsectSpeciesName.of("test-species-xx"), LifeStageKind.EGG),
                InsectSpeciesName.of("test-species-xx"),
                phenology(),
                habitat(),
                null,
                description(),
                null, null, null);
    }
```

`ghostEntity()` — same shape with `test-ghost-xx`:
```java
    @Override
    default LifeStage ghostEntity() {
        return new EggStage(
                LifeStageName.of(InsectSpeciesName.of("test-ghost-xx"), LifeStageKind.EGG),
                InsectSpeciesName.of("test-ghost-xx"),
                phenology(),
                habitat(),
                null,
                description(),
                null, null, null);
    }
```

`modifiedEntity(original)` — `original.name()` is a `LifeStageName`; carry the original's typed parent forward (every mutable field changes, but `parentName` is part of identity-adjacent structure and the `name` is immutable, so keep `original.parentName()`):
```java
    @Override
    default LifeStage modifiedEntity(LifeStage original) {
        return new EggStage(
                original.name(),
                original.parentName(),
                phenology(),
                habitat(),
                new StageChemistryRole(StageChemistryRole.Role.ACQUISITION, RandomValue.string()),
                description(),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string());
    }
```

Add the null-rejection contract case (domain-specific method now validates its argument). Append to the interface body:
```java
    @org.junit.jupiter.api.Test
    default void getByParentName_rejectsNull() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository().getByParentName(null))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class)
                .hasMessageContaining("parentName");
    }
```
(Fully-qualified names avoid adding imports to a file that may not currently import JUnit `Test`/AssertJ. If the file already imports them, use the short names.)

- [ ] **Step 5: Migrate `life-stages.json` (60 entries)**

Every entry gains two keys mirroring the `InsectImage` flat shape: `"parentRank"` (the discriminator) and `"parentName"` (the parent slug). Derive both from the entry's existing composite `name` (`{parent-slug}-{kind}`): `parentName` is the parent slug, `parentRank` is the parent's rank. Insert them immediately **after** the `"name"` line of each entry.

Rank classification of the 16 distinct parent slugs (apply to all kinds of each):

| parent slug | parentRank |
|---|---|
| tachinidae | FAMILY |
| braconidae | FAMILY |
| syrphidae | FAMILY |
| carabidae | FAMILY |
| tipulidae | FAMILY |
| hesperiidae | FAMILY |
| halictus | GENUS |
| andrena | GENUS |
| chrysoperla | GENUS |
| empoasca | GENUS |
| hippodamia-convergens | SPECIES |
| blattella-vaga | SPECIES |
| xylocopa-varipuncta | SPECIES |
| vanessa-cardui | SPECIES |
| battus-philenor | SPECIES |
| colias-eurytheme | SPECIES |

Example — the first entry becomes:
```json
{
  "name": "tachinidae-egg",
  "parentRank": "FAMILY",
  "parentName": "tachinidae",
  "phenology": {
  ...
```

Implementation guidance: script this in Python for reliability — load with `json.load`, for each entry split `name` on the last hyphen to get the parent slug, look the slug up in the table above to get the rank, then rebuild each object as an insertion-ordered dict with `parentRank`/`parentName` placed right after `name`, and `json.dump(..., indent=2, ensure_ascii=False)` (the `ensure_ascii=False` is required — descriptions contain em-dashes). Verify the result is valid JSON and still has 60 entries, and that every entry has both new keys. Cross-check a few: `halictus-egg`→GENUS/halictus, `battus-philenor-larva`→SPECIES/battus-philenor, `carabidae-pupa`→FAMILY/carabidae.

- [ ] **Step 6: Find and fix any other `LifeStage` construction sites**

Run `grep -rn "new EggStage(\|new LarvaStage(\|new PupaStage(\|new AdultStage(" domains apps --include=*.java`. For each hit outside the files already edited in this task, insert the `parentName` argument in position 2 (a sensible rank name for the fixture). Expected: only the contract test (already handled). If the `identify-insect` skill or any console code constructs stages, update those too (none known at plan time — verify).

- [ ] **Step 7: Verify build (user runs)**

Ask the user to run, from the repo root:

```
mvn -q verify
```

Expected: PASS. Call out to the user:
- The 60-entry catalog deserializes (Jackson reads `parentRank`/`parentName` into the typed `InsectRankName` slot) — any mismatch surfaces as a `LifeStageTestEntitySource` load failure.
- `LifeStageEntityRepositoryTest` passes, including the new `getByParentName_rejectsNull`.
- The console `detail`/`lifeStages`/`genera`/`families` pages still resolve life-stage galleries (the mock now matches on typed `parentName`; behavior is equivalent for the seeded data).

Do not proceed until the user confirms PASS.

- [ ] **Step 8: Stage (do NOT commit — controller checkpoints for user review)**

```bash
git add \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/LifeStage.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/EggStage.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/LarvaStage.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/PupaStage.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/AdultStage.java \
  domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/lifestage/LifeStageEntityRepositoryMock.java \
  domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/lifestage/LifeStageEntityRepositoryTest.java \
  domains/insects/insects-repository-test/src/main/resources/insects/life-stages.json
```

Report to the controller: "B1 staged — LifeStage carries a typed parent FK, catalog migrated, build green. Awaiting review before commit." Suggested commit message: `Add typed parentName:InsectRankName to LifeStage (R6)`.

---

## Out of scope (parked — do not implement here)

R2 (drop the null-tolerant aggregate `belongsTo*` — the kept ones stay), R3 (`aggregateOrNull` deletion), R4 (`Insect` as a read model / remove `with*` mutators), R5 (split observations out of the taxon view), R7 (rank-aggregate fate). These remain open in `docs/notes/2026-06-03-insect-aggregate-bounded-context-review.md`.

## Cleanup

Delete the scratch bundle used while authoring this plan: `temp/plan-source-bundle.md` (not part of any commit).
