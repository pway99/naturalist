# Soil

The soil-chemistry record for the site — lab analyses, per-nutrient
measurements, and the physical characteristics of each managed bed, anchored to
the zone it sits in.

Its fixtures are real: every value traces to a Fruit Growers Laboratory report
(FGL CH 2671853, sampled March 2026) for the Oak Vista site near Chico,
California. A failing test points at a domain-model error or a real-world change,
not a made-up number.

---

## Core concepts

**A measurement is a fact; an optimum or a status is interpretation.** This is
the domain's central design decision. `NutrientReading` and
`SoilPhysicalCharacteristics` store what the instrument measured and nothing
else — no target, no band, no status hangs off a measurement. Interpretation is
derived by applying a crop context over those values, and lives in its own types.

**`SoilProfileInfo` is the aggregate root and spatial anchor.** A `NamedEntity`
that ties a managed soil unit to its `Zone` (and optionally a `SubZone`)
by typed name — soil-api has no compile-time dependency on zone-api. Everything
else points upward at it, one level at a time; there are no skip-level references
and no downward collections on the stored records. Children are gathered by
reverse lookup at assembly time.

**The observation grain is deliberately split.** `LabAnalysisInfo` is a report
header; `NutrientReading` is one nutrient value on one analysis (the grain that
makes "track a nutrient over time" a direct query); `SoilPhysicalCharacteristics`
is the small, richly-typed set of derived properties (CEC, pH, EC, limestone,
base saturation) that come one-per-analysis. Calcium, magnesium, potassium, and
sodium are each reported as two separate readings — *exchangeable* (held on the
soil colloid, the reserve) and *soluble* (dissolved, available now).

**Lab-printed optima and recommendations are stored, not computed.**
`ReportedOptimum` and `ReportedRecommendation` capture the ranges and inputs the
lab actually printed beside each reading, as their own append-only facts with the
same grain as a reading. FGL's exchangeable-cation ranges turn out to be
crop-invariant base-saturation targets projected through the sample's own CEC;
storing what the lab printed is what lets a future replica strategy be tested
against reality rather than against its own assumptions.

**The `*Info` / bare-noun convention runs throughout.** A `*Info` type is the
persisted fact — an entity with a repository, a query, and a JSON catalog. The
bare noun is the assembled `ReadModel`, composed on read by `SoilProfileFactory`
and never stored: `SoilProfileInfo` → `SoilProfile`, `LabAnalysisInfo` →
`LabAnalysis`, and `NutrientPanel` grouping the seventeen readings into the FGL
report's primary / secondary / micro sections.

**Amendment history is modelled but not yet wired.** The `event` sub-context
carries `AmendmentEvent`, `TillageEvent`, `IrrigationEvent`, and a soil-local
`PrecipitationEvent` as immutable domain events with real behavior (tillage
recovery estimates, leaching-event detection), but no repositories, fixtures, or
assembly yet — a designed, dormant sub-context.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance). Domain-specific notes:

- **`soil-api`** uses flat top-level types with no namespace wrappers — the root
  package and the `observation` sub-context both apply the single-entity
  namespace collapse. A third sub-package, `event`, is modelled but unwired (see
  above). Soil is not an organism domain: it depends on `kernels/measurements`
  but carries no taxonomy and no field-notes `Description`.
- **`soil-core`** holds the query adapters and `SoilProfileFactory`, the
  read-model assembler.
- **`soil-repository-rdms`** is the production-named persistence adapter; it
  currently delegates to the in-memory mock — a temporary state while the app is
  built out behind a stable port.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the domain vocabulary and FGL optimum ranges.
- [`docs/briefings/soil-domain.md`](../../docs/briefings/soil-domain.md) — the
  authoritative type-by-type tour: package map, the measurement-vs-interpretation
  rule, cross-domain references, and the Oak Vista fixture values.
