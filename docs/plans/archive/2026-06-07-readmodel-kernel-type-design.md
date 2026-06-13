# `ReadModel` kernel type + retype/rename insect read-models — design

**Date:** 2026-06-07
**Status:** Approved design; implementation plan to follow.
**Origin:** Bounded-context review
[`docs/notes/2026-06-03-insect-aggregate-bounded-context-review.md`](../notes/2026-06-03-insect-aggregate-bounded-context-review.md)
("kernel gap: no read-model/projection type") and the R7 fork.

## Problem

The `Insect` composition and the sealed `InsectAggregate` family (four rank
permits = rank entity + its `ImageCollection`) wear the `Aggregate` marker but
are **not** Evans aggregates. They are assembled at read time from
independently-persisted entities (rank records, `InsectImage`, `LifeStage` —
each in its own repository), are immutable, enforce no invariant that spans
their members, and are never the unit of a write/transaction (`Insect` has no
identity at all). They are read models. They carry `Aggregate` only because the
kernel offers no better slot among its five markers (`NamedEntity` / `Entity` /
`Aggregate` / `ValueObject` / `BehavioralCollection`).

The mislabel is load-bearing harm: calling them `Aggregate` invited the
cross-rank FK invariant machinery the review found tautological — the type's
name created gravity to make it act like a consistency boundary it isn't.

`Zone` and `SoilProfile` are, by contrast, **genuine aggregates** (they own
child collections and maintain consistency across them via mutators like
`withUpdatedSubZone` / `withAddedLabAnalysis`). They stay `Aggregate`. So after
this change, `Aggregate` and `ReadModel` each have correct, real users.

## Principle

Give read-side compositions an honest type. A `ReadModel` is an `Observable`
whose `invariants()` assert the **structural well-formedness of a projection**,
not the consistency of a transactional boundary. `Aggregate` is reserved for
true consistency boundaries (owns children, mutated as a unit).

## Scope

In scope: add the `ReadModel` kernel type; add a `Constraints.readModel(...)`
descent method; retype `Insect` + the `InsectAggregate` family to `ReadModel`;
rename the `*Aggregate` family to `*View`; update the identity-model docs.

Explicitly **out of scope** (parked, separate concerns):

- **R4/R5** — `Insect`'s `with*` mutators and the observations-vs-taxon
  conflation. We retype `Insect`; we do **not** reshape it. `Insect` keeps its
  name (it is not a `*Aggregate`); its eventual rename/reshape rides with R4/R5.
- **`Zone`, `SoilProfile`** — genuine aggregates; untouched.
- **`@AggregateRoot` annotation** (on `InsectSpecies`, `Compound`,
  `SoilProfileInfo`) — orthogonal (marks a root entity); untouched.
- **No ADR** — documentation-only updates to the CLAUDE.md files.

## Design

### 1. The kernel type

```java
// kernels/framework/src/main/java/com/naturalist/ddd/ReadModel.java
public interface ReadModel extends Observable {}
```

Javadoc states the contract distinguishing it from `Aggregate`:

- Assembled at read time from already-persisted parts that live in their own
  repositories; **never the unit of a write/transaction**.
- Immutable; **identity is optional** (a read model may have none — e.g.
  `Insect`).
- **Not a consistency boundary.** `invariants()` validates structural
  well-formedness of the projection (required parts non-null, monotonic-fill,
  FK-of-the-parts agreement) — not cross-entity consistency owned by this type.
- Contrast clause pointing at `Aggregate` (consistency boundary, owns children,
  mutated as a unit — `Zone`, `SoilProfile`).

### 2. `Constraints.readModel(...)` descent method

`Insect.invariants()` descends into the four rank views via `.aggregate(...)`;
once those are `ReadModel`, the `aggregate(A extends Aggregate, …)` overload no
longer accepts them. Add a parallel pair mirroring the existing `aggregate(...)`:

```java
public <R extends ReadModel> Constraints readModel(R readModel, String name) {
    return readModel(readModel, Function.identity(), name);
}
public <O, R extends ReadModel> Constraints readModel(O o, Function<O, R> valueFunction, String name) {
    // same body shape as aggregate(...) — descend into the child's invariants()
}
```

Swap the four `.aggregate(order|family|genus|species, …)` calls in
`Insect.invariants()` to `.readModel(...)`. Preferred over reusing the generic
`observable(...)` — symmetric and self-documenting.

### 3. Retype + rename mapping

| Today | Becomes | Type after |
|---|---|---|
| `Insect` | `Insect` *(name unchanged)* | `implements ReadModel` |
| sealed `InsectAggregate` | `InsectTaxonView` | `extends ReadModel` |
| `InsectSpeciesAggregate` | `InsectSpeciesView` | `implements InsectTaxonView` |
| `InsectGenusAggregate` | `InsectGenusView` | `implements InsectTaxonView` |
| `InsectFamilyAggregate` | `InsectFamilyView` | `implements InsectTaxonView` |
| `InsectOrderAggregate` | `InsectOrderView` | `implements InsectTaxonView` |
| `InsectAggregateFactory` (core) | `InsectTaxonViewFactory` | — |
| `InsectQuery.InsectAggregateQuery` | `InsectQuery.TaxonViewQuery` | — |
| `InsectQuery.insect()` accessor | `InsectQuery.taxonView()` | — |
| `InsectAggregateQueryImpl` (core) | `TaxonViewQueryImpl` | — |
| `InsectAggregateTest` / `InsectAggregateFactoryTest` / `InsectAggregateQueryImplTest` | `InsectTaxonViewTest` / `InsectTaxonViewFactoryTest` / `TaxonViewQueryImplTest` | — |

Pure rename + retype — no behavior change. The permits' own `invariants()`
(`namedEntity(rank) + behavioralCollection(images)`) are unchanged. Any test
that observes a view via `mo.namedEntity(...)` switches to `mo.observable(...)`
(a read model is not a `NamedEntity`/`Aggregate`); tests already using
`mo.observable(...)` are unaffected.

The change is self-contained to `kernels/framework` + `domains/insects`
(`insects-api`, `insects-core`, and the insect test classes). The console never
calls `insect()`, so it is untouched. The implementation plan must grep the repo
for every reference to `InsectAggregate`, `InsectSpeciesAggregate` (and the
other three), `InsectAggregateFactory`, `InsectAggregateQuery`, and `.insect()`
to catch all call sites (per the lesson from the rank-FK effort: enumerate call
sites repo-wide, including `*-test-context` wiring and `domains/insects/CLAUDE.md`).

### 4. Documentation updates (no ADR)

- `domains/CLAUDE.md` — identity model: "exactly one of `NamedEntity`, `Entity`,
  `Aggregate`, `ValueObject`, `BehavioralCollection`" → add `ReadModel` (now
  six), with a one-line description and the `Aggregate`-vs-`ReadModel` contrast.
- top-level `CLAUDE.md` — the "Identity" / non-negotiables list and the
  "Records for …" line that enumerates the markers.
- `kernels/CLAUDE.md` — add `ReadModel` to the framework kernel's type list and
  the "Testing Observables" enumeration.
- `domains/insects/CLAUDE.md` — update the `InsectAggregate` vocabulary entry to
  `InsectTaxonView` and describe it as a read model.

### 5. Testing

Per the kernel testing convention, `ReadModel` and `Constraints.readModel(...)`
are exercised through their first consumers — the migrated `Insect` and rank
view tests (which observe via `mo.observable(...)` and walk the constraint
graph). No dedicated kernel test is added (real consumers exist immediately).

## Risks / notes

- The rename touches the public api surface of `InsectQuery` (`taxonView()` /
  `TaxonViewQuery`). It has no non-test consumers today (the console chain-walks
  instead), so the blast radius is the insects module's own tests + test-context
  wiring.
- This does not resolve the deeper "is `Insect` a taxon or a sighting?" question
  (R5) or the `with*`-mutator question (R4) — by design. It makes the *type*
  honest so those reshapes happen against a correct vocabulary later.
