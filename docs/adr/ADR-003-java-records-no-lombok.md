# ADR-003: Java Records, No Lombok

**Status:** Accepted

## Context

Domain classes need value semantics — immutability, equality by value, concise declaration.
Lombok was initially used to generate `equals`, `hashCode`, `toString`, and `with*` methods.
The project targets Java 17+.

## Decision

All domain classes (Entity, Aggregate, ValueObject) are Java records. Lombok is not used.

### Rationale

Records are a first-class Java language feature. They make immutability, component declaration,
and value semantics explicit and readable without annotation processing. Lombok annotation
processing adds build complexity, IDE dependency, and a layer of indirection between source
and behaviour — when Lombok is removed, the generated methods disappear silently, which is
exactly the class of bug that caused `EntityName` and `PersistenceId` equality to break.

### equals/hashCode on EntityName and PersistenceId

Records generate component-based `equals`/`hashCode` automatically. However, `EntityName`
and `PersistenceId` are abstract classes (not records) because they are parameterized base types
that concrete subclasses extend. They require explicit `equals`/`hashCode` defined on the
abstract class itself, using `getClass()` for the type check — not `instanceof` — so that
`ElementName.of("carbon")` and `CompoundName.of("carbon")` are never equal.

```java
// EntityName
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EntityName that = (EntityName) o;
    return Objects.equals(value, that.value);
}
```

### Custom equals/hashCode on Entity records

Entity records that require identity-based equality (rather than full component equality)
declare custom `equals`/`hashCode` inside the record body:

```java
// Element uses identity-based equality on id only
@Override
public boolean equals(Object o) {
    if (!(o instanceof Element e)) return false;
    return id.equals(e.id);
}
```

## Consequences

- No annotation processing required at build time
- `equals`/`hashCode`/`toString` are always visible in source — no hidden generation
- Removing a dependency cannot silently break equality semantics
- `with*` methods are explicit on each record, which is more verbose but makes mutation
  points visible and intentional
