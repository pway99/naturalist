# Parking Lot

Short entries (~5 lines each) for forks discovered mid-work and decisions in flight. Not loaded into conversation context — reference explicitly when working the relevant question. Entries are removed when resolved (the answer lands in code or strategy doc, not here).

**Shape per entry:** *what surfaced* / *when raised* / *where in the work it came up* / *blocking the current slice yes/no* / *resolution path if known*.

---

## PL-1 — LifeStage modeling: dual-home problem

**Raised:** 2026-05-10 (mid-slice-2 execution); reinforced 2026-05-11 after Pat's overnight research.
**Where:** Discovered while applying the green-lacewing → chrysoperla data move; slice 1 had just spread the inline-life-stage pattern from `InsectSpecies` to `InsectGenus` and `InsectFamily`.
**The smell:** `LifeStage` is a `NamedEntity` with its own repository and standalone `life-stages.json`, but is *also* held by value as a component on `InsectSpecies` (annotated `@AggregateRoot`) and now on `InsectGenus` / `InsectFamily` (plain `NamedEntity`, not aggregates per the framework rules). Two homes for the same data; framework rule violation (NamedEntity owning NamedEntity by value).
**Blocking:** YES — for slice 2 onwards. Doing the per-organism data moves in the current inline-copy shape compounds the duplication across 10 organisms. Per Pat's call: park slice 2; resolve the modeling first.
**Resolution path:** Add `Clade` and `Rank` to `kernels/taxonomy`. Model `LifeStage` as referenced-by-name from parent records (Clade + Rank + stage-kind composes the identity). Slice 2 then becomes "replace inline duplicates with references," far smaller per-organism work.

---

## PL-2 — Slice 2 (green-lacewing rank correction) paused

**Raised:** 2026-05-11 (this session).
**Where:** Per-organism data reorganization for Phase 0 of the identification roadmap.
**Status:** Plan doc committed at `16e4910` (`docs/plans/green-lacewing-rank-correction.md`); the data-move JSON edits were reverted and never committed.
**Blocking:** Pat's reframing on PL-1 makes the inline-copy shape wrong. Resume after PL-1 lands.
**Expected resume shape:** "Remove green-lacewing species record + add `chrysoperla` references to existing clade life-stages." Editorial mass drops significantly because there's no description-merge work — the clade life-stage records exist independently of the inline copies that would have been promoted.

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

## Resolved (kept for grep)

- **Q4 — Insects vs Apiary** (resolved pre-PL rename). Apiary is its own domain module (Colony aggregate root). Insects module covers Insecta (six legs). SHB control: H. indica only (not S. feltiae).

---

## Conventions

- New entries get the next `PL-N` ID; numbers are never reused.
- Resolving an entry means the answer landed in code or in a strategy doc (identification.md, structural-commitments.md, an ADR). Remove the entry; the resolution lives at the answer's home.
- Don't ripple to other docs when raising an entry. The parking lot is the one place a fork lands. Strategy docs only update when the question is *resolved*.
