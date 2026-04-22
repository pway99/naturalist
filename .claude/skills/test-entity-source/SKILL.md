---
name: test-entity-source
description: Create a NamedTestEntitySource, its test class, and JSON catalog file for an existing NamedEntity. Use when adding a new test data source for a domain entity.
allowed-tools: Read Write Edit Glob Grep Bash
argument-hint: <EntityClassName> in <domain> module
---

Create a `NamedTestEntitySource` for the entity specified in $ARGUMENTS.

## Step 1 — Locate the Entity

Find the entity class. Read it to determine:

- The entity's `EntityName` type — the `NAME` parameter of `NamedEntity<NAME>` (e.g. `CompoundName`)
- The entity's package (e.g. `com.naturalist.chemistry.compound`)
- The domain module name (e.g. `chemistry`)
- All record components — these become the JSON keys
- Which fields need **secondary** unique constraints beyond the canonical `name()`:
  - `@EntityIdentifier` on a field means it is a secondary unique `EntityName` field
    (not a foreign key reference — those carry no annotation)
  - `@UniqueValue` on a field means it is a unique plain-value field (`String`, `int`, enum)
  - The canonical `name` component is **never** annotated `@EntityIdentifier`; its
    uniqueness is enforced automatically by `NamedTestEntitySource`

## Step 2 — Verify EntityName Class Exists

Check `domains/identifiers/` for the entity's `EntityName` subclass. It must have a
`@JsonCreator` factory method. If missing, create it:

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

Domain records carry no persistence id (ADR-021) — there is no `EntityId` subclass to
create or reference.

## Step 3 — Create the JSON Catalog File

Create `domains/<domain>/<domain>-repository-test/src/main/resources/<domain>/<subpackage>/<entityPlural>.json`
(resource sub-directory mirrors the Java sub-package under the domain, e.g.
`chemistry/compound/compounds.json` for `com.naturalist.chemistry.compound`)

Rules:

- `"name": "<slug>"` — the `EntityName` natural key value (lowercase kebab-case)
- There is no `"id"` field — domain records carry no persistence id (ADR-021)
- Remaining fields match the entity's constructor parameter names exactly
- Enum values use constant names: `"ROOT_MASS_FLOW"`, `"INORGANIC_SALT"`
- `PeriodicElement` enum uses chemical symbols: `"Ca"`, `"Mg"`, `"K"`
- Nullable fields use JSON `null`
- Boolean fields use JSON `true`/`false`
- When the entity is a child profile extracted from a parent compound catalog,
  include a `compoundName` field with the parent compound's slug

## Step 4 — Create the NamedTestEntitySource Class

Create in `domains/<domain>/<domain>-repository-test/src/main/java/` in the entity's package.

`NamedTestEntitySource<NAME, ENTITY>` enforces:

- **Primary-key constraint** — duplicate `EntityName` on insert throws `PrimaryKeyConstraintException`
- **Secondary unique constraints** — declared via `uniqueConstraints()`, which defaults to
  `List.of()`. Override only when the entity has constraints beyond the canonical name.

Determine secondary constraints from entity field annotations:

- `@EntityIdentifier` — secondary `EntityName` fields that must be unique (not the canonical
  `name` component, not foreign key references)
- `@UniqueValue` — plain value fields (`String`, `int`, enums) that must be unique

```java
package com.naturalist.<domain>.<subpackage>;

import com.naturalist.data.NamedTestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class <Entity>TestEntitySource extends NamedTestEntitySource<<Entity>Name, <Entity>> {

    public <Entity>TestEntitySource() {
        loadFile("<domain>/<subpackage>/<entityPlural>.json");
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

import com.naturalist.data.NamedTestEntitySourceTest;

class <Entity>TestEntitySourceTest
        extends NamedTestEntitySourceTest<<Entity>Name, <Entity>, <Entity>TestEntitySource> {
}
```

## Verification Checklist

Before finishing, confirm:

- [ ] Entity class implements `NamedEntity<<Entity>Name>` (or `Entity<<Entity>Name>` for fact records)
- [ ] The canonical `name` component is NOT annotated `@EntityIdentifier`
- [ ] Secondary unique `EntityName` fields carry `@EntityIdentifier`; secondary unique plain
      fields carry `@UniqueValue`; cross-domain FK `EntityName` fields carry neither
- [ ] EntityName class exists in `domains/identifiers/` with `@JsonCreator`
- [ ] JSON file field names match entity constructor parameter names exactly
- [ ] JSON contains no `"id"` field
- [ ] `NamedTestEntitySource` type parameters are `<EntityName, Entity>` (two args)
- [ ] `uniqueConstraints()` is overridden only for secondary constraints; canonical name is absent
- [ ] Test class extends `NamedTestEntitySourceTest` with three type parameters
