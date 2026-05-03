# ADR-002: Repository Behavioral Contract via Test Interface

> [rationale](rationale/ADR-002-repository-behavioral-contract.md)

- Module name is `<domain>-repository-test` (named for purpose).
- Contract interface `FooRepositoryTest` with `@Test default` methods lives in
  `src/main/java` of that module; compiled into the jar and reused by adapter tests.
- In-memory adapter test `FooRepositoryMockTest implements FooRepositoryTest` lives in
  `src/main/java` of the same module.
- RDBMS adapter test lives in `<domain>-repository-rdms/src/test/java`, implements the
  same interface, supplies its own `repository()`.
- Module DAG: `<domain>-repository-rdms → <domain>-repository-test (test scope) → <domain>-api`.

## Per-method test cases

Each select method has three: argument validation (null/invalid → `InvariantViolationException`
via `observer().arguments(...)`), empty result (unknown name/id → `Optional.empty()` or
empty collection), expected result (structural equality ignoring `"id"`, expected sourced
from `TestEntitySource`).

Set-based select methods require **≥ 2 known + 1 not-found** in the input to distinguish
partial-match from single-entity lookup.

Each write method (`insert`, `update`) has three:

- `insert`: arg validation, constraint violation (dup id → `PrimaryKeyConstraintException`,
  dup unique → `UniqueConstraintException`), expected result (retrievable by id and name,
  observed via Observer).
- `update`: arg validation, no-match (silent — see ADR-006), expected result (every mutable
  field changed via `RandomValue`; `PersistenceId`/`EntityName` carried forward and asserted
  unchanged; persisted entity observed).

## EntityRepositoryContractTest (Amendment 1)

- 22 standard cases live in `EntityRepositoryContractTest<ID, NAME, ENTITY>` in
  `kernels/framework-test`. Domain contract interfaces extend it; concrete tests supply hooks.
- Hooks: `repository()`, `source()`, `notFoundName()`, `knownEntityNames()`, `notFoundId()`,
  `newEntity()`, `ghostEntity()`, `modifiedEntity(original)`.
- `assertEntityEquals` defaults to recursive compare ignoring `"id"`; override for custom equality.
- `entityWithDuplicateName` derived via `existing.withId(null)`.
- Insert/update verification observes the persisted entity:
  `mo.entity(persisted, "persisted").violations()`. Replaces field-by-field assertions.
- No `assertFieldsUpdated`; update check = id/name unchanged + structural equality + Observer walk.
- `TestEntitySourceTest` requires ≥ 4 entities per source.
