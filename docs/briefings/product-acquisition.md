# Product Acquisition Briefing

Use this document as the system prompt / first message when asking a chat
assistant (Claude.ai, ChatGPT, etc.) to produce product entries for the
chemistry catalog. The output is a JSON object with **two arrays** — one for
products, one for any new compounds the products reference — that can be
appended directly to:

- `domains/chemistry/chemistry-repository-test/src/main/resources/chemistry/product/products-base.json`
- `domains/chemistry/chemistry-repository-test/src/main/resources/chemistry/compound/compounds.json`

A product is a commercial SKU (e.g. *Apiguard*, *TPS Nutrients CalMag OAC*,
*Bonide Rot-Stop RTU*) that contains one or more `Compound` formulations.
The product authoritatively defines its ingredient list; every compound it
references must already exist in `compounds.json` or be acquired in the
same response.

---

## Goal

Produce one or more `Product` JSON entries plus any `Compound` entries
required to satisfy referential integrity, for the SKU(s) named at the end
of this brief.

Example invocation: *"Acquire `apiguard-vetopharma` and any compounds it
needs."*

## Authoring Rules

1. **Top-level shape is a single object with two arrays.** Always emit
   valid JSON, even when only one product or zero new compounds are
   produced.

   ```jsonc
   {
     "products":  [ { ... }, ... ],
     "compounds": [ { ... }, ... ]
   }
   ```

   The `compounds` array is **required** but may be empty when every
   referenced compound already exists in the catalog.

2. **Slug naming.** `name` is a kebab-case slug — lowercase, ASCII, hyphen
   separated. Brand and manufacturer disambiguation goes in the slug
   (`apiguard-vetopharma`, `bonide-rot-stop-rtu`,
   `down-to-earth-0-0-50`). Avoid punctuation other than hyphens and
   digits.
3. **No `id` field.** Products and compounds carry no persistence
   identifier.
4. **Field order** must match the schemas below for diff readability.
5. **Use real data.** Cite the manufacturer label, product datasheet, EPA
   registration database, OMRI listings, CDFA registry, and peer review
   where relevant. If a value is unknown, omit the property rather than
   fabricating numbers — `properties` is `Map<String,String>` and may be
   empty.
6. **Display name uniqueness.** `displayName` must be globally unique
   across the catalog. Include the manufacturer in parentheses or as a
   prefix when two SKUs share a generic name
   (`"Apiguard (Véto-pharma)"`).
7. **Compounds set must be non-empty.** Every product references at least
   one `Compound` by `CompoundName` slug. If any of those compounds is
   not present in the existing `compounds.json`, acquire it under the
   `compounds` array of this response (schema in §3 below).
8. **Properties map** is the open-ended escape hatch for SKU-scoped
   attributes. Use `String → String` only. Document concentration,
   application rate, NPK, application window, EPA / OMRI / CDFA
   registration, package size, target pest, etc.

---

## 1. Product JSON Schema

```jsonc
{
  "name": "<kebab-case-slug>",                      // ProductName — required
  "displayName": "<Display Name>",                  // @UniqueValue String — required
  "compounds": ["<compound-slug>", ...],            // non-empty Set<CompoundName>
  "properties": {                                   // Map<String,String> — required (may be empty)
    "<key>": "<value>"
  }
}
```

### Recurring property keys

These keys appear repeatedly in the existing catalog — reuse them when
applicable so consumers can look up consistently. Add new keys freely when
nothing existing fits.

| Key                     | Meaning                                                               |
|-------------------------|-----------------------------------------------------------------------|
| `concentration`         | Active-ingredient concentration as labeled (e.g. `"1.6% CaCl2"`)      |
| `npk`                   | Nitrogen-Phosphorus-Potassium ratio for fertilizers (e.g. `"0-0-50"`) |
| `applicationMethod`     | RTU foliar spray, soil drench, broadcast granular, etc.               |
| `applicationRate`       | Per-area or per-volume rate as labeled                                |
| `optimumRange`          | Manufacturer-recommended rate band                                    |
| `epaRegistered`         | `"true"` / `"false"`                                                  |
| `omriListed`            | `"true"` / `"false"` — OMRI organic certification                     |
| `cdfaRegistered`        | `"true"` / `"false"` — California CDFA registration                   |
| `varroaTreatment`       | `"true"` for Varroa mite acaricide products                           |
| `targetPest`            | Free-text — `"Varroa destructor"`, `"Tetranychus urticae"`            |
| `treatmentDurationDays` | Integer number of days for in-hive treatments                         |
| `manufacturer`          | Brand owner (e.g. `"Véto-pharma"`, `"Bonide"`)                        |
| `dataSheet`             | URL of the canonical product datasheet                                |

---

## 2. When the Product References a New Compound

If a product references a compound that is **not** present in the existing
`compounds.json`, you must produce a full compound entry for it in the
same response under the `compounds` array. Use the schema and rules from
**`docs/briefings/compound-acquisition.md`** verbatim — that briefing is the
authoritative source of truth for compound JSON. The relevant constraints
in summary:

- Top-level fields: `name`, `commonName`, `compoundInfo`, `solubility`,
  `bioavailability`, `volatilization` (nullable), `safety` (nullable),
  `properties`.
- `commonName` is `@UniqueValue` — globally unique across the compound
  catalog.
- `compoundInfo.functionalRoles` is a non-empty set of
  `{ "kind": "<FunctionalRoleKind>" }` records.
- `volatilization` is `null` for non-volatile compounds; `safety` is
  `null` for inert compounds. Inside non-null profiles every field is
  required.
- Numeric / unit / scale conventions are listed in the compound briefing
  §"Numeric / units conventions".

If you are acquiring multiple products that share new compounds, list each
compound **once** in the `compounds` array — duplicates will fail
deserialization on the unique-name constraint.

---

## 3. Worked Example — Reference Entries

```json
{
  "products": [
    {
      "name": "apiguard-vetopharma",
      "displayName": "Apiguard (Véto-pharma)",
      "compounds": ["thymol"],
      "properties": {
        "concentration": "25% thymol gel",
        "applicationMethod": "Tray placed on top bars",
        "treatmentDurationDays": "28",
        "varroaTreatment": "true",
        "targetPest": "Varroa destructor",
        "epaRegistered": "true",
        "manufacturer": "Véto-pharma"
      }
    },
    {
      "name": "formic-acid-mite-treatment",
      "displayName": "Formic Acid Mite Treatment (generic)",
      "compounds": ["formic-acid"],
      "properties": {
        "concentration": "65% formic acid pad",
        "applicationMethod": "Pad placed on top bars",
        "treatmentDurationDays": "21",
        "varroaTreatment": "true",
        "targetPest": "Varroa destructor",
        "minApplicationTempF": "50",
        "maxApplicationTempF": "85"
      }
    }
  ],
  "compounds": []
}
```

When the product introduces a brand-new compound, the second array is
populated:

```json
{
  "products": [
    {
      "name": "example-new-product",
      "displayName": "Example New Product",
      "compounds": ["hypothetical-acid"],
      "properties": { "manufacturer": "Acme" }
    }
  ],
  "compounds": [
    {
      "name": "hypothetical-acid",
      "commonName": "Hypothetical Acid",
      "compoundInfo": { /* full schema per compound-acquisition.md */ },
      "solubility":      { /* ... */ },
      "bioavailability": { /* ... */ },
      "volatilization":  null,
      "safety":          null,
      "properties":      {}
    }
  ]
}
```

---

## 4. Output Contract

When you respond, output **only** the JSON object with the `products` and
`compounds` keys. No markdown fences, no prose, no commentary. The object
must parse cleanly with Jackson 2.19. Do not include trailing commas. Do
not include comments in the final JSON. Use double-quoted keys and string
values.

If you have low confidence in any value, **omit the property** (the
`properties` map can drop keys without breaking deserialization) rather
than emit a guess. For low-confidence compound fields, follow the compound
briefing's `dataGap` convention — set the enclosing profile to `null`
where allowed and note the omission in `properties` with
`"dataGap": "<what is missing and why>"`.

---

## 5. Referential Integrity Checklist

Before finalising the response, verify:

- [ ] Every entry in `products[*].compounds` either exists in the current
  `compounds.json` or is present in `compounds[*].name` of this
  response.
- [ ] Every `products[*].name` and `compounds[*].name` is a kebab-case
  slug, unique within its array.
- [ ] Every `products[*].displayName` and `compounds[*].commonName` is
  unique within the existing catalog. Disambiguate with manufacturer
  or qualifier where needed.
- [ ] No `id` fields on either products or compounds.
- [ ] Field order matches the schemas in §1 (products) and the compound
  briefing (compounds).

---

## 6. Products to Acquire

Replace this section with the SKUs you want. Example:

> Acquire entries for **Apiguard (Véto-pharma)** and **Mite Away Quick
> Strips (NOD Apiary)**. Both are Varroa acaricide products. Apiguard's
> active ingredient (`thymol`) already exists in `compounds.json`; MAQS
> uses `formic-acid` which also already exists, so the `compounds` array
> should be empty. Capture EPA registration status, treatment duration,
> manufacturer, and labeled application temperature window in the
> `properties` map.
