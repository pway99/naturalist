# Sensors

The soil-monitoring hardware layer — a catalog of the physical sensors installed
around the site and the timestamped moisture readings they produce.

---

## Core concepts

**A sensor is a stable, named installation.** `Sensor` is a `NamedEntity` keyed
by a `SensorName` slug. It records the installation context needed to read its
data correctly: the hardware model, the `ZoneName` (and optional `SubZoneName`)
it monitors, the burial depth, install and relocation dates, and its calibration
mode. Model-specific capability is answered by behavior — `measuresEc()`,
`isDeepProbeCapable()`, `hasBeenRelocated()` — rather than duplicated as flags.

**A reading is an immutable measurement.** `SensorReading` is an `Entity` keyed
by a UUIDv7 `SensorReadingId`, carrying the raw hardware AD value alongside the
firmware-derived `MoisturePercent`, plus optional electrical conductivity and
temperature for models that report them. It is a child entity of the soil
domain's `SoilProfile` aggregate, referenced across that boundary by name.

**Raw and derived values are both kept.** Factory calibration is tuned for
mineral soil, so the unprocessed AD count is retained next to the converted
percentage — the raw value is the stable basis for a custom calibration curve.

**Threshold facts live elsewhere.** Temperature and moisture triggers are owned
by the [climate](../climate/README.md) domain; readings are evaluated against
them, they are not defined here.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance): `sensors-api`
(the `Sensor` and `SensorReading` entities and their typed identifiers),
`sensors-core`, and `sensors-repository-test` (the in-memory adapter and its
behavioral contract).

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the working conventions, source-data format, and the
  intended analysis pipeline for this domain.
