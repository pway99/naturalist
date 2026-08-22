# `Plant` Read Model — Chunk 4: Role + Images (photo gallery) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fold the ecological `role` and the taxon's own `images` into the `Plant` read model, migrate the species page's role off its hand-collated query, and render a real photo gallery on every rank page — porting the insects image-serving stack (HEIC→JPEG conversion, upload store, resilience) so all four seeded plant photos display.

**Architecture:** `Plant` gains two components — `@Nullable PlantEcologicalRole role` (composed at the subject rank via `ecologicalRoles().forPlantName`) and `ImageCollection images` (via `images().forParentName`). `PlantFactory.base()` composes both. The species handler reads `plant.role()`; all four handlers pass `plant.images()` to a new `plants/gallery.jte`. Image bytes are served by a plants port of the insects `/images/{filename}` (HEIC→JPEG via `sips`, cached, resilience-timed) + `/uploads/{filename}` endpoints, backed by a `PlantImageStorageService`; `PlantsController` gains a `Resilience` constructor dependency.

**Tech Stack:** Java 21 records + sealed types, JTE templates, Spring Web MVC, the kernel `Resilience` facade (framework, transitive), `sips` (macOS HEIC→JPEG), JUnit 5 + AssertJ. Build: scoped `mvn -pl <module> -am test`; the user runs full `mvn verify`.

**Design of record:** [2026-08-20-plant-read-model-design.md](2026-08-20-plant-read-model-design.md) (Chunk 4 of 6). Prior chunks shipped: `73cc06ed` skeleton, `57d2e269` features, `fbf36888` children.

## Global Constraints

- **Insects is the reference; plants moves, insects holds still** (`domains/plants/CLAUDE.md` rule 1). Faithfully port `ImageStorageService`, the `InsectsController` `/images/{filename}` + `/uploads/{filename}` endpoints + `convert`/`jpegResponse` helpers, and `insects/cardImages.jte`, adapting `insects`→`plants` throughout.
- **Full insects-parity image serving (user decision).** `/plants/images/{filename}` converts HEIC→JPEG with a `jpegCache` + a `Resilience` timeout named `"image.conversion"` (reuse the same name to share the app's resilience config); `/plants/uploads/{filename:.+}` streams user uploads from a `PlantImageStorageService` at `data/images/plants`. Bundled catalog photos live on the classpath at `plants/images/` (all 4 `resourceName`s in `plant-images.json` already exist there).
- **Role is an `Entity`, nullable.** `PlantEcologicalRole implements Entity<PlantEcologicalRoleId>`; `ecologicalRoles().forPlantName` returns `Optional`, composed via `.orElse(null)`. Its `Plant.invariants()` descent is `.namedEntityOrNull(role, "role")` — valid because both identity branches implement `Named<?>` (ADR-022), the same call insects' `Insect` uses for its role.
- **`images` is a non-null `ImageCollection`** (`PlantEntityCollections.ImageCollection`, a `BehavioralCollection<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>>`), empty by default; invariant `.behavioralCollection(images, "images")`.
- **Record-arity ripple.** `Plant` grows from 6 to 8 components (role + images). The only `new Plant(...)` sites are in `Plant.java` — confirm with `grep -rn "new Plant(" --include='*.java' domains`.
- **No pom change expected.** `com.naturalist.resilience.Resilience`/`Resilient` live in `kernels/framework` (transitive via `plants-api`); insects-console declares no resilience dep either. `OrganismImage` is transitive via `plants-api`. If the build reports a missing symbol, add the dep the insects-console pom uses — but verify transitivity first.
- **Gallery renders `plant.images()` only** — the taxon's OWN photos (`forParentName`, direct rank), a simple image grid. Do NOT port the insects `observationGallery.jte` (its confidence / "Why this ID?" / notes-form / FieldObservation workflow is out of scope). No CSRF/servlet/Spring-Security types in `gallery.jte` (the `page.jte` classpath trap — see `domains/insects/CLAUDE.md`).
- **Build gotchas (this effort):** `mvn -pl … -am -Dtest=X` needs `-Dsurefire.failIfNoSpecifiedTests=false`.

---

### Task 1: `Plant` gains `role` + `images`; `PlantFactory` composes both

**Files:**
- Modify: `domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java`
- Modify: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantTest.java`
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantFactory.java`
- Modify: `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantQueryImpl.java`
- Modify: `domains/plants/plants-core/src/test/java/com/naturalist/plants/PlantFactoryTest.java`

**Interfaces:**
- Consumes: `PlantQuery.EcologicalRoleQuery.forPlantName(PlantRankName) → Optional<PlantEcologicalRole>`; `PlantQuery.ImageQuery.forParentName(PlantRankName) → ImageCollection`.
- Produces: `Plant.role() → @Nullable PlantEcologicalRole`, `Plant.images() → ImageCollection`; `Plant.withRole(...)`, `Plant.withImages(...)`; `PlantFactory(SpeciesQuery, GenusQuery, FamilyQuery, OrderQuery, FeatureQuery, EcologicalRoleQuery, ImageQuery)` (7-arg).

- [ ] **Step 1: Grep arity sites.** `grep -rn "new Plant(" --include='*.java' domains` — confirm only `Plant.java`.

- [ ] **Step 2: Write the failing tests.** Append to `PlantTest.java`:

```java
    @Test
    void withRole_and_withImages_carry() {
        PlantEcologicalRole role = new PlantEcologicalRole(
                PlantEcologicalRoleId.create(), PlantGenusName.of("helianthus"),
                java.util.Set.of(com.naturalist.plants.PlantRole.KEYSTONE_HOST));
        Plant plant = Plant.empty().withRole(role);
        assertThat(plant.role()).isEqualTo(role);
        assertThat(plant.images()).isNotNull();
        assertThat(plant.images().isEmpty()).isTrue();
    }

    @Test
    void emptyPlant_roleNull_imagesEmpty_noViolations() {
        Plant plant = Plant.empty();
        assertThat(plant.role()).isNull();
        assertThat(observer.forMethod("roleImages").observable(plant, "plant").violations()).isEmpty();
    }
```

(Confirm the `PlantEcologicalRole` ctor arg order + that `PlantRole.KEYSTONE_HOST` exists, against `PlantEcologicalRole.java`/`PlantRole.java`; if the ctor differs, mirror `PlantEcologicalRoleTest`'s construction.)

In `PlantFactoryTest.java`, extend the `factory()` helper to pass the two new queries and add composition tests. The helper already builds `speciesQuery`/`genusQuery`/`familyQuery`/`orderQuery` + the feature query; add:

```java
        PlantQuery.EcologicalRoleQuery roleQuery =
                new PlantEcologicalRoleQueryImpl(new PlantEcologicalRoleRepositoryMock(db));
        PlantQuery.ImageQuery imageQuery =
                new PlantImageQueryImpl(new PlantImageRepositoryMock(db));
        return new PlantFactory(speciesQuery, genusQuery, familyQuery, orderQuery,
                featureQuery, roleQuery, imageQuery);
```

and:

```java
    @Test
    void buildByName_composesImages_forSpeciesWithSeededPhotos() {
        // aristolochia-californica has 2 seeded images (plant-images.json).
        Plant plant = factory().buildByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.images().stream().toList()).hasSize(2);
    }
```

(Confirm the `PlantEcologicalRoleQueryImpl`/`PlantImageQueryImpl` + mock class names against `PlantsTestContext.createPlantQuery`. If a seeded taxon has a role, add a `plant.role() != null` assertion pinned to that taxon; otherwise the empty-role path is covered by `PlantTest`.)

- [ ] **Step 3: Run to verify failure.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest,PlantTest -Dsurefire.failIfNoSpecifiedTests=false` → FAIL.

- [ ] **Step 4: Add `role` + `images` to `Plant`.** Add `import com.naturalist.plants.PlantEntityCollections.ImageCollection;` (and `PlantEcologicalRole` is same-package). Add two components after `children`... **no** — order matters for readability; add `@Nullable PlantEcologicalRole role` and `ImageCollection images` as the 7th and 8th components (after `features`, before or after `children` — put them after `children` to append). Update `empty()` (append `null, ImageCollection.empty()`), thread both through ALL existing `with*`, add `withRole`/`withImages`. In `invariants()` add:

```java
                .namedEntityOrNull(role, "role")
                .behavioralCollection(images, "images")
```

- [ ] **Step 5: Compose in `PlantFactory.base()`.** Add `EcologicalRoleQuery roleQuery` + `ImageQuery imageQuery` as the 6th/7th ctor params (null-checked + assigned). Extend `base`:

```java
    private Plant base(PlantRankName name) {
        return Plant.empty()
                .withFeatures(featureQuery.findByRankName(name))
                .withRole(roleQuery.forPlantName(name).orElse(null))
                .withImages(imageQuery.forParentName(name));
    }
```

- [ ] **Step 6: Pass the queries in `PlantQueryImpl`.** Append `plantEcologicalRoleEntityQuery` + `imageQuery` (both existing fields) to the `new PlantFactory(...)` construction.

- [ ] **Step 7: Run + module green.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFactoryTest,PlantTest -Dsurefire.failIfNoSpecifiedTests=false` → PASS; then `mvn -pl domains/plants/plants-core -am test` → green.

- [ ] **Step 8: Stage (do NOT commit).** `git add` the five files above.

---

### Task 2: `PlantImageStorageService`

**Files:**
- Create: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantImageStorageService.java`

**Interfaces:**
- Produces: package-private `PlantImageStorageService(Path storageDir)` with `FileName store(byte[])` and `Path resolve(String filename)` (path-traversal guarded).

- [ ] **Step 1: Port `ImageStorageService`.** Copy `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/ImageStorageService.java` verbatim into the new file, changing only: the class name → `PlantImageStorageService`, the package → `com.naturalist.plants.console`, and the doc comment's `data/images/insects/` → `data/images/plants/`. Keep the magic-byte validation, `MAX_SIZE`, `store`, `resolve`, and helpers identical (imports `com.naturalist.data.FileName`, `com.naturalist.ddd.EntityId`).

- [ ] **Step 2: Build.** `mvn -pl domains/plants/plants-console -am test-compile` (or the module test) → compiles. (No behavior test — it is exercised by Task 3's endpoints; the insects original has no unit test either, per the "no test-infra-for-test-infra" convention.)

- [ ] **Step 3: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantImageStorageService.java`.

---

### Task 3: `PlantsController` image endpoints + `Resilience` DI

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java`

**Interfaces:**
- Consumes: `com.naturalist.resilience.Resilience` (framework, injected); `PlantImageStorageService` (Task 2).
- Produces: `GET /plants/images/{filename}` (HEIC→JPEG, cached, resilience-timed) and `GET /plants/uploads/{filename:.+}` (streamed upload).

- [ ] **Step 1: Change the constructor to inject `Resilience`.** Replace the current no-arg `PlantsController()` with `PlantsController(Resilience resilience)`, keeping the in-body `PlantsTestContext.create(...)` wiring. Add fields (mirror `InsectsController`):

```java
    private static final String IMAGE_CONVERSION = "image.conversion";
    private final Resilience resilience;
    private final PlantImageStorageService imageStorageService =
            new PlantImageStorageService(java.nio.file.Path.of("data/images/plants"));
    private final java.util.Map<String, byte[]> jpegCache = new java.util.concurrent.ConcurrentHashMap<>();
```

and assign `this.resilience = resilience;` in the constructor. (The `Resilience` bean is provided by `apps/management-console`'s `ResilienceConfiguration` — see how `InsectsController` receives it.)

- [ ] **Step 2: Port the two endpoints + helpers.** Copy the `image(...)` (`@GetMapping("/images/{filename}")`, `@Resilient(name = IMAGE_CONVERSION)`), `serveUpload(...)` (`@GetMapping("/uploads/{filename:.+}")`), `convert(...)`, and `jpegResponse(...)` methods from `InsectsController` (lines ~952–1023) verbatim, changing ONLY: the classpath prefix `insects/images/` → `plants/images/`, the temp-file prefixes `"insect-"` → `"plant-"`, and (in `convert`) nothing else. Add the imports these need if absent: `com.naturalist.resilience.Resilient`, `org.springframework.core.io.ClassPathResource`, `org.springframework.http.ResponseEntity`, `org.springframework.http.MediaType`, `org.springframework.http.CacheControl`, `java.util.concurrent.TimeUnit`, `java.nio.file.Files`, `java.nio.file.Path`, `java.io.IOException`, `jakarta.servlet.http.HttpServletResponse`. Keep the `sips` `ProcessBuilder` conversion identical.

- [ ] **Step 3: Build + module green.** `mvn -pl domains/plants/plants-console -am test` → green. Confirm the app context still wires: the constructor now needs the `Resilience` bean, which management-console provides — if any plants-console `@SpringBootTest`/template test constructs `PlantsController` directly with no args, update it to pass a test `Resilience` (or confirm none do; the template tests render templates via `TestTemplateEngine` and do not instantiate the controller).

- [ ] **Step 4: Stage (do NOT commit).** `git add domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java`.

---

### Task 4: `plants/gallery.jte` + render `plant.images()` on rank pages; species reads `plant.role()`

**Files:**
- Create: `domains/plants/plants-console/src/main/jte/plants/gallery.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/orders/detail.jte`, `families/detail.jte`, `genera/detail.jte`, `detail.jte`
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsController.java` (4 handlers: pass `images`; species reads `plant.role()`)
- Modify (if needed): the four `Plants*DetailTemplateTest.java` (add the `images` param default is `.of()`, so likely no change — verify)

**Interfaces:**
- Consumes: `Plant.images()` (Task 1); `OrganismImage.resourceName()` → `FileName` with `.value()`.

- [ ] **Step 1: Create `plants/gallery.jte`** (mirror the `<img>` uploads→images fallback of `insects/cardImages.jte`, as a standalone section):

```jte
@import com.naturalist.observation.OrganismImage
@import com.naturalist.plants.PlantImageId
@import com.naturalist.plants.PlantObservationId
@import com.naturalist.plants.PlantRankName
@import java.util.List

@param List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> images = java.util.List.of()
@param String alt = ""

@if(!images.isEmpty())
    <section class="photos">
        <h2>Photos</h2>
        <div class="photo-grid">
            @for(int i = 0; i < images.size(); i++)
                <%-- Uploaded photos live under /uploads; bundled catalog images under /images.
                     Try uploads first, fall back once to the classpath copy. --%>
                <img src="/plants/uploads/${images.get(i).resourceName().value()}"
                     alt="${alt}"
                     loading="lazy"
                     onerror="this.onerror=null; this.src='/plants/images/${images.get(i).resourceName().value()}'">
            @endfor
        </div>
    </section>
@endif
```

- [ ] **Step 2: Include the gallery on the four rank templates.** In each of `orders/detail.jte`, `families/detail.jte`, `genera/detail.jte`, `detail.jte`, add near the other `@param`s:

```jte
@param java.util.List<com.naturalist.observation.OrganismImage<com.naturalist.plants.PlantImageId, com.naturalist.plants.PlantObservationId, com.naturalist.plants.PlantRankName>> images = java.util.List.of()
```

and place the include after the description block (before the child-cards section), passing an `alt` of the page's common name:

```jte
    @template.plants.gallery(images = images, alt = commonName)
```

(Each template already computes a `commonName` local. For `detail.jte` — the species page — use its existing display-name local; confirm its name.)

- [ ] **Step 3: Wire the handlers.** In `PlantsController`, add to `orderDetail`/`familyDetail`/`genusDetail`/species `detail`:

```java
        model.addAttribute("images", plant.get().images().stream().toList());
```

And in the species `detail` handler, replace `model.addAttribute("ecologicalRole", plantQuery.ecologicalRoles().forPlantName(plantName).orElse(null));` with `model.addAttribute("ecologicalRole", plant.get().role());` (the template's `ecologicalRole` param + "Ecological roles" section are unchanged).

- [ ] **Step 4: Build + verify template tests.** `mvn -pl domains/plants/plants-console -am test` → green. The four detail template tests pass their existing maps; the new `images` param defaults to `List.of()`, so they still render (empty photos section). If any test asserts on the presence of a photos section, feed it `images` built as `List.of(OrganismImage…)` from the seed source — otherwise no test change is needed. Confirm no `ecologicalRole` handler regression: the species template still renders roles from the `ecologicalRole` attribute (now sourced from `plant.role()`).

- [ ] **Step 5: Stage (do NOT commit).** `git add` `plants/gallery.jte`, the four detail templates, `PlantsController.java`, and any updated template test.

---

## Self-Review

**Spec coverage (design Chunk 4 + user decision "role + render a photo gallery"):**
- `Plant.role()` composed + species handler reads it → Task 1 + Task 4 Step 3. ✓
- `Plant.images()` composed → Task 1. ✓
- Full insects-parity image serving (`/plants/images` HEIC→JPEG + `/plants/uploads` + storage service + `Resilience` DI) → Tasks 2–3. ✓
- Gallery rendered on all four rank pages → Task 4. ✓

**Deferred (named, not silent):** the batched `EcologicalRoleQuery.getByPlantNames(Set)` + child-card role badges from the original design sketch are NOT built — no consumer exists (child cards show no badges today); role composes per-subject via `forPlantName` (one call per page, not a fan-out), so no N+1. Filed for a future slice if child role badges are ever wanted.

**Placeholder scan:** the two verbatim ports (Task 2 `ImageStorageService`, Task 3 endpoints) name the exact source files + the exact adaptations (class/package/path/temp-prefix) — the reference code is the spec, not a placeholder. The "confirm ctor arg order / impl class names / commonName local" notes carry concrete anchors.

**Type consistency:** `role` is `@Nullable PlantEcologicalRole` end to end (`.orElse(null)`, `.namedEntityOrNull`); `images` is `ImageCollection` on `Plant`, streamed to `List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>>` at the handler→template boundary (matching `gallery.jte`'s param). `PlantFactory` 7-arg ctor matches `PlantQueryImpl` + `PlantFactoryTest`. Arity threads through `empty()` + all `with*`.

**Batching honored:** `base()` composes one role + one image collection per subject Plant — single calls, no per-element fetch.

**Notes for the executor:** `Resilience`/`Resilient` are transitive (framework) — no pom change expected; the `Resilience` bean is app-provided (mirror how `InsectsController` gets it); reuse the resilience name `"image.conversion"` to share config; `sips` is macOS-only (dev); the gallery is the taxon's own photos only, NOT the insects observation/identification workflow.
