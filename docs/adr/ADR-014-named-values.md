# ADR-014: Named Values

**Status:** Draft
**Full rationale:** [rationale/ADR-014-named-values.md](rationale/ADR-014-named-values.md)

## Decision

Introduce `NamedValue<T>` in `kernels/framework` — a typed, non-identifying, single-value
wrapper whose type IS its domain name. Fills the gap between `EntityName<N>` (identity)
and `ValueObject` (multi-field composite).

### Interface
```java
public interface NamedValue<T> {
    T value();
    boolean isValid();
    default boolean isNotValid() { return !isValid(); }
}
```
- **Does not extend `Observable`.** No invariant graph of its own — the owning container
  declares invariants on its behalf via `Invariants#namedValue(...)`.

### Observability integration
- `Invariants#namedValue(o, fn, name)` constraint added alongside `entityId`/`entityName`
- `NamedValueConstraints.NamedValueConstraint` delegates to `NamedValue#isValid()`

### What it IS
- Typed distinct wrapper (`TaxonomicOrder` ≠ `TaxonomicFamily`, both `NamedValue<String>`)
- Carries validation and domain behavior on the type
- Travels with the value through the system

### What it is NOT
- **Not `EntityName<N>`** — no uniqueness, not used in lookups
- **Not `ValueObject`** — single value, no collective meaning
- **Not a substitute for** `Map<String, String> properties` open-ended runtime attributes

### Implementation pattern
- Record implementing `NamedValue<T>`
- **No throwing constructor** — may exist in invalid state; `isValid()` is the sole
  validation mechanism; `NamedValueConstraint` surfaces invalidity at the boundary
- `public static of(T value)` factory per ADR-012
- **`@JsonCreator` required on every `of(...)` factory** (same reason as `EntityName`
  and `PersistenceId<Long>` subclasses — Jackson would otherwise bypass the factory)
- Domain behaviors declared on the concrete type, derived from the wrapped value alone,
  no cross-domain references

### `NumericNamedValue` — decimal precision
```java
public interface NumericNamedValue extends NamedValue<BigDecimal> {
    int scale();
    RoundingMode roundingMode();
    default BigDecimal normalized() { return value().setScale(scale(), roundingMode()); }
}
```
- Scale and rounding mode are domain facts encoded in the type
- **Integer quantities** use `NamedValue<Integer>` — not `NumericNamedValue` with `scale=0`
  (false precision, obscures integral nature)
- **Physical constants** are `static final BigDecimal` on a domain class, not `NamedValue<T>`
- **`double`/`float` prohibited** in domain model for decimals — see ADR-015

### Module placement
- **Tier 1** — named value that is a component of a type in a kernel module belongs in
  that same kernel (e.g. `TaxonomicOrder` in `kernels/taxonomy`)
- **Tier 2** — named value that is a component of a domain api entity/VO belongs in that
  api module (e.g. `MolecularFormula` in `chemistry-api`)
- **`domains/identifiers/` is never the home** — that's identity infrastructure only

## Consequences

- Field types compile-time distinct — transposition errors become compile errors
- Validation structurally enforced at instantiation
- Domain behavior travels with the value
- `NamedValue<T>` not `Observable` — container owns invariant declaration
- `EntityName<N>` retains identity-only role; `ValueObject` retains composite role
- Linnaean rank types live in `kernels/taxonomy`, not `domains/identifiers`
- `NumericNamedValue` eliminates IEEE 754 rounding errors from decimal fields
- Physical constants are `static final BigDecimal`, not `NamedValue<T>`
- Reference library depends on named-value modules, never the reverse (DAG invariant)
- Migration candidate: `TaxonomicClassification` — `String` components → concrete
  `NamedValue<String>` types
