# LifeStage Refactor — Design Document

**Status:** Draft sketch for review
**Scope:** Promoting `LifeStage` from nested value object to a first-class entity with its own sub-context inside
`insects-api`

---

## 1. Why this refactor

The current `InsectSpecies.LifeStages` is a `ValueObject` wrapping four nested stage value objects. As ecological
modeling needs expanded — stage-specific phenology, habitat, chemistry participation, host plants, diapause regulation —
the value-object shape became insufficient:

- Each stage has enough internal structure to justify its own invariants.
- Stages need stable, addressable identity for cross-domain references and for the ecological DAG layer.
- Cross-stage relationships (e.g. chemistry story coherence) are already straining the model.
- ADR-013 forbids a `ValueObject` from owning entities. The moment stages become entities, `LifeStages` must either
  reclassify or be removed.

The decision is to promote each stage to a `NamedEntity`, remove the `LifeStages` wrapper entirely, and have
`InsectSpecies` hold four nullable stage entities directly.

---

## 2. Sub-context vs. sub-module

`com.naturalist.insects.lifestages` is a **sub-context within `insects-api`** — a Java package, not a separate Maven
module, not a separate bounded context in the strict DDD sense.

The pragmatic framing: a Java package within a module functions as a sub-context because package boundaries are where
Java gives meaningful structural tools (package-private visibility, import discipline, package-info documentation,
ArchUnit rules). Treating them as sub-contexts is honest about what the code is doing and uses Java's strengths.

This is not encoded in any ADR today. If the pattern recurs (e.g. `com.naturalist.plants.propagation`,
`com.naturalist.chemistry.organic`), it probably deserves an ADR codifying the convention.

---

## 3. Identity: `LifeStageName`

Per ADR-022, the identity question is: *does this thing have a natural key?* For a life stage, yes: a
`(parentRankName, LifeStageKind)` pair, where `parentRankName` is whichever insect-rank name — family, genus,
species, or subspecies — the field naturalist's confidence allowed. The identity is biologically determined, stable
before persistence, and survives across deployments.

Therefore `LifeStage` is a `NamedEntity<LifeStageName>`.

### Composite slug form

ADR-005 Amendment 3 (carried forward through ADR-022) requires `EntityName` slugs to satisfy
`^[a-z0-9]+(-[a-z0-9]+)*$` — a single string. The composite identity is encoded as a compound slug:

```
battus-philenor-larva
chrysoperla-rufilabris-egg
apis-mellifera-adult
```

Structure is recovered through accessors:

```java
LifeStageName name = LifeStageName.of(InsectSpeciesName.of("battus-philenor"), LifeStageKind.LARVA);
name.value();          // "battus-philenor-larva"
name.parentSlug();     // "battus-philenor" — the rank-prefix slug; rank itself is consumer-known
name.stageKind();      // LifeStageKind.LARVA
```

Rank-flexible parent: `LifeStageName.of(...)` is overloaded over `InsectSpeciesName`,
`InsectGenusName`, and `InsectFamilyName`, so a stage may be keyed under any rank
(`battus-philenor-larva` at species rank, `chrysoperla-larva` at genus rank,
`syrphidae-larva` at family rank). `parentSlug()` returns the raw prefix; it carries
no rank discriminator because the slug itself doesn't. The query surface accepts the
sealed `InsectRankName` marker — `insectLifeStageQuery.lifeStages().forParentName(name)`
— and matches by slug.

Parsing splits on the **last** hyphen. This works because `LifeStageKind` slugs are always single tokens without
internal hyphens.

### Location

`LifeStageName` and `LifeStageKind` live in the **identifiers module** alongside `InsectSpeciesName`, `PlantName`,
`CompoundName`, and every other cross-domain identifier. They are not in `insects-api`.

Rationale: `LifeStageKind` is a component of a cross-domain identifier. If it lived in `insects-api`, then `identifiers`
would need to reference `insects`, creating a reverse dependency. Keeping identifier vocabulary alongside identifiers
preserves the dependency direction.

Per ADR-014 Tier 3, this is a "universal cross-module need" — the exact kind of type that belongs in the `identifiers`
module.

### ⚠ Caveat: composite `EntityName` is novel

No other `EntityName` subclass in the codebase (that I've seen) uses composite structure. Every other identifier is a
single-token concept. This one has structure inside the slug.

The `ADR-005 Amendment 3` slug regex still holds, so it's technically compliant — but it's a deliberate departure from
precedent worth flagging. If the pattern recurs (e.g. if the ecology domain needs composite identifiers), it deserves
ADR treatment.

---

## 4. The sealed hierarchy

```
LifeStage (sealed interface)
├── EggStage       (final record)
├── LarvaStage     (final record)
├── PupaStage      (final record)
└── AdultStage     (final record)
```

Sealed types give us exhaustive pattern matching. When the DAG layer walks a species' stages to emit edges,
`switch (stage)` is compiler-checked to handle every case. Adding a fifth stage (e.g. `NymphStage` for hemimetabolous
species) would force every consumer to handle it — a feature, not a bug.

Records are implicitly final, so no extra keyword on the permitted subtypes.

### Shared contract

Every stage provides:

- `LifeStageName name()` — natural-key identity
- `LifeStageKind kind()` — self-identification into the vocabulary
- `StagePhenology phenology()` — seasonal timing
- `StageHabitat habitat()` — where it lives
- `@Nullable StageChemistryRole chemistryRole()` — role in species chemistry story
- `Description description()` — four-resolution narrative

### Stage-specific structure

Held on the concrete subtypes, not on the interface:

- `EggStage`: `colorProgression`, `layingPattern`, `adaptiveSignificance`
- `LarvaStage`: `feedingStrategy`, `hostPlants`, `parasitoidHosts`, `remarkableBehavior`, `instarProgression`
- `PupaStage`: `appearance`, `diapauseRegulation`, `adaptiveSignificance`
- `AdultStage`: `feedingHabit`, `nectarSources`, `ecologicalRole`, `lifespan`

### Why not further seal `LarvaStage`?

`LarvaStage` could in principle be sealed into `PhytophagousLarva`, `PredatoryLarva`, `ParasitoidLarva`, etc. — feeding
strategy as type rather than field.

**Not doing this yet.** The argument for it is that feeding strategy drives which relationships exist (host plants vs.
prey vs. host hosts). The argument against is premature abstraction — the DAG layer doesn't exist yet and hasn't
articulated concrete needs. Start with a single `LarvaStage` carrying a `FeedingStrategy` enum; seal deeper when the
domain forces it.

This is reversible in one direction (flat → sealed is easy) and costly in the other (sealed → flat loses callers). Start
simple.

---

## 5. Stage-shared value objects

Three VOs live in the `lifestages` package, reused across all four stages:

### `StagePhenology`

Replaces the current narrative strings (`flightPeriod`, etc.) with structured activity windows:

```java
record StagePhenology(List<ActivityWindow> windows, @Nullable String notes)
record ActivityWindow(MonthDay onset,

        @Nullable
        MonthDay peak, MonthDay
tail,
@Nullable
String cohortLabel)
```

Multi-window because many species have multi-peak flights. `cohortLabel` distinguishes semantically distinct cohorts —
the *Battus philenor* pupa carries two windows labeled `"direct-developer"` and `"diapauser"`.

`MonthDay` is used rather than `LocalDate` — phenology is year-agnostic.

**Known gap:** `ActivityWindow` doesn't enforce `onset` before `tail` because `MonthDay` comparison doesn't handle
wrap-around (legitimate overwintering windows span December→March). The correct solution is a `CalendarRange` type in
the kernel. Deferred.

### `StageHabitat`

Wraps the `habitat` kernel's `HabitatProfile` with insect-specific narrative detail:

```java
record StageHabitat(HabitatProfile profile, @Nullable String substrate,
                    @Nullable String microclimate, @Nullable String spatialNotes)
```

The structured profile is what cross-domain habitat queries reason over. The narrative fields capture insect-specific
detail the profile cannot express (leaf underside, stem attachment, microclimate narrower than the containing zone).

### `StageChemistryRole`

The stage's participation in the species chemistry story:

```java
record StageChemistryRole(Role role, @Nullable String notes)

enum Role {ACQUISITION, RETENTION, EXPRESSION, MATERNAL_TRANSFER}
```

*Battus philenor* as the test case:

- Larva: `ACQUISITION` (sequesters aristolochic acids from *Aristolochia*)
- Pupa: `RETENTION` (preserves through metamorphosis)
- Adult: `EXPRESSION` (wing scales, hemolymph, aposematic signaling)
- Egg: `MATERNAL_TRANSFER` (deposited in chorion, brick-red coloration)

The species-level `ChemicalDefense` continues to hold mechanism, source compounds, and aposematic signal. The per-stage
role on each stage is the projection. `ChemicalDefense.protectedStages: Set<LifeStageKind>` becomes derivable by walking
stages — that redundant field should be removed.

Cross-stage consistency (`EXPRESSION` requires upstream `ACQUISITION`, etc.) is enforced at the `InsectSpecies`
aggregate root because it needs visibility into multiple stages.

---

## 6. The `PupaStage.DiapauseRegulation` sealed sub-hierarchy

The pupa is the showcase for sealed hierarchies at the value-object level:

```
DiapauseRegulation (sealed interface extends ValueObject)
├── PhotoperiodRegulated        (criticalDaylength, chillRequirement)
├── FoodWaterContentRegulated   (mechanism, cohortSplitNotes)  ← Battus philenor
├── TemperatureRegulated        (entryThreshold, exitThreshold)
└── NonDiapausing               ()
```

Each mechanism has structurally different fields. Modeling them as a flat record with nullable fields per mechanism
loses the guarantee that each case carries the right fields. Sealed hierarchy expresses the biology directly.

**`NonDiapausing` vs. null:** Null `diapauseRegulation` means "not yet documented." `NonDiapausing` means "positively
documented as not diapausing." This is a pattern worth committing to across the codebase or rejecting explicitly — it
surfaces every time a "documented absence" is ecologically meaningful.

---

## 7. Host plants and nectar sources — typed cross-domain references

- `LarvaStage.hostPlants: List<PlantName>` — phytophagous specialization as catalog trait
- `LarvaStage.parasitoidHosts: List<InsectSpeciesName>` — parasitoid host specialization
- `AdultStage.nectarSources: List<PlantName>` — nectar is produced only by flowering plants; the type reflects this

**Implication:** `insects-api` gains a compile-time dependency on `plants-api` (or wherever `PlantName` lives). This
should be reflected in the ADR-004 module DAG if not already. Direction is insects → plants (acyclic).

**Non-nectar adult feeding** (sap, honeydew, carrion, blood) is not modeled as a generalization of nectar — when needed,
each becomes a separate typed field (e.g. `sapSources`) with its own semantic type. Keeping these distinct avoids
overload.

---

## 8. Prey — deferred to a future ecology domain

Predatory larvae and adult predators have **no prey field** on their stage records.

### Why prey is different from host plants

Host-plant specialization is a stable species-level trait. *Battus philenor* feeds on *Aristolochia* as a species-level
fact. `List<PlantName>` on the stage reflects this.

Prey is different:

1. **Polymorphic targets.** Prey spans most of the biological kingdom — insects, arachnids, molluscs, worms,
   vertebrates. No single typed list covers it.
2. **Context-dependent.** Observed prey at Oak Vista is not a stable species-level trait. It depends on what's
   available, preference strength, seasonal variation.
3. **Prey is a node, not a leaf.** When the DAG walks from a predator to its prey, the prey has its own outbound
   ecological relationships. "Prey" is an inbound edge to another branch of the graph, not a terminal reference.

### The resolution: prey is a relationship entity

Prey predation will be modeled as `PreyRelationship` entities in a future `ecology` (or `trophic`, or `relationships`)
domain:

```java
record PreyRelationship(
    PreyRelationshipId id,          // Entity<PreyRelationshipId>, UUIDv7
    LifeStageName predator,          // pointer to the predator stage
    PreyReference prey,              // sealed: polymorphic across domains
    // metadata: preference, context, observed at, etc.
)
```

### The `PreyReference` sealed wrapper pattern

A role-wrapper sum type rather than a supertype of existing identifiers:

```java
sealed interface PreyReference
    permits InsectPrey, VertebratePrey, ArachnidPrey, MolluscPrey, WormPrey { }

record InsectPrey(InsectSpeciesName species) implements PreyReference { }
record VertebratePrey(VertebrateSpeciesName species) implements PreyReference { }
// ...
```

**Important:** `InsectSpeciesName` does **not** extend `PreyName`. That would be a category error — an insect species
name isn't structurally a prey name. It's an insect species name that can *play the role* of prey in a relationship. The
wrapper encodes the role; the underlying identifier stays clean.

### The general pattern this points to

Ecological roles are not identifier types. They are relationship-endpoint sum types in the ecology domain:

- `PreyReference`, `PollinatorReference`, `HostReference`, `CompetitorReference` — all sealed sum types wrapping catalog
  identifiers
- Relationship entities (`PreyRelationship`, `PollinationRelationship`, etc.) use these wrappers to type polymorphic
  endpoints
- The DAG layer reads relationship entities, unwraps the sum types, dispatches to catalog domains to resolve vertex
  details

This pattern deserves its own ADR when the ecology domain is first built.

### What this means right now

- `LarvaStage` has no prey field. `FeedingStrategy.PREDATORY` marks the stage as predatory; the actual prey species are
  future inbound edges.
- `AdultStage` has no prey field. Same reasoning.
- `AdultStage.feedingHabit` can be `PREDATORY`; same treatment.
- `LifeStage.md` records the decision so it's not re-litigated.

---

## 9. Vertices and edges — the identity split

The refactor exposes a clean identity discipline that falls out of ADR-022:

- **Vertices in the ecological DAG** are `NamedEntity<NAME extends EntityName>` — species, life stages, plants,
  compounds. They exist before the graph observes them. Slug identity.
- **Edges in the ecological DAG** are `Entity<ID extends EntityId>` with UUIDv7 — recorded ecological relationships (
  prey predation, pollination, parasitism). Each observed interaction is a fact without a natural key.

This maps the two branches of ADR-022 onto the two classes of DAG element. No extra machinery needed.

---

## 10. Impact on `InsectSpecies`

Not sketched in this round but implied:

1. **Delete `LifeStages`** (the wrapper VO). ADR-013 forbids it from being a VO once its members are entities.
2. **`InsectSpecies` holds four nullable stage fields directly** (`egg`, `larva`, `pupa`, `adult`) typed as the concrete
   subtypes.
3. **`InsectSpecies.invariants()` descends into each stage** using the observability framework's entity-descent
   equivalent of `valueObjectOrNull`.
4. **Cross-stage invariants stay on `InsectSpecies`**:
    - Metabolous-type consistency (holometabolous requires all four stages; hemimetabolous has null `pupa`)
    - Chemistry-story coherence (`EXPRESSION` requires upstream `ACQUISITION`, etc.)
5. **`ChemicalDefense.protectedStages: Set<LifeStageKind>` is removed.** Derivable from walking stages.
6. **`Voltinism` stays at species level** as the "reported pattern" summary. Its relationship to pupal
   `DiapauseRegulation` may warrant a derived-vs-primary fact split, but that's a follow-on question.

---

## 11. Open questions for review

1. **Composite `EntityName` precedent.** Is `LifeStageName` genuinely the first composite `EntityName` in the codebase?
   If yes, does it warrant an ADR?
2. **`NonDiapausing` vs. null pattern.** Do we commit to "documented absence" as a positive subtype elsewhere?
   ADR-worthy.
3. **Package-as-sub-context convention.** Worth an ADR codifying what `com.naturalist.{domain}.{subcontext}` means for
   visibility, repositories, commands?
4. **`LarvaStage` further sealing.** Stay flat now, seal later when the DAG layer articulates needs. Revisit when
   building the ecology domain.
5. **Prey relationship entity design.** Deferred until the ecology domain is first designed; a separate ADR should open
   that discussion.
6. **`ActivityWindow` calendar-range invariants.** Deferred pending a `CalendarRange` kernel type.
7. **Host plants vs. parasitoid hosts consolidation.** Currently separate fields on `LarvaStage`. Could unify under a
   sealed `LarvalFoodReference`. Defer until the pattern earns it.

---

## 12. Review checklist

Before accepting this sketch, confirm:

- [ ] `LifeStageName` composite-slug form is consistent with existing `EntityName` discipline
- [ ] `LifeStageName` and `LifeStageKind` belong in the `identifiers` module (vs. `insects-api`)
- [ ] The four stages accurately reflect the project's ecological modeling needs
- [ ] `StagePhenology` multi-window shape handles your known edge cases (split diapause, multi-peak flight,
  overwintering)
- [ ] `StageHabitat` leans correctly on the `habitat` kernel (vs. inventing its own vocabulary)
- [ ] `StageChemistryRole.Role` enum covers your documented chemistry stories; nothing missing
- [ ] Deferring prey to a future ecology domain matches your DAG architectural direction
- [ ] Module DAG impact (insects → plants, insects → identifiers) is acyclic
- [ ] Package-as-sub-context framing is the right level for now (vs. new Maven module)

---

## 13. Files in this sketch

```
identifiers/
├── LifeStageKind.java
└── LifeStageName.java

insects/lifestages/
├── LifeStage.java                  (sealed interface)
├── EggStage.java
├── LarvaStage.java
├── PupaStage.java                  (includes DiapauseRegulation sealed sub-hierarchy)
├── AdultStage.java
├── StagePhenology.java             (includes ActivityWindow)
├── StageHabitat.java
└── StageChemistryRole.java
```

Nine files total.

---

## 14. What is *not* in this sketch

- `InsectSpecies` changes (aggregate root restructuring)
- Removal of the `LifeStages` wrapper
- Repository and query scaffolding (namespace-pattern via ADR-020)
- Test fixtures
- JSON serialization wiring (`@JsonTypeInfo` / `@JsonSubTypes` for the sealed hierarchy)
- Migration of existing `Battus philenor` catalog entry from old shape to new
- The ecology domain and `PreyRelationship` entity
- `ChemicalDefense.protectedStages` removal
- ArchUnit rules enforcing sub-context boundaries

Each of these is its own concern and deserves its own round.
