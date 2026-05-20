# Insect-Image Parent-Rank Extension

> **Status:** drafted 2026-05-19, ahead of PL-2's remaining 3 organisms.
> Step 1 of the Path A sequencing settled in conversation: image-model
> extension first, then Hemimetabolous trait permit, then potato-leafhopper
> reclassification.

**Goal.** Let an `InsectImage` attach to whichever rank a field naturalist
can identify the organism to — species, genus, or family. Today the
record's `insectSpeciesName` component locks the parent reference to
species only, which forced under-identified organisms (potato-leafhopper,
green-lacewing, the sweat bees, …) to live as fake species-rank records.
Most of those have already been migrated to their correct rank under
PL-2; potato-leafhopper is the last one and its images are what's
blocking the move.

After this slice lands, an `InsectImage` carries an `InsectRankName`
parent — the existing typed slug of whichever rank record (`InsectFamily`,
`InsectGenus`, `InsectSpecies`) is the most specific rank the observation
firmed up to. Rank transitions ("we now know this is *Empoasca fabae*,
not just an Empoasca") become a single-field update.

This slice is **additive on the image model and additive on the JSON
schema**. The 10 existing image records all reference species today; they
remain valid post-migration after a one-time JSON shape rewrite.

---

## Scope decisions

### Sealed marker over the existing typed name classes (plus subspecies)

```java
public sealed interface InsectRankName
        permits InsectFamilyName,
                InsectGenusName,
                InsectSpeciesName,
                InsectSubspeciesName {}
```

The three existing `EntityName` subclasses gain `implements InsectRankName`
— one-line change each. `InsectSubspeciesName` is a new sibling class
that ships in the identifiers module *ahead of* any subspecies entity
record, so the sealed permit list closes over the full Linnaean range
an `InsectImage` may attach to. Adding subspecies later would force a
revisit of every `@JsonSubTypes` declaration and any future `switch`
over the permit list — cheaper to land it now.

The image's parent field is typed `InsectRankName`, constraining storage
at compile time to the four insect-side rank names. Cross-domain names
(`PlantSpeciesName`, etc.) do not compile in the slot.

**Rejected — sealed wrappers (`SpeciesRef(InsectSpeciesName)` …).**
Adds a layer of destructuring at every consumer site without buying
anything; the existing name classes already carry the slug.

**Rejected — three nullable parent slots with "exactly one non-null".**
Splits one logical reference across three fields; invariant noise; no
type-system benefit over the sealed marker.

**Rejected — bare `String` slug.** Cross-DAG slug collisions ("papilionidae"
the family slug equals "papilionidae" the Clade slug as strings)
become silent; loses class-qualified equality on EntityName.
Breaks the ADR-022 rule that cross-`NamedEntity` references go by
typed `EntityName`, never raw String.

### JSON shape — polymorphic via consumer-field `@JsonTypeInfo`

Step 0 (Jackson verification) revealed that placing `@JsonTypeInfo` on
the sealed interface itself leaks the polymorphic envelope into every
leaf-class serialization site — including `InsectSpecies.name`,
`InsectImage.name`, every other `InsectSpeciesName` ever serialized —
breaking the catalog's JSON schema globally. Jackson resolves
polymorphic annotations from the runtime class hierarchy, not from the
declared static field type. So the annotation must live on the
*consumer field*, not the interface.

The consumer record carries the dispatch:

```java
public record InsectImage(
        InsectImageId name,
        @JsonTypeInfo(use = Id.NAME,
                      property = "parentRank",
                      include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
            @Type(value = InsectFamilyName.class,     name = "FAMILY"),
            @Type(value = InsectGenusName.class,      name = "GENUS"),
            @Type(value = InsectSpeciesName.class,    name = "SPECIES"),
            @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName parentName,
        ...
) {}
```

JSON shape — flat, two sibling fields on the containing record:

```json
{
  "name": "...",
  "parentRank": "SPECIES",
  "parentName": "potato-leafhopper",
  ...
}
```

`As.EXTERNAL_PROPERTY` flattens the discriminator into the parent
object's field set. The value itself stays a plain string because
`EntityName.value()` is still `@JsonValue`. Hand-editable, greppable,
and the envelope applies only where opted in. Direct uses of the
leaf classes (e.g., `InsectSpecies.name : InsectSpeciesName`)
continue to serialize as plain strings.

The shape is verified by `InsectRankNameJacksonTest` (Step 0). The
test uses a throwaway `ParentHolder` record annotated identically to
the planned `InsectImage` shape.

### Field naming

`InsectImage.parentName : InsectRankName`. The existing accessor
`insectSpeciesName()` goes away; consumers switch to `parentName()`.
Field name choice:
- Drops the rank-specific "Species" qualifier (the type itself is
  rank-polymorphic).
- Follows the project convention of `xxxName` for EntityName-typed
  fields.
- Reads naturally: an image's parent is the taxonomic record it
  identifies to.

### Query surface — single rank-polymorphic method

Per the consumer-profile clarification ("the question is always 'give me
images for species/genus/family'"), there's no caller asking "what rank
is this image about?" — the rank is known by the caller passing the
typed name. So the image query collapses to one method:

```java
ImageCollection forParentName(InsectRankName parentName);
```

Existing call sites:
- `InsectAggregateFactory` passes a species — still compiles since
  `InsectSpeciesName implements InsectRankName`.
- `InsectsController` passes `species.name()` — same.
- Tests passing `InsectSpecies.TachinidFly.name` (an `InsectSpeciesName`)
  — same.

The old `forSpeciesName(InsectSpeciesName)` method is removed; callers
that currently pass an `InsectSpeciesName` get the same behavior via
the new method without any source change beyond renaming the call.

The repository method follows the same naming: `getByParentName`.

### Out of scope

- **LifeStage's parallel issue.** `LifeStageRepository.getBySpeciesName`
  + `InsectLifeStageQuery.forSpeciesName` accept `InsectSpeciesName`
  only, even though life-stage records keyed under genus/family
  composite slugs (`chrysoperla-egg`, `halictus-larva`, …) already
  exist in the catalog post-PL-2. The data shape is rank-flexible; the
  query API isn't. Same recipe as this slice applies — but it's a
  separate slice. Note in the parking lot.
- **Hemimetabolous trait permit.** Path A step 2.
- **potato-leafhopper reclassification.** Path A step 3.
- **InsectGenus.placedIn migration to typed `Clade` instead of slug
  reference.** Out of scope; the existing `placedIn` is the typed
  `Clade` already.

---

## Architecture

```
domains/identifiers/src/main/java/com/naturalist/insects/
├── InsectRankName.java                    — NEW sealed interface
├── InsectFamilyName.java                  — gains `implements InsectRankName`
├── InsectGenusName.java                   — gains `implements InsectRankName`
└── InsectSpeciesName.java                 — gains `implements InsectRankName`

domains/insects/insects-api/src/main/java/com/naturalist/insects/
├── InsectImage.java                       — component rename + retype
├── InsectRepository.java                  — ImageRepository.getByParentName
└── InsectQuery.java                       — ImageQuery.forParentName

domains/insects/insects-core/src/main/java/com/naturalist/insects/
├── ImageQueryImpl.java                    — method rename, argument retype
└── InsectAggregateFactory.java            — uses new method name

domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/
├── InsectImageTestEntitySource.java       — field accessor swap
└── InsectImageRepositoryMock.java         — method + filter retype

domains/insects/insects-repository-test/src/main/resources/insects/
└── insect-images.json                     — schema migration (10 records)

domains/insects/insects-console/src/main/java/com/naturalist/insects/console/
└── InsectsController.java                 — call sites: forSpeciesName → forParentName
```

---

## Steps

### Step 0 — Verify Jackson polymorphism + @JsonValue interaction *(complete)*

- [x] Wrote `InsectRankNameJacksonTest` against a throwaway
  `ParentHolder` record. **Finding:** the first iteration put
  `@JsonTypeInfo` on `InsectRankName` itself, which leaked the
  polymorphic envelope into every leaf-class serialization site
  (`directLeafTypeUseStillSerializesAsPlainString` caught it —
  Jackson emitted `["SPECIES","battus-philenor"]` for a directly-typed
  `InsectSpeciesName`). **Fix:** move dispatch off the interface to
  the consumer field with `As.EXTERNAL_PROPERTY`. Leaf-class direct
  uses go back to plain-string serialization; the polymorphic envelope
  applies only at opted-in sites.
- [x] Added `InsectSubspeciesName` to the sealed permit list ahead of
  any subspecies entity record, so the permit list closes over the
  full Linnaean range an `InsectImage` may attach to.

### Step 1 — Introduce `InsectRankName`

- [ ] New file `domains/identifiers/.../InsectRankName.java` —
  sealed interface with the three permits.
- [ ] Add `implements InsectRankName` to `InsectFamilyName`,
  `InsectGenusName`, `InsectSpeciesName`.
- [ ] Annotate the sealed interface with `@JsonTypeInfo` / `@JsonSubTypes`.

### Step 2 — Retype `InsectImage.parentName`

- [ ] Rename component `insectSpeciesName` → `parentName`; retype
  `InsectSpeciesName` → `InsectRankName`.
- [ ] Update invariants — `entityName(parentName, "parentName")` only
  works on the leaf class; either provide a constraint helper that
  delegates through the sealed type, or use `notNull(parentName,
  "parentName")` and trust the type system for the rest.
- [ ] Update Javadoc.

### Step 3 — Update repository / query interfaces

- [ ] `InsectRepository.ImageRepository.getBySpeciesName(InsectSpeciesName)`
  → `getByParentName(InsectRankName)`.
- [ ] `InsectQuery.ImageQuery.forSpeciesName(InsectSpeciesName)` →
  `forParentName(InsectRankName)`.

### Step 4 — Update implementations

- [ ] `ImageQueryImpl.forSpeciesName` → `forParentName`; observer
  argument validation switches to the new type.
- [ ] `InsectImageRepositoryMock.getBySpeciesName` → `getByParentName`;
  filter expression updates to `image.parentName().equals(parentName)`.
- [ ] `InsectImageTestEntitySource` — accessor reference
  `InsectImage::insectSpeciesName` → `InsectImage::parentName`,
  `@EntityIdentifier`-style field name string `"insectSpeciesName"` →
  `"parentName"`.
- [ ] `InsectAggregateFactory` — call site `imageQuery.forSpeciesName(...)`
  → `forParentName(...)`. The argument `species.name()` continues to
  compile since `InsectSpeciesName implements InsectRankName`.

### Step 5 — Update console template + controller

- [ ] `InsectsController.java` — three call sites of `forSpeciesName`
  rename to `forParentName`.
- [ ] `InsectsListTemplateTest.java` — `InsectImage::insectSpeciesName`
  → `InsectImage::parentName`. The groupingBy collector returns
  `Map<InsectRankName, …>` instead of `Map<InsectSpeciesName, …>`;
  consumer assertions may need adjustment if they specifically expected
  the species-typed key.

### Step 6 — Update consumer tests

- [ ] `ImageQueryImplTest` — `forSpeciesName_*` → `forParentName_*`
  method names; argument types swap.
- [ ] `InsectAggregateFactoryTest` — `image.insectSpeciesName()` →
  `image.parentName()`; equality assertion uses `equals(value.species().name())`
  which still works because `InsectSpeciesName.equals` is
  class-qualified and the loaded image's parentName is the
  `InsectSpeciesName` permit.
- [ ] `ImageCommandImplTest` — argument types and assertions update
  the same way.
- [ ] `InsectImageRepositoryMockTest` (if it exists) — same.

### Step 7 — Migrate `insect-images.json` schema

- [ ] Rewrite all 10 image records from
  `"insectSpeciesName": "<slug>"` to
  `"parentName": {"rank": "SPECIES", "value": "<slug>"}`.
- [ ] Verify the loaded count matches and the catalog tests stay green.

### Step 8 — Add genus/family-attached image fixtures (positive coverage)

- [ ] Add at least one image record to `insect-images.json` whose
  `parentName.rank` is `"GENUS"` or `"FAMILY"`, attaching to an
  existing genus or family record. Wire a corresponding
  `TestInsectsIdentifiers` constant (e.g., a new
  `InsectGenus.Chrysoperla.Images.Img<n>` or family analog) and add an
  end-to-end resolver test that loads the image and confirms its
  parent rank.
- [ ] If adding a new image fixture is heavy, defer to Step 3 of Path A
  (the potato-leafhopper reclassification) — that move will produce
  the first organic genus-attached images. Note in the slice.

### Step 9 — Verify

- [ ] User runs `mvn verify`. Expect green.
- [ ] Commit.
- [ ] Roll work-tracker forward.

---

## Test plan

### New tests

- **`InsectRankNameJacksonTest`** (Step 0 — kernel-level Jackson
  verification): polymorphic round-trip for each permit.
- **`ImageQueryImplTest.forParentName_acceptsGenusName`** —
  query path works when the parent is a genus.
- **`ImageQueryImplTest.forParentName_acceptsFamilyName`** — same for
  family.

### Rewritten tests

Existing `forSpeciesName_*` cases on `ImageQueryImplTest` rename to
`forParentName_*` and pass the species typed name unchanged. Behavior
contract is identical because the species path is the same
sealed-interface permit it was before.

### Verified-only tests

All existing image-related repository/aggregate-factory/template tests
should pass without behavioral assertions changing — the species
permit is the same data, just typed under a different declared
parameter type.

---

## Risks

- **Jackson polymorphism × `@JsonValue` interaction (Step 0).**
  Mitigated by the focused round-trip test before any production code
  is touched. If the envelope form doesn't work cleanly, the wrapper-
  permits fallback adds a small amount of code but doesn't change any
  consumer-side behavior.
- **`@EntityIdentifier` field-name semantic.** `InsectImage` carries
  `@EntityIdentifier` on `insectSpeciesName` (verify in
  `InsectImageTestEntitySource`'s `uniqueConstraints()`). The
  `EntityIdentifier`-by-field-name lookup in the TestEntitySource
  framework needs the renamed field to also rename its constraint
  declaration. Caught at runtime if missed (NamedTestEntitySource
  validation), trivial to fix.
- **Polymorphic JSON breaks downstream tools.** Any non-test JSON
  consumer (none currently known in the repo, but worth a final grep)
  would need the schema migration. Mitigated by grepping for
  `"insectSpeciesName"` in non-source paths before commit.

---

## Out of scope (recap)

- LifeStage parallel refactor (`getBySpeciesName` → `getByParentName`
  for `LifeStageEntityRepository` + `InsectLifeStageQuery`). Note in
  parking lot — same recipe, separate slice.
- Hemimetabolous trait permit (Path A step 2).
- potato-leafhopper reclassification (Path A step 3).

---

## Done when

- `mvn verify` is green at repo root.
- `InsectImage` carries `InsectRankName parentName`.
- `ImageQuery.forParentName(InsectRankName)` is the single query method;
  callers that pass an `InsectSpeciesName` still compile and behave
  identically.
- `insect-images.json` records all use the new polymorphic shape.
- At least one image record (existing or new) demonstrates a non-species
  parent rank — proving the new model works end-to-end.
