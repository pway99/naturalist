# Insect Aggregate — Effort Plan

A new top-level aggregate that composes everything known about an insect at
Oak Vista — its photographic observation, its identified rank chain (Order
through Species), and its life-stage catalog — into a single in-memory
domain object whose `invariants()` becomes the legitimate home for
cross-rank constraints.

This is a sketch in the style of [`clades-kernel.md`](clades-kernel.md) and
[`identification.md`](identification.md) — direction and rationale,
phased delivery, no per-step TDD recipe. Each phase is promoted to its own
slice plan when its predecessor lands.

> **Plan history.** Imported 2026-05-31 from a brainstorming session that
> surfaced two intertwined motivators: (a) the species-detail controller in
> [`InsectsController.detail(...)`](../../domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java)
> repeats `insectQuery.X.getByName(...).orElseThrow()` chain-walks across
> every rank-aware route — an unstated *"if a rank exists, its parent FKs
> resolve"* invariant living in the controller; (b) the cancelled Phase 5b
> PR 3 ([`clades-kernel-phase-5b-life-stage-inline-removal.md`](clades-kernel-phase-5b-life-stage-inline-removal.md))
> tried to put a cross-record clade-conformance invariant on
> `LifeStageEntityRepository.insert(...)` — a layering violation per
> `domains/CLAUDE.md`: *"A repository has exactly four responsibilities
> ... no logic."* Both motivators converge on the same answer: an
> aggregate whose consistency boundary holds the rank chain + life-stages
> together.

---

## Why this exists

Two concrete problems point at one missing construct:

1. **FK-chain resolution scattered through the console.** Every
   rank-aware route in `InsectsController` resolves the species's
   ancestors by repeated `getByName(...).orElseThrow()` — `detail` does
   it for genus/family/order, `lifeStages` does the same chain, and the
   genus/family pages do partial versions. Each `orElseThrow()` is an
   informal invariant: *"if this rank is in the catalog, its parent FK
   resolves to a catalogued parent."* No domain construct enforces this
   today — it is only true because the in-memory seed loads consistently.
2. **No legitimate home for cross-record clade invariants.** Phase 5b's
   PR 3 needed to enforce *"every cataloged `LifeStage`'s `kind` ∈
   resolved `Metaboly.stages()`"* — a cross-record constraint requiring
   the organism's rank chain to resolve `placedIn` and walk the clade
   DAG. The repository was the wrong host (forbidden by project rule);
   `LifeStage.invariants()` was the wrong host (the record doesn't
   know its organism's chain); the existing rank-rooted `InsectAggregate`
   permits don't carry the chain either. The aggregate that would host
   this invariant did not exist.

The `Insect` aggregate is that missing construct. It is the consistency
boundary for *"what we know about an organism at Oak Vista, at whatever
identification depth has been reached"* — composing the rank chain, the
observation that started identification, and the life-stages catalogued
for the organism. Invariants that span any subset of these pieces find
their natural home in `Insect.invariants()`.

---

## Project integration notes

**Relationship to the existing `InsectAggregate` (sealed).** Today's
`InsectAggregate` is a sealed interface with four permits
(`InsectOrderAggregate`, `InsectFamilyAggregate`, `InsectGenusAggregate`,
`InsectSpeciesAggregate`), each composing one rank entity with its
`ImageCollection`. It has no consumers — defined, factored, queried,
tested, but the console doesn't read it. The new `Insect` aggregate
**composes** these permits rather than replacing them: each permit
becomes a unit-of-load that `Insect` may or may not hold depending on
identification depth. The existing sealed type and its permits stay; the
new `Insect` is layered above.

**Relationship to "Class Insecta".** Class Insecta is the implicit
ceiling of every observation in the insects domain — nobody identifies
an organism only to "the class Insecta." Insecta is not modelled as a
domain entity. The console's "Class Insecta" descriptor stays a static
render. The `Insect` aggregate's rank chain runs Order → Family → Genus
→ Species; Insecta is not a field.

**Relationship to two use cases.** The same `Insect` aggregate serves
both. *Catalog read* (today's console): load an `Insect` from a rank
name, full chain populated to that depth, returned for display. *Future
identification workflow*: create an `Insect` from an `InsectImage` with
no rank set, refine via `with*` mutators as the naturalist identifies
the organism. Same record type; the loaded state differs.

**Cancels** Phase 5b PR 3 in
[`clades-kernel-phase-5b-life-stage-inline-removal.md`](clades-kernel-phase-5b-life-stage-inline-removal.md).
That PR proposed write-time clade validation on
`LifeStageEntityRepository`; the layering was wrong. When the
clade-conformance invariant is added, it goes on `Insect.invariants()`,
not on the repository.

---

## Target architecture

The aggregate is a record. Sum-of-parts. Not persisted — its constituent
parts each live in their respective repositories; `Insect` is the
in-memory composition that orchestrates invariants across them.

```java
package com.naturalist.insects;

public record Insect(
        @Nullable InsectImage observation,
        @Nullable InsectOrderAggregate order,
        @Nullable InsectFamilyAggregate family,
        @Nullable InsectGenusAggregate genus,
        @Nullable InsectSpeciesAggregate species,
        List<LifeStage> lifeStages
) implements Aggregate {

    /** Most-specific identified rank, if any. Empty for image-only workflow start. */
    public Optional<InsectRankName> identifiedTo() { … }

    /** Workflow refinement — each returns a new Insect. */
    public Insect withObservation(InsectImage image) { … }
    public Insect withOrder(InsectOrderAggregate order) { … }
    public Insect withFamily(InsectFamilyAggregate family) { … }
    public Insect withGenus(InsectGenusAggregate genus) { … }
    public Insect withSpecies(InsectSpeciesAggregate species) { … }
    public Insect withLifeStages(List<LifeStage> stages) { … }

    @Override
    public Consumer<? extends Constraints> invariants() { … }
}
```

**`identifiedTo()` semantics.** Returns the most-specific present rank's
typed name. `species.name()` if species set, else `genus.name()`, else
`family.name()`, else `order.name()`, else `Optional.empty()`
(image-only workflow start).

**`with*` semantics.** Each mutator returns a new `Insect` with the
target field replaced. Invariants run on every construction (record
constructor), so an invalid mutation throws at the `with*` call site.
There is no `withoutX` — narrowing the aggregate is not a valid
workflow operation (identification refines, it doesn't retreat). The
exception is correction: re-running identification produces a new
`Insect` from scratch, not by removing fields from an existing one.

### Invariants — first cut (this slice)

`Insect.invariants()` enforces structural consistency only:

1. **Monotonic fill** — if `species` is set, `genus` must be set; if
   `genus` is set, `family` must be set; if `family` is set, `order`
   must be set. Skipping ranks is invalid.
2. **Image FK consistency** — if `observation` is set and the most-
   specific identified rank is non-empty, `observation.parentName()`
   must equal that rank's typed name. (An observation can sit on an
   aggregate that has no rank yet — image-only start; once a rank is
   identified, the observation must reference it.)
3. **Permit-to-rank consistency** — `order` must be an
   `InsectOrderAggregate`, `family` an `InsectFamilyAggregate`, etc.
   The record's type signature enforces this at compile time; the
   invariant codifies the rule for runtime diagnostic clarity.

### Invariants — deferred to follow-up efforts

- **Placement chain monotonicity** — if multiple ranks set `placedIn`,
  the chain of clades must be consistent (child's placedIn must descend
  from parent's placedIn in the clade DAG).
- **Resolvable metaboly** — at least one rank in the chain must yield a
  `MetabolyTrait` via the existing `InsectLifeStages` walk-up resolver.
- **Clade-conformance of LifeStages** — every `LifeStage.kind()` in
  `lifeStages` must be in the resolved `Metaboly.stages()` set. This
  is the invariant that PR 3 of the Phase 5b slice was trying to host;
  here it has a legitimate home.

These three deferred invariants are *what the aggregate is ultimately
for*. They land in a later slice when the catalog has accumulated
enough data to drive them and when a consumer wants the guarantee. The
first slice ships the aggregate's structure without them.

### What the aggregate does NOT include

- **A factory.** Loading an `Insect` from a rank name (or from an
  observation) is the next slice's concern. The first slice ships the
  record in isolation — exercised by tests that construct it directly.
- **A query namespace.** No `InsectQuery.insects()` entry until the
  factory exists.
- **Console refactor.** The `InsectsController` keeps doing its
  `getByName().orElseThrow()` chain walks for now. Migration to the
  new aggregate happens in the slice that introduces the factory +
  query.
- **A clade-context field.** Deferred until the clade-conformance
  invariant lands; until then, callers that need the resolved Metaboly
  use the existing `InsectLifeStages.stagesOf(species, genus, family,
  order)` helper directly.

---

## Phase summary

| Phase | Name                                                | Status     | Gates on                                                                |
| ----- | --------------------------------------------------- | ---------- | ----------------------------------------------------------------------- |
| 1     | Build the `Insect` record + structural invariants   | next       | —                                                                       |
| 2     | Factory + query + console refactor                  | deferred   | Phase 1                                                                 |
| 3     | Clade-context field + clade-conformance invariants  | deferred   | Phase 2; concrete consumer for resolved Metaboly                        |
| 4     | Identification workflow integration                 | deferred   | Identification module work; phases 1–3                                  |

---

## Phase 1 — Build the `Insect` record + structural invariants

**Why first.** Stand up the aggregate type in isolation. No factory, no
query, no consumer. Tested by direct construction. Lets the shape settle
and structural invariants prove themselves before any integration
pressure.

**Delivers.**

- `Insect` record in `insects-api` with the six fields shown above.
- `identifiedTo()` accessor.
- Six `with*` mutators (observation, order, family, genus, species,
  lifeStages).
- `invariants()` enforcing monotonic-fill, image FK consistency, and
  permit-to-rank consistency.
- Direct construction tests covering: each invariant's happy path and
  each invariant's violation; each `with*` mutator's return shape and
  invariant trigger; `identifiedTo()` for each loaded state including
  empty.
- No public factory. No query exposure.

**Out of scope.** Factory. Query. Console refactor. Clade context.
Workflow entrypoints. Plant-side mirrors.

---

## Phase 2 — Factory + query + console refactor

**Why.** Make the aggregate reachable for the catalog-read use case.
First consumer is the console.

**Delivers (sketched, decided when promoted).**

- `InsectFactory` in `insects-core` with at least one entrypoint:
  `fromRankName(InsectRankName) : Optional<Insect>` — looks up the rank
  entity, walks up to root, loads `ImageCollection`s and `LifeStage`s
  along the way, returns the assembled `Insect`. The `orElseThrow()`
  chain-walks the console does today move into this factory.
- `InsectQuery.insects()` namespace exposing the factory as a query
  surface.
- `InsectsController.detail(...)` (and `lifeStages` / genus / family /
  order routes) refactored to call `insectQuery.insects().fromRankName(...)`
  once, then read the resulting `Insect`. The four-query chain dance
  disappears from the controller.

**Out of scope.** Workflow entrypoints (`fromObservation`). Clade
context. Anything that requires the aggregate to mutate.

---

## Phase 3 — Clade-context field + clade-conformance invariants

**Why.** Host the invariant Phase 5b PR 3 was trying to place. Land
when a concrete consumer wants the resolved Metaboly or when catalog
drift makes the unenforced state painful.

**Delivers (sketched).**

- A `resolvedMetaboly()` accessor on `Insect` (and/or a stored
  `@Nullable Clade rootPlacement` field — to be decided when the phase
  is promoted). Runs the existing `InsectLifeStages` walk-up against
  the aggregate's rank chain.
- Three new invariants on `Insect.invariants()`: placement chain
  monotonicity, resolvable metaboly (if any rank carries `placedIn`),
  clade-conformance of cataloged life-stages.
- Repository-level write hooks updated as needed — the aggregate is
  read-side; the write-side enforcement is that *new* LifeStage
  inserts must produce an aggregate that still satisfies its
  invariants. The mechanism (insert-then-validate-aggregate, or a
  pre-insert aggregate validation) is decided when the phase is
  promoted.

**Out of scope.** Workflow entrypoints.

---

## Phase 4 — Identification workflow integration

**Why.** The aggregate's second motivator. When the identification
module begins, the aggregate is already structural — phase 4 wires it
into the workflow surface.

**Delivers (sketched, decided when promoted).**

- `InsectFactory.fromObservation(InsectImageId) : Optional<Insect>` —
  starts an aggregate from just an observation, no rank identified.
- Whatever command surface the identification module needs to refine
  the aggregate (`identifyToOrder`, `identifyToFamily`, etc.) — these
  call the existing `with*` mutators after the rank entity is
  resolved.
- Persistence story for an in-progress identification — the aggregate
  itself is not persisted, but the constituent parts are (observation
  exists in `InsectImage` repository; identified rank narrows the FK
  on the observation). The exact mechanism is the identification
  module's design.

**Out of scope.** UI for identification (that's the identification
module's concern).

---

## Key decisions and rationale

| Decision                                                | Rationale                                                                                                                                  |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Single concrete record, not sealed permits              | The aggregate is always factory-loaded; sealed-permit compile-time exhaustiveness adds machinery without earning its keep when runtime construction already enforces the same guarantees. One type, simpler model. |
| Composes existing rank-rooted `InsectAggregate` permits | The existing permits are the unit-of-load for "rank entity + its images." Keeping them as `Insect`'s composed parts preserves the existing structure as a load primitive. |
| Sum-of-parts, not persisted                             | The aggregate's identity comes from its constituent parts — the observation's UUID, the species's slug, etc. — not from an aggregate-level identifier. Persisting it separately would duplicate state already managed by the constituent repositories. |
| `identifiedTo()` returns Optional                        | An image-only aggregate (workflow start) has no identified rank. Forcing a default would lie about the state. |
| Clade-conformance invariant deferred                     | The catalog is small and hand-curated; drift is not yet a felt pain. Hosting the invariant on `Insect.invariants()` is the right *home*; landing it on day one would block on data that doesn't exist yet. |
| Phase 5b PR 3 cancelled, not deferred                    | The repository was the wrong host. Cancelling rather than deferring makes the architectural correction explicit. The Phase 5b slice plan is updated to flip PR 3's status. |
| Single `lifeStages` list (not per-rank lists or map)    | Each `LifeStage` already carries its own `parentName`. A consumer that wants per-rank grouping can group locally. Premature structure otherwise. |

---

## Out of scope for this effort

- Promoting Class Insecta to a domain entity.
- Persisting in-progress identification state (covered by the future
  identification module, not the aggregate itself).
- Reconciling `Insect` with the existing `InsectAggregate` sealed type
  beyond the compose-not-replace decision documented above. If
  `InsectAggregate`'s permits gain new responsibilities or `Insect`
  subsumes their last use case, a separate consolidation effort.
- Plant-side mirror (e.g. a `Plant` aggregate over plant rank chains).
  Same pattern would apply; not a current need.
- A backstop write-time clade validation on `LifeStageEntityRepository`
  (was Phase 5b PR 3; cancelled).

---

## Risk register

- **Aggregate accumulates fields over time.** Each new ecological
  concern that an insect might carry (functional role, chemical
  defense, etc.) is a candidate for inclusion in `Insect`. The record
  can grow long and unfocused. *Mitigation:* hold the line on
  composition-of-existing-aggregates as the inclusion criterion; if a
  concern already has its own entity / query, the aggregate composes
  it only if cross-concern invariants exist that need the joint
  consistency boundary. Otherwise the concern stays separate, and
  consumers compose what they need at the application layer.

- **Workflow entrypoints contradict catalog-read assumptions.** When
  phase 4 adds `fromObservation`, the aggregate's invariants must
  accommodate "image-only, no rank" states that the catalog-read use
  case never produces. *Mitigation:* the structural invariants from
  phase 1 are already image-only-tolerant (monotonic-fill allows the
  observation-without-any-rank state; image FK consistency is
  conditional on a rank existing). Future invariants follow the same
  discipline: state validity is conditional on what's loaded.

- **Console refactor (Phase 2) may surface controller responsibilities
  that don't belong in the aggregate.** Breadcrumb construction,
  ancestor-intro rendering, view-model assembly, etc. are presentation
  concerns. *Mitigation:* the factory's job is loading the aggregate;
  the controller's job stays "compose the aggregate-derived data with
  presentation helpers." If a presentation helper naturally wants
  derived data from `Insect`, that's a `view-model` concern, not an
  aggregate accessor.

- **The deferred clade-conformance invariant is the load-bearing
  reason this aggregate exists.** Shipping phase 1 + 2 without phase
  3 risks the aggregate looking like "just a controller helper" and
  losing its domain weight. *Mitigation:* the design document
  (this file) captures the invariant as the long-term motivation;
  the phase 3 slice plan, when promoted, restates it clearly.

---

## Slot in the work-tracker

Row to add to [`docs/work-tracker.md`](../work-tracker.md):

> **Insect aggregate** — Plan (sketch) — *active — Phase 1 next* —
> [`plans/insect-aggregate.md`](plans/insect-aggregate.md) — *New
> top-level aggregate composing the rank chain + observation +
> life-stages; the legitimate home for cross-record clade invariants
> Phase 5b PR 3 was trying to place on the wrong layer. Multi-phase:
> Phase 1 ships the record + structural invariants in isolation;
> Phase 2 adds factory + query + console refactor; Phase 3 adds clade
> context + conformance invariants; Phase 4 wires identification
> workflow.*

Phase 5b slice plan's PR 3 entry flips from *planned* to *cancelled*,
with a pointer to this effort.

---

## Open questions (deferred to per-phase plans)

- **Phase 2** — Factory signature: typed methods per entrypoint
  (`fromRankName`, `fromObservation`) vs. a single polymorphic
  `load(InsectSource)` over a sealed union. Decided when phase 2 is
  drafted.
- **Phase 2** — Whether `InsectQuery.insects().fromRankName(...)`
  returns `Optional<Insect>` (current convention) or throws on missing
  root entity. Decided when phase 2 is drafted.
- **Phase 3** — Clade context shape: derived-on-demand accessor only
  vs. eager-stored `@Nullable Clade rootPlacement` field. Decided when
  phase 3 is drafted.
- **Phase 3** — Write-side enforcement mechanism: insert-then-validate
  vs. pre-insert aggregate validation. Decided when phase 3 is drafted.
- **Phase 4** — Persistence story for in-progress identification
  workflows. Decided in coordination with the identification module
  design.
