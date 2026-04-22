# ADR-001: Repository Architecture

**Status:** Draft
**Full rationale:** [rationale/ADR-001-repository-architecture.md](rationale/ADR-001-repository-architecture.md)

## Decision

- **Four repository responsibilities only:** persistent entity cache, referential integrity,
  unique constraints, transactional consistency. No processing logic, no derived values, no
  business rules.
- **Repository interfaces are package-private** in `<domain>-api`. Inter-domain interaction
  goes through public service/query classes.
- **Dual-key strategy:** `PersistenceId<Long>` (numeric surrogate, intra-domain joins, null
  in JSON, assigned by `TestEntitySource`/RDBMS) + `EntityName` (slug, cross-domain references,
  never null).
- **Joins:** intra-domain joins permitted; cross-domain joins prohibited. Cross-domain
  references held as `EntityName`, resolved by querying the foreign repository.
- **One monolithic database.** Domains namespaced as PostgreSQL schemas
  (`chemistry.compound_info`, etc.).
- **Cross-domain FK enforcement:** not enforced in-memory (would violate DAG); enforced
  by RDBMS adapter via DDL `FOREIGN KEY` declarations.
- **`TestEntitySource` ≡ RDBMS table.** Enforces primary key, unique, and reference
  constraints. Transactional consistency enforced by `Observer.throwWhenInvalid()` at insert.
- **`NaturalistDatabase`** is the in-memory analog of the database itself — monolithic,
  the only object permitted to instantiate `TestEntitySource`. Owns all sources across all
  domains, wires intra-domain reference constraints, resets via `BeforeEachCallback`.
- **`TestContext` (future):** JUnit extension above `NaturalistDatabase` for domain services.
  Does not own `TestEntitySource` instances.

## Consequences

- Domain logic decoupled from persistence infrastructure
- Repositories invisible outside their domain — cross-domain coupling structurally impossible
- In-memory adapter sufficient for all development until RDBMS adapter is needed
- Cross-domain FK integrity deferred to the only layer that can enforce it without DAG violation
