---
name: entity-repository
description: >
  Create the full repository stack for an existing NamedEntity: the repository
  super-interface (or add to an existing one), the in-memory mock, the behavioral
  contract test interface, and the mock test class. Use when adding a repository for
  a domain entity that already has a NamedTestEntitySource and TestIdentifiers. Triggers
  on phrases like "create a repository for Foo", "add repository for the Bar entity", or
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

- The entity class implementing `NamedEntity<<Entity>Name>`
- `<Entity>Name` in `domains/identifiers/` (domain records carry no persistence id per ADR-021)
- `<Entity>TestEntitySource` extending `NamedTestEntitySource` in
  `<domain>-repository-test/src/main/java/`
- `Test<Domain>Identifiers` in `domains/identifiers-test/src/main/java/` with at least
  two known `EntityName` constants and a `NotFound` inner class for this entity type

---

## Step 1 — Locate the Entity and Gather Context

Find the entity class. Read it to determine:

- The entity's `EntityName` type (e.g. `PlantName`)
- The entity's package (e.g. `com.naturalist.plants`)
- The domain module name (e.g. `plants`)
- All record components — classify each as:
    - **Immutable**: canonical `EntityName` (`name`) — never modified in update tests
    - **FK EntityName**: an `EntityName` referencing another entity (e.g. `PlantName plantName`
      on `Cultivar`) — the referenced entity must exist in test data when constructing
      update/insert test instances
    - **Mutable**: all other components — must be modified in the update expected-result test

Also locate:

- The `NamedTestEntitySource` subclass for this entity
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
    interface <Entity>EntityRepository extends NamedEntityRepository<<Entity>Name, <Entity>> {}
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

@Incubating("Investigating a pattern where repositories are nested within a single interface")
interface <Package>Repository {
    interface <Entity>EntityRepository extends NamedEntityRepository<<Entity>Name, <Entity>> {}
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

public class <Entity>EntityRepositoryMock
        extends AbstractTestNamedEntityRepository<<Entity>Name, <Entity>, <Entity>TestEntitySource>
        implements <Package>Repository.<Entity>EntityRepository {

    protected <Entity>EntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

`AbstractNamedEntityRepository` already holds a class-scoped `Observer` and exposes it
via `observer()`. Do not redeclare one on the mock.

---

## Step 4 — Behavioral Contract Test Interface

Create in `<domain>-repository-test/src/main/java/` in the entity's package. This is the
most complex artifact — read the entity's record components carefully to construct correct
test instances.

The contract reuses `NamedEntityRepositoryContractTest` from `framework-test`, which
supplies the full test suite for the four repository methods (`getByName`,
`getByEntityNameSet`, `insert`, `update`). The per-domain interface supplies identity
hooks and entity-construction helpers.

### Identity hooks (required)

- `notFoundName()` — a fictitious `<Entity>Name` guaranteed absent from the catalog
  (use `Test<Domain>Identifiers.NotFound.<entity>`)
- `knownEntityNames()` — at least two known names present in the test data

### Write-side hooks (required)

- `newEntity()` — a new valid entity with a unique name not in the catalog
  (synthetic name, e.g. `"test-<entity>-xx"`)
- `ghostEntity()` — an entity whose name does not exist in the catalog (used for the
  `update_unknownName_throwsEntityNotFoundException` test)
- `modifiedEntity(ENTITY original)` — the original entity with every mutable field
  changed to a distinct value; `name` is carried forward unchanged

### Test instance construction rules

- Domain records carry no persistence id (ADR-021) — do not pass or assert on one
- Use the entity's record constructor directly — no factory methods needed in tests
- For ValueObject components, construct inline with minimal but valid values
- For FK `EntityName` fields, reference a name that exists in the test data (from
  `Test<Domain>Identifiers`)
- For `@Nullable` fields, use `null` in insert/ghost tests; use non-null distinct values in
  the update expected-result test

### Template structure

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.EntityRepositoryTest;
// ... entity-specific imports ...

import java.util.List;

/**
 * Behavioral contract for {@link <Package>Repository.<Entity>EntityRepository}.
 * <p>
 * Inherits the full test suite from {@link NamedEntityRepositoryContractTest}; supplies
 * only the identity hooks and entity-construction helpers specific to {@link <Entity>}.
 */
interface <Entity>EntityRepositoryTest
        extends NamedEntityRepositoryContractTest<<Entity>Name, <Entity>> {

    @Override
    default <Entity>Name notFoundName() {
        return Test<Domain>Identifiers.NotFound.<entity>;
    }

    @Override
    default List<<Entity>Name> knownEntityNames() {
        return List.of(
                Test<Domain>Identifiers.<entity>A,
                Test<Domain>Identifiers.<entity>B);
    }

    @Override
    default <Entity> newEntity() {
        return new <Entity>(<Entity>Name.of("test-<entity>-xx"), /* remaining valid fields */);
    }

    @Override
    default <Entity> ghostEntity() {
        return new <Entity>(<Entity>Name.of("test-<entity>-ghost"), /* remaining valid fields */);
    }

    @Override
    default <Entity> modifiedEntity(<Entity> original) {
        return new <Entity>(original.name(), /* every mutable field changed */);
    }
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

    @Override
    public <Entity>TestEntitySource source() {
        return db.getNamed(<Entity>TestEntitySource.class);
    }
}
```

`db` is the `NaturalistDatabaseExtension` field contributed by
`NamedEntityRepositoryContractTest`; no extra `@RegisterExtension` is needed here.

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

The full inherited contract (plus any existing tests in the module) must pass. Report the
results to the user.

---

## Naming Conventions Summary

| Artifact         | Name                               | Location                                      |
|------------------|------------------------------------|-----------------------------------------------|
| Super-interface  | `<Package>Repository`              | `<domain>-api/src/main/java/.../`             |
| Nested interface | `<Entity>EntityRepository`         | nested inside super-interface                 |
| Mock             | `<Entity>EntityRepositoryMock`     | `<domain>-repository-test/src/main/java/.../` |
| Contract test    | `<Entity>EntityRepositoryTest`     | `<domain>-repository-test/src/main/java/.../` |
| Mock test        | `<Entity>EntityRepositoryMockTest` | `<domain>-repository-test/src/test/java/.../` |
