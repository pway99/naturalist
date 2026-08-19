# OrganismImage Kernel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the two duplicated per-domain image records (`InsectImage`, `PlantImage`) with one generic `OrganismImage<IMG_ID extends EntityId, OBS_ID extends EntityId, R extends RankName>` in `kernels/observation`, reusing the `RankName` `{rank,value}` codec built for `OrganismObservation`.

**Architecture:** The photo-evidence sibling of the shipped `OrganismObservation` kernel. Metadata-only (no image behavior kernel — behavior lives in `vision`/`ImageStorageService`/`FileName`). `parentName` moves off `@JsonSubTypes` onto the kernel `RankNameSerializer`/`RankNameDeserializer`; each domain's image source reads its catalog via a three-arg parametric Jackson type and registers a `RankNameReconstructor`. Insects migrates first, then plants — each an atomic, separately-gated commit.

**Tech Stack:** Java 21 (records, sealed interfaces, generics), Maven multi-module, Jackson (databind + jsr310), JUnit 5 + AssertJ, JTE templates.

## Global Constraints

- **Typed identifiers only.** No raw `String`/`Long`/`UUID` as an entity reference across a boundary.
- **UUIDv7 only.** Ids via `EntityId.newUUID()`; never `UUID.randomUUID()`.
- **Records for Entity/ValueObject**, immutable, `invariants()` declared.
- **Entity ids never cross a domain boundary by value.** The generic id params are the entity's own id and its same-domain observation link — not cross-domain references.
- **ADR-020 §5 naming:** nested namespace ports are bare EntitySubject (`ImageQuery`, `ImageRepository`, `ImageCollection`); standalone concrete adapters carry the domain prefix (`InsectImageQueryImpl`, `PlantImageTestEntitySource`). This effort **preserves** the current names (both were standardized by the recent ADR-020 sweep) and only retypes them.
- **Kernel signature changes need a clean install.** After editing `kernels/observation`, run `mvn clean install` (not incremental) so downstream resolves fresh classes.
- **Claude may run Maven** (`mvn verify`/scoped `-pl` builds). Prefer scoped during a task; full `mvn clean install` at the domain-migration gate.
- **JSON rewrites use `ensure_ascii=False`** (Python) — never escape UTF-8 in catalog files.
- **The name is `OrganismImage`, never bare `Image`** — `kernels/vision/Image` already exists.

---

### Task 1: `OrganismImage` record in the observation kernel

**Files:**
- Create: `kernels/observation/src/main/java/com/naturalist/observation/OrganismImage.java`
- Test: `kernels/observation/src/test/java/com/naturalist/observation/OrganismImageTest.java`

**Interfaces:**
- Produces: `record OrganismImage<IMG_ID extends EntityId, OBS_ID extends EntityId, R extends RankName>(IMG_ID id, R parentName, Instant dateAdded, FileName resourceName, @Nullable OBS_ID observationId) implements Entity<IMG_ID>`. `parentName` carries `@JsonSerialize(using = RankNameSerializer.class)` + `@JsonDeserialize(using = RankNameDeserializer.class)`.
- Consumes: `RankNameSerializer`/`RankNameDeserializer`/`RankNameReconstructor` (taxonomy, already shipped); `FileName` (framework); `RankName` (taxonomy).

- [ ] **Step 1: Write the failing test**

```java
// kernels/observation/src/test/java/com/naturalist/observation/OrganismImageTest.java
package com.naturalist.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameReconstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganismImageTest {

    static final class FakeImageId extends EntityId {
        private FakeImageId(UUID v) { super(v); }
        @JsonCreator static FakeImageId of(UUID v) { return new FakeImageId(v); }
        static FakeImageId create() { return new FakeImageId(EntityId.newUUID()); }
    }

    static final class FakeObsId extends EntityId {
        private FakeObsId(UUID v) { super(v); }
        @JsonCreator static FakeObsId of(UUID v) { return new FakeObsId(v); }
        static FakeObsId create() { return new FakeObsId(EntityId.newUUID()); }
    }

    record FakeGenusName(String value) implements RankName {
        @Override public LinealRank rank() { return LinealRank.GENUS; }
    }

    private static final RankNameReconstructor RECONSTRUCTOR = (slug, rank) -> new FakeGenusName(slug);

    private static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setInjectableValues(new InjectableValues.Std()
                        .addValue(RankNameReconstructor.class, RECONSTRUCTOR));
    }

    @Test
    void serializesParentNameAsSelfDescribingObjectAndRoundtrips() throws Exception {
        var img = new OrganismImage<FakeImageId, FakeObsId, FakeGenusName>(
                FakeImageId.create(), new FakeGenusName("empoasca"),
                Instant.parse("2026-04-16T00:00:00Z"), FileName.of("IMG_9047.HEIC"), null);

        ObjectMapper mapper = mapper();
        String json = mapper.writeValueAsString(img);
        assertThat(json).contains("\"rank\":\"GENUS\"").contains("\"value\":\"empoasca\"");

        var type = mapper.getTypeFactory().constructParametricType(
                OrganismImage.class, FakeImageId.class, FakeObsId.class, FakeGenusName.class);
        OrganismImage<FakeImageId, FakeObsId, FakeGenusName> decoded = mapper.readValue(json, type);
        assertThat(decoded.id()).isEqualTo(img.id());
        assertThat(decoded.parentName().value()).isEqualTo("empoasca");
        assertThat(decoded.observationId()).isNull();
    }

    @Test
    void nullObservationIdIsAllowed() {
        var img = new OrganismImage<FakeImageId, FakeObsId, FakeGenusName>(
                FakeImageId.create(), new FakeGenusName("empoasca"),
                Instant.now(), FileName.of("a.heic"), null);
        assertThat(img.observationId()).isNull();
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl kernels/observation -am test`
Expected: FAIL — `OrganismImage` does not exist.

- [ ] **Step 3: Implement `OrganismImage`**

```java
// kernels/observation/src/main/java/com/naturalist/observation/OrganismImage.java
package com.naturalist.observation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.naturalist.data.FileName;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameDeserializer;
import com.naturalist.taxonomy.RankNameSerializer;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A photographic record attached to an organism at a taxonomic rank — the shared
 * evidence unit across organism domains, and the photo sibling of
 * {@link OrganismObservation}. {@code parentName} is the taxon the photo identifies
 * (at whatever rank the naturalist's confidence allows); {@code observationId} links
 * the photo to the sighting it belongs to, and is null for a photo attached directly
 * to a catalog rank with no observation.
 *
 * <p>{@code parentName} is a domain permit ({@code InsectSpeciesName}, …) widened to
 * {@link RankName}; it serializes as a self-describing {@code {"rank":…,"value":…}}
 * object and rebuilds the concrete permit through the {@code RankNameReconstructor}
 * registered on the reading mapper (see each domain's image test-entity source).
 */
public record OrganismImage<IMG_ID extends EntityId, OBS_ID extends EntityId, R extends RankName>(
        IMG_ID id,
        @JsonSerialize(using = RankNameSerializer.class)
        @JsonDeserialize(using = RankNameDeserializer.class)
        R parentName,
        Instant dateAdded,
        FileName resourceName,
        @Nullable OBS_ID observationId
) implements Entity<IMG_ID> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(parentName, "parentName")
                .notNull(dateAdded, "dateAdded")
                .namedValue(resourceName, "resourceName")
                .whenNotNull(observationId, c -> c.entityId(observationId, "observationId"));
    }
}
```

- [ ] **Step 4: Run to verify green**

Run: `mvn -q -pl kernels/observation -am test`
Expected: PASS (both new tests; existing observation tests unaffected).

- [ ] **Step 5: Commit**

```bash
git add kernels/observation/src/main/java/com/naturalist/observation/OrganismImage.java kernels/observation/src/test/java/com/naturalist/observation/OrganismImageTest.java
git commit -m "feat(observation): OrganismImage generic evidence record"
```

- [ ] **Step 6: Clean-install the kernel** so the domain migrations resolve the new class.

Run: `mvn clean install -q -pl kernels/observation -am`
Expected: BUILD SUCCESS.

---

### Task 2: Migrate insects to `OrganismImage`

Atomic per-domain migration (won't compile half-done). `InsectImageId` is **not** renamed (already well-named).

**Type mapping (apply across `domains/insects/**`):**

| old | new |
| --- | --- |
| `com.naturalist.insects.InsectImage` | `com.naturalist.observation.OrganismImage<InsectImageId, InsectObservationId, InsectRankName>` |

**Files to edit** (verify with the Step-8 grep; do not trust this list to be exhaustive): delete `insects-api/.../InsectImage.java`; retype nested `InsectRepository.ImageRepository`, `InsectQuery.ImageQuery`, `InsectEntityCollections.ImageCollection`; the standalone `InsectImageQueryImpl`, `InsectImageCommandImpl`, `InsectImageRepositoryMock`, `InsectImageTestEntitySource` (+ their `*Test` companions, `ImageGalleryTest`); consumers `PhotoAddition`, `CatalogIdentification`, `InsectFeatureAssignment`, `InsectFunctionalRole`, `InsectTaxonView*`, `InsectsController` + JTE galleries, `ImageStorageService`; and delete `InsectRankNameJacksonTest`.

- [ ] **Step 1: Confirm the observation dependency**

`insects-api` already depends on `observation` (added in the observation effort). Verify `domains/insects/insects-api/pom.xml` has the `observation` dependency; add it if missing (groupId `com.naturalist`, artifactId `observation`).

- [ ] **Step 2: Delete `InsectImage.java`, repoint api types**

Delete `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectImage.java`. In the api files, replace `InsectImage` → `OrganismImage<InsectImageId, InsectObservationId, InsectRankName>`. Nested ports become e.g.:
```java
protected interface ImageRepository
        extends EntityRepository<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> { … }
```
```java
final class ImageCollection extends BehavioralCollection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> { … }
```
Add `import com.naturalist.observation.OrganismImage;` where needed. The nested type NAMES (`ImageRepository`/`ImageQuery`/`ImageCollection`) stay bare per ADR-020 §5.

- [ ] **Step 3: Repoint the standalone plumbing (keep prefixed names)**

`InsectImageQueryImpl`, `InsectImageCommandImpl`, `InsectImageRepositoryMock` keep their file/class names; retype every `InsectImage` reference to `OrganismImage<InsectImageId, InsectObservationId, InsectRankName>`.

- [ ] **Step 4: Rewrite `InsectImageTestEntitySource` for the parametric read**

Replace its body to mirror the shipped `InsectObservationTestEntitySource` (`domains/insects/insects-repository-test/.../InsectObservationTestEntitySource.java`), with the THREE-arg parametric type:

```java
package com.naturalist.insects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestDataHelper;
import com.naturalist.data.TestEntitySource;
import com.naturalist.observation.OrganismImage;
import com.naturalist.taxonomy.RankNameReconstructor;

import java.io.UncheckedIOException;
import java.util.List;

public class InsectImageTestEntitySource
        extends TestEntitySource<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> {

    private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
            .setInjectableValues(new InjectableValues.Std()
                    .addValue(RankNameReconstructor.class, (RankNameReconstructor) InsectRankName::of));

    public InsectImageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-images.json", this::parse);
    }

    private List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> parse(String json) {
        try {
            JavaType t = mapper.getTypeFactory().constructParametricType(
                    OrganismImage.class, InsectImageId.class, InsectObservationId.class, InsectRankName.class);
            JavaType listT = mapper.getTypeFactory().constructCollectionType(List.class, t);
            return mapper.readValue(json, listT);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override protected ObjectMapper mapper() { return mapper; }

    @Override protected Class<?> writableClass() { return OrganismImage.class; }

    @Override
    protected List<ForeignKeyConstraint<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of("parentName (family)",
                        image -> image.parentName() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of("parentName (genus)",
                        image -> image.parentName() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of("parentName (species)",
                        image -> image.parentName() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }
}
```

- [ ] **Step 5: Migrate the insects image catalog JSON**

Transform `domains/insects/insects-repository-test/src/main/resources/insects/insect-images.json` — flat `parentRank`+`parentName` → nested `parentName: {rank,value}`:

```python
import json
p = "domains/insects/insects-repository-test/src/main/resources/insects/insect-images.json"
data = json.load(open(p))
for o in data:
    rank = o.pop("parentRank"); slug = o.pop("parentName")
    o["parentName"] = {"rank": rank, "value": slug}
json.dump(data, open(p, "w"), indent=2, ensure_ascii=False)
open(p, "a").write("\n")
```
(`id` and `observationId` stay bare UUID strings / null.)

- [ ] **Step 6: Repoint consumers + JTE galleries**

Repoint `PhotoAddition`, `CatalogIdentification`, `InsectFeatureAssignment`, `InsectFunctionalRole`, the `InsectTaxonView` gallery assembly, `InsectsController`, and the JTE gallery templates. In JTE, `@import com.naturalist.insects.InsectImage` → `com.naturalist.observation.OrganismImage` and reference the raw `OrganismImage` (templates read fields; match whatever the controller passes). `ImageStorageService` references the type only for its `resourceName`/id — retype accordingly.

- [ ] **Step 7: Delete `InsectRankNameJacksonTest`**

Delete `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectRankNameJacksonTest.java` — it validates the old `@JsonSubTypes` `parentName` dispatch, now replaced by the kernel codec (proven by `OrganismImageTest` + the repository contract tests).

- [ ] **Step 8: Sweep**

Run: `grep -rnE '\bInsectImage\b' domains/insects apps --include='*.java' --include='*.jte'`
Expected: ZERO hits (the standalone `InsectImage*Impl`/`InsectImageId`/`InsectImageTestEntitySource` identifiers have a suffix and are NOT matched by `\bInsectImage\b`; only the bare entity type is). Fix any hit. Leave `InsectImageId` alone (it stays).

- [ ] **Step 9: Verify green**

Run: `mvn clean install` from the repo root.
Expected: BUILD SUCCESS — including `insects-repository-test` image contract tests loading the migrated JSON and reconstructing typed permits, and the console/JTE galleries.

- [ ] **Step 10: Commit**

```bash
git add domains/insects
git commit -m "refactor(insects): adopt shared OrganismImage kernel type"
```

---

### Task 3: Migrate plants to `OrganismImage`

Mirror Task 2 for plants: `PlantImage` → `OrganismImage<PlantImageId, PlantObservationId, PlantRankName>`; reconstructor `PlantRankName::of`; delete `PlantRankNameDispatchTest`. `PlantImageId` is not renamed. Plants has four ranks (ORDER/FAMILY/GENUS/SPECIES) in its FK extractors; plants may have no `ImageCommand` (only wire what exists).

- [ ] **Step 1: Confirm `plants-api` depends on `observation`** (added in the observation effort; add if missing).

- [ ] **Step 2: Delete `plants.PlantImage`; repoint api types** — nested `ImageRepository`/`ImageQuery`/`ImageCollection` bare, retyped to `OrganismImage<PlantImageId, PlantObservationId, PlantRankName>`.

- [ ] **Step 3: Repoint standalone plumbing** (`PlantImageQueryImpl`, `PlantImageRepositoryMock`, keep prefixed names) — retype references.

- [ ] **Step 4: Rewrite `PlantImageTestEntitySource`** exactly as Task 2 Step 4 but with `PlantImageId`, `PlantObservationId`, `PlantRankName`, `PlantRankName::of`, catalog `plants/plant-images.json`, and FK extractors for `PlantOrderName`/`PlantFamilyName`/`PlantGenusName`/`PlantSpeciesName`:

```java
private List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> parse(String json) {
    try {
        JavaType t = mapper.getTypeFactory().constructParametricType(
                OrganismImage.class, PlantImageId.class, PlantObservationId.class, PlantRankName.class);
        JavaType listT = mapper.getTypeFactory().constructCollectionType(List.class, t);
        return mapper.readValue(json, listT);
    } catch (JsonProcessingException e) { throw new UncheckedIOException(e); }
}
@Override protected ObjectMapper mapper() { return mapper; }
@Override protected Class<?> writableClass() { return OrganismImage.class; }
```
with `mapper` built as `TestDataHelper.newBaseMapper().setInjectableValues(new InjectableValues.Std().addValue(RankNameReconstructor.class, (RankNameReconstructor) PlantRankName::of))` and `loadFile("plants/plant-images.json", this::parse)`.

- [ ] **Step 5: Migrate `plants/plant-images.json`** to the `{rank,value}` `parentName` shape:

```python
import json
p = "domains/plants/plants-repository-test/src/main/resources/plants/plant-images.json"
data = json.load(open(p))
for o in data:
    rank = o.pop("parentRank"); slug = o.pop("parentName")
    o["parentName"] = {"rank": rank, "value": slug}
json.dump(data, open(p, "w"), indent=2, ensure_ascii=False)
open(p, "a").write("\n")
```

- [ ] **Step 6: Repoint plants consumers + JTE galleries** (PlantTaxonView assembly, plants console gallery templates, any `PhotoAddition`/role references).

- [ ] **Step 7: Delete `PlantRankNameDispatchTest`** (`domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantRankNameDispatchTest.java`).

- [ ] **Step 8: Sweep** — `grep -rnE '\bPlantImage\b' domains/plants --include='*.java' --include='*.jte'` → ZERO hits (leave `PlantImageId`).

- [ ] **Step 9: Verify green** — `mvn clean install` from the repo root → BUILD SUCCESS.

- [ ] **Step 10: Commit**

```bash
git add domains/plants
git commit -m "refactor(plants): adopt shared OrganismImage kernel type"
```

---

## Follow-up (not in this plan)

- **Media / behavior kernel** — when shared image pre-processing (resize/thumbnail/EXIF) is actually built; it owns behavior and can absorb `ImageStorageService` + coordinate with `kernels/vision/Image`.
- **Soil / weather images** — different metadata in their own domain, if ever built.
- Carried over from the observation effort: the future production RDBMS data adapter must register the `RankNameReconstructor`; minor `RankNameDeserializer` malformed-JSON hardening.
