# Photo sizes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** No stored photo exceeds 1024 px, every photo has a 640 px tile that every photo position
shows, and clicking a gallery photo opens it in a view over the page.

**Architecture:**
- The filesystem image store decodes each upload once, safely. It keeps the photo as is when
  it is 1024 px or smaller, and otherwise keeps a downscaled JPEG instead. It also writes a 640 px
  JPEG tile beside it.
- A read names its variant, `PHOTO` or `TILE`.
- A public `conform(space)` brings an existing store root into line. The seeder runs it on every
  start.
- The identification command checks that a photo can be stored before charging the budget.
- The consoles add a tile URL. The templates show tiles, with a client-side placeholder when a
  tile is missing and a `<dialog>` view for the photo.

**Tech Stack:** Java 25, JDK `javax.imageio`, TwelveMonkeys `imageio-webp` (read-only plugin),
Spring Boot 4 MVC, JTE, plain JS.

**Spec:** [`2026-11-11-photo-sizes-design.md`](2026-11-11-photo-sizes-design.md)

## Global Constraints

- **Photo:** at most 1024 px on its longest edge.
- **Tile:** 640 px on its longest edge. Never upscaled; a smaller photo's tile is the photo
  re-encoded.
- **Scaling arithmetic:** the shorter edge is `Math.round(short * edge / (double) long)`, so 3000 ×
  2000 becomes 1024 × 683 and its tile 640 × 427.
- **Re-encoded images:** JPEG at quality 0.85, flattened onto white, rotated upright from the Exif
  orientation, with no metadata at all.
- **Pass-through:** a stripped upload at or under 1024 px is stored byte for byte unchanged.
- **Oversized uploads:** the original is never kept anywhere.
- **Pixel ceiling:** 100,000,000 declared pixels, the same as `ImageDownscaler.MAX_PIXELS`. Above
  it, the upload is refused as invalid without being decoded.
- **Bounded decode:** use source subsampling, so the raster is never much larger than twice the
  target edge.
- **Missing tile:**
  - the store observes it at `WARN`;
  - the endpoint answers 404 with `Cache-Control: no-store`;
  - the page swaps in a placeholder;
  - it never falls back to the photo.
- **TwelveMonkeys version:** in root `pom.xml` `<properties>` only (`twelvemonkeys.version`). Pick
  a release at least two weeks old (check with `maven_available_versions`).
- **Unchanged:** `store` returns one `FileName`. `InsectImage.resourceName` and the plant image
  records are unchanged.
- **Repo rules:**
  - Observer for argument checks, never `requireNonNull`.
  - Java edits through Kaname engine atoms (hooks enforce this).
  - Never weaken a gate.
  - Commit only when Pat says "commit".
  - Each task ends staged, not committed.

## Review Focus

1. **A PNG with transparency becomes a white-backed JPEG,** never black. Test in Task 1.
2. **A portrait phone photo (orientation 6) has an upright tile,** with width < height after
   rotation. Test in Task 1.
3. **A JS-resized 1024 px JPEG upload is stored byte-identical** to the stripped upload. Test in
   Task 2.
4. **A tiny header declaring 50,000 × 50,000 px is refused without a decode,** and refused before
   the budget is charged. Tests in Tasks 1 and 4.
5. **A tile-less photo already in a root,** whether a seed, a local upload or a volume, gets its
   tile from `conform`, and running `conform` twice changes nothing. Test in Task 3.

---

### Task 1: `PhotoScaler`, the bounded decoder and encoder

**Files:**
- Create: `kernels/image-store-filesystem/src/main/java/com/naturalist/observation/filesystem/PhotoScaler.java` (package-private `final class`)
- Modify: `kernels/image-store-filesystem/pom.xml` — add `com.twelvemonkeys.imageio:imageio-webp`, runtime scope (an `ImageIO` SPI plugin, never referenced in code)
- Modify: root `pom.xml` — the `twelvemonkeys.version` property and a `dependencyManagement` entry
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/ImageUpload.java` — add `public Optional<Integer> orientation()`, mirroring `gps()` (`ImageMetadataSegments.exifTiff(...).flatMap(t -> ImageMetadataSegments.orientation(t, m))`)
- Create: `kernels/image-store-filesystem/src/test/resources/scaler/alpha.webp`, a real lossy-with-alpha WebP of about 64 × 48 px, under 4 KB. Generate it with `cwebp` or Pillow and say which in the commit message. The JDK can't write WebP.
- Test: `kernels/image-store-filesystem/src/test/java/com/naturalist/observation/filesystem/PhotoScalerTest.java`
- Test: add `orientation_readsTheExifTag` to `kernels/observation/src/test/java/com/naturalist/observation/ImageUploadTest.java`, using `ImageFixtures.app1Exif(BIG_ENDIAN, 6, null, null)`

**Interfaces:**
- Produces, in `PhotoScaler`:
  - `static final int PHOTO_EDGE = 1024`
  - `static final int TILE_EDGE = 640`
  - `static final long MAX_PIXELS = 100_000_000L`
  - `record Dimensions(int width, int height)`, with `long pixels()` and `int longestEdge()`
  - `static Optional<Dimensions> declared(byte[] bytes)`: reads the header only. Empty when no `ImageIO` reader accepts the bytes.
  - `static boolean storable(byte[] bytes)`: `declared` is present and `pixels() <= MAX_PIXELS`.
  - `static byte[] fitJpeg(byte[] bytes, int edge, int orientation)`: decodes with subsampling, rotates and flips by Exif `orientation` (1–8; anything else is treated as 1), scales so the longest edge is `min(edge, its own)`, flattens onto white, and encodes as JPEG at 0.85 with no metadata. Throws `IllegalArgumentException` when `!storable(bytes)`.

- [ ] **Step 1: Write the failing tests.** Build the test images in the test with `ImageIO.write` on a `BufferedImage`, which needs no fixtures. For orientation, splice `ImageFixtures.app1Exif(BIG_ENDIAN, 6, null, null)` in right after the SOI of an `ImageIO`-written JPEG.

```java
@Test void fitJpeg_scalesTheLongestEdgeTo640() { /* 2000x1500 JPEG → decoded result 640x480 */ }
@Test void fitJpeg_neverUpscales() { /* 300x200 PNG, edge 640 → 300x200 JPEG */ }
@Test void fitJpeg_appliesOrientation6() { /* 800x600 + orientation 6 → result width < height, 480x640 */ }
@Test void fitJpeg_flattensTransparencyOntoWhite() { /* 10x10 fully transparent ARGB PNG → pixel (5,5) RGB 0xFFFFFF */ }
@Test void fitJpeg_writesNoMetadata() { /* result: ImageUpload.of(r).withoutMetadata() equals ImageUpload.of(r); orientation() empty */ }
@Test void fitJpeg_decodesWebp() { /* scaler/alpha.webp → a JPEG whose declared() is 64x48 */ }
@Test void declared_readsDimensionsWithoutDecoding() { /* 2000x1500 → Dimensions(2000,1500) */ }
@Test void storable_refusesADeclaredBomb() { /* a PNG whose IHDR declares 50000x50000 (hand-built header + IDAT stub) → false, and fitJpeg throws IllegalArgumentException */ }
@Test void storable_refusesUndecodableBytes() { /* ImageFixtures JPEG magic + junk → false */ }
```

- [ ] **Step 2: Run them and confirm they fail.** Run `mvn -q test -pl kernels/image-store-filesystem -Dtest=PhotoScalerTest`. Expected: compilation failure, because there is no `PhotoScaler` yet.
- [ ] **Step 3: Implement `PhotoScaler` and `ImageUpload.orientation()`.**
  - Follow `adapters/anthropic-vision/.../ImageDownscaler.java` for the reader, `ImageReadParam.setSourceSubsampling`, `MemoryCacheImageInputStream` and the JPEG writer.
  - Derive the subsampling factor as `max(1, longestEdge / (2 * edge))`.
  - Apply the orientation transform after decoding and before the final scale.
  - Use `Graphics2D` with bilinear interpolation for the final scale.
- [ ] **Step 4: Run the tests and confirm they pass.** Run `mvn -q test -pl kernels/observation,kernels/image-store-filesystem`. Expected: BUILD SUCCESS.
- [ ] **Step 5: Stage the changes.** Run `git add` on the files above.

---

### Task 2: Port variants, store-time sizing, the store's write strategy

**Files:**
- Create: `kernels/observation/src/main/java/com/naturalist/observation/ImageVariant.java`
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/ImageStore.java`
- Modify: `kernels/image-store-filesystem/src/main/java/com/naturalist/observation/filesystem/FilesystemImageStore.java`
- Modify: `apps/management-console/src/main/java/com/naturalist/console/resilience/ResilienceConfiguration.java` — register the write strategy, and update the class javadoc
- Modify tests: `apps/management-console/src/test/java/com/naturalist/console/resilience/ResilienceConfigurationTest.java` (`bulkheadNames` gains `image.store.write`), and `apps/management-console/src/test/java/com/naturalist/console/admin/AdminResilienceControllerWebMvcTest.java` (the bulkhead and timeout sections contain `image.store.write`)
- Test: `kernels/image-store-filesystem/src/test/java/com/naturalist/observation/filesystem/FilesystemImageStoreTest.java`

**Interfaces:**
- Consumes: `PhotoScaler` and `ImageUpload.orientation()`, from Task 1.
- Produces:
  - `public enum ImageVariant { PHOTO, TILE }`
  - On `ImageStore`:
    - `Optional<StoredImage> read(String space, FileName name, ImageVariant variant)`
    - `default Optional<StoredImage> read(String space, FileName name)`, which returns `read(space, name, ImageVariant.PHOTO)`
    - `void requireStorable(ImageUpload upload)`, which throws `InvariantViolationException` (via the Observer, `"storable"`) when the upload can't be decoded or exceeds the pixel ceiling, and writes nothing
  - The `ImageStore` javadoc states the 1024 px and 640 px guarantees.
  - `FilesystemImageStore.WRITE_STRATEGY = "image.store.write"`
  - A tile lives at `<root>/<space>/tiles/<name>`: the photo's own file name, holding JPEG bytes. Its media type is sniffed, as today.

- [ ] **Step 1: Write the failing tests** in `FilesystemImageStoreTest`. Build images with `ImageIO`, as in Task 1.

```java
@Test void store_keepsAPhotoWithin1024ByteForByte() { /* 1024x768 JPEG → read(PHOTO).readAllBytes() equals upload.withoutMetadata().bytes() */ }
@Test void store_downscalesAnOversizedPhotoAndKeepsNoOriginal() { /* 3000x2000 → PHOTO declares 1024x683; space dir holds exactly 1 regular file + tiles/ */ }
@Test void store_writesA640Tile() { /* 3000x2000 → read(TILE) declares 640x427, media type image/jpeg */ }
@Test void store_namesAReencodedPngJpg() { /* 2000x2000 PNG → returned FileName ends ".jpg" */ }
@Test void store_refusesAnUnstorableUploadAndWritesNothing() { /* declared bomb → InvariantViolationException; space dir absent or empty */ }
@Test void delete_removesPhotoAndTile() { /* both reads empty after delete */ }
@Test void read_missingTileIsEmpty() { /* write a photo file directly into root, no tile → read(TILE) empty */ }
@Test void requireStorable_refusesABombAndAcceptsAPhoto() { /* bomb throws InvariantViolationException; 800x600 JPEG does not */ }
@Test void store_runsUnderTheWriteBulkhead() { /* rejecting-bulkhead Resilience (proxy, as read_propagatesTheBulkheadRefusal) → store throws BulkheadFullException, nothing written */ }
```

  Also update every existing test that writes the 16-byte synthetic `JPEG` through `store`. Those bytes don't decode, so `store` now refuses them. Switch them to an `ImageIO`-built JPEG. Tests that write files straight into the root, such as the traversal, octet-stream and sniff tests, stay as they are.

- [ ] **Step 2: Run them and confirm they fail.** Run `mvn -q test -pl kernels/image-store-filesystem`. Expected: compilation failure, because `ImageVariant` and `requireStorable` don't exist yet.
- [ ] **Step 3: Implement.**
  - `store`:
    1. Strip as today.
    2. Check `PhotoScaler.storable` through the Observer.
    3. Work out the photo bytes: the stripped bytes when `longestEdge <= 1024`, otherwise `fitJpeg(stripped, 1024, orientation)`.
    4. Make the tile with `fitJpeg(stripped, 640, orientation)`.
    5. Mint the name from the photo bytes' extension.
    6. Write the photo, then the tile, under `resilience.bulkhead(WRITE_STRATEGY)` wrapping `resilience.timeout(WRITE_STRATEGY)`.
    7. On any failure, delete both files and rethrow.
  - Annotate `store` with method-level `@Resilient(name = WRITE_STRATEGY)`, mirroring how `AnthropicVisionService` declares an upload strategy.
  - `read(TILE)` resolves `tiles/<name>`. When it is absent, `m.state(s -> s.isTrue(false, "tilePresent")).observe(Level.WARN)`, then return empty.
  - `delete` removes both files.
  - In `ResilienceConfiguration`, add the beans `new BulkheadConfig(WRITE_STRATEGY, 2, Duration.ofSeconds(2))` and `new TimeoutConfig(WRITE_STRATEGY, Duration.ofSeconds(10), true)`.
- [ ] **Step 4: Run the tests and confirm they pass.** Run `mvn -q install -DskipTests -pl kernels/image-store-filesystem,apps/management-console -am`, then `mvn -q test -pl kernels/observation,kernels/image-store-filesystem,domains/insects/insects-core,domains/insects/insects-console,domains/plants/plants-console,apps/management-console`. Expected: BUILD SUCCESS.
  - The insects-core `store` callers' tests use `ImageFixtures` bytes. Where those no longer decode, switch them to an `ImageIO`-built JPEG. Never loosen an assertion.
- [ ] **Step 5: Stage the changes.**

---

### Task 3: `conform(space)` backfill, seeder wiring, seed photo rewrite

**Files:**
- Modify: `FilesystemImageStore.java`. Add `public ConformReport conform(String space)` and `public record ConformReport(int downscaled, int tiled, int unchanged)`.
- Modify: `apps/test-db-seeder/pom.xml`. Add `image-store-filesystem` (compile) and `framework` if it isn't already transitive.
- Modify: `apps/test-db-seeder/src/main/java/com/naturalist/seeder/Seeding.java`. Add `static void conformPhotos()`, which runs `conform("insects")` and `conform("plants")` against `naturalist.images.root` (default `data/images`) with `Resilience.noOp()`, and prints each report.
- Modify: `DeploymentDbInitializer.main`. Call `Seeding.conformPhotos()` after the `switch`, for `EMPTY` and `INITIALIZED` alike. `NOT_EMPTY` still throws first.
- Modify: `TestDbSeeder.main`. Call `Seeding.conformPhotos()` after loading.
- Modify (binary): the 18 seed photos over 1024 px under `domains/insects/insects-repository-test/src/main/resources/insects/images/` and `domains/plants/plants-repository-test/src/main/resources/plants/images/`.
- Test: `FilesystemImageStoreTest`
- Test: `apps/management-console/src/test/java/com/naturalist/console/images/SeedImagesCarryNoMetadataTest.java`. Add `seedImagesAreWithinThePhotoLimit`: `ImageIO` header dimensions with longest edge `<= 1024` for every `classpath*:{insects,plants}/images/*`.

**Interfaces:**
- Consumes: Task 2's `store` pipeline. `conform` reuses the same private sizing methods.
- Produces: `FilesystemImageStore.conform(String space)`. It is idempotent. It handles only the regular files directly in `<root>/<space>/`, never `tiles/`. A photo over 1024 px is rewritten in place under the same name, and a missing tile is written.

- [ ] **Step 1: Write the failing tests.**

```java
@Test void conform_downscalesInPlaceAndTiles() { /* 2048x1536 file written directly → report(1,1,0); PHOTO 1024x768 same name; TILE 640x480 */ }
@Test void conform_tilesAPhotoWithinTheLimitWithoutTouchingIt() { /* 1024x768 → bytes unchanged, report(0,1,0) */ }
@Test void conform_isIdempotent() { /* second run → report(0,0,n) and every file's bytes unchanged */ }
@Test void conform_skipsAnUndecodableFile() { /* junk.jpg → left alone, counted unchanged, observed WARN; others still processed */ }
```

  Plus `seedImagesAreWithinThePhotoLimit`, which fails until Step 4.
- [ ] **Step 2: Run them and confirm they fail.** Run `mvn -q test -pl kernels/image-store-filesystem -Dtest=FilesystemImageStoreTest`. Expected: FAIL, because `conform` doesn't exist yet.
- [ ] **Step 3: Implement `conform`, `ConformReport` and the seeder wiring.**
- [ ] **Step 4: Rewrite the seed photos once.**
  1. Copy both `images/` directories into `temp/seed-conform/{insects,plants}/`.
  2. Run `conform` on that root from `jshell`, with the classpath from `mvn -q dependency:build-classpath -pl kernels/image-store-filesystem -Dmdep.outputFile=temp/cp.txt` plus `kernels/image-store-filesystem/target/classes`.
  3. Copy only the rewritten photos back. Don't copy `tiles/`: tiles are never checked in.
  4. Confirm that `git status` shows exactly 18 modified binaries.
- [ ] **Step 5: Run the tests and confirm they pass.** Run `mvn -q install -DskipTests -pl apps/test-db-seeder -am`, then `mvn -q test -pl kernels/image-store-filesystem,apps/management-console -Dtest='FilesystemImageStoreTest,SeedImagesCarryNoMetadataTest'`. Expected: PASS. Both seed tests are green: no metadata, and within 1024.
- [ ] **Step 6: Stage the changes.**

---

### Task 4: Identification checks the photo before charging

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommandImpl.java`, `identify`, around line 185. Right after the `strippedUpload` state check and before the rate gate (`// 0a`), add `imageStore.requireStorable(stripped);` with a comment: `// 0. STORABLE -- a photo the store cannot use is refused before any rate, budget or vision spend.`
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectIdentificationCommandImplTest.java`

**Interfaces:**
- Consumes: `ImageStore.requireStorable(ImageUpload)`, from Task 2.
- Produces: an unstorable upload throws `InvariantViolationException` from `identify`. The controller already maps that to `?error=image`. `InsectsControllerTest` or the WebMvc tests cover it; check, and add `identify_redirectsWithErrorImage_whenThePhotoIsUnstorable` if nothing does.

- [ ] **Step 1: Write the failing test.**

```java
@Test void identify_refusesAnUnstorablePhotoBeforeChargingTheBudget() {
    // a JPEG-magic upload whose header declares 50000x50000 (valid ImageUpload, unstorable)
    assertThatThrownBy(() -> command.identify(bomb, metadata, naturalist, null, null))
            .isInstanceOf(InvariantViolationException.class);
    // budget untouched: the recording/real budget shows no reservation for naturalist
    // vision never called: the scripted VisionService records zero calls
    // nothing stored: the store root's insects/ dir is absent or empty
}
```

  Use the test's existing budget and vision fakes, and the same assertion style as the existing `BUDGET_EXHAUSTED` and `RATE_LIMITED` tests.
- [ ] **Step 2: Run it and confirm it fails.** Run `mvn -q test -pl domains/insects/insects-core -Dtest=InsectIdentificationCommandImplTest#identify_refusesAnUnstorablePhotoBeforeChargingTheBudget`. Expected: FAIL, because the budget is reserved before `store` throws.
- [ ] **Step 3: Add the call** described under Files.
- [ ] **Step 4: Run the tests and confirm they pass.** Run `mvn -q test -pl domains/insects/insects-core,domains/insects/insects-console`. Expected: BUILD SUCCESS.
- [ ] **Step 5: Stage the changes.** In the task report, state that an infrastructure refusal from `store` (bulkhead, timeout, I/O) still happens after the reservation. That is the command's existing behaviour for failures after `0c`.

---

### Task 5: Tile endpoints

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java` and `domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantsController.java`.
  - Add `@GetMapping("/images/tile/{filename:.+}") ResponseEntity<Resource> tile(@PathVariable String filename)`.
  - Extract one private `serve(String space, String filename, ImageVariant variant)` that both `image` and `tile` call.
  - A 404 carries `Cache-Control: no-store` for both variants.
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/InsectsCatalogWebMvcTest.java` and `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsImagesWebMvcTest.java`
- Test: `InsectsControllerBusyTest`. Add `tile_isServiceUnavailable_whenTheBulkheadRejects`.

**Interfaces:**
- Consumes: `ImageStore.read(space, name, ImageVariant)`.
- Produces: `GET /insects/images/tile/{filename}` and `GET /plants/images/tile/{filename}`. On success they send 200 with `image/jpeg`, `Content-Length` and `max-age=31536000, public, immutable`. A missing tile is a 404 with `no-store`. A full bulkhead is a 503 with `Retry-After: 1`.

- [ ] **Step 1: Write the failing tests.**

```java
@Test void tile_servesTheStoredTile() { /* store an ImageIO 2000x1500 JPEG → GET tile 200, image/jpeg, immutable, body declares 640x480 */ }
@Test void tile_missingIs404NoStore() { /* GET /…/images/tile/nope.jpg → 404, Cache-Control no-store */ }
@Test void image_missingIs404NoStore() { /* existing missing-photo test gains the no-store assertion */ }
```

  Then update `image_servesStoredUpload` and `PlantsImagesWebMvcTest`'s `TINY_JPEG` to use a decodable `ImageIO`-built JPEG, since the synthetic bytes are now refused by `store`.
- [ ] **Step 2: Run them and confirm they fail.** Run `mvn -q test -pl domains/insects/insects-console,domains/plants/plants-console`. Expected: FAIL with 404 on the tile URL.
- [ ] **Step 3: Implement `tile` and `serve`** in both controllers.
- [ ] **Step 4: Run the tests and confirm they pass.** Use the same command as Step 2. Expected: BUILD SUCCESS.
- [ ] **Step 5: Stage the changes.**

---

### Task 6: Templates, placeholder, the photo view

**Files:**
- Create: `apps/management-console/src/main/resources/static/images/photo-not-found.svg`. A neutral sepia frame with the text "Photo not found", 4:3, under 2 KB, styled to match `naturalist.css`'s `--sepia-rule`.
- Create: `apps/management-console/src/main/resources/static/js/photo-view.js`.
- Modify: `apps/management-console/src/main/jte/layout/page.jte`. Add `<script src="/js/photo-view.js" defer></script>` beside `card-image-rotate.js`. A plain tag only: page.jte may reference spring-web types only.
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css`. Style `dialog.photo-view`, with the image limited to 90vw by 90vh, a close button, and a `::backdrop`.
- Modify: `domains/insects/insects-console/src/main/jte/insects/cardImages.jte`. `src="/insects/images/tile/${…value()}"` plus `onerror="this.onerror=null;this.src='/images/photo-not-found.svg'"`. The card link is unchanged.
- Modify: `domains/insects/insects-console/src/main/jte/insects/observationGallery.jte` and `domains/plants/plants-console/src/main/jte/plants/gallery.jte`. Wrap the tile `<img>`, with the same `onerror`, in `<a href="/{space}/images/${name}" data-photo-view>`.
- Test: `InsectsCatalogWebMvcTest` or the existing rank-page WebMvc test, plus a plants page test.

**Interfaces:**
- Consumes: Task 5's URLs.
- Produces: `photo-view.js`.
  - It creates one `<dialog class="photo-view">` lazily, holding an `<img>` and a `<button type="button" aria-label="Close">`.
  - Clicking an `a[data-photo-view]` calls `preventDefault`, sets `img.src = a.href` and `img.alt` from the tile's `alt`, then calls `showModal()`.
  - The dialog closes on Escape (native), on a click whose target is the dialog itself (the backdrop), and on the button.
  - Without JavaScript the link opens the photo URL.

- [ ] **Step 1: Write the failing render tests.**

```java
@Test void rankPage_showsTilesNotPhotos() { /* GET a rank page with seeded images → HTML contains "/insects/images/tile/", contains "photo-not-found.svg", and no <img src="/insects/images/<name>" without /tile/ */ }
@Test void gallery_linksEachTileToItsPhoto() { /* HTML contains data-photo-view and href="/insects/images/<name>" */ }
@Test void plantPage_showsTilesLinkedToPhotos() { /* same two checks on a plant page with images */ }
```

- [ ] **Step 2: Run them and confirm they fail.** Run `mvn -q test -pl domains/insects/insects-console,domains/plants/plants-console`. Expected: FAIL, because there are no tile URLs yet.
- [ ] **Step 3: Implement** the templates, SVG, JS and CSS.
- [ ] **Step 4: Run the tests and confirm they pass.** Use the same command as Step 2, then `mvn -q verify -pl apps/management-console`. That includes `PackagedJarIT`, so the template gather has to pass. Expected: BUILD SUCCESS.
- [ ] **Step 5: Check it by hand in the running app,** via `spring-boot:run` or the packaged jar, after running `TestDbSeeder`.
  - A rank page loads tiles.
  - Clicking a gallery photo opens the dialog, and Escape, a backdrop click and the button all close it.
  - Rename one tile file away and reload: the placeholder shows, the card layout holds, and the cycling still runs.
- [ ] **Step 6: Stage the changes.**

---

### Task 7: Docs and the full gate

**Files:**
- Modify: `kernels/CLAUDE.md` (the observation `ImageStore` section): variants, the 1024 and 640 guarantees, `requireStorable`, `conform`, `image.store.write`, and tile layout.
- Modify: `docs/briefings/vision-identification.md` ("Image serving"): the tile URL and the placeholder.
- Modify: `docs/plans/2026-11-05-public-launch-work-plan.md`. Add row 1.7, "**new** Photo sizes", linking the design and this plan, marked done with the date.
- Modify: `domains/insects/CLAUDE.md`, only if a statement there about photo serving is now wrong.

- [ ] **Step 1: Update the docs** listed above.
- [ ] **Step 2: Run the full gate.**
  - `mvn install`, then expect BUILD SUCCESS.
  - `mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`, then expect BUILD SUCCESS.
  - The credential grep from the root `CLAUDE.md`, then expect no output.
- [ ] **Step 3: Check the size result by hand** against the packaged jar, with `-Xmx1152m` and the PL-16 command from the 1.6 notes.
  - Upload a 4032 px photo through a direct POST, without the JS.
  - The stored photo is 1024 px and its tile is 640 px.
  - `curl -sI` on the tile shows a `Content-Length` well under 150 KB.
- [ ] **Step 4: Stage everything and report.** Pat commits.
