# ADR-011: Behavioral Collections

**Status:** Draft

## Context

Any public-facing read boundary — a query port, a service method — that returns multiple
results faces a predictable downstream problem: every consumer independently reimplements
the same sort, filter, group, and aggregation logic against a raw `List<T>`. This
scatters domain vocabulary across call sites, violates DRY, and forces consumers to
reason at the level of stream mechanics rather than domain intent.

Repositories are explicitly excluded from this concern. Repository interfaces are
package-private, structurally simple by ADR-001, and exist only to feed raw entities
to the query layer. The query is the assembly point; the repository is the source.

## Decision

Any public-facing method that returns multiple results returns a behavioral collection
rather than a raw `List<T>`, `Set<T>`, or `Collection<T>`.

### Abstract Base — `BehavioralCollection<T extends Observable>`

All behavioral collections extend a common abstract base class in `kernels/framework`.
The type bound `T extends Observable` is not incidental — every domain type (Entity,
Aggregate, ValueObject) implements `Observable`, so the collection can hold any of them
while retaining access to `invariants()` on each member.

```java
// kernels/framework
public abstract class BehavioralCollection<T extends Observable> implements Observable {

    private final List<T> elements;

    // Protected — subclass constructors in other packages must call super(...)
    // Package-private instantiation is enforced at the concrete subclass level
    protected BehavioralCollection(Collection<T> elements) {
        this.elements = List.copyOf(elements);   // defensive copy, once, here
    }

    /** Content access — consumers compose further operations via Stream. */
    public Stream<T> stream() {
        return elements.stream();
    }

    public boolean isEmpty() { return elements.isEmpty(); }
    public int size()        { return elements.size(); }

    @Override
    public Consumer<? extends Invariants> invariants() {
        return i -> i.notNull(this, c -> ((BehavioralCollection<?>) c).elements, "elements");
    }
}
```

The `protected` constructor is a deliberate exception to the package-private constructor
rule in ADR-012. Subclasses live in `<domain>-api` modules that are separate packages
from `kernels/framework`; `protected` is the minimum visibility that permits `super(...)`
calls across that package boundary while still denying direct instantiation to consumers.
The concrete subclass constructor is package-private, preserving the enforcement at the
domain level.

### Concrete Collection Types

Domain modules extend `BehavioralCollection` with a final class. The `List.copyOf`
defensive copy is inherited from the base constructor — it does not appear in subclasses.

```java
// chemistry-api
public final class CompoundCollection extends BehavioralCollection<Compound> {

    // Package-private — only the query adapter (same package) constructs directly
    CompoundCollection(Collection<Compound> compounds) {
        super(compounds);
    }

    public static CompoundCollection of(Collection<Compound> compounds) {
        return new CompoundCollection(compounds);
    }

    public static CompoundCollection empty() {
        return new CompoundCollection(List.of());
    }

    /** Filtering — returns a new CompoundCollection containing only matches. */
    public CompoundCollection withSolubilityAbove(double thresholdGramsPerLitre) {
        return new CompoundCollection(
                stream()
                        .filter(c -> c.solubilityProfile().solubility() > thresholdGramsPerLitre)
                        .toList()
        );
    }

    /** Grouping — partitions by a domain concept. */
    public Map<CompoundType, List<Compound>> groupByType() { ... }

    /** Single extraction — the one element that best matches a domain criterion. */
    public Optional<Compound> highestMolecularWeight() { ... }
}
```

### Structure Rules

**It is not a ValueObject.** Behavioral collections implement `Observable` directly, not
`ValueObject`. A collection of entities fails ADR-013 Constraint 1 — a `ValueObject` must
not contain an `Entity`, an `Aggregate`, or a collection of either. `Observable` is the
correct root: it provides invariant checking without claiming a DDD classification the
type does not satisfy.

**It is immutable.** The backing `List<T>` component must be an unmodifiable list.
Any method that conceptually "modifies" the collection — filtering, narrowing, sorting —
returns a **new instance** of the same collection type wrapping the transformed list.
No method mutates state in place.

```java
// Correct — transformation returns a new instance
public CompoundCollection withSolubilityAbove(double threshold) {
    return new CompoundCollection(compounds.stream()
            .filter(...)
            .toList());
}

// Wrong — mutates the backing list
public void filterBySolubility(double threshold) {
    compounds.removeIf(...); // never
}
```

**Content is accessed via `Stream<T>`, not the raw list.** Every behavioral collection
exposes a `stream()` method returning `Stream<T>`. This gives consumers full compositional
power — map, filter, reduce, collect — without exposing the backing list directly.
The record component accessor (e.g. `compounds()`) remains accessible as a consequence of
the record contract, but `stream()` is the intended content access point and the one
that should appear in consumer code.

**It lives in `<domain>-api`.** The collection type is part of the public API, co-located
with the query port that returns it. It is visible to all consumers of the domain.

**Method categories.** A behavioral collection may expose:

- **Content access** — `stream()`, always present
- **Filtering** — returns a new instance of the same collection type (`withSolubilityAbove`, `withTypeOf`)
- **Aggregation** — reduces to a scalar (`size`, `isEmpty`, `totalMolecularWeight`)
- **Grouping** — partitions into a `Map` keyed by a domain concept (`groupByType`)
- **Single extraction** — `Optional<T>` for "the one that best matches a criterion" (`highestMolecularWeight`)

Not every category is required on every collection. Add methods when there is a real
consumer need, not speculatively.

**Naming.** The collection type is named `<Entity>Collection` or `<Concept>Collection`
(e.g. `CompoundCollection`, `AmendmentCollection`). Filtering methods use `with` prefix.
Extraction methods are descriptive domain terms.

### Raw Collections Are a Review Flag

A public method returning `List<T>`, `Set<T>`, or `Collection<T>` anywhere outside
a package-private repository is a code smell and a review flag. The correct fix is
to introduce or extend a behavioral collection for that return type.

## Consequences

- Domain query vocabulary is centralized in one type per result concept, not scattered
  across consumer stream chains
- `stream()` gives consumers full Java Stream API power at the call site without
  breaking encapsulation; the collection retains ownership of its contents
- Immutable transformation via new-instance returns means collections can be passed,
  filtered, and narrowed freely without defensive copying at call sites
- Behavioral collection types are the natural home for query-specific domain logic that
  does not belong on the entity itself
- Adding a new query pattern means extending the collection type in one place — all
  existing consumers gain the method without modification
- `<domain>-api` modules carry collection types alongside entity types and query ports;
  this is intentional and does not indicate scope creep
- Repositories remain simple and package-private — they are sources, not assemblers
