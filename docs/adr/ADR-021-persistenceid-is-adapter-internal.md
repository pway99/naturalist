# ADR-021: PersistenceId is Adapter-Internal; Cross-Entity Refs Use EntityName
> [rationale](rationale/ADR-021-persistenceid-is-adapter-internal.md) — piloted in `insects`

### Rule 1 — `PersistenceId` is adapter-internal
`PersistenceId<Long>` addresses a specific row in a specific adapter.
- A domain record MUST NOT carry another entity's `PersistenceId` as a component.
- A domain record MAY carry its own `PersistenceId` on `id()` (kernel unchanged in this ADR).
- A repository/query method MUST NOT accept or return another domain's `PersistenceId`
  across a sub-context or domain boundary.
- RDBMS adapters MAY (and SHOULD for performance) use numeric surrogate keys for FK
  columns — adapter-internal only, resolved from `EntityName` at insert, joined internally
  for reads. Materialized domain record carries only `EntityName`.

### Rule 2 — Cross-entity references use `EntityName`
Every reference from one entity to another (intra-domain or cross-domain) is an
`EntityName` component. No "denormalized convenience" id columns on domain records.

### Rule 3 — Aggregates assemble by name
Aggregate factories resolve root by name, then fetch children by name. Factory never
threads `PersistenceId` between repositories.

```java
// correct
Optional<InsectSpecies> species = speciesQuery.getByName(name);
ImageCollection images = imageQuery.forSpeciesName(species.get().name());

// incorrect — threads storage key across a query boundary
ImageCollection images = imageQuery.forSpeciesId(species.get().id());
```

### Pilot scope — `insects` only
- `PersistenceId` removed from `insects-api` and `insects-core` entirely.
- `InsectSpecies` / `InsectImage` drop `id()` at the domain layer.
- RDBMS schema still carries the numeric PK (invisible across the port).
- Kernel adds parallel `NamedEntity<NAME>`, `NamedEntityRepository<NAME, ENTITY>`,
  `NamedTestEntitySource<NAME, ENTITY>`, `NamedEntityQuery<NAME, ENTITY, COLLECTION>`
  alongside existing `Entity<ID, NAME>` — other domains remain source- and byte-compatible.
- Broader roll-out deferred to a post-pilot ADR: whether `NamedEntity` replaces
  `Entity<ID, NAME>` kernel-wide; whether to delete `<Domain>XxxId` classes.

### RDBMS adapter gate
No `<domain>-repository-rdms` module until the owning api is "complete and approved for
production testing" (not merely "tests pass on in-memory adapter"). Until then, in-memory
`TestEntitySource` is the only implementation. Schema is the expensive motion — stabilize
api first.

### What this ADR does NOT change
- `Entity<ID, NAME>` unchanged for every non-pilot domain.
- `TestEntitySource.nextNumericId()` still assigns ids for `Entity<ID, NAME>` impls.
- `EntityRepository.getById(ID)` stays on the repository contract (never called with an
  id sourced from a cross-entity reference, because no such reference exists).
- RDBMS schemas remain free to carry numeric FK columns (invisible across the port).

### Review flags (applicability signals)
- Domain record has a component of type `<OtherEntity>Id`
- Query/repository/factory accepts or returns another entity's `PersistenceId`
- JSON fixture declares a non-null id for a foreign-key reference
- Aggregate factory threads `id()` between two repository calls
- `forXxxId(...)` where `forXxxName(...)` exists or would serve equally
- Cross-domain `*Id` import across a sub-context boundary

### Notes
- MyBatis is the project's mapper. JPA/Hibernate requires `@Id` on the Java entity —
  incompatible with the stronger form (rationale Appendix A.7).
- Reference implementation (post-pilot): `InsectSpecies` / `InsectImage` implement
  `NamedEntity<NAME>`; `InsectSpeciesId` / `InsectImageId` deleted; `InsectImage` carries
  only `InsectSpeciesName`; `InsectQuery.ImageQuery` exposes `forSpeciesName(...)` only;
  `insects.json` / `insect-images.json` omit all id fields.
