# measurements

Typed physical quantities shared across the catalog, so a depth, an area, or a
conductivity is never a bare number that could be confused with another.

---

## What it provides

A set of single-value `record` types, each implementing the framework's
`NumericNamedValue` over a `BigDecimal`, with a fixed decimal scale and rounding
mode and an `isValid()` range check:

| Type | Unit | Notes |
|------|------|-------|
| `DepthInches` | inches | sensor installation, mulch, and tillage depth; must be positive |
| `AreaSquareFeet` | square feet | zone and sub-zone boundaries |
| `SlopeDegrees` | degrees | terrain slope and aspect, constrained to 0–90° |
| `MoisturePercent` | % volumetric | sensor-derived soil moisture, 0–100% |
| `PrecipitationInches` | inches | precipitation accumulation from a station or gauge |
| `ElectricalConductivity` | dS/m | soil soluble-salt concentration, per the FGL reporting standard |

Each type carries its own precision. `ElectricalConductivity`, for example,
rounds to two decimal places `HALF_UP` — matching the reporting precision of the
Fruit Growers Laboratory and the soil sensors — and rejects negative values.
Because each quantity is its own type, a method that takes a `DepthInches` cannot
be handed an `AreaSquareFeet`, and the unit is fixed by the type rather than
carried as a convention.

---

## Learn more

- [`docs/measurement-standards.md`](../../docs/measurement-standards.md) — the units used across the catalog.
- [ADR-014 — Named values](../../docs/adr/ADR-014-named-values.md)
- [ADR-015 — BigDecimal numeric precision](../../docs/adr/ADR-015-bigdecimal-numeric-precision.md)
