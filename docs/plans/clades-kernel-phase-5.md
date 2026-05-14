# Clades Kernel — Phase 5 Slice Plan

> Promoted from [`clades-kernel.md`](clades-kernel.md) Phase 5 on 2026-05-13.

**Goal.** Provide the resolver that answers *"which life-stage kinds
does this organism have?"* by walking `placedIn` → `CladeTraversal`
→ `MetabolyTrait` → `Metaboly.stages()`. This is the consumer-facing
surface the inline `egg`/`larva`/`pupa`/`adult` fields on
`InsectSpecies` / `InsectGenus` / `InsectFamily` were a workaround
for.

After this phase lands, [PL-2](../notes/parking-lot.md) (the
green-lacewing rank correction) can resume — its work becomes
*"replace inline duplicates with clade references"* rather than
*"move duplicates around between ranks"*.

This is the **resolver phase**. No behavioural shift for existing
consumers because nothing currently calls "which stages does this
species have" — Phase 5 introduces the *new* surface that PL-2 and
subsequent slices consume. The inline fields stay in place; their
removal is PL-2's deliberate per-organism work, not a Phase 5
sweep.

---

## Scope decisions

### Static utility, not interface

`InsectLifeStages` is a freestanding `final class` with private
constructor and `public static` methods. Same shape as
`InsectClades` from Phase 2 — pure function over inputs, no state,
no DI. The kernel-style "pattern-matching switch over a sealed
type" is exactly the right structure: the resolver is a pure
function from organism → stage list, not a service with lifecycle
or backing storage.

The alternative (`InsectLifeStageQuery.stagesFor(speciesName)`
returning via repository lookup) is rejected for now — it conflates
*resolution* (clade traversal, pure) with *lookup* (repository
read). The clade walk has no need for the repository; it operates
on the record the caller already holds.

If a future consumer needs the by-name version, it can compose:
```java
var species = speciesQuery.getByName(name).orElseThrow();
var stages = InsectLifeStages.stagesOf(species);
```

That's the same composition pattern Phase 4's
`CladePlacementResolutionTest` already demonstrates with
`CladeTraversal.findTrait` directly. The new utility is a small
ergonomic wrapper.

### Overloads per record type, not a single `Optional<Clade>` form

The natural call site is `stagesOf(species)` not
`stagesOf(species.placedInOptional())`. The three overloads
(`InsectSpecies`, `InsectGenus`, `InsectFamily`) each do the
`placedInOptional()` → traversal dance internally. The caller hands
in the record; they get back the stage list.

A single `stagesOf(Optional<Clade>)` form is rejected because it
pushes the placement-extraction noise onto every call site and
gains nothing — the three overloads are three identical-shape
one-liners.

### Return `List<LifeStageKind>`, not `Set<LifeStageKind>` or a `BehavioralCollection`

`Metaboly.stages()` already returns `List<LifeStageKind>` because
the developmental sequence has order (egg → larva → pupa → adult
is meaningful; egg → pupa → larva → adult is wrong). The resolver
preserves that order.

A `BehavioralCollection` is overkill for a stage list that's at
most four elements long with no behavioural method needs. Plain
`List<LifeStageKind>` is the right return shape.

### Empty list when unplaced or trait-less

If `placedIn` is null, or the traversal hits no `MetabolyTrait`
declaration, the resolver returns `List.of()`. No exceptions, no
`Optional<List>` wrapper. The empty list reads naturally at call
sites: a species without a placement has no resolvable stages, and
"no resolvable stages" is `List.of()`.

### Out of scope

- **Removal of inline `egg`/`larva`/`pupa`/`adult` fields on
  `InsectSpecies` / `InsectGenus` / `InsectFamily`.** This is the
  PL-2 follow-up. The parent plan explicitly calls it out as a
  separate change. The inline fields remain populated in JSON; the
  resolver works alongside them.
- **Write-time enforcement** (a `LifeStage` record validating its
  stage-kind against the organism's clade at construction time).
  Open question in the parent plan; deferred. Query-time
  resolution is what Phase 5 ships.
- **Routing the existing `LifeStageRepository.getBySpeciesName(...)`
  through the resolver.** That repository returns *full
  `LifeStage` records* (with descriptions, phenology, habitat) for
  the stages we have *data* for — a different question from "what
  stage kinds *should* exist according to the clade". The resolver
  answers the latter; the repository continues to answer the
  former. Composing them (e.g., "stages with data vs. expected") is
  a consumer-side operation, not a Phase 5 deliverable.

---

## Architecture

```
domains/insects/insects-api/src/main/java/com/naturalist/insects/lifestage/
└── InsectLifeStages.java                — NEW, static utility

domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/
└── InsectLifeStagesTest.java            — NEW, unit tests (inline records)

domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/
└── CladePlacementResolutionTest.java    — extended with stages-resolution case
```

The utility lives in `insects-api/lifestage/` alongside `Metaboly`,
`MetabolyTrait`, `Holometabolous`, etc. — every type it touches is
in this package or imported from `kernels/clades/` and `insects-api`.

---

## Code shape

```java
public final class InsectLifeStages {

    private InsectLifeStages() {
    }

    public static List<LifeStageKind> stagesOf(InsectSpecies species) {
        return resolve(species.placedInOptional());
    }

    public static List<LifeStageKind> stagesOf(InsectGenus genus) {
        return resolve(genus.placedInOptional());
    }

    public static List<LifeStageKind> stagesOf(InsectFamily family) {
        return resolve(family.placedInOptional());
    }

    private static List<LifeStageKind> resolve(Optional<Clade> placedIn) {
        return placedIn
                .flatMap(c -> CladeTraversal.findTrait(c, MetabolyTrait.class, InsectClades::traitsFor))
                .map(t -> t.metaboly().stages())
                .orElse(List.of());
    }
}
```

---

## Test plan

### Unit tests (`InsectLifeStagesTest`)

Inline records — no JSON load, no repository. Covers the resolver
semantics in isolation.

- `stagesOfSpeciesPlacedInPapilionidaeReturnsHolometabolousStages`
  — species with `placedIn = new Papilionidae()` resolves to
  `[EGG, LARVA, PUPA, ADULT]`.
- `stagesOfUnplacedSpeciesReturnsEmptyList` — species with
  `placedIn = null` resolves to `List.of()`.
- `stagesOfSpeciesPlacedInTraitlessCladeReturnsEmptyList` —
  species placed in a clade with no MetabolyTrait ancestor (e.g.,
  if a future clade is added above `Holometabola` without trait
  inheritance) resolves to `List.of()`. *Note:* every current
  permit either declares MetabolyTrait or inherits it via
  Holometabola, so this case requires construction of a placement
  outside the current trait coverage. Defer until a relevant
  permit lands; for now, the unplaced case is sufficient
  coverage.
- `stagesOfGenusPlacedInPapilionidaeReturnsHolometabolousStages`
  — same for `InsectGenus`.
- `stagesOfFamilyPlacedInPapilionidaeReturnsHolometabolousStages`
  — same for `InsectFamily`.

### End-to-end test (extend `CladePlacementResolutionTest`)

One new test:

- `battusPhilenorStagesResolveToHolometabolousSequenceViaResolver`
  — loads battus-philenor via TestEntitySource (real JSON),
  calls `InsectLifeStages.stagesOf(species)`, asserts the result
  is `[EGG, LARVA, PUPA, ADULT]`.

This is the analogue of `battusPhilenorPlacementResolvesToHolometabolyTraitViaPapilionidae`
but exercises the higher-level `InsectLifeStages` surface rather
than calling `CladeTraversal.findTrait` directly.

---

## Steps

- [ ] Write `InsectLifeStages.java`.
- [ ] Write `InsectLifeStagesTest.java`.
- [ ] Extend `CladePlacementResolutionTest` with one new test.
- [ ] User runs `mvn verify`. Expect green.
- [ ] Commit.
- [ ] Roll work-tracker forward — Phase 5 ✅, Phase 6 deferred,
  PL-2 unblocked.
- [ ] Update [`notes/parking-lot.md`](../notes/parking-lot.md) PL-2
  to "unblocked — resume when ready" status.

---

## Out of scope (recap)

- Removal of inline `egg`/`larva`/`pupa`/`adult` on
  `InsectSpecies` / `InsectGenus` / `InsectFamily` (PL-2's work).
- Write-time stage-kind validation on `LifeStage` records.
- Routing `LifeStageRepository.getBySpeciesName` through the resolver.
- Phase 6 (extending to plants) — deferred until plant
  identification work demands it.

---

## Risks

- **Resolver returns empty list silently.** A species with
  `placedIn = null` resolves to `[]`. If a consumer treats that
  as "no stages", they may miss the fact that the species simply
  lacks placement. *Mitigation:* downstream consumers that care
  about the distinction should check `placedInOptional().isEmpty()`
  separately. The resolver's contract is "stages declared at or
  above the placement, or empty if no placement". This is documented
  in the javadoc.
- **Trait declaration drift.** If `InsectClades.traitsFor` is later
  edited to remove the Holometabola → Holometabolous mapping (or
  misplace it), the resolver silently returns empty for every
  catalogued holometabolous insect. *Mitigation:* the end-to-end
  test on battus-philenor catches the regression — any change
  that breaks the Papilionidae → Holometabola → MetabolyTrait
  chain turns this test red.
