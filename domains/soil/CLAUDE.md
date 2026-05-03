# Soil Domain

## Domain Vocabulary

**SoilProfile** — Aggregate root. Complete soil system within a Zone — chemical history,
biological activity, physical structure.

**LabAnalysis** — Immutable value object. External laboratory soil chemistry test.
Current lab: FGL Environmental, Chico CA. Lab ID CH 2671853. Nutrient values in lbs/1000 sqft.

**NutrientPanel** — ValueObject owned by LabAnalysis. Measured nutrient values.
`NutrientStatus`: VERY_LOW, LOW, OPTIMAL, HIGH, VERY_HIGH.

**OptimumRanges** — Domain knowledge as code. Ca: 16–26, K: 6.5–19, Mg: 2.8–9.0, P: 8–12 lbs/1000 sqft.
EC: below 2.0 dS/m. pH: 6.5–7.5. Limestone: below 0.50%.

**SensorReading** — Immutable value object. Timestamped moisture measurement. Raw AD value
(hardware) plus derived moisture percent (calibrated). AD value preferred for custom calibration.

**AmendmentEvent** — Immutable domain event. Record of a soil amendment application.
Never updated — only inserted. Historical fact.

**TillageEvent** — Immutable domain event. Rototilling record.
HIGH_DISRUPTION (> 6 inches) → 21-day biological structure recovery.

**Biological Amplification Factor** — Multiplier on nitrogen release rates in biologically
active soils. Worm casting blend: 1.6–2.0× mineral soil baseline.

**Rehabilitation Trajectory** — Multi-year projected path of soil nutrients toward optimum ranges.
Gypsum calcium rehabilitation: 3–5 seasons. Tracked annually by FGL March soil test.

## Domain Model

**SoilProfile** — Aggregate root. The complete soil system within a Zone — chemical history,
biological activity, physical structure. Contains child entities and value objects.

**LabAnalysis** — Immutable value object. External laboratory soil chemistry test.
Current lab: FGL Environmental, Chico CA. Lab ID CH 2671853. Nutrient values in lbs/1000 sqft.

**NutrientPanel** — ValueObject owned by LabAnalysis. Measured nutrient values.
`NutrientStatus`: VERY_LOW, LOW, OPTIMAL, HIGH, VERY_HIGH.

**AmendmentEvent** — Immutable domain event. Record of a soil amendment application.
Never updated — only inserted. Historical fact.

**TillageEvent** — Immutable domain event. Rototilling record.
HIGH_DISRUPTION (> 6 inches) → 21-day biological structure recovery.
Current instance: April 6, 2026 rototill, Box 1 and Backyard.

**SensorReading** — Immutable value object. Timestamped moisture measurement.
Fields: raw AD value (hardware), derived moisture percent (calibrated).
Use AD values for custom calibration on non-mineral soils.

## FGL Optimum Ranges (domain facts — encode as `OptimumRanges`)

| Nutrient  | Optimum               |
|-----------|-----------------------|
| Ca        | 16–26 lbs/1000 sqft   |
| K         | 6.5–19 lbs/1000 sqft  |
| Mg        | 2.8–9.0 lbs/1000 sqft |
| P         | 8–12 lbs/1000 sqft    |
| EC        | below 2.0 dS/m        |
| pH        | 6.5–7.5               |
| Limestone | below 0.50%           |

**Oak Vista pH is ~7.2 — already optimal. Never recommend lime.**

## Biological Amplification Factor

Multiplier applied to nitrogen release rates in biologically active soils.
Worm casting blend: 1.6–2.0× mineral soil baseline.
Blood meal nitrogen mineralises faster than standard tables predict.
Must account for this factor in `NitrogenStatus` computation (see pending Q3 in root CLAUDE.md).

## Oak Vista Baseline Values (April 2026 FGL test CH 2671853)

These are real measured values — use them as test fixture data:

- Soluble Ca: very low (BER risk elevated)
- Limestone: 2.9% in backyard bed (in-situ gypsum formation from sulfur amendment)
- pH: ~7.2 (optimal — no lime)

## Sensor Calibration

WH51 factory calibration is for mineral soil. Oak Vista worm casting/coco coir blend reads
3-5% high. AD values (raw hardware measurements) are usable for custom calibration.
Box 1 resting baseline: 52% (AD ~251). Backyard resting baseline: 59% (AD ~277).
Watering triggers: Box 1 = 35%, Backyard = 40%.

## Rehabilitation Trajectory

Multi-year projected path of soil nutrients toward optimum ranges.
Gypsum calcium rehabilitation: 3–5 seasons projected.
Tracked annually by FGL March soil test.
