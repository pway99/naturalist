# insects-domain — Chat Briefing

**Purpose.** Domain vocabulary plus current shape of the insects module
(four-level Linnaean hierarchy, species, image, functional role, life-stage
sub-context), sized for a chat Claude session.
Pair with `docs/briefings/framework-kernel.md` (framework / structural
glue) and, when generating life-stage JSON,
`docs/briefings/insect-lifestage-acquisition.md`.

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time (2026-06-14), not
extrapolated. If you need a type not listed here, ask before inventing one.

---

## 1. Module Scope and DAG

```
insects-api  →  framework, identifiers, field-notes, taxonomy, habitat,
                clades, plants-api (PlantName only)
```

`insects-api` depends on `plants-api` for `PlantName` (typed slug only —
used by `LarvaStage.hostPlants` and `AdultStage.nectarSources`), and on
`clades` for the `Clade` sealed type (each rank entity carries
`@Nullable Clade placedIn`). No taxonomy-light shortcut: each rank carries
its own `Taxonomic*` epithet from `kernels/taxonomy`.

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
                                        + 7 nested ValueObject records

  InsectImage                         — Entity<InsectImageId>
  InsectFunctionalRole                — Entity<InsectFunctionalRoleId>

  InsectTaxonView                     — sealed ReadModel interface
    InsectOrderView / InsectFamilyView  — record permits (rank + images)
    InsectGenusView / InsectSpeciesView — record permits (rank + images)

  InsectCitationView                  — ReadModel (hierarchy-inherited citations)
                                        + nested RankedCitation ValueObject
  Insect                              — ReadModel (rank-chain composite)

  InsectQuery                         — public namespace interface
                                        (taxonView / species / images /
                                         families / genera / functionalRoles /
                                         orders / citations)
  InsectCommand                       — public namespace interface
                                        (species / images)
  InsectRepository                    — package-private namespace class
                                        (SpeciesRepository, ImageRepository,
                                         FamilyRepository, GenusRepository,
                                         FunctionalRoleRepository,
                                         OrderRepository)
  InsectEntityCollections             — public namespace interface
                                        (SpeciesCollection, ImageCollection,
                                         ImageGallery, FamilyCollection,
                                         GenusCollection,
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

| Type                     | Package                  |
|--------------------------|--------------------------|
| `InsectRankName`         | `com.naturalist.insects` |
| `InsectOrderName`        | `com.naturalist.insects` |
| `InsectFamilyName`       | `com.naturalist.insects` |
| `InsectGenusName`        | `com.naturalist.insects` |
| `InsectSpeciesName`      | `com.naturalist.insects` |
| `InsectSubspeciesName`   | `com.naturalist.insects` |
| `InsectImageId`          | `com.naturalist.insects` |
| `InsectFunctionalRoleId` | `com.naturalist.insects` |
| `LifeStageName`          | `com.naturalist.insects` |
| `LifeStageKind`          | `com.naturalist.insects` |

The *module* is `identifiers`; the *package* mirrors the home domain.
`InsectRankName` is a sealed interface over the five rank names (order,
family, genus, species, subspecies); it carries `value()` and `rank()`
for polymorphic slug/rank access without down-casting.
`LifeStageKind` is an enum (not an EntityName) but lives with the
identifiers because it is a structural component of `LifeStageName`.

---

## 3. Entity Summary

| Type                   | Identity                 | Branch                         | DDD role                           |
|------------------------|--------------------------|--------------------------------|------------------------------------|
| `InsectOrder`          | `InsectOrderName`        | `NamedEntity` (slug)           | Rank entity (hierarchy root)       |
| `InsectFamily`         | `InsectFamilyName`       | `NamedEntity` (slug)           | Rank entity (parent-only FK)       |
| `InsectGenus`          | `InsectGenusName`        | `NamedEntity` (slug)           | Rank entity (parent-only FK)       |
| `InsectSpecies`        | `InsectSpeciesName`      | `NamedEntity` (slug)           | **@AggregateRoot** (value objects) |
| `InsectImage`          | `InsectImageId`          | `Entity` (UUIDv7, component `id`) | Observation record                 |
| `InsectFunctionalRole` | `InsectFunctionalRoleId` | `Entity` (UUIDv7, component `id`) | Cross-rank ecology assignment      |
| `LifeStage` (sealed)   | `LifeStageName`          | `NamedEntity` (composite slug) | Sealed family (4 permits)          |
| `InsectTaxonView`      | `InsectRankName`         | `ReadModel` (sealed)           | Rank + images view (4 permits)     |
| `InsectCitationView`   | (no identity)            | `ReadModel`                    | Hierarchy-inherited citations      |
| `Insect`               | (no identity)            | `ReadModel`                    | Rank-chain composite               |

### Parent-only FK chain (no grandparent skip-level references)

```
InsectOrder  (no parent FK — hierarchy root)
  ← InsectFamily.orderName
    ← InsectGenus.familyName
      ← InsectSpecies.genusName
```

Each rank carries only its immediate parent's `EntityName` FK.
Grandparent resolution (e.g. species → order) requires walking the
chain through intermediate entities.

### Cross-rank `InsectRankName` pattern

`InsectImage`, `InsectFunctionalRole`, and each `LifeStage` permit carry
`parentName: InsectRankName` — a sealed polymorphic FK that can point to
any insect rank (order, family, genus, species, or future subspecies).
Jackson dispatch is declared at the consuming field
(`@JsonTypeInfo(As.EXTERNAL_PROPERTY)`) with a `parentRank` discriminator,
not on the `InsectRankName` interface itself.

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
        @Nullable IdentificationFeatures identificationFeatures,
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

### What changed from the old briefing

- `taxonomy: TaxonomicClassification` replaced by `genusName: InsectGenusName`
  (typed upward FK) + `epithet: TaxonomicSpecies` (species epithet).
- `guilds: Set<FunctionalGuild>` and `beneficial: boolean` moved out to the
  separate `InsectFunctionalRole` entity (PL-11). Guild predicates
  (`isPollinator()`, `isKeystone()`, etc.) are gone from this record.
- `commonNames: Set<CommonName>` added for locale-tagged vernacular names.
- Inline stage fields (`egg`, `larva`, `pupa`, `adult`) **removed**. Life
  stages are now independent `LifeStage` entities queried via
  `InsectLifeStageQuery`, attached by `parentName: InsectRankName`.
- `withPlacedIn(Clade)` mutator added.

### Required vs nullable fields

- `name`, `genusName`, `epithet`, `description`, `commonNames` — always
  populated.
- All seven value-object fields are nullable; populated incrementally as
  the catalog matures.
- `beneficialProfile` should only be populated when the species's
  `InsectFunctionalRole.beneficial()` is `true` (intent constraint, not
  invariant).
- `chemicalDefense` is populated only on chemically defended species
  (*Battus philenor*, etc.). Empty/default `ChemicalDefense` is not a
  legal alternative encoding — use `null`.

### Nested value-object graph

All seven owned exclusively by `InsectSpecies`; each is a `static record`
inside `InsectSpecies.java`:

| Type                     | Required components                      | Notes                                                 |
|--------------------------|------------------------------------------|-------------------------------------------------------|
| `IdentificationFeatures` | `List<String> features`                  | Ordered conspicuous → diagnostic                      |
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
    FileName resourceName
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

---

## 8. InsectTaxonView — sealed ReadModel

```java
public sealed interface InsectTaxonView extends ReadModel
    permits InsectOrderView, InsectFamilyView, InsectGenusView, InsectSpeciesView
```

Catalog-view read model — rank entity + `ImageCollection`. Each permit
composes a single rank entity with its photographs. Identity is the root
rank's typed `InsectRankName`, returned polymorphically by `name()`.

| Permit              | Composes        | Identity            |
|---------------------|-----------------|---------------------|
| `InsectOrderView`   | `InsectOrder`   | `InsectOrderName`   |
| `InsectFamilyView`  | `InsectFamily`  | `InsectFamilyName`  |
| `InsectGenusView`   | `InsectGenus`   | `InsectGenusName`   |
| `InsectSpeciesView` | `InsectSpecies` | `InsectSpeciesName` |

Each permit carries a `belongsTo*(parentView)` predicate (null-tolerant)
used by the `Insect` read model's cross-rank FK invariants.

`InsectSubspeciesName` is a permit on `InsectRankName` but has no view
permit — no subspecies entity exists yet. The factory returns
`Optional.empty()` for subspecies-rank requests.

---

## 9. Insect — rank-chain ReadModel

```java
public record Insect(
    ImageCollection observations,
    @Nullable InsectOrderView order,
    @Nullable InsectFamilyView family,
    @Nullable InsectGenusView genus,
    @Nullable InsectSpeciesView species,
    LifeStageCollection lifeStages
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

`identifiedTo()` returns the most-specific identified rank as
`Optional<InsectRankName>`. `with*` mutators refine the read model.
`Insect.empty()` is the zero-state starting point.

Cross-rank clade invariants (placement-chain monotonicity, metaboly
conformance) are deferred to Phase 3.

---

## 10. InsectCitationView — hierarchy-inherited citations

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
(species → genus → family → order) and collects
`CitationAssociation` records from the library domain's
`CitationAssociationQuery` at each level.

### `RankedCitation` (nested ValueObject)

```java
public record RankedCitation(
    CitationName citationName,
    InsectRankName attachedAt,
    @Nullable String note
) implements ValueObject
```

`attachedAt` preserves provenance — which level in the hierarchy the
citation was originally attached to. A species query for
`battus-philenor` returns direct citations on the species plus inherited
citations from genus Battus, family Papilionidae, and order Lepidoptera,
each with `attachedAt` identifying the attachment point.

---

## 11. Life Stage Sub-context

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
(overwintering windows span Dec → Mar). Wrap-around windows are emitted
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

## 12. FunctionalGuild — eight ecological roles

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

## 13. Query / Repository / Collection / Command Surface

### `InsectQuery` (public namespace interface)

```java
public interface InsectQuery {
    TaxonViewQuery taxonView();
    SpeciesQuery species();
    ImageQuery images();

    FamilyQuery families();

    GenusQuery genera();

    FunctionalRoleQuery functionalRoles();

    OrderQuery orders();

    CitationQuery citations();

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

    interface FamilyQuery extends EntityQuery<InsectFamilyName, InsectFamily, FamilyCollection> {
        FamilyCollection forOrderName(InsectOrderName orderName);
    }

    interface GenusQuery extends EntityQuery<InsectGenusName, InsectGenus, GenusCollection> {
        GenusCollection forFamilyName(InsectFamilyName familyName);

        GenusCollection forOrderName(InsectOrderName orderName);
    }

    interface FunctionalRoleQuery
        extends EntityQuery<InsectFunctionalRoleId, InsectFunctionalRole, FunctionalRoleCollection> {
        FunctionalRoleCollection getByGuild(FunctionalGuild guild);

        Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName);
    }

    interface OrderQuery extends EntityQuery<InsectOrderName, InsectOrder, OrderCollection> {
    }

    interface CitationQuery {
        InsectCitationView findByRankName(InsectRankName rankName);
    }
}
```

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

    interface SpeciesCommand extends EntityCommand<InsectSpeciesName, InsectSpecies> {
    }

    interface ImageCommand extends EntityCommand<InsectImageId, InsectImage> {
    }
}
```

Family and genus commands are not part of the pilot — add them when a
write surface becomes a concrete requirement.

### `InsectRepository` (package-private namespace class)

```java
class InsectRepository {
    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesName, InsectSpecies> {
        List<InsectSpecies> getByGenusName(InsectGenusName genusName);
    }
    protected interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImage> {
        List<InsectImage> getByParentName(InsectRankName parentName);
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
}
```

### `InsectEntityCollections` (public namespace interface)

`SpeciesCollection`, `ImageCollection`, `FamilyCollection`,
`GenusCollection`, `FunctionalRoleCollection`, `OrderCollection` — all
`final class extends BehavioralCollection<…>`, package-private constructor,
public `of(Collection<…>)` / `empty()` factories.

`ImageGallery extends BehavioralMap<InsectRankName, InsectImage>` — groups
images by their `parentName` for display. `forEntity(InsectRankName)`
returns an `ImageCollection` for a specific rank entity.

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

## 14. Clade Integration and Metaboly Resolution

Each rank entity (order, family, genus, species) carries
`@Nullable Clade placedIn`. The clade placement drives life-stage kind
resolution via the clade-DAG walk-up:

```java
// InsectLifeStages.stagesOf walk-up:
//   species.placedIn → genus.placedIn → family.placedIn → order.placedIn
//   first non-null → resolve MetabolyTrait → List<LifeStageKind>
```

`InsectClades.traitsFor(Clade)` declares:

- `Holometabola` → `Holometabolous` → stages: `[EGG, LARVA, PUPA, ADULT]`
- `Hemiptera` → `Hemimetabolous` → stages: `[EGG, NYMPH, ADULT]`

`Metaboly` is a sealed interface in the lifestage package with permits
`Ametabolous`, `Hemimetabolous`, `Holometabolous`.

---

## 15. JSON Catalog Locations (insects-repository-test, NOT insects-api)

The api ships no JSON. Catalog files live in repository-test resources:

```
insects-repository-test/src/main/resources/insects/
  insect-species.json         — InsectSpecies records
  insect-images.json          — InsectImage records keyed by UUIDv7
  insect-orders.json          — InsectOrder records
  insect-families.json        — InsectFamily records
  insect-genera.json          — InsectGenus records
  insect-functional-roles.json — InsectFunctionalRole records
  life-stages.json            — flat LifeStage records keyed by LifeStageName
  NOTES.md                    — acquisition follow-up triage
  images/                     — IMG_*.HEIC / .JPG photo binaries
```

Life stages were previously embedded inline in the species file (`egg`,
`larva`, `pupa`, `adult` fields). They are now independent records in
`life-stages.json` only, loaded by `LifeStageTestEntitySource`.

Catalog conventions:

- `"name": "<slug>"` is the `EntityName` natural key on `NamedEntity`
  records.
- `InsectImage` and `InsectFunctionalRole` entries carry `"id"` as a
  UUIDv7 string (the `Entity` identity component), plus `"parentRank"` /
  `"parentName"` for polymorphic Jackson dispatch.
- Enum values serialize by constant name (`"PARASITOID"`,
  `"FOLIAR_BOTH"`, `"FOOD_WATER_CONTENT_REGULATED"`).
- Cross-domain `PlantName` references on `LarvaStage.hostPlants` and
  `AdultStage.nectarSources` are slug strings.
- The `kind` discriminator (`"EGG"`, `"LARVA"`, `"PUPA"`, `"ADULT"`)
  is required on every `LifeStage` entry in `life-stages.json`.

---

## 16. Site Context — Oak Vista (Chico, CA)

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

## 17. Cross-domain References — by slug only

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
```

`LifeStageKind` is the only insects type a consumer can carry by *value*
(it is an enum, not a `NamedEntity`). Anything else that crosses a domain
boundary crosses by `EntityName` slug.

---

## 18. Current State — What's Built, What's Not

**Built and stable.**

- Four-level Linnaean hierarchy: `InsectOrder`, `InsectFamily`,
  `InsectGenus`, `InsectSpecies` — all first-class catalog citizens with
  parent-only FKs.
- `InsectTaxonView` sealed ReadModel with four permits, assembled by
  `InsectTaxonViewFactory` in `insects-core`.
- `Insect` rank-chain ReadModel with structural invariants (ancestor-
  presence, cross-rank FK consistency, per-rank descent).
- `InsectImage` with polymorphic `InsectRankName parentName`.
- `InsectFunctionalRole` — cross-rank ecology assignment (PL-11).
- `InsectQuery` namespace with 8 nested queries (taxonView, species,
  images, families, genera, functionalRoles, orders, citations).
- `InsectCommand` namespace (species + images).
- All six `BehavioralCollection` types + `ImageGallery` map.
- `LifeStage` sealed family with Jackson polymorphic wiring, independent
  entities with `parentName: InsectRankName`.
- `InsectLifeStageQuery` namespace.
- `InsectCitationView` read model — hierarchy-inherited citations via
  `insectQuery.citations().findByRankName(rankName)`. Walks the
  species→genus→family→order FK chain collecting
  `CitationAssociation` records from the library domain.
- `InsectsDomain` registered with the catalog kernel.
- Clade integration + metaboly resolution via `InsectLifeStages`.
- `InsectsCatalogContribution` — searchable tokens per species
  (slug, genus epithet, scientific binomial, abbreviated binomial,
  common names), plus family/genus/order contributions.

**Known invariant gaps.**

- Cross-rank clade invariants on `Insect` (placement-chain monotonicity,
  metaboly conformance, life-stage kind conformance) are deferred to
  Phase 3.
- Cross-stage invariants on `InsectSpecies` (metabolous-type consistency,
  chemistry-story coherence, `protectedStages` ↔ `chemistryRole`
  coherence) are documented but not enforced by `invariants()`.
- `ActivityWindow` does not enforce `onset <= tail` (wrap-around).

**Missing on the api surface.**

- No `InsectSubspecies` entity (the `InsectSubspeciesName` permit exists
  but yields `Optional.empty()` everywhere).
- No family/genus commands on `InsectCommand` — species and image only.
- No `ChemicalDefense.protectedStages` / per-stage `chemistryRole`
  reconciliation — both encodings exist in parallel by design.

---

## 19. Anti-patterns Specific to insects-api

- **Do not invent an `InsectSpeciesId`.** Identity is `InsectSpeciesName`
  (slug). Same for all rank entities — identity is the respective
  `EntityName`, never a surrogate id.
- **Do not invent a `LifeStages` wrapper value object.** Stages are
  independent `LifeStage` entities queried via `InsectLifeStageQuery`.
- **Do not put stage fields back on `InsectSpecies`.** The inline
  `egg`, `larva`, `pupa`, `adult` fields were deliberately removed.
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
