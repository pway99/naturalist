# Naturalist — Framework Reference Briefing

**Purpose.** Companion to `framework-core.md` — upload this file
alongside the core briefing when a chat session needs the complete
`Constraints` API, data-layer port signatures, Observer ceremony,
Resilience facade, or `BehavioralMap` details. Most Java modeling
sessions do **not** need this file.

**Briefing date.** 2026-06-14. If a type or method listed here does not
match what you observe in code, trust the code.

---

## 1. Complete `Constraints` API

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

### Descent constraints (walk child invariants)

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

### Identifier constraints

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

### Scalar constraints

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

### Control flow (not constraint types)

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

## 2. BehavioralMap

`BehavioralMap<K, V>` extends `BehavioralCollection<V>` with a keyed
secondary index.

- Two construction modes: element-derived grouping (key extractor) or
  pre-computed grouping (caller supplies map).
- `elementsForKey(K)` is `protected` — domain subclasses wrap it in a
  typed method returning their own collection type.
- `hasKey(K)` and `keys()` are public.
- Lives alongside the domain's other collections.

---

## 3. Data-Layer Ports

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

## 4. Observer Pattern

`Observer` is the entry point for the observability framework:

```java
// Construction (one per class, stored as field)
Observer observer = Observer.forClass(MyClass.class);

// Argument validation — throws on violation
observer.arguments("methodName", i -> i
        .entityName(name, "name")
).throwWhenInvalid();

// Method-body observation — metrics only (producer rule)
observer.namedEntity(entity, "label").observe(Level.WARN);

// Method-scoped observer for tests
MethodObserver mo = observer.forMethod("testMethod");
mo.namedEntity(entity, "entity");
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

## 5. Resilience Facade

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
