# ADR-021: PersistenceId is Adapter-Internal; Cross-Entity References Use EntityName

**Status:** Draft — piloted in the `insects` domain
**Full rationale:** [rationale/ADR-021-persistenceid-is-adapter-internal.md](rationale/ADR-021-persistenceid-is-adapter-internal.md)

## Decision

### Rule 1 — PersistenceId is adapter-internal
`PersistenceId<Long>` is a storage-layer concept. It exists to address a specific row in
a specific adapter.

- A domain record **MUST NOT** carry another entity's `PersistenceId` as a component.
- A domain record **MAY** carry its own `PersistenceId` on its `id()` (as `Entity<ID, NAME>`
  defines — kernel unchanged in this ADR).
- A repository/query method **MUST NOT** accept or return another domain's `PersistenceId`
  across a sub-context or domain boundary.
- RDBMS adapters **MAY (and SHOULD for performance)** use a numeric surrogate key for
  FK columns — entirely inside the adapter, resolved from `EntityName` at insert time,
  joined internally for reads. The materialized domain record carries only the `EntityName`.

### Rule 2 — Cross-entity references use EntityName
Every reference from one entity to another (intra-domain or cross-domain) is an
`EntityName` component on the referring record. No "denormalized convenience" id columns
on domain records.

### Rule 3 — Aggregate assembly goes by name
Aggregate factories resolve the root by name, then fetch children by name (or by a query
method that takes `EntityName`). Factory never threads `PersistenceId` between repositories.

```java
// correct
Optional<InsectSpecies> species = speciesQuery.getByName(name);
ImageCollection images = imageQuery.forSpeciesName(species.get().name());

// incorrect — threads storage key across a query boundary
ImageCollection images = imageQuery.forSpeciesId(species.get().id());
```

### Scope — pilot on `insects`
- `PersistenceId` removed from `insects-api` and `insects-core` entirely
- `InsectSpecies` / `InsectImage` drop `id()` at the domain layer
- RDBMS schema still carries the numeric PK (invisible across the port)
- Kernel adds parallel `NamedEntity<NAME>`, `NamedEntityRepository<NAME, ENTITY>`,
  `NamedTestEntitySource<NAME, ENTITY>`, `NamedEntityQuery<NAME, ENTITY, COLLECTION>`
  alongside existing `Entity<ID, NAME>` — other domains remain byte- and source-compatible
- Broader roll-out deferred to a post-pilot ADR that will decide: whether `NamedEntity`
  replaces `Entity<ID, NAME>` kernel-wide, and whether to relocate/delete
  `<Domain>XxxId` classes from `domains/identifiers/`

### RDBMS adapter work is gated on api completeness
No `<domain>-repository-rdms` module is written until the owning api model is stable
under exercise by real application code ("complete and approved for production testing",
not merely "tests pass on the in-memory adapter"). Until then, in-memory `TestEntitySource`
is the only implementation; schema/mapper/adapter scaffolding deferred.

Rationale: schema motion is the expensive motion; stabilize the api first.

### What this ADR does NOT change
- `Entity<ID, NAME>` unchanged for every non-pilot domain — chemistry, plants, soil,
  apiary, etc. use it unchanged
- `TestEntitySource.nextNumericId()` still assigns ids at insert time for `Entity<ID, NAME>`
  implementations; JSON fixtures still omit ids
- `EntityRepository.getById(ID)` stays on the repository contract (but is never called
  with an id sourced from another entity's domain reference, because no such reference exists)
- RDBMS schemas remain free to carry numeric FK columns (invisible across the port)

## Applicability signals (review flags)

- Domain record has a component of type `<OtherEntity>Id`
- Query/repository/factory method accepts or returns another entity's `PersistenceId` subtype
- JSON fixture declares a non-null id field for a foreign-key reference
- Aggregate factory threads `id()` between two repository calls
- Proposed `forXxxId(...)` method where `forXxxName(...)` exists or would serve equally

## Consequences

- Insects aggregate bug resolves by deletion: `InsectImage.insectSpeciesId`,
  `withInsectSpeciesId`, `ImageQuery.forSpeciesId`, and corresponding repository method
  go away
- Domain records smaller, no nullable persistence-artifact components. `@Nullable` on an
  id-shaped field is a review flag
- `insects-repository-rdms`, when written, holds the numeric PK privately and resolves
  slugs via SQL (CTE or scalar subquery against `UNIQUE (name)` index). Zero adapter
  state, single round trip, MVCC-consistent. Port exposes no numeric ids
- RDBMS joins still use numeric FKs under production load — the performance argument
  stands, served by adapter-internal schema
- Cross-domain `*Id` imports across a sub-context boundary → ADR-021 violation, flaggable
  without further investigation
- MyBatis is the project's chosen mapper framework. JPA/Hibernate requires `@Id` on the
  Java entity, which is incompatible with the stronger form (see rationale Appendix A.7)

## Reference implementation (post-pilot)
- `InsectSpecies` / `InsectImage` implement `NamedEntity<NAME>`; neither carries `id()`
- `InsectSpeciesId` / `InsectImageId` deleted from `domains/identifiers/`
- `InsectImage` carries `InsectSpeciesName` only
- `InsectQuery.ImageQuery` exposes `forSpeciesName(InsectSpeciesName)` only
- `InsectAggregateQueryImpl` delegates to `InsectAggregateFactory`, which composes by name
- `insects.json` / `insect-images.json` omit all id fields

## Related ADRs
ADR-001, ADR-004, ADR-005, ADR-007, ADR-010, ADR-017.
