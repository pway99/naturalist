# Structural Commitments — Identity Model

**Status.** Pre-Phase-1b. These commitments are made *upstream* of the audit
and are the assumed identity model the Phase 1b findings will reference.

**Purpose.** Capture the identity-model decisions made during the Phase 1a
review that are larger than any single finding and that must be settled
before per-node evaluation begins. Without this document, every Phase 1b
finding that touches identity would have to re-derive these decisions.

**Sibling documents.**
- `00-charter.md` — governance, the five evaluation axes, workflow rhythm.
- `phase-1a-orientation.md` — the shared map of nodes and edges the
  swallowtail story exercises.
- `01-findings.md` — Phase 1b output (not yet started).
- `99-followups.md` — deferred items, including questions named here but
  not resolved here.

**Resumption note.** A fresh chat reading these three documents — charter,
orientation, this — has enough context to start Phase 1b. The commitments
below are settled; pushback is welcome but should be raised explicitly
rather than absorbed implicitly.

---

## 1. Context — why this document exists

The Phase 1a orientation surfaced a slug-mismatch finding at §7: the
insect-side reference `larva.hostPlants : ["aristolochia-californica"]`
does not resolve to any `Plant.name` in the production catalog (the plant
is slugged `california-pipevine`). This is a real referential-integrity
break in the swallowtail story.

What looked initially like a single-finding slug normalization question
opened, under examination, into a structural identity discussion. The
discussion converged on a small set of architectural commitments that
together resolve the slug mismatch and codify identity discipline for
living-organism aggregates. Those commitments are recorded here.

The commitments below are made in light of these framings, all settled
during the Phase 1a review:

- **Path A.** Living-organism aggregates remain `NamedEntity` (slug
  identity). The slug mismatch is treated as a data and convention
  problem, not as evidence that surrogate-identity (UUIDv7) promotion is
  needed. Reclassification events and surrogate-identity promotion are
  explicitly deferred — see `99-followups.md`.
- **Multi-tenant target.** The catalog is intended to support many
  naturalists, not one. Vernacular slugs are not sufficient at this
  scope: regional vernacular collisions (e.g., "robin" naming different
  birds) become unavoidable. Binomial form is the only globally stable
  identity option for living-organism slugs.
- **`kernels/taxonomy` correctly placed.** The shape (Linnaean
  classification structure) is shared across living-organism domains;
  the data is not. The kernel stays as it is and grows additively.

---

## 2. Commitment 1 — Binomial form for living-organism `EntityName` slugs

`PlantName`, `InsectSpeciesName`, and any future living-organism slug type
encode the **binomial name in lowercase kebab form**: `genus-species`.

| Domain  | Old (vernacular)        | New (binomial)              |
|---------|-------------------------|-----------------------------|
| Plant   | `california-pipevine`   | `aristolochia-californica`  |
| Plant   | `tomato`                | `solanum-lycopersicum`      |
| Insect  | `pipevine-swallowtail`  | `battus-philenor`           |
| Insect  | `green-lacewing`        | `chrysoperla-rufilabris`*   |
| Insect  | `braconid-wasp`         | (binomial of catalogued species) |

*Specific binomial pending — `green-lacewing` may catalog at family or
genus level; see Commitment 5 about under-identified organisms.

This choice is justified by:

- **Stability across regional vernacular variation.** A Sacramento Valley
  naturalist and a New England naturalist agree on *Battus philenor*;
  they may disagree on the common name.
- **Established naming authority.** Linnaean nomenclature has explicit
  rules for resolving disagreement (priority, ICZN/ICN governance).
  Vernacular naming has none.
- **Globally lower collision rate.** A small number of binomial
  homonyms exist across kingdoms, but they're rare. Vernacular collisions
  within a single kingdom are everyday occurrences in a multi-tenant catalog.

---

## 3. Commitment 2 — `CommonName` carries the vernacular surface

`Plant` already carries `Set<CommonName>` (each `CommonName` carries a
`label` and a `locale`). This is the catalog's vernacular surface and is
load-bearing under multi-tenancy.

Living-organism aggregates that do not yet expose a `Set<CommonName>` —
notably `InsectSpecies` — will gain one. This is additive; no field is
removed.

The catalog kernel's `CatalogContribution` SPI emits surface tokens for
search routing. For a living-organism entity, the contribution should
emit the binomial slug, every `CommonName` label, the genus epithet
alone, and any regional or alternate-language forms registered. A
naturalist searching "California pipevine" finds `aristolochia-californica`
through the search index, even though that exact phrase is not the slug.

This means the binomial slug is the **identity**; vernacular forms are
**findable but not authoritative**. Multi-tenant disagreement on
vernacular naming is accommodated through the `Set` shape — a single
species can carry many common names, locale-tagged, without forcing the
catalog to pick one.

---

## 4. Commitment 3 — `kernels/taxonomy` codifies species- and subspecies-rank contracts

The taxonomy kernel grows two interfaces that codify what it means for an
entity to represent a species-rank or subspecies-rank Linnaean taxon:

- **`LinnaeanSpecies`** — contract for entities at species rank. The
  contract:
  - Exposes the genus and species epithet as taxonomic identity.
  - Asserts that both are non-null on this entity (where the underlying
    `TaxonomicClassification` VO permits null genus and species for
    higher-rank uses, this interface narrows the contract).
  - Exposes the binomial slug, derived from the epithets.

- **`LinnaeanSubspecies`** — contract for entities at subspecies rank.
  The contract:
  - Exposes the parent species reference (typed as a domain-specific
    `EntityName` subtype via the same generic pattern the framework's
    `Named<NAME>` already uses).
  - Exposes the genus, species, and subspecies epithet — the full
    trinomial — as taxonomic identity, redundant with the parent's
    genus and species but locally self-sufficient.
  - Exposes the trinomial slug, derived from the epithets.

The kernel grows interfaces, not new aggregate types or sealed
hierarchies. Existing `Plant` and `InsectSpecies` aggregates implement
`LinnaeanSpecies`. No `InsectSubspecies` aggregate is created today;
when a subspecies entry first needs to enter the catalog, the
implementing record is designed at that point.

The interfaces are kernel-resident because their shape is identical
across living-organism domains — exactly the criterion the framework
applies for kernel candidacy.

---

## 5. Commitment 4 — Slugs are *derived*, not assigned

The most architecturally significant commitment. Slugs for entities
implementing `LinnaeanSpecies` and `LinnaeanSubspecies` are computed
from the entity's taxonomic components, not supplied independently.

For `LinnaeanSpecies`:

```
slug = lowerKebab(genus) + "-" + lowerKebab(species)
```

For `LinnaeanSubspecies`:

```
slug = lowerKebab(genus) + "-" + lowerKebab(species) + "-" + lowerKebab(subspeciesEpithet)
```

The kernel provides default methods on the interface that perform the
composition; the implementing record provides the parts.

### Why this matters

- **There is no slug-design judgment per entry.** The slug is mechanical
  output of taxonomic data. The slug-naturalness test from the charter
  passes by construction.
- **Slug-mismatch is architecturally impossible.** Since the slug is
  derived, an entity cannot disagree with itself — `genus = Aristolochia`
  cannot coexist with `slug = california-pipevine`, because the slug
  is a function of the genus.
- **Cross-domain references resolve correctly by construction.** When
  `larva.hostPlants` carries `["aristolochia-californica"]`, that slug
  *will* match the plant whose `genus = Aristolochia, species = californica` —
  because that plant's slug is derivable from those exact epithets.

### What this rules out

- **A `LinnaeanSpecies` cannot have null genus or species.** The slug
  derivation requires both. This is a real contract narrowing relative
  to the underlying `TaxonomicClassification` VO. Entities that cannot
  populate both genus and species — for example, a naturalist's
  family-level identification awaiting refinement — cannot implement
  `LinnaeanSpecies` and therefore cannot enter the `Plant` or
  `InsectSpecies` catalog as currently shaped. This is a real workflow
  question; see Commitment 5.

### Composite-key inheritance

Composite-key slugs (`LifeStageName`, `PlantProgramName`, `CultivarName`,
`SeedLineageName`, `PhytochemicalConstituentName`) compose from
binomial-derived inputs. The factory pattern is unchanged; the resulting
slugs change because their inputs change.

| Composite slug                  | Old form                                            | New form                                                |
|---------------------------------|-----------------------------------------------------|---------------------------------------------------------|
| `LifeStageName`                 | `pipevine-swallowtail-larva`                        | `battus-philenor-larva`                                 |
| `PhytochemicalConstituentName`  | `california-pipevine-aristolochic-acid-i`           | `aristolochia-californica-aristolochic-acid-i`          |
| `PlantProgramName`              | `pipevine-pesticide-exclusion`                      | (no change — program names are activity-named, not plant-named) |
| `CultivarName`                  | `tomato-amish-paste`                                | `solanum-lycopersicum-amish-paste`                      |
| `SeedLineageName`               | (depends on cultivar)                               | (depends on cultivar)                                   |

Plant programs are an interesting case worth flagging: the briefing
says programs are "named after the activity, not the plant." If
`PlantProgramName` is *not* a composite of `PlantName` and an activity,
it is unaffected by this commitment. If it *is* a composite (which the
existing code should reveal — verify during migration), it inherits
the binomial form.

---

## 6. Commitment 5 — Under-identified organisms are deferred

A naturalist engaged in field identification commonly has organisms with
genus-level or family-level confidence but no species identification.
Under the slug-derivation rule, such organisms cannot enter the
`Plant` or `InsectSpecies` catalog as `LinnaeanSpecies` implementations
because the contract requires non-null genus *and* species.

Three resolution paths exist:

1. **Defer entry until species-level identification is achieved.**
2. **Separate aggregate for unidentified specimens** (e.g.,
   `UnidentifiedSpecimen`), promoted to `Plant` / `InsectSpecies` when
   identification firms.
3. **Accommodate provisional epithets** (e.g., `Aristolochia sp.`) in
   `LinnaeanSpecies`, with derived slug `aristolochia-sp` or similar.

The swallowtail story does not exercise this — *Battus philenor*
identification is firm, and every catalogued organism in the current
JSON files has species-level taxonomy. **The question is therefore
deferred** and recorded in `99-followups.md` as `FU-1` for later
resolution.

The audit proceeds with the swallowtail story under the assumption that
all catalogued organisms in scope have species-level identification.
This assumption holds for the production data we have read.

---

## 7. Commitment 6 — Migration scope and shape

Adopting these commitments requires a one-time migration touching:

| Catalog file                                              | Records affected | Type of change                              |
|-----------------------------------------------------------|------------------|---------------------------------------------|
| `plants/plants.json`                                      | 19               | Slug changes from vernacular to binomial    |
| `insect-species.json`                                     | 16               | Slug changes from vernacular to binomial    |
| `life-stages.json`                                        | 60               | Composite slug rederives                    |
| `plants/phytochemistry/phytochemical-constituents.json`   | 4                | Composite slug rederives                    |
| `plants/cultivar/cultivars.json`                          | 4                | Composite slug rederives (if composite)     |
| `plants/heritage/seed-lineages.json`                      | 6                | Composite slug rederives (if composite)     |
| `plants/management/plant-programs.json`                   | 8                | May or may not rederive (verify pattern)    |

Plus every cross-domain reference field carrying old slugs is updated.

The migration is **algorithmic** — for each affected record, the new
slug is computed from current taxonomic data; for each cross-reference,
the slug is updated to match its target's new slug. There is no per-record
judgment.

Migration happens in a single coordinated batch — slugs do not migrate
piecemeal because cross-references would break in transit.

The migration is the closure work for Phase 1b finding `A1-F1` (the
slug mismatch finding to be logged when Phase 1b begins). The migration
produces the new slugs; the closure is the bundle and catalog data
expressing the swallowtail story under the new convention.

---

## 8. What was considered and rejected

For traceability, the alternatives we considered and explicitly rejected:

- **Path B (surrogate-identity promotion).** Promoting `Plant` and
  `InsectSpecies` to `Entity<XId>` to handle reclassification through
  identity-stability rather than convention. Rejected because the
  reclassification problem is rare enough in practice to handle as data
  migration when it arises, and Path B's cost (rebuilding cross-domain
  reference shape, since surrogate ids do not cross domain boundaries by
  value) outweighs the benefit at current scope.

- **Per-domain taxonomy (no shared kernel).** Each living domain owns
  its taxonomic structure independently. Rejected because the shape is
  identical across living domains; sharing the type does not share the
  data; the framework's "kingdom-specific belongs in the domain" rule is
  not violated by structural sharing.

- **Vernacular slugs.** Rejected because the catalog's multi-tenant
  target makes regional vernacular collisions a daily occurrence. Local
  defensibility within a single naturalist's catalog does not generalize.

- **Sealed-supertype hierarchy** (`InsectTaxon` sealed over `InsectSpecies`
  + `InsectSubspecies`). Rejected in favor of interface contracts
  (`LinnaeanSpecies`, `LinnaeanSubspecies`) because interfaces compose
  additively without forcing existing consumers to handle a sealed
  family.

- **Rank-as-data on a unified `Taxon` aggregate.** Rejected because it
  collapses meaningful distinctions into nullability patterns, and
  because the existing aggregates (`Plant`, `InsectSpecies`) already
  model species-rank organisms cleanly.

---

## 9. Audit consequences — what these commitments mean for Phase 1b

Phase 1b proceeds with these commitments in force. Concretely:

- **Findings touching identity reference this document** rather than
  re-deriving the convention. A finding can read "per Structural
  Commitment 4, slug derivation is binding" rather than re-arguing it.
- **The first Phase 1b finding will be the slug mismatch (`A1-F1`).**
  Its proposed resolution is the migration described in §7. Its closure
  is the bundle expressing the swallowtail story under the new slugs.
- **Under-identified organisms (Commitment 5) are explicitly out of
  audit scope** and tracked in `99-followups.md` as `FU-1`.
- **The kernel grows during Phase 1b** to add `LinnaeanSpecies` and
  `LinnaeanSubspecies` interfaces. This is a kernel change made
  in-session per the charter's real-time coevolution rhythm.

---

## 10. Followups raised by these commitments

The following items are surfaced here and recorded in `99-followups.md`
for later resolution:

- **`FU-1`** — under-identified organisms. The slug-derivation rule
  precludes family-level or genus-level provisional entries from
  `LinnaeanSpecies`. Resolution path TBD (see Commitment 5).
- **`FU-2`** — bibliography / provenance kernel. Captured during the
  identity discussion; not resolved here. See `99-followups.md`.
- **`FU-3`** — identification as a per-domain first-class concern.
  Captured during the identity discussion; not resolved here.
- **`FU-4`** — reclassification events. Considered (Path B) and rejected
  for now. Recorded so it is not silently reopened.
