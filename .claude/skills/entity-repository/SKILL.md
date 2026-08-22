---
name: entity-repository
description: >
  Create the full repository stack for an existing domain entity: the repository
  super-interface (or add to an existing one), the in-memory mock, the behavioral
  contract test interface, and the mock test class. Use when adding a repository for
  a domain entity that already has a TestEntitySource and TestIdentifiers. Triggers
  on phrases like "create a repository for Foo", "add repository for the Bar entity", or
  explicit invocations like "/entity-repository <EntityClassName>".
allowed-tools: Read Write Edit Glob Grep Bash
argument-hint: <EntityClassName>
---

# Entity Repository Skill

## Purpose

Given an entity class name in $ARGUMENTS, scaffold the complete repository stack:

1. Repository super-interface namespace in the api module (or add a nested interface to an existing one)
2. In-memory mock repository in the repository-test module
3. Behavioral contract test interface in the repository-test module (`src/main/java`)
4. Mock test class in the repository-test module (`src/test/java`)
5. Add `identifiers-test` dependency to repository-test pom.xml if not already present

Naming follows ADR-020 §5: the **nested** repository interface drops the domain prefix and
the `Entity` infix (`SpeciesRepository` inside `InsectRepository`); the **standalone**
concrete classes carry the full entity class name and drop the infix
(`InsectSpeciesRepositoryMock`, not `InsectSpeciesEntityRepositoryMock`).

---

## Prerequisites

Before starting, verify the following exist. If any are missing, stop and report to the user.

- The entity class implementing `NamedEntity<<Entity>Name>` (slug identity) or
  `Entity<<Entity>Id>` (UUIDv7 identity — observations, images, fact records)
- The key class in `domains/identifiers/` — an `EntityName` subclass for a `NamedEntity`
  (records carry no `PersistenceId`; ADR-022), or an `EntityId` subclass for an `Entity`
- `<Entity>TestEntitySource` extending `TestEntitySource` in
  `<domain>-repository-test/src/main/java/`
- `Test<Domain>Identifiers` in `domains/identifiers-test/src/main/java/` with at least
  two known key constants and a `NotFound` inner class for this entity type

---

## Step 1 — Locate the Entity and Gather Context

Find the entity class. Read it to determine:

- The entity's **key type** — its `EntityName` subclass (e.g. `PlantSpeciesName`) or
  `EntityId` subclass (e.g. `PlantImageId`)
- The entity's package (e.g. `com.naturalist.plants`) and the domain module (e.g. `plants`)
- The **domain noun** for the namespace (e.g. `Plant` → `PlantRepository`) and the
  **entity subject** (the entity class name minus the domain prefix: `PlantSpecies` →
  `Species`)
- All record components — classify each as:
    - **Immutable**: the canonical key (`name`/`id`) — never modified in update tests
    - **FK EntityName**: an `EntityName` referencing another entity (e.g. `PlantGenusName
      genusName` on `PlantSpecies`) — the referenced entity must exist in test data when
      constructing update/insert test instances
    - **Mutable**: all other components — must be modified in the update expected-result test

Also locate the `<Entity>TestEntitySource` subclass, the `Test<Domain>Identifiers` class
and its constants for this entity, and the `<domain>-repository-test/pom.xml`.

---

## Step 2 — Repository Super-Interface Namespace in the API Module

Reference implementation: `InsectRepository` in `insects-api` (the namespace `class`) and
`PlantRepository` in `plants-api`.

Check whether a repository namespace already exists in the entity's package
(`*Repository.java`).

### If a namespace `class` exists in the same package (N>1)

Read it and add a new nested entity repository interface, named by the bare entity subject:

```java
class <DomainNoun>Repository {
    // ... existing nested interfaces ...
    protected interface <EntitySubject>Repository extends EntityRepository<<Entity>Name, <Entity>> {}
}
```

### If no namespace exists and this is the first entity in the package (N=1)

Per the ADR-020 N=1 collapse rule, skip the namespace: declare a top-level
package-private `<EntitySubject>Repository` interface directly (no wrapping class). Promote
it into a namespace `class` only when a second entity joins the package.

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.EntityRepository;

interface <EntitySubject>Repository extends EntityRepository<<Entity>Name, <Entity>> {}
```

Key rules:

- **Package-private** — the namespace `class` and (via `protected`) its nested interfaces;
  a top-level collapsed interface is package-private too
- The namespace is a `class`, not an `interface`, so nested contracts can carry their own
  access modifiers (inside an interface they would be implicitly `public`) — ADR-020
- Domain-specific select methods (e.g. `getByGenusName`, `getByParentNames`) are declared
  on the nested interface; **fan-out must batch** — add a `Set`-taking sibling rather than
  looping a single-key select (domains/CLAUDE.md)

---

## Step 3 — In-Memory Mock Repository

Create in `<domain>-repository-test/src/main/java/` in the entity's package. The mock
carries the full entity class name, drops the `Entity` infix, and is marked `@DomainService`
so the Spring runtime bridge can discover it (ADR-025):

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class <Entity>RepositoryMock
        extends AbstractTestEntityRepository<<Entity>Name, <Entity>, <Entity>TestEntitySource>
        implements <DomainNoun>Repository.<EntitySubject>Repository {

    <Entity>RepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    // Implement any domain-specific select methods declared on the nested interface here,
    // resolving the source via testEntitySource(). Validate arguments through observer()
    // first; batch Set-taking selects — never loop a single-key select.
}
```

`AbstractTestEntityRepository` resolves the shared source via
`naturalistDatabase.getNamed(<Entity>TestEntitySource.class)` and holds a class-scoped
`Observer` exposed via `observer()`. Do not redeclare one on the mock. For an N=1 collapsed
package the `implements` clause is the top-level `<EntitySubject>Repository` instead of the
nested form.

---

## Step 4 — Behavioral Contract Test Interface

Create in `<domain>-repository-test/src/main/java/` in the entity's package. This is the
most complex artifact — read the entity's record components carefully to construct correct
test instances. Reference: `InsectSpeciesRepositoryTest`.

The contract extends `EntityRepositoryTest<NAME, ENTITY>` from `framework-test`, which
supplies the full test suite for the four repository methods (`getByName`,
`getByEntityNameSet`, `insert`, `update`), the `@RegisterExtension NaturalistTestExtension
db` field, and the abstract `repository()` / `source()` hooks. The per-domain interface
supplies identity hooks and entity-construction helpers.

### Hooks

- `repository()` — narrow the return type to the nested repository interface
- `source()` — `default` returning `db.getNamed(<Entity>TestEntitySource.class)`
- `notFoundName()` — a fictitious key guaranteed absent (use `Test<Domain>Identifiers.NotFound.<entity>`)
- `knownEntityNames()` — at least two known keys present in the test data
- `newEntity()` — a new valid entity with a unique key not in the catalog (synthetic slug,
  e.g. `"test-<entity>-xx"`, or a fresh `EntityId`)
- `ghostEntity()` — an entity whose key does not exist in the catalog (used for
  `update_unknownName_throwsEntityNotFoundException`)
- `modifiedEntity(ENTITY original)` — the original with every mutable field changed to a
  distinct value via `RandomValue` where constraints permit; the key is carried forward unchanged

### Test instance construction rules

- Records carry no `PersistenceId` (ADR-022) — a `NamedEntity` carries only its `EntityName`;
  an `Entity` carries its `EntityId` (which *is* part of identity and asserted)
- Use the entity's record constructor directly — no factory methods needed in tests
- For ValueObject components, construct inline with minimal but valid values
- For FK `EntityName` fields, reference a name that exists in the test data (from
  `Test<Domain>Identifiers`)
- For `@Nullable` fields, use `null` in insert/ghost tests; use non-null distinct values in
  the update expected-result test

### Template structure

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
// ... entity-specific imports ...

import java.util.List;

/**
 * Behavioral contract for {@link <DomainNoun>Repository.<EntitySubject>Repository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002); supplies the identity
 * hooks and entity-construction helpers specific to {@link <Entity>}.
 */
interface <Entity>RepositoryTest
        extends EntityRepositoryTest<<Entity>Name, <Entity>> {

    @Override
    <DomainNoun>Repository.<EntitySubject>Repository repository();

    @Override
    default TestEntitySource<<Entity>Name, <Entity>> source() {
        return db.getNamed(<Entity>TestEntitySource.class);
    }

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

Create in `<domain>-repository-test/src/test/java/` in the entity's package. It supplies
only the concrete `repository()`; `db` and `source()` are inherited:

```java
package com.naturalist.<domain>.<subpackage>;

class <Entity>RepositoryMockTest implements <Entity>RepositoryTest {
    @Override
    public <DomainNoun>Repository.<EntitySubject>Repository repository() {
        return new <Entity>RepositoryMock(db);
    }
}
```

`db` is the `NaturalistTestExtension` field contributed by `EntityRepositoryTest`; no extra
`@RegisterExtension` is needed here.

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

| Artifact          | Name                          | Location                                      |
|-------------------|-------------------------------|-----------------------------------------------|
| Namespace class   | `<DomainNoun>Repository`      | `<domain>-api/src/main/java/.../`             |
| Nested interface  | `<EntitySubject>Repository`   | nested inside the namespace class              |
| Mock              | `<Entity>RepositoryMock`      | `<domain>-repository-test/src/main/java/.../` |
| Contract test     | `<Entity>RepositoryTest`      | `<domain>-repository-test/src/main/java/.../` |
| Mock test         | `<Entity>RepositoryMockTest`  | `<domain>-repository-test/src/test/java/.../` |

`<Entity>` is the full entity class name (`InsectSpecies`); `<EntitySubject>` drops the
domain prefix (`Species`); `<DomainNoun>` is the namespace prefix (`Insect`). For an N=1
package the namespace class is skipped and the nested interface becomes a top-level
package-private `<EntitySubject>Repository`.
