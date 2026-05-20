# Work Tracker

Dashboard for what's currently in flight. Strategy lives in [`plans/identification.md`](plans/identification.md); forks and open questions live in [`notes/parking-lot.md`](notes/parking-lot.md) (the parking lot). This file does NOT synthesize either — it is just the current view.

Last updated: 2026-05-19 (Path A complete — three-step sequence landed; PL-11 raised for the structural functional-ecology follow-up).

---

## Current phase

**Identification roadmap Phase 0 — Taxonomic reorganization + navigation console** ([`plans/identification.md`](plans/identification.md)).

## Current slice

**No active slice — Path A just closed.** The three-step Path A sequence is fully landed:

- **Step 1** — Insect-image parent-rank extension (`600bc1d` + `4c7449d`). `InsectImage.parentName : InsectRankName`; rank-polymorphic query (`forParentName`); flat `EXTERNAL_PROPERTY` JSON shape; rank-aware FK constraints.
- **Step 2** — Hemiptera Clade permit + Hemimetabolous trait declaration (`2e469f0`). New kernel permit `kernels/clades/Hemiptera.java` (parent = Insecta); `InsectClades.traitsFor` declares Hemiptera → MetabolyTrait(Hemimetabolous).
- **Step 3** — potato-leafhopper → empoasca genus reclassification (`05d430f`). Species record deleted; standalone life-stage records renamed to `empoasca-egg`/`empoasca-adult`; empoasca genus placed in Hemiptera; four leafhopper image records repointed from species to genus rank; test consumers swapped off `PotatoLeafhopper` to `BattusPhilenor` + `Empoasca`.

**Side-effect from step 3** — PL-11 raised. Deleting the potato-leafhopper species record exposed that `guilds: Set<FunctionalGuild>` and `beneficial: boolean` had no home at genus/family rank — the same loss had hit the six previously-migrated PL-2 organisms silently. Decision (confirmed in conversation): Option B — introduce a cross-rank `InsectFunctionalRole` entity keyed by `InsectRankName`, mirroring the `InsectImage` cross-rank pattern. 15 records to migrate (8 species + 7 PL-2 ranks), all recoverable from git history.

**Candidate next slices** (pick when ready):
- **PL-11 — `InsectFunctionalRole` cross-rank entity.** Scaffold the entity + repository + query + mock + contract test; back-fill 15 records; remove `guilds` / `beneficial` from `InsectSpecies`; update `/insects/guild/{guild}` console to fan across ranks. Scope sketched in PL-11.
- **PL-2 — tachinid-fly + braconid-wasp rank corrections.** Same recipe as the 7 landed PL-2 organisms, but blocked on fixture-migration scope across `SpeciesRepositoryTest`, `SpeciesCommandImplTest`, `SpeciesQueryImplTest`, `LifeStageEntityQueryImplTest`, `LifeStageEntityRepositoryTest`, `InMemoryCatalogTest`. Needs a deliberate fixture-replacement sub-slice (likely repoint to battus-philenor).
- **LifeStage parallel rank-polymorphism slice.** `LifeStageRepository.getBySpeciesName` + `InsectLifeStageQuery.forSpeciesName` still only accept `InsectSpeciesName`, even though life-stage records keyed under genus / family composite slugs (`chrysoperla-egg`, `halictus-larva`, `empoasca-adult`, …) already exist in the catalog. Same recipe as the image-parent-rank slice. Noted in `insect-image-parent-rank.md` as out-of-scope.

## Parking lot

[`notes/parking-lot.md`](notes/parking-lot.md) — currently 10 entries (PL-11 raised today). PL-2 has 2 remaining organisms (tachinid-fly, braconid-wasp) blocked by fixture-migration scope. PL-11 is the structural-ecology follow-up triggered by Path A step 3.

## Recently completed

| Effort                                                                                  | Completed  | Source                                                                       | Final commit |
|-----------------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------|--------------|
| Path A step 3 — potato-leafhopper → empoasca + PL-11 raised                             | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `05d430f`    |
| Path A step 2 — Hemiptera Clade permit + Hemimetabolous trait declaration               | 2026-05-19 | conversation; [`kernels/clades/`](../kernels/clades/)                        | `2e469f0`    |
| Path A step 1 — Insect-image parent-rank Steps 1–8 (retype to InsectRankName)           | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `4c7449d`    |
| Insect-image parent-rank — Step 0 (sealed marker + Jackson verify)                      | 2026-05-19 | [`plans/insect-image-parent-rank.md`](plans/insect-image-parent-rank.md)     | `600bc1d`    |
| PL-2 — four family-rank corrections (hoverfly, ground-beetle, crane-fly, skipper-butterfly) | 2026-05-14 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                    | `c3a3f8d`    |
| Drop `placedInOptional()` — consumers wrap at the call site                             | 2026-05-14 | inline cleanup                                                               | `b2f415a`    |
| PL-2 — grey-mining-bee → andrena                                                        | 2026-05-13 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                        | `d12c3b2`    |
| PL-2 — native-sweat-bee → halictus                                                      | 2026-05-13 | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-2)                        | `be0bbce`    |
| PL-2 — green-lacewing → chrysoperla (pilot)                                             | 2026-05-13 | [`plans/green-lacewing-rank-correction.md`](plans/green-lacewing-rank-correction.md) | `e5e3c8d` |
| Clades kernel — Phase 5 (InsectLifeStages resolver)                                     | 2026-05-13 | [`plans/clades-kernel-phase-5.md`](plans/clades-kernel-phase-5.md)           | `ad7d7c7`    |

## Active efforts (read the source doc for status; this is just the index)

| #  | Effort                                | Type           | Source                                                                            |
|----|---------------------------------------|----------------|-----------------------------------------------------------------------------------|
| 1  | Identification roadmap                | Plan (sketch)  | [`plans/identification.md`](plans/identification.md)                              |
| 2  | PL-11 — cross-rank functional ecology | Parking lot    | [`notes/parking-lot.md`](notes/parking-lot.md) (PL-11) — Option B decided; 15 records to migrate; sized but not yet slice-planned |
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
