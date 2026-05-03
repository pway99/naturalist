# ADR-015: BigDecimal for Decimal Domain Values

> [rationale](rationale/ADR-015-bigdecimal-numeric-precision.md)

`double`/`float` prohibited for decimal domain values. All decimal record components,
enum fields, and method parameters use `BigDecimal`.

1. **Record components.** Every decimal component is `BigDecimal`. Never `double`/`Double`/`float`.
2. **`NumericNamedValue` for named decimal concepts.** When the decimal names a domain
   concept (temperature, pH, EC, solubility, molecular weight), use a `NumericNamedValue`
   subtype (ADR-014) over plain `BigDecimal`. Threshold: if a domain expert would name the
   concept independently of the field, it's a `NumericNamedValue`. Dimensionless multipliers
   / purely contextual scalars stay `BigDecimal`.
3. **Method parameters.** Behavior methods take the `NumericNamedValue` subtype where a
   domain name exists. Eliminates transposition errors from `(double tempF, double ppm)`.
4. **Enum fields.** Enums can't implement `NumericNamedValue`. Declare private `SCALE`
   and `ROUNDING_MODE` constants; normalise in constructor. **`BigDecimal.valueOf(double)`
   never `new BigDecimal(double)`** — the latter preserves IEEE 754 artifacts.
5. **JSON.** Jackson 2.19.x handles `BigDecimal` record components natively. No
   `@JsonDeserialize` required. Catalog JSON unchanged.

`double` in the domain model is a review flag.
