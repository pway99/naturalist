# ADR-014: Named Values

**Status:** Draft

## Context

Every domain field backed by a primitive — `String`, `int`, `double` — carries its
meaning in two places: the component name in the declaring record and the variable name
at the call site. Both are AST artifacts. Neither travels with the value once it escapes
its container.

`TaxonomicClassification#order` returns a `String`. Within the record, the component
name `order` and the `@Nullable` annotations on `genus` and `species` hold the Linnaean
context. At the call site, that context is whatever the receiving variable happens to be
named. The value is a character array. Nothing in the type system communicates that it
represents a rank in the Linnaean hierarchy, that it must begin with a capital letter, or
that `"Coleoptera"` and `"coleoptera"` are a validation error rather than two different
values.

The same applies across the domain. A soil pH reading, a molecular formula string, a
temperature in Fahrenheit — each is meaningful in isolation but the type system treats
them as structurally identical. A method accepting two `String` arguments, or two
`double` arguments, cannot prevent transposition. Javadoc documents intent; the compiler
does not enforce it.

`EntityName<N>` solves this for identity: `CompoundName`, `TaxonomicOrderName` are
strongly typed natural keys. They are not the right fit for non-identifying fields.
An order string is not unique across the domain; the same rank name appears on every
beetle, every butterfly, every longhorn. Modelling it as an `EntityName` subclass would
misrepresent its role and break the uniqueness contract `TestEntitySource` enforces on
all `EntityName` values.

`ValueObject` is not the right fit either. A `ValueObject` is a multi-field composite
where the members have collective meaning as a domain concept (ADR-013). A single-value
typed wrapper is not a composite. Modelling a taxonomic order string as a `ValueObject`
record would satisfy the constraints only by degeneration — one field does not "have
collective meaning."

There is a gap: typed, non-identifying, single-value wrappers that carry domain semantics
and can own validation and behavior. This is the `NamedValue<T>` concept.

## Decision

Introduce `NamedValue<T>` in `kernels/framework` as a public interface. It does not
extend `Observable`. Concrete implementations are records in the module that defines the
domain concept.

```java
// kernels/framework
public interface NamedValue<T> {
    T value();
    boolean isValid();

    default boolean isNotValid() {
        return !isValid();
    }
}
```

`NamedValue<T>` deliberately mirrors the contract of `EntityName<N>` — `value()` plus
`isValid()` — without extending `Observable`. A named value is a simple behavioral
wrapper. It has no invariant graph of its own to declare; the container that owns the
field is `Observable` and declares the invariant on the field's behalf, exactly as it
does for `PersistenceId<?>` and `EntityName<N>` fields today.

### Observability Integration

`Invariants` gains a `namedValue` constraint method alongside the existing `entityId`
and `entityName` overloads. The constraint delegates to `NamedValue#isValid()`,
consistent with how `EntityNameConstraints` delegates to `EntityName#isValid()`.

```java
// Invariants.java — new overload
public <O, V extends NamedValue<?>> Invariants namedValue(O o, Function<O, V> valueFunction, String name) {
    return add(new NamedValueConstraints.NamedValueConstraint<>(o, valueFunction, name));
}
```

```java
// observability/invariants/NamedValueConstraints.java
public interface NamedValueConstraints {
    class NamedValueConstraint<O, V extends NamedValue<?>> extends InvariantByFunction<O, V> {

        public NamedValueConstraint(O o, Function<O, V> valueFunction, String name) {
            super(o, valueFunction, name);
        }

        @Override
        public boolean isValid() {
            V v = value();
            return v != null && v.isValid();
        }
    }
}
```

The container's `invariants()` uses the new constraint exactly as it uses `entityName`:

```java
// TaxonomicClassification
@Override
public Consumer<? extends Invariants> invariants() {
    return i -> i
            .namedValue(this, TaxonomicClassification::order, "order")
            .namedValue(this, TaxonomicClassification::family, "family");
}
```

Nullable `NamedValue<T>` fields (e.g. `genus`, `species`) are validated with
`notNull` only when the owning domain rule requires their presence — the same pattern
used for nullable `EntityName` fields on partially-identified entities.

### What NamedValue<T> Is

A `NamedValue<T>` is a typed, non-identifying, single-value wrapper whose type IS its
domain name. The type `TaxonomicOrder` carries its meaning structurally. A method
accepting `TaxonomicOrder` cannot accidentally receive a `TaxonomicFamily` — both are
`NamedValue<String>` but they are distinct types. The compiler enforces the distinction
that variable names previously only implied.

### What NamedValue<T> Is Not

It is not `EntityName<N>`. `EntityName<N>` is a natural key — it implies uniqueness and
participates in entity lookup contracts. `NamedValue<T>` makes no uniqueness claim.
`TaxonomicOrder.of("Coleoptera")` is a field value on any of the thousands of beetle
catalog entries; it identifies nothing.

It is not `ValueObject`. A `ValueObject` is a multi-field composite domain concept
where the members have collective meaning together (ADR-013 Constraint 4). A
`NamedValue<T>` wraps a single value. It is not `Observable` and carries no invariant
graph.

It is not a substitute for the `Map<String, String> properties` pattern used for
open-ended, runtime-extensible compound attributes. `NamedValue<T>` is for domain
fields known at compile time whose types, validation rules, and behaviors are fixed by
the domain model. Open-ended attribute maps represent extensibility concerns that are
orthogonal to this ADR.

### Concrete Implementation Pattern

Concrete types are records implementing `NamedValue<T>`. They carry no throwing
constructor. A `NamedValue<T>` may exist in an invalid state; `isValid()` is the
sole expression of the validation predicate, and the `NamedValueConstraint` surfaces
invalidity at the boundary. Domain-specific behavior methods are declared on the
concrete type, not on the interface.

```java
// kernels/taxonomy — not identifiers, because TaxonomicOrder is not a natural key
public record TaxonomicOrder(String value) implements NamedValue<String> {
    
    @JsonCreator
    public static TaxonomicOrder of(String value) {
        return new TaxonomicOrder(value);
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank();
    }
}
```

The declaring `ValueObject` (e.g. `TaxonomicClassification`) replaces the raw
primitive component with its concrete `NamedValue<T>` type and declares the constraint
in its own `invariants()`:

```java
public record TaxonomicClassification(
        TaxonomicOrder order,
        TaxonomicFamily family,
        @Nullable TaxonomicGenus genus,
        @Nullable TaxonomicSpecies species
) implements ValueObject {

    @Override
    public Consumer<? extends Invariants> invariants() {
        return i -> i
                .namedValue(this, TaxonomicClassification::order, "order")
                .namedValue(this, TaxonomicClassification::family, "family");
    }
}
```

### NumericNamedValue — Decimal Precision

A sub-interface of `NamedValue<BigDecimal>` in `kernels/framework` for domain fields
that carry a decimal quantity. It mandates a scale and rounding mode as domain facts
encoded in the type, not as runtime configuration at the call site.

```java
// kernels/framework
public interface NumericNamedValue extends NamedValue<BigDecimal> {
    int scale();
    RoundingMode roundingMode();

    default BigDecimal normalized() {
        return value().setScale(scale(), roundingMode());
    }
}
```

Concrete implementations declare their precision contract as constant overrides:

```java
public record TemperatureFahrenheit(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static TemperatureFahrenheit of(BigDecimal value) {
        return new TemperatureFahrenheit(value);
    }

    @Override public int scale()                 { return 1; }
    @Override public RoundingMode roundingMode() { return RoundingMode.HALF_UP; }
    @Override public boolean isValid()           { return value != null; }
}

public record MolecularWeight(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static MolecularWeight of(BigDecimal value) {
        return new MolecularWeight(value);
    }

    @Override public int scale()                 { return 4; }
    @Override public RoundingMode roundingMode() { return RoundingMode.HALF_UP; }
    @Override public boolean isValid()           { return value != null && value.compareTo(BigDecimal.ZERO) > 0; }
}
```

**Integer quantities** — domain values that are whole numbers by scientific definition
(e.g. `TemperatureKelvin` for the precision levels this application requires) use
`NamedValue<Integer>` directly. Applying `NumericNamedValue` with `scale=0` to an
inherently integer quantity is a false precision — `BigDecimal` with scale=0 solves no
rounding problem that `Integer` does not already solve, and it obscures the domain fact
that the quantity is integral. Use `NamedValue<Integer>` when the integer nature is a
domain constraint, not an approximation.

**Physical constants** such as Avogadro's number are not `NamedValue<T>` instances.
A constant does not travel as a field on an entity; it belongs as a `static final
BigDecimal` on an appropriate domain class, annotated with its source reference per
ADR-009.

**`double` and `float` are prohibited** in the domain model for decimal quantities.
The precision contract belongs to the type; IEEE 754 rounding is a silent error that
`NumericNamedValue` eliminates at the definition site. A future ADR will codify the
domain-wide `BigDecimal` mandate and address existing `double` fields such as
`CompoundInfo#molecularWeight` and `SolubilityProfile#solubility`.

### Module Placement

`NamedValue<T>` and `NamedValueConstraints` live in `kernels/framework`. Concrete
implementations follow a two-tier rule.

**Tier 1 — Kernel-level named values.** A named value that is a component of a type
already in a kernel module belongs in that same kernel. `TaxonomicOrder`,
`TaxonomicFamily`, `TaxonomicGenus`, and `TaxonomicSpecies` are components of
`TaxonomicClassification`, which lives in `kernels/taxonomy` — they go there. Any
domain module already depending on `kernels/taxonomy` gains access at no additional
cost to the dependency graph.

A new kernel module (e.g. `kernels/measurements`) is warranted only when a named
value concept is genuinely universal — used across multiple unrelated domain modules
with no natural home in any existing kernel. `TemperatureFahrenheit` used equally by
soil, insects, and climate events would qualify. Anticipating this need speculatively
is premature; the threshold is active cross-domain use with no existing kernel home.

**Tier 2 — Domain api-level named values.** A named value that is a component of an
entity or value object in a domain api belongs in that api module. `MolecularFormula`
and `MolecularWeight` are components of `CompoundInfo` in `chemistry-api` — they
belong there. Visibility follows the accessor: if the named value surfaces through a
public record component it must be public; package-private is only appropriate for
named values internal to a sub-package that never appear on the module's public API
surface.

**`domains/identifiers/` is not the right home for named values.** That module is
identity infrastructure — `PersistenceId<Long>` and `EntityName<N>` subclasses used
cross-domain as FK references. Named values are not referenced cross-domain by
identity. A `NamedValue<T>` implementation does not belong in `domains/identifiers/`
under any circumstance; if a concept needs to serve as a cross-domain natural key it
should be classified as an `EntityName` subclass instead.

### Behaviors

A `NamedValue<T>` implementation may expose domain-specific behavior methods. These
must derive from the wrapped value alone; they must not reference other entities or
cross domain boundaries. Legitimate behaviors on `TaxonomicOrder` include
`isKnown()` (checked against a controlled Linnaean rank vocabulary) or `rank()` returning
a `LinnaeanRank` enum. The behavior lives on the type and travels with the value — not
scattered across call sites.

### Static Factory

All concrete implementations expose a `public static of(T value)` factory method per
ADR-012. The canonical constructor remains accessible as a Java record requirement but
`of(...)` is the documented instantiation path.

`@JsonCreator` is required on every `of(...)` factory. Jackson 2.19.x deserialises
plain records natively via `Class.getRecordComponents()`, but `NamedValue<T>`
implementations are records whose sole instantiation path through the domain contract
is the static factory. Without `@JsonCreator`, Jackson bypasses the factory and
attempts canonical-constructor deserialization, producing incorrect or failing
results. This applies equally to `EntityName` subclasses and `PersistenceId<Long>`
subclasses for the same reason — all three types use `of(...)` as their construction
contract, and Jackson must be told explicitly.

### Validation

`isValid()` is the sole validation mechanism. A `NamedValue<T>` may be constructed in
an invalid state — from a null JSON field, a blank deserialized string, or a value that
fails a domain rule — without throwing. The `NamedValueConstraint` surfaces invalidity
at the boundary via the `Observer`. No compact constructor logic, no exception-based
control flow. This is identical to the `EntityName<N>` contract and for the same reason:
the constraint system is the enforcement point, not the constructor.

## Consequences

- Domain field types become compile-time distinct: `TaxonomicOrder` and `TaxonomicFamily`
  are different types even though both wrap `String`. Transposition errors that were
  previously runtime surprises become compile errors.
- Validation moves to the instantiation point and is structurally enforced. A
  `TaxonomicOrder` that escapes construction is a valid taxonomic order.
- Domain behavior travels with the value. `isKnown()`, `rank()`, and similar methods
  are defined once on the type, accessible everywhere the type appears, rather than
  reimplemented inline at each call site.
- `NamedValue<T>` is not `Observable`. It carries no invariant graph. The container
  owns the invariant declaration via `Invariants#namedValue(...)`, consistent with how
  `entityId` and `entityName` constraints work today. The observability framework gains
  one new constraint class and one new `Invariants` overload; nothing else changes.
- `EntityName<N>` retains its identity-only role. `NamedValue<T>` types do not
  participate in entity lookup contracts and are never passed to repository query methods.
- `ValueObject` retains its multi-field composite role. The two concepts no longer
  compete for the same modelling slot; a single-value domain field is a `NamedValue<T>`,
  a multi-field domain concept is a `ValueObject`.
- Linnaean rank types belong in `kernels/taxonomy`, not `domains/identifiers`. The
  identifier package remains identity infrastructure; domain vocabulary lives with the
  domain.
- The `Map<String, String> properties` pattern on `Compound` is unaffected. Named values
  and open-ended property maps solve different problems. Do not replace a `properties`
  map with a `NamedValue<T>` field unless the attribute has become a stable, named,
  compile-time concept warranting its own type and validation rule.
- `NumericNamedValue` eliminates IEEE 754 rounding errors from decimal domain fields.
  Scale and rounding mode are domain facts declared on the type — not ad-hoc `setScale`
  calls scattered at call sites. `normalized()` applies the contract in one place.
- Integer quantities use `NamedValue<Integer>`; `NumericNamedValue` is for decimal
  quantities only. The distinction is a domain fact, not a precision preference.
- Physical constants are `static final BigDecimal` fields, not `NamedValue<T>` instances.
- The reference library (ADR-009) is a consumer of named values, not a producer of them.
  The library module depends on `kernels/taxonomy`, `chemistry-api`, and other domain
  modules to access the named value types it needs for traceability and annotation. Named
  value modules must never take a dependency on the reference library in return. This
  dependency direction is a DAG invariant: named values are foundational vocabulary;
  the library is a higher-order domain concept that draws on them.
- Migration: `TaxonomicClassification` is the first candidate for migration. Replace
  `String order`, `String family`, `@Nullable String genus`, `@Nullable String species`
  with their concrete `NamedValue<String>` counterparts. This is a breaking change to
  the `TaxonomicClassification` API and must be coordinated across all organism domain
  modules that construct or consume it.
