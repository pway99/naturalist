# Phase 1: Refactor InsectGenus — Drop Local Epithet Copies

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `TaxonomicOrder order` and `TaxonomicFamily family` local copies from `InsectGenus`, aligning it with the `InsectSpecies` FK pattern where each rank carries only typed `EntityName` FKs to ancestors and its own `Taxonomic*` epithet.

**Architecture:** `InsectGenus` drops two components and the `LinnaeanGenus` kernel interface drops its `family()` method. Console templates that displayed the family epithet or order from the genus record now resolve the parent `InsectFamily` entity instead. The catalog contribution is unaffected — it only uses `InsectGenus.genus()` (retained).

**Tech Stack:** Java 21 records, JTE templates, Jackson JSON deserialization, JUnit 5 + AssertJ.

**Design spec:** [`docs/plans/insect-order-design.md`](insect-order-design.md) — Phase 1 section.

---

### Task 1: Update LinnaeanGenus kernel interface

**Files:**
- Modify: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/LinnaeanGenus.java`

- [ ] **Step 1: Remove the `family()` method and update Javadoc**

Replace the entire file contents with:

```java
package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;

/**
 * Contract for catalog entities that represent a genus-rank Linnaean taxon.
 * <p>
 * The contract exposes the upward typed reference to the parent family
 * aggregate and the genus epithet, which is non-null at the implementing
 * entity level. The genus slug is derived mechanically from the epithet.
 *
 * <p>Implemented by per-domain genus aggregates (e.g.
 * {@code com.naturalist.insects.InsectGenus}). The implementing entity
 * supplies its own typed name via its {@code NamedEntity<GENUS_NAME>}
 * binding; this interface is concerned only with the rank-level contract.
 *
 * @param <FAMILY_NAME> the parent family aggregate's typed name
 */
public interface LinnaeanGenus<FAMILY_NAME extends EntityName> {

    FAMILY_NAME familyName();

    TaxonomicGenus genus();

    /**
     * The genus slug derived from the genus epithet — lowercase kebab form.
     * The slug is {@code identity}; common names are findable but not
     * authoritative.
     */
    default String genusSlug() {
        return TaxonomicSlugs.genusSlug(genus());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add kernels/taxonomy/src/main/java/com/naturalist/taxonomy/LinnaeanGenus.java
git commit -m "LinnaeanGenus: drop family() — domain models the world, not the catalog

The parent-child consistency check that motivated the local family()
epithet moves to catalog-assembly time where the parent entity is
resolved anyway. The domain model should not carry redundant data to
serve a presentation concern."
```

---

### Task 2: Refactor InsectGenus record — drop order and family components

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java`

- [ ] **Step 1: Remove `order` and `family` components, imports, and update Javadoc**

Replace the entire file contents with:

```java
package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.PupaStage;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanGenus;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued insect genus — the rank between {@link InsectFamily} and
 * {@link InsectSpecies} in the Linnaean hierarchy.
 * <p>
 * Genus-rank records are first-class catalog citizens — a naturalist who
 * recognises a {@code Halictus} sweat bee without resolving the species has
 * a permanent home for that observation here. As identification firms, an
 * {@link InsectSpecies} record is added alongside this genus record; the
 * genus record is never replaced or migrated.
 * <p>
 * The {@link #familyName} component is the upward typed reference to the
 * parent {@link InsectFamily}. When the family epithet or order is needed
 * for display, the consumer resolves the parent entity — the genus does
 * not carry local copies of ancestor epithets.
 */
public record InsectGenus(
        InsectGenusName name,
        InsectFamilyName familyName,
        TaxonomicGenus genus,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult
) implements NamedEntity<InsectGenusName>, LinnaeanGenus<InsectFamilyName> {

    public InsectGenus withPlacedIn(@Nullable Clade value) {
        return new InsectGenus(name, familyName, genus,
                description, commonNames, value, egg, larva, pupa, adult);
    }

    public InsectGenus withEgg(@Nullable EggStage value) {
        return new InsectGenus(name, familyName, genus,
                description, commonNames, placedIn, value, larva, pupa, adult);
    }

    public InsectGenus withLarva(@Nullable LarvaStage value) {
        return new InsectGenus(name, familyName, genus,
                description, commonNames, placedIn, egg, value, pupa, adult);
    }

    public InsectGenus withPupa(@Nullable PupaStage value) {
        return new InsectGenus(name, familyName, genus,
                description, commonNames, placedIn, egg, larva, value, adult);
    }

    public InsectGenus withAdult(@Nullable AdultStage value) {
        return new InsectGenus(name, familyName, genus,
                description, commonNames, placedIn, egg, larva, pupa, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(familyName, "familyName")
                .namedValue(genus, "genus")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames")
                .namedEntityOrNull(egg, "egg")
                .namedEntityOrNull(larva, "larva")
                .namedEntityOrNull(pupa, "pupa")
                .namedEntityOrNull(adult, "adult");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java
git commit -m "InsectGenus: drop TaxonomicOrder and TaxonomicFamily local copies

Aligns with the InsectSpecies FK pattern — each rank carries only typed
EntityName FKs to ancestors and its own Taxonomic epithet. When the
family epithet or order is needed for display, consumers resolve the
parent InsectFamily entity."
```

---

### Task 3: Update InsectGenusTest

**Files:**
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectGenusTest.java`

- [ ] **Step 1: Remove `TaxonomicOrder` and `TaxonomicFamily` from all constructor calls**

Replace the entire file contents with:

```java
package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.StageHabitat;
import com.naturalist.insects.lifestage.StagePhenology;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.junit.jupiter.api.Test;

import java.time.MonthDay;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectGenusTest {
    private static final Observer observer = Observer.forClass(InsectGenusTest.class);

    @Test
    void genusWithLarvaAtGenusRankIsValidAndCarriesCompositeSlug() {
        var mo = observer.forMethod("genusWithLarvaAtGenusRankIsValidAndCarriesCompositeSlug");
        InsectGenusName name = InsectGenusName.of("chrysoperla");
        LarvaStage larva = new LarvaStage(
                LifeStageName.of(name, LifeStageKind.LARVA),
                phenology(),
                habitat(),
                null,
                description(),
                null,
                List.of(),
                List.of(),
                null,
                null);

        InsectGenus genus = new InsectGenus(
                name,
                InsectFamilyName.of("chrysopidae"),
                TaxonomicGenus.of("Chrysoperla"),
                description(),
                Set.of(),
                null,
                null,
                larva,
                null,
                null);

        InvariantObservation result = mo.namedEntity(genus, "genus");

        assertThat(result.violations()).isEmpty();
        assertThat(genus.larva().name().value()).isEqualTo("chrysoperla-larva");
        assertThat(genus.egg()).isNull();
        assertThat(genus.pupa()).isNull();
        assertThat(genus.adult()).isNull();
    }

    @Test
    void withLarvaReturnsNewInstanceCarryingTheStage() {
        InsectGenusName name = InsectGenusName.of("chrysoperla");
        InsectGenus genus = new InsectGenus(
                name,
                InsectFamilyName.of("chrysopidae"),
                TaxonomicGenus.of("Chrysoperla"),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
        LarvaStage larva = new LarvaStage(
                LifeStageName.of(name, LifeStageKind.LARVA),
                phenology(),
                habitat(),
                null,
                description(),
                null,
                List.of(),
                List.of(),
                null,
                null);

        InsectGenus updated = genus.withLarva(larva);

        assertThat(updated.larva()).isEqualTo(larva);
        assertThat(genus.larva()).isNull();
    }

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectGenus genus = genusWithPlacedIn(null);

        InsectGenus updated = genus.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(genus.placedIn()).isNull();
    }

    @Test
    void withLarvaPreservesPlacedIn() {
        InsectGenus genus = genusWithPlacedIn(new Papilionidae());

        InsectGenus updated = genus.withLarva(null);

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
    }

    private static InsectGenus genusWithPlacedIn(Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn,
                null,
                null,
                null,
                null);
    }

    private static StagePhenology phenology() {
        return new StagePhenology(
                List.of(new StagePhenology.ActivityWindow(
                        MonthDay.of(4, 1), MonthDay.of(6, 15), MonthDay.of(10, 15), null)),
                null);
    }

    private static StageHabitat habitat() {
        return new StageHabitat(
                new HabitatProfile(
                        Set.of(HabitatZone.CULTIVATED),
                        MoistureRegime.MESIC,
                        LightRegime.PARTIAL_SUN,
                        null),
                null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectGenusTest.java
git commit -m "InsectGenusTest: align with refactored InsectGenus record"
```

---

### Task 4: Update remaining test callers that construct InsectGenus

**Files:**
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectAggregateTest.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java`

- [ ] **Step 1: Update `InsectAggregateTest.validGenus()` (around line 98)**

Replace:
```java
    private static InsectGenus validGenus() {
        return new InsectGenus(
                InsectGenusName.of(RandomValue.string()),
                InsectFamilyName.of("papilionidae"),
                TaxonomicOrder.of("Lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null,
                null, null, null, null);
    }
```

With:
```java
    private static InsectGenus validGenus() {
        return new InsectGenus(
                InsectGenusName.of(RandomValue.string()),
                InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null,
                null, null, null, null);
    }
```

Remove unused `TaxonomicOrder` and `TaxonomicFamily` imports from the file.

- [ ] **Step 2: Update `InsectLifeStagesTest.genusWithPlacedIn()` (around line 89)**

Replace:
```java
    private static InsectGenus genusWithPlacedIn(Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                TaxonomicOrder.of("Lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn,
                null, null, null, null);
    }
```

With:
```java
    private static InsectGenus genusWithPlacedIn(Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn,
                null, null, null, null);
    }
```

Remove unused `TaxonomicOrder` and `TaxonomicFamily` imports from the file.

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectAggregateTest.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java
git commit -m "Update InsectAggregateTest and InsectLifeStagesTest for refactored InsectGenus"
```

---

### Task 5: Update repository contract test

**Files:**
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java`

- [ ] **Step 1: Remove `TaxonomicOrder` and `TaxonomicFamily` from `newEntity()`, `ghostEntity()`, and `modifiedEntity()`**

In `GenusRepositoryTest.java`, update the three entity construction methods. Replace the imports and methods:

Remove these imports:
```java
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicOrder;
```

Replace `newEntity()`:
```java
    @Override
    default InsectGenus newEntity() {
        return new InsectGenus(
                InsectGenusName.of("test-genus-xx"),
                InsectFamilyName.of("tachinidae"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
    }
```

Replace `ghostEntity()`:
```java
    @Override
    default InsectGenus ghostEntity() {
        return new InsectGenus(
                InsectGenusName.of("test-ghost-xx"),
                InsectFamilyName.of("test-ghost-family-xx"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
    }
```

Replace `modifiedEntity()`:
```java
    @Override
    default InsectGenus modifiedEntity(InsectGenus original) {
        return new InsectGenus(
                original.name(),
                InsectFamilyName.of("braconidae"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
                null,
                null,
                null,
                null,
                null);
    }
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java
git commit -m "GenusRepositoryTest: align entity builders with refactored InsectGenus"
```

---

### Task 6: Migrate insect-genera.json — remove order and family fields

**Files:**
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`

- [ ] **Step 1: Remove `"order"` and `"family"` fields from all 10 genus records**

Remove these two lines from each of the 10 records (the field names and values
differ per record but the pattern is the same):

| Record       | Lines to remove                                          |
|-------------|----------------------------------------------------------|
| halictus     | `"order": "Hymenoptera",` and `"family": "Halictidae",` |
| andrena      | `"order": "Hymenoptera",` and `"family": "Andrenidae",` |
| chrysoperla  | `"order": "Neuroptera",` and `"family": "Chrysopidae",` |
| empoasca     | `"order": "Hemiptera",` and `"family": "Cicadellidae",` |
| hippodamia   | `"order": "Coleoptera",` and `"family": "Coccinellidae",` |
| blattella    | `"order": "Blattodea",` and `"family": "Ectobiidae",`   |
| xylocopa     | `"order": "Hymenoptera",` and `"family": "Apidae",`     |
| vanessa      | `"order": "Lepidoptera",` and `"family": "Nymphalidae",` |
| battus       | `"order": "Lepidoptera",` and `"family": "Papilionidae",` |
| colias       | `"order": "Lepidoptera",` and `"family": "Pieridae",`   |

After migration, each record's top-level fields should be:
`name`, `familyName`, `genus`, `description`, `commonNames`, and optionally `placedIn`.

Example — halictus before:
```json
{
  "name": "halictus",
  "familyName": "halictidae",
  "order": "Hymenoptera",
  "family": "Halictidae",
  "genus": "Halictus",
  "description": { ... },
  ...
}
```

After:
```json
{
  "name": "halictus",
  "familyName": "halictidae",
  "genus": "Halictus",
  "description": { ... },
  ...
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json
git commit -m "insect-genera.json: remove order and family local copies from all 10 records"
```

---

### Task 7: Update console — genus detail page resolves parent family

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (lines 123-143)
- Modify: `domains/insects/insects-console/src/main/jte/insects/genus.jte`

- [ ] **Step 1: Update genusDetail controller to resolve and pass the parent family**

In `InsectsController.java`, replace the `genusDetail` method (lines 123-143) with:

```java
    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, Model model) {
        var genusName = InsectGenusName.of(name);
        var genus = insectQuery.genera().getByName(genusName);
        if (genus.isEmpty()) {
            return "redirect:/insects/genera";
        }
        InsectFamily family = insectQuery.families().getByName(genus.get().familyName()).orElseThrow();
        var description = genus.get().description();
        var members = insectQuery.species()
                .forGenusName(genusName)
                .stream()
                .sorted(Comparator.comparing(s -> s.name().value()))
                .toList();
        model.addAttribute("genus", genus.get());
        model.addAttribute("family", family);
        model.addAttribute("species", members);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/genus";
    }
```

The key change: resolve `InsectFamily` via `genus.get().familyName()` and add it
to the model as `"family"`.

- [ ] **Step 2: Update genus.jte to read order and family from the resolved parent**

Replace the full contents of `genus.jte` with:

```jte
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectSpecies
@import java.util.List

@param InsectGenus genus
@param InsectFamily family
@param List<InsectSpecies> species
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = genus.genus().value(), content = @`
    @template.insects.nav(active = "genus")
    <h1><em>${genus.genus().value()}</em></h1>
    <p class="taxonomy">
        ${family.order().value()} &middot;
        <a href="/insects/families/${genus.familyName().value()}">${family.family().value()}</a>
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
                            <dt>species</dt>
                            <dd><em>${s.epithet().value()}</em></dd>
                        </dl>
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
```

Key changes:
- Added `@import com.naturalist.insects.InsectFamily`
- Added `@param InsectFamily family`
- Line 18: `${family.order().value()}` instead of `${genus.order().value()}`
- Line 19: `${family.family().value()}` instead of `${genus.family().value()}`

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/genus.jte
git commit -m "Genus detail page: resolve parent family for order/family display

The genus record no longer carries local copies of the order and family
epithets. The controller resolves the parent InsectFamily entity and
passes it to the template."
```

---

### Task 8: Update console — genera listing page resolves family names

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (lines 115-121)
- Modify: `domains/insects/insects-console/src/main/jte/insects/genera.jte`

- [ ] **Step 1: Update genera controller to build a family lookup map**

In `InsectsController.java`, replace the `genera` method (lines 115-121) with:

```java
    @GetMapping("/genera")
    String genera(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectGenus> genusPage = insectQuery.genera()
                .findPage(PageRequest.console(Math.max(0, page)));
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        for (var genus : genusPage.content()) {
            familyByName.computeIfAbsent(genus.familyName(),
                    n -> insectQuery.families().getByName(n).orElseThrow());
        }
        model.addAttribute("genusPage", genusPage);
        model.addAttribute("familyByName", familyByName);
        return "insects/genera";
    }
```

- [ ] **Step 2: Update genera.jte to read family epithet from the resolved family map**

Replace the full contents of `genera.jte` with:

```jte
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectFamilyName
@import com.naturalist.insects.InsectGenus
@import com.naturalist.data.Page
@import java.util.Map

@param Page<InsectGenus> genusPage
@param Map<InsectFamilyName, InsectFamily> familyByName

@template.layout.page(title = "Insect Genera", content = @`
    @template.insects.nav(active = "genus")
    <h1>Insect Genera</h1>
    <p>Genus-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var g : genusPage.content())
            !{var commonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
            !{var family = familyByName.get(g.familyName());}
            <article>
                <header>
                    <a href="/insects/genera/${g.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>family</dt>
                    <dd><a href="/insects/families/${g.familyName().value()}">${family.family().value()}</a></dd>
                    <dt>genus</dt>
                    <dd><em>${g.genus().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = genusPage, baseUrl = "/insects/genera")
`)
```

Key changes:
- Added imports for `InsectFamily`, `InsectFamilyName`, `Map`
- Added `@param Map<InsectFamilyName, InsectFamily> familyByName`
- Added `!{var family = familyByName.get(g.familyName());}` lookup per genus
- Line with family display: `${family.family().value()}` instead of `${g.family().value()}`

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/genera.jte
git commit -m "Genera listing: resolve parent families for family epithet display

Build a family lookup map in the controller and pass it to the template,
replacing the genus record's former local family() copy."
```

---

### Task 9: Update InsectsGeneraTemplateTest

**Files:**
- Modify: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java`

- [ ] **Step 1: Update template test to pass required family data**

The `genera.jte` template now requires a `familyByName` map, and `genus.jte`
requires a `family` param. Replace the entire file contents with:

```java
package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.*;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsGeneraTemplateTest {

    @Test
    void genera_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectGenus> genusPage = new InsectGenusTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        InsectFamilyTestEntitySource familySource = database.getNamed(InsectFamilyTestEntitySource.class);
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        for (var genus : genusPage.content()) {
            familyByName.computeIfAbsent(genus.familyName(),
                    n -> familySource.getByName(n).orElseThrow());
        }
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genera.jte",
                Map.of("genusPage", genusPage,
                       "familyByName", familyByName),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void genus_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectGenus anyGenus = new InsectGenusTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        InsectFamily family = database.getNamed(InsectFamilyTestEntitySource.class)
                .getByName(anyGenus.familyName()).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genus.jte",
                Map.of(
                        "genus", anyGenus,
                        "family", family,
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

Key changes:
- `genera_rendersWithoutError`: builds a `familyByName` map from the family
  source and passes it alongside `genusPage`
- `genus_rendersWithoutError`: resolves the parent `InsectFamily` and passes it
  as `"family"` in the template params

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java
git commit -m "InsectsGeneraTemplateTest: pass resolved family data to templates"
```

---

### Task 10: Verify the build compiles and tests pass

- [ ] **Step 1: Run the build**

The user runs `mvn verify` from the repository root. Expected: all tests pass,
no compilation errors. The key modules to watch:

- `insects-api` — `InsectGenusTest` must pass with the refactored record
- `insects-repository-test` — `GenusRepositoryMockTest` must pass with updated
  entity builders
- `insects-console` — JTE template compilation must succeed with the updated
  template parameters
- `taxonomy` — `LinnaeanGenus` interface change must not break any other
  consumer (currently only `InsectGenus` implements it)

- [ ] **Step 2: Fix any compilation or test failures**

If failures occur, they will be in one of:
1. A caller constructing `InsectGenus` that was missed — search for
   `new InsectGenus(` across the codebase and update
2. A template referencing `genus.order()` or `genus.family()` that was missed —
   search JTE files for these method calls
3. A test referencing `TaxonomicOrder` or `TaxonomicFamily` in genus context

- [ ] **Step 3: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "Fix remaining callers after InsectGenus refactoring"
```
