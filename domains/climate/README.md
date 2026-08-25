# Climate

The site's atmospheric context — the seasonal normals and temperature thresholds
that other domains read but do not own.

---

## Core concepts

**Thresholds are shared domain facts.** `ClimateThreshold` names a temperature or
climate value that triggers a biological event when crossed — Thiobacillus
activation at 77°F, the formic-acid contraindication at 85°F, thrips emergence at
65°F. The rule that makes this domain worth isolating is single ownership:
climate *defines* thresholds; sensors, apiary, and soil *evaluate* their data
against them. Knowledge lives in one place, the process of applying it in another.

**A climate profile composes the seasonal picture.** `ClimateProfile` is the
aggregate root for a location, owning `SeasonalProfile` (monthly normals),
`FrostCalendar` (last and first frost dates, frost-free days), and
`GrowingSeasonProfile` (chill hours, growing degree days, production windows) as
value objects — the inputs a naturalist needs to reason about what will grow and
when.

---

## Module layout

This domain is a specified skeleton: the standard module structure —
`climate-api`, `climate-core`, `climate-repository-test` — is in place following
the split described in the
[top-level README](../../README.md#architecture-at-a-glance), and the domain
model is defined in its `CLAUDE.md`, but the entities are not yet implemented in
code.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the domain vocabulary, the current threshold set, and
  the knowledge-versus-process design rule.
