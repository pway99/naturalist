# Garden

The cultivated layer over the wild catalog. Where the organism domains record
what *grows* at Oak Vista, garden records what was *planted* — the intent to
grow something, somewhere, for a season.

The distinguishing idea is intent. A salvia in the meadow is an observation; the
same species set deliberately in a bed is a crop. The plant is identical; the
intent is not, and the intent is what garden models.

---

## Core concepts

**A planting is the finest grain — what went into the ground, where, and for how
long.** `Planting` is an `Entity<PlantingId>` carrying two optional soft
references into the plants catalog on independent axes: `plantName` is a
`PlantRankName` — the Linnaean identification at whatever rank the gardener can
support, so a tray of unlabelled salvia starts is a genus-rank planting rather
than a missing one — and `cultivarName` is the orthogonal horticultural
selection. At least one must be present; `removedDate` null means still growing.

**A planted zone is what is growing in a bed, composed on read.** `PlantedZone`
is a `ReadModel` keyed by place, at either grain — a whole zone or one sub-zone —
because that is how a gardener holds it: the back bed is one place with tomatoes
and an eggplant in it, not two separate records. It is assembled by
`PlantedZoneFactory` and never stored.

**Garden owns intent, not the things it references.** Zone owns *where*, plants
owns what a species *is*, and soil owns measured values. Garden references each
only by typed name and stores none of their state — every cross-domain edge is a
soft `EntityName`, so the module imports no other domain's api.

**A crop type is agronomic, not botanical.** `CropTypeName` is an identifier with
no entity behind it — the category (`tomato`, `lettuce`) a soil analysis is
interpreted for. One plant species maps to many crop types (*Brassica oleracea*
is kale, cabbage, broccoli, kohlrabi, and brussels sprouts) and the reverse, so
neither collapses into the other.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance). Read-only, like
soil — there is no command surface. Domain-specific notes:

- **`garden-api`** — `Planting`, the `PlantedZone` read model, and their queries.
  ADR-020's N=1 collapse applies (top-level package-private repository, top-level
  public query, no namespace wrappers).
- **`garden-core`** — `PlantingQueryImpl`, `PlantedZoneQueryImpl`, and the
  package-private `PlantedZoneFactory` read-model assembler.
- **`garden-repository-rdms`** — production persistence adapter (currently
  delegating to the in-memory mock; see the top-level README).

Fixture data is the real 2026 beds — nine plantings cross-checked against the
cultivars the plants catalog carries and the zones soil samples.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the working vocabulary, invariants, and the several
  places garden deliberately declines to model something (`Crop`, `CropProfile`).
- [`docs/garden-domain-bootstrap.md`](../../docs/garden-domain-bootstrap.md) —
  what the domain is, decided before any code existed.
- [`docs/plans/organism-domain-blueprint.md`](../../docs/plans/organism-domain-blueprint.md) —
  section D covers `PlantRankName` and why a planting's identification is
  rank-polymorphic.
