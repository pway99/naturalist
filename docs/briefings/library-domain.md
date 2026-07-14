# library-domain — Chat Briefing

**Purpose.** Domain vocabulary plus current shape of the library module
(four parallel sub-contexts: concept, citation, citation-association,
clade-navigation), sized for a chat Claude session. Pair with
`docs/briefings/framework-core.md` (framework / structural glue).

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time (2026-07-13), not
extrapolated. If you need a type not listed here, ask before inventing one.

---

## 1. Module Scope and DAG

```
library-api  →  framework, identifiers, field-notes, authority, catalog,
                clades, taxonomy
```

`library-api` depends on four kernels beyond the standard set:

- `kernels/authority` — for `Citation` (sealed `NamedEntity`),
  `CitationName`, `OnlineSource`, `AuthorityReference`, `AuthoritySource`.
  Citation is a kernel type, not a library-domain record.
- `kernels/catalog` — for `EntityRef` (domain-agnostic entity reference
  used by `CitationAssociation` to point at any cataloged entity).
- `kernels/clades` — for `Clade` (sealed interface), `CladeCatalog`,
  `CladeTraversal`. Used by the clade-navigation sub-context.
- `kernels/taxonomy` — for `LinealRank` (enum). Used by `CladeStep` and
  `CladeRanks` to associate Linnaean ranks with clades.

No cross-domain api dependencies. All external references go to kernels.

---

## 2. Package Map

```
com.naturalist.library/
  Concept                            — NamedEntity<ConceptName>
  ConceptRepository                  — package-private (N=1 collapse)
  ConceptQuery                       — public (N=1 collapse)
  ConceptCollection                  — BehavioralCollection<Concept>

  CitationRepository                 — package-private (N=1 collapse)
  CitationQuery                      — public (N=1 collapse)
  CitationCollection                 — BehavioralCollection<Citation>

  CitationAssociation                — Entity<CitationAssociationId>
  CitationAssociationRepository      — package-private (N=1 collapse)
  CitationAssociationQuery           — public (standalone, not EntityQuery)
  CitationAssociationCollection      — BehavioralCollection<CitationAssociation>

  CladeStep                          — ValueObject (clade traversal node)
  CladeView                          — ReadModel (clade + ancestry + children)
  CladeQuery                         — public (standalone, not EntityQuery)
```

All four sub-contexts (concept, citation, citation-association,
clade-navigation) live in the same `com.naturalist.library` package.
The N=1 collapse rule is applied to the first three — no outer namespace
wrappers. Clade-navigation has no repository or collection — it reads
from the `kernels/clades` sealed permits.

### Identifier locations

| Type                    | Package                    | Module               |
|-------------------------|----------------------------|----------------------|
| `ConceptName`           | `com.naturalist.library`   | `identifiers`        |
| `CitationAssociationId` | `com.naturalist.library`   | `identifiers`        |
| `CitationName`          | `com.naturalist.authority` | `authority` (kernel) |

`CitationName` lives in the authority kernel alongside `Citation`, not
in `domains/identifiers`. This follows the kernel-owns-its-names
pattern — kernel-level `EntityName` subclasses stay with their entity.

No identifiers are needed for clade-navigation — it uses plain string
slugs (`Clade.slug()`), not `EntityName` types.

---

## 3. Entity Summary

| Type                  | Identity                | Branch                            | DDD role                        |
|-----------------------|-------------------------|-----------------------------------|---------------------------------|
| `Concept`             | `ConceptName`           | `NamedEntity` (slug)              | Teaching/reference entry        |
| `Citation` (sealed)   | `CitationName`          | `NamedEntity` (slug, kernel type) | External authority citation     |
| `CitationAssociation` | `CitationAssociationId` | `Entity` (UUIDv7, component `id`) | Citation-to-entity binding      |
| `CladeStep`           | (no identity)           | `ValueObject`                     | Single clade in traversal chain |
| `CladeView`           | (no identity)           | `ReadModel`                       | Clade position projection       |

### Sub-context independence

The four sub-contexts are explicitly non-interacting:

- **Concept** — teaching/reference entries, standalone.
- **Citation + CitationAssociation** — external authority citations and
  their cross-domain bindings. A citation never references a concept.
- **Clade-navigation** — read-side projections over the clades kernel.
  No interaction with concepts or citations.

---

## 4. Concept — teaching/reference entry

```java
public record Concept(
    ConceptName name,
    String title,
    Description description
) implements NamedEntity<ConceptName>
```

A human-titled, four-level Durrell `Description` of a concept the
catalog needs to explain (e.g. what a clade is, how clades relate to
taxonomy). Identity by slug (`ConceptName`, max 64 chars).

Invariants: `entityName(name)`, `notBlank(title)`,
`valueObject(description)`.

No `with*` methods — the record is structurally simple enough that
the canonical constructor suffices.

---

## 5. Citation — kernel type, library-hosted

`Citation` is a **sealed interface** declared in `kernels/authority`,
not in the library domain. The library domain wraps it in a
`CitationCollection`, `CitationQuery`, and `CitationRepository`.

```java
// in kernels/authority
@JsonTypeInfo(use = Id.NAME, property = "kind")
@JsonSubTypes({
    @Type(value = OnlineSource.class, name = "ONLINE_SOURCE")
})
public sealed interface Citation extends NamedEntity<CitationName>
    permits OnlineSource
```

### `OnlineSource` — the sole permit

```java
public record OnlineSource(
    CitationName name,
    AuthorityReference authorityReference,
    String title,
    @Nullable String author,
    @Nullable Integer year,
    @Nullable Instant lastModified
) implements Citation
```

Identity by slug (`CitationName`, max 200 chars). Invariants:
`entityName(name)`, `valueObject(authorityReference)`,
`notBlank(title)`.

### `AuthorityReference` (ValueObject, in authority kernel)

```java
public record AuthorityReference(
    AuthoritySource source,
    URI url
) implements ValueObject
```

A deep-link pointer into an external authority's catalogue. Self-
describing — carries its own `AuthoritySource`.

### `AuthoritySource` (ValueObject, in authority kernel)

```java
public record AuthoritySource(
    String id,
    String displayName
) implements ValueObject
```

Open provider metadata — the kernel names no concrete source; each
provider defines its own `AuthoritySource` constant in its api.

---

## 6. CitationAssociation — citation-to-entity binding

```java
public record CitationAssociation(
    CitationAssociationId id,
    CitationName citationName,
    EntityRef subject,
    @Nullable String note
) implements Entity<CitationAssociationId>
```

Binds a `Citation` to any cataloged entity in any domain. Identity by
UUIDv7 — accessor `id()`, invariant `.entityId(id, "id")`.

- `citationName` — FK to the citation being associated.
- `subject` — `EntityRef` (from `kernels/catalog`): the entity the
  citation is about. Domain-agnostic — can point to an insect species,
  a chemical compound, a plant, etc.
- `note` — optional annotation on this specific association.

`withNote(@Nullable String)` is the sole `with*` method.

### `EntityRef` (ValueObject, in catalog kernel)

```java
public record EntityRef(
    DomainId domain,
    EntityName name
) implements ValueObject
```

`DomainId` is an open interface — each domain ships its own subtype.
`EntityRef` is the generic cross-domain pointer: domain + entity slug.

---

## 7. Clade-navigation — tree-of-life read models

The clade-navigation sub-context provides read-side projections over
the `kernels/clades` sealed `Clade` type. It has no repository, no
collection, and no persisted entities — everything is derived from
the kernel's sealed permits at query time.

### `CladeStep` — ValueObject

```java
public record CladeStep(
    String cladeSlug,
    String displayName,
    Optional<LinealRank> rank
) implements ValueObject
```

A single clade in a traversal chain. `rank` is empty for clades that
have no corresponding Linnaean rank (e.g. Holometabola is a clade but
not a rank). When present, `rank` carries the `LinealRank` enum value
(e.g. `ORDER` for Lepidoptera, `FAMILY` for Papilionidae).

Invariants: `notBlank(cladeSlug)`, `notBlank(displayName)`,
`notNull(rank)`.

### `CladeView` — ReadModel

```java
public record CladeView(
    CladeStep subject,
    List<CladeStep> ancestry,
    List<CladeStep> children
) implements ReadModel
```

Read-side projection showing a clade's position in the tree of life:
the subject clade plus its ancestor chain (root-first) and direct
children (sorted by display name).

Invariants: `valueObject(subject)`, `notNull(ancestry)`,
`notNull(children)`.

### `CladeQuery` — public, standalone

```java
public interface CladeQuery {
    Optional<CladeView> getBySlug(String slug);
    CladeTreeNode tree();

    record CladeTreeNode(
        String slug,
        String displayName,
        Optional<LinealRank> rank,
        List<CladeTreeNode> children
    ) {}
}
```

`getBySlug` returns a `CladeView` for a specific clade slug — ancestry
from root to parent, direct children sorted. `tree()` returns the full
clade tree rooted at Eukaryota as a recursive `CladeTreeNode` structure.

Does **not** extend `EntityQuery` — clades are kernel types (sealed
permits), not library-domain entities. There is no `CladeRepository`
or `CladeCollection`.

### `CladeRanks` — curated rank mapping (package-private, in library-core)

Static mapping of 8 clade slugs to `LinealRank` values:

| Clade slug     | LinealRank |
|----------------|------------|
| `animalia`     | `KINGDOM`  |
| `arthropoda`   | `PHYLUM`   |
| `insecta`      | `CLASS`    |
| `blattodea`    | `ORDER`    |
| `hemiptera`    | `ORDER`    |
| `lepidoptera`  | `ORDER`    |
| `papilionidae` | `FAMILY`   |
| `termitoidae`  | `FAMILY`   |

Unranked clades (Holometabola, Apoidea, Anthophila, etc.) return
`Optional.empty()`. The mapping is curated, not exhaustive — new
entries are added when a clade gains a Linnaean-rank association in
the domain.

### `CladeViewFactory` — assembly (package-private, in library-core)

Builds `CladeView` from the clade hierarchy using `CladeCatalog`,
`CladeTraversal`, and `CladeRanks`. Also builds the full `CladeTreeNode`
tree recursively from the Eukaryota root.

---

## 8. Query / Repository / Collection Surface

### `ConceptQuery` (public)

```java
public interface ConceptQuery
    extends EntityQuery<ConceptName, Concept, ConceptCollection>
```

No additional methods beyond the `EntityQuery` port.

### `CitationQuery` (public)

```java
public interface CitationQuery
    extends EntityQuery<CitationName, Citation, CitationCollection>
```

No additional methods beyond the `EntityQuery` port.

### `CitationAssociationQuery` (public, standalone)

```java
public interface CitationAssociationQuery {
    CitationAssociationCollection findByCitationName(CitationName citationName);
    CitationAssociationCollection findBySubject(EntityRef subject);
}
```

**Does not extend `EntityQuery`.** The association is looked up by its
FK references (`citationName` or `subject`), not by its own surrogate
id. No `getByName` — the UUID id is internal to the bounded context.

### `CladeQuery` (public, standalone)

See section 7 above. Does not extend `EntityQuery`. No repository or
collection backing.

### Repositories (all package-private)

```java
interface ConceptRepository
    extends EntityRepository<ConceptName, Concept>

interface CitationRepository
    extends EntityRepository<CitationName, Citation>

interface CitationAssociationRepository
    extends EntityRepository<CitationAssociationId, CitationAssociation> {
    List<CitationAssociation> getByCitationName(CitationName citationName);
    List<CitationAssociation> getBySubject(EntityRef subject);
}
```

`CitationAssociationRepository` has domain-specific methods matching
the query's two lookup axes.

### BehavioralCollections

`ConceptCollection`, `CitationCollection`,
`CitationAssociationCollection` — all `final class extends
BehavioralCollection<...>`, package-private constructor, public
`of(Collection<...>)` / `empty()` factories. No domain-specific
filtering methods on any of them currently.

---

## 9. JSON Catalog Locations (library-repository-test)

```
library-repository-test/src/main/resources/library/
  concepts.json               — 9 Concept records (NamedEntity, keyed by slug)
  citations.json              — 16 Citation records (NamedEntity, keyed by slug)
  citation-associations.json  — 4 CitationAssociation records (Entity, keyed by UUIDv7)
```

No JSON catalog files for clade-navigation — it reads from the kernel's
sealed permits, not from persisted data.

Catalog conventions:

- `"name": "<slug>"` is the natural key on `Concept` and `Citation`
  entries.
- `CitationAssociation` entries carry `"id"` as a UUIDv7 string (the
  `Entity` identity component).
- `Citation` entries carry `"kind": "ONLINE_SOURCE"` for Jackson
  polymorphic dispatch.
- `CitationAssociation` entries carry `"subject"` as an object with
  `"domain"` and `"name"` fields matching `EntityRef`.

### Concept catalog

Concepts: `clade`, `clade-taxonomy-relation`, `taxonomic-rank`,
`binomial-nomenclature`, `taxonomy`, `kingdom`, `phylum`, `class`,
`order`, `family`, `genus`, `species`, `placing-clades`. Each carries
a four-level Durrell description.

### Citation catalog

Online sources primarily from Encyclopedia of Life (EOL), covering
species (*Battus philenor*, *Danaus plexippus*, *Apis mellifera*,
*Drosophila melanogaster*, etc.) and higher taxa (Lepidoptera,
Hymenoptera, Holometabola, Apoidea).

---

## 10. Cross-domain References

| Reference      | Direction               | Type                         |
|----------------|-------------------------|------------------------------|
| `CitationName` | library → authority     | `EntityName` slug (kernel)   |
| `EntityRef`    | library → catalog       | `ValueObject` (DomainId + EntityName) |

No domain-to-domain cross-references. All external references go to
kernels only. The library domain is a service domain — it attaches
citations to entities in other domains via the domain-agnostic
`EntityRef` pointer, without importing any domain's api.

### Consumer pattern

Other domains consume `CitationAssociationQuery` to discover citations
attached to their entities. The insects domain, for example, uses it in
`InsectCitationQueryImpl` to walk the Linnaean hierarchy and collect
inherited citations. The library domain exposes the raw associations;
the consuming domain owns the inheritance/aggregation logic.

---

## 11. Current State — What's Built, What's Not

**Built and stable.**

- `Concept` with `ConceptQuery` and `ConceptCollection`.
- `Citation` (sealed, kernel type) with `CitationQuery` and
  `CitationCollection`, hosted in the library domain.
- `CitationAssociation` with `CitationAssociationQuery` (standalone,
  not `EntityQuery`) and `CitationAssociationCollection`.
- All three repository mocks, behavioral contract tests, and test
  entity sources.
- `CitationAssociationQueryImpl` in `library-core`.
- `LibraryTestContext` for cross-domain test wiring.
- Console controllers for citations and concepts.
- `CladeStep` ValueObject, `CladeView` ReadModel, `CladeQuery`
  interface.
- `CladeViewFactory` and `CladeRanks` in `library-core`.
- Clade tree and single-clade view fully operational via
  `CladeQuery.tree()` and `CladeQuery.getBySlug()`.

**Not yet built.**

- No `ConceptCommand` or `CitationCommand` — read-only surface today.
- No `CitationAssociation` console surface beyond the raw controller.
- No `ExternalAuthority` consumer in the library domain (the authority
  kernel declares the port; no library-domain adapter connects to EOL
  or other sources yet).
- No `BookSource`, `JournalSource`, or other `Citation` permits beyond
  `OnlineSource`.

---

## 12. Anti-patterns Specific to library-api

- **Do not invent new `Citation` permits in the library domain.**
  `Citation` is a kernel sealed interface in `kernels/authority`. New
  permits (`BookSource`, `JournalSource`, etc.) belong there, not in
  library-api.
- **Do not put `CitationName` in `domains/identifiers`.** It lives in
  `kernels/authority` alongside `Citation`. The kernel owns its names.
- **Do not make `CitationAssociationQuery` extend `EntityQuery`.**
  The association is looked up by FK references, not by its own UUID.
  The standalone interface shape is deliberate.
- **Do not make `CladeQuery` extend `EntityQuery`.** Clades are kernel
  types (sealed permits), not library-domain entities. There is no
  `CladeRepository` or `CladeCollection`.
- **Do not create `CladeEntityName` or `CladeId` types.** Clade
  navigation uses plain string slugs from `Clade.slug()`. Adding
  identity types would imply clades are library-domain entities.
- **Do not add a `CladeRepository` or `CladeCollection`.** The
  clade-navigation sub-context reads from kernel sealed permits —
  there is no persisted data to wrap.
- **Do not create a `LibraryQuery` or `LibraryRepository` namespace
  wrapper.** All sub-contexts use N=1 collapse or standalone
  interfaces — no grouping.
- **Do not make Concept and Citation interact.** They are parallel
  sub-contexts by design.
- **Do not add domain-specific imports to library-api.** The library
  domain depends only on kernels (framework, identifiers, field-notes,
  authority, catalog, clades, taxonomy). Adding `insects-api` or
  `chemistry-api` would create a cycle — the consuming domain depends
  on library, not the reverse.
- **Do not invent an `AssociationId` or `BindingId`.** The type is
  `CitationAssociationId` — matches the entity name.
