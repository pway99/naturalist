# ADR-020: Namespace Pattern for API Surface
> [rationale](rationale/ADR-020-namespace-interface-pattern.md)

Three coordinated patterns organize the api surface. Nested types inside a Java
`interface` are implicitly `public static` (visibility cannot be restricted); nested types
inside a `class` honor declared access. Pick outer type by whether nested types must be
public (queries — interface) or restricted (repositories — class).

### 1. Repositories — namespace class (package-private)
- Group ≥ 2 entity repositories under a package-private non-instantiable class.
- Nested repository interfaces are `protected` (package-private + subclass access).
- Private constructor; stateless.

```java
class InsectRepository {
    private InsectRepository() {}
    protected interface SpeciesRepository extends EntityRepository<...> {}
    protected interface ImageRepository extends EntityRepository<...> {}
}
```

### 2. Queries — namespace interface (public)
- Group ≥ 2 queries (+ aggregate query) under a public interface.
- Nested types implicitly public — desired.

```java
public interface InsectQuery {
    InsectAggregateQuery insect();
    SpeciesQuery species();
    ImageQuery images();

    interface InsectAggregateQuery extends AggregateQuery<...> {}
    interface SpeciesQuery extends EntityQuery<...> {}
    interface ImageQuery extends EntityQuery<...> {}
}
```

### 3. Collections — namespace interface (public)
- Group ≥ 2 `BehavioralCollection` return types under `<DomainNoun>EntityCollections`.
- Plural `EntityCollections` avoids clash with singular domain meaning (e.g.
  "InsectCollection" = museum specimen collection).
- Nested-class constructors package-private; `of(...)` / `empty()` are the public paths.

### 4. Aggregate value-object nesting
A `ValueObject` reachable only through a single `Entity`/`Aggregate` nests inside that
entity's file as `static record`.

- Eligibility: no independent lifecycle, not referenced cross-domain by name, appears in
  only one entity's component graph.
- Promote back to top-level when any condition changes (most often: reused across entities).

### Single-entity collapse (N=1)
Skip the namespace:
- `interface <Entity>Repository extends EntityRepository<...>` (package-private)
- `public interface <Entity>Query extends EntityQuery<...>`

### Naming

| Outer | Type | Visibility | Nested |
|---|---|---|---|
| `<DomainNoun>Repository` | `class` | package-private | `<EntitySubject>Repository` (`protected interface`) |
| `<DomainNoun>Query` | `interface` | public | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public | `<EntitySubject>Collection` (`final class`) |

`EntitySubject` drops the domain prefix — `InsectSpecies` → `Species`. Outer namespace
carries the prefix.

### Revisit when
- Nested value object becomes cross-domain referenced → promote.
- Aggregate file > ~600 lines and hard to navigate after splitting.
- Namespace accumulates > ~6 nested types → split the package into sub-contexts.
- New visibility requirement the current class/interface mix can't express.
