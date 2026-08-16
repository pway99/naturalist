# Work Tracker

Dashboard for what's currently in flight. Strategy lives in [`plans/identification.md`](plans/identification.md); forks and open questions live in [`notes/parking-lot.md`](notes/parking-lot.md) (the parking lot). This file does NOT synthesize either — it is just the current view.

> **Sibling index.** [`plans/README.md`](plans/README.md) lists the active plan
> files driving current code work and the archived plans for completed efforts.
> This work-tracker is broader (sketches, paused efforts, recently-completed
> rows). Use whichever surface fits the question.

Last updated: 2026-08-16 (Plants ↔ insects consistency — effort #17; rank chain Order→Species with FK at every rung, cross-rank roles/refs, console lands on orders with a taxonomic breadcrumb. M2f/M4/M5 remain.)

---

## Current phase

**Identification roadmap** ([`plans/identification.md`](plans/identification.md)). Phase 0 ✅ (taxonomic reorganization + family/genus console). Phase 1 ✅ (external-authority seam — `kernels/authority` port + EOL mock client). Phase 2 citation/library ✅ (`Citation` sealed NamedEntity, `domains/library` domain stack, `Eol.citation()` factory). Phase 2 console ✅ (citations page at `/citations`). Citation association ✅.

**Vision-assisted identification** shipped end-to-end (2026-07-12 → 2026-07-14): `kernels/vision` port + Anthropic adapter, `InsectIdentificationService` (now `InsectIdentificationCommand`), console `/insects/identify` route with photo upload, filesystem image storage, field notes, re-identify. The identification logic was refactored from the console controller into `insects-core` via `Transaction<CatalogIdentification>` + `InsectIdentificationCommand`, establishing the `TestContextInternal` pattern for core tests that need the full wired graph.

**Identification enrichment** shipped 2026-07-15: authority-validated rank-polymorphic identification (ORDER/FAMILY/GENUS/SPECIES with Linnaean fallback), grounded Durrell descriptions for parent ranks via `TextGenerationService` + authority content, structured `InsectFeature` persistence with dedup, cross-domain citation writes to library. New kernels: `text-generation` port + `anthropic-text-generation` adapter, `ExternalAuthority.fetchContent()`. `InsectIdentificationCommand` now returns `Optional<InsectRankName>` — empty when authority rejects all ranks.

**Present the evidence** shipped 2026-08-08. The enrichment effort persisted features and
citations but never displayed them; three identifications of the same beetle produced
`carabidae`, `chrysomelidae`, and `cleridae` with no surfaced grounds for a human to
adjudicate between them. Citation associations now survive the flush (they were being
dropped for a null origin file), field marks render on every rank page, and family /
genus / order pages carry the photo gallery with per-observation confidence, evidence,
and alternatives that previously existed only on the species page.

**Nutrient → chemistry links** shipped 2026-08-15. Every nutrient row on `/soil/profiles/{name}`
labelled a lab reading in dead text — no path from "calcium" to the chemistry catalog that
already holds its symbol, atomic weight, and ionic form. Soil now declares the reference
(`Nutrients.chemistryOf` → `NutrientChemistry`) and chemistry owns the destination (six
previously-uncatalogued elements, new `/chemistry/elements/{slug}` pages, elements now
searchable); the soil console resolves the link through the existing `Catalog.findBySlug` +
composite `EntityRefLinker` seam, so it gained no dependency on chemistry. A
compounds-by-element section was designed and cut on evidence — useful for 6 elements, empty
for 7 (every micronutrient, exactly what a reader clicks from the micro table), noise for 3
(carbon/hydrogen/oxygen match nearly the whole catalog). Soil back-references on the element
page ("appears in your lab reports as Calcium (Exch), Calcium (Sol)") is the strongest next
slice — non-empty for all 13 nutrient-mapped elements — and stays unbuilt.

**Naturalist login** shipped 2026-07-06; **collection feature** (FieldObservation, collection lens, header filter chip) shipped 2026-07-12.

## Current slice

**Candidate next slices** (lead first):
- **Dual-strategy Slice 3 — collection lens** ([design `8eaadd5`](plans/archive/2026-06-29-dual-strategy-breadcrumb.md)) — each clade/rank node lists the user's catalogued insects under it. Cross-domain via the `catalog` kernel; the `Catalog.findBySlug` seam shipped 2026-07-05 is reusable here. (Slice 2 source-of-truth links reuses external-authority Phase 1.)
- **`/concepts` restyle** — the library concept pages still use the plain breadcrumb; bring them onto the context-bar now that popovers link readers there.
- **Insect read-model backlog** ([2026-06-03 review](notes/2026-06-03-insect-aggregate-bounded-context-review.md)) — R8/R1/R6 ✅ done; R7 half-done (types relabeled via the ReadModel effort, but the keep-and-adopt-vs-delete decision for the `InsectTaxonView` stack is open); R2/R3/R4/R5 parked (the `Insect` read-model reshape is a deferred WIP).
- **Taxonomic-scope breadcrumb primitive.** Phase 0's reusable breadcrumb (also reused by Phase 2). Deferred out of the family/genus pages slice.

## Parking lot

[`notes/parking-lot.md`](notes/parking-lot.md) — PL-2 closed out 2026-05-24 (moved to `parking-lot-resolved.md`). PL-12 (typed `LifeStage.parentName`) **resolved 2026-06-07** by R6 of the rank-FK effort (`InsectRankName parentName` on `LifeStage`), moved to `parking-lot-resolved.md`.

## Recently completed

| Effort                                                                                  | Completed  | Source                                                                       | Final commit |
|-----------------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------|--------------|
| Present the evidence — `CitationAssociation` flush fix (`TestEntitySource.writable`/`writableClass` seam + parser-supplying `loadFile`), `FeatureGroup` + `features.jte` field marks on all four rank pages, `observationGallery.jte` extracted from `detail.jte` and added to family/genus/order, notes-form ownership check (`InsectsController.owns`) closing a cross-naturalist field-notes overwrite | 2026-08-08 | [`plans/2026-08-08-present-the-evidence-plan.md`](plans/2026-08-08-present-the-evidence-plan.md) | `d2bf585` |
| Identification enrichment — authority-validated rank-polymorphic ID (ORDER→SPECIES with fallback), grounded parent descriptions via `TextGenerationService`, `InsectFeature` persistence, cross-domain citations, `anthropic-text-generation` adapter | 2026-07-15 | [`plans/2026-07-15-identify-enrichment-plan.md`](plans/2026-07-15-identify-enrichment-plan.md) | `40f3f8e` |
| Hierarchical image query + add-photo command — `ImageQuery.forRankHierarchy()` replaces three controller helpers; `InsectAddPhotoCommand` + `PhotoAddition` aggregate + `InsectAddPhotoTransaction` for atomic observation+image insert at any rank | 2026-07-14 | [`plans/2026-07-14-insect-hierarchical-image-query.md`](plans/2026-07-14-insect-hierarchical-image-query.md), [`plans/2026-07-14-insect-add-photo-command.md`](plans/2026-07-14-insect-add-photo-command.md) | `4a5a71b` |
| Identification transaction refactor — `Transaction<CatalogIdentification>`, `InsectIdentificationCommand` (replaces `InsectIdentificationService`), `InsectsTestContextInternal` pattern for core tests that need the full wired graph | 2026-07-14 | conversation (plan was inline) | `e5fb481` |
| Domain record helpers — `InsectRankName.of(slug, rank)` static factory, `FieldObservation.withNotes`/`.withSubject` mutation helpers, controller simplification | 2026-07-14 | [`plans/archive/2026-07-14-insect-record-helpers.md`](plans/archive/2026-07-14-insect-record-helpers.md) | `17a84e2` |
| Vision-assisted identification MVP — `kernels/vision` port + Anthropic adapter, `InsectIdentificationService`, console `/insects/identify` route, filesystem image storage, field notes editing, re-identify, novel-species parent-rank creation | 2026-07-14 | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md) | `3403023` |
| Naturalist collection — `FieldObservation` entity, collection lens header chip, species/rank "in your collection" indicators, observe/un-observe endpoints, per-naturalist photo ownership | 2026-07-12 | [`plans/archive/2026-07-12-naturalist-insect-collection-plan.md`](plans/archive/2026-07-12-naturalist-insect-collection-plan.md) | `7633ac3` |
| Naturalist login — form login, `NaturalistHeaderInterceptor`, `ROLE_NATURALIST` authority, composite `UserDetailsService` | 2026-07-06 | conversation | `d609b2e` |
| Clade rank→catalog bridge — two-axis clade links: rank eyebrows resolve into the insects catalog via a new side-effect-free `Catalog.findBySlug` seam + `InsectsLinker` order case + `CladeRankLinks`; names stay on the tree axis | 2026-07-05 | [`plans/archive/2026-07-05-clade-rank-catalog-bridge.md`](plans/archive/2026-07-05-clade-rank-catalog-bridge.md) | `f608ba0` |
| Clades console "?" info popovers — reusable `infoPopover` component (native HTML `popover`, anchor-positioned) on the clades breadcrumb + insects tree-of-life trail + Linnaean rank ladder | 2026-07-05 | conversation | `79fd174` |
| Clades console context-bar restyle — `/clades` pages moved onto the shared `.context-bar`; awkward `.dual-breadcrumb` grid retired | 2026-07-05 | [`superpowers/specs/2026-07-05-clades-console-context-bar-restyle-design.md`](superpowers/specs/2026-07-05-clades-console-context-bar-restyle-design.md) | `973fd0d` |
| Dual-strategy clade navigation + insect-console streamline — two parallel breadcrumb rows (phylogenetic lineage + Linnaean rank ladder), learnable nodes, clade trail with research-gap affordance; expanded clade override map | 2026-07-05 | [`plans/archive/2026-06-29-dual-strategy-breadcrumb.md`](plans/archive/2026-06-29-dual-strategy-breadcrumb.md), [`plans/archive/2026-07-02-insect-console-navigation.md`](plans/archive/2026-07-02-insect-console-navigation.md) | `558bd99` |
| InsectFeature entity migration — `InsectFeature` promoted from embedded VO to independent `Entity<InsectFeatureId>` with many-to-many `InsectFeatureAssignment`; `InsectFeatureQueryImpl` lineage-composite resolution; `InsectAncestryResolver` extracted; `identificationFeatures` removed from rank entities | 2026-06-25 | [`plans/archive/2026-06-25-insect-feature-entity-design.md`](plans/archive/2026-06-25-insect-feature-entity-design.md) | `35451c7` |
| Citation association — `CitationAssociation` entity + `InsectCitationQueryImpl` hierarchical citation discovery + `Insect` read model via `InsectFactory` + detail page rendering | 2026-06-25 | [`plans/archive/2026-06-14-citation-association-design.md`](plans/archive/2026-06-14-citation-association-design.md) | `34091d2` |
| Citations page — `/citations` single-page listing in library-console; `CitationQuery` wired into `LibraryTestContext`; nav link added | 2026-06-14 | conversation | `41aa0b3` |
| Library-console module extraction — `CladesController`, `ConceptsController`, JTE templates, `LibraryDataConfiguration`, `LibraryLinker` moved from management-console to `domains/library/library-console` | 2026-06-14 | [`plans/archive/2026-06-14-library-console-module.md`](plans/archive/2026-06-14-library-console-module.md) | `7c3b8e6` |
| Clades & taxonomy console — `Concept` entity + `CladeCatalog` + concept pages + clade browser + insect cross-links + nav; UI polish (breadcrumbs, field-guide styling, More→inline nav) | 2026-06-14 | [`plans/archive/2026-06-13-clades-taxonomy-console-plan.md`](plans/archive/2026-06-13-clades-taxonomy-console-plan.md) | `5ee23a6` |
| Paraphyly fixtures — 9 clade permits (Papilionoidea, Troidini, Drosophilinae, Sophophora, DrosophilaSensuStricto, Blattodea, Termitoidae, Apoidea, Anthophila); 3 teaching-exemplar lineages; Blattodea→Hemimetabolous trait; monotonicity acceptance test (21 tests); 11 EOL citations | 2026-06-13 | [`plans/archive/2026-06-13-paraphyly-fixtures.md`](plans/archive/2026-06-13-paraphyly-fixtures.md) | `9fbed25`    |
| Citation + Library — `Citation` sealed NamedEntity (+ `OnlineSource` permit) in `kernels/authority`; `domains/library` domain stack (repository, mock, contract tests, query); `Eol.citation()` factory | 2026-06-13 | [`plans/archive/2026-06-13-citation-and-library-design.md`](plans/archive/2026-06-13-citation-and-library-design.md) | `16615a6`    |
| ReadModel kernel type — 6th identity-model marker; insect read-models retyped + renamed `*Aggregate`→`*View`/`InsectTaxonView`, `insect()`→`taxonView()` | 2026-06-07 | [`plans/archive/2026-06-07-readmodel-kernel-type-design.md`](plans/archive/2026-06-07-readmodel-kernel-type-design.md) | `abf5345` (+ docs) |
| Insect rank-FK normalization — parent-only FKs (R8), trimmed `Insect` invariants (R1), typed `LifeStage.parentName` (R6); resolves PL-12 | 2026-06-07 | [`plans/archive/2026-06-07-insect-rank-fk-normalization-design.md`](plans/archive/2026-06-07-insect-rank-fk-normalization-design.md) | `730a218`    |
| _Earlier efforts (2026-05-31 and before — clades kernel 5b, insect page images, InsectOrder, catalogue-completeness, PL-11/13/14, Path A, PL-2 rank corrections)_ | ≤2026-05-31 | see the archived-plans table in [`plans/README.md`](plans/README.md#archived) | — |

## Active efforts (read the source doc for status; this is just the index)

| #  | Effort                                | Type           | Source                                                                            |
|----|---------------------------------------|----------------|-----------------------------------------------------------------------------------|
| 1  | Identification roadmap                | Plan (sketch)  | [`plans/identification.md`](plans/identification.md) — Phase 0 ✅; **Phase 1** (external-authority seam) ✅; **Phase 2** citation/library ✅ + console ✅; citation association design drafted |
| 2  | Clades kernel + life-stage refactor   | Plan (sketch)  | [`plans/clades-kernel.md`](plans/clades-kernel.md) — Phases 1–5 ✅; Phase 5b ✅; paraphyly fixtures ✅ (9 permits, 3 lineages, monotonicity test); Phase 6 (plants) deferred |
| 3  | FU-1 — Family/Genus catalog tiers     | Plan (archived) | [`plans/archive/fu-1-plan.md`](plans/archive/fu-1-plan.md) (PR-1 / PR-2a–e ✅; PR-2f / PR-2g / PR-3 folded into the identification roadmap) |
| 4  | Catalog kernel — M9b/M10/M11/M12      | Plan           | [`plans/catalog-kernel.md`](plans/catalog-kernel.md)                              |
| 5  | Command framework — follow-ups        | Plan           | [`plans/command-framework.md`](plans/command-framework.md)                        |
| 6  | Admin console — view 4 (deferred)     | Plan           | [`plans/admin-console.md`](plans/admin-console.md)                                |
| 7  | Pressure test — *Battus philenor*     | Pressure test  | [`pressure-test/battus-philenor/01-findings.md`](pressure-test/battus-philenor/01-findings.md) — **paused** while the identification roadmap builds the capability A1-F1 surfaced |
| 8  | Backlog — Soil/Sensor services        | Notes          | [`notes/pending-implementation.md`](notes/pending-implementation.md)              |
| 9  | Vision-assisted identification        | Plan           | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md) — **shipped** 2026-07-14 (vision kernel, Anthropic adapter, console route, identification command, transaction refactor) |
| 10 | Insect read-model review (R-backlog)  | Review notes   | [`notes/2026-06-03-insect-aggregate-bounded-context-review.md`](notes/2026-06-03-insect-aggregate-bounded-context-review.md) — R8/R1/R6 ✅, R7 half-done (ReadModel relabel); R2/R3/R4/R5 open |
| 11 | Hierarchical image query              | Plan           | [`plans/2026-07-14-insect-hierarchical-image-query.md`](plans/2026-07-14-insect-hierarchical-image-query.md) — **shipped** 2026-07-14 |
| 12 | Add-photo command                     | Plan           | [`plans/2026-07-14-insect-add-photo-command.md`](plans/2026-07-14-insect-add-photo-command.md) — **shipped** 2026-07-14 (`PhotoAddition` aggregate + `InsectAddPhotoTransaction`; accepts `InsectRankName`) |
| 13 | Identification enrichment             | Plan           | [`plans/2026-07-15-identify-enrichment-plan.md`](plans/2026-07-15-identify-enrichment-plan.md) — **shipped** 2026-07-15 (authority validation, rank-polymorphic ID, grounded descriptions, features, citations, text-generation adapter) |
| 14 | Present the evidence                  | Plan           | [`plans/2026-08-08-present-the-evidence-plan.md`](plans/2026-08-08-present-the-evidence-plan.md) — **shipped** 2026-08-08 (citation-association flush fix, field marks, rank observation gallery) |
| 15 | Soil↔naturalist ownership (Property)  | Design memo    | [`notes/2026-08-09-soil-profile-naturalist-association.md`](notes/2026-08-09-soil-profile-naturalist-association.md) — design agreed 2026-08-09, **not scheduled**. New `Property` aggregate = tenancy root above Zone (PUBLIC/PRIVATE + `owner`) in the zone-domain `com.naturalist.zone.property` sub-context; Zone gains `PropertyName` FK (`identifiers`); soil unchanged (owner derived `zone→property`). Zone-domain multi-PR slice; watch the `ZoneInfo` arity ripple. Next step: writing-plans → PR breakdown. |
| 16 | Nutrient → chemistry links            | Plan           | [`plans/2026-08-15-nutrient-chemistry-links-plan.md`](plans/2026-08-15-nutrient-chemistry-links-plan.md) — **shipped** 2026-08-15 (six missing elements + element pages + catalog contribution in chemistry; `Nutrients.chemistryOf` in soil; soil-console links nutrient rows via `Catalog.findBySlug` + `EntityRefLinker`). Compounds-by-element and soil back-references on the element page considered and deferred — see [design §2](plans/2026-08-15-nutrient-chemistry-links-design.md#2-what-the-element-page-deliberately-omits). |
| 17 | Plants ↔ insects consistency          | Plan           | [`plans/2026-08-15-plants-domain-consistency-plan.md`](plans/2026-08-15-plants-domain-consistency-plan.md) — M1/M2a–e/M2g/M3 **shipped** 2026-08-15–16 (typed `PlantOrder→Family→Genus→Species` chain with FK at every rung; `PlantRankName` sealed; `PlantEcologicalRole` cross-rank; `PlantProgram`/`PhytochemicalConstituent`/garden `Planting` carry `PlantRankName`). Console-parity follow-on **shipped** 2026-08-16 (lands on `/plants/orders`, species at `/plants/species`, taxonomic breadcrumb). M2f rollup + M4 ADR-020 namespaces + M5 N=1 collapse **shipped** 2026-08-16 (M4 also fixed a `DomainServiceScan` bean-name collision via FQN names). **Remaining:** M6 split `PlantLifeForm`→`GrowthHabit`+`LifeCycle` (decision recorded 2026-08-16). Gap review + next steps: [`plans/2026-08-16-plants-insects-gap-review.md`](plans/2026-08-16-plants-insects-gap-review.md). Plants briefing needs a rewrite (stale). |

---

## Conventions

- This file is a **dashboard**, not a synthesis. Each row points to its source-of-truth doc.
- The **current slice** section is the one row most often edited. The rest is reference.
- **Forks** (discoveries that derail the current slice) go to the [parking lot](notes/parking-lot.md) — not into this file or the strategy doc. Strategy updates only when the parking-lot answer is resolved.
- **Lighter slice plans for repetitive work.** Novel slices (new pattern, new kernel, contentious design) get full implementation plans. Slices that repeat an already-established pattern (e.g., per-organism rank corrections after one pilot landed) get a 1-paragraph note, not a 300-line doc.
