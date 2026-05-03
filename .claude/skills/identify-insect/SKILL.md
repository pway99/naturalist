---
name: identify-insect
description: >
  Use this skill whenever the user wants to identify an insect from photos and catalog it
  in the insects module. Triggers on phrases like "identify this insect", "what bug is
  this", "add these insect photos to the catalog", "catalog this insect sighting", or any
  time the user provides image files alongside an insect question. Also triggers on
  explicit invocations like "identify-insect [folder] [files...]". This skill handles the
  complete workflow autonomously: vision-based identification, checking whether the species
  is already cataloged, creating a new InsectSpecies entry if needed (with four-level
  Durrell descriptions), and appending InsectImage records for every photo. Use it even
  if the user only says "add this to the insects catalog" -- don't ask them to do the steps
  manually.
---

# Identify Insect Skill

## Purpose

Given a folder and one or more image filenames, perform the full catalog workflow:

1. Read and visually identify the insect in the images
2. Check `insects.json` — add a new `InsectSpecies` entry if the species isn't there yet
3. Append `InsectImage` records to `insect-images.json` for every photo
4. Verify both files are valid JSON and report the result

Read `references/domain-model.md` for the complete entity schemas and guild reference
before writing any JSON. You will need it.

---

## Arguments

| Argument               | Description                                                                                                                                                                                                 | Example                                                                      |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `imageFolder`          | Path to the directory containing the images                                                                                                                                                                 | `domains/insects/insects-repository-test/src/main/resources/insects/images/` |
| `fileNames`            | Images to **read via vision** for identification. These are converted, visually analysed, and also cataloged as `InsectImage` records.                                                                      | `[IMG_9047.HEIC, IMG_9048.HEIC]`                                             |
| `catalogOnly`          | Images of the **same insect** that should be cataloged as `InsectImage` records **without** reading them through vision. Saves resources when the user already knows multiple photos show the same species. | `[IMG_9049.HEIC, IMG_9050.HEIC]`                                             |
| `observationTimestamp` | Local date-time of the observation in `America/Los_Angeles` timezone. Convert to UTC for the JSON `dateAdded` field.                                                                                        | `2026-04-17T15:39`                                                           |

**Resource conservation:** Only `fileNames` images go through HEIC conversion and vision
analysis. `catalogOnly` images are assumed to be the same species as the `fileNames` images
and get `InsectImage` records with the same `insectSpeciesName` — no conversion or vision
read is performed. Both lists produce image records in Step 5.

If `observationTimestamp` is not provided, ask the user for it — observation time matters
for correlating with flight periods, light conditions, and sensor temperature data. Do not
default to midnight.

---

## Canonical File Paths

All paths relative to the project root (`amateur-naturalist/`):

| File            | Path                                                                                    |
|-----------------|-----------------------------------------------------------------------------------------|
| Species catalog | `domains/insects/insects-repository-test/src/main/resources/insects/insects.json`       |
| Image catalog   | `domains/insects/insects-repository-test/src/main/resources/insects/insect-images.json` |
| Images dir      | `domains/insects/insects-repository-test/src/main/resources/insects/images/`            |

---

## Step 1 — Prepare Images for Vision

**Only process `fileNames` images in this step — skip `catalogOnly` images entirely.**
`catalogOnly` images are not converted or read; they are handled in Step 5 as image
records only.

Read each `fileNames` image with the Read tool. If any file is in **HEIC format** (Apple's
native camera format), it cannot be read directly — convert to JPEG first, then read the
JPEG:

```bash
# Convert HEIC to JPEG (tries sips first — always available on macOS hosts)
sips -s format jpeg "<imageFolder>/<filename>.HEIC" --out "/tmp/<filename>.jpg" 2>/dev/null \
  || convert "<imageFolder>/<filename>.HEIC" "/tmp/<filename>.jpg"
```

Always use the **original filename** (including extension) as `resourceName` in the JSON —
not the converted temp filename. The conversion is only for reading; the stored reference
is to the original file as it sits in the images directory.

Read all `fileNames` images before making your identification — more angles improve accuracy.

---

## Step 2 — Identify the Insect

Analyse all images together and determine:

- **Order**, **Family**, **Genus** (if determinable), **Species** (only if clearly
  identifiable from the photo — null otherwise)
- **Common name** → this becomes the slug (kebab-case, e.g. `"potato-leafhopper"`)
- **Functional guilds** (see domain-model.md — a species can hold multiple)
- **Beneficial** (true = net positive for garden; false = pest/neutral)
- **Host plant** if visible
- **Confidence level** — photo-id only vs. specimen-confirmed

When uncertain about species, leave `species: null` and document the uncertainty in
`sightingNotes`. An honest genus-level ID is always better than an overconfident species
call from a photograph.

---

## Step 3 — Check for Existing Species

Read `insects.json`. Look for an entry whose `name` field (the slug) matches the identified
insect's common name in kebab-case.

**If found → skip Step 4.** Note the existing slug for use in the image records.

**If not found → proceed to Step 4.**

Also check `insect-images.json` for any existing records whose `resourceName` matches
the files you are about to add — skip those files to avoid duplicates.

---

## Step 4 — Add New InsectSpecies (only if not in catalog)

Append a new entry to `insects.json`. Follow the schema in `references/domain-model.md`
exactly — field order, null values, and guild casing all matter for deserialization.

Write descriptions in the **Durrell pattern** — the same ecological truth at four
levels of understanding. A child reading the preschool description and a researcher
reading the university description should both come away with an accurate picture;
neither should feel talked down to or lost:

| Level        | Tone & length                          | What it covers                                                                                                                |
|--------------|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `preschool`  | Wonder-first, 2–3 sentences            | Appearance, what it does in plain words, why it matters to "our garden"                                                       |
| `elementary` | Mechanism, 3–5 sentences               | How it eats/hunts/helps, concrete numbers where available (aphids per day, etc.)                                              |
| `secondary`  | Biological accuracy, 4–6 sentences     | Family taxonomy, scientific mechanism, host specificity, ecological role, correct terminology                                 |
| `university` | Full scientific context, 5–8 sentences | Binomial + authority + year, morphological/behavioral detail, chemical ecology if relevant, Oak Vista management implications |

For `sightingNotes`: always include the observation date (today's date), location (Oak
Vista), host plant if visible, individual count, and any photo-id caveats.

---

## Step 5 — Add InsectImage Records

Create `InsectImage` records for **all** images — both `fileNames` and `catalogOnly`.
`catalogOnly` images use the same `insectSpeciesName` determined from the `fileNames`
identification in Step 2.

Generate one UUID per image file (total = `fileNames` count + `catalogOnly` count):

```bash
python3 -c "import uuid; [print(uuid.uuid4()) for _ in range(<N>)]"
```

Convert the `observationTimestamp` (local `America/Los_Angeles` time) to UTC:

```bash
python3 -c "
from datetime import datetime
from zoneinfo import ZoneInfo
local = datetime.fromisoformat('<observationTimestamp>')
local = local.replace(tzinfo=ZoneInfo('America/Los_Angeles'))
print(local.astimezone(ZoneInfo('UTC')).strftime('%Y-%m-%dT%H:%M:%SZ'))
"
```

Append one record per image to `insect-images.json`. See the schema in
`references/domain-model.md`. Key points:

- `name` → the generated UUID string
- `insectSpeciesName` → must exactly match the `name` field of the `InsectSpecies` entry
- `dateAdded` → ISO-8601 instant in UTC (`"2026-04-17T22:39:00Z"`). The Java type is
  `java.time.Instant` — never use date-only format (`"2026-04-17"`) as it will fail
  deserialization
- `resourceName` → original filename as it exists in the images directory

---

## Step 6 — Verify and Report

Validate both JSON files:

```bash
python3 -c "
import json
species = json.load(open('domains/insects/insects-repository-test/src/main/resources/insects/insects.json'))
images  = json.load(open('domains/insects/insects-repository-test/src/main/resources/insects/insect-images.json'))
print(f'insects.json: {len(species)} species — valid')
print(f'insect-images.json: {len(images)} images — valid')
"
```

Report back to the user:

- Whether the species was **new** (added) or **existing** (already in catalog)
- Species identified (common name + binomial if known)
- Number of images added
- Any identification caveats or confidence notes

---

## Edge Cases

**Ambiguous identity** — multiple possible species: identify to the lowest confident
taxon (genus or family), document the ambiguity in `sightingNotes` and the secondary/
university descriptions.

**Multiple species in the images** — if different images show different insects, process
each species separately. Group images by species before generating records.

**Image unreadable** — if conversion fails, note the filename in the report and skip
that image. Do not leave the JSON in a partial state — complete all other images first.

**Duplicate images already in catalog** — silently skip any `resourceName` already
present in `insect-images.json`. Report which files were skipped.
