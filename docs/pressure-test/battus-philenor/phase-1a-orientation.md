# Phase 1a — DAG Orientation

**Status.** Draft, end of orientation pass.
**Purpose.** Confirm shared understanding of the model as it currently stands,
in the language of the working application. No findings logged in this pass —
that is Phase 1b.

**Sibling documents.** `00-charter.md` (governance, the five evaluation axes,
the workflow rhythm). `01-findings.md` (Phase 1b output, not yet started).
`99-followups.md` (deferred items, not yet started).

**Resumption note.** This document is designed to bridge a chat. A fresh chat
reading this plus `00-charter.md` should be able to start Phase 1b without
backstory. The "Source landmarks" section at the end gives concrete file paths
into the codebase a new instance can read directly.

---

## 0. What this document is

A map. The codebase already exists; Pat already navigates it through the
running application's DAG visualizer. This document does not replace that —
it is a written confirmation that I (Claude) read the same model Pat built,
narrated in the language we will use in `01-findings.md`.

The map covers only what *Battus philenor* directly exercises. Nodes the
swallowtail does not touch are out of scope for this document but are present
in the codebase and may surface during Phase 1c stress tests.

No findings, no commitments, no proposed changes. Three things this document
does establish:

1. **The nodes** — the DDD building blocks the swallowtail story exercises.
2. **The edges** — the typed cross-domain references (and their direction).
3. **The catalog mechanism** — the runtime cross-domain navigation kernel
   that runs alongside the compile-time edges, providing a second axis of
   "how do domains talk to each other."

A short closing section names what I expected and didn't find, what I found
that I didn't expect, and where I'm carrying provisional understanding into
Phase 1b.

---

## 1. The story, in one paragraph

*Battus philenor* (Pipevine Swallowtail) is an obligate specialist on
*Aristolochia californica* (California Pipevine). Larvae feed on the host
and sequester aristolochic acid I and II — nitrophenanthrene-carboxylic
defensive secondary metabolites the plant produces constitutively in leaf,
stem, and root. The toxin is retained through pupation, expressed in adult
wings and hemolymph (where it produces aposematic blue iridescence), and
maternally transferred to brick-red egg clusters. Every stage of the life
cycle is chemically defended. The compound is a Group 1 IARC carcinogen
and a documented mammalian nephrotoxin; the host plant carries an absolute
pesticide-exclusion management constraint at Oak Vista.

The story crosses three domains: chemistry (the compound), plants (the
producer and the bridge), insects (the sequesterer). It is the first
*trilateral* story in the catalog — the audit's reason for existing.

---

## 2. Nodes — the DDD building blocks the story exercises

### Convention used in this document

For each node: **DDD kind**, **module**, **identity** (slug branch or surrogate
branch, with the typed identifier class), **what it carries that the swallowtail
story needs**. Sub-context within a module is shown as `module/sub-context`.
Permits of sealed families are vocabulary, not nodes — they're listed alongside
their parent.

### 2.1 Chemistry side

**`Compound`** — `@AggregateRoot`, `chemistry-api/compound`. Identity:
`NamedEntity<CompoundName>` (slug). The aggregate root for a chemical
compound. Composes one `CompoundInfo`, four profile VOs (`SolubilityProfile`,
`BioavailabilityProfile`, optional `VolatilizationProfile`, optional
`SafetyProfile`), an open `Map<String, String> properties`, and two boolean
flags (`omriListed`, `cdfaRegistered`). The aristolochic acids `aristolochic-acid-i`
and `aristolochic-acid-ii` are catalog instances of this aggregate.

**`CompoundInfo`** — `ValueObject`, `chemistry-api/compound`. Owned by
`Compound`. Carries the four classification axes that define a compound's
identity along orthogonal dimensions:

- `ChemicalNature` (enum) — `ORGANIC` / `INORGANIC` / `ORGANOMETALLIC`.
- `PhysicalForm` (enum) — `ELEMENT` / `MINERAL` / `SALT` / `ACID` / `BASE` / `COMPLEX`.
- `StructuralType` (sealed VO interface, ~26 permits) — carbon-skeleton family.
  Required, non-null. Currently uses `OtherAlkaloid` as a documented fallback
  for AA-I/II ("phenanthrenoid alkaloids" per the Javadoc).
- `Set<FunctionalRole>` (sealed VO interface, 5 permits: `Chelator`,
  `Fumigant`, `BiologicalCatalyst`, `Fertilizer`, `Acaricide`) — what the
  compound *does* in the field. Required, non-empty. AA-I/II carry
  `BiologicalCatalyst` in production data.
- Plus `formula : String`, `molecularWeight : MolecularWeight` (a
  `NumericNamedValue`), `phCharacter : PhCharacter` (enum),
  `Set<PeriodicElement> constituentElements`.

**`StructuralType`** — sealed `ValueObject` interface, `chemistry-api/compound/structure`.
Permits documented inline; `OtherAlkaloid` is the temporary fallback the
`Compound`-level design notes say should be promoted out when `compounds.json`
populates a class that earns its own permit. AA-I/II are the named example.

**`FunctionalRole`** — sealed `ValueObject` interface, `chemistry-api/compound/role`.
Five stateless permits, all agricultural-input flavored.

**`SolubilityProfile`, `BioavailabilityProfile`, `VolatilizationProfile`,
`SafetyProfile`** — `ValueObject`s owned by `Compound`. Carry physical
chemistry and safety information. Volatilization and safety are nullable;
solubility and bioavailability are required.

**`Element`** and **`PeriodicElement`** — `Element` is a `NamedEntity<ElementName>`
in `chemistry-api/element` carrying an atomic-element record loaded from
`elements.json`; `PeriodicElement` is the 118-element enum used for typed
sets on `CompoundInfo.constituentElements`. The swallowtail story touches
both lightly (AA-I/II declare elements `C, H, N, O`).

**`CompoundCategory`** — enum in `chemistry-api/compound`. Coarse roll-up
of `StructuralType` (every permit answers `category()`). Used for the
boolean predicates on `Compound` (`isAlkaloid()`, `isPhenolic()`, etc.).

**`Product`** — `NamedEntity<ProductName>`, `chemistry-api/product`. Carries
`Set<CompoundName> compounds` — a typed reference set. Same shape as what a
typed `ChemicalDefense.sourceCompounds` would look like; it's the precedent
within chemistry-api for a record carrying typed compound references.
Doesn't directly participate in the swallowtail story but is precedent.

### 2.2 Plants side

**`Plant`** — `NamedEntity<PlantName>`, `plants-api`. The botanical record.
Carries `taxonomy : TaxonomicClassification`, `description : Description`,
`Set<PlantRole> roles`, `lifeForm : PlantLifeForm`, `nativeBioregions :
Set<Bioregion>`, `commonNames : Set<CommonName>`. Boolean predicates include
`isKeystoneHost()` (true for California Pipevine). The swallowtail story
exercises this through the plant `california-pipevine`. Chico is in
`sacramento-valley` and `coast-ranges` bioregions; California Pipevine is
recorded native to both.

**`PhytochemicalConstituent`** — `NamedEntity<PhytochemicalConstituentName>`,
`plants-api/phytochemistry`. **The cross-domain bridge entity** between
plants and chemistry. Carries:

- `plantName : PlantName` (soft FK to `Plant`).
- `compoundName : CompoundName` (soft FK to chemistry's `Compound`,
  imported from the `identifiers` module — no compile-time edge).
- `description : Description` (Durrell four-level).
- `category : PhytochemicalCategory` (enum, 16 values; coarse ecological/use
  bucket; deliberately *not* the same axis as chemistry's `StructuralType`).
- `Set<PhytochemicalRole> roles` (sealed VO interface, **22 permits** organized
  along four axes: defense, signaling, environmental interaction, medicinal/
  commercial). Required non-empty. AA-I/II constituents carry four roles each:
  `HerbivoreDeterrent`, `InsectDeterrent`, `HumanToxin`, `LivestockToxin`.
- `Set<PlantTissue> tissues` (enum). Required non-empty. Pipevine AA records
  carry `LEAF, STEM, ROOT`.
- `induction : InductionMode` (enum: `CONSTITUTIVE`, `INDUCED`, `DEVELOPMENTAL`).
  Pipevine AA records are `CONSTITUTIVE`.
- `notes : @Nullable String` — operational guidance.

The slug form encodes both sides: `california-pipevine-aristolochic-acid-i`,
`california-pipevine-aristolochic-acid-ii`. The two-argument factory
`PhytochemicalConstituentName.of(PlantName, CompoundName)` codifies the
convention. This is the second precedent (after `LifeStageName`) for
composite-key `NamedEntity` slugs.

**`PhytochemicalRole`** — sealed `ValueObject` interface, `plants-api/phytochemistry/role`.
22 stateless permits across four axes. The vocabulary is rich and recently
developed. Behavioral predicates on `PhytochemicalConstituent` (`isDefensive()`,
`isSignaling()`, `mediatesEnvironmentalStress()`, `hasMedicinalApplication()`,
`hasCommercialApplication()`, `isToxicToMammals()`) provide the consumer
surface — call sites do not `instanceof`-chain the sealed family.

### 2.3 Insects side

**`InsectSpecies`** — `@AggregateRoot`, `insects-api`. Identity:
`NamedEntity<InsectSpeciesName>` (slug). The species record. *Composes* its
four life stages directly as nullable fields (`@Nullable EggStage egg`,
`@Nullable LarvaStage larva`, `@Nullable PupaStage pupa`, `@Nullable AdultStage adult`).
Carries a large body of nested VOs as inner records: `IdentificationFeatures`,
`ChemicalDefense`, `Voltinism` (with `VoltinismPattern` enum including
`INDETERMINATE`), `HabitatRequirements`, `GardenConnections`, `BeneficialProfile`,
`EcologicalSignificance`. `pipevine-swallowtail` is the canonical instance
exercising every nullable field except `habitatProfile` (null at species
level; populated per stage).

**`ChemicalDefense`** — `ValueObject`, nested `static record` inside
`InsectSpecies`. Carries:

- `mechanism : String` (required, not blank). Production data carries a
  natural-language sentence about sequestration.
- `sourceCompounds : @Nullable String`. Production data carries a prose
  reference to AA-I/II — the field is currently untyped.
- `aposematicSignal : @Nullable String`.
- `protectedStages : Set<LifeStageKind>`. Production data lists all four;
  the Javadoc explicitly flags this as redundant with each stage's own
  `chemistryRole` and a candidate for removal.

**`LifeStage`** — sealed `NamedEntity<LifeStageName>` interface,
`insects-api/lifestage`, with four permits: `EggStage`, `LarvaStage`,
`PupaStage`, `AdultStage`. Each permit is a record. Each carries:

- `name : LifeStageName` — composite slug `{species}-{kind}`,
  e.g. `pipevine-swallowtail-larva`.
- `phenology : StagePhenology` (windowed activity periods, multi-window for
  multi-cohort species; the swallowtail's adult uses two cohort-labeled
  windows for spring and summer broods).
- `habitat : StageHabitat` (carries `HabitatProfile` from `kernels/habitat`).
- `chemistryRole : @Nullable StageChemistryRole`.
- `description : Description`.
- Plus stage-specific fields (`LarvaStage` carries `hostPlants : List<PlantName>`
  and `parasitoidHosts : List<InsectSpeciesName>`; `AdultStage` carries
  `nectarSources : List<PlantName>`; etc.)

**`StageChemistryRole`** — `ValueObject`, `insects-api/lifestage`. Carries
`role : Role` (enum: `ACQUISITION`, `RETENTION`, `EXPRESSION`, `MATERNAL_TRANSFER`)
and `notes : @Nullable String`. The four-act verb arc the swallowtail story
captures cleanly. Notes are free-text in production data and reference AA-I/II
in prose.

**`PupaStage.DiapauseRegulation`** — sealed `ValueObject` interface inside
`PupaStage`. Four permits: `PhotoperiodRegulated`, `FoodWaterContentRegulated`,
`TemperatureRegulated`, `NonDiapausing`. The Javadoc on `FoodWaterContentRegulated`
calls out *Battus philenor* as the named case. Production swallowtail data
has `diapauseRegulation: null` despite the type system being ready — narrative
description carries the information instead.

**`InsectAggregate`** — `Aggregate`, `insects-api`. Composes `InsectSpecies`
with an `ImageCollection` (a `BehavioralCollection` of `InsectImage` entities).
`InsectImage` is `Entity<InsectImageId>` — UUIDv7 surrogate identity, the
only pure `Entity` (UUIDv7 branch) the swallowtail story directly touches.
Photographs of the swallowtail would be `InsectImage` records.

### 2.4 Identifiers — the cross-domain currency

Eight named identifier types are exercised by the swallowtail story, all
in the `identifiers` module:

| Slug type                      | Branch      | Subpackage                    |
|--------------------------------|-------------|-------------------------------|
| `CompoundName`                 | NamedEntity | `chemistry/compound`          |
| `ElementName`                  | NamedEntity | `chemistry/element`           |
| `PlantName`                    | NamedEntity | `plants`                      |
| `PhytochemicalConstituentName` | NamedEntity | `plants/phytochemistry`       |
| `InsectSpeciesName`            | NamedEntity | `insects`                     |
| `LifeStageName`                | NamedEntity | `insects` (composite slug)    |
| `LifeStageKind`                | enum        | `insects` (component of slug) |
| `InsectImageId`                | EntityId    | `insects` (UUIDv7 branch)     |

`PhytochemicalConstituentName` is special: it lives in identifiers but
its programmatic factory `of(PlantName, CompoundName)` imports two other
identifier types. Both imports are within the identifiers module —
no cross-module dependency.

### 2.5 Kernels exercised

- `kernels/framework` — DDD building blocks (`NamedEntity`, `Entity`,
  `Aggregate`, `ValueObject`, `BehavioralCollection`), observability
  (`Observable`, `Constraints`, `Observer`), the Resilience facade.
- `kernels/field-notes` — `Description` (four-level Durrell), `CommonName`
  (vernacular label + locale).
- `kernels/taxonomy` — `TaxonomicClassification` plus the rank `NamedValue`
  types. Both `Plant` and `InsectSpecies` carry classifications. Pipevine
  is `Piperales / Aristolochiaceae / Aristolochia / californica`; swallowtail
  is `Lepidoptera / Papilionidae / Battus / philenor`.
- `kernels/biogeography` — `Bioregion` sealed type with six California
  permits. Pipevine is native to `sacramento-valley` and `coast-ranges`.
- `kernels/habitat` — `HabitatProfile` (composable VO with `HabitatZone`,
  `MoistureRegime`, `LightRegime`, `Set<VerticalLayer>`). Each swallowtail
  stage carries one.
- `kernels/catalog` — see §4.

---

## 3. Edges — typed cross-domain references the story uses

The "compile-time edges" — typed `EntityName` slugs flowing through Java
type signatures. Every cross-domain edge in the story:

### 3.1 Plants → Chemistry (single edge)

```
PhytochemicalConstituent.compoundName : CompoundName
```

This is the canonical edge. `plants-api` imports `CompoundName` from
`identifiers`; there is no compile-time edge from `plants-api` to
`chemistry-api`. The query `PhytochemicalConstituentQuery.forCompoundName(CompoundName)`
is the reverse-lookup port.

### 3.2 Insects → Plants (two edges)

```
LarvaStage.hostPlants : List<PlantName>
AdultStage.nectarSources : List<PlantName>
```

Both list-shaped, both typed. The swallowtail's larva references
`["aristolochia-californica"]` (host); the adult lists six nectar plants
including `wild-radish`. `insects-api` imports `PlantName` from `identifiers`;
there is no compile-time edge from `insects-api` to `plants-api`.

### 3.3 Insects → Insects (one edge, unexercised by the swallowtail)

```
LarvaStage.parasitoidHosts : List<InsectSpeciesName>
```

Empty for the swallowtail. Will be exercised by the braconid-wasp stress
test in Phase 1c.

### 3.4 Insects → Chemistry (no edge)

There is currently no typed cross-domain reference from `insects-api` to
chemistry. `ChemicalDefense.sourceCompounds : String` is the *only* place
the connection exists, and it's free text. Adding a typed edge here is
one of the obvious Phase 1b candidates — but as the next section shows,
it's not the only mechanism the architecture provides.

### 3.5 Module-level DAG verification

The compile-time edges above are consistent with the module-level DAG
declared in ADR-004 and the framework briefing. `*-api` modules depend
only on `framework`, `identifiers`, `field-notes`, and (organism only)
`taxonomy`. The cross-domain edges do not violate this — they all flow
through `identifiers`.

---

## 4. The catalog kernel — runtime cross-domain navigation

A second mechanism for cross-domain navigation runs alongside the
compile-time edges. `kernels/catalog` provides:

- **`DomainId`** — open per-domain identity. Each domain ships a concrete
  subtype in its `*-api` module (`PlantsDomain`, `ChemistryDomain`,
  `InsectsDomain`).
- **`EntityRef`** — value-object pairing of `DomainId` and `EntityName` —
  the typed pointer carried by catalog query results.
- **`Catalog`** — public query interface with two responsibilities:
    - `search(text) → SearchResults` — forward token-based search across
      every contributing domain.
    - `findReferencesTo(EntityName) → Map<DomainId, List<EntityRef>>` —
      inverse fan-out asking each domain "do any of your entities reference
      this?"
- **`CatalogContribution`** — search-direction SPI. Each `*-core` ships
  one, declaring its searchable entities and the surface tokens under
  which each is findable.
- **`EntityReferences<T extends EntityName>`** — inverse-direction SPI.
  Each `*-core` may ship multiple, keyed by the foreign `EntityName`
  subtype it can answer for.

**The architectural significance for the swallowtail audit.** The catalog
provides cross-domain navigation *without* requiring compile-time edges
between api modules. The question "what insects sequester this compound?"
can be answered today — given an `EntityReferences<CompoundName>` provider
in `insects-core` and a typed compound reference somewhere in the insect
data — without `insects-api` ever importing chemistry types beyond
`CompoundName` from `identifiers`.

This means the Phase 1b question about insects → chemistry is genuinely
two-sided. Not "should we add a typed `Set<CompoundName>` reference, yes
or no" — but "what is the right division of labor between compile-time
typed references on `*-api` records and runtime catalog routing through
`*-core` providers?" Either mechanism solves the navigation problem;
they answer different design questions about coupling and discoverability.

The bundle data already provides evidence the inverse direction is needed:
AA-I/II's `properties` map carries `"hostInsect": "Battus philenor (Pipevine
Swallowtail)"` as a free-text string — chemistry is *also* trying to point
at insects, encoded as untyped properties because no typed mechanism is
in place.

---

## 5. The verb arc — chemistry's life-cycle representation

The swallowtail story's distinctive shape is a four-act verb arc across
the life stages:

| Stage | `StageChemistryRole.Role` | What happens                                               |
|-------|---------------------------|------------------------------------------------------------|
| egg   | `MATERNAL_TRANSFER`       | Maternal AA deposition into the chorion (brick-red color). |
| larva | `ACQUISITION`             | Sequestration from host plant during feeding.              |
| pupa  | `RETENTION`               | Toxin retained through metamorphosis.                      |
| adult | `EXPRESSION`              | AA in wing scales, hemolymph, aposematic blue iridescence. |

The four-permit enum captures this cleanly. Cross-stage coherence
(`EXPRESSION` requires upstream `ACQUISITION`, etc.) is documented in
`LifeStage.md` §10 as an `InsectSpecies`-level invariant, *not yet
enforced* in the current `invariants()` method. This will surface in
Phase 1b under Axis 3 (story coherence).

---

## 6. What I expected and did not find

- **No insects → chemistry compile-time edge.** As above, this is by
  design; the catalog kernel covers the navigation question. Whether the
  typed-reference mechanism *should* also exist is the central Axis 4
  question.
- **No `protectedStages` enforcement.** The field exists, but the Javadoc
  itself flags the field as derivable and a candidate for removal. The
  redundancy is acknowledged but not addressed.
- **No `NitrophenanthreneAlkaloid` permit.** AA-I/II carry `OtherAlkaloid`,
  with the Javadoc explicitly naming aristolochic acids as the documented
  fallback case.
- **No `FunctionalRole` permit that fits AA-I/II.** All five existing
  permits are agricultural-input flavored; none describes a defensive
  secondary metabolite. The invariant `notEmpty(functionalRoles)` forces
  the catalog to assign *something*; production data assigns
  `BiologicalCatalyst` — a category error visible in the live data, not
  just the bundle.
- **No mimicry-complex modeling.** Confirmed out of scope per the charter;
  narrative descriptions reference Müllerian/Batesian mimics but no typed
  insect ↔ insect relationship exists.
- **No prey relationships.** Per `LifeStage.md` §8, prey is deferred to a
  future `ecology` (or `trophic`, or `relationships`) domain that will use
  surrogate-identity `Entity<XId>` records and a sealed `PreyReference`
  wrapper. Out of scope for the swallowtail audit but architecturally
  relevant — the same pattern would apply to any future "sequesters from"
  relationship if it were promoted to a first-class entity.

---

## 7. What I found that I did not expect

- **Slug mismatch — production-live, not just the bundle.** The
  swallowtail's `larva.hostPlants : ["aristolochia-californica"]` does
  not resolve to any `Plant.name` in the catalog (`Plant.name` is
  `california-pipevine`). The plants-side `PhytochemicalConstituent`
  records use `california-pipevine`, consistent with the plant. The
  catalog kernel's runtime token search would route around this via the
  plant's taxonomy and common names; the strict typed-reference path is
  broken.
- **Stages duplicated between `insect-species.json` and `life-stages.json`.**
  Each life stage record exists in both places — embedded inside its
  species record *and* as a standalone catalog entry. Identical content
  in the production data. The `LifeStageRepository` carries an
  `@Incubating` annotation explaining Pat is exploring the pattern; this
  is in-flight design, not a finished decision.
- **AA-I/II's `properties` map carries the *correct* answers to the type-system
  questions as prose.** `chemicalClass: "nitrophenanthrene carboxylic acid"`
  is the structural-type answer that should be a `StructuralType` permit.
  `hostInsect: "Battus philenor (Pipevine Swallowtail)"` is the inverse
  cross-domain reference that has no typed home. Both encoded as untyped
  string properties because the type system doesn't yet support them.
- **`pupa.diapauseRegulation: null` despite the type system being ready.**
  The sealed `DiapauseRegulation` family with `FoodWaterContentRegulated`
  named after the swallowtail case is in place. Production data has not
  populated it; the prose narrative on the pupa carries the information
  instead.
- **Thymol contrast.** Thymol's chemistry-side `FunctionalRole` (`Fumigant`,
  `Acaricide`) is correct and useful — thymol *is* an agricultural input
  with those behaviors. Thymol's plants-side `PhytochemicalRole` (`AntiFungal`,
  `AntiMicrobial`, `InsectDeterrent`, `Pharmaceutical`, etc.) covers the
  ecological story. Both axes work, simultaneously, for thymol. The
  AA-I/II case breaks because AAs have no agricultural-input story —
  their entire "what does this compound do" answer is plant-side and
  insect-side. This is concrete evidence that the chemistry-side
  vocabulary was designed for one class of compound and is not yet
  generalizing to defensive secondary metabolites.

---

## 8. Catalog scope as of orientation

For Phase 1c stress-test planning later:

| Catalog file                                            | Records |
|---------------------------------------------------------|---------|
| `chemistry/compound/compounds-base.json`                | 13      |
| `chemistry/compound/compounds-aristolochic-acid.json`   | 2       |
| `chemistry/compound/depictions.json`                    | 15      |
| `chemistry/element/elements.json`                       | 10      |
| `chemistry/product/products-base.json`                  | 12      |
| `plants/plants.json`                                    | 19      |
| `plants/phytochemistry/phytochemical-constituents.json` | 4       |
| `plants/cultivar/cultivars.json`                        | 4       |
| `plants/heritage/seed-lineages.json`                    | 6       |
| `plants/management/plant-programs.json`                 | 8       |
| `insect-species.json`                                   | 16      |
| `life-stages.json`                                      | 60      |

The phytochemistry sub-context is sparsest (4 constituents) — it's the
newest sub-context and just starting to populate. Two of the four are the
swallowtail's AA records.

The three nominated stress-test organisms are present:
`green-lacewing`, `braconid-wasp`, `tomato`. Cleanly representative cases
for the Phase 1c probe.

---

## 9. The five evaluation axes — Phase 1b preview

These are restated from the charter for resumption convenience. Phase 1b
applies them per node, with confidence-gated fixing as described in
`00-charter.md` §3:

1. **Identity and slug flow.** Two-branch `NamedEntity` / `Entity`
   discipline; slug-naturalness test for relationship-shaped entities
   (slug falls out *and* reads reasonably as a name). Verifies the
   swallowtail story can be told end-to-end with `EntityName` slugs.
2. **Vocabulary alignment.** Across overlapping vocabularies
   (`FunctionalRole`, `PhytochemicalRole`, `StageChemistryRole.Role`,
   `StructuralType`, `PhytochemicalCategory`, `CompoundCategory`),
   does the swallowtail data fit each one without category errors?
   Missing-but-needed vocabulary is itself a finding (rendered in
   chevron tokens like `«SecondaryMetabolite»`).
3. **Story coherence.** Where the same fact is asserted in multiple
   places, do the assertions agree? Are coherence rules enforced?
4. **Direction of coupling.** For every cross-domain edge the swallowtail
   uses or *would* need: does it exist? Should it exist? If not, what
   carries the information (typed reference, catalog provider, prose)?
5. **Generalization fitness.** Does this node express a *Battus philenor*
   property or an organism-domain property? Are sequestration-specific
   details (the verb arc, AA-shaped structures) the case or the shape?

A sixth axis remains provisionally open if one emerges (charter §4).

---

## 10. Source landmarks (for resumption)

If a fresh chat needs to verify any of the above, these are the exact
files in the codebase. Paths are relative to the repo root.

**ADRs.** `docs/adr/` — load-bearing for the audit:

- ADR-022 (entity identity, current). `rationale/ADR-022-entity-identity-unified.md`.
- ADR-001 (repository architecture, dual-key strategy). Both index and rationale.
- ADR-004 (modular monolith, the module DAG). Index.
- ADR-013 (ValueObject contract — the four conditions to be a VO). Index.
- ADR-020 (namespace pattern for api surface). Index.
- ADR-017 (observability and invariants). Index.
- ADR-014 (NamedValue). Index.

**Per-domain CLAUDE.md briefings.** `kernels/CLAUDE.md`,
`domains/CLAUDE.md`, `domains/{insects,plants,chemistry}/CLAUDE.md`.

**Critical source files for the swallowtail story:**

- `domains/identifiers/.../{CompoundName,PlantName,InsectSpeciesName,LifeStageName,PhytochemicalConstituentName}.java`.
- `domains/chemistry/chemistry-api/.../compound/{Compound,CompoundInfo}.java`.
- `domains/chemistry/chemistry-api/.../compound/structure/StructuralType.java`.
- `domains/chemistry/chemistry-api/.../compound/role/FunctionalRole.java`.
- `domains/plants/plants-api/.../phytochemistry/{PhytochemicalConstituent,PhytochemicalRole}.java`.
- `domains/plants/plants-api/.../Plant.java`.
- `domains/insects/insects-api/.../InsectSpecies.java` (with nested
  `ChemicalDefense`, `Voltinism`, etc.).
-

`domains/insects/insects-api/.../lifestage/{LifeStage,EggStage,LarvaStage,PupaStage,AdultStage,StageChemistryRole}.java`.

- `domains/insects/insects-api/.../lifestage/LifeStage.md` — design notes.
  This is *gold* for context: §8 (prey deferred to ecology domain),
  §9 (vertices vs. edges identity discipline), §10 (cross-stage chemistry
  coherence), §11 (open questions including `protectedStages` removal).

**Critical kernels:**

- `kernels/framework/.../ddd/{NamedEntity,Entity,EntityName,EntityId,Named,Aggregate,ValueObject}.java`.
- `kernels/framework/docs/constraints-ubl.md` — the canonical reference
  for the `Constraints` fluent builder. **Source of truth for what
  invariant methods exist.** Past sessions have invented methods that
  don't exist; this doc is the guard.
- `kernels/catalog/.../{Catalog,DomainId,EntityRef,EntityReferences,CatalogContribution}.java`.
- `kernels/field-notes/.../Description.java` and `CommonName.java`.
- `kernels/biogeography/.../Bioregion.java`.
- `kernels/habitat/.../HabitatProfile.java`.

**Catalog data (JSON) for the swallowtail story:**

- `chemistry/compound/compounds-aristolochic-acid.json` — AA-I and AA-II.
- `plants/plants.json` — `california-pipevine` plant record.
- `plants/phytochemistry/phytochemical-constituents.json` — the bridge
  records linking pipevine to the AAs.
- `insect-species.json` — `pipevine-swallowtail` species record.
- `life-stages.json` — the four `pipevine-swallowtail-{egg,larva,pupa,adult}`
  stage records (duplicated with the species-embedded copies).
- `pipevine-aristolochia-bundle.json` — the original cross-domain bundle
  used to seed the audit. Predates the catalog data slightly; treat the
  catalog data as authoritative when they differ.

---

## 11. Decision for next session

Phase 1a is complete. Phase 1b (node-by-node evaluation) begins with one
node per session.

**Recommended starting node: `Compound`** — most upstream in the dependency
chain (chemistry sits at the bottom; plants and insects both reference
into it). Evaluating `Compound` and `CompoundInfo` first establishes the
chemistry-side surface without yet entangling cross-domain coupling
questions. The known Axis 2 issues (`FunctionalRole` vocabulary,
`StructuralType.OtherAlkaloid` placeholder) can be diagnosed against
this node alone before we move outward.

**Alternative starting node: `PhytochemicalConstituent`** — most edge-rich
node in the story. Carries two soft FKs and is the canonical bridge.
Evaluating it first puts the cross-domain shape question front and center
but means we touch chemistry and plants nodes before they've been
individually surveyed.

Pat decides at start of next session.

---

## 12. Resumption checklist

For a fresh chat picking up Phase 1b:

- [ ] Read `00-charter.md` first. The five axes, severity tiers, finding
  template, discipline rules, and resumption discipline are all there.
- [ ] Read this document (`phase-1a-orientation.md`) second. The map of
  what the swallowtail story exercises and what is in the codebase.
- [ ] When ready, ask Pat which node starts Phase 1b. Defaults are
  proposed in §11 but Pat decides.
- [ ] Per axis: do not invent constraint methods that aren't in
  `kernels/framework/docs/constraints-ubl.md` §3. Past sessions have
  invented `namedEntityOrNull`, `observableOrNull`, and a direct-form
  `notEmpty(value, name)`. None exist. The doc is authoritative.
- [ ] Findings get stable IDs (`A{axis}-F{number}`). Followups get
  `FU-{number}`. Severity tiers: BLOCK / STRAIN / SMELL / NOTE.
- [ ] Diagnosis-then-fix when confidence is high (Pat refactors Java
  in-session); diagnosis-only and contingent finding when confidence
  is medium or fix is cross-cutting.
- [ ] A finding is closed only when (a) language proposed, (b) Java
  change made, (c) bundle JSON expresses the new shape.
