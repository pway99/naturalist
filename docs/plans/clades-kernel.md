# Clades Kernel + Life-Stage Refactor — Effort Plan

A refactor that introduces a **Clade DAG** alongside the existing Linnaean
**Rank DAG**, and moves life-stage modelling from value-objects on
`InsectSpecies` / `InsectGenus` / `InsectFamily` to a traversal-based
resolver over the clade tree.

This is a sketch in the style of [`identification.md`](identification.md)
and [`catalog-kernel.md`](catalog-kernel.md) — direction and rationale,
phased delivery, no per-step TDD recipe. Each phase is promoted to its own
slice plan when its predecessor lands.

> **Plan history.** Imported 2026-05-12 from a separate research chat that
> worked through the LifeStage modelling fork [PL-1](../notes/parking-lot.md#pl-1--lifestage-modeling-dual-home-problem)
> raised on 2026-05-10. The work-tracker's prior one-line "add Clade and
> Rank to kernels/taxonomy" framing is superseded by the dual-DAG
> decomposition documented below.

---

## Why this exists

Life stages cannot be modelled cleanly as value objects on `InsectSpecies`
(or `InsectGenus` / `InsectFamily`). The reasons:

- Holometaboly is a **clade-level** trait that originated once at the
  Holometabola branch (~350 Mya) and is inherited by every descendant.
  Attaching it to species duplicates the same data across thousands of
  entities.
- The Linnaean ranks (Family, Genus, Species) are a *classification scheme*
  with a fixed number of named tiers; they do not always align with
  evolutionary branch points where traits actually live.
- Putting life stages on species bloats the species aggregate and pulls
  developmental-biology knowledge into the field-notes context, which only
  needs to record "I saw a caterpillar."

The correct decomposition is **two DAGs**:

- **Clade DAG** — the evolutionary tree of life. Carries traits like
  `Metaboly` at the node where they originated. Shared by plants, insects,
  fungi, everything.
- **Rank DAG** — the Linnaean rank hierarchy (Family, Genus, Species,
  Subspecies). Each taxon node references the clade it sits within.

Ranks depend on clades; clades know nothing about ranks.

PL-1's "dual home" smell is the surface symptom of this missing
distinction. `LifeStage` was modelled as a `NamedEntity` *and* held by
value on each `InsectSpecies` — neither home was the right one because
the trait lives at the *clade*, not at any rank.

---

## Project integration notes

**Terminology mapping.** The imported plan refers to "taxonomy (domain)
with ranks (kernel) and clades (kernel)" as nested levels. In this
codebase, `kernels/taxonomy/` is itself a kernel (per the DAG in the
root [`CLAUDE.md`](../../CLAUDE.md)); there is no "taxonomy domain"
above it. The substrate decision — one kernel with two packages
(`kernels/taxonomy/ranks/`, `kernels/taxonomy/clades/`) vs. two sibling
kernels (`kernels/ranks/`, `kernels/clades/`) — is a Phase 1 call.
Existing types (`LinnaeanFamily`, `LinnaeanGenus`, `LinnaeanSpecies`,
`LinnaeanSubspecies`, `TaxonomicClassification`) stay; whether they move
package is part of that decision.

**Existing entities.** Where the imported plan says "Family / Genus /
Species (taxon entities)", read it as the existing `LinnaeanFamily` /
`LinnaeanGenus` / `LinnaeanSpecies` / `LinnaeanSubspecies` records in
`kernels/taxonomy/`. The `placedIn: Clade` reference is added to each.

**Existing `LifeStage` record.** Today `LifeStage` is a `NamedEntity`
with its own repository and per-organism descriptions ("how a
green-lacewing larva looks"). The clade resolver answers *which stages
exist* (Egg / Larva / Pupa / Adult for a holometabolous insect); the
per-organism record continues to answer *what the organism looks like at
each stage*. Phase 5 reshapes the keying — stage-kind comes from the
clade's `Metaboly`, the per-organism record carries the description —
but the per-organism record does not disappear.

**Closes** [PL-1](../notes/parking-lot.md#pl-1--lifestage-modeling-dual-home-problem)
when Phase 5 lands. Unblocks
[PL-2](../notes/parking-lot.md#pl-2--slice-2-green-lacewing-rank-correction-paused)
(green-lacewing rank correction) at the same point: the resume shape
becomes "replace inline life-stage duplicates with clade references."

---

## Target architecture

```
kernels/taxonomy/
├── ranks/        — LinnaeanFamily / LinnaeanGenus / LinnaeanSpecies /
│                   LinnaeanSubspecies; the Linnaean DAG (existing)
└── clades/       — Clade nodes, traits, the evolutionary DAG (new)
```

(Final package / module split decided in Phase 1.)

### Clades kernel

- `Clade` node: id, name, parent reference (zero or one — Eukaryota is
  the root), trait bag.
- Open trait mechanism — traits are typed declarations attached to a
  clade. Initial trait: `MetabolyTrait(Metaboly)`.
- Traversal function: given a clade, walk up the parent chain looking
  for the nearest declaration of a given trait type.

### `Metaboly` value type

Sealed hierarchy defining the *kinds* of developmental patterns and the
stages each entails:

- `Ametabolous` → [Egg, Juvenile, Adult]
- `Hemimetabolous` → [Egg, Nymph(instar), Adult]
- `Holometabolous` → [Egg, Larva(instar), Pupa, Adult]

`Metaboly` is a value type, not an entity. It defines the stage
sequence. Clades reference one via a trait declaration.

The type lives in the kernel that owns developmental-biology vocabulary;
the clade kernel only stores the trait declaration. Trait *semantics*
are not the clades kernel's concern.

### Rank-side change

Each taxon node (`LinnaeanFamily`, `LinnaeanGenus`, `LinnaeanSpecies`,
`LinnaeanSubspecies`) gains an optional `placedIn: Clade` reference
indicating the most specific clade the taxon belongs to.

Resolving life stages for an organism:

1. Start at the species' `placedIn` clade.
2. Walk up the clade DAG.
3. Return the nearest declared `MetabolyTrait`.

Inheritance is **traversal-based, not duplicated**. Only the originating
clade declares the trait.

### Boundary discipline

- Clade kernel stores nodes, parent links, and trait bags. It does not
  know what a trait means.
- Trait *semantics* (what `Metaboly` defines, what stages exist) live in
  the kernels that own those concepts.
- Chemistry, organism, and interactions kernels remain independent. They
  consume resolved stage sets from the clade resolver; they do not own
  them.

---

## Validation case — Pipevine Swallowtail (*Battus philenor*)

This case exercises the full kernel collaboration, and overlaps directly
with the active pressure test at
[`pressure-test/battus-philenor/01-findings.md`](../pressure-test/battus-philenor/01-findings.md):

- **Clade DAG** declares `Holometabolous` at Holometabola. *B. philenor*
  resolves to [Egg, Larva, Pupa, Adult].
- **Chemistry kernel** defines `AristolochicAcidI` and `AristolochicAcidII`
  as `Compound` aggregates (already modelled).
- **Organism / species kernel** carries a stage-indexed `ChemicalDefense`
  for *B. philenor*: aristolochic acids present at every stage
  (maternally provisioned in egg; actively sequestered through larval
  instars; retained through pupation; retained in adult; transferred via
  spermatophore in males).
- **Interactions kernel** carries the trophic relationship: *B. philenor*
  larvae → *Aristolochia* (genus) host plants → outcome
  `SequestersCompound`. Scoped to the larval stage.

Aposematic coloration and mimicry relationships (Spicebush Swallowtail,
dark-morph female Eastern Tiger Swallowtail, Red-spotted Purple) are
modelled as separate signal / interaction traits on the adult stage, not
conflated with the chemical defense itself.

The defense is **not** a clade trait — it is specific to *B. philenor*
and a few related Papilionidae. The clade DAG only contributes the stage
*skeleton*; the species fills in what is true at each stage.

---

## Phase summary

| Phase | Name                                                | Status     | Gates on                                                                |
| ----- | --------------------------------------------------- | ---------- | ----------------------------------------------------------------------- |
| 1     | Build the `clades` kernel standalone                | **next**   | Nothing — fully unblocked                                               |
| 2     | Define `Metaboly` and attach to Holometabola        | sketched   | Phase 1                                                                 |
| 3     | Add `placedIn: Clade` to taxon entities             | sketched   | Phase 1 (Phase 2 not strictly required)                                 |
| 4     | Place insects into the clade DAG                    | sketched   | Phases 2 + 3                                                            |
| 5     | Route life-stage queries through the clade resolver | sketched   | Phase 4; closes PL-1                                                    |
| 6     | Extend to plants when needed                        | deferred   | Plant identification work demanding it                                  |

---

## Phase 1 — Build the `clades` kernel standalone

**Why first.** Stand up the substrate before any consumer depends on it.
The kernel is small (a tree, a typed trait bag, a traversal) and can be
exercised in isolation.

**Delivers.**

- Domain types: `Clade` (record), parent reference, trait bag, traversal
  function. `Clade` carries an `EntityName` per the project's identity
  rules (cross-`NamedEntity` references are by `EntityName`).
- No wiring to existing taxonomy yet. The kernel ships with its own
  in-memory test-source seeded with a minimal slice: Eukaryota →
  Animalia → Arthropoda → Insecta → Holometabola → Lepidoptera →
  Papilionidae.
- Substrate decision: one taxonomy kernel with `ranks/` and `clades/`
  packages, or two sibling kernels `kernels/ranks/` (renamed from
  `kernels/taxonomy/`) and `kernels/clades/`. Decided in this phase, not
  pre-committed.
- Repository + query stack per the project's standard scaffolding
  (entity-repository, entity-query, test-entity-source skills).

**Out of scope.** Trait semantics. Linkage to the rank DAG. Life-stage
removal.

---

## Phase 2 — Define `Metaboly` and attach to Holometabola

**Why.** Prove the trait-declaration / traversal contract before tying
the clade kernel to anything downstream.

**Delivers.**

- Implement `Metaboly` as a sealed type with the three developmental
  patterns (`Ametabolous`, `Hemimetabolous`, `Holometabolous`) and their
  stage sequences. Located in the kernel that owns
  developmental-biology vocabulary — naming and exact module placement
  decided in this phase. The clade kernel imports nothing from that
  module; only consumers do.
- Declare `MetabolyTrait(Holometabolous)` on the Holometabola clade in
  the Phase 1 seed.
- Verification test: query Lepidoptera for its metaboly via traversal;
  expect `Holometabolous`. Query Insecta (above Holometabola); expect
  nothing.

**Out of scope.** Other traits. Stage-indexed organism data. Removing
the existing `LifeStage` NamedEntity (Phase 5).

---

## Phase 3 — Add `placedIn: Clade` to taxon entities

**Why.** Connect the Rank DAG to the Clade DAG. One optional reference;
no behavioural shift on its own.

**Delivers.**

- Single optional field on existing `LinnaeanFamily`, `LinnaeanGenus`,
  `LinnaeanSpecies`, `LinnaeanSubspecies` records — `placedIn:
  Optional<Clade>` (or `ClassName` per the project's identifier rules;
  cross-`NamedEntity` references are by `EntityName`).
- All existing data remains valid; the field is null for unplaced taxa.
- No other changes to the rank model. No reorganisation of existing
  catalog data — that lands in Phase 4.

**Out of scope.** Placing existing records into the seeded clade tree
(Phase 4). Resolver wiring (Phase 5).

---

## Phase 4 — Place insects into the clade DAG

**Why.** Make the end-to-end resolution path real for at least one
species.

**Delivers.**

- Place *Battus philenor* (`LinnaeanSpecies`) in Papilionidae (`Clade`).
- Place Papilionidae (`LinnaeanFamily`) alongside Papilionidae
  (`Clade`). Family-rank and family-clade have the same scientific name
  by convention; they are still two distinct records (one Rank-DAG,
  one Clade-DAG).
- Confirm end-to-end resolution: species → `placedIn` → traversal →
  metaboly → stage set [Egg, Larva, Pupa, Adult].
- Extend to the remaining catalogued insects only as far as is needed
  for Phase 5's resolver to replace inline life-stage usage.
  Reorganisation across all ~16 species is incremental; not gated on
  this phase.

**Out of scope.** Bulk import of a taxonomic backbone (out of scope for
the refactor entirely — see "Out of scope" section).

---

## Phase 5 — Route life-stage queries through the clade resolver

**Why.** Close PL-1. Replace the dual-home value-object with traversal.

**Delivers.**

- Wherever the existing model asks "what life stages does this organism
  have," route through the clade resolver. The resolver answers *which
  stages exist*; per-organism description data (descriptions, image
  references, instar notes) remains keyed by `(organismName,
  stageKind)`.
- If life-stage *stage-kind enumeration* was previously stored on
  `InsectSpecies` / `InsectGenus` / `InsectFamily` as value-object
  duplicates, remove those duplicates once the resolver is wired in.
  Removal is a separate change from the resolver introduction.
- Unblocks [PL-2](../notes/parking-lot.md#pl-2--slice-2-green-lacewing-rank-correction-paused).
  Slice 2 resumes as "replace inline duplicates with clade references"
  — much smaller per-organism work because the per-organism description
  records already exist independently of the inline copies that would
  otherwise have been promoted up-rank.

**Open question — `LifeStage` NamedEntity shape.** The existing
`LifeStage` carries per-organism descriptions. After Phase 5, it
remains keyed by (organism, stage-kind); the *kinds* it can take come
from the clade's `Metaboly`, no longer from a free-form
`life-stages.json` enumeration. Whether the kind is enforced at write
time (the `LifeStage` record validates its stage-kind against the
organism's clade) or only at query time is decided when the phase
plan is promoted.

---

## Phase 6 — Extend to plants when needed

**Why.** Same kernel, no schema changes. Gated on plant identification
work demanding it. Deferred until then.

**Delivers when activated.**

- Place *Aristolochia* by walking up the plant side: Aristolochiaceae →
  Piperales → Magnoliids → Angiosperms → Plantae → Eukaryota (joining
  the existing animal side at Eukaryota).
- Plant-specific traits (e.g., photosynthesis) declared at the
  appropriate clade level when a consumer demands them. None demanded
  by the current roadmap.

---

## Key decisions and rationale

| Decision                                                | Rationale                                                                                                                          |
| ------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Two DAGs, not one                                       | Clades and ranks model different things (ancestry vs. classification scheme). Conflating them forces rank-rigidity biology doesn't honor. |
| Clades sits in `kernels/taxonomy/` (Phase 1 confirms)   | Clades and ranks are two views of the same underlying tree of life; sharing a kernel keeps the DAG layer simple. Final split decided in Phase 1. |
| Shared clade DAG across plants and insects              | The tree of life is one tree. Plant and animal lineages meet at Eukaryota. The same kernel serves both.                            |
| Inheritance by traversal, not by duplication            | A trait is declared once at its originating clade. Descendants resolve it by walking up. No data duplication; corrections propagate naturally. |
| `placedIn` on every taxon, not just Family / Genus      | Restricting to specific ranks reintroduces rank-rigidity. Species sits in a clade too.                                             |
| Clade kernel is trait-agnostic                          | Storage and traversal only. Trait semantics live in the kernels that own them. Keeps clades simple and reusable.                   |

---

## Out of scope for this refactor

- Importing a full taxonomic backbone (Catalogue of Life / ChecklistBank).
  Considered separately; the model is compatible with either local seed
  data or an imported ColDP snapshot.
- Modelling instars as first-class entities. The `Metaboly` stage
  definitions allow instar parameters; whether to track specific instars
  on observations is a field-notes decision, not a taxonomy one.
- Caste differentiation in eusocial Hymenoptera. Same mechanism
  (stage-indexed traits on species), addressed when a relevant species
  is modelled.
- Sexual dimorphism appearing only at adult stage. Same mechanism.

---

## Risk register

- **Clade kernel scope creep.** The kernel must remain a tree with
  traits. Resist adding semantics. *Mitigation:* code-review checklist;
  any new capability in the clade kernel requires explicit
  justification.
- **Trait declarations at the wrong level.** Misplacing `Metaboly` —
  e.g., declaring it on Insecta instead of Holometabola — silently
  produces wrong results for non-holometabolous orders. *Mitigation:*
  validation test that walks known species (silverfish, dragonfly,
  butterfly) and confirms expected stage sets, even though only
  butterfly is in the catalog today.
- **Stale `placedIn` after taxonomic revision.** If a species is
  reclassified, its clade reference may need updating. *Mitigation:*
  treat `placedIn` as an explicit, reviewable field; do not
  auto-populate from rank lookups.

---

## Slot in the work-tracker

Row in [`docs/work-tracker.md`](../work-tracker.md):

> **Clades kernel + life-stage refactor** — Plan (sketch) — *active —
> Phase 1 next* — [`plans/clades-kernel.md`](plans/clades-kernel.md) —
> *Multi-week. Introduces a Clade DAG alongside the existing Rank DAG;
> moves life-stage modelling from value-objects to traversal-based
> resolution. Phase 1 stands up the kernel; Phases 2–4 wire trait
> semantics and place the *Battus philenor* validation case end-to-end;
> Phase 5 routes life-stage queries through the resolver and closes
> PL-1; Phase 6 extends to plants when demanded.*

As each phase is promoted to its own slice plan, that plan gets its own
row; the roadmap row remains as the index.

---

## Open questions (deferred to per-phase plans)

- **Phase 1** — One kernel (`kernels/taxonomy/{ranks,clades}/`) vs. two
  sibling kernels (`kernels/ranks/` + `kernels/clades/`). Decided when
  the kernel is drafted.
- **Phase 2** — Module placement of the `Metaboly` sealed type. A
  `kernels/developmental-biology/` for the developmental vocabulary, or
  fold into `kernels/field-notes/`, or keep in the consumer
  (`domains/insects/*-api/`). Decided when the trait is drafted.
- **Phase 3** — Field type on the rank entities — `Clade` (record
  reference) vs. `CladeName` (`EntityName` subclass per project rules
  for cross-`NamedEntity` references). The project's identity model
  favours the latter; confirmed when the phase plan is written.
- **Phase 5** — Whether `LifeStage` validates its stage-kind against
  the organism's clade at write time or only at query time. Decided
  when the phase plan is written.
- **Phase 6** — Naming and root choice on the plant side
  (Plantae vs. Viridiplantae vs. Archaeplastida — different sources
  draw the line differently). Decided when plant work activates.
