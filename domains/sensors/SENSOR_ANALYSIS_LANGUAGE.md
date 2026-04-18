# Sensor Analysis — Ubiquitous Language
## Naturalist Application — Addendum to UBIQUITOUS_LANGUAGE.md

---

## Sensor Module Terms

### SensorReading
An immutable timestamped measurement from a soil sensor.
Entity within the Soil/observation sub-context.
Contains both rawAdValue (hardware measurement) and moisturePct
(factory-calibrated derived value).

Fields:
- id: SensorReadingId
- sensorId: SensorId
- zoneId: ZoneId
- recordedAt: Instant
- depth: SoilDepth
- rawAdValue: int        — unprocessed hardware capacitance output
- moisturePct: double    — factory-calibrated volumetric percentage
- moistureDelta: double  — change from previous reading (computed on ingest)

### RawAdValue
The unprocessed analog-to-digital converter output from the sensor hardware.
Used for custom calibration in worm casting/coco coir blend where factory
calibration reads 3-5% high compared to true volumetric water content.
Higher AD = wetter soil. Stable reference independent of calibration algorithm.

### MoisturePct
Factory-calibrated volumetric water content as percentage.
Calibrated for standard mineral soil — reads elevated in worm casting blend.
Use for relative trend analysis and watering trigger decisions.
Use RawAdValue for cross-bed comparison and custom calibration.

### MoistureBaseline
The stable resting moisture percentage a Zone's sensor returns to
between watering events under ambient evapotranspiration conditions.
Established empirically from pre-disturbance stable readings.
Oak Vista baselines (April 2026):
  Box 1: 52% (AD ~250-253)
  Backyard: 59% (AD ~275-279)
NOT a fixed value — baselines shift seasonally as temperature and
plant water demand change. Recalibrate monthly.

### WateringTrigger
The moisture percentage below which irrigation should be applied.
Established from plant stress observations and agronomic knowledge.
Oak Vista triggers: Box 1 below 35%, Backyard below 40%.
Expressed as percentage not AD — more intuitive for operational decisions.

### WateringEvent
A detected moisture increase event where moistureDelta exceeds the
watering detection threshold (default 3%).
Computed during enrichment phase of sensor data processing.
Immutable record once detected — describes what the sensor observed.
Fields: zoneId, detectedAt, peakMoisturePct, preMoisturePct, deltaPct.

### DrainageRecoveryTime
The elapsed time between a WateringEvent peak moisture reading and
the sensor returning to within 2% of the pre-watering baseline.
The primary quantitative indicator of soil biological health.
Healthy soil with intact macropore architecture: 4-8 hours.
Rototilled disrupted soil: 20-24+ hours.
Tracks biological recovery progress over the growing season.
Units: hours (decimal).

### DrainageRecoveryTrend
The directional change in DrainageRecoveryTime across successive
watering events within a Zone. Decreasing trend = biological recovery.
Increasing trend = compaction or waterlogging developing.
The core biological health signal derived from sensor data.

### ClimateThresholdEvent
A recorded instance of a climate measurement crossing a defined
biological threshold from the Climate module threshold catalog.
Immutable domain event — it happened at a specific time and cannot change.
Fields: thresholdId, zoneId, occurredAt, measuredValue, direction.

### ThiobacillusActivityScore
The count of readings at or above 77°F soil/air temperature
within a defined period. Proxy for cumulative sulfur oxidation activity.
Directly relevant to Oak Vista gypsum rehabilitation program —
higher score means more in-situ gypsum formation from elemental sulfur.

### BERRiskScore
Composite risk score for Blossom End Rot across a Zone.
Computed from:
  - Soil calcium status (from latest LabAnalysis)
  - Moisture consistency (standard deviation of recent readings)
  - Temperature stress events (days above 95°F)
  - Current foliar treatment program status
Higher score = higher BER risk. Drives CalMag OAC protocol urgency.

### SensorChannelMap
The mapping between Ecowitt export column names and ZoneIds.
Defined in sensor-channel-map.json.
The column name is set in the Ecowitt app and appears verbatim
in XLSX exports. This map is the translation layer between
hardware naming and domain naming.

### SensorAnalysisReport
Aggregate root of the Sensor Analysis module.
Produced by SensorAnalysisService from a raw sensor export.
Owns KpiSummary, DailySummaryList, WateringEventList,
ThresholdEventList, FindingList, RawReadingList as value objects.
Represents a complete analytical interpretation of a sensor export period.

### KpiSummary
Value object owned by SensorAnalysisReport.
Key performance indicators derived from the analysis period:
peak temperature, current moisture per zone, watering trigger status,
rainfall total, threshold crossing count, drainage recovery trend.

### Finding
Value object owned by SensorAnalysisReport.
A structured observation with title, narrative, severity, and
recommended action. Produced by scientific analysis functions.
Severity: INFO, WATCH, CAUTION, ACTION_REQUIRED.

### SensorExportParser
Infrastructure component — reads Ecowitt XLSX export format.
Handles multi-level header parsing, column extraction, timestamp parsing.
Returns structured RawReadingList for domain processing.
Lives in the infrastructure layer — not the domain.

---

## Measurement Standards for Sensor Data

| Measurement | Unit | Source | Notes |
|---|---|---|---|
| Moisture | % volumetric | WH51/WH51L sensor | Factory calibrated for mineral soil |
| Raw capacitance | AD (integer) | WH51/WH51L sensor | Hardware output — calibration independent |
| EC | dS/m | WH52 sensor | Pending installation ~April 14 |
| Soil temperature | °F | WH52 sensor | Pending installation |
| Air temperature | °F | GW1200 gateway | Outdoor sensor |
| Humidity | % relative | GW1200 gateway | Outdoor sensor |
| Solar radiation | W/m² | GW1200 gateway | Solar sensor |
| Rainfall | inches | GW1200 gateway | Tipping bucket gauge |
| Sensor voltage | V | GW1200 gateway | Battery health indicator |

---

## Scientific Functions — Summary

Full implementations in SensorAnalysisService.java

### drainageRecoveryTime(readings, zoneId, event)
Given a watering event and subsequent readings for a zone,
compute elapsed hours until moisture returns to within 2%
of pre-event baseline. Returns Optional — empty if still recovering.

### detectWateringEvents(readings, zoneId, threshold=3.0)
Scan readings for moisture delta above threshold.
Returns List<WateringEvent> ordered by time.

### computeBaseline(readings, zoneId, stableWindowHours=48)
Find the stable resting moisture by identifying periods with
moisture delta below 0.5% over a defined window.
Returns baseline moisture % and AD value.

### evaluateThresholdCrossings(climateReadings, thresholds)
For each ClimateThreshold in the catalog, find readings that
cross the threshold value in the defined direction.
Returns List<ClimateThresholdEvent>.

### thiobacillusActivityScore(climateReadings, windowDays)
Count readings at or above 77°F within the window.
Returns integer score and list of qualifying timestamps.

### berRiskScore(zone, labAnalysis, recentReadings, treatmentStatus)
Composite BER risk assessment combining soil chemistry,
moisture consistency, and heat stress data.
Returns BERRiskScore value object with component breakdown.

### moistureTrend(readings, zoneId, windowDays)
Linear regression on recent moisture readings.
Returns trend direction (DRYING, STABLE, WETTING) and rate.

### drainageRecoveryTrend(events)
Compare DrainageRecoveryTime across successive watering events.
Returns IMPROVING, STABLE, or WORSENING with rate of change.
