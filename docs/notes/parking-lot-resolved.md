# Parking Lot — Resolved

Entries that landed in code or in a strategy doc. Kept here (separate
from the active [`parking-lot.md`](parking-lot.md)) so grep can still find
the resolution context without inflating the live file.

When a new resolution lands, move the entry here from `parking-lot.md`
and keep its original `PL-N` ID. IDs never reuse.

---

## PL-1 — LifeStage modeling: dual-home problem

**Raised:** 2026-05-10 (mid-slice-2 execution); reinforced 2026-05-11 after Pat's overnight research.
**Resolved:** 2026-05-13.
**Where:** Discovered while applying the green-lacewing → chrysoperla data move; slice 1 had just spread the inline-life-stage pattern from `InsectSpecies` to `InsectGenus` and `InsectFamily`.
**The smell:** `LifeStage` is a `NamedEntity` with its own repository and standalone `life-stages.json`, but is *also* held by value as a component on `InsectSpecies` (annotated `@AggregateRoot`) and now on `InsectGenus` / `InsectFamily` (plain `NamedEntity`, not aggregates per the framework rules). Two homes for the same data; framework rule violation (NamedEntity owning NamedEntity by value).
**Resolution:** Closed by Phase 5 of [`plans/clades-kernel.md`](../plans/clades-kernel.md) (slice plan: [`plans/archive/clades-kernel/phase-5.md`](../plans/archive/clades-kernel/phase-5.md)). `InsectLifeStages.stagesOf(species|genus|family)` is now the canonical answer to "which stages exist" — it walks `placedIn` → `CladeTraversal.findTrait` → `MetabolyTrait` → `Metaboly.stages()`. The inline `egg`/`larva`/`pupa`/`adult` fields are still present on the records (Phase 5 was deliberately additive) and will be removed per-organism via PL-2.

---

## PL-11 — Cross-rank functional ecology

**Resolved:** 2026-05-19.
**Resolution:** Landed as `InsectFunctionalRole` (Entity, cross-rank by `InsectRankName`) per [`plans/archive/insect-functional-role.md`](../plans/archive/insect-functional-role.md). Commits `8f6072a` (entity stack), `bcce0e0` (16 seed records + smoke/contract tests), `2b0c38a` (cross-rank `getByGuild`), `327c5db` (strip `guilds`/`beneficial` from `InsectSpecies`, console fanout). `sightingNotes` rehoming explicitly deferred to the future sightings entity — two facts unique to the deleted potato-leafhopper record (dated crimson-clover observation, "first pest documented" sequencing) noted for that slice.

---

## PL-2 — Per-organism rank corrections (closed out 2026-05-24)

**Raised:** 2026-05-11. Pilot (green-lacewing) landed 2026-05-13.
**Resolved:** 2026-05-24 with the migration of the final two organisms (tachinid-fly + braconid-wasp).
**Resolution timeline:**
- Green-lacewing pilot — 2026-05-13.
- native-sweat-bee → halictus, grey-mining-bee → andrena — 2026-05-13.
- hoverfly → syrphidae, ground-beetle → carabidae, crane-fly → tipulidae, skipper-butterfly → hesperiidae — 2026-05-14.
- potato-leafhopper → empoasca — 2026-05-19 (Path A step 3, commit `05d430f`).
- tachinid-fly → tachinidae, braconid-wasp → braconidae — 2026-05-24. The fixture-migration blocker resolved by repointing `TachinidFly` / `BraconidWasp` references across `SpeciesRepositoryTest`, `SpeciesQueryImplTest`, `SpeciesCommandImplTest`, `ImageQueryImplTest`, `ImageCommandImplTest`, `InsectAggregateFactoryTest`, `LifeStageEntityRepositoryTest`, `LifeStageEntityQueryImplTest`, `InMemoryCatalogTest`, `InsectFunctionalRoleTest`, and `InsectsControllerWebMvcTest` to `BattusPhilenor` / `ColiasEurytheme` / `HippodamiaConvergens` (added the latter two as `TestInsectsIdentifiers` constants). `InsectsCatalogContributionTest.speciesWithoutGenus…` deleted — the under-identified-species code path is no longer reachable. Family-page species enrichment (commit `a81cccd`) reverted as vestigial — no under-identified species exist in the catalog any more.

**Outcome.** Every species record in the catalog is a real species — fully identified to genus + species + epithet, with typed `genusName` + `familyName` FKs to catalogued parents. Sets up the follow-up slice (Slice 2) to strip the redundant `TaxonomicClassification` from `InsectSpecies` and tighten the invariants to non-nullable.

---

## PL-14 — Rank-polymorphic `InsectAggregate`

**Raised:** 2026-05-22 (Phase 0 console-pages slice; user noted `InsectAggregate` only referenced `InsectSpecies`).
**Resolved:** 2026-05-23.
**Resolution:** `InsectAggregate` is now a sealed interface permitting `InsectFamilyAggregate`, `InsectGenusAggregate`, and `InsectSpeciesAggregate` — each a record composing its rank entity with the `ImageCollection`. The aggregate's identity is the root rank's typed slug, returned polymorphically via `InsectRankName name()`. `InsectAggregateQuery.getByName` widened to accept `InsectRankName`; `InsectAggregateFactory` switches on the rank-name permit and dispatches to the matching rank query (`SpeciesQuery` / `GenusQuery` / `FamilyQuery`). `InsectSubspeciesName` returns `Optional.empty()` — no subspecies entity exists yet, so the factory degrades gracefully until the entity lands. The console was unaffected — it composes its own view models from entity queries (the parking-lot note had flagged this). Followed plan [`plans/archive/pl-14-rank-polymorphic-insect-aggregate.md`](../plans/archive/pl-14-rank-polymorphic-insect-aggregate.md).

---

## PL-13 — Typed `InsectGenusName` / `InsectFamilyName` FK on `InsectSpecies`

**Raised:** 2026-05-22 (Phase 0 console-pages slice api audit).
**Resolved:** 2026-05-23.
**Resolution:** `InsectSpecies` gained `@Nullable InsectGenusName genusName` and `@Nullable InsectFamilyName familyName` components, validated via `entityNameOrNull` invariants. `SpeciesRepository` exposes typed `getByGenusName` / `getByFamilyName` (replacing the `getByGenusEpithet(TaxonomicGenus)` text stopgap); `SpeciesQuery` exposes `forGenusName` / `forFamilyName`. The console genus detail page now routes through the typed FK. JSON migration updated the 8 species records: `tachinid-fly` → `familyName: "tachinidae"`, `braconid-wasp` → `"braconidae"`, `battus-philenor` → `"papilionidae"`; the remaining 5 carry null on both fields because their parent genera/families are not yet catalogued. `InsectSpecies.belongsToGenus(TaxonomicGenus)` was removed (text-equality predicate retired alongside the stopgap; `TaxonomicClassification.belongsToGenus` remains for any future cross-rank caller). `SpeciesRepositoryTest` rewrote its contract cases to cover null-rejection, matching, and unknown-parent paths for both new methods.

---

## Q4 — Insects vs Apiary

**Resolved:** Pre-PL rename.
**Resolution:** Apiary is its own domain module (Colony aggregate root). Insects module covers Insecta (six legs). SHB control: H. indica only (not S. feltiae).

---

## PL-12 — Promote `parentName` to a typed component on `LifeStage`

**Raised:** 2026-05-20.
**Resolved:** 2026-06-07 (R6 of the rank-FK normalization effort).
**Resolution:** Implemented as option (a) — mirrored the `InsectImage.parentName` pattern verbatim, without waiting for the `LinneanRank` abstraction (the coupled question was not blocking). The `LifeStage` sealed interface and all four permits (`EggStage` / `LarvaStage` / `PupaStage` / `AdultStage`) gained a typed `InsectRankName parentName` as the second component, carrying the same `@JsonTypeInfo(property = "parentRank", include = As.EXTERNAL_PROPERTY)` + `@JsonSubTypes` dispatch `InsectImage` uses. The 60-record `life-stages.json` catalog migrated to carry `parentRank` + `parentName`; `LifeStageEntityRepositoryMock.getByParentName` now matches on class-qualified typed equality (rank-safe) instead of the slug substring, with argument validation and a full three-case contract test. The cross-encoding consistency invariant (`name.parentSlug().equals(parentName.value())`) was **not** added — only a null-check on `parentName`; pinning the two encodings together is folded into the still-deferred phase-3 conformance invariant. Landed in `730a218`; see [`plans/2026-06-07-insect-rank-fk-normalization-design.md`](../plans/2026-06-07-insect-rank-fk-normalization-design.md).
