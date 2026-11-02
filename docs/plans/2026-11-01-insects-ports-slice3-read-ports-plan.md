# Insects Ports — Slice 3: Read Ports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every insects page is one api call returning a read model that already carries what the page shows — the viewer's collection state and own photos paired with their observations, ordered life stages, display names and binomials, species and rank list cards — so `InsectsController`'s read handlers only bind, call and render.

**Architecture:** Two new api namespaces carry everything that depends on a viewer or a list layout, so `InsectQuery` and the top-level file tree stay about what insects are. `InsectViews` (the `InsectEntityCollections` pattern) holds the presentation read models — `RankPage`, `Viewer`, `SpeciesCard`, `RankCard`. `InsectViewQuery` is the port that returns them: `rankPage`, `speciesCards`, `collectedBy`, `orderCards`, `familyCards`, `genusCards`. Its implementation, `InsectViewQueryImpl`, depends only on `InsectQuery` — views are derived from the biology, never the reverse — and assembles through a package-private `InsectViewFactory`. `Insect` stays the viewer-free biological read model, and `InsectQuery` gains no method. Display names, binomials and life-stage order become domain accessors on the biology. Templates switch to the accessors.

**Tech Stack:** Java 25 records, `BehavioralCollection`, `Page`/`PageRequest`, JTE, JUnit 5.

**Spec:** `docs/plans/2026-11-01-insects-ports-design.md` (D2, D5, slice 3). Requires slice 2 merged. Revised 2026-11-01 (user decisions): the viewer lens wraps `Insect` (`InsectViews.RankPage`) instead of becoming an `Insect` component; every viewer- or list-shaped read moves off `InsectQuery` onto a new `InsectViewQuery` port; `InsectTaxonView` and `InsectCitationView` stay where they are (they are components of `Insect` — rank-plus-photographs evidence and sources — so moving them into the views namespace would make the biology depend on presentation). This amends design D2's `getByName(name, viewer)` to `InsectViewQuery.rankPage(name, viewer)`. Split as the design allows: Tasks 1–2 (accessors, views namespace + rank page) and Tasks 3–5 (cards + controller) can be reviewed and merged as two PRs if the diff passes 400 lines.

## Global Constraints

- Java edits through Kaname `apply_change` atoms (`envelope: "naturalist"`); hand-edit only after a recorded atom gap. Record-component additions ripple (TestEntitySource method refs, sibling tests, templates): use `find_type`/`find_method` before every arity change.
- ADR-010: query impls observe, dispatch, delegate; composition lives in package-private core factories — `InsectFactory` for the biology, `InsectViewFactory` for the views. Raw `List<T>` never crosses a port; `Page<ReadModel>` and the `*Collection` types do.
- Fan-out must batch: no per-element repository call. Both N+1 gates stay armed.
- Ordering is owned by the domain: a controller never calls `sorted(...)` on domain data.
- **Views namespace.** A read model that exists for a viewer or a list layout (viewer state, page wrappers, list cards) is a nested `record` of `InsectViews` implementing `ReadModel` (`ValueObject` for a component), and only `InsectViewQuery` returns one. `InsectQuery` gains no method in this slice. `InsectViewQueryImpl` depends only on `InsectQuery` (never a repository, never `InsectFactory`). `Insect` and the rank/feature/role/life-stage records gain no viewer- or page-shaped component, and no biology type references `InsectViews`.
- Stage, never commit. Done = root `mvn verify` + `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` green.

## Review Focus

1. A viewer with no observations of a species must see `collected == false` and an empty own-images set, while the public gallery still shows every image — pinned in Task 2 `rankPage_viewerWithoutObservations`.
2. An anonymous request (no viewer) must produce the identical page as today — pinned in Task 2 `rankPage_noViewer_carriesTheSameInsectAsGetByName` and Task 5's unchanged `InsectsDetailTemplateTest` assertions.
3. Life stages of a species with no resolvable metaboly must still render, in enum order — pinned in Task 1 `orderedBy_unknownKindsFallBackToEnumOrder`.
4. A species with no common name must display a stable name, never an empty heading — pinned in Task 1 `displayName_fallsBackToScientificName`.
5. The collection lens must page over the collection, so a naturalist with more collected species than a page shows gets a second page, never an empty first page — pinned in Task 3 `collectedBy_pagesOverTheCollection`.
---

### Task 1: Domain accessors — `displayName()`, `Insect.binomial()`, `LifeStageCollection.orderedBy`, `FunctionalRoleCollection.sortedByParentName`

**Files:**
- Modify: `domains/insects/insects-api/.../InsectOrder.java`, `InsectFamily.java`, `InsectGenus.java`, `InsectSpecies.java` — add `public String displayName()`.
- Modify: `domains/insects/insects-api/.../Insect.java` — add `public String binomial()`.
- Modify: `domains/insects/insects-api/.../lifestage/InsectLifeStageEntityCollections.java` — `LifeStageCollection.orderedBy(List<LifeStageKind> sequence)`.
- Modify: `domains/insects/insects-api/.../InsectEntityCollections.java` — `FunctionalRoleCollection.sortedByParentName()`.
- Tests: the existing api record tests (`InsectSpeciesTest` etc.) gain the cases below; `LifeStageCollectionTest` and `InsectEntityCollectionsTest` (create if absent, in `insects-api/src/test`).

**Interfaces (produces):**
- `displayName()`: the common name whose `label()` sorts first case-insensitively (deterministic over the `Set`), else the scientific name: `order().value()` / `family().value()` / `genus().value()`; for `InsectSpecies`, else `name().value()` (the record has no genus epithet).
- `Insect.binomial()`: `genus.genus().genus().value() + " " + species.species().epithet().value()` when both ranks are present; `species.species().name().value()` when only the species is; `""` when no species.
- `LifeStageCollection orderedBy(List<LifeStageKind> sequence)`: stages whose kind is in `sequence` first, in that order; the rest after, by `kind().ordinal()`; returns a new collection.
- `FunctionalRoleCollection sortedByParentName()`: by `parentName().value()`.

- [ ] **Step 1: Write the failing tests**
```java
// InsectSpeciesTest (and the three rank tests, same shape)
@Test void displayName_prefersAlphabeticallyFirstCommonName()   // labels {"Zebra Swallowtail","Pipevine Swallowtail"} → "Pipevine Swallowtail"
@Test void displayName_fallsBackToScientificName()               // Set.of() → "battus-philenor" for species; "papilionidae" for family
// InsectTest (api)
@Test void binomial_joinsGenusAndEpithet()                       // "Battus philenor"
@Test void binomial_isEmptyWithoutSpecies()                      // ""
// LifeStageCollectionTest
@Test void orderedBy_followsTheGivenSequence()                   // [ADULT, EGG, LARVA] with sequence [EGG, LARVA, PUPA, ADULT] → [EGG, LARVA, ADULT]
@Test void orderedBy_unknownKindsFallBackToEnumOrder()           // sequence List.of() → enum order
// InsectEntityCollectionsTest
@Test void sortedByParentName_isAlphabeticalBySlug()
```
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-api test` Expected: compile failures.
- [ ] **Step 3: Implement** (`java_method_add` ×7).
- [ ] **Step 4: Run** same. Expected: PASS. Install: `mvn -q -pl domains/insects/insects-api -am install -DskipTests`.
- [ ] **Step 5: Stage** `git add domains/insects/insects-api`

---

### Task 2: `InsectViews` + `InsectViewQuery` — `rankPage`; ordered life stages in `InsectFactory`

`Insect` gains no component (only Task 1's `binomial()` accessor), so no `new Insect(` ripple. `InsectQuery` is not changed.

**Files:**
- Create: `domains/insects/insects-api/.../InsectViews.java` — `public interface InsectViews`, class javadoc: "Presentation read models: what a page or list card shows a particular viewer, as opposed to what insects are. Assembled from the biology by `InsectViewQuery`; nothing in the biology refers to them." First members:
  ```java
  /** One rank page as a given naturalist sees it: the biology plus what that naturalist has collected of it. */
  record RankPage(Insect insect, Viewer viewer) implements ReadModel {
      public static RankPage anonymous(Insect insect) { return new RankPage(insect, Viewer.anonymous()); }
      // invariants: readModel insect; valueObject viewer
  }

  /** What the page knows about the naturalist looking at it; {@link #anonymous()} when no one is signed in. */
  record Viewer(boolean collected,
                ImageCollection ownImages,
                Map<InsectImageId, OrganismObservation<InsectObservationId, InsectRankName>> ownObservationByImage) implements ValueObject {
      public static Viewer anonymous() { return new Viewer(false, ImageCollection.empty(), Map.of()); }
      // invariants: notNull ownImages, notNull ownObservationByImage; every key must be the id of an image in ownImages (isTrue)
  }
  ```
- Create: `domains/insects/insects-api/.../InsectViewQuery.java` — `public interface InsectViewQuery` (flat; no nested ports — six methods by the end of Task 4), javadoc: "The read port for presentation: every call returns an `InsectViews` model. Biology reads stay on `InsectQuery`." This task declares `Optional<InsectViews.RankPage> rankPage(InsectRankName name, @Nullable NaturalistName viewer);` (javadoc: `viewer` null means anonymous; empty when `InsectQuery.getByName(name)` is).
- Create: `domains/insects/insects-core/.../InsectViewQueryImpl.java` — package-private, `@DomainService` (its one dependency, `InsectQuery`, is a bean; it constructs its factory itself, as `InsectQueryImpl` does — so this is not the factory-backed-query trap), constructor `InsectViewQueryImpl(InsectQuery insectQuery)`; `rankPage` observes `identifier(name)`, `whenNotNull(viewer, entityName)`, delegates to the factory.
- Create: `domains/insects/insects-core/.../InsectViewFactory.java` — package-private, constructor `(InsectQuery insectQuery)`. `Optional<RankPage> buildRankPage(InsectRankName name, @Nullable NaturalistName viewer)`: `insectQuery.getByName(name)`; `viewer == null` → `RankPage.anonymous(insect)`; else `mine = insectQuery.observations().forNaturalistAndSubjects(viewer, Set.of(name))`; `collected = !mine.isEmpty()`; `ownObservationByImage` = for each image in `insect.observations()` with `observationId` in `mine`'s ids → `(image.id(), observation)`; `ownImages = ImageCollection.of(those images)`; `new RankPage(insect, new Viewer(...))`.
- Modify: `domains/insects/insects-core/.../InsectFactory.java` — in `buildByName`, after the rank chain is built: `lifeStages` → `insect.lifeStages().orderedBy(sequence)` where `sequence = InsectLifeStages.stagesOf(species, genus, family, order)` for a species page and `stagesOf(genus|family)` for those ranks (`List.of()` for an order). Biology, so it stays on `getByName`.
- Modify: `InsectsTestContext` and `InsectsTestContextInternal` (every `create` overload) — build `new InsectViewQueryImpl(insectQuery)` and expose `InsectViewQuery insectViewQuery()`.
- Modify: `domains/CLAUDE.md` § API Surface: Namespace Patterns — add two table rows, `| <DomainNoun>Views | interface | public | presentation read models (records implementing ReadModel) |` and `| <DomainNoun>ViewQuery | interface | public | none — flat methods returning <DomainNoun>Views models |`, and one paragraph: "**Views namespace.** A read model that exists for a viewer or a list layout — viewer state, page wrappers, list cards — nests in `<DomainNoun>Views`, and only `<DomainNoun>ViewQuery` returns one, so the domain's file tree and its `<DomainNoun>Query` show only the domain's nature. The view-query impl depends only on `<DomainNoun>Query`: views are derived from the biology, and no biology type refers to a view. Types that are components of the composed biological read model (`InsectTaxonView`, `InsectCitationView`) are biology, not views. Plants adopts this when ported."
- Modify: `docs/adr/ADR-020-namespace-interface-pattern.md` — one-line addendum naming the two namespaces and pointing to `domains/CLAUDE.md`.
- Tests: new `domains/insects/insects-core/src/test/.../InsectViewQueryImplTest.java`; `InsectFactoryTest` (exists) for the ordering; new `InsectViewsTest` in `insects-api/src/test` for the `Viewer` invariant.

**Interfaces:**
- Consumes: Task 1 accessors; `InsectQuery.getByName(InsectRankName)`, `ObservationQuery.forNaturalistAndSubjects(NaturalistName, Set<InsectRankName>)` (existing, batched).
- Produces: `insectViewQuery.rankPage(name, viewer)` → `RankPage`; `page.insect()`, `page.viewer()`; `insect.lifeStages()` ordered; `InsectViewFactory(InsectQuery)` for Tasks 3–4; `context.insectViewQuery()`.

- [ ] **Step 1: Write the failing tests** (fixtures: `battus-philenor` has a seeded catalog image with no observation; check `insect-observations.json` for a naturalist with an image-linked observation, else create one through `insectCommand.photos().add(...)` in the test):
```java
// InsectViewQueryImplTest
@Test void rankPage_noViewer_carriesTheSameInsectAsGetByName()   // rankPage(n, null).insect() equals insectQuery.getByName(n); viewer() equals Viewer.anonymous()
@Test void rankPage_viewerWithoutObservations()                   // collected false; ownImages empty; insect().observations() unchanged
@Test void rankPage_viewerWithPhotoObservation()                  // after photos().add as "flora-mendez": collected true; ownImages has that image; ownObservationByImage maps its id to the observation
@Test void rankPage_unknownName_isEmpty()                         // Optional.empty()
@Test void rankPage_rejectsNullName()                             // InvariantViolationException
// InsectFactoryTest
@Test void buildByName_lifeStagesFollowMetabolySequence()         // species whose clade resolves holometabolous: kinds in EGG, LARVA, PUPA, ADULT order
// InsectViewsTest
@Test void viewer_rejectsObservationKeyedByForeignImage()         // InvariantViolationException
```
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-core test -Dtest=InsectViewQueryImplTest,InsectFactoryTest` Expected: compile failure.
- [ ] **Step 3: Implement** api, impl, factories, contexts (`java_type_add` for the new types).
- [ ] **Step 4: Run** `mvn -q -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-test-context -am test` Expected: PASS, including every pre-existing `Insect` test.
- [ ] **Step 5: Docs** — the `domains/CLAUDE.md` rows + paragraph and the ADR-020 addendum.
- [ ] **Step 6: Stage** `git add domains docs/adr`

---

### Task 3: `SpeciesCard`, `InsectViewQuery.speciesCards` / `collectedBy`

**Files:**
- Modify: `domains/insects/insects-api/.../InsectViews.java` — add nested `SpeciesCard`
  ```java
  /** One species-list card: the species with the labels, role, photos and viewer state the card shows. */
  record SpeciesCard(InsectSpecies species, @Nullable InsectGenus genus, @Nullable InsectFamily family,
                            @Nullable InsectFunctionalRole role, ImageCollection images, boolean collected) implements ReadModel {
      public String displayName()   // species.displayName()
      public String binomial()      // genus != null ? genus.genus().value() + " " + species.epithet().value() : species.name().value()
      public String familyLabel()   // family != null ? family.family().value() : (genus != null ? genus.familyName().value() : "—")
      public String genusLabel()    // genus != null ? genus.genus().value() : species.genusName().value()
      // invariants: namedEntity species; notNull images; whenNotNull genus/family/role → namedEntity/entity
  }
  ```
- Modify: `InsectViewQuery` — add `Page<InsectViews.SpeciesCard> speciesCards(PageRequest request, @Nullable NaturalistName viewer);` and `Page<InsectViews.SpeciesCard> collectedBy(NaturalistName viewer, PageRequest request);` (javadoc: `collectedBy` pages over the species the viewer has observed, by name; `collected` is true on every card it returns).
- Modify: `InsectViewQueryImpl` — both methods observe and delegate to `InsectViewFactory`.
- Modify: `InsectViewFactory` — add `Page<InsectViews.SpeciesCard> speciesCards(PageRequest request, @Nullable NaturalistName viewer)` and `Page<InsectViews.SpeciesCard> collectedBy(NaturalistName viewer, PageRequest request)`, reading only through `insectQuery`'s ports. `speciesCards`: `species().findPage(request)` → batched `images().forParentNames(names)`, `functionalRoles().getByParentNames(names)`, `genera().findByNameSet(genusNames)`, `families().findByNameSet(familyNames)`, and when `viewer != null` `observations().forNaturalistAndSubjects(viewer, names)` → cards in page order; `new Page<>(cards, page.pageNumber(), page.pageSize(), page.pagesAheadKnown(), page.moreBeyondLookahead())` (copy the paging fields through). `collectedBy`: `observations().forNaturalist(viewer)` → distinct species-rank subjects (`instanceof InsectSpeciesName`) → `species().findByNameSet(set)` → sorted by `name().value()` → slice `[offset, offset+pageSize)` → same card assembly with `collected = true`; page fields: `pagesAheadKnown = min(lookahead, ceil(remaining/pageSize))`, `moreBeyondLookahead = remaining > lookahead * pageSize` (mirror `PageRequest`'s contract as `InsectSpeciesEntityRepositoryRdbms.doGetPage` does). If a port method named here is not on the public `InsectQuery` surface, stop and report — do not reach past the port.
- Tests: `InsectViewQueryImplTest`.

**Interfaces:**
- Consumes: Task 1 (`displayName`), Task 2 (`InsectViewFactory`, `InsectViewQueryImpl`).
- Produces: `insectViewQuery.speciesCards(PageRequest, viewer)`, `insectViewQuery.collectedBy(viewer, PageRequest)`, `InsectViews.SpeciesCard`.

- [ ] **Step 1: Write the failing tests**
```java
@Test void speciesCards_firstPage_carriesLabelsRoleAndImages()     // battus-philenor card: familyLabel "Papilionidae"-style value from fixture, genusLabel, role non-null if seeded, images non-empty, collected false (no viewer)
@Test void speciesCards_withViewer_marksCollected()         // after sightings().record as "flora-mendez" → that card collected true, others false
@Test void collectedBy_returnsOnlyObservedSpecies()         // record two species → exactly those two cards, both collected, sorted by name
@Test void collectedBy_pagesOverTheCollection()             // record 3 species, PageRequest size 2 → page 0 has 2, page 1 has 1 (use PageRequest.of / the constructor the kernel exposes)
@Test void speciesCards_rejectsNullRequest()                 // InvariantViolationException
```
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-core test -Dtest=InsectViewQueryImplTest` Expected: compile failure.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the three insects modules' tests (as Task 2 Step 4). Expected: PASS; the runtime N+1 gate stays quiet (five batched calls per page).
- [ ] **Step 5: Stage** `git add domains/insects`

---

### Task 4: `RankCard`, `InsectViewQuery.orderCards` / `familyCards` / `genusCards`

**Files:**
- Modify: `domains/insects/insects-api/.../InsectViews.java` — add nested `RankCard`
  ```java
  /** One rank-list card (order / family / genus): names, labels and the rank's subtree gallery. */
  record RankCard(InsectRankName name, String displayName, String scientificName,
                         @Nullable InsectRankName parentName, @Nullable String parentLabel,
                         ImageCollection images) implements ReadModel { /* invariants: identifier name; notBlank displayName, scientificName; notNull images */ }
  ```
- Modify: `InsectViewQuery` — add `Page<InsectViews.RankCard> orderCards(PageRequest request);`, `familyCards(PageRequest)`, `genusCards(PageRequest)`.
- Modify: `InsectViewQueryImpl` — observe and delegate to `InsectViewFactory.orderCards / familyCards / genusCards(PageRequest)`.
- Modify: `InsectViewFactory` — the three methods, through `insectQuery`'s ports: `orders()/families()/genera().findPage` → `images().forRankHierarchies(roots)` (existing grouped gallery) → parent labels via one `findByNameSet` (`orders()` for families, `families()` for genera; none for orders) → cards in page order with `displayName()` from Task 1 and `scientificName` = `order()/family()/genus().value()`; page fields copied through.

- [ ] **Step 1: Write the failing tests** (`InsectViewQueryImplTest`): `familyCards_carryOrderLabelAndSubtreeImages()`, `genusCards_carryFamilyLabel()`, `orderCards_haveNoParent()`.
- [ ] **Step 2: Run** → compile failure. **Step 3: Implement.** **Step 4: Run** the insects modules → PASS. **Step 5: Stage** `git add domains/insects`.
- [ ] **Step 6: Install** `mvn -q -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-test-context -am install -DskipTests`.

---

### Task 5: Controller read handlers and templates

**Files:**
- Modify: `domains/insects/insects-console/.../InsectsController.java` — constructor gains `InsectViewQuery insectViewQuery` (seven args); update `InsectsControllerTestConfig` and every `@WebMvcTest` slice that supplies the controller's beans (the app picks the impl up by scan).
  - `list` (`:511-571`) → `viewer = currentNaturalist(request)`; `mine = collectionLensOn(request) && viewer.isPresent()`; `Page<InsectViews.SpeciesCard> cards = mine ? insectViewQuery.collectedBy(viewer.get(), PageRequest.console(page)) : insectViewQuery.speciesCards(PageRequest.console(page), viewer.orElse(null))`; model: `cards`, `mine`, plus the existing `ancestorIntros`/`breadcrumb`/`cladeTrail`. Delete the five local joins.
  - `families` / `genera` / `orders` → `insectViewQuery.familyCards(PageRequest.console(page))` etc.; model attribute `cards`; delete `orderByName`/`familyByName`/`gallery` joins.
  - `detail` (`:754-810`) → `insectViewQuery.rankPage(speciesName, viewer.orElse(null))`; model: `page` (the `RankPage`), `lens`, plus the description renderings, breadcrumb/trail/intros as today. Delete the stages sort, the lens image filter, `myObservations`, `collected` derivation and the `observationLookup` call.
  - `familyDetail` / `genusDetail` / `orderDetail` → `insectViewQuery.rankPage(name, viewer.orElse(null))`; drop the `forParentName` fetch and `observationLookup`; pass `page`.
  - `lifeStages` (`:944-966`) → `insectQuery.getByName(speciesName)`; `stages = insect.lifeStages()` (already ordered); ranks from the read model.
  - `guild` (`:734-752`) → `insectQuery.functionalRoles().getByGuild(selected).sortedByParentName()`.
  - Delete `observationLookup` (`:199-224`).
- Modify templates (`domains/insects/insects-console/src/main/jte/insects/`):
  - `list.jte` → `@param Page<InsectViews.SpeciesCard> cards`; loop `cards.content()`; `card.displayName()`, `card.familyLabel()`, `card.genusLabel()`, `card.species().epithet().value()`, `card.role()`, `card.images()`, `card.collected()` (render the existing collected badge style on the card); pager over `cards`.
  - `families.jte` / `genera.jte` / `orders.jte` → `@param Page<InsectViews.RankCard> cards`; `card.displayName()`, `card.scientificName()`, `card.parentLabel()`, `card.images()`; links from `card.name().value()`.
  - `detail.jte` → `@param InsectViews.RankPage page` replaces `species/genus/family/order/images/stages/role/citations/collected/observations/features`; `insect = page.insect()`; `commonName = insect.species().species().displayName()`, `binomial = insect.binomial()`; gallery `images = lens ? page.viewer().ownImages().stream().toList() : insect.observations().stream().toList()`, `observations = page.viewer().ownObservationByImage()`; `collected = page.viewer().collected()`; `stages = insect.lifeStages()`.
  - `family.jte` / `genus.jte` / `order.jte` → `@param InsectViews.RankPage page`; `displayName()`; gallery from `page.insect().observations()` + `page.viewer().ownObservationByImage()`.
  - `life-stages.jte` → `species.displayName()`; stages as given (keep the ordinal badge: `${String.valueOf(i + 1)}` from the loop index, not `kind().ordinal()`).
  - `observationGallery.jte` → unchanged signature (`List` + `Map`), callers pass the read-model values.
- Modify tests in `insects-console/src/test`: `InsectsListTemplateTest`, `InsectsDetailTemplateTest`, `InsectsFamiliesTemplateTest`, `InsectsGeneraTemplateTest`, `InsectsOrdersTemplateTest`, `MyCollectionWebMvcTest`, `CollectedIndicatorWebMvcTest`, `GalleryLensWebMvcTest`, `InsectsGuildTemplateTest` — template-level tests that construct model attributes directly must build `SpeciesCard`/`RankCard`/`RankPage` instead of the old maps; WebMvc tests keep their assertions (same rendered text).

**Interfaces:** consumes Tasks 1–4.

- [ ] **Step 1: Add one failing WebMvc test** to `MyCollectionWebMvcTest`: `mine_withManyCollectedSpecies_pagesInsteadOfEmptying()` — record more species than a console page as one naturalist (via `POST …/observe`), then `GET /insects/species?page=1` with the lens on → 200 and at least one species card. (Pins Review Focus 5 end-to-end.)
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-console test -Dtest=MyCollectionWebMvcTest` Expected: FAIL (page 1 empty today).
- [ ] **Step 3: Implement** controller, templates, tests.
- [ ] **Step 4: Run** `mvn -q -pl domains/insects/insects-console test` Expected: PASS.
- [ ] **Step 5: Confirm the controller is delegation-only and the views stayed in their namespace** — the only new top-level files in `insects-api/src/main/java/com/naturalist/insects/` are `InsectViews.java` and `InsectViewQuery.java`, and `git diff main -- domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java` is empty. Then `file_outline` on `InsectsController`: no `sorted`, `filter`, `HashMap`, `observationLookup`; the only remaining logic members are the clade/placement family (`InsectCladeAnchors` users, `*PlacedAt`, `cladePrefix`, `cladeTaxonomicPrefix`, `lineageTo*`, `breadcrumbTo*`, `introsFor*`) and `safeReturnPath`. List them in the report.
- [ ] **Step 6: Stage** `git add domains/insects/insects-console`

---

### Task 6: Verify, gate, hand-off

- [ ] **Step 1: Full verify** — `mvn verify` Expected: BUILD SUCCESS.
- [ ] **Step 2: Enforcement gate** — `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` Expected: BUILD SUCCESS.
- [ ] **Step 3: Run the console** (`mvn install -DskipTests`, start the app) and click through `/insects/species` (lens on and off), a species, a family, a genus, an order, `/insects/{name}/life-stages`, a guild. Report anything that rendered differently from before.
- [ ] **Step 4: Stage and stop for review** — `git status`. The user commits; the next effort (clade/placement) starts from this controller.
