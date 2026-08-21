# `Plant` Read Model — Chunk 3: Children Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fold each rank's direct sub-taxa into the `Plant` read model as `plant.children()` — a `List<PlantTaxonView>` of the child-rank view permits — composed by `PlantFactory`, and switch the three rank templates + handlers to render child cards from those permits instead of the controller's hand-collated `families`/`genera`/`species` lists.

**Architecture:** `Plant` gains a sixth component `List<PlantTaxonView> children` (non-null, empty for species). `PlantFactory` fills it per rank from the child queries it already holds (`familyQuery.forOrderName`, `genusQuery.forFamilyName`, `speciesQuery.forGenusName`), each child wrapped as its view permit with **empty images**. The three rank templates (`orders`/`families`/`genera` detail) iterate `children`, narrow each permit with `instanceof`, and render the same card markup from the wrapped entity. The three handlers drop their child queries and pass `plant.children()`.

**Tech Stack:** Java 21 records + sealed types, JTE templates, JUnit 5 + AssertJ, the Observer framework, `NaturalistDatabase` + repository mocks seeded from JSON. Build: scoped `mvn -pl <module> -am test`; the user runs full `mvn verify`.

**Design of record:** [2026-08-20-plant-read-model-design.md](2026-08-20-plant-read-model-design.md) (Chunk 3 of 6). Prior chunks shipped: `73cc06ed` (skeleton), `57d2e269` (features).

## Global Constraints

- **Insects is the reference; plants moves, insects holds still** (`domains/plants/CLAUDE.md` rule 1). Mirror `Insect.children` (a `List<InsectTaxonView>`) + `InsectFactory`'s `familyChildren`/`genusChildren`/`speciesChildren` helpers + the insects child-card template rewrite (stage-2 pattern), adapted to the four `PlantRankName` permits.
- **Children carry EMPTY galleries — deliberate deviation from the design's "carrying their galleries" phrasing.** The plants child cards render name + taxonomy + description-preview only (no thumbnails — confirmed in the current templates), and plants `ImageQuery` has **no** subtree query (`forRankHierarchy` does not exist — only `forParentName`). Building child thumbnails would be a new feature (a subtree image query + a card-image component) beyond "children". So each child view is built with the 1-arg `PlantFamilyView.of(f)` (empty images). If child thumbnails are wanted later, that is its own slice.
- **`children` is non-null**, empty by default and for species pages. Invariant: `.notNull(children, "children")` (mirror `Insect.invariants()`).
- **Record-arity ripple.** `Plant` grows from 5 to 6 components. The only `new Plant(...)` sites are inside `Plant.java` (`empty()` + five `with*`) — confirmed by `grep -rn "new Plant(" --include='*.java' domains`. Add `import java.util.List;` to `Plant.java`.
- **Child sort order is `v.name().value()`** — matches the current controller sort (`families`/`genera`/`species` are each sorted by `.name().value()` today), so page order is preserved.
- **Behavior-preserving rendering.** The rewritten child-card markup must render byte-equivalent output (same links, labels, taxonomy line, description preview) — only the data source changes (permit-narrowed entity instead of a raw entity list).
- **Out of scope:** the genus page's parent-`family` attribute still fetched via `plantQuery.families().getByName(...)` — that is the ancestor (rank chain), not a child; leave it (a later cleanup can source it from `plant.family()`). Species `detail` handler is untouched (species has no children).
- **Build gotchas (this effort):** `mvn -pl … -am -Dtest=X` needs `-Dsurefire.failIfNoSpecifiedTests=false`.

---

### Task 1: `Plant.children` + `PlantFactory` child composition

**Files:**
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java` (add `List<PlantTaxonView> children` — 6th component)
- Modify: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java` (arity + children tests)
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java` (three child helpers; `.withChildren(...)` per arm)
- Modify: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java` (children assertions)

**Interfaces:**
- Consumes: `PlantTaxonView` + the four view permits (Chunk 1); `FamilyQuery.forOrderName`, `GenusQuery.forFamilyName`, `SpeciesQuery.forGenusName` (all existing, returning collections).
- Produces: `Plant.children() → List<PlantTaxonView>` (non-null); `Plant.withChildren(List<PlantTaxonView>)`.

- [ ] **Step 1: Grep the arity sites.** `grep -rn "new Plant(" --include='*.java' domains` — confirm the only file is `Plant.java`.

- [ ] **Step 2: Write the failing tests.** Append to `PlantTest.java`:

```java
    @Test
    void withChildren_carriesTheList() {
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("asterales"),
                com.naturalist.taxonomy.TaxonomicOrder.of("Asterales"),
                new com.naturalist.fieldnotes.Description("a", "b", "c", "d"),
                java.util.Set.of(), null);
        Plant plant = Plant.empty().withChildren(java.util.List.of(PlantOrderView.of(order)));
        assertThat(plant.children()).hasSize(1);
    }

    @Test
    void emptyPlant_hasEmptyChildren_notNull_andNoViolations() {
        Plant plant = Plant.empty();
        assertThat(plant.children()).isEmpty();
        assertThat(observer.forMethod("emptyChildren").observable(plant, "plant").violations()).isEmpty();
    }

    @Test
    void nullChildren_reportsViolation() {
        var mo = observer.forMethod("nullChildren");
        Plant plant = Plant.empty().withChildren(null);
        assertThat(mo.observable(plant, "plant").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".plant.children");
    }
```

And in `PlantFactoryTest.java` add:

```java
    @Test
    void buildByName_orderName_composesFamilyChildren() {
        Plant plant = factory().buildByName(PlantOrderName.of("asterales")).orElseThrow();
        assertThat(plant.children()).isNotEmpty();
        assertThat(plant.children()).allSatisfy(c ->
                assertThat(c).isInstanceOf(PlantFamilyView.class));
    }

    @Test
    void buildByName_speciesName_hasNoChildren() {
        Plant plant = factory().buildByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.children()).isEmpty();
    }
```

(Confirm `asterales` has ≥1 seeded family — `asteraceae` — in `plant-families.json`; `PlantOrder` ctor arg order + `Description` 4-arg ctor per the Chunk 1/2 tests already in these files.)

- [ ] **Step 3: Run to verify failure.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest,PlantTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL (compile: `withChildren`/`children()` absent).

- [ ] **Step 4: Add `children` to `Plant`.** Add `import java.util.List;`. Add `List<PlantTaxonView> children` as the 6th record component (after `features`). Update `empty()` to pass `List.of()` as the 6th arg; thread `children` through ALL FIVE existing `with*` (each becomes `new Plant(order, family, genus, species, features, children)` with its one component swapped); add:

```java
    public Plant withChildren(List<PlantTaxonView> children) {
        return new Plant(order, family, genus, species, features, children);
    }
```

In `invariants()`, add as the final line (after the `features` block):

```java
                .notNull(children, "children")
```

Document on the `children` component that its permits carry **empty** `images()` (plants render no child thumbnails), unlike a future gallery-bearing variant.

- [ ] **Step 5: Compose children in `PlantFactory`.** Add three private helpers (mirror `InsectFactory`), each wrapping the child entities as empty-image view permits, sorted by name:

```java
    private java.util.List<PlantTaxonView> familyChildren(PlantOrderName orderName) {
        return familyQuery.forOrderName(orderName).stream()
                .map(f -> (PlantTaxonView) PlantFamilyView.of(f))
                .sorted(java.util.Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private java.util.List<PlantTaxonView> genusChildren(PlantFamilyName familyName) {
        return genusQuery.forFamilyName(familyName).stream()
                .map(g -> (PlantTaxonView) PlantGenusView.of(g))
                .sorted(java.util.Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private java.util.List<PlantTaxonView> speciesChildren(PlantGenusName genusName) {
        return speciesQuery.forGenusName(genusName).stream()
                .map(s -> (PlantTaxonView) PlantSpeciesView.of(s))
                .sorted(java.util.Comparator.comparing(v -> v.name().value()))
                .toList();
    }
```

Then add `.withChildren(...)` to each `buildByName` arm (the order/family/genus arms get their child list; species gets an explicit empty):

```java
            case PlantSpeciesName sn -> speciesQuery.getByName(sn).map(s -> observe(
                    resolveGenus(base(sn).withSpecies(PlantSpeciesView.of(s)).withChildren(java.util.List.of()),
                            s.genusName())));
            case PlantGenusName gn -> genusQuery.getByName(gn).map(g -> observe(
                    resolveFamily(base(gn).withGenus(PlantGenusView.of(g)).withChildren(speciesChildren(gn)),
                            g.familyName())));
            case PlantFamilyName fn -> familyQuery.getByName(fn).map(f -> observe(
                    resolveOrder(base(fn).withFamily(PlantFamilyView.of(f)).withChildren(genusChildren(fn)),
                            f.orderName())));
            case PlantOrderName on -> orderQuery.getByName(on).map(o -> observe(
                    base(on).withOrder(PlantOrderView.of(o)).withChildren(familyChildren(on))));
```

(The `resolve*` helpers fill only the ancestor rank-chain views and thread `children` through unchanged via the `with*` — no change needed there. `withChildren` is applied on the entry rank's own Plant before ancestor resolution, so it survives.)

- [ ] **Step 6: Run to verify pass + module green.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest,PlantTest -Dsurefire.failIfNoSpecifiedTests=false` → PASS. Then `mvn -pl domains/plants/plants-core -am test` → green.

- [ ] **Step 7: Stage (do NOT commit).** `git add domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java`.

---

### Task 2: Rewrite the three rank templates to iterate `children` permits (+ update their template tests)

**Files:**
- Modify: `domains/plants/plants-console/src/main/jte/plants/orders/detail.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/families/detail.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/genera/detail.jte`
- Modify: `domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantsOrderDetailTemplateTest.java`
- Modify: `domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantsFamilyDetailTemplateTest.java`
- Modify: `domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantsGenusDetailTemplateTest.java`

**Interfaces:**
- Consumes: `Plant.children()` shape — `List<PlantTaxonView>` narrowed to `PlantFamilyView`/`PlantGenusView`/`PlantSpeciesView` (Task 1 + Chunk 1 permits).

- [ ] **Step 1: Rewrite `orders/detail.jte`'s child section.** Swap the import + param, and rewrite the loop to narrow the permit — preserving the exact card markup:
  - Replace `@import com.naturalist.plants.PlantFamily` with `@import com.naturalist.plants.PlantFamilyView` and add `@import com.naturalist.plants.PlantTaxonView`.
  - Replace `@param List<PlantFamily> families = java.util.List.of()` with `@param List<PlantTaxonView> children = java.util.List.of()`.
  - Rewrite the `<section>` body:

```jte
    <section>
        <h2>Families in this order</h2>
        @if(children.isEmpty())
            <p><em>No families catalogued under this order yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var child : children)
                    @if(child instanceof PlantFamilyView fv)
                        !{var f = fv.family();}
                        !{var familyCommonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.family().value());}
                        <article>
                            <header>
                                <a href="/plants/families/${f.name().value()}">
                                    <strong>${familyCommonName}</strong>
                                </a>
                            </header>
                            <p class="taxonomy"><em>${f.family().value()}</em></p>
                            <p class="description-preview">${f.description().preschool()}</p>
                        </article>
                    @endif
                @endfor
            </div>
        @endif
    </section>
```

- [ ] **Step 2: Mirror the rewrite in `families/detail.jte`** (children = `PlantGenusView`): replace `@import PlantGenus` with `@import PlantGenusView` + add `@import PlantTaxonView`; swap `@param List<PlantGenus> genera` → `@param List<PlantTaxonView> children = java.util.List.of()`; narrow `child instanceof PlantGenusView gv`, `var g = gv.genus();`, keeping the `/plants/genera/${g.name().value()}` link, `${g.genus().value()}` taxonomy, and `${g.description().preschool()}` preview exactly. Keep the `@import PlantFamily` (used by `@param PlantFamily family`).

- [ ] **Step 3: Mirror in `genera/detail.jte`** (children = `PlantSpeciesView`): replace `@import PlantSpecies` with `@import PlantSpeciesView` + add `@import PlantTaxonView`; swap `@param List<PlantSpecies> species` → `@param List<PlantTaxonView> children = java.util.List.of()`; narrow `child instanceof PlantSpeciesView sv`, `var s = sv.species();`. Preserve the existing card exactly, which reads BOTH the child species (`s.epithet()`, `s.name()`, `s.description()`, `s.commonNames()`) AND the parent `genus` param (`genus.genus().value()` in `speciesLabel` and the taxonomy line) — the `@param PlantGenus genus` stays. Keep the `@import PlantFamily`/`PlantGenus` (used by the `family`/`genus` params).

- [ ] **Step 4: Update the three template tests** to pass `children` as a `List<PlantTaxonView>` of permits instead of the raw entity list. For `PlantsOrderDetailTemplateTest.java`, wherever it builds `List<PlantFamily> families` and renders `Map.of("order", order, "families", families)`, wrap to permits and rename the key:

```java
import com.naturalist.plants.PlantFamilyView;
import com.naturalist.plants.PlantTaxonView;
// ...
List<PlantTaxonView> children = families.stream()
        .map(f -> (PlantTaxonView) PlantFamilyView.of(f))
        .toList();
template.render("plants/orders/detail.jte", Map.of("order", order, "children", children), output);
```

The empty-branch test passes `Map.of("order", anyOrder, "children", List.of())`. The `contains("/plants/families/lamiaceae")` / `contains("No families catalogued")` assertions are unchanged — the rewritten template renders the same strings. Apply the analogous change to `PlantsFamilyDetailTemplateTest` (genera → `PlantGenusView`, key `"children"`, assertion link `/plants/genera/...`) and `PlantsGenusDetailTemplateTest` (species → `PlantSpeciesView`, key `"children"`; keep the existing `"genus"`/`"family"` map entries those tests pass).

- [ ] **Step 5: Build + run the console tests.** `mvn -pl domains/plants/plants-console -am test` → green. Then confirm no dangling old-param references: `grep -rn "@param List<PlantFamily> families\|@param List<PlantGenus> genera\|@param List<PlantSpecies> species\|\"families\"\|\"genera\"\|\"species\"" domains/plants/plants-console/src` — the three detail templates and their three tests should no longer reference the old params/keys (other legitimate uses — the `species` LIST page, the `PlantsListTemplateTest`, etc. — may remain; verify each hit).

- [ ] **Step 6: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/jte/plants/orders/detail.jte domains/plants/plants-console/src/main/jte/plants/families/detail.jte domains/plants/plants-console/src/main/jte/plants/genera/detail.jte domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantsOrderDetailTemplateTest.java domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantsFamilyDetailTemplateTest.java domains/plants/plants-console/src/test/java/com/naturalist/plants/console/PlantsGenusDetailTemplateTest.java`.

---

### Task 3: Switch the three handlers to pass `plant.children()`

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java` (`orderDetail`, `familyDetail`, `genusDetail`)

**Interfaces:**
- Consumes: `Plant.children()` (Task 1). Each handler already resolves `plant = plantQuery.getByName(...)` (Chunk 2).

- [ ] **Step 1: `orderDetail`** — delete the `families` query + its `model.addAttribute("families", ...)`, and add `model.addAttribute("children", plant.get().children())`. Result:

```java
    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, Model model) {
        var orderName = PlantOrderName.of(name);
        var plant = plantQuery.getByName(orderName);
        if (plant.isEmpty() || plant.get().order() == null) {
            return "redirect:/plants";
        }
        PlantOrder order = plant.get().order().order();
        model.addAttribute("order", order);
        model.addAttribute("children", plant.get().children());
        model.addAttribute("features", plant.get().features());
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("cladeTrail", cladeTrailFor(order));
        addDescription(model, order.description());
        return "plants/orders/detail";
    }
```

- [ ] **Step 2: `familyDetail`** — delete the `genera` query + `model.addAttribute("genera", ...)`, add `model.addAttribute("children", plant.get().children())`. Keep `family`, `features`, breadcrumb, cladeTrail, description exactly.

- [ ] **Step 3: `genusDetail`** — delete the `species` query + `model.addAttribute("species", ...)`, add `model.addAttribute("children", plant.get().children())`. Keep the parent-`family` attribute (`plantQuery.families().getByName(genus.familyName()).orElse(null)`) as-is, plus `genus`, `features`, breadcrumb, cladeTrail, description.

- [ ] **Step 4: Confirm no leftover child-list collation + `Comparator` still used.** `grep -n "forOrderName\|forFamilyName\|forGenusName\|addAttribute(\"families\"\|addAttribute(\"genera\"\|addAttribute(\"species\"" domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java` — the three rank-detail handlers no longer build child lists. `Comparator` is still imported/used by the species-list handler and cultivar/program/constituent sorts — leave it. (If `Comparator` is now unused, remove the import; verify.)

- [ ] **Step 5: Build.** `mvn -pl domains/plants/plants-console -am test` → green.

- [ ] **Step 6: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java`.

---

## Self-Review

**Spec coverage (design Chunk 3):**
- `Plant.children()` as `List<PlantTaxonView>` composed by `PlantFactory` → Task 1. ✓
- Child cards rendered from permits in the three rank templates → Task 2. ✓
- Handlers pass `plant.children()`, child queries dropped → Task 3. ✓
- Species has no children (empty list) → Task 1 (species arm `withChildren(List.of())`). ✓

**Deliberate deviation (flagged):** children carry EMPTY galleries, not populated ones — plants render no child thumbnails and have no subtree image query; building them is a separate future slice. Recorded in Global Constraints.

**Placeholder scan:** The "confirm seeded family under asterales / ctor arg order" notes are verification instructions with concrete anchors (asterales→asteraceae is in `plant-families.json`), not placeholders. All template/test/factory code blocks are complete.

**Type consistency:** `children` is `List<PlantTaxonView>` in `Plant` (component + `withChildren`), the `PlantFactory` helpers (returning `List<PlantTaxonView>` via `(PlantTaxonView)` casts), the three templates (narrowed via `instanceof` to the rank's permit), and the three template tests (`(PlantTaxonView) PlantXView.of(entity)`). Arity threads through `empty()` + all six `with*`.

**Behavior preservation:** rewritten child cards emit the same links/labels/taxonomy/preview; child sort by `v.name().value()` matches the current controller sort; the empty-state text is unchanged.

**Notes for the executor:** children are keyed off the entry rank (applied before `resolve*` fills ancestors); the genus page's parent-`family` attribute is intentionally left on its own query (ancestor, not child); `Plant.java` needs a new `import java.util.List;`.
