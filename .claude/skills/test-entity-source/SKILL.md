---
name: test-entity-source
description: Create a TestEntitySource, its test class, and JSON catalog file for an existing Entity. Use when adding a new test data source for a domain entity.
allowed-tools: Read Write Edit Glob Grep Bash
argument-hint: <EntityClassName> in <domain> module
---

Create a TestEntitySource for the entity specified in $ARGUMENTS.

## Step 1 — Locate the Entity

Find the entity class. Read it to determine:

- The entity's `EntityId` type (e.g. `CompoundId`)
- The entity's `EntityName` type — the `NAME` parameter of `Entity<ID, NAME>` (e.g. `CompoundName`)
- The entity's package (e.g. `com.naturalist.chemistry.compound`)
- The domain module name (e.g. `chemistry`)
- All constructor fields — these become the JSON keys
- Which fields need **secondary** unique constraints beyond the canonical `name()`:
  - `@EntityIdentifier` on a field means it is a secondary unique `EntityName` field
    (not a foreign key reference — those carry no annotation)
  - `@UniqueValue` on a field means it is a unique plain-value field (`String`, `int`, enum)
  - The canonical `name` component is **never** annotated `@EntityIdentifier`; its
    uniqueness is enforced automatically by `TestEntitySource`

## Step 2 — Verify Identifier Classes Exist

Check `domains/identifiers/` for the entity's `EntityId` and `EntityName` subclasses.
Both must have `@JsonCreator` factory methods. If missing, create them:

```java
package com.naturalist.<domain>.<subpackage>;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

public final class <Entity>Id extends EntityId<Long> {
    private <Entity>Id(Long value) {
        super(value);
    }

    @JsonCreator
    public static <Entity>Id of(Long value) {
        return new <Entity>Id(value);
    }
}
```

## Step 3 — Create the JSON Catalog File

Create `domains/<domain>/<domain>-repository-test/src/main/resources/<domain>/<subpackage>/<entityPlural>.json`
(resource sub-directory mirrors the Java sub-package under the domain, e.g.
`chemistry/compound/compounds.json` for `com.naturalist.chemistry.compound`)

Rules:

- `"id": null` — persistence ID is always null in catalog data
- `"name": "<slug>"` — the EntityName natural key value
- Remaining fields match the entity's constructor parameter names exactly
- Enum values use constant names: `"ROOT_MASS_FLOW"`, `"INORGANIC_SALT"`
- `PeriodicElement` enum uses chemical symbols: `"Ca"`, `"Mg"`, `"K"`
- Nullable fields use JSON `null`
- Boolean fields use JSON `true`/`false`
- When the entity is a child profile extracted from `chemical-science/catalog/compounds.json`,
  include a `compoundName` field with the parent compound's slug

If extracting from `chemical-science/catalog/compounds.json`:

1. Read compounds.json
2. For each compound that has a non-null value for the relevant nested object, extract it
3. Add `"id": null` and `"compoundName": "<compound-id-slug>"` to each extracted object
4. Copy field values verbatim — do not rename or restructure

## Step 4 — Create the TestEntitySource Class

Create in `domains/<domain>/<domain>-repository-test/src/main/java/` in the entity's package.

`TestEntitySource<ID, NAME, ENTITY>` enforces:

- **Primary key constraint** — duplicate `EntityId` on insert throws `PrimaryKeyConstraintException`
- **Name uniqueness** — the canonical `name()` is automatically checked on every insert; no
  subclass declaration is required or expected
- **Secondary unique constraints** — declared via `uniqueConstraints()`, which defaults to
  `List.of()`. Override only when the entity has constraints beyond the canonical name.

Determine secondary constraints from entity field annotations:

- `@EntityIdentifier` — secondary `EntityName` fields that must be unique (not the canonical
  `name` component, not foreign key references)
- `@UniqueValue` — plain value fields (`String`, `int`, enums) that must be unique

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class <Entity>TestEntitySource extends TestEntitySource<<Entity>Id, <Entity>Name, <Entity>> {

    public <Entity>TestEntitySource() {
        loadFile("<domain>/<subpackage>/<entityPlural>.json");
    }

    @Override
    protected <Entity>Id nextId() {
        return <Entity>Id.of(nextNumericId());
    }

    // Only override uniqueConstraints() if the entity has secondary unique fields
    // annotated @EntityIdentifier or @UniqueValue. If there are none, omit this method.
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

## Step 5 — Create the Test Class

Create in `domains/<domain>/<domain>-repository-test/src/test/java/` in the entity's package:

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.TestEntitySourceTest;

class <Entity>TestEntitySourceTest
        extends TestEntitySourceTest<<Entity>Id, <Entity>Name, <Entity>, <Entity>TestEntitySource> {
}
```

## Verification Checklist

Before finishing, confirm:

- [ ] Entity class implements `Entity<SomeId, SomeName>` (or `CatalogEntity` / `FactEntity`)
- [ ] The canonical `name` component is NOT annotated `@EntityIdentifier`
- [ ] Secondary unique `EntityName` fields carry `@EntityIdentifier`; secondary unique plain
      fields carry `@UniqueValue`; cross-domain FK `EntityName` fields carry neither
- [ ] EntityId class exists in `domains/identifiers/` with `@JsonCreator`
- [ ] EntityName class exists in `domains/identifiers/` with `@JsonCreator`
- [ ] JSON file field names match entity constructor parameter names exactly
- [ ] JSON `"id"` is `null` for every entry
- [ ] `TestEntitySource` type parameters are `<EntityId, EntityName, Entity>` (three args)
- [ ] `uniqueConstraints()` is overridden only for secondary constraints; canonical name is absent
- [ ] Test class extends `TestEntitySourceTest` with four type parameters
