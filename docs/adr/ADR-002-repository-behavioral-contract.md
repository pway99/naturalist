# ADR-002: Repository Behavioral Contract via Test Interface

**Status:** Accepted (Amended)
**Full rationale:** [rationale/ADR-002-repository-behavioral-contract.md](rationale/ADR-002-repository-behavioral-contract.md)

## Decision

- **Module naming:** `<domain>-repository-test` — named for purpose (tests repositories),
  not implementation. In-memory `TestEntitySource` is the mechanism, not the name.
- **Contract as distributable interface:** `FooRepositoryTest` interface with
  `@Test default` methods in `src/main/java` of `<domain>-repository-test`. Compiled into
  the jar, pulled by adapters, same tests run against all implementations.
- **In-memory adapter test** lives in `src/main/java` of the same module (part of the
  distributed artifact) as `FooRepositoryMockTest implements FooRepositoryTest`.
- **RDBMS adapter test** lives in `<domain>-repository-rdms/src/test/java` — also implements
  the same interface, provides its own `repository()` instance.
- **Module DAG:** `<domain>-repository-rdms → <domain>-repository-test (test scope) → <domain>-api`.

### Three test cases per select method
1. **Argument validation** — reject null/invalid args via
   `InvariantViolationException` (delegated to `observer().arguments(...)`).
2. **Empty result** — unknown name/id returns `Optional.empty()` or empty collection.
3. **Expected result** — structural equality ignoring `"id"`, using `TestEntitySource`
   as the expected-value source.

### Set-based select methods
- Partial-match test requires **≥ 2 known values + 1 not-found** in the input set. A single
  known + not-found cannot distinguish partial-match from single-entity lookup.

### Three test cases per write method (`insert`, `update`)
- `insert`: argument validation, constraint violation (duplicate id →
  `PrimaryKeyConstraintException`, duplicate unique → `UniqueConstraintException`),
  expected result (retrievable by id and name, observed via Observer).
- `update`: argument validation, no-match case (throws), expected result (every mutable
  field changed via `RandomValue`; `PersistenceId`/`EntityName` carried forward and asserted
  unchanged; persisted entity observed).

## Amendment 1 — EntityRepositoryContractTest

- All 22 standard `EntityRepository` test cases live once in
  `EntityRepositoryContractTest<ID, NAME, ENTITY>` in `kernels/framework-test`. Domain
  contract interfaces extend it; concrete tests supply hooks only.
- **Hooks:** `repository()`, `source()`, `notFoundName()`, `knownEntityNames()`,
  `notFoundId()`, `newEntity()`, `ghostEntity()`, `modifiedEntity(original)`.
- `assertEntityEquals` defaults to recursive comparison ignoring `"id"`; override for
  custom equality.
- `entityWithDuplicateName` hook eliminated — derived via `existing.withId(null)`.
- **Persistence verification via Observer:** insert/update expected-result tests observe
  the persisted entity (`mo.entity(persisted, "persisted").violations()`). Walks the full
  constraint graph. Replaces hand-written field-by-field assertions.
- `assertFieldsUpdated` hook removed; update verification is: (1) id/name unchanged,
  (2) recursive structural equality, (3) Observer constraint-graph walk.

## Consequences

- Single contract guarantees in-memory and RDBMS adapter equivalence by construction
- In-memory adapter sufficient for all development; RDBMS deferrable
- No test-containers or DB connections during development
- Every select/write method has 3 minimum documented test cases, enforced structurally
- `EntityRepositoryContractTest` eliminates ~250 lines boilerplate per entity repository
- New entity repository = ~8 hook implementations + 4-line mock test class; 22 cases inherited
- `TestEntitySourceTest` requires ≥ 4 entities per source
