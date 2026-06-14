# Citation Association Design

Cross-domain citation associations: any `NamedEntity` in any domain can be cited
by any `Citation` in the library domain. Citations attach at the highest appropriate
rank; children discover inherited citations by walking their domain's hierarchy.

## Entity Model

### `CitationAssociation` (library-api)

`Entity<CitationAssociationId>` — records the fact that a citation covers a named
entity.

```java
public record CitationAssociation(
        CitationAssociationId name,
        CitationName citationName,
        EntityRef subject,
        @Nullable String note
) implements Entity<CitationAssociationId> { ... }
```

- `CitationAssociationId` — new `EntityId` subclass in `domains/identifiers`.
- **Unique constraint:** `(citationName, subject)` — one citation attaches to a
  given entity at most once. The note can be updated, but the pairing is the
  constraint.
- `EntityRef` from `kernels/catalog` carries `DomainId` + `EntityName`, fully
  qualifying the subject: `(insects, lepidoptera)` vs `(plants, some-plant)`.
- Jackson dispatch for `EntityRef`'s `DomainId` uses `@JsonTypeInfo` external
  property on the `subject` field — same pattern as `InsectImage.parentName`
  with `InsectRankName`. Unlike sealed `InsectRankName`, `DomainId` is open —
  the `@JsonSubTypes` on the field lists the concrete `DomainId` subtypes
  relevant to the citation association (initially `InsectsDomain`; extended
  as other domains gain citations). Test fixtures and composition root both
  need the concrete subtypes on the classpath.

### `CitationAssociationCollection` (library-api)

`BehavioralCollection<CitationAssociation>` — standard collection type returned
by queries. `final class`, package-private constructor, `of(...)` and `empty()`
factories.

## Repository and Query (library domain)

### Repository (package-private, library-api)

```java
interface CitationAssociationRepository
        extends EntityRepository<CitationAssociationId, CitationAssociation> {

    List<CitationAssociation> getByCitationName(CitationName citationName);
    List<CitationAssociation> getBySubject(EntityRef subject);
}
```

Two domain-specific queries beyond inherited CRUD:
- `getByCitationName` — "what entities does this citation cover?"
- `getBySubject` — "what citations cover this entity?"

### Query (public, library-api)

```java
public interface CitationAssociationQuery {

    CitationAssociationCollection findByCitationName(CitationName citationName);
    CitationAssociationCollection findBySubject(EntityRef subject);
}
```

### Query Adapter (library-core)

`CitationAssociationQueryImpl` — `@DomainService`. Thin: validates arguments via
observer, delegates to repository.

## Test Infrastructure (library domain)

### `CitationAssociationId` (domains/identifiers)

Standard `EntityId` subclass: `create()` factory using `EntityId.newUUID()`,
`of(UUID)` with `@JsonCreator` for deserialization.

### TestLibraryIdentifiers additions (domains/identifiers-test)

New inner class nested under `Citations` (associations are children of the
citation sub-context):

```java
public static class Citations {
    // ... existing CitationName constants ...

    public static class Associations {
        public static final CitationAssociationId EolSwallowtailOnLepidoptera = ...;
        public static final CitationAssociationId EolSwallowtailOnPapilionidae = ...;

        public static class NotFound {
            public static final CitationAssociationId name = ...;
        }
    }
}
```

### CitationAssociationTestEntitySource (library-repository-test)

Loads from `library/citation-associations.json`. JSON carries `citationName` as a
slug, `subject` as a `(domain, name)` pair with domain discriminator, optional
`note`.

### CitationAssociationEntityRepositoryTest (library-repository-test)

Behavioral contract interface extending framework contract test. Hooks:
`repository()`, `source()`, `notFoundName()`, `knownEntityNames()`,
`newEntity()`, `ghostEntity()`, `modifiedEntity(original)`.

### CitationAssociationRepositoryMock (library-repository-test)

In-memory implementation enforcing the `(citationName, subject)` unique
constraint.

## Insect-Specific Citation Discovery

Domain-specific layer — insects walk the Linnaean hierarchy to collect inherited
citations. Lives in `insects-api` (types) and `insects-core` (query adapter).

### InsectCitationView (insects-api)

`ReadModel` preserving which rank each citation was attached at:

```java
public record InsectCitationView(
        InsectRankName subject,
        List<RankedCitation> citations
) implements ReadModel { ... }
```

Nested value object:

```java
public record RankedCitation(
        CitationName citationName,
        InsectRankName attachedAt,
        @Nullable String note
) implements ValueObject { ... }
```

A species query for `battus-philenor` returns:
- Direct citations on the species (`attachedAt = battus-philenor`)
- Inherited from genus Battus (`attachedAt = battus`)
- Inherited from family Papilionidae (`attachedAt = papilionidae`)
- Inherited from order Lepidoptera (`attachedAt = lepidoptera`)

### InsectQuery.CitationQuery (insects-api)

Nested inside the existing `InsectQuery` namespace:

```java
interface CitationQuery {
    InsectCitationView findByRankName(InsectRankName rankName);
}
```

### Query Adapter (insects-core)

`@DomainService` class that:
1. Resolves full ancestry (species -> genus -> family -> order) via existing
   insect repositories
2. Calls `citationAssociationQuery.findBySubject(entityRef)` for each rank
3. Assembles `InsectCitationView` with `attachedAt` provenance

O(4) ancestry lookups at most (fixed-depth Linnaean chain). Each
`findBySubject` is one query against the library's association store.

No factory needed — straightforward serial collect, handled directly by the
query adapter.

## Dependency Changes

### library-api/pom.xml — one new compile dependency

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>catalog</artifactId>
</dependency>
```

Justified: citation associations are cross-domain references, which is what the
catalog kernel's vocabulary exists for. Also naturally positions the library
domain for future catalog participation (`LibraryDomain implements DomainId`).

### insects-core/pom.xml — two new dependencies

```xml
<!-- compile -->
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>library-api</artifactId>
</dependency>

<!-- test -->
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>library-repository-test</artifactId>
    <scope>test</scope>
</dependency>
```

Legal per DAG: core may import another domain's api.

### library-repository-test — test-scope dependency

Test fixtures referencing insect entities need the concrete `InsectsDomain` and
insect `EntityName` types:

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>insects-api</artifactId>
    <scope>test</scope>
</dependency>
```

Test-scope only. Production wiring happens at the composition root.

## Module Placement Summary

No new modules. All types fit existing modules:

| Type                                   | Module                    |
|----------------------------------------|---------------------------|
| `CitationAssociation`                  | `library-api`             |
| `CitationAssociationQuery`             | `library-api`             |
| `CitationAssociationCollection`        | `library-api`             |
| `CitationAssociationQueryImpl`         | `library-core`            |
| `CitationAssociationRepositoryMock`    | `library-repository-test` |
| `CitationAssociationTestEntitySource`  | `library-repository-test` |
| `CitationAssociationEntityRepositoryTest` | `library-repository-test` |
| `CitationAssociationId`               | `domains/identifiers`     |
| Test identifier constants              | `domains/identifiers-test` |
| `InsectCitationView`                   | `insects-api`             |
| `InsectQuery.CitationQuery`            | `insects-api`             |
| Insect citation discovery adapter      | `insects-core`            |