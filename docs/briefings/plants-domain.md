# Plants — Chat Briefing

Narrative context for conversations about the plants domain. For coding conventions
and structural rules, see `CLAUDE.md` in this directory.

## What this domain tracks

The plants domain catalogs every species established or cultivated at the
application's site of cultivation, plus the cultivars within those species,
the seed lineages worth preserving across generations, the management
programs that govern how each plant is tended in the field, and the
phytochemical constituents that link each plant to specific compounds in
the chemistry catalog.

The botanical record is intentionally stable: taxonomy, growth form, ecological
roles, native bioregions. Operational concerns — what to spray, what not to
spray, when to inspect for larvae, how to save seed — live on `PlantProgram`
records keyed off `PlantName`. Chemical concerns — what compound, what
tissue, what role, induced or constitutive — live on `PhytochemicalConstituent`
records that bridge `PlantName` and chemistry's `CompoundName`. The split
keeps Plant a clean cross-site reference while letting management and
phytochemistry evolve independently.

## Site context — Oak Vista (Chico, CA)

The fixture data and most of the running examples come from Oak Vista, a
Sacramento Valley site under summer heat (100–110°F peaks). Plant records fall
into three categories there:

- **Permanent structural plantings** — California Pipevine, scented geranium
  hedge, fruit trees (peach, nectarine, Asian pear, fig, Pineapple Guava,
  citrus, persimmon).
- **Cover crop and habitat species** — clover carpet, alyssum, dill, borage,
  creeping thyme, tall fescue.
- **Food crops** — tomatoes (multiple cultivars), Passiflora edulis.

A plant's `nativeBioregions` set is informational at the species level but
loaded with downstream meaning: native species are exempt from invasive-plant
concerns and have co-evolved with the local pollinator and butterfly community.
California Pipevine is the canonical example — Pipevine Swallowtail
(*Battus philenor*) co-evolved with *Aristolochia californica* specifically and
will not breed without it.

## Heritage program — Nick's Italian Pear

The heritage sub-context exists for one reason: a 50+ year family selection of
an Italian Pear paste tomato, maintained in coastal California, now beginning
its Chico climate adaptation program. **Generation 1 of the Chico adaptation
runs in the 2026 season.** Selection criteria: earliest ripening, highest
production, best flavour under Sacramento Valley heat.

`SeedLineage` is the aggregate root that tracks this — provenance, generation
count, selection criteria, performance notes. The lineage is irreplaceable;
seed must be saved annually from the highest performers without exception.

Domain rule that lives at the service layer (not in record invariants because
it crosses entities): a `SeedLineage` must reference a cultivar with
`VarietyType.OPEN_POLLINATED`. F1 hybrids do not breed true and cannot anchor
a lineage.

## Cultivars (Oak Vista 2026)

The cultivar sub-context sits below species. The `Plant` record `"tomato"` is
*Solanum lycopersicum* the species; the cultivars below it are the actual
varieties in the ground:

- `amish-paste` — UNKNOWN variety type (The Plant Barn, Chico). 12 plants,
  evaluating as primary sauce variety for the 90-quart annual target.
  `SeedSavingPolicy.CONDITIONAL` pending provenance verification from Baker Creek.
- `italian-pear-nicks` — OPEN_POLLINATED, the heritage line.
  `SeedSavingPolicy.SAVE_ANNUALLY` without exception.
- `sungold-cherry` — HYBRID_F1 commercial. 1 plant, fresh eating.
  `SeedSavingPolicy.DO_NOT_SAVE`.
- `san-marzano-f2` — HYBRID_F2 from saved F1 seed. 3 struggling plants;
  no seed saving — kept only as evaluation evidence that F1 saved seed
  segregates unpredictably.

`FruitType` (PASTE / CHERRY / SLICER / BEEFSTEAK) governs culinary suitability;
`VarietyType` governs whether seed saving is genetically meaningful;
`SeedSavingPolicy` is the management decision derived from both plus heritage
significance.

## Management programs

`PlantProgram` is operational guidance focused on a single ecological concern
for a specific plant: pest exclusion, disorder prevention, dormant treatment,
larval monitoring, seed saving. Programs are named after the activity, not the
plant — the Pipevine carries both `pipevine-pesticide-exclusion` (the absolute
zero-pesticide rule while larvae are present) and `pipevine-larval-monitoring`
(weekly egg/larva inspections). One plant, many programs.

The `constraint` field on a program is the non-negotiable rule that the
PestManagement application module surfaces whenever a treatment is proposed
near the referenced plant. Not every program has a constraint; some are pure
schedules.

## BER (Blossom End Rot) — context for the chemistry/soil interface

BER is a physiological disorder, not a disease — localised calcium deficiency
at developing fruit. **No cure once present. Prevention only.** Calcium is
immobile in plant tissue; every new cell needs fresh delivery via xylem mass
flow driven by transpiration. Anything that interrupts that flow — water
stress, low soil Ca, coir binding, heat-induced stomatal closure — produces
BER on susceptible crops.

Oak Vista BER risk is HIGH:

- Very low soluble Ca (FGL CH 2671853).
- Coco coir in substrate binds Ca.
- Chico summer heat drives stomatal closure during the critical fruit-fill window.
- Foliar chelated calcium (TPS CalMag OAC, organic-acid chelation) bypasses
  the root-to-xylem pathway when delivery is interrupted.

This context lives here because the BER prevention program will eventually be
modeled as a `PlantProgram` on tomato cultivars — but the chemistry of
calcium transport and chelation belongs to the chemistry domain. When you see
BER discussed in chat, expect a multi-domain conversation: plants (program),
chemistry (compound, chelation), soil (Ca availability, FGL data).

## Phytochemistry — the plant ↔ compound bridge

`PhytochemicalConstituent` is the single point where the plants catalog meets
the chemistry catalog. One record names one compound in one plant, with the
role(s) that compound plays in *that* species. The same compound carries
different stories in different plants — caffeine deters insects in coffee
seed and (in trace amounts) attracts pollinators to citrus nectar — so the
plant-specific story has its own home rather than being smeared onto either
the species or the compound.

The constituent record carries two orthogonal classification axes:

- **Structural type** lives on the chemistry side (`CompoundInfo` / sealed
  `StructuralType` permits) — carbon-skeleton facts independent of any plant.
- **Ecological / use category** is `PhytochemicalCategory` on the constituent
  — the coarse bucket a naturalist or phytochemistry textbook would group by
  (alkaloid, terpenoid, glucosinolate, latex, …).

Roles are a sealed `PhytochemicalRole` interface with stateless permits
(`HerbivoreDeterrent`, `PollinatorAttractant`, `Pharmaceutical`, …). One
constituent commonly carries several — caffeine is at once `INSECT_DETERRENT`,
`PHARMACEUTICAL`, and `NUTRACEUTICAL`. Consumers do not pattern-match the
sealed hierarchy at the call site; axis-level rollups on the record
(`isDefensive()`, `isSignaling()`, `mediatesEnvironmentalStress()`,
`hasMedicinalApplication()`, `hasCommercialApplication()`,
`isToxicToMammals()`) are the stable surface, with `PlantTissue` and
`InductionMode` queries (`isPresentIn`, `isInduced`, `isDevelopmental`)
covering the where/when axes.

Aristolochic acid I in California Pipevine is the canonical example — a
defensive alkaloid that the Pipevine Swallowtail co-opts as larval
sequestration, exactly the kind of cross-domain story the constituent
record exists to capture.

The cross-domain link is by slug only. `PhytochemicalConstituent.compoundName`
is a `CompoundName` from the `identifiers` module; `plants-api` has no
compile-time dependency on `chemistry-api`. Verifying that the referenced
compound actually exists in the chemistry catalog is a service-layer rule,
not a record invariant.

## Cross-domain references that already exist

- `Plant.nativeBioregions` — `Set<Bioregion>` from the biogeography kernel.
- `PlantProgram.plantName` — soft FK to `Plant`.
- `Cultivar.plantName` — soft FK to `Plant` (species).
- `SeedLineage.cultivarName` — soft FK to `Cultivar`.
- `PhytochemicalConstituent.plantName` — soft FK to `Plant`.
- `PhytochemicalConstituent.compoundName` — cross-domain soft FK to
  chemistry's `Compound` (the only soft FK in this domain that crosses a
  *domain* boundary rather than a sub-context boundary).
- `Plant.isKeystoneHost()` — drives zero-pesticide constraints in the
  PestManagement module (consumer side, not yet modeled).

All cross-domain references are by `EntityName` slug via the shared
`identifiers` module. No compile-time dependency from `plants-api` on
`insects-api`, `soil-api`, or `chemistry-api`.

## Open design questions

- **GrowthForm — resolved 2026-08-16.** The floated `GrowthForm` enum
  (FRUIT_TREE, FRUIT_VINE, VEGETABLE_CROP, COVER_CROP, …) is rejected: those are
  *use* categories, not botanical forms. "Fruit tree" alone spans unrelated taxa —
  stone fruit (*Prunus*, Rosaceae), citrus (*Citrus*, Rutaceae), fig (*Ficus*,
  Moraceae), pome (*Malus/Pyrus*, Rosaceae) — so a value like FRUIT_TREE triple-counts
  taxonomy (the rank chain), habit (tree), and use (fruit) in one token.

  The real defect is in the *existing* field: `PlantLifeForm` conflates two orthogonal
  botanical axes — **growth habit** (vine/shrub/tree/grass) and **life-cycle duration**
  (annual/perennial). Its own javadoc admits it ("vines may be annual or perennial —
  captured in life form regardless of duration"), and herbaceous non-grass plants
  (tomato, borage, dill) have no habit term at all, only a duration.

  The fix uses recognised botanical vocabularies rather than inventing one — split into
  two enums:

  - **`GrowthHabit`** — the USDA PLANTS *Growth Habit* set a botanist recognises:
    `TREE, SHRUB, SUBSHRUB, FORB_HERB, GRAMINOID, VINE`.
  - **`LifeCycle`** — `ANNUAL, BIENNIAL, PERENNIAL`.

  Use categories stay off both. Ecological function is already the many-to-many
  `Set<PlantRole>` (borage remains the test case: POLLINATOR_SUPPORT + GROUND_COVER +
  INSECT_LARVAL_HOST at once); agronomic use (fruit/vegetable/cover crop) belongs to a
  future `CropType` axis (anticipated, not yet built). The academic Raunkiær life-form
  system (phanerophyte/therophyte/…) was considered and set aside as too abstract for a
  garden-management and young-naturalist catalog. **Shipped 2026-08-16 (M6):** parsley
  moved to `BIENNIAL`, the value the flat enum could not express.

- **Aggregate factory placement.** No `PlantAggregate` yet. If one materializes
  (Plant + Cultivars + Programs + Constituents assembled by name), the
  factory lives in `plants-core` as a package-private concrete class — never
  in `plants-api`. The aggregate query would join its sibling `*Query`
  interfaces below into a single discoverable read surface.

## Reference points

- Identifiers: `domains/identifiers/.../plants/`
  (`PlantName`, `cultivar/CultivarName`, `heritage/SeedLineageName`,
  `management/PlantProgramName`,
  `phytochemistry/PhytochemicalConstituentName`).
- Read-side surface: every sub-context now exposes a `*Query` namespace
  interface (`PlantQuery`, `cultivar/CultivarQuery`,
  `heritage/SeedLineageQuery`, `management/PlantProgramQuery`,
  `phytochemistry/PhytochemicalConstituentQuery`) with paired
  `*EntityCollections` for multi-result returns. Repositories remain
  package-private classes per the namespace pattern in `domains/CLAUDE.md`.
- Repository contracts: `plants-repository-test/`
  (`PlantEntityRepositoryTest`, `CultivarEntityRepositoryTest`,
  `SeedLineageEntityRepositoryTest`, `PlantProgramEntityRepositoryTest`,
  `PhytochemicalConstituentEntityRepositoryTest`).
- Cross-domain anchors: `kernels/biogeography/Bioregion`,
  `kernels/field-notes/Description`, `kernels/taxonomy/TaxonomicClassification`,
  and (soft FK only) `chemistry/CompoundName` via `identifiers`.
