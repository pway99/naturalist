# insects-domain — Chat Briefing

**Purpose.** Domain vocabulary plus current shape of the insects module
(four-level Linnaean hierarchy, species, image, field observation,
features, functional role, life-stage sub-context), sized for a chat
Claude session.
Pair with `docs/briefings/framework-core.md` (framework / structural
glue) and, when generating life-stage JSON,
`docs/briefings/insect-lifestage-acquisition.md`.

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time (2026-07-13), not
extrapolated. If you need a type not listed here, ask before inventing one.

---

## 1. Module Scope and DAG

```
insects-api  →  framework, identifiers, field-notes, taxonomy, habitat,
                clades, plants-api (PlantName only),
                naturalists-api (NaturalistName only)
```

`insects-api` depends on `plants-api` for `PlantName` (typed slug only —
used by `LarvaStage.hostPlants` and `AdultStage.nectarSources`), on
`clades` for the `Clade` sealed type (each rank entity carries
`@Nullable Clade placedIn`), and on `naturalists-api` for
`NaturalistName` (used by `FieldObservation.observedBy`). No
taxonomy-light shortcut: each rank carries its own `Taxonomic*` epithet
from `kernels/taxonomy`.

Cross-domain consumers reference insects only through the api — by
`InsectSpeciesName`, `InsectGenusName`, `InsectFamilyName`,
`InsectOrderName`, `LifeStageName`, or `InsectImageId` (the last one
deliberately rare, since `Entity` ids do not cross domain boundaries by
value — cross the parent's `InsectRankName` slug instead).

---

## 2. Package Map

```
com.naturalist.insects/
  InsectsDomain                       — DomainId record (catalog kernel hook)
  FunctionalGuild                     — enum (8 ecological roles)

  InsectOrder                         — NamedEntity<InsectOrderName>, LinnaeanOrder
  InsectFamily                        — NamedEntity<InsectFamilyName>, LinnaeanFamily
  InsectGenus                         — NamedEntity<InsectGenusName>, LinnaeanGenus
  InsectSpecies                       — @AggregateRoot, NamedEntity<InsectSpeciesName>
                                        + 6 nested ValueObject records

  InsectImage                         — Entity<InsectImageId>
  FieldObservation                    — Entity<FieldObservationId>
  InsectFunctionalRole                — Entity<InsectFunctionalRoleId>
  InsectFeature                       — Entity<InsectFeatureId>
  InsectFeatureAssignment             — Entity<InsectFeatureAssignmentId>

  InsectTaxonView                     — sealed ReadModel interface
    InsectOrderView / InsectFamilyView  — record permits (rank + images + features)
    InsectGenusView / InsectSpeciesView — record permits (rank + images + features)

  InsectCitationView                  — ReadModel (hierarchy-inherited citations)
                                        + nested RankedCitation ValueObject
  InsectFeatureView                   — ReadModel (lineage-composite features)
                                        + nested RankedFeature ValueObject
  Insect                              — ReadModel (rank-chain composite)

  InsectQuery                         — public namespace interface
                                        (taxonView / species / images /
                                         fieldObservations / families / genera /
                                         functionalRoles / orders / citations /
                                         features) + top-level getByName
  InsectCommand                       — public namespace interface
                                        (species / images / fieldObservations)
  InsectRepository                    — package-private namespace class
                                        (SpeciesRepository, ImageRepository,
                                         FieldObservationRepository,
                                         FamilyRepository, GenusRepository,
                                         FunctionalRoleRepository,
                                         OrderRepository, FeatureRepository,
                                         FeatureAssignmentRepository)
  InsectEntityCollections             — public namespace interface
                                        (SpeciesCollection, ImageCollection,
                                         FieldObservationCollection,
                                         FeatureCollection, ImageGallery,
                                         FamilyCollection, GenusCollection,
                                         FunctionalRoleCollection,
                                         OrderCollection)

  lifestage/
    LifeStage                         — sealed interface, NamedEntity<LifeStageName>
                                        permits {Egg,Larva,Pupa,Adult}Stage
                                        @JsonTypeInfo(property = "kind")
    EggStage / LarvaStage             — record implements LifeStage
    PupaStage / AdultStage            — record implements LifeStage
    StagePhenology                    — ValueObject (+ nested ActivityWindow)
    StageHabitat                      — ValueObject (wraps HabitatProfile)
    StageChemistryRole                — ValueObject (+ nested Role enum)
    InsectLifeStageQuery              — public namespace interface
    InsectLifeStageEntityCollections  — public namespace interface
                                        (LifeStageCollection)
    LifeStageRepository               — LifeStageEntityRepository
```

### Identifier locations (in the `identifiers` module, not in insects-api)

| Type                        | Package                  |
|-----------------------------|--------------------------|
| `InsectRankName`            | `com.naturalist.insects` |
| `InsectOrderName`           | `com.naturalist.insects` |
| `InsectFamilyName`          | `com.naturalist.insects` |
| `InsectGenusName`           | `com.naturalist.insects` |
| `InsectSpeciesName`         | `com.naturalist.insects` |
| `InsectSubspeciesName`      | `com.naturalist.insects` |
| `InsectImageId`             | `com.naturalist.insects` |
| `InsectFunctionalRoleId`    | `com.naturalist.insects` |
| `FieldObservationId`        | `com.naturalist.insects` |
| `InsectFeatureId`           | `com.naturalist.insects` |
| `InsectFeatureAssignmentId` | `com.naturalist.insects` |
| `LifeStageName`             | `com.naturalist.insects` |
| `LifeStageKind`             | `com.naturalist.insects` |

The *module* is `identifiers`; the *package* mirrors the home domain.
`InsectRankName` is a sealed interface over the five rank names (order,
family, genus, species, subspecies); it carries `value()` and `rank()`
for polymorphic slug/rank access without down-casting.
`LifeStageKind` is an enum (not an EntityName) but lives with the
identifiers because it is a structural component of `LifeStageName`.

---

## 3. Entity Summary

| Type                      | Identity                    | Branch                            | DDD role                           |
|---------------------------|-----------------------------|-----------------------------------|------------------------------------|
| `InsectOrder`             | `InsectOrderName`           | `NamedEntity` (slug)              | Rank entity (hierarchy root)       |
| `InsectFamily`            | `InsectFamilyName`          | `NamedEntity` (slug)              | Rank entity (parent-only FK)       |
| `InsectGenus`             | `InsectGenusName`           | `NamedEntity` (slug)              | Rank entity (parent-only FK)       |
| `InsectSpecies`           | `InsectSpeciesName`         | `NamedEntity` (slug)              | **@AggregateRoot** (value objects) |
| `InsectImage`             | `InsectImageId`             | `Entity` (UUIDv7, component `id`) | Observation record                 |
| `FieldObservation`        | `FieldObservationId`        | `Entity` (UUIDv7, component `id`) | Naturalist collection unit         |
| `InsectFunctionalRole`    | `InsectFunctionalRoleId`    | `Entity` (UUIDv7, component `id`) | Cross-rank ecology assignment      |
| `InsectFeature`           | `InsectFeatureId`           | `Entity` (UUIDv7, component `id`) | Typed field mark                   |
| `InsectFeatureAssignment` | `InsectFeatureAssignmentId` | `Entity` (UUIDv7, component `id`) | Feature-to-rank link               |
| `LifeStage` (sealed)      | `LifeStageName`             | `NamedEntity` (composite slug)    | Sealed family (4 permits)          |
| `InsectTaxonView`         | `InsectRankName`            | `ReadModel` (sealed)              | Rank + images + features view      |
| `InsectCitationView`      | (no identity)               | `ReadModel`                       | Hierarchy-inherited citations      |
| `InsectFeatureView`       | (no identity)               | `ReadModel`                       | Lineage-composite features         |
| `Insect`                  | (no identity)               | `ReadModel`                       | Rank-chain composite               |

### Parent-only FK chain (no grandparent skip-level references)

```
InsectOrder  (no parent FK — hierarchy root)
  <- InsectFamily.orderName
    <- InsectGenus.familyName
      <- InsectSpecies.genusName
```

Each rank carries only its immediate parent's `EntityName` FK.
Grandparent resolution (e.g. species -> order) requires walking the
chain through intermediate entities.

### Cross-rank `InsectRankName` pattern

`InsectImage`, `FieldObservation`, `InsectFunctionalRole`,
`InsectFeatureAssignment`, and each `LifeStage` permit carry a
polymorphic `InsectRankName` field — a sealed FK that can point to any
insect rank. Jackson dispatch is declared at the consuming field
(`@JsonTypeInfo(As.EXTERNAL_PROPERTY)`) with a discriminator field
(`parentRank`, `subjectRank`, or `rank`), not on the `InsectRankName`
interface itself.

---

## 4. Linnaean Rank Entities

### `InsectOrder`

```java
public record InsectOrder(
    InsectOrderName name,
    TaxonomicOrder order,
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn
) implements NamedEntity<InsectOrderName>, LinnaeanOrder
```

Root of the hierarchy. No parent FK. `order` is the proper-cased Linnaean
epithet (e.g. `"Diptera"`). Slug identity derived via
`LinnaeanOrder.orderSlug()`.

### `InsectFamily`

```java
public record InsectFamily(
    InsectFamilyName name,
    InsectOrderName orderName,
    TaxonomicFamily family,
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn
) implements NamedEntity<InsectFamilyName>, LinnaeanFamily<InsectOrderName>
```

Typed upward FK `orderName` to parent order. `belongsToOrder(orderName)`
convenience predicate.

### `InsectGenus`

```java
public record InsectGenus(
    InsectGenusName name,
    InsectFamilyName familyName,
    TaxonomicGenus genus,
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn
) implements NamedEntity<InsectGenusName>, LinnaeanGenus<InsectFamilyName>
```

Typed upward FK `familyName` to parent family. `belongsToFamily(familyName)`
convenience predicate.

### All three share

- Four-level Durrell `Description`
- `Set<CommonName>` for locale-tagged vernacular names
- `@Nullable Clade placedIn` for clade-DAG placement
- `withPlacedIn(Clade)` mutator

---

## 5. InsectSpecies — the Aggregate Root

```java
@AggregateRoot
public record InsectSpecies(
        InsectSpeciesName name,
        InsectGenusName genusName,
        TaxonomicSpecies epithet,
        Description description,
        Set<CommonName> commonNames,
        @Nullable String sightingNotes,
        @Nullable Clade placedIn,
        @Nullable ChemicalDefense chemicalDefense,
        @Nullable Voltinism voltinism,
        @Nullable HabitatProfile habitatProfile,
        @Nullable HabitatRequirements habitatRequirements,
        @Nullable GardenConnections gardenConnections,
        @Nullable BeneficialProfile beneficialProfile,
        @Nullable EcologicalSignificance ecologicalSignificance
) implements NamedEntity<InsectSpeciesName>
```

### Required vs nullable fields

- `name`, `genusName`, `epithet`, `description`, `commonNames` — always
  populated.
- All six value-object fields are nullable; populated incrementally as
  the catalog matures.
- `beneficialProfile` should only be populated when the species's
  `InsectFunctionalRole.beneficial()` is `true` (intent constraint, not
  invariant).
- `chemicalDefense` is populated only on chemically defended species
  (*Battus philenor*, etc.). Empty/default `ChemicalDefense` is not a
  legal alternative encoding — use `null`.

### Nested value-object graph

All six owned exclusively by `InsectSpecies`; each is a `static record`
inside `InsectSpecies.java`:

| Type                     | Required components                      | Notes                                                 |
|--------------------------|------------------------------------------|-------------------------------------------------------|
| `ChemicalDefense`        | `String mechanism`, `Set<LifeStageKind>` | `sourceCompounds`, `aposematicSignal` nullable        |
| `Voltinism`              | `VoltinismPattern` enum                  | UNIVOLTINE / BIVOLTINE / MULTIVOLTINE / INDETERMINATE |
| `HabitatRequirements`    | (all five fields nullable)               | Insect-side narrative — pair with `habitatProfile`    |
| `GardenConnections`      | `List<String> supportingPlants`          | `supportingPlants` are *future* `PlantName` slugs     |
| `BeneficialProfile`      | `String significance`                    | Populate only when `beneficial == true`               |
| `EcologicalSignificance` | (all three fields nullable)              | Indicator value, food-web position, regional context  |

`habitatProfile` (top-level field on `InsectSpecies`) is the structured
classification from `kernels/habitat`; `HabitatRequirements` is the
complementary narrative axis. Both are nullable and complementary, not
substitutes.

Identification features (formerly the inline `IdentificationFeatures`
value object) are now modeled as independent `InsectFeature` +
`InsectFeatureAssignment` entities — see sections 9–10.

---

## 6. InsectFunctionalRole — cross-rank ecology

```java
public record InsectFunctionalRole(
    InsectFunctionalRoleId id,
    InsectRankName parentName,
    Set<FunctionalGuild> guilds,
    boolean beneficial
) implements Entity<InsectFunctionalRoleId>
```

Identity by UUIDv7 — accessor `id()`, invariant `.entityId(id, "id")`.
Extracted from `InsectSpecies` (PL-11). Attaches to any rank via
`parentName: InsectRankName`. One role record per organism (uniqueness
enforced on `parentName`). `guilds` must be non-empty — "not yet
documented" is expressed by the absence of a role record, not by an
empty set on a present record. Jackson dispatch on `parentName` mirrors
`InsectImage`.

---

## 7. InsectImage

```java
public record InsectImage(
    InsectImageId id,
    InsectRankName parentName,
    Instant dateAdded,
    FileName resourceName,
    @Nullable FieldObservationId observationId
) implements Entity<InsectImageId>
```

`InsectImage` is a photographic observation record — identity by UUIDv7,
accessor `id()`, invariant `.entityId(id, "id")`. `parentName` is
`InsectRankName` (sealed polymorphic) — can attach to any rank the
naturalist's confidence allows. Rank transitions ("we now know this is
*Empoasca fabae*, not just an Empoasca") become a single-field update.

`resourceName` is a `FileName` (kernel `NamedValue<String>`), not a path.
The path prefix `insects/images/` is a stable convention — compose with
`resourceName.path("insects/images/")` at the use site.

`observationId` is a nullable `FieldObservationId` link from the photo
to the `FieldObservation` it was captured under. `null` means a shared
catalog image with no owning naturalist (the pre-collection default).
When a naturalist is signed in and adds a photo, the controller creates
a `FieldObservation` and sets this link.

---

## 8. FieldObservation — the naturalist's collection unit

```java
public record FieldObservation(
    FieldObservationId id,
    NaturalistName observedBy,
    InsectRankName subject,
    Instant observedOn,
    @Nullable String notes,
    @Nullable String location,
    @Nullable Double confidence
) implements Entity<FieldObservationId>
```

Records that a naturalist (`observedBy`) encountered an insect at a
taxonomic rank (`subject`) at a point in time. Identity by UUIDv7.
Photographic evidence is optional and lives on `InsectImage` via its
`observationId` link — an observation needs no photo.

`location` is a free-text string (e.g. "Deer Creek, Butte County, CA"),
null for observations without location data. `confidence` is the vision
model's identification confidence (0.0–1.0), null for manual sightings.

"Insects I've collected" is the distinct set of `subject`s across a
naturalist's observations. Multiple observations of the same subject
are allowed (distinct sightings). Collection membership dedups by
`subject`.

Jackson dispatch on `subject` uses `@EXTERNAL_PROPERTY` with a
`subjectRank` discriminator.

Invariants: `entityId(id)`, `identifier(observedBy)`,
`identifier(subject)`, `notNull(observedOn)`.

---

## 9. InsectFeature — typed field mark

```java
public record InsectFeature(
    InsectFeatureId id,
    @UniqueValue String value
) implements Entity<InsectFeatureId>
```

Identity by UUIDv7. The compact constructor normalizes `value` to
lowercase and trimmed so that `"Sucking mouthparts"` and
`"sucking mouthparts"` correctly collide on the `@UniqueValue`
constraint.

The value space is deliberately **open** — no enum, sealed set, or
controlled registry. The same feature string may denote non-homologous
characters in different lineages. Ownership-by-rank via
`InsectFeatureAssignment` carries the disambiguating context.

`of(InsectFeatureId, String)` static factory. Invariants:
`entityId(id)`, `notBlank(value)`.

---

## 10. InsectFeatureAssignment — feature-to-rank link

```java
public record InsectFeatureAssignment(
    InsectFeatureAssignmentId id,
    InsectFeatureId featureId,
    InsectRankName rankName,
    int ordinal
) implements Entity<InsectFeatureAssignmentId>
```

Many-to-many link between an `InsectFeature` and a Linnaean rank.
The pair `(featureId, rankName)` is unique — a feature can only be
assigned to a given rank once.

`ordinal` is per-rank ordering (conspicuous-to-diagnostic), not global.
The assignment at order Hemiptera has its own ordinal sequence
independent of the assignment at family Cicadellidae.

Jackson dispatch on `rankName` uses `@EXTERNAL_PROPERTY` with a
`rank` discriminator.

`of(InsectFeatureAssignmentId, InsectFeatureId, InsectRankName, int)`
static factory. Invariants: `entityId(id)`, `entityId(featureId)`,
`identifier(rankName)`.

---

## 11. InsectTaxonView — sealed ReadModel

```java
public sealed interface InsectTaxonView extends ReadModel
    permits InsectOrderView, InsectFamilyView, InsectGenusView, InsectSpeciesView
```

Catalog-view read model — rank entity + `ImageCollection` +
`FeatureCollection`. Each permit composes a single rank entity with its
photographs and directly assigned features. Identity is the root rank's
typed `InsectRankName`, returned polymorphically by `name()`.

| Permit              | Composes        | Identity            |
|---------------------|-----------------|---------------------|
| `InsectOrderView`   | `InsectOrder`   | `InsectOrderName`   |
| `InsectFamilyView`  | `InsectFamily`  | `InsectFamilyName`  |
| `InsectGenusView`   | `InsectGenus`   | `InsectGenusName`   |
| `InsectSpeciesView` | `InsectSpecies` | `InsectSpeciesName` |

Each permit carries:

- `name(): InsectRankName` — polymorphic identity
- `images(): ImageCollection` — photographs at this rank
- `features(): FeatureCollection` — direct features assigned at this rank
- `belongsTo*(parentView)` predicate (null-tolerant)

`InsectSubspeciesName` is a permit on `InsectRankName` but has no view
permit — no subspecies entity exists yet. The factory returns
`Optional.empty()` for subspecies-rank requests.

---

## 12. Insect — rank-chain ReadModel

```java
public record Insect(
    ImageCollection observations,
    @Nullable InsectOrderView order,
    @Nullable InsectFamilyView family,
    @Nullable InsectGenusView genus,
    @Nullable InsectSpeciesView species,
    LifeStageCollection lifeStages,
    @Nullable InsectCitationView citations
) implements ReadModel
```

Sum-of-parts composition of everything known about an insect at whatever
identification depth has been reached. Not persisted — assembled in memory
from the constituent repositories.

Structural invariants:

- **Required collections** — `observations` and `lifeStages` non-null
  (use `empty()` for zero state).
- **Per-rank descent** — each present rank's own invariants are walked.
- **Ancestor-presence** — when a child rank is set, all ancestor slots
  must also be set.
- **Cross-rank FK consistency** — child's typed parent FK must equal the
  ancestor's name (one check per direct FK: `familyBelongsToOrder`,
  `genusBelongsToFamily`, `speciesBelongsToGenus`).
- **Citation descent** — when `citations` is set, its invariants are
  walked via `whenNotNull`.

`identifiedTo()` returns the most-specific identified rank as
`Optional<InsectRankName>`. `with*` mutators refine the read model
(including `withCitations`). `Insect.empty()` is the zero-state starting
point.

Cross-rank clade invariants (placement-chain monotonicity, metaboly
conformance) are deferred to Phase 3.

---

## 13. InsectCitationView — hierarchy-inherited citations

```java
public record InsectCitationView(
    InsectRankName subject,
    List<RankedCitation> citations
) implements ReadModel
```

Read model assembling all citation associations that apply to a given
rank — both citations attached directly at that rank and citations
inherited from ancestor ranks. Queried via
`insectQuery.citations().findByRankName(rankName)`.

The hierarchy walk resolves ancestry through the parent-only FK chain
(species -> genus -> family -> order) and collects
`CitationAssociation` records from the library domain's
`CitationAssociationQuery` at each level.

### `RankedCitation` (nested ValueObject)

```java
public record RankedCitation(
    Citation citation,
    InsectRankName attachedAt,
    @Nullable String note
) implements ValueObject
```

`citation` carries the full `Citation` kernel type (resolved, not just
the name). `attachedAt` preserves provenance — which level in the
hierarchy the citation was originally attached to. A species query for
`battus-philenor` returns direct citations on the species plus inherited
citations from genus Battus, family Papilionidae, and order Lepidoptera,
each with `attachedAt` identifying the attachment point.

---

## 14. InsectFeatureView — lineage-composite features

```java
public record InsectFeatureView(
    InsectRankName subject,
    List<RankedFeature> features
) implements ReadModel
```

Lineage-composite view of identification features at a given rank —
composites the subject rank's own features with those inherited from
its ancestors via `InsectFeatureAssignment`. Queried via
`insectQuery.features().findByRankName(subject)`.

Ordering contract: ancestor ranks first (most general), descendant
ranks last (most specific). Within a rank, features are ordered by
`ordinal`. The composited list is therefore globally
conspicuous-to-diagnostic.

### `RankedFeature` (nested ValueObject)

```java
public record RankedFeature(
    InsectFeature feature,
    InsectRankName assignedAt,
    int ordinal
) implements ValueObject
```

`feature` is the resolved `InsectFeature` entity. `assignedAt`
identifies which rank in the lineage contributed this feature.
Consumers can group display by rank ("Order-level marks: ... /
Family-level marks: ...").

---

## 15. Life Stage Sub-context

`LifeStage` is a sealed interface — four permitted records — implementing
`NamedEntity<LifeStageName>`. Jackson polymorphic wiring is in place
(`@JsonTypeInfo(property = "kind")`, four `@JsonSubTypes`).

Life stages are **independent entities** queried via
`InsectLifeStageQuery`, not inline fields on `InsectSpecies`. Each stage
carries `parentName: InsectRankName` — can attach to any rank, not just
species.

### Common stage fields (every populated stage)

```
name           : LifeStageName            (composite slug)
parentName     : InsectRankName           (sealed polymorphic FK)
phenology      : StagePhenology           (non-empty windows list)
habitat        : StageHabitat             (HabitatProfile + narrative)
chemistryRole  : @Nullable StageChemistryRole
description    : Description              (four-level Durrell, all four levels non-null)
```

`kind() : LifeStageKind` is provided by each subtype (returns the matching
enum constant). Cross-stage invariants live on the `InsectSpecies`
aggregate root, not on `LifeStage`.

### `LifeStageName` composite identity

Format: `{parent-rank-slug}-{stage-kind-slug}`, e.g.
`battus-philenor-larva`, `green-lacewing-egg`. Parser splits on the **last**
hyphen. Helpers: `speciesName()` and `stageKind()` walk the parts back. Max
length 80.

### Stage-specific fields

| Stage        | Specific components                                                                                                                                            |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EggStage`   | `colorProgression?`, `layingPattern?`, `adaptiveSignificance?`                                                                                                 |
| `LarvaStage` | `feedingStrategy? : FeedingStrategy`, `hostPlants : List<PlantName>`, `parasitoidHosts : List<InsectSpeciesName>`, `remarkableBehavior?`, `instarProgression?` |
| `PupaStage`  | `appearance?`, `diapauseRegulation? : DiapauseRegulation` (sealed), `adaptiveSignificance?`                                                                    |
| `AdultStage` | `feedingHabit? : FeedingHabit`, `nectarSources : List<PlantName>`, `ecologicalRole?`, `lifespan?`                                                              |

`hostPlants` and `parasitoidHosts` are `@NotNull` (empty list valid);
`nectarSources` is `@NotNull` (empty list valid). `?` marks remaining
nullable fields.

`FeedingStrategy` (larva): `PHYTOPHAGOUS, PREDATORY, PARASITOID,
DETRITIVORE, OMNIVOROUS`. `FeedingHabit` (adult): `NECTAR, SAP, HONEYDEW,
POLLEN, PREDATORY, HEMATOPHAGOUS, NON_FEEDING, OMNIVOROUS`.

### `LarvaStage` / `AdultStage` — no prey field

Predatory larvae and adults carry only the strategy/habit label. Prey
relationships are reserved for a future ecology domain. Do **not** invent
`prey`, `preyTargets`, `preySpecies`, etc.

### `AdultStage.nectarSources` is nectar-only

Adult sap, carrion, blood, honeydew feeding are not modeled here. Adding
them means a separate typed field per resource — never a generalization
of `nectarSources`.

### `StagePhenology` and `ActivityWindow`

```
StagePhenology(List<ActivityWindow> windows, @Nullable String notes)
ActivityWindow(MonthDay onset, @Nullable MonthDay peak,
               MonthDay tail, @Nullable String cohortLabel)
```

`MonthDay` serializes as `--MM-dd` (ISO-8601, two leading hyphens).
`windows` is non-empty; `onset` and `tail` are required per window.

**Known framework gap.** `ActivityWindow` does not enforce
`onset <= tail` because `MonthDay` comparison does not handle wrap-around
(overwintering windows span Dec -> Mar). Wrap-around windows are emitted
when biologically correct and are accepted at the boundary today.

### `StageHabitat`

```
profile      : HabitatProfile           (kernel type — zones, moisture,
                                         light, vertical layers)
substrate    : @Nullable String
microclimate : @Nullable String
spatialNotes : @Nullable String
```

`profile` is required and structurally validated (all four enum vocabs
exposed by `kernels/habitat`: `HabitatZone`, `MoistureRegime`,
`LightRegime`, `VerticalLayer`).

### `StageChemistryRole`

```
role  : Role           (ACQUISITION, RETENTION, EXPRESSION, MATERNAL_TRANSFER)
notes : @Nullable String
```

The canonical *Battus philenor* mapping: larva ACQUISITION (host-plant
sequestration of aristolochic acids), pupa RETENTION, adult EXPRESSION,
egg MATERNAL_TRANSFER. Cross-stage coherence is enforced on the
`InsectSpecies` aggregate root, not on `StageChemistryRole`.

### `PupaStage.DiapauseRegulation` — sealed, nullable

```java
sealed interface DiapauseRegulation extends ValueObject permits
        PhotoperiodRegulated,
        FoodWaterContentRegulated,    // the Battus philenor case
        TemperatureRegulated,
        NonDiapausing                 // positively documented absence
```

`DiapauseRegulation` now has `@JsonTypeInfo` / `@JsonSubTypes` wiring
via `valueObjectOrNull` in `PupaStage.invariants()`. Treat `null` as
undocumented; treat `NonDiapausing` as positively absent.

---

## 16. FunctionalGuild — eight ecological roles

```
PARASITOID, PREDATOR, APEX_PREDATOR, POLLINATOR,
DECOMPOSER, FOOD_WEB, MIGRATORY, KEYSTONE
```

A species (or genus, or family) belongs to multiple guilds — hoverfly
is `PREDATOR` (larva) and `POLLINATOR` (adult). `KEYSTONE` flags species
whose loss cascades broadly; *Battus philenor* + *Aristolochia californica*
is the canonical keystone host pair.

Guild assignments now live on `InsectFunctionalRole`, not on
`InsectSpecies`.

---

## 17. Query / Repository / Collection / Command Surface

### `InsectQuery` (public namespace interface)

```java
public interface InsectQuery {
    TaxonViewQuery taxonView();
    SpeciesQuery species();
    ImageQuery images();

    FieldObservationQuery fieldObservations();
    FamilyQuery families();
    GenusQuery genera();
    FunctionalRoleQuery functionalRoles();
    OrderQuery orders();
    CitationQuery citations();

    FeatureQuery features();

    Optional<Insect> getByName(InsectRankName name);

    interface TaxonViewQuery {
        Optional<InsectTaxonView> getByName(InsectRankName name);
    }

    interface SpeciesQuery extends EntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> {
        SpeciesCollection forGenusName(InsectGenusName genusName);
        SpeciesCollection forFamilyName(InsectFamilyName familyName);
    }

    interface ImageQuery extends EntityQuery<InsectImageId, InsectImage, ImageCollection> {
        ImageCollection forParentName(InsectRankName parentName);
    }

    interface FieldObservationQuery
        extends EntityQuery<FieldObservationId, FieldObservation,
        FieldObservationCollection> {
        FieldObservationCollection forNaturalist(NaturalistName observedBy);

        FieldObservationCollection forNaturalistAndSubjects(
            NaturalistName observedBy, Set<InsectRankName> subjects);
    }

    interface FamilyQuery extends EntityQuery<InsectFamilyName, InsectFamily, FamilyCollection> {
        FamilyCollection forOrderName(InsectOrderName orderName);
    }

    interface GenusQuery extends EntityQuery<InsectGenusName, InsectGenus, GenusCollection> {
        GenusCollection forFamilyName(InsectFamilyName familyName);
        GenusCollection forOrderName(InsectOrderName orderName);
    }

    interface FunctionalRoleQuery
        extends EntityQuery<InsectFunctionalRoleId, InsectFunctionalRole,
        FunctionalRoleCollection> {
        FunctionalRoleCollection getByGuild(FunctionalGuild guild);
        Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName);
    }

    interface OrderQuery extends EntityQuery<InsectOrderName, InsectOrder, OrderCollection> {
    }

    interface CitationQuery {
        InsectCitationView findByRankName(InsectRankName rankName);
    }

    interface FeatureQuery {
        Optional<InsectFeatureView> findByRankName(InsectRankName subject);

        Set<InsectRankName> findByFeature(InsectFeatureId featureId);
    }
}
```

The top-level `getByName(InsectRankName)` assembles the full `Insect`
read model — resolves the rank chain, fetches images, life stages,
citations, and returns the composed result. Returns `Optional.empty()`
for unknown names or subspecies-rank names.

### `InsectLifeStageQuery` (public namespace interface)

```java
public interface InsectLifeStageQuery {
    LifeStageEntityQuery lifeStages();

    interface LifeStageEntityQuery
        extends EntityQuery<LifeStageName, LifeStage, LifeStageCollection> {
        LifeStageCollection forParentName(InsectRankName parentName);
    }
}
```

### `InsectCommand` (public namespace interface)

```java
public interface InsectCommand {
    SpeciesCommand species();
    ImageCommand images();

    FieldObservationCommand fieldObservations();

    interface SpeciesCommand extends EntityCommand<InsectSpeciesName, InsectSpecies> {
    }

    interface ImageCommand extends EntityCommand<InsectImageId, InsectImage> {
    }

    interface FieldObservationCommand
        extends EntityCommand<FieldObservationId, FieldObservation> {
    }
}
```

Family and genus commands are not part of the pilot — add them when a
write surface becomes a concrete requirement.

### `InsectRepository` (package-private namespace class)

```java
class InsectRepository {
    // carries final fields + create() factory

    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesName, InsectSpecies> {
        List<InsectSpecies> getByGenusName(InsectGenusName genusName);
    }
    protected interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImage> {
        List<InsectImage> getByParentName(InsectRankName parentName);
    }

    protected interface FieldObservationRepository
        extends EntityRepository<FieldObservationId, FieldObservation> {
        List<FieldObservation> getByNaturalist(NaturalistName observedBy);

        List<FieldObservation> getByNaturalistAndSubjects(
            NaturalistName observedBy, Set<InsectRankName> subjects);
    }
    protected interface FamilyRepository
        extends EntityRepository<InsectFamilyName, InsectFamily> {
        List<InsectFamily> getByOrderName(InsectOrderName orderName);
    }
    protected interface GenusRepository
        extends EntityRepository<InsectGenusName, InsectGenus> {
        List<InsectGenus> getByFamilyName(InsectFamilyName familyName);
    }
    protected interface FunctionalRoleRepository
        extends EntityRepository<InsectFunctionalRoleId, InsectFunctionalRole> {
        List<InsectFunctionalRole> getByGuild(FunctionalGuild guild);
        Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName);
    }
    protected interface OrderRepository
        extends EntityRepository<InsectOrderName, InsectOrder> {
    }

    protected interface FeatureRepository
        extends EntityRepository<InsectFeatureId, InsectFeature> {
    }

    protected interface FeatureAssignmentRepository
        extends EntityRepository<InsectFeatureAssignmentId, InsectFeatureAssignment> {
        List<InsectFeatureAssignment> getByRankName(InsectRankName rankName);

        List<InsectFeatureAssignment> getByFeatureId(InsectFeatureId featureId);
    }
}
```

`InsectRepository` carries final fields for each nested repository and a
`create(...)` static factory — it is instantiable (not non-instantiable
like the standard namespace pattern).

### `InsectEntityCollections` (public namespace interface)

`SpeciesCollection`, `ImageCollection`, `FieldObservationCollection`,
`FeatureCollection`, `FamilyCollection`, `GenusCollection`,
`FunctionalRoleCollection`, `OrderCollection` — all
`final class extends BehavioralCollection<...>`, package-private constructor,
public `of(Collection<...>)` / `empty()` factories.

`ImageGallery extends BehavioralMap<InsectRankName, InsectImage>` — groups
images by their `parentName` for display. `forEntity(InsectRankName)`
returns an `ImageCollection` for a specific rank entity. Two construction
modes: `of(Collection)` for auto-grouping, `grouped(Map)` for
pre-computed grouping.

### `LifeStageRepository`

```java
interface LifeStageRepository {
    interface LifeStageEntityRepository extends EntityRepository<LifeStageName, LifeStage> {
        List<LifeStage> getByParentName(InsectRankName parentName);
    }
}
```

`LifeStageRepository` is declared as an `interface` (not the standard
namespace `class`). Do **not** "fix" this to the standard namespace shape
without checking with the user first — the deviation is a deliberate
investigation.

### N=1 collapse not applied to life stages

`InsectLifeStageEntityCollections` carries one collection
(`LifeStageCollection`) and `InsectLifeStageQuery` carries one nested
query — by the N=1 collapse rule they could be top-level. They remain
namespaced today because the life stage sub-context is expected to grow
secondary entities (moult records, phenology observations). Do not
collapse without coordinating that roadmap.

---

## 18. Clade Integration and Metaboly Resolution

Each rank entity (order, family, genus, species) carries
`@Nullable Clade placedIn`. The clade placement drives life-stage kind
resolution via the clade-DAG walk-up:

```java
// InsectLifeStages.stagesOf walk-up:
//   species.placedIn -> genus.placedIn -> family.placedIn -> order.placedIn
//   first non-null -> resolve MetabolyTrait -> List<LifeStageKind>
```

`InsectClades.traitsFor(Clade)` declares:

- `Holometabola` -> `Holometabolous` -> stages: `[EGG, LARVA, PUPA, ADULT]`
- `Hemiptera` -> `Hemimetabolous` -> stages: `[EGG, NYMPH, ADULT]`

`Metaboly` is a sealed interface in the lifestage package with permits
`Ametabolous`, `Hemimetabolous`, `Holometabolous`.

---

## 19. JSON Catalog Locations (insects-repository-test, NOT insects-api)

The api ships no JSON. Catalog files live in repository-test resources:

```
insects-repository-test/src/main/resources/insects/
  insect-species.json              — InsectSpecies records
  insect-images.json               — InsectImage records keyed by UUIDv7
  insect-orders.json               — InsectOrder records
  insect-families.json             — InsectFamily records
  insect-genera.json               — InsectGenus records
  insect-functional-roles.json     — InsectFunctionalRole records
  insect-features.json             — InsectFeature records keyed by UUIDv7
  insect-feature-assignments.json  — InsectFeatureAssignment records keyed by UUIDv7
  field-observations.json          — FieldObservation records keyed by UUIDv7
  life-stages.json                 — flat LifeStage records keyed by LifeStageName
  NOTES.md                         — acquisition follow-up triage
  images/                          — IMG_*.HEIC / .JPG photo binaries
```

Life stages were previously embedded inline in the species file (`egg`,
`larva`, `pupa`, `adult` fields). They are now independent records in
`life-stages.json` only, loaded by `LifeStageTestEntitySource`.

Catalog conventions:

- `"name": "<slug>"` is the `EntityName` natural key on `NamedEntity`
  records.
- `InsectImage`, `InsectFunctionalRole`, `FieldObservation`,
  `InsectFeature`, and `InsectFeatureAssignment` entries carry `"id"` as
  a UUIDv7 string (the `Entity` identity component).
- `InsectImage` entries carry `"parentRank"` / `"parentName"` for
  polymorphic Jackson dispatch, plus optional `"observationId"`.
- `FieldObservation` entries carry `"subjectRank"` / `"subject"` for
  polymorphic Jackson dispatch, plus `"observedBy"` (NaturalistName slug).
- `InsectFeatureAssignment` entries carry `"rank"` / `"rankName"` for
  polymorphic Jackson dispatch, plus `"featureId"` (UUIDv7 FK).
- `InsectFeature` entries carry `"value"` (normalized lowercase string).
- Enum values serialize by constant name (`"PARASITOID"`,
  `"FOLIAR_BOTH"`, `"FOOD_WATER_CONTENT_REGULATED"`).
- Cross-domain `PlantName` references on `LarvaStage.hostPlants` and
  `AdultStage.nectarSources` are slug strings.
- The `kind` discriminator (`"EGG"`, `"LARVA"`, `"PUPA"`, `"ADULT"`)
  is required on every `LifeStage` entry in `life-stages.json`.

---

## 20. Site Context — Oak Vista (Chico, CA)

The April 2026 ecological assessment seeded the catalog. The catalog now
spans orders, families, genera, and species. Representative species:

| Slug                    | Order       | Metabolous | Note                                          |
|-------------------------|-------------|------------|-----------------------------------------------|
| `battus-philenor`       | Lepidoptera | Holo       | **Keystone**, AA chemistry, *Battus philenor* |
| `hippodamia-convergens` | Coleoptera  | Holo       | Convergent ladybug, confirmed breeding        |
| `colias-eurytheme`      | Lepidoptera | Holo       | Orange sulphur, *Colias eurytheme*            |
| `vanessa-cardui`        | Lepidoptera | Holo       | Painted lady, migratory                       |

Hemimetabolous orders (Blattodea, Hemiptera) — life stages resolve to
`[EGG, NYMPH, ADULT]` via clade DAG.

Species slugs now use binomial form (`battus-philenor`) rather than
common-name form (`pipevine-swallowtail`). Common names are in the
`commonNames` field.

---

## 21. Cross-domain References — by slug only

A consumer of insects-api references its entities by name:

```java
InsectSpeciesName.of("battus-philenor")
InsectGenusName.

of("battus")
InsectFamilyName.

of("papilionidae")
InsectOrderName.

of("lepidoptera")
LifeStageName.

of(InsectSpeciesName.of("battus-philenor"),

LifeStageKind.LARVA)            // "battus-philenor-larva"
    PlantName.

of("aristolochia-californica")          // referenced *from* insects
NaturalistName.

of("patrick-way")                  // referenced *from* insects
```

`LifeStageKind` is the only insects type a consumer can carry by *value*
(it is an enum, not a `NamedEntity`). Anything else that crosses a domain
boundary crosses by `EntityName` slug.

---

## 22. Current State — What's Built, What's Not

**Built and stable.**

- Four-level Linnaean hierarchy: `InsectOrder`, `InsectFamily`,
  `InsectGenus`, `InsectSpecies` — all first-class catalog citizens with
  parent-only FKs.
- `InsectTaxonView` sealed ReadModel with four permits, assembled by
  `InsectTaxonViewFactory` in `insects-core`. Each permit composes
  entity + images + features.
- `Insect` rank-chain ReadModel with structural invariants (ancestor-
  presence, cross-rank FK consistency, per-rank descent, citation
  descent).
- `InsectImage` with polymorphic `InsectRankName parentName` and
  optional `FieldObservationId observationId` link.
- `FieldObservation` — naturalist collection unit with `observedBy`
  (NaturalistName) and `subject` (InsectRankName).
- `InsectFunctionalRole` — cross-rank ecology assignment (PL-11).
- `InsectFeature` and `InsectFeatureAssignment` — typed field marks
  with per-rank ordering (replaces inline `IdentificationFeatures`).
- `InsectFeatureView` — lineage-composite feature read model.
- `InsectQuery` namespace with 10 nested queries + top-level
  `getByName` (taxonView, species, images, fieldObservations,
  families, genera, functionalRoles, orders, citations, features).
- `InsectCommand` namespace (species + images + fieldObservations).
- All eight `BehavioralCollection` types + `ImageGallery` map.
- `LifeStage` sealed family with Jackson polymorphic wiring, independent
  entities with `parentName: InsectRankName`.
- `InsectLifeStageQuery` namespace.
- `InsectCitationView` read model — hierarchy-inherited citations via
  `insectQuery.citations().findByRankName(rankName)`. Walks the
  species->genus->family->order FK chain collecting
  `CitationAssociation` records from the library domain.
- `InsectsDomain` registered with the catalog kernel.
- Clade integration + metaboly resolution via `InsectLifeStages`.
- `InsectsCatalogContribution` — searchable tokens per species
  (slug, genus epithet, scientific binomial, abbreviated binomial,
  common names), plus family/genus/order contributions.
- Vision-assisted identification via `InsectIdentificationService`
  (`insects-core`, `@DomainService`). Photo → structured tool_use →
  `InsectSpecies` + `FieldObservation` + `InsectImage` catalog insert.
  See `docs/briefings/vision-identification.md` for the full pipeline.
- Client-side image resize (1024px, JPEG 0.85) and server-side
  `ImageStorageService` with magic-byte validation (JPEG/PNG/WebP).
- Console routes: `GET/POST /insects/identify` (identify new species),
  `POST /{name}/images` (multipart upload), `POST /{name}/notes`
  (field notes), `POST /{name}/re-identify` (reclassification).

**Known invariant gaps.**

- Cross-rank clade invariants on `Insect` (placement-chain monotonicity,
  metaboly conformance, life-stage kind conformance) are deferred to
  Phase 3.
- Cross-stage invariants on `InsectSpecies` (metabolous-type consistency,
  chemistry-story coherence, `protectedStages` <-> `chemistryRole`
  coherence) are documented but not enforced by `invariants()`.
- `ActivityWindow` does not enforce `onset <= tail` (wrap-around).

**Missing on the api surface.**

- No `InsectSubspecies` entity (the `InsectSubspeciesName` permit exists
  but yields `Optional.empty()` everywhere).
- No family/genus/order/feature/featureAssignment commands on
  `InsectCommand` — species, image, and fieldObservation only. This means
  vision-identified species whose genus is not already in the catalog
  cannot be fully cataloged (the detail page redirect guard handles this).
- No `ChemicalDefense.protectedStages` / per-stage `chemistryRole`
  reconciliation — both encodings exist in parallel by design.
- No `ImageQuery` method to find images by `observationId` — the
  re-identify flow cannot update image `parentName` yet.

---

## 23. Anti-patterns Specific to insects-api

- **Do not invent an `InsectSpeciesId`.** Identity is `InsectSpeciesName`
  (slug). Same for all rank entities — identity is the respective
  `EntityName`, never a surrogate id.
- **Do not invent a `LifeStages` wrapper value object.** Stages are
  independent `LifeStage` entities queried via `InsectLifeStageQuery`.
- **Do not put stage fields back on `InsectSpecies`.** The inline
  `egg`, `larva`, `pupa`, `adult` fields were deliberately removed.
- **Do not put `IdentificationFeatures` back on `InsectSpecies`.**
  Features are now modeled as `InsectFeature` + `InsectFeatureAssignment`
  entities, queried via `InsectQuery.features()`.
- **Do not model hemimetabolous nymphs as a `LifeStage` permit.**
  Nymphs resolve via `LifeStageKind.NYMPH` in metaboly resolution
  but there is no `NymphStage` record permit. Adding one is a
  domain-model change that has not been agreed.
- **Do not invent a `prey` / `preyTargets` field on `LarvaStage` or
  `AdultStage`.** Predatory stages carry only the strategy/habit label;
  prey relationships are reserved for a future ecology domain.
- **Do not generalize `AdultStage.nectarSources`.** Sap, carrion, blood,
  honeydew adult feeding need separate typed fields when added — not a
  rename of `nectarSources` to "foodSources" or similar.
- **Do not put life-stage types in `com.naturalist.insects` directly.**
  They live in `com.naturalist.insects.lifestage` (sub-package). Only
  `LifeStageName` and `LifeStageKind` live in the identifiers module
  under `com.naturalist.insects`.
- **Do not collapse `InsectLifeStageQuery` to a top-level
  `LifeStageQuery`.** The N=1 collapse is intentionally withheld.
- **Do not "fix" `LifeStageRepository` to a class** without confirming
  the experiment is over.
- **Do not promote `InsectImageId` to a cross-domain reference.**
  `Entity` ids do not cross domain boundaries by value. Cross the
  `InsectRankName` slug instead.
- **Do not add a numeric-id field to any rank entity.** All are
  `NamedEntity`; no `id`, no `withId`. There is no `PersistenceId` type
  in Java — surrogate keys are adapter-internal only.
- **Do not put `guilds` or `beneficial` on `InsectSpecies`.** Functional
  ecology lives on `InsectFunctionalRole` (PL-11). Do not move it back.
- **Do not add grandparent FK fields to rank entities.** Each rank
  carries only its immediate parent's FK. Walk the chain to resolve
  ancestors.
- **Do not invent a `FieldObservationName`.** `FieldObservation` is an
  `Entity<FieldObservationId>` (surrogate UUIDv7), not a `NamedEntity`.
  Collection membership is determined by the `subject` field, not by the
  observation's identity.
- **Do not put `observedBy` on `InsectImage`.** The naturalist
  attribution lives on `FieldObservation`; images link to observations
  via `observationId`.
