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

## M1 — Decide what `Plant` means  🚧 BLOCKED ON A DECISION

Everything else depends on this. Do not start M2 or M3 before it is settled.

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

## M2 — Typed upward FK on `Plant`

**Depends on:** M1 (Option A or C; impossible under B).

Today `PlantGenus.familyName` is typed and FK-constrained, and the chain stops there.
`Plant`'s position is held only as strings inside `TaxonomicClassification`, so nothing
enforces that a plant's genus exists, and no query can walk the hierarchy.
`Plant.genus()`'s javadoc already flags this as pending "PR-2f of FU-1".

- [ ] Add `PlantGenusName genusName` to `Plant`; validate with `.entityName(...)`.
- [ ] Add the `ForeignKeyConstraint` to `PlantTestEntitySource`.
- [ ] Backfill `genusName` in `plants.json` — requires a `PlantGenus` record per genus
      currently referenced. Today 5 of 21 genera exist; the other 16 must be authored
      (real Linnaean data, four-level `Description` each). **This is the bulk of M2.**
- [ ] Decide whether `Plant` implements `LinnaeanSpecies<PlantGenusName>`, mirroring
      `PlantGenus implements LinnaeanGenus<PlantFamilyName>`.
- [ ] Add `forGenusName` / `forFamilyName` to `PlantQuery.plants()` and
      `forFamilyName` to `genera()`, matching `InsectQuery.SpeciesQuery`. The
      family-level variant composes through the genus query, as `SpeciesQueryImpl` does.
- [ ] Repository methods + mock validation + three contract cases each.
- [ ] Re-anchor `Plant.genus()` / `Plant.species()` and delete the stale PR-2f javadoc.

**Ripple warning:** adding a component to `Plant` breaks `PlantEntityRepositoryTest`
(three constructor sites), `PlantTest`, all 22 `plants.json` rows, `PlantCatalogContribution`,
and `plants/detail.jte` / `list.jte`. Grep for `new Plant(`.

---

## M3 — Make family and genus reachable

**Depends on:** M1. Independent of M2.

`PlantCatalogContribution` pages the whole family and genus catalog and emits search
tokens for both. `PlantsLinker` has no case for `PlantFamilyName` or `PlantGenusName`, and
`SearchController.buildGroups` skips any hit whose linker returns null. Every family and
genus hit is silently dropped — searching "Lamiaceae" returns nothing. There are also no
console routes to link *to*.

Do the routes first: per `EntityRefLinker`'s contract, a linker "should not synthesize
URLs whose controller routes do not exist."

- [ ] `GET /plants/families/{name}` + `families/detail.jte` — family record, its genera,
      Durrell description. Model on `insects/family.jte`.
- [ ] `GET /plants/genera/{name}` + `genera/detail.jte` — genus record, parent family,
      its plants (needs M2's `forGenusName`, or filter on `taxonomy.genus` until then).
- [ ] Index pages if the insects `families.jte` / `genera.jte` pattern is wanted.
- [ ] Add both cases to `PlantsLinker`.
- [ ] `PlantsLinkerTest`, modelled on `InsectsLinkerTest` — this is the test that would
      have caught the gap.
- [ ] Template tests, matching the existing `PlantsDetailTemplateTest` pattern.
- [ ] Check `plants/nav.jte` (or the console nav tab) surfaces the new pages. Watch the
      Pico `nav ul{display:flex}` / `details` leakage — declare display and margin
      explicitly on every new nav list, li, link, and summary.

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
  (`InsectOrder`, `InsectTaxonView`, `Insect`) have no plants counterpart. M1 Option A
  makes `PlantRankName` a prerequisite; the read model can follow later.
- **`TestPlantsIdentifiers` uses plural scope names** (`PlantFamilies`, `PlantGenera`)
  where insects uses the singular entity name (`InsectFamily`, `InsectGenus`). Cosmetic;
  fold into M4 if that milestone is already touching the file.

---

## Suggested ordering

```
M1 (decision)
 ├─→ M2 (typed FK + genus backfill)   ← largest, data-heavy
 └─→ M3 (routes + linker)             ← highest user-visible payoff

M4 + M5 (namespace cleanup)           ← independent, do whenever
```

M3 delivers the most visible fix for the least work once M1 is settled, and M4/M5 can
slot into any gap. M2 is the long pole because of the 16 missing genus records.
