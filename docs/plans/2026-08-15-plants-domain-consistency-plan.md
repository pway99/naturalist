# Plants Domain Consistency — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans or
> subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Bring the plants domain onto the design the insects domain established —
typed rank chain, one identifier space per organism, ADR-020 namespace shapes, and a
console/catalog surface where every indexed entity is reachable.

**Context:** Plants predates insects. It grew a rank layer (`PlantFamily`, `PlantGenus`)
after the fact, bolted alongside the existing `Plant` catalog rather than absorbing it,
and never picked up the namespace conventions that ADR-020 settled later. The result is
three coupled problems: five organisms exist twice under colliding slugs, `Plant` has no
typed parent, and the family/genus records that *are* indexed for search cannot be
rendered.

**Already shipped** (branch `plants-consistency`, separate from this plan): repository
mock argument validation, package-private mocks, the `plants-api` invariant test suite,
and `domains/plants/CLAUDE.md` accuracy. Those were mechanical; everything below needs a
decision or touches data.

## Global Constraints

- Framework types are `TestEntitySource` / `EntityRepository` / `EntityRepositoryTest` /
  `EntityQuery` / `AbstractEntityQuery` (NOT the `Named*` names in CLAUDE.md prose).
- Repository interfaces package-private in `plants-api`; queries public; factories
  package-private concrete classes in `plants-core`, never in `plants-api`.
- `new Foo(...)` only inside the type's own class; elsewhere use static factories.
- Invariant tests use the Observer idiom, not `assertThrows`.
- Catalogs are real Oak Vista data. A failing test means a domain model error or a
  real-world change — never "fix the fixture to match the code".
- **Record arity changes ripple repo-wide.** Adding a component to `Plant` breaks every
  `new Plant(...)` site: contract tests, api tests, JSON catalogs, and any JTE template
  reading the record. Grep the whole repo; do not trust this plan's file list.
- **Build/verify:** the user runs `mvn verify` from repo root. Agent sessions cannot run
  mvn; use IDE per-file problem checks. Do not claim "passing" — claim "written,
  IDE-clean, awaiting `mvn verify`".
- Kernel/record signature changes need a clean install, not incremental `-pl -am`.
- No commits unless the user says "commit".

---

## M1 — Decide what `Plant` means  ✅ DECIDED 2026-08-15: Option A

**`Plant` becomes strictly species-rank.** A `Plant` record requires a resolved species
epithet; an organism identified only to genus lives in `PlantGenus`, and one identified
only to family lives in `PlantFamily`. This is the insects model — `InsectSpecies.epithet`
is required and validated, and under-identified organisms have a permanent home at their
actual rank rather than a species record with a null epithet. It is also what
`PlantGenus`'s own javadoc already promises and what the code currently fails to enforce.
The alternatives were rejected because Option B abandons the rank layer weeks after adding
it and makes M2's typed FK impossible, and Option C codifies the duplication behind
cosmetically distinct slugs.

Consequences, all of which M2 must carry:

- The five duplicated organisms (`creeping-thyme`, `ornamental-passiflora`, `dianthus`,
  `sage`, `citrus`) are deleted from `plants.json`; their `PlantGenus` records become the
  sole home for those taxa.
- `Plant` gains a required species epithet — either by tightening the `taxonomy`
  component's validation or by promoting the epithet to its own component.
- A `PlantRankName` sealed type is needed so records that attach to *any* rank can be
  typed. See the sub-fork in M2.
- `creeping-thyme-thymol` is re-keyed to the `thymus` genus, which means
  `PhytochemicalConstituent.plantName` must accept a rank name rather than a `PlantName`.

Everything below depends on this. Do not start M2 or M3 before reading it.

**The problem.** Five organisms are recorded twice:

| Slug                     | As `Plant`                                  | As `PlantGenus`        |
|--------------------------|---------------------------------------------|------------------------|
| `creeping-thyme`         | `species: null`, GROUND_COVER, PERENNIAL    | `thymus`               |
| `ornamental-passiflora`  | `species: null`, KEYSTONE_HOST, VINE        | `passiflora`           |
| `dianthus`               | `species: null`, ORNAMENTAL, PERENNIAL      | `dianthus` ← collides  |
| `sage`                   | `species: null`, POLLINATOR_SUPPORT         | `salvia`               |
| `citrus`                 | `species: null`, FOOD_CROP, TREE            | `citrus`  ← collides   |

`PlantName.of("dianthus")` and `PlantGenusName.of("dianthus")` are the same string in two
identifier spaces. `PlantsCatalogContribution` emits the token `"Dianthus"` for both refs.

Insects resolved this shape by making `InsectSpecies.epithet` required and giving
under-identified organisms a home at their actual rank. `PlantGenus`'s own javadoc claims
the same intent — "a naturalist who recognises a *Thymus* mat-forming herb without
resolving the species has a permanent home for that observation here" — but the code does
not enforce it, because `TaxonomicClassification` permits a null species and `Plant`
validates only the classification as a whole.

**Why it is not a simple delete.** The five `Plant` rows carry `roles` and `lifeForm`,
which `PlantGenus` has no components for. `creeping-thyme` is also the FK target of the
`creeping-thyme-thymol` constituent. Deleting the rows loses ecological data and breaks a
soft FK.

### Option A — `Plant` becomes strictly species-rank (recommended)

Mirrors insects exactly. `Plant` requires a non-null species epithet; the five genus-level
organisms move to `PlantGenus`, which gains `roles` and `lifeForm`.

- **For:** one organism, one record, one identifier space. Makes M2's typed FK
  well-defined. `PlantGenus` becomes a real catalog citizen rather than a lookup table,
  which is what its javadoc already promises.
- **Against:** widest change. `PlantGenus` gains two components (arity ripple).
  `creeping-thyme-thymol` must be re-keyed to a genus-level constituent, which means
  `PhytochemicalConstituent.plantName` has to accept a genus — either a `PlantRankName`
  sealed type (the insects answer) or a second nullable FK.
- **Cost:** the `PlantRankName` sealed type is the honest version and is a prerequisite
  for M3 doing rank-polymorphic routing the way insects does.

### Option B — `Plant` is any-rank; delete the `PlantGenus` records

Drop the five `PlantGenus` rows, keep the `Plant` rows, and let `Plant` continue to mean
"a taxon at whatever rank we resolved."

- **For:** smallest change. No arity ripple, no FK re-keying.
- **Against:** abandons the rank layer four weeks after adding it, leaves
  `PlantFamily`/`PlantGenus` as a 14-row family index with no genus tier, and diverges
  permanently from insects. M2's typed `genusName` FK becomes impossible — there is
  nothing to point at.

### Option C — keep both, disambiguate the slugs

Rename the colliding `Plant` slugs (`dianthus` → `dianthus-spp`), keep both records.

- **For:** no data loss, no arity change.
- **Against:** codifies the duplication. Two records per organism, two search hits, two
  detail pages, and a reader with no way to know which is authoritative. This is the
  status quo with a coat of paint.

**Recommendation: Option A.** It is the only one that leaves plants and insects on the
same model, and the `PlantRankName` work it forces is a prerequisite for M3 anyway. Take
Option B only as a deliberate decision that plants will *not* follow the insects rank
model, in which case M2 and M3 both need rewriting.

- [ ] **Decision recorded** in this file with a date and one paragraph of rationale.

---

## M2 — Species-rank `Plant`, typed rank chain, cross-rank roles

**Depends on:** M1. This is the long pole — do it in the lettered order below, each step
its own PR.

**Target shape** (decided 2026-08-15 alongside M1):

```
Plant(name, genusName, epithet, description, lifeForm, nativeBioregions, commonNames)
PlantGenus(name, familyName, order, family, genus, description, lifeForm, commonNames)
PlantEcologicalRole(id, PlantRankName parentName, Set<PlantRole> roles)
```

`Plant` mirrors `InsectSpecies`: a typed upward FK plus its own epithet, no local
`TaxonomicClassification`. `roles` becomes a cross-rank entity for the same reason
insects made that move in PL-11 — an organism identified only to genus has ecological
roles too, and duplicating the component onto every rank record is the shape insects
backed out of. `lifeForm` stays on the taxon records: it is a morphological trait
intrinsic to the taxon, not a site-specific assignment, and a reader should not need a
second query to learn that a plant is a vine.

### M2a — `PlantRankName` sealed type  ✅ SHIPPED 2026-08-15

- [x] `PlantRankName` permitting `PlantFamilyName`, `PlantGenusName`, `PlantSpeciesName`, with
      `value()`, `rank()` and `of(String, LinealRank)`. `PlantRankNameTest` covers each
      permit, the factory, rejection of uncatalogued ranks, and class-qualified equality.
- [x] No `PlantOrderName` permit — no `PlantOrder` entity existed. **This was debt, not
      a decision; M2g built the entity and added the fourth permit.**
- [x] **`CultivarName` is deliberately not a permit.** A cultivated variety is a selection
      within a species, not a rung below it. Admitting it would have forced `rank()` to
      return null for one permit and — because a sealed type's permits must share a package
      in the unnamed module — dragged `CultivarName` out of its sub-package. Both costs were
      the type reporting that the concept does not belong. Cultivar is an orthogonal axis;
      see the blueprint §D.
- [x] **Garden migrated in the same effort.** `Planting.plantName` widened from a species
      name to a `PlantRankName`, so a planting can finally be recorded at genus —
      "a tray of unlabelled salvia starts" was previously unrepresentable. `cultivarName`
      stays a separate component: two axes, two fields. `planting.json` gained a
      `plantRank` discriminator per row.
- [x] Jackson dispatch declared at the consuming field, not on the interface.

**Naming decision — ✅ SHIPPED 2026-08-15.** `PlantName` was the species-rank permit
but does not say so — insects has no bare `InsectName`, because every insect name states
its rank. The bare name asserts a primacy that does not exist: a plant is named on three
axes (rank, cultivar, crop type), and `PlantName` is one rung of one of them. Renaming
covers **both** the name and the entity:

```
Plant                     → PlantSpecies
PlantName                 → PlantSpeciesName
PlantCollection           → PlantSpeciesCollection
PlantTestEntitySource     → PlantSpeciesTestEntitySource
plants/plants.json        → plants/plant-species.json
```

~75 files in the union, a pure rename with no behaviour change. It also **dissolves M4's
degeneracy**: with the entity named `PlantSpecies`, ADR-020's subject is "Species", giving
`PlantQuery.SpeciesQuery` exactly as insects has — no documented exception needed.
Out of scope for the rename: component names (`Cultivar.plantName` stays, still
species-bound and still accurate), JSON field names, and the ADR-020 nested-type renames
(M4's job).

### M2a′ — Rank audit of the existing catalog  ✅ DONE 2026-08-16

**Result: 17 rows keep species rank, 5 demote to genus.** Every genus needed already
exists in `plant-genera.json`, so no authoring was required. The audit read each row's
description and common names and asked whether the record names a taxon distinguishable in
the field from its congeners.

| Row | Evidence | Verdict |
|---|---|---|
| `creeping-thyme` | `species: null`; "a low, spreading herb" — never commits to *serpyllum* or *praecox* | → genus `thymus` |
| `ornamental-passiflora` | `species: null`; "striking blue-and-white flowers" hints at *caerulea* but is unstated | → genus `passiflora` |
| `dianthus` | `species: null`; no species evidence anywhere in the record | → genus `dianthus` |
| `sage` | `species: null`; "an aromatic perennial shrub in the mint family" — generic | → genus `salvia` |
| `citrus` | `species: null`; "the citrus trees … include **several varieties**" — plural by its own admission | → genus `citrus` |

The remaining 17 carry a species epithet and a description that evidences it. Three warrant
a note without changing rank:

- **`viola-odorata`** — common name recorded as "Wild violet", not "Sweet violet". California
  fritillary hosts include several native *Viola*; a naturalised *V. odorata* in a shaded
  garden bed is the likeliest reading and the description fits it. **Kept as species,
  flagged** — if the plant was never keyed out, this is a sixth demotion.
- **`pelargonium-graveolens`** — scented pelargoniums in cultivation are frequently hybrids.
  The rose-mint description matches *P. graveolens* specifically. **Kept as species.**
- **`prunus-persica`** — the description is about "the Oh Henry peach", which is a
  *cultivar*, not the species. Rank is correct; the record is carrying cultivar detail that
  belongs in a `Cultivar`. Not a rank change — a data-modelling note for later.

**Downstream references to demoted rows — only two, and both are the reason the widening
in M2e exists:**

| Reference | Points at | Needs |
|---|---|---|
| `citrus-bloom-pesticide-window` (program) | `citrus` | `PlantProgram.plantName` → `PlantRankName` |
| `creeping-thyme-thymol` (constituent) | `creeping-thyme` | `PhytochemicalConstituent.plantName` → `PlantRankName` |

No cultivar, no planting and no seed lineage references a demoted row. `TestPlantsIdentifiers`
holds `CreepingThyme.name` as a `PlantSpeciesName`; it moves under `PlantGenera` as a
`PlantGenusName`.

<details><summary>Original milestone description</summary>

**Do this before any record is reshaped.** M2b adds a `genusName` foreign key and requires
every plant to resolve to a genus; a row whose slug claims a rank its evidence does not
support would have that wrong rank cemented behind an FK and a data migration.

The catalog was authored before the rank layer existed, so **a binomial slug is a claim
about identification confidence, not a fact**. This is the blueprint's B1 discipline —
catalogue at the most specific rank the evidence supports — applied retrospectively. The
five known duplicates are the obvious cases, not necessarily the only ones.

- [ ] For each of the 22 rows in `plants.json`, record: the current slug, what the entry's
      description and common names actually evidence, the rank that evidence supports, and
      the resulting action (keep as species / demote to genus / demote to family / merge
      with an existing rank record).
- [ ] Output is a **reclassification table reviewed before code moves**. Data authoring and
      rank judgment are the deliverable here; no Java changes.
- [ ] Carry the same test through `cultivars.json`, `plant-programs.json` and
      `phytochemical-constituents.json` — each references a plant, and a demoted plant
      re-points every reference to it.
- [ ] Known from prior work: `creeping-thyme`, `ornamental-passiflora`, `dianthus`, `sage`
      and `citrus` carry `"species": null` and duplicate an existing `PlantGenus` record.
      Treat these as confirmed demotions, not as the full answer.

**Rule of thumb for the audit:** a slug is honest at species rank only if the record names
a species that could be distinguished in the field from its congeners. "Some salvia in the
front bed" is a genus record whatever its slug says.

</details>

### M2b — Reshape `Plant` to species rank

**Depends on M2a′.** Reshape only what the audit has confirmed is species rank.

- [ ] Replace `TaxonomicClassification taxonomy` with `PlantGenusName genusName` +
      `TaxonomicSpecies epithet`; both validated (`.entityName`, `.namedValue`).
- [ ] Apply the audit's reclassification: demoted rows leave `plants.json`, their
      references re-point at the surviving rank record, and their `PlantGenus`/`PlantFamily`
      record becomes the sole home for that taxon.
- [ ] `ForeignKeyConstraint` on `PlantTestEntitySource` → `PlantGenusTestEntitySource`.
- [ ] Decide whether `Plant implements LinnaeanSpecies<PlantGenusName>`, mirroring
      `PlantGenus implements LinnaeanGenus<PlantFamilyName>`.
- [ ] Re-anchor or delete `Plant.genus()` / `Plant.species()` and the stale PR-2f javadoc.

**Ripple:** this changes `Plant`'s arity *and* drops a component other code reads.
`PlantsCatalogContribution.tokensFor(Plant)` builds the binomial and abbreviated-binomial
tokens from `taxonomy.genus()` / `taxonomy.species()` — it must now resolve the genus
through the FK or take the genus epithet as a parameter. Also breaks
`PlantEntityRepositoryTest` (three constructor sites), `PlantTest`, all `plants.json`
rows, and `plants/detail.jte` / `list.jte`. Grep for `new Plant(` and `taxonomy()`.

### M2c — `PlantEcologicalRole` cross-rank entity  ✅ DONE 2026-08-16

- [ ] `PlantEcologicalRoleId` (UUIDv7) in `domains/identifiers`; entity in `plants-api`
      modelled on `InsectFunctionalRole` — `notEmpty(roles)`, uniqueness on `parentName`.
- [ ] Remove `roles` from `Plant`. Migrate `isKeystoneHost()`,
      `supportsBiocontrolInsects()`, and `isNitrogenFixer()` onto the new entity.
- [ ] **Consumer surface is smaller than the domain doc implies.** `domains/plants/CLAUDE.md`
      describes these predicates as driving "zero-pesticide constraints in the
      PestManagement application module" — **no such module exists**; the language is
      aspirational and should be softened when this milestone lands. Verified consumers
      are six call sites in two templates (`plants/detail.jte:62,65,68`,
      `plants/list.jte:17,20,23`) plus `PlantTest`. `PlantRole` has zero references
      outside `domains/plants`. The migration is therefore contained: the controller
      loads the role record alongside the plant and passes it to the template.
- [ ] Repository + mock (with argument validation) + contract test + `getByParentName`.
- [ ] New `plants/plant-ecological-roles.json` seeded from the `roles` arrays currently
      in `plants.json`, including the five moved organisms — no ecological data is lost
      in the move, which was the whole objection to a naive delete.

### M2d — Genus backfill  ✅ SHIPPED 2026-08-15

- [x] Author a `PlantGenus` record per genus referenced by a surviving `Plant`.
      **14 distinct genera were missing, not 16** — the earlier figure counted plant
      *rows* with an uncatalogued genus, and `Trifolium` and `Solanum` each carry two
      plants. Catalog goes 5 → 19 records; all 14 parent families already existed, so no
      family authoring was needed.
- [x] `PlantGenusCatalogDataTest` — encodes M2d's acceptance criterion as a test rather
      than a one-time achievement: every genus a `Plant` references has a record, every
      genus resolves to a catalogued family, each genus's locally-carried `family`
      epithet agrees with its parent record, and slugs are valid lower-kebab-case
      matching the lowercased epithet. Without the first assertion, adding a plant with
      an uncatalogued genus silently re-opens the gap and M2b's backfill has to be redone.

Descriptions are written at **genus** level, not species level — under Option A these
records are the permanent home for genus-only identifications, so `Trifolium` covers both
clovers and `Solanum` covers tomato and eggplant. The five pre-existing genus records
(`thymus`, `passiflora`, `dianthus`, `salvia`, `citrus`) still read as species
descriptions, a legacy of having been created as stand-ins for the duplicated plant rows.
Worth rewriting when M2c touches them to add `lifeForm`.

### M2e — Re-key the genus-level constituent  ✅ DONE 2026-08-16

**Widens `PlantsCompoundReferences` too.** That provider is species-specific throughout
today — `Set<PlantSpeciesName> seenPlants` is its dedup key, and its javadoc tells consumers
to filter by `instanceof PlantSpeciesName`. Once the constituent's plant reference is a
`PlantRankName`, "which plants produce thymol" answers *Thymus* at genus rank, and the
dedup key and the emitted ref widen with it.

**Expect a cast at the kernel boundary.** `EntityRef(DomainId, EntityName)` takes an
`EntityName`, and `PlantRankName` is a sibling interface rather than a subclass — an
interface cannot extend the abstract `EntityName` class. Insects hit this first and casts:
`new EntityRef(INSECTS_DOMAIN, (EntityName) rankName)` in `InsectIdentificationCommand`,
plus two more at `ExternalAuthority.lookup`. Follow the same pattern; do not invent a
different workaround. **This is a genuine kernel wart** — every rank-name consumer at a
kernel API needs the cast — and it is exactly the kind of thing the side-by-side
abstraction pass (rule 3 in `domains/plants/CLAUDE.md`) should decide how to fix.


- [ ] Change `PhytochemicalConstituent.plantName` to a `PlantRankName` (component rename
      to `parentName` for honesty), with the field-level Jackson dispatch from M2a.
- [ ] Re-key `creeping-thyme-thymol` to the `thymus` genus.
- [ ] `PhytochemicalConstituentQuery.forPlantName` becomes `forParentName`;
      `PlantsCompoundReferences` emits the parent ref at whatever rank it resolves.

### M2g — `PlantOrder`, closing the top of the chain  ✅ DONE 2026-08-16

**The chain is unfinished at the top, not by decision.** M2b gave the species rung a typed
parent and the UBL now asserts that "position is never carried as loose epithet strings; a
taxon's parent is a reference, not a description." `PlantFamily.order` is exactly such a
string — a `TaxonomicOrder` with no entity behind it and no constraint on it. `PlantGenus`
is worse: it carries `order` *two levels up* from itself, a redundancy its javadoc only
justifies for `family`.

Insects has had this right throughout: `InsectFamily(name, orderName: InsectOrderName, …)
implements LinnaeanFamily<InsectOrderName>`, referencing a real `InsectOrder`.

**Do it now, while the epithets still agree.** 13 distinct orders across the 14 families,
and every genus's `order` currently matches its family's. That consistency is what makes
the replacement mechanical; it will not survive the next hand-authored record.

- [x] `PlantOrderName` in `domains/identifiers/.../plants/`, and a fourth `PlantRankName`
      permit returning `LinealRank.ORDER`. Update `PlantRankName.of` and its test.
- [x] `PlantOrder` in `plants-api` — `NamedEntity<PlantOrderName>`, `LinnaeanOrder`,
      carrying its `TaxonomicOrder` epithet, `Description` and `commonNames`, mirroring
      `InsectOrder`.
- [x] `PlantFamily` swaps `TaxonomicOrder order` for `PlantOrderName orderName` and
      implements `LinnaeanFamily<PlantOrderName>`.
- [x] `PlantGenus` drops `order` entirely — two levels up is not a chain check, it is a
      copy. `family` stays for the one-level check its javadoc describes.
- [x] Author 13 `PlantOrder` records with real Linnaean data and four-level descriptions,
      as M2d did for genera.
- [x] Repository, query, mock, contract cases, console route and linker case, matching
      what M3 built for families and genera.

**Then every rank source carries a foreign key.** This is the payoff worth naming: once
`PlantOrder` exists, `PlantFamilyTestEntitySource` gains the constraint it has never been
able to declare, and the whole ladder is enforced at load time —

| Source | FK target | Status |
|---|---|---|
| `PlantOrderTestEntitySource` | — top of the chain | n/a |
| `PlantFamilyTestEntitySource` | `PlantOrderTestEntitySource` | ✅ (M2g) |
| `PlantGenusTestEntitySource` | `PlantFamilyTestEntitySource` | ✅ |
| `PlantSpeciesTestEntitySource` | `PlantGenusTestEntitySource` | ✅ (M2b) |

A parent that does not exist then fails at fixture load with a named constraint, rather
than surviving as a string nobody checks. `PlantEcologicalRoleTestEntitySource` stays
without one — its `plantName` is a `PlantRankName` spanning three sources, and the
framework's `ForeignKeyConstraint` resolves a single source class; its catalog-data test
covers that integrity instead.

**Expect the fixtures to be wrong, and fix the fixtures.** The JSON was authored against a
model that had no order rank; it will not satisfy these constraints as written. That is the
expected direction of work — the model is made correct and the data is reshaped to fit it,
never the reverse.

**What landed.** 13 `PlantOrder` records; `plant-families.json` re-keyed from order epithet
to order slug; `order` dropped from all 19 genus records. The data needed no other reshaping
— every family resolved to one of the 13 orders on the first pass, and no order came out
without a family. Two data tests now hold that line: `PlantFamilyCatalogDataTest` asserts
both directions of the order↔family relation and the order slug/epithet convention, and the
FK constraint refuses a bad fixture at load.

`getByOrderName` on the family repository is exercised through `malpighiales` — the only
order with two catalogued families (Passifloraceae, Violaceae), so the test distinguishes a
real join from a single-entity lookup.

### M2f — Hierarchy queries

- [ ] `forGenusName` / `forFamilyName` on the plant query, `forFamilyName` on the genus
      query, matching `InsectQuery.SpeciesQuery`. The family-level variant composes
      through the genus query, as `SpeciesQueryImpl` does.
- [ ] Repository methods + mock validation + three contract cases each.

---

## M3 — Make family and genus reachable  ✅ SHIPPED 2026-08-15

**Depends on:** M1. Independent of M2.

`PlantsCatalogContribution` pages the whole family and genus catalog and emits search
tokens for both. `PlantsLinker` has no case for `PlantFamilyName` or `PlantGenusName`, and
`SearchController.buildGroups` skips any hit whose linker returns null. Every family and
genus hit is silently dropped — searching "Lamiaceae" returns nothing. There are also no
console routes to link *to*.

Do the routes first: per `EntityRefLinker`'s contract, a linker "should not synthesize
URLs whose controller routes do not exist."

- [x] `GET /plants/families/{name}` + `families/detail.jte` — family record, its genera,
      Durrell description.
- [x] `GET /plants/genera/{name}` + `genera/detail.jte` — genus record, upward link to
      its family. The member-plants section renders a placeholder: listing them needs
      `Plant.genusName`, and reaching for `taxonomy.genus` would only have to be undone
      by M2b.
- [x] `GET /plants/families` + `families/list.jte` — browse entry point, linked from the
      plant catalog header. No genera index: genera are reached from their family's
      cards, as insects does.
- [x] Both cases added to `PlantsLinker`.
- [x] `PlantsLinkerTest` — covers all seven owned name types plus the
      not-mine-returns-null contract.
- [x] Template tests: `PlantsFamilyListTemplateTest`, `PlantsFamilyDetailTemplateTest`
      (populated *and* empty genera branches), `PlantsGenusDetailTemplateTest`.
- [x] Nav: no change needed. The console nav has a single `/plants` entry and the rank
      pages hang off the catalog, so the Pico `nav ul{display:flex}` / `details` leakage
      never comes into play.

**Pulled forward from M2f:** `PlantGenusEntityQuery.forFamilyName` plus its repository
method, mock validation, and three contract cases. `PlantGenus.familyName` already
exists, so the family → genera rollup needed no data migration — and without it the
family page would have been an empty shell. The plant-side hierarchy queries stay in M2f
because they depend on `Plant.genusName`, which does not exist yet.

**Deferred to M2c:** the rank pages do not render `lifeForm` or ecological roles.
`PlantGenus` has no `lifeForm` component until M2c adds one, and `PlantEcologicalRole`
does not exist yet. Both pages get a section then.

---

## M4 — ADR-020 namespace naming

**Depends on:** nothing. Mechanical rename, safe to do first or last.

The convention drops the domain prefix and adds no infix: `InsectSpecies` → `SpeciesQuery`,
`FamilyRepository`. Plants carries both.

| Today                                          | ADR-020                          |
|------------------------------------------------|----------------------------------|
| `PlantQuery.PlantEntityQuery`                   | `PlantQuery.PlantQuery`¹         |
| `PlantQuery.PlantFamilyEntityQuery`             | `PlantQuery.FamilyQuery`         |
| `PlantQuery.PlantGenusEntityQuery`              | `PlantQuery.GenusQuery`          |
| `PlantRepository.PlantFamilyEntityRepository`   | `PlantRepository.FamilyRepository` |
| `PlantEntityQueryImpl`                          | `PlantsQueryImpl`¹               |
| `PlantFamilyEntityQueryImpl`                    | `FamilyQueryImpl`                |

¹ `Plant` is both the domain noun and an entity subject, so the collapse is degenerate —
`PlantQuery.PlantQuery` does not compile. Insects never hit this because no entity is
named `Insect`. Pick a convention and record it in `domains/CLAUDE.md`, since the next
domain with a same-named root entity will hit it too. Suggested: keep the plural accessor
`plants()` and name the nested type `PlantEntityQuery` as an explicit, documented
exception rather than an accident.

- [ ] Rename nested query/repository types and their `*Impl` adapters.
- [ ] Update `plants-test-context`, `plants-core` tests, `PlantsCatalogContribution`,
      `PlantsCompoundReferences`, `PlantsController`.
- [ ] Record the root-entity exception in `domains/CLAUDE.md` §API Surface.

---

## M5 — N=1 collapse in the four single-entity sub-contexts

**Depends on:** nothing. Do with M4 — same files, same reviewers.

`domains/CLAUDE.md`: "When a package contains exactly one entity, skip the namespace:
declare a top-level package-private `<Entity>Repository` interface and a top-level public
`<Entity>Query` interface." `cultivar`, `heritage`, `management`, and `phytochemistry` each
hold exactly one entity and each wraps it anyway — eight namespace types carrying one
member apiece, plus four `*EntityCollections` interfaces holding one class each.

- [ ] `CultivarQuery` becomes `interface CultivarQuery extends EntityQuery<...>` with
      `forPlantName` inline; drop the `cultivars()` accessor and `CultivarEntityQuery`.
- [ ] Same for `SeedLineageQuery`, `PlantProgramQuery`, `PhytochemicalConstituentQuery`.
- [ ] Same for the four repository namespace classes.
- [ ] Collapse `CultivarEntityCollections` → top-level `CultivarCollection`; ×4.
- [ ] Delete the now-redundant `*QueryImpl` pass-through adapters (`CultivarQueryImpl`
      only delegates to `CultivarEntityQueryImpl`) and fold the four `*TestContext`
      classes into `PlantsTestContext`.
- [ ] Update call sites: `cultivarQuery.cultivars().forPlantName(x)` →
      `cultivarQuery.forPlantName(x)` in `PlantsController` and the core tests.

**Note:** this reverses a claim in the old `domains/plants/CLAUDE.md` ("N=1 per
sub-context, so the repository namespace collapses to a single nested interface") which
read the rule backwards. That line is already corrected on the `plants-consistency` branch.

---

## Deliberately out of scope

Forward-looking gaps, listed so they are not mistaken for oversights:

- **No `habitat` / `clades` dependency.** `Plant` carries no `HabitatProfile` and no
  `placedIn` clade. `kernels/CLAUDE.md` documents both as pending plants activation;
  clades additionally needs a plants-owned trait function.
- **No write side.** No `PlantCommand`, no `Transaction`, no `with*` methods. Fine while
  the console is read-only — but `domains/CLAUDE.md` requires a `with*` per mutable field,
  so this becomes a gap the moment writes land.
- **No rank-polymorphic read model.** The insects equivalents (`InsectTaxonView`,
  `Insect`) have no plants counterpart. M2g closed the order-rank half of this gap,
  taking `PlantRankName` to four permits; the read model can follow once M2 lands.
  Neither blocks anything here.
- **`TestPlantsIdentifiers` uses plural scope names** (`PlantFamilies`, `PlantGenera`)
  where insects uses the singular entity name (`InsectFamily`, `InsectGenus`). Cosmetic;
  fold into M4 if that milestone is already touching the file.

---

## Suggested ordering

```
M1  ✅ decided (Option A)
M2a ✅ PlantRankName + garden migration     ← shipped 2026-08-15
M2d ✅ genus backfill (14 records)          ← shipped 2026-08-15
M3  ✅ routes + linker                      ← shipped 2026-08-15
 │
 ├─→ RENAME ✅ Plant → PlantSpecies          ← shipped 2026-08-15
 │
 └─→ M2a′  rank audit of the catalog        ← DATA JUDGMENT, blocks M2b
       └─→ M2b  reshape to species rank     ← largest remaining change
             ├─→ M2c  PlantEcologicalRole
             ├─→ M2e  re-key the constituent
             └─→ M2f  hierarchy queries

M4 + M5  namespace cleanup                  ← independent; M4 got simpler after the rename
```

**Next is M2a′.** The rename landed first, so every milestone after it writes
`PlantSpecies` from the start rather than being corrected later.

**M2a′** is the one piece of this plan that is not a coding task. It is a judgment about
plants, made by reading the catalog, and its output is a reviewed table rather than a diff.
It blocks M2b absolutely: reshaping before re-assessing would cement a wrong rank behind a
foreign key.

After those, M2b is the largest remaining change — it drops a component other code reads.
M2c is smaller than it looks: the role predicates have six template call sites and no
cross-domain consumers.
