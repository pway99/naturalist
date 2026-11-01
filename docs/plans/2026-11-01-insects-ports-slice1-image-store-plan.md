# Insects Ports — Slice 1: Image Store Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the two copied console image-storage services and the `sips` HEIC conversion path with one kernel `ImageStore` port, one filesystem implementation, and JPEG seed fixtures.

**Architecture:** `kernels/observation` gains the port (`ImageStore`) and the validated upload value (`ImageUpload`, which sniffs JPEG/PNG/WebP and caps size). A new `kernels/image-store-filesystem` implements the port over `data/images/<space>` with a classpath fallback for seed photos, behind an `image.store` timeout. The app wires one bean; both consoles inject it, serve images through one endpoint each, and lose their storage classes, caches and conversion code. The 16 HEIC seed photos become JPEGs once.

**Tech Stack:** Java 25 records, kernel `Observer`/`Constraints`, `Resilience` facade, Spring Boot 4 `@ConfigurationProperties`, JUnit 5 `@TempDir`, JTE, `sips` (macOS, one-time fixture conversion only).

**Spec:** `docs/plans/2026-11-01-insects-ports-design.md` (D4, D5, slice 1). Deviation recorded here: the format whitelist and size cap live on the kernel `ImageUpload` value's `invariants()` rather than as constants on the slice-2 command, so this slice ships complete on its own and slice 2's command simply observes the value. The policy is still enforced before any store touch.

## Global Constraints

- Java edits go through Kaname `apply_change` atoms (`java_type_add`, `java_method_add`, `java_statement_*`, `java_import_add`, …) with `envelope: "naturalist"`; hand-edit a `.java` file only after `scripts/atom_gap_capture.py --kind MISSING_ATOM` records the gap. Non-Java files (poms, yml, jte, js, json, md) are edited directly.
- No `Service` suffix on new types. Records for value objects; `invariants()` on every `Observable`.
- No `UUID.randomUUID()`: file names use `EntityId.newUUID()`.
- Every third-party version lives in the root pom only; new modules reference `${project.version}` via `dependencyManagement`.
- A filesystem call is cross-boundary (`docs/resilience-policy.md`): the store declares `@Resilient(name = "image.store")` and wraps its I/O in `resilience.timeout("image.store")`.
- Stage changes (`git add`); do not commit — the user commits after review (repo convention).
- Done means `mvn verify` from the root passes, then `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` passes, then the standing `naturalist_test` database is reseeded.

## Review Focus

1. A served filename containing `..` or a slash must 404, never read outside the images root — pinned in Task 2's `read_rejectsPathTraversal`.
2. A seed photo requested before `data/images/insects` exists must be served from the classpath — pinned in Task 2's `read_fallsBackToClasspath`.
3. An upload whose bytes are not JPEG/PNG/WebP (e.g. HEIC magic) must redirect back to the form with the error channel, not 500 — pinned in Task 4's `addImage_rejectsUnsupportedFormat`.
4. A stored name with an unknown extension must serve as `application/octet-stream`, never guess — pinned in Task 2's `read_unknownExtensionIsOctetStream`.
5. A desktop browser that cannot decode the chosen file must not hang on "Identifying…" — Task 7's manual check (JavaScript has no unit harness here).

---

### Task 1: `ImageUpload` value and `ImageStore` port in `kernels/observation`

**Files:**
- Create: `kernels/observation/src/main/java/com/naturalist/observation/ImageUpload.java`
- Create: `kernels/observation/src/main/java/com/naturalist/observation/StoredImage.java`
- Create: `kernels/observation/src/main/java/com/naturalist/observation/ImageStore.java`
- Test: `kernels/observation/src/test/java/com/naturalist/observation/ImageUploadTest.java`

**Interfaces:**
- Produces: `public record ImageUpload(byte[] bytes) implements ValueObject` with `public static ImageUpload of(byte[] bytes)`, `public String mediaType()` (one of `image/jpeg`, `image/png`, `image/webp`, else `null`), `public String extension()` (`.jpg`, `.png`, `.webp`, else `null`), `public static final int MAX_BYTES = 20 * 1024 * 1024`; `equals`/`hashCode` over `Arrays.equals`/`Arrays.hashCode` (record default compares the array by reference).
- Produces: `public record StoredImage(byte[] bytes, String mediaType)` (plain record, no invariants: it is read-side transport).
- Produces: `public interface ImageStore { FileName store(String space, ImageUpload upload); Optional<StoredImage> read(String space, FileName name); }`. `space` is the domain namespace (`"insects"`, `"plants"`). Javadoc states: an invalid `ImageUpload` is the caller's defect (observe it first); `store` never trusts a client-supplied name.

- [ ] **Step 1: Write the failing test**

```java
class ImageUploadTest {
    private static final Observer observer = Observer.forClass(ImageUploadTest.class);
    static final byte[] JPEG = {(byte)0xFF,(byte)0xD8,(byte)0xFF,(byte)0xE0, 0,0,0,0,0,0,0,0,0,0,0,0};
    static final byte[] PNG  = {(byte)0x89,0x50,0x4E,0x47, 0,0,0,0,0,0,0,0,0,0,0,0};
    static final byte[] WEBP = {0x52,0x49,0x46,0x46, 0,0,0,0, 0x57,0x45,0x42,0x50, 0,0,0,0};
    static final byte[] HEIC = {0,0,0,0x18,0x66,0x74,0x79,0x70,0x68,0x65,0x69,0x63, 0,0,0,0};

    @Test void jpeg_isValidWithMediaTypeAndExtension()   // mediaType()=="image/jpeg", extension()==".jpg", invalidInvariants().isEmpty()
    @Test void png_isValid()                              // "image/png", ".png"
    @Test void webp_isValid()                             // "image/webp", ".webp"
    @Test void heic_isInvalid_format()                    // invalid invariant names == {"format"}
    @Test void oversize_isInvalid_size()                  // new byte[MAX_BYTES + 1] prefixed with JPEG magic → {"size"}
    @Test void tooShort_isInvalid_format()                // 3 bytes → {"format"}
    @Test void equality_isByContent()                     // of(JPEG).equals(of(JPEG.clone())) and same hashCode
}
```
Use the repo's `Observer → MethodObserver → InvariantObservation` pattern (`mo.observable(upload, "upload")`, `invalidInvariantNamesRemovingPrefix`).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl kernels/observation test -Dtest=ImageUploadTest`
Expected: compilation failure, `ImageUpload` not found.

- [ ] **Step 3: Create `ImageUpload`, `StoredImage`, `ImageStore`**

Via `apply_change { tool: java_type_add, kind: record }` ×2 and `java_type_add kind: interface`. `invariants()`: `c -> c.isTrue(mediaType() != null, "format").isTrue(bytes.length <= MAX_BYTES, "size")` (sniff: JPEG `FF D8 FF`; PNG `89 50 4E 47`; WebP `RIFF????WEBP`; anything under 12 bytes is no format). Carry the magic-byte constants over from `insects-console/ImageStorageService` (they are deleted in Task 4).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl kernels/observation test -Dtest=ImageUploadTest`
Expected: 7 tests pass.

- [ ] **Step 5: Stage**

```bash
git add kernels/observation/src
```

---

### Task 2: `kernels/image-store-filesystem` — `FilesystemImageStore`

**Files:**
- Create: `kernels/image-store-filesystem/pom.xml` (parent `kernels`; deps `framework`, `observation`; test deps `framework-test`, junit, assertj as sibling kernels declare them)
- Create: `kernels/image-store-filesystem/src/main/java/com/naturalist/observation/filesystem/FilesystemImageStore.java`
- Create: `kernels/image-store-filesystem/src/test/java/com/naturalist/observation/filesystem/FilesystemImageStoreTest.java`
- Create: `kernels/image-store-filesystem/src/test/resources/testspace/images/seed.jpg` (any ≥16-byte JPEG-magic file)
- Modify: `kernels/pom.xml` (`<module>image-store-filesystem</module>`, after `framework-test`’s alphabetical neighbours)
- Modify: `pom.xml` root `<dependencyManagement>` KERNELS section (alphabetical: after `identifiers-test`, before `measurements`)
- Modify: `kernels/CLAUDE.md` (one line under Core Kernels for `observation`'s new port and the new module; add `image-store-filesystem` and `observation` to the "Scope of this convention" kernel list)
- Modify: `CLAUDE.md` root Module layout block (one line for `image-store-filesystem`)

**Interfaces:**
- Consumes: `ImageStore`, `ImageUpload`, `StoredImage` (Task 1); `Resilience`, `Resilient`, `EntityId.newUUID()`, `FileName` (framework).
- Produces: `public final class FilesystemImageStore implements ImageStore` with `public static final String STRATEGY = "image.store"`, constructor `FilesystemImageStore(Path root, Resilience resilience)`, class-level `@Resilient(name = FilesystemImageStore.STRATEGY)`.

Behaviour the signature does not determine:
- `store`: observes its arguments first — `observer.arguments("store", i -> i.notBlank(space, "space").valueObject(upload, "upload")).throwWhenInvalid()` — so an invalid upload (wrong format, oversize) raises `InvariantViolationException` at the boundary and nothing is written; `root.resolve(space)` is created if absent on every call; file name `EntityId.newUUID() + upload.extension()`; write wrapped in `resilience.timeout(STRATEGY).execute(...)`; returns `FileName.of(name)`.
- `read`: reject any `name.value()` that contains `/`, `\\` or `..`, or whose normalised path escapes `root/space`, by returning `Optional.empty()`; look in `root/space/<name>` first, then the classpath resource `<space>/images/<name>`; media type from `name.nameType()`: `JPG`/`JPEG` → `image/jpeg`, `PNG` → `image/png`, `WEBP` → `image/webp`, else `application/octet-stream`. Reads are also wrapped in the timeout.

- [ ] **Step 1: Write the failing test**

```java
class FilesystemImageStoreTest {
    @TempDir Path root;
    FilesystemImageStore store;   // new FilesystemImageStore(root, Resilience.noOp()) in @BeforeEach

    @Test void store_writesUnderSpaceWithSniffedExtension()   // store("insects", ImageUpload.of(JPEG)) → name ends ".jpg"; Files.exists(root/insects/name)
    @Test void store_createsSpaceDirectoryWhenMissing()        // Files.delete(root/insects) first (if present) → store succeeds
    @Test void store_rejectsInvalidUpload()                    // store("insects", ImageUpload.of(HEIC)) throws InvariantViolationException; root/insects stays empty
    @Test void read_roundTripsStoredBytesAndMediaType()        // read("insects", name) → bytes equal, mediaType "image/jpeg"
    @Test void read_missingIsEmpty()                           // read("insects", FileName.of("nope.jpg")).isEmpty()
    @Test void read_rejectsPathTraversal()                     // FileName.of("../../etc/passwd"), FileName.of("a/b.jpg") → empty, nothing read
    @Test void read_fallsBackToClasspath()                     // read("testspace", FileName.of("seed.jpg")) → present, "image/jpeg" (root has no testspace dir)
    @Test void read_unknownExtensionIsOctetStream()            // Files.write(root/insects/blob.bin, …) → mediaType "application/octet-stream"
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl kernels/image-store-filesystem -am test -Dtest=FilesystemImageStoreTest` (after the pom entries exist)
Expected: compilation failure, `FilesystemImageStore` not found.

- [ ] **Step 3: Implement `FilesystemImageStore`** (`java_type_add kind: class`, then `java_method_add` for `store`/`read` and the private traversal guard + media-type helper).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl kernels/image-store-filesystem -am test -Dtest=FilesystemImageStoreTest`
Expected: 8 tests pass.

- [ ] **Step 5: Install the kernels** so dependants compile against the new signatures (memory: kernel changes need a clean install).

Run: `mvn -q -pl kernels/observation,kernels/image-store-filesystem -am install -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Stage**

```bash
git add kernels/image-store-filesystem kernels/pom.xml pom.xml kernels/CLAUDE.md CLAUDE.md
```

---

### Task 3: App wiring — `ImageStore` bean, `image.store` strategy, config

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/images/ImageStoreProperties.java` (`@ConfigurationProperties("naturalist.images")`, `record ImageStoreProperties(String root)`, compact ctor defaults blank/null to `"data/images"` — mirror `AccountProperties`)
- Create: `apps/management-console/src/main/java/com/naturalist/console/images/ImageStoreConfiguration.java` (`@Configuration`, enables the properties the same way `AccountConfiguration` enables `AccountProperties`; `@Bean ImageStore imageStore(ImageStoreProperties p, Resilience r) { return new FilesystemImageStore(Path.of(p.root()), r); }`)
- Modify: `apps/management-console/src/main/java/com/naturalist/console/resilience/ResilienceConfiguration.java:100-103` — replace `imageConversionTimeout` with `@Bean TimeoutConfig imageStoreTimeout() { return new TimeoutConfig(FilesystemImageStore.STRATEGY, Duration.ofSeconds(2)); }`; update the class javadoc bullet at line 28.
- Modify: `apps/management-console/src/main/resources/application.yml` — add under `naturalist:` → `images:\n    root: data/images`
- Modify: `apps/management-console/pom.xml` — add `image-store-filesystem` (compile) and `observation` (compile, if not already transitive-explicit)
- Modify tests: `apps/management-console/src/test/java/com/naturalist/console/resilience/ResilienceConfigurationTest.java:38` → `resilience.timeout("image.store")`; `apps/management-console/src/test/java/com/naturalist/console/admin/AdminResilienceControllerWebMvcTest.java:31,93,97` → `image.store`.

**Interfaces:**
- Consumes: `FilesystemImageStore(Path, Resilience)`, `FilesystemImageStore.STRATEGY` (Task 2).
- Produces: a single `ImageStore` bean; the registered strategy name `image.store`.

- [ ] **Step 1: Update the two tests first** (rename the expected strategy) and run them.

Run: `mvn -q -pl apps/management-console test -Dtest=ResilienceConfigurationTest,AdminResilienceControllerWebMvcTest`
Expected: FAIL — `image.store` not registered.

- [ ] **Step 2: Implement the properties, configuration and resilience bean; edit `application.yml` and the pom.**

- [ ] **Step 3: Run the two tests**

Run: same as Step 1. Expected: PASS.

- [ ] **Step 4: Boot-level check** — the `ResilienceNameValidator` must accept the new name and the context must start with the bean:

Run: `mvn -q -pl apps/management-console test -Dtest=MockPersistenceWiringTest`
Expected: PASS (context starts; no `UnconfiguredResilienceException`).

- [ ] **Step 5: Stage**

```bash
git add apps/management-console/src apps/management-console/pom.xml
```

---

### Task 4: Insects console — inject the store, one serve endpoint, error channel, delete the old service

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java`
  - constructor (`:102-136`): add parameter `ImageStore imageStore` (last), field `private final ImageStore imageStore`; delete `this.imageStorageService = new ImageStorageService(Path.of("data/images/insects"))` and the field; delete `IMAGE_CONVERSION` (`:60`) and `jpegCache` (`:100`).
  - `identify` (`:472-509`): replace `imageStorageService.store(imageBytes)` with `var storedFileName = imageStore.store("insects", ImageUpload.of(imageBytes));` inside a `try` that catches the store's `InvariantViolationException` → `return "redirect:/insects/identify?error=image";`. Keep the rest unchanged for this slice (slice 2 moves the store call into the use case).
  - `identifyForm` (`:455-470`): accept `@RequestParam(name = "error", required = false) String error` and put `model.addAttribute("imageError", true)` when `"image".equals(error)`.
  - `addImage` (`:841-853`): same store call; on `InvariantViolationException` → `return "redirect:/insects/" + name + "?error=image";`.
  - `detail` (`:754`): accept `@RequestParam(name = "error", required = false) String error` → `model.addAttribute("imageError", true)` when `"image"`.
  - Delete `image` (`:968-990`), `serveUpload` (`:992-1006`), `convert` (`:1008-1032`), `jpegResponse` (`:1034-1039`). Add:
    ```java
    @GetMapping("/images/{filename:.+}")
    ResponseEntity<byte[]> image(@PathVariable String filename)
    ```
    → `imageStore.read("insects", FileName.of(filename))` → `ResponseEntity.ok().contentType(MediaType.parseMediaType(stored.mediaType())).cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable()).body(stored.bytes())`, else `ResponseEntity.notFound().build()`. Remove the now-unused imports (`ClassPathResource`, `Files`, `Path`, `ConcurrentHashMap`, `HttpServletResponse`, `Resilient` if no other use).
- Delete: `domains/insects/insects-console/src/main/java/com/naturalist/insects/ImageStorageService.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/cardImages.jte:16-19` and `observationGallery.jte:23-26` — `src="/insects/images/${…resourceName().value()}"`, delete the `onerror` attribute.
- Modify: `domains/insects/insects-console/src/main/jte/insects/identify.jte` (near the existing `limit` message block above line 30) and `detail.jte` (above the Add Photo form, ~line 123) — render `<p class="error">That file is not a JPEG, PNG or WebP photo under 20 MB.</p>` when `imageError` is set. Add `@param boolean imageError = false` the way the template declares its other optional params.
- Modify: `domains/insects/insects-console/pom.xml` — add `observation` (compile) and `image-store-filesystem` (test).
- Modify tests:
  - `InsectsControllerTestConfig.java` — pass `new FilesystemImageStore(IMAGE_ROOT, Resilience.noOp())` as the new last constructor argument, where `static final Path IMAGE_ROOT = Files.createTempDirectory("insects-images")` (wrap the checked exception in a static initialiser helper).
  - `InsectsCatalogWebMvcTest.java`, `CaptureLinksObservationWebMvcTest.java`, `InsectsControllerOverBudgetTest.java`, `InsectsControllerRateLimitedTest.java` — delete the `IMAGE_DIR` constants and `@AfterAll` cleanups; direct-construction tests pass a `FilesystemImageStore` over a `@TempDir`.
  - `GalleryLensWebMvcTest.java:22,39,48` — `pipevine-swallow-tail.HEIC` → `pipevine-swallow-tail.jpg` (the fixture rename lands in Task 6; the assertion is on the rendered name only).

**Interfaces:**
- Consumes: `ImageStore`, `ImageUpload`, `StoredImage` (Task 1); `FilesystemImageStore` (Task 2, test scope).
- Produces: `GET /insects/images/{filename}` serving seed and uploaded photos alike; `?error=image` redirect channel on `/insects/identify` and `/insects/{name}`.

- [ ] **Step 1: Write the failing tests** in `InsectsCatalogWebMvcTest`:

```java
@Test void addImage_rejectsUnsupportedFormat()   // multipart with HEIC magic bytes → 3xx, redirectedUrl("/insects/battus-philenor?error=image")
@Test void detail_rendersImageErrorWhenFlagged()  // get("/insects/battus-philenor?error=image") → contains "not a JPEG, PNG or WebP"
@Test void image_servesStoredUpload()             // post TINY_JPEG via addImage, then follow: find the stored name through the test's ImageStore? Simpler: store directly via InsectsControllerTestConfig's store, then get("/insects/images/" + name) → 200, content-type image/jpeg, Cache-Control contains "immutable"
@Test void image_missingIs404()                   // get("/insects/images/nope.jpg") → 404
```
Expose the test config's store as `static final FilesystemImageStore IMAGE_STORE` so the third test can store a file.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -pl domains/insects/insects-console test -Dtest=InsectsCatalogWebMvcTest`
Expected: compilation failure (constructor arity) or 404/500 on the new endpoints.

- [ ] **Step 3: Implement the controller, template, pom and test-config changes; delete `ImageStorageService`.** Controller edits via atoms (`java_field_add`, `java_method_remove`, `java_method_add`, `java_statement_change`, `java_import_remove`); the constructor signature change is a known atom gap — capture it, then hand-edit.

- [ ] **Step 4: Run the module's tests**

Run: `mvn -q -pl domains/insects/insects-console test`
Expected: all pass, including the four new tests and `GalleryLensWebMvcTest` (fixture name updated in Task 6 — until then run it last or expect the single name assertion to fail and re-run after Task 6).

- [ ] **Step 5: Stage**

```bash
git add domains/insects/insects-console
```

---

### Task 5: Plants console — same switch

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantsController.java` — constructor takes `ImageStore imageStore`; delete `IMAGE_CONVERSION` (`:56`), `jpegCache`, `new PlantImageStorageService(...)` (`:66-67`), `image` (`:457-481`), `serveUpload` (`:483-497`), `convert`, `jpegResponse`; add the same `GET /images/{filename:.+}` as Task 4 with space `"plants"`. (Plants has no upload endpoint today; it only serves.)
- Delete: `domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantImageStorageService.java`
- Modify: `domains/plants/plants-console/src/main/jte/plants/gallery.jte:17-20` — `src="/plants/images/…"`, drop `onerror`.
- Modify: `domains/plants/plants-console/pom.xml` — `observation` (compile), `image-store-filesystem` (test).
- Modify tests: whichever plants test constructs `PlantsController` (read `PlantsListTemplateTest` / its config to find the one place) — pass a `FilesystemImageStore` over a temp directory.

**Interfaces:** as Task 4, space `"plants"`.

- [ ] **Step 1: Write the failing test** in the plants slice test that renders the detail page: `image_missingIs404()` and `image_servesClasspathSeed()` (`get("/plants/images/" + <a .jpg name from plant-images.json after Task 6>)` → 200 `image/jpeg`; until Task 6 lands use a name you store through the test's store).
- [ ] **Step 2: Run to verify it fails.** Run: `mvn -q -pl domains/plants/plants-console test -Dtest=<that test>` Expected: FAIL.
- [ ] **Step 3: Implement** (same atom sequence as Task 4).
- [ ] **Step 4: Run the module's tests.** Run: `mvn -q -pl domains/plants/plants-console test` Expected: PASS.
- [ ] **Step 5: Stage** `git add domains/plants/plants-console`

---

### Task 6: Seed fixtures HEIC → JPEG, fixture JSON, standing DB reseed

**Files:**
- Convert and rename: all 14 files under `domains/insects/insects-repository-test/src/main/resources/insects/images/*.HEIC` and the 2 under `domains/plants/plants-repository-test/src/main/resources/plants/images/*.HEIC` → same base name with `.jpg`; delete the `.HEIC` originals.
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-images.json` — every `"resourceName": "<x>.HEIC"` → `"<x>.jpg"` (11 rows, incl. `pipevine-swallow-tail`).
- Modify: `domains/plants/plants-repository-test/src/main/resources/plants/plant-images.json:25,35` — `IMG_9313.HEIC`, `IMG_9264.HEIC` → `.jpg`.

- [ ] **Step 1: Convert** (one-time, macOS):

```bash
cd domains/insects/insects-repository-test/src/main/resources/insects/images && for f in *.HEIC; do sips -s format jpeg -s formatOptions 85 "$f" --out "${f%.HEIC}.jpg" && rm "$f"; done
```
and the same loop in `domains/plants/plants-repository-test/src/main/resources/plants/images`.
Expected: `ls` shows only `.jpg`/`.JPEG` files; each new file begins with bytes `FF D8 FF` (`xxd -l 3`).

- [ ] **Step 2: Rewrite the JSON references** (`sed -i '' 's/\.HEIC"/.jpg"/g'` on the two JSON files) and confirm no `.HEIC` remains in either fixture tree:

Run: `mvn -q -pl domains/insects/insects-repository-test,domains/plants/plants-repository-test test`
Expected: PASS (the `TestEntitySource` tests load the catalogs).

- [ ] **Step 3: Re-run the console suites touched in Tasks 4–5** (`GalleryLensWebMvcTest` now sees `.jpg`).

Run: `mvn -q -pl domains/insects/insects-console,domains/plants/plants-console test`
Expected: PASS.

- [ ] **Step 4: Reseed the standing database** so `insect_image.resource_name` / `plant_image.resource_name` carry the new names (standing DB is a fixed fixture; repair by rerunning the seeder — memory):

```bash
mvn -q -pl apps/test-db-seeder -am install -DskipTests
mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder
```
Then: `psql jdbc… -c "select resource_name from insect_image"` shows only `.jpg` names. If the seeder refuses because rows exist, follow its own README/`TestDbSeeder` reset path rather than deleting rows by hand.

- [ ] **Step 5: Stage**

```bash
git add domains/insects/insects-repository-test/src/main/resources domains/plants/plants-repository-test/src/main/resources
```

---

### Task 7: Upload script fail-fast, full verify, enforcement gate

**Files:**
- Modify: `apps/management-console/src/main/resources/static/js/image-upload.js:53-70` — in `resizeImage`, add `img.onerror = function () { reject(new Error('This browser cannot read that photo. Choose a JPEG, PNG or WebP image.')); };` (make the promise `new Promise(function (resolve, reject) …)`); the existing `.catch` at `:126-130` already re-enables the button and alerts.

- [ ] **Step 1: Edit the script.**
- [ ] **Step 2: Manual check** (Review Focus 5): run the console (`mvn install -DskipTests` first, then the app), open `/insects/identify`, choose a `.HEIC` file on a desktop browser → alert appears, button re-enabled. Choose a `.jpg` → identification proceeds (or the budget/rate redirect, as before).
- [ ] **Step 3: Full verify**

Run: `mvn verify`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Enforcement gate**

Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS, no pending fixes.

- [ ] **Step 5: Stage and stop for review**

```bash
git add apps/management-console/src/main/resources/static/js/image-upload.js
git status
```
Report: files changed, the two suites run, and the reseed result. The user commits.
