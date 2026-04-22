# ADR-011: Behavioral Collections

**Status:** Draft
**Full rationale:** [rationale/ADR-011-behavioral-collections.md](rationale/ADR-011-behavioral-collections.md)

## Decision

Any public-facing method returning multiple results returns a behavioral collection, not
raw `List<T>`/`Set<T>`/`Collection<T>`. Repositories excluded (package-private, feed
queries raw entities).

### Abstract base
- `BehavioralCollection<T extends Observable>` in `kernels/framework`
- `Observable` bound — every domain type implements it; collection retains `invariants()` access
- **`protected` constructor** (deliberate exception to ADR-012's package-private rule —
  subclasses live in other packages and need `super(...)` access)
- `List.copyOf(elements)` defensive copy in base constructor; inherited by all subclasses
- Provides `stream()`, `isEmpty()`, `size()`, `invariants()`

### Concrete collection types
- **`final class`, not record** (enables package-private constructor for structural
  enforcement — see ADR-012)
- Extends `BehavioralCollection<T>`, lives in `<domain>-api`
- **Package-private constructor** — only the query adapter (same package) constructs directly
- `public static of(Collection<T>)` and `public static empty()` are the external paths
- **Immutable.** Transformation methods (`with*`) return new instances via the
  package-private constructor; never mutate in place

### Structure rules
- Not a `ValueObject` — implements `Observable` directly (would fail ADR-013 Constraint 1)
- Content accessed via `Stream<T>` from `stream()`
- Method categories: **content access**, **filtering** (returns new collection), **aggregation**
  (scalar), **grouping** (`Map` by domain concept), **single extraction** (`Optional<T>`)
- Add methods per real consumer need, not speculatively

### Naming
- Type: `<Entity>Collection` / `<Concept>Collection` (`CompoundCollection`, `AmendmentCollection`)
- Filter methods: `with` prefix
- Extraction methods: descriptive domain terms

### Raw collections are a review flag
Public `List<T>`/`Set<T>`/`Collection<T>` anywhere outside a package-private repository is
a review flag. Fix = introduce/extend a behavioral collection.

## Consequences

- Domain query vocabulary centralized in one type per result concept
- `stream()` gives full Stream API power without breaking encapsulation
- Immutable `with*` returns allow free pass/filter/narrow without defensive copying
- Collections are the natural home for query-specific logic that doesn't belong on the entity
- New query patterns extend the collection in one place — all consumers gain the method
- `<domain>-api` modules carry collection types alongside entity types and query ports
- Repositories stay simple and package-private — sources, not assemblers
