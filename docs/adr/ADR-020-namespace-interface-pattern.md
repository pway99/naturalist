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

### 5. Standalone concrete adapters carry the domain prefix

The bare-`EntitySubject` rule above applies **only to types nested inside a
namespace** (`InsectQuery.SpeciesQuery`) and to the N=1-collapsed ports that stand in
for one (`CultivarQuery`, `LifeStageRepository`). A nested port is already
disambiguated by its outer type: two domains' `SpeciesQuery` are
`InsectQuery.SpeciesQuery` and `PlantQuery.SpeciesQuery`, distinct at every use site.

A **standalone concrete class** has no such outer type. It sits loose in its package,
and the IDE's file-open dialog, symbol search, and auto-complete key on its simple
name alone. When insects and plants both ship a bare `SpeciesQueryImpl`,
`SpeciesRepositoryMock`, or `SpeciesTestEntitySource`, those simple names collide —
every navigation lands on a disambiguation prompt. So:

> **Standalone concrete adapters, sources, mocks, and contract-tests carry the domain
> prefix; nested namespace ports stay bare.**

Concretely, prefix `<DomainNoun>` onto the simple name of every:

- query/command adapter — `InsectSpeciesQueryImpl`, `PlantCultivarQueryImpl`
- repository mock — `InsectSpeciesRepositoryMock`
- test-entity-source — `PlantImageTestEntitySource`
- behavioral contract test — `InsectSpeciesRepositoryTest`,
  `InsectFeatureEntityRepositoryTest`
- the concrete unit test of any of the above — `InsectSpeciesQueryImplTest`,
  `PlantCultivarTestEntitySourceTest`, `InsectSpeciesRepositoryMockTest`

Leave unchanged: the outer namespace types (`InsectQuery`, `InsectRepository`,
`InsectEntityCollections`) and their impls (`InsectQueryImpl` — already prefixed), the
nested ports (`SpeciesQuery`, `ImageRepository`), N=1-collapsed ports (`CultivarQuery`,
`LifeStageRepository`), and the shared entity/value types (`InsectSpecies`,
`Species`-subjected collections).

Rationale: the namespace `interface`/`class` *is* the disambiguator for the types it
encloses, so repeating the prefix there would be noise (`InsectQuery.InsectSpeciesQuery`
reads worse and buys nothing). A standalone class carries no such context; the prefix is
the only thing that makes `InsectSpeciesQueryImpl` findable without collision. The two
rules are the same principle — *disambiguate at the point of navigation* — applied to two
different structures.

### Naming

| Outer                           | Type        | Visibility      | Nested                                                  |
|---------------------------------|-------------|-----------------|---------------------------------------------------------|
| `<DomainNoun>Repository`        | `class`     | package-private | `<EntitySubject>Repository` (`protected interface`)     |
| `<DomainNoun>Query`             | `interface` | public          | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public          | `<EntitySubject>Collection` (`final class`)             |

`EntitySubject` drops the domain prefix — `InsectSpecies` → `Species`. Outer namespace
carries the prefix.

Standalone concrete classes (section 5) invert this: they carry `<DomainNoun><EntitySubject>`.

| Standalone class    | Example                                                    |
|---------------------|-----------------------------------------------------------|
| query/command impl  | `InsectSpeciesQueryImpl`, `PlantCultivarQueryImpl`        |
| repository mock     | `InsectSpeciesRepositoryMock`                              |
| test-entity-source  | `PlantImageTestEntitySource`                               |
| contract test       | `InsectSpeciesRepositoryTest`                              |
| concrete unit test  | `InsectSpeciesQueryImplTest`, `PlantCultivarTestEntitySourceTest` |

### Revisit when

- Nested value object becomes cross-domain referenced → promote.
- Aggregate file > ~600 lines and hard to navigate after splitting.
- Namespace accumulates > ~6 nested types → split the package into sub-contexts.
- New visibility requirement the current class/interface mix can't express.
