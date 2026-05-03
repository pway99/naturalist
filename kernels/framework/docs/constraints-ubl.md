# `Constraints` — Chat Briefing (Kernel Framework)

**Purpose.** The complete, observed-from-source surface of
`com.naturalist.observability.Constraints` — the fluent invariants builder every
`Observable` declares against. Sized for upload to a chat Claude session that needs
to write or review `invariants()` bodies without inventing methods.

**Primary rule.** If a method is not in §3 below, it does not exist on `Constraints`.
Past sessions invented `namedEntityOrNull`, `observableOrNull`, and a direct-form
`notEmpty(value, name)`. None exist. When in doubt, say so.

---

## 1. What `Constraints` Is

`Constraints` is a mutable accumulator passed into the `Consumer<? extends Constraints>`
returned by `Observable.invariants()`. Every constraint method registers a
`Constraint<?>` (or a `ConstraintCollection` for descent), and returns `this` for
fluent chaining. The graph walker — `Observer.flatten(...)` — descends through every
`ConstraintCollection` node automatically and qualifies each constraint name with the
fully-qualified dotted path through the graph.

Source: `kernels/framework/src/main/java/com/naturalist/observability/Constraints.java`.

---

## 2. The Two Forms

Every constraint method exists in one of two shapes.

**Direct-value form.** Pass the component value directly. Use inside `invariants()` on
the record that owns the field — `this` cannot be null while its own `invariants()` is
being walked, so the safe-null indirection adds no value:

```java
i.entityName(name, "name")
 .notBlank(commonName, "commonName")
 .notNull(value, "value");
```

**By-function form.** Pass the parent object and a method reference. The function is
short-circuited when the parent is null. Use this form when:

1. Descending into a child `Observable` (`namedEntity`, `valueObject`,
   `valueObjectOrNull`, `valueObjectCollection`, `observable`).
2. Walking a `NamedValue<?>` child.
3. Asserting a primitive/value field on a parent that may itself be null
   (rare inside `invariants()`; common inside argument validation).

```java
i.valueObject(this, Compound::compoundInfo, "compoundInfo")
 .valueObjectOrNull(this, Compound::safety, "safety")
 .namedValue(this, SolubilityProfile::gramsPerLiterAt20C, "gramsPerLiterAt20C");
```

**Do not use the by-function form when the direct-value form suffices.** The functional
indirection adds no value when `this` cannot be null.

---

## 3. The Complete Method List

Every public method on `Constraints` as of source-of-truth. No method outside this list
exists. Each row identifies the underlying `Constraint` record so you can read the
exact `isValid()` semantics in `kernels/framework/src/main/java/com/naturalist/observability/constraints/`.

### NamedEntity / Observable descent

| Method                                                                  | Form           | Backing record                    | Behaviour                                                                                                                                                                  |
|-------------------------------------------------------------------------|----------------|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `namedEntity(E entity, String name)`                                    | direct         | `ObservableConstraint`            | Non-null `Named<?>`. Acts as leaf (presence) **and** `ConstraintCollection` (descend).                                                                                     |
| `namedEntity(O o, Function<O,E> fn, String name)`                       | by-fn          | `ObservableConstraint`            | Same, via parent. Null parent → null value → fails.                                                                                                                        |
| `valueObject(V vo, String name)`                                        | direct         | `ObservableConstraint`            | Non-null `ValueObject`. Descends into child invariants.                                                                                                                    |
| `valueObject(O o, Function<O,V> fn, String name)`                       | by-fn          | `ObservableConstraint`            | Same, via parent.                                                                                                                                                          |
| `valueObjectOrNull(O o, Function<O,V> fn, String name)`                 | by-fn **only** | `ValueObjectOrNullConstraint`     | Null permitted. Descends only when present. `isValid()` is hard-coded `true` — meaningfulness is the **child's** responsibility.                                           |
| `valueObjectCollection(O o, Function<O,Collection<V>> fn, String name)` | by-fn **only** | `ValueObjectCollectionConstraint` | Non-null `Collection<V extends ValueObject>`. Each element walked under indexed path `[0]`, `[1]`, … . Empty collection passes — pair with `notEmpty` if empty is illegal. |
| `observable(O o, Function<O,V> fn, String name)`                        | by-fn **only** | `ObservableConstraint`            | Any non-null `Observable` — typically a `BehavioralCollection` held by an `Aggregate`.                                                                                     |

### Identifier validation

| Method                                                             | Form   | Backing record                                         | Behaviour                                                                                                                                                                          |
|--------------------------------------------------------------------|--------|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `entityName(E e, String name)`                                     | direct | `EntityNameConstraints.EntityNameConstraint`           | `EntityName.isValid(value)` — non-null + slug regex.                                                                                                                               |
| `entityId(F f, String name)`                                       | direct | `EntityIdConstraints.EntityIdConstraint`               | `EntityId.isValid(value)` — non-null + UUID `version()==7`.                                                                                                                        |
| `identifier(V value, String name)`                                 | direct | `IdentifierConstraints.IdentifierConstraint`           | Polymorphic — runtime dispatch on `EntityName` vs `EntityId`. Used at boundaries (`AbstractEntityQuery`, `AbstractEntityRepository`) where the branch isn't fixed at compile time. |
| `identifierSet(Set<V> value, String name)`                         | direct | `IdentifierConstraints.IdentifierSetConstraint`        | Null set fails. Each element validated by runtime type.                                                                                                                            |
| `entityNameCollection(Collection<E> names, String name)`           | direct | `EntityNameConstraints.EntityNameCollectionConstraint` | Null collection fails; null or invalid element fails.                                                                                                                              |
| `entityNameSet(EntityNameSet<E> set, String name)`                 | direct | `EntityNameConstraints.EntityNameSetConstraint`        | Null set fails; every element must be non-null and valid.                                                                                                                          |
| `entityNameSet(O o, Function<O,EntityNameSet<E>> fn, String name)` | by-fn  | `EntityNameConstraints.EntityNameSetConstraint`        | Same via parent. Null parent → null set → fails.                                                                                                                                   |

### NamedValue

| Method                                                                 | Form           | Backing record                               | Behaviour                                                                                                                                     |
|------------------------------------------------------------------------|----------------|----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `namedValue(O o, Function<O,V extends NamedValue<?>> fn, String name)` | by-fn **only** | `NamedValueConstraints.NamedValueConstraint` | Delegates to `NamedValue.isValid()`. The `NamedValue` is **not** an `Observable` — it has no `invariants()` to descend; this is a leaf check. |

### Primitive / scalar checks

| Method                                              | Form           | Backing record       | Behaviour                                                                                                                                                   |
|-----------------------------------------------------|----------------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `notNull(R value, String name)`                     | direct         | `NotNullConstraint`  | General null check.                                                                                                                                         |
| `notNull(T t, Function<T,R> fn, String name)`       | by-fn          | `NotNullConstraint`  | Same via parent.                                                                                                                                            |
| `notBlank(String value, String name)`               | direct         | `NotBlankConstraint` | `StringUtils.isNotBlank` — rejects null, empty, whitespace-only.                                                                                            |
| `notBlank(T t, Function<T,String> fn, String name)` | by-fn          | `NotBlankConstraint` | Same via parent. **`notBlank` is String-only** — there is no `notBlank` for non-String types.                                                               |
| `notEmpty(T t, Function<T,R> fn, String name)`      | **by-fn only** | `NotEmptyConstraint` | Rejects null + empty `Collection`, `Map`, `CharSequence`. Non-collection non-null values pass. **No direct-value `notEmpty(value, name)` overload exists.** |

### Terminal

| Method        | Returns                                                                                    |
|---------------|--------------------------------------------------------------------------------------------|
| `collected()` | `List<Constraint<?>>` — unmodifiable; the accumulated raw constraints (not yet flattened). |

---

## 4. What Does NOT Exist (Common Inventions)

Past sessions have called these. None exist. Do not add them without the user's
explicit go-ahead — they're absent by design.

- ❌ `namedEntityOrNull(...)` — for nullable `NamedEntity` descent, write a block
  lambda with a null guard:
  ```java
  return i -> {
      i.entityName(name, "name")
       .valueObject(this, Parent::taxonomy, "taxonomy");
      if (stage != null) i.namedEntity(this, Parent::stage, "stage");
  };
  ```
- ❌ `observableOrNull(...)` — same pattern.
- ❌ `notEmpty(value, name)` (direct-value form) — only the by-function form exists.
- ❌ `notBlank(t, fn, name)` for non-String types — `notBlank` is String-only.
- ❌ Direct-value form for `valueObjectOrNull`, `valueObjectCollection`, `observable`,
  `namedValue` — these are by-function only by design (the function is the descent
  hook; without a parent there's nothing to descend from).
- ❌ Raw collection-set descent beyond `valueObjectCollection`,
  `entityNameCollection`, and `entityNameSet`.

---

## 5. Semantic Distinctions Worth Internalising

### `valueObject` vs `valueObjectOrNull`

`valueObject` requires a non-null reference (its `isValid()` returns false on null).
`valueObjectOrNull` permits null and its `isValid()` is hard-coded `true` — the
nullable child either is absent (valid) or is descended into (the child's invariants
decide). **An empty-but-present value object must be rejected by the child's own
`invariants()`, not tolerated by the parent.** A nullable value object whose every
component is null should be illegal at its own invariant level; the parent then
carries `null` instead of a non-null-but-empty reference.

### `valueObjectCollection` vs `notEmpty`

`valueObjectCollection` asserts non-null + descends into each element. It does **not**
assert non-empty — empty is valid. Pair with `notEmpty(this, Parent::field, "field")`
when empty is illegal. Keeping the two concerns separate keeps policies composable.

### `namedEntity` vs `valueObject`

Both back to `ObservableConstraint`. The split is documentation-of-intent at the call
site: `namedEntity` for `Named<?>` children (signals identity-bearing relationship);
`valueObject` for `ValueObject` children (signals owned attribute). Reviewers and
greppability benefit; runtime behaviour is identical.

### `namedValue` vs `notNull`

`namedValue` delegates to `NamedValue.isValid()` — the value type's own validity
contract (non-null + scale/sign/etc.). `notNull` is a presence check only. Use
`namedValue` for any `NamedValue<?>` field; the value object's `isValid()` is the
authoritative rule.

### Indexed paths in collections

`valueObjectCollection` qualifies each element's invariants with `[0]`, `[1]`, …,
producing names like `windows.[0].onset` and `windows.[1].tail`. This matches the
dotted-path convention `ConstraintCollection` uses for nesting; failures stay
attributable to a specific element.

---

## 6. The Larger Loop — Where `Constraints` Plugs In

```
Observable.invariants() ──► Consumer<? extends Constraints>
          ▲                                │
          │                                ▼
   Observer.flatten()  ◄── walks ConstraintCollection nodes
          │                                │
          ▼                                ▼
   InvariantObservation   (emits metrics, optionally throws)
```

Three callers of the constraint graph, each with its own terminal operation contract:

1. **`Observer.arguments(methodName, i -> ...)`** — argument validation. Builds an
   `InvariantObservation`; the caller invokes `.throwWhenInvalid()`. This is the
   producer's boundary contract — argument validation **always throws**.
2. **`Observer.namedEntity(e, label)` / `Observer.observable(o, label)`** — method-body
   observation of state the method has constructed or received. Terminal-operation
   choice follows the producer/consumer rule (ADR-017): a producer observing its own
   output calls `.observe()` (metrics only); only a consumer acting on received state
   may call `.throwWhenInvalid()`.
3. **`MethodObserver.observable(o, label)`** — test pattern. One `Observer` per test
   class (static field), one `MethodObserver` per test method (`mo.forMethod("...")`
   must match the test method name exactly). Tests inspect `violations()` /
   `violationNamesRemovingPrefix(mo.observationPoint())`.

`Observer.MonitoringMode` is `ON_FAILURE` (default — metric per violation only) or
`ALWAYS` (metric per inspected constraint, valid or invalid). Metric emission is
wrapped in a try/catch — the observability framework never throws from metric
emission.

---

## 7. Path-Composition Rules

When the walker descends, paths compose one segment per level. A `Compound` aggregate
declaring:

```java
i.entityName(name, "name")
 .valueObject(this, Compound::compoundInfo, "compoundInfo")
 .valueObjectCollection(this, Compound::windows, "windows");
```

…with `CompoundInfo.invariants()` declaring `notNull(this, CompoundInfo::formula, "formula")`
and `windows.[1]` failing `onset`, produces flattened constraint names:

```
Compound.compoundIsValid.c.name
Compound.compoundIsValid.c.compoundInfo
Compound.compoundIsValid.c.compoundInfo.formula
Compound.compoundIsValid.c.windows
Compound.compoundIsValid.c.windows.[0].onset
Compound.compoundIsValid.c.windows.[1].onset
```

`scope` from `Observer` / `MethodObserver` contributes the leading
`ClassName.methodName.label` — **three** segments before the domain-relative path.
Tests strip this with `mo.observationPoint()` (returns `ClassName.methodName`) plus
the trailing `.label.` to leave only the domain-relative portion.

---

## 8. Argument-Validation Pattern (Repository / Query Adapters)

Every public adapter method validates its arguments through the same builder:

```java
public Optional<Compound> getByName(CompoundName name) {
    observer.arguments("getByName", i -> i.entityName(name, "name"))
            .throwWhenInvalid();
    return delegate.findByName(name);
}
```

`observer.arguments(...)` is the only path that should reach `throwWhenInvalid()`
from a producer. Method-body observation of *constructed* state uses `.observe()`
and returns the value; the consumer chooses the terminal operation. See ADR-017.

---

## 9. Testing-Pattern Cheatsheet

```java
class CompoundTest {
    private static final Observer observer = Observer.forClass(CompoundTest.class);

    @Test void compoundIsValid() {
        var mo = observer.forMethod("compoundIsValid");
        Compound c = /* required fields populated via RandomValue; nullables null */;
        assertThat(mo.observable(c, "c").violations()).isEmpty();
    }

    @Test void compoundIsInvalid() {
        var mo = observer.forMethod("compoundIsInvalid");
        Compound c = /* construct with null for every component */;
        var obs = mo.observable(c, "c");
        assertThat(obs.violationNamesRemovingPrefix(mo.observationPoint() + ".c."))
            .containsExactlyInAnyOrder(
                "name", "commonName", "compoundInfo", "solubility",
                "bioavailability", "properties"
            );
    }
}
```

Rules:

- One `Observer` per test class (static field).
- One `MethodObserver` per test method via `observer.forMethod(...)`. The string
  **must match the test method name exactly** — the framework uses it to scope the
  observation.
- `mo.namedEntity(e, label)` for `NamedEntity`/`Aggregate`; `mo.observable(o, label)`
  for any `Observable` (covers `BehavioralCollection`, `ValueObject`).
- Valid case: `assertThat(...violations()).isEmpty()`.
- Invalid case: `containsExactlyInAnyOrder` with exact domain-relative paths — exact
  set, no extras, no missing.
- **Never call `isValid()` on individual `Constraint` objects in tests.** Always go
  through `InvariantObservation` so the full graph is walked in one pass.

---

## 10. The `Constraint<V>` Contract (for context; rarely called directly)

Each constraint method registers a record implementing `Constraint<V>`:

```java
public interface Constraint<V> {
    String name();          // dotted path; updated on each descent via withName(...)
    V value();              // the value being validated (may be null)
    boolean isValid();      // the actual rule
    Constraint<V> withName(String name);  // immutable rename for path qualification
    default String source();              // first dotted segment (class)
    default String methodName();          // second dotted segment (method)
    default String errorMessage();        // optional human message
}
```

Composite nodes additionally implement `ConstraintCollection`, which exposes
`constraints()` (the next level of children) and the default `collectConstraints()`
that prepends `name() + "."` to each child's name and recurses.

You only touch this surface when extending the framework with a new constraint type —
which is a kernel change, out of scope for normal domain work.

---

## 11. Anti-patterns Specific to `Constraints`

- **Do not call `isValid()` on individual `Constraint` objects.** Always go through an
  `InvariantObservation` so the full graph is walked in one pass.
- **Do not use the by-function form when the direct-value form suffices.** Inside
  `invariants()`, `this` cannot be null — the indirection is dead weight.
- **Do not call `notEmpty(value, name)` direct-form.** The overload doesn't exist.
- **Do not call `notBlank` on a non-String type.** `notBlank` is String-only.
- **Do not invent `namedEntityOrNull` / `observableOrNull`.** Use a block lambda with
  a null guard.
- **Do not `throwWhenInvalid()` from a producer observing its own output.** Use
  `.observe()`; let the consumer decide the terminal operation. Argument validation is
  the one exception (it always throws — boundary contract).
- **Do not include a `notNull` for a `valueObject(...)` you've already declared.** The
  `ObservableConstraint` already asserts non-null **and** descends; an extra
  `notNull(this, P::field, "field")` is redundant and produces a duplicated violation
  on null.
- **Do not include a `notNull` for an `entityName(...)` you've already declared.**
  `EntityName.isValid(value)` rejects null already.
- **Do not pair `valueObjectOrNull` with a `notNull` on the same field.** The "or
  null" is the contract; asserting non-null elsewhere contradicts it.
- **Do not assume empty-collection failure from `valueObjectCollection`.** Pair with
  `notEmpty` when empty must be rejected.