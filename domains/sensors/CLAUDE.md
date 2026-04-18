# Sensors Domain

## Purpose

The sensors domain ingests raw hardware export data (Ecowitt GW1200 gateway XLSX files),
enriches and analyses it through the domain model, and produces structured
`SensorAnalysisReport` aggregates. Goal: replace ad-hoc Python scripting with
reproducible domain-model-driven computation.

## DAG Position

```
Identifiers
     │
     ├── Climate  ─────────── (provides threshold catalog)
     │                               │
     ├── Zones                       │
     │       │                       │
     └── Soil                        │
          │                          │
          └── SensorAnalysis ────────┘
```

Application module: `SoilMonitoring` (Soil + SensorAnalysis + Climate + Zones).

## Source Data

**Hardware:** Ecowitt GW1200B gateway (Device ID: WIFIA178)
**Export format:** XLSX — two-row multi-level header, one data sheet (`result_list`)
**Export interval:** 4-hour readings
**File naming:** `all-{DeviceId}_{StartDate}-{EndDate}_.xlsx`

## Header Parsing — Critical

The XLSX export uses TWO header rows (group name + measurement name). Parse with pandas:

```python
df = pd.read_excel(file_path, sheet_name='result_list', header=[0,1])
df.columns = [
    ' '.join([str(c) for c in col if 'Unnamed' not in str(c)]).strip()
    for col in df.columns
]
```

This produces: `"Garden box 1 moisture Soil Moisture(%)"`, `"Garden box 1 moisture AD"`, etc.
The sensor channel name prefix (`"Garden box 1 moisture"`) maps to a `ZoneId` via
`sensor-channel-map.json`.

## Zone-to-Channel Mapping

```json
{
  "Garden box 1 moisture": "garden-box-1",
  "Backyard garden moisture": "backyard-garden"
}
```

Loaded by `SensorChannelRegistry` at startup. New sensors (WH52, WH51L) require new entries.

**Pending sensors:**
- WH52 (EC + temperature + moisture) — channel name: `"Backyard garden EC"`
- WH51L x2 (deep probe) — channel names: `"Garden box 1 deep moisture"`, `"Backyard garden deep moisture"`

## Five-Stage Pipeline

### Stage 1: Ingest
Parse XLSX → flatten headers → extract relevant columns → sort chronologically → `RawReadingList`

### Stage 2: Enrich
Per reading: compute `moistureDelta` from previous reading, detect watering events (Δ% > 3.0),
evaluate climate thresholds, tag: `WATERING_EVENT`, `THRESHOLD_CROSSED`, `TRIGGER_APPROACHING`

### Stage 3: Aggregate
Daily summaries per zone: temperature high/low, moisture high/low/range, AD high/low,
watering event count, threshold crossing count

### Stage 4: Analyse (scientific functions — see below)

### Stage 5: Report
Produce `SensorAnalysisReport` aggregate → XLSX via `ReportExporter`
Sheets: Dashboard, Sensor Data (raw + delta + event tags), Moisture Trends (chart data)

## Module Layer Boundaries

- `SensorExportParser` → infrastructure layer (not domain)
- `SensorAnalysisService` → domain service (pure, no infrastructure)
- `SensorAnalysisReport` → aggregate root (domain)
- `ReportExporter` → infrastructure layer (XLSX output)

Python script (`parse_ecowitt_export.py`) handles XLSX-to-JSON conversion.
The Java domain layer reads JSON — never touches XLSX directly.

## Scientific Functions

### Watering Event Detection
Threshold: Δmoisture > 3.0 percentage points per 4-hour interval.
Validated: April 4 event +14%, April 6 event +8%.

### Drainage Recovery Time
Time from peak moisture to within 2% of pre-watering baseline.

| Recovery time  | Classification | Meaning                         |
|----------------|----------------|---------------------------------|
| < 6 hours      | HEALTHY        | Intact macropore architecture   |
| 6–12 hours     | RECOVERING     | Partial biological reconstruction|
| 12–24 hours    | DISRUPTED      | Significant macropore disruption |
| > 24 hours     | IMPAIRED       | Severe disruption or compaction  |

April 4 (3 days post-rototill): 20–24 hours → DISRUPTED.
April 6 (8 days post-rototill): faster → RECOVERING (trend: IMPROVING).

### Moisture Baseline
Mean of stable readings (Δ < 0.5% per interval).
Oak Vista: Box 1 = 52% (AD 251), Backyard = 59% (AD 277).
Worm casting/coco coir blend has different field capacity than mineral soil —
factory WH51 calibration is not accurate; use AD values for custom calibration.

### Thiobacillus Activity Score
Count of readings at or above 77°F (25°C).
Thiobacillus thiooxidans sulfur oxidation increases significantly above 25°C (Q10 ~2).
Each 4-hour above-threshold reading = 1 activity unit.
April 4–9: 7 consecutive above-threshold readings (peak 84.8°F April 6 13:00).
Sulfur oxidation was active. Backyard CaCO3 (2.9%) → in-situ gypsum:
  H₂SO₄ + CaCO₃ → CaSO₄ + H₂O + CO₂

### Climate Threshold Evaluation
Thresholds defined in Climate module (`thresholds.json`) — analysis service evaluates
data against them, does not define them. Separation of knowledge (Climate) from process (Sensors).

Oak Vista April 2–10 2026:
- thiobacillus-activation (77°F): 7 crossings
- formic-acid-contraindication (85°F): 0 (peak 84.8°F)
- tomato-pollen-viability-risk (95°F): 0

### BER Risk Score
Composite of moisture consistency + soil Ca status.
High risk in Oak Vista: very low soluble Ca + coco coir binding + Chico heat.

### Finding Generation (priority order)
1. Watering trigger crossed → ACTION_REQUIRED
2. Approaching trigger (within 5%) → WATCH
3. Drainage worsening → CAUTION
4. Drainage recovering → INFO
5. Thiobacillus active (≥3 readings) → INFO
6. Peak temperature approaching 85°F → WATCH
7. Zero rainfall in period → INFO

## Report Formatting (Naturalist Ecological colour palette)

| Element      | Hex     | Usage                                  |
|--------------|---------|----------------------------------------|
| Forest       | #1E3A1E | Primary headers, dashboard title       |
| Moss         | #2D5016 | Section headers, Box 1 data            |
| Fern         | #4A7C2F | Sub-headers, threshold events          |
| Honey        | #C8860A | Watering events, warnings              |
| Blue Cool    | #1A3A5C | Backyard data, climate data            |
| Parchment    | #FDF8F0 | Alternate row background               |
| Light Green  | #EAF2E0 | Primary row background                 |
| Light Amber  | #FEF8EC | Watering event highlight rows          |
| Alert Orange | #CC6600 | Warning text, hot temperature cells    |

Conditional formatting:
1. Watering event rows: light amber background, honey bold text on timestamp
2. Temperature ≥ 77°F: light orange background, orange bold text
3. Temperature ≥ 84°F: stronger orange — approaching formic acid limit
4. Moisture below trigger: red background
5. Moisture within 5% of trigger: yellow background
