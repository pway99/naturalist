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

---

## PL-17 — Raw uploads with JavaScript off keep EXIF/GPS and are served publicly with one-year caching

**Raised:** 2026-11-01.
**Resolved:** 2026-11-09.
**Where:** Surfaced in the whole-branch review of insects-ports slice 1 (image store). The add-photo and identify forms resize the photo client-side before upload, which re-encodes it and drops its metadata — but only when JavaScript runs. With JavaScript off (or a direct POST) the original bytes are stored as-is by `ImageStore.store`, EXIF and GPS coordinates included, and `GET /insects/images/{filename}` serves them to anyone with `Cache-Control: public, max-age=31536000, immutable`. The slice-1 design bans server-side transcoding, so the server never strips anything. The MVP goal says images stay on the user's device; a raw upload breaks that promise silently, and the one-year cache means a later fix cannot recall copies already cached.
**Resolution:** Option (a), taken further: strip at the use case and again at the store, and keep a coarse location instead of none. `ImageUpload.withoutMetadata()` is a metadata-only rewrite (no transcoding) that drops EXIF, XMP, IPTC and comment segments and keeps only the EXIF Orientation, so the image still displays upright. A JPEG keeps only `APP0` (JFIF), `APP2` (ICC) and `APP14` (Adobe) of its application segments: every other `APPn`, including the `APP11` JUMBF/C2PA content credentials recent phones write (which can embed Exif again), is dropped. Bytes appended after the image's end (a phone's trailer, a motion photo's MP4) are dropped too, and the JPEG walk skips fill bytes and resynchronises past junk the way a decoder does, so nothing placed before an Exif segment can hide it from the strip. The insects commands strip every upload before it goes anywhere, and `FilesystemImageStore.store` applies the same strip, so every image a store writes is clean whatever the caller does (JavaScript off or a direct POST included). The 18 seed photos were rewritten without metadata. Location the app wants for identification is kept as `GridCell`, a 2.5 arc-minute cell, never as coordinates: `OrganismObservation.area` persists the cell as two nullable indices, and an ArchUnit rule (`LocationPrivacyComplianceTest`) bars `Coordinates` from any persisted component. Tests pin the strip at the command, at the store and on every classpath seed photo (`SeedImagesCarryNoMetadataTest`). Design: [`plans/2026-11-08-photo-location-privacy-design.md`](../plans/2026-11-08-photo-location-privacy-design.md); plan: [`plans/2026-11-08-photo-location-privacy-plan.md`](../plans/2026-11-08-photo-location-privacy-plan.md).

---

---

## PL-16 — Image reads share the resilience timeout pool with vision and catalog fan-out

**Raised:** 2026-11-01.
**Resolved:** 2026-11-10 (launch item 1.6).
**Where:** Surfaced in the whole-branch review of insects-ports slice 1 (image store). `FilesystemImageStore` runs every `store` and `read` under `resilience.timeout("image.store")`, and `Resilience4jTimeout` schedules every named timeout on one shared `SCHEDULER` executor. A rank or gallery page that serves many large photos therefore competes for the same threads as `vision.identification` and the `catalog.fanout` provider calls: a burst of slow image reads can delay or starve the catalog fan-out's timeouts, so a page full of photos degrades search and back-references that have nothing to do with images.
**Blocking:** No. Local use serves a handful of photos per page; the contention needs concurrent load.
**Resolution path:** Before hosting, give `image.store` a bounded or separate executor — either a per-strategy executor in the resilience4j adapter (`ResilienceConfig` names the pool) or a dedicated bounded pool for blocking I/O strategies — and size it so a gallery cannot exhaust what `catalog.fanout` needs. Measure with a gallery-sized concurrent read before choosing.
**Update 2026-11-03:** the shared fixed pool is gone. `Resilience4jTimeout` now runs each timed call on its own virtual thread (ADR-027 PR 5, because `challenge.verify` would have queued on the same pool), so one strategy no longer spends another's deadline. What remains of this entry: nothing caps concurrent `image.store` work except its callers, so measure a gallery-sized read before hosting and add a bulkhead if it needs one.
**Measured 2026-11-10 (launch item 1.6):** the packaged jar at the production heap cap (`-Xmx1152m`, 75% of the 1536 MB container) against the standing test database, with `ab` and parallel `curl` on `/insects/images/{name}`. `read` loads the whole file into the heap, and `store` strips metadata but does not downscale, so a served photo can be as large as `ImageUpload.MAX_BYTES` (20 MB).

| Photo | Concurrent | Result |
|---|---|---|
| 2.9 MB (phone-sized) | 10 / 50 / 100 / 200 | all served; ~1,070 req/s at 100+; p50 175 ms, p99 510 ms at 200 |
| 20.4 MB (near the cap) | 10 / 50 | all served; p50 233 ms at 50, heap peaked at 979 MB of 1,152 MB |
| 20.4 MB | 100 | **`OutOfMemoryError`**: it killed Tomcat's poller thread and the app stopped serving every request, pages included, until restarted |

Pages kept working under the 50-reader load (`/insects/orders` p50 9 ms unchanged, p95 22 → 249 ms), so CPU and thread contention is not the problem; heap is. **Verdict: a cap is needed.** Fewer than 100 concurrent reads of large uploads, a few users opening galleries at once, take the whole site down. Two side findings: the seed-photo jars (`*-repository-test`) are not in the packaged app, so the store's classpath fallback never serves a seed photo there (production relies on `db-init` copying them into the image volume, which it does); and `ab` reports spurious "Length" failures on large bodies, which parallel `curl` did not reproduce.

**Resolution:** heap was the problem, and buffering was its cause. Two steps:

1. The classpath fallback went first (it was dead in the packaged jar, and photos are data, not static content): `read` serves only the image root, which `db-init` fills.
2. An `image.store` bulkhead (16 permits, 1 s wait) alone did **not** fix it. Re-measured at 100 concurrent 20 MB reads, the app still threw ~250 `OutOfMemoryError`s and answered 500s: the permit covers the read, but the byte array lives on while Tomcat writes it to a slow client. So `read` no longer returns bytes. `StoredImage` is now `(mediaType, size, Content content)`; the store stats the file and sniffs 12 bytes, and the consoles stream `content()` as an `InputStreamResource` with `Content-Length`. The bulkhead stays, capping concurrent lookups; a full one gives an uncached 503 with `Retry-After: 1`.

| 20.4 MB photo, `-Xmx1152m` | Concurrent | Result |
|---|---|---|
| Buffered, bulkhead | 100 / 200 | 46 / 173 × 500 (OOM), 83 × 503 at 200; heap 1,151 MB |
| Streamed, bulkhead | 50 / 100 / 200 | 150 / 300 / 600 × 200, no errors; heap peak 154 MB |

During 200 concurrent large downloads, a phone-sized photo served at p50 63 ms / p99 195 ms and `/login` at p50 1 ms.
