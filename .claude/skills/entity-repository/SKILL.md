---
name: entity-repository
description: >
  Create the full entity repository stack for an existing Entity: the repository
  super-interface (or add to an existing one), the in-memory mock, the behavioral
  contract test interface, and the mock test class. Use when adding a repository for
  a domain entity that already has a TestEntitySource and TestIdentifiers. Triggers on
  phrases like "create a repository for Foo", "add repository for the Bar entity", or
  explicit invocations like "/entity-repository <EntityClassName>".
allowed-tools: Read Write Edit Glob Grep Bash
argument-hint: <EntityClassName>
---

# Entity Repository Skill

## Purpose

Given an entity class name in $ARGUMENTS, scaffold the complete repository stack:

1. Repository super-interface in the api module (or add a nested interface to an existing one)
2. In-memory mock repository in the repository-test module
3. Behavioral contract test interface in the repository-test module (`src/main/java`)
4. Mock test class in the repository-test module (`src/test/java`)
5. Add `identifiers-test` dependency to repository-test pom.xml if not already present

---

## Prerequisites

Before starting, verify the following exist. If any are missing, stop and report to the user.

- The entity class implementing `Entity<ID, NAME>` (or `CatalogEntity` / `FactEntity`)
- `<Entity>Id` and `<Entity>Name` in `domains/identifiers/`
- `<Entity>TestEntitySource` in `<domain>-repository-test/src/main/java/`
- `Test<Domain>Identifiers` in `domains/identifiers-test/src/main/java/` with at least
  two known `EntityName` constants and a `NotFound` inner class for this entity type

---

## Step 1 — Locate the Entity and Gather Context

Find the entity class. Read it to determine:

- The entity's `PersistenceId` type (e.g. `PlantId`)
- The entity's `EntityName` type (e.g. `PlantName`)
- The entity's package (e.g. `com.naturalist.plants`)
- The domain module name (e.g. `plants`)
- All record components — classify each as:
  - **Immutable**: `PersistenceId` (`id`) and canonical `EntityName` (`name`) — never modified in update tests
  - **FK EntityName**: an `EntityName` referencing another entity (e.g. `PlantName plantName` on `Cultivar`) — the referenced entity must exist in test data when constructing update/insert test instances
  - **Mutable**: all other components — must be modified in the update expected-result test

Also locate:

- The `TestEntitySource` class for this entity
- The `Test<Domain>Identifiers` class and its constants for this entity
- The `<domain>-repository-test/pom.xml`

---

## Step 2 — Repository Super-Interface in the API Module

Check whether a repository super-interface already exists in the entity's package. Search
for a file matching `*Repository.java` in the package directory.

### If a super-interface exists in the same package

Read it and add the new nested entity repository interface:

```java
interface <Existing>Repository {
    // ... existing nested interfaces ...
    interface <Entity>EntityRepository extends EntityRepository<<Entity>Id, <Entity>Name, <Entity>> {}
}
```

### If no super-interface exists in the package

Create a new one. The name is the package-level domain noun + `Repository` (e.g.
`PlantRepository` for `com.naturalist.plants`, `CultivarRepository` for
`com.naturalist.plants.cultivar`):

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.Incubating;
import com.naturalist.data.EntityRepository;

@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface <Package>Repository {
    interface <Entity>EntityRepository extends EntityRepository<<Entity>Id, <Entity>Name, <Entity>> {}
}
```

Key rules:
- **Package-private** — both the outer and nested interfaces
- **One super-interface per package** — entities in different packages get their own
- The `@Incubating` annotation is required while the pattern is under evaluation

---

## Step 3 — In-Memory Mock Repository

Create in `<domain>-repository-test/src/main/java/` in the entity's package:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Observer;

public class <Entity>EntityRepositoryMock
        extends AbstractTestEntityRepository<<Entity>Id, <Entity>Name, <Entity>, <Entity>TestEntitySource>
        implements <Package>Repository.<Entity>EntityRepository {

    private static final Observer observer = Observer.forClass(<Entity>EntityRepositoryMock.class);

    protected <Entity>EntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Observer observer() {
        return observer;
    }
}
```

---

## Step 4 — Behavioral Contract Test Interface

Create in `<domain>-repository-test/src/main/java/` in the entity's package. This is the
most complex artifact — read the entity's record components carefully to construct correct
test instances.

The interface must cover all six `EntityRepository` methods with the following test cases:

### getByName (3 tests)
- `getByName_nullArgument` — throws `InvariantViolationException`, message contains `"name"`
- `getByName_unknownName_returnsEmpty` — uses `NotFound.name`
- `getByName_knownName_returns<Entity>` — recursive comparison ignoring `"id"`

### getByEntityNameSet (5 tests)
- `getByEntityNameSet_nullArgument` — throws `InvariantViolationException`, message contains `"nameSet"`
- `getByEntityNameSet_emptySet_returnsEmptyList`
- `getByEntityNameSet_noMatchingNames_returnsEmptyList` — uses `NotFound.name`
- `getByEntityNameSet_partialMatch_returnsOnlyMatching<Entity>s` — **two known names + NotFound.name**, asserts `hasSize(2)`
- `getByEntityNameSet_allKnownNames_returnsAllMatching<Entity>s` — two known names, asserts `hasSize(2)`

### getById (3 tests)
- `getById_nullArgument_throwsInvariantViolationException`
- `getById_unknownId_returnsEmpty` — uses `<Entity>Id.of(Long.MAX_VALUE)`
- `getById_knownId_returns<Entity>` — recursive comparison ignoring `"id"`

### getByIdSet (5 tests)
- `getByIdSet_nullArgument_throwsInvariantViolationException`
- `getByIdSet_emptySet_returnsEmptyList`
- `getByIdSet_noMatchingIds_returnsEmptyList` — uses `<Entity>Id.of(Long.MAX_VALUE)`
- `getByIdSet_partialMatch_returnsOnlyMatching<Entity>s` — **two known ids + `<Entity>Id.of(Long.MAX_VALUE)`**, asserts `hasSize(2)`
- `getByIdSet_allKnownIds_returnsAllMatching<Entity>s` — two known ids, asserts `hasSize(2)`

### insert (3 tests)
- `insert_nullArgument_throwsInvariantViolationException`
- `insert_duplicateName_throwsUniqueConstraintException` — construct with `null` id, duplicate name from an existing entity, all other fields valid but distinct
- `insert_new<Entity>_isRetrievableByNameAndById` — construct with `null` id, synthetic name (e.g. `"test-<entity>-xx"`), assert id is non-null after insert, recursive comparison ignoring `"id"`

### update (3 tests)
- `update_nullArgument_throwsInvariantViolationException`
- `update_unknownId_throwsEntityNotFoundException` — construct with `<Entity>Id.of(Long.MAX_VALUE)`, synthetic name
- `update_existing<Entity>_isRetrievableWithNewValues` — **modify every mutable field** to a distinct value, assert each field individually:
  - `id` and `name` are immutable — assert equal to original
  - ValueObject components (Description, TaxonomicClassification, etc.) — use `usingRecursiveComparison()`
  - Enums, primitives, Strings — use direct equality
  - FK `EntityName` fields — the referenced entity must exist in the test data

### Test instance construction rules

- `id` is always `null` for insert tests (persistence-assigned)
- `id` is `<Entity>Id.of(Long.MAX_VALUE)` for update-unknown-id tests
- Use the entity's record constructor directly — no factory methods needed in tests
- For ValueObject components, construct inline with minimal but valid values
- For FK `EntityName` fields, reference a name that exists in the test data (from `Test<Domain>Identifiers`)
- For `@Nullable` fields, use `null` in insert/update-ghost tests; use non-null distinct values in the update expected-result test

### Template structure

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.exception.UniqueConstraintException;
// ... entity-specific imports ...
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link <Package>Repository}.
 * <p>
 * Every method is covered by three cases per ADR-002:
 * <ol>
 *   <li>Argument validation — null or invalid input is rejected before any query executes</li>
 *   <li>No-match — the method returns empty when no entities satisfy the query</li>
 *   <li>Expected result — the method returns the correct entities</li>
 * </ol>
 */
interface <Entity>EntityRepositoryTest {

    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    <Package>Repository.<Entity>EntityRepository repository();

    default <Entity>TestEntitySource source() {
        return db.get(<Entity>TestEntitySource.class);
    }

    // ... 22 test methods as described above ...
}
```

---

## Step 5 — Mock Test Class

Create in `<domain>-repository-test/src/test/java/` in the entity's package:

```java
package com.naturalist.<domain>.<subpackage>;

class <Entity>EntityRepositoryMockTest implements <Entity>EntityRepositoryTest {
    @Override
    public <Package>Repository.<Entity>EntityRepository repository() {
        return new <Entity>EntityRepositoryMock(db);
    }
}
```

---

## Step 6 — Update pom.xml

Read `<domain>-repository-test/pom.xml`. If `identifiers-test` is not already a dependency,
add it:

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>identifiers-test</artifactId>
</dependency>
```

---

## Step 7 — Verify

Run the tests:

```bash
mvn test -pl domains/<domain>/<domain>-repository-test -am
```

All 22 contract tests (plus any existing tests in the module) must pass. Report the
results to the user.

---

## Naming Conventions Summary

| Artifact | Name | Location |
|---|---|---|
| Super-interface | `<Package>Repository` | `<domain>-api/src/main/java/.../` |
| Nested interface | `<Entity>EntityRepository` | nested inside super-interface |
| Mock | `<Entity>EntityRepositoryMock` | `<domain>-repository-test/src/main/java/.../` |
| Contract test | `<Entity>EntityRepositoryTest` | `<domain>-repository-test/src/main/java/.../` |
| Mock test | `<Entity>EntityRepositoryMockTest` | `<domain>-repository-test/src/test/java/.../` |