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

## PL-2 — Slice 2 (green-lacewing rank correction) — unblocked 2026-05-13

**Raised:** 2026-05-11 (this session).
**Where:** Per-organism data reorganization for Phase 0 of the identification roadmap.
**Status:** Plan doc committed at `16e4910` (`docs/plans/green-lacewing-rank-correction.md`); the data-move JSON edits were reverted and never committed. **Unblocked** as of clades-kernel Phase 5 — the resolver replaces the dual-home shape that made the original slice-2 approach wrong.
**Resume shape:** Per-organism removal of inline `egg`/`larva`/`pupa`/`adult` fields on `InsectSpecies`/`InsectGenus`/`InsectFamily`, replaced with reliance on `InsectLifeStages.stagesOf(...)` + per-organism `LifeStage` records keyed by `(organismName, stageKind)`. Green-lacewing is the pilot; the same recipe applies across the catalog. Editorial mass dropped significantly versus the original slice-2 shape because the description-merge work doesn't exist — the clade life-stage records already exist independently.

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

## Resolved (kept for grep)

- **Q4 — Insects vs Apiary** (resolved pre-PL rename). Apiary is its own domain module (Colony aggregate root). Insects module covers Insecta (six legs). SHB control: H. indica only (not S. feltiae).

---

## Conventions

- New entries get the next `PL-N` ID; numbers are never reused.
- Resolving an entry means the answer landed in code or in a strategy doc (identification.md, structural-commitments.md, an ADR). Remove the entry; the resolution lives at the answer's home.
- Don't ripple to other docs when raising an entry. The parking lot is the one place a fork lands. Strategy docs only update when the question is *resolved*.
