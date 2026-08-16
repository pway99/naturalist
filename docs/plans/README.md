# Plans

Single index for every effort plan in the repo. Each plan is a durable,
section-by-section work tracker — the kind that survives across sessions
and lets a fresh contributor pick up cold from the document alone.

> **Broader dashboard.** For sketch-level efforts, paused pressure tests,
> and the cross-effort dependency view, see
> [`../work-tracker.md`](../work-tracker.md). That file is the
> single index of *every* active and recently-completed effort. This
> README is just the active plan-files subset.

When a plan is complete, move it to [`archive/`](archive/) and update its
row below. When a new effort starts, drop a file in this directory and add
a row.

## Reference

Not efforts — durable design references that outlive any single plan. Indexed
here because there is nowhere better and they are useless undiscovered.

| Reference                                                          | Scope                                                                                                                                                                                    |
|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [organism-domain-blueprint.md](organism-domain-blueprint.md)       | Decision index for any domain cataloguing organisms: rank-as-entity, confidence-bounded placement, cross-rank attachment, clade as an orthogonal axis, and the per-domain ladder rule. Points at `domains/insects/` as the reference implementation rather than restating it. |

## Active

| Plan                                                                   | Status                                 | Scope                                                                                                                                                              |
|------------------------------------------------------------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [admin-console.md](admin-console.md)                                   | active — view 4+ pending               | Secure `/admin/**` surface in `apps/management-console`. Views 1–3 (`resilience`, `domain-services`, `catalog`) shipped.                                           |
| [catalog-kernel.md](catalog-kernel.md)                                 | active — M9b / M10 / M11 / M12 pending | Cross-domain reference resolution kernel. Search + inverse-routing + URL-linker SPIs. The 2026-04-29 search-and-discovery redirect is folded into this single doc. |
| [clades-kernel.md](clades-kernel.md)                                   | active — Phases 1–5 ✅; Phase 6 plants deferred | Curated evolutionary tree of life as a sealed-type vocabulary kernel. Phase slice plans archived under [`archive/clades-kernel/`](archive/clades-kernel/). |
| [command-framework.md](command-framework.md)                           | active — pilot landed; follow-ups      | Pilot shipped 2026-05-09 (`482b48d`): `EntityCommand` kernel + insects-only adapters. Remaining: console controller wiring, second-domain rollout (chemistry), aggregate-level commands. |
| [2026-07-14-insect-add-photo-command.md](2026-07-14-insect-add-photo-command.md) | active — not started | Extract `addImage()` coordination (optional FieldObservation + InsectImage) into `InsectAddPhotoCommand` in `insects-core`. |
| [2026-07-14-insect-hierarchical-image-query.md](2026-07-14-insect-hierarchical-image-query.md) | active — not started | Move recursive image-aggregation from console controller into `ImageQuery.forRankHierarchy()` core query. |
| [identification.md](identification.md)                                 | active — Phase 0 ✅; Phase 1 ✅; Phase 2 citation/library ✅; Phase 2 console ✅; citation association ✅ | Multi-phase identification roadmap. Phase 0 reorganization + console UI; Phase 1 external-source seam; Phase 2 citation/library (renamed from `kernels/bibliography`); Phase 3 FU-1 path-1 confirmation; Phase 4 EOL adapter. |
| [insect-aggregate.md](insect-aggregate.md)                             | sketch — R7 half-done; R2/R3/R4/R5 open | `Insect` top-level aggregate composing rank chain, images, life stages, citations, features. Sketch-level direction; per-phase slice plans when predecessor lands. |
| [runtime-data-persistence.md](runtime-data-persistence.md)             | active — implemented; awaiting smoke   | Origin-tracked write-back on `TestEntitySource`: console writes update canonical JSON in source tree; classpath heuristic locates the write target; `static final` flag keeps tests no-op. Implemented 2026-05-09; verified by manual console smoke. |
| [vision-assisted-identification.md](vision-assisted-identification.md) | sketch — deferred (long-horizon)       | Console / web-driven Claude Vision identification: image → preprocess → tool-call → typed `IdentificationDraft<T>` → review → `EntityCommand.insert`. Revisit after manual entry has been daily workflow for weeks. |

## Archived

Completed efforts that no longer drive ongoing work. Kept for context;
not loaded by default.

| Plan                                                                                              | Completed  | Scope                                                                                                                                             |
|---------------------------------------------------------------------------------------------------|------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| [archive/chemistry-taxonomy-refactor.md](archive/chemistry-taxonomy-refactor.md)                  | 2026-04-26 | Split `CompoundType` into `ChemicalNature` × `PhysicalForm` × `Set<FunctionalRole>`. Phase 13 followups also captured.                            |
| [archive/runtime-architecture-refactor.md](archive/runtime-architecture-refactor.md)              | 2026-05-03 | Atlas → catalog rename, open `DomainId`, `apps/` and `adapters/` trees, `Resilience` facade, `@DomainService`, ArchUnit gate. M0–M10 all shipped. |
| [archive/paged-queries-plan.md](archive/paged-queries-plan.md)                                    | 2026-05-09 | Paged-query contract for repository ports; landed as MyBatis-friendly cursor shape with thin in-memory probe. Contract reused by all `findPage` consumers. |
| [archive/fu-1-plan.md](archive/fu-1-plan.md)                                                      | 2026-05-10 | FU-1 family/genus catalog tiers. PR-1 / PR-2a–e shipped; PR-2f / PR-2g / PR-3 folded into the identification roadmap (per Pat's 2026-05-10 reframing). |
| [archive/clades-kernel/phase-1.md](archive/clades-kernel/phase-1.md)                              | 2026-05-12 | Stand up the `clades` kernel as sealed-type vocabulary: Eukaryota → Animalia → Arthropoda → Insecta → Holometabola → Lepidoptera → Papilionidae. |
| [archive/clades-kernel/phase-2.md](archive/clades-kernel/phase-2.md)                              | 2026-05-12 | Define `Metaboly` trait and attach to Holometabola.                                                                                              |
| [archive/clades-kernel/phase-3.md](archive/clades-kernel/phase-3.md)                              | 2026-05-13 | Add `placedIn: Clade` to taxon entities; per-record opt-in placements.                                                                           |
| [archive/clades-kernel/phase-4.md](archive/clades-kernel/phase-4.md)                              | 2026-05-13 | First catalog placements (*Battus philenor*) — clade DAG fanout from insect records.                                                             |
| [archive/clades-kernel/phase-5.md](archive/clades-kernel/phase-5.md)                              | 2026-05-13 | `InsectLifeStages.stagesOf(...)` resolver routing life-stage queries through clade traversal. Closes PL-1.                                       |
| [archive/green-lacewing-rank-correction.md](archive/green-lacewing-rank-correction.md)            | 2026-05-13 | Original pilot plan for the green-lacewing → chrysoperla rank correction. Pilot landed; doc predates clades-kernel Phase 5 and overstates the work (see PL-2). |
| [archive/insect-image-parent-rank.md](archive/insect-image-parent-rank.md)                        | 2026-05-19 | Retype `InsectImage.parentName` from `InsectSpeciesName` to `InsectRankName`; Steps 0–8 (Path A) landed.                                          |
| [archive/insect-functional-role.md](archive/insect-functional-role.md)                            | 2026-05-19 | `InsectFunctionalRole` entity stack (cross-rank by `InsectRankName`); strip `guilds`/`beneficial` from `InsectSpecies`. Closes PL-11. |
| [archive/insects-family-genus-console.md](archive/insects-family-genus-console.md)                | 2026-05-23 | Phase 0 UI: `/insects/families` + `/insects/genera` list/detail pages, `GenusQuery.forFamilyName` typed-FK gap, `SpeciesQuery.forGenusEpithet` text stopgap, `InsectsLinker` family/genus routing. Raised PL-13 + PL-14. |
| [archive/genus-family-life-stages.md](archive/genus-family-life-stages.md)                        | 2026-05-13 | Slice 1 of identification roadmap Phase 0: extend `InsectGenus` / `InsectFamily` records with optional life-stage components. |
| [archive/insect-order-design.md](archive/insect-order-design.md)                                  | 2026-05-25 | Design spec for `InsectOrder` entity: order-rank + genus/family refactoring to drop local epithet copies and use typed `InsectOrderName` FKs. |
| [archive/insect-order-phase1-plan.md](archive/insect-order-phase1-plan.md)                        | 2026-05-25 | Phase 1: refactor `InsectGenus` — drop `TaxonomicOrder`/`TaxonomicFamily` local copies, update `LinnaeanGenus` kernel, console resolves parent families. |
| [archive/insect-order-phase2-plan.md](archive/insect-order-phase2-plan.md)                        | 2026-05-25 | Phase 2: introduce `InsectOrder` entity, `InsectOrderName` identifier, `LinnaeanOrder` kernel, refactor `InsectFamily`/`InsectGenus` FKs, console order pages, catalog contribution. |
| [archive/console-clade-context-design.md](archive/console-clade-context-design.md)                | 2026-05-25 | Design spec for clade lineage trail and Class Insecta landing description on insects console pages. |
| [archive/console-clade-context-plan.md](archive/console-clade-context-plan.md)                    | 2026-05-25 | Clade context implementation: `.clade-lineage` CSS, `nav.jte` lineage trail (Animalia › Arthropoda › Insecta), Class Insecta description on landing page. |
| [archive/2026-05-24-insect-page-images.md](archive/2026-05-24-insect-page-images.md)              | 2026-05-25 | Image carousels on all insect listing/detail pages: `BehavioralMap` kernel type, `ImageGallery`, `cardImages.jte` component, hierarchy image helpers. |
| [archive/2026-05-24-insect-page-images-design.md](archive/2026-05-24-insect-page-images-design.md) | 2026-05-25 | Design spec for insect page images: `BehavioralMap` kernel, `ImageGallery`, carousel component, hierarchy-walking controller helpers. |
| [archive/2026-05-25-breadcrumb-primitive.md](archive/2026-05-25-breadcrumb-primitive.md)          | 2026-05-25 | Taxonomic-scope breadcrumb primitive: `BreadcrumbSegment` record, `breadcrumb.jte` template, unified lineage trail on all insect console pages. |
| [archive/clades-kernel-phase-5b-life-stage-inline-removal.md](archive/clades-kernel-phase-5b-life-stage-inline-removal.md) | 2026-05-31 | Phase 5b umbrella: life-stage inline removal + resolver walk-up. PRs 1+2 landed; PR 3 cancelled. |
| [archive/clades-kernel-phase-5b-pr1-resolver-walkup.md](archive/clades-kernel-phase-5b-pr1-resolver-walkup.md) | 2026-05-31 | Phase 5b PR 1: `InsectLifeStages.stagesOf(species, genus, family, order)` resolver walk-up overload. |
| [archive/clades-kernel-phase-5b-pr2-detail-page-migration.md](archive/clades-kernel-phase-5b-pr2-detail-page-migration.md) | 2026-05-31 | Phase 5b PR 2: species-detail-page migration to `LifeStage` repository + inline field drop from all four rank records. |
| [archive/2026-06-03-typed-constraint-methods.md](archive/2026-06-03-typed-constraint-methods.md)  | 2026-06-03 | Add `aggregate`, `aggregateOrNull`, `behavioralCollection`, `whenNotNull` methods to `Constraints`. |
| [archive/2026-06-07-insect-rank-fk-normalization-design.md](archive/2026-06-07-insect-rank-fk-normalization-design.md) | 2026-06-07 | Design spec for insect rank-FK normalization: parent-only FKs (R8), trimmed invariants (R1), typed `LifeStage.parentName` (R6). |
| [archive/2026-06-07-insect-rank-fk-normalization-plan.md](archive/2026-06-07-insect-rank-fk-normalization-plan.md) | 2026-06-07 | Implementation plan for rank-FK normalization. Resolves PL-12. |
| [archive/2026-06-07-readmodel-kernel-type-design.md](archive/2026-06-07-readmodel-kernel-type-design.md) | 2026-06-07 | Design spec for `ReadModel` as the 6th identity-model marker in `kernels/framework`. |
| [archive/2026-06-07-readmodel-kernel-type-plan.md](archive/2026-06-07-readmodel-kernel-type-plan.md) | 2026-06-07 | Implementation plan for `ReadModel` kernel type + insect read-model retype (`*Aggregate` → `*View`/`InsectTaxonView`). |
| [archive/2026-06-09-external-authority-phase1-design.md](archive/2026-06-09-external-authority-phase1-design.md) | 2026-06-09 | Design spec for `kernels/authority` port + EOL mock client (identification Phase 1). |
| [archive/2026-06-09-external-authority-phase1-implementation.md](archive/2026-06-09-external-authority-phase1-implementation.md) | 2026-06-09 | Implementation plan for external-authority Phase 1: `AuthorityReference`, `AuthoritySource`, `ExternalAuthority` port, `EolPageId`, EOL mock client. |
| [archive/2026-06-13-citation-and-library-design.md](archive/2026-06-13-citation-and-library-design.md) | 2026-06-13 | Design spec for `Citation` sealed NamedEntity in `kernels/authority` + `domains/library` domain stack. Naming: `kernels/bibliography` → `kernels/authority`, `LiteratureReference` → `Citation`. |
| [archive/2026-06-13-citation-and-library-plan.md](archive/2026-06-13-citation-and-library-plan.md) | 2026-06-13 | Implementation plan for citation + library: 11 tasks covering kernel types, domain scaffolding, repository stack, EOL factory, docs. |
| [archive/2026-06-13-paraphyly-fixtures.md](archive/2026-06-13-paraphyly-fixtures.md) | 2026-06-13 | Paraphyly teaching-exemplar fixtures: 9 clade permits, 3 lineages (Drosophila genus paraphyly, Blattodea/termites, bees-in-wasps), monotonicity acceptance test, EOL citations. |
| [archive/2026-06-13-clades-taxonomy-console-design.md](archive/2026-06-13-clades-taxonomy-console-design.md) | 2026-06-14 | Design spec for clade browser + concept pages + cross-links on insects console pages. |
| [archive/2026-06-13-clades-taxonomy-console-plan.md](archive/2026-06-13-clades-taxonomy-console-plan.md) | 2026-06-14 | Implementation plan for clades/taxonomy console teaching surface: `Concept` entity, `CladeCatalog`, controllers, JTE templates, nav cross-links. |
| [archive/2026-06-14-citation-association-design.md](archive/2026-06-14-citation-association-design.md) | 2026-06-25 | Design spec for cross-domain citation associations via `CitationAssociation` entity + insect-specific hierarchical citation discovery via `InsectCitationQueryImpl`. |
| [archive/2026-06-14-citation-association-plan.md](archive/2026-06-14-citation-association-plan.md) | 2026-06-25 | Implementation plan for citation association: `CitationAssociation` entity, `CitationAssociationQuery`, `InsectCitationQueryImpl`, `InsectAncestryResolver` extraction. |
| [archive/2026-06-14-insect-citation-rendering.md](archive/2026-06-14-insect-citation-rendering.md) | 2026-06-25 | Implementation plan for insect citation rendering: `InsectFactory`, `Insect` read model with `InsectCitationView`, detail page citation links. |
| [archive/2026-06-14-insect-feature-api-plan.md](archive/2026-06-14-insect-feature-api-plan.md) | 2026-06-25 | Predecessor plan for `InsectFeature` — proposed `IdentificationFeatures` as embedded VO on rank entities. Superseded by the entity-design spec. |
| [archive/2026-06-14-library-console-module.md](archive/2026-06-14-library-console-module.md) | 2026-06-14 | Extract `CladesController` + `ConceptsController` from management-console into `domains/library/library-console` module. |
| [archive/2026-06-25-insect-feature-entity-design.md](archive/2026-06-25-insect-feature-entity-design.md) | 2026-06-25 | `InsectFeature` promoted to independent `Entity<InsectFeatureId>` with many-to-many `InsectFeatureAssignment`; lineage-composite `InsectFeatureView`; `InsectAncestryResolver` extracted. Supersedes the 2026-06-14 API plan. |
| [archive/2026-06-29-dual-strategy-breadcrumb.md](archive/2026-06-29-dual-strategy-breadcrumb.md) | 2026-07-05 | Two-row phylogenetic/Linnaean clade breadcrumb; `library-api` gains `CladeStep`/`CladeView`/`CladeQuery`, `library-core` owns `CladeRanks` + `CladeViewFactory`. Design `8eaadd5`. |
| [archive/2026-07-02-insect-console-navigation.md](archive/2026-07-02-insect-console-navigation.md) | 2026-07-05 | Insect-console streamline: drop the rank tab bar, add the phylogenetic clade trail with a "not yet placed" research affordance (`InsectCladeAnchors`, `/concepts/placing-clades`). |
| [archive/2026-07-05-clade-rank-catalog-bridge.md](archive/2026-07-05-clade-rank-catalog-bridge.md) | 2026-07-05 | Two-axis clade links: rank eyebrows resolve into the insects catalog via the new `Catalog.findBySlug` seam + `InsectsLinker` order case + `CladeRankLinks`; names stay on `/clades/{slug}`. |
| [archive/2026-07-12-naturalist-insect-collection-design.md](archive/2026-07-12-naturalist-insect-collection-design.md) | 2026-07-12 | Design spec for naturalist collection: `FieldObservation` entity, collection lens, header filter chip, per-naturalist photo ownership. |
| [archive/2026-07-12-naturalist-insect-collection-plan.md](archive/2026-07-12-naturalist-insect-collection-plan.md) | 2026-07-12 | Implementation plan for naturalist collection feature. |
| [archive/2026-07-14-insect-record-helpers.md](archive/2026-07-14-insect-record-helpers.md) | 2026-07-14 | `InsectRankName.of(slug, rank)` static factory, `FieldObservation.withNotes`/`.withSubject` mutation helpers, controller simplification. |

## Where else to look

Decisions and questions that don't fit the milestone shape live next door
in [`../`](..):

- [`../work-tracker.md`](../work-tracker.md) — single dashboard for every effort (active, sketch, paused, recently-completed).
- [`../notes/pending-implementation.md`](../notes/pending-implementation.md) — prioritized backlog of work not yet
  planned.
- [`../notes/parking-lot.md`](../notes/parking-lot.md) — forks and design decisions in flight (the single landing site when a discovery diverts current work).
- [`../briefings/`](../briefings/) — chat-prompt context documents (not plans).
- [`../adr/`](../adr/) — Architecture Decision Records.

## Conventions

- One plan per file. Don't split a coherent effort across multiple plans;
  do split a milestone if it grows mid-execution (per the "How to resume
  across sessions" pattern in the existing plans).
- Status in this index is one of: **active**, **paused**, **superseded**, **archived**.
- Each plan opens with a short narrative, then declarative architectural
  decisions, then numbered milestones with a `**Status:**` line per milestone.
- Update the row in this index whenever a plan's status changes.
- Multi-phase efforts group their phase slice plans into a subdirectory
  under `archive/<effort>/` once the phases land (see
  [`archive/clades-kernel/`](archive/clades-kernel/) for the template).
