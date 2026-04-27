# Plants — Chat Briefing

Narrative context for conversations about the plants domain. For coding conventions
and structural rules, see `CLAUDE.md` in this directory.

## What this domain tracks

The plants domain catalogs every species established or cultivated at the
application's site of cultivation, plus the cultivars within those species,
the seed lineages worth preserving across generations, and the management
programs that govern how each plant is tended in the field.

The botanical record is intentionally stable: taxonomy, growth form, ecological
roles, native bioregions. Operational concerns — what to spray, what not to
spray, when to inspect for larvae, how to save seed — live on `PlantProgram`
records keyed off `PlantName`. The split keeps Plant a clean cross-site
reference while letting management evolve season by season.

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

## Cross-domain references that already exist

- `Plant.nativeBioregions` — `Set<Bioregion>` from the biogeography kernel.
- `PlantProgram.plantName` — soft FK to `Plant`.
- `Cultivar.plantName` — soft FK to `Plant` (species).
- `SeedLineage.cultivarName` — soft FK to `Cultivar`.
- `Plant.isKeystoneHost()` — drives zero-pesticide constraints in the
  PestManagement module (consumer side, not yet modeled).

All cross-domain references are by `EntityName` slug. No compile-time
dependency from plants on insects, soil, or chemistry modules.

## Open design questions

- **GrowthForm vs PlantRole.** The current model uses `PlantLifeForm` (annual,
  perennial, vine, shrub, tree, grass) for management cadence and a many-to-many
  `Set<PlantRole>` for ecological function. Borage is the test case for the
  many-to-many shape: POLLINATOR_SUPPORT + GROUND_COVER + INSECT_LARVAL_HOST
  simultaneously. The open question is whether a separate `GrowthForm` enum
  (FRUIT_TREE, FRUIT_VINE, VEGETABLE_CROP, COVER_CROP, ORNAMENTAL_WOODY,
  POLLINATOR_PLANT) earns its keep alongside the existing axes, or whether
  it would just duplicate information already encoded in `PlantLifeForm` +
  `PlantRole`.

- **Read-side query surface.** No `PlantQuery` interface yet — neither for
  Plant nor for the cultivar/heritage/management sub-contexts. When read-side
  queries materialize, the namespace pattern from `domains/CLAUDE.md` applies:
  package-private `PlantRepository` (already in place), public `PlantQuery`
  interface, public `PlantEntityCollections` for multi-result return types.

- **Aggregate factory placement.** No `PlantAggregate` yet. If one materializes
  (Plant + Cultivars + Programs assembled by name), the factory lives in
  `plants-core` as a package-private concrete class — never in `plants-api`.

## Reference points

- Identifiers: `domains/identifiers/.../plants/`
  (`PlantName`, `cultivar/CultivarName`, `heritage/SeedLineageName`,
  `management/PlantProgramName`).
- Repository contracts: `plants-repository-test/`
  (`PlantEntityRepositoryTest`, `PlantProgramEntityRepositoryTest`).
- Cross-domain anchors: `kernels/biogeography/Bioregion`,
  `kernels/field-notes/Description`, `kernels/taxonomy/TaxonomicClassification`.
