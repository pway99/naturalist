# Entity Identity Accessor — `id`, not `name`

## Problem

`Named<KEY>` (ADR-022) is the shared data-layer port. It declares a single accessor,
`name()`, and **both** identity branches extend it:

```
Named<KEY>  →  KEY name()
├─ NamedEntity<NAME extends EntityName>   (name() returns a slug — natural)
└─ Entity<ID extends EntityId>            (name() returns a UUID — awkward)
```

A Java record's component name *is* its accessor name, so every `Entity` record is
forced to declare its identity component as `name`. The result is
`InsectImage(InsectImageId name, …)`, the invariant `.entityId(name, "name")`, and
`insectImage.name()` returning a UUID — a surrogate id wearing the label of a natural
key.

This document specifies the change that lets `Entity` records carry their identity as
`id` while keeping the unified data-layer port intact.

## Decision

Split the infrastructural port from the branch-semantic accessors. The port keeps one
neutral accessor that the data layer calls; each branch declares its own honest
accessor and supplies the port method as a free delegating default.

### Why `key()` for the port

The port returns the typed identifier, which itself unwraps via `value()`
(`EntityName.value()` → `String`, `EntityId.value()` → `UUID`). Naming the port
`value()` would produce a confusing `entity.value().value()` chain and falsely imply
value-object semantics on an identity type. `key()` names the role the port actually
plays — the data-layer lookup/cache key — and matches the ADR-022 type parameter
`Named<KEY>`. (`identity()` was the runner-up; `key()` wins on brevity and on echoing
the type-param name.)

### Kernel shape

```java
// Named.java — rename method and type parameter
public interface Named<KEY> extends Observable {
    KEY key();                                       // the data-layer port
}

// NamedEntity.java — declare name() explicitly, delegate key()
public interface NamedEntity<NAME extends EntityName> extends Named<NAME> {
    NAME name();
    default NAME key() { return name(); }
}

// Entity.java — declare id(), delegate key()
public interface Entity<ID extends EntityId> extends Named<ID> {
    ID id();
    default ID key() { return id(); }
}
```

Consequences of the shape:

- `Named.name()` ceases to exist; the port is `key()`. `Entity` no longer exposes
  `name()` at all — every `.name()` call on a concrete `Entity` becomes a compile
  error, so the compiler enumerates the read sites for us.
- `NamedEntity` records are **unchanged** — they still declare a `name` component, and
  `key()` is inherited as a free default delegating to `name()`.
- `EntityName` and `EntityId` are **untouched**. Only the accessor that *returns* them
  is renamed; the identifier value types, their `@JsonValue`/`@JsonCreator` wiring, and
  cross-domain slug references (ADR-001) are unaffected.

### What does NOT change — verbs stay

The data-layer *method vocabulary* is name-shaped (`getByName`, `getByNameSet`,
`notFoundName()`, `knownEntityNames()`), but a method name is independent of the
accessor, so these keep compiling and keep their names. Their bodies simply pass
`entity.key()` instead of `entity.name()`.

This leaves one accepted residual wart: `imageQuery.getByName(someInsectImageId)` reads
as "get by name" handed an id. Renaming the verbs (`getByName → getByKey`, etc.) would
ripple through every domain's query interface, repository, mock, and contract test —
`NamedEntity` domains included — for a consistency payoff disproportionate to the cost.
It is deferred as a separate, independently-weighable follow-up (work tracker / parking
lot), not part of this effort.

## Scope of change

The exact set of `Entity` records is whatever exists at implementation time (the
identity branch is growing — e.g. the in-flight `CitationAssociation`). At the time of
writing the design, the `Entity` implementers are:

`SensorReading`, `CompoundDepiction`, `ZonePrecipitationEvent`, soil
`PrecipitationEvent` / `IrrigationEvent` / `TillageEvent` / `AmendmentEvent` /
`LabAnalysis`, `InsectImage`, `InsectFunctionalRole`, weather `PrecipitationEvent`.

Do not trust this list at implementation time — re-grep `implements Entity<` and let
the compiler confirm completeness.

### 1. Kernel (`kernels/framework/ddd`)

- `Named.java` — rename `name()` → `key()`, type param `NAME` → `KEY`.
- `NamedEntity.java` — add abstract `NAME name()` + `default NAME key() { return name(); }`.
- `Entity.java` — add abstract `ID id()` + `default ID key() { return id(); }`.

### 2. `Entity` records

For each: component `name` → `id`; the invariant `.entityId(name, "name")` →
`.entityId(id, "id")`; any internal self-reference to the component follows.

### 3. JSON fixtures

The repository-test fixtures for the `Entity` records relabel the UUID field
`"name"` → `"id"` (e.g. `insect-images.json`, `insect-functional-roles.json`,
`depictions.json`, the soil event files, zone/weather precipitation, sensor readings).
`NamedEntity` fixtures keep `"name"`.

### 4. Generic data layer (`kernels/framework-test/data`)

Swap entity-keyed `.name()` → `.key()` in `TestEntitySource`,
`AbstractTestEntityRepository`, `EntityQueryContractTest`, `EntityCommandContractTest`,
`EntityRepositoryTest`. Method **names** (`getByName`, hooks) are unchanged.

**Must NOT change:** `uniqueConstraint.name()` and `fk.name()` in `TestEntitySource` —
these are constraint/column labels, not entity keys.

### 5. `Entity` read sites

`.name()` → `.id()` wherever a concrete `Entity` is read — `ImageQueryImpl`,
`InsectTaxonViewFactory`, console JTE templates, the `Entity` `TestEntitySource`
subclasses, and `TestIdentifiers` `Entity` constants. The compiler lists these once the
kernel change lands.

### 6. Docs

- ADR-022: one-line clarification — the shared port accessor is `key()` (`name()` on
  `NamedEntity`, `id()` on `Entity`); a JSON `"id"` field carrying the domain `EntityId`
  is correct, and the still-forbidden case is a persistence / `Long` surrogate key. The
  `Named<KEY>` prose now matches the Java.
- `domains/CLAUDE.md` and `domains/insects/CLAUDE.md`: update the lines describing
  `Entity` / `InsectImage` carrying its id as `name`.

## Non-goals

- Renaming the data-layer method verbs (`getByName`, etc.) — deferred follow-up.
- Any change to `NamedEntity`, `EntityName`, `EntityId`, slug semantics, or
  cross-domain reference rules.
- Any change to persistence behavior — `PersistenceId` remains absent (ADR-022).

## Risk

A pure rename, not an arity change, but it ripples to every `Entity` read site and
every `Entity` fixture. The kernel change makes omissions compile errors (for accessor
calls) rather than silent failures; the JSON relabeling and the `.entityId(…, "id")`
invariant string are the parts the compiler *cannot* catch, so they need a deliberate
`mvn verify` and a fixture-by-fixture check. Sequence the kernel change first, then let
the build enumerate the rest.
