# ADR-015: BigDecimal for Decimal Domain Values

**Status:** Draft
**Full rationale:** [rationale/ADR-015-bigdecimal-numeric-precision.md](rationale/ADR-015-bigdecimal-numeric-precision.md)

## Decision

`double` and `float` are prohibited for decimal domain values. All decimal record
components, enum fields, and method parameters use `BigDecimal`.

### Rule 1 — Record components
Every decimal record component is `BigDecimal`. No `double`, `Double`, `float`.

### Rule 2 — `NumericNamedValue` for named decimal concepts
When a decimal field represents a named domain concept (temperature, pH, EC, solubility,
molecular weight), use a `NumericNamedValue` subtype (ADR-014) over plain `BigDecimal`.
The type carries the semantic label, scale, and rounding mode as domain facts.

Threshold: if a domain expert would name the concept independently of the field, it's
a `NumericNamedValue`. Dimensionless multipliers or purely contextual scalars can stay
as plain `BigDecimal`.

### Rule 3 — Method parameters
Behavior methods accepting decimal parameters take the `NumericNamedValue` subtype where
a domain name exists. Eliminates transposition errors from `(double tempF, double ppm)`.
`BigDecimal` is the minimum acceptable type for generic quantities.

### Rule 4 — Enum fields
Enums cannot implement `NumericNamedValue`. Enum constants with decimals declare private
`SCALE` and `ROUNDING_MODE` constants and normalise in the constructor. **Use
`BigDecimal.valueOf(double)`, never `new BigDecimal(double)`** — the latter preserves
IEEE 754 artifacts before rounding.

### Rule 5 — JSON deserialisation
Jackson 2.19.x handles `BigDecimal` record components natively. No `@JsonDeserialize`
required. Catalog JSON files need no format change.

### Scale/rounding reference

| Concept | Scale | Rounding |
|---|---|---|
| `TemperatureFahrenheit` | 1 | HALF_UP |
| `SoilPH` | 2 | HALF_UP |
| `ElectricalConductivity` (dS/m) | 2 | HALF_UP |
| `MolecularWeight` (g/mol) | 4 | HALF_UP |
| `AtomicWeight` (g/mol) | 4 | HALF_UP |
| `Solubility` (g/L at 20°C) | 2 | HALF_UP |
| `AmendmentRate` (lbs/1000 sqft) | 2 | HALF_UP |
| `AreaSqft` | 1 | HALF_UP |
| `MoisturePercent` | 1 | HALF_UP |
| `DepthInches` | 2 | HALF_UP |
| `PrecipitationInches` | 2 | HALF_UP |

New `NumericNamedValue` types add an entry here or in the implementing type's Javadoc.

### Migration
Module by module; within a module, record components before method parameters. No mixed
`BigDecimal`/`double` APIs in any committed state. Order: `kernels` → `chemistry-api`
(`PeriodicElement` first — 118 constants, single focused commit; then profiles) →
`soil-api` → `sensors-api` → `zone-api` → `weather-api`.

## Consequences

- Silent floating-point accumulation errors structurally eliminated
- Scale/rounding declared once per type — no `setScale` at call sites
- `BigDecimal` arithmetic is verbose (`add`/`multiply`/`divide` with `MathContext`);
  correct trade-off — precision is a domain requirement
- `double` in domain model after this ADR is a review flag
