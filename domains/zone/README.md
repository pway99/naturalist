# Zone

The spatial backbone of the site — the domain that owns *where*. A zone is a
named physical space with permanent geographic, solar, and substrate
characteristics that exists independently of whatever is growing or living in it.
Other domains reference these places by name; zone stores no crop, no organism,
no measurement.

---

## Core concepts

**A zone is an aggregate rooted in place.** `Zone` is the aggregate root — a bed,
plot, or area with a stable identity and a set of value objects describing it:
`ZoneInfo` (name and type), a geographic boundary with its area, `SunExposure`
(orientation, afternoon shade), `Microclimate` (thermal risk, shading), and a
substrate description that determines drainage. The root delegates to these
children for convenience accessors; they are behavior, not extra components.

**A sub-zone is a finer management grain within a zone.** `SubZone` is an entity
inside the aggregate, introduced when pest management needed to track pressure at
a granularity below the whole zone. It carries its own substrate surface
(`MulchType`, which drives a `ThripsHabitatRisk`), an optional link to a soil
profile, and a history of `PestPressureRecord` value objects that drive crop
rotation decisions.

**Treatment can target either grain.** `TreatmentTarget` is a sealed interface
over a whole zone or a specific sub-zone, so the application layer resolves a
treatment's scope by exhaustive pattern match rather than a nullable field.

**The domain describes the ground, not its use.** Cultivation intent belongs to
garden, measured values to soil, organisms to their own domains. Zone holds only
the durable properties of a place — which is what lets every other domain point
at a zone by name without inheriting its concerns.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance). Zone is one of the
aggregate-shaped domains: `Zone` owns `SubZone` and its value objects as a single
consistency boundary, assembled from persisted parts rather than referenced
across the boundary by value.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the domain vocabulary, the sub-zone design, and the
  mulch/substrate tables that drive pest-pressure risk.
