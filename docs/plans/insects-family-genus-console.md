# Insects Family/Genus Console Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/insects/families` and `/insects/genera` list-and-detail pages to the management console so the catalog surfaces every rank that exists in `InsectQuery`, not just species.

**Architecture:** Mirror the species shape inside `insects-console`: a paged list per rank, a per-name detail page, and member-rank cross-links. Fill one real api gap — `GenusQuery.forFamilyName(InsectFamilyName)` — using the typed `InsectGenus.familyName()` upward FK that already exists. Member-species listings on the genus detail page use a text-based stopgap (`SpeciesQuery.forGenusEpithet(TaxonomicGenus)`) because `InsectSpecies` does not yet carry a typed `InsectGenusName` component; the typed promotion is parked as a separate slice. Update `InsectsLinker` so existing search hits for families and genera (already emitted by `InsectsCatalogContribution`) resolve to URLs instead of being dropped.

**Tech Stack:** Java records, JTE templates, Spring MVC, JUnit Jupiter, Jackson 2.19, Maven multi-module. Conventions per `domains/CLAUDE.md`, `domains/insects/CLAUDE.md`, and `apps/CLAUDE.md`.

**Scope boundary.** This slice is the UI half of Phase 0 of the identification roadmap (`docs/plans/identification.md` lines 209–220). It does NOT:

- Add a typed `InsectGenusName` / `InsectFamilyName` foreign key to `InsectSpecies`. The convenience accessors `species.taxonomy().genus()` / `.family()` remain the only join key. A targeted parking-lot follow-up (PL-13, raised in Task 1) captures the typed-FK retypeover.
- Promote `InsectAggregate` to rank-polymorphic (family / genus / species aggregates). The console pages compose their view models from entity queries directly; no aggregate is exercised. PL-14 (raised in Task 1) captures the shape question for Phase 2 to decide.
- Ship the formal taxonomic-scope breadcrumb primitive (Phase 0 also mentions it; Phase 2 reuses it). This slice uses simple upward anchors. The primitive is its own follow-up slice.
- Promote `placedIn` (`Clade`) into the family/genus detail UI beyond the existing pattern on species. Phase 5 of `clades-kernel.md` already covers stage resolution; that UI fold-in is separate work.
- Add console pages for plants. Phase 0 explicitly notes plants get the same treatment when plant identification activates — out of scope here.
- Add `LifeStage` listings on family/genus detail pages. The records carry `egg`/`larva`/`pupa`/`adult` already, but the rendering of those stages is its own follow-up.
- Modify any fixture JSON. The four `insect-genera.json` records and eleven `insect-families.json` records render unchanged.

**Size note.** This slice is larger than `genus-family-life-stages.md` because it touches the api, the in-memory adapter, the controller, the linker, and four new templates. If the diff approaches the 400-line PR ceiling (per `domains/CLAUDE.md`'s size discipline), split at Task 6 — Tasks 1–6 ship the api + adapter changes; Tasks 7–11 ship the console.

---

## File Structure

**Modify:**

- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java` — add `GenusQuery.forFamilyName(InsectFamilyName)` and `SpeciesQuery.forGenusEpithet(TaxonomicGenus)`.
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java` — add matching repository methods on `GenusRepository` and `SpeciesRepository`.
- `domains/insects/insects-core/src/main/java/com/naturalist/insects/GenusQueryImpl.java` — implement `forFamilyName`.
- `domains/insects/insects-core/src/main/java/com/naturalist/insects/SpeciesQueryImpl.java` — implement `forGenusEpithet`.
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryMock.java` — implement `getByFamilyName`.
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryMock.java` — implement `getByGenusEpithet`.
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java` — add happy-path contract tests for the new method.
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryTest.java` — same.
- `domains/insects/insects-core/src/test/java/com/naturalist/insects/SpeciesQueryImplTest.java` — append null-rejection + happy-path test for `forGenusEpithet`.
- `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — add four routes.
- `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/catalog/InsectsLinker.java` — route `InsectFamilyName` and `InsectGenusName` to detail URLs.
- `domains/insects/insects-console/src/main/jte/insects/list.jte` — add header links to `/insects/families` and `/insects/genera`.
- `docs/notes/parking-lot.md` — add two audit findings: **PL-13** (typed `InsectGenusName` / `InsectFamilyName` FK on `InsectSpecies` — the gap this slice papers over with text matching) and **PL-14** (rank-polymorphic `InsectAggregate` — the gap this slice leaves untouched, awaiting Phase 2's session-scope decision).
- `docs/work-tracker.md` — set this plan as the current slice; on close, move it to "Recently completed" and clear the current-slice row.

**Create:**

- `domains/insects/insects-console/src/main/jte/insects/families.jte` — paged grid of `InsectFamily`.
- `domains/insects/insects-console/src/main/jte/insects/family.jte` — single-family detail with member-genera grid.
- `domains/insects/insects-console/src/main/jte/insects/genera.jte` — paged grid of `InsectGenus`.
- `domains/insects/insects-console/src/main/jte/insects/genus.jte` — single-genus detail with member-species grid.
- `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java` — smoke test for `families.jte` and `family.jte`.
- `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java` — smoke test for `genera.jte` and `genus.jte`.
- `domains/insects/insects-core/src/test/java/com/naturalist/insects/GenusQueryImplTest.java` — new query-impl test (no precedent file exists; mirror `FunctionalRoleQueryImplTest` shape).

**Verify-only:**

- `domains/insects/insects-repository-test/src/main/resources/insects/insect-families.json` (eleven records) and `insect-genera.json` (four records) — confirm unchanged loads through the existing test sources.

---

## Task 1: Raise PL-13 + PL-14 parking-lot entries from the audit pass

**Why first.** The audit Phase 0 calls for happens as part of this slice. Both gaps surface immediately: the species-side typed-FK gap that the slice papers over with a text stopgap (PL-13), and the rank-polymorphism gap in `InsectAggregate` that the slice doesn't touch but the audit exposes (PL-14). Recording both before any code lands keeps the work visible.

**Files:**

- Modify: `docs/notes/parking-lot.md`

- [ ] **Step 1: Append PL-13 entry**

Add a new entry at the end of `docs/notes/parking-lot.md`:

```markdown
## PL-13 — typed `InsectGenusName` / `InsectFamilyName` FK on `InsectSpecies`

**Raised:** 2026-05-22 (during Phase 0 console-pages slice api audit).

**Where:** `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpecies.java` — `taxonomy` (TaxonomicClassification) carries `family()` and `genus()` as text `TaxonomicFamily` / `TaxonomicGenus` values, but no typed `InsectFamilyName` / `InsectGenusName` upward FK.

**The smell.** `GenusQuery.forFamilyName(InsectFamilyName)` is typed (uses `genus.familyName()`). The matching species lookups — "members of this genus", "members of this family" — fall back to text-equality on `taxonomy().genus().value()` / `taxonomy().family().value()`. This works today (one slug per taxonomic name in the current data) but quietly couples the species record's display string to the genus record's slug. A rename of either drops the link silently.

**Why deferred.** Promoting `genusName` / `familyName` to typed components on `InsectSpecies` requires (a) the record retype, (b) JSON migration on 38 species records, (c) repository unique-constraint review, (d) updates to `SpeciesRepositoryTest` / `SpeciesCommandImplTest` / `SpeciesQueryImplTest`. That is its own slice. Doing it together with the console pages over-scopes this PR.

**Resolution path.** Add `InsectGenusName genusName` (and `InsectFamilyName familyName` — for under-identified-species cases where genus is unknown but family is) as nullable components on `InsectSpecies`. Promote the existing text accessors to convenience methods. Migrate JSON. Retype `SpeciesQuery.forGenusEpithet(TaxonomicGenus)` introduced in this slice to `forGenusName(InsectGenusName)` and remove the text stopgap. Same for any species-by-family follow-up.

**Blocking:** Not currently — text-based join works on the present dataset.
```

- [ ] **Step 2: Append PL-14 entry**

Add a second entry below PL-13 in `docs/notes/parking-lot.md`:

```markdown
## PL-14 — Rank-polymorphic `InsectAggregate`

**Raised:** 2026-05-22 (during Phase 0 console-pages slice; user noted `InsectAggregate` only references `InsectSpecies`).

**Where:** `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectAggregate.java` — record carries `(InsectSpecies species, ImageCollection images)`; rooted at `InsectSpeciesName`. `InsectAggregateFactory` assembles species + images by name.

**The smell.** `InsectImage.parentName()` is already `InsectRankName` (Path A landed — images can attach to family, genus, or species). But the aggregate that exists to present "an insect at Oak Vista" hardcodes species rank. Family- and genus-rank under-identified organisms have no aggregate representation; the catalog-view machinery (`insectQuery.insect().getByName(...)`) silently doesn't apply to them. The console papers over this by composing its own view models from entity queries — fine for now, but Phase 2's identification workflow expects an aggregate at the session's current rank.

**Shape options.**

1. Sealed `InsectAggregate` with permits `InsectFamilyAggregate`, `InsectGenusAggregate`, `InsectSpeciesAggregate` — pattern-match at consumers. Discoverable; clean visitor semantics.
2. Three parallel records, no shared supertype — simpler today, more duplication. Mirror of the entity records themselves.
3. Generic `InsectAggregate<ROOT extends InsectRankName, ENTITY>` — most compact but loses the per-rank semantic distinctions Phase 2 likely wants on its scope value object.

**Resolution path.** Decide between (1) and (2) when Phase 2 starts (the workflow's session-scope value object will inform which shape is ergonomic). Until then the console composes from entity queries; this is fine.

**Blocking:** Not currently — entity queries cover the console; Phase 1's mock seam doesn't need a polymorphic aggregate either.
```

- [ ] **Step 3: Commit**

```bash
git add docs/notes/parking-lot.md
git commit -m "Parking lot: PL-13 typed FK on InsectSpecies; PL-14 rank-polymorphic InsectAggregate"
```

---

## Task 2: Add `GenusQuery.forFamilyName(InsectFamilyName)` — api method

**Files:**

- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`

- [ ] **Step 1: Add the method to the `GenusQuery` interface**

In `InsectQuery.java`, modify the `GenusQuery` interface:

```java
    interface GenusQuery extends EntityQuery<InsectGenusName, InsectGenus, GenusCollection> {

        GenusCollection forFamilyName(InsectFamilyName familyName);
    }
```

Update the Javadoc usage block at the top of the file to list the new method:

```java
 * insectQuery.genera().forFamilyName(familyName);  // genera under a family
```

- [ ] **Step 2: Add the failing repository method**

Modify `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java`, replacing the `GenusRepository` interface:

```java
    protected interface GenusRepository
            extends EntityRepository<InsectGenusName, InsectGenus> {

        List<InsectGenus> getByFamilyName(InsectFamilyName familyName);
    }
```

Add the import if missing:

```java
import java.util.List;
```

- [ ] **Step 3: Confirm compile fails in the mock adapter**

Run: `mvn -pl domains/insects/insects-repository-test -am compile`

Expected: FAIL with "GenusRepositoryMock is not abstract and does not override abstract method getByFamilyName(InsectFamilyName) in InsectRepository.GenusRepository". Confirms the file Task 3 modifies — `GenusRepositoryMock.java`.

---

## Task 3: Implement `getByFamilyName` in `GenusRepositoryMock` + contract test

**Files:**

- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryMock.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java`

> **Pattern note.** `GenusRepositoryMock` extends `AbstractTestEntityRepository<NAME, ENTITY, SOURCE>` and the accessor is `testEntitySource()`. Mocks in this module **do not** carry observer-validation — null-rejection lives in the query-impl layer (Task 4). The mock's added methods are pure stream-filter, mirroring `InsectFunctionalRoleRepositoryMock.getByGuild`.

- [ ] **Step 1: Add the failing happy-path contract tests**

Append to the body of the `GenusRepositoryTest` interface (no existing `@Test default` methods today — these are the first two):

```java
    @Test
    default void getByFamilyName_returnsGeneraWithMatchingFamilyName() {
        InsectFamilyName halictidae = InsectFamilyName.of("halictidae");

        var results = repository().getByFamilyName(halictidae);

        assertThat(results)
                .extracting(InsectGenus::name)
                .extracting(InsectGenusName::value)
                .contains("halictus");
    }

    @Test
    default void getByFamilyName_returnsEmptyForUnknownFamily() {
        InsectFamilyName unknown = InsectFamilyName.of("unobtainium-idae");

        var results = repository().getByFamilyName(unknown);

        assertThat(results).isEmpty();
    }
```

Add imports as needed:

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
```

(Null-rejection is verified in Task 4 at the query-impl layer, matching the project pattern.)

- [ ] **Step 2: Run test to verify it fails to compile**

Run: `mvn -pl domains/insects/insects-repository-test -am test -Dtest=GenusRepositoryMockTest`

Expected: FAIL — the mock does not yet implement `getByFamilyName`.

- [ ] **Step 3: Implement `getByFamilyName` in `GenusRepositoryMock`**

Open `GenusRepositoryMock.java`. Add the method body — pure stream-filter, no observer:

```java
    @Override
    public List<InsectGenus> getByFamilyName(InsectFamilyName familyName) {
        return testEntitySource().entityStream()
                .filter(g -> familyName.equals(g.familyName()))
                .toList();
    }
```

Add the `java.util.List` import.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl domains/insects/insects-repository-test -am test -Dtest=GenusRepositoryMockTest`

Expected: PASS — two new tests plus all inherited contract tests.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java \
        domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java \
        domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryMock.java \
        domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java
git commit -m "GenusQuery: forFamilyName(InsectFamilyName) for member-genera lookups"
```

---

## Task 4: Wire `GenusQueryImpl.forFamilyName` + create `GenusQueryImplTest`

**Files:**

- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/GenusQueryImpl.java`
- Create: `domains/insects/insects-core/src/test/java/com/naturalist/insects/GenusQueryImplTest.java` (no precedent — mirror `FunctionalRoleQueryImplTest`)

- [ ] **Step 1: Create the failing query-impl test**

Create `domains/insects/insects-core/src/test/java/com/naturalist/insects/GenusQueryImplTest.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenusQueryImplTest
        implements EntityQueryContractTest<InsectGenusName, InsectGenus, GenusCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    GenusRepositoryMock repository = new GenusRepositoryMock(db);
    InsectQuery.GenusQuery query = new GenusQueryImpl(repository);

    @Override
    public EntityQuery<InsectGenusName, InsectGenus, GenusCollection> query() {
        return query;
    }

    @Override
    public InsectGenusName notFoundName() {
        return TestInsectsIdentifiers.InsectGenus.NotFound.name;
    }

    @Override
    public List<InsectGenusName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectGenus.Halictus.name,
                TestInsectsIdentifiers.InsectGenus.Andrena.name);
    }

    @Test
    void forFamilyName_rejectsNull() {
        assertThatThrownBy(() -> query.forFamilyName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyName");
    }

    @Test
    void forFamilyName_returnsGeneraInThatFamily() {
        GenusCollection collection = query.forFamilyName(InsectFamilyName.of("halictidae"));

        assertThat(collection.stream())
                .extracting(g -> g.name().value())
                .contains("halictus");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=GenusQueryImplTest`

Expected: FAIL — `GenusQueryImpl` does not yet implement `forFamilyName`.

- [ ] **Step 3: Add the implementation**

In `GenusQueryImpl.java`, append the method after `findByNameSet`:

```java
    @Override
    public GenusCollection forFamilyName(InsectFamilyName familyName) {
        observer().arguments("forFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByFamilyName(familyName));
    }
```

- [ ] **Step 4: Run the core module tests**

Run: `mvn -pl domains/insects/insects-core -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/GenusQueryImpl.java \
        domains/insects/insects-core/src/test/java/com/naturalist/insects/GenusQueryImplTest.java
git commit -m "GenusQueryImpl: implement forFamilyName + query-impl test"
```

---

## Task 5: Add `SpeciesQuery.forGenusEpithet(TaxonomicGenus)` — stopgap api method

**Files:**

- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryTest.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryMock.java`

- [ ] **Step 1: Add the api method**

Modify the `SpeciesQuery` interface in `InsectQuery.java`:

```java
    interface SpeciesQuery extends EntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

        /**
         * Members of a genus, joined by the text {@link TaxonomicGenus} epithet on the
         * species's {@link TaxonomicClassification}. Stopgap until {@code InsectSpecies}
         * carries a typed {@code InsectGenusName} upward reference (see PL-13).
         */
        SpeciesCollection forGenusEpithet(TaxonomicGenus genusEpithet);
    }
```

Add the import:

```java
import com.naturalist.taxonomy.TaxonomicGenus;
```

Update the file's Javadoc usage block:

```java
 * insectQuery.species().forGenusEpithet(TaxonomicGenus.of("Halictus"));
```

- [ ] **Step 2: Add the repository method**

Modify the `SpeciesRepository` interface in `InsectRepository.java`:

```java
    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesName, InsectSpecies> {

        List<InsectSpecies> getByGenusEpithet(TaxonomicGenus genusEpithet);
    }
```

Add the import:

```java
import com.naturalist.taxonomy.TaxonomicGenus;
```

- [ ] **Step 3: Add the failing happy-path contract tests**

Append to `SpeciesRepositoryTest.java`:

```java
    @Test
    default void getByGenusEpithet_returnsSpeciesWithMatchingGenus() {
        TaxonomicGenus battus = TaxonomicGenus.of("Battus");

        var results = repository().getByGenusEpithet(battus);

        assertThat(results)
                .extracting(s -> s.name().value())
                .contains("battus-philenor");
    }

    @Test
    default void getByGenusEpithet_returnsEmptyForUnknownGenus() {
        TaxonomicGenus unknown = TaxonomicGenus.of("Unobtainium");

        var results = repository().getByGenusEpithet(unknown);

        assertThat(results).isEmpty();
    }
```

Imports as needed:

```java
import com.naturalist.taxonomy.TaxonomicGenus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
```

(Null-rejection is verified in Task 6 at the query-impl layer.)

- [ ] **Step 4: Run test to verify it fails**

Run: `mvn -pl domains/insects/insects-repository-test -am test -Dtest=SpeciesRepositoryMockTest`

Expected: FAIL — compile error (method missing in mock).

- [ ] **Step 5: Implement in `SpeciesRepositoryMock`**

Add the method — pure stream-filter, no observer (mocks delegate validation to the query layer):

```java
    @Override
    public List<InsectSpecies> getByGenusEpithet(TaxonomicGenus genusEpithet) {
        return testEntitySource().entityStream()
                .filter(s -> {
                    TaxonomicGenus g = s.taxonomy().genus();
                    return g != null && g.equals(genusEpithet);
                })
                .toList();
    }
```

Add imports as needed:

```java
import com.naturalist.taxonomy.TaxonomicGenus;
import java.util.List;
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn -pl domains/insects/insects-repository-test -am test -Dtest=SpeciesRepositoryMockTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java \
        domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java \
        domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryTest.java \
        domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/SpeciesRepositoryMock.java
git commit -m "SpeciesQuery: forGenusEpithet stopgap (PL-13 retypeover pending)"
```

---

## Task 6: Wire `SpeciesQueryImpl.forGenusEpithet` + extend `SpeciesQueryImplTest`

**Files:**

- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/SpeciesQueryImpl.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/SpeciesQueryImplTest.java`

- [ ] **Step 1: Add failing query-impl tests**

Append to `SpeciesQueryImplTest.java`. The existing file declares the contract setup; add a `SpeciesRepositoryMock` field if not already present, plus the new `@Test` methods:

```java
    @Test
    void forGenusEpithet_rejectsNull() {
        assertThatThrownBy(() -> ((InsectQuery.SpeciesQuery) query).forGenusEpithet(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusEpithet");
    }

    @Test
    void forGenusEpithet_returnsSpeciesWithMatchingTaxonomyGenus() {
        SpeciesCollection collection =
                ((InsectQuery.SpeciesQuery) query).forGenusEpithet(TaxonomicGenus.of("Battus"));

        assertThat(collection.stream())
                .extracting(s -> s.name().value())
                .contains("battus-philenor");
    }
```

Imports to add:

```java
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

> Note: the existing test holds the query as `EntityQuery<...>`. Either change the field type to `InsectQuery.SpeciesQuery` so the new methods are callable without a cast (preferred), or use the cast inline as shown. If you change the field type, also change the `query()` accessor to wrap-and-return.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=SpeciesQueryImplTest`

Expected: FAIL — `SpeciesQueryImpl` does not implement `forGenusEpithet`.

- [ ] **Step 3: Add the implementation**

Append to `SpeciesQueryImpl.java`:

```java
    @Override
    public InsectEntityCollections.SpeciesCollection forGenusEpithet(TaxonomicGenus genusEpithet) {
        observer().arguments("forGenusEpithet",
                        i -> i.namedValue(genusEpithet, "genusEpithet"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(
                repository().getByGenusEpithet(genusEpithet));
    }
```

Import:

```java
import com.naturalist.taxonomy.TaxonomicGenus;
```

- [ ] **Step 4: Run module tests**

Run: `mvn -pl domains/insects/insects-core -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/SpeciesQueryImpl.java \
        domains/insects/insects-core/src/test/java/com/naturalist/insects/SpeciesQueryImplTest.java
git commit -m "SpeciesQueryImpl: implement forGenusEpithet + tests"
```

> **Optional cut point.** If the diff so far (Tasks 1–6) approaches the 400-line PR ceiling, push this as PR 1: "Insects api — forFamilyName + forGenusEpithet" and continue Tasks 7–11 in PR 2: "Insects console — family/genus pages".

---

## Task 7: Families list page — `/insects/families`

**Files:**

- Create: `domains/insects/insects-console/src/main/jte/insects/families.jte`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

- [ ] **Step 1: Add the controller route**

In `InsectsController.java`, add the new route below the existing `@GetMapping` for `/insects`:

```java
    @GetMapping("/families")
    String families(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectFamily> familyPage = insectQuery.families()
                .findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("familyPage", familyPage);
        return "insects/families";
    }
```

Imports (add if missing):

```java
import com.naturalist.insects.InsectFamily;
```

- [ ] **Step 2: Create the template**

Create `domains/insects/insects-console/src/main/jte/insects/families.jte`:

```jte
@import com.naturalist.insects.InsectFamily
@import com.naturalist.data.Page

@param Page<InsectFamily> familyPage

@template.layout.page(title = "Insect Families", content = @`
    <a href="/insects">&larr; Back to species catalog</a>
    <h1>Insect Families</h1>
    <p>Family-rank records in the Oak Vista catalog.
       <a href="/insects/genera">Browse genera &rarr;</a></p>

    <div class="entity-grid">
        @for(var f : familyPage.content())
            !{var commonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
            <article>
                <header>
                    <a href="/insects/families/${f.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>order</dt>
                    <dd>${f.order().value()}</dd>
                    <dt>family</dt>
                    <dd><em>${f.family().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = familyPage, baseUrl = "/insects/families")
`)
```

- [ ] **Step 3: Add the smoke test**

Create `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectFamilyTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsFamiliesTemplateTest {

    @Test
    void families_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectFamily> familyPage = new InsectFamilyTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/families.jte",
                Map.of("familyPage", familyPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
```

> Note: confirm `InsectFamilyTestEntitySource` has a `pageOf(PageRequest)` accessor. If the existing source uses a different method name, follow the same idiom `InsectSpeciesTestEntitySource` uses (which is what `InsectsListTemplateTest` exercises).

- [ ] **Step 4: Run module tests**

Run: `mvn -pl domains/insects/insects-console -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
        domains/insects/insects-console/src/main/jte/insects/families.jte \
        domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java
git commit -m "Console: /insects/families paged list view"
```

---

## Task 8: Family detail page — `/insects/families/{name}`

**Files:**

- Create: `domains/insects/insects-console/src/main/jte/insects/family.jte`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java` (add `family_rendersWithoutError`)

- [ ] **Step 1: Add the controller route**

Append to `InsectsController.java`:

```java
    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, Model model) {
        var familyName = InsectFamilyName.of(name);
        var family = insectQuery.families().getByName(familyName);
        if (family.isEmpty()) {
            return "redirect:/insects/families";
        }
        var description = family.get().description();
        var genera = insectQuery.genera().forFamilyName(familyName).stream()
                .sorted(Comparator.comparing(g -> g.name().value()))
                .toList();
        model.addAttribute("family", family.get());
        model.addAttribute("genera", genera);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/family";
    }
```

Imports (add if missing):

```java
import com.naturalist.insects.InsectFamilyName;
```

- [ ] **Step 2: Create the template**

Create `domains/insects/insects-console/src/main/jte/insects/family.jte`:

```jte
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import java.util.List

@param InsectFamily family
@param List<InsectGenus> genera
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = family.family().value(), content = @`
    <a href="/insects/families">&larr; Back to families</a>

    <h1>${family.family().value()}</h1>
    <p class="taxonomy">${family.order().value()}</p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Genera in this family</h2>
        @if(genera.isEmpty())
            <p><em>No genera catalogued under this family yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var g : genera)
                    !{var commonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
                    <article>
                        <header>
                            <a href="/insects/genera/${g.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>genus</dt>
                            <dd><em>${g.genus().value()}</em></dd>
                        </dl>
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
```

- [ ] **Step 3: Add the smoke test**

Append a second test to `InsectsFamiliesTemplateTest.java`:

```java
    @Test
    void family_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectFamily anyFamily = new InsectFamilyTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", anyFamily,
                        "genera", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
```

Add imports as needed:

```java
import java.util.List;
import com.naturalist.insects.InsectFamily;
```

- [ ] **Step 4: Run module tests**

Run: `mvn -pl domains/insects/insects-console -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
        domains/insects/insects-console/src/main/jte/insects/family.jte \
        domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java
git commit -m "Console: /insects/families/{name} detail with member genera"
```

---

## Task 9: Genera list + genus detail pages

**Files:**

- Create: `domains/insects/insects-console/src/main/jte/insects/genera.jte`
- Create: `domains/insects/insects-console/src/main/jte/insects/genus.jte`
- Create: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

- [ ] **Step 1: Add the controller routes**

Append to `InsectsController.java`:

```java
    @GetMapping("/genera")
    String genera(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectGenus> genusPage = insectQuery.genera()
                .findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("genusPage", genusPage);
        return "insects/genera";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, Model model) {
        var genusName = InsectGenusName.of(name);
        var genus = insectQuery.genera().getByName(genusName);
        if (genus.isEmpty()) {
            return "redirect:/insects/genera";
        }
        var description = genus.get().description();
        var members = insectQuery.species()
                .forGenusEpithet(genus.get().genus())
                .stream()
                .sorted(Comparator.comparing(s -> s.name().value()))
                .toList();
        model.addAttribute("genus", genus.get());
        model.addAttribute("species", members);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/genus";
    }
```

Imports:

```java
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectGenusName;
```

- [ ] **Step 2: Create the genera list template**

Create `domains/insects/insects-console/src/main/jte/insects/genera.jte`:

```jte
@import com.naturalist.insects.InsectGenus
@import com.naturalist.data.Page

@param Page<InsectGenus> genusPage

@template.layout.page(title = "Insect Genera", content = @`
    <a href="/insects">&larr; Back to species catalog</a>
    <h1>Insect Genera</h1>
    <p>Genus-rank records in the Oak Vista catalog.
       <a href="/insects/families">Browse families &rarr;</a></p>

    <div class="entity-grid">
        @for(var g : genusPage.content())
            !{var commonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
            <article>
                <header>
                    <a href="/insects/genera/${g.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>family</dt>
                    <dd><a href="/insects/families/${g.familyName().value()}">${g.family().value()}</a></dd>
                    <dt>genus</dt>
                    <dd><em>${g.genus().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = genusPage, baseUrl = "/insects/genera")
`)
```

- [ ] **Step 3: Create the genus detail template**

Create `domains/insects/insects-console/src/main/jte/insects/genus.jte`:

```jte
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectSpecies
@import java.util.List

@param InsectGenus genus
@param List<InsectSpecies> species
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = genus.genus().value(), content = @`
    <a href="/insects/genera">&larr; Back to genera</a>

    <h1><em>${genus.genus().value()}</em></h1>
    <p class="taxonomy">
        ${genus.order().value()} &middot;
        <a href="/insects/families/${genus.familyName().value()}">${genus.family().value()}</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Species in this genus</h2>
        @if(species.isEmpty())
            <p><em>No species catalogued under this genus yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var s : species)
                    !{var commonName = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(s.name().value());}
                    <article>
                        <header>
                            <a href="/insects/${s.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            @if(s.taxonomy().species() != null)
                                <dt>species</dt>
                                <dd><em>${s.taxonomy().species().value()}</em></dd>
                            @endif
                        </dl>
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
```

- [ ] **Step 4: Add the smoke tests**

Create `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectGenusTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsGeneraTemplateTest {

    @Test
    void genera_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectGenus> genusPage = new InsectGenusTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genera.jte",
                Map.of("genusPage", genusPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void genus_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectGenus anyGenus = new InsectGenusTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genus.jte",
                Map.of(
                        "genus", anyGenus,
                        "species", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
```

- [ ] **Step 5: Run module tests**

Run: `mvn -pl domains/insects/insects-console -am test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
        domains/insects/insects-console/src/main/jte/insects/genera.jte \
        domains/insects/insects-console/src/main/jte/insects/genus.jte \
        domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java
git commit -m "Console: /insects/genera list + /insects/genera/{name} detail"
```

---

## Task 10: `InsectsLinker` + species-list nav

**Files:**

- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/catalog/InsectsLinker.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/list.jte`

- [ ] **Step 1: Route family + genus search hits**

Modify `InsectsLinker.java`:

```java
package com.naturalist.insects.console.catalog;

import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectSpeciesName;

@DomainService
public class InsectsLinker implements EntityRefLinker {

    @Override
    public String linkFor(EntityRef ref) {
        return switch (ref.name()) {
            case InsectSpeciesName n -> "/insects/" + n.value();
            case InsectGenusName n -> "/insects/genera/" + n.value();
            case InsectFamilyName n -> "/insects/families/" + n.value();
            default -> null;
        };
    }
}
```

- [ ] **Step 2: Add browse links to the species catalog header**

In `domains/insects/insects-console/src/main/jte/insects/list.jte`, replace the existing intro paragraph with:

```jte
    <h1>Insect Catalog</h1>
    <p>Insect species documented at Oak Vista.
       <a href="/insects/families">Browse families</a> &middot;
       <a href="/insects/genera">Browse genera</a></p>
```

(Keep the existing `<div class="entity-grid">` block and everything after it unchanged.)

- [ ] **Step 3: Verify console build**

Run: `mvn -pl domains/insects/insects-console -am test`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/catalog/InsectsLinker.java \
        domains/insects/insects-console/src/main/jte/insects/list.jte
git commit -m "Console: link family + genus search hits; surface browse links from species catalog"
```

---

## Task 11: Full-build verification + work-tracker rollforward

**Files:**

- Modify: `docs/work-tracker.md`

- [ ] **Step 1: Run `mvn verify` from repo root**

Run: `mvn verify`

Expected: BUILD SUCCESS across all modules. Per the project's memory, failures concentrate in console / Spring Boot / JTE modules — JTE-compile errors surface here if any `@param` declaration drifts.

- [ ] **Step 2: Manual smoke**

Start the console (the user runs this themselves):

```bash
mvn -pl apps/management-console spring-boot:run
```

Visit `http://localhost:8080/insects`, click "Browse families", click into a family, confirm the member genera grid is populated. Click into `halictidae` → confirm `halictus` shows; click into `halictus` → confirm species list (text-matched) renders. Click "Browse genera" from the home page; click `chrysoperla` → confirm `green-lacewing` species record is not listed (it was deleted in PL-2; only species records still in `insect-species.json` whose taxonomy genus epithet matches will appear).

- [ ] **Step 3: Roll work-tracker forward**

Update `docs/work-tracker.md`:

- Bump the "Last updated" line and the **Current slice** paragraph to reflect the close of this slice.
- Move a new row to the top of the **Recently completed** table:

```markdown
| Insects console — family + genus list/detail pages | 2026-05-22 | [`plans/insects-family-genus-console.md`](plans/insects-family-genus-console.md) | `<final-commit-hash>` |
```

Backfill `<final-commit-hash>` once Step 4 commits.

- Replace the **Current slice** with a fresh "No active slice — Phase 0 console pages just landed" line, plus the standing candidate-next-slices block (PL-2 fixture migration, sightings entity).
- Update the parking lot count line to reflect PL-13 (raised in Task 1).

- [ ] **Step 4: Commit**

```bash
git add docs/work-tracker.md
git commit -m "Roll work-tracker forward — Phase 0 console family/genus pages closed"
```

Then update the table row with the actual commit hash if you'd like to backfill (per project convention; see commit `4aa768b` for an example).

---

## Done When

- `mvn verify` is green from the repo root.
- `/insects/families`, `/insects/families/{name}`, `/insects/genera`, `/insects/genera/{name}` all render against the live fixture data.
- The species catalog header at `/insects` surfaces "Browse families" / "Browse genera" links.
- Search hits whose target is `InsectFamilyName` or `InsectGenusName` resolve to the new detail URLs (verifiable: `/search?q=halictidae` — the family hit's link now points at `/insects/families/halictidae` instead of being dropped).
- `GenusQuery.forFamilyName(InsectFamilyName)` is wired through repository + impl + contract tests.
- `SpeciesQuery.forGenusEpithet(TaxonomicGenus)` is wired through with explicit PL-13 follow-up.
- `docs/notes/parking-lot.md` carries the new PL-13 entry.
- `docs/work-tracker.md` is rolled forward with the new "Recently completed" row.

## Out of Scope (carry into follow-up slices)

- Typed `InsectGenusName` / `InsectFamilyName` foreign keys on `InsectSpecies` (PL-13 — the follow-up this slice raises).
- Formal taxonomic-scope breadcrumb component (Phase 0's reusable primitive; Phase 2 also reuses it).
- Life-stage rendering on family / genus detail pages.
- Plants console pages for family / genus (Phase 0 plants UI is gated on plant identification activating).
- Family / genus detail edit forms (the species detail has a "Register photo" form; family / genus get nothing in this slice).
- `InsectsCatalogContribution` token changes — already emits family/genus tokens; this slice just gives them somewhere to land.
