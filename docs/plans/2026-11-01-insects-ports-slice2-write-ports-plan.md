# Insects Ports — Slice 2: Write Ports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every insects write (identify from a photo, add a photo, record a sighting, update field notes) becomes a named use case on `InsectCommand`, implemented in `insects-core` and discovered by `@DomainService` scan, so `InsectsController` constructs nothing from core and only delegates.

**Architecture:** `InsectCommand` gains three nested ports (`identification()`, `sightings()`, `photos()`). Their `@DomainService` implementations absorb today's package-private `InsectIdentificationCommand` and `InsectAddPhotoCommand`; the two core transactions become beans too and take a package-private bundle of the eight entity commands (`InsectEntityCommands`) instead of `InsectCommand`, which breaks the bean cycle. A new `kernels/identification` holds the port-level refusal type; photo metadata reuses `kernels/vision`'s `ImageMetadata`. The use cases store uploads through the slice-1 `ImageStore` and delete the file if persistence fails. The app supplies the two collaborators the controller used to build inline.

**Tech Stack:** Java 25, kernel `Observer`, `Resilience` facade, `@DomainService` + `DomainServiceScan`, Spring Boot 4, JUnit 5.

**Spec:** `docs/plans/2026-11-01-insects-ports-design.md` (D3, D5, D6, slice 2). Requires slice 1 (`docs/plans/2026-11-01-insects-ports-slice1-image-store-plan.md`) merged. Deviations recorded here: (1) `IdentificationRefused` is one exception class with a `Reason` enum rather than a sealed hierarchy — exceptions cannot be records and two subclasses buy nothing; (2) the bundle type `InsectEntityCommands` is introduced to avoid a constructor cycle the design did not foresee (transactions ← `InsectCommand` ← `InsectCommandImpl` ← use cases ← transactions).

## Global Constraints

- Java edits through Kaname `apply_change` atoms (`envelope: "naturalist"`); hand-edit only after `scripts/atom_gap_capture.py --kind MISSING_ATOM` records the gap (constructor-signature changes and `implements` clauses are known gaps).
- No `Service` suffix. Query/command impls carry the `<DomainNoun><Subject>CommandImpl` naming (ADR-020 §5); nested ports stay bare.
- api signatures may name only api, `framework`, `identifiers`, `observation`, `vision` and `identification` kernel types; root `CLAUDE.md` rule 1 is updated in Task 1 to list `vision` and `identification`.
- Every domain-specific port method observes its arguments with `observer().arguments(...).throwWhenInvalid()` before acting.
- Stage (`git add`), never commit; the user commits after review.
- Done means root `mvn verify`, then `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`, both green.

## Review Focus

1. A non-owner posting another naturalist's observation id to update notes must change nothing and learn nothing (same redirect as not-found) — pinned in Task 4 `updateNotes_nonOwnerLeavesNotesUnchanged`.
2. A rate-limited or over-budget identification must store no file — pinned in Task 6 `identify_rateLimited_storesNothing` / `identify_overBudget_storesNothing`.
3. An invalid upload (wrong format / oversize) to add-photo or identify must persist nothing and store nothing — pinned in Task 5 `add_invalidUpload_storesAndPersistsNothing`.
4. A persistence failure after the file is stored must delete the file — pinned in Task 5 `add_persistFailure_deletesStoredFile`.
5. The app context must resolve every new bean (generic `FeatureSearch<InsectFeatureId>`, `TextGenerationService`, the two transactions, the three use cases) — pinned in Task 7 by booting `MockPersistenceWiringTest`.

Known limitation, not fixed here: `Transaction.execute` is `final`, so a CGLIB proxy cannot wrap it; making the transactions beans does not by itself make them transactional. Record this in the work tracker (Task 9) as a kernel follow-up.

---

### Task 1: `kernels/identification`, `IdentificationRefused`, allow-list

**Files:**
- Create: `kernels/identification/pom.xml` (parent `kernels`; dep `framework`; no tests — kernel tested through its consumer, Task 6)
- Create: `kernels/identification/src/main/java/com/naturalist/identification/IdentificationRefused.java`
- Modify: `kernels/pom.xml` (`<module>identification</module>`), root `pom.xml` KERNELS `dependencyManagement` (alphabetical, after `framework-test`... `identification` sorts after `identifiers-test`), `kernels/CLAUDE.md` (one paragraph: shared identification vocabulary, pipeline extraction deferred to plants), root `CLAUDE.md` (module layout line; DAG line `identification → framework`; rule 1: add `vision` and `identification` to the allow-list and to the "who uses what" sentence: `vision` and `identification` in insects)
- Modify: `domains/insects/insects-api/pom.xml` — add `vision`, `identification` (compile)

**Interfaces:**
- Produces: `public final class IdentificationRefused extends RuntimeException` with `public enum Reason { RATE_LIMITED, BUDGET_EXHAUSTED }`, constructor `IdentificationRefused(Reason reason, @Nullable String detail, @Nullable Instant resetAt)` (message `"Identification refused: " + reason + (detail == null ? "" : " (" + detail + ")")`), accessors `reason()`, `detail()`, `resetAt()`. `detail` carries the budget's limit kind name (`MONTHLY`, …) so an adapter can word the message without importing `usage-api`.

- [ ] **Step 1: Create the module, class and pom entries** (`java_type_add kind: class` for the exception; poms by hand).
- [ ] **Step 2: Build it** — Run: `mvn -q -pl kernels/identification -am install -DskipTests` Expected: BUILD SUCCESS.
- [ ] **Step 3: Update the three CLAUDE.md files and `insects-api/pom.xml`**; run `mvn -q -pl domains/insects/insects-api -am install -DskipTests` Expected: BUILD SUCCESS.
- [ ] **Step 4: Stage** `git add kernels/identification kernels/pom.xml pom.xml kernels/CLAUDE.md CLAUDE.md domains/insects/insects-api/pom.xml`

---

### Task 2: `ImageStore.delete`

**Files:**
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/ImageStore.java` — add `void delete(String space, FileName name);` (javadoc: best-effort; a missing file is not an error; a traversal-shaped name is ignored).
- Modify: `kernels/image-store-filesystem/.../FilesystemImageStore.java` — implement (same guard as `read`; `Files.deleteIfExists` inside the timeout).
- Test: `kernels/image-store-filesystem/src/test/.../FilesystemImageStoreTest.java`

- [ ] **Step 1: Write the failing tests** — `delete_removesStoredFile()` (store, delete, `read` → empty) and `delete_missingIsNoOp()` (no throw); `delete_rejectsPathTraversal()` (a file outside the space is untouched).
- [ ] **Step 2: Run** `mvn -q -pl kernels/image-store-filesystem -am test -Dtest=FilesystemImageStoreTest` Expected: compile failure.
- [ ] **Step 3: Implement** (`java_method_add` on the interface and the class).
- [ ] **Step 4: Run** same command. Expected: PASS. Then `mvn -q -pl kernels/observation,kernels/image-store-filesystem -am install -DskipTests`.
- [ ] **Step 5: Stage** `git add kernels/observation kernels/image-store-filesystem`

---

### Task 3: Break the cycle — `InsectEntityCommands` bundle; transactions become beans

**Files:**
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectEntityCommands.java`
- Modify: `domains/insects/insects-core/.../InsectCommandImpl.java` — constructor becomes `InsectCommandImpl(InsectEntityCommands commands)`; accessors delegate to the bundle.
- Modify: `domains/insects/insects-core/.../InsectCatalogIdentificationTransaction.java:44-50` — field/ctor `InsectEntityCommands commands` replaces `InsectCommand insectCommand`; every `insectCommand.xxx()` → `commands.xxx()`; add `@DomainService`.
- Modify: `domains/insects/insects-core/.../InsectAddPhotoTransaction.java` — same; `@DomainService`.
- Modify: `domains/insects/insects-test-context/.../InsectsTestContext.java` and `domains/insects/insects-core/src/test/.../InsectsTestContextInternal.java` — build `new InsectEntityCommands(speciesCommand, imageCommand, observationCommand, orderCommand, familyCommand, genusCommand, featureCommand, featureAssignmentCommand)` and pass it to the three constructors.

**Interfaces:**
- Produces: `@DomainService final class InsectEntityCommands` (package-private) with the eight-arg constructor above (null-observed like `InsectCommandImpl` today) and accessors `species()`, `images()`, `observations()`, `orders()`, `families()`, `genera()`, `features()`, `featureAssignments()` returning the nested `InsectCommand.*Command` types.

- [ ] **Step 1: Implement** (`java_type_add` for the bundle; `java_field_*`/`java_statement_change` for the two transactions and `InsectCommandImpl`; constructor changes are atom gaps — capture, then hand-edit).
- [ ] **Step 2: Run the core and test-context suites** — `mvn -q -pl domains/insects/insects-core,domains/insects/insects-test-context -am test` Expected: PASS (pure refactor; `InsectCatalogIdentificationTransactionTest`, `InsectAddPhotoCommandTest`, `InsectIdentificationCommandTest` unchanged and green).
- [ ] **Step 3: Stage** `git add domains/insects/insects-core domains/insects/insects-test-context`

---

### Task 4: `sightings()` — record a sighting, update field notes with ownership

**Files:**
- Modify: `domains/insects/insects-api/.../InsectCommand.java` — add `SightingCommand sightings();` and nested:
  ```java
  interface SightingCommand {
      /** Records that {@code naturalist} observed {@code subject} now; blank notes are stored as null. */
      InsectObservationId record(InsectRankName subject, NaturalistName naturalist, @Nullable String notes);
      /** Replaces the field notes of the viewer's own observation; a missing observation or one
       *  recorded by someone else is refused silently (no exception, no change) so the id alone
       *  reveals nothing. Blank notes clear the field. */
      void updateNotes(InsectObservationId observationId, NaturalistName viewer, @Nullable String notes);
  }
  ```
- Create: `domains/insects/insects-core/.../InsectSightingCommandImpl.java` — `@DomainService class InsectSightingCommandImpl implements InsectCommand.SightingCommand`, ctor `(InsectCommand.ObservationCommand observations, InsectQuery.ObservationQuery observationQuery)`.
- Modify: `InsectCommandImpl` — ctor `(InsectEntityCommands commands, InsectCommand.SightingCommand sightings)`; `sightings()` accessor. Test contexts wire `new InsectSightingCommandImpl(observationCommand, observationQuery)`.
- Test: `domains/insects/insects-core/src/test/.../InsectSightingCommandImplTest.java`
- Delete (Task 8 does it, listed here for the interface): `insects-console/.../InsectsControllerNotesOwnershipTest.java` — its three cases move here.

**Interfaces:**
- Consumes: `InsectEntityCommands` (Task 3); `InsectQuery.ObservationQuery.getByName(InsectObservationId)`; `OrganismObservation.withNotes(String)`.
- Produces: `insectCommand.sightings().record(...)` / `.updateNotes(...)`.

- [ ] **Step 1: Write the failing test** (uses `InsectsTestContextInternal`, `TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name`, naturalists `pat-way` / `mallory`):
```java
@Test void record_insertsObservationForNaturalist()        // id returned; query.observations().getByName(id): observedBy, subject, notes "seen today", identification null
@Test void record_blankNotesStoredAsNull()                  // notes "  " → null
@Test void record_rejectsNullNaturalist()                   // InvariantViolationException
@Test void updateNotes_ownerChangesNotes()                  // record then updateNotes(id, same, "new") → notes "new"
@Test void updateNotes_blankClearsNotes()                   // → null
@Test void updateNotes_nonOwnerLeavesNotesUnchanged()       // updateNotes(id, MALLORY, "x") → no throw, notes unchanged
@Test void updateNotes_missingObservationIsSilent()         // InsectObservationId.create() → no throw
@Test void updateNotes_rejectsNullViewer()                  // InvariantViolationException
```
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-core test -Dtest=InsectSightingCommandImplTest` Expected: compile failure.
- [ ] **Step 3: Implement** the port, the impl (`record`: observe `identifier(subject)`, `entityName(naturalist)`; build `new OrganismObservation<>(InsectObservationId.create(), naturalist, subject, Instant.now(), blankToNull(notes), null, null)`; insert; return id. `updateNotes`: observe `entityId`, `entityName`; `observationQuery.getByName(id).filter(o -> o.observedBy().equals(viewer)).ifPresent(o -> observations.update(o.withNotes(blankToNull(notes))))`), and the wiring.
- [ ] **Step 4: Run** the core suite: `mvn -q -pl domains/insects/insects-core,domains/insects/insects-test-context -am test` Expected: PASS.
- [ ] **Step 5: Stage** `git add domains/insects/insects-api domains/insects/insects-core domains/insects/insects-test-context`

---

### Task 5: `photos()` — add a photo through the image store

**Files:**
- Modify: `InsectCommand.java` — add `PhotoCommand photos();` and nested:
  ```java
  interface PhotoCommand {
      /** Stores the upload, then persists an image at {@code subject} (plus an observation when
       *  {@code naturalist} is given) atomically; if persistence fails the stored file is deleted.
       *  @throws InvariantViolationException for an invalid upload — nothing is stored */
      void add(InsectRankName subject, ImageUpload upload, @Nullable NaturalistName naturalist,
               @Nullable String notes, @Nullable String location);
  }
  ```
- Rename: `insects-core/.../InsectAddPhotoCommand.java` → `InsectPhotoCommandImpl.java` (`@DomainService class InsectPhotoCommandImpl implements InsectCommand.PhotoCommand`, ctor `(ImageStore imageStore, InsectAddPhotoTransaction transaction)`, `public static final String SPACE = "insects"`).
- Rename test: `InsectAddPhotoCommandTest.java` → `InsectPhotoCommandImplTest.java`.
- Modify: `InsectCommandImpl` ctor adds `InsectCommand.PhotoCommand photos`; accessor. Test contexts: `InsectsTestContext` gains the overload `create(NaturalistDatabase db, ImageStore imageStore)` (the one-arg form builds `new FilesystemImageStore(Files.createTempDirectory("insects-images"), Resilience.noOp())`); `InsectsTestContextInternal.create(db, imageStore)` likewise. Poms: `insects-core` test-scope `image-store-filesystem`; `insects-test-context` compile-scope `image-store-filesystem` and `observation`.

**Interfaces:**
- Consumes: `ImageStore.store/delete` (slice 1 + Task 2), `ImageUpload`, `InsectAddPhotoTransaction` (Task 3), `PhotoAddition`.
- Produces: `insectCommand.photos().add(...)`.

- [ ] **Step 1: Adapt and extend the test** (`JPEG` = the 16-byte JPEG-magic array from slice 1's tests; `store` = the context's `ImageStore` over a `@TempDir`):
```java
@Test void add_withNaturalist_insertsImageAndObservation()   // (existing, now add(species, ImageUpload.of(JPEG), naturalist, notes, location)); plus store.read(SPACE, photo.resourceName()) isPresent
@Test void add_withoutNaturalist_insertsImageOnly()          // existing shape
@Test void add_genusRank_insertsImageAndObservationAtGenus()  // existing shape
@Test void add_invalidUpload_storesAndPersistsNothing()      // ImageUpload.of(HEIC) → InvariantViolationException; images for subject unchanged; temp dir empty
@Test void add_persistFailure_deletesStoredFile()            // new InsectPhotoCommandImpl(store, throwingTransaction).add(...) → RuntimeException propagates; temp dir has no file
```
`throwingTransaction` = anonymous `InsectAddPhotoTransaction(commands) { protected void doExecute(PhotoAddition a) { throw new IllegalStateException("boom"); } }`.
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-core test -Dtest=InsectPhotoCommandImplTest` Expected: compile failure.
- [ ] **Step 3: Implement**: observe `identifier(subject)`, `valueObject(upload, "upload")`, `whenNotNull(naturalist, entityName)`; `FileName stored = imageStore.store(SPACE, upload)`; build observation/image exactly as today's `addPhoto`; `try { transaction.execute(new PhotoAddition(image, observation)); } catch (RuntimeException e) { imageStore.delete(SPACE, stored); throw e; }`.
- [ ] **Step 4: Run** the core suite. Expected: PASS.
- [ ] **Step 5: Stage** `git add domains/insects`

---

### Task 6: `identification()` — the pipeline behind a port that speaks api vocabulary

**Files:**
- Modify: `InsectCommand.java` — add `IdentificationCommand identification();` and nested:
  ```java
  interface IdentificationCommand {
      /** Rate gate → budget → store upload → vision → enrichment → features → citations →
       *  transactional catalog write. Returns the identified rank.
       *  @throws IdentificationRefused RATE_LIMITED or BUDGET_EXHAUSTED (nothing stored)
       *  @throws InvariantViolationException for an invalid upload (nothing stored) */
      InsectRankName identify(ImageUpload upload, ImageMetadata metadata,
                              NaturalistName naturalist, @Nullable String notes);
  }
  ```
- Rename: `insects-core/.../InsectIdentificationCommand.java` → `InsectIdentificationCommandImpl.java` (`@DomainService class InsectIdentificationCommandImpl implements InsectCommand.IdentificationCommand`); constructor becomes `(Resilience resilience, IdentificationBudget budget, VisionService visionService, TextGenerationService textGenerationService, ExternalAuthority externalAuthority, LibraryCommand libraryCommand, InsectQuery insectQuery, FeatureSearch<InsectFeatureId> featureSearch, ImageStore imageStore, InsectCatalogIdentificationTransaction transaction)`; `public static final String RATE_STRATEGY = "vision.identification"` (the rate limiter is `resilience.rateLimiter(RATE_STRATEGY)`, resolved in the constructor); `public static final String SPACE = "insects"`.
- Rename test: `InsectIdentificationCommandTest.java` → `InsectIdentificationCommandImplTest.java`.
- Modify: `InsectCommandImpl` ctor adds `InsectCommand.IdentificationCommand identification`; accessor. Test contexts: `InsectsTestContext.create(NaturalistDatabase db, ImageStore imageStore, Resilience resilience, IdentificationBudget budget, VisionService visionService, ExternalAuthority authority)` becomes the full overload; the shorter forms fill `Resilience.noOp()`, `IdentificationBudget.noOp()`, `new NoOpVisionService()`, and a package-private `NoOpExternalAuthority` (promote `InsectIdentificationCommandTest`'s `STUB_AUTHORITY` shape into `insects-test-context` main source as that class; it confirms nothing and fetches no content). `TextGenerationService` is always `new NoOpTextGenerationService()`; `FeatureSearch` is `new InMemoryFeatureSearch<>(() -> insectQuery.features().corpus().map(f -> new FeatureCorpus.Indexed<>(f.id(), f.value())))` built inside the context. `InsectsTestContextInternal` mirrors with the stub authority it already has.

**Interfaces:**
- Consumes: `IdentificationRefused` (Task 1), `ImageStore` (Task 2), `InsectCatalogIdentificationTransaction` (Task 3), `vision.Image`/`ImageMetadata`, `BudgetExceededException` (`limitKind()`, `resetAt()`), `RateLimitExceededException`.
- Produces: `insectCommand.identification().identify(...)`.

Body changes the signature does not determine:
- Step order: observe (`valueObject(upload)`, `valueObject(metadata)`, `entityName(naturalist)`) → rate gate (`catch RateLimitExceededException → throw new IdentificationRefused(RATE_LIMITED, null, null)`) → `budget.reserve(naturalist)` (`catch BudgetExceededException e → throw new IdentificationRefused(BUDGET_EXHAUSTED, e.limitKind().name(), e.resetAt())`) → `FileName stored = imageStore.store(SPACE, upload)` → `var image = new Image(upload.bytes(), upload.mediaType(), metadata)` → the existing pipeline unchanged → `transaction.execute(catalogId)` wrapped like Task 5 (delete on failure) → return rank.

- [ ] **Step 1: Adapt and extend the test**: every existing case calls `identify(ImageUpload.of(JPEG), new ImageMetadata(location, capturedAt), naturalist, notes)` against `new InsectIdentificationCommandImpl(resilienceOrStub, budget, stubService, new NoOpTextGenerationService(), STUB_AUTHORITY, libraryCommand, query, featureSearch, store, context.catalogIdentificationTransaction())`; add:
```java
@Test void identify_rateLimited_storesNothing()      // RejectingRateLimiterResilience (copy from InsectsControllerRateLimitedTest) → IdentificationRefused with reason RATE_LIMITED; temp dir empty
@Test void identify_overBudget_storesNothing()       // budget throws BudgetExceededException(MONTHLY, resetAt) → reason BUDGET_EXHAUSTED, detail "MONTHLY", resetAt equal; temp dir empty
@Test void identify_success_storesTheUpload()        // the persisted image's resourceName is readable from the store
```
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-core test -Dtest=InsectIdentificationCommandImplTest` Expected: compile failure.
- [ ] **Step 3: Implement** the port, the rename, the body changes above, the wiring and test-context overloads.
- [ ] **Step 4: Run** `mvn -q -pl domains/insects/insects-core,domains/insects/insects-test-context -am test` Expected: PASS.
- [ ] **Step 5: Install** `mvn -q -pl domains/insects/insects-core,domains/insects/insects-test-context -am install -DskipTests` (the console compiles against the new api).
- [ ] **Step 6: Stage** `git add domains/insects`

---

### Task 7: App wiring — feature search and text generation beans, security rule

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/IdentificationConfiguration.java` — `@Configuration` with `@Bean FeatureSearch<InsectFeatureId> insectFeatureSearch(InsectQuery insectQuery)` (the `InMemoryFeatureSearch` expression from Task 6, moved here from the controller) and `@Bean TextGenerationService textGenerationService()` returning `new NoOpTextGenerationService()` (the Anthropic text adapter is not on the app classpath today; keep parity).
- Modify: `apps/management-console/.../SecurityConfiguration.java:37` — drop `"/insects/*/re-identify"` from the VISION matcher.
- Modify: `apps/management-console/pom.xml` — `feature-search` and `text-generation` kernels if not already transitive at compile scope.

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Boot check** — Run: `mvn -q -pl apps/management-console test -Dtest=MockPersistenceWiringTest,ResilienceConfigurationTest` Expected: PASS — the context resolves `InsectCommandImpl` with its three use cases and both transactions (fails here, not in production, if a generic bean or a transaction cannot be wired).
- [ ] **Step 3: Stage** `git add apps/management-console`

---

### Task 8: Controller delegation; delete re-identify and the ownership test

**Files:**
- Modify: `domains/insects/insects-console/.../InsectsController.java`
  - constructor → `InsectsController(InsectQuery insectQuery, InsectCommand insectCommand, InsectLifeStageQuery insectLifeStageQuery, CladeQuery cladeQuery, GlossaryTermQuery glossaryTermQuery, ImageStore imageStore)`; delete fields `resilience`, `identificationCommand`, `addPhotoCommand`, and the constructor's `new …Transaction/Command/FeatureSearch` lines.
  - `identify`: parse `capturedAt` as today; then
    ```java
    try {
        var rank = insectCommand.identification().identify(
                ImageUpload.of(imageFile.getBytes()), new ImageMetadata(location, capturedInstant), me.get(), notes);
        return "redirect:/insects/" + rank.value();
    } catch (IdentificationRefused refused) {
        return "redirect:/insects/identify?limit=" + (refused.reason() == IdentificationRefused.Reason.RATE_LIMITED ? "RATE" : refused.detail());
    } catch (InvariantViolationException invalidUpload) {
        return "redirect:/insects/identify?error=image";
    }
    ```
    (removes the slice-1 `imageStore.store` call; the `?limit=` values are unchanged so `identify.jte` needs no edit).
  - `addImage`: `insectCommand.photos().add(InsectSpeciesName.of(name), ImageUpload.of(imageFile.getBytes()), me.orElse(null), notes, location)` inside the same `InvariantViolationException` → `?error=image` guard.
  - `observe`: `insectCommand.sightings().record(InsectSpeciesName.of(name), me.get(), notes)`.
  - `updateNotes`: keep id parsing and `safeReturnPath`; replace the get/owns/withNotes/update block with `currentNaturalist(request).ifPresent(viewer -> insectCommand.sightings().updateNotes(obsId, viewer, notes));` then redirect.
  - Delete `reIdentify` (`:921-942`) and `owns` (`:884-896`); remove unused imports (`Instant` stays for `capturedAt`; `OrganismObservation`, `Resilience`, `FeatureSearch`, `InMemoryFeatureSearch`, `FeatureCorpus`, `NoOpTextGenerationService`, `Resilient`).
- Delete: `domains/insects/insects-console/src/test/.../InsectsControllerNotesOwnershipTest.java`
- Modify tests:
  - `InsectsControllerTestConfig` — expose `static final InsectsTestContext INSECTS = InsectsTestContext.create(DATABASE, IMAGE_STORE)` beside `DATABASE`/`IMAGE_STORE` and build the controller from it with the six-arg constructor, so slice tests can read state back through `INSECTS.insectQuery()`.
  - `InsectsControllerOverBudgetTest` — `InsectsTestContext.create(nte, store, Resilience.noOp(), EXCEEDED_BUDGET, new NoOpVisionService(), InsectsTestContext.noOpAuthority())` (Task 6 makes `noOpAuthority()` a `public static` accessor on the context returning the package-private `NoOpExternalAuthority`); controller via the six-arg constructor; assertion unchanged (`?limit=MONTHLY`).
  - `InsectsControllerRateLimitedTest` — same with `new RejectingRateLimiterResilience()`; assertion unchanged (`?limit=RATE`).
  - `ObserveWebMvcTest`, `CaptureLinksObservationWebMvcTest`, `InsectsCatalogWebMvcTest` — unchanged expectations; they now exercise the ports.
- Modify: `domains/insects/insects-console/pom.xml` — drop `authority`, `usage-api`, `text-generation` if no remaining reference (check with `find_type`); keep `library-api`.

**Interfaces:** consumes Tasks 4–6 ports and `IdentificationRefused`.

- [ ] **Step 1: Write the failing test** in `InsectsCatalogWebMvcTest`: `updateNotes_byNonOwner_redirectsWithoutChange()` — record a sighting as `pat-way` via `POST /insects/battus-philenor/observe`, find its id through `InsectsControllerTestConfig.INSECTS.insectQuery().observations().forNaturalist(NaturalistName.of("pat-way"))`, then `POST /insects/battus-philenor/notes` as `mallory` with that id → 3xx to `/insects/battus-philenor`; notes unchanged via the query.
- [ ] **Step 2: Run** `mvn -q -pl domains/insects/insects-console test -Dtest=InsectsCatalogWebMvcTest` Expected: FAIL (compile: constructor arity) .
- [ ] **Step 3: Implement** the controller and test changes; delete the two files.
- [ ] **Step 4: Run** `mvn -q -pl domains/insects/insects-console test` Expected: PASS, every suite.
- [ ] **Step 5: Stage** `git add domains/insects/insects-console`

---

### Task 9: Work tracker, verify, gate

**Files:**
- Modify: `docs/work-tracker.md` — under "Candidate next slices" add: **Re-identification** — rebuild `/insects/{name}/re-identify` on `InsectCommand.sightings()` with image re-parenting (`ImageQuery.forObservation(s)` on mock + rdbms), after the authorization work; and under the same list: **`Transaction.execute` is `final`** — bean transactions are not proxied; decide whether to drop `final` or move `@Transactional` to the subclass hook.

- [ ] **Step 1: Edit the tracker.**
- [ ] **Step 2: Full verify** — Run: `mvn verify` Expected: BUILD SUCCESS.
- [ ] **Step 3: Enforcement gate** — Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` Expected: BUILD SUCCESS.
- [ ] **Step 4: Stage and stop for review** — `git add docs/work-tracker.md && git status`. Report the controller's remaining non-delegating members (should be only the clade/placement methods and `safeReturnPath`).
