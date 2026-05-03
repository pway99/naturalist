# Compound Acquisition Briefing

Use this document as the system prompt / first message when asking a chat
assistant (Claude.ai, ChatGPT, etc.) to produce compound entries for the
chemistry catalog. The output is a JSON array of one or more compounds that
can be appended directly to
`domains/chemistry/chemistry-repository-test/src/main/resources/chemistry/compound/compounds.json`.

---

## Goal

Produce one or more `Compound` JSON entries matching the schema below, for
the substance(s) named at the end of this brief.

Example invocation: *"Acquire `aristolochic-acid-i` and
`aristolochic-acid-ii`."*

## Authoring Rules

1. **Top-level shape is an array of objects.** Always emit valid JSON, even
   for a single compound.
2. **Slug naming.** `name` is a kebab-case slug — lowercase, ASCII, hyphen
   separated. Roman numerals become digits or lowercase letters consistently
   (`aristolochic-acid-i`, `aristolochic-acid-ii`). Avoid punctuation.
3. **No `id` field.** Compounds carry no persistence identifier.
4. **Field order** must match the schema below for diff readability.
5. **Use real data.** Cite IUPAC, PubChem, ECHA, USDA, ChemSpider, peer
   review where relevant. If a value is unknown, leave the *parent profile*
   `null` rather than fabricating numbers — `volatilization` and `safety` are
   the only nullable profile slots.
6. **Common name uniqueness.** `commonName` must be globally unique across
   the catalog. Disambiguate with parenthetical notes if needed
   (`"Aristolochic Acid I"`, not just `"Aristolochic Acid"`).
7. **Functional roles** must be a non-empty set. If a substance has no
   agronomic role, choose the closest match (`BIOLOGICAL_CATALYST`,
   `FERTILIZER`, etc.) and explain in the `properties` map.
8. **Constituent elements** are the IUPAC chemical symbols of the elements
   that appear in the molecular formula — symbols only, e.g. `["C","H","N","O"]`.
9. **Properties map** is the open-ended escape hatch. Use `String → String`
   only. Document hazards, regulatory status (OMRI, EPA, CDFA, IARC),
   pharmacology, source organism, etc.

---

## Compound JSON Schema

```jsonc
{
  "name": "<kebab-case-slug>",                      // CompoundName — required
  "commonName": "<Display Name>",                   // @UniqueValue String — required
  "compoundInfo": {
    "formula": "<Hill-system formula>",             // String — required
    "molecularWeight": <number | null>,             // g/mol, scale 4
    "phCharacter": "<PhCharacter>",                 // required, see enum
    "chemicalNature": "<ChemicalNature>",           // required, see enum
    "physicalForm": "<PhysicalForm>",               // required, see enum
    "functionalRoles": [                            // non-empty Set
      { "kind": "<FunctionalRoleKind>" }
    ],
    "constituentElements": ["<Symbol>", ...]        // IUPAC symbols
  },
  "solubility": {                                   // SolubilityProfile — required
    "gramsPerLiterAt20C": <number>,                 // Solubility, scale 2, ≥ 0
    "category": "<SolubilityCategory>",
    "ecContributionFactor": <number>,               // BigDecimal
    "notes": "<plain-language description>"
  },
  "bioavailability": {                              // BioavailabilityProfile — required
    "primaryPathway": "<AbsorptionPathway>",
    "relativeAbsorptionRate": <number>,             // 0.0 – 1.0
    "cuticular": <bool>,
    "stomatal": <bool>,
    "isChelateEnhanced": <bool>,                    // exact JSON name — do not rename
    "mechanism": "<plain-language description>"
  },
  "volatilization": null | {                        // VolatilizationProfile — nullable
    "minEffectiveTempF": <number>,
    "maxSafeTempF":      <number>,
    "optimalTempF":      <number>,
    "vaporPressureAt20C": <number>,
    "efficacyNotes": "<text>",
    "safetyNotes":   "<text>"
  },
  "safety": null | {                                // SafetyProfile — nullable
    "hazardLevel": "<HazardLevel>",
    "maxSafeConcentrationPpm": <number>,
    "minApplicationTempF": <number | null>,
    "maxApplicationTempF": <number | null>,
    "requiresProtectiveEquipment": <bool>,
    "hazardousToBeesWhenWet":     <bool>,
    "requiresEveningApplication": <bool>,
    "applicationConstraints": "<text>"
  },
  "properties": {                                   // Map<String,String> — required (may be empty)
    "<key>": "<value>"
  }
}
```

### Enumerations

`phCharacter` (`PhCharacter`)

- `STRONGLY_ACIDIC` (pH < 3)
- `ACIDIC` (pH 3–6)
- `NEUTRAL` (pH 6–8)
- `ALKALINE` (pH 8–10)
- `STRONGLY_ALKALINE` (pH > 10)

`chemicalNature` (`ChemicalNature`)

- `ORGANIC` — characterised by C–H bonds
- `INORGANIC` — includes carbonates, oxides, cyanides, pure carbon
- `ORGANOMETALLIC` — at least one carbon-to-metal bond

`physicalForm` (`PhysicalForm`)

- `ELEMENT`, `MINERAL`, `SALT`, `ACID`, `BASE`, `COMPLEX`
- Independent of `chemicalNature` — NaCl is `INORGANIC` + `SALT`.

`functionalRoles[].kind` (`FunctionalRole`)

- `CHELATOR`
- `FUMIGANT`
- `BIOLOGICAL_CATALYST`
- `FERTILIZER`
- `ACARICIDE`
- Set must be non-empty. Add multiple entries when the compound plays
  multiple roles. If none truly applies, pick the nearest analogue and
  document the choice in `properties`.

`constituentElements` — IUPAC symbols only. Examples:
`H, C, N, O, S, Cl, Ca, Mg, K, Na, Fe, Cu, Zn, P`. Symbol case matters
(`Ca`, not `CA`).

`solubility.category` (`SolubilityProfile.SolubilityCategory`)

- `INSOLUBLE`         (< 0.1 g/L)
- `SPARINGLY_SOLUBLE` (0.1 – 2.4 g/L)
- `SLIGHTLY_SOLUBLE`  (2.4 – 10 g/L)
- `SOLUBLE`           (10 – 100 g/L)
- `HIGHLY_SOLUBLE`    (> 100 g/L)
- `MISCIBLE`          (fully miscible)

`bioavailability.primaryPathway` (`BioavailabilityProfile.AbsorptionPathway`)

- `ROOT_MASS_FLOW` — carried to roots in transpiration stream
- `ROOT_DIFFUSION` — diffuses along concentration gradient
- `FOLIAR_STOMATAL` — enters through stomata
- `FOLIAR_CUTICULAR` — penetrates waxy cuticle
- `FOLIAR_BOTH` — stomatal + cuticular (typical for chelates)
- `VOLATILIZATION` — vapor phase (fumigants)
- `CONTACT` — kills/acts on contact, no systemic absorption

`safety.hazardLevel` (`SafetyProfile.HazardLevel`)

- `NONE`     — completely safe (gypsum, Epsom salt)
- `LOW`      — mild irritant (neem oil, insecticidal soap)
- `MODERATE` — irritant, harmful if ingested (oxalic acid)
- `HIGH`     — corrosive, dangerous vapor (formic acid)
- `EXTREME`  — reserved for severe acute toxins

### Numeric / units conventions

| Field                     | Unit                                       | Scale    |
|---------------------------|--------------------------------------------|----------|
| `molecularWeight`         | g/mol                                      | 4        |
| `gramsPerLiterAt20C`      | g/L at 20 °C                               | 2        |
| `ecContributionFactor`    | dS/m per g/L (approx)                      | flexible |
| `relativeAbsorptionRate`  | unitless 0.0 – 1.0                         | flexible |
| `vaporPressureAt20C`      | mmHg or kPa — note unit in `efficacyNotes` | flexible |
| `*TempF`                  | degrees Fahrenheit                         | flexible |
| `maxSafeConcentrationPpm` | ppm                                        | flexible |

### When to use `null`

- `compoundInfo.molecularWeight` — for chelate categories or biological
  complexes with no defined molar mass.
- `volatilization` — for non-volatile compounds (almost everything except
  fumigants).
- `safety` — for inert / non-hazardous compounds.
- `safety.minApplicationTempF` / `maxApplicationTempF` — when no temperature
  constraint applies.

Inside non-null profiles every field is required.

---

## Worked Example — Reference Entry

```json
{
  "name": "formic-acid",
  "commonName": "Formic Acid",
  "compoundInfo": {
    "formula": "HCOOH",
    "molecularWeight": 46.025,
    "phCharacter": "STRONGLY_ACIDIC",
    "chemicalNature": "ORGANIC",
    "physicalForm": "ACID",
    "functionalRoles": [
      { "kind": "FUMIGANT" },
      { "kind": "ACARICIDE" }
    ],
    "constituentElements": ["H", "C", "O"]
  },
  "solubility": {
    "gramsPerLiterAt20C": 1000.0,
    "category": "MISCIBLE",
    "ecContributionFactor": 0.0,
    "notes": "Fully miscible with water. Used as 65% pad strips for Varroa mite control."
  },
  "bioavailability": {
    "primaryPathway": "VOLATILIZATION",
    "relativeAbsorptionRate": 1.0,
    "cuticular": false,
    "stomatal": false,
    "isChelateEnhanced": false,
    "mechanism": "Vaporises from pad and penetrates Varroa tracheal system. Mite kill via respiratory acidification."
  },
  "volatilization": {
    "minEffectiveTempF": 50.0,
    "maxSafeTempF": 85.0,
    "optimalTempF": 65.0,
    "vaporPressureAt20C": 42.6,
    "efficacyNotes": "Vapor pressure climbs sharply above 75°F — efficacy improves but bee tolerance drops.",
    "safetyNotes": "Above 85°F brood loss and queen kill risk rise sharply."
  },
  "safety": {
    "hazardLevel": "HIGH",
    "maxSafeConcentrationPpm": 5.0,
    "minApplicationTempF": 50.0,
    "maxApplicationTempF": 85.0,
    "requiresProtectiveEquipment": true,
    "hazardousToBeesWhenWet": false,
    "requiresEveningApplication": false,
    "applicationConstraints": "Apply in temperate window only — wear acid-rated gloves and respirator. Do not seal hive."
  },
  "properties": {
    "epaRegistered": "true",
    "varroaTreatment": "true",
    "padStrengthPercent": "65",
    "treatmentDurationDays": "21"
  }
}
```

---

## Output Contract

When you respond, output **only** the JSON array. No markdown fences, no
prose, no commentary. The array must parse cleanly with Jackson 2.19. Do not
include trailing commas. Do not include comments in the final JSON. Use
double-quoted keys and string values.

If you have low confidence in any value, prefer to set the *enclosing
profile* to `null` (where allowed) rather than emit a guess. Note the
omission in `properties` with a `"dataGap": "<what is missing and why>"`
key.

---

## Compounds to Acquire

Replace this section with the substances you want. Example:

> Acquire entries for **aristolochic acid I** and **aristolochic acid II**
> — natural-product nephrotoxins from *Aristolochia* spp. Capture IARC
> Group 1 classification, banned-substance regulatory status, and the
> structural difference between the two congeners (methoxy on AA-I,
> hydrogen on AA-II at the C-8 position) in the `properties` map.
