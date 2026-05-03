# Open Design Questions

Decisions in flight. Not loaded into conversation context — reference explicitly when working on the relevant question.

**Q0: Aggregate and the CatalogEntity/Entity model** — `Aggregate` sits orthogonal to the CatalogEntity/Entity identity
classification. A `Colony` (Apiary) or `NaturalistJournal` is an Aggregate (consistency boundary) but may also be a
`CatalogEntity` (stable, named, referenced). These concerns should compose cleanly, but the relationship between
`Aggregate`, `CatalogEntity`, and `Entity` needs an explicit ADR before Aggregate roots are implemented. Defer until
CatalogEntity/Entity implementation is complete.

**Q1: IrrigationEvent repository adapter** — PostgreSQL: standard table, TimescaleDB hypertable, or separate time-series
store. Current decision: deferred; in-memory adapter sufficient.

**Q2: Elemental Sulfur amendment type** — no dedicated `ELEMENTAL_SULFUR` type yet; placeholder uses potassium sulfate.
Add before writing nitrogen status computation.

**Q3: Nitrogen status computation** — needs biological amplification factor from Zone. Cross-module dependency (Soil →
Zones) is valid in DAG; should be mediated through a `ZoneService` port.

**Q4: Insects vs Apiary (RESOLVED)** — Apiary is its own domain module (Colony aggregate root). Insects module covers
Insecta (six legs). SHB control: H. indica only (not S. feltiae).

**Q5: Plants growth form vs category** — `GrowthForm` enum for tree/vine/etc. Proposed categories: FruitTree, FruitVine,
VegetableCrop, CoverCrop, OrnamentalWoody, PollinatorPlant. `PlantRole` as many-to-many. `SeedLineage` for the Italian
Pear adaptation program. Entity model sketched, no Java written.

**Q6: Custom WH51 calibration** — factory calibration is for mineral soil; Oak Vista worm casting/coco coir blend reads
3-5% high. AD values usable for custom calibration curve. Deferred pending gravimetric correlation study.

**Q7: Event-based production adapter** — append-only PostgreSQL tables for `AmendmentEvent`, `SensorReading`,
`IrrigationEvent` as a middle path. Deferred; hexagonal architecture makes this a swap of adapters.
