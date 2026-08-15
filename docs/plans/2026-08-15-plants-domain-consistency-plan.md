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
identifier spaces. `PlantCatalogContribution` emits the token `"Dianthus"` for both refs.

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

### M2a — `PlantRankName` sealed type

- [ ] `domains/identifiers/.../plants/PlantRankName.java`, modelled on `InsectRankName`:
      `sealed interface PlantRankName permits PlantFamilyName, PlantGenusName, PlantName`,
      exposing `value()` and `rank()` (`LinealRank`), plus `of(String, LinealRank)`.
- [ ] No `PlantOrderName` permit — there is no `PlantOrder` entity, and `InsectRankName`
      only permits ranks that have one. Add it if and when an order entity lands.
- [ ] **Naming wart to resolve here:** `PlantName` is the species-rank permit but is not
      called `PlantSpeciesName`. Either rename it (wide but mechanical — it is referenced
      across `plants-api`, `plants-core`, `garden`, and the JSON catalogs) or keep it and
      document the exception on the sealed interface. Decide before M2b so consumers are
      written once.
- [ ] Jackson dispatch is declared **at the consuming field**, not on the interface —
      copy the `@JsonTypeInfo(EXTERNAL_PROPERTY)` + `@JsonSubTypes` block from
      `InsectFunctionalRole.parentName`. Getting this wrong silently emits
      `{"valid":…,"notValid":…}` envelopes on write.

### M2b — Reshape `Plant` to species rank

- [ ] Replace `TaxonomicClassification taxonomy` with `PlantGenusName genusName` +
      `TaxonomicSpecies epithet`; both validated (`.entityName`, `.namedValue`).
- [ ] Delete the five genus-level rows from `plants.json`; their `PlantGenus` records are
      now the sole home for those taxa.
- [ ] `ForeignKeyConstraint` on `PlantTestEntitySource` → `PlantGenusTestEntitySource`.
- [ ] Decide whether `Plant implements LinnaeanSpecies<PlantGenusName>`, mirroring
      `PlantGenus implements LinnaeanGenus<PlantFamilyName>`.
- [ ] Re-anchor or delete `Plant.genus()` / `Plant.species()` and the stale PR-2f javadoc.

**Ripple:** this changes `Plant`'s arity *and* drops a component other code reads.
`PlantCatalogContribution.tokensFor(Plant)` builds the binomial and abbreviated-binomial
tokens from `taxonomy.genus()` / `taxonomy.species()` — it must now resolve the genus
through the FK or take the genus epithet as a parameter. Also breaks
`PlantEntityRepositoryTest` (three constructor sites), `PlantTest`, all `plants.json`
rows, and `plants/detail.jte` / `list.jte`. Grep for `new Plant(` and `taxonomy()`.

### M2c — `PlantEcologicalRole` cross-rank entity

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

### M2d — Genus backfill

- [ ] Author a `PlantGenus` record per genus referenced by a surviving `Plant`. **16 of
      21 are missing** — real Linnaean data plus a four-level `Description` each. This is
      the bulk of the calendar time in M2 and is data authoring, not code.

### M2e — Re-key the genus-level constituent

- [ ] Change `PhytochemicalConstituent.plantName` to a `PlantRankName` (component rename
      to `parentName` for honesty), with the field-level Jackson dispatch from M2a.
- [ ] Re-key `creeping-thyme-thymol` to the `thymus` genus.
- [ ] `PhytochemicalConstituentQuery.forPlantName` becomes `forParentName`;
      `PlantCompoundReferences` emits the parent ref at whatever rank it resolves.

### M2f — Hierarchy queries

- [ ] `forGenusName` / `forFamilyName` on the plant query, `forFamilyName` on the genus
      query, matching `InsectQuery.SpeciesQuery`. The family-level variant composes
      through the genus query, as `SpeciesQueryImpl` does.
- [ ] Repository methods + mock validation + three contract cases each.

---

## M3 — Make family and genus reachable  ✅ SHIPPED 2026-08-15

**Depends on:** M1. Independent of M2.

`PlantCatalogContribution` pages the whole family and genus catalog and emits search
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
- [ ] Update `plants-test-context`, `plants-core` tests, `PlantCatalogContribution`,
      `PlantCompoundReferences`, `PlantsController`.
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
- **No `PlantOrder` entity and no rank-polymorphic read model.** The insects equivalents
  (`InsectTaxonView`, `Insect`) have no plants counterpart, and there is no order-rank
  entity — so `PlantRankName` (M2a) permits three names, not five. Both can follow once
  M2 lands; neither blocks anything here.
- **`TestPlantsIdentifiers` uses plural scope names** (`PlantFamilies`, `PlantGenera`)
  where insects uses the singular entity name (`InsectFamily`, `InsectGenus`). Cosmetic;
  fold into M4 if that milestone is already touching the file.

---

## Suggested ordering

```
M1 ✅ decided (Option A)
 │
 ├─→ M2a  PlantRankName                    ← unblocks M2b/M2c/M2e
 │    ├─→ M2b  Plant → species rank
 │    ├─→ M2c  PlantEcologicalRole         ← audit PestManagement first
 │    └─→ M2e  re-key the constituent
 │   M2d  genus backfill (16 records)      ← data authoring, parallelisable
 │   M2f  hierarchy queries                ← after M2b + M2d (genus side done in M3)
 │
 └─→ M3 ✅ routes + linker                 ← shipped 2026-08-15

M4 + M5  namespace cleanup                 ← independent, do whenever
```

**M3 is done.** Next best is **M2d** — authoring the 16 missing `PlantGenus` records is
pure data work, blocks M2b's backfill, and needs no code decisions. **M2a** can run
alongside it and is the gate for the rest of M2.

M2a is the gate for the rest of M2 and should land as its own PR, including the
`PlantName` vs `PlantSpeciesName` naming call. M2d is pure data authoring and can run in
parallel with anyone's code work.

M2b is the largest *code* change (it drops a component other code reads); M2d is the
largest *time* cost (16 genus records of real Linnaean data). M2c is smaller than it
looks — the role predicates have six template call sites and no cross-domain consumers.
