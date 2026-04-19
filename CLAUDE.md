# The amateur Naturalist

Modular-monolith domain application in `com.naturalist`. Architecture decisions live in
[`docs/adr/`](docs/adr/README.md) — see the index there.

Detailed conventions are split by area and loaded only when working in that area:

- [`kernels/CLAUDE.md`](kernels/CLAUDE.md) — framework, observability, testing observables,
  BehavioralCollection
- [`domains/CLAUDE.md`](domains/CLAUDE.md) — test identifiers, TestEntitySource, repository
  architecture, new-module scaffolding, PR size discipline
- [`domains/<domain>/CLAUDE.md`](domains/) — per-domain vocabulary and invariants
- [`docs/open-questions.md`](docs/open-questions.md) — design questions in flight
- [`docs/pending-implementation.md`](docs/pending-implementation.md) — priority-ordered TODO
- [`docs/measurement-standards.md`](docs/measurement-standards.md) — units reference

## Identity Model

Every domain class implements one of three interfaces from `kernels/framework`:

- **Entity\<ID extends PersistenceId\<?\>, NAME extends EntityName\<?\>\>** — stable
  identity, `id()`, non-nullable `name()`, `withId(ID)`, `invariants()`
- **Aggregate** — consistency boundary, owns child entities and value objects. No
  `withId()`; declare explicit `with*` methods per field
- **ValueObject** — immutable, no identity, equality by value. Must satisfy all four
  ADR-013 constraints: no Entity/Aggregate members, does not uniquely identify an entity,
  cohesive ubiquitous-language concept, members have collective meaning (not a projection
  of an entity's fields)
- **BehavioralCollection\<T extends Observable\>** — abstract base class in
  `kernels/framework` for multi-result query return types. Extended by `final class` per
  domain (e.g. `CompoundCollection`). Not a record. See ADR-011.

All four extend `Observable` and require `invariants()`.

`PersistenceId<Long>` is null in JSON catalogs — assigned by `TestEntitySource.nextId()`
or RDBMS. Concrete subclasses live in `domains/identifiers/`.

`EntityName` is an abstract class wrapping a `String` — the domain's human-readable
natural key (slug) for catalog entities, or a UUID string for fact entities (`FactName`).
Never null on any entity.

### Field Annotations

- `@EntityIdentifier` — marks a *secondary* `EntityName` component as unique within its
  data source. The canonical `name()` component is automatically enforced — do not
  annotate it. Cross-domain FK `EntityName` references carry no annotation.
- `@UniqueValue` — marks plain value components (`String`, `int`, enums) that must be
  unique. Both annotations require explicit declaration in `uniqueConstraints()`.

## Java Record Conventions

Entity, Aggregate, and ValueObject are Java records. No Lombok. `BehavioralCollection` is
the single exception — `final class` to enable package-private construction (see ADR-011).

Reference implementations: `Compound` (Entity), `PotatoLeafhopper` (Entity),
`CompoundInfo` (ValueObject), `CompoundCollection` (BehavioralCollection). Reading the
source is faster than a spec.

Rules:
- Accessor names match component names exactly: `id()`, `name()`, `someField()` — never
  `getId()`, `getName()`
- Boolean components use plain names: `active`, `beneficial`. Predicate methods use `is*`
  prefix only when they are behavior methods, not component accessors
- `withId(ID id)` is the only with-method on Entity — all others are explicit on the
  concrete record
- Jackson 2.19.x natively deserializes records. No `@JsonCreator` on entity/aggregate/
  value object records. JSON field names must match component names exactly
- `@JsonCreator` **is** required on the `public static of(...)` factory of any non-record
  Jackson must deserialize: `PersistenceId<Long>` subclasses, `EntityName` subclasses,
  `NamedValue<T>` implementations. Without it, deserialization fails silently or with a
  misleading error
- `@EntityIdentifier` placed before the type: `@EntityIdentifier FooName name`
- Optional-returning query methods must not share a name with any component: a component
  `String biologicalCatalyst` needs accessor `biologicalCatalystOptional()`, not
  `biologicalCatalyst()`
- Static factory methods (`of(...)`, `empty()`, `from(...)`) are the public instantiation
  API for all domain types. `new Foo(...)` at a call site outside the type's own class is
  a review flag. See ADR-012

## Module Structure and DAG

```
kernels/
  framework/          — Entity, PersistenceId, EntityName, Aggregate, ValueObject,
                        Observable, Observer, BehavioralCollection
  framework-test/     — TestEntitySource, TestEntitySourceTest, TestDataHelper
  field-notes/        — Description (four-level Durrell description)
  taxonomy/           — TaxonomicClassification (organism domains only)

domains/
  identifiers/        — typed IDs and names only
  <domain>/<domain>-api, <domain>-core, <domain>-repository-test
```

DAG (no cycles — call out violations immediately):

```
bootstrap                   →  application
<domain>-repository-test    →  <domain>-api
<domain>-repository-rdms    →  <domain>-api
<domain>-core               →  <domain>-api
<domain>-api                →  framework, identifiers, field-notes
<organism>-api              →  framework, identifiers, field-notes, taxonomy
identifiers                 →  framework
field-notes                 →  framework
taxonomy                    →  framework
framework                   →  (nothing — external libs only)
framework-test              →  framework
```

Hard rules:

1. **api modules may only depend on `framework`, `identifiers`, `field-notes`, and
   (organism domains) `taxonomy`.** No other inter-module dependencies in api modules.
2. **Domain modules access other domains only via their api.** A core module may import
   from a foreign domain's api; never from core, repository-test, or repository-rdms.
3. **repository modules depend only on their own api** (plus framework).
4. **bootstrap depends on everything; nothing depends on bootstrap.**
5. **Repository interfaces are package-private** in the api module — inter-domain
   interaction goes through public service/query classes.
6. **Repository behavior contracts** are defined as interfaces in repository-test,
   implemented by both in-memory and rdms adapters.

Package-private visibility is the primary enforcement mechanism. ArchUnit tests may
verify the DAG at build time.

## Sub-Context Boundaries

Within a module, sub-contexts enforce boundaries via Java visibility.
`package-private` = internal to sub-context. `public` = explicitly crosses sub-context
boundary.

```
soil/
  observation/
    SensorReading.java       ← package-private (internal)
    ObservationService.java  ← public (crosses boundary)
  event/
    AmendmentEvent.java      ← package-private (internal)
    EventService.java        ← public (crosses boundary)
```

## Strongly Typed Identifiers

Never raw `String` or `Long` as an entity reference across any boundary. Every entity has
a concrete `PersistenceId<Long>` subclass in `domains/identifiers/`.

```java
repository.findByZone(ZoneId.of("backyard-garden"));    // correct
repository.findByZone("backyard-garden");                // compile error by design
repository.findByZone(SensorId.of("wh51-backyard"));     // wrong type
```

## Domain Knowledge as Code

Agronomic rules are code, not configuration. `OptimumRanges`, `Amendments`,
`Amendment.evaluateSafety()` are domain invariants, not configurable parameters.
Observations and Events are immutable facts — they happened and cannot be changed.
Records or final fields, no setters, equality by value.
