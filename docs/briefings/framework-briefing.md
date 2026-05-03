# Naturalist — Framework Briefing

**Purpose.** Shared structural briefing — paired with **one or more**
domain briefings (e.g. `insects-domain.md`, `chemistry-api.md`,
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
- Identity model
- Record conventions
- Annotations the api uses
- Jackson rules that affect the api wire form
- The complete `Constraints` API for `invariants()`
- The api namespace pattern (`Query`, `EntityCollections`, `Repository`
  contract surface)
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

---

## 1. Module Layout — api side only

```
kernels/
  framework/        NamedEntity, Entity, Aggregate, ValueObject,
                    BehavioralCollection, Observable, Constraints,
                    EntityName, EntityId, EntityNameSet, NamedValue,
                    AggregateRoot (annotation)
  field-notes/      Description (four-level Durrell description)
  taxonomy/         TaxonomicClassification, TaxonomicOrder, TaxonomicFamily
  habitat/          HabitatProfile (structured habitat classification)
  biogeography/     Bioregion (cross-domain native-range vocabulary)
  measurements/     Unit value objects (temperature, mass, volume, etc.)

domains/
  identifiers/      EntityName + EntityId subclasses ONLY
  <domain>/<domain>-api    ← what an api author or consumer touches
```

Other modules exist (`<domain>-core` adapters, `<domain>-repository-*`,
`<domain>-console`, `framework-test`, `catalog`) but the api never
depends on or knows about them.

### api-side dependency rules

- `<domain>-api` depends only on `framework`, `identifiers`,
  `field-notes`, and (organism only) `taxonomy`, `habitat`,
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

**Every domain class implements exactly one of four interfaces from
`kernels/framework`:**

| Interface                              | Identity                      | Usage                                                   |
|----------------------------------------|-------------------------------|---------------------------------------------------------|
| `NamedEntity<NAME extends EntityName>` | Natural-key slug              | Catalog entries, stable cross-domain references         |
| `Entity<ID extends EntityId>`          | Surrogate UUIDv7              | Events, observations, relationships with no natural key |
| `Aggregate`                            | Via aggregate root's identity | Consistency boundaries                                  |
| `ValueObject`                          | None — equality by value      | Immutable components                                    |

Plus `BehavioralCollection<T extends Observable>` — `final class` (not a
record) for multi-result query return types.

All four extend `Observable` and declare `invariants()`.

### Naming rules

- `EntityName` subclasses are slug-identified: regex
  `^[a-z0-9]+(-[a-z0-9]+)*$`, kebab-case, never null.
- `EntityId` subclasses are UUIDv7, validated `version() == 7` at
  boundary, generated at record construction via the kernel's generator.
- **Do not call `UUID.randomUUID()` anywhere in api code.** Use the
  kernel generator only.
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

Canonical locations:

| Type                | Actual package                      | Module        |
|---------------------|-------------------------------------|---------------|
| `InsectSpeciesName` | `com.naturalist.insects`            | `identifiers` |
| `InsectImageId`     | `com.naturalist.insects`            | `identifiers` |
| `PlantName`         | `com.naturalist.plants`             | `identifiers` |
| `CultivarName`      | `com.naturalist.plants.cultivar`    | `identifiers` |
| `CompoundName`      | `com.naturalist.chemistry.compound` | `identifiers` |
| `ElementName`       | `com.naturalist.chemistry.element`  | `identifiers` |
| `ZoneName`          | `com.naturalist.zone`               | `identifiers` |
| `SoilProfileName`   | `com.naturalist.soil`               | `identifiers` |
| `NaturalistName`    | `com.naturalist.naturalist`         | `identifiers` |

**There is no `com.naturalist.identifiers` package.** A class at that
path does not exist. The *module* is `identifiers`; the *package* tracks
the home domain.

### Framework types — canonical packages

| Type                                                                                                                                                                | Package                                    | Module        |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|---------------|
| `NamedEntity`, `Entity`, `Aggregate`, `ValueObject`, `AggregateRoot`, `Observable`, `EntityName`, `EntityId`, `EntityNameSet`, `NamedValue`, `BehavioralCollection` | `com.naturalist.ddd`                       | `framework`   |
| `Constraints`, `Constraint`, `ConstraintCollection`, `Observer`, `MethodObserver`, `InvariantObservation`, `Metric`                                                 | `com.naturalist.observability`             | `framework`   |
| Constraint records (`NotNullConstraint`, `NotBlankConstraint`, etc.)                                                                                                | `com.naturalist.observability.constraints` | `framework`   |
| `Description`                                                                                                                                                       | `com.naturalist.fieldnotes`                | `field-notes` |
| `TaxonomicClassification`, `TaxonomicOrder`, `TaxonomicFamily`                                                                                                      | `com.naturalist.taxonomy`                  | `taxonomy`    |
| `HabitatProfile`                                                                                                                                                    | `com.naturalist.habitat`                   | `habitat`     |

---

## 3. Record Conventions

- `Entity`, `Aggregate`, `NamedEntity`, `ValueObject` are **Java
  records**.
- `BehavioralCollection` is the single exception: `final class` (records
  cannot have a package-private constructor distinct from public
  factories, which the collection contract requires).
- No Lombok. Java 17+ — records, sealed interfaces, pattern matching,
  switch expressions.

### Accessor rules

- Accessor names match component names exactly: `name()`, `someField()`
  — never `getName()`.
- Boolean components use plain names: `active`, `beneficial`. Predicate
  methods use `is*` prefix only when they are behavior methods, not
  component accessors.
- Every mutable field on a concrete `NamedEntity` record needs an
  explicit `with*` method; `name()` is immutable.
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
- `@Nullable` — JSpecify (`org.jspecify.annotations.Nullable`) for
  nullable field documentation.

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
null (safe null-short-circuit).

### Complete method list (canonical, as of the current kernel)

| Method                               | Form   | Behavior                                                                                                     |
|--------------------------------------|--------|--------------------------------------------------------------------------------------------------------------|
| `namedEntity(e, name)`               | direct | Non-null NamedEntity, descends into child's invariants                                                       |
| `namedEntity(o, fn, name)`           | by-fn  | Same, via parent                                                                                             |
| `valueObject(v, name)`               | direct | Non-null ValueObject, descends                                                                               |
| `valueObject(o, fn, name)`           | by-fn  | Same, via parent                                                                                             |
| `valueObjectOrNull(o, fn, name)`     | by-fn  | Null permitted; descends only when present                                                                   |
| `valueObjectCollection(o, fn, name)` | by-fn  | Non-null `Collection<V extends ValueObject>`, descends into each element with indexed path `[0]`, `[1]`, ... |
| `observable(o, fn, name)`            | by-fn  | Any non-null Observable (e.g. BehavioralCollection)                                                          |
| `entityName(e, name)`                | direct | Validates EntityName (kebab-case + subtype `maxLength()`)                                                    |
| `entityNameOrNull(e, name)`          | direct | Null permitted; non-null must satisfy `EntityName.isValid()`                                                 |
| `entityId(f, name)`                  | direct | Validates EntityId (UUIDv7)                                                                                  |
| `identifier(v, name)`                | direct | Polymorphic identifier (runtime dispatch on EntityName vs EntityId)                                          |
| `identifierSet(set, name)`           | direct | Polymorphic identifier set                                                                                   |
| `entityNameCollection(c, name)`      | direct | Collection of EntityName                                                                                     |
| `entityNameSet(set, name)`           | direct | EntityNameSet wrapper                                                                                        |
| `entityNameSet(o, fn, name)`         | by-fn  | Same via parent                                                                                              |
| `namedValue(o, fn, name)`            | by-fn  | NamedValue child                                                                                             |
| `notBlank(value, name)`              | direct | Rejects null, empty, whitespace-only String                                                                  |
| `notBlank(t, fn, name)`              | by-fn  | Same via parent                                                                                              |
| `kebabFormat(value, name)`           | direct | Asserts String matches the project kebab-case slug regex                                                     |
| `kebabFormat(t, fn, name)`           | by-fn  | Same via parent                                                                                              |
| `notNull(value, name)`               | direct | General null check                                                                                           |
| `notNull(t, fn, name)`               | by-fn  | Same via parent                                                                                              |
| `notEmpty(t, fn, name)`              | by-fn  | Rejects null + empty Collection/Map/CharSequence                                                             |
| `inRange(value, min, max, name)`     | direct | `Comparable` value must satisfy `min <= value <= max`                                                        |
| `inRange(t, fn, min, max, name)`     | by-fn  | Same via parent                                                                                              |
| `atLeast(value, min, name)`          | direct | `Comparable` value must satisfy `value >= min`                                                               |
| `atLeast(t, fn, min, name)`          | by-fn  | Same via parent                                                                                              |
| `atMost(value, max, name)`           | direct | `Comparable` value must satisfy `value <= max`                                                               |
| `atMost(t, fn, max, name)`           | by-fn  | Same via parent                                                                                              |

### What does NOT exist (do not call)

- `namedEntityOrNull(...)` — **not in the kernel.** For nullable
  `NamedEntity` descent, use a block lambda with a null guard:
  ```java
  Consumer<Constraints> body = i -> {
      i.entityName(name, "name")
       .valueObject(taxonomy, "taxonomy");
      if (stage != null) i.namedEntity(this, Parent::stage, "stage");
  };
  return body;
  ```
  (`entityNameOrNull` and `valueObjectOrNull` DO exist — see the table —
  the gap is specifically on the `NamedEntity` descent path.)
- `observableOrNull(...)` — not in the kernel.
- `notEmpty(value, name)` (direct form) — only by-function form exists.
- `notBlank(t, fn, name)` for non-String — `notBlank` is String-only.
- Raw collection-set-descent beyond `valueObjectCollection` /
  `entityNameCollection` / `entityNameSet`.

### Semantics

- **`valueObjectOrNull` vs `valueObject`.** Nullable VO fields must use
  `valueObjectOrNull` — the difference between "child is null" (valid)
  and "child is structurally invalid" (its own invariants). An
  empty-but-present VO must be rejected by the child's own invariants,
  not tolerated by the parent.
- **`valueObjectCollection` vs `notEmpty`.** `valueObjectCollection`
  asserts non-null + descends into each element. It does NOT assert
  non-empty. Pair with `notEmpty` when empty is illegal. Keeps policies
  composable.
- **Indexed paths** for collections: `windows.[0].onset`,
  `windows.[1].tail`. Matches the dotted-path convention in
  `ConstraintCollection`.

---

## 5. api Namespace Patterns

A domain api groups its consumer surface under three coordinated
namespace types:

| Outer                           | Java type   | Visibility      | Nested                                                  |
|---------------------------------|-------------|-----------------|---------------------------------------------------------|
| `<DomainNoun>Repository`        | `class`     | package-private | `<EntitySubject>Repository` (`protected interface`)     |
| `<DomainNoun>Query`             | `interface` | public          | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public          | `<EntitySubject>Collection` (`final class`)             |

`EntitySubject` drops the domain prefix: `InsectSpecies` → `Species`,
`InsectImage` → `Image`. The outer namespace carries the prefix.

**Why class for repository, interface for query.** Nested types inside
an interface are implicitly `public static` — visibility cannot be
restricted. A class keeps repository contracts hidden (`protected` =
package-private + subclass access). Queries *want* their nested types
public.

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
  `Optional<Aggregate>`, or a `BehavioralCollection` subclass.
- **Raw `List<T>` on a public query method is a review flag.**
- `Optional` carries "found / not found"; `BehavioralCollection` carries
  multi-result with descent into each element's invariants.

### BehavioralCollection rules

- `final class`, not a record.
- Lives in the owning `<domain>-api`, declared inside
  `<DomainNoun>EntityCollections`.
- Constructor is package-private; `public static of(...)` and
  `public static empty()` are the only external instantiation paths.
- Domain-specific filtering methods return new instances via the
  package-private constructor.

---

## 6. Anti-Patterns — Specific Things Previous Chat Sessions Got Wrong

**Do not invent package paths.**

- ❌ `com.naturalist.identifiers.LifeStageKind`
- ✅ `com.naturalist.insects.lifestage.LifeStageKind` (in insects-api) or
  `com.naturalist.insects.LifeStageKind` (in identifiers module)
- **When in doubt, say "I don't know the package path for X — please
  confirm."**

**Do not invent methods on `Constraints`.**

- Check the table in §4 before calling a method. If it's not in the
  table, it doesn't exist.
- `namedEntityOrNull` and `observableOrNull` are still NOT in the
  kernel — use the `if (x != null) i.namedEntity(...)` ladder shown in
  §4. `entityNameOrNull` and `valueObjectOrNull` DO exist.

**Do not call `UUID.randomUUID()` in api code.**

- Use the kernel's UUIDv7 generator only.

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

- Return `Optional<Entity>`, `Optional<Aggregate>`, or a
  `BehavioralCollection` subclass.

**Do not declare repository implementations in the api.**

- The api carries `<EntitySubject>Repository` *contracts* only,
  package-private. Implementations live outside the api and are not
  part of this briefing's scope.

**Do not skip the "why" in feedback.**

- Reasons behind conventions (prior incidents, architectural goals)
  matter for judgment calls at edges. The "why" is what lets chat
  extrapolate correctly.

---

## 7. Glossary

- **Module** (Maven) vs. **package** (Java). The `identifiers` module
  contains multiple packages (`com.naturalist.insects`,
  `com.naturalist.plants`, etc.). "`identifiers`" alone refers to the
  Maven artifact, not a Java package — there is no
  `com.naturalist.identifiers`.
- **api** — the `<domain>-api` Maven module: a domain's records,
  identifiers, public query / collection namespace, and package-private
  repository contracts. The only module a cross-domain consumer ever
  touches by import. This briefing is scoped to the api side; adapter
  modules are out of scope.
- **Sub-context** — a Java package within a domain api. Package-private
  visibility is the enforcement mechanism. Not a separate bounded
  context in the strict DDD sense.
- **NamedEntity branch** vs. **Entity branch.** Two parallel identity
  disciplines. Choose per entity based on "does this thing have a
  natural key?"
- **Aggregate root** — the `NamedEntity` / `Entity` at the top of an
  aggregate's composition graph. The aggregate's identity is the root's
  identity.
- **Invariants** — structural rules the domain enforces always,
  declared via `Constraints` inside `invariants()`. Walked by the
  Observability framework on every observation.
- **Observable** — anything carrying invariants. All four identity
  interfaces extend it; `BehavioralCollection` extends it directly.
