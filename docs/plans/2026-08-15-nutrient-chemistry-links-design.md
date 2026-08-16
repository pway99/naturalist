# Nutrient → chemistry links (design)

**Date:** 2026-08-15
**Status:** shipped 2026-08-15 (PRs A–C)
**Surface:** `/soil/profiles/{name}` nutrient tables → `/chemistry/elements/{name}`

## The gap

The soil profile page renders 17 nutrient rows per lab analysis
([`nutrientTable.jte`](../../domains/soil/soil-console/src/main/jte/soil/nutrientTable.jte) →
[`readingRow.jte`](../../domains/soil/soil-console/src/main/jte/soil/readingRow.jte)), labelled
from `Nutrients.printedNameOf`. Every label is dead text. A reader who wants to know what
*calcium* is — its symbol, atomic weight, the ionic form roots actually take up — has no path
from the lab report to the chemistry catalog that already holds those facts.

There is also no destination to link to. `Element` is a real `NamedEntity<ElementName>` with
symbol, atomic weight, ionic form, and charge, but:

- `elements.json` has 10 entries; **sodium, zinc, manganese, iron, copper, and boron are
  missing**, and five of those six are micronutrients the lab reports.
- the chemistry console has no element route at all — only compounds and products.
  `ChemistryTestContext` does not expose `elementQuery()`, and
  [`ChemistryLinker`](../../domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/catalog/ChemistryLinker.java)
  has no `ElementName` case.
- `ChemistryCatalogContribution` contributes compounds and products, so elements are not
  even searchable.

Soil readings, by contrast, are complete: `nutrient-reading.json` carries values for all 17
nutrients.

## Ownership: soil declares the reference

A nutrient reading references either an element (calcium, boron) or, in principle, a compound.
The association could live on either side. It goes in **soil**, because the referencing domain
owns its outbound reference — the pattern `plants` already follows: `plants-api` holds
`PhytochemicalConstituent.compoundName` as a `CompoundName` from `identifiers`, `plants-core`
ships the back-reference provider, and `chemistry-api` knows nothing about plants.

| Fact | Owner | Why |
| --- | --- | --- |
| nitrate-N is a primary nutrient printed "Nitrate-Nitrogen" | soil (`Nutrients`, already) | FGL's reporting convention |
| nitrate-N's reading measures nitrogen, as an ion | **soil** (this change) | soil is making the reference |
| what nitrogen *is* — symbol, weight, ionic form | chemistry | substance identity |
| P₂O₅→P stoichiometry (×0.436) | chemistry, if ever needed | derivable from molar masses chemistry holds; **out of scope** — no page uses it |

`NutrientName`, `ElementName`, and `CompoundName` all live in `domains/identifiers`, and
`chemistry-api` already depends on `identifiers` and `catalog`. The crossing therefore needs
no new module dependency and creates no cycle.

The association table is **code, not an entity stack** — a third map in `Nutrients`, which
already holds `CATEGORIES` and `PRINTED_NAMES` for the same 17 slugs. A repository, mock,
contract test, and JSON catalog for a 17-row lookup with one caller is scaffolding without a
customer. Promote it to an entity the day a second lab's panel needs its own table.

## 1. Chemistry gains a destination

### Element test data

`elements.json` grows from 10 to 16:

| slug | symbol | atomic weight | ionic form | charge |
| --- | --- | --- | --- | --- |
| sodium | Na | 22.99 | Na+ | 1 |
| zinc | Zn | 65.38 | Zn2+ | 2 |
| manganese | Mn | 54.94 | Mn2+ | 2 |
| iron | Fe | 55.85 | Fe2+ | 2 |
| copper | Cu | 63.55 | Cu2+ | 2 |
| boron | B | 10.81 | H3BO3 | 0 |

Existing entries carry the *agronomically relevant* ion rather than the textbook default —
nitrogen is `NO3-`, sulfur is `SO4 2-`. These follow suit: iron as Fe²⁺ (the form roots take
up), boron as undissociated boric acid, which is what dominates at Oak Vista's pH 7.2. Boron's
charge of 0 is deliberate and correct: `isCation()` and `isAnion()` both answer false, and
`Element.invariants()` constrains only `name` and `atomicWeight`, so 0 is a legal value rather
than a hole in the data.

### Element console pages

- `/chemistry/elements` — table of all 16, linked to detail. Reached from the existing inline
  sub-link line on `/chemistry` ("View products →" gains an elements sibling), so the primary
  nav is untouched.
- `/chemistry/elements/{name}` — symbol, atomic weight, ionic form, cation/anion. Nothing
  else; see §2.
- [`chemistry/detail.jte`](../../domains/chemistry/chemistry-console/src/main/jte/chemistry/detail.jte)
  already renders a compound's constituent elements as text. Those chips become links to the
  new element route — no query behind it, and the only inbound path to element pages from
  within chemistry. Two details:
  - **A latent bug blocks it.** `PeriodicElement.Ca.elementName()` returns
    `ElementName.of("Calcium")` — capitalized, which fails `EntityName`'s lower-kebab-case
    validity rule. Nothing validates it today (the single call site renders it as display
    text), so the invalid slug is invisible. PR A splits the two concerns: `elementName()`
    returns a valid lowercase slug, a new `displayName()` returns `"Calcium"` for the page to
    print. One call site to update.
  - **Only link elements that exist.** 16 of 118 `PeriodicElement` constants have an `Element`
    entity. The controller passes the set of catalogued element slugs and the template links
    only those — the same shape the compound list page already uses for `depictableSlugs`.
    Unlinked chips keep rendering as text rather than pointing at a redirect.
- `ElementTestContext` in `chemistry-test-context`, mirroring `CompoundTestContext`;
  `ChemistryTestContext` gains `elementQuery()`. The query adapter
  (`ElementQueryImpl`), the repository mock, and `ElementTestEntitySource` all exist already.

### Catalog wiring

- `ChemistryLinker` gains `case ElementName n -> "/chemistry/elements/" + n.value()`.
- `ChemistryCatalogContribution` emits one `SearchableEntity` per element, tokens = slug +
  symbol. This is what makes `Catalog.findBySlug("calcium")` resolve, and it makes elements
  searchable from the cross-domain search page as a side effect.
- **Slug uniqueness verified:** none of the 16 element slugs collides with an existing
  compound, product, or glossary-term slug, so `CatalogAssembly`'s fail-fast uniqueness check
  stays green.

## 2. What the element page deliberately omits

A "compounds containing this element" section was considered and cut. Measured against the
real catalog, the section it would render:

| element | compounds in catalog |
| --- | --- |
| calcium | 5 — gypsum, calcium carbonate, calcium chloride, calcium pectate, OAC chelate |
| sulfur 4 · nitrogen 3 · magnesium 2 · potassium 2 · chlorine 1 | useful |
| **phosphorus, sodium, zinc, manganese, iron, copper, boron** | **0** |
| carbon 11 · hydrogen 12 · oxygen 14 | ~the entire 14-compound catalog |

Useful for six elements, permanently empty for seven — including every micronutrient, which is
precisely what a reader clicks from the micro table — and noise for three. Filtering to the
`FERTILIZER` functional role does not rescue it: the empties stay empty (no catalogued compound
carries a micronutrient) and oxygen only falls from 14 to 6.

It also isn't cheap. `constituentElements` is a `Set<PeriodicElement>` with no read port, so it
would need a package-private repository method, a validating mock with a null-rejection
contract test, and a paged query pair — paged rather than input-bounded, since the match set
grows with the catalog.

The element page therefore ships with the element's own facts and nothing more. It is a thin
page, but an honest destination for "what is boron", and it is reached from both directions
(soil nutrient rows, compound element chips). Both enrichments considered here —
compounds-by-element, and soil back-references — stay available and are easier to judge once
the page exists to look at. See *Out of scope*.

## 3. The crossing

`NutrientChemistry` in `soil-api`'s `observation` package, a `ValueObject`:

```java
public record NutrientChemistry(
        NutrientName nutrient,      // nitrate-n   — soil's row
        EntityName substance,       // nitrogen    — chemistry's entity
        ReportedForm reportedForm   // ION         — how FGL prints it
) implements ValueObject
```

It carries its own key so it can travel without losing its subject: an element page asking
"which soil nutrients reference me?" streams all 17 and filters on `substance`, which a bare
map value cannot answer. `substance` is typed `EntityName`, so a nutrient that maps to a
compound instead of an element needs no new type — `ChemistryLinker` already dispatches on the
concrete name class, and the element-or-compound union is expressed by polymorphism rather
than a sealed hierarchy.

`ReportedForm` is an enum in the same package — `ELEMENTAL`, `OXIDE_EQUIVALENT`, `ION` — each
with a `label()` for the link's `title` attribute, following `AssessmentSource.label()`.

`Nutrients` gains the map plus `Optional<NutrientChemistry> chemistryOf(NutrientName)`.
`Optional` for the same reason `printedNameOf` falls back to the slug: an uncatalogued lab
slug still has to render.

| nutrient row | substance | reported form |
| --- | --- | --- |
| nitrate-n | nitrogen | ION (nitrate) |
| phosphorus-p2o5 | phosphorus | OXIDE_EQUIVALENT |
| potassium-exchangeable, potassium-soluble | potassium | OXIDE_EQUIVALENT (K₂O) |
| calcium-exchangeable, calcium-soluble | calcium | ELEMENTAL |
| magnesium-exchangeable, magnesium-soluble | magnesium | ELEMENTAL |
| sodium-exchangeable, sodium-soluble | sodium | ELEMENTAL |
| sulfate | sulfur | ION |
| zinc, manganese, iron, copper, boron | same-named element | ELEMENTAL |
| chloride | chlorine | ION |

The exchangeable/soluble split collapses on purpose: both calcium fractions are calcium. The
split is a property of how the lab extracts, not of the substance, and the soil domain already
says so on the profile page ("Each cation is reported as two fractions…").

### Console resolution

`NutrientChemistryLinks` in `soil-console`, modelled on
[`CladeRankLinks`](../../domains/library/library-console/src/main/java/com/naturalist/library/console/clade/CladeRankLinks.java):

```java
Nutrients.chemistryOf(name)
    .map(c -> c.substance().value())
    .flatMap(catalog::findBySlug)   // does any domain own this slug?
    .map(linker::linkFor)           // → /chemistry/elements/calcium
    .orElse(null);                  // → plain text, never a broken link
```

`SoilsController` takes `(Catalog, EntityRefLinker)` in its constructor exactly as
`ChemistryController` does — `CompositeEntityRefLinker` is already a bean.

`readingRow.jte` renders the label as an anchor when a URL resolves, with a `title` naming the
form ("reported as an oxide equivalent of phosphorus"), and as today's plain text otherwise.
`nutrientTable.jte` threads the resolver through as a defaulted param — the same shape
`profile.jte` uses for `glossaryLinker = GlossaryLinker.none()` — so existing template tests
and any other caller keep compiling.

**The fallback is the design, not a safety net.** Soil states which substance a nutrient
measures; the catalog decides whether anyone owns that slug; the linker decides whether it has
a route. Any of the three coming up empty renders the row as it renders today. The six new
elements make 15 of 17 rows link on the first pass, and an 18th nutrient from a future lab
degrades to text instead of a 404.

## PR split

Three PRs, each inside the ≤400-line guideline (ADR-019), merged in order:

| PR | Module | Contents |
| --- | --- | --- |
| A | chemistry | 6 elements, `/chemistry/elements` list + detail, `ElementTestContext`, `ChemistryTestContext.elementQuery()`, `ChemistryLinker` case, element catalog contribution, constituent-element chips linked on compound detail |
| B | soil-api | `NutrientChemistry`, `ReportedForm`, `Nutrients.chemistryOf` + map |
| C | soil-console | `NutrientChemistryLinks`, `readingRow.jte` / `nutrientTable.jte` changes, `SoilsController` wiring |

A and B are independent and can land in either order; C depends on both.

**Build the destination first.** With PR A merged, the element-or-compound question needs no
further design: `ChemistryLinker`'s `CompoundName` case is already live, so a nutrient that
points at a compound rather than an element resolves on day one through the same three lines in
`NutrientChemistryLinks`. Every remaining decision — whether the element page wants compounds,
whether it wants soil back-references — becomes a judgment about a page that exists rather than
one being imagined.

## Testing

- **Element data** — `ElementTestEntitySourceTest` count and per-element assertions; the existing
  repository contract test covers the new rows without change.
- **Catalog contribution** — `ChemistryCatalogContributionTest` gains cases asserting an
  element is findable by slug and by symbol, and that `findBySlug("calcium")` returns a ref
  the `ChemistryLinker` resolves to `/chemistry/elements/calcium`.
- **Linker** — `ElementName` maps to the element route; an unknown name type still returns
  null.
- **`PeriodicElement`** — `elementName()` is a valid `EntityName` for every one of the 118
  constants (`isValid()` true, matches lower-kebab-case), and `displayName()` still returns the
  printed form.
- **Compound detail template** — a chip for a catalogued element renders as an anchor to
  `/chemistry/elements/{slug}`; a chip for an element with no entity (e.g. `Se`) renders as
  plain text.
- **Drift guard (soil-api)** — a test asserting every name in `Nutrients.ALL` has a declared
  `chemistryOf`, so the printed-name and chemistry maps cannot drift apart when a nutrient is
  added.
- **Template (soil-console)** — `SoilConsoleTemplateTest` cases for a row that links and a row
  whose substance resolves nowhere (plain text, no anchor, no stray markup).

## Out of scope

- **Oxide→elemental conversion factors.** Real chemistry, no consumer. Add when the profile
  page shows elemental equivalents.
- **Compounds containing this element** (§2). Cut on the evidence: useful for six elements,
  empty for seven, noise for three, and it costs a repository method, a validating mock, a
  contract test, and a paged query pair. Revisit when the compound catalog carries
  micronutrient amendments.
- **Back-references from the element page to soil.** A `soil-core` `EntityReferences` provider
  answering for `ElementName` would let an element page list the nutrient rows that reference
  it — "appears in your lab reports as Calcium (Exch), Calcium (Sol)" — symmetric with the
  compound page's "Found in" panel. Non-empty for all 13 nutrient-mapped elements, which is the
  case compounds-by-element fails, and it completes the round trip the reader just walked.
  `NutrientChemistry` carrying its own `nutrient` key is what keeps it cheap. The strongest
  candidate for the next slice, deliberately not bundled: judge it against the page once the
  page is real.
- **A nutrient whose substance is a compound.** The type admits it, no current row needs it,
  and no mapping asserts one.
- **Primary-nav changes.** Elements are reached from `/chemistry`.
