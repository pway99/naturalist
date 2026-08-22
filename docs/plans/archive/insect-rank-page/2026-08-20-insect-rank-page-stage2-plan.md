# Insect Rank-Page Read Model — Stage 2 (role + children) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fold the last two things the rank handlers still collate — the functional `role` and the child-rank cards — into the `Insect` read model, so each handler consumes `Insect` (+ presentation/viewer concerns) instead of firing its own role/child/gallery queries.

**Architecture:** Two build-green slices. **Task 1** adds `@Nullable InsectFunctionalRole role` and `List<InsectTaxonView> children` to `Insect`, composed by `InsectFactory` (role symmetric with `citations`/`features`; children = the direct sub-taxa as reused `InsectTaxonView` permits carrying their gallery in the permit's `images()` slot). **Task 2** switches the four handlers to `insect.role()` / `insect.children()`, rewrites the child-card sections of `order`/`family`/`genus.jte` to iterate the permits, and deletes the now-dead per-handler queries. Stage 3 (retire the dead `taxonView()` + drop the permits' `features()` slot) is a separate plan. Design: [2026-08-20-insect-rank-page-read-model-design.md](2026-08-20-insect-rank-page-read-model-design.md) §2.2/§3; the children-shape decision (§4) is resolved to **permit-reuse**.

**Tech Stack:** Java 21 records + sealed types, JTE templates, JUnit 5 + AssertJ, the Observer framework, `NaturalistDatabaseExtension` + repository mocks seeded from JSON.

## Global Constraints

- `role` is composed **symmetric with `citations`/`features`**: a `withRole`, an `invariants()` descent, and `empty()` + every `with*` + the canonical constructor carry it. `FunctionalRoleQuery.getByParentName` returns `Optional<InsectFunctionalRole>`, so the factory passes `.orElse(null)`.
- `children` is a **non-null** `List<InsectTaxonView>` (empty by default, never null); species pages have no children (empty list). Each child is a rank View permit built with `ChildView.of(entity, images, FeatureCollection.empty())` — images populated, features empty. Child image semantics preserve today's handlers: `imageQuery.forRankHierarchy(childName)` for genera/families (subtree), `imageQuery.forParentName(childName)` for species (direct).
- Record-arity: `Insect` goes from 8 to 10 components. EVERY `new Insect(...)` / `with*` / `empty()` site updates — grep the whole repo (`grep -rn "new Insect(" --include=*.java`), do not trust this file list ([[feedback_record_arity_ripple]]).
- The child View permit is reused for two roles: rank-chain entries (empty images) and child cards (populated images). Document this on the `children` component.
- Presentation/viewer stays in the controller: rendered descriptions, breadcrumb, clade trail, ancestor intros, collection lens, `observationLookup`. Only role + child *data* move into the read model.
- Out of scope: N+1 in `InsectFeatureQueryImpl`; the `InsectsController:114` relative image path; stage 3.
- Builds: scoped `mvn -pl <module> -am test`; user runs full `mvn verify` at the gate.

---

### Task 1: `Insect` gains `role` + `children`; `InsectFactory` composes them

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java` (~line 65, `new InsectFactory(...)`)
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectFactoryTest.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectTest.java`

**Interfaces:**
- Consumes: `InsectQuery.FunctionalRoleQuery.getByParentName(InsectRankName) -> Optional<InsectFunctionalRole>`; `FamilyQuery.forOrderName -> FamilyCollection`; `GenusQuery.forFamilyName -> GenusCollection`; `SpeciesQuery.forGenusName -> SpeciesCollection`; `ImageQuery.forRankHierarchy/forParentName -> ImageCollection`; `InsectOrderView.of(InsectOrder, ImageCollection, FeatureCollection)` and the `Genus`/`Species`/`Family` equivalents.
- Produces: `Insect.role() -> @Nullable InsectFunctionalRole`; `Insect.children() -> List<InsectTaxonView>` (non-null); `Insect.withRole(...)`, `Insect.withChildren(...)`; `InsectFactory(SpeciesQuery, ImageQuery, GenusQuery, FamilyQuery, OrderQuery, InsectLifeStageQuery, CitationQuery, FeatureQuery, FunctionalRoleQuery)` (roleQuery appended).

- [ ] **Step 1: Write the failing factory tests**

Add to `InsectFactoryTest.java`. Wire a `FunctionalRoleQuery` into the existing setup (it needs the functional-role repository mock — confirm the class name, e.g. `InsectFunctionalRoleRepositoryMock`, and the impl `InsectFunctionalRoleQueryImpl`; both are wired in `InsectsTestContextInternal`). Append `roleQuery` as the final arg to the existing `new InsectFactory(...)`. Then:

```java
@Test
void buildByName_orderName_composesFamilyChildren() {
    Insect insect = factory.buildByName(InsectOrderName.of("lepidoptera")).orElseThrow();
    assertThat(insect.children()).isNotEmpty();
    assertThat(insect.children()).allSatisfy(c ->
            assertThat(c).isInstanceOf(InsectFamilyView.class));
}

@Test
void buildByName_speciesName_hasNoChildren() {
    Insect insect = factory.buildByName(
            TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name).orElseThrow();
    assertThat(insect.children()).isEmpty();
}

@Test
void buildByName_composesRole_whenPresent() {
    // Pick a rank name that has a functional-role record in seed data; assert it is composed.
    // (Confirm a seeded role's parentName from insect-functional-roles.json; if BattusPhilenor
    // has one, assert insect.role() != null and role.parentName() == that species name.)
    Insect insect = factory.buildByName(
            TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name).orElseThrow();
    // If the species has a seeded role:
    assertThat(insect.role()).satisfiesAnyOf(
            r -> assertThat(r).isNull(),
            r -> assertThat(r.parentName()).isEqualTo(
                    TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
}
```

Confirm against `insect-functional-roles.json` which record to pin, and tighten `buildByName_composesRole_whenPresent` to a concrete non-null assertion on a rank that genuinely has a role (do not leave the `satisfiesAnyOf` escape hatch in the final test).

- [ ] **Step 2: Run to verify failure**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=InsectFactoryTest`
Expected: FAIL to compile — `Insect.children()`/`role()` and the 9-arg `InsectFactory` do not exist yet.

- [ ] **Step 3: Add `role` + `children` to `Insect`**

Add two components after `features` (from stage 1): `@Nullable InsectFunctionalRole role` and `List<InsectTaxonView> children`. Update the canonical constructor, `empty()` (pass `null` and `List.of()`), and EVERY `with*` to thread both through; add `withRole` and `withChildren`:

```java
public Insect withRole(@Nullable InsectFunctionalRole role) {
    return new Insect(observations, order, family, genus, species, lifeStages,
            citations, features, role, children);
}

public Insect withChildren(List<InsectTaxonView> children) {
    return new Insect(observations, order, family, genus, species, lifeStages,
            citations, features, role, children);
}
```

`invariants()` gains, after the `features` descent:
```java
.whenNotNull(role, r -> r.entity(role, "role"))   // InsectFunctionalRole is an Entity; use the matching Constraints method
.notNull(children, "children")
```
(If `Constraints` has no `entity(...)`, use the method the other `Entity` fields use; check an existing `Entity` invariant in the codebase.) Document on the `children` component that its permits carry populated `images()` (child galleries), unlike the empty-image rank-chain permits.

⚠️ Grep every `new Insect(` / `Insect.empty()` / `with*` site repo-wide and update to the new arity (Step 6 covers tests).

- [ ] **Step 4: Compose `role` + `children` in `InsectFactory`**

Add `InsectQuery.FunctionalRoleQuery roleQuery` as the final constructor param (null-check in the `observer.arguments` block + field). In each rank branch add `.withRole(roleQuery.getByParentName(name).orElse(null))` after `.withFeatures(...)`, and `.withChildren(...)` from the entry rank's direct children. Add a private helper per child rank; example for the order branch:

```java
case InsectOrderName orderName -> orderQuery.getByName(orderName)
        .map(order -> observe(Insect.empty()
                .withObservations(imageQuery.forParentName(orderName))
                .withOrder(InsectOrderView.of(order))
                .withLifeStages(lifeStageQuery.lifeStages().forParentName(orderName))
                .withCitations(citationQuery.findByRankName(orderName))
                .withFeatures(featureQuery.findByRankName(orderName))
                .withRole(roleQuery.getByParentName(orderName).orElse(null))
                .withChildren(familyChildren(orderName))));
```
```java
private List<InsectTaxonView> familyChildren(InsectOrderName orderName) {
    return familyQuery.forOrderName(orderName).stream()
            .map(f -> (InsectTaxonView) InsectFamilyView.of(
                    f, imageQuery.forRankHierarchy(f.name()), FeatureCollection.empty()))
            .toList();
}
private List<InsectTaxonView> genusChildren(InsectFamilyName familyName) {
    return genusQuery.forFamilyName(familyName).stream()
            .map(g -> (InsectTaxonView) InsectGenusView.of(
                    g, imageQuery.forRankHierarchy(g.name()), FeatureCollection.empty()))
            .toList();
}
private List<InsectTaxonView> speciesChildren(InsectGenusName genusName) {
    return speciesQuery.forGenusName(genusName).stream()
            .map(s -> (InsectTaxonView) InsectSpeciesView.of(
                    s, imageQuery.forParentName(s.name()), FeatureCollection.empty()))
            .toList();
}
```
Genus/family branches call `speciesChildren(genusName)` / `genusChildren(familyName)`; the species branch uses `.withChildren(List.of())`. Add `import ...InsectEntityCollections.FeatureCollection;` if not present. (Confirm the collection stream elements expose `.name()` returning the child's typed name.)

- [ ] **Step 5: Thread `functionalRoleQuery` through `InsectQueryImpl`**

At `new InsectFactory(...)` (~line 65) append `this.functionalRoleQuery` (already a field, line 16/54) as the final arg.

- [ ] **Step 6: Run factory tests + fix `InsectTest` arity**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=InsectFactoryTest`
Expected: PASS. Then update `InsectTest.java`: every direct `new Insect(...)` gains the two trailing args (`null` role, `List.of()` children unless the case tests them); add descent tests:

```java
@Test
void childrenNull_reportsInvariantViolation() {
    Insect insect = Insect.empty().withChildren(null);
    MethodObserver mo = observer.forMethod("childrenNull_reportsInvariantViolation");
    assertThat(mo.observable(insect, "insect").violationNamesRemovingPrefix(mo.observationPoint()))
            .contains(".insect.children");
}
```
Run: `mvn -pl domains/insects/insects-api -am test -Dtest=InsectTest` → PASS.

---

### Task 2: Handlers consume `insect.role()` / `insect.children()`; templates render children from permits

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (the `orderDetail`, `familyDetail`, `genusDetail`, `detail` handlers)
- Modify: `domains/insects/insects-console/src/main/jte/insects/order.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/family.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/genus.jte`

**Interfaces:**
- Consumes: `Insect.children() -> List<InsectTaxonView>`, `Insect.role() -> @Nullable InsectFunctionalRole` (from Task 1). Each child on an order page is an `InsectFamilyView`; family → `InsectGenusView`; genus → `InsectSpeciesView`.

- [ ] **Step 1: Rewrite `order.jte`'s child-card section to iterate the permits**

Replace the two params `@param List<InsectFamily> families` and `@param InsectEntityCollections.ImageGallery gallery = ...` with:
```jte
@param java.util.List<com.naturalist.insects.InsectTaxonView> children = java.util.List.of()
```
Add `@import com.naturalist.insects.InsectFamilyView`. Rewrite the loop (preserving the exact card markup) to narrow each permit and read its entity + images:
```jte
@if(children.isEmpty())
    <p><em>No families catalogued under this order yet.</em></p>
@else
    <div class="entity-grid">
        @for(var child : children)
            @if(child instanceof InsectFamilyView fv)
                !{var f = fv.family();}
                !{var familyCommonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
                <article>
                    <header>
                        <a href="/insects/families/${f.name().value()}">
                            <strong>${familyCommonName}</strong>
                        </a>
                    </header>
                    <dl class="taxonomy">
                        <dt>family</dt>
                        <dd>${f.family().value()}</dd>
                    </dl>
                    !{var childImages = fv.images().stream().toList();}
                    @if(!childImages.isEmpty())
                        @template.insects.cardImages(images = childImages, linkUrl = "/insects/families/" + f.name().value(), alt = familyCommonName)
                    @endif
                </article>
            @endif
        @endfor
    </div>
@endif
```

- [ ] **Step 2: Mirror the rewrite in `family.jte` (children = `InsectGenusView`) and `genus.jte` (children = `InsectSpeciesView`)**

Same transformation: swap the `List<InsectGenus> genera`+`gallery` (resp. `List<InsectSpecies> species`+`gallery`) params for `List<InsectTaxonView> children`, `@import` the `InsectGenusView`/`InsectSpeciesView` permit, narrow with `instanceof`, read `gv.genus()`/`sv.species()` and `gv.images()`/`sv.images()`. Keep every existing link path (`/insects/genera/...`, `/insects/...`) and label exactly as they are today.

- [ ] **Step 3: Collapse the handlers**

In `InsectsController`:
- `orderDetail`/`familyDetail`/`genusDetail`: delete the child-entity query (`families().forOrderName`, `genera().forFamilyName`, `species().forGenusName`), the `imagesBy*` map building, and the `ImageGallery.grouped(...)` call. Replace the `families`/`genera`/`species` + `gallery` model attributes with `model.addAttribute("children", insect.get().children())` (use the `Insect` each handler already resolved).
- `detail` (species): replace `model.addAttribute("role", insectQuery.functionalRoles().getByParentName(speciesName).orElse(null))` with `model.addAttribute("role", i.role())`. Remove the now-unused import/local if any. (Leave the `/guild/{guild}` handler's `functionalRoles().getByGuild(...)` and the species-list handler's role lookup at lines ~492/733 alone — those are different pages, out of scope.)

- [ ] **Step 4: Build the console + run template tests**

Run: `mvn -pl domains/insects/insects-console -am test`
Expected: PASS. Confirm no dangling references to the removed params: `grep -rn "ImageGallery\|@param List<InsectFamily> families\|@param List<InsectGenus> genera\|@param List<InsectSpecies> species" domains/insects/insects-console/src/main/jte` returns only legitimate remaining uses (the `gallery`/`ImageGallery` type may still be used elsewhere — verify each hit). Grep the controller for leftover `imagesBy`/`ImageGallery.grouped` in the three collapsed handlers.

- [ ] **Step 5: Verify each rank page still renders its children (manual smoke via the reviewer's judgment, or an existing template test)**

Confirm the order/family/genus template tests still pass and that a child card renders name + rank + thumbnail from the permit. If a template test constructs the model with the old `families`/`gallery` attributes, update it to pass `children` as a `List<InsectTaxonView>` of the appropriate permits.

---

## Self-Review

**Spec coverage (design §2.2/§3 stage 2):**
- `role` → `Insect` symmetric with citations → Task 1 Steps 3–4. ✓
- `children` as reused `InsectTaxonView` permits with populated images → Task 1 Steps 3–4 (factory helpers preserve `forRankHierarchy`/`forParentName` semantics). ✓
- Handlers collapse to `insect.role()`/`insect.children()` → Task 2 Step 3. ✓
- Templates render children from permits → Task 2 Steps 1–2. ✓
- Species has no children; presentation/viewer stays in controller → Task 1 (species branch `List.of()`), Task 2 (only role/children move). ✓

**Placeholder scan:** The two "confirm against seed JSON / Constraints method / collection element `.name()`" notes are verification instructions with concrete fallbacks, not placeholders. The `buildByName_composesRole_whenPresent` test MUST be tightened to a concrete assertion before commit (Step 1 says so) — not left as `satisfiesAnyOf`.

**Type consistency:** `children` is `List<InsectTaxonView>` in `Insect`, the factory helpers, `InsectTest`, and all three templates (narrowed via `instanceof` to `InsectFamilyView`/`InsectGenusView`/`InsectSpeciesView`). `InsectFactory`'s new 9th param `FunctionalRoleQuery` matches the `InsectQueryImpl` call site and `InsectFactoryTest` wiring. `role` is `@Nullable InsectFunctionalRole` end to end, composed via `.orElse(null)`.

**Note for the executor:** `InsectFactoryTest` and the role assertions need the functional-role repository mock + `InsectFunctionalRoleQueryImpl` wired (see `InsectsTestContextInternal` for the exact class names); the child-listing methods are already on the `familyQuery`/`genusQuery`/`speciesQuery` the factory holds.
