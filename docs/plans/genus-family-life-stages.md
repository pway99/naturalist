# Genus / Family Life-Stage API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend `InsectGenus` and `InsectFamily` records to optionally carry life-stage observations (egg/larva/pupa/adult), so under-identified organisms can move to their actual rank in subsequent slices without losing their life-stage data.

**Architecture:** Mirror the species-level shape onto the two new aggregates — four nullable life-stage components per record. Add `LifeStageName.of(...)` factory overloads for genus and family parents (the composite slug shape `{parent-slug}-{stage-slug}` is unchanged; only the typed parent identifier changes). Jackson native record deserialization handles the new optional JSON fields automatically; existing JSON files load unchanged with nulls in the new components. No data migration in this slice — that lands in subsequent slices once the API supports it.

**Tech Stack:** Java records, JUnit Jupiter, Jackson 2.19, Maven multi-module. Conventions per `domains/CLAUDE.md` and `domains/insects/CLAUDE.md`.

**Scope boundary.** This slice is Phase 0 slice 1 of the identification roadmap (`docs/plans/identification.md`). It does NOT:
- Move any catalog data between files (slice 2 / 3).
- Add console pages (slices 4–7).
- Emit life-stage search tokens from the catalog contribution (deferred; species-level life stages don't emit tokens either — symmetric).
- Add cross-stage invariants on genus / family records (e.g., ChemicalDefense coherence). Species-level cross-stage invariants stay where they are.
- Add `with*` methods for fields *other than* the four new life-stage fields. Existing `InsectGenus` and `InsectFamily` already lack `with*` methods for their pre-existing fields; that gap is not this slice's concern.

---

## File Structure

**Modify:**
- `domains/identifiers/src/main/java/com/naturalist/insects/LifeStageName.java` — add two factory overloads
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java` — add 4 life-stage components + invariants + 4 `with*` methods
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamily.java` — same as InsectGenus
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java` — update `newEntity` / `ghostEntity` / `modifiedEntity` to pass nulls for life-stage fields
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryTest.java` — same
- Any other `new InsectGenus(...)` or `new InsectFamily(...)` call sites discovered by grep — pass nulls for the new fields

**Create:**
- `domains/identifiers/src/test/java/com/naturalist/insects/LifeStageNameTest.java` — first dedicated test for `LifeStageName` (none exist today)

**Verify-only:**
- `domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json` — confirms existing records still load
- `domains/insects/insects-repository-test/src/main/resources/insects/insect-families.json` — same

---

### Task 1: Add `LifeStageName.of(InsectGenusName, LifeStageKind)` factory

**Files:**
- Create: `domains/identifiers/src/test/java/com/naturalist/insects/LifeStageNameTest.java`
- Modify: `domains/identifiers/src/main/java/com/naturalist/insects/LifeStageName.java`

- [ ] **Step 1: Write the failing test**

Create `domains/identifiers/src/test/java/com/naturalist/insects/LifeStageNameTest.java`:

```java
package com.naturalist.insects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LifeStageNameTest {

    @Test
    void factoryComposesGenusSlugWithStageKind() {
        LifeStageName name = LifeStageName.of(
                InsectGenusName.of("chrysoperla"),
                LifeStageKind.LARVA);

        assertThat(name.value()).isEqualTo("chrysoperla-larva");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl domains/identifiers -am test -Dtest=LifeStageNameTest`

Expected: FAIL with `cannot find symbol: method of(InsectGenusName, LifeStageKind)`.

- [ ] **Step 3: Add the genus-parent factory**

Modify `domains/identifiers/src/main/java/com/naturalist/insects/LifeStageName.java`. Add this method after the existing `of(InsectSpeciesName, LifeStageKind)`:

```java
    public static LifeStageName of(InsectGenusName genus, LifeStageKind kind) {
        Objects.requireNonNull(genus, "genus");
        Objects.requireNonNull(kind, "kind");
        return new LifeStageName(genus.value() + "-" + kind.slug());
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl domains/identifiers -am test -Dtest=LifeStageNameTest`

Expected: PASS, 1 test.

- [ ] **Step 5: Commit**

```bash
git add domains/identifiers/src/main/java/com/naturalist/insects/LifeStageName.java \
        domains/identifiers/src/test/java/com/naturalist/insects/LifeStageNameTest.java
git commit -m "Add LifeStageName factory for InsectGenusName parent"
```

---

### Task 2: Add `LifeStageName.of(InsectFamilyName, LifeStageKind)` factory

**Files:**
- Modify: `domains/identifiers/src/test/java/com/naturalist/insects/LifeStageNameTest.java`
- Modify: `domains/identifiers/src/main/java/com/naturalist/insects/LifeStageName.java`

- [ ] **Step 1: Add the failing test**

Append to `LifeStageNameTest.java`, inside the class body:

```java
    @Test
    void factoryComposesFamilySlugWithStageKind() {
        LifeStageName name = LifeStageName.of(
                InsectFamilyName.of("syrphidae"),
                LifeStageKind.PUPA);

        assertThat(name.value()).isEqualTo("syrphidae-pupa");
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl domains/identifiers -am test -Dtest=LifeStageNameTest`

Expected: FAIL with `cannot find symbol: method of(InsectFamilyName, LifeStageKind)`.

- [ ] **Step 3: Add the family-parent factory**

Modify `LifeStageName.java`, adding after the genus factory from Task 1:

```java
    public static LifeStageName of(InsectFamilyName family, LifeStageKind kind) {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(kind, "kind");
        return new LifeStageName(family.value() + "-" + kind.slug());
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl domains/identifiers -am test -Dtest=LifeStageNameTest`

Expected: PASS, 2 tests.

- [ ] **Step 5: Commit**

```bash
git add domains/identifiers/src/main/java/com/naturalist/insects/LifeStageName.java \
        domains/identifiers/src/test/java/com/naturalist/insects/LifeStageNameTest.java
git commit -m "Add LifeStageName factory for InsectFamilyName parent"
```

---

### Task 3: Find all `new InsectGenus(...)` call sites

**Files:**
- Inspect only — no edits in this task. The output drives Task 4's call-site update list.

- [ ] **Step 1: List call sites**

Run: `grep -rn "new InsectGenus(" /Users/pat/dev/naturalist --include="*.java" | grep -v target | grep -v worktrees`

Record the full list. Expected callers (verify against actual output):
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java` — three call sites (`newEntity`, `ghostEntity`, `modifiedEntity`).
- Any `*Test.java` under `domains/insects/insects-core` or `domains/insects/insects-api` that constructs an `InsectGenus`.

- [ ] **Step 2: List `new InsectFamily(...)` call sites symmetrically**

Run: `grep -rn "new InsectFamily(" /Users/pat/dev/naturalist --include="*.java" | grep -v target | grep -v worktrees`

Expected callers (verify):
- `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryTest.java` — three call sites.
- Possibly other test files.

Save both lists; Tasks 5 and 8 reference them.

---

### Task 4: Extend `InsectGenus` with life-stage components — failing test

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java`
- Create or modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectGenusTest.java`

- [ ] **Step 1: Check whether `InsectGenusTest` exists**

Run: `find /Users/pat/dev/naturalist/domains/insects/insects-api/src/test -name "InsectGenusTest.java" | grep -v target | grep -v worktrees`

If the file exists, modify it. If not, create it.

- [ ] **Step 2: Add the failing test**

Create or extend `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectGenusTest.java`:

```java
package com.naturalist.insects;

import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.StageHabitat;
import com.naturalist.insects.lifestage.StagePhenology;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectGenusTest {

    @Test
    void carriesNullableLarvaStageWithCompositeSlugDerivedFromGenusName() {
        InsectGenusName name = InsectGenusName.of("chrysoperla");
        LarvaStage larva = new LarvaStage(
                LifeStageName.of(name, LifeStageKind.LARVA),
                StagePhenology.empty(),
                stageHabitat(),
                null,
                description("L"),
                null, null, null, null, null);

        InsectGenus genus = new InsectGenus(
                name,
                InsectFamilyName.of("chrysopidae"),
                TaxonomicOrder.of("Neuroptera"),
                TaxonomicFamily.of("Chrysopidae"),
                TaxonomicGenus.of("Chrysoperla"),
                description("G"),
                Set.of(),
                null,
                larva,
                null,
                null);

        assertThat(genus.larva()).isEqualTo(larva);
        assertThat(genus.larva().name().value()).isEqualTo("chrysoperla-larva");
        assertThat(genus.egg()).isNull();
        assertThat(genus.pupa()).isNull();
        assertThat(genus.adult()).isNull();
    }

    private static Description description(String tag) {
        return new Description(tag + "1", tag + "2", tag + "3", tag + "4");
    }

    private static StageHabitat stageHabitat() {
        return new StageHabitat(
                new HabitatProfile(
                        Set.of(HabitatZone.MEADOW),
                        MoistureRegime.MESIC,
                        LightRegime.FULL_SUN,
                        Set.of(VerticalLayer.HERBACEOUS_LAYER)),
                null, null, null);
    }
}
```

> Note: `LarvaStage` field order is taken from the existing source — verify against `domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/LarvaStage.java`. If the field list has changed, adjust the constructor call to match.

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -pl domains/insects/insects-api -am test -Dtest=InsectGenusTest`

Expected: FAIL — compilation error, "constructor InsectGenus in class InsectGenus cannot be applied to given types" (the 11-arg call doesn't match the current 7-arg record).

---

### Task 5: Extend `InsectGenus` record — implementation

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java`
- Modify: all `new InsectGenus(...)` call sites from Task 3.

- [ ] **Step 1: Replace the `InsectGenus` record body**

Replace the entire record header and `invariants()` method in `InsectGenus.java`:

```java
public record InsectGenus(
        InsectGenusName name,
        InsectFamilyName familyName,
        TaxonomicOrder order,
        TaxonomicFamily family,
        TaxonomicGenus genus,
        Description description,
        Set<CommonName> commonNames,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult
) implements NamedEntity<InsectGenusName>, LinnaeanGenus<InsectFamilyName> {

    public InsectGenus withEgg(@Nullable EggStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, value, larva, pupa, adult);
    }

    public InsectGenus withLarva(@Nullable LarvaStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, egg, value, pupa, adult);
    }

    public InsectGenus withPupa(@Nullable PupaStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, egg, larva, value, adult);
    }

    public InsectGenus withAdult(@Nullable AdultStage value) {
        return new InsectGenus(name, familyName, order, family, genus,
                description, commonNames, egg, larva, pupa, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {
            i.entityName(name, "name")
                    .entityName(familyName, "familyName")
                    .namedValue(order, "order")
                    .namedValue(family, "family")
                    .namedValue(genus, "genus")
                    .valueObject(description, "description")
                    .notNull(commonNames, "commonNames");
            if (egg != null) i.namedEntity(this, InsectGenus::egg, "egg");
            if (larva != null) i.namedEntity(this, InsectGenus::larva, "larva");
            if (pupa != null) i.namedEntity(this, InsectGenus::pupa, "pupa");
            if (adult != null) i.namedEntity(this, InsectGenus::adult, "adult");
        };
    }
}
```

Add imports at the top of the file as needed:

```java
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.PupaStage;
import org.jspecify.annotations.Nullable;
```

> Note: the explicit `if (field != null) i.namedEntity(...)` pattern matches `InsectSpecies.invariants()` for its nullable life-stage fields — `Constraints` has `valueObjectOrNull(...)` for value objects but no equivalent `namedEntityOrNull(...)` for `NamedEntity` references, so the null-check is explicit. Body block uses a statement lambda (`i -> { ... }`) rather than expression form so the `if`-guards compose.

- [ ] **Step 2: Update `GenusRepositoryTest` call sites**

Modify `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java`. For each `new InsectGenus(...)` call (`newEntity`, `ghostEntity`, `modifiedEntity`), append `null, null, null, null` to the argument list. Example for `newEntity`:

```java
    @Override
    default InsectGenus newEntity() {
        return new InsectGenus(
                InsectGenusName.of("test-genus-xx"),
                InsectFamilyName.of("tachinidae"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null,
                null,
                null);
    }
```

Apply the same `null, null, null, null` suffix to `ghostEntity()` and `modifiedEntity()`.

- [ ] **Step 3: Update any other `new InsectGenus(...)` call sites**

For each remaining caller from Task 3's grep output, append `null, null, null, null` to the argument list.

- [ ] **Step 4: Run the InsectGenus test to verify it passes**

Run: `mvn -pl domains/insects/insects-api -am test -Dtest=InsectGenusTest`

Expected: PASS, 1 test.

- [ ] **Step 5: Run full insects-api + repository-test compile**

Run: `mvn -pl domains/insects/insects-api,domains/insects/insects-repository-test -am test`

Expected: All tests PASS. Watch for surprise call-site failures — if any, add `null, null, null, null` to those sites too and re-run.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java \
        domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectGenusTest.java \
        domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java
# Add any other modified call sites from Step 3.
git commit -m "InsectGenus: optional life-stage components (egg/larva/pupa/adult)"
```

---

### Task 6: Verify `insect-genera.json` still loads with the new optional fields

**Files:**
- Verify only: `domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`
- Verify: existing genus test source unit test.

- [ ] **Step 1: Run the test source's own test**

Run: `mvn -pl domains/insects/insects-repository-test -am test -Dtest=InsectGenusTestEntitySourceTest`

Expected: PASS. The four existing JSON records (`halictus`, `andrena`, `chrysoperla`, `empoasca`) load into `InsectGenus` instances with `null` in all four new life-stage fields. Jackson's record support handles missing JSON fields by passing `null` when the record component type permits it.

- [ ] **Step 2: If Jackson behaviour surprises (unlikely with native record support)**

If the test fails with `MissingKotlinParameterException`-equivalent or "missing required property `egg`" from Jackson:

Add `@com.fasterxml.jackson.annotation.JsonInclude(JsonInclude.Include.NON_NULL)` to the new components, OR check the project's `ObjectMapper` configuration in the `data` kernel for whether `FAIL_ON_MISSING_CREATOR_PROPERTIES` is disabled (the standard project setting per existing nullable fields on `InsectSpecies`).

Resolution lives at the `ObjectMapper` level, not on the record. Do not annotate the record fields; match the existing `InsectSpecies` pattern.

- [ ] **Step 3: No commit if Step 1 passed clean**

If Task 5's commit already covered everything and this task is verification-only, skip the commit. Otherwise commit any fixes from Step 2.

---

### Task 7: Extend `InsectFamily` with life-stage components — failing test

**Files:**
- Create or modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectFamilyTest.java`

- [ ] **Step 1: Add the failing test**

Create or extend `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectFamilyTest.java`:

```java
package com.naturalist.insects;

import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.StageHabitat;
import com.naturalist.insects.lifestage.StagePhenology;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectFamilyTest {

    @Test
    void carriesNullableAdultStageWithCompositeSlugDerivedFromFamilyName() {
        InsectFamilyName name = InsectFamilyName.of("syrphidae");
        AdultStage adult = new AdultStage(
                LifeStageName.of(name, LifeStageKind.ADULT),
                StagePhenology.empty(),
                stageHabitat(),
                null,
                description("A"),
                null, null, null, null, null, null);

        InsectFamily family = new InsectFamily(
                name,
                TaxonomicOrder.of("Diptera"),
                TaxonomicFamily.of("Syrphidae"),
                description("F"),
                Set.of(),
                null,
                null,
                null,
                adult);

        assertThat(family.adult()).isEqualTo(adult);
        assertThat(family.adult().name().value()).isEqualTo("syrphidae-adult");
        assertThat(family.egg()).isNull();
        assertThat(family.larva()).isNull();
        assertThat(family.pupa()).isNull();
    }

    private static Description description(String tag) {
        return new Description(tag + "1", tag + "2", tag + "3", tag + "4");
    }

    private static StageHabitat stageHabitat() {
        return new StageHabitat(
                new HabitatProfile(
                        Set.of(HabitatZone.MEADOW),
                        MoistureRegime.MESIC,
                        LightRegime.FULL_SUN,
                        Set.of(VerticalLayer.HERBACEOUS_LAYER)),
                null, null, null);
    }
}
```

> Note: `AdultStage`'s exact constructor signature is in `domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/AdultStage.java`. Verify against the actual source; adjust the constructor call to match the current component list (it has more nullable fields than `LarvaStage`).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl domains/insects/insects-api -am test -Dtest=InsectFamilyTest`

Expected: FAIL — "constructor InsectFamily cannot be applied to given types" (5-arg record can't accept 9 args).

---

### Task 8: Extend `InsectFamily` record — implementation

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamily.java`
- Modify: all `new InsectFamily(...)` call sites from Task 3.

- [ ] **Step 1: Replace the `InsectFamily` record body**

Replace the entire record header and `invariants()` method:

```java
public record InsectFamily(
        InsectFamilyName name,
        TaxonomicOrder order,
        TaxonomicFamily family,
        Description description,
        Set<CommonName> commonNames,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult
) implements NamedEntity<InsectFamilyName>, LinnaeanFamily {

    public InsectFamily withEgg(@Nullable EggStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                value, larva, pupa, adult);
    }

    public InsectFamily withLarva(@Nullable LarvaStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                egg, value, pupa, adult);
    }

    public InsectFamily withPupa(@Nullable PupaStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                egg, larva, value, adult);
    }

    public InsectFamily withAdult(@Nullable AdultStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                egg, larva, pupa, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {
            i.entityName(name, "name")
                    .namedValue(order, "order")
                    .namedValue(family, "family")
                    .valueObject(description, "description")
                    .notNull(commonNames, "commonNames");
            if (egg != null) i.namedEntity(this, InsectFamily::egg, "egg");
            if (larva != null) i.namedEntity(this, InsectFamily::larva, "larva");
            if (pupa != null) i.namedEntity(this, InsectFamily::pupa, "pupa");
            if (adult != null) i.namedEntity(this, InsectFamily::adult, "adult");
        };
    }
}
```

Add imports as in Task 5.

- [ ] **Step 2: Update `FamilyRepositoryTest` call sites**

Modify `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryTest.java`. For each `new InsectFamily(...)` (in `newEntity`, `ghostEntity`, `modifiedEntity`), append `null, null, null, null` to the argument list.

- [ ] **Step 3: Update any other `new InsectFamily(...)` call sites**

For each remaining caller from Task 3's grep output, append `null, null, null, null`.

- [ ] **Step 4: Run the InsectFamily test to verify it passes**

Run: `mvn -pl domains/insects/insects-api -am test -Dtest=InsectFamilyTest`

Expected: PASS, 1 test.

- [ ] **Step 5: Verify `insect-families.json` still loads**

Run: `mvn -pl domains/insects/insects-repository-test -am test -Dtest=InsectFamilyTestEntitySourceTest`

Expected: PASS. All existing family records load with `null` in life-stage fields.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamily.java \
        domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectFamilyTest.java \
        domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryTest.java
# Add any other modified call sites from Step 3.
git commit -m "InsectFamily: optional life-stage components (egg/larva/pupa/adult)"
```

---

### Task 9: Full-build verification

**Files:**
- None modified — verification step only.

- [ ] **Step 1: Run `mvn verify` from repo root**

Run: `mvn verify`

Expected: BUILD SUCCESS across all modules. Per the project's memory, failures concentrate in console / Spring Boot / JTE modules — if anything fails there, the most likely cause is a `new InsectGenus(...)` or `new InsectFamily(...)` call site in a console controller or JTE-rendering test that the earlier grep missed.

- [ ] **Step 2: If a missed call site surfaces**

Add `null, null, null, null` to it. Re-run `mvn verify`. Commit any fixes:

```bash
git add <missed-call-site-file>
git commit -m "Update <module> call sites for InsectGenus/InsectFamily life-stage components"
```

- [ ] **Step 3: No commit if Step 1 passed clean**

---

## Done When

- `mvn verify` is green from the repo root.
- `InsectGenus` and `InsectFamily` records each carry four nullable life-stage components.
- Each record has `withEgg` / `withLarva` / `withPupa` / `withAdult` methods.
- `LifeStageName.of(InsectGenusName, LifeStageKind)` and `LifeStageName.of(InsectFamilyName, LifeStageKind)` factories exist and produce the expected composite slugs.
- Existing `insect-genera.json` and `insect-families.json` data still loads via the test sources (every life-stage field is `null` in current data).
- No JSON data was modified in this slice — the API just *accepts* life stages at genus/family rank now.

## Out of Scope (carry into subsequent slices)

- Moving any under-identified species records into the genus/family files (slice 2 — insects; slice 3 — plants).
- Console pages for family/genus catalog views (slices 4–6).
- Taxonomic-scope rendering primitive (slice 7).
- Cross-stage invariants (ChemicalDefense coherence, etc.) on `InsectGenus` / `InsectFamily`.
- `with*` methods for the pre-existing fields on `InsectGenus` / `InsectFamily`.
- Catalog token emission for life-stage names attached to genera/families — symmetric with species side, which doesn't emit life-stage tokens either.
