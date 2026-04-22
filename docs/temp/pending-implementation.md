# Pending Implementation

Priority order. Not loaded into conversation context — reference explicitly when picking up work.

1. **BehavioralCollection abstraction** — implement `BehavioralCollection<T extends Observable>`
   in `kernels/framework`. Abstract base class, `protected` constructor, `stream()`,
   `isEmpty()`, `size()`, `invariants()`. Bounded context: `kernels/framework` only.
   See ADR-011, ADR-012.
2. **ObservationService** — `latestReading(SensorId)`, `readingsBetween(SensorId, Instant, Instant)`, `latestLabAnalysis(ZoneId)`, `nutrientStatusFor(ZoneId, Nutrient)`
3. **EventService** — `recordAmendment(...)`, `recordIrrigation(...)`, `totalNitrogenApplied(ZoneId)`, `amendmentHistory(ZoneId, from, to)`
4. **IrrigationEvent** — `isLeachingIrrigation()`, correlation with sensor drainage curve
5. **TillageEvent** — `biologicalImpact()` → HIGH/MODERATE/LOW, `estimatedStructureRecovery()` → Duration. Current instance: April 6, 2026 rototill, Box 1 and Backyard.
6. **StateProjectionService** — computes current `SoilState`. Depends on ObservationService + EventService. Nitrogen status computation.
7. **NitrogenStatus** — current excess diagnosis. Must account for biological amplification factor from Zone. Critical: April 6, 2026 blood meal over-application.
8. **OakVistaZoneFixtures** — populate ZoneRepository with real property zones (substrate types, sun exposure, aspect per zone).
