---
name: test-entity-source
description: Create a TestEntitySource, its test class, and JSON catalog file for an existing domain entity. Use when adding a new test data source for a domain entity.
allowed-tools: Read Write Edit Glob Grep Bash
argument-hint: <EntityClassName> in <domain> module
---

Create a `TestEntitySource` for the entity specified in $ARGUMENTS.

`TestEntitySource` is the in-memory analog of an RDBMS table (ADR-001). It is keyed on the
entity's identity (`key()`) and enforces primary-key uniqueness, secondary unique
constraints, and intra-domain foreign-key constraints. The surrounding
`NaturalistDatabase` owns every source and is the sole thing that constructs them (see
Step 4).

## Step 1 — Locate the Entity

Find the entity class. Read it to determine:

- The entity's **key type** — the type parameter of its identity marker:
    - `NamedEntity<NAME extends EntityName>` → the `NAME` type (e.g. `CompoundName`) — the
      common case, a natural-key slug.
    - `Entity<ID extends EntityId>` → the `ID` type (e.g. `PlantImageId`) — a surrogate
      UUIDv7 key, used for observations, images, and other fact records.
  Both branches share the `Named<KEY>` port whose `key()` accessor the source keys on
  (ADR-022).
- The entity's package (e.g. `com.naturalist.chemistry.compound`)
- The domain module name (e.g. `chemistry`)
- All record components — these become the JSON keys
- **Intra-domain foreign keys** — components that reference another entity in the *same*
  domain by its `EntityName` (e.g. `PlantSpecies.genusName → PlantGenus`). Each becomes a
  `ForeignKeyConstraint` (Step 4). Cross-*domain* references carry no FK constraint —
  the DAG forbids resolving them in-memory; they are a service-layer concern.
- Which fields need **secondary** unique constraints beyond the canonical key:
    - `@EntityIdentifier` on a field means it is a secondary unique `EntityName` field
      (not a foreign key reference — those carry no annotation)
    - `@UniqueValue` on a field means it is a unique plain-value field (`String`, `int`, enum)
    - The canonical key component is **never** annotated `@EntityIdentifier`; its
      uniqueness is enforced automatically by `TestEntitySource`

## Step 2 — Verify the Identifier Class Exists

Check `domains/identifiers/` for the entity's key class.

- **`NamedEntity`** — an `EntityName` subclass with a `@JsonCreator` factory. If missing,
  create it:

```java
package com.naturalist.identifiers.<domain>;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class <Entity>Name extends EntityName {
    private static final int MAX_LENGTH = 64;

    private <Entity>Name(String value) {
        super(value);
    }

    @JsonCreator
    public static <Entity>Name of(String value) {
        return new <Entity>Name(value);
    }

    @Override
    protected int maxLength() {
        return MAX_LENGTH;
    }
}
```

- **`Entity`** — an `EntityId` subclass. Its values are UUIDv7 generated at record
  construction via the kernel generator (never `UUID.randomUUID()`); JSON fixtures for
  `Entity` records carry the id as a UUIDv7 string in an `id` field.

## Step 3 — Create the JSON Catalog File

Create `domains/<domain>/<domain>-repository-test/src/main/resources/<domain>/<subpackage>/<entityPlural>.json`
(resource sub-directory mirrors the Java sub-package under the domain, e.g.
`chemistry/compound/compounds.json` for `com.naturalist.chemistry.compound`)

Rules:

- For a `NamedEntity`: `"name": "<slug>"` — the `EntityName` natural key (lowercase
  kebab-case). There is **no** `"id"` field — `NamedEntity` records carry no surrogate id
  (ADR-022).
- For an `Entity`: `"id": "<uuidv7>"` — the `EntityId` value as a UUIDv7 string.
- Remaining fields match the entity's constructor parameter names exactly
- Enum values use constant names: `"ROOT_MASS_FLOW"`, `"INORGANIC_SALT"`
- `PeriodicElement` enum uses chemical symbols: `"Ca"`, `"Mg"`, `"K"`
- Nullable fields use JSON `null`
- Boolean fields use JSON `true`/`false`
- Intra-domain foreign keys are the referenced entity's slug (e.g. `"genusName": "thymus"`)

## Step 4 — Create the TestEntitySource Class

Create in `domains/<domain>/<domain>-repository-test/src/main/java/` in the entity's
package. The standalone class carries the domain prefix and drops any `Entity` infix per
ADR-020 §5 (`PlantSpeciesTestEntitySource`, `PlantImageTestEntitySource`).

`TestEntitySource<KEY, ENTITY extends Named<KEY>>` enforces:

- **Primary-key constraint** — a duplicate `key()` on insert throws
  `PrimaryKeyConstraintException`. Enforced automatically; no subclass declaration.
- **Secondary unique constraints** — declared via `uniqueConstraints()`, which defaults to
  `List.of()`. Override only when the entity has constraints beyond the canonical key.
- **Foreign-key constraints** — declared via `foreignKeyConstraints()`, which defaults to
  `List.of()`. Override for each intra-domain reference so a fixture whose parent is
  missing fails at load rather than at a later query. Throws `ForeignKeyConstraintException`.

**Construction contract.** The constructor takes a `NaturalistDatabase` and passes it to
`super(...)`, then loads the catalog. This exact single-arg signature is required:
`NaturalistDatabase#getNamed` reflectively invokes `getDeclaredConstructor(NaturalistDatabase.class)`
to build and cache the one shared instance of each source (ADR-001 — `NaturalistDatabase`
is the only object that instantiates a `TestEntitySource`). The `database` reference is
held so `foreignKeyConstraints()` can resolve foreign peer sources through the same
registry.

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class <Entity>TestEntitySource extends TestEntitySource<<Entity>Name, <Entity>> {

    public <Entity>TestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("<domain>/<subpackage>/<entityPlural>.json");
    }

    // Override only for intra-domain foreign keys. One entry per reference.
    @Override
    protected List<ForeignKeyConstraint<<Entity>, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "<fkFieldName>",
                <Entity>::<fkFieldName>,
                <ForeignEntity>TestEntitySource.class));
    }

    // Override only if the entity has secondary unique fields annotated @EntityIdentifier
    // or @UniqueValue. If there are none, omit this method.
    @Override
    protected List<UniqueConstraint<<Entity>>> uniqueConstraints() {
        return List.of(
            new UniqueConstraint<>() {
                @Override
                public String name() {
                    return "<secondaryFieldName>";
                }

                @Override
                public Function<<Entity>, ?> valueFunction() {
                    return <Entity>::<secondaryFieldName>;
                }
            }
        );
    }
}
```

Reference implementations: `PlantSpeciesTestEntitySource` (one FK to its parent genus),
`InsectFamilyTestEntitySource` (FK to its parent order).

## Step 5 — Create the Test Class

Create in `domains/<domain>/<domain>-repository-test/src/test/java/` in the entity's package.
`TestEntitySourceTest` takes three type parameters — key, entity, and the concrete source —
and asserts the catalog loads with at least the minimum number of entities (default 4;
override `minimumEntities()` only for a domain that genuinely holds fewer real instances):

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.TestEntitySourceTest;

class <Entity>TestEntitySourceTest
        extends TestEntitySourceTest<<Entity>Name, <Entity>, <Entity>TestEntitySource> {
}
```

## Verification Checklist

Before finishing, confirm:

- [ ] Entity class implements `NamedEntity<<Entity>Name>` (slug) or `Entity<<Entity>Id>`
  (surrogate UUIDv7 — observations, images, fact records)
- [ ] The canonical key component is NOT annotated `@EntityIdentifier`
- [ ] Secondary unique `EntityName` fields carry `@EntityIdentifier`; secondary unique plain
  fields carry `@UniqueValue`; foreign-key `EntityName` fields carry neither
- [ ] The identifier class exists in `domains/identifiers/` (`EntityName` subclass with
  `@JsonCreator`, or `EntityId` subclass)
- [ ] JSON file field names match entity constructor parameter names exactly
- [ ] JSON carries `"name"` for a `NamedEntity` (no `"id"`), or `"id"` for an `Entity`
- [ ] The constructor is `public <Entity>TestEntitySource(NaturalistDatabase database)` and
  calls `super(database)` — the exact signature `getNamed` reflects on
- [ ] `TestEntitySource` type parameters are `<KeyType, EntityType>` (two args)
- [ ] `foreignKeyConstraints()` declares one entry per intra-domain reference; cross-domain
  references declare none
- [ ] `uniqueConstraints()` is overridden only for secondary constraints; the canonical key is absent
- [ ] Test class extends `TestEntitySourceTest` with three type parameters
