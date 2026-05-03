# ADR-011: Behavioral Collections

> [rationale](rationale/ADR-011-behavioral-collections.md)

Public methods returning multiple results return a behavioral collection, not raw
`List<T>`/`Set<T>`/`Collection<T>`. Repositories excluded (package-private, feed raw
entities to queries).

**Base.** `BehavioralCollection<T extends Observable>` in `kernels/framework`.

- `Observable` bound; retains `invariants()`.
- `protected` constructor (deliberate exception to ADR-012 — subclasses live in other
  packages, need `super(...)`).
- `List.copyOf(elements)` defensive copy in base constructor; inherited by subclasses.
- Provides `stream()`, `isEmpty()`, `size()`, `invariants()`.

**Concrete collections.**

- `final class`, not record (enables package-private constructor).
- Extends `BehavioralCollection<T>`, lives in `<domain>-api`.
- Package-private constructor — only the query adapter (same package) calls it directly.
- `public static of(Collection<T>)` and `public static empty()` are the external paths.
- Immutable; `with*` transformations return new instances via the package-private constructor.

**Structure.**

- Not a `ValueObject` — implements `Observable` directly (would fail ADR-013 Constraint 1).
- Content accessed via `stream()`.
- Method categories: content access, filtering (returns new collection), aggregation
  (scalar), grouping (`Map` by domain concept), single extraction (`Optional<T>`).
- Add methods per real consumer need; not speculatively.

**Naming.** Type: `<Entity>Collection` / `<Concept>Collection`. Filters: `with` prefix.
Extractions: descriptive domain terms.

Public `List<T>`/`Set<T>`/`Collection<T>` anywhere outside a package-private repository
is a review flag.
