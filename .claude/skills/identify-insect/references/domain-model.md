# Insects Domain Model Reference

## InsectSpecies JSON Schema

```json
{
  "name": "<kebab-case-common-name>",
  "taxonomy": {
    "order": "<Order — capitalised>",
    "family": "<Family — capitalised>",
    "genus": "<Genus — capitalised, or null>",
    "species": "<species epithet — lowercase, or null>"
  },
  "genusName": "<kebab-case InsectGenus slug if the parent genus is catalogued, else null>",
  "familyName": "<kebab-case InsectFamily slug if the parent family is catalogued, else null>",
  "description": {
    "preschool": "...",
    "elementary": "...",
    "secondary": "...",
    "university": "..."
  },
  "guilds": ["<GUILD>"],
  "beneficial": true,
  "sightingNotes": "..."
}
```

### Field rules

- Domain records carry no persistence id (ADR-021) — there is no `id` field in catalog JSON
- `name` — kebab-case slug; this is the stable natural key used cross-entity.
  Examples: `"tachinid-fly"`, `"convergent-ladybug"`, `"potato-leafhopper"`
- `taxonomy.genus` and `taxonomy.species` — `null` when not determinable from a photo;
  never guess. Family-level ID is correct; spurious species epithets create false records.
- `guilds` — JSON array of guild constant strings (see Guild Reference below).
  A species may occupy multiple guilds, e.g. `["PREDATOR", "POLLINATOR"]`
- `beneficial` — `true` for parasitoids, predators, pollinators, decomposers, keystone species;
  `false` for phloem feeders, plant pests, and other guild members whose net garden impact
  is neutral or negative

---

## InsectImage JSON Schema

```json
{
  "name": "<UUID string — e.g. dfc3f072-0ee1-4a30-b81b-a4b8f4a9221b>",
  "insectSpeciesName": "<slug matching InsectSpecies.name exactly>",
  "dateAdded": "<ISO-8601 instant in UTC — e.g. 2026-04-17T22:39:00Z>",
  "resourceName": "<original filename including extension — e.g. IMG_9047.HEIC>"
}
```

### Field rules

- Domain records carry no persistence id (ADR-021) — there is no `id` or `insectSpeciesId` field
- `name` — UUID v4 string; each image record gets its own UUID; never reuse
- `insectSpeciesName` — must match `InsectSpecies.name` byte-for-byte; any mismatch
  will cause a runtime resolution failure
- `dateAdded` — ISO-8601 instant in UTC (`"2026-04-17T22:39:00Z"`); Jackson deserialises
  this as `java.time.Instant` with `JavaTimeModule`. Never use date-only format — it will
  fail deserialization
- `resourceName` — the filename as it exists in the `insects/images/` directory;
  include the extension; do not include the directory path

---

## FunctionalGuild Reference

| Constant        | Ecological role                                                                                                                                                          | Typical beneficial value |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `PARASITOID`    | Lays eggs in/on host; larva consumes host and kills it. Parasitic wasps, tachinid flies.                                                                                 | true                     |
| `PREDATOR`      | Directly consumes other insects. Ladybugs, lacewing larvae, assassin bugs, hoverfly larvae.                                                                              | true                     |
| `APEX_PREDATOR` | Generalist top-of-food-web predator. Ground beetles (Carabidae).                                                                                                         | true                     |
| `POLLINATOR`    | Transfers pollen; critical for fruit set. Bees, butterflies, adult hoverflies.                                                                                           | true                     |
| `DECOMPOSER`    | Breaks down organic matter; feeds the soil food web. Crane flies, field roaches.                                                                                         | true                     |
| `FOOD_WEB`      | Basal prey supporting vertebrate and arachnid predators; also used for plant-feeding insects that are controlled by natural enemies. Aphids, leafhoppers, scale insects. | false (typically)        |
| `MIGRATORY`     | Seasonal visitor; population regulated at landscape scale, not locally. Painted lady.                                                                                    | true                     |
| `KEYSTONE`      | Disproportionate ecological impact beyond what guild membership implies. Use sparingly. Pipevine swallowtail (Battus philenor).                                          | true                     |

### Assignment guidance

- Assign all guilds that apply — a hoverfly is `["PREDATOR", "POLLINATOR"]`
- `FOOD_WEB` alone with `beneficial: false` = phloem/cell feeder that is prey for
  the beneficial community (leafhoppers, aphids, whitefly)
- `KEYSTONE` should accompany another guild when the keystone status comes from
  a specific relationship (e.g. a butterfly that is the sole pollinator of a plant)
- Do not invent new guild constants; map to the closest existing one

---

## Slug naming convention

The `name` field is the entity's natural key — it must be stable across versions:

- Use the well-known English common name in kebab-case
- Prefer the most specific common name that unambiguously identifies the taxon
- Family-level entries use the family common name: `"tachinid-fly"`, `"braconid-wasp"`
- Species-level entries: `"convergent-ladybug"`, `"potato-leafhopper"`, `"painted-lady"`
- When two common names are equally valid, prefer the one used in Western US / California
  gardening literature

### Existing catalog entries (as of 2026-04-17)

These slugs are already in `insects.json` — do not add duplicates:

```
tachinid-fly, braconid-wasp, hoverfly, convergent-ladybug, ground-beetle,
crane-fly, field-roach, native-sweat-bee, grey-mining-bee, valley-carpenter-bee,
skipper-butterfly, painted-lady, green-lacewing, pipevine-swallowtail, potato-leafhopper,
orange-sulphur
```
