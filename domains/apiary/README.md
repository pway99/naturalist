# Apiary

Beekeeping records for the site's managed honeybee colonies — the hive as a unit
of management, not the individual bee.

---

## Core concepts

**The colony is the aggregate.** `Colony` is the aggregate root and the managed
superorganism; individual bees are never tracked. Its lifecycle is an explicit
state progression — installed, establishing, established, productive, winter
prep, wintering, spring buildup — that frames what each season's management is for.

**Two kinds of observation.** An `InspectionRecord` is the full hive-opening
report — queen status, egg presence, brood pattern, mite count, honey stores,
pest sightings. An `ExternalObservationRecord` is the lighter check made without
opening the hive — pollen intake, forager traffic, temperament. Both are
immutable entities within the colony aggregate.

**Treatment windows are domain rules, not settings.** Varroa treatment options
are constrained by season and temperature — formic acid excluded through the hot
months, thymol effective in a bounded range, oxalic acid only during the
broodless period. These are encoded as domain facts, and the temperature bound
they depend on is the same `ClimateThreshold` owned by the
[climate](../climate/README.md) domain.

---

## Module layout

This domain is a specified skeleton: the standard module structure —
`apiary-api`, `apiary-core`, `apiary-repository-test` — is in place following the
split described in the
[top-level README](../../README.md#architecture-at-a-glance), and the domain
model is defined in its `CLAUDE.md`, but the entities are not yet implemented in
code.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the domain vocabulary, colony lifecycle, and the
  Chico-specific treatment-window facts.
