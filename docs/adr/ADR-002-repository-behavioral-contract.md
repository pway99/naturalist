# ADR-002: Repository Behavioral Contract via Test Interface

**Status:** Accepted (Amended)

## Context

Every domain repository has two adapter implementations:

- **In-memory** (`<domain>-repository-test`) — backed by `TestEntitySource` and JSON catalog files
- **RDBMS** (`<domain>-repository-rdms`) — backed by PostgreSQL

The risk is behavioral drift: the two adapters diverge over time, the in-memory adapter
gives false confidence, and bugs appear only when the RDBMS adapter is exercised.

A secondary concern is development speed. Test-containers and real database connections
carry significant startup cost. Developers should be able to maintain full focus on the
domain without waiting for infrastructure.

## Decision

### The repository-test Module Name

The `<domain>-repository-test` module is named for its purpose — it is the module that
**tests repositories**, not an implementation detail. The name is not `repository-mock` or
`repository-inmem`. Those names would expose the implementation and obscure the purpose.
The in-memory `TestEntitySource` is merely the mechanism by which the behavioral contract
can be exercised without an RDBMS.

### Behavioral Contract as a Distributable Interface

Each domain defines a `FooRepositoryTest` interface with `@Test default` methods in
`src/main/java` of the `<domain>-repository-test` module. This interface is the behavioral
contract for `FooRepository` — what any correct adapter must do.

```java
// chemistry-repository-test/src/main/java/.../ElementRepositoryTest.java
interface ElementRepositoryTest {
    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    ElementRepository repository();

    @Test
    default void getByName() {
        // contract test — must pass for every adapter
    }
}
```

Because it lives in `src/main/java`, the interface is compiled into the module's jar and
distributed as an artifact. Any adapter module can pull it in and run the same tests.

### In-Memory Adapter Test

`FooRepositoryMockTest` implements `FooRepositoryTest` and also lives in `src/main/java`
of the `<domain>-repository-test` module. It is **not** in `src/test/java`. This is
intentional — it is part of the distributed artifact, serving as both a runnable test and
a reference implementation of the contract interface.

```java
// chemistry-repository-test/src/main/java/.../ElementRepositoryMockTest.java
class ElementRepositoryMockTest implements ElementRepositoryTest {
    @Override
    public ElementRepository repository() {
        return new ElementRepositoryMock(db);
    }
}
```

### RDBMS Adapter Test

The `<domain>-repository-rdms` module adds `<domain>-repository-test` as a test dependency
and provides its own concrete implementation:

```java
// chemistry-repository-rdms/src/test/java/.../ElementRepositoryRdmsTest.java
class ElementRepositoryRdmsTest implements ElementRepositoryTest {
    @Override
    public ElementRepository repository() {
        return new ElementRepositoryRdms(dataSource);
    }
}
```

The same `@Test default` methods run against both adapters. Behavioral equivalence is
guaranteed by construction, not by convention.

### Module DAG Entry

```
<domain>-repository-rdms  →  <domain>-repository-test (test scope)
                           →  <domain>-api
```

### Required Test Cases Per Select Method

Every select method on a repository interface — whether defined on `EntityRepository`,
`CatalogEntityRepository`, `FactEntityRepository`, or a domain-specific extension — must
be covered by exactly three test cases in the contract interface.

#### 1. Argument Validation

The method must reject null or structurally invalid query parameters before any query is
executed. This is not a courtesy check — it is the primary defence against runaway queries
that return an unbounded result set, exhaust the connection pool, and take down the
application. The RDBMS adapter delegates this to the `EntityRepository` argument validation
layer (`observer().arguments(...)`); the contract test confirms it fires.

```java
@Test
default void getByName_nullArgument_throws() {
    assertThatThrownBy(() -> repository().getByName(null))
        .isInstanceOf(InvalidVariantException.class);
}
```

#### 2. Empty Result

When none of the persisted entities match the query parameters, the method must return
`Optional.empty()` or an empty collection. This verifies that the SQL `WHERE` clause (or
its in-memory equivalent) is correctly scoped and does not silently fall through to a full
table scan or return stale data.

```java
@Test
default void getByName_unknownName_returnsEmpty() {
    assertThat(repository().getByName(ElementName.of("unobtainium"))).isEmpty();
}
```

#### 3. Expected Result

When matching entities exist, the method must return them. This is the functional
equivalence assertion: the RDBMS adapter's SQL statement must produce the same result as
the in-memory mock for the same input. Comparison ignores the "id" while using recursive structural equality, not
reference equality.

```java
@Test
default void getByName_knownName_returnsElement() {
   // Arrange
   ElementName calcium = TestChemistryIdentifiers.Elements.Ca;
   Element expected = source().getByName(calcium).orElseThrow();

   // Act & Assert
   assertThat(repository().getByName(calcium))
           .isPresent()
           .get()
           .usingRecursiveComparison().ignoringFields("id")
           .isEqualTo(expected);
}
```

Note that the expected value in case 3 is sourced from the `TestEntitySource` directly —
not from the repository under test. The RDBMS adapter test seeds its own data and provides
its own expected-value mechanism; the `TestEntitySource` reference in the contract interface
is only reachable from the in-memory adapter test, which is fine because both implement the
same interface and the RDBMS implementation will override the seeding strategy.

### Set-Based Select Methods: Partial-Match Convention

Set-based select methods (`getByEntityNameSet`, `getByIdSet`) require two additional test
cases beyond the three above: an **all-known** case and a **partial-match** case.

The partial-match test must include **at least two known values plus one not-found value**
in the input set. A single known value plus a not-found value does not distinguish
partial-match behavior from single-entity lookup — the adapter could be silently falling
back to a single-entity query path and the test would still pass. Two known values in the
result prove that the set-based query path is functioning correctly and that the not-found
value was filtered without affecting the matched results.

```java
@Test
default void getByEntityNameSet_partialMatch_returnsOnlyMatchingPlants() {
    // Arrange — two known + one fictitious
    Set<PlantName> names = Set.of(
            TestPlantsIdentifiers.Plants.CaliforniaPipevine,
            TestPlantsIdentifiers.Plants.Borage,
            TestPlantsIdentifiers.Plants.NotFound.name
    );
    Set<PlantName> knownNames = Set.of(
            TestPlantsIdentifiers.Plants.CaliforniaPipevine,
            TestPlantsIdentifiers.Plants.Borage
    );
    List<Plant> expected = source().getByEntityNameSet(knownNames);

    // Act
    List<Plant> result = repository().getByEntityNameSet(names);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrderElementsOf(expected);
}
```

This convention applies identically to `getByIdSet` — two known ids plus
`EntityId.of(Long.MAX_VALUE)` as the not-found entry.

### Required Test Cases Per Write Method

The same three-case requirement applies to write operations — `insert` and `update`. The
cases are structurally identical; only the semantics of "no match" differ.

#### insert

1. **Argument validation** — `insert(null)` throws before any state is mutated.
2. **Constraint violation** — inserting an entity whose `PersistenceId` already exists throws
   `PrimaryKeyConstraintException`; inserting a duplicate unique value throws
   `UniqueConstraintException`. These are the "conflict" analog of the empty-result case.
3. **Expected result** — after a successful `insert`, the entity is retrievable by id (and
   by name for catalog entities). The returned entity is structurally equal to what was
   inserted. The persisted entity is observed (see Amendment 1).

#### update

1. **Argument validation** — `update(null)` throws before any state is mutated.
2. **No-match case** — `update` with an id that does not exist in the repository throws.
   The exact exception type is defined per domain — this ADR requires only that it throws
   rather than silently succeeding.
3. **Expected result** — after a successful `update`, a subsequent read by id returns an
   entity with every mutable field accurately reflecting the updated values.

The expected-result case must modify **every mutable field** to a value distinct from the
original using `RandomValue` helpers where field constraints permit. `PersistenceId` and `EntityName` are immutable —
they are carried forward from the original and asserted unchanged. If the entity carries a
foreign key `EntityName` referencing another entity, the referenced entity must exist in
the test data. Equality is verified via recursive structural comparison ignoring `"id"`.
The persisted entity is observed (see Amendment 1).

### Dependency on the Observer Framework

The argument validation cases (case 1 for both select and write methods) depend on
`InvariantObservation.throwWhenInvalid()`. This method walks the constraint graph
produced by `Observer.arguments(...)`, collects every violated constraint, and throws
a single `InvariantViolationException` carrying all violations if any are present.
Collecting all violations in a single pass means the caller receives a complete picture
of what is wrong, not just the first failure — this is especially important at API and
form boundaries where iterative error discovery is a poor user experience.

All `EntityRepository` methods — `getByName`, `getByEntityNameSet`, `getById`,
`getByIdSet`, `insert`, and `update` — validate arguments via `observer().arguments(...)`
before delegating to the `do*` implementation method.

### TestEntitySource.update() Design

`TestEntitySource.update()` validates arguments via `observer.arguments(...)`, then
verifies the entity exists by id — throwing `EntityNotFoundException` if the id is null,
invalid, or absent from the map. `preSaveChecks(entity, excludeId)` enforces unique
constraints while excluding the entity's own id from collision detection, allowing the
entity to retain its current name and unique values during an update.

## Consequences

- A single contract interface assures absolute consistency between the in-memory and RDBMS adapters
- The in-memory adapter is sufficient for all development; the RDBMS adapter can be deferred to the very end
- No test-containers, no database connections, no infrastructure overhead during development
- The `repository-test` module name correctly describes purpose, not implementation
- Developers maintain complete focus on the domain model — the database is an implementation detail that arrives last
- Every select and write method has a documented minimum of three test cases — argument validation, the no-match/conflict case, and the expected result — enforced by the abstract contract, not convention
- Argument validation is enforced structurally via `EntityRepository.observer().arguments(...)` — every public method validates before delegating to its `do*` implementation

---

## Amendment 1 — EntityRepositoryContractTest and Observer-Based Persistence Verification

### Context

The original ADR required hand-written field-by-field assertions in every update
expected-result test — `assertThat(persisted.symbol()).isEqualTo(modified.symbol())` for
every mutable field on every entity. The rationale was diagnostic clarity: when a field
fails to persist, the assertion names exactly which one broke.

Two things changed:

1. **Scale.** With 20+ domain entities ahead, the field-by-field `assertFieldsUpdated`
   hook became the most labor-intensive per-entity cost in the repository test stack. Every
   new entity required enumerating every mutable field in a hand-written assertion method.

2. **The Observer framework arrived (ADR-017).** The diagnostic clarity concern in the
   original ADR was actually an observability concern — surfacing which fields diverged
   between adapters — that predated the observability infrastructure. The Observer walks
   the full constraint graph of any `Observable` and reports every violation with its
   fully-qualified dotted path. This is structurally superior to field-by-field assertions:
   it catches invariant violations across the entire object graph (nested value objects,
   named values, entity names) that top-level field assertions would miss entirely.

Additionally, Java records are immutable. Changing a field on a nested value object
produces a new value object instance, which produces a new entity instance. There is no
scenario where a record's component reference mutates in place. Recursive structural
comparison already covers field-level equality for the full object graph.

### Decision

#### EntityRepositoryContractTest

All 22 standard `EntityRepository` test cases are defined once in
`EntityRepositoryContractTest<ID, NAME, ENTITY>` in `kernels/framework-test`. This is
an interface with `@Test default` methods — the same distributable contract pattern as
the original ADR, lifted one level higher. Domain-specific contract interfaces extend it
and supply only identity constants and entity construction hooks.

The concrete test interface provides:

| Hook | Purpose |
|------|---------|
| `repository()` | The repository under test |
| `source()` | The `TestEntitySource` backing the test data |
| `notFoundName()` | A fictitious `NAME` guaranteed absent from the catalog |
| `knownEntityNames()` | At least two known `NAME` constants from the test data |
| `notFoundId()` | An `ID` guaranteed absent (typically `XxxId.of(Long.MAX_VALUE)`) |
| `newEntity()` | A valid entity with null id and unique name, using `RandomValue` where field constraints permit |
| `ghostEntity()` | An entity with a non-existent id, using `RandomValue` where field constraints permit |
| `modifiedEntity(original)` | The original with every mutable field changed via `RandomValue` |

The `assertEntityEquals` method defaults to recursive comparison ignoring `"id"` and is
overridable for entities with custom equality semantics.

The `entityWithDuplicateName` hook was eliminated — the abstraction derives the duplicate
directly via `existing.withId(null)`, which exploits the fact that `Entity.withId()` returns
a new record instance with all other components unchanged.

#### Observer-based persistence verification

The insert and update expected-result tests observe the persisted entity after the
roundtrip:

```java
var mo = observer.forMethod("insert_newEntity_isRetrievableByNameAndById");
assertThat(mo.entity(persisted, "persisted").violations()).isEmpty();
```

This walks the full constraint graph of the entity returned by the repository —
every `entityName`, `entityId`, `namedValue`, `notNull`, `notBlank`, and nested
`Observable` constraint declared in the entity's `invariants()`. If an adapter
corrupts a field during serialization — truncates a `CatalogName`, drops a nullable
profile, nullifies a `NamedValue` — the observation catches it as a named violation
with its fully-qualified path.

This replaces field-by-field assertions with a mechanism that is:

- **Zero-cost to the concrete test author** — no hook to implement
- **Complete** — walks the full object graph, not just top-level fields
- **Diagnostic** — violations carry dotted paths like
  `EntityRepositoryContractTest.insert_newEntity.persisted.compoundInfo.formula`
- **Adapter-parity** — the same observation runs against both the in-memory and RDBMS
  adapters, catching serialization drift that field-level assertions cannot

#### Field-level assertions removed

The `assertFieldsUpdated` hook is removed from the contract. The update test verifies:

1. `id` and `name` are unchanged from the original (immutability assertion)
2. Recursive structural equality between persisted and modified (field parity)
3. Observer constraint-graph walk on the persisted entity (invariant integrity)

Together these three checks provide stronger coverage than hand-written field assertions
with no per-entity authoring cost.

### Consequences

- `EntityRepositoryContractTest` in `kernels/framework-test` eliminates ~250 lines of
  boilerplate per entity repository — the concrete interface supplies only hooks
- Entity construction in tests uses `RandomValue` helpers — tests prove the contract
  holds for any valid input, not just magic constants
- The Observer framework serves both its original purpose (ADR-017: runtime observability)
  and the persistence verification concern that this ADR originally addressed with
  field-level assertions
- `TestEntitySourceTest` requires at least 4 entities per source — sufficient for
  single-entity, set-based, partial-match, and write-side test coverage
- Adding a new entity repository requires implementing ~8 hook methods on the contract
  interface and one 4-line mock test class — the 22 test cases are inherited
