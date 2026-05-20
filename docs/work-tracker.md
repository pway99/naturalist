# Work Tracker

Dashboard for what's currently in flight. Strategy lives in [`plans/identification.md`](plans/identification.md); forks and open questions live in [`notes/parking-lot.md`](notes/parking-lot.md) (the parking lot). This file does NOT synthesize either — it is just the current view.

Last updated: 2026-05-20 (LifeStage rank-polymorphism widening landed — `forParentName(InsectRankName)` mirror of the image-parent-rank slice. PL-12 raised: promote `LifeStage.parentName` to a typed component, parked pending LinneanRank).

---

## Current phase

**Identification roadmap Phase 0 — Taxonomic reorganization + navigation console** ([`plans/identification.md`](plans/identification.md)).

## Current slice

**No active slice — LifeStage rank-polymorphism widening just landed.** `InsectLifeStageQuery.forParentName(InsectRankName)` now accepts any of the four insect-rank names; the species-only `forSpeciesName` is gone. Mirror of the image-parent-rank slice; no JSON migration (the LifeStageName slug was already rank-flexible post-PL-2). Typed `LifeStage.parentName` component promotion deferred as PL-12, pending the parallel LinneanRank session.

**Candidate next slices** (pick when ready):

- **PL-2 — tachinid-fly + braconid-wasp rank corrections.** Same recipe as the eight landed PL-2 organisms, but blocked on fixture-migration scope across `SpeciesRepositoryTest`, `SpeciesCommandImplTest`, `SpeciesQueryImplTest`, `LifeStageEntityQueryImplTest`, `LifeStageEntityRepositoryTest`, `InMemoryCatalogTest`. Needs a deliberate fixture-replacement sub-slice (likely repoint to `battus-philenor`).
- **Sightings entity (identification roadmap Phase 1+).** The PL-11 closeout flagged two facts unique to the deleted potato-leafhopper record that need rehoming when a sightings entity arrives: the dated crimson-clover April 2026 observation, and the "first pest species documented in Oak Vista census" sequencing fact.

## Parking lot

[`notes/parking-lot.md`](notes/parking-lot.md) — currently 10 entries. PL-12 raised today: promote `LifeStage.parentName` to a typed component (parked pending LinneanRank). PL-2 has 2 remaining organisms (tachinid-fly, braconid-wasp) blocked by fixture-migration scope.

## Recently completed

| Effort                                                                                  | Completed  | Source                                                                       | Final commit |
|-----------------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------|--------------|
| LifeStage query rank-polymorphism — `forParentName(InsectRankName)`                     | 2026-05-20 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md) (mirror) | _commit_     |
| PL-11 Phase 4 — strip species fields; console fanout                                    | 2026-05-19 | [`plans/insect-functional-role.md`](plans/insect-functional-role.md)         | `327c5db`    |
| PL-11 Phase 3 — cross-rank `getByGuild` query stack                                     | 2026-05-19 | [`plans/insect-functional-role.md`](plans/insect-functional-role.md)         | `2b0c38a`    |
| PL-11 Phase 2 — seed 16 records + smoke + contract tests                                | 2026-05-19 | [`plans/insect-functional-role.md`](plans/insect-functional-role.md)         | `bcce0e0`    |
| PL-11 Phase 1 — `InsectFunctionalRole` entity stack                                     | 2026-05-19 | [`plans/insect-functional-role.md`](plans/insect-functional-role.md)         | `8f6072a`    |
| Path A step 3 — potato-leafhopper → empoasca + PL-11 raised                             | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `05d430f`    |
| Path A step 2 — Hemiptera Clade permit + Hemimetabolous trait declaration               | 2026-05-19 | conversation; [`kernels/clades/`](../kernels/clades/)                        | `2e469f0`    |
| Path A step 1 — Insect-image parent-rank Steps 1–8 (retype to InsectRankName)           | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `4c7449d`    |
| Insect-image parent-rank — Step 0 (sealed marker + Jackson verify)                      | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `600bc1d`    |
| PL-2 — four family-rank corrections (hoverfly, ground-beetle, crane-fly, skipper-butterfly) | 2026-05-14 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                    | `c3a3f8d`    |
| PL-2 — green-lacewing / halictus / andrena rank corrections                             | 2026-05-13 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                        | `d12c3b2`    |

## Active efforts (read the source doc for status; this is just the index)

| #  | Effort                                | Type           | Source                                                                            |
|----|---------------------------------------|----------------|-----------------------------------------------------------------------------------|
| 1  | Identification roadmap                | Plan (sketch)  | [`plans/identification.md`](plans/identification.md)                              |
| 2  | Clades kernel + life-stage refactor   | Plan (sketch)  | [`plans/clades-kernel.md`](plans/clades-kernel.md) — Phases 1–5 ✅; Phase 6 (plants) deferred |
| 3  | FU-1 — Family/Genus catalog tiers     | Plan (notes)   | [`notes/fu-1-plan.md`](notes/fu-1-plan.md) (PR-1 / PR-2a–e ✅; PR-2f / PR-2g / PR-3 folded into the identification roadmap) |
| 4  | Catalog kernel — M9b/M10/M11/M12      | Plan           | [`plans/catalog-kernel.md`](plans/catalog-kernel.md)                              |
| 5  | Command framework — follow-ups        | Plan           | [`plans/command-framework.md`](plans/command-framework.md)                        |
| 6  | Admin console — view 4 (deferred)     | Plan           | [`plans/admin-console.md`](plans/admin-console.md)                                |
| 7  | Pressure test — *Battus philenor*     | Pressure test  | [`pressure-test/battus-philenor/01-findings.md`](pressure-test/battus-philenor/01-findings.md) — **paused** while the identification roadmap builds the capability A1-F1 surfaced |
| 8  | Backlog — Soil/Sensor services        | Notes          | [`notes/pending-implementation.md`](notes/pending-implementation.md)              |
| 9  | Vision-assisted identification        | Plan (sketch)  | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md) — deferred |

---

## Conventions

- This file is a **dashboard**, not a synthesis. Each row points to its source-of-truth doc.
- The **current slice** section is the one row most often edited. The rest is reference.
- **Forks** (discoveries that derail the current slice) go to the [parking lot](notes/parking-lot.md) — not into this file or the strategy doc. Strategy updates only when the parking-lot answer is resolved.
- **Lighter slice plans for repetitive work.** Novel slices (new pattern, new kernel, contentious design) get full implementation plans. Slices that repeat an already-established pattern (e.g., per-organism rank corrections after one pilot landed) get a 1-paragraph note, not a 300-line doc.
