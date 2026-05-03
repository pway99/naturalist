# ADR-020: Namespace Pattern for API Surface

**Status:** Accepted

## Context

A domain api package accumulates one Java file per repository, query, return-type
collection, and value object. For a domain with two entities and one aggregate, the
flat package directory contains 17+ files: entities, child entities, value objects,
repositories, queries, collections, factories. A consumer scanning the directory
cannot tell at a glance what the entry point is or which types are internal
scaffolding.

The team experimented with a single super-interface holding nested entity
repositories (`@Incubating` for several weeks). That experiment proved the value of
collapsing related types into a namespace. The same problem exists in three other
places:

- **Queries** — one entry point per consistency boundary; a domain with N entities
  produces N query files plus an aggregate-query file
- **Return-type collections** — one `BehavioralCollection` subclass per entity
- **Aggregate-owned value objects** — N value objects in N files even when every
  one is exclusively reachable through a single Entity

A subtlety surfaced during evaluation: a nested type inside a Java `interface` is
implicitly `public static`. Marking the outer `interface` package-private hides
only its outer name; the nested types remain bytecode-public. For repositories —
which must be hidden from foreign packages — this is structurally weaker than
the original sibling-files pattern, where every file could carry its own
package-private modifier.

The fix is to use a `class` (not `interface`) as the namespace for repositories.
A nested type inside a class honors its declared access modifier. `protected`
or package-private keeps nested repository contracts hidden at both source and
bytecode level while still permitting same-package adapter implementations.

## Decision

Three coordinated patterns organize the api surface around discoverability,
ownership, and visibility.

### 1. Internal Contracts: Namespace Class (Repositories)

Group two or more entity repositories under a single non-instantiable
package-private class. Nested repository interfaces are `protected`, which is
package-private + accessible to subclasses (permitting future composite
implementations such as a single class that implements every nested contract).

```java
class InsectRepository {

    private InsectRepository() {}

    protected interface SpeciesRepository
            extends EntityRepository<InsectSpeciesId, InsectSpeciesName, InsectSpecies> {}

    protected interface ImageRepository
            extends EntityRepository<InsectImageId, InsectImageName, InsectImage> {}
}
```

When a package contains exactly one entity, the namespace adds no value — declare
the repository as a top-level package-private interface:

```java
interface ElementRepository extends EntityRepository<ElementId, ElementName, Element> {}
```

Why a class for the namespace and not an interface:

- Nested types in an `interface` are implicitly `public static` — visibility cannot be restricted
- Nested types in a `class` honor their declared access modifier
- The class is non-instantiable (private constructor) and stateless — purely a namespace
- `protected` on the nested interfaces yields package-private + subclass access; package-private (no modifier) is also
  acceptable when no subclass implementation is anticipated

### 2. Public Contracts: Namespace Interface (Queries)

Group two or more entity queries (and the aggregate query) under a single public
interface. The outer interface is the consumer-facing entry point — public; the
implicitly-public nested types are *desired* here because consumers reference
them directly as return types.

```java
public interface InsectQuery {

    InsectAggregateQuery insect();
    SpeciesQuery         species();
    ImageQuery           images();

    interface InsectAggregateQuery extends AggregateQuery<InsectSpeciesId, InsectSpeciesName, InsectAggregate> {}
    interface SpeciesQuery         extends EntityQuery<InsectSpeciesId, InsectSpeciesName, InsectSpecies, SpeciesCollection> {}
    interface ImageQuery           extends EntityQuery<InsectImageId, InsectImageName, InsectImage, ImageCollection> {}
}
```

Single-entity package: declare as a top-level public interface, no namespace.

The asymmetry with repositories (interface here, class there) is intentional. The
type that's right for the job depends on whether the nested types should be
public (queries — yes) or restricted (repositories — yes).

### 3. Public Return Types: Namespace Interface (Collections)

Group two or more `BehavioralCollection` return types under a public interface
named `<DomainNoun>EntityCollections`:

```java
public interface InsectEntityCollections {

    final class SpeciesCollection extends BehavioralCollection<InsectSpecies> {
        SpeciesCollection(Collection<InsectSpecies> species) { super(species); }
        public static SpeciesCollection of(Collection<InsectSpecies> species) { return new SpeciesCollection(species); }
        public static SpeciesCollection empty() { return new SpeciesCollection(List.of()); }
    }

    final class ImageCollection extends BehavioralCollection<InsectImage> { ... }
}
```

The plural `EntityCollections` suffix is deliberate: it avoids clashing with
domain meanings of the singular form (an "InsectCollection" is a museum specimen
collection, not an api return type). Nested-class constructors stay
package-private; static `of(...)` and `empty()` are the public construction surface.

### 4. Aggregate Value-Object Nesting

A value object exclusively reachable through a single `Entity` or `Aggregate`
nests inside that entity's file as a `static` `record`:

```java
public record InsectSpecies(
        ...
        @Nullable LifeStages lifeStages,
        @Nullable BeneficialProfile beneficialProfile,
        ...
) implements CatalogEntity<InsectSpeciesId, InsectSpeciesName> {

    public record LifeStages(@Nullable EggStage egg, @Nullable LarvaStage larva, AdultStage adult)
            implements ValueObject {
        public record EggStage(...)   implements ValueObject {}
        public record LarvaStage(...) implements ValueObject {}
        public record AdultStage(...) implements ValueObject {}
    }
    public record BeneficialProfile(...) implements ValueObject {}
    ...
}
```

**Eligibility:**

- The value object has no independent lifecycle (never persisted standalone, no `EntityName`)
- It is not referenced cross-domain by name
- It appears in only one entity's component graph

**Promotion rule:** if any condition above changes — most often a value object
becoming reused across entities — promote it back to top-level. Nesting is the
default until ownership becomes plural.

### Naming Conventions

| Outer                           | Type        | Visibility      | Nested                                                  |
|---------------------------------|-------------|-----------------|---------------------------------------------------------|
| `<DomainNoun>Repository`        | `class`     | package-private | `<EntitySubject>Repository` (`protected` interface)     |
| `<DomainNoun>Query`             | `interface` | public          | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public          | `<EntitySubject>Collection` (final class)               |

`EntitySubject` is the distinctive part of the entity's class name with the
domain prefix dropped — `InsectSpecies` → `Species`, `InsectImage` → `Image`.
The outer namespace carries the domain prefix.

When N=1 (single-entity package), skip the namespace and use a top-level
declaration: `interface <Entity>Repository extends EntityRepository<...>`.

## Consequences

- **Discoverability up.** `InsectQuery` is the read entry point and commands are
  the entry point for persistence concerns; `InsectEntityCollections` holds
  return types. `InsectRepository` is the package-private namespace for internal
  write contracts and is never exposed to consumers.
- **Visibility is structural and honest.** Repository contracts are package-private
  at both source and bytecode level. Queries and collections are public — the
  whole nested graph is intended as the consumer surface.
- **Import surface collapses.** Reaching the `InsectSpecies` value-object graph
  takes one import. The dot-path expresses ownership: `species.lifeStages().adult()`.
- **Aggregate files grow.** Nesting the value graph can push an entity file to
  ~470 lines. Tolerable; promote a value object back out if a single entity becomes
  hard to navigate.
- **Single-entity packages stay simple.** The "N >= 2" threshold avoids namespace
  ceremony where there's nothing to group.
- **Migration is opportunistic.** Existing single-entity packages that already use
  the `<Outer>Repository.<Outer>EntityRepository` shape (e.g. `chemistry/element`)
  may collapse to a top-level `<Outer>Repository` when the package is next touched —
  not as a dedicated PR.

## Reference Implementation

`domains/insects/insects-api/src/main/java/com/naturalist/insects/`:

| File                           | Pattern                                         |
|--------------------------------|-------------------------------------------------|
| `InsectQuery.java`             | namespace interface (queries, public)           |
| `InsectRepository.java`        | namespace class (repositories, package-private) |
| `InsectEntityCollections.java` | namespace interface (collections, public)       |
| `InsectSpecies.java`           | aggregate root + nested value-object graph      |

## Revisitation

This pattern is the standard moving forward. Concrete signals to revisit:

- A nested value object becomes referenced cross-domain
- An aggregate file exceeds ~600 lines and remains hard to navigate after splitting
- A namespace accumulates more than ~6 nested types — likely the package itself
  should split into sub-contexts
- A new visibility requirement emerges that the current `class`/`interface` mix
  cannot express

## Related ADRs

- ADR-001 — Repository Architecture (responsibilities, package-private interfaces)
- ADR-002 — Repository Behavioral Contract via Test Interface
- ADR-010 — Query Design Contract
- ADR-011 — Behavioral Collections
- ADR-013 — Value Object Contract
