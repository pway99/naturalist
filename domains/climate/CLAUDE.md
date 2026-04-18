# Climate Domain

## Domain Vocabulary

**ClimateProfile** — Aggregate root. Complete atmospheric and seasonal characteristics
of a geographic location. Owns SeasonalProfile, FrostCalendar, GrowingSeasonProfile
as value objects. Instances in `climate-zones.json`.

**ClimateThreshold** — Entity. A temperature or climate value that triggers a biological
event when crossed. Referenced by all domain modules — thresholds are domain facts defined
here, not in the consuming module.

Current Oak Vista thresholds:
| Name                          | Trigger | Domain significance                          |
|-------------------------------|---------|----------------------------------------------|
| thiobacillus-activation       | 77°F    | Sulfur oxidation rate increases (Q10 ~2)     |
| thrips-emergence              | 65°F    | Thrips become active                         |
| formic-acid-contraindication  | 85°F    | Formic acid Varroa treatment excluded above  |
| thymol-minimum-efficacy       | 59°F    | Apiguard minimum effective temperature       |
| tomato-pollen-viability-risk  | 95°F    | Pollen viability drops                       |

**SeasonalProfile** — ValueObject. Monthly normals — temperature, precipitation, sun hours,
humidity. Includes precipitation pattern and season character descriptions.

**FrostCalendar** — ValueObject. Last spring frost date, first fall frost date,
frost-free days, frost probability curve.
Chico: ~March 15 last frost, ~November 20 first frost, 280 frost-free days.

**GrowingSeasonProfile** — ValueObject. Frost-free days, annual chill hours, growing degree days,
production windows.
Chico: 280 frost-free days, ~800 chill hours, ~3200 GDD.

**Chill Hours** — Hours of temperature below 45°F accumulated during winter dormancy.
Required by deciduous fruit trees to break dormancy and fruit reliably.
Oh Henry Peach: 650 hours required — reliably met in Chico.
Shinseiki Asian Pear: 250–300 hours required — reliably met in Chico.

## Design Rule

The Climate module defines thresholds. Other modules (Sensors, Apiary, Soil) evaluate
data *against* those thresholds — they do not define threshold values themselves.
This separates domain knowledge (Climate) from analysis process (consumers).
