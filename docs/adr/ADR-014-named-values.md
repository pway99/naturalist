# ADR-014: Named Values
> [rationale](rationale/ADR-014-named-values.md)

`NamedValue<T>` in `kernels/framework` — typed, non-identifying, single-value wrapper
whose type IS its domain name. Fills the gap between `EntityName<N>` (identity) and
`ValueObject` (multi-field composite).

```java
public interface NamedValue<T> {
    T value();
    boolean isValid();
    default boolean isNotValid() { return !isValid(); }
}
```
- **Not `Observable`.** Owning container declares invariants via `Invariants#namedValue(...)`.

**Observability**
- `Invariants#namedValue(o, fn, name)` constraint alongside `entityId`/`entityName`.
- `NamedValueConstraints.NamedValueConstraint` delegates to `NamedValue#isValid()`.

**What it IS / IS NOT**
- IS: typed distinct wrapper (`TaxonomicOrder` ≠ `TaxonomicFamily`, both `NamedValue<String>`);
  validation + domain behavior on the type.
- NOT: `EntityName` (no uniqueness, not used in lookups); `ValueObject` (single value);
  substitute for `Map<String, String>` open-ended properties.

**Implementation pattern**
- Record implementing `NamedValue<T>`.
- **No throwing constructor** — may exist in invalid state; `isValid()` + `NamedValueConstraint`
  surface invalidity at the boundary.
- `public static of(T value)` factory per ADR-012.
- **`@JsonCreator` on every `of(...)` factory** (Jackson would otherwise bypass it — same
  as `EntityName` / `PersistenceId<Long>` subclasses).
- Domain behaviors on the concrete type derive only from the wrapped value — no
  cross-domain refs.

**`NumericNamedValue` — decimal precision**
```java
public interface NumericNamedValue extends NamedValue<BigDecimal> {
    int scale();
    RoundingMode roundingMode();
    default BigDecimal normalized() { return value().setScale(scale(), roundingMode()); }
}
```
- Scale and rounding are domain facts on the type.
- Integer quantities use `NamedValue<Integer>`, not `NumericNamedValue` with `scale=0`.
- Physical constants are `static final BigDecimal` on a domain class.
- `double`/`float` prohibited in domain decimals (ADR-015).

**Module placement**
- Tier 1 — component of a kernel type → same kernel (e.g. `TaxonomicOrder` in `kernels/taxonomy`).
- Tier 2 — component of a domain api entity/VO → that api module (e.g. `MolecularFormula` in `chemistry-api`).
- `domains/identifiers/` is never the home — identity infrastructure only.
