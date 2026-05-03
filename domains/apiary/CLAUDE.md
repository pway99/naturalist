# Apiary Domain

## Domain Vocabulary

**Colony** — Aggregate root. The managed honeybee superorganism. Individual bees are
never tracked — Colony is the unit of management.

**ColonyLifecycleState** — INSTALLED → ESTABLISHING → ESTABLISHED → PRODUCTIVE →
WINTER_PREP → WINTERING → SPRING_BUILDUP → PRODUCTIVE.

**InspectionRecord** — Immutable entity within Colony aggregate.
Queen status, egg presence, brood pattern, mite count, honey stores, pest observations.

**ExternalObservationRecord** — Lightweight observation without opening hive.
Pollen intake level, forager traffic, temperament.

## Domain Model

**Colony** — Aggregate root. The managed honeybee superorganism. Individual bees are never
tracked — Colony is the unit of management.

**ColonyLifecycleState** — INSTALLED → ESTABLISHING → ESTABLISHED → PRODUCTIVE →
WINTER_PREP → WINTERING → SPRING_BUILDUP → PRODUCTIVE

**InspectionRecord** — Immutable entity within Colony aggregate.
Fields: queen status, egg presence, brood pattern, mite count, honey stores, pest observations.

**ExternalObservationRecord** — Lightweight observation without opening hive.
Fields: pollen intake level, forager traffic, temperament.
April 8 2026 observed: IMPRESSIVE pollen intake, colony establishing strongly.

## Varroa Treatment Windows (Chico-specific domain facts)

These are hard constraints encoded as domain rules — not configurable:

| Treatment         | Window                  | Constraint                 |
|-------------------|-------------------------|----------------------------|
| Formic acid       | EXCLUDED June–September | Contraindicated above 85°F |
| Thymol (Apiguard) | Late August–September   | Effective range 59–105°F   |
| Oxalic acid       | December–January        | Broodless period only      |

Formic acid contraindication threshold is defined in Climate module as `ClimateThreshold`.
Peak temperature April 6 2026: 84.8°F — just below formic acid limit.
Validates Apiguard (thymol) as correct Varroa treatment for Chico summers.

## Insect Module Boundary

- **Apiary** module: Colony as aggregate root, lifecycle management, inspection records
- **Insects** module: Insecta taxonomy and organism catalog (six legs, not colony-managed)
- **VarroaManagement**: application module combining Apiary + Chemistry + Climate + Arachnids

## SHB (Small Hive Beetle) Control

Only *Heterorhabditis indica* (H. indica) nematode is effective for SHB control.
*Steinernema feltiae* (S. feltiae) does NOT work for SHB. Never recommend S. feltiae for SHB.
