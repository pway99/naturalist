# Plants Domain

## Domain Vocabulary

**PlantSpecies** — Aggregate root. Botanical identity — taxonomy, growth form, lifecycle.
Instances in `species.json`.

**Cultivar** — Entity. A named variety within a species.
Open-pollinated (breeds true) or F1 hybrid (does not breed true).

**SeedLineage** — Aggregate root. Generational history of a seed-saved variety.
Tracks selection criteria, source provenance, generation history.
Critical for the Italian Pear adaptation program.

**Heritage** — Sub-context within Plants module. Ethnobotanical dimension.
Nick's Italian Pear: 50+ year family selection in coastal California. Chico adaptation begins 2026.
IRREPLACEABLE — save seed annually from highest performers.

**BER (Blossom End Rot)** — Physiological disorder. Localised calcium deficiency at
developing fruit. NOT a disease. No cure once present. Prevention only.

**Calcium Transport** — Delivered exclusively via xylem mass flow driven by transpiration.
Foliar chelated calcium bypasses root limitation.

## Domain Model

**PlantSpecies** — Aggregate root. Botanical identity — taxonomy, growth form, lifecycle.
Instances in `species.json`.

**Cultivar** — Entity within PlantSpecies. A named variety.
May be open-pollinated (breeds true) or F1 hybrid (does not breed true).

**SeedLineage** — Aggregate root (separate from PlantSpecies). Generational history of a
seed-saved variety. Tracks selection criteria, source provenance, generation history.
Critical for the Italian Pear adaptation program.

## BER (Blossom End Rot)

BER is a physiological disorder — localised calcium deficiency at developing fruit.
**NOT a disease. No cure once present. Prevention only.**

Calcium is immobile in plant tissue — every new cell requires fresh delivery via xylem.
Interrupted by: water stress, soil Ca deficiency, coir binding, heat-induced stomatal closure.

**Oak Vista BER risk: HIGH**
- Very low soluble Ca (FGL CH 2671853)
- Coco coir in substrate binds Ca
- Chico summer heat induces stomatal closure
- Foliar chelated calcium (TPS CalMag OAC) bypasses root limitation

## Calcium Transport

Delivered exclusively via xylem mass flow driven by transpiration.
Foliar chelated Ca (organic acid chelation) bypasses the root-to-xylem pathway.
TPS CalMag OAC uses organic acid chelation — effective for foliar delivery.

## Heritage Sub-context (Nick's Italian Pear)

50+ year family selection in coastal California. Beginning Chico adaptation program 2026.
**IRREPLACEABLE — save seed annually from highest performers.**
`SeedLineage` tracks: selection criteria, source provenance, generation history, performance notes.

## Open Design Questions

**Plants growth form vs category (Q5 from root CLAUDE.md):**
- `GrowthForm` enum: FRUIT_TREE, FRUIT_VINE, VEGETABLE_CROP, COVER_CROP, ORNAMENTAL_WOODY, POLLINATOR_PLANT
- `PlantRole` as many-to-many (borage = POLLINATOR + COMPANION)
- Entity model sketched, no Java written yet

## Oak Vista Species (test fixture data)

Key species for fixtures:
- Oh Henry Peach — 650 chill hours required (reliably met in Chico)
- Shinseiki Asian Pear — 250–300 chill hours required (reliably met)
- Borage — dual role: POLLINATOR + COMPANION
- Nick's Italian Pear — SeedLineage tracking required
