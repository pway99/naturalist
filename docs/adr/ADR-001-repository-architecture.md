# ADR-001: Repository Architecture

> [rationale](rationale/ADR-001-repository-architecture.md)

- Four repository responsibilities only: persistent entity cache, referential integrity,
  unique constraints, transactional consistency. No processing, derivations, or business rules.
- Repository interfaces are package-private in `<domain>-api`. Inter-domain interaction
  goes through public query/service classes.
- Single identity per entity via `Named.key()`: `EntityName` slug (`NamedEntity`,
  cross-domain references, never null) or UUIDv7 `EntityId` (`Entity`, never crosses a
  domain boundary by value). No `PersistenceId` (ADR-022, superseding ADR-021).
- Intra-domain joins permitted. Cross-domain joins prohibited — cross-domain references
  held as `EntityName` and resolved by querying the foreign repository.
- One monolithic database; domains map to PostgreSQL schemas.
- Cross-domain FK enforcement: none in-memory (would violate DAG); enforced in RDBMS
  adapter via DDL `FOREIGN KEY`.
- `TestEntitySource` ≡ RDBMS table — enforces PK (`key()` collision), unique
  (`uniqueConstraints()`), and intra-domain foreign-key (`foreignKeyConstraints()`)
  constraints. Transactional consistency enforced by `Observer.throwWhenInvalid()` at insert.
- `NaturalistDatabase` is the only object that instantiates `TestEntitySource`. Owns all
  sources across domains, lazily constructs + caches each via `getNamed(...)`; the
  `NaturalistTestExtension` subclass resets them per test via `BeforeEachCallback`.
- `TestContext` (future) = JUnit extension above `NaturalistDatabase` for domain services;
  does not own `TestEntitySource` instances.
