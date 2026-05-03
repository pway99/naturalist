# ADR-001: Repository Architecture

> [rationale](rationale/ADR-001-repository-architecture.md)

- Four repository responsibilities only: persistent entity cache, referential integrity,
  unique constraints, transactional consistency. No processing, derivations, or business rules.
- Repository interfaces are package-private in `<domain>-api`. Inter-domain interaction
  goes through public query/service classes.
- Dual keys: `PersistenceId<Long>` (intra-domain joins, null in JSON, assigned at insert)
    + `EntityName` (cross-domain references, never null).
- Intra-domain joins permitted. Cross-domain joins prohibited — cross-domain references
  held as `EntityName` and resolved by querying the foreign repository.
- One monolithic database; domains map to PostgreSQL schemas.
- Cross-domain FK enforcement: none in-memory (would violate DAG); enforced in RDBMS
  adapter via DDL `FOREIGN KEY`.
- `TestEntitySource` ≡ RDBMS table — enforces PK, unique, and reference constraints.
  Transactional consistency enforced by `Observer.throwWhenInvalid()` at insert.
- `NaturalistDatabase` is the only object that instantiates `TestEntitySource`. Owns all
  sources across domains, wires intra-domain reference constraints, resets via `BeforeEachCallback`.
- `TestContext` (future) = JUnit extension above `NaturalistDatabase` for domain services;
  does not own `TestEntitySource` instances.
