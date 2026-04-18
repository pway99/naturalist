# Chemistry Domain

## Domain Vocabulary

**Element** — Entity. An atomic building block with symbol, atomic weight, and ionic form.
Exists independently; referenced by Compounds. Instances in `elements.json`.
Uses identity-based `equals`/`hashCode` (on `id` only) — see `Element.java` for custom overrides.

**CompoundInfo** — `ValueObject` owned by `Compound`. Chemical classification facts:
formula, molecular weight, type, pH character, constituent elements. No identity of its
own — exists only as an attribute of its parent compound.

**Compound** — `CatalogEntity<CompoundId, CompoundName>` + `@AggregateRoot`. The single
identity and consistency boundary for compound data. Carries `id`, `name` (slug),
`@UniqueValue String commonName`, and owns `CompoundInfo` (ValueObject)
plus all profile value objects (`SolubilityProfile`, `BioavailabilityProfile`,
`VolatilizationProfile`, `SafetyProfile`) and the `Map<String, String> properties`.
Has a `TestEntitySource`. Instances in `compounds.json`.
Java model is agnostic to specific compounds — the catalog is data, not code.
Cross-domain references use `CompoundName` slug.

**CompoundType** — INORGANIC_SALT, ORGANIC_ACID, MINERAL, ELEMENT,
CHELATE, BIOLOGICAL_COMPOUND, VOLATILE_ORGANIC.

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

## PeriodicElement Enum

Uses IUPAC chemical symbols as enum constant names (`Ca`, `Mg`, `K`, not `CALCIUM`).
Full element name is in `elementName()` field. All 118 elements included.
Used for `Set<PeriodicElement> constituentElements` on `CompoundInfo` (a ValueObject owned by `Compound`).

## Data Extraction Pattern (chemical-science catalog)

`chemical-science/catalog/compounds.json` is the historical source of truth. The
current authoritative catalog is `chemistry-repository-test/src/main/resources/chemistry/compound/compounds.json`,
which is loaded by `CompoundTestEntitySource` at test time.

Each entry in `compounds.json` must include:
- `"id": null` — persistence-assigned by `CompoundTestEntitySource`
- `"name": "<compound-slug>"` — the `CompoundName` natural key
- `"commonName": "<display name>"` — the human-readable common name (`@UniqueValue String`)
- `"compoundInfo": { ... }` — nested `CompoundInfo` ValueObject (formula, molecularWeight, type, phCharacter, constituentElements)
- `"solubility": { ... }` — `SolubilityProfile` ValueObject
- `"bioavailability": { ... }` — `BioavailabilityProfile` ValueObject
- `"volatilization": null | { ... }` — `VolatilizationProfile` ValueObject, null for non-fumigants
- `"safety": null | { ... }` — `SafetyProfile` ValueObject, null for non-hazardous compounds
- `"properties": { ... }` — `Map<String, String>` open-ended key-value attributes
