# N+1 Runtime Gate — Genuine Findings for Remediation

**Date:** 2026-08-23
**Status:** the AspectJ runtime N+1 gate is **armed** (wired into `NaturalistTestExtension`).
It deliberately reds the genuine N+1s below. Each is a **fixing-session task**: batch the
fan-out, and the flagged tests go green. Work them on worktrees off `main`.

## What the gate is

`SelectCountAspect` (load-time woven) counts repository selects per **outermost query
invocation**; `NaturalistTestExtension` arms it in `beforeEach` and, in `afterEach`, throws
`RepeatedSelectException` when one query invocation repeats a repository select — an N+1.
Gating is per-invocation, so a test that calls one query N times (each doing one select) is
**not** flagged; only a single invocation looping a select is.

This is the runtime complement to the static `NoSelectInIteration` OpenRewrite recipe
(`docs/plans/2026-08-23-n-plus-one-rewrite-gate-design.md`). **3 of the 4 findings below were
missed entirely by the static gate** — they are composed / cross-method read-model fan-outs
the lexical recipe cannot see.

## The findings (4 heads, 14 failing tests)

| # | N+1 head | Repeated select(s) | Fix (batch to) | Failing tests |
|---|----------|--------------------|----------------|---------------|
| 1 | `InsectCitationQueryImpl.findByRankName` (insects-core) | `CitationAssociationRepositoryMock.getBySubject` (×2–4, one per ancestor rank) | resolve all subjects in one call (`getBySubjects(Set)` / `findByNameSet`) | `InsectCitationQueryImplTest` (2) |
| 2 | `InsectQueryImpl.getByName` (insects-core) | `Insect{Family,Genus,Species}RepositoryMock.getByName` (×3 — one per ancestor rank, via `InsectFactory`) | batch the ancestor lookups (`getByEntityNameSet` per rank) in `InsectFactory` | `InsectQueryImplTest` (1), `InsectFactoryTest` (6) |
| 3 | `PlantQueryImpl.getByName` (plants-core; exercised by plants-console) | `Plant{Family,Genus,Species}RepositoryMock.getByName` (×3 — same composed-read-model pattern as #2) | mirror the insects fix in the plant factory/composition | `PlantBreadcrumbTest` (2), `PlantDetailGraphTest` (2) |
| 4 | `SoilProfileQueryImpl.getBySoilProfileName` (soil-core) | `getByLabAnalysisId` on 4 observation repos (Nutrient / ReportedOptimum / ReportedRecommendation / SoilPhysicalCharacteristics) | batch each observation type by lab-analysis-id set | `SoilProfileFactoryTest` (1) |

## How to fix (per finding)

1. Read the flagged query/factory. Confirm it resolves a set of children by calling a
   single-key select once per element (the loop / `stream().map` / recursive descent).
2. Add or use a batched sibling — `getByXNames(Set<NAME>)` / `findByNameSet(Set)` /
   `getByXIds(Set)` — and resolve the whole set in one call (`domains/CLAUDE.md`,
   "Fan-out must batch"). `InsectImageQueryImpl.forRankHierarchy` is the reference shape.
3. Re-run the module's tests; the `RepeatedSelectException` should clear.
4. **Only if a repeat is genuinely un-batchable** (two distinct single lookups the domain
   cannot combine), whitelist that one test method with
   `@AllowRepeatedSelect(query="…", select="…")` and a one-line justification. None of the
   four above are expected to need this — they are all composable batches.

## Notes for the fixing sessions

- Findings #2 and #3 are the same composed-read-model pattern (ancestor rank lookups in the
  organism factory); fixing insects first gives plants a template.
- The gate is single-threaded-safe (surefire is not parallelised; no cross-thread fan-out in
  test paths). If you ever parallelise surefire, the head-stack under-counts on worker
  threads — it can miss an N+1, never false-fail.
- Do not weaken the gate to make tests pass. The failing tests are the signal; fix the
  production fan-out.
