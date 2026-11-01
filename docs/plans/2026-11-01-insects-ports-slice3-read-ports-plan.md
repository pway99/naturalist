# Insects Ports — Slice 3: Read Ports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every insects page is one api call returning a read model that already carries what the page shows — the viewer's collection state and own photos paired with their observations, ordered life stages, display names and binomials, species and rank list cards — so `InsectsController`'s read handlers only bind, call and render.

**Architecture:** `InsectQuery.getByName` gains an optional viewer and `Insect` gains a `Viewer` lens built by `InsectFactory`. Display names, binomials and life-stage order become domain accessors. Two new read models, `SpeciesCard` and `RankCard`, replace the controller's five-port list-page joins; the "my collection" lens becomes a paged query over the viewer's observations. Templates switch to the accessors.

**Tech Stack:** Java 25 records, `BehavioralCollection`, `Page`/`PageRequest`, JTE, JUnit 5.

**Spec:** `docs/plans/2026-11-01-insects-ports-design.md` (D2, D5, slice 3). Requires slice 2 merged. Split as the design allows: Tasks 1–2 (accessors + viewer lens) and Tasks 3–5 (cards + controller) can be reviewed and merged as two PRs if the diff passes 400 lines.

## Global Constraints

- Java edits through Kaname `apply_change` atoms (`envelope: "naturalist"`); hand-edit only after a recorded atom gap. Record-component additions ripple (TestEntitySource method refs, sibling tests, templates): use `find_type`/`find_method` before every arity change.
- ADR-010: query impls observe, dispatch, delegate; composition lives in `InsectFactory` (package-private, core). Raw `List<T>` never crosses a port; `Page<ReadModel>` and the `*Collection` types do.
- Fan-out must batch: no per-element repository call. Both N+1 gates stay armed.
- Ordering is owned by the domain: a controller never calls `sorted(...)` on domain data.
- Stage, never commit. Done = root `mvn verify` + `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` green.

## Review Focus

1. A viewer with no observations of a species must see `collected == false` and an empty own-images set, while the public gallery still shows every image — pinned in Task 2 `buildByName_viewerWithoutObservations`.
2. An anonymous request (no viewer) must produce the identical page as today — pinned in Task 2 `buildByName_noViewer_equalsOneArgForm` and Task 5's unchanged `InsectsDetailTemplateTest`.
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

### Task 2: Viewer-aware `getByName` and ordered life stages in the factory

**Files:**
- Modify: `domains/insects/insects-api/.../Insect.java` — add component `Viewer viewer` (last) and nested
  ```java
  /** What the read model knows about the naturalist looking at it; {@link #anonymous()} when no one is signed in. */
  public record Viewer(boolean collected,
                       ImageCollection ownImages,
                       Map<InsectImageId, OrganismObservation<InsectObservationId, InsectRankName>> ownObservationByImage) implements ValueObject {
      public static Viewer anonymous() { return new Viewer(false, ImageCollection.empty(), Map.of()); }
      // invariants: notNull ownImages, notNull ownObservationByImage; every key must be the id of an image in ownImages (isTrue)
  }
  ```
  `Insect.empty()` fills `Viewer.anonymous()`; add `withViewer(Viewer)`; `invariants()` descends into `viewer` with `valueObject`. Every `new Insect(...)` site repo-wide gains the argument (find them with `find_method methodName: "<init>", ownerFqn: com.naturalist.insects.Insect`).
- Modify: `domains/insects/insects-api/.../InsectQuery.java:65-72` — add `Optional<Insect> getByName(InsectRankName name, @Nullable NaturalistName viewer);` with javadoc; the one-arg form's javadoc says "equivalent to `getByName(name, null)`".
- Modify: `domains/insects/insects-core/.../InsectQueryImpl.java` — implement the two-arg form (observe `identifier(name)`, `whenNotNull(viewer, entityName)`; delegate to the factory); one-arg delegates with `null`.
- Modify: `domains/insects/insects-core/.../InsectFactory.java` — constructor gains `InsectQuery.ObservationQuery observationQuery` (update its construction site in `InsectQueryImpl` and both test contexts); `buildByName(InsectRankName name)` delegates to new `buildByName(InsectRankName name, @Nullable NaturalistName viewer)`; after the rank chain is built: (a) `lifeStages` → `insect.lifeStages().orderedBy(sequence)` where `sequence = InsectLifeStages.stagesOf(species, genus, family, order)` for a species page and `stagesOf(genus|family)` for those ranks (`List.of()` for an order); (b) when `viewer != null`: `mine = observationQuery.forNaturalistAndSubjects(viewer, Set.of(name))`; `collected = !mine.isEmpty()`; `ownObservationByImage` = for each image in `insect.observations()` with `observationId` in `mine`'s ids → `(image.id(), observation)`; `ownImages = ImageCollection.of(those images)`; `insect = insect.withViewer(new Viewer(...))`.
- Tests: `domains/insects/insects-core/src/test/.../InsectFactoryTest.java` (exists) and `InsectQueryImplTest.java` (exists).

**Interfaces:**
- Consumes: Task 1 accessors; `ObservationQuery.forNaturalistAndSubjects(NaturalistName, Set<InsectRankName>)` (existing, batched).
- Produces: `insectQuery.getByName(name, viewer)`; `insect.viewer()`; `insect.lifeStages()` ordered.

- [ ] **Step 1: Write the failing tests** (fixtures: `battus-philenor` has a seeded catalog image with no observation; `patrick-way` has seeded observations — check `insect-observations.json` for a naturalist with an image-linked observation, else create one through `insectCommand.photos().add(...)` in the test):
```java
@Test void buildByName_noViewer_equalsOneArgForm()                // getByName(n) equals getByName(n, null); viewer() is anonymous()
@Test void buildByName_viewerWithoutObservations()               // collected false; ownImages empty; observations() unchanged
@Test void buildByName_viewerWithPhotoObservation()              // after photos().add as "flora-mendez": collected true; ownImages has that image; ownObservationByImage maps its id to the observation
@Test void buildByName_lifeStagesFollowMetabolySequence()        // species whose clade resolves holometabolous: kinds in EGG, LARVA, PUPA, ADULT order
```
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-core test -Dtest=InsectFactoryTest,InsectQueryImplTest` Expected: compile failure.
- [ ] **Step 3: Implement** api (record component — atom gap, capture, hand-edit; fix every `new Insect(` site), query, factory, contexts.
- [ ] **Step 4: Run** `mvn -q -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-test-context -am test` Expected: PASS, including every pre-existing `Insect` test.
- [ ] **Step 5: Stage** `git add domains/insects`

---

### Task 3: `SpeciesCard`, `SpeciesQuery.cards`, `SpeciesQuery.collectedBy`

**Files:**
- Create: `domains/insects/insects-api/.../SpeciesCard.java`
  ```java
  /** One species-list card: the species with the labels, role, photos and viewer state the card shows. */
  public record SpeciesCard(InsectSpecies species, @Nullable InsectGenus genus, @Nullable InsectFamily family,
                            @Nullable InsectFunctionalRole role, ImageCollection images, boolean collected) implements ReadModel {
      public String displayName()   // species.displayName()
      public String binomial()      // genus != null ? genus.genus().value() + " " + species.epithet().value() : species.name().value()
      public String familyLabel()   // family != null ? family.family().value() : (genus != null ? genus.familyName().value() : "—")
      public String genusLabel()    // genus != null ? genus.genus().value() : species.genusName().value()
      // invariants: namedEntity species; notNull images; whenNotNull genus/family/role → namedEntity/entity
  }
  ```
- Modify: `InsectQuery.SpeciesQuery` — add `Page<SpeciesCard> cards(PageRequest request, @Nullable NaturalistName viewer);` and `Page<SpeciesCard> collectedBy(NaturalistName viewer, PageRequest request);` (javadoc: `collectedBy` pages over the species the viewer has observed, by name; `collected` is true on every card it returns).
- Modify: `domains/insects/insects-core/.../InsectSpeciesQueryImpl.java` — both methods observe and delegate to `InsectFactory` (its constructor already has the species/genus/family/role/image/observation queries; `InsectSpeciesQueryImpl` must receive the factory — follow how `InsectQueryImpl` obtains it, or construct the two-arg call through `InsectQueryImpl` if the factory lives there; keep one factory instance).
- Modify: `InsectFactory` — add `Page<SpeciesCard> speciesCards(PageRequest request, @Nullable NaturalistName viewer)` and `Page<SpeciesCard> collectedBy(NaturalistName viewer, PageRequest request)`. `speciesCards`: `speciesQuery.findPage(request)` → batched `imageQuery.forParentNames(names)`, `roleQuery.getByParentNames(names)`, `genusQuery.findByNameSet(genusNames)`, `familyQuery.findByNameSet(familyNames)`, and when `viewer != null` `observationQuery.forNaturalistAndSubjects(viewer, names)` → cards in page order; `new Page<>(cards, page.pageNumber(), page.pageSize(), page.pagesAheadKnown(), page.moreBeyondLookahead())` (copy the paging fields through). `collectedBy`: `observationQuery.forNaturalist(viewer)` → distinct species-rank subjects (`instanceof InsectSpeciesName`) → `speciesQuery.findByNameSet(set)` → sorted by `name().value()` → slice `[offset, offset+pageSize)` → same card assembly with `collected = true`; page fields: `pagesAheadKnown = min(lookahead, ceil(remaining/pageSize))`, `moreBeyondLookahead = remaining > lookahead * pageSize` (mirror `PageRequest`'s contract as `InsectSpeciesEntityRepositoryRdbms.doGetPage` does).
- Tests: `InsectFactoryTest` (or a new `InsectSpeciesCardsTest` in core).

**Interfaces:**
- Consumes: Task 1 (`displayName`), Task 2 factory shape.
- Produces: `insectQuery.species().cards(PageRequest, viewer)`, `.collectedBy(viewer, PageRequest)`, `SpeciesCard`.

- [ ] **Step 1: Write the failing tests**
```java
@Test void cards_firstPage_carriesLabelsRoleAndImages()     // battus-philenor card: familyLabel "Papilionidae"-style value from fixture, genusLabel, role non-null if seeded, images non-empty, collected false (no viewer)
@Test void cards_withViewer_marksCollected()                // after sightings().record as "flora-mendez" → that card collected true, others false
@Test void collectedBy_returnsOnlyObservedSpecies()         // record two species → exactly those two cards, both collected, sorted by name
@Test void collectedBy_pagesOverTheCollection()             // record 3 species, PageRequest size 2 → page 0 has 2, page 1 has 1 (use PageRequest.of / the constructor the kernel exposes)
@Test void cards_rejectsNullRequest()                       // InvariantViolationException
```
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-core test -Dtest=InsectFactoryTest` Expected: compile failure.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the three insects modules' tests (as Task 2 Step 4). Expected: PASS; the runtime N+1 gate stays quiet (five batched calls per page).
- [ ] **Step 5: Stage** `git add domains/insects`

---

### Task 4: `RankCard` and `cards(PageRequest)` for orders, families, genera

**Files:**
- Create: `domains/insects/insects-api/.../RankCard.java`
  ```java
  /** One rank-list card (order / family / genus): names, labels and the rank's subtree gallery. */
  public record RankCard(InsectRankName name, String displayName, String scientificName,
                         @Nullable InsectRankName parentName, @Nullable String parentLabel,
                         ImageCollection images) implements ReadModel { /* invariants: identifier name; notBlank displayName, scientificName; notNull images */ }
  ```
- Modify: `InsectQuery.OrderQuery`, `FamilyQuery`, `GenusQuery` — each gains `Page<RankCard> cards(PageRequest request);`.
- Modify: the three `*QueryImpl`s — observe and delegate to `InsectFactory.orderCards / familyCards / genusCards(PageRequest)`.
- Modify: `InsectFactory` — the three methods: `findPage` → `imageQuery.forRankHierarchies(roots)` (existing grouped gallery) → parent labels via one `findByNameSet` (`orderQuery` for families, `familyQuery` for genera; none for orders) → cards in page order with `displayName()` from Task 1 and `scientificName` = `order()/family()/genus().value()`; page fields copied through.

- [ ] **Step 1: Write the failing tests** (`InsectFactoryTest`): `familyCards_carryOrderLabelAndSubtreeImages()`, `genusCards_carryFamilyLabel()`, `orderCards_haveNoParent()`.
- [ ] **Step 2: Run** → compile failure. **Step 3: Implement.** **Step 4: Run** the insects modules → PASS. **Step 5: Stage** `git add domains/insects`.
- [ ] **Step 6: Install** `mvn -q -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-test-context -am install -DskipTests`.

---

### Task 5: Controller read handlers and templates

**Files:**
- Modify: `domains/insects/insects-console/.../InsectsController.java`
  - `list` (`:511-571`) → `viewer = currentNaturalist(request)`; `mine = collectionLensOn(request) && viewer.isPresent()`; `Page<SpeciesCard> cards = mine ? insectQuery.species().collectedBy(viewer.get(), PageRequest.console(page)) : insectQuery.species().cards(PageRequest.console(page), viewer.orElse(null))`; model: `cards`, `mine`, plus the existing `ancestorIntros`/`breadcrumb`/`cladeTrail`. Delete the five local joins.
  - `families` / `genera` / `orders` → `insectQuery.families().cards(PageRequest.console(page))` etc.; model attribute `cards`; delete `orderByName`/`familyByName`/`gallery` joins.
  - `detail` (`:754-810`) → `insectQuery.getByName(speciesName, viewer.orElse(null))`; model: `insect` (the whole read model), `lens`, plus the description renderings, breadcrumb/trail/intros as today. Delete the stages sort, the lens image filter, `myObservations`, `collected` derivation and the `observationLookup` call.
  - `familyDetail` / `genusDetail` / `orderDetail` → `getByName(name, viewer.orElse(null))`; drop the `forParentName` fetch and `observationLookup`; pass `insect`.
  - `lifeStages` (`:944-966`) → `insectQuery.getByName(speciesName)`; `stages = insect.lifeStages()` (already ordered); ranks from the read model.
  - `guild` (`:734-752`) → `insectQuery.functionalRoles().getByGuild(selected).sortedByParentName()`.
  - Delete `observationLookup` (`:199-224`).
- Modify templates (`domains/insects/insects-console/src/main/jte/insects/`):
  - `list.jte` → `@param Page<SpeciesCard> cards`; loop `cards.content()`; `card.displayName()`, `card.familyLabel()`, `card.genusLabel()`, `card.species().epithet().value()`, `card.role()`, `card.images()`, `card.collected()` (render the existing collected badge style on the card); pager over `cards`.
  - `families.jte` / `genera.jte` / `orders.jte` → `@param Page<RankCard> cards`; `card.displayName()`, `card.scientificName()`, `card.parentLabel()`, `card.images()`; links from `card.name().value()`.
  - `detail.jte` → `@param Insect insect` replaces `species/genus/family/order/images/stages/role/citations/collected/observations/features`; `commonName = insect.species().species().displayName()`, `binomial = insect.binomial()`; gallery `images = lens ? insect.viewer().ownImages().stream().toList() : insect.observations().stream().toList()`, `observations = insect.viewer().ownObservationByImage()`; `collected = insect.viewer().collected()`; `stages = insect.lifeStages()`.
  - `family.jte` / `genus.jte` / `order.jte` → `@param Insect insect`; `displayName()`; gallery from `insect.observations()` + `insect.viewer().ownObservationByImage()`.
  - `life-stages.jte` → `species.displayName()`; stages as given (keep the ordinal badge: `${String.valueOf(i + 1)}` from the loop index, not `kind().ordinal()`).
  - `observationGallery.jte` → unchanged signature (`List` + `Map`), callers pass the read-model values.
- Modify tests in `insects-console/src/test`: `InsectsListTemplateTest`, `InsectsDetailTemplateTest`, `InsectsFamiliesTemplateTest`, `InsectsGeneraTemplateTest`, `InsectsOrdersTemplateTest`, `MyCollectionWebMvcTest`, `CollectedIndicatorWebMvcTest`, `GalleryLensWebMvcTest`, `InsectsGuildTemplateTest` — template-level tests that construct model attributes directly must build `SpeciesCard`/`RankCard`/`Insect` instead of the old maps; WebMvc tests keep their assertions (same rendered text).

**Interfaces:** consumes Tasks 1–4.

- [ ] **Step 1: Add one failing WebMvc test** to `MyCollectionWebMvcTest`: `mine_withManyCollectedSpecies_pagesInsteadOfEmptying()` — record more species than a console page as one naturalist (via `POST …/observe`), then `GET /insects/species?page=1` with the lens on → 200 and at least one species card. (Pins Review Focus 5 end-to-end.)
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-console test -Dtest=MyCollectionWebMvcTest` Expected: FAIL (page 1 empty today).
- [ ] **Step 3: Implement** controller, templates, tests.
- [ ] **Step 4: Run** `mvn -q -pl domains/insects/insects-console test` Expected: PASS.
- [ ] **Step 5: Confirm the controller is delegation-only** — `file_outline` on `InsectsController`: no `sorted`, `filter`, `HashMap`, `observationLookup`; the only remaining logic members are the clade/placement family (`InsectCladeAnchors` users, `*PlacedAt`, `cladePrefix`, `cladeTaxonomicPrefix`, `lineageTo*`, `breadcrumbTo*`, `introsFor*`) and `safeReturnPath`. List them in the report.
- [ ] **Step 6: Stage** `git add domains/insects/insects-console`

---

### Task 6: Verify, gate, hand-off

- [ ] **Step 1: Full verify** — `mvn verify` Expected: BUILD SUCCESS.
- [ ] **Step 2: Enforcement gate** — `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` Expected: BUILD SUCCESS.
- [ ] **Step 3: Run the console** (`mvn install -DskipTests`, start the app) and click through `/insects/species` (lens on and off), a species, a family, a genus, an order, `/insects/{name}/life-stages`, a guild. Report anything that rendered differently from before.
- [ ] **Step 4: Stage and stop for review** — `git status`. The user commits; the next effort (clade/placement) starts from this controller.
