# NOTES — Insect Life-Stage Data Population

Companion to `insect-species.json`. Triage input for the follow-on PR.

---

## 1. Catalog-state mismatch — the 15 bare species entries

The task brief assumed every species had the extended schema
(`chemicalDefense`, `voltinism`, `habitatRequirements`, `gardenConnections`,
`beneficialProfile`, `ecologicalSignificance`, `identificationFeatures`,
`habitatProfile`) with `egg`/`larva`/`pupa`/`adult` as null stage fields ready
to populate. The actual catalog as uploaded has this shape only on
`pipevine-swallowtail`. The other 15 species are bare entries with only
`name`, `taxonomy`, `description`, `guilds`, `beneficial`, `sightingNotes`,
and no stage keys at all.

**What this deliverable does.** Only the four stage fields are added —
consistent with the task's "do not modify other species-level fields"
constraint. `pipevine-swallowtail` has its stage fields overwritten in
place (preserving the existing key order). The other 15 get
`egg`/`larva`/`pupa`/`adult` keys appended after their existing fields.

**What this deliverable does NOT do.** It does not add the missing
species-level fields to the 15 bare entries. The stage-level data is at
full richness regardless, drawn from domain knowledge — but there are
consequences:

- Stage `habitat.spatialNotes` can echo Oak-Vista-specific context for
  `pipevine-swallowtail` (Aristolochia trellis, April 2026 eggs,
  southeast-facing growth) because the species-level `sightingNotes`
  provides the detail. For the other 15 species, spatial notes are general
  ("mulched bed margins," "hedgerow leaf litter," etc.) rather than tied to
  specific Oak Vista plantings — the catalog has no such detail to echo.
- No `chemicalDefense.protectedStages` cross-check is applicable for the
  15 bare species because they have no `chemicalDefense` field. For
  `pipevine-swallowtail`, `protectedStages: [EGG, LARVA, PUPA, ADULT]`
  matches the four populated `chemistryRole` entries.
- Species-level `voltinism`, `habitatProfile`, `habitatRequirements`, and
  `ecologicalSignificance` are present only on `pipevine-swallowtail`, so
  only that species's stage data can draw on them.

**Recommendation.** The stage-level work has run ahead of the
species-level curation for 15/16 species. Plan the follow-on PR to fill in
the missing species-level fields before the next ADR layer touches the
catalog — otherwise the new aggregates will carry asymmetric richness.

---

## 2. `diapauseRegulation` — pending Jackson polymorphic wiring

All pupae carry `"diapauseRegulation": null` per the task instruction. The
intended content below should be populated once `@JsonTypeInfo` /
`@JsonSubTypes` (or an explicit discriminator property) is wired onto the
sealed `DiapauseRegulation` hierarchy.

### pipevine-swallowtail

```
FoodWaterContentRegulated(
  mechanism = "Pupal dormancy is regulated by the water content of the
    larval food (Aristolochia californica leaf hydration at pupation),
    not photoperiod. Uniquely among local swallowtails, Battus philenor
    is unresponsive to day length; mixed direct-developer and diapauser
    cohorts emerge from a single clutch because sibling larvae differ in
    accumulated food-water metrics at the pupation threshold.",
  cohortSplitNotes = "Every female's offspring include both
    direct-developers and diapausers. Some diapausing pupae eclose later
    the same year rather than overwintering, producing an August
    emergence bump. Site-level voltinism cannot be reduced to a single
    integer — at any given site it may be two, three, four broods, or
    all of these overlapping at once. Host plant typically stops
    growing in June and ceases providing oviposition sites thereafter;
    off-season breeding is triggered only by post-catastrophe host
    regeneration."
)
```

Source text drawn from the species's `voltinism.notes` and
`ecologicalSignificance.regionalContext`.

### Other species — candidate regulation types

Not emitted; candidates flagged for future work once species-level
curation fills in `voltinism` fields. Tentative guidance:

| Species | Likely DiapauseRegulation subtype | Reasoning |
|---|---|---|
| `tachinid-fly` | `PhotoperiodRegulated` or `TemperatureRegulated` | Family-level variable; overwinters as puparium. |
| `braconid-wasp` | `PhotoperiodRegulated` | Standard Aphidiinae / Microgastrinae overwintering. |
| `hoverfly` | `PhotoperiodRegulated` | Syrphinae typically photoperiod-cued at prepupal / pupal stage. |
| `convergent-ladybug` | `NonDiapausing()` (at pupal stage) | Hippodamia overwinters as adults in aggregations — pupal stage itself does not diapause. |
| `ground-beetle` | `PhotoperiodRegulated` or `TemperatureRegulated` | Family-level variable; many Carabidae overwinter as adults, not pupae. |
| `crane-fly` | `TemperatureRegulated` | Overwintering dominated by larval stage; short pupal window. |
| `native-sweat-bee` | `NonDiapausing()` (at pupal stage) | Halictus overwinter as mated foundresses, not pupae. |
| `grey-mining-bee` | `TemperatureRegulated` | Late-winter pupation cued by accumulated soil warmth. |
| `valley-carpenter-bee` | `NonDiapausing()` (at pupal stage) | Xylocopa overwinter as adults in galleries. |
| `skipper-butterfly` | `PhotoperiodRegulated` | Family-level pattern; larval overwintering is the norm — pupal diapause where it occurs is photoperiod-cued. |
| `painted-lady` | `NonDiapausing()` | Vanessa cardui has no documented diapause at any life stage; seasonal dynamics driven by migration. |
| `green-lacewing` | `PhotoperiodRegulated` or `NonDiapausing()` | Chrysoperla carnea group typically overwinters as adults; some species with pupal diapause. Genus-level resolution needed. |
| `orange-sulphur` | `NonDiapausing()` | Colias eurytheme has no robust pupal diapause at Central Valley temperatures; seasonal dynamics driven by migration/reimmigration. |

Hemimetabolous species (`field-roach`, `potato-leafhopper`) have no pupa
stage and therefore no `DiapauseRegulation` field to populate.

**Note on `NonDiapausing()` usage.** Per prior design discussion,
`NonDiapausing` is a positive subtype distinct from `null`. For species
whose pupal stage demonstrably does not diapause — either because
overwintering occurs at a different stage (adult, larva) or because no
diapause occurs at all — emit `NonDiapausing()`. Reserve `null` for
species whose pupal diapause regulation is undocumented at catalog level.

---

## 3. Plant and insect slugs referenced but unverified

The following plant slugs appear in stage-level `hostPlants` or
`nectarSources`. They are kebab-case binomial-derived per the task
constraint but have NOT been verified against the plant catalog. If the
plant catalog does not yet contain these entries, either add them or
adjust the stage references:

### Host plants (larval `hostPlants`)

- `aristolochia-californica` — California Pipevine (pipevine-swallowtail,
  monophagous)
- `cirsium-vulgare` — Bull thistle (painted-lady)
- `centaurea-solstitialis` — Yellow star thistle (painted-lady,
  also nectar for pipevine-swallowtail, skipper-butterfly,
  orange-sulphur)
- `malva-parviflora` — Cheeseweed mallow (painted-lady)
- `borago-officinalis` — Borage (painted-lady)
- `urtica-dioica` — Stinging nettle (painted-lady)
- `medicago-sativa` — Alfalfa (orange-sulphur)
- `trifolium-incarnatum` — Crimson clover (orange-sulphur)
- `trifolium-pratense` — Red clover (orange-sulphur)
- `vicia-villosa` — Hairy vetch (orange-sulphur)

### Adult nectar sources

- `raphanus-sativus` — Wild Radish
- `aesculus-californica` — California Buckeye
- `dichelostemma-capitatum` — Blue Dicks
- `triteleia-laxa` — Ithuriel's Spear
- `eriodictyon-californicum` — Yerba Santa
- `anethum-graveolens` — Dill
- `foeniculum-vulgare` — Fennel
- `daucus-carota` — Queen Anne's lace
- `achillea-millefolium` — Yarrow
- `lobularia-maritima` — Sweet alyssum
- `fagopyrum-esculentum` — Buckwheat
- `eschscholzia-californica` — California poppy
- `calendula-officinalis` — Calendula
- `cosmos-bipinnatus` — Cosmos
- `asclepias-speciosa` — Showy milkweed
- `monardella-villosa` — Coyote mint
- `origanum-vulgare` — Oregano
- `verbena-lilacina` — Lilac verbena
- `lantana-camara` — Lantana
- `salix-lasiolepis` — Arroyo willow
- `brassica-rapa` — Field mustard
- `prunus-armeniaca` — Apricot
- `salvia-clevelandii` — Cleveland sage
- `salvia-apiana` — White sage
- `lavandula-angustifolia` — English lavender
- `diplacus-aurantiacus` — Sticky monkeyflower
- `wisteria-floribunda` — Japanese wisteria
- `eriogonum-fasciculatum` — California buckwheat

### Parasitoid hosts

- **None populated.** `tachinid-fly` and `braconid-wasp` carry
  `parasitoidHosts: []`. Task brief advised against inventing `aphid`
  as a slug — real aphid species catalog entries don't exist yet (a
  deferred cataloging question: is "aphid" a taxonomic group, or do we
  intend per-species Aphididae entries?). Lepidopteran hosts for
  tachinid are broad and family-level; resolving to specific slugs
  requires either the catalog's policy on family-level catalog entries
  or a position on synthetic group-level identifiers.

**Recommendation.** Audit the full plant-slug set above against the
plants catalog. Several of these (especially the California natives and
the garden-specific nectar plants like Wild Radish, Blue Dicks, Yerba
Santa) are very likely to be absent and need catalog entries added.

---

## 4. Cross-stage invariant tension

### pipevine-swallowtail

- **Chemistry chain** is fully coherent: `MATERNAL_TRANSFER` (egg) ←
  `EXPRESSION` (adult) and `RETENTION` (pupa) ← `ACQUISITION` (larva).
- **`chemicalDefense.protectedStages`** in the catalog is `[EGG, LARVA,
  PUPA, ADULT]`, matching the four populated `chemistryRole` entries.
  **Invariant satisfied.**
- **Voltinism ↔ pupal diapause.** `voltinism.pattern = INDETERMINATE`
  aligns with the split-cohort pupa phenology (two windows: `direct-
  developer` and `diapauser` with a wrap-around tail at `--03-31`).
  The narrative is consistent across `voltinism.notes`, the pupa's
  `adaptiveSignificance`, and the (future) `diapauseRegulation`.
  **Invariant satisfied.**

### crane-fly

- **Wrap-around larval window** emitted: `--04-01 → --03-31`.
  Leatherjackets overwinter as larvae in many species; this reflects
  the typical two-season larval development pattern. Framework gap
  noted in the task brief — `ActivityWindow` does not enforce
  `onset <= tail`. Flagging for awareness.

### painted-lady

- **Phenology reflects migratory status**, not local overwintering.
  Spring influx from Mexico / desert-southwest begins at `--03-15`;
  the catalog's adult window `--03-15 → --11-15` spans both migratory
  arrivals and locally-bred summer generations. No winter-resident
  population assumed.

### potato-leafhopper, orange-sulphur

- Both species have **seasonal re-immigration** dynamics rather than
  robust local overwintering. Phenology windows reflect the observable
  Central Valley flight season, not a strict resident population.
  Species-level `voltinism` would clarify this if present.

### convergent-ladybug

- **April 2026 breeding confirmed at Oak Vista** (per the task brief's
  per-species guidance). Egg and larva stage `spatialNotes` reference
  the April 2026 sighting. The species-level `sightingNotes` for
  `convergent-ladybug` in the current catalog is a plain generic
  description ("small red beetle with black spots") — consider
  enriching it to note the April 2026 breeding record if that detail
  is canonical at catalog level.

---

## 5. Scope notes

- **No prey fields.** Predatory larvae (`hoverfly`, `convergent-ladybug`,
  `ground-beetle`, `green-lacewing`) carry `feedingStrategy: "PREDATORY"`
  and nothing more. Predatory adults (`ground-beetle`) likewise carry
  only `feedingHabit: "PREDATORY"`. Prey relationships are deferred to
  the future ecology domain per prior design discussion; not emitted
  here.
- **No nymph data.** Hemimetabolous species (`field-roach`,
  `potato-leafhopper`) have `larva: null` and `pupa: null`. Nymphs are
  not modeled as a distinct stage; nymphal biology is covered in the
  stage-level descriptions of `egg` (hatching, clutch behavior) and
  `adult` (developmental context, feeding damage).
- **Stage-level descriptions** respect the four-level Durrell register
  and are narrower in scope than the species-level descriptions —
  focused on that stage specifically, not recapitulating species-level
  facts.

---

## 6. Suggested follow-on work order

1. Fill in missing species-level fields (`chemicalDefense`,
   `voltinism`, `habitatProfile`, `habitatRequirements`,
   `gardenConnections`, `beneficialProfile`, `ecologicalSignificance`,
   `identificationFeatures`) for the 15 bare species.
2. Add the plant catalog entries identified in §3.
3. Land `@JsonTypeInfo` / `@JsonSubTypes` on `DiapauseRegulation` sealed
   hierarchy.
4. Populate `diapauseRegulation` on all pupae per §2 guidance.
5. When the ecology domain is scoped, retrofit `PreyReference`
   relationships for the predatory species.
