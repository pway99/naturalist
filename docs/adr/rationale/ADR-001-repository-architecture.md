# ADR-001: Repository Architecture

**Status:** Draft

## Context

The project needs a persistence strategy that keeps domain logic clean, enforces referential
integrity, and supports rapid iteration without requiring a running database during development.

## Decision

### Responsibilities

A repository has exactly four responsibilities:

1. **Persistent entity cache** — stores and retrieves domain entities by identity
2. **Referential integrity** — enforces foreign key constraints between related entities
3. **Unique constraints** — enforces uniqueness within an entity collection
4. **Transactional consistency** — the RDBMS provides a transactional framework enforcing
   the DDD invariant contract. An aggregate's invariants are validated within the transaction
   boundary — if any invariant fails, the transaction rolls back and no partial state is
   persisted. The repository does not implement invariant logic; it delegates to the
   transactional infrastructure to guarantee that only valid aggregates are committed.

No processing logic. No derived values. No business rules. A repository is not allowed to
transform, compute, or make decisions. Full stop.

### Interface Location

Repository interfaces are **package-private** in the `<domain>-api` module. They are never
visible outside the domain. Inter-domain interaction goes through public service or query
classes, never through direct repository access.

### Dual-Key Strategy

Every entity carries two identity values:

| Key                   | Type              | Purpose                                                 |
|-----------------------|-------------------|---------------------------------------------------------|
| `PersistenceId<Long>` | Numeric surrogate | Intra-domain joins, RDBMS index performance             |
| `EntityName`          | Slug natural key  | Cross-domain references, external APIs, stable identity |

`PersistenceId<Long>` is null in JSON catalog data — assigned by `TestEntitySource` or the RDBMS
on insert. It never crosses a domain boundary in application code.

`EntityName` is always present, never null in catalog data, and is the stable reference used
when one domain refers to an entity in another.

### Join Policy

- **Intra-domain joins: permitted.** A domain's repository may join its own tables using
  `PersistenceId<Long>` primary keys.
- **Cross-domain joins: prohibited.** A repository never joins tables from another domain.
  Cross-domain references are held as `EntityName` slugs. The consuming domain resolves
  the reference by querying the foreign domain's repository separately.

### Database Strategy

**One monolithic database.** The RDBMS is not split per domain. Domain boundaries are
enforced at the Java module level — the database does not need to mirror that isolation.

Domains are namespaced within the single database using PostgreSQL schemas:
`chemistry.compound_info`, `soil.soil_profile`, `zone.zone`, etc.

### Cross-Domain FK Enforcement

**In-memory:** Cross-domain foreign key constraints are **not enforced**. Domain api modules
are strictly isolated at compile time — a `TestEntitySource` for one domain cannot resolve
slugs into foreign-domain entities at insert time without importing that domain's api, which
would violate the DAG. Cross-domain references in fixture data are assumed valid.

**RDBMS adapter:** Cross-domain FK constraints are enforced by the database via DDL
(`FOREIGN KEY` declarations). Verified once when RDBMS adapter implementations are written
and tested against a real database.

### TestEntitySource ≡ RDBMS Table

`TestEntitySource` is the in-memory analog of a database table. It enforces the same
constraint types (transactional consistency is handled by `Observer.throwWhenInvalid()` at
insert time — the in-memory equivalent of a transaction-scoped invariant check):

- **Primary key** — duplicate `PersistenceId` on insert throws `PrimaryKeyConstraintException`
- **Unique constraints** — declared via `uniqueConstraints()`, throw `UniqueConstraintException`
- **Reference constraints** — declared via `referenceConstraints()`, throw
  `ReferentialIntegrityException` (wired by `NaturalistDatabase` at construction)

### NaturalistDatabase

`NaturalistDatabase` is the in-memory analog of the database itself — monolithic, matching
the single-database production strategy. It is the **only** object permitted to instantiate
`TestEntitySource` instances.

Responsibilities:

- Constructs and owns **all** `TestEntitySource` instances across **all domains**
- Wires intra-domain reference constraints between sources at construction time
- Implements `BeforeEachCallback` — resets all sources before each test method
- Registered in tests as a static `@RegisterExtension` field

The monolithic design is honest: the database is not split per domain, so the test database
is not split per domain. A single `NaturalistDatabase` on the classpath gives every test
full access to the catalog without per-domain assembly ceremony. This is orders of magnitude
faster than a Spring application context with a real database connection.

### TestContext (future)

`TestContext` is a future JUnit extension that lives above `NaturalistDatabase` in the test
infrastructure stack. It constructs and wires domain services — the service tier analog of
what a DI container provides in production.

```
TestContext              ← domain services, application layer (future)
NaturalistDatabase       ← all TestEntitySources, data layer
```

`TestContext` does **not** own or manage `TestEntitySource` instances.

## Consequences

- Domain logic is completely decoupled from persistence infrastructure
- Repositories are not visible outside their domain — cross-domain coupling is structurally impossible
- The in-memory adapter is sufficient for all development until the RDBMS adapter is needed
- Cross-domain FK integrity is deferred to the infrastructure layer, which is the only layer that can enforce it without
  violating the module DAG
