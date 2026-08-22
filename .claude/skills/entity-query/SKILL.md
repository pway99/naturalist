---
name: entity-query
description: >
  Create the full query stack for an existing domain entity: the collection, the
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

1. `<EntitySubject>Collection` in the domain's entity-collections namespace (new
   file if none exists in the package)
2. `<EntitySubject>Query` contract — nested in an existing namespace `*Query`
   interface, or a new `<DomainNoun>Query` interface
3. `<Entity>QueryImpl` adapter in the core module, extending `AbstractEntityQuery`
4. Optional `<DomainNoun>QueryImpl` wiring — if the query is nested inside a
   namespace, a package-private impl exposes it
5. `<Entity>QueryImplTest` implementing `EntityQueryContractTest` from
   `framework-test`
6. Optional `<DomainNoun>QueryImplTest` covering constructor null-checks and
   accessor idempotence when a new namespace impl is created

Naming follows ADR-020 §5: **nested** namespace types drop the domain prefix and
the `Entity` infix (`SpeciesQuery`, `SpeciesCollection` inside `InsectQuery` /
`InsectEntityCollections`); **standalone** concrete classes carry the full entity
class name and drop the infix (`InsectSpeciesQueryImpl`,
`InsectSpeciesQueryImplTest`).

Reference implementation: the insects `InsectQuery` / `InsectSpeciesQueryImpl`
stack, and the sub-package `InsectLifeStageQuery` / `InsectLifeStageEntityQueryImpl`
stack.

---

## Prerequisites

Before starting, verify the following exist. If any are missing, stop and
report to the user.

- The entity class implementing `NamedEntity<<Entity>Name>` (slug) or
  `Entity<<Entity>Id>` (UUIDv7)
- The key class in `domains/identifiers/` (an `EntityName` or `EntityId` subclass)
- `<DomainNoun>Repository.<EntitySubject>Repository` contract (or an equivalent
  top-level repository interface for an N=1 package) in `<domain>-api`
- `<Entity>RepositoryMock` + `<Entity>TestEntitySource` in
  `<domain>-repository-test/src/main/java/`
- `Test<Domain>Identifiers` with at least two known key constants and a
  `NotFound` fictitious constant

---

## Step 1 — Locate the Entity and Gather Context

Find the entity class. Read it to determine:

- The entity's key type (e.g. `InsectSpeciesName`, `LifeStageName`)
- The entity's Java package (e.g. `com.naturalist.insects`,
  `com.naturalist.insects.lifestage`)
- The domain module name (e.g. `insects`)
- Whether an existing namespace query / collections namespace / namespace impl
  already lives in the same package:
    - `find <domain>-api/... -name "*Query.java"`
    - `find <domain>-api/... -name "*EntityCollections.java"`
    - `find <domain>-core/... -name "*QueryImpl.java"`

**Namespace naming.** `<DomainNoun>Query` is the domain noun for the package:
`com.naturalist.insects` → `InsectQuery`; `com.naturalist.insects.lifestage` →
`InsectLifeStageQuery` (sub-package compound noun). `<EntitySubject>` drops the
namespace prefix — `InsectSpecies` → `Species`, `InsectImage` → `Image`. A
sub-package entity that already reads cleanly without a prefix (`LifeStage`) keeps
its name as the subject.

---

## Step 2 — Entity Collection in the API Module

### If `<DomainNoun>EntityCollections.java` exists in the entity's package

Add a nested `<EntitySubject>Collection` final class. Nested types inside a public
interface are implicitly `public static`; the constructor stays package-private
so only the domain's adapters can instantiate directly — callers go through
`of(...)` or `empty()`.

```java
final class <EntitySubject>Collection extends BehavioralCollection<<Entity>> {

    <EntitySubject>Collection(Collection<<Entity>> items) {
        super(items);
    }

    public static <EntitySubject>Collection of(Collection<<Entity>> items) {
        return new <EntitySubject>Collection(items);
    }

    public static <EntitySubject>Collection empty() {
        return new <EntitySubject>Collection(List.of());
    }
}
```

### If no collections namespace exists in the package

Create a new `<DomainNoun>EntityCollections.java` in
`<domain>-api/src/main/java/.../<package>/`:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public interface <DomainNoun>EntityCollections {

    final class <EntitySubject>Collection extends BehavioralCollection<<Entity>> {
        <EntitySubject>Collection(Collection<<Entity>> items) { super(items); }
        public static <EntitySubject>Collection of(Collection<<Entity>> items) {
            return new <EntitySubject>Collection(items);
        }
        public static <EntitySubject>Collection empty() {
            return new <EntitySubject>Collection(List.of());
        }
    }
}
```

---

## Step 3 — Entity Query Contract in the API Module

### If `<DomainNoun>Query.java` exists in the entity's package

Read it. Add a new delegate accessor method and nested interface:

```java
public interface <DomainNoun>Query {
    // ... existing accessors ...
    <EntitySubject>Query <entitySubjectPlural>();   // e.g. species(), images()

    // ... existing nested interfaces ...
    interface <EntitySubject>Query
            extends EntityQuery<<Entity>Name, <Entity>, <EntitySubject>Collection> {
        // domain-specific methods go here, if any
    }
}
```

### If no namespace query exists

Create a new `<DomainNoun>Query.java` in
`<domain>-api/src/main/java/.../<package>/`:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.EntityQuery;
import com.naturalist.identifiers.<domain>.<Entity>Name;
import com.naturalist.<domain>.<subpackage>.<DomainNoun>EntityCollections.<EntitySubject>Collection;

/**
 * Namespace query for the <package> sub-context — the single discoverable
 * entry point for reading <Entity> data.
 */
public interface <DomainNoun>Query {

    <EntitySubject>Query <entitySubjectPlural>();

    interface <EntitySubject>Query
            extends EntityQuery<<Entity>Name, <Entity>, <EntitySubject>Collection> {
    }
}
```

Key rules:

- **Always a `public interface`** — ADR-020: nested types inside an interface
  are implicitly `public static`, which is what queries want
- **N=1 collapse**: if the package has exactly one entity and no existing
  namespace, declare a top-level `<EntitySubject>Query interface extends
  EntityQuery<...>` directly and skip the namespace wrapper. Ask the user
  before doing this — the existing `InsectQuery` namespace is the usual choice.

---

## Step 4 — Entity Query Adapter in the Core Module

Create in `<domain>-core/src/main/java/` in the entity's package. The standalone
impl carries the full entity class name and drops the `Entity` infix
(`InsectSpeciesQueryImpl`):

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.identifiers.<domain>.<Entity>Name;
import com.naturalist.<domain>.<subpackage>.<DomainNoun>EntityCollections.<EntitySubject>Collection;
import com.naturalist.<domain>.<subpackage>.<DomainNoun>Query.<EntitySubject>Query;

import java.util.Set;

class <Entity>QueryImpl
        extends AbstractEntityQuery<
                        <Entity>Name,
                        <Entity>,
                        <EntitySubject>Collection,
                        <DomainNoun>Repository.<EntitySubject>Repository>
        implements <EntitySubject>Query {

    <Entity>QueryImpl(<DomainNoun>Repository.<EntitySubject>Repository repository) {
        super(repository);
    }

    @Override
    public <EntitySubject>Collection findByNameSet(Set<<Entity>Name> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return <EntitySubject>Collection.of(repository().getByEntityNameSet(names));
    }
}
```

Rules:

- **Package-private** — the adapter is visible only to the namespace impl in
  the same package
- `getByName` is inherited from `AbstractEntityQuery`; override only to add
  domain-specific semantics
- Use `entityNameCollection(names, "names")` when the key `extends EntityName`
  (the common case). For UUID-keyed entities use `identifierSet(names, "names")`
- **Sub-package disambiguation:** when the impl name would collide with the
  namespace impl (e.g. both would be `InsectLifeStageQueryImpl`), the entity-level
  impl keeps the `Entity` infix — `InsectLifeStageEntityQueryImpl` — to distinguish
  it from the namespace `InsectLifeStageQueryImpl`

---

## Step 5 — Namespace Query Adapter (if the namespace is new)

If Step 3 created a new `<DomainNoun>Query`, also create the namespace impl in
`<domain>-core/src/main/java/` in the entity's package:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.observability.Observer;

class <DomainNoun>QueryImpl implements <DomainNoun>Query {

    private final <EntitySubject>Query <entitySubject>Query;

    <DomainNoun>QueryImpl(<EntitySubject>Query <entitySubject>Query) {
        Observer.forClass(<DomainNoun>QueryImpl.class).arguments("constructor", i -> i
                        .notNull(<entitySubject>Query, "<entitySubject>Query"))
                .throwWhenInvalid();
        this.<entitySubject>Query = <entitySubject>Query;
    }

    @Override
    public <EntitySubject>Query <entitySubjectPlural>() {
        return <entitySubject>Query;
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
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.identifiers.<domain>.<Entity>Name;
import com.naturalist.<domain>.Test<Domain>Identifiers;
import com.naturalist.<domain>.<subpackage>.<DomainNoun>EntityCollections.<EntitySubject>Collection;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class <Entity>QueryImplTest
        implements EntityQueryContractTest<<Entity>Name, <Entity>, <EntitySubject>Collection> {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    <Entity>RepositoryMock repository = new <Entity>RepositoryMock(db);
    <DomainNoun>Query.<EntitySubject>Query query = new <Entity>QueryImpl(repository);

    @Override
    public EntityQuery<<Entity>Name, <Entity>, <EntitySubject>Collection> query() {
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

`EntityQueryContractTest` lives in `kernels/framework-test`. The inherited
`@Test default` methods cover argument validation, no-match, and
expected-result paths for both `getByName` and `findByNameSet`.

Add any domain-specific test methods (extra query methods defined on the
nested interface) to this class as `@Test` methods.

---

## Step 7 — Namespace Query Test (if a new namespace impl was created)

Create a lightweight test covering constructor validation and accessor
semantics, mirroring `InsectLifeStageQueryImplTest`:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class <DomainNoun>QueryImplTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    <Entity>RepositoryMock repository = new <Entity>RepositoryMock(db);
    <DomainNoun>Query.<EntitySubject>Query <entitySubject>Query =
            new <Entity>QueryImpl(repository);
    <DomainNoun>Query query = new <DomainNoun>QueryImpl(<entitySubject>Query);

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(query.<entitySubjectPlural>()).isSameAs(<entitySubject>Query);
    }

    @Test
    void accessors_idempotent() {
        assertThat(query.<entitySubjectPlural>()).isSameAs(query.<entitySubjectPlural>());
    }

    @Test
    void constructor_rejectsNull<EntitySubject>Query() {
        assertThatThrownBy(() -> new <DomainNoun>QueryImpl(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("<entitySubject>Query");
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

The full inherited contract plus any domain-specific tests must pass.

---

## Naming Conventions Summary

| Artifact       | Name                                                              | Location                                     |
|----------------|------------------------------------------------------------------|----------------------------------------------|
| Collection     | `<EntitySubject>Collection` nested in `<DomainNoun>EntityCollections` | `<domain>-api/src/main/java/.../<package>/`  |
| Query contract | `<EntitySubject>Query` nested in `<DomainNoun>Query`             | `<domain>-api/src/main/java/.../<package>/`  |
| Query adapter  | `<Entity>QueryImpl`                                               | `<domain>-core/src/main/java/.../<package>/` |
| Namespace impl | `<DomainNoun>QueryImpl`                                           | `<domain>-core/src/main/java/.../<package>/` |
| Contract test  | `<Entity>QueryImplTest`                                           | `<domain>-core/src/test/java/.../<package>/` |
| Namespace test | `<DomainNoun>QueryImplTest`                                       | `<domain>-core/src/test/java/.../<package>/` |

`<Entity>` is the full entity class name (`InsectSpecies`); `<EntitySubject>`
drops the domain prefix (`Species`); `<DomainNoun>` is the namespace prefix
(`Insect`).

---

## Cross-references

- ADR-010 — query design contract (thin, delegating, observe-and-dispatch)
- ADR-011 — BehavioralCollection and `final class` rule
- ADR-020 — namespace interface pattern (why queries use `interface` and
  repositories use `class`; §5 standalone-vs-nested naming)
- ADR-022 — single identity per entity (`key()`); no `PersistenceId`
- `domains/CLAUDE.md` — query rules, N=1 collapse rule, namespace patterns
