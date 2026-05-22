# Clades Kernel — Phase 3 Slice Plan

> Promoted from [`clades-kernel.md`](../../clades-kernel.md) Phase 3 on 2026-05-12.
> Revised 2026-05-13 to drop the kernel-level coupling between `kernels/taxonomy/`
> and `kernels/clades/`.

**Goal.** Give each insect record (`InsectFamily`, `InsectGenus`,
`InsectSpecies`) an optional `Clade` placement, so that life-stage traits
declared on the Clade DAG can be resolved for a species via clade traversal.

The link lives in `domains/insects/insects-api/` — **not** in
`kernels/taxonomy/`. The two kernels remain siblings; neither knows about
the other. Insects opts in.

No data is filled in yet — Phase 4 lands the first actual placement
(`"placedIn": "papilionidae"` on the Papilionidae JSON entry).

This is the **contract-extension phase**. No behavioural change for any
existing consumer; every current `new InsectFamily(…)` call site grows
by one `null` argument, and JSON files stay valid unchanged because
Jackson 2.19 treats missing fields on record components as `null`.

---

## Open question (settled) — kernel coupling

**Should `kernels/taxonomy/` depend on `kernels/clades/`?**

The natural-looking design extends each `Linnaean*` interface with a default
`Optional<Clade> placedInOptional()` method, which forces
`kernels/taxonomy/ → kernels/clades/` as a new DAG edge.

That edge is the wrong shape. The two kernels have fundamentally different
identity and lifecycle properties:

- `taxonomy` is open, revisable, value-object-shaped. Linnaean classifications
  change as science evolves; the values themselves are mutable references to
  mutable scientific consensus.
- `clades` is closed, curated, identity-shaped. The sealed-records design is
  deliberately *not* data — adding a clade is a kernel PR, and trait
  declarations rely on exhaustive `switch` over a known finite set of permits.

The kernels also have different consumer profiles:

- `taxonomy` is universal across every organism domain (insects, plants,
  arachnids, worms, microbes, molluscs, vertebrates).
- `clades` is opt-in. Some organism domains will never reach for it.

A clade placement on a Linnaean rank is a *kingdom-specific* decision —
which clade an insect family belongs to is an insects-domain concern.
The kernels doc states: *"kingdom-specific concerns belong in the domain
module, not here."* The link belongs where the decision is made.

**Resolution:** drop the kernel-level coupling. Each organism domain that
wants clade integration adds `@Nullable Clade placedIn` to its own records
and exposes a freestanding `placedInOptional()` method. `kernels/taxonomy/`
stays unaware of `kernels/clades/`. Plants opts in on its own timeline.

---

## Open question (settled) — accessor name + Optional vs. nullable

Three competing constraints:

1. The accessor contract should be **Optional-shaped** for consistency with
   `Clade.parent() : Optional<Clade>` from Phase 1.
2. The record component should follow the **`@Nullable X x` convention**
   already used for `egg` / `larva` / `pupa` / `adult` on `InsectFamily`
   — no codebase precedent for a record-component named `placedIn` of
   type `Optional<Clade>`, and components-of-Optional is generally an
   anti-pattern in Java records.
3. Java compilation: a record component named `X x` auto-generates an
   accessor `x() : X`. A separate `Optional`-returning accessor needs a
   distinct name.

**Resolution:** adopt the chemistry domain's existing pattern (see
`domains/chemistry/CLAUDE.md`, "Optional Profile Methods"):

- Record component is `@Nullable Clade placedIn` — auto-accessor
  `placedIn() : @Nullable Clade` returns the raw nullable.
- Record exposes a freestanding method:
  `public Optional<Clade> placedInOptional() { return Optional.ofNullable(placedIn); }`.

Both accessors coexist. Consumers wanting a nullable view call
`family.placedIn()`; consumers wanting an Optional call
`family.placedInOptional()`. JSON wire format is `"placedIn":
"papilionidae"` because the component name drives the Jackson field
name — descriptive and matches the kernel-interface terminology.

The freestanding pattern is uniform across all three records — no
`@Override` annotations, no interface participation. `InsectSpecies`
(which does not implement `LinnaeanSpecies`) and `InsectFamily` /
`InsectGenus` (which do) follow the same shape.

---

## Open question (settled) — does `InsectSpecies` implement `LinnaeanSpecies`?

Audit finding from drafting this plan: `InsectFamily` implements
`LinnaeanFamily`, `InsectGenus` implements `LinnaeanGenus<InsectFamilyName>`,
but `InsectSpecies` does **not** implement `LinnaeanSpecies` — only
`NamedEntity<InsectSpeciesName>`. This is pre-existing and not Phase 3's
problem.

**Decision: leave the species-side interface relationship alone.** With
the freestanding pattern adopted uniformly above, the LinnaeanSpecies
question is now decoupled from Phase 3 entirely — InsectSpecies gains
`placedIn` and `placedInOptional()` independently, the same way as
InsectFamily and InsectGenus.

If later `InsectSpecies implements LinnaeanSpecies` is wanted for other
reasons, the existing `placedInOptional()` method satisfies nothing on
the interface (because we are no longer extending `LinnaeanSpecies`
with that method) — zero migration cost either way.

---

## Architecture

```
kernels/taxonomy/                              — UNCHANGED
└── (no edits)

kernels/clades/                                — UNCHANGED
└── (Phase 1 + Phase 2 only)

domains/insects/insects-api/                   — three records gain the component
└── src/main/java/com/naturalist/insects/
    ├── InsectFamily.java                      — add @Nullable Clade placedIn, placedInOptional, withPlacedIn
    ├── InsectGenus.java                       — same
    └── InsectSpecies.java                     — same
```

**Plants** (`domains/plants/plants-api/`) — no changes. If plants wants to
participate in the Clade DAG later, it adds `@Nullable Clade placedIn` to
`PlantFamily` / `PlantGenus` / `PlantSpecies` and a freestanding
`placedInOptional()` — same per-domain pattern.

---

## Dependency-graph changes

```
domains/insects/insects-api  →  framework, identifiers, field-notes, taxonomy, clades
```

`insects-api` already depends on `framework`, `identifiers`, `field-notes`,
and `taxonomy`. The only new edge is `insects-api → clades`. The two
kernels stay siblings; no kernel-to-kernel edge is added.

---

## Code shapes (reference)

### Record — `InsectFamily.java` (after, abbreviated)

```java
public record InsectFamily(
        InsectFamilyName name,
        TaxonomicOrder order,
        TaxonomicFamily family,
        Description description,
        Set<CommonName> commonNames,
        @Nullable Clade placedIn,            // NEW — record component
        @Nullable EggStage egg,
        @Nullable LarvaStage larva,
        @Nullable PupaStage pupa,
        @Nullable AdultStage adult
) implements NamedEntity<InsectFamilyName>, LinnaeanFamily {

    public Optional<Clade> placedInOptional() {     // NEW — freestanding, no @Override
        return Optional.ofNullable(placedIn);
    }

    public InsectFamily withPlacedIn(@Nullable Clade value) {    // NEW — mutator
        return new InsectFamily(name, order, family, description, commonNames,
                value, egg, larva, pupa, adult);
    }

    // existing withEgg / withLarva / withPupa / withAdult — every existing
    // mutator's `new InsectFamily(…)` call site grows by one argument
    // (passing the existing `placedIn` through unchanged)
    public InsectFamily withEgg(@Nullable EggStage value) {
        return new InsectFamily(name, order, family, description, commonNames,
                placedIn, value, larva, pupa, adult);  // NEW: placedIn passed through
    }
    // … withLarva, withPupa, withAdult identical pattern …

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {
            i.entityName(name, "name")
                    .namedValue(order, "order")
                    .namedValue(family, "family")
                    // … existing invariants unchanged; placedIn is nullable and
                    //   carries no invariant of its own at the InsectFamily level
                    ;
        };
    }
}
```

`InsectGenus` and `InsectSpecies` follow the same pattern. None of the
three records override an interface method — `placedInOptional()` is
freestanding on each.

### Position of the new component

Inserted **between `commonNames` and the life-stage `@Nullable` block**.
Rationale:

- All `@Nullable` components grouped at the end of the constructor —
  pre-existing convention in `InsectFamily`.
- `placedIn` is conceptually a higher-level placement (taxonomic) than
  the per-instance life-stage fields, so it sits first within the
  nullable block.
- The constructor argument count grows by exactly one; every existing
  call site can be migrated with a single `null` insertion at a
  predictable position.

---

## Call-site update surface

`grep -rn "new InsectFamily(\|new InsectGenus(\|new InsectSpecies("` outside
worktrees lists 8 files with constructor invocations:

| File | Why it constructs |
| --- | --- |
| `insects-api/src/main/java/.../InsectFamily.java` | `withEgg`/`withLarva`/`withPupa`/`withAdult` internals |
| `insects-api/src/main/java/.../InsectGenus.java` | same pattern |
| `insects-api/src/test/java/.../InsectFamilyTest.java` | direct construction in invariants tests |
| `insects-api/src/test/java/.../InsectGenusTest.java` | same |
| `insects-api/src/test/java/.../InsectAggregateTest.java` | constructing fixtures |
| `insects-repository-test/src/main/java/.../FamilyRepositoryTest.java` | `newEntity` / `ghostEntity` / `modifiedEntity` hooks |
| `insects-repository-test/src/main/java/.../GenusRepositoryTest.java` | same |
| `insects-repository-test/src/main/java/.../SpeciesRepositoryTest.java` | same |
| `insects-core/src/test/java/.../SpeciesCommandImplTest.java` | command-test fixtures |

Each site grows by one `null` argument inserted in the new component's
position. No test logic changes — the `null` is functionally identical
to today's behaviour where `placedIn` doesn't exist.

JSON files in `insects-repository-test/src/main/resources/` are **not
touched**. Jackson 2.19 passes `null` to record components when the JSON
field is absent. Every existing JSON entry continues to deserialise.

---

## Test plan

### New unit tests

`insects-api/src/test/java/com/naturalist/insects/InsectFamilyTest.java` gains:

- `placedInComponentRoundtripsViaJackson` — construct an `InsectFamily`
  with `placedIn = new Papilionidae()`, write to JSON via `ObjectMapper`,
  read back, assert equal.
- `placedInOptionalReturnsPresentWhenSet` — `family.placedInOptional()`
  returns `Optional.of(new Papilionidae())`.
- `placedInOptionalReturnsEmptyWhenAbsent` — same with `null` placedIn,
  expect `Optional.empty()`.
- `withPlacedInReturnsNewInstanceWithUpdatedClade`.
- `withEggPreservesPlacedIn` — guard against regressions in the existing
  mutators dropping the placement (see Risks below).

Same set of tests in `InsectGenusTest.java` and `InsectSpeciesTest.java`
(create the latter if it doesn't exist; if InsectSpecies is currently
tested elsewhere, add the placedIn cases to whatever class owns its
invariants).

### Interface contract tests

**None.** No `Linnaean*` interface gains a `placedInOptional()` default,
so no taxonomy-kernel test changes are needed.

### End-to-end smoke

Add to `InsectFamilyTest` (or a new `InsectFamilyClassificationTest`
if separation helps):

- `papilionidaeFamilyResolvesItsCladePlacement` — construct an `InsectFamily`
  named `"papilionidae"` with `placedIn = new Papilionidae()`,
  call `CladeTraversal.findTrait(family.placedInOptional().get(),
  MetabolyTrait.class, InsectClades::traitsFor)`, assert the trait is
  present and equal to `new MetabolyTrait(new Holometabolous())`.

This is the first test that exercises the full Phase 1 → 2 → 3 chain
end-to-end. It's the smoke for "the design fits together."

---

## Steps

- [ ] Add `clades` dependency to `domains/insects/insects-api/pom.xml`
  (not to `kernels/taxonomy/pom.xml`).
- [ ] Update `InsectFamily.java`:
  - Add `@Nullable Clade placedIn` component (between `commonNames`
    and `egg`).
  - Add freestanding `Optional<Clade> placedInOptional()` (no `@Override`).
  - Add `withPlacedIn(@Nullable Clade)`.
  - Update all four existing `with*` methods to pass `placedIn` through.
- [ ] Update `InsectGenus.java` — same set of changes.
- [ ] Update `InsectSpecies.java` — same set of changes (freestanding
  `placedInOptional()` was already the planned shape here).
- [ ] Walk every other call site (`grep -l 'new InsectFamily(\|new InsectGenus(\|new InsectSpecies('` outside worktrees) and add `null` at the new component position:
  - `InsectFamilyTest.java`
  - `InsectGenusTest.java`
  - `InsectAggregateTest.java`
  - `FamilyRepositoryTest.java`
  - `GenusRepositoryTest.java`
  - `SpeciesRepositoryTest.java`
  - `SpeciesCommandImplTest.java`
- [ ] Add the new test cases — placedIn roundtrip, placedInOptional
  present/absent, withPlacedIn, withEggPreservesPlacedIn — to
  `InsectFamilyTest`, `InsectGenusTest`, and (the file owning
  `InsectSpecies` invariants).
- [ ] Add the end-to-end smoke test
  `papilionidaeFamilyResolvesItsCladePlacement`.
- [ ] User runs `mvn verify`. Expect green.
- [ ] Commit.
- [ ] Roll work-tracker forward — Phase 3 ✅, Phase 4 next.

---

## Out of scope

- **Filling in placedIn data.** No JSON entry gains `"placedIn": "…"`
  in Phase 3. That's Phase 4 — and starts with a single entry
  (papilionidae) for the *Battus philenor* validation case.
- **Touching `kernels/taxonomy/`.** No changes to `LinnaeanFamily`,
  `LinnaeanGenus`, `LinnaeanSpecies`, `LinnaeanSubspecies`, their pom,
  or their tests.
- **`InsectSpecies implements LinnaeanSpecies`.** Pre-existing gap;
  out of scope.
- **Plants-side updates.** Plants does not gain `placedIn` in Phase 3.
  If/when plants wants clade integration, it gets its own phase
  following the same per-domain pattern.
- **Backfilling clade permits.** Phase 3 doesn't add new clade permits.
  If a placement we want isn't covered by the seven Phase 1 permits, the
  fix is to add a permit in a separate phase. Out of scope here.
- **Routing life-stage queries through the resolver.** That's Phase 5
  (`InsectLifeStages.placementOf` / `stagesOf` from the integration
  walkthrough). Phase 3 just opens the door.

---

## Risks

- **Constructor argument count grows by one.** Every existing
  `new InsectFamily(…)` site must add `null` in the new position. If a
  call site is missed the build fails loudly with a constructor-arity
  error — easy to catch via `mvn verify` and fix in the same PR.
- **Internal `with*` mutators pass placedIn through unchanged.** Easy
  to forget on one of the existing mutators (especially in long files);
  if missed, the mutator silently drops the placement. The new
  `withPlacedIn` round-trip test catches this for itself; the existing
  `withEgg` / `withLarva` / `withPupa` / `withAdult` tests catch their
  own. If existing tests don't assert that `placedIn` survives a
  `withEgg` call, the regression slips. **Mitigation:** add at least
  one test confirming the round-trip — e.g.
  `withEggPreservesPlacedIn` — once per record.
- **JSON roundtrip with `null` placedIn.** Jackson 2.19 on records:
  unspecified JSON field → record component is `null`. Already the
  pattern for `egg` / `larva` / `pupa` / `adult`; no new risk surface,
  but the placedIn roundtrip test confirms the property for the new
  component too.

---

## Done-when

- `mvn verify` green at repo root.
- An `InsectFamily` constructed with `placedIn = new Papilionidae()`
  satisfies `family.placedInOptional().isPresent()` and roundtrips
  through Jackson with the JSON field `"placedIn": "papilionidae"`.
- `CladeTraversal.findTrait(family.placedInOptional().get(),
  MetabolyTrait.class, InsectClades::traitsFor)` returns
  `Optional.of(new MetabolyTrait(new Holometabolous()))` for the
  Papilionidae-placed family — exercises Phases 1 + 2 + 3 end-to-end.
- `kernels/taxonomy/` is untouched. `kernels/clades/` is untouched.
  The only modules with diff are `domains/insects/insects-api/` and
  any test modules that construct insect records.
- Work-tracker rolls to "Phase 4 — place insects into the clade DAG"
  as the next slice.
