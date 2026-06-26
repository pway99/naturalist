# InsectFeature Entity Design

**Status.** Design spec replacing `insect-feature-api-plan.md` (2026-06-14).
That plan proposed `IdentificationFeatures` as a `ValueObject` embedded on
each rank entity. This spec revises the persistence model: features become
their own `Entity<InsectFeatureId>` linked to ranks via a many-to-many
`InsectFeatureAssignment`, composed into `InsectTaxonView` the same way
`ImageCollection` already is.

**Motivation.** The VO-on-entity approach maps poorly to RDBMS — it forces
either an opaque JSON blob column (not queryable, no referential integrity)
or ORM-managed embeddables (fragile, hard to evolve). Worse, a feature like
"sucking mouthparts" is one concept that applies to multiple ranks; embedding
it as a list component on each rank duplicates the concept and reduces the
reverse lookup ("which taxa carry this feature?") to string-matching instead
of a proper relational join.

**Pair with.** `domains/insects/CLAUDE.md` (domain vocabulary),
`domains/CLAUDE.md` (identity model, record conventions, repository
architecture), `insect-feature-api-plan.md` (predecessor — its §7 decided
decisions still apply except §7.2, overturned here).

---

## 1. The Feature Entity

```java
record InsectFeature(
    InsectFeatureId id,                  // UUIDv7 surrogate identity
    @UniqueValue String value            // normalized display text, unique within data source
) implements Entity<InsectFeatureId>
```

- `InsectFeatureId` — new `EntityId` subclass in `domains/identifiers`.
- Identity is surrogate, not natural-key. There is no authority guaranteeing
  feature name uniqueness the way the ICZN guarantees Linnaean rank names.
  `EntityId` is the honest choice; `EntityName` would require inventing and
  maintaining slugs with no external backing.
- `value` carries `@UniqueValue` and is declared in `uniqueConstraints()`.
  "Sucking mouthparts" is one feature, one entity. Multiple-rank attachment
  is the association's job, not a second entity with identical text.
- Normalized in `InsectFeature.of(InsectFeatureId, String)` — trim,
  case-fold. Normalization makes uniqueness sound: "Sucking mouthparts" and
  "sucking mouthparts" normalize to the same value and correctly collide.
- No separate `NamedValue<String>` wrapper. The entity itself is the
  normalization seam and the identity wrapper.
- Authored in a JSON catalog: `insect-features.json` with `id` + `value`
  per entry.

---

## 2. The Association Entity

The many-to-many link between a feature and a rank. Carries the ordinal
because conspicuous-to-diagnostic ordering is contextual to where the
feature appears in the taxonomy, not intrinsic to the feature itself.

```java
record InsectFeatureAssignment(
    InsectFeatureAssignmentId id,    // UUIDv7 surrogate identity
    InsectFeatureId featureId,       // FK to InsectFeature
    InsectRankName rankName,         // which rank this feature is assigned to
    int ordinal                      // position in conspicuous-to-diagnostic ordering for this rank
) implements Entity<InsectFeatureAssignmentId>
```

- `InsectFeatureAssignmentId` — new `EntityId` subclass in
  `domains/identifiers`.
- The pair `(featureId, rankName)` is unique — a feature can only be
  assigned to a given rank once. Declared in `uniqueConstraints()`.
- `ordinal` is per-rank ordering, not global. The assignment at rank
  Hemiptera has its own ordinal sequence independent of the assignment at
  family Cicadellidae.
- Authored in a JSON catalog: `insect-feature-assignments.json` referencing
  feature IDs and rank name slugs.

---

## 3. Composition into InsectTaxonView

`InsectTaxonView` currently composes rank entity + `ImageCollection`.
Features join the same pattern:

```java
public sealed interface InsectTaxonView extends ReadModel
        permits InsectOrderView, InsectFamilyView, InsectGenusView, InsectSpeciesView {

    InsectRankName name();
    ImageCollection images();
    FeatureCollection features();   // direct features assigned to this rank
}
```

Each permit gains `FeatureCollection features` as a component:

- `InsectSpeciesView(InsectSpecies species, ImageCollection images, FeatureCollection features)`
- `InsectGenusView(InsectGenus genus, ImageCollection images, FeatureCollection features)`
- `InsectFamilyView(InsectFamily family, ImageCollection images, FeatureCollection features)`
- `InsectOrderView(InsectOrder order, ImageCollection images, FeatureCollection features)`

`FeatureCollection` is a new `BehavioralCollection<InsectFeature>` in
`InsectEntityCollections`, parallel to `ImageCollection`.

### Removals from rank entities

- `@Nullable IdentificationFeatures identificationFeatures` — removed from
  `InsectOrder`, `InsectFamily`, `InsectGenus`, `InsectSpecies`.
- `IdentificationFeatures` record — deleted.
- `FeatureBearing` interface — deleted.

Features no longer live on rank entities. The rank records return to their
core concern: Linnaean identity, Durrell description, common names, clade
placement.

---

## 4. The Lineage-Composite Read Model

Direct features on `InsectTaxonView` answer "what features are assigned to
this rank." The lineage composite answers "what features apply to this
organism across its entire ancestry" — the same question
`InsectCitationView` answers for citations.

```java
record InsectFeatureView(
    InsectRankName subject,
    List<RankedFeature> features    // composited up the lineage, ancestor-first
) implements ReadModel

record RankedFeature(
    InsectFeature feature,          // the resolved feature entity
    InsectRankName assignedAt,      // which rank in the lineage contributed this
    int ordinal                     // preserved from the assignment
) implements ValueObject
```

### Assembly — lineage walk

Mirrors the citation lineage walk in `InsectCitationQueryImpl`:

1. Resolve ancestry: species -> genus -> family -> order.
2. For each rank in ancestry, query feature assignments by `rankName`.
3. Resolve `InsectFeature` entities by `featureId`.
4. Tag each with its source rank as `RankedFeature`.
5. Composite: ancestor ranks first (most general/conspicuous), descendant
   ranks last (most specific/diagnostic).

Ordering within a rank: by `ordinal`. Across ranks: ancestor-first. The two
orderings align naturally — the composited list reads
conspicuous-to-diagnostic without extra sorting.

### Deduplication

A feature assigned to both an order and a family within that order appears
twice in the composited list — once per assignment, each tagged with its
source rank. This is correct: the assignment at order level and at family
level are distinct editorial acts with potentially different ordinals. The
consumer can deduplicate by `featureId` if desired, but the default
composite preserves provenance.

---

## 5. Query Surface

```java
interface InsectFeatureQuery {
    Optional<InsectFeatureView> findByRankName(InsectRankName subject);  // forward: lineage composite
    RankNameCollection findByFeature(InsectFeatureId featureId);         // reverse: which ranks carry this feature
}
```

Reached via `insectQuery.features()`, mirroring `insectQuery.citations()`.

- **Forward** (`findByRankName`): returns the lineage-composited
  `InsectFeatureView` for the subject rank — the full conspicuous-to-diagnostic
  feature list an organism inherits.
- **Reverse** (`findByFeature`): joins through `InsectFeatureAssignment` by
  `featureId`, returns a `RankNameCollection` of carrying ranks. A
  naturalist's browse aid ("show me insects with chewing mouthparts"), not a
  phylogenetic claim.

Multi-result returns are `BehavioralCollection` subclasses per convention.
`RankNameCollection` already exists in `InsectEntityCollections`.

---

## 6. Decided and Preserved

### 6.1 Open value space — NO closed registry / enum

Carried forward from the predecessor plan (§7.1). `InsectFeature.value` is
an open `String` — no enum, no controlled vocabulary. `@UniqueValue`
enforces that each text appears at most once, but does not restrict which
texts are valid. The value space is as wide as the naturalist's descriptive
language.

### 6.2 Features are shared entities with intra-domain associations

**Overturns** predecessor plan §7.2. That section argued features fail the
three tests justifying the citation-association machinery (independent
entity, cross-domain, binding carries data). The revised analysis:

- **Independent entity?** Yes — "sucking mouthparts" is a concept with
  identity apart from any single rank. It applies to Hemiptera at order
  level and to specific families within it. The association handles the
  many-to-many; the feature is the shared concept.
- **Cross-domain?** No — and this design does not use `EntityRef` or
  cross-domain erasure. The association is purely intra-domain, linking
  `InsectFeature` to `InsectRankName` within `com.naturalist.insects`.
- **Does the binding carry its own data?** Yes — `ordinal`, the
  conspicuous-to-diagnostic position, is per-assignment.

The practical persistence concern is decisive: a VO list on an entity
forces JSON blobs or fragile embeddables. Entities with associations get
proper tables, proper rows, proper relational queries.

### 6.3 No inverted index for reverse lookup (deferred, YAGNI)

Carried forward from predecessor plan (§7.3). `findByFeature` scans
through assignments. An inverted index is not built now.

### 6.4 Relationship to the deferred typed-trait kernel SPI

Carried forward from predecessor plan (§7.4). `InsectFeature` is the
insects-domain, one-entity feature model. It is distinct from the
cross-domain typed-trait vocabulary deferred in
`identification-module-design.md`. They are related but separate.

---

## 7. Migration

1. **Add `InsectFeatureId` and `InsectFeatureAssignmentId`** to
   `domains/identifiers`.
2. **Add `InsectFeature` entity** in `com.naturalist.insects` with
   `@UniqueValue` on `value`, normalizing `of()` factory.
3. **Add `InsectFeatureAssignment` entity** in `com.naturalist.insects`
   with compound uniqueness on `(featureId, rankName)`.
4. **Add `FeatureCollection`** to `InsectEntityCollections`.
5. **Remove `IdentificationFeatures` VO, `FeatureBearing` interface** from
   rank entities. Remove the `identificationFeatures` component from
   `InsectOrder`, `InsectFamily`, `InsectGenus`, `InsectSpecies`.
6. **Add `FeatureCollection features()`** to `InsectTaxonView` and each
   permit. Update `InsectTaxonViewFactory` to query and compose features.
7. **Add `RankedFeature` VO and `InsectFeatureView` read model.**
8. **Add `InsectFeatureQuery`** with `findByRankName` (forward composite)
   and `findByFeature` (reverse join), exposed via the insects query
   facade.
9. **Migrate existing feature data** from rank entity JSON files to
   `insect-features.json` + `insect-feature-assignments.json`.
10. **Test entity sources, repository contracts, and behavioral tests**
    mirroring the citation and image patterns.

---

## 8. Predecessor plan disposition

`insect-feature-api-plan.md` (2026-06-14) is superseded by this spec. Its
§7 decided decisions are carried forward (§6.1, §6.3, §6.4 above) except
§7.2, which is overturned (§6.2 above). Its §5 open decision (Option A vs
Option B) is moot — features are neither on-entity nor in a rank-keyed
catalog; they are independent entities with many-to-many associations.