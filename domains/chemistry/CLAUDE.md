# Chemistry Domain

## Domain Vocabulary

**Element** — `NamedEntity<ElementName>`. An atomic building block with symbol, atomic
weight, and ionic form. Exists independently; referenced by Compounds. Instances in
`elements.json`. Equality is by `ElementName` (ADR-022); no `id` component.

**CompoundInfo** — `ValueObject` owned by `Compound`. Chemical classification facts:
formula, molecular weight, pH character, chemical nature, physical form, structural type,
functional roles, constituent elements. No identity of its own — exists only as an
attribute of its parent compound.

**Compound** — `NamedEntity<CompoundName>` + `@AggregateRoot`. The single identity
and consistency boundary for compound data. Carries `name` (slug),
`@UniqueValue String commonName`, and owns `CompoundInfo` (ValueObject)
plus all profile value objects (`SolubilityProfile`, `BioavailabilityProfile`,
`VolatilizationProfile`, `SafetyProfile`) and the `Map<String, String> properties`.
Has a `TestEntitySource`. Instances in `compounds.json`.
Java model is agnostic to specific compounds — the catalog is data, not code.
Cross-domain references use `CompoundName` slug.

**Product** — `NamedEntity<ProductName>`. A commercial SKU (e.g. `"Apiguard (Véto-pharma)"`,
`"TPS Nutrients CalMag OAC"`) carrying `name` (slug), `@UniqueValue String displayName`,
`Set<CompoundName> compounds` (the formulation), and a `Map<String, String> properties`
of SKU-scoped attributes (concentration, application window, NPK ratio, etc.). The product
authoritatively defines its ingredient list; the reverse lookup is a query.
Lives in its own subpackage (`com.naturalist.chemistry.product`) so it can graduate to a
standalone `product` domain without disturbing chemistry consumers. Instances in
`products-base.json`.

**ChemicalNature** — ORGANIC, INORGANIC, ORGANOMETALLIC. The chemical-nature axis on
`CompoundInfo`. Carbonates, oxides, cyanides, and pure carbon allotropes are conventionally
INORGANIC despite containing carbon.

**PhysicalForm** — ELEMENT, MINERAL, SALT, ACID, BASE, COMPLEX. The physical-form axis on
`CompoundInfo`. Independent of `ChemicalNature` — NaCl is both INORGANIC and SALT.

**CompoundCategory** — enum in `compound/`, the structural-axis bucket every `StructuralType` permit
rolls up into. Values: `ALKALOID`, `TERPENOID`, `PHENOLIC`, `FLAVONOID`, `TANNIN`, `GLYCOSIDE`,
`SAPONIN`, `GLUCOSINOLATE`, `ORGANIC_ACID`, `POLYSACCHARIDE`, `FATTY_ACID_LIPID`, `OTHER_ORGANIC`,
`ELEMENT`, `INORGANIC`. Vocabulary deliberately aligns with `plants.phytochemistry.PhytochemicalCategory`
where the two enums overlap (11 shared names with identical naturalist meaning); the chemistry side
adds the structural-axis-only values `ELEMENT`, `INORGANIC`, `FATTY_ACID_LIPID`, and drops the
ecological/physical-state values (`ESSENTIAL_OIL`, `RESIN`, `LATEX`, `NON_PROTEIN_AMINO_ACID`,
`PRIMARY_METABOLITE`) that have no carbon-skeleton equivalent. The two enums are separate types
because they describe different axes — see the two-axis section below.

**StructuralType** — sealed interface in `compound.structure` with stateless record permits
across the major carbon-skeleton families: alkaloids (`IndoleAlkaloid`, `TropaneAlkaloid`,
`PurineAlkaloid`, `PyrrolizidineAlkaloid`, `QuinolineAlkaloid`, `IsoquinolineAlkaloid`,
`OtherAlkaloid`); terpenoids by carbon count (`Monoterpene`, `Sesquiterpene`, `Diterpene`,
`Triterpene`, `Tetraterpene`); phenolic family (`SimplePhenolic`, `Flavonoid`, `Anthocyanin`,
`Tannin`); glycosides (`CardiacGlycoside`, `CyanogenicGlycoside`, `Saponin`,
`OtherGlycoside`); sulfur-containing (`Glucosinolate`); other organic (`OrganicAcid`,
`FattyAcidLipid`, `Polysaccharide`, `OtherOrganic`); plus the explicit "axis does not
apply" permits `Element` (pure elemental compounds) and `Inorganic` (inorganic salts,
minerals, oxides, simple inorganic acids). The structural-type axis on `CompoundInfo`
is **non-null** — every compound carries a positive answer rather than encoding the
inorganic case as a missing value. The `Element` and `Inorganic` permits intentionally
restate information also visible through `ChemicalNature` and `PhysicalForm`; they are
the structural-type axis's own way of saying "the carbon-skeleton question has no
answer here", which is a different statement from "this molecule has no organic
chemistry." Each permit answers `category()` itself via a default method on the
interface — an exhaustive switch over the sealed permits, so adding a permit forces the
compiler to require a corresponding case in one place. Consumers compare on
`compound.category()` (or `switch` over it) rather than `instanceof`-chaining the
sealed permits.

`StructuralType` extends `ValueObject` and provides a default no-op `invariants()` —
stateless permits inherit it without ceremony, but the validation pipeline is wired up
now so promoting a permit to a stateful record (e.g. a future `IndoleAlkaloid(RingSubstitution
substitution)`) does not require chasing down validation call sites. `CompoundInfo` already
validates `structuralType` via `valueObject(...)`, walking whatever invariants the concrete
permit declares — when a future permit overrides the default, it is automatically picked up. Top-level family membership
is also surfaced as behavioral predicates on
`Compound`: `isAlkaloid()`, `isTerpenoid()`, `isPhenolic()`, `isGlycoside()`,
`isGlucosinolate()`. These are one-liner rollups over `category()` — `isPhenolic()`
spans `PHENOLIC`, `FLAVONOID`, `TANNIN`; `isGlycoside()` spans `GLYCOSIDE`, `SAPONIN`.
Adding a permit means adding it to the switch in `StructuralType.category()`; consumers
never need to update. The list is intentionally non-exhaustive; add permits when real
catalog entries call for them, and use `OtherOrganic` only as a fallback while a new
permit is being agreed.

**FunctionalRole** — sealed interface in `compound.role` with stateless record permits
(`Chelator`, `Fumigant`, `BiologicalCatalyst`, `Fertilizer`, `Acaricide`). What a compound
*does* in the field. `CompoundInfo` carries a non-empty `Set<FunctionalRole>`. Behavioral
predicates `Compound.isFumigant()`, `Compound.isHazardous()`, `Compound.isChelated()` and
`Compound.playsRole(role)` are first-class — upstream domains never inspect the structural
form to learn what a compound does. `FunctionalRole` extends `ValueObject` with a default
no-op `invariants()` for the same forward-proofing reason as `StructuralType` — `CompoundInfo`
validates the set via `notEmpty` plus `valueObjectCollection`, so a future stateful permit is
picked up automatically without call-site updates.

**SolubilityProfile** — ValueObject owned by `Compound`. Required (non-nullable).
Water solubility at 20°C in g/L. Categories: INSOLUBLE through MISCIBLE.
Determines dissolution rate in soil and EC contribution.

**BioavailabilityProfile** — ValueObject owned by `Compound`. Required (non-nullable).
Absorption pathways: root mass flow, stomatal, cuticular, volatilization, contact.
Contains `boolean isChelateEnhanced` component — note: JSON field is `"isChelateEnhanced"`,
do not rename this component or deserialization will break.

**VolatilizationProfile** — ValueObject owned by `Compound`. Nullable (`@Nullable`).
Only present for fumigant compounds (formic acid, thymol).
Temperature window for safe and effective application.
Critical for Varroa treatment selection in Chico climate.

**SafetyProfile** — ValueObject owned by `Compound`. Nullable (`@Nullable`).
Only present for hazardous compounds. `HazardLevel`: NONE, LOW, MODERATE, HIGH, EXTREME.
Temperature window, bee safety (`hazardousToBeesWhenWet`), evening application requirement.

**Chelation** — Binding of a metal ion (Ca²⁺, Mg²⁺) by an organic acid molecule
forming a stable complex that resists precipitation and improves foliar absorption
through the waxy cuticle. TPS CalMag OAC uses organic acid chelation.

## Optional Profile Methods

`Compound` has two nullable profile components (`volatilization`, `safety`).
The Optional-returning accessor methods are named with the `Optional` suffix
to avoid conflicting with the record component accessors:

```java
public Optional<VolatilizationProfile> volatilizationOptional() { ... }
public Optional<SafetyProfile>         safetyOptional()         { ... }
```

`bioavailability` and `solubility` are required (non-nullable) — access them directly
via `compound.bioavailability()` and `compound.solubility()`.

`structuralType` is required (non-null) on `CompoundInfo` and is exposed on `Compound`
via a plain delegating accessor (`StructuralType structuralType()`) — there is no record
component on `Compound` of this name to conflict with, so the simple delegation pattern
applies rather than the `Optional` suffix. Consumers usually do not need the structural
type directly; the family predicates (`isAlkaloid()`, `isTerpenoid()`, …) are the
intended interface.

## Two-axis classification with phytochemistry

A plant compound is classified along two complementary axes:

1. **Structural type** (this domain) — `CompoundInfo.structuralType`. Carbon-skeleton
   taxonomy: indole alkaloid, monoterpene, flavonol, glucosinolate, etc. Property of the
   molecule itself, plant-agnostic.
2. **Ecological/use category** (`plants` domain) —
   `PhytochemicalConstituent.category` (`PhytochemicalCategory` enum). Coarser, organised
   for ecological queries (alkaloid, latex, essential oil, glucosinolate as a flat
   bucket). Plant-side, since the same compound may sit in different categories per
   plant in the literature.

The two axes are deliberately not collapsed into each other — thymol is a `Monoterpene`
structurally and an `ESSENTIAL_OIL` ecologically, and a query may want either answer.
Cross-domain reference is by `CompoundName` slug from the plants domain into the
chemistry catalog; `plants-api` does not depend on `chemistry-api`.

## Compound Properties

`Compound` owns a `Map<String, String> properties` — an open-ended set of named attributes
(e.g. `safeRateLbsPer1000Sqft`, `biologicalCatalyst`). These are compound-scoped
key-value pairs with no independent lifecycle and no cross-domain identity. Persisted via
`@ElementCollection` at the RDBMS adapter layer.

Recurring regulatory flags have been promoted out of the map onto `Compound` itself:
`boolean omriListed` and `boolean cdfaRegistered`. Missing values default to `false`.

`CompoundProperty` was previously modeled as a standalone `NamedEntity`. That model
was wrong on two counts:

1. `NamedEntity` requires a globally unique `EntityName`. A property key (e.g.
   `"safeRateLbsPer1000Sqft"`) is unique only within a compound — not globally. Multiple
   compounds share the same key names, which violated `TestEntitySource` uniqueness
   enforcement.
2. A compound property has no independent lifecycle. It exists only as an attribute of its
   parent compound, is never referenced cross-domain by identity, and has no repository of
   its own. This is a value collection, not an entity.

The correct analogy is `CompoundInfo#constituentElements` (`Set<PeriodicElement>`): both
are aggregate-owned collections with no entity identity. The RDBMS adapter will persist
`properties` as an `@ElementCollection` with `@MapKeyColumn` — the same structural pattern
as the elements join table, with an extra value column. The domain model is not driven by
the persistence concern.

Do not re-introduce `CompoundProperty` as an entity. If a specific property needs typed
behavior or cross-domain visibility, promote it to an explicit field on `Compound` or
`CompoundInfo` with its own accessor.

## PeriodicElement Enum

Uses IUPAC chemical symbols as enum constant names (`Ca`, `Mg`, `K`, not `CALCIUM`).
Full element name is in `elementName()` field. All 118 elements included.
Used for `Set<PeriodicElement> constituentElements` on `CompoundInfo` (a ValueObject owned by `Compound`).

## Data Extraction Pattern (chemical-science catalog)

`chemical-science/catalog/compounds.json` is the historical source of truth. The
current authoritative catalog is `chemistry-repository-test/src/main/resources/chemistry/compound/compounds.json`,
which is loaded by `CompoundTestEntitySource` at test time.

Each entry in `compounds.json` must include:

- `"name": "<compound-slug>"` — the `CompoundName` natural key (no `id` field; ADR-022)
- `"commonName": "<display name>"` — the human-readable common name (`@UniqueValue String`)
- `"compoundInfo": { ... }` — nested `CompoundInfo` ValueObject (formula, molecularWeight,
  phCharacter, chemicalNature, physicalForm, structuralType, functionalRoles,
  constituentElements)
- `"solubility": { ... }` — `SolubilityProfile` ValueObject
- `"bioavailability": { ... }` — `BioavailabilityProfile` ValueObject
- `"volatilization": null | { ... }` — `VolatilizationProfile` ValueObject, null for non-fumigants
- `"safety": null | { ... }` — `SafetyProfile` ValueObject, null for non-hazardous compounds
- `"properties": { ... }` — `Map<String, String>` open-ended key-value attributes

`compoundInfo.structuralType` is `null` for inorganic compounds (calcium-sulfate-dihydrate,
elemental-sulfur, all simple metal salts and minerals) and a discriminated record for
organic compounds, e.g. `{"kind": "MONOTERPENE"}`, `{"kind": "ORGANIC_ACID"}`,
`{"kind": "OTHER_ALKALOID"}`. The `kind` discriminator names match the `@JsonSubTypes`
registration on `StructuralType`.
