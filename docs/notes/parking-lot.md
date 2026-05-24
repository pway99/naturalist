# Parking Lot

Short entries (~5 lines each) for forks discovered mid-work and decisions in flight. Not loaded into conversation context — reference explicitly when working the relevant question. Entries move to [`parking-lot-resolved.md`](parking-lot-resolved.md) when the answer lands in code or a strategy doc.

**Shape per entry:** *what surfaced* / *when raised* / *where in the work it came up* / *blocking the current slice yes/no* / *resolution path if known*.

---

## PL-2 — Per-organism rank corrections (green-lacewing pilot landed 2026-05-13)

**Raised:** 2026-05-11.
**Where:** Per-organism data reorganization for Phase 0 of the identification roadmap.
**Pilot status:** Green-lacewing landed 2026-05-13. The `green-lacewing` `InsectSpecies` record was deleted; the four `green-lacewing-*` standalone `LifeStage` records were renamed to `chrysoperla-*`; the `chrysoperla` `InsectGenus` record gained `"placedIn": "holometabola"` so `InsectLifeStages.stagesOf(...)` resolves its stage list via the clade traversal. No inline life-stage fields were added to the genus record — Phase 5 made that workaround obsolete.
**Remaining work (each a separate per-organism slice):**
- ~~native-sweat-bee → halictus (genus)~~ landed 2026-05-13
- ~~grey-mining-bee → andrena (genus)~~ landed 2026-05-13
- ~~hoverfly → syrphidae (family)~~ landed 2026-05-14
- ~~ground-beetle → carabidae (family)~~ landed 2026-05-14
- ~~crane-fly → tipulidae (family)~~ landed 2026-05-14
- ~~skipper-butterfly → hesperiidae (family)~~ landed 2026-05-14 (placed at `lepidoptera`, more specific than the others)
- **potato-leafhopper → empoasca (genus)** — *not bundled.* Two blockers: (1) Cicadellidae is Hemiptera/Hemimetabolous, no kernel permit declares that trait, so any `placedIn` choice would either resolve to wrong stages (`holometabola`) or empty (`insecta`). (2) `PotatoLeafhopper` is a test fixture in TestInsectsIdentifiers carrying `Images.Img9047`/`Img9048` constants tied to records in `insect-images.json`, used across image command/query tests. Resolution requires either adding a `Hemimetabolous` trait permit (kernel work) or accepting empty stage resolution; image records and their consumers need repointing or deletion.
- **tachinid-fly → tachinidae (family)** — *not bundled.* `TachinidFly` is the canonical species fixture in `SpeciesRepositoryTest`, `SpeciesCommandImplTest`, `SpeciesQueryImplTest`, `LifeStageEntityQueryImplTest`, `LifeStageEntityRepositoryTest`, and `InMemoryCatalogTest` (`knownEntityNames()`, life-stage lookup assertions, cross-domain catalog ref). Migrating means picking replacement fixtures (likely battus-philenor) across all those test classes — a separate refactor with explicit scope, not a 5-line JSON edit.
- **braconid-wasp → braconidae (family)** — *not bundled.* Same fixture-migration shape as tachinid-fly: appears alongside it in the `knownEntityNames()` lists across multiple test classes.

Clean recipe (applies to the 6 landed organisms): delete the under-identified `InsectSpecies` record, rename its four standalone `LifeStage` records to the parent-rank composite slug, add `"placedIn": "..."` to the destination genus/family record so the resolver works. Each clean move is a focused JSON edit + one resolver test. The original plan at `docs/plans/archive/green-lacewing-rank-correction.md` predates Phase 5 and overstates the work — there is no inline-life-stage merge step.

---

## PL-3 — Aggregate × CatalogEntity / Entity composition ADR

**Raised:** Pre-Phase-1b (rolled forward from old `Q0`).
**Where:** Identity model — `Aggregate` sits orthogonal to `NamedEntity` / `Entity`. A `Colony` (Apiary) or `NaturalistJournal` is an Aggregate (consistency boundary) but may also be a `CatalogEntity` (stable, named, referenced).
**Blocking:** Not currently — no aggregate-shaped work is in flight on the path. Becomes relevant if PL-1's resolution promotes `InsectSpecies` / `InsectGenus` / `InsectFamily` to true `Aggregate`.
**Resolution path:** ADR documenting how `Aggregate`, `CatalogEntity`, and `Entity` compose, before any aggregate root is implemented.

---

## PL-4 — IrrigationEvent repository adapter shape

**Raised:** Pre-Phase-1b (rolled forward from old `Q1`).
**Where:** Soil/Sensor backlog (no active slice).
**Blocking:** No.
**Resolution path:** Decide between PostgreSQL standard table, TimescaleDB hypertable, or separate time-series store. In-memory adapter is sufficient until RDBMS work begins.

---

## PL-5 — Elemental Sulfur amendment type

**Raised:** Pre-Phase-1b (rolled forward from old `Q2`).
**Where:** Soil chemistry domain; placeholder currently uses potassium sulfate.
**Blocking:** Blocks nitrogen status computation (PL-6).
**Resolution path:** Add a dedicated `ELEMENTAL_SULFUR` amendment type before that computation lands.

---

## PL-6 — Nitrogen status computation

**Raised:** Pre-Phase-1b (rolled forward from old `Q3`).
**Where:** Soil domain.
**Blocking:** No (no active soil work).
**Resolution path:** Needs biological amplification factor from Zone — cross-module dependency mediated through a `ZoneService` port.

---

## PL-7 — Plants growth form vs category

**Raised:** Pre-Phase-1b (rolled forward from old `Q5`).
**Where:** Plants domain.
**Blocking:** No.
**Resolution path:** `GrowthForm` enum (tree/vine/etc.); proposed categories — FruitTree, FruitVine, VegetableCrop, CoverCrop, OrnamentalWoody, PollinatorPlant. `PlantRole` as many-to-many. `SeedLineage` for the Italian Pear adaptation program. Entity model sketched, no Java written.

---

## PL-8 — Custom WH51 calibration

**Raised:** Pre-Phase-1b (rolled forward from old `Q6`).
**Where:** Sensor domain.
**Blocking:** No.
**Resolution path:** Factory calibration is for mineral soil; Oak Vista worm casting/coco coir blend reads 3–5% high. AD values usable for custom calibration curve. Deferred pending gravimetric correlation study.

---

## PL-9 — Event-based production adapter

**Raised:** Pre-Phase-1b (rolled forward from old `Q7`).
**Where:** Cross-domain (event sourcing question).
**Blocking:** No.
**Resolution path:** Append-only PostgreSQL tables for `AmendmentEvent`, `SensorReading`, `IrrigationEvent` as a middle path. Deferred; hexagonal architecture makes this a swap of adapters.

---

## PL-10 — `with*` mutator observability pattern

**Raised:** 2026-05-12.
**Where:** Surfaced while threading `namedEntityOrNull` through `InsectFamily`. Piloted on `InsectFamily.withEgg` in commit `150009d`:

```java
public InsectFamily withEgg(@Nullable EggStage value) {
    observer.arguments("withEgg", i -> i.namedEntityOrNull(value, "value"))
            .throwWhenInvalid();
    return new InsectFamily(name, …, value, …);
}
```

**The smell.** Today's `with*` mutators are plain constructors — they accept whatever the caller passes and return a new record. An invalid child entity (e.g. a `PupaStage` with a stale enum string from a downstream deserialiser) flows through `family.withPupa(stage).withEgg(…)` and only surfaces at the next insertion site, or worse, never. The InsectFamily pilot validates input at the mutation boundary instead, throwing `InvariantViolationException` immediately and surfacing the failure on the observer-framework dashboard.

**Blocking:** No. Phase 3 lands cleanly without it.

**Why it may earn its keep, despite the cost:**

- Each `with*` adds ~3 lines of Observer.arguments boilerplate.
- Each pattern adoption requires at least one new test confirming invalid input throws (and that valid input — including `null` for nullable fields — passes through).
- BUT — adoption gives **per-mutation control and awareness**: you know exactly when and where an object got into a bad shape, which is a real diagnostic win. The pattern's analogue in Pat's day-job framework caught a production bug (DB returned a String with extra whitespace that failed to deserialise to an enum); the dashboard metric pinpointed it in minutes where boundary-only validation would have surfaced it as a downstream EntityNotFoundException with no breadcrumb.

**Resolution path (when revisited):**

1. Decide scope: just the holometabolous-stage `with*` family across InsectFamily / InsectGenus / InsectSpecies, or every `with*` on every record project-wide?
2. Settle the Observer-source question. Today each pilot adds `private static final Observer observer = Observer.forClass(X.class);`. Acceptable; or possibly a thread-local / injected observer if dashboard metrics need consumer routing.
3. Write a "withFoo preserves Bar" round-trip test per mutator at the same time (catches the *other* silent failure — a `with*` method that drops an unrelated field, which the Lombok-`@With` discussion identified as an ongoing tax of the no-Lombok rule).

---

## PL-12 — Promote `parentName` to a typed component on `LifeStage`

**Raised:** 2026-05-20.
**Where:** Surfaced while landing the LifeStage rank-polymorphism widening (mirror of the image-parent-rank slice). The query API now accepts `InsectRankName`, but `LifeStage` itself carries no typed parent — the parent rank is encoded only in the `LifeStageName` slug prefix, and `LifeStageName.parentSlug()` returns `String` because the slug carries no rank discriminator. `InsectImage` does it cleanly (typed `parentName: InsectRankName` component, polymorphic JSON via `@JsonTypeInfo`) because it is `Entity<InsectImageId>` (surrogate identity) where parent reference is a separate field. `LifeStage` is `NamedEntity<LifeStageName>` (natural-key identity), so the parent is double-encoded if promoted: once in the slug, once typed.

**Blocking:** No. The query-widening slice landed on the slug-string path and is sufficient for current consumers.

**Why it may earn its keep:**
- Honest type-recovery — `lifeStage.parentName(): InsectRankName` instead of consumer-known-only rank.
- Symmetry with `InsectImage` (same parent-rank shape across image and life-stage records).
- Catches the "rank changed without slug update" class of bug at the boundary (e.g., a record migrated from genus to species rank where only one of the two encodings was updated).

**Cost:**
- Schema change across four sealed subtypes (`EggStage` / `LarvaStage` / `PupaStage` / `AdultStage`).
- JSON migration on ~70 records: add `"parentName": "<slug>"` + polymorphic envelope `"parentRank": "GENUS"|"FAMILY"|"SPECIES"|"SUBSPECIES"`.
- New invariant pinning `name.parentSlug().equals(parentName.value())` — the two encodings must stay consistent.

**Coupled question (deferred to inform):** A parallel session is working on a `LinneanRank` abstraction. If that lands a typed rank discriminator at the framework/identifiers level, the polymorphic JSON envelope and the `name`↔`parentName` consistency invariant may have a cleaner shape than the per-record `@JsonTypeInfo` repetition. Hold PL-12 until that direction is known.

**Resolution path (when revisited):** Decide after `LinneanRank` lands. Then either (a) mirror the `InsectImage` pattern verbatim with `@JsonTypeInfo` per consumer field, or (b) use whatever the LinneanRank abstraction yields. Either way: schema + JSON migration + invariant.

---

## PL-14 — Rank-polymorphic `InsectAggregate`

**Raised:** 2026-05-22 (during Phase 0 console-pages slice; user noted `InsectAggregate` only references `InsectSpecies`).

**Where:** `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectAggregate.java` — record carries `(InsectSpecies species, ImageCollection images)`; rooted at `InsectSpeciesName`. `InsectAggregateFactory` assembles species + images by name.

**The smell.** `InsectImage.parentName()` is already `InsectRankName` (Path A landed — images can attach to family, genus, or species). But the aggregate that exists to present "an insect at Oak Vista" hardcodes species rank. Family- and genus-rank under-identified organisms have no aggregate representation; the catalog-view machinery (`insectQuery.insect().getByName(...)`) silently doesn't apply to them. The console papers over this by composing its own view models from entity queries — fine for now, but Phase 2's identification workflow expects an aggregate at the session's current rank.

**Shape options.**

1. Sealed `InsectAggregate` with permits `InsectFamilyAggregate`, `InsectGenusAggregate`, `InsectSpeciesAggregate` — pattern-match at consumers. Discoverable; clean visitor semantics.
2. Three parallel records, no shared supertype — simpler today, more duplication. Mirror of the entity records themselves.
3. Generic `InsectAggregate<ROOT extends InsectRankName, ENTITY>` — most compact but loses the per-rank semantic distinctions Phase 2 likely wants on its scope value object.

**Resolution path.** Decide between (1) and (2) when Phase 2 starts (the workflow's session-scope value object will inform which shape is ergonomic). Until then the console composes from entity queries; this is fine.

**Blocking:** Not currently — entity queries cover the console; Phase 1's mock seam doesn't need a polymorphic aggregate either.

---

## Conventions

- New entries get the next `PL-N` ID; numbers are never reused.
- Resolving an entry means the answer landed in code or in a strategy doc (identification.md, structural-commitments.md, an ADR). Move the entry to [`parking-lot-resolved.md`](parking-lot-resolved.md) with a `**Resolved:**` line; the live file stays scannable, grep still finds the history.
- Don't ripple to other docs when raising an entry. The parking lot is the one place a fork lands. Strategy docs only update when the question is *resolved*.
