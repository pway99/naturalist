# Insect Life Stage Acquisition Briefing

**Your role.** You have rich ecological / entomological domain knowledge.
This task uses that knowledge to produce structured life-stage data for
species in the Oak Vista insect catalog — either populating stages on
existing species whose four stage fields are still `null`, or producing
stage entries when new species are added to the catalog.

**Inputs this briefing assumes are attached to this conversation:**

1. `framework-core.md` — framework, identity model, package
   locations, JSON conventions, anti-patterns. Read that first.
2. `insects-domain.md` — insects-api shape and current catalog state. Pair
   with this briefing.
3. `insect-species.json` — the current insect-species catalog at
   `domains/insects/insects-repository-test/src/main/resources/insects/insect-species.json`.
   Each species entry exposes `egg`, `larva`, `pupa`, `adult` directly on
   the record; your job is to populate the requested stage fields.

**Deliverable.** A single zip file containing a drop-in replacement
`insect-species.json` with the four stage fields populated per species.
Do not rename the file; do not change any field other than the four stage
fields. Do not modify other species-level fields
(`taxonomy`, `description`, `guilds`, `beneficial`, `sightingNotes`,
`identificationFeatures`, `chemicalDefense`, `voltinism`, `habitatProfile`,
`habitatRequirements`, `gardenConnections`, `beneficialProfile`,
`ecologicalSignificance`) — they're already curated.

---

## 1. Stage Model — Structural Overview

Each species has four nullable stage fields: `egg`, `larva`, `pupa`,
`adult`. Each populated stage is a `NamedEntity` with composite slug
identity and rich structured content.

### Which stages each species has — biology rules

| Metamorphosis                   | Orders                                                    | Fields populated                                                                      |
|---------------------------------|-----------------------------------------------------------|---------------------------------------------------------------------------------------|
| **Holometabolous** (complete)   | Lepidoptera, Coleoptera, Diptera, Hymenoptera, Neuroptera | `egg`, `larva`, `pupa`, `adult`                                                       |
| **Hemimetabolous** (incomplete) | Blattodea, Orthoptera, Hemiptera                          | `egg`, `adult`. **`larva` and `pupa` stay null.** Nymphs are not modelled as a stage. |

Apply this strictly. A `larva` entry for a hemimetabolous species (e.g.
`field-roach`, `potato-leafhopper`) is a domain-model error.

### Cataloging-state rule

Any stage field may be `null` if the stage is simply undocumented at
catalog level. Prefer populated stages — chat has the knowledge — but it is
better to emit `null` than to fabricate data you cannot ground. Explicitly
flag any species where knowledge is thin.

---

## 2. Composite Identity — `LifeStageName`

Every populated stage carries a `name` of the form:

```
{species-slug}-{stage-kind-slug}
```

where `stage-kind-slug` is one of `egg`, `larva`, `pupa`, `adult`.
Examples:

- `battus-philenor-larva`
- `green-lacewing-egg`
- `convergent-ladybug-adult`

Slug regex: `^[a-z0-9]+(-[a-z0-9]+)*$`. Kebab-case, no underscores, no
capitals. The species slug must exactly match the parent species's `name`
field (`name.value()` on the existing `InsectSpeciesName`).

---

## 3. Common Stage Fields — required on every populated stage

```json
{
  "name": "<species-slug>-<stage-kind>",
  "phenology": { ... },
  "habitat": { ... },
  "chemistryRole": null | { ... },
  "description": {
    "preschool": "...",
    "elementary": "...",
    "secondary": "...",
    "university": "..."
  }
}
```

### `description` — four-level Durrell description (REQUIRED, all four levels non-null)

- **Preschool (age <6).** Wonder, direct sensory observation. No jargon,
  no mechanisms. One or two short sentences per level is fine.
- **Elementary (age 6–11).** Simple ecological relationship. Cause and
  effect without biochemistry or Latin.
- **Secondary (age 12–18).** Mechanism and process. Scientific names in
  context. Observable evidence.
- **University (age 18+).** Precise scientific terminology, taxonomic
  placement, physiological mechanisms, quantitative ecology, Oak Vista
  management relevance.

All four levels are simultaneously true — additive, not contradictory. The
existing species-level `description` blocks in `insect-species.json` are
the reference for tone and depth. Per-stage descriptions are narrower in
scope (this stage specifically) but at the same register.

### `phenology` — `StagePhenology` (REQUIRED, non-empty windows)

```json
{
  "windows": [
    {
      "onset": "--04-01",
      "peak":  "--06-15",
      "tail":  "--10-15",
      "cohortLabel": null
    }
  ],
  "notes": "Optional narrative — null if none."
}
```

- `windows` is a non-empty list of `ActivityWindow`. Multi-window supports
  multi-peak flights and split cohorts (diapauser vs. direct-developer).
- `MonthDay` serializes as `"--MM-dd"` (ISO-8601 with two leading hyphens).
- `onset` and `tail` are required on each window; `peak` and `cohortLabel`
  are nullable.
- Use `cohortLabel` only when multiple windows describe semantically
  distinct cohorts (e.g. `"direct-developer"` vs. `"diapauser"` for
  *Battus philenor* pupae) — not for multi-peak flights of the same
  cohort.
- **Known framework gap:** `ActivityWindow` does not enforce
  `onset <= tail` because `MonthDay` comparison doesn't handle wrap-around
  (overwintering windows span Dec→Mar). Emit wrap-around windows when
  biologically correct (overwintering adults, pupae). The user is aware.

### `habitat` — `StageHabitat` (REQUIRED)

```json
{
  "profile": {
    "zones": ["CULTIVATED", "WOODLAND_EDGE"],
    "moisture": "MESIC",
    "light": "PARTIAL_SUN",
    "layers": ["HERBACEOUS_LAYER"]
  },
  "substrate": "Optional narrative — null if none.",
  "microclimate": "Optional narrative — null if none.",
  "spatialNotes": "Optional narrative — null if none."
}
```

`profile` is required; narrative fields are nullable. **`profile.zones`
must be non-null and non-empty.** `profile.moisture`, `profile.light`,
`profile.layers` are nullable (documented incrementally).

**Enum vocabularies (use exact constant names):**

- `HabitatZone`: `MEADOW`, `BARE_GROUND`, `CULTIVATED`, `HEDGEROW`,
  `WOODLAND_EDGE`, `WOODLAND`, `CHAPARRAL`, `WETLAND`, `RIPARIAN`,
  `COMPOST_HEAP`
- `MoistureRegime`: `XERIC`, `MESIC`, `HYDRIC`, `SEASONALLY_XERIC`
- `LightRegime`: `FULL_SUN`, `PARTIAL_SUN`, `DAPPLED`, `FULL_SHADE`
- `VerticalLayer`: `CANOPY`, `UNDERSTORY`, `SHRUB_LAYER`,
  `HERBACEOUS_LAYER`, `GROUND_SURFACE`, `SUBTERRANEAN`

Narrative `substrate`, `microclimate`, `spatialNotes` capture
insect-specific detail the structured profile cannot express — leaf
underside, silk girdle, compost pile internal temperature band.

### `chemistryRole` — `StageChemistryRole` (nullable)

```json
{
  "role": "ACQUISITION",
  "notes": "Larval sequestration of aristolochic acids from Aristolochia californica."
}
```

`role` vocabulary: `ACQUISITION`, `RETENTION`, `EXPRESSION`,
`MATERNAL_TRANSFER`. Populate only on chemically defended species. For
*Battus philenor* the full story is:

- larva → `ACQUISITION`
- pupa → `RETENTION`
- adult → `EXPRESSION`
- egg → `MATERNAL_TRANSFER`

If a species has `chemicalDefense: null`, every stage's `chemistryRole`
should also be `null`. If `chemicalDefense` is populated but the chemistry
story is not documented per-stage, leave `chemistryRole: null` on the
stages and rely on `chemicalDefense.protectedStages` to express the set.

---

## 4. Stage-specific Fields

### `egg` — `EggStage`

```json
{
  "name": "...-egg",
  "phenology": { ... },
  "habitat": { ... },
  "chemistryRole": null | { ... },
  "description": { ... four-level ... },
  "colorProgression": "Optional — null if none.",
  "layingPattern": "Optional — null if none.",
  "adaptiveSignificance": "Optional — null if none."
}
```

Narrative fields capture: colour shift through development
(`colorProgression`), laying behavior / clutch arrangement
(`layingPattern`), and catalog-level adaptive fact (`adaptiveSignificance`
— e.g. Chrysoperla's silk-stalked eggs preventing sibling cannibalism).

### `larva` — `LarvaStage` (holometabolous species only)

```json
{
  "name": "...-larva",
  "phenology": { ... },
  "habitat": { ... },
  "chemistryRole": null | { ... },
  "description": { ... four-level ... },
  "feedingStrategy": "PHYTOPHAGOUS" | "PREDATORY" | "PARASITOID" | "DETRITIVORE" | "OMNIVOROUS" | null,
  "hostPlants": ["plant-slug-1", "plant-slug-2"],
  "parasitoidHosts": ["insect-species-slug-1"],
  "remarkableBehavior": "Optional — null if none.",
  "instarProgression": "Optional — null if none."
}
```

- `hostPlants` is a **non-null** `List<PlantName>`. Each element is a slug
  string (JSON wire form). Empty list is valid for non-phytophagous
  larvae. Use canonical kebab-case plant slugs — e.g.
  `"aristolochia-californica"`, `"medicago-sativa"`,
  `"trifolium-incarnatum"`.
- `parasitoidHosts` is a **non-null** `List<InsectSpeciesName>`. Slug
  strings. Populate for parasitoid larvae (braconid, tachinid,
  ichneumonid, some chalcids). Empty list otherwise.
- **No prey field.** Predatory larvae carry `feedingStrategy: "PREDATORY"`
  and nothing more — actual prey lists are a future ecology-domain
  concern. Do NOT invent a `prey` field.
- `instarProgression`: narrative of instar count, duration, or color
  shifts per instar.
- `remarkableBehavior`: catalog-level behavioral facts (debris-carrying
  camouflage, leaf-rotation in *Battus philenor*, tent-building).

### `pupa` — `PupaStage` (holometabolous species only)

```json
{
  "name": "...-pupa",
  "phenology": { ... },
  "habitat": { ... },
  "chemistryRole": null | { ... },
  "description": { ... four-level ... },
  "appearance": "Optional — null if none.",
  "diapauseRegulation": null,
  "adaptiveSignificance": "Optional — null if none."
}
```

- `appearance`: colour, pattern, polymorphism (Battus philenor chrysalis
  is dimorphic brown/green with golden filigree).
- `adaptiveSignificance`: species-level pupal adaptations — split
  diapause, cryptic vs. aposematic.

#### `diapauseRegulation` — polymorphic sealed type (KNOWN JACKSON ISSUE)

The `DiapauseRegulation` sealed interface has four permitted records:

- `PhotoperiodRegulated(criticalDaylength, chillRequirement)`
- `FoodWaterContentRegulated(mechanism, cohortSplitNotes)` ← the *Battus
  philenor* case
- `TemperatureRegulated(entryThreshold, exitThreshold)`
- `NonDiapausing()` ← positively documented absence, distinct from null

**Jackson polymorphic wiring (`@JsonTypeInfo` / `@JsonSubTypes`) is NOT
yet in place on the Java source.** For this first data pass:

- Emit `"diapauseRegulation": null` on every pupa.
- In a README-style note at the top of your response (outside the zip),
  list the species where diapause regulation IS documented and tell the
  user: "Once `DiapauseRegulation` gains `@JsonTypeInfo`, the following
  species need a populated `diapauseRegulation`: X, Y, Z." For *Battus
  philenor* specifically, include the expected content:
  `FoodWaterContentRegulated` with mechanism narrative drawn from the
  species-level `voltinism.notes` and `ecologicalSignificance.regionalContext`.

This keeps the deliverable loadable today and flags the follow-on work.

### `adult` — `AdultStage`

```json
{
  "name": "...-adult",
  "phenology": { ... },
  "habitat": { ... },
  "chemistryRole": null | { ... },
  "description": { ... four-level ... },
  "feedingHabit": "NECTAR" | "SAP" | "HONEYDEW" | "POLLEN" | "PREDATORY" | "HEMATOPHAGOUS" | "NON_FEEDING" | "OMNIVOROUS" | null,
  "nectarSources": ["plant-slug-1", "plant-slug-2"],
  "ecologicalRole": "Optional — null if none.",
  "lifespan": "Optional — null if none."
}
```

- `nectarSources` is a **non-null** `List<PlantName>`. Populate only when
  `feedingHabit` is `NECTAR` (or when nectar is documented among other
  feeding modes). Empty list otherwise. Non-nectar feeding is NOT
  expressed here — no `sapSources`, no `preyTargets`. Keep this field
  strictly nectar-typed.
- `feedingHabit`: the dominant adult feeding mode. `NON_FEEDING` applies
  to adult mayflies and some moths with vestigial mouthparts.
- `ecologicalRole`: one-line function at Oak Vista — "Pollinator; reproductive
  stage", "Apex invertebrate predator", "Specialist parasitoid".
- `lifespan`: narrative — "a few days", "approximately one month",
  "overwintering adults 6–8 months".

---

## 5. Cross-stage Consistency — Invariants to Respect

Enforce these when composing multi-stage data for one species:

1. **Metabolous-type consistency.**
    - Holometabolous species (Coleoptera, Diptera, Hymenoptera, Lepidoptera,
      Neuroptera): `larva` and `pupa` are populated (or explicitly null with
      a reason flagged).
    - Hemimetabolous species (Blattodea, Orthoptera, Hemiptera): `larva` and
      `pupa` MUST be null. Never emit them.

2. **Chemistry-story coherence.**
    - `EXPRESSION` (adult) requires upstream `ACQUISITION` in the larva or
      `MATERNAL_TRANSFER` into the egg.
    - `RETENTION` (pupa) requires upstream `ACQUISITION` in the larva.
    - `MATERNAL_TRANSFER` (egg) requires `EXPRESSION` in the parent's adult
      generation.
    - If the chemistry story is documented partially, leave stages `null`
      rather than breaking coherence.

3. **Species-level `chemicalDefense.protectedStages` must equal the set
   of stages carrying a non-null `chemistryRole`.** If they disagree, the
   stage data is wrong (or the species data needs updating — flag it,
   don't silently adjust).

4. **Voltinism ↔ pupal diapause.** `voltinism.pattern == INDETERMINATE`
   usually pairs with `FoodWaterContentRegulated` or similar cohort-split
   mechanisms. Keep the narratives consistent across
   `voltinism.notes`, `pupa.adaptiveSignificance`, and
   `pupa.diapauseRegulation` (when you're able to emit it).

---

## 6. Species in the Catalog — Per-species Guidance

Skim `insect-species.json` for full context. Summary:

| Slug                   | Order       | Metabolous | Notes for stage generation                                                                                                                                                                    |
|------------------------|-------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tachinid-fly`         | Diptera     | Holo       | Family-level; endoparasitoid larva on lepidopteran hosts. Populate `parasitoidHosts` with general categories or leave empty and use narrative.                                                |
| `braconid-wasp`        | Hymenoptera | Holo       | Aphidiinae + Microgastrinae. Larva parasitizes aphids (`parasitoidHosts: ["aphid"]` is NOT a real slug — flag as needing catalog entries; empty list is acceptable).                          |
| `hoverfly`             | Diptera     | Holo       | Aphidophagous larva; nectar/pollen adult. Classic dual-guild species.                                                                                                                         |
| `convergent-ladybug`   | Coleoptera  | Holo       | Hippodamia convergens. Breeds at Oak Vista April 2026. Full life cycle documented.                                                                                                            |
| `ground-beetle`        | Coleoptera  | Holo       | Family-level; larva predatory, soil-dwelling. Pupa in soil chamber.                                                                                                                           |
| `crane-fly`            | Diptera     | Holo       | Tipulidae; larvae are saprophagous leatherjackets in soil. Adults non- or minimally-feeding.                                                                                                  |
| `field-roach`          | Blattodea   | **Hemi**   | `larva: null`, `pupa: null`. Only `egg` and `adult`. Oothecal egg-carrying.                                                                                                                   |
| `native-sweat-bee`     | Hymenoptera | Holo       | Halictus sp. Solitary ground-nester.                                                                                                                                                          |
| `grey-mining-bee`      | Hymenoptera | Holo       | Andrena sp. Early-spring solitary bee.                                                                                                                                                        |
| `valley-carpenter-bee` | Hymenoptera | Holo       | Xylocopa varipuncta. Nests in dead wood. Sexually dimorphic adults.                                                                                                                           |
| `skipper-butterfly`    | Lepidoptera | Holo       | Hesperiidae family-level. Larval host: Poaceae.                                                                                                                                               |
| `painted-lady`         | Lepidoptera | Holo       | Vanessa cardui. Migratory — Oak Vista adults are spring migrants, not residents. Adjust `phenology` accordingly.                                                                              |
| `green-lacewing`       | Neuroptera  | Holo       | Chrysoperla sp. Silk-stalked eggs (notable). Predatory aphid-lion larva.                                                                                                                      |
| `pipevine-swallowtail` | Lepidoptera | Holo       | **Full treatment required.** Battus philenor. Keystone. Aristolochic acid chemistry across all four stages. Split diapause. Host-plant monophagy: `hostPlants: ["aristolochia-californica"]`. |
| `potato-leafhopper`    | Hemiptera   | **Hemi**   | `larva: null`, `pupa: null`. Only `egg` and `adult`.                                                                                                                                          |
| `orange-sulphur`       | Lepidoptera | Holo       | Colias eurytheme. Larval host: Fabaceae (crimson clover at Oak Vista → `trifolium-incarnatum`, alfalfa → `medicago-sativa`).                                                                  |

Count verify: 16 rows above. Cross-check against `insect-species.json`
when you write the zip — if the catalog has diverged, follow the catalog.

---

## 7. Quality Bar

- **Durrell register.** All four levels of `description` must read at the
  resolution established by the species-level descriptions already in
  `insect-species.json`. Match depth; don't flatten university-level into
  a glossed summary.
- **Typed references.** Plant and insect slugs must be plausible catalog
  entries (kebab-case binomial-derived). Do not invent `"alfalfa"` — use
  `"medicago-sativa"`. Do not invent `"aphid"` — leave the list empty
  and note the gap.
- **Oak Vista specificity.** Where the species-level fields name
  Oak-Vista-specific facts (dill border, crimson clover cover, April 2026
  sightings, Aristolochia californica planting), echo that context in
  the stage's `habitat.spatialNotes` or `description.university`. Do not
  paste the species-level `sightingNotes` into stages.
- **Real phenology.** Phenology windows must reflect California Central
  Valley / Chico climate. `MonthDay` values should be plausible — e.g.
  *Battus philenor* adult flight onset near `"--02-15"`, tail near
  `"--10-31"`, not generic "spring–autumn" bands.
- **Invariants first.** If you cannot satisfy the non-null invariants on a
  stage (phenology windows, habitat.zones, description four-level), emit
  `null` for the whole stage rather than a partial record.

---

## 8. Deliverable Format

A single zip file named `insect-lifestage-data.zip` containing:

1. `insect-species.json` — the drop-in replacement file. All 16 species
   entries preserved; only the four stage fields populated (or justified
   nulls).
2. `NOTES.md` — a brief (≤2 page) companion document listing:
    - Species where you emitted partial or null stages and why.
    - Species needing `diapauseRegulation` once Jackson polymorphic wiring
      is added (with the intended content for each).
    - Plant / insect slugs you referenced but were unable to verify are
      real catalog entries.
    - Any cross-stage invariant tension surfaced by the biology that the
      user should know about.

The user imports the zip. The `insect-species.json` replaces the catalog
file; `NOTES.md` is triage input for the follow-on PR.

---

## 9. What NOT to Do

- Do not modify species-level fields other than the four stage fields.
- Do not invent Java types, field names, or packages — use exactly the
  field names in §§3–4.
- Do not emit a `lifeStages` wrapper object — the old `LifeStages` value
  object was removed in the refactor that motivated this task. Stages are
  now four direct nullable fields on `InsectSpecies`.
- Do not invent prey fields on predatory larvae or adults. Prey
  relationships live in a future ecology domain; not your concern here.
- Do not emit `diapauseRegulation` values. Leave as `null`; list the
  intended content in `NOTES.md`.
- Do not generate nymph data for hemimetabolous species. `larva` and
  `pupa` stay null.
- Do not fabricate `MonthDay` precision beyond what the biology supports.
  If the species is family-level in `insect-species.json`, phenology is
  necessarily broader; reflect that.

Ready to proceed.
