# Parking Lot

Short entries (~5 lines each) for forks discovered mid-work and decisions in flight. Not loaded into conversation context — reference explicitly when working the relevant question. Entries are removed when resolved (the answer lands in code or strategy doc, not here).

**Shape per entry:** *what surfaced* / *when raised* / *where in the work it came up* / *blocking the current slice yes/no* / *resolution path if known*.

---

## PL-1 — LifeStage modeling: dual-home problem (RESOLVED 2026-05-13)

**Raised:** 2026-05-10 (mid-slice-2 execution); reinforced 2026-05-11 after Pat's overnight research.
**Where:** Discovered while applying the green-lacewing → chrysoperla data move; slice 1 had just spread the inline-life-stage pattern from `InsectSpecies` to `InsectGenus` and `InsectFamily`.
**The smell:** `LifeStage` is a `NamedEntity` with its own repository and standalone `life-stages.json`, but is *also* held by value as a component on `InsectSpecies` (annotated `@AggregateRoot`) and now on `InsectGenus` / `InsectFamily` (plain `NamedEntity`, not aggregates per the framework rules). Two homes for the same data; framework rule violation (NamedEntity owning NamedEntity by value).
**Resolution:** Closed by Phase 5 of [`plans/clades-kernel.md`](../plans/clades-kernel.md) (slice plan: [`plans/clades-kernel-phase-5.md`](../plans/clades-kernel-phase-5.md)). `InsectLifeStages.stagesOf(species|genus|family)` is now the canonical answer to "which stages exist" — it walks `placedIn` → `CladeTraversal.findTrait` → `MetabolyTrait` → `Metaboly.stages()`. The inline `egg`/`larva`/`pupa`/`adult` fields are still present on the records (Phase 5 was deliberately additive) and will be removed per-organism via PL-2.

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

Clean recipe (applies to the 6 landed organisms): delete the under-identified `InsectSpecies` record, rename its four standalone `LifeStage` records to the parent-rank composite slug, add `"placedIn": "..."` to the destination genus/family record so the resolver works. Each clean move is a focused JSON edit + one resolver test. The original plan at `docs/plans/green-lacewing-rank-correction.md` predates Phase 5 and overstates the work — there is no inline-life-stage merge step.

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

## PL-11 — Cross-rank functional ecology (`InsectFunctionalRole`)

**Raised:** 2026-05-19 (during Path A step 3 — potato-leafhopper → empoasca migration).
**Where:** Surfaced when the empoasca migration deleted the `potato-leafhopper` species record. The species record carried `guilds: Set<FunctionalGuild>` and `beneficial: boolean` — structured, enum-typed ecology, queryable through `species().getByFunctionalGuild(...)`. `InsectGenus` and `InsectFamily` have no equivalent fields, so deleting the species record silently drops the structured assignment. The narrative survives (`empoasca-adult.ecologicalRole` text; genus description tiers), but the enum-tagged query path no longer reaches the organism. The same loss occurred silently for the six previously-migrated PL-2 organisms (chrysoperla, halictus, andrena, syrphidae, carabidae, tipulidae, hesperiidae) — Pat overlooked it at the time.

**Blocking:** No. Current slice (potato-leafhopper migration) ships without it; the follow-up reattaches structured ecology cross-rank.

**Decision (confirmed 2026-05-19):** Option B — introduce a new cross-rank entity `InsectFunctionalRole` keyed by `InsectRankName`, mirroring the `InsectImage` cross-rank pattern landed in commits `600bc1d` + `4c7449d`. One repository, one JSON file, one query: `getByGuild(POLLINATOR)` returns all rank-records (family/genus/species/subspecies) carrying that guild assignment. Single source of truth for "what role does this organism play."

**Rejected:**
- *Option A — add `guilds` / `beneficial` fields to `InsectGenus` and `InsectFamily`.* Three parallel query paths; cross-rank "show me all pollinators" has to fan out.
- *Option C — declare functional guild as a clade trait (like `MetabolyTrait`).* Doesn't fit — functional ecology isn't strictly phylogenetic; Carabidae has both predator and herbivore lineages, etc.

**Scope (when the slice runs):**
- New `InsectFunctionalRoleId` (UUIDv7) in `domains/identifiers/`.
- New `InsectFunctionalRole` entity with `parentName: InsectRankName`, `guilds: Set<FunctionalGuild>`, `beneficial: boolean`. Same field-level `@JsonTypeInfo` / `@JsonSubTypes` dispatch as `InsectImage`.
- Repository + query + mock + behavioral contract test (use `/entity-repository` + `/entity-query` skills).
- Rank-aware FK constraint (3 constraints — family / genus / species) in `InsectFunctionalRoleTestEntitySource`, mirroring `InsectImageTestEntitySource`.
- `parentName` is `@EntityIdentifier` (one role record per organism record).
- Initial JSON catalog: 15 records — 8 species (back-filled from current `insect-species.json`), 7 PL-2-migrated genus/family records (rehydrated from git history of commits `e5e3c8d`, `be0bbce`, `d12c3b2`, `c3a3f8d`, `4c7449d`).
- Remove `guilds` and `beneficial` fields from `InsectSpecies` record; update `SpeciesRepository.getByFunctionalGuild(...)` and `SpeciesQuery.getByFunctionalGuild(...)` to delegate to the new entity (or remove and replace with the cross-rank query).
- Update the `/insects/guild/{guild}` console page to fan results across ranks.

**Out of scope for the follow-up slice:**
- `sightingNotes` rehoming — that's observation-event content, belongs to the eventual sightings entity in the identification roadmap, not to functional-role. The two `sightingNotes` lines unique to the deleted potato-leafhopper record (the dated crimson-clover observation; the "first pest documented" sequencing fact) are noted here for the sightings-entity slice when it lands.

**Resolution path:** Lands in `domains/insects/insects-api/InsectFunctionalRole` + its repository / query stack. Remove this entry when the new entity is wired and the 15 records are seeded.

---

## Resolved (kept for grep)

- **Q4 — Insects vs Apiary** (resolved pre-PL rename). Apiary is its own domain module (Colony aggregate root). Insects module covers Insecta (six legs). SHB control: H. indica only (not S. feltiae).

---

## Conventions

- New entries get the next `PL-N` ID; numbers are never reused.
- Resolving an entry means the answer landed in code or in a strategy doc (identification.md, structural-commitments.md, an ADR). Remove the entry; the resolution lives at the answer's home.
- Don't ripple to other docs when raising an entry. The parking lot is the one place a fork lands. Strategy docs only update when the question is *resolved*.
