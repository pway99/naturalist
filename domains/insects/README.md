# Insects

The reference organism domain — the one every other organism domain (plants,
arachnids, fungi, …) copies rather than re-derives. If you want to understand
how this codebase models living things, read this one.

It catalogs insects at whatever taxonomic rank the evidence supports, links
photographs and field observations to the naturalist who made them, and layers
ecology, chemistry, and life-stage biology on top of the Linnaean backbone.

---

## Core concepts

**The catalog is a Linnaean hierarchy of first-class entities.** `InsectOrder`,
`InsectFamily`, `InsectGenus`, and `InsectSpecies` are each a `NamedEntity`
with a typed slug identity (`lepidoptera`, `papilionidae`, `battus`,
`battus-philenor`) and a typed foreign key to its immediate parent. There is no
grandparent shortcut — resolving species → order walks the chain.

**Identification lands at the most specific rank the evidence supports — and
stays there.** A photograph that only proves "some kind of leafhopper" is
cataloged at family, permanently, not as a placeholder for a species you might
confirm later. To make that work, records that attach to organisms —
`InsectImage`, `FieldObservation`, functional roles, feature assignments —
carry a polymorphic `InsectRankName` that can point at *any* rank. Promoting an
observation from genus to species is a single-field update.

**A field observation is the unit of a naturalist's collection.**
`FieldObservation` records that a given naturalist encountered a subject at a
point in time. "Insects I've collected" is the distinct set of subjects across
a naturalist's observations — repeated sightings don't multiply the collection.
Photographs are optional evidence that link back to the observation.

**Vision-assisted identification is the marquee feature.** A photo flows through
an LLM-backed `VisionService` to a structured identification, which is assembled
into a `CatalogIdentification` aggregate and persisted atomically — species (if
new), taxonomy, image, and observation in one transaction.

**Ecology, chemistry, and biology are separate axes, not extra columns.**
Functional guilds (pollinator, predator, keystone …), chemical defense, and the
four insect life stages (egg / larva / pupa / adult, resolved from a clade's
metaboly) are modeled as their own entities and value objects, so the species
record stays about identity rather than accreting every fact anyone might know.

**Read models compose the pieces for display.** `Insect` is a rank-chain read
model — a sum of everything known at the reached identification depth — with
structural invariants (ancestor-presence, cross-rank FK consistency) walked on
assembly. It is built in memory from the constituent repositories, never
persisted.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance):

- **`insects-api`** — entities, typed identifiers, and the `InsectQuery` /
  `InsectCommand` / `InsectEntityCollections` namespaces.
- **`insects-core`** — query and command adapters, the `InsectFactory` read-model
  assembler, and the `@DomainService` identification pipeline.
- **`insects-repository-test`** — the in-memory adapter, the behavioral contract,
  and the JSON seed catalog (real Oak Vista species).
- **`insects-repository-rdms`** — production persistence adapter (currently
  delegating to the in-memory mock; see the top-level README).
- **`insects-console`** — JTE view fragments for the management console.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the working conventions and invariants for this domain.
- [`docs/briefings/insects-domain.md`](../../docs/briefings/insects-domain.md) —
  a full type-by-type tour of the module.
- [`docs/briefings/vision-identification.md`](../../docs/briefings/vision-identification.md) —
  the photo → catalog identification pipeline end to end.
- [`docs/plans/organism-domain-blueprint.md`](../../docs/plans/organism-domain-blueprint.md) —
  the shared organism-domain design this module is the reference for.
