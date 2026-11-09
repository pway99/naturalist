# Photo Location Privacy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** No stored or served photo carries metadata; an observation may carry a 2.5′ grid cell as its area, used for identification and shown publicly; exact coordinates never persist and never leave the device when JavaScript runs.

**Architecture:** `GridCell` and the EXIF read/strip are pure functions in `kernels/observation`. The insects commands own the flow (strip first, store, send to vision with the area sentence, persist the area); the controller only resolves the area from posted cell indices or EXIF and stays thin; `FilesystemImageStore.store` strips again as the storage-boundary guarantee. The browser mirrors two lines of floor arithmetic to show the area and post only the cell.

**Tech Stack:** Java 25, Maven reactor, JUnit 5 + AssertJ, `NaturalistTestExtension` / `@Wired` / `@Fake`, MyBatis + Postgres, JTE, plain JavaScript, ArchUnit.

**Spec:** [`docs/plans/2026-11-08-photo-location-privacy-design.md`](2026-11-08-photo-location-privacy-design.md)

## Global Constraints

- No new third-party dependency. EXIF read and segment/chunk rewrite are hand-written.
- No transcoding: image data bytes (JPEG entropy-coded data, PNG `IDAT`, WebP `VP8*` chunks) are copied unchanged.
- Cell lattice is 1/24° (`floor(deg × 24)`); lat index in `[-2160, 2159]`, lon index in `[-4320, 4319]`.
- `Coordinates` is never a component of an `Entity`, `Aggregate` or `ReadModel`.
- Argument checks through `Observer` (`observer.arguments(...)`); a producer handing out a value (`gps()`) observes a violation and returns empty rather than throwing.
- Versions only in root `pom.xml`; never weaken a gate; `*-repository-test` mocks validate domain-specific method arguments.
- Commit after each task. Commands as the plan shows; Pat pushes.

## Review Focus

1. A JPEG whose segment walk hits a malformed length (the test suites' `TINY_JPEG`: `FF D8 FF E0 00 00…`) — `withoutMetadata()` must return it byte-identical and `gps()` must return empty, not throw. (Task 3, Task 2)
2. A JPEG with *two* `APP1` segments (Exif then XMP, as phones emit) — both removed, orientation taken from the Exif one. (Task 3)
3. A WebP with `EXIF` chunk and an odd-length chunk payload (RIFF pads to even) — RIFF size and `VP8X` flags correct after removal. (Task 3)
4. Posted `areaLat`/`areaLon` present but not integers, or only one of the two — treated as no posted cell, fall through to EXIF. (Task 8)
5. A cell straddling the equator or antimeridian edge (`latIndex = -1`, `lonIndex = 4319`) — `describe()` prints sensible hemisphere letters and `bounds()` stays within ±90/±180. (Task 1)

---

### Task 1: `GridCell` and `Coordinates`

**Files:**
- Create: `kernels/observation/src/main/java/com/naturalist/observation/GridCell.java`
- Create: `kernels/observation/src/main/java/com/naturalist/observation/Coordinates.java`
- Test: `kernels/observation/src/test/java/com/naturalist/observation/GridCellTest.java`

**Interfaces:**
- Produces:
  - `record Coordinates(double latitude, double longitude) implements ValueObject` — invariants: `latitude` in `[-90, 90]`, `longitude` in `[-180, 180]`.
  - `record GridCell(int latIndex, int lonIndex) implements ValueObject`
    - `static GridCell of(Coordinates c)` and `static GridCell of(double latitude, double longitude)` → `floor(x × 24)`; latitude 90.0 clamps to index 2159, longitude 180.0 to 4319.
    - `static Optional<GridCell> ofIndices(int latIndex, int lonIndex)` → empty when out of range (observed, not thrown).
    - `record Bounds(double south, double north, double west, double east)`; `Bounds bounds()`.
    - `GridCell quadrangle()` → `(Math.floorDiv(latIndex, 3), Math.floorDiv(lonIndex, 3))` (the 7.5′ quad, still a `GridCell` by index on the coarser lattice).
    - `String describe()` → `"%.2f–%.2f°%s, %.2f–%.2f°%s"` (`Locale.ROOT`): latitude south edge then north edge as absolute values with `N` when `south >= 0` else `S`; longitude west edge then east edge as absolute values with `E` when `west >= 0` else `W`. Portland → `45.50–45.54°N, 122.71–122.67°W`.
    - invariants: index ranges.

- [ ] **Step 1: Write the failing tests**

```java
class GridCellTest {
    @Test void portland_snapsToCell1092_minus2945() {
        var cell = GridCell.of(45.52, -122.68);
        assertThat(cell).isEqualTo(new GridCell(1092, -2945));
        assertThat(cell.bounds().south()).isCloseTo(45.5, within(1e-9));
        assertThat(cell.bounds().north()).isCloseTo(45.5 + 1.0 / 24, within(1e-9));
        assertThat(cell.bounds().west()).isCloseTo(-2945.0 / 24, within(1e-9));
        assertThat(cell.describe()).isEqualTo("45.50–45.54°N, 122.71–122.67°W");
    }
    @Test void southernWestern_pointHasNegativeIndices_andSWLetters()      // -33.87, 151.21 (Sydney) → S, E; -22.9, -43.2 → S, W
    @Test void exactBoundary_belongsToTheCellItOpens()                    // of(45.5, -122.75) == new GridCell(1092, -2946)
    @Test void poles_andAntimeridian_clampToTheLastCell()                 // of(90, 180) == (2159, 4319); of(-90, -180) == (-2160, -4320)
    @Test void equatorStraddle_describesHemisphereBySouthEdge()           // new GridCell(-1, 0).describe() startsWith "0.04–0.00°S, 0.00–0.04°E"
    @Test void quadrangle_nestsThreeByThree_includingAcrossZero()         // cells (0,0),(1,0),(2,0) → quad (0,0); (-1,0) → (-1,0); (1092,-2945) → (364,-982)
    @Test void ofIndices_outOfRange_isEmpty()                             // ofIndices(2160, 0), ofIndices(0, -4321) → empty; ofIndices(0,0) → present
    @Test void coordinates_outOfRange_failInvariants()                    // new Coordinates(91, 0).observer()... isNotValid (see ImageUploadTest for the idiom)
    @Test void gridCell_roundTripsThroughJackson()                        // new ObjectMapper().writeValueAsString / readValue → equal
}
```

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q -o test -pl kernels/observation -Dtest=GridCellTest`
Expected: compilation failure, `GridCell` not found.

- [ ] **Step 3: Implement `Coordinates` and `GridCell`** in `kernels/observation` as specified in Interfaces. Javadoc on `GridCell`: the 2.5′ lattice, the USGS 7.5′ nesting, and that it is the only form of location the app keeps.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -o test -pl kernels/observation -Dtest=GridCellTest`
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add kernels/observation
git commit -m "feat(observation): GridCell, the 2.5-minute cell an observation may carry as its area"
```

---

### Task 2: `ImageUpload.gps()` — EXIF GPS reader

**Files:**
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/ImageUpload.java`
- Create: `kernels/observation/src/main/java/com/naturalist/observation/ImageMetadataSegments.java` (package-private helper: locates the TIFF block per format, walks IFDs)
- Create: `kernels/observation/src/test/java/com/naturalist/observation/ImageFixtures.java` (test helper that assembles images byte by byte)
- Test: `kernels/observation/src/test/java/com/naturalist/observation/ImageUploadGpsTest.java`

**Interfaces:**
- Produces: `Optional<Coordinates> ImageUpload.gps()`.
- Produces (test helper, reused by Tasks 3, 4, 7, 8):
  - `static byte[] ImageFixtures.jpeg(byte[]... segments)` → `FF D8` + segments + `FF DA 00 02` + 4 data bytes + `FF D9`.
  - `static byte[] ImageFixtures.app0Jfif()`, `app1Exif(ByteOrder order, Integer orientation, Double lat, Double lon)` (builds TIFF header, IFD0 with optional `0x0112` and `0x8825` GPS pointer, GPS IFD with tags 1–4 as rationals), `app1Xmp()`, `app2Icc()`, `app13Iptc()`, `app14Adobe()`, `com(String)`.
  - `static byte[] ImageFixtures.png(byte[]... chunks)` with `chunk(String type, byte[] data)` computing CRC, plus `ihdr()`, `idat()`, `iend()`, `exif(tiffBytes)`, `text(key, value)`.
  - `static byte[] ImageFixtures.webp(boolean exif, boolean xmp)` → RIFF/WEBP with `VP8X` flags, a minimal `VP8L` chunk, optional `EXIF`/`XMP ` chunks (odd-length payload for one of them).
  - `static byte[] ImageFixtures.tiff(ByteOrder, Integer orientation, Double lat, Double lon)` shared by the three.
  - Package-private: `static Optional<byte[]> ImageMetadataSegments.exifTiff(byte[] image)` (the TIFF payload of a JPEG Exif `APP1`, PNG `eXIf` or WebP `EXIF`), `static Optional<Integer> orientation(byte[] tiff)`, `static Optional<Coordinates> gps(byte[] tiff)`.

- [ ] **Step 1: Write the failing tests**

```java
class ImageUploadGpsTest {
    @Test void jpeg_bigEndianExif_readsNorthWest()        // app1Exif(BIG_ENDIAN, null, 45.52, -122.68) → Coordinates within 1e-6
    @Test void jpeg_littleEndianExif_readsSouthEast()     // LITTLE_ENDIAN, -33.87, 151.21
    @Test void jpeg_exifWithoutGpsIfd_isEmpty()           // app1Exif(BIG_ENDIAN, 6, null, null)
    @Test void jpeg_xmpOnly_isEmpty()                     // app1Xmp() only
    @Test void jpeg_truncatedApp1_isEmpty_andObserved()   // app1 whose declared length exceeds the array → empty; no exception
    @Test void jpeg_garbageTiff_isEmpty()                 // "Exif\0\0" followed by random bytes
    @Test void tinyJpegFromTheSuites_isEmpty()            // FF D8 FF E0 00 00 + 10 zero bytes
    @Test void png_eXIfChunk_reads()                      // png(ihdr(), exif(tiff(...)), idat(), iend())
    @Test void webp_exifChunk_reads()                     // webp(true, false) carrying tiff with gps
    @Test void noMetadata_isEmpty()                       // jpeg(app0Jfif())
}
```

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q -o test -pl kernels/observation -Dtest=ImageUploadGpsTest`
Expected: compilation failure, `gps()` / `ImageFixtures` not found.

- [ ] **Step 3: Implement `ImageFixtures`, `ImageMetadataSegments`, and `ImageUpload.gps()`**

`gps()` = `ImageMetadataSegments.exifTiff(bytes).flatMap(ImageMetadataSegments::gps)`; any parse failure inside is caught at the segment layer, observed as a `gps` violation on `Observer.forClass(ImageUpload.class)` at `Level.DEBUG`, and yields empty. GPS decoding: tags `1` (`N`/`S`), `2` (3 rationals), `3` (`E`/`W`), `4` (3 rationals); degrees + minutes/60 + seconds/3600, negated for `S`/`W`. Segment walk for JPEG: from offset 2, read marker and 16-bit length; stop at `SOS` (`FF DA`) or at any marker that is not `FF xx`, any length `< 2`, or any segment running past the array.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -o test -pl kernels/observation`
Expected: all observation kernel tests pass, including the existing `ImageUploadTest`.

- [ ] **Step 5: Commit**

```bash
git add kernels/observation
git commit -m "feat(observation): read EXIF GPS from an upload, for the JavaScript-off path"
```

---

### Task 3: `ImageUpload.withoutMetadata()` and the storage-boundary strip

**Files:**
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/ImageUpload.java`
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/ImageMetadataSegments.java`
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/ImageStore.java` (Javadoc contract)
- Modify: `kernels/image-store-filesystem/src/main/java/com/naturalist/observation/filesystem/FilesystemImageStore.java` (`store` applies `upload.withoutMetadata()`)
- Test: `kernels/observation/src/test/java/com/naturalist/observation/ImageUploadStripTest.java`
- Test: `kernels/image-store-filesystem/src/test/java/com/naturalist/observation/filesystem/FilesystemImageStoreTest.java` (add one test)

**Interfaces:**
- Consumes: `ImageFixtures`, `ImageMetadataSegments.orientation` (Task 2).
- Produces: `ImageUpload ImageUpload.withoutMetadata()`; `static byte[] ImageMetadataSegments.orientationOnlyApp1(int orientation)` (the write-back segment: `FF E1`, length, `Exif\0\0`, big-endian TIFF `MM 00 2A 00 00 00 08`, IFD0 count 1, entry `0112 0003 00000001 <value><0000>`, next-IFD `00000000`).

- [ ] **Step 1: Write the failing tests**

```java
class ImageUploadStripTest {
    @Test void jpeg_dropsExifXmpIptcAndComments_keepsJfifIccAdobe()
        // in: jpeg(app0Jfif(), app1Exif(BE, null, 45.5, -122.7), app1Xmp(), app2Icc(), app13Iptc(), app14Adobe(), com("x"))
        // out: segments present == [APP0, APP2, APP14]; out.gps() isEmpty; entropy data (from SOS to EOI) byte-identical
    @Test void jpeg_keepsOrientationAsTheOnlyExifTag()
        // in: app1Exif(LE, 6, 45.5, -122.7) → out has exactly one APP1 == orientationOnlyApp1(6); out.gps() isEmpty
    @ParameterizedTest @ValueSource(ints = {1,2,3,4,5,6,7,8}) void jpeg_everyOrientationValueSurvives(int o)
        // orientation 1 → no APP1 at all (the default); 2..8 → orientationOnlyApp1(o)
    @Test void jpeg_twoApp1Segments_bothRemoved_orientationFromTheExifOne()
    @Test void jpeg_isIdempotent()                  // strip(strip(x)) == strip(x)
    @Test void jpeg_withoutMetadata_isUnchanged()   // jpeg(app0Jfif()) → identical bytes, same instance not required
    @Test void tinyJpegFromTheSuites_isUnchanged()  // FF D8 FF E0 00 00 ... → identical
    @Test void png_dropsExifAndTextChunks_keepsTheRest()
        // in: png(ihdr(), exif(..), text("Comment","x"), chunk("zTXt",..), chunk("iTXt",..), idat(), iend()) → chunks == [IHDR, IDAT, IEND]; CRCs unchanged
    @Test void webp_dropsExifAndXmp_fixesRiffSizeAndVp8xFlags()
        // in: webp(true, true) → no EXIF/XMP chunks; RIFF size == length-8; VP8X flags & 0b1100 == 0; VP8L chunk identical
    @Test void webp_oddLengthChunk_paddingHandled()  // the fixture's odd-length chunk removed cleanly, next chunk intact
    @Test void mediaType_andValidity_preservedAfterStrip()  // for all three formats
}
```

In `FilesystemImageStoreTest`:

```java
@Test void store_stripsMetadata_readReturnsNone() {
    var name = store.store("insects", ImageUpload.of(ImageFixtures.jpeg(ImageFixtures.app0Jfif(), ImageFixtures.app1Exif(BIG_ENDIAN, 6, 45.5, -122.7))));
    var stored = store.read("insects", name).orElseThrow();
    var upload = ImageUpload.of(stored.bytes());
    assertThat(upload.gps()).isEmpty();
    assertThat(upload.withoutMetadata()).isEqualTo(upload);   // already stripped
}
```

(`ImageFixtures` is in `kernels/observation`'s test sources. Expose it by attaching a `test-jar` from `kernels/observation` (`maven-jar-plugin` `test-jar` goal in its pom) and adding it as a `<type>test-jar</type>` test-scoped dependency here, and later in `insects-core` (Task 7) and `insects-console` (Task 8). `pom.xml` changes only, no version literals.)

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q -o test -pl kernels/observation -Dtest=ImageUploadStripTest`
Expected: compilation failure, `withoutMetadata()` not found.

- [ ] **Step 3: Implement**

`withoutMetadata()` by `mediaType()`: JPEG — walk segments (same stop rules as Task 2); copy every segment except `APP1`, `APP13`, `COM`; remember the orientation from the first Exif `APP1` via `ImageMetadataSegments.orientation`; if orientation present and `!= 1`, insert `orientationOnlyApp1(o)` right after `APP0` (or after `SOI` when there is no `APP0`); copy the remainder from the stop point unchanged. PNG — copy signature, then chunks except `eXIf`, `tEXt`, `zTXt`, `iTXt`. WebP — rebuild RIFF: copy chunks except `EXIF` and `XMP `, honour pad bytes, recompute the RIFF size field, clear bits 3 (EXIF) and 2 (XMP) of the `VP8X` flags byte if a `VP8X` chunk exists. Return `this` when nothing changed. Then `FilesystemImageStore.store(space, upload)` → `upload = upload.withoutMetadata()` before writing. Update `ImageStore` Javadoc with the design's sentence.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -o install -pl kernels/observation -DskipTests && mvn -q -o test -pl kernels/observation,kernels/image-store-filesystem`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add kernels/observation kernels/image-store-filesystem
git commit -m "feat(observation): strip image metadata, keeping only orientation; the store strips every upload"
```

---

### Task 4: Rewrite the seed photos once, and guard them

**Files:**
- Modify (binary): every file under `domains/insects/insects-repository-test/src/main/resources/insects/images/` and `domains/plants/plants-repository-test/src/main/resources/plants/images/`
- Test: `apps/management-console/src/test/java/com/naturalist/console/images/SeedImagesCarryNoMetadataTest.java`

**Interfaces:**
- Consumes: `ImageUpload.withoutMetadata()`, `ImageUpload.gps()` (Task 3).

- [ ] **Step 1: Write the failing test**

```java
/** Every classpath seed photo is already stripped: withoutMetadata() is the identity on it and it carries no GPS. */
class SeedImagesCarryNoMetadataTest {
    static Stream<String> seedImages() throws IOException  // list "insects/images" and "plants/images" from the classpath (PathMatchingResourcePatternResolver "classpath*:insects/images/*" and plants)
    @ParameterizedTest @MethodSource("seedImages") void seedImage_isAlreadyStripped(String resource) {
        var upload = ImageUpload.of(bytesOf(resource));
        assertThat(upload.isValid()).isTrue();
        assertThat(upload.gps()).isEmpty();
        assertThat(upload.withoutMetadata().bytes()).isEqualTo(upload.bytes());
    }
    @Test void thereAreSeedImages()  // the stream is not empty (guards a broken classpath pattern)
}
```

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q -o install -DskipTests && mvn -q -o test -pl apps/management-console -Dtest=SeedImagesCarryNoMetadataTest`
Expected: FAIL — every seed JPEG currently carries EXIF (Orientation, ICC, IPTC).

- [ ] **Step 3: Rewrite the seed files in place (one-off, not a build step)**

```bash
mvn -q -o install -pl kernels/observation -DskipTests
CP="kernels/observation/target/classes:$(mvn -q -o dependency:build-classpath -pl kernels/observation -Dmdep.outputFile=/dev/stdout)"
jshell --class-path "$CP" - <<'EOF'
import com.naturalist.observation.ImageUpload; import java.nio.file.*;
for (String dir : new String[]{"domains/insects/insects-repository-test/src/main/resources/insects/images",
                               "domains/plants/plants-repository-test/src/main/resources/plants/images"}) {
  try (var files = Files.list(Path.of(dir))) {
    for (Path p : files.toList()) {
      byte[] in = Files.readAllBytes(p); byte[] out = ImageUpload.of(in).withoutMetadata().bytes();
      System.out.println(p.getFileName() + ": " + in.length + " -> " + out.length);
      Files.write(p, out);
    }
  }
}
/exit
EOF
```

Expected: every file shrinks; none fails.

- [ ] **Step 4: Verify the photos still display upright**

Run: `mvn -q -o install -DskipTests -pl domains/insects/insects-repository-test,domains/plants/plants-repository-test && mvn -q -o test -pl apps/management-console -Dtest=SeedImagesCarryNoMetadataTest`
Expected: PASS. Then open two portrait seed photos (one insect, one plant) in a browser or Preview and confirm orientation is unchanged from `git show HEAD:<path>`.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-repository-test/src/main/resources/insects/images domains/plants/plants-repository-test/src/main/resources/plants/images apps/management-console/src/test/java/com/naturalist/console/images/SeedImagesCarryNoMetadataTest.java
git commit -m "chore(seed): strip metadata from the seed photos; guard that none carries any"
```

---

### Task 5: `OrganismObservation.area` — kernel, api, core, repository-test

**Files:**
- Modify: `kernels/observation/src/main/java/com/naturalist/observation/OrganismObservation.java` (component `@Nullable GridCell area` after `location`; both `with*` methods; `invariants()` adds `.valueObjectOrNull(area, "area")`; add `withArea(@Nullable GridCell)`)
- Modify: `kernels/observation/src/test/java/com/naturalist/observation/OrganismObservationTest.java`
- Modify (constructor sites, pass `null` unless noted): `domains/insects/insects-api/src/test/.../InsectObservationTest.java`, `PhotoAdditionTest.java`, `CatalogIdentificationTest.java`, `InsectViewsTest.java`; `domains/insects/insects-repository-test/src/main/.../InsectObservationEntityRepositoryTest.java`; `domains/insects/insects-core/src/test/.../InsectCatalogIdentificationTransactionTest.java`, `InsectObservationCommandImplTest.java`; `domains/insects/insects-core/src/main/.../InsectIdentificationCommandImpl.java`, `InsectPhotoCommandImpl.java`, `InsectSightingCommandImpl.java`; `domains/plants/plants-api/src/test/.../PlantObservationTest.java`; `domains/plants/plants-repository-test/src/main/.../PlantObservationRepositoryTest.java`
- Verify unchanged: `InsectObservationTestEntitySource` / `PlantObservationTestEntitySource` and their JSON (`field-observations.json`) — a missing `area` deserialises to null.

**Interfaces:**
- Produces: `OrganismObservation(ID id, NaturalistName observedBy, R subject, Instant observedOn, @Nullable String notes, @Nullable String location, @Nullable GridCell area, @Nullable Identification identification)`; `withArea(@Nullable GridCell)`.

- [ ] **Step 1: Write the failing tests** in `OrganismObservationTest`: `area_isOptional` (null valid), `area_roundTripsAndWithAreaReplaces`, and in `InsectObservationEntityRepositoryTest` (contract, both mock and rdbms implement): `insertWithArea_readsBackTheSameCell` using `new GridCell(1092, -2945)`.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q -o test -pl kernels/observation -Dtest=OrganismObservationTest`
Expected: compilation failure (arity).

- [ ] **Step 3: Change the record and fix every constructor site** listed above (the rdbms DBOs are Task 6; the reactor will not fully compile until Task 6 lands — compile only up to `insects-repository-test`/`plants-repository-test` here).

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -o install -DskipTests -pl kernels/observation && mvn -q -o test -pl kernels/observation,domains/insects/insects-api,domains/insects/insects-repository-test,domains/plants/plants-api,domains/plants/plants-repository-test`
Expected: PASS (the contract test's new case runs against the mocks).

- [ ] **Step 5: Commit**

```bash
git add kernels/observation domains/insects/insects-api domains/insects/insects-repository-test domains/insects/insects-core domains/plants/plants-api domains/plants/plants-repository-test
git commit -m "feat(observation): an observation may carry a GridCell area"
```

---

### Task 6: `area` in the RDBMS adapters, schemas, and the standing test database

**Files:**
- Modify: `domains/insects/insects-repository-rdbms/src/main/resources/schema/insects.sql` (`insect_observation`: `area_lat INTEGER, area_lon INTEGER` after `location`)
- Modify: `domains/plants/plants-repository-rdbms/src/main/resources/schema/plants.sql` (`plant_observation`: same)
- Modify: `domains/insects/insects-repository-rdbms/src/main/java/com/naturalist/insects/InsectObservationDbo.java` (`Integer areaLat, areaLon`; `from` / `toEntity`; invariant: both null or both present)
- Modify: `domains/insects/insects-repository-rdbms/src/main/java/com/naturalist/insects/InsectObservationMapper.java` (every column list, INSERT, UPDATE)
- Modify: the plants equivalents `PlantObservationDbo.java`, `PlantObservationMapper.java`
- Test: the existing rdbms `*IT` for observations in both modules pick up the contract case from Task 5.

- [ ] **Step 1: Run the contract against rdbms to see it fail**

Run: `mvn -q -o install -DskipTests && mvn -q -o verify -pl domains/insects/insects-repository-rdbms -Dit.test=InsectObservationEntityRepositoryRdbmsIT` (use the module's actual IT class name; standing Postgres on 5432 required)
Expected: FAIL — `insertWithArea_readsBackTheSameCell` reads back null (or the DBO does not compile).

- [ ] **Step 2: Add the columns, DBO fields, and mapper SQL** in both domains. `toEntity`: `areaLat == null ? null : new GridCell(areaLat, areaLon)`.

- [ ] **Step 3: Recreate the standing test database** (schema changed; it is a fixed fixture)

Run: follow `apps/test-db-seeder/README.md` (or `java -jar apps/test-db-seeder/target/test-db-seeder-1.0.0-SNAPSHOT-jar-with-dependencies.jar` after `mvn -q -o install -DskipTests`).
Expected: seeder completes; `usage_event`/`usage_alert` empty.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -o verify -pl domains/insects/insects-repository-rdbms,domains/plants/plants-repository-rdbms`
Expected: PASS, including the fan-out gate.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-repository-rdbms domains/plants/plants-repository-rdbms
git commit -m "feat(rdbms): persist the observation area as two nullable cell indices"
```

---

### Task 7: Commands own the strip, the area, and the identification context

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCommand.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommandImpl.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectPhotoCommandImpl.java`
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectIdentificationCommandImplTest.java`, `InsectPhotoCommandImplTest.java`

**Interfaces:**
- Produces:
  - `InsectCommand.PhotoCommand.add(InsectRankName subject, ImageUpload upload, @Nullable NaturalistName naturalist, @Nullable String notes, @Nullable String location, @Nullable GridCell area)`
  - `InsectCommand.IdentificationCommand.identify(ImageUpload upload, ImageMetadata metadata, NaturalistName naturalist, @Nullable String notes, @Nullable GridCell area)`
  - Package-private `static String InsectIdentificationCommandImpl.locationContext(@Nullable String typed, @Nullable GridCell area)` → `null` when both absent; the typed text alone; or typed text (if any) + `" "` + `"Photographed within " + area.describe() + ". Treat this as the observation's location: rule out species whose known range does not include it."`

- [ ] **Step 1: Write the failing tests**

In `InsectIdentificationCommandImplTest`, a capturing stub replacing `stubService` where needed:

```java
private final AtomicReference<Image> seenByVision = new AtomicReference<>();
private final VisionService capturing = (image, tool, prompt) -> { seenByVision.set(image); return fixedExchange("propose_insect_species", SAMPLE_RESULT_JSON); };

@Test void identify_sendsTheAreaSentence_andStrippedBytes_toVision()
    // upload = ImageUpload.of(ImageFixtures.jpeg(app0Jfif(), app1Exif(BE, 6, 45.52, -122.68)))
    // identify(upload, new ImageMetadata("Chico, CA", null), pat, null, new GridCell(1092, -2945))
    // seenByVision.metadata().location() == "Chico, CA Photographed within 45.50–45.54°N, 122.71–122.67°W. Treat this as the observation's location: rule out species whose known range does not include it."
    // ImageUpload.of(seenByVision.bytes()).gps() isEmpty
@Test void identify_persistsTheArea_andKeepsTypedLocationSeparate()
    // observation.area() == new GridCell(1092, -2945); observation.location() == "Chico, CA"
@Test void identify_withoutArea_leavesLocationContextAsTyped_andAreaNull()
@Test void identify_storedImageCarriesNoMetadata()   // read the stored FileName back through the ImageStore fake → gps() empty
@Test void locationContext_bothNull_isNull()
```

In `InsectPhotoCommandImplTest`: `add_persistsAreaOnTheObservation`, `add_storesStrippedBytes`.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q -o test -pl domains/insects/insects-core -Dtest='InsectIdentificationCommandImplTest,InsectPhotoCommandImplTest'`
Expected: compilation failure (new parameter).

- [ ] **Step 3: Implement**

Both commands: first statement after argument observation is `upload = upload.withoutMetadata();`. Identification: build `Image` from the stripped upload with `new ImageMetadata(locationContext(metadata.location(), area), metadata.capturedAt())`; the observation gets `metadata.location()` (typed only) and `area`. `buildSystemPrompt` receives the same composed context. Photo command: observation gets `area`. Update the two API Javadocs.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -o install -DskipTests -pl domains/insects/insects-api && mvn -q -o test -pl domains/insects/insects-core`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-api domains/insects/insects-core
git commit -m "feat(insects): commands strip uploads, carry the area, and hand it to identification"
```

---

### Task 8: Controller resolves the area; slice tests

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java` (`identify` at ~410, `addImage` at ~633)
- Modify: `domains/insects/insects-console/src/main/jte/insects/identify.jte`, `detail.jte` (render the `?error=area` message beside the existing `?error=image` one)
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/ObservationAreaWebMvcTest.java`

**Interfaces:**
- Consumes: Task 7 signatures; `GridCell.ofIndices`, `ImageUpload.gps()`.
- Produces: request params `areaLat`, `areaLon` (optional) on `POST /insects/identify` and `POST /insects/{name}/images`; private `@Nullable GridCell resolveArea(@Nullable String areaLat, @Nullable String areaLon, ImageUpload upload)` returning `Optional.empty()`-equivalent `null`, and a sentinel for "posted but invalid" that the handler turns into `redirect:…?error=area`.

- [ ] **Step 1: Write the failing tests** (model on `InsectsCatalogWebMvcTest`; `InsectsControllerTestConfig.IMAGE_STORE` and `DATABASE` are available)

```java
@WebMvcTest class ObservationAreaWebMvcTest {
    @Test void addImage_postedCell_winsOverExif()
        // multipart with GPS JPEG (45.52,-122.68) + areaLat=1000 areaLon=-2000 → observation area == (1000,-2000)
    @Test void addImage_noJavaScript_areaComesFromExif_andServedBytesHaveNoMetadata()
        // GPS JPEG only → area == (1092,-2945); GET the stored image → ImageUpload.of(bytes).gps() empty and no APP1 with GPS
    @Test void addImage_noGps_noCell_areaIsNull()
    @Test void addImage_outOfRangeCell_redirectsWithAreaError()     // areaLat=2160 → redirectedUrl("/insects/battus-philenor?error=area"); nothing stored
    @Test void addImage_nonNumericOrHalfCell_fallsThroughToExif()    // areaLat="abc" alone, or only areaLon → behaves as no posted cell
    @Test void identify_passesTheAreaToTheCommand()                  // with the test config's identification fake, assert the area argument (see InsectsControllerTestConfig for how commands are faked)
    @Test void detail_rendersAreaError()                             // GET ?error=area contains "area"
}
```

Find the observation via `InsectsControllerTestConfig`'s wired `InsectQuery` (or the test context) — the newest observation for `battus-philenor` by the current naturalist.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q -o test -pl domains/insects/insects-console -Dtest=ObservationAreaWebMvcTest`
Expected: FAIL (params ignored; signatures).

- [ ] **Step 3: Implement** `resolveArea`: both params present and parse as `int` → `GridCell.ofIndices(...)`, empty → error sentinel; otherwise `upload.gps().map(GridCell::of).orElse(null)`. Pass to the commands. Add the `?error=area` copy: *"The area could not be read; the photo was not added."*

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -o install -DskipTests -pl domains/insects/insects-core,domains/insects/insects-test-context && mvn -q -o test -pl domains/insects/insects-console`
Expected: PASS, whole module.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console
git commit -m "feat(insects-console): resolve an observation's area from the posted cell or EXIF"
```

---

### Task 9: ArchUnit — `Coordinates` never persists

**Files:**
- Create: `apps/management-console/src/test/java/com/naturalist/console/architecture/LocationPrivacyComplianceTest.java` (pattern: `ResilienceComplianceTest`)

- [ ] **Step 1: Write the test**

```java
@AnalyzeClasses(packages = "com.naturalist", importOptions = ImportOption.DoNotIncludeTests.class)
class LocationPrivacyComplianceTest {
    @ArchTest static final ArchRule coordinatesNeverPersist = fields().that()
        .haveRawType(Coordinates.class)
        .should().notBeDeclaredInClassesThat().areAssignableTo(Entity.class)
        .andShould().notBeDeclaredInClassesThat().areAssignableTo(Aggregate.class)
        .andShould().notBeDeclaredInClassesThat().areAssignableTo(ReadModel.class)
        .because("exact coordinates are request-scoped; only a GridCell may be stored (design 2026-11-08)");
}
```

- [ ] **Step 2: Run** `mvn -q -o test -pl apps/management-console -Dtest=LocationPrivacyComplianceTest` → PASS. Temporarily add a `Coordinates` field to a scratch record implementing `Entity` in the module's test sources to see it fail, then delete it.

- [ ] **Step 3: Commit**

```bash
git add apps/management-console/src/test/java/com/naturalist/console/architecture/LocationPrivacyComplianceTest.java
git commit -m "test(arch): exact coordinates may never be a persisted component"
```

---

### Task 10: Browser — EXIF GPS, the area line, Clear, and the gallery

**Files:**
- Modify: `apps/management-console/src/main/resources/static/js/image-upload.js`
- Modify: `domains/insects/insects-console/src/main/jte/insects/identify.jte`, `detail.jte` (hidden `areaLat`/`areaLon`, `<p id="area-line" hidden>Area: <span id="area-text"></span> <button type="button" id="area-clear">Clear</button></p>`)
- Modify: `domains/insects/insects-console/src/main/jte/insects/observationGallery.jte` (after the `obs-location` paragraph: `@if(obs.area() != null) <p class="obs-area">Observed in the area ${obs.area().describe()}</p> @endif`)
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/InsectsDetailTemplateTest.java` (add: hidden fields and area line render; an observation with an area renders `Observed in the area 45.50–45.54°N`)

- [ ] **Step 1: Write the failing template tests**, then run `mvn -q -o test -pl domains/insects/insects-console -Dtest=InsectsDetailTemplateTest` → FAIL.

- [ ] **Step 2: Templates** — add the markup; declare `display`/`margin` explicitly on the new `<p>` and button (Pico leaks). Run the test → PASS.

- [ ] **Step 3: JavaScript** — replace `parseExifApp1` with a real reader: `Exif\0\0` check, byte order (`II`/`MM`), IFD0 walk for `0x8825`, GPS IFD tags 1–4 (rationals), sign by ref letters. On file `change`: `readExif(file)` → if lat/lng: `latIdx = Math.floor(lat*24)`, `lonIdx = Math.floor(lng*24)`; set hidden fields; text = same format as `GridCell.describe()` (two decimals, hemisphere letters); show the line. Clear button: empty both hidden fields, hide the line. Remove the code that wrote coordinates into the `location` input. The `submit` handler already copies hidden inputs into the `FormData`.

- [ ] **Step 4: Manual check** — `mvn -q -o install -DskipTests` then run the app against Mailpit (README) and upload a GPS-tagged phone JPEG on `/insects/identify` with the network tab open: the request carries `areaLat`/`areaLon` and no coordinates; the area line shows the expected cell; Clear removes it; the resulting observation page shows *Observed in the area …*. Then disable JavaScript and upload the same file: same area, and `curl -s <image url> | xxd | head` shows no `Exif` string.

- [ ] **Step 5: Commit**

```bash
git add apps/management-console/src/main/resources/static/js/image-upload.js domains/insects/insects-console
git commit -m "feat(console): show the photo's area before submit and post only the cell"
```

---

### Task 11: Docs, gate, and close-out

**Files:**
- Modify: `docs/notes/parking-lot.md` (move PL-17 to `docs/notes/parking-lot-resolved.md` with the resolution)
- Modify: `docs/hosting-readiness.md` (tick "Strip photo metadata")
- Modify: `docs/plans/2026-11-05-public-launch-work-plan.md` (1.1 ✅; 5.3 reworded: location is in, only "use it to pre-filter candidates" remains)
- Modify: `docs/work-tracker.md` (one row)
- Modify: `domains/insects/CLAUDE.md` (vocabulary: *area* = `GridCell`, the only location the domain keeps; `location` = the naturalist's words)
- Modify: `kernels/CLAUDE.md` (observation kernel: `GridCell`, `Coordinates` request-scoped, `ImageUpload.withoutMetadata()` contract on `ImageStore`)

- [ ] **Step 1: Full gate**

Run: `mvn -q -o verify && mvn -q -o install -DskipTests && mvn -q -o rewrite:dryRun -Drewrite.failOnDryRunResults=true && git ls-files -co --exclude-standard | xargs grep -nIiE '[a-z0-9]+[-_]api[-_]?key|sk-ant[-]'`
Expected: verify green; dryRun green; grep prints nothing.

- [ ] **Step 2: Update the docs** listed above.

- [ ] **Step 3: Commit**

```bash
git add docs domains/insects/CLAUDE.md kernels/CLAUDE.md
git commit -m "docs: close PL-17 and launch item 1.1; record the observation area vocabulary"
```
