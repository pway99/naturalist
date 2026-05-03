# ADR-015: BigDecimal for Decimal Domain Values

**Status:** Draft

## Context

IEEE 754 double-precision floating-point arithmetic is the default numeric type for
decimal values in Java. It is unsuitable for scientific domain models. The problem is
not edge-case precision loss — it is that `double` arithmetic silently produces results
that are wrong by small amounts that accumulate invisibly across computations:

```java
double pH = 6.8 + 0.1;  // 6.8999999999999995 — not 6.9
double rate = 1.25 * 3;  // 3.75 — happens to be exact here
double rate2 = 0.1 * 3;  // 0.30000000000000004 — not 0.3
```

In a soil chemistry model, a pH value of `6.8999999999999995` is not a minor
representation artefact — it is a false value. A nutrient threshold expressed as
`0.30000000000000004 lbs/1000 sqft` is wrong. An amendment rate calculated from
accumulated floating-point multiplications will drift from the agronomically correct
value in ways that are hard to detect and harder to trace.

`BigDecimal` solves this. It represents decimal values exactly within a defined scale
and applies rounding only at explicitly declared points. The scale and rounding mode are
domain facts — they belong on the type, not scattered as ad-hoc `setScale` calls at
call sites.

### Current state

Every decimal field in the domain model is currently `double` or `Double`. The affected
modules and fields are:

**chemistry-api**

- `SolubilityProfile` — `gramsPerLiterAt20C`, `ecContributionFactor`
- `VolatilizationProfile` — `minEffectiveTempF`, `maxSafeTempF`, `optimalTempF`, `vaporPressureAt20C`
- `SafetyProfile` — `maxSafeConcentrationPpm`
- `CationExchangeProfile` — `selectivityCoefficient`, `exchangeCapacityCmolKg`
- `Element` — `atomicWeight`
- `PeriodicElement` enum — `atomicWeight` (all 118 constants)
- Behavior method parameters on `Compound`, `VolatilizationProfile`, `ReactionConditions`

**soil-api**

- `NutrientPanel` — `cecMeqPer100g`, `pH`, `ecDsPerMeter`, `limestonePct`, `saturationPct`
- `NutrientReading` — `value`
- `AmendmentEvent` — `amount`
- `IrrigationEvent` — `volumeGallons`
- `PrecipitationEvent` — `totalInches`, `peakIntensityInchesPerHour`
- `TillageEvent` — `depthInches`
- `MulchLayer` — `depthInches`

**sensors-api**

- `SensorReading` — `moisturePercent`
- `Sensor` — `depthInches`

**zone-api**

- `GeographicBoundary` — `areaSqft`
- `Aspect` — `slopeDegrees`
- `SubstrateCharacteristics` — `biologicalAmplificationFactor`
- `ZonePrecipitationEvent` — `totalInches`, `peakIntensityInchesPerHour`

**weather-api**

- `PrecipitationEvent` — `totalInches`, `peakIntensityInchesPerHour`

## Decision

`double` and `float` are prohibited for decimal domain values. All decimal components
on records, all decimal fields on enums, and all decimal method parameters are
`BigDecimal`.

### Rule 1 — Record components

Every decimal component on a domain record is `BigDecimal`. No `double`, no `Double`,
no `float`.

```java
// Before
public record SolubilityProfile(double gramsPerLiterAt20C, double ecContributionFactor, ...) { ... }

// After
public record SolubilityProfile(BigDecimal gramsPerLiterAt20C, BigDecimal ecContributionFactor, ...) { ... }
```

### Rule 2 — NumericNamedValue for named decimal concepts

When a decimal field represents a domain concept with a name — temperature, pH,
electrical conductivity, solubility, molecular weight — it should be a `NumericNamedValue`
subtype (ADR-014) rather than a plain `BigDecimal`. The type carries the semantic label,
the scale, and the rounding mode as domain facts.

```java
// Plain BigDecimal — acceptable for anonymous scalars and dimensionless factors
BigDecimal ecContributionFactor

// NumericNamedValue — required when the concept has a domain name
TemperatureFahrenheit minEffectiveTempF
SoilPH pH
ElectricalConductivity ecDsPerMeter
```

The threshold: if a domain expert would name the concept independently of the field it
appears on, it is a `NumericNamedValue`. If it is a dimensionless multiplier or a purely
contextual scalar, plain `BigDecimal` is acceptable.

### Rule 3 — Method parameters

Behavior methods that currently accept raw `double` parameters must be updated. Where
the parameter has a domain name, accept the `NumericNamedValue` subtype. This eliminates
the transposition error that `double tempF, double concentrationPpm` silently permits.

```java
// Before
public boolean isSafeAtTemperature(double temperatureFahrenheit) { ... }

// After
public boolean isSafeAtTemperature(TemperatureFahrenheit temperature) { ... }
```

Where a parameter is a generic quantity without a distinct domain name, `BigDecimal`
is the minimum acceptable type.

### Rule 4 — Enum fields

Enum constants carrying decimal values use `BigDecimal`. `PeriodicElement#atomicWeight`
is the primary instance — 118 constants, each with a `double atomicWeight` that must
become `BigDecimal`.

Enums cannot implement `NumericNamedValue` — they do not extend records. Instead, the
enum declares private `SCALE` and `ROUNDING_MODE` constants and normalises the value
in the constructor. The enum constants retain readable double literals in source; the
precision contract is enforced once at construction.

```java
public enum PeriodicElement {
    Ca(20, "Calcium", 40.078),
    Mg(12, "Magnesium", 24.305),
    // ...;

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final int atomicNumber;
    private final String elementName;
    private final BigDecimal atomicWeight;

    PeriodicElement(int atomicNumber, String elementName, double atomicWeight) {
        this.atomicNumber = atomicNumber;
        this.elementName = elementName;
        this.atomicWeight = BigDecimal.valueOf(atomicWeight).setScale(SCALE, ROUNDING_MODE);
    }

    public BigDecimal atomicWeight() { return atomicWeight; }
}
```

`BigDecimal.valueOf(double)` is used rather than `new BigDecimal(double)`. The latter
converts the IEEE 754 representation directly and can produce values like
`40.07800000000000...` before rounding. `BigDecimal.valueOf` routes through
`Double.toString()`, which gives the canonical shortest decimal representation
(`"40.078"`), from which `setScale` rounds predictably. This is the safe path whenever
a `double` literal must become a `BigDecimal`.

### Rule 5 — JSON deserialisation

Jackson 2.19.x deserialises `BigDecimal` record components natively. JSON numeric
literals are parsed to `BigDecimal` without loss when the record component type is
`BigDecimal`. No `@JsonDeserialize` annotation is required. Catalog JSON files do not
need to change format — numeric literals deserialise to `BigDecimal` as-is.

### Scale and rounding mode reference

Each `NumericNamedValue` implementation declares its own scale. The following table
records the scale decisions for concepts identified at the time of this ADR. New
`NumericNamedValue` types must add an entry here or in the implementing type's
Javadoc.

| Concept                         | Scale | Rounding  | Notes                               |
|---------------------------------|-------|-----------|-------------------------------------|
| `TemperatureFahrenheit`         | 1     | `HALF_UP` | Sensor precision; tenths sufficient |
| `SoilPH`                        | 2     | `HALF_UP` | FGL reporting standard              |
| `ElectricalConductivity` (dS/m) | 2     | `HALF_UP` | FGL reporting standard              |
| `MolecularWeight` (g/mol)       | 4     | `HALF_UP` | IUPAC significant figures           |
| `AtomicWeight` (g/mol)          | 4     | `HALF_UP` | IUPAC standard values               |
| `Solubility` (g/L at 20°C)      | 2     | `HALF_UP` | Practical measurement precision     |
| `AmendmentRate` (lbs/1000 sqft) | 2     | `HALF_UP` | FGL reporting standard              |
| `AreaSqft`                      | 1     | `HALF_UP` | Tape-measure precision              |
| `MoisturePercent`               | 1     | `HALF_UP` | WH51 sensor resolution              |
| `DepthInches`                   | 2     | `HALF_UP` | Practical installation precision    |
| `PrecipitationInches`           | 2     | `HALF_UP` | Weather station precision           |

### Migration order

Migration proceeds module by module. Within a module, record components are migrated
before method parameters.

1. `kernels/` — no decimal fields currently; `NumericNamedValue` interface already present
2. `chemistry-api` — `PeriodicElement` enum first (foundational); then profiles
3. `soil-api` — `NutrientPanel` and `NutrientReading` first; events second
4. `sensors-api`
5. `zone-api`
6. `weather-api`

Domain modules that depend on a migrated module must be updated in the same commit.
Mixed-type APIs — a record with some `BigDecimal` and some remaining `double` components
— are not permitted at any committed state.

## Consequences

- Silent floating-point accumulation errors are structurally eliminated from the domain
  model. A `SoilPH` of `6.9` is exactly `6.9`, not `6.8999999999999995`.
- Scale and rounding mode are domain facts declared once on each `NumericNamedValue`
  type. `setScale` does not appear at call sites.
- `PeriodicElement` migration touches all 118 enum constants — this is mechanical but
  non-trivial. It should be a single focused commit.
- JSON catalog files require no format change; Jackson handles `BigDecimal`
  deserialisation from numeric literals natively.
- Arithmetic on `BigDecimal` is verbose compared to `double` (`add`, `multiply`,
  `divide` with explicit `MathContext` rather than `+`, `*`, `/`). This is the correct
  trade-off: precision is a domain requirement; syntactic convenience is not.
- `double` appearing in the domain model after this ADR is a review flag, as `new Foo(...)`
  at a call site outside the domain type's own class is a review flag per ADR-012.
