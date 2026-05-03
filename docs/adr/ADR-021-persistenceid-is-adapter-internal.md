# ADR-021: PersistenceId is Adapter-Internal; Cross-Entity Refs Use EntityName

> [rationale](rationale/ADR-021-persistenceid-is-adapter-internal.md)

### Rule 1 — `PersistenceId` is adapter-internal

`PersistenceId<Long>` addresses a specific row in a specific adapter.

- A domain record MUST NOT carry another entity's `PersistenceId` as a component.
- A domain record MUST NOT carry its own `PersistenceId` — no `id()` at the domain layer.
- A repository/query method MUST NOT accept or return a `PersistenceId` across a
  sub-context or domain boundary.
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

### Scope

Every domain. All domain records implement `NamedEntity<NAME>`; the `domains/identifiers`
module holds `EntityName` subclasses only.

### RDBMS adapter gate

No `<domain>-repository-rdms` module until the owning api is "complete and approved for
production testing" (not merely "tests pass on in-memory adapter"). Until then, in-memory
`NamedTestEntitySource` is the only implementation. Schema is the expensive motion —
stabilize api first.

### Review flags (applicability signals)

- Domain record has an `id()` component or a component of type `<OtherEntity>Id`
- Query/repository/factory accepts or returns a `PersistenceId`
- JSON fixture declares an `id` field on any record
- Aggregate factory threads `id()` between two repository calls
- `forXxxId(...)` where `forXxxName(...)` exists or would serve equally
- Cross-domain `*Id` import across a sub-context boundary

### Notes

- MyBatis is the project's mapper. JPA/Hibernate requires `@Id` on the Java entity —
  incompatible with the stronger form (rationale Appendix A.7).
- Reference implementation: `InsectSpecies` / `InsectImage` implement `NamedEntity<NAME>`;
  `InsectImage` carries only `InsectSpeciesName`; `InsectQuery.ImageQuery` exposes
  `forSpeciesName(...)` only; `insects.json` / `insect-images.json` omit all id fields.
