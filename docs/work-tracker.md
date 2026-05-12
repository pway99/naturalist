# Work Tracker

Dashboard for what's currently in flight. Strategy lives in [`plans/identification.md`](plans/identification.md); forks and open questions live in [`notes/parking-lot.md`](notes/parking-lot.md) (the parking lot). This file does NOT synthesize either — it is just the current view.

Last updated: 2026-05-12.

---

## Current phase

**Identification roadmap Phase 0 — Taxonomic reorganization + navigation console** ([`plans/identification.md`](plans/identification.md)).

## Current slice

**Clades kernel + life-stage refactor — Phase 1 next** ([`plans/clades-kernel.md`](plans/clades-kernel.md)). Stands up the Clade DAG kernel alongside the existing Rank DAG, then moves life-stage modelling from value-objects to traversal-based resolution.

**Resumes when complete:** PL-2 (green-lacewing rank correction) unblocks once Phase 5 lands. The resume shape becomes "replace inline life-stage duplicates with clade references" — much smaller per-organism work.

## Parking lot

[`notes/parking-lot.md`](notes/parking-lot.md) — currently 9 entries, 1 blocking the current path (PL-1).

## Recently completed

| Effort                                         | Completed  | Source                                                                       | Final commit                                                     |
|------------------------------------------------|------------|------------------------------------------------------------------------------|------------------------------------------------------------------|
| InsectGenus / InsectFamily life-stage API      | 2026-05-10 | [`plans/genus-family-life-stages.md`](plans/genus-family-life-stages.md)     | `ba04d3e`                                                        |
| Identification roadmap reframe                 | 2026-05-10 | [`plans/identification.md`](plans/identification.md)                         | `d5981e1`                                                        |
| First console write route                      | 2026-05-09 | [`plans/command-framework.md`](plans/command-framework.md)                   | `b6c4957`                                                        |
| Runtime data persistence                       | 2026-05-09 | [`plans/runtime-data-persistence.md`](plans/runtime-data-persistence.md)     | `b6c4957` (final follow-up)                                      |
| Command framework pilot                        | 2026-05-09 | [`plans/command-framework.md`](plans/command-framework.md)                   | `482b48d`                                                        |

## Active efforts (read the source doc for status; this is just the index)

| #  | Effort                                | Type           | Source                                                                            |
|----|---------------------------------------|----------------|-----------------------------------------------------------------------------------|
| 1  | Identification roadmap                | Plan (sketch)  | [`plans/identification.md`](plans/identification.md)                              |
| 2  | Clades kernel + life-stage refactor   | Plan (sketch)  | [`plans/clades-kernel.md`](plans/clades-kernel.md) — current slice; resolves PL-1, unblocks PL-2 when Phase 5 lands |
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
