# habitat

A structural vocabulary for describing the habitat character of an organism or
location — where a species lives, in general terms, rather than the specific
named place it was found.

---

## What it provides

**`HabitatProfile`.** A `ValueObject` that composes four orthogonal axes into a
single, comparable description of habitat: a set of zones, a moisture regime, a
light regime, and a set of vertical layers. It carries convenience predicates —
`occupies(zone)`, `operatesIn(layer)`, `hasMoistureRegime(...)`,
`hasLightRegime(...)`. Zones and layers are sets because many organisms span
more than one; moisture, light, and layers are nullable because habitat
characterisation is incremental, where null means "not yet characterised". Only
`zones` is required — at least one zone must be known for a profile to mean
anything.

The distinction the type is built around: a profile is a *description* of
habitat character that travels with a catalogued organism ("riparian edge,
mesic, partial sun"), not a named, located place — the latter is a Zone in the
zones domain.

**Four enum vocabularies**, each an axis of the profile:

| Vocabulary | Axis | Values |
|-----------|------|--------|
| `HabitatZone` | landscape-scale zone | `MEADOW`, `BARE_GROUND`, `CULTIVATED`, `HEDGEROW`, `WOODLAND_EDGE`, `WOODLAND`, `CHAPARRAL`, `WETLAND`, `RIPARIAN`, `COMPOST_HEAP` |
| `MoistureRegime` | substrate moisture | `XERIC`, `MESIC`, `HYDRIC`, `SEASONALLY_XERIC` |
| `LightRegime` | daily direct-sun exposure | `FULL_SUN`, `PARTIAL_SUN`, `DAPPLED`, `FULL_SHADE` |
| `VerticalLayer` | vertical stratum | `CANOPY`, `UNDERSTORY`, `SHRUB_LAYER`, `HERBACEOUS_LAYER`, `GROUND_SURFACE`, `SUBTERRANEAN` |

Constants are ordered along their natural gradient — open-to-closed and dry-to-wet
for zones, canopy-to-below-ground for layers — and each carries documentation of
the ecological niche it represents in a Mediterranean garden context.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — shared-kernel conventions.
- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) — the habitat kernel in context.
