# Work Tracker

Dashboard for what's currently in flight. Strategy lives in [`plans/identification.md`](plans/identification.md); forks and open questions live in [`notes/parking-lot.md`](notes/parking-lot.md) (the parking lot). This file does NOT synthesize either — it is just the current view.

Last updated: 2026-05-19 (Insect-image parent-rank slice Steps 0–8 landed; Path A step 1 of 3 complete).

---

## Current phase

**Identification roadmap Phase 0 — Taxonomic reorganization + navigation console** ([`plans/identification.md`](plans/identification.md)).

## Current slice

**Insect-image parent-rank extension — Path A step 1 of 3 complete** ([`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)). Step 0 (`InsectRankName` sealed marker + Jackson polymorphism verification) and Steps 1–8 (retype `InsectImage.parentName : InsectRankName`, collapse `forSpeciesName`/`getBySpeciesName` to rank-polymorphic `forParentName`/`getByParentName`, migrate the 11 image records to flat `EXTERNAL_PROPERTY` JSON shape, ripple through ~10 consumer files) landed in `600bc1d` + `4c7449d`. The image model now accepts any of the four insect-side rank permits at compile time, with field-level `@JsonTypeInfo` dispatch keeping leaf-class direct serializations as plain strings.

**Next on the path:**
- **Path A step 2** — add a `Hemimetabolous` trait permit to `kernels/clades/` + `InsectClades.traitsFor` so Hemiptera resolves to the right stage list (currently no kernel permit declares the trait).
- **Path A step 3** — reclassify potato-leafhopper → empoasca genus (the last PL-2 organism whose blocker was "images and Hemiptera trait"). This will produce the first organic genus-attached image records and let `ImageQueryImplTest.forParentName_acceptsGenusName` upgrade from "returns empty" to a real positive assertion.

## Parking lot

[`notes/parking-lot.md`](notes/parking-lot.md) — currently 9 entries, none blocking the current path. PL-2 has 3 remaining organisms (potato-leafhopper on Path A; tachinid-fly + braconid-wasp blocked by fixture-migration scope across 6+ test classes, separate slice).

## Recently completed

| Effort                                                              | Completed  | Source                                                                       | Final commit                       |
|---------------------------------------------------------------------|------------|------------------------------------------------------------------------------|------------------------------------|
| Insect-image parent-rank — Steps 1–8 (retype to InsectRankName)     | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `4c7449d`                          |
| Insect-image parent-rank — Step 0 (sealed marker + Jackson verify)  | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `600bc1d`                          |
| PL-2 — four family-rank corrections (hoverfly, ground-beetle, crane-fly, skipper-butterfly) | 2026-05-14 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2) | `c3a3f8d`                          |
| Drop `placedInOptional()` — consumers wrap at the call site         | 2026-05-14 | inline cleanup                                                               | `b2f415a`                          |
| PL-2 — grey-mining-bee → andrena                                    | 2026-05-13 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                        | `d12c3b2`                          |
| PL-2 — native-sweat-bee → halictus                                  | 2026-05-13 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                        | `be0bbce`                          |
| PL-2 — green-lacewing → chrysoperla (pilot)                         | 2026-05-13 | [`plans/green-lacewing-rank-correction.md`](plans/green-lacewing-rank-correction.md) | `e5e3c8d`                  |
| Clades kernel — Phase 5 (InsectLifeStages resolver)                 | 2026-05-13 | [`plans/clades-kernel-phase-5.md`](plans/clades-kernel-phase-5.md)           | `ad7d7c7`                          |
| Clades kernel — Phase 4 (first catalog placements)                  | 2026-05-13 | [`plans/clades-kernel-phase-4.md`](plans/clades-kernel-phase-4.md)           | `1cfd9ba`                          |
| Clades kernel — Phase 3 (placedIn on insect records)                | 2026-05-13 | [`plans/clades-kernel-phase-3.md`](plans/clades-kernel-phase-3.md)           | `5e67c1b`                          |

## Active efforts (read the source doc for status; this is just the index)

| #  | Effort                                | Type           | Source                                                                            |
|----|---------------------------------------|----------------|-----------------------------------------------------------------------------------|
| 1  | Identification roadmap                | Plan (sketch)  | [`plans/identification.md`](plans/identification.md)                              |
| 2  | Insect-image parent-rank (Path A)     | Plan           | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md) — Step 0 ✅ `600bc1d`, Steps 1–8 ✅ `4c7449d`; Path A step 2 (Hemimetabolous trait) + step 3 (potato-leafhopper → empoasca) next |
| 3  | Clades kernel + life-stage refactor   | Plan (sketch)  | [`plans/clades-kernel.md`](plans/clades-kernel.md) — Phases 1–5 ✅; Phase 6 (plants) deferred |
| 4  | FU-1 — Family/Genus catalog tiers     | Plan (notes)   | [`notes/fu-1-plan.md`](notes/fu-1-plan.md) (PR-1 / PR-2a–e ✅; PR-2f / PR-2g / PR-3 folded into the identification roadmap) |
| 5  | Catalog kernel — M9b/M10/M11/M12      | Plan           | [`plans/catalog-kernel.md`](plans/catalog-kernel.md)                              |
| 6  | Command framework — follow-ups        | Plan           | [`plans/command-framework.md`](plans/command-framework.md)                        |
| 7  | Admin console — view 4 (deferred)     | Plan           | [`plans/admin-console.md`](plans/admin-console.md)                                |
| 8  | Pressure test — *Battus philenor*     | Pressure test  | [`pressure-test/battus-philenor/01-findings.md`](pressure-test/battus-philenor/01-findings.md) — **paused** while the identification roadmap builds the capability A1-F1 surfaced |
| 9  | Backlog — Soil/Sensor services        | Notes          | [`notes/pending-implementation.md`](notes/pending-implementation.md)              |
| 10 | Vision-assisted identification        | Plan (sketch)  | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md) — deferred |

---

## Conventions

- This file is a **dashboard**, not a synthesis. Each row points to its source-of-truth doc.
- The **current slice** section is the one row most often edited. The rest is reference.
- **Forks** (discoveries that derail the current slice) go to the [parking lot](notes/parking-lot.md) — not into this file or the strategy doc. Strategy updates only when the parking-lot answer is resolved.
- **Lighter slice plans for repetitive work.** Novel slices (new pattern, new kernel, contentious design) get full implementation plans. Slices that repeat an already-established pattern (e.g., per-organism rank corrections after one pilot landed) get a 1-paragraph note, not a 300-line doc.
