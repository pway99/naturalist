# OrganismImage — unifying the per-domain image metadata record

Date: 2026-08-19
Status: **SHIPPED 2026-08-19** (commits `48c731c7` kernel, `b0d7fd93` insects,
`9c01c2bf` plants; full `mvn clean install` green; whole-branch review SOUND, no
blocking findings). Package kept FLAT in `com.naturalist.observation` (a `.organism`
sub-package considered and rejected as YAGNI — revisit only if a non-organism
observation type appears). Deferred, as designed: media/behavior kernel; soil/weather
images. Follow-up filed (task_d18d6f66): add arg-validation to
`InsectImageRepositoryMock.getByParentName` (pre-existing gap vs the plants side).

Sibling to the just-shipped `OrganismObservation` kernel
([design](2026-08-19-organism-observation-kernel-design.md)). Same pattern,
three refinements: a third type parameter, no own-id rename, and the
`parentName` codec swap.

## 1. Problem

`InsectImage` and `PlantImage` are near-identical per-domain records — the
photo-evidence analogue of the `FieldObservation` duplication just unified.
Each is:

```
<Domain>Image(<Domain>ImageId id, <Domain>RankName parentName, Instant dateAdded,
              FileName resourceName, @Nullable <Domain>ObservationId observationId)
```

They differ only by the two typed ids and the rank type. This is the second
copy the `OrganismObservation` design explicitly deferred ("OrganismImage —
unify `Insect/PlantImage` reusing this kernel + codec pattern; migrate
`InsectRankNameJacksonTest` at that point").

## 2. Scope decision: metadata only, not behavior

An image carries both **metadata** (what taxon, which observation, filename,
date) and **behavior** (store, resize, encode-for-vision). They split on
subject-specificity:

- **Metadata is subject-specific.** An organism image and a future soil image
  share nothing here (taxon vs soil subject), so there is no shared metadata to
  extract — and the `Insect/PlantImage` records have **zero behavior today**
  (pure records + `invariants()`).
- **Behavior is subject-agnostic** and already has homes: `kernels/vision/Image`
  + `ImageMetadata` (bytes for the model), `ImageStorageService`
  (insects-console, filesystem), `FileName` (framework — `path()`, `nameType()`).
  Heavy pre-processing (resize/thumbnail/EXIF) is **not built**.

**Therefore this effort unifies only the metadata record.** No media/behavior
kernel is created — that would be speculative (nothing shared to house yet) and
does not paint us in: a metadata record does not block a later behavior kernel,
and a future soil image is *different metadata* living in soil. When shared
pre-processing becomes real, a media kernel owns *behavior* and can absorb
`ImageStorageService` + coordinate with `vision`'s `Image`.

## 3. The type — three parameters (fully typed)

```java
// kernels/observation
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

- Insects binds `OrganismImage<InsectImageId, InsectObservationId, InsectRankName>`;
  plants `<PlantImageId, PlantObservationId, PlantRankName>`.
- **Three type params** because an image references *two* per-domain ids (its own
  `ImageId` and the linked `ObservationId`) plus the taxon `RankName` — full
  typing keeps every reference domain-specific and exhaustive, consistent with the
  `OrganismObservation<ID, R>` decision. Verbose but honest.
- `observationId` stays **`@Nullable`** — a photo can exist without a linked
  observation.
- **Placement:** `kernels/observation`, alongside `OrganismObservation` and
  `Identification` (the organism-evidence cluster). Deps already satisfied
  (framework for `FileName`/`Entity`, taxonomy for `RankName`, identifiers for the
  ids). No new module.
- **Name stays `OrganismImage`** — never bare `Image` (collides with the existing
  `kernels/vision/Image`).
- **No own-id rename.** `InsectImageId`/`PlantImageId` already carry correct
  domain-prefixed names (unlike `FieldObservationId`, which had to become
  `InsectObservationId`).

## 4. The `parentName` codec + the dispatch tests

Today `parentName` uses a field-level
`@JsonTypeInfo(EXTERNAL_PROPERTY)` + `@JsonSubTypes` envelope naming the domain
permits (`parentRank` discriminator + `parentName` slug). A kernel record cannot
name domain permits, so `parentName` moves onto the **same self-describing
`{"rank","value"}` codec** `OrganismObservation.subject` already uses:
`RankNameSerializer` / `RankNameDeserializer` (taxonomy) + the per-domain
`RankNameReconstructor` injected on the source's mapper.

Consequences:
- **Catalog JSON shape changes** per image record: flat `"parentRank": "GENUS",
  "parentName": "empoasca"` → `"parentName": {"rank": "GENUS", "value":
  "empoasca"}`. (`id` and `observationId` stay bare UUID strings.)
- **`InsectRankNameJacksonTest` and `PlantRankNameDispatchTest`** exist
  specifically to validate the *old* `@JsonSubTypes` Image dispatch. They are
  removed/replaced — the kernel codec round-trip is proven by the kernel
  `OrganismObservationTest` + the per-domain contract tests. (The observation
  effort left these two tests intact precisely because Image was out of scope;
  they now go.)

## 5. Migration (insects first, then plants — separate gated commits)

Mirrors the observation migration; per domain, atomic (a type unification does
not compile half-done):

1. Add nothing to `identifiers` — the ids already exist and keep their names.
2. `insects-api` already depends on `observation` (from the observation effort);
   confirm and add if a module reports it missing.
3. Repoint `com.naturalist.insects.InsectImage` →
   `com.naturalist.observation.OrganismImage<InsectImageId, InsectObservationId,
   InsectRankName>` at every use site; delete the insects `InsectImage.java`
   record.
4. **Source** (`InsectImageTestEntitySource`): read via the three-arg
   `constructParametricType(OrganismImage.class, InsectImageId.class,
   InsectObservationId.class, InsectRankName.class)` through the
   `loadFile(path, parser)` overload; register the `RankNameReconstructor`
   (`InsectRankName::of`) on its mapper; override `writableClass()` → raw
   `OrganismImage.class` (required — `entityClass()` reflects the generic arg and
   throws `ClassCastException` on flush otherwise). FK extractors on `parentName`
   (`instanceof InsectFamilyName/…`) unchanged.
5. **Catalog JSON:** migrate every `parentName` to the `{rank,value}` shape.
6. **Plumbing:** nested `InsectQuery.ImageQuery`, `InsectRepository.ImageRepository`,
   `InsectEntityCollections.ImageCollection` stay **bare**, retyped to
   `OrganismImage<…>`. Standalone `InsectImageQueryImpl`, `InsectImageCommandImpl`,
   `InsectImageRepositoryMock`, `InsectImageTestEntitySource` (+ their `*Test`
   companions) keep their prefixed names (just standardized by the ADR-020 §5
   sweep), retyped.
7. **Consumers:** `PhotoAddition`, `CatalogIdentification`,
   `InsectFeatureAssignment`, `InsectFunctionalRole`, the `InsectTaxonView`
   gallery composition, `InsectsController`/JTE galleries, `ImageStorageService`.
8. **Remove** `InsectRankNameJacksonTest`.
9. Green scoped build, then full `mvn clean install`; gated commit.
10. Repeat for plants (`PlantImage` → `OrganismImage<PlantImageId,
    PlantObservationId, PlantRankName>`; remove `PlantRankNameDispatchTest`).

Ripple to grep (per the arity/rename scars): every `new InsectImage(...)` site,
`ImageCollection`/gallery return types, TaxonView assembly, JTE `@import` lines,
`TestInsectsIdentifiers` image-id fixtures, and the `parentName` FK method-refs.

## 6. Out of scope / deferred

- **Media / behavior kernel** — created only when shared pre-processing is real;
  it owns behavior, not this metadata record.
- **Soil / weather images** — different metadata, different subject; live in
  their own domain if built.
- **`ImageStorageService` relocation** — stays in the console for now; a future
  media kernel is its natural home.

## 7. Testing

- Per-domain repository contract tests (they now store the generic type via the
  parametric read) — the primary proof of the codec round-trip end-to-end.
- The kernel gains an `OrganismImageTest` (invariants + a `{rank,value}`
  `parentName` round-trip with a fake reconstructor + fake ids), matching
  `OrganismObservationTest`.
- Full `mvn clean install` after each domain migration (the console/JTE galleries
  are the broad blast radius).
