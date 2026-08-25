# Plants

The botanical catalog for the site — the species and lower taxa established or
cultivated there, together with the cultivars, seed lineages, management
programs, and phytochemistry that hang off them.

It follows the organism-domain design the insects module worked out: catalog a
plant at whatever rank its evidence supports, attach photographs, features, and
roles at that rank, and compose the pieces into a `Plant` read model for display.

---

## Core concepts

**The catalog is a Linnaean chain of first-class entities.** `PlantOrder`,
`PlantFamily`, `PlantGenus`, and `PlantSpecies` are each a `NamedEntity` with a
typed slug identity and a typed upward foreign key to its immediate parent. The
chain is closed and enforced — each rank's test source declares a foreign-key
constraint against its parent, so a fixture whose parent is missing fails at
load. Four rungs get entities here, against insects' five; the ladder is a
per-domain decision.

**Records attach at the rank the evidence reached.** The four rank names are
permits of a sealed `PlantRankName`, so a role, a management program, a
photograph, or a phytochemical constituent can name whichever rank was actually
supported — `thymus-thymol` is a genus-level constituent, not a placeholder for
a species. `rank()` is total: every permit answers with its `LinealRank`.

**Clade placement is an axis, and it attaches at order only.** `PlantOrder`
carries a nullable `Clade` (`kernels/clades`) locating the order in the
rank-free phylogenetic tree; families, genera, and species resolve their clade
by walking up. This is a stated deviation from insects — every mainstream
botanical clade node is supra-ordinal (APG IV), so a per-rank placement would
only replicate the order's value.

**Cultivar, heritage, management, and phytochemistry are separate sub-contexts.**
`Cultivar` sits below species (the actual varieties in the ground); `SeedLineage`
tracks a preserved open-pollinated line across generations; `PlantProgram` holds
operational guidance keyed to a taxon; and `PhytochemicalConstituent` is the one
bridge into the chemistry catalog, naming a compound in a plant with the roles it
plays there. A compound's structural type lives on the chemistry side; its
ecological use category lives here — two complementary axes, not one flattened
field.

**The cross-domain edge is by slug only.** `PhytochemicalConstituent.compoundName`
references a chemistry `Compound` through the shared `identifiers` module;
`plants-api` has no compile-time dependency on `chemistry-api`. Native range is a
set of `Bioregion` values from the biogeography kernel.

**`Plant` composes the pieces for display.** `Plant` is a `ReadModel` — the rank
chain assembled top-down to the reached depth, plus features
(`OrganismFeatureView`), child taxa, ecological role, images, cultivars,
programs, and constituents. It is built in memory by `PlantFactory` from the
constituent queries, never persisted, and walks structural invariants
(ancestor-presence, cross-rank foreign-key consistency) on assembly.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance). Domain-specific notes:

- **`plants-api`** carries five sub-contexts as sub-packages — the rank/role core
  plus `cultivar/`, `heritage/`, `management/`, and `phytochemistry/`. Each is a
  visibility boundary; the four single-entity sub-contexts are namespace-collapsed
  (one entity, so no wrapping namespace).
- **`plants-core`** holds the query adapters and `PlantFactory`, the read-model
  assembler.
- **`plants-repository-rdms`** is the production-named persistence adapter; it
  currently delegates to the in-memory mock — a temporary state while the app is
  built out behind a stable port.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the working conventions, identity model, and the
  insects-alignment rules for this domain.
- [`docs/plants-ubl.md`](docs/plants-ubl.md) — the domain vocabulary and the
  cross-domain relationship map.
- [`docs/briefings/plants-domain.md`](../../docs/briefings/plants-domain.md) —
  narrative and ecological context: the Oak Vista lineup, the heritage program,
  and the plant ↔ compound bridge.
- [`docs/plans/organism-domain-blueprint.md`](../../docs/plans/organism-domain-blueprint.md) —
  the shared organism-domain design this module inherits from insects.
