---
name: entity-query
description: >
  Create the full query stack for an existing NamedEntity: the collection, the
  namespace query (or a nested entity query inside an existing one), the adapter
  implementation in the core module, and the contract test. Use when adding a
  read-side query for a domain entity that already has a repository and mock.
  Triggers on phrases like "add a query for Foo", "create FooQuery", or explicit
  invocations like "/entity-query <EntityClassName>".
allowed-tools: Read Write Edit Glob Grep Bash
argument-hint: <EntityClassName>
---

# Entity Query Skill

## Purpose

Given an entity class name in $ARGUMENTS, scaffold the complete read-side query
stack:

1. `<Entity>Collection` in the domain's entity-collections namespace (new file if
   none exists in the package)
2. `<Entity>EntityQuery` contract — nested in an existing namespace `*Query`
   interface, or a new `<Package>Query` interface
3. `<Entity>EntityQueryImpl` adapter in the core module, extending
   `AbstractEntityQuery`
4. Optional `<Package>QueryImpl` wiring — if the query is nested inside a
   namespace, a package-private impl exposes it
5. `<Entity>EntityQueryImplTest` implementing `EntityQueryContractTest` from
   `framework-test`
6. Optional `<Package>QueryImplTest` covering constructor null-checks and
   accessor idempotence when a new namespace impl is created

Reference implementation: the insects `InsectQuery` / `SpeciesQueryImpl` stack
and the sub-package `InsectLifeStageQuery` / `LifeStageEntityQueryImpl` stack.

---

## Prerequisites

Before starting, verify the following exist. If any are missing, stop and
report to the user.

- The entity class implementing `NamedEntity<<Entity>Name>`
- `<Entity>Name` in `domains/identifiers/`
- `<Package>Repository.<Entity>EntityRepository` contract (or equivalent
  top-level repository interface) in `<domain>-api`
- `<Entity>EntityRepositoryMock` + `<Entity>TestEntitySource` in
  `<domain>-repository-test/src/main/java/`
- `Test<Domain>Identifiers` with at least two known `<Entity>Name` constants
  and a `NotFound` fictitious constant

---

## Step 1 — Locate the Entity and Gather Context

Find the entity class. Read it to determine:

- The entity's `EntityName` type (e.g. `LifeStageName`)
- The entity's Java package (e.g. `com.naturalist.insects.lifestage`)
- The domain module name (e.g. `insects`)
- Whether an existing namespace query interface already lives in the same
  package:
    - `find <domain>-api/src/main/java/.../<package>/ -name "*Query.java"`
- Whether an existing entity-collections namespace already lives in the same
  package:
    - `find <domain>-api/src/main/java/.../<package>/ -name "*EntityCollections.java"`
- Whether an existing namespace query impl lives in `<domain>-core` in the
  same package:
    - `find <domain>-core/src/main/java/.../<package>/ -name "*QueryImpl.java"`

**Namespace naming.** `<Package>Query` is the domain noun for the package:
`com.naturalist.insects` → `InsectQuery`; `com.naturalist.insects.lifestage` →
`InsectLifeStageQuery` (sub-package compound noun). `<Entity>` drops the
namespace prefix where unambiguous — `InsectSpecies` → `Species`,
`InsectImage` → `Image`. For a sub-package entity that already reads cleanly
without a prefix (`LifeStage`), keep the entity name as-is.

---

## Step 2 — Entity Collection in the API Module

### If `<Package>EntityCollections.java` exists in the entity's package

Add a nested `<Entity>Collection` final class. Nested types inside a public
interface are implicitly `public static`; the constructor stays package-private
so only the domain's adapters can instantiate directly — callers go through
`of(...)` or `empty()`.

```java
final class <Entity>Collection extends BehavioralCollection<<Entity>> {

    <Entity>Collection(Collection<<Entity>> items) {
        super(items);
    }

    public static <Entity>Collection of(Collection<<Entity>> items) {
        return new <Entity>Collection(items);
    }

    public static <Entity>Collection empty() {
        return new <Entity>Collection(List.of());
    }
}
```

### If no collections namespace exists in the package

Create a new `<Package>EntityCollections.java` in
`<domain>-api/src/main/java/.../<package>/`:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public interface <Package>EntityCollections {

    final class <Entity>Collection extends BehavioralCollection<<Entity>> {
        <Entity>Collection(Collection<<Entity>> items) { super(items); }
        public static <Entity>Collection of(Collection<<Entity>> items) {
            return new <Entity>Collection(items);
        }
        public static <Entity>Collection empty() {
            return new <Entity>Collection(List.of());
        }
    }
}
```

---

## Step 3 — Entity Query Contract in the API Module

### If `<Package>Query.java` exists in the entity's package

Read it. Add a new delegate accessor method and nested interface:

```java
public interface <Package>Query {
    // ... existing accessors ...
    <Entity>EntityQuery <entityPlural>();   // e.g. lifeStages(), cultivars()

    // ... existing nested interfaces ...
    interface <Entity>EntityQuery
            extends EntityQuery<<Entity>Name, <Entity>, <Entity>Collection> {
        // domain-specific methods go here, if any
    }
}
```

### If no namespace query exists

Create a new `<Package>Query.java` in
`<domain>-api/src/main/java/.../<package>/`:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.EntityQuery;
import com.naturalist.<domain>.<Entity>Name;
import com.naturalist.<domain>.<subpackage>.<Package>EntityCollections.<Entity>Collection;

/**
 * Namespace query for the <package> sub-context — the single discoverable
 * entry point for reading <Entity> data.
 */
public interface <Package>Query {

    <Entity>EntityQuery <entityPlural>();

    interface <Entity>EntityQuery
            extends EntityQuery<<Entity>Name, <Entity>, <Entity>Collection> {
    }
}
```

Key rules:

- **Always a `public interface`** — ADR-020: nested types inside an interface
  are implicitly `public static`, which is what queries want
- **N=1 collapse**: if the package has exactly one entity and no existing
  namespace, you may declare a top-level `<Entity>Query interface extends
  EntityQuery<...>` directly and skip the namespace wrapper. Ask the user
  before doing this — the existing InsectQuery pattern is the usual choice.

---

## Step 4 — Entity Query Adapter in the Core Module

Create in `<domain>-core/src/main/java/` in the entity's package:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.<domain>.<Entity>Name;
import com.naturalist.<domain>.<subpackage>.<Package>EntityCollections.<Entity>Collection;
import com.naturalist.<domain>.<subpackage>.<Package>Query.<Entity>EntityQuery;

import java.util.Set;

class <Entity>EntityQueryImpl
        extends AbstractEntityQuery<
                        <Entity>Name,
                        <Entity>,
                        <Entity>Collection,
                        <Package>Repository.<Entity>EntityRepository>
        implements <Entity>EntityQuery {

    <Entity>EntityQueryImpl(<Package>Repository.<Entity>EntityRepository repository) {
        super(repository);
    }

    @Override
    public <Entity>Collection findByNameSet(Set<<Entity>Name> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return <Entity>Collection.of(repository().getByEntityNameSet(names));
    }
}
```

Rules:

- **Package-private** — the adapter is visible only to the namespace impl in
  the same package
- `getByName` is inherited from `AbstractEntityQuery`; override only to add
  domain-specific semantics
- Use `entityNameCollection(names, "names")` when `NAME extends EntityName`
  (the common case). For UUID-keyed entities use `identifierSet(names, "names")`

---

## Step 5 — Namespace Query Adapter (if the namespace is new)

If Step 3 created a new `<Package>Query`, also create the namespace impl in
`<domain>-core/src/main/java/` in the entity's package:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.observability.Observer;

class <Package>QueryImpl implements <Package>Query {

    private final <Entity>EntityQuery <entity>EntityQuery;

    <Package>QueryImpl(<Entity>EntityQuery <entity>EntityQuery) {
        Observer.forClass(<Package>QueryImpl.class).arguments("constructor", i -> i
                        .notNull(<entity>EntityQuery, "<entity>EntityQuery"))
                .throwWhenInvalid();
        this.<entity>EntityQuery = <entity>EntityQuery;
    }

    @Override
    public <Entity>EntityQuery <entityPlural>() {
        return <entity>EntityQuery;
    }
}
```

If the namespace impl already exists, add a new field, accessor, and
constructor parameter with the matching null check — keep the single-pass
validation pattern used by existing impls.

---

## Step 6 — Contract Test

Create in `<domain>-core/src/test/java/` in the entity's package:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.<domain>.<Entity>Name;
import com.naturalist.<domain>.Test<Domain>Identifiers;
import com.naturalist.<domain>.<subpackage>.<Package>EntityCollections.<Entity>Collection;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class <Entity>EntityQueryImplTest
        implements EntityQueryContractTest<<Entity>Name, <Entity>, <Entity>Collection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    <Entity>EntityRepositoryMock repository = new <Entity>EntityRepositoryMock(db);
    <Package>Query.<Entity>EntityQuery query = new <Entity>EntityQueryImpl(repository);

    @Override
    public EntityQuery<<Entity>Name, <Entity>, <Entity>Collection> query() {
        return query;
    }

    @Override
    public <Entity>Name notFoundName() {
        return Test<Domain>Identifiers./* path to */ notFound<Entity>Name;
    }

    @Override
    public List<<Entity>Name> knownEntityNames() {
        return List.of(
                Test<Domain>Identifiers./* path to */ knownA,
                Test<Domain>Identifiers./* path to */ knownB);
    }
}
```

`EntityQueryContractTest` lives in `kernels/framework-test`. The eight
inherited `@Test default` methods cover argument validation, no-match, and
expected-result paths for both `getByName` and `findByNameSet`.

Add any domain-specific test methods (extra query methods defined on the
nested interface) to this class as `@Test` methods.

---

## Step 7 — Namespace Query Test (if a new namespace impl was created)

Create a lightweight test covering constructor validation and accessor
semantics, mirroring `InsectLifeStageQueryImplTest`:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class <Package>QueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    <Entity>EntityRepositoryMock repository = new <Entity>EntityRepositoryMock(db);
    <Package>Query.<Entity>EntityQuery <entity>EntityQuery =
            new <Entity>EntityQueryImpl(repository);
    <Package>Query query = new <Package>QueryImpl(<entity>EntityQuery);

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(query.<entityPlural>()).isSameAs(<entity>EntityQuery);
    }

    @Test
    void accessors_idempotent() {
        assertThat(query.<entityPlural>()).isSameAs(query.<entityPlural>());
    }

    @Test
    void constructor_rejectsNull<Entity>EntityQuery() {
        assertThatThrownBy(() -> new <Package>QueryImpl(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("<entity>EntityQuery");
    }
}
```

For an existing namespace impl, extend its test instead of creating a new
one — add the new accessor assertions and the null-check for the new
constructor parameter.

---

## Step 8 — Verify

Do **not** run Maven yourself. Stop at the source edits and tell the user the
command to run:

```bash
mvn test -pl domains/<domain>/<domain>-core -am
```

The full inherited contract (eight tests per entity query) plus any
domain-specific tests must pass.

---

## Naming Conventions Summary

| Artifact       | Name                                                        | Location                                     |
|----------------|-------------------------------------------------------------|----------------------------------------------|
| Collection     | `<Entity>Collection` nested in `<Package>EntityCollections` | `<domain>-api/src/main/java/.../<package>/`  |
| Query contract | `<Entity>EntityQuery` nested in `<Package>Query`            | `<domain>-api/src/main/java/.../<package>/`  |
| Query adapter  | `<Entity>EntityQueryImpl`                                   | `<domain>-core/src/main/java/.../<package>/` |
| Namespace impl | `<Package>QueryImpl`                                        | `<domain>-core/src/main/java/.../<package>/` |
| Contract test  | `<Entity>EntityQueryImplTest`                               | `<domain>-core/src/test/java/.../<package>/` |
| Namespace test | `<Package>QueryImplTest`                                    | `<domain>-core/src/test/java/.../<package>/` |

---

## Cross-references

- ADR-010 — query design contract (thin, delegating, observe-and-dispatch)
- ADR-011 — BehavioralCollection and `final class` rule
- ADR-020 — namespace interface pattern (why queries use `interface` and
  repositories use `class`)
- ADR-021 — `PersistenceId` is adapter-internal
- `domains/CLAUDE.md` — query rules, N=1 collapse rule, namespace patterns