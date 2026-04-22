# ADR-020: Namespace Pattern for API Surface

**Status:** Accepted
**Full rationale:** [rationale/ADR-020-namespace-interface-pattern.md](rationale/ADR-020-namespace-interface-pattern.md)

## Decision

Three coordinated patterns organize the api surface around discoverability, ownership,
and visibility.

### Key subtlety
Nested types inside a Java `interface` are implicitly `public static` — visibility
cannot be restricted. Nested types inside a `class` honor their declared access
modifier. Pick the outer type based on whether nested types should be public (queries —
yes, interface) or restricted (repositories — yes, class).

### 1. Internal contracts: namespace class (repositories)
- Group ≥ 2 entity repositories under a package-private non-instantiable class
- Nested repository interfaces are `protected` (package-private + subclass access)
- Private constructor; stateless

```java
class InsectRepository {
    private InsectRepository() {}
    protected interface SpeciesRepository extends EntityRepository<...> {}
    protected interface ImageRepository extends EntityRepository<...> {}
}
```

### 2. Public contracts: namespace interface (queries)
- Group ≥ 2 queries (+ aggregate query) under a public interface
- Outer interface is the consumer entry point; nested types are implicitly public — *desired*

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

### 3. Public return types: namespace interface (collections)
- Group ≥ 2 `BehavioralCollection` return types under public interface
  `<DomainNoun>EntityCollections`
- Plural `EntityCollections` avoids domain-meaning clash with singular (e.g.
  "InsectCollection" = museum specimen collection)
- Nested-class constructors package-private; `of(...)` / `empty()` are public paths

### 4. Aggregate value-object nesting
A `ValueObject` exclusively reachable through a single `Entity`/`Aggregate` nests inside
that entity's file as `static record`.

**Eligibility:** no independent lifecycle, not referenced cross-domain by name, appears
in only one entity's component graph.

**Promotion rule:** promote back to top-level when any condition changes (most often:
reused across entities).

### Single-entity collapse (N=1)
Skip the namespace. Declare top-level:
- `interface <Entity>Repository extends EntityRepository<...>` (package-private)
- `public interface <Entity>Query extends EntityQuery<...>`

### Naming
| Outer | Type | Visibility | Nested |
|---|---|---|---|
| `<DomainNoun>Repository` | `class` | package-private | `<EntitySubject>Repository` (`protected interface`) |
| `<DomainNoun>Query` | `interface` | public | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public | `<EntitySubject>Collection` (`final class`) |

`EntitySubject` drops the domain prefix — `InsectSpecies` → `Species`. Outer namespace
carries the domain prefix.

## Reference implementation
`domains/insects/insects-api/src/main/java/com/naturalist/insects/`:

| File | Pattern |
|---|---|
| `InsectQuery.java` | namespace interface (queries, public) |
| `InsectRepository.java` | namespace class (repositories, package-private) |
| `InsectEntityCollections.java` | namespace interface (collections, public) |
| `InsectSpecies.java` | aggregate root + nested value-object graph |

## Consequences

- Discoverability up: `InsectQuery` = read entry point; `InsectEntityCollections` =
  return types; `InsectRepository` = internal writes, never exposed
- Visibility structural and honest — repository contracts package-private at source AND bytecode
- Import surface collapses — one import reaches the whole value-object graph
- Aggregate files grow (nesting value graph can hit ~470 lines; tolerable — promote out
  if navigation suffers)
- Single-entity packages stay simple — namespace threshold is N ≥ 2

## Revisitation signals
- Nested value object becomes cross-domain referenced → promote
- Aggregate file > ~600 lines and still hard to navigate after splitting
- Namespace accumulates > ~6 nested types → package itself should split into sub-contexts
- New visibility requirement the current class/interface mix can't express

## Related ADRs
ADR-001, ADR-002, ADR-010, ADR-011, ADR-013.
