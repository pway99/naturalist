# insects-domain — Chat Briefing

**Purpose.** Domain vocabulary plus current shape of the `insects-api` module
(species, image, life-stage sub-context), sized for a chat Claude session.
Pair with `docs/briefings/framework-briefing.md` (framework / structural
glue) and, when generating life-stage JSON,
`docs/briefings/insect-lifestage-acquisition.md`.

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time, not extrapolated. If you
need a type not listed here, ask before inventing one.

---

## 1. Module Scope and DAG

```
insects-api  →  framework, identifiers, field-notes, taxonomy, habitat,
                plants-api (PlantName only — typed cross-domain reference)
```

`insects-api` declares `LarvaStage.hostPlants : List<PlantName>` and
`AdultStage.nectarSources : List<PlantName>`, which is why it depends on
`plants-api` (typed slug only — not on plants-core or any plants
repository module). No taxonomy-light shortcut: every species carries a
full `TaxonomicClassification`.

Cross-domain consumers reference insects only through the api — by
`InsectSpeciesName`, `LifeStageName`, or `InsectImageId` (the last one
deliberately rare, since `Entity` ids do not cross domain boundaries by
value — cross the parent's `EntityName` slug instead).

---

## 2. Package Map

```
com.naturalist.insects/
  InsectsDomain                       — DomainId record (catalog kernel hook)
  FunctionalGuild                     — enum (8 ecological roles)
  InsectSpecies                       — @AggregateRoot, NamedEntity<InsectSpeciesName>
                                        + 7 nested ValueObject records
  InsectImage                         — Entity<InsectImageId>
  InsectAggregate                     — Aggregate (species + ImageCollection)
  InsectQuery                         — public namespace interface
                                        (insect / species / images)
  InsectRepository                    — package-private namespace class
                                        (SpeciesRepository, ImageRepository)
  InsectEntityCollections             — public namespace interface
                                        (SpeciesCollection, ImageCollection)

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
    LifeStageRepository               — @Incubating; nested
                                        LifeStageEntityRepository
```

### Identifier locations (in the `identifiers` module, not in insects-api)

| Type                | Package                  |
|---------------------|--------------------------|
| `InsectSpeciesName` | `com.naturalist.insects` |
| `LifeStageName`     | `com.naturalist.insects` |
| `LifeStageKind`     | `com.naturalist.insects` |
| `InsectImageId`     | `com.naturalist.insects` |

The *module* is `identifiers`; the *package* mirrors the home domain.
`LifeStageKind` is an enum (not an EntityName) but lives with the
identifiers because it is a structural component of `LifeStageName`.

---

## 3. The Two NamedEntity Branches + One Surrogate Entity

| Type            | Identity            | Branch                         | Aggregate?                 |
|-----------------|---------------------|--------------------------------|----------------------------|
| `InsectSpecies` | `InsectSpeciesName` | `NamedEntity` (slug)           | **Yes** (`@AggregateRoot`) |
| `LifeStage`     | `LifeStageName`     | `NamedEntity` (composite slug) | No (sealed family)         |
| `InsectImage`   | `InsectImageId`     | `Entity` (UUIDv7)              | No                         |

`InsectAggregate` is the `Aggregate` shell composing `InsectSpecies` plus
`ImageCollection` — its identity is the species's `InsectSpeciesName`. There
is no `InsectAggregate` for life stages today; stages compose directly into
the species record.

### `LifeStageName` composite identity

Format: `{species-slug}-{stage-kind-slug}`, e.g.
`battus-philenor-larva`, `green-lacewing-egg`. Parser splits on the **last**
hyphen. Helpers: `speciesName()` and `stageKind()` walk the parts back. Max
length 80.

Stage entities are referenced by name across the api surface
(`InsectLifeStageQuery.lifeStages().getByName(LifeStageName)`), but they
also appear *by value* nested directly under their parent
`InsectSpecies.egg/larva/pupa/adult` because each stage is biologically
inseparable from its species. Both representations are populated from JSON
in parallel files (see §10).

---

## 4. InsectSpecies — the Aggregate

```java

@AggregateRoot
public record InsectSpecies(
        InsectSpeciesName name,
        TaxonomicClassification taxonomy,
        Description description,
        Set<FunctionalGuild> guilds,
        boolean beneficial,
        @Nullable String sightingNotes,
        @Nullable IdentificationFeatures identificationFeatures,
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult,
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

- `name`, `taxonomy`, `description`, `guilds`, `beneficial` — always
  populated.
- All four stage fields and all eight value-object fields are nullable;
  populated incrementally as the catalog matures. See §5 for nullability
  semantics per stage.
- `beneficialProfile` should only be populated when `beneficial == true`
  (intent constraint, not invariant).
- `chemicalDefense` is populated only on chemically defended species
  (*Battus philenor*, etc.). Empty/default `ChemicalDefense` is not a
  legal alternative encoding — use `null`.

### Behavioral queries

`isPollinator()`, `isKeystone()`, `isBiocontrolAgent()` — convenience
predicates over `guilds`.

### Mutators

`InsectSpecies` carries no `with*` methods today. Updates go through full
record reconstruction at the repository boundary.

### Aggregate invariants (current)

```java
i.entityName(name, "name")
 .

valueObject(taxonomy, "taxonomy")
 .

valueObject(description, "description")
 .

notNull(this,InsectSpecies::guilds, "guilds")
 .

valueObjectOrNull(this,InsectSpecies::identificationFeatures, "identificationFeatures")
 .

valueObjectOrNull(this,InsectSpecies::chemicalDefense, "chemicalDefense")
 .

valueObjectOrNull(this,InsectSpecies::voltinism, "voltinism")
 .

valueObjectOrNull(this,InsectSpecies::habitatProfile, "habitatProfile")
 .

valueObjectOrNull(this,InsectSpecies::habitatRequirements, "habitatRequirements")
 .

valueObjectOrNull(this,InsectSpecies::gardenConnections, "gardenConnections")
 .

valueObjectOrNull(this,InsectSpecies::beneficialProfile, "beneficialProfile")
 .

valueObjectOrNull(this,InsectSpecies::ecologicalSignificance, "ecologicalSignificance");
// Stage children descended conditionally — Constraints has no
// nullable-namedEntity helper today:
if(egg !=null)i.

namedEntity(this,InsectSpecies::egg,   "egg");
if(larva !=null)i.

namedEntity(this,InsectSpecies::larva, "larva");
if(pupa !=null)i.

namedEntity(this,InsectSpecies::pupa,  "pupa");
if(adult !=null)i.

namedEntity(this,InsectSpecies::adult, "adult");
```

The conditional descent is a known framework gap: there is no
`namedEntityOrNull` on `Constraints`. Until it lands, mirror this pattern
on any aggregate composing nullable `NamedEntity` children.

### Cross-stage invariants enforced here, not on the stages

- Metabolous-type consistency (hemimetabolous orders ⇒ `larva == null` and
  `pupa == null`).
- Chemistry-story coherence (EXPRESSION on adult requires upstream
  ACQUISITION on larva or MATERNAL_TRANSFER on egg, etc.).
- `chemicalDefense.protectedStages` should equal the set of stages
  carrying a non-null `chemistryRole`.

These are documented in `domains/insects/CLAUDE.md` and in the
lifestage acquisition briefing (§5). They are not yet enforced as
record-level invariants.

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

## 5. Life Stage Sub-context

`LifeStage` is a sealed interface — four permitted records — implementing
`NamedEntity<LifeStageName>`. Jackson polymorphic wiring is in place
(`@JsonTypeInfo(property = "kind")`, four `@JsonSubTypes`).

### Common stage fields (every populated stage)

```
name           : LifeStageName            (composite slug)
phenology      : StagePhenology           (non-empty windows list)
habitat        : StageHabitat             (HabitatProfile + narrative)
chemistryRole  : @Nullable StageChemistryRole
description    : Description              (four-level Durrell, all four levels non-null)
```

`kind() : LifeStageKind` is provided by each subtype (returns the matching
enum constant). Cross-stage invariants live on the `InsectSpecies`
aggregate root, not on `LifeStage`.

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

**Known Jackson gap.** `DiapauseRegulation` does **not** carry
`@JsonTypeInfo` / `@JsonSubTypes` on the source. JSON catalog entries
must emit `"diapauseRegulation": null` until polymorphic wiring lands.
Treat `null` as undocumented; treat `NonDiapausing` (once writable) as
positively absent. This is the gap to close before back-filling pupal
diapause data into the catalog.

### Nullability semantics on the species

| Field   | `null` means                                                                                                               |
|---------|----------------------------------------------------------------------------------------------------------------------------|
| `egg`   | Egg stage not yet documented at catalog level. Always populate-able.                                                       |
| `larva` | Hemimetabolous (Blattodea, Orthoptera, Hemiptera) ⇒ semantically absent (nymph, not larva). Holometabolous ⇒ undocumented. |
| `pupa`  | Hemimetabolous ⇒ semantically absent (no pupation). Holometabolous ⇒ undocumented.                                         |
| `adult` | Adult stage not yet documented at catalog level. Always populate-able.                                                     |

Hemimetabolous nymphs are explicitly **not** modeled as a stage today.

---

## 6. InsectImage

```java
public record InsectImage(
        InsectImageId name,            // UUIDv7, generated at construction
        InsectSpeciesName insectSpeciesName,   // soft FK to species
        Instant dateAdded,
        FileName resourceName          // bare filename, e.g. "IMG_9047.HEIC"
) implements Entity<InsectImageId>
```

`InsectImage` is the reference implementation of the framework's
`Entity` branch — surrogate UUIDv7 identity, surrogate keys private to
the RDBMS adapter, cross-entity references by `EntityName` slug.

`resourceName` is a `FileName` (kernel `NamedValue<String>`), not a path.
The path prefix `insects/images/` is a stable convention of the bounded
context — compose with `resourceName.path("insects/images/")` at the use
site. `resourceName.nameType()` returns the format suffix
(`"HEIC"`, `"JPG"`) for conversion branching.

`InsectImage` is referenced by surrogate id only in tests and through the
`ImageQuery.getByName(InsectImageId)` port; in practice, consumers fetch
images for a species via `forSpeciesName(InsectSpeciesName)`.

---

## 7. InsectAggregate

```java
public record InsectAggregate(
        InsectSpecies species,
        ImageCollection images
) implements Aggregate
```

Catalog-view assembly — species record + image collection. `images`
is non-null, may be empty (a species can be catalogued without
photographs). Two static factories: `of(species, images)` and
`of(species)` (defaulting to `ImageCollection.empty()`).

Referential integrity between `InsectImage.insectSpeciesName()` and
`InsectSpecies.name()` is the assembly factory's responsibility — see
`InsectAggregateFactory` in `insects-core` for the canonical
factory-placement template (concrete, package-private, no interface, no
`Impl` suffix, never declared in the api).

---

## 8. Query / Repository / Collection Surface

### `InsectQuery` (public namespace interface)

```java
public interface InsectQuery {
    InsectAggregateQuery insect();

    SpeciesQuery species();

    ImageQuery images();

    interface InsectAggregateQuery {
        Optional<InsectAggregate> getByName(InsectSpeciesName name);
    }

    interface SpeciesQuery extends EntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> {
        EntityNameSet<InsectSpeciesName> allSpeciesNames();

        SpeciesCollection getByFunctionalGuild(FunctionalGuild functionalGuild);
    }

    interface ImageQuery extends EntityQuery<InsectImageId, InsectImage, ImageCollection> {
        ImageCollection forSpeciesName(InsectSpeciesName speciesName);
    }
}
```

### `InsectRepository` (package-private namespace class)

```java
class InsectRepository {                       // class (not interface) so
    // nested types can be hidden
    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesName, InsectSpecies> {
        List<InsectSpeciesName> getAllSpeciesNames();

        List<InsectSpecies> getByFunctionalGuild(FunctionalGuild functionalGuild);
    }

    protected interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImage> {
        List<InsectImage> getBySpeciesName(InsectSpeciesName speciesName);
    }
}
```

Reference implementation of the namespace pattern (see
`framework-briefing.md` §5). Foreign packages can use `InsectQuery` but
cannot see `SpeciesRepository` / `ImageRepository`.

### `InsectEntityCollections` (public namespace interface)

`SpeciesCollection` and `ImageCollection` — both `final class extends
BehavioralCollection<…>`, package-private constructor, public
`of(Collection<…>)` / `empty()` factories.

### `InsectLifeStageQuery` (public namespace interface)

```java
public interface InsectLifeStageQuery {
    LifeStageEntityQuery lifeStages();

    interface LifeStageEntityQuery
            extends EntityQuery<LifeStageName, LifeStage, LifeStageCollection> {
        LifeStageCollection forSpeciesName(InsectSpeciesName speciesName);
    }
}
```

### `LifeStageRepository` — INCUBATING

```java
@Incubating("Investigating a pattern where EntityRepositories are nested
within a
single interface")

interface LifeStageRepository {
    interface LifeStageEntityRepository extends EntityRepository<LifeStageName, LifeStage> {
        List<LifeStage> getBySpeciesName(InsectSpeciesName speciesName);
    }
}
```

`LifeStageRepository` is declared as an `interface` (mirror of the
`ElementRepository` experiment in `chemistry-api`), not a `class`. Do
**not** "fix" this to the standard namespace shape (package-private
`class` with nested `protected interface`) without checking with the
user first — both deviations are a deliberate investigation of a
single-entity collapse rule.

### N=1 collapse not applied here

`InsectLifeStageEntityCollections` carries one collection
(`LifeStageCollection`) and `InsectLifeStageQuery` carries one nested
query — by the N=1 collapse rule (`domains/CLAUDE.md` §"API Surface")
they could be top-level. They remain namespaced today because the life
stage sub-context is expected to grow secondary entities (moult records,
phenology observations). Do not collapse without coordinating that
roadmap.

---

## 9. FunctionalGuild — eight ecological roles

```
PARASITOID, PREDATOR, APEX_PREDATOR, POLLINATOR,
DECOMPOSER, FOOD_WEB, MIGRATORY, KEYSTONE
```

A species belongs to multiple guilds — hoverfly is `PREDATOR` (larva)
and `POLLINATOR` (adult). `KEYSTONE` flags species whose loss cascades
broadly; *Battus philenor* + *Aristolochia californica* is the canonical
keystone host pair. Pesticide constraints triggered by `isKeystone()`
are surfaced by the (future) PestManagement application module.

---

## 10. JSON Catalog Locations (insects-repository-test, NOT insects-api)

The api ships no JSON. Catalog files live in repository-test resources:

```
insects-repository-test/src/main/resources/insects/
  insect-species.json     — full InsectSpecies records (egg/larva/pupa/adult inline)
  insect-images.json      — InsectImage records keyed by UUIDv7
  life-stages.json        — flat LifeStage records keyed by LifeStageName
  NOTES.md                — acquisition follow-up triage
  images/                 — IMG_*.HEIC / .JPG photo binaries
```

`insect-species.json` and `life-stages.json` are intentionally redundant:

- The species file composes stages **by value** under each species record
  (`InsectSpecies.egg`, `.larva`, `.pupa`, `.adult`).
- `life-stages.json` exposes the same stage entities as a flat
  `LifeStageName`-keyed collection, loaded by
  `LifeStageTestEntitySource` so `InsectLifeStageQuery` can resolve them
  by name without walking through species records.

Keep both files in sync when adding or modifying a stage. The
`insect-lifestage-acquisition` briefing covers stage-only updates; full
species acquisition has no dedicated briefing yet.

Catalog conventions (per insects `CLAUDE.md`):

- `"name": "<slug>"` is the `InsectSpeciesName` / `LifeStageName`. No
  `id` field on `InsectSpecies` or `LifeStage` — surrogate keys are
  adapter-internal only.
- `InsectImage` entries carry `"name"` as a UUIDv7 string.
- Enum values serialize by constant name (`"PARASITOID"`,
  `"FOLIAR_BOTH"`, `"FOOD_WATER_CONTENT_REGULATED"`).
- Cross-domain `PlantName` references on
  `LarvaStage.hostPlants` and `AdultStage.nectarSources` are slug
  strings — `"aristolochia-californica"`, `"medicago-sativa"`.
- The `kind` discriminator (`"EGG"`, `"LARVA"`, `"PUPA"`, `"ADULT"`)
  is required on every `LifeStage` entry in `life-stages.json`; in
  `insect-species.json` the discriminator is implicit by field name.

---

## 11. Site Context — Oak Vista (Chico, CA)

The April 2026 ecological assessment seeded the catalog. 16 species
documented; the canonical reference set is:

| Slug                   | Order       | Metabolous | Note                                          |
|------------------------|-------------|------------|-----------------------------------------------|
| `tachinid-fly`         | Diptera     | Holo       | Endoparasitoid                                |
| `braconid-wasp`        | Hymenoptera | Holo       | Aphid + lepidopteran parasitoid               |
| `hoverfly`             | Diptera     | Holo       | Aphidophagous larva, nectar adult             |
| `convergent-ladybug`   | Coleoptera  | Holo       | Confirmed breeding April 2026                 |
| `ground-beetle`        | Coleoptera  | Holo       | APEX_PREDATOR                                 |
| `crane-fly`            | Diptera     | Holo       | Saprophagous leatherjacket larva              |
| `field-roach`          | Blattodea   | **Hemi**   | Egg + adult only                              |
| `native-sweat-bee`     | Hymenoptera | Holo       | *Halictus* sp.                                |
| `grey-mining-bee`      | Hymenoptera | Holo       | *Andrena* sp.                                 |
| `valley-carpenter-bee` | Hymenoptera | Holo       | *Xylocopa varipuncta*                         |
| `skipper-butterfly`    | Lepidoptera | Holo       | Hesperiidae family-level                      |
| `painted-lady`         | Lepidoptera | Holo       | *Vanessa cardui* — migratory                  |
| `green-lacewing`       | Neuroptera  | Holo       | Silk-stalked eggs                             |
| `pipevine-swallowtail` | Lepidoptera | Holo       | **Keystone**, *Battus philenor*, AA chemistry |
| `potato-leafhopper`    | Hemiptera   | **Hemi**   | Egg + adult only                              |
| `orange-sulphur`       | Lepidoptera | Holo       | *Colias eurytheme*, Fabaceae host             |

Hemimetabolous orders (Blattodea, Orthoptera, Hemiptera) — `larva` and
`pupa` MUST be `null` on the species record. *Pipevine Swallowtail* +
California Pipevine is the keystone host pair (`isKeystone() == true`,
zero-pesticide rule on Pipevine).

---

## 12. Cross-domain References — by slug only

A consumer of insects-api references its entities by name:

```java
InsectSpeciesName.of("pipevine-swallowtail")
LifeStageName.

of(InsectSpeciesName.of("pipevine-swallowtail"),

LifeStageKind.LARVA)            // "pipevine-swallowtail-larva"
        PlantName.

of("aristolochia-californica")          // referenced *from* insects
```

`LifeStageKind` is the only insects type a consumer can carry by *value*
(it is an enum, not a `NamedEntity`). Anything else that crosses a domain
boundary crosses by `EntityName` slug.

`InsectAggregate` is never referenced cross-domain by value; consumers
fetch by `InsectSpeciesName` through their own port and let the insects
side resolve.

---

## 13. Current State — What's Built, What's Not

**Built and stable.**

- `InsectSpecies` aggregate with full nested value-object graph and four
  composed life-stage children.
- `InsectImage` (the `Entity`-branch reference implementation) +
  `ImageCollection`.
- `InsectAggregate` + `InsectAggregateFactory` (the canonical factory
  template for the project).
- `InsectQuery` namespace + `SpeciesQuery` + `ImageQuery` +
  `InsectAggregateQuery` adapters in `insects-core`.
- `LifeStage` sealed family with Jackson polymorphic wiring on the
  family-level discriminator.
- `InsectLifeStageQuery` namespace + adapter.
- All four `FunctionalGuild` predicates (`isPollinator`, `isKeystone`,
  `isBiocontrolAgent`).
- `InsectsDomain` registered with the catalog kernel.

**Built but explicitly experimental.**

- `LifeStageRepository` — `@Incubating`, declared as an `interface` (not
  the standard namespace `class`). Mirrors the chemistry
  `ElementRepository` experiment.
- `InsectLifeStageEntityCollections` is namespaced even though only one
  collection lives there — see §8 (N=1 collapse not applied).

**Known invariant gaps.**

- Cross-stage invariants on `InsectSpecies` (metabolous-type consistency,
  chemistry-story coherence, `protectedStages` ↔ `chemistryRole`
  coherence) are documented but not enforced by `invariants()`.
- `Constraints.namedEntityOrNull` does not exist; the species aggregate
  uses an `if (stage != null)` ladder for stage descent.
- `ActivityWindow` does not enforce `onset <= tail` (wrap-around).

**Known Jackson gap.**

- `PupaStage.DiapauseRegulation` lacks `@JsonTypeInfo` /
  `@JsonSubTypes`. Catalog entries emit `null` until wiring lands; see
  the lifestage acquisition briefing for the back-fill plan.

**Missing on the api surface.**

- No `withSpecies(...)` / `withGuilds(...)` / etc. on `InsectSpecies`.
  Updates flow through full record reconstruction at the boundary.
- No `ChemicalDefense.protectedStages` / per-stage `chemistryRole`
  reconciliation — both encodings exist in parallel by design pending
  removal of the species-level set once every stage is populated.

These are the points where invented APIs are most likely to creep in. If
chat is asked to "add a `with*` method on `InsectSpecies`" or "wire
`DiapauseRegulation` Jackson polymorphism" or "add a hemimetabolous
nymph stage", the honest answer is "that is a deliberate gap — confirm
the scope before generating code".

---

## 14. Anti-patterns Specific to insects-api

- **Do not invent an `InsectSpeciesId`.** Identity is `InsectSpeciesName`
  (slug). Same for `LifeStage` — it is `LifeStageName` (composite slug),
  never `LifeStageId`.
- **Do not invent a `LifeStages` wrapper value object.** It was removed
  in the refactor that motivated the lifestage acquisition briefing;
  stages are four direct nullable fields on `InsectSpecies`.
- **Do not model hemimetabolous nymphs as a stage.** Hemimetabolous
  species (Blattodea, Orthoptera, Hemiptera) carry `larva == null` and
  `pupa == null`. Adding a `nymph` stage is a domain-model change that
  has not been agreed.
- **Do not invent a `prey` / `preyTargets` field on `LarvaStage` or
  `AdultStage`.** Predatory stages carry only the strategy/habit label;
  prey relationships are reserved for a future ecology domain.
- **Do not generalize `AdultStage.nectarSources`.** Sap, carrion, blood,
  honeydew adult feeding need separate typed fields when added — not a
  rename of `nectarSources` to "foodSources" or similar.
- **Do not put life-stage types in `com.naturalist.insects` directly.**
  They live in `com.naturalist.insects.lifestage` (sub-package). Only
  `LifeStageName` and `LifeStageKind` live in the identifiers module
  under `com.naturalist.insects` because they are EntityName /
  enum-component types shared with the parent package.
- **Do not collapse `InsectLifeStageQuery` to a top-level
  `LifeStageQuery`.** The N=1 collapse is intentional withheld — the
  sub-context is planned to grow.
- **Do not "fix" `LifeStageRepository` to a class** without confirming
  the experiment is over.
- **Do not promote `InsectImageId` to a cross-domain reference.**
  `Entity` ids do not cross domain boundaries by value. Cross the
  species slug instead.
- **Do not add a numeric-id field to `InsectSpecies` or `LifeStage`.**
  Both are `NamedEntity`; no `id`, no `withId`. There is no
  `PersistenceId` type in Java — surrogate keys are adapter-internal
  only.
- **Do not emit `diapauseRegulation` in JSON catalog data.** The
  polymorphic discriminator is not wired; emit `null` until it is.
