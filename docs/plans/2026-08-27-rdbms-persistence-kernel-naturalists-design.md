# RDBMS Persistence Kernel + Naturalists Pilot — Design

**Date:** 2026-08-27
**Status:** Draft (pending review)
**Pilot domain:** `domains/naturalists`

## Goal

Stand up the project's first **real RDBMS repository adapter** — Postgres + MyBatis —
as a thin vertical slice through the `naturalists` domain, on top of two new persistence
kernels. The adapter is an **Anti-Corruption Layer (ACL)**: the persistence schema is
free to diverge from the domain model, translated across a layer of plain-Java Database
Objects (DBOs). Verification is the domain's **existing behavioral contract**
(`EntityRepositoryTest`, ADR-002) re-run against real Postgres.

This slice establishes the pattern and the shared test-database infrastructure. It does
**not** wire the application to run on Postgres — see Non-Goals.

## Strategy (as directed)

- **MyBatis as the ORM, hand-authored SQL.** We author custom SQL that translates the
  api model into a persistence model. No JPA, no derived queries.
- **DBOs are the ACL boundary.** Every entity maps to one-or-more plain-Java `…Dbo`
  POJOs. `EntityName`/`EntityId` are **unwrapped to `String`** and enums to `String`
  inside the DBO, so **no MyBatis type handlers are ever needed**. Each DBO carries a
  `@DboSchema` annotation (table, primary key, unique constraints, foreign constraints)
  and static factories to build it from its associated entity.
- **Auto-mapping only — no mapper configuration.** DBO field names **match the table
  column names exactly** (snake_case; the column names are the contract, and a DBO is a
  persistence artifact, not idiomatic domain code). Because every result column maps to an
  identically-named field of a plain type, MyBatis **auto-mapping** covers select→DBO and
  DBO→parameter in both directions. There is **never** a `@Results`, `@ResultMap`, or
  `<resultMap>` — the hand-authored `@Select`/`@Insert`/`@Update` SQL is the only thing we
  write. Multi-row reads that need a value from a joined table alias it to the DBO field
  name (e.g. `n.name AS name`) so auto-mapping still applies.
- **DBOs are `Observable`.** Each DBO declares `invariants()` like every other framework
  type, and the adapter walks them through the Observer at the translation boundary. This
  captures any error that arises between the entity and its DBO (a null in a `NOT NULL`
  column, a malformed slug) and — crucially — lets the DBO **assert its column widths**
  (e.g. `name` ≤ 64 chars) so an over-long value fails with a clear domain diagnostic
  *before* it reaches Postgres. Requires one small additive constraint in the framework
  (see "DBO validation" below).
- **Monolithic database for the modular monolith.** One physical Postgres database holds
  every domain's tables. This is deliberate: it lets a future domain's table declare a
  **real Postgres foreign key** to `naturalist(id)`, and lets the rdbms repository tests —
  seeded with every domain's JSON into that one DB — assert **true cross-domain
  referential integrity** on the test data. The in-memory `TestEntitySource` structurally
  cannot do this (the DAG forbids cross-domain resolution in memory); this test bed can.
  That capability is the strategic payoff. It arrives fully when a second domain gains an
  rdbms adapter; this slice builds the `naturalist(id)` anchor it will reference.
- **Schema optimized for persistence.** The naturalist's durable identity in the database
  is a **sequenced numeric id**, not the slug. A rename touches one row; credentials and
  all future cross-domain references hang off `naturalist.id`.
- **One shared, seeded test database.** Initialized once from the *same*
  `-repository-test` JSON that backs the mock repositories, so mock and SQL share a single
  seed truth. Schema + seed are applied **once per suite** (not per test); isolation
  between tests is a **transaction per test, rolled back**. No Testcontainers, no per-test
  schema initialization.

## Non-Goals (explicitly out of scope for this slice)

- **No application runtime wiring.** Nothing chooses SQL-vs-mock at app startup. No Spring
  profile, no `@DomainService` on the adapter, no bean swap. The mock-backed path and its
  tests are untouched. Production cutover to Postgres is a separate, much-later effort.
- **No Flyway / migration tooling.** DDL is hand-authored `.sql` applied by the test
  fixture. A prod migration story is deferred.
- **No second domain, no actual cross-domain FK yet.** Only `naturalist(id)` (the anchor)
  is built. The first real cross-domain FK lands with the next domain's adapter.
- **No `ProtectiveEquipment` adapter.** The pilot covers `Naturalist` and
  `NaturalistCredential` — the two entities with repositories.
- **No generic `AbstractRdbmsRepository` beyond what two repositories actually share.**
  Genericity waits for a second domain to reveal the real shape (avoid premature
  abstraction).

## Module topology

Four new modules. The kernel split mirrors `framework` / `framework-test`.

| Module | DAG role | Contents |
|---|---|---|
| `kernels/persistence` | prod support | `@DboSchema` + `@Fk` annotations; `Dbo` marker interface (`extends Observable`); MyBatis `Configuration`/`SqlSessionFactory` assembly given a `DataSource` + mapper set; HikariCP; PostgreSQL JDBC driver; any minimal shared repository plumbing (Observer arg-validation helpers). |
| `kernels/persistence-test` | test support | The pure `@RegisterExtension` connect + per-test-rollback JUnit 5 extension. The reusable seeding engine: a `SchemaApplier` seam (drop+recreate now; generator/Flyway later) and the replay-through-`insert()` seeder driven by ordered `(TestEntitySource, EntityRepository)` contributions. Depends on `kernels/persistence` + `kernels/framework-test`. |
| `domains/naturalists/naturalists-repository-rdbms` | prod adapter | `NaturalistDbo`, `NaturalistCredentialDbo` (+ `@DboSchema`); annotated MyBatis mappers with text-block SQL; hand DDL `schema/naturalists.sql`; `NaturalistEntityRepositoryRdbms`, `NaturalistCredentialRepositoryRdbms`. Split-package `com.naturalist.naturalist` to reach the package-private repository interfaces. `src/test` holds the contract-implementing integration tests. |
| `apps/test-db-seeder` | manual tool | Small Spring Boot app: the manually-invoked entrypoint that discovers each rdbms domain's seed contribution and runs the seeding engine against the standing/CI Postgres. Composition root over the rdbms modules; this slice wires only naturalists. |

### DAG notes

- `kernels/persistence` → `framework` (+ MyBatis, PostgreSQL driver, HikariCP, JSpecify).
- `kernels/persistence-test` → `persistence`, `framework-test` (mirrors `framework-test`
  → `framework`).
- `naturalists-repository-rdbms` (main) → `naturalists-api`, `kernels/persistence`,
  `framework`. **Test scope only:** `naturalists-repository-test` (contract interfaces +
  JSON) and `kernels/persistence-test`.
- `apps/test-db-seeder` → `kernels/persistence-test`, and every `*-repository-rdbms` +
  its `*-repository-test` (for the DBOs/adapters and the JSON-backed `TestEntitySource`s it
  replays). A composition root, so depending broadly is expected; nothing depends on it.
- New-module checklist (domains/CLAUDE.md): register each in its parent `<modules>` and
  add root `<dependencyManagement>` entries at `${project.version}`.

## `@DboSchema` — pure metadata

DDL is hand-authored (full control over Postgres-specific SQL). `@DboSchema` is **pure,
inert declarative metadata** — it *does nothing* on its own and generates no DDL. It
records the table, primary key, unique constraints, and foreign constraints **alongside
the DBO** so the persistence mapping is legible in one place and available to any future
reader or tooling.

Its `@Fk` edges also happen to express the load-dependency graph, so the manual seeder
**reads them as data** to order inserts (parents before children). That is the seeder
consuming passive metadata — not the annotation doing anything. Insert ordering could
equally come from explicit per-domain seed registration; either way `@DboSchema` stays inert.

```java
@Retention(RUNTIME) @Target(TYPE)
public @interface DboSchema {
    String table();
    String primaryKey();
    String[] unique() default {};
    Fk[] foreignKeys() default {};
}
public @interface Fk {          // nested; one per FK edge
    String columns();
    String references();        // e.g. "naturalist(id)"
}
```

## Naturalists schema — hand-authored DDL

`naturalists-repository-rdbms/src/main/resources/schema/naturalists.sql`:

```sql
CREATE TABLE naturalist (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- durable numeric key
    name        VARCHAR(64)  NOT NULL UNIQUE,   -- slug; UNIQUE ⇒ btree index. width = NaturalistName.maxLength() (64)
    given_name  VARCHAR(100) NOT NULL,
    family_name VARCHAR(100),
    role        VARCHAR(32)  NOT NULL,          -- NaturalistRole.name()
    stage       VARCHAR(32)  NOT NULL,          -- EcologicalStage.name()
    notes       TEXT                            -- free prose, unbounded
);

CREATE TABLE naturalist_credential (
    naturalist_id BIGINT PRIMARY KEY REFERENCES naturalist(id),   -- 1:1, FK to durable id
    password_hash VARCHAR(80) NOT NULL          -- {bcrypt}$2a$…, fixed-width encoded hash
);
```

**Bounded columns mirror the DBO invariants.** Each `VARCHAR(n)` width is asserted a
second time by the DBO's `invariants()` (see below), so the column width, the entity's own
slug `maxLength()`, and the DBO length check all agree. `notes` is unbounded prose, so it
is `TEXT` with no length invariant.

- **One `naturalist` table** carries identity and ecological data together. `UNIQUE(name)`
  gives an index, so `SELECT id … WHERE name = ?` is an index lookup, not a scan — a
  separate skinny id/name table would only buy a narrower/covering index and is not worth
  it. `Naturalist` *is* the identity anchor.
- `naturalist_credential` is keyed by the numeric id and FKs to `naturalist`. A rename
  updates only `naturalist.name`. The FK means inserting a credential for a non-existent
  naturalist **fails at the database** — the real referential-integrity check the
  in-memory source cannot make.
- Enums (`role`, `stage`, `NaturalistRole`, `EcologicalStage`) stored as `VARCHAR(32)`
  (constant name); unwrapped to the constant name in the DBO factory, `Enum.valueOf` on the
  way back. No type handler.

## DBOs — pure Java, no type handlers

Field names are **snake_case, identical to the columns**, so auto-mapping needs no config.

```java
@DboSchema(table = "naturalist", primaryKey = "id", unique = {"name"})
final class NaturalistDbo implements Dbo {                       // Dbo extends Observable
    Long   id;            // null before insert; DB identity fills it
    String name;          // NaturalistName unwrapped
    String given_name;
    String family_name;   // nullable
    String role;          // NaturalistRole.name()
    String stage;         // EcologicalStage.name()
    String notes;         // nullable, unbounded

    static NaturalistDbo from(Naturalist n) { … }   // camelCase accessors → snake_case fields; unwrap slug + enums
    Naturalist toEntity() { … }                      // re-wrap NaturalistName, Enum.valueOf

    @Override public Consumer<? extends Constraints> invariants() {
        return c -> c
            .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")  // = NaturalistName.maxLength()
            .notNull(given_name, "given_name").maxLength(given_name, 100, "given_name")
            .maxLength(family_name, 100, "family_name")        // null-tolerant
            .notNull(role, "role").maxLength(role, 32, "role")
            .notNull(stage, "stage").maxLength(stage, 32, "stage");
        // notes: unbounded, no length invariant
    }
}

@DboSchema(table = "naturalist_credential", primaryKey = "naturalist_id",
           foreignKeys = @Fk(columns = "naturalist_id", references = "naturalist(id)"))
final class NaturalistCredentialDbo implements Dbo {
    Long   naturalist_id; // resolved from naturalist.name at seed/insert time
    String password_hash;
    String name;          // populated on read only (aliased n.name); null on the write path

    static NaturalistCredentialDbo from(NaturalistCredential c, long naturalistId) { … }
    NaturalistCredential toEntity() { … }            // uses name (from the joined read) + password_hash

    @Override public Consumer<? extends Constraints> invariants() {
        return c -> c
            .notNull(naturalist_id, "naturalist_id")
            .notBlank(password_hash, "password_hash").maxLength(password_hash, 80, "password_hash");
    }
}
```

DBO fields are mutable POJO fields (MyBatis populates them by column, auto-mapped by exact
name). Nothing crosses a domain boundary; the DBOs are package-private in
`naturalists-repository-rdbms`. `NaturalistCredentialDbo.name` is a read-only projection
field — filled from the aliased `n.name` on multi-row reads so the slug round-trips, unset
on the write path where the numeric id is what's persisted.

### DBO validation + a new `maxLength` constraint

- **`Dbo` extends `Observable`.** The marker interface in `kernels/persistence` is
  `interface Dbo extends Observable {}`, so every DBO must supply `invariants()`.
- **The adapter validates at the boundary.** On write, after `…Dbo.from(entity)`, the
  adapter walks the DBO's invariants through the Observer and `throwWhenInvalid()` before
  issuing SQL — an over-long or malformed value fails with an `InvariantViolationException`
  naming the field, not an opaque Postgres error. (Reads can optionally validate the
  round-tripped DBO to catch adapter/serialization drift, symmetric with ADR-002's
  insert/update observe-the-persisted-entity rule.)
- **New framework constraint — `StringLengthLessThanConstraint`.** `kernels/framework`
  gains one additive constraint and a `Constraints` builder method:

  ```java
  // Constraints.java — null-tolerant (null passes; pair with notNull when presence required)
  public Constraints maxLength(String value, int max, String name);
  public <T> Constraints maxLength(T t, Function<T,String> valueFunction, int max, String name);
  ```

  Semantics: `value == null || value.length() <= max` (inclusive — matches `VARCHAR(max)`).
  Backed by `constraints/StringLengthLessThanConstraint.java`, mirroring the shape of
  `NotBlankConstraint` / `KebabFormatConstraint`. This is the only framework change in the
  slice; it is generally useful beyond DBOs.

## Mappers + repository adapters

**Mappers: annotated interfaces with Java text-block SQL, auto-mapping only** (start here;
drop an individual statement to an XML `<mapper>` only if it becomes unwieldy). No
`@Results`/`@ResultMap` — result columns auto-map onto the identically-named DBO fields,
and `#{…}` parameters reference DBO fields by the same names.

```java
interface NaturalistMapper {
    @Select("""
        SELECT id, name, given_name, family_name, role, stage, notes
        FROM naturalist WHERE name = #{name}
        """)                                       // columns auto-map onto NaturalistDbo fields
    NaturalistDbo selectByName(String name);

    @Insert("""
        INSERT INTO naturalist (name, given_name, family_name, role, stage, notes)
        VALUES (#{name}, #{given_name}, #{family_name}, #{role}, #{stage}, #{notes})
        """)                                       // #{…} = DBO field names, unchanged
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(NaturalistDbo dbo);
    // updateByName, selectByNameSet (IN clause), selectPage (LIMIT #{limit_plus_one} …)
}
```

Each adapter implements the six `EntityRepository` methods, validating arguments through
the Observer (`observer().arguments(...).throwWhenInvalid()`) exactly as the mock does,
then delegating to its mapper and translating DBO ⇄ entity:

- `NaturalistEntityRepositoryRdbms implements NaturalistRepository.NaturalistEntityRepository`
  — straightforward per-column auto-mapping.
- `NaturalistCredentialRepositoryRdbms implements NaturalistRepository.CredentialRepository`
  — resolves slug ↔ numeric id inside SQL, aliasing the joined slug to the DBO field name:
  - `getByName`: `SELECT c.password_hash, n.name AS name FROM naturalist_credential c
    JOIN naturalist n ON n.id = c.naturalist_id WHERE n.name = #{name}` — `password_hash`
    and `name` auto-map onto `NaturalistCredentialDbo`.
  - `insert`: `INSERT INTO naturalist_credential (naturalist_id, password_hash)
    SELECT id, #{password_hash} FROM naturalist WHERE name = #{name}` — resolves the id
    inline; the FK enforces the naturalist exists.
- **`getPage(PageRequest)`** honors the paging contract: `LIMIT n+1` + a thin has-next
  probe (project `paged_queries_gate` convention), ordered by `name`.
- **`save`** is update-if-present-else-insert on the key. Neither naturalist table has a
  *secondary* unique constraint, so the `EntityRepository#save` unique-constraint dedup
  case (case 2) does not arise here.

### FK resolution — nested selects on the domain key

The credential insert generalizes to the ACL's standing rule for **every** foreign key:

> A DBO stores a numeric FK, but the domain only ever knows the referent by its
> `EntityName` slug (or `EntityId` uuid). So each write **resolves the FK inline** with a
> nested select on the referenced table's domain-key column, and each read **joins** on the
> same column — the numeric id never leaves the persistence layer.

```sql
-- write: resolve slug → numeric id at insert time
INSERT INTO naturalist_credential (naturalist_id, password_hash)
SELECT id, #{password_hash} FROM naturalist WHERE name = #{name};

-- read: join back to recover the slug
SELECT c.password_hash, n.name AS name
FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
WHERE n.name = #{name};
```

Properties this gives us:

- **Referential integrity is enforced, not assumed.** A missing referent makes the
  subselect yield no row, so the `NOT NULL`/FK fails at the database — the check the
  in-memory source cannot make.
- **Renames stay local.** Because references resolve through `naturalist.name` to the id, a
  rename updates one column; nothing that points at the id moves.
- **It is the same shape cross-domain.** A future `insects.field_observation` resolves its
  observer with `(SELECT id FROM naturalist WHERE name = #{observedBy})` and declares a real
  FK to `naturalist(id)` — which is what lets the monolithic test DB assert cross-domain
  integrity on the seed.
- **`EntityId` referents work identically**, keyed on an indexed `entity_id UUID` column
  instead of `name` (a uuid never changes, so the numeric surrogate there is about
  locality/compactness, not rename-safety). No such table exists in this slice.

## Test database — a standing instance, maintained out-of-band

The overriding constraint is **`mvn verify` speed** — it runs constantly during work. So
the `mvn verify` path does **zero** DB maintenance: no schema check, no seed, no checksum.
The test DB is a **standing Postgres** whose committed content is a materialization of the
JSON, refreshed by a **manually-invoked seeder**, not by the test run. Changing seed JSON
or DDL and forgetting to reseed makes the rdbms tests fail — and that failure *is* the
"reseed" reminder. This trades a small, self-announcing manual step for the fastest
possible steady-state verify.

### The runtime extension is pure (`kernels/persistence-test`)

A JUnit 5 extension (working name `NaturalistRdbmsExtension`) that rdbms contract tests
register via `@RegisterExtension`, doing only two things:

- **Connect.** JDBC URL from `NATURALIST_TEST_JDBC_URL`, default
  `jdbc:postgresql://localhost:5432/naturalist_test`. Locally a **standing Postgres** kept
  running between `mvn` invocations; in CI a **`services: postgres` container** at the same
  `localhost:5432`. Identical test code either way.
- **Per-test rollback.** Before each test, begin a transaction on a bound connection; after
  each test (pass *or* fail), **roll back**. Test writes therefore **never commit**, so the
  committed content of the standing DB is *always* exactly the seed — which is what lets the
  seeder run rarely and the verify path do nothing. (Same mechanism as Spring's
  `@Transactional` test rollback; managed by the extension since contract tests construct
  repositories directly, not via a Spring context.)

No schema or seed logic lives here. If the DB is unseeded or stale, tests fail loudly.

### The seeder — auto-generated inserts from the test data

A **manually-invoked** process (a small Spring Boot app, per your suggestion —
`apps/test-db-seeder`) that materializes the JSON into the standing DB. Run it locally when
you change seed JSON or DDL; run it as one explicit CI step. It:

1. **Applies schema through a single seam.** For this slice: drop + recreate from the
   hand-authored `schema/*.sql`. That seam (`SchemaApplier`) is the one place a future
   DBO→DDL generator or `Flyway.migrate()` swaps in — nothing else changes.
2. **Auto-generates the seed inserts from the test data** by **replaying each entity
   through its rdbms adapter's `insert()`** — no hand-written seed SQL, no separate INSERT
   generator. The mapper's auto-mapped `@Insert` *is* the generated statement, so the seed
   goes through the exact ACL path the app uses: DBO validation runs, and the credential's
   `name→id` `INSERT … SELECT` resolves FKs for free. Entities are replayed in `@Fk`
   dependency order (naturalists before credentials), then **committed**.
3. **Iterates every registered domain's seed contribution.** Each `*-repository-rdbms`
   contributes its `(TestEntitySource, EntityRepository, schema.sql)` as a bean the seeder
   discovers; the monolithic DB is filled by every contribution in one run. This slice
   registers only naturalists.

Because the seeder replays through the real `insert()` path, it is also the first bulk
exercise of the ACL — a data problem (an over-long value, a broken FK) surfaces here with a
clear domain error before any contract test runs.

### CI shape (answers the pipeline question)

```yaml
jobs:
  build:
    services:
      postgres:
        image: postgres:16
        env: { POSTGRES_DB: naturalist_test, POSTGRES_PASSWORD: postgres }
        ports: ["5432:5432"]
        options: >-
          --health-cmd pg_isready --health-interval 5s
          --health-timeout 5s --health-retries 10
    env:
      NATURALIST_TEST_JDBC_URL: jdbc:postgresql://localhost:5432/naturalist_test
    steps:
      - checkout
      - setup-java
      - run: mvn -DskipTests install          # build jars incl. the seeder
      - run: java -jar apps/test-db-seeder/target/*.jar   # seed the fresh service DB
      - run: mvn verify                        # tests run against the seeded DB
```

CI's container is fresh per job, so the seeder step is required there every run; locally you
run it only after editing seed JSON or DDL.

## Verification — the existing contract, re-run on SQL

The deliverable's proof is the domain's own behavioral contract, bound to the SQL adapter as
a **Failsafe integration test** (`*IT`) so it runs during `mvn verify` (against the standing
Postgres) while the lighter `mvn test` phase stays infra-free:

```java
class NaturalistEntityRepositoryRdbmsIT implements NaturalistEntityRepositoryTest {
    @RegisterExtension static NaturalistRdbmsExtension rdbms = NaturalistRdbmsExtension.shared();

    @Override public NaturalistRepository.NaturalistEntityRepository repository() {
        return new NaturalistEntityRepositoryRdbms(rdbms.mapper(NaturalistMapper.class));
    }
    // source() stays the in-memory JSON oracle; the seeder materialized the SAME JSON into
    // the standing DB, so every inherited ADR-002 case (null-reject, not-found, expected,
    // insert/update/save) now proves the SQL adapter. Per-test rollback keeps the seed pristine.
}
```

- **Failsafe (`*IT`), bound to `verify`.** These tests require the standing/CI Postgres, so
  they live in the `integration-test`/`verify` phase — `mvn verify` runs them; `mvn test`
  does not. The consequence, by design: **`mvn verify` now depends on the standing Postgres
  being up and seeded**; if it is down, these `*IT`s fail (and, like the stale-seed case,
  that failure is the signal to start/seed the DB). The mock `*Test`s stay in Surefire and
  keep `mvn test` DB-free.
- `repository()` returns the SQL adapter; `source()` remains the JSON-loaded in-memory
  oracle. Because the SQL database is seeded from the same JSON, `getByName(PATRICK)` and
  friends assert identical expected values. Write cases read back through `repository()`
  (the SUT) per ADR-002, so they observe the SQL round-trip.
- A parallel `NaturalistCredentialRepositoryRdbmsIT implements
  NaturalistCredentialEntityRepositoryTest` proves the credential adapter, including the
  slug↔id join and the FK (insert-credential-for-missing-naturalist fails).
- The mock tests (`…RepositoryMockTest`) are unchanged and continue to run in Surefire.

## Rollout after this slice (future, not now)

- **Second domain adapter** (e.g. insects) → declares the first real cross-domain FK to
  `naturalist(id)`; the shared test DB then asserts cross-domain integrity on seed data.
- **Production runtime selection** — how a deployed app chooses SQL over mock (profile,
  module swap, or removing `spring-test-data`). Deferred until there is a reason to deploy
  on Postgres.
- **Seeder optimization — explicit-id seed mode.** When seed volume (or the monolithic
  multi-domain seeder) justifies it, the seeder can assign deterministic ids and insert with
  `OVERRIDING SYSTEM VALUE`, resolving every FK from an in-memory `name→id` registry it
  builds as it seeds — so seed inserts become plain `VALUES` with no per-row FK subselect.
  Requires restarting each identity sequence to `max(id)+1` after seeding (explicit inserts
  don't advance it) and a seed-specific insert path. Deferred: it only *adds* to the app's
  own `INSERT … SELECT` (which the live app still needs), and the subselect is negligible at
  test-seed volume. The `SchemaApplier`/seeder seam keeps this a localized change.
- **Flyway / migrations**, a generalized `AbstractRdbmsRepository`, and `ProtectiveEquipment`
  — as their need is demonstrated.

## Decisions locked

1. Two kernels: `kernels/persistence` (prod) + `kernels/persistence-test` (test harness).
2. Pilot module `naturalists-repository-rdbms`, split-package `com.naturalist.naturalist`.
3. One `naturalist` table (identity + data), `name` unique-indexed; `naturalist_credential`
   keyed by and FK'd to `naturalist.id`.
4. DBOs unwrap all typed keys/enums to `String`/`Long` — no MyBatis type handlers.
5. `@DboSchema` is **pure, inert metadata** — no DDL generation, no active behavior; DDL
   is hand-authored SQL. Its `@Fk` edges are read as data by the seeder only.
6. Mappers: annotated interfaces with text-block SQL; **MyBatis auto-mapping only** — DBO
   field names are snake_case and identical to the column names, so there is never a
   `@Results`/`@ResultMap`. XML per-statement only if a query becomes unwieldy.
7. Test DB: **standing local Postgres (CI: service container)** at
   `NATURALIST_TEST_JDBC_URL`, maintained **out-of-band by a manually-invoked seeder**
   (`apps/test-db-seeder`), not by the test run — the `mvn verify` path does zero DB
   maintenance (fastest steady state); a stale DB simply fails the rdbms tests, which is the
   reseed signal. The seeder applies hand DDL through a `SchemaApplier` **seam** (generator/
   Flyway swap in later) and **auto-generates seed inserts by replaying each JSON entity
   through the rdbms adapter's `insert()`** (FK-ordered, committed). Per-test transaction
   rollback keeps the seed pristine so test writes never commit. CI runs the seeder as one
   explicit step before `mvn verify`.
8. **DBOs are `Observable`** (`Dbo extends Observable`); the adapter validates each DBO's
   `invariants()` at the translation boundary. Invariants assert column widths, mirroring
   the `VARCHAR(n)` DDL. This adds one framework constraint —
   `StringLengthLessThanConstraint` + `Constraints.maxLength(...)` — the slice's only
   change to `kernels/framework`.
9. Verification: the existing `EntityRepositoryTest` contracts, bound to the SQL adapters as
   **Failsafe `*IT`s** running in `mvn verify` (mock `*Test`s stay in Surefire, `mvn test`
   DB-free). `mvn verify` therefore requires the standing Postgres up + seeded.
10. **Phase boundary.** This phase delivers the naturalist schema + adapters + the rdbms
    `*IT`s in `mvn verify`. Wiring the running application onto the rdbms repositories is a
    **future effort**, out of scope here.

## Local Postgres setup

`mvn verify` runs the rdbms `*IT`s, so a local Postgres must be up. Recommended (mirrors CI
exactly — same image and credentials, disposable, no system install):

```bash
docker run --name naturalist-pg -e POSTGRES_DB=naturalist_test \
  -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

It stays up across `mvn` runs — that is the "standing instance." Seed it with the seeder
after cloning and whenever seed JSON or DDL changes. Connection is read from
`NATURALIST_TEST_JDBC_URL` (default `jdbc:postgresql://localhost:5432/naturalist_test`), with
user/password from `NATURALIST_TEST_DB_USER`/`NATURALIST_TEST_DB_PASSWORD` (default
`postgres`/`postgres`, matching the container and CI). A Homebrew `postgresql@16` instance
works too, with the URL/creds pointed at it.
