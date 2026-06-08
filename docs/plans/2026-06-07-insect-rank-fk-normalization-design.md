# Insect rank-FK normalization — design

**Date:** 2026-06-07
**Status:** Approved design; implementation plan to follow.
**Source critique:** [`docs/notes/2026-06-03-insect-aggregate-bounded-context-review.md`](../notes/2026-06-03-insect-aggregate-bounded-context-review.md)
(items R8, R1, R6).

## Problem

Each insect rank `NamedEntity` denormalizes its **full** ancestor chain rather than
just its immediate parent:

- `InsectGenus` carries `familyName` (parent) **and** `orderName` (grandparent).
- `InsectSpecies` carries `genusName` (parent) **and** `familyName` (grandparent).

This creates a management/drift concern: if a species is later re-identified to a
different genus, its `familyName` must be hand-corrected in lockstep, and the two
FKs can silently disagree. The denormalization is the root cause of the skip-level
FK invariants in the `Insect` aggregate (`speciesBelongsToFamily`,
`genusBelongsToOrder`) — they exist only to police drift that normalization removes
entirely.

`InsectFamily` (`orderName`, parent) and `InsectOrder` (root, no parent) are already
parent-only and are **not** changed.

## Principle

Every rank entity references **only its immediate parent** by typed `EntityName`.
Re-parenting is a single-field update. With exactly one path up the tree, the
drift class — and the invariants that defended against it — disappears.

## Scope

In this effort:

- **R8** — remove the two grandparent FKs (`InsectGenus.orderName`,
  `InsectSpecies.familyName`); reimplement the two skip-level queries as core
  fan-outs; remove the now-impossible skip-level aggregate invariants.
- **R1** — collapse the redundant ancestor re-descents in `Insect.invariants()`
  (folded into the same `invariants()` rewrite R8 already performs).
- **R6** — add a typed `parentName : InsectRankName` to `LifeStage` (extends the
  parent-only-typed-reference theme; independent slice).

Explicitly **parked** (separate follow-ups): R2 (null-tolerant `belongsTo*`
cleanup), R3 (`aggregateOrNull` deletion), R4 (`Insect` as read model / drop
`with*` mutators), R5 (observations split), R7 (rank-aggregate fate).

## Changes

### 1. Entity records (core)

| Entity | Today | After |
|---|---|---|
| `InsectOrder` | root, no parent | unchanged |
| `InsectFamily` | `orderName` (parent) | unchanged |
| `InsectGenus` | `familyName` + **`orderName`** | `familyName` only |
| `InsectSpecies` | `genusName` + **`familyName`** | `genusName` only |

Removes, as direct fallout:

- `InsectGenus.belongsToOrder(InsectOrderName)` and its `entityName(orderName, …)`
  invariant line.
- `InsectSpecies.belongsToFamily(InsectFamilyName)` and its `entityName(familyName, …)`
  invariant line.

Kept: the parent-level `belongsTo*` (`species→genus`, `genus→family`,
`family→order`). `LinnaeanGenus<InsectFamilyName>` exposes only `familyName()`
(parent) and `LinnaeanFamily<InsectOrderName>` only `orderName()` (family's parent);
neither interface breaks.

### 2. Skip-level queries → core fan-outs

The two skip-level queries keep their **public signatures** but move the join from
a repository index into the core query adapter:

- `SpeciesQueryImpl.forFamilyName(family)` → `genera().forFamilyName(family)`, then
  union `species().forGenusName(g)` over each genus.
- `GenusQueryImpl.forOrderName(order)` → `families().forOrderName(order)`, then
  union `genera().forFamilyName(f)` over each family.

The parent-level queries these delegate to (`species().forGenusName`,
`genera().forFamilyName`, `families().forOrderName`) already exist.

Repository changes:

- Remove `SpeciesRepository.getByFamilyName` and `GenusRepository.getByOrderName`
  (the FK index they backed is gone) and their mock implementations.
- Move their three-case behavioral contract tests **up** from `SpeciesRepositoryTest`
  / `GenusRepositoryTest` to the core `SpeciesQueryImplTest` / `GenusQueryImplTest`,
  where the behavior now lives.

### 3. `Insect.invariants()` rewrite (R8 fallout + R1, one edit)

- **R8:** delete the skip-level FK checks `speciesBelongsToFamily` and
  `genusBelongsToOrder`. The three parent-level checks (`familyBelongsToOrder`,
  `genusBelongsToFamily`, `speciesBelongsToGenus`) stay.
- **R1:** descend into each present rank **once**; drop the duplicate ancestor
  re-descents (`family:order`, `genus:family`, `genus:order`, `species:genus`,
  `species:family`, `species:order`) that today emit duplicate violations for one
  underlying problem.
- Remove aggregate-level delegates `InsectSpeciesAggregate.belongsToFamily` /
  `familyName()` and `InsectGenusAggregate.belongsToOrder` / `orderName()`.
  `Insect.familyName()` / `orderName()` are unaffected — they read the rank-aggregate
  *slots*, not the denormalized FK.

### 4. Console display hops

Three sites in `InsectsController` (~220, ~417, ~466) that read
`species.familyName()` directly resolve the family via the genus instead:
`genera().getByName(s.genusName()).orElseThrow().familyName()`. No `genus.orderName`
console sites exist (order lookups already route through `family.orderName()`).

### 5. R6 — typed parent FK on `LifeStage`

Add `InsectRankName parentName` to the `LifeStage` sealed interface and its four
permits (`EggStage`, `LarvaStage`, `PupaStage`, `AdultStage`), using the **exact**
`@JsonTypeInfo(use = Id.NAME, property = "parentRank", include = As.EXTERNAL_PROPERTY)`
/ `@JsonSubTypes` dispatch already proven on `InsectImage.parentName`. Update the
`invariants()` of each permit to validate the new field, plus `LifeStageRepository`,
its mock, the behavioral contract, and `TestInsectsIdentifiers`.

This *enables* the deferred clade-conformance invariant (the aggregate can now
type-safely confirm a life stage belongs to one of its ranks) but does **not**
implement it — that stays phase-3 deferred.

## Data migration

- `insect-species.json` — remove `familyName` from every entry.
- `insect-genera.json` — remove `orderName` from every entry.
- `life-stages.json` — add `parentRank` + `parentName` to every entry, derived from
  the existing composite slug (e.g. `"tachinidae-egg"` →
  `"parentRank": "FAMILY"`, `"parentName": "tachinidae"`).

## Slicing (one concern per PR — ADR-019)

1. **PR-A — R8 + R1**: entities, JSON catalogs, repository/query fan-out, console
   hops, `Insect.invariants()` rewrite, and the affected tests
   (`InsectTest`, `TestInsectsIdentifiers`, repository/query contract tests).
   Atomic for compilation; single concern (normalization). If it exceeds the
   ~400-line reviewability target, split at plan time along the
   entity+JSON+repository / console+aggregate seam.
2. **PR-B — R6**: `LifeStage` typed parent FK. Fully independent of PR-A; may land
   before or after.

## Non-goals

The parked items above. In particular this effort does **not** reframe `Insect` as
a read model, split observations out of the taxon view, or decide the fate of the
rank aggregates — those remain open in the bounded-context review.
