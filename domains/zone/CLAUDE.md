# Zone Domain

## Domain Vocabulary

**Zone** — Aggregate root. A named physical space with permanent geographic, solar,
and substrate characteristics. Exists independently of its occupants.

**SubZone** — Entity within Zone aggregate. A named spatial subdivision within a Zone
with distinct management characteristics. Emerged from April 7 2026 TSWV outbreak management.

**MulchType** — STRAW (HIGH thrips habitat risk), PINE_NEEDLE (LOW), WOOL (VERY_LOW — lanolin
deters insects), BURLAP_COLLAR, BARE.

**TreatmentTarget** — Sealed interface: `ZoneTreatmentTarget | SubZoneTreatmentTarget`.
Enables exhaustive pattern matching at application layer.

**PestPressureRecord** — Immutable value object. Dated record of pest or disease pressure
in a SubZone. Drives crop rotation decisions.

**SubstrateType** — NATIVE_CLAY, RAISED_BED_AMENDED, NATIVE_CLAY_AMENDED, CONTAINER.
Determines drainage characteristics and biological amplification factor.

## Domain Model

**Zone** — Aggregate root. A named physical space with permanent geographic, solar,
and substrate characteristics. Exists independently of its occupants.

**SubZone** — Entity within Zone aggregate. A named spatial subdivision with distinct
management characteristics. Emerged from April 7 2026 TSWV outbreak management.

**ZoneInfo** — ValueObject. Zone name, type, and descriptive metadata.

**Boundary** — ValueObject. `areaSqft()` and spatial coordinates.

**Microclimate** — ValueObject. `summerThermalRisk()`, shading characteristics.

**SunExposure** — ValueObject. `hasAfternoonShade()`, orientation.

## SubZone Design

SubZone was introduced as a named sub-division within a Zone when TSWV management
required tracking pest pressure at a finer granularity than the whole Zone.

Key SubZone fields:
- `@Nullable SoilProfileName soilProfileName` — links to a SoilProfile (nullable: not all sub-zones have a dedicated soil profile)
- `ThripsHabitatRisk surfaceHabitatRisk` — driven by `MulchType`
- `List<PestPressureRecord> pestPressureHistory` — immutable historical records

**PestPressureRecord** — Immutable value object. Dated record of pest or disease pressure
in a SubZone. Drives crop rotation decisions.

## TreatmentTarget (sealed interface)

```java
sealed interface TreatmentTarget permits ZoneTreatmentTarget, SubZoneTreatmentTarget {}
```

Enables exhaustive pattern matching at the application layer when a treatment
applies to either an entire Zone or a specific SubZone.

## MulchType and Thrips Habitat Risk

| MulchType     | ThripsHabitatRisk | Notes                              |
|---------------|-------------------|------------------------------------|
| STRAW         | HIGH              | High thrips overwintering habitat  |
| PINE_NEEDLE   | LOW               |                                    |
| WOOL          | VERY_LOW          | Lanolin deters insects             |
| BURLAP_COLLAR | —                 |                                    |
| BARE          | —                 |                                    |

## SubstrateType

NATIVE_CLAY, RAISED_BED_AMENDED, NATIVE_CLAY_AMENDED, CONTAINER.
Determines drainage characteristics and biological amplification factor.

## Zone Delegate Methods

Zone delegates to its child value objects for convenience accessors:

```java
public ZoneName zoneName()            { return zoneInfo.name(); }
public ZoneType zoneType()            { return zoneInfo.type(); }
public double areaSqft()              { return boundary.areaSqft(); }
public ThermalRisk summerThermalRisk(){ return microclimate.summerThermalRisk(); }
public boolean hasAfternoonShade()    { return sunExposure.hasAfternoonShade(); }
```

These are behavior methods — not record components. Do not confuse with record accessors.

## Pending: OakVistaZoneFixtures

Populate ZoneRepository with real property zones. Each zone needs:
substrate type, sun exposure, aspect, area, summer thermal risk.
