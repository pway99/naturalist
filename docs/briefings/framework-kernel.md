# Naturalist — Framework Kernel Briefing

**Purpose.** Shared structural briefing — paired with **one or more**
domain briefings (e.g. `insects-domain.md`, `chemistry-domain.md`,
`plants-domain.md`) to give a chat Claude instance everything it needs
to model an Aggregate, Entity, or ValueObject **for the api of those
domains** and emit Java records that drop cleanly into the
`com.naturalist` codebase. Cross-domain conversations (e.g. an insect
chemical-defense story that spans `insects-api` and `chemistry-api`)
upload one domain briefing per domain involved.

**Architecture.** `com.naturalist` is a **modular monolith** — multiple
bounded contexts (`<domain>-api` modules) inside a single deployable.
Cross-domain references cross *bounded-context* boundaries, not network
boundaries: they are typed `EntityName` slugs resolved in-process, not
REST calls or messaging contracts. Do not propose distributed-systems
patterns (service contracts, sagas, eventual-consistency reconciliation)
at the api boundary.

**Scope.** What an api consumer or api author needs:

- Module layout (api-side only)
- Identity model (six identity interfaces)
- Record conventions
- Annotations the api uses
- Jackson rules that affect the api wire form
- The complete `Constraints` API for `invariants()`
- The api namespace pattern (`Query`, `Command`, `EntityCollections`,
  `Repository` contract surface)
- Data-layer ports (`EntityQuery`, `EntityCommand`, `EntityRepository`)
- Anti-patterns

**Out of scope.** Persistence adapters (`<domain>-core`,
`<domain>-repository-test`, `<domain>-repository-rdms`), JTE templates,
test scaffolding, JSON catalog files, ADR text. None of those affect the
shape of the api a domain exposes.

**Primary rule.** When you don't know whether something exists (a method
on `Constraints`, a package path, a type name), say so rather than
invent. Invented APIs and invented package paths are the dominant
failure mode.

**How to use.** Upload this briefing **plus** one matching
`<domain>-*.md` per domain involved from `docs/briefings/`. Each domain
briefing carries that domain's vocabulary and the current state of its
api; this briefing carries the structural glue every api shares.

**Briefing date.** 2026-06-14. If a type or method listed here does not
match what you observe in code, trust the code.

---

## 1. Module Layout — api side only

```
kernels/
  framework/        NamedEntity, Entity, Aggregate, ReadModel, ValueObject,
                    BehavioralCollection, BehavioralMap, Observable,
                    Constraints, EntityName, EntityId, EntityNameSet,
                    NamedValue, NumericNamedValue,
                    Named (common supertype),
                    EntityRepository, EntityQuery, EntityCommand,
                    Page, PageRequest, FileName,
                    AggregateRoot, @EntityIdentifier, @UniqueValue,
                    @DomainService, @Incubating,
                    Resilience facade (@Resilient, @ResilienceExempt)
  framework-test/   TestEntitySource, EntityRepositoryTest,
                    EntityQueryContractTest, RandomValue,
                    NaturalistDatabase, TestDataHelper
  field-notes/      Description (four-level Durrell), CommonName
  taxonomy/         TaxonomicClassification, TaxonomicOrder/Family/Genus/Species,
                    LinnaeanOrder/Family/Genus/Species (interfaces),
                    LinealRank, TaxonomicSlugs
  clades/           Clade (sealed type — evolutionary tree of life)
  habitat/          HabitatProfile (structured habitat classification)
  biogeography/     Bioregion (cross-domain native-range vocabulary)
  measurements/     Unit value objects (temperature, mass, volume, etc.)
  catalog/          DomainId, Catalog, CatalogContribution (cross-domain
                    reference resolution)
  catalog-inmem/    InMemoryCatalog (in-process reference adapter)
  authority/        External-authority seam (EOL, etc.)

domains/
  identifiers/      EntityName + EntityId subclasses ONLY
  <domain>/<domain>-api    ← what an api author or consumer touches
```

Other modules exist (`<domain>-core` adapters, `<domain>-repository-*`,
`<domain>-console`, `<domain>-test-context`) but the api never
depends on or knows about them.

### api-side dependency rules

- `<domain>-api` depends only on `framework`, `identifiers`,
  `field-notes`, and (organism only) `taxonomy`, `habitat`, `clades`,
  `biogeography`, `measurements`.
- A domain `api` may also reference another domain's `api` if it carries
  that domain's typed `EntityName` (e.g. `insects-api → plants-api` to
  type `LarvaStage.hostPlants` as `List<PlantName>`). This is a
  **slug-only** dependency — never reach across to another domain's
  records, queries, or aggregates.
- Repository **interfaces** declared in `<domain>-api` are
  package-private. The implementations live in `<domain>-core` and are
  not part of this briefing's scope.

---

## 2. Identity Model

**Every domain class implements exactly one of six interfaces from
`kernels/framework`:**

| Interface                              | Identity                      | Usage                                                     |
|----------------------------------------|-------------------------------|-----------------------------------------------------------|
| `NamedEntity<NAME extends EntityName>` | Natural-key slug              | Catalog entries, stable cross-domain references           |
| `Entity<ID extends EntityId>`          | Surrogate UUIDv7              | Events, observations, relationships with no natural key   |
| `Aggregate`                            | Via aggregate root's identity | Consistency boundaries that own children                  |
| `ReadModel`                            | Optional                      | Read-side projections assembled from persisted parts      |
| `ValueObject`                          | None — equality by value      | Immutable components                                      |
| `BehavioralCollection<T>`              | None                          | Multi-result query return types (final class, not record) |

All six extend `Observable` and declare `invariants()`.

### `Named<KEY>` — shared data-layer port

`NamedEntity` and `Entity` both extend `Named<KEY>`, which declares
a single neutral accessor `KEY key()` and extends `Observable`. Each
branch declares its own semantic accessor and supplies `key()` as a
free delegating default:

- `NamedEntity<NAME>` declares `NAME name()`;
  `default NAME key() { return name(); }`
- `Entity<ID>` declares `ID id()`;
  `default ID key() { return id(); }`

This split lets `Entity` records carry their identity as `id` (accessor
`id()`) while keeping the unified data-layer port intact. The data-layer
method vocabulary is name-shaped (`getByName`, `getByNameSet`,
`notFoundName()`, `knownEntityNames()`) — method names are independent
of the accessor; their bodies pass `entity.key()`.

`Aggregate`, `ReadModel`, and `ValueObject` do NOT extend `Named`.

### `ReadModel` vs `Aggregate`

An `Aggregate` is a consistency boundary that **owns** child entities
and value objects and is **mutated as a unit**. A `ReadModel` is a
read-side projection **assembled** from already-persisted parts — it
owns nothing, is never persisted, and is never a write unit. Its
`invariants()` assert the projection's structural well-formedness
(required parts non-null, monotonic fill, cross-FK consistency), not
cross-entity consistency.

Use `ReadModel` when the type owns nothing and is never mutated as a
unit (e.g. `InsectTaxonView`, `Insect`). Use `Aggregate` when it
is a transactional boundary (e.g. `Zone`, `SoilProfile`).

### Naming rules

- `EntityName` subclasses are slug-identified: regex
  `^[a-z0-9]+(-[a-z0-9]+)*$`, kebab-case, never null. Each subclass
  declares `maxLength()`.
- `EntityId` subclasses are UUIDv7, validated `version() == 7` at
  boundary, generated at record construction via `EntityId.newUUID()`.
- **Do not call `UUID.randomUUID()` anywhere in api code.** Use the
  kernel generator only.
- `NamedEntity` records carry their identity as the `name` component
  (accessor `name()`). `Entity` records carry their identity as the
  `id` component (accessor `id()`).
- Cross-`NamedEntity` references are by `EntityName`.
- `Entity` records are never referenced cross-domain by value — cross
  the parent's `EntityName` slug instead.
- The two identity branches (`NamedEntity` slug, `Entity` UUIDv7) are
  the **only** identity-shaped components on a domain record. No
  `PersistenceId` type exists in Java; surrogate primary keys are
  private to whichever adapter persists the record and never surface on
  the api.

### Identifier placement — CRITICAL

**Identifier classes live in the `identifiers` module, under a
sub-package named after their home domain — NOT under
`com.naturalist.identifiers`.**

Representative locations:

| Type                | Actual package                      | Module        |
|---------------------|-------------------------------------|---------------|
| `InsectSpeciesName` | `com.naturalist.insects`            | `identifiers` |
| `InsectRankName`    | `com.naturalist.insects`            | `identifiers` |
| `InsectImageId`     | `com.naturalist.insects`            | `identifiers` |
| `PlantName`         | `com.naturalist.plants`             | `identifiers` |
| `CultivarName`      | `com.naturalist.plants.cultivar`    | `identifiers` |
| `CompoundName`      | `com.naturalist.chemistry.compound` | `identifiers` |
| `ElementName`       | `com.naturalist.chemistry.element`  | `identifiers` |
| `SoilProfileName`   | `com.naturalist.soil`               | `identifiers` |
| `AmendmentEventId`  | `com.naturalist.soil.event`         | `identifiers` |
| `ZoneName`          | `com.naturalist.zone`               | `identifiers` |
| `NaturalistName`    | `com.naturalist.naturalist`         | `identifiers` |

**There is no `com.naturalist.identifiers` package.** The *module* is
`identifiers`; the *package* tracks the home domain.

### Framework types — canonical packages

| Type                                                                         | Package                                    | Module        |
|------------------------------------------------------------------------------|--------------------------------------------|---------------|
| `Named`, `NamedEntity`, `Entity`, `Aggregate`, `ReadModel`, `ValueObject`    | `com.naturalist.ddd`                       | `framework`   |
| `EntityName`, `EntityId`, `EntityNameSet`, `NamedValue`, `NumericNamedValue` | `com.naturalist.ddd`                       | `framework`   |
| `BehavioralCollection`, `BehavioralMap`                                      | `com.naturalist.ddd`                       | `framework`   |
| `AggregateRoot`, `EntityIdentifier`, `UniqueValue`                           | `com.naturalist.ddd`                       | `framework`   |
| `Observable`                                                                 | `com.naturalist.observability`             | `framework`   |
| `Constraints`, `Constraint`, `ConstraintCollection`, `Observer`              | `com.naturalist.observability`             | `framework`   |
| `MethodObserver`, `InvariantObservation`, `Metric`, `Level`                  | `com.naturalist.observability`             | `framework`   |
| Constraint records (`NotNullConstraint`, `NotBlankConstraint`, etc.)         | `com.naturalist.observability.constraints` | `framework`   |
| `EntityRepository`, `EntityQuery`, `EntityCommand`                           | `com.naturalist.data`                      | `framework`   |
| `AbstractEntityRepository`, `AbstractEntityQuery`, `AbstractEntityCommand`   | `com.naturalist.data`                      | `framework`   |
| `Page`, `PageRequest`, `FileName`                                            | `com.naturalist.data`                      | `framework`   |
| `@DomainService`                                                             | `com.naturalist.infrastructure`            | `framework`   |
| `@Incubating`                                                                | `com.naturalist`                           | `framework`   |
| `Resilience`, `Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead`               | `com.naturalist.resilience`                | `framework`   |
| `@Resilient`, `@ResilienceExempt`, `ResilienceConfig`                        | `com.naturalist.resilience`                | `framework`   |
| `Description`, `CommonName`                                                  | `com.naturalist.fieldnotes`                | `field-notes` |
| `TaxonomicOrder/Family/Genus/Species`, `LinnaeanOrder/Family/Genus/Species`  | `com.naturalist.taxonomy`                  | `taxonomy`    |
| `TaxonomicClassification`, `LinealRank`, `TaxonomicSlugs`                    | `com.naturalist.taxonomy`                  | `taxonomy`    |
| `Clade` (sealed), per-clade permits (`Holometabola`, `Hemiptera`, ...)       | `com.naturalist.clades`                    | `clades`      |
| `HabitatProfile`                                                             | `com.naturalist.habitat`                   | `habitat`     |
| `DomainId`, `Catalog`, `CatalogContribution`                                 | `com.naturalist.catalog`                   | `catalog`     |

---

## 3. Record Conventions

- `NamedEntity`, `Entity`, `ReadModel`, `ValueObject` are **Java
  records**.
- `Aggregate` may be a record.
- `BehavioralCollection` is the exception: `final class` (records
  cannot have a package-private constructor distinct from public
  factories, which the collection contract requires).
- `BehavioralMap<K, V>` extends `BehavioralCollection<V>` — abstract
  class with a secondary index (keyed lookup without raw `Map` at the
  boundary).
- No Lombok. Java 17+ — records, sealed interfaces, pattern matching,
  switch expressions.

### Accessor rules

- Accessor names match component names exactly: `name()`, `someField()`
  — never `getName()`.
- Boolean components use plain names: `active`, `beneficial`. Predicate
  methods use `is*` prefix only when they are behavior methods, not
  component accessors.
- `NamedEntity` records: identity component is `name`, accessor
  `name()`, immutable. `Entity` records: identity component is `id`,
  accessor `id()`, immutable.
- Every mutable field on a concrete `NamedEntity` or `Entity` record
  needs an explicit `with*` method; the identity component is immutable.
- Optional-returning query methods must not share a name with any
  component: a component `String biologicalCatalyst` needs accessor
  `biologicalCatalystOptional()`, not `biologicalCatalyst()`.

### Construction

- Static factory methods (`of(...)`, `empty()`, `from(...)`) are the
  public instantiation API.
- `new Foo(...)` at a call site outside the type's own class is a
  review flag.

### Annotations

- `@AggregateRoot` — marks the record at the root of an aggregate's
  composition graph.
- `@EntityIdentifier` — marks a *secondary* unique `EntityName`
  component within a data source. Do NOT annotate the canonical
  `name()` component (it is enforced automatically). Placed before the
  type: `@EntityIdentifier FooName otherName`.
- `@UniqueValue` — marks plain value components (`String`, `int`, enums)
  that must be unique.
- `@DomainService` — runtime marker for domain-boundary classes.
  No third-party meta-annotations; the `adapters/spring-runtime/` module
  discovers via classpath scanning (ADR-025). No Spring import in domain
  code.
- `@Incubating("reason")` — marks experimental APIs still under
  investigation.
- `@Nullable` — JSpecify (`org.jspecify.annotations.Nullable`) for
  nullable field documentation.

### `NamedValue<T>` and `NumericNamedValue`

`NamedValue<T>` is a typed, non-identifying single-value wrapper.
Concrete implementations are records with an `of(T)` factory. Contract:
`T value()`, `boolean isValid()`. Distinguished from `EntityName`
(no uniqueness claim, never a query parameter) and from `ValueObject`
(single value, not `Observable`). `@JsonCreator` IS required on the
`of(...)` factory.

`NumericNamedValue extends NamedValue<BigDecimal>` — for decimal domain
quantities. Adds `int scale()`, `RoundingMode roundingMode()`, and
`BigDecimal normalized()`. Raw `double`/`float` are prohibited for
decimal domain values. Integer quantities use `NamedValue<Integer>`.

### Jackson (api wire-form rules)

- Jackson 2.19.x natively deserializes records — **no `@JsonCreator` on
  entity / aggregate / value-object records.**
- JSON field names must match component names exactly.
- **`@JsonCreator` IS required on the `public static of(...)` factory
  of any non-record Jackson must deserialize:** `EntityName` subclasses,
  `NamedValue<T>` implementations.
- Enum values serialize by constant name (`"INORGANIC_SALT"`,
  `"ROOT_MASS_FLOW"`).
- Sealed hierarchies Jackson must round-trip need `@JsonTypeInfo` +
  `@JsonSubTypes` or an explicit discriminator property on the sealed
  interface.
- `EntityName.value()` and `EntityId.value()` carry `@JsonValue`,
  serializing as plain string/UUID. `NamedValue.value()` also carries
  `@JsonValue`.

---

## 4. Observability Framework — Complete `Constraints` API

`Constraints` is the fluent invariants builder. Every `Observable`
implements `Consumer<? extends Constraints> invariants()`. The graph
walker descends through `ConstraintCollection` nodes automatically.

### Method forms

**Direct-value form** — pass the component value directly. Use inside
`invariants()` on the record that owns the field.

**By-function form** — pass the parent object and a method reference.
Use when descending into a child from outside, or when the parent may be
null (safe null-short-circuit). Do not use when the direct-value form
suffices — the functional indirection adds no value when `this` can
never be null.

### Complete method list (canonical, as of the current kernel)

#### Descent constraints (walk child invariants)

| Method                               | Forms         | Behavior                                                                              |
|--------------------------------------|---------------|---------------------------------------------------------------------------------------|
| `namedEntity(e, name)`               | direct, by-fn | Non-null `Named`, descends into child's invariants                                    |
| `namedEntityOrNull(e, name)`         | direct, by-fn | Null permitted; descends only when present                                            |
| `aggregate(a, name)`                 | direct, by-fn | Non-null `Aggregate`, descends                                                        |
| `aggregateOrNull(a, name)`           | direct, by-fn | Null-tolerant `Aggregate`, descends when present                                      |
| `readModel(r, name)`                 | direct, by-fn | Non-null `ReadModel`, descends                                                        |
| `valueObject(v, name)`               | direct, by-fn | Non-null `ValueObject`, descends                                                      |
| `valueObjectOrNull(v, name)`         | direct, by-fn | Null permitted; descends only when present                                            |
| `valueObjectCollection(o, fn, name)` | by-fn only    | Non-null `Collection<V extends ValueObject>`, descends each element with indexed path |
| `behavioralCollection(b, name)`      | direct, by-fn | Non-null `BehavioralCollection`, descends                                             |
| `observable(o, name)`                | direct, by-fn | Any non-null `Observable`, descends                                                   |

#### Identifier constraints

| Method                          | Forms         | Behavior                                                                |
|---------------------------------|---------------|-------------------------------------------------------------------------|
| `entityName(e, name)`           | direct only   | Validates `EntityName` (kebab-case + subtype `maxLength()`)             |
| `entityNameOrNull(e, name)`     | direct only   | Null permitted; non-null must satisfy `EntityName.isValid()`            |
| `entityId(f, name)`             | direct only   | Validates `EntityId` (UUIDv7)                                           |
| `identifier(v, name)`           | direct only   | Polymorphic identifier — runtime dispatch on `EntityName` vs `EntityId` |
| `identifierSet(set, name)`      | direct only   | Polymorphic identifier set                                              |
| `entityNameCollection(c, name)` | direct only   | Collection of `EntityName`                                              |
| `entityNameSet(set, name)`      | direct, by-fn | `EntityNameSet` wrapper                                                 |
| `namedValue(v, name)`           | direct, by-fn | `NamedValue<?>` child                                                   |

#### Scalar constraints

| Method                           | Forms         | Behavior                                                 |
|----------------------------------|---------------|----------------------------------------------------------|
| `notNull(value, name)`           | direct, by-fn | General null check                                       |
| `notBlank(value, name)`          | direct, by-fn | Rejects null, empty, whitespace-only String              |
| `notEmpty(value, name)`          | direct, by-fn | Rejects null + empty Collection/Map/CharSequence         |
| `kebabFormat(value, name)`       | direct, by-fn | Asserts String matches the kebab-case slug regex         |
| `inRange(value, min, max, name)` | direct, by-fn | `Comparable` value in `[min, max]` inclusive             |
| `atLeast(value, min, name)`      | direct, by-fn | `Comparable` value `>= min`                              |
| `atMost(value, max, name)`       | direct, by-fn | `Comparable` value `<= max`                              |
| `isTrue(value, name)`            | direct only   | Generic boolean predicate — fires violation when `false` |

#### Control flow (not constraint types)

| Method                      | Form        | Behavior                                                                                       |
|-----------------------------|-------------|------------------------------------------------------------------------------------------------|
| `whenNotNull(value, block)` | direct only | Runs the block only when `value != null`. Constraints inside append to the same builder (flat) |

### Semantics

- **`valueObjectOrNull` vs `valueObject`.** Nullable VO fields must use
  `valueObjectOrNull` — the difference between "child is null" (valid)
  and "child is structurally invalid" (its own invariants). An
  empty-but-present VO must be rejected by the child's own invariants,
  not tolerated by the parent.
- **`namedEntityOrNull` vs `namedEntity`.** Same null-tolerance pattern
  for nullable `NamedEntity` / `Named` children. `null` passes;
  non-null triggers descent into the child's `invariants()`.
- **`valueObjectCollection` vs `notEmpty`.** `valueObjectCollection`
  asserts non-null + descends into each element. It does NOT assert
  non-empty. Pair with `notEmpty` when empty is illegal.
- **`whenNotNull` vs nullable constraints.** `whenNotNull` is pure
  control flow — no `Constraint` object is created. Use for conditional
  invariant blocks where multiple constraints depend on a nullable
  field's presence (e.g. monotonic-fill rules). `valueObjectOrNull` /
  `namedEntityOrNull` / `aggregateOrNull` are constraint types that
  null-gate a single descent.
- **`isTrue` for cross-field invariants.** Use for invariants expressed
  as a domain-specific predicate (e.g. `belongsToOrder(order)`). Compose
  with `whenNotNull` when the predicate depends on a nullable field.
- **Indexed paths** for collections: `windows.[0].onset`,
  `windows.[1].tail`. Matches the dotted-path convention in
  `ConstraintCollection`.

---

## 5. Data-Layer Ports

### `EntityRepository<NAME, ENTITY extends Named<NAME>>`

Pure vocabulary port — five public methods:

```java
Optional<ENTITY> getByName(NAME name);

List<ENTITY> getByEntityNameSet(Set<NAME> nameSet);

Page<ENTITY> getPage(PageRequest pageRequest);

void insert(ENTITY entity);

void update(ENTITY entity);
```

Identity at the port is the entity's `key()`. The type parameter is
still named `NAME` and the methods still say `getByName` — these verb
names are independent of the `key()`/`name()`/`id()` accessor split
and were deliberately kept for stability. Serves both `NamedEntity`
and `Entity` with one implementation via the `Named<NAME>` bound.

`AbstractEntityRepository` provides the template-method layer:
validation via `Observer.arguments(...)` in final public methods,
delegation to `doGetByName()` / `doInsert()` / etc. hooks.

### `EntityQuery<NAME, E extends Named<NAME>, EC extends BehavioralCollection<E>>`

Query-side port — three public methods:

```java
Optional<E> getByName(NAME name);

EC findByNameSet(Set<NAME> nameSet);

Page<E> findPage(PageRequest pageRequest);
```

Single-result: `Optional`. Multi-result: domain-specific
`BehavioralCollection` (not raw `List`).

`AbstractEntityQuery` provides the adapter base: holds repository +
observer, validates arguments, delegates.

### `EntityCommand<NAME, E extends Named<NAME>>`

Write-side port — two public methods:

```java
void insert(E entity);

void update(E entity);
```

Symmetric analogue of `EntityQuery`. Errors propagate unchanged
(fail-fast). `AbstractEntityCommand` provides the adapter base.

### `Page<T>` and `PageRequest`

`Page` carries `content`, `pageNumber`, `pageSize`, `pagesAheadKnown`,
`moreBeyondLookahead`. No total count — horizon info from lookahead.

`PageRequest` carries `pageNumber`, `pageSize`, `lookahead`. Constants:
`MAX_PAGE_SIZE = 1000`, `MAX_LOOKAHEAD = 10`,
`DEFAULT_CONSOLE_PAGE_SIZE = 25`. Factories: `of(...)`, `first(size)`,
`console(pageNumber)`.

### `FileName`

Record implementing `NamedValue<String>` — filename only, no path.
Methods: `path(directory)` to compose, `nameType()` for extension
(upper-cased), `baseName()` for name without extension. The directory
path is a stable convention of the bounded context, not stored.

---

## 6. api Namespace Patterns

A domain api groups its consumer surface under four coordinated
namespace types:

| Outer                           | Java type   | Visibility      | Nested                                                |
|---------------------------------|-------------|-----------------|-------------------------------------------------------|
| `<DomainNoun>Repository`        | `class`     | package-private | `<EntitySubject>Repository` (`protected interface`)   |
| `<DomainNoun>Query`             | `interface` | public          | `<EntitySubject>Query`, `AggregateQuery`, `ViewQuery` |
| `<DomainNoun>Command`           | `interface` | public          | `<EntitySubject>Command`                              |
| `<DomainNoun>EntityCollections` | `interface` | public          | `<EntitySubject>Collection` (`final class`)           |

`EntitySubject` drops the domain prefix: `InsectSpecies` → `Species`,
`InsectImage` → `Image`. The outer namespace carries the prefix.

**Why class for repository, interface for query/command.** Nested types
inside an interface are implicitly `public static` — visibility cannot be
restricted. A class keeps repository contracts hidden (`protected` =
package-private + subclass access). Queries and commands *want* their
nested types public.

**Repository contracts in the api are package-private.** They are part
of the api module but not part of the api consumer surface. Cross-domain
consumers go through `<DomainNoun>Query`.

**N=1 collapse.** When a package has exactly one entity, skip the
namespace: top-level package-private `<Entity>Repository` interface +
top-level public `<Entity>Query` interface.

**Aggregate value-object nesting.** A `ValueObject` exclusively
reachable through a single `Entity` / `Aggregate` nests inside that
entity's file as a `static record`. Promote back to top-level when it
gains a standalone lifecycle, is referenced cross-domain by name, or
appears in more than one entity's graph.

### Query return-type contract

- Return types on api `Query` methods are `Optional<Entity>`,
  `Optional<Aggregate>`, `Optional<ReadModel>`,
  or a `BehavioralCollection` subclass.
- **Raw `List<T>` on a public query method is a review flag.**
- `Optional` carries "found / not found"; `BehavioralCollection` carries
  multi-result with descent into each element's invariants.

### BehavioralCollection rules

- `final class`, not a record.
- Lives in the owning `<domain>-api`, declared inside
  `<DomainNoun>EntityCollections`.
- Constructor is package-private; `public static of(...)` and
  `public static empty()` are the only external instantiation paths.
- `List.copyOf` defensive copy inherited from base class constructor.
- Domain-specific filtering methods return new instances via the
  package-private constructor.

### BehavioralMap rules

- Extends `BehavioralCollection<V>` with a keyed secondary index.
- Two construction modes: element-derived grouping (key extractor) or
  pre-computed grouping (caller supplies map).
- `elementsForKey(K)` is `protected` — domain subclasses wrap it in a
  typed method returning their own collection type.
- `hasKey(K)` and `keys()` are public.
- Lives alongside the domain's other collections.

---

## 7. Observer Pattern

`Observer` is the entry point for the observability framework:

```java
// Construction (one per class, stored as field)
Observer observer = Observer.forClass(MyClass.class);

// Argument validation — throws on violation
observer.

arguments("methodName",i ->i
        .

entityName(name, "name")
).

throwWhenInvalid();

// Method-body observation — metrics only (producer rule)
observer.

namedEntity(entity, "label").

observe(Level.WARN);

// Method-scoped observer for tests
MethodObserver mo = observer.forMethod("testMethod");
mo.

namedEntity(entity, "entity");
```

### Producer/consumer rule (ADR-017)

- **Producer** observing its own output: `.observe(Level)` (metrics
  only, no throw).
- **Consumer** observing received state: `.throwWhenInvalid()`.
- **Argument validation** always throws — boundary contract.

### `InvariantObservation`

Result of constraint graph walk. Key methods:

- `violations()` — failing constraints
- `violationNames()` — set of failing constraint dotted paths
- `throwWhenInvalid()` — emits error metrics, throws
  `InvariantViolationException` if any violation
- `observe(Level)` — emits metrics at given level, no throw

---

## 8. Shared Kernel Types

### `Description` (field-notes)

Four-level Durrell description — preschool, elementary, secondary,
university. All four levels non-null, all simultaneously true. Every
domain entity should carry one unless deliberately omitted.

```java
public record Description(
        String preschool, String elementary,
        String secondary, String university
) implements ValueObject
```

### `CommonName` (field-notes)

Locale-tagged vernacular name. Carried as `Set<CommonName>` on catalog
entities for search findability.

```java
public record CommonName(String label, Locale locale) implements ValueObject
```

### Taxonomy types (taxonomy kernel)

- `TaxonomicOrder`, `TaxonomicFamily`, `TaxonomicGenus`,
  `TaxonomicSpecies`, `TaxonomicSubspecies` — `NamedValue<String>`
  implementations wrapping proper-cased Linnaean epithets.
- `LinnaeanOrder`, `LinnaeanFamily`, `LinnaeanGenus`, `LinnaeanSpecies`,
  `LinnaeanSubspecies` — interfaces that rank entities implement, each
  providing a `*Slug()` method for deriving the slug `EntityName`.
- `LinealRank` — enum (`ORDER`, `FAMILY`, `GENUS`, `SPECIES`,
  `SUBSPECIES`) for polymorphic rank identification.
- `TaxonomicClassification` — full classification record with
  `binomialName()` and `isSpeciesLevel()`.

### `Clade` (clades kernel)

Sealed interface with one stateless record permit per recognised clade.
Each permit carries slug, display name, Durrell `Description`, and
`Optional<Clade> parent`. Organism rank entities carry
`@Nullable Clade placedIn` for evolutionary placement. Trait resolution
via pure functions (pattern-matching `switch` over sealed permits) in
the consuming domain.

### `HabitatProfile` (habitat kernel)

Structured habitat classification: zones, moisture regime, light regime,
vertical layers. Shared across organism domains.

### `DomainId` (catalog kernel)

Open interface — each domain ships its own `DomainId` subtype from its
`*-api` module. The catalog kernel knows no domain names.

---

## 9. Resilience Facade

Domain `*-core` code references only the facade
(`com.naturalist.resilience`), never Resilience4j directly. Production
implementation in `adapters/resilience-resilience4j/`.

```java
public interface Resilience {
  Retry retry(String name);

  Timeout timeout(String name);

  CircuitBreaker circuitBreaker(String name);

  Bulkhead bulkhead(String name);

  static Resilience noOp();  // for unit tests
}
```

Each primitive (`Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead`) has:
`<T> T execute(Supplier<T>)` and `void execute(Runnable)`.

`@Resilient(name = "catalog.fanout")` applies all configured primitives.
`@ResilienceExempt(reason = "...")` opts out with justification.
`ResilienceConfig` (sealed, 4 permits) validates configuration at
composition-root startup.

---

## 10. Anti-Patterns — Specific Things Previous Chat Sessions Got Wrong

**Do not invent package paths.**

- There is no `com.naturalist.identifiers` package.
- **When in doubt, say "I don't know the package path for X — please
  confirm."**

**Do not invent methods on `Constraints`.**

- Check the table in section 4 before calling a method. If it's not in the
  table, it doesn't exist.

**Do not call `UUID.randomUUID()` in api code.**

- Use `EntityId.newUUID()` only.

**Do not nest an Entity or Aggregate inside a ValueObject.**

- A `ValueObject` cannot contain `Entity` or `Aggregate` members,
  cannot uniquely identify an entity, must be a cohesive
  ubiquitous-language concept, and its members must have collective
  meaning. The moment a nested type becomes an Entity, the wrapper
  must reclassify or be removed.

**Do not add `@JsonCreator` to records.**

- Jackson 2.19 handles records natively. `@JsonCreator` belongs on the
  `public static of(...)` factory of `EntityName` subclasses and
  `NamedValue<T>` implementations only.

**Do not use raw `List<T>` at api query method signatures.**

- Return `Optional<Entity>`, `Optional<Aggregate>`, `Optional<ReadModel>`,
  or a `BehavioralCollection` subclass.

**Do not declare repository implementations in the api.**

- The api carries `<EntitySubject>Repository` *contracts* only,
  package-private. Implementations live outside the api.

**Do not confuse `ReadModel` with `Aggregate`.**

- If the type owns nothing and is never mutated as a unit, it is a
  `ReadModel`, not an `Aggregate`.

**Do not put `guilds` / `beneficial` on entity records directly when
they belong on a separate functional-role entity.**

- Functional ecology (guild assignments, beneficial flag) may be a
  cross-rank concern — check the domain briefing for whether it's on
  the entity or on a separate `FunctionalRole` entity.

---

## 11. Glossary

- **Module** (Maven) vs. **package** (Java). The `identifiers` module
  contains multiple packages (`com.naturalist.insects`,
  `com.naturalist.plants`, etc.). "`identifiers`" alone refers to the
  Maven artifact, not a Java package.
- **api** — the `<domain>-api` Maven module: a domain's records,
  identifiers, public query / command / collection namespace, and
  package-private repository contracts. The only module a cross-domain
  consumer ever touches by import.
- **Sub-context** — a Java package within a domain api. Package-private
  visibility is the enforcement mechanism. Not a separate bounded
  context in the strict DDD sense.
- **`Named<KEY>`** — common supertype of `NamedEntity` and `Entity`.
  Declares `KEY key()` as the data-layer port accessor. Each branch
  declares its own semantic accessor (`name()` / `id()`) and defaults
  `key()` to it. Enables shared data-layer infrastructure.
- **NamedEntity branch** vs. **Entity branch.** Two parallel identity
  disciplines. `NamedEntity` records carry a `name` component (slug);
  `Entity` records carry an `id` component (UUIDv7). Choose per entity
  based on "does this thing have a natural key?"
- **Aggregate root** — the `NamedEntity` / `Entity` at the top of an
  aggregate's composition graph, marked `@AggregateRoot`.
- **ReadModel** — read-side projection assembled from persisted parts.
  Not a consistency boundary, not persisted.
- **Invariants** — structural rules the domain enforces always,
  declared via `Constraints` inside `invariants()`. Walked by the
  observability framework on every observation.
- **Observable** — anything carrying invariants. All six identity
  interfaces extend it; `BehavioralCollection` extends it directly.
- **NamedValue<T>** — typed single-value wrapper with domain meaning.
  Not `Observable`, not identity. `@JsonValue` on `value()`.
