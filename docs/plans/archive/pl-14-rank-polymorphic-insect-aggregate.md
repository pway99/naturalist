# PL-14 — Rank-Polymorphic `InsectAggregate`

> **Status:** drafted 2026-05-23, executing in-session. Picks up the parking-lot
> entry [PL-14](../notes/parking-lot.md#pl-14--rank-polymorphic-insectaggregate).

**Goal.** Let `InsectAggregate` root at whichever rank a naturalist's
identification confidence allows — species, genus, or family — mirroring the
rank-polymorphism that `InsectImage.parentName` already carries. Today the
aggregate hardcodes `InsectSpecies` as its root and `InsectSpeciesName` as its
identity; family-/genus-rank under-identified organisms (`tachinid-fly`,
`empoasca`, `halictus`, …) have no aggregate representation, and
`insectQuery.insect().getByName(...)` silently does not apply to them.

After this slice lands, `InsectAggregate` is a sealed interface with three
record permits — one per insect-side Linnaean rank that currently has a
catalogued entity. The aggregate query widens to `getByName(InsectRankName)`
and pattern-matches at the factory to dispatch on the rank. Consumers reading
the polymorphic result destructure via `switch` over the sealed permits.

This slice is **internal to the insects domain**. The console composes its own
view models from entity queries (per the parking-lot note) — no UI code
touches `InsectAggregate` today.

---

## Scope decisions

### Shape: sealed interface + three record permits (Option 1)

```java
public sealed interface InsectAggregate extends Aggregate
        permits InsectFamilyAggregate, InsectGenusAggregate, InsectSpeciesAggregate {

    InsectRankName name();        // root identity, polymorphic across permits
    ImageCollection images();     // every permit composes the same image side
}
```

```java
public record InsectFamilyAggregate(InsectFamily family, ImageCollection images)
        implements InsectAggregate {
    @Override public InsectRankName name() { return family.name(); }
    @Override public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(family, "family").observable(images, "images");
    }
}
// InsectGenusAggregate, InsectSpeciesAggregate analogous
```

**Why sealed permits, not parallel records (Option 2):** mirrors the
codebase's sealed marker pattern (`InsectRankName`, `Clade`). Pattern-match
dispatch over `switch (aggregate)` is the natural consumer form — the same
shape Phase 2's session-scope value object will want when it asks "what
aggregate is rooted at the current rank?". Parallel records force three
parallel query methods and three parallel consumer paths, then collapse them
behind a hand-rolled union later.

**Why not generic `InsectAggregate<R, E>` (Option 3):** loses the per-rank
semantic distinctions Phase 2 wants. A genus aggregate exposing `genus()` is
more legible at the consumer than `entity()` typed `Object`/`E`.

**Subspecies handling:** `InsectRankName` permits `InsectSubspeciesName`, but
no `InsectSubspecies` entity exists yet (parking lot has not raised that). The
factory returns `Optional.empty()` for subspecies-rank requests — graceful
no-op until the entity lands. A `default` case in the rank `switch` covers it
explicitly with a comment pointing at this decision.

### Aggregate is now a sealed interface, not a record

Today's `InsectAggregate(species, images)` becomes `InsectSpeciesAggregate`
verbatim — same components, same invariants. Behavioral equivalence preserved
for any (test-only) caller that constructs species aggregates directly.

The sealed interface itself carries no behavior beyond the two accessors and
the inherited `Aggregate`/`Observable` contract. `invariants()` is
permit-specific: each record validates its own rank entity.

### Query widens to `InsectRankName`

```java
interface InsectAggregateQuery {
    Optional<InsectAggregate> getByName(InsectRankName name);
}
```

The single overload replaces the species-only signature. Callers that were
passing an `InsectSpeciesName` (covariant `InsectRankName` permit) compile
unchanged.

### Factory takes GenusQuery + FamilyQuery in its constructor

```java
class InsectAggregateFactory {
    InsectAggregateFactory(SpeciesQuery, ImageQuery, GenusQuery, FamilyQuery) { ... }

    Optional<InsectAggregate> buildByName(InsectRankName name) {
        return switch (name) {
            case InsectSpeciesName  n -> speciesQuery.getByName(n).map(s ->
                    new InsectSpeciesAggregate(s, imageQuery.forParentName(s.name())));
            case InsectGenusName    n -> genusQuery.getByName(n).map(g ->
                    new InsectGenusAggregate(g, imageQuery.forParentName(g.name())));
            case InsectFamilyName   n -> familyQuery.getByName(n).map(f ->
                    new InsectFamilyAggregate(f, imageQuery.forParentName(f.name())));
            case InsectSubspeciesName n -> Optional.empty();  // no entity yet
        };
    }
}
```

Each branch observes the resulting aggregate at `Level.WARN` (preserving the
existing producer/consumer observability rule).

### Out of scope

- **Console.** No file under `insects-console` or `apps/management-console`
  imports `InsectAggregate` (verified). The console's existing view-model
  composition stands.
- **Subspecies entity.** Parking lot has not raised it; the `Optional.empty()`
  branch holds the seam until it does.
- **JSON migration.** Aggregates are computed, never persisted — no catalog
  files to edit.
- **`forParentName` cross-rank lookup tests.** Already covered by Path A's
  image-parent-rank slice.

---

## Steps

Each step lands as one commit and `mvn verify` runs green between them.

### Step 1 — Sealed `InsectAggregate` + three record permits (api module)

- Convert `InsectAggregate.java` to a sealed interface; declare
  `InsectRankName name()` and `ImageCollection images()` accessors.
- Add `InsectFamilyAggregate`, `InsectGenusAggregate`, `InsectSpeciesAggregate`
  records in the same package; each implements the sealed interface.
- `InsectSpeciesAggregate` carries the same components, factories
  (`of(species)`, `of(species, images)`), and invariants as today's record.
- `InsectFamilyAggregate` / `InsectGenusAggregate` mirror the shape (with
  `family`/`genus` components instead of `species`).
- Update `InsectAggregateTest` (api): the existing `aggregateIsValid` /
  `aggregateIsNotValid` cases become per-permit tests (one per rank).
- Update the `domains/insects/CLAUDE.md` `InsectAggregate` paragraph to
  describe the sealed shape.

### Step 2 — Widen `InsectAggregateQuery` to `InsectRankName` (api module)

- Change the query method signature in `InsectQuery.InsectAggregateQuery` from
  `getByName(InsectSpeciesName)` to `getByName(InsectRankName)`.
- Update the `InsectQuery` Javadoc usage block.

### Step 3 — Factory rank dispatch (core module)

- Add `GenusQuery` and `FamilyQuery` constructor parameters to
  `InsectAggregateFactory`. Update its observer-validated constructor.
- Rewrite `buildByName` to `switch` on the `InsectRankName` permit, producing
  the correct rank aggregate per branch.
- Subspecies branch returns `Optional.empty()` with a one-line comment
  referencing PL-14.
- Update `InsectAggregateQueryImpl.getByName` signature to match.
- Update `InsectQueryImpl` constructor to pass `genusQuery` and `familyQuery`
  into the factory.

### Step 4 — Factory + query tests (core module)

- Update `InsectAggregateFactoryTest`:
  - Existing species cases unchanged (assert via the sealed-interface
    accessors and via `instanceof InsectSpeciesAggregate`).
  - Add `buildByName_genus_*` and `buildByName_family_*` cases using
    `TestInsectsIdentifiers.InsectGenus.Halictus.name` and
    `TestInsectsIdentifiers.InsectFamily.Tachinidae.name`.
  - Add `buildByName_subspecies_returnsEmpty` (synthesized
    `InsectSubspeciesName` value).
  - Add constructor null-rejection cases for the two new query parameters.
- Update `InsectAggregateQueryImplTest` to cover species + genus + family,
  plus the subspecies-empty case.

### Step 5 — Docs

- Move PL-14 entry from `notes/parking-lot.md` to
  `notes/parking-lot-resolved.md` with a `**Resolved:**` line citing the new
  plan + final commit.
- Update `docs/work-tracker.md`: clear `Current slice`, add PL-14 to
  `Recently completed`, refresh the `Last updated` line.
- Archive `docs/plans/pl-14-rank-polymorphic-insect-aggregate.md` to
  `docs/plans/archive/` (the standard end-of-slice archival).

---

## Verification

- `mvn verify` green between steps and at the end.
- New per-rank factory tests assert structural validity via the Observer
  framework (`observer.observable(agg, "agg").violations()` empty).
- Sealed exhaustiveness: the factory `switch` is exhaustive over the four
  permits — javac enforces this; no need for a runtime `default`.
