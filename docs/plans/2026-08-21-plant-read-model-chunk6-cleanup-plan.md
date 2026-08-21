# `Plant` Read Model — Chunk 6: Cleanup (final) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retire the dead read-model cruft and the last leftover controller collation now that `Plant` carries the whole graph — remove the unused `FeatureCollection` type, and derive the breadcrumb + clade trail from the resolved `Plant` rank chain instead of re-querying ancestors, collapsing eleven private helpers into two static ones.

**Architecture:** Behavior-preserving. **Task 1** deletes `PlantEntityCollections.FeatureCollection` (zero references since Chunk 2's `findByRankName` replaced `forRankName`). **Task 2** replaces the four `breadcrumbTo*` + four `cladeTrailFor*` + three `orderOf`/`familyOf`/`genusOf` re-query helpers with static `breadcrumbFor(Plant)` + `cladeTrailFor(Plant)` that read `plant.order()/family()/genus()/species()`, switches the four rank handlers, sources the genus page's parent-`family` from `plant.family()`, and adds a characterization test pinning the breadcrumb/clade-trail output.

**Tech Stack:** Java 21, JUnit 5 + AssertJ, `PlantsTestContext`, JTE (unchanged). Build: scoped `mvn -pl <module> -am test`; the user runs full `mvn verify`.

**Design of record:** [2026-08-20-plant-read-model-design.md](2026-08-20-plant-read-model-design.md) (Chunk 6 of 6 — the last). The design's Chunk 1 note explicitly anticipated deriving the breadcrumb "from the resolved Plant's rank chain rather than re-walking parents." Prior chunks: `73cc06ed` skeleton, `57d2e269` features, `fbf36888` children, `25bdf510` role+gallery, `ed2e8c95` species extras.

## Global Constraints

- **Behavior-preserving.** No page output changes — the breadcrumb segments and clade-trail lists must be byte-identical to today's. The only observable effect is a smaller api surface and fewer redundant queries per page (the read model already resolved the ancestry; the breadcrumb stops re-fetching it).
- **`FeatureCollection` is dead** — grep-confirmed: the only reference is its own declaration in `PlantEntityCollections.java`. Removing it must not leave an unused `PlantFeature`/`List`/`Collection` import behind.
- **The breadcrumb/clade-trail helpers become `static`** and read only the `Plant` rank chain (no `plantQuery`). `plantaeRoot()`, `orderLink`/`familyLink`/`genusLink` (already static), `catalogCladeRoot()` (list pages), and `lowestCommonAncestor` STAY. The eleven removed helpers: `breadcrumbToOrder/Family/Genus/Species`, `cladeTrailFor(PlantOrder)`, `cladeTrailForFamily/Genus/Species`, `orderOf`, `familyOf`, `genusOf` — confirm each has no caller outside the set being removed before deleting.
- **Rank-chain accessor path:** `plant.order()` → `PlantOrderView`; `.order()` → `PlantOrder`; `.order()` → `TaxonomicOrder`; `.value()` → String. So the order epithet is `plant.order().order().order().value()`. Family: `plant.family().family().family().value()`. Genus: `plant.genus().genus().genus().value()`. Species epithet: `plant.species().species().epithet().value()`. `placedIn`: `plant.order().order().placedIn()`.
- **Ancestry is monotonic** (the factory resolves the full chain up to order): a species `Plant` has non-null order/family/genus/species; an order `Plant` has only order. A gap leaves the shallower views null — the helpers must null-guard exactly as the old `Optional`-based ones did (a missing ancestor shortens the trail, never NPEs).
- **Build gotcha:** `mvn -pl … -am -Dtest=X` needs `-Dsurefire.failIfNoSpecifiedTests=false`.

---

### Task 1: Delete dead `FeatureCollection`

**Files:**
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantEntityCollections.java`

- [ ] **Step 1: Confirm it is dead.** `grep -rn "FeatureCollection" domains/plants --include='*.java' | grep -v target | grep -v PlantEntityCollections.java` → no output. (If anything appears, STOP — it is not dead; report.)

- [ ] **Step 2: Delete the `FeatureCollection` nested `final class`** from `PlantEntityCollections.java` (lines around 106–118 — the class + its `of`/`empty` factories). If removing it leaves an import used only by it (e.g. a `PlantFeature` import referenced nowhere else in the file), remove that import too. Leave every other collection (`SpeciesCollection`/`OrderCollection`/`FamilyCollection`/`GenusCollection`/`EcologicalRoleCollection`/`ImageCollection`/`ObservationCollection`) untouched.

- [ ] **Step 3: Build + module green.** `mvn -pl domains/plants/plants-api -am test` → green. Then `grep -rn "FeatureCollection" domains/plants --include='*.java' | grep -v target` → no hits at all.

- [ ] **Step 4: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantEntityCollections.java`.

---

### Task 2: Derive breadcrumb + clade trail from `Plant`; retire the re-query helpers

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java`
- Create: `domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantBreadcrumbTest.java`

**Interfaces:**
- Consumes: `Plant.order()/family()/genus()/species()` (rank-chain views); `Plant.identifiedTo()`.
- Produces: `static List<BreadcrumbSegment> breadcrumbFor(Plant)`, `static List<Clade> cladeTrailFor(Plant)` (package-private on `PlantsController`).

- [ ] **Step 1: Write the characterization test** `PlantBreadcrumbTest.java` (build real `Plant`s via `PlantsTestContext`, call the new static helpers, assert exact output). This pins current behavior BEFORE the refactor lands:

```java
package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantOrderName;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.PlantsTestContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantBreadcrumbTest {

    private final PlantsTestContext context = PlantsTestContext.create(NaturalistDatabase.create());

    @Test
    void breadcrumb_forSpecies_isPlantaeThroughAncestorsToCurrent() {
        var plant = context.plantQuery().getByName(
                PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        var crumbs = PlantsController.breadcrumbFor(plant);

        assertThat(crumbs).extracting(BreadcrumbSegment::label).containsExactly(
                "Plantae", "Piperales", "Aristolochiaceae", "Aristolochia", "Aristolochia californica");
        assertThat(crumbs).extracting(BreadcrumbSegment::url).containsExactly(
                "/plants/orders", "/plants/orders/piperales", "/plants/families/aristolochiaceae",
                "/plants/genera/aristolochia", null);   // current segment has no link
        assertThat(crumbs.get(crumbs.size() - 1).currentPage()).isTrue();
    }

    @Test
    void breadcrumb_forOrder_isPlantaeThenCurrentOrder() {
        var plant = context.plantQuery().getByName(PlantOrderName.of("lamiales")).orElseThrow();
        var crumbs = PlantsController.breadcrumbFor(plant);

        assertThat(crumbs).extracting(BreadcrumbSegment::label).containsExactly("Plantae", "Lamiales");
        assertThat(crumbs.get(1).currentPage()).isTrue();
    }

    @Test
    void cladeTrail_forSpecies_walksUpToTheOrdersPlacement() {
        var plant = context.plantQuery().getByName(
                PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        // Same as the order's placedIn ancestry (species resolves via its order).
        assertThat(PlantsController.cladeTrailFor(plant)).isNotEmpty();
    }
}
```

(Confirm the `current` segment's `url` is `null` in `BreadcrumbSegment.current(...)` — check the record's factory; if it uses `""` not `null`, adjust the expected value. Confirm the epithet capitalisation of `TaxonomicOrder`/`TaxonomicFamily`/`TaxonomicGenus`/`TaxonomicSpecies` `.value()` against the seed JSON — `Piperales`/`Aristolochiaceae`/`Aristolochia`/`californica`. Confirm `aristolochia-californica`'s genus is `aristolochia` and order `piperales` in `plant-genera.json`/`plant-families.json`. `BreadcrumbSegment.label()`/`url()`/`currentPage()` are the record accessors.)

- [ ] **Step 2: Run — expect compile failure.** `mvn -pl domains/plants/plants-console -am test -Dtest=PlantBreadcrumbTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL (`breadcrumbFor`/`cladeTrailFor(Plant)` do not exist yet).

- [ ] **Step 3: Add the two static helpers** to `PlantsController` (import `com.naturalist.plants.Plant` if needed):

```java
    /**
     * The taxonomic breadcrumb for a resolved {@link Plant}, read straight off its rank
     * chain — Plantae root, a link per ancestor, the subject rank as the current segment.
     * Replaces the former per-rank re-query helpers now that the read model carries the
     * ancestry. A missing ancestor (null view) shortens the trail rather than failing.
     */
    static List<BreadcrumbSegment> breadcrumbFor(Plant plant) {
        var segments = plantaeRoot();
        boolean speciesSubject = plant.species() != null;
        boolean genusSubject = !speciesSubject && plant.genus() != null;
        boolean familySubject = !speciesSubject && !genusSubject && plant.family() != null;

        boolean orderIsAncestor = plant.family() != null || plant.genus() != null || plant.species() != null;
        if (plant.order() != null && orderIsAncestor) {
            segments.add(orderLink(plant.order().order()));
        }
        if (plant.family() != null && !familySubject) {
            segments.add(familyLink(plant.family().family()));
        }
        if (plant.genus() != null && !genusSubject) {
            segments.add(genusLink(plant.genus().genus()));
        }
        if (speciesSubject) {
            String genusEpithet = plant.genus() != null ? plant.genus().genus().genus().value() + " " : "";
            segments.add(BreadcrumbSegment.current(
                    genusEpithet + plant.species().species().epithet().value(), "Species"));
        } else if (genusSubject) {
            segments.add(BreadcrumbSegment.current(plant.genus().genus().genus().value(), "Genus"));
        } else if (familySubject) {
            segments.add(BreadcrumbSegment.current(plant.family().family().family().value(), "Family"));
        } else if (plant.order() != null) {
            segments.add(BreadcrumbSegment.current(plant.order().order().order().value(), "Order"));
        }
        return segments;
    }

    /**
     * The tree-of-life clade row for a resolved {@link Plant}: the lineage from the plant
     * kingdom down to the order's {@code placedIn} clade. Read off {@code plant.order()}
     * (present for every rank via the resolved ancestry); empty when the order carries no
     * placement. Plant clades are supra-ordinal, so lower ranks resolve through the order.
     */
    static List<Clade> cladeTrailFor(Plant plant) {
        if (plant.order() == null) {
            return List.of();
        }
        Clade placedIn = plant.order().order().placedIn();
        if (placedIn == null) {
            return List.of();
        }
        return CladeTraversal.ancestry(placedIn).reversed();
    }
```

(This reproduces the old logic exactly: `breadcrumbToSpecies` built Plantae → orderLink → familyLink → genusLink → `current(genusEpithet + " " + speciesEpithet, "Species")`; `cladeTrailFor(PlantOrder)` walked `placedIn`. Confirm `orderLink`/`familyLink`/`genusLink` take the entity — `plant.order().order()` is a `PlantOrder`, matching `orderLink(PlantOrder)`.)

- [ ] **Step 4: Switch the four rank handlers.** In `detail` (species), `orderDetail`, `familyDetail`, `genusDetail`, replace `breadcrumbToX(entity)` → `breadcrumbFor(plant.get())` and `cladeTrailForX(entity)` → `cladeTrailFor(plant.get())`. In `genusDetail`, replace the parent-family attribute `plantQuery.families().getByName(genus.familyName()).orElse(null)` with `plant.get().family() == null ? null : plant.get().family().family()`. Leave every other attribute (children, features, role, images, extras, description) unchanged.

- [ ] **Step 5: Delete the eleven dead helpers.** Remove `breadcrumbToOrder`, `breadcrumbToFamily`, `breadcrumbToGenus`, `breadcrumbToSpecies`, `cladeTrailFor(PlantOrder)`, `cladeTrailForFamily`, `cladeTrailForGenus`, `cladeTrailForSpecies`, `orderOf`, `familyOf`, `genusOf`. Then grep to confirm none is still referenced: `grep -n "breadcrumbTo\|cladeTrailForFamily\|cladeTrailForGenus\|cladeTrailForSpecies\|orderOf\|familyOf\|genusOf\|cladeTrailFor(order" PlantsController.java` → only the new `breadcrumbFor`/`cladeTrailFor(Plant)` remain. Remove any import left unused by the deletions (e.g. `java.util.Optional` if nothing else uses it — verify).

- [ ] **Step 6: Run the characterization test + module green.** `mvn -pl domains/plants/plants-console -am test -Dtest=PlantBreadcrumbTest -Dsurefire.failIfNoSpecifiedTests=false` → PASS. Then `mvn -pl domains/plants/plants-console -am test` → green (all 47+ tests, confirming the handlers still compile + render).

- [ ] **Step 7: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantBreadcrumbTest.java`.

---

## Self-Review

**Spec coverage (design Chunk 6):**
- Retire dead direct-rank query surface (`FeatureCollection`) → Task 1. ✓
- Retire leftover controller collation (breadcrumb/clade-trail ancestor re-queries + genus parent-family) → Task 2. ✓
- Behavior-preserving, grep-confirmed no dangling references → Task 1 Step 3, Task 2 Step 5. ✓

**Placeholder scan:** the "confirm `current` url is null / epithet capitalisation / seed chain" notes are verification instructions with concrete anchors (the aristolochia-californica → aristolochia → aristolochiaceae → piperales chain, quoted expected labels). The refactored helper code is complete and reproduces the old logic line-for-line.

**Type consistency:** `breadcrumbFor(Plant) → List<BreadcrumbSegment>` and `cladeTrailFor(Plant) → List<Clade>` are the two new static helpers, called by all four handlers and the characterization test; the rank-chain accessor paths (`plant.order().order().order().value()` etc.) match the view→entity→epithet chain used by the retained `orderLink`/`familyLink`/`genusLink`.

**Behavior preservation:** the new `breadcrumbFor` reproduces `breadcrumbToSpecies`/`Genus`/`Family`/`Order` exactly (same Plantae root, same link sequence, same current-segment label incl. the `"genus epithet"` species label); `cladeTrailFor(Plant)` reproduces `cladeTrailFor(PlantOrder)` sourced from the resolved order. The characterization test asserts the exact segment labels/urls for a species and an order, and a non-empty clade trail — pinning the output.

**Effort complete:** with Chunk 6, `Plant` carries the full taxon graph and the controller reads every rank page entirely off `plantQuery.getByName(...)` — no hand-collation, no per-page ancestor re-queries. The Plant read-model effort is done.

**Notes for the executor:** confirm no caller of the eleven removed helpers survives before deleting; the helpers become `static` (they no longer touch `plantQuery`); `catalogCladeRoot`/`lowestCommonAncestor`/`plantaeRoot`/the three `*Link` helpers stay; the genus parent-`family` now comes from `plant.family()`.
