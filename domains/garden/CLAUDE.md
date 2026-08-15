# Garden Domain

**Garden is the domain of cultivation intent.** It answers: what is being grown, where,
when, and what does it require. Every type here should be justifiable as an answer to one
of those four questions.

The distinguishing feature is *intent*. A plant growing in the meadow is an observation.
The same species planted deliberately in a bed is a crop. The plant is identical; the
intent is not, and the intent is what garden models.

Design source: [`docs/garden-domain-bootstrap.md`](../../docs/garden-domain-bootstrap.md).

## Domain Vocabulary

**CropInfo** — Aggregate root, keyed by `CropName`. The cultivated category (`tomato`,
`lettuce`). Identity only: an optional soft `PlantName` to the botanical species, and
nothing else. Requirements belong to `CropProfile`, not here.

**Cultivar** — `NamedEntity<CultivarName>`. A named variety belonging to exactly one crop.
`amish-paste` and `san-marzano` are both tomatoes; what separates them is horticultural
selection, not phylogeny.

**Planting** — `Entity<PlantingId>`. A crop in a place over a period. `removedDate` null
means currently growing. The spatial nullability mirrors `SoilProfileInfo`: `zoneName`
always present, `subZoneName` null when the planting covers a whole zone.

**GardenPlan** — `ReadModel`. A crop assembled with its cultivars and its plantings.
Composed on read by `GardenPlanFactory`; never stored.

**CropProfile** — *not built.* Requirements keyed by `(source, crop, revision)`, because
different sources publish different tables and labs revise them silently. Second slice; it
needs decisions the fall 2026 soil data will inform.

## What garden is not

- **Not zone.** `zone-api` owns *where*. Garden references `ZoneName` / `SubZoneName` as
  soft names and owns no geometry, containment, or spatial hierarchy.
- **Not plants.** `plants-api` owns what *Solanum lycopersicum* is. Garden owns `tomato`, a
  cultivated category. Many-to-many, and the link is an optional soft name — a crop is not
  a taxon and this module does not use the `taxonomy` kernel.
- **Not soil.** No measurements, no lab values, no CEC. Garden supplies the requirement
  side and never stores an observed value.
- **Not the owner of soil-management events.** `AmendmentEvent`, `IrrigationEvent`,
  `TillageEvent` change soil state and stay in `soil.event`. Garden owns the crop
  lifecycle.
- **Not where assessment happens.** Applying a crop's requirements over a soil profile's
  measurements produces a status. Garden and soil are peers and neither may depend on the
  other, so that computation belongs to a module above both.

## Planting ↔ soil analysis: correlation, not a foreign key

A `Planting` carries **no `SoilProfileName`**. The link between a planting and a
crop-scoped `LabAnalysisInfo` is inferred from `zoneName` plus the date window — a bed
holds many plantings over time against one soil profile, and a second spatial key could
disagree with `zoneName`. Decided 2026-08-14; revisit only if a consumer needs the join to
be exact rather than inferred.

## Cross-domain references — typed names only

`garden-api` imports no other domain's api. Every edge is a soft reference through
`domains/identifiers`.

| Reference | Direction | Type |
|---|---|---|
| `ZoneName`, `SubZoneName` | garden → zone | `EntityName` |
| `PlantName` | garden → plants | `EntityName` |
| `CropName` | soil → garden (reverse) | `EntityName` |

`CropName`, `CultivarName` and `PlantingId` live in `com.naturalist.garden` under
`domains/identifiers`. `CropName` moved there from `com.naturalist.soil` on 2026-08-14 —
soil was simply built first.

## Conventions

Inherited from soil unless stated otherwise: the `*Info` / bare-noun split, ADR-020's N=1
collapse (top-level package-private repository, top-level public query, no namespace
wrappers), `for*` on queries and `getBy*` on repositories, and a package-private concrete
aggregate factory in `garden-core` that is never declared in the api module.

Identity types are named for the concept, not the record — `CropInfo` is keyed by
`CropName`, not `CropInfoName`.

Read-only, like soil: no command surface.

## Fixture data — real Oak Vista plantings

| Crop | Cultivars | Zone | Planted | Removed |
|---|---|---|---|---|
| `tomato` | `amish-paste`, `san-marzano` | `box-1`, `backyard` | spring 2026 | Aug 2026 |
| `lettuce` | — | `box-1`, `backyard` | winter 2026–27 | active |

These correspond to the two `SoilProfileInfo` fixtures and the two FGL analyses, so the
planting ↔ analysis correlation is exercisable in tests with no cross-module import.
