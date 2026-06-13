# Work Tracker

Dashboard for what's currently in flight. Strategy lives in [`plans/identification.md`](plans/identification.md); forks and open questions live in [`notes/parking-lot.md`](notes/parking-lot.md) (the parking lot). This file does NOT synthesize either — it is just the current view.

> **Sibling index.** [`plans/README.md`](plans/README.md) lists the active plan
> files driving current code work and the archived plans for completed efforts.
> This work-tracker is broader (sketches, paused efforts, recently-completed
> rows). Use whichever surface fits the question.

Last updated: 2026-06-13 (External-authority Phase 1 — `kernels/authority` port + EOL mock client landed.)

---

## Current phase

**Identification roadmap Phase 1 — external-authority seam (`kernels/authority` port + EOL mock client)** ([`plans/identification.md`](plans/identification.md)). Phase 0 (taxonomic reorganization + family/genus console) is effectively complete — data reorg landed via PL-2 + the catalogue-completeness slices; family/genus list/detail pages shipped 2026-05-23.

## Current slice

*None active.* The Insect aggregate (former candidate) was built, reviewed (2026-06-03), and partially reworked — see the read-model backlog below.

**Candidate next slices** (lead first):

- **`kernels/bibliography`** (identification Phase 2) — `LiteratureReference` value object; ADR-009 prerequisite for the identification workflow, and independent of the insect read-model cleanup.
- **Insect read-model backlog** ([2026-06-03 review](notes/2026-06-03-insect-aggregate-bounded-context-review.md)) — R8/R1/R6 ✅ done; R7 half-done (types relabeled via the ReadModel effort, but the keep-and-adopt-vs-delete decision for the `InsectTaxonView` stack is open); R2/R3/R4/R5 parked (the `Insect` read-model reshape is a deferred WIP).

- **Sightings entity (identification roadmap Phase 1+).** The PL-11 closeout flagged two facts unique to the deleted potato-leafhopper record that need rehoming when a sightings entity arrives: the dated crimson-clover April 2026 observation, and the "first pest species documented in Oak Vista census" sequencing fact.
- **Taxonomic-scope breadcrumb primitive.** Phase 0's reusable breadcrumb (also reused by Phase 2). Deferred out of the family/genus pages slice.

## Parking lot

[`notes/parking-lot.md`](notes/parking-lot.md) — PL-2 closed out 2026-05-24 (moved to `parking-lot-resolved.md`). PL-12 (typed `LifeStage.parentName`) **resolved 2026-06-07** by R6 of the rank-FK effort (`InsectRankName parentName` on `LifeStage`), moved to `parking-lot-resolved.md`.

## Recently completed

| Effort                                                                                  | Completed  | Source                                                                       | Final commit |
|-----------------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------|--------------|
| ReadModel kernel type — 6th identity-model marker; insect read-models retyped + renamed `*Aggregate`→`*View`/`InsectTaxonView`, `insect()`→`taxonView()` | 2026-06-07 | [`plans/2026-06-07-readmodel-kernel-type-design.md`](plans/2026-06-07-readmodel-kernel-type-design.md) | `abf5345` (+ docs) |
| Insect rank-FK normalization — parent-only FKs (R8), trimmed `Insect` invariants (R1), typed `LifeStage.parentName` (R6); resolves PL-12 | 2026-06-07 | [`plans/2026-06-07-insect-rank-fk-normalization-design.md`](plans/2026-06-07-insect-rank-fk-normalization-design.md) | `730a218`    |
| Clades kernel Phase 5b — life-stage inline removal + resolver walk-up (PRs 1 + 2; PR 3 cancelled — see [`plans/insect-aggregate.md`](plans/insect-aggregate.md)) | 2026-05-31 | [`plans/clades-kernel-phase-5b-life-stage-inline-removal.md`](plans/clades-kernel-phase-5b-life-stage-inline-removal.md) | `be4639a`    |
| Insect page images — `BehavioralMap` kernel, `ImageGallery`, carousels on all listing/detail pages | 2026-05-25 | [`plans/archive/2026-05-24-insect-page-images.md`](plans/archive/2026-05-24-insect-page-images.md) | `a8f56f5`    |
| Console clade context — lineage trail (Animalia › Arthropoda › Insecta), Class Insecta description on landing page | 2026-05-25 | [`plans/archive/console-clade-context-plan.md`](plans/archive/console-clade-context-plan.md) | `a540825`    |
| InsectOrder entity — order-rank entity, genus/family FK refactoring (Phase 1 + 2)       | 2026-05-25 | [`plans/archive/insect-order-phase2-plan.md`](plans/archive/insect-order-phase2-plan.md)   | `41562e1`    |
| InsectGenus refactor — drop `TaxonomicOrder`/`TaxonomicFamily` local copies             | 2026-05-25 | [`plans/archive/insect-order-phase1-plan.md`](plans/archive/insect-order-phase1-plan.md)   | `11faa76`    |
| Catalogue-completeness slice 2 — `InsectSpecies` drops `TaxonomicClassification`; honest non-null invariants | 2026-05-24 | conversation; closes the simplification arc opened by slice 1               | `db9623b`    |
| PL-2 closeout — tachinid-fly + braconid-wasp → family rank; fixtures repointed          | 2026-05-24 | [`notes/parking-lot-resolved.md`](notes/parking-lot-resolved.md) (PL-2)      | `16ff792`    |
| Catalogue-completeness slice 1 — 5 families + 6 genera + species FK wiring              | 2026-05-23 | conversation; prerequisite for stripping `TaxonomicClassification` from `InsectSpecies` | `412912f`    |
| Family-page species enrichment — under-identified species on `/insects/families/{name}` | 2026-05-23 | conversation; uses `SpeciesQuery.forFamilyName` from PL-13 (reverted in PL-2 closeout) | `a81cccd`    |
| PL-14 — rank-polymorphic `InsectAggregate` (sealed interface + 3 record permits)        | 2026-05-23 | [`plans/archive/pl-14-rank-polymorphic-insect-aggregate.md`](plans/archive/pl-14-rank-polymorphic-insect-aggregate.md) | `d698ac5`    |
| PL-13 — typed `InsectGenusName` / `InsectFamilyName` FK on `InsectSpecies`              | 2026-05-23 | [`notes/parking-lot-resolved.md`](notes/parking-lot-resolved.md) (PL-13)     | `c91efc0`    |
| Insects console — family + genus list/detail pages                                      | 2026-05-23 | [`plans/archive/insects-family-genus-console.md`](plans/archive/insects-family-genus-console.md) | `b1216de`    |
| LifeStage query rank-polymorphism — `forParentName(InsectRankName)`                     | 2026-05-20 | [`plans/archive/insect-image-parent-rank.md`](plans/archive/insect-image-parent-rank.md) (mirror) | `f5878d7`    |
| PL-11 Phase 4 — strip species fields; console fanout                                    | 2026-05-19 | [`plans/archive/insect-functional-role.md`](plans/archive/insect-functional-role.md)         | `327c5db`    |
| PL-11 Phase 3 — cross-rank `getByGuild` query stack                                     | 2026-05-19 | [`plans/archive/insect-functional-role.md`](plans/archive/insect-functional-role.md)         | `2b0c38a`    |
| PL-11 Phase 2 — seed 16 records + smoke + contract tests                                | 2026-05-19 | [`plans/archive/insect-functional-role.md`](plans/archive/insect-functional-role.md)         | `bcce0e0`    |
| PL-11 Phase 1 — `InsectFunctionalRole` entity stack                                     | 2026-05-19 | [`plans/archive/insect-functional-role.md`](plans/archive/insect-functional-role.md)         | `8f6072a`    |
| Path A step 3 — potato-leafhopper → empoasca + PL-11 raised                             | 2026-05-19 | [`plans/archive/insect-image-parent-rank.md`](plans/archive/insect-image-parent-rank.md)     | `05d430f`    |
| Path A step 2 — Hemiptera Clade permit + Hemimetabolous trait declaration               | 2026-05-19 | conversation; [`kernels/clades/`](../kernels/clades/)                        | `2e469f0`    |
| Path A step 1 — Insect-image parent-rank Steps 1–8 (retype to InsectRankName)           | 2026-05-19 | [`plans/archive/insect-image-parent-rank.md`](plans/archive/insect-image-parent-rank.md)     | `4c7449d`    |
| Insect-image parent-rank — Step 0 (sealed marker + Jackson verify)                      | 2026-05-19 | [`plans/archive/insect-image-parent-rank.md`](plans/archive/insect-image-parent-rank.md)     | `600bc1d`    |
| PL-2 — four family-rank corrections (hoverfly, ground-beetle, crane-fly, skipper-butterfly) | 2026-05-14 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                    | `c3a3f8d`    |
| PL-2 — green-lacewing / halictus / andrena rank corrections                             | 2026-05-13 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                        | `d12c3b2`    |

## Active efforts (read the source doc for status; this is just the index)

| #  | Effort                                | Type           | Source                                                                            |
|----|---------------------------------------|----------------|-----------------------------------------------------------------------------------|
| 1  | Identification roadmap                | Plan (sketch)  | [`plans/identification.md`](plans/identification.md) — Phase 0 ✅; **Phase 1** (external-authority seam) landed; **Phase 2 next** (bibliography + consumer wiring) |
| 2  | Clades kernel + life-stage refactor   | Plan (sketch)  | [`plans/clades-kernel.md`](plans/clades-kernel.md) — Phases 1–5 ✅; Phase 5b (inline removal + resolver walk-up) ✅ — PRs 1+2 landed, PR 3 cancelled (moved to [`plans/insect-aggregate.md`](plans/insect-aggregate.md)); Phase 6 (plants) deferred |
| 3  | FU-1 — Family/Genus catalog tiers     | Plan (archived) | [`plans/archive/fu-1-plan.md`](plans/archive/fu-1-plan.md) (PR-1 / PR-2a–e ✅; PR-2f / PR-2g / PR-3 folded into the identification roadmap) |
| 4  | Catalog kernel — M9b/M10/M11/M12      | Plan           | [`plans/catalog-kernel.md`](plans/catalog-kernel.md)                              |
| 5  | Command framework — follow-ups        | Plan           | [`plans/command-framework.md`](plans/command-framework.md)                        |
| 6  | Admin console — view 4 (deferred)     | Plan           | [`plans/admin-console.md`](plans/admin-console.md)                                |
| 7  | Pressure test — *Battus philenor*     | Pressure test  | [`pressure-test/battus-philenor/01-findings.md`](pressure-test/battus-philenor/01-findings.md) — **paused** while the identification roadmap builds the capability A1-F1 surfaced |
| 8  | Backlog — Soil/Sensor services        | Notes          | [`notes/pending-implementation.md`](notes/pending-implementation.md)              |
| 9  | Vision-assisted identification        | Plan (sketch)  | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md) — deferred |
| 10 | Insect read-model review (R-backlog)  | Review notes   | [`notes/2026-06-03-insect-aggregate-bounded-context-review.md`](notes/2026-06-03-insect-aggregate-bounded-context-review.md) — R8/R1/R6 ✅, R7 half-done (ReadModel relabel); R2/R3/R4/R5 open |

---

## Conventions

- This file is a **dashboard**, not a synthesis. Each row points to its source-of-truth doc.
- The **current slice** section is the one row most often edited. The rest is reference.
- **Forks** (discoveries that derail the current slice) go to the [parking lot](notes/parking-lot.md) — not into this file or the strategy doc. Strategy updates only when the parking-lot answer is resolved.
- **Lighter slice plans for repetitive work.** Novel slices (new pattern, new kernel, contentious design) get full implementation plans. Slices that repeat an already-established pattern (e.g., per-organism rank corrections after one pilot landed) get a 1-paragraph note, not a 300-line doc.
