# Plants ↔ Insects — Gap Review & Next Steps

**Date:** 2026-08-16
**Purpose:** Where the plants domain stands against insects (the reference organism
domain), what is still missing, and the doc/source sync state. Written for picking work
back up. This doc owns no scope — the source-of-truth plan is
[`2026-08-15-plants-domain-consistency-plan.md`](2026-08-15-plants-domain-consistency-plan.md);
the shared design is [`organism-domain-blueprint.md`](organism-domain-blueprint.md).

---

## 1. Where plants stands

The **rank-model alignment is essentially done.** As of 2026-08-16 plants has:

- The full Linnaean chain as typed rank entities: `PlantOrder → PlantFamily → PlantGenus
  → PlantSpecies`, each with a typed upward FK and a `ForeignKeyConstraint` at every rung
  (a bad parent fails at fixture load).
- `PlantRankName` sealed over the four rank names; anything attaching to a plant names the
  rank the evidence supports.
- `PlantEcologicalRole` holds roles at any rank (cross-rank), keyed by `PlantRankName`.
- Cross-rank references carry `PlantRankName`: `PlantProgram`, `PhytochemicalConstituent`,
  and garden's `Planting`. Their integrity is covered by catalog-data tests (no
  single-source FK can span ranks).
- Console parity: `/plants` lands on `/plants/orders`, species list at `/plants/species`,
  and the taxonomic breadcrumb header on every rank page (shipped 2026-08-16, not part of
  the consistency plan).

Plants also has **genuine domain richness insects lacks** — `cultivar`, `heritage`
(seed lineage + provenance), `phytochemistry`, and `management` (programs). These are not
gaps; they are what makes plants plants. Do not "align" them away.

---

## 2. Gaps vs insects

Enumerated from `insects-api` vs `plants-api`. "Type" distinguishes a **true gap** (plants
should have it and doesn't) from an **intentional difference** (plants models this concern
differently, or deliberately defers).

| Insects capability | Plants status | Type | Notes |
|---|---|---|---|
| Rank entities Order→Species + `RankName` | ✅ present | — | Reached parity this session (M2b/M2g). |
| **Rank-polymorphic read model** (`InsectTaxonView` sealed over per-rank `*View`, + factory; `Insect`) | ❌ none | **true gap** | No `PlantTaxonView`. A rank page assembles its parts ad hoc in the controller. Biggest structural gap. |
| **Write side** (`InsectCommand`, `CatalogIdentification` aggregate, `Transaction`, `with*` methods) | ❌ none | **true gap** | Plants is read-only. `domains/CLAUDE.md` requires a `with*` per mutable field the moment writes land. |
| **Collection unit** (`FieldObservation` — "a naturalist observed this") | ❌ none | **true gap** | No way to record that someone grew/observed a plant. Analogue would be a planting/observation, and `garden.Planting` may already be the plant-side collection unit — worth deciding rather than duplicating. |
| **Images at a rank** (`InsectImage`, hierarchical image query, add-photo command) | ❌ none | **true gap** | No `PlantImage`. Relevant to the MVP vision-ID goal (images stay on device). |
| **Vision identification** (`VisionService`, `InsectIdentificationCommand`) | ❌ none | **true gap (later)** | The MVP feature per project memory. Depends on the write side + images. |
| **Features/field marks** (`InsectFeature`, `FeatureAssignment`, `InsectFeatureView`) | ❌ none | **true gap (later)** | The "why this ID?" evidence surface. |
| **Clade placement** (`@Nullable Clade placedIn`, `InsectClades`) | ❌ none | **intentional defer** | Kernel has no plant clade permits yet; needs a plants-owned trait function. Blueprint & `kernels/CLAUDE.md` document this as pending. |
| **Functional guild / roles** (`InsectFunctionalRole`, `FunctionalGuild`) | ~ partial | **intentional difference** | Plants has `PlantEcologicalRole` + `PlantRole`. Covers the "roles" need; no guild *pages* yet. |
| **Life stages** (rich `lifestage` subpackage) | n/a | **intentional difference** | No plant analogue by nature; plants has cultivar/heritage/phytochemistry instead. |
| **Citations / library integration** (`InsectCitationView`, citation associations) | ❌ none | **true gap (later)** | Plants records reference chemistry compounds but have no citation surface. |
| Namespace convention (ADR-020: `SpeciesQuery`, drops prefix/infix) | ✗ deviates | **known debt** | Plants uses `PlantFamilyEntityQuery` etc. → M4. |
| N=1 namespace collapse for single-entity sub-contexts | ✗ deviates | **known debt** | cultivar/heritage/management/phytochemistry each wrap one entity → M5. |

---

## 3. Next steps (suggested order)

### A. Finish the consistency plan (small, mechanical, unblocked)

1. ~~**M2f — genus→species rollup.**~~ ✅ DONE 2026-08-16 — `forGenusName`/`forFamilyName`
   on the plant query, genus detail page species rollup, stale comment removed. Verified live.
2. **M4 — ADR-020 namespace rename** (`PlantFamilyEntityQuery` → `FamilyQuery`, etc.).
   Record the root-entity exception (`Plant` is both domain noun and subject) in
   `domains/CLAUDE.md`.
3. **M5 — N=1 collapse** for the four single-entity sub-contexts. Do with M4.
4. **M6 — split `PlantLifeForm`** into `GrowthHabit` (USDA growth-habit vocab) + `LifeCycle`; use categories stay off both. Decision recorded 2026-08-16; do with M4/M5.
5. **Cosmetic:** `TestPlantsIdentifiers` uses plural scope names (`PlantFamilies`,
   `PlantGenera`) where insects uses singular; fold into M4.

### B. Structural parity (medium, needs design)

6. **Rank-polymorphic read model** — a `PlantTaxonView` sealed over per-rank views plus a
   package-private factory, mirroring `InsectTaxonView`. This is the cleanest next parity
   step and would let the console stop hand-assembling rank pages.
7. **Write side** — `PlantCommand` + a `Transaction`, and `with*` methods on the mutable
   records. Prerequisite for anything that creates/edits plant data.

### C. Strategic / MVP (large, sequence later)

8. **Decide the plant collection unit.** Is `garden.Planting` the plant-side
   `FieldObservation`, or does plants need its own observation entity? Decide before
   building, to avoid duplicating the concept.
9. **Images + vision identification** — the MVP feature (images stay on device). Depends on
   the write side and a decision on the collection unit.
10. **Clade activation** — kernel work (plant clade permits + trait function); blocked on the
   clades kernel, tracked as effort #2 Phase 6.

### D. The abstraction pass (rule 3)

Once plants matches insects, do the side-by-side pass that rule 3 in
`domains/plants/CLAUDE.md` defers to: the `EntityRef((EntityName) rankName)` cast wart is
the first candidate — every rank-name consumer at a kernel API needs it, in both domains.

---

## 4. Documentation sync audit

Checked every plants-facing `.md` against current source. **Fixed in this pass** (docs
only, no code touched):

| File | Drift | Action |
|---|---|---|
| `domains/plants/CLAUDE.md` | FK chain listed `PlantProgram`/`PhytochemicalConstituent` `plantName` as `PlantSpeciesName`; they are `PlantRankName`. "Sole FK exception" paragraph named only `PlantEcologicalRole`. JSON docs called them "soft FK to a PlantSpecies" and omitted the `plantRank` discriminator. Stale `creeping-thyme-thymol` example; aspirational `PestManagement`. | ✅ corrected |
| `domains/CLAUDE.md` | "three for plants" (now four rank entities). | ✅ → four |
| `2026-08-15-plants-domain-consistency-plan.md` | M2b unmarked (it's done); M2c/M2e checkboxes unchecked; "suggested ordering" called M2b "the largest remaining change"; M2e overstated a `parentName` rename that was superseded. | ✅ statuses + next-steps rewritten |

**Needs your call — not fixed:**

- **`docs/briefings/plants-domain.md` (213 lines) is substantially stale** — it predates the
  entire rank refactor. It still describes `Plant`/`PlantName`, `taxonomy`/
  `TaxonomicClassification` on the record, roles/predicates on `Plant`, `PestManagement`,
  and `plantName` as "soft FK to Plant". It never mentions `PlantOrder`/`PlantFamily`/
  `PlantGenus`, `PlantRankName`, `PlantEcologicalRole`, or the rank chain. This is a
  narrative rewrite (voice matters), so I left it for us to do together rather than
  find-replace it into Frankenstein prose.
- **`PlantsController` stale comment** (code, not a doc) — the genus-detail "genusName does
  not exist yet" comment; folds naturally into M2f. Left untouched per "no code changes
  without review".

**Verified in sync:** `plants-ubl.md` (rank ladder, types, order permit), the blueprint
ladder table, `taxonomy-ubl.md`/`clades-ubl.md` (kernel, unaffected).

Also added: a plants-consistency row to `docs/work-tracker.md` (the effort had none).

---

## 5. One-line recommendation

M2f is done; pick up with the **M4/M5/M6 cleanup batch** (same files, one reviewer), then decide the
**collection-unit question** (Planting vs new observation) because it gates the MVP path —
and schedule the **plants briefing rewrite** as its own focused session.
