# Shared Kernels

The kernels are the foundation every other module builds on. Changes here ripple across
the entire codebase — treat them as stable contracts, not convenient places to add things.

## Four Kernels

### framework
DDD building blocks. `NamedEntity`, `FactEntity`, `Aggregate`, `ValueObject`,
`BehavioralCollection`, `Observable`, `Constraints`, `EntityName`, `FactName`, `Observer`.
No domain knowledge — pure structural vocabulary. Everything else depends on this.

### framework-test
Test infrastructure. `NamedTestEntitySource`, `NamedTestEntitySourceTest`,
`NamedEntityRepositoryContractTest`, `UniqueConstraint`. Used by all
`<domain>-repository-test` modules. Never a compile-scope dependency.

### field-notes  (`com.naturalist.fieldnotes`)
The Durrell principle in code. `Description` carries the same truth at four levels of
understanding — preschool, elementary, secondary, university — for any entity the system
can describe. Chemistry compounds, climate thresholds, plants, organisms: all of them.

**Every domain api depends on this.** If you are building a new named entity and it does
not have a `Description`, that is a deliberate omission worth questioning.

```java
import com.naturalist.fieldnotes.Description;
```

### taxonomy  (`com.naturalist.taxonomy`)
Linnaean classification. `TaxonomicClassification` holds order, family, genus, and species
with `binomialName()` and `isSpeciesLevel()` convenience methods. Nullable genus and species
accommodate family-level field identifications where species cannot be confirmed.

**Organism domain apis only** — insects, arachnids, worms, microbes, molluscs, vertebrates,
plants. Chemistry, climate, soil, zone, and sensors have no use for Linnaean taxonomy.

```java
import com.naturalist.taxonomy.TaxonomicClassification;
```

## DAG Position

```
field-notes  →  framework
taxonomy     →  framework

<any>-api         →  framework, identifiers, field-notes
<organism>-api    →  framework, identifiers, field-notes, taxonomy
```

## Hard Rules

- `identifiers` contains typed names only — `EntityName` subclasses (slug) and `FactName`
  subclasses (UUID). No value objects. No behavior. If it is not an identifier, it does
  not belong there.
- Domain records carry no persistence identifier — identity is the `EntityName` (ADR-021).
- `field-notes` and `taxonomy` contain shared value objects only.
  No entity definitions. No domain-specific logic.
- Do not add a new class to any kernel without considering whether it is truly
  cross-cutting. Kingdom-specific concerns belong in the domain module, not here.

## Observability Framework

All domain types implement `Observable`, which requires `Consumer<? extends Constraints> invariants()`. `Constraints` is a fluent builder with constraint methods in two forms:

**Direct-value form** — pass the component value directly. Use this inside `invariants()` on the record that owns the field (when `this` is the enclosing record):

- `notNull(value, name)` — general null check
- `notBlank(value, name)` — rejects null, empty, and whitespace-only Strings (`StringUtils.isNotBlank`)
- `entityName(e, name)` — validates `EntityName`

**By-function form** — pass the parent object and a method reference. Use this only when evaluating a child property from outside the parent (e.g. `namedEntity(this, FooAggregate::fooInfo, "fooInfo")` to descend into a child Observable):

- `namedEntity(o, fn, name)` — validates a `NamedEntity` child and descends into its invariants
- `valueObject(o, fn, name)` — validates a `ValueObject` child and descends into its invariants
- `observable(o, fn, name)` — validates any `Observable` child (e.g. a `BehavioralCollection`)
- `notNull(o, fn, name)` — general null check on a child property
- `notBlank(o, fn, name)` — not-blank check on a child String property
- `namedValue(o, fn, name)` — validates a `NamedValue<?>` child property

Do not use the by-function form when the direct-value form suffices — the functional indirection adds no value when `this` can never be null.

`Observer` validates entities at insertion points — `NamedTestEntitySource.insert()` calls `observer.arguments("insert", i -> i.namedEntity(entity, "entity")).throwWhenInvalid()`. Insertion is an argument-validation site: the producer refuses bad input at its boundary. For method-body observation of *produced* state, the producer uses `.observe()` (metrics only) and hands the value to the consumer, which chooses the terminal operation. See ADR-017's *Producer vs. consumer* section. Use `@Nullable` from JSpecify (`org.jspecify`) for nullable field documentation.

## Testing Observables

Every domain type (`NamedEntity`, `Aggregate`, `ValueObject`, `BehavioralCollection`)
implements `Observable` and declares `invariants()`. Unit tests verify invariants via
`Observer` → `MethodObserver` → `InvariantObservation` — never by calling `isValid()` on
individual `Constraint` objects. The `InvariantObservation` gives the full set of failing
invariant names in a single assertion, eliminating the need to debug which constraint broke.

One `Observer` per test class (static field). One `MethodObserver` per test method. See
any `*Test.java` in a `*-api` module for the canonical pattern.

**Valid case:** construct with all required fields populated via `RandomValue` helpers;
nullable components are null; assert `invalidInvariants().isEmpty()`.

**Invalid case:** construct with nulls for every component. Use
`invalidInvariantNamesRemovingPrefix` with `mo.observationPoint()` to strip the
test-infrastructure prefix, then assert the exact set of domain-relative invariant paths
with `containsExactlyInAnyOrder`.

Key rules:
- `mo.forMethod(...)` must match the test method name exactly — it scopes the observation
- `mo.namedEntity(e, label)` for `NamedEntity`/`Aggregate`; `mo.observable(o, label)` for any `Observable`
- Invalid-case assertions use `containsExactlyInAnyOrder` — exact set, no extras, no missing
- `observationPoint()` returns `ClassName.methodName` — the prefix before the label segment

## BehavioralCollection

Behavioral collections are `final class`, not records. They extend
`BehavioralCollection<T extends Observable>` from `kernels/framework` and live in the
owning `<domain>-api`. Constructor is package-private;
`public static of(...)` and `public static empty()` are the only external instantiation
paths. `List.copyOf` defensive copy is inherited from the base class constructor.
Domain-specific filtering methods return new instances via the package-private constructor.
See ADR-011 and ADR-012. Reference implementation: `CompoundCollection` in `chemistry-api`.
