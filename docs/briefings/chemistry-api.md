# chemistry-api — Chat Briefing

**Purpose.** Domain vocabulary and current shape of the `chemistry-api` module, sized
for a chat Claude session. Pair with `docs/briefings/framework-briefing.md` (framework / structural
glue). This briefing covers chemistry-api **only** — not chemistry-core, not
chemistry-repository-test, not any future chemistry-repository-rdms.

**Primary rule.** Names, packages, components, and visibility below are observed from
the source tree at briefing time, not extrapolated. If you need a type not listed here,
ask before inventing one.

---

## 1. Module Scope and DAG

```
chemistry-api  →  framework, identifiers, field-notes, catalog
```

No taxonomy, no habitat (chemistry is not an organism domain). Cross-domain consumers
(soil, insects, plants) reference chemistry **only** through the api — by `CompoundName`,
`ElementName`, `ProductName`, `ReactionName` slug, or `DepictionId` for the rare cases
where a depiction is referenced directly.

---

## 2. Package Map

```
com.naturalist.chemistry/
  ChemistryDomain                  — DomainId record (catalog kernel hook)
  TemperatureFahrenheit            — top-level NumericNamedValue, shared across profiles

  compound/
    Compound                       — @AggregateRoot, NamedEntity<CompoundName>
                                     (now carries omriListed, cdfaRegistered booleans)
    CompoundInfo                   — ValueObject (4-axis classification facts)
    ChemicalNature                 — enum (ORGANIC, INORGANIC, ORGANOMETALLIC)
    PhysicalForm                   — enum (ELEMENT, MINERAL, SALT, ACID, BASE, COMPLEX)
    CompoundCategory               — enum (14 buckets — structural-axis rollup)
    PhCharacter                    — enum
    MolecularWeight                — NumericNamedValue (g/mol, scale 4)
    Solubility                     — NumericNamedValue (g/L @ 20°C, scale 2)
    SolubilityProfile              — ValueObject (+ nested SolubilityCategory enum)
    BioavailabilityProfile         — ValueObject (+ nested AbsorptionPathway enum)
    VolatilizationProfile          — ValueObject (+ nested TemperatureAssessment enum)
    SafetyProfile                  — ValueObject (+ nested HazardLevel enum)
    CompoundDepiction              — Entity<DepictionId> (SMILES + note for rendering)
    CompoundQuery                  — public namespace interface
                                     (compounds, depictions)
    CompoundRepository             — package-private namespace CLASS
                                     (CompoundEntityRepository, DepictionRepository)
    CompoundEntityCollections      — public namespace interface
                                     (CompoundCollection, DepictionCollection)

    role/
      FunctionalRole               — sealed interface, ValueObject
                                     permits {Chelator, Fumigant, BiologicalCatalyst,
                                              Fertilizer, Acaricide}
                                     @JsonTypeInfo(property = "kind")

    structure/
      StructuralType               — sealed interface, ValueObject
                                     permits 28 stateless records covering alkaloids,
                                     terpenoids, phenolics, glycosides, glucosinolates,
                                     organic-acid / fatty-acid / polysaccharide /
                                     "other organic", plus Element + Inorganic
                                     "axis does not apply" permits
                                     @JsonTypeInfo(property = "kind")

  element/
    Element                        — NamedEntity<ElementName>
    AtomicWeight                   — NumericNamedValue (g/mol, scale 4)
    PeriodicElement                — enum, all 118 IUPAC elements (symbol-keyed)
    ElementCollection              — final class, BehavioralCollection<Element>
    ElementQuery                   — public interface (extends EntityQuery)
    ElementRepository              — package-private TOP-LEVEL interface
                                     (no longer nested, no longer @Incubating)

  product/
    Product                        — NamedEntity<ProductName>
                                     (commercial SKU; carries Set<CompoundName>)
    ProductCollection              — public final class, BehavioralCollection<Product>
    ProductQuery                   — public TOP-LEVEL interface
                                     (N=1 collapse — single entity in subpackage)
    ProductRepository              — package-private TOP-LEVEL interface

  reaction/
    ReactionProfile                — NamedEntity<ReactionName>
    ReactionConditions             — ValueObject
    CationExchangeProfile          — ValueObject
    ReactionType                   — package-private enum
```

### Identifier locations (in the `identifiers` module, not in chemistry-api)

| Type           | Package                             |
|----------------|-------------------------------------|
| `CompoundName` | `com.naturalist.chemistry.compound` |
| `DepictionId`  | `com.naturalist.chemistry.compound` |
| `ElementName`  | `com.naturalist.chemistry.element`  |
| `ProductName`  | `com.naturalist.chemistry.product`  |
| `ReactionName` | `com.naturalist.chemistry.reaction` |

There is no `com.naturalist.identifiers.*`. The *module* is `identifiers`; the *package*
mirrors the home domain.

---

## 3. NamedEntity / Entity Branches

Chemistry has four `NamedEntity` types and one `Entity`. Only `Compound` is an
aggregate.

| Type                | Identity       | Branch                        | Aggregate?                 | Owns                                                                            |
|---------------------|----------------|-------------------------------|----------------------------|---------------------------------------------------------------------------------|
| `Element`           | `ElementName`  | NamedEntity (slug = symbol)   | No                         | `AtomicWeight`, ionic form/charge                                               |
| `Compound`          | `CompoundName` | NamedEntity (slug)            | **Yes** (`@AggregateRoot`) | `CompoundInfo`, four profiles, omri/cdfa flags, `Map<String,String> properties` |
| `Product`           | `ProductName`  | NamedEntity (slug)            | No                         | `displayName`, `Set<CompoundName>`, `Map<String,String> properties`             |
| `ReactionProfile`   | `ReactionName` | NamedEntity (slug)            | No                         | `ReactionConditions`, reactant/product `List<CompoundName>`                     |
| `CompoundDepiction` | `DepictionId`  | **Entity** (UUIDv7 surrogate) | No                         | `CompoundName` (FK, `@EntityIdentifier`), `smiles`, `note`                      |

`Compound` is the only consistency boundary; the rest stand alone.

`Element.equals/hashCode` is overridden to compare by `name` only — symbol, atomic
weight, and ionic form are not part of identity.

`CompoundDepiction.compoundName` carries `@EntityIdentifier` — at most one depiction
per compound (secondary unique constraint).

---

## 4. Compound — the Aggregate

```java
@AggregateRoot
public record Compound(
    CompoundName name,
    @UniqueValue String commonName,
    CompoundInfo compoundInfo,
    SolubilityProfile solubility,           // required
    BioavailabilityProfile bioavailability, // required
    @Nullable VolatilizationProfile volatilization,  // nullable: fumigants only
    @Nullable SafetyProfile safety,                  // nullable: hazardous only
    boolean omriListed,
    boolean cdfaRegistered,
    Map<String, String> properties
) implements NamedEntity<CompoundName>
```

### Required vs nullable profiles

- `solubility`, `bioavailability` — every compound has them; access directly.
- `volatilization`, `safety` — nullable; access via `volatilizationOptional()` /
  `safetyOptional()`. The optional accessor name carries the `Optional` suffix to avoid
  colliding with the record component (per domains/CLAUDE.md).

### Promoted regulatory flags

`omriListed` and `cdfaRegistered` are first-class boolean components on `Compound`.
They were previously string entries in the `properties` map and got promoted because
they are queried frequently and shared across many compounds. Missing values default
to `false` at deserialization. Other regulatory / SKU-scoped attributes
(`safeRateLbsPer1000Sqft`, `biologicalCatalyst`, etc.) remain in `properties` until
they earn the same promotion.

### Mutators

`withSafety(SafetyProfile)` is the only `with*` method. Other fields have no mutator
on the api today.

### Behavioral queries on Compound

Classification delegations to `CompoundInfo`: `formula()`, `molecularWeight()`,
`phCharacter()`, `isPHNeutral()`, `isAcidic()`, `structuralType()`, `category()`.

Structural-family rollups over `CompoundCategory`: `isAlkaloid()`, `isTerpenoid()`,
`isPhenolic()` (spans PHENOLIC + FLAVONOID + TANNIN), `isGlycoside()` (spans
GLYCOSIDE + SAPONIN), `isGlucosinolate()`.

Functional-role predicate: `playsRole(FunctionalRole role)` checks
`compoundInfo.functionalRoles().contains(role)`. Convenience predicates:
`isFumigant()`, `isHazardous()`, `isChelated()`.

Profile / property: `volatilizationOptional()`, `safetyOptional()`, `property(String)`.

Behavioral: `isHighlySoluble()`, `ecContributionPerGramPerLiter()`, `isSafe()`,
`isVolatile()`, `isSafeAtTemperature(temp)`, `isEffectiveAtTemperature(temp)`.

### `properties` is NOT an entity collection

`Map<String, String>` open-ended attributes (e.g. `safeRateLbsPer1000Sqft`,
`biologicalCatalyst`). It was previously modeled as a standalone `CompoundProperty`
`NamedEntity` and that was wrong on two counts:

1. The key (e.g. `"omriListed"`) is unique only within a compound, not globally —
   violated `NamedTestEntitySource` uniqueness.
2. A property has no independent lifecycle and is never referenced cross-domain.

Do not re-introduce `CompoundProperty`. Promote a key to a typed field on `Compound`
or `CompoundInfo` only when it gains typed behavior or cross-domain visibility — as
`omriListed` and `cdfaRegistered` did.

### Compound invariants (current)

```java
i.entityName(name, "name")
 .notBlank(commonName, "commonName")
 .valueObject(this, Compound::compoundInfo, "compoundInfo")
 .valueObject(this, Compound::solubility, "solubility")
 .valueObject(this, Compound::bioavailability, "bioavailability")
 .valueObjectOrNull(this, Compound::volatilization, "volatilization")
 .valueObjectOrNull(this, Compound::safety, "safety")
 .notNull(this, Compound::properties, "properties");
```

Note: nullable profiles use `valueObjectOrNull` (descend only when present); required
profiles use `valueObject` (must be non-null and structurally valid). The two boolean
flags need no constraint.

---

## 5. Profile and Classification Value Objects

All `ValueObject` records owned by `Compound` (directly or transitively through
`CompoundInfo`). None has identity of its own; none has a repository.

### `CompoundInfo` — four-axis classification

What the compound *is*, not who it is.

```
formula              : String
molecularWeight      : @Nullable MolecularWeight    (null for biologicals/chelates)
phCharacter          : PhCharacter
chemicalNature       : ChemicalNature                (axis 1)
physicalForm         : PhysicalForm                  (axis 2)
structuralType       : StructuralType                (axis 3 — required, sealed)
functionalRoles      : Set<FunctionalRole>           (axis 4 — non-empty, sealed)
constituentElements  : Set<PeriodicElement>          (uses chemical symbols: "Ca", "Mg")
```

Invariants: `formula`, `phCharacter`, `chemicalNature`, `physicalForm` are non-null;
`structuralType` is non-null and structurally validated as a `ValueObject`;
`functionalRoles` is non-empty and each element is descended into;
`constituentElements` is non-empty.

The four classification axes are deliberately orthogonal:

- **`ChemicalNature`** = ORGANIC | INORGANIC | ORGANOMETALLIC.
- **`PhysicalForm`** = ELEMENT | MINERAL | SALT | ACID | BASE | COMPLEX. NaCl is
  INORGANIC + SALT — these axes do not collapse.
- **`StructuralType`** = sealed family of carbon-skeleton permits, plus explicit
  `Element` and `Inorganic` "axis does not apply" permits so the field is never null.
- **`FunctionalRole`** = sealed family of role permits — what a compound *does*.

### `SolubilityProfile`

```
gramsPerLiterAt20C       : Solubility (NumericNamedValue, scale 2)
category                 : SolubilityCategory  (INSOLUBLE, SPARINGLY_SOLUBLE,
                                                SLIGHTLY_SOLUBLE, SOLUBLE,
                                                HIGHLY_SOLUBLE, MISCIBLE)
ecContributionFactor     : BigDecimal
notes                    : String
```

`HIGHLY_SOLUBLE` threshold is **> 100 g/L**.

### `BioavailabilityProfile`

```
primaryPathway           : AbsorptionPathway  (ROOT_MASS_FLOW, ROOT_DIFFUSION,
                                               FOLIAR_STOMATAL, FOLIAR_CUTICULAR,
                                               FOLIAR_BOTH, VOLATILIZATION, CONTACT)
relativeAbsorptionRate   : BigDecimal  (0.0–1.0)
cuticular                : boolean
stomatal                 : boolean
isChelateEnhanced        : boolean   ← JSON field MUST stay "isChelateEnhanced".
                                       The "is" prefix is part of the wire contract;
                                       renaming the component breaks deserialization.
mechanism                : String
```

### `VolatilizationProfile` (nullable on `Compound`)

```
minEffectiveTempF        : TemperatureFahrenheit
maxSafeTempF             : TemperatureFahrenheit
optimalTempF             : TemperatureFahrenheit
vaporPressureAt20C       : BigDecimal
efficacyNotes            : String
safetyNotes              : String
```

`assess(temp)` returns `TemperatureAssessment` ∈ `{TOO_COLD_INEFFECTIVE, ACCEPTABLE,
OPTIMAL, TOO_HOT_DANGEROUS}`. `OPTIMAL_WINDOW_DEGREES` constant = 10°F.

### `SafetyProfile` (nullable on `Compound`)

```
hazardLevel                  : HazardLevel  (NONE, LOW, MODERATE, HIGH, EXTREME)
maxSafeConcentrationPpm      : BigDecimal
minApplicationTempF          : @Nullable TemperatureFahrenheit
maxApplicationTempF          : @Nullable TemperatureFahrenheit
requiresProtectiveEquipment  : boolean
hazardousToBeesWhenWet       : boolean
requiresEveningApplication   : boolean
applicationConstraints       : String
```

`isWithinTemperatureWindow(temp)` treats null min/max bounds as unbounded on that
side.

---

## 6. Sealed Hierarchies — `FunctionalRole`, `StructuralType`

Both are sealed `ValueObject` interfaces with stateless record permits. Both carry
Jackson polymorphic wiring (`@JsonTypeInfo(property = "kind")` + `@JsonSubTypes`)
and a default no-op `invariants()` so consumers can promote a permit from stateless
to stateful without chasing call sites.

### `FunctionalRole` permits (5)

`Chelator`, `Fumigant`, `BiologicalCatalyst`, `Fertilizer`, `Acaricide`. JSON
discriminator names: `"CHELATOR"`, `"FUMIGANT"`, `"BIOLOGICAL_CATALYST"`,
`"FERTILIZER"`, `"ACARICIDE"`.

`CompoundInfo.functionalRoles : Set<FunctionalRole>` is non-empty.

### `StructuralType` permits (28) and `CompoundCategory` (14 buckets)

Permits, grouped by family — full list with JSON discriminators:

- **Non-organic** (axis-does-not-apply): `Element` ("ELEMENT"), `Inorganic` ("INORGANIC").
- **Alkaloids**: `IndoleAlkaloid`, `TropaneAlkaloid`, `PurineAlkaloid`,
  `PyrrolizidineAlkaloid`, `QuinolineAlkaloid`, `IsoquinolineAlkaloid`, `OtherAlkaloid`.
- **Terpenoids by carbon count**: `Monoterpene`, `Sesquiterpene`, `Diterpene`,
  `Triterpene`, `Tetraterpene`.
- **Phenolic family**: `SimplePhenolic`, `Flavonoid`, `Anthocyanin`, `Tannin`.
- **Glycosides**: `CardiacGlycoside`, `CyanogenicGlycoside`, `Saponin`, `OtherGlycoside`.
- **Sulfur-containing**: `Glucosinolate`.
- **Other organic**: `OrganicAcid`, `FattyAcidLipid`, `Polysaccharide`, `OtherOrganic`.

JSON `kind` strings are the SCREAMING_SNAKE_CASE versions of permit names
(`"INDOLE_ALKALOID"`, `"OTHER_GLYCOSIDE"`, etc.).

`StructuralType.category()` is a `default` method backed by an exhaustive switch over
the sealed permits — adding a permit forces a compiler error in `category()` until
its bucket is named. Consumers compare on `compound.category()` (or `switch` over it)
rather than `instanceof`-chaining the permits.

`CompoundCategory` enum (14 values): `ALKALOID`, `TERPENOID`, `PHENOLIC`, `FLAVONOID`,
`TANNIN`, `GLYCOSIDE`, `SAPONIN`, `GLUCOSINOLATE`, `ORGANIC_ACID`, `POLYSACCHARIDE`,
`FATTY_ACID_LIPID`, `OTHER_ORGANIC`, `ELEMENT`, `INORGANIC`. Vocabulary aligns with
`plants.phytochemistry.PhytochemicalCategory` where the two enums share names — the
two enums are intentionally separate types because they describe different axes
(structural vs. ecological).

`OtherOrganic` and `OtherAlkaloid` / `OtherGlycoside` are fallbacks. Use only while a
new permit is being agreed; never as a permanent home.

---

## 7. Element Sub-package

### `Element` (NamedEntity)

```
name          : ElementName
symbol        : String
atomicWeight  : AtomicWeight
ionicForm     : String
ionicCharge   : int
```

Behavior: `isCation()`, `isAnion()`, `isDivalent()`. `equals/hashCode` by `name` only;
`toString()` returns `symbol`. Invariants validate only `name` and `atomicWeight`.

### `PeriodicElement` enum vs `Element` entity

Two coordinated representations of "an element":

- `PeriodicElement` — type-safe enum of all 118 IUPAC elements, keyed by chemical
  symbol (`Ca`, `Mg`, not `CALCIUM`). Carries atomic number, IUPAC name,
  `BigDecimal atomicWeight` (scale 4, HALF_UP).
- `Element` — JSON-loaded `NamedEntity` with domain-enriched ionic context
  (`ionicForm`, `ionicCharge`).

`CompoundInfo.constituentElements` uses `Set<PeriodicElement>` — symbol enum, not
`Element`. JSON serializes by enum constant name.

### `ElementRepository`

`ElementRepository` is a top-level package-private interface:

```java
interface ElementRepository extends EntityRepository<ElementName, Element> {
}
```

The earlier nested-interface experiment and `@Incubating` annotation are gone —
`ElementRepository` now follows the simplest shape (single entity in the subpackage,
no namespace needed). `ElementQuery extends EntityQuery<ElementName, Element,
ElementCollection>` is the public consumer surface.

### `ElementCollection`

`final class extends BehavioralCollection<Element>`. Constructor is package-private;
`public static of(Collection<Element>)` and `public static empty()` are the only
entry points.

---

## 8. Compound Read-Side — Query / Repository / Collections

### `CompoundQuery` (public namespace interface)

```java
public interface CompoundQuery {
  CompoundEntityQuery compounds();

  DepictionQuery depictions();

  interface CompoundEntityQuery extends EntityQuery<CompoundName, Compound, CompoundCollection> {
    EntityNameSet<CompoundName> allCompoundNames();
  }

  interface DepictionQuery extends EntityQuery<DepictionId, CompoundDepiction, DepictionCollection> {
    Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName);

    EntityNameSet<CompoundName> allDepictedCompounds();
  }
}
```

### `CompoundRepository` (package-private namespace class)

```java
class CompoundRepository {                     // class (not interface) so
  // nested types can be hidden
  protected interface CompoundEntityRepository
          extends EntityRepository<CompoundName, Compound> {
    List<CompoundName> getAllCompoundNames();
  }

  protected interface DepictionRepository
          extends EntityRepository<DepictionId, CompoundDepiction> {
    Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName);

    List<CompoundName> getAllDepictedCompoundNames();
  }
}
```

### `CompoundEntityCollections`

`CompoundCollection` and `DepictionCollection`. `CompoundCollection` carries
domain-specific filters that return new collections through the package-private
constructor:

```java
CompoundCollection withChemicalNature(ChemicalNature nature);

CompoundCollection withFunctionalRole(FunctionalRole role);
```

`DepictionCollection.getByCompoundName(CompoundName)` returns `Optional<CompoundDepiction>`
because the unique-per-compound constraint guarantees at most one match.

---

## 9. Product Sub-package

### `Product` (NamedEntity)

```java
public record Product(
        ProductName name,
        @UniqueValue String displayName,
        Set<CompoundName> compounds,            // non-empty — the formulation
        Map<String, String> properties          // open-ended SKU attributes
) implements NamedEntity<ProductName>
```

Products are commercial SKUs (`"Apiguard (Véto-pharma)"`, `"TPS Nutrients CalMag OAC"`).
The product authoritatively defines its ingredient list; the reverse lookup (which
products contain compound X) is a query, not a stored field on `Compound`.

`property(String key)` returns `Optional<String>`. Invariants: `entityName`,
`notBlank(displayName)`, `notEmpty(compounds)`, `notNull(properties)`.

The product subpackage is N=1 — one entity in the subpackage, so the namespace
collapse rule applies:

### `ProductQuery` (public top-level interface)

```java
public interface ProductQuery extends EntityQuery<ProductName, Product, ProductCollection> {
  ProductCollection findByCompoundName(CompoundName compoundName);

  EntityNameSet<ProductName> allProductNames();
}
```

### `ProductRepository` (package-private top-level interface)

```java
interface ProductRepository extends EntityRepository<ProductName, Product> {
  List<Product> getByCompoundName(CompoundName compoundName);

  List<ProductName> getAllProductNames();
}
```

### `ProductCollection`

`public final class ProductCollection extends BehavioralCollection<Product>`.
Package-private constructor; public `of` / `empty` factories. No domain-specific
filter methods today.

The product subpackage is positioned to graduate to a standalone `product` domain
without disturbing chemistry consumers — keep it self-contained.

---

## 10. CompoundDepiction — the Entity Branch

```java
public record CompoundDepiction(
        DepictionId name,                       // surrogate UUIDv7
        @EntityIdentifier CompoundName compoundName,  // FK + unique constraint
        String smiles,                          // SMILES line notation
        String note                             // domain commentary on the depiction
) implements Entity<DepictionId>
```

Rendering inputs have no natural slug — a depiction is a row attached to a compound,
not a globally-named thing — so the surrogate-id branch of the identity model
applies. `compoundName` is `@EntityIdentifier`-annotated: at most one depiction per
compound.

The `note` field flags how the rendered structure relates to the catalogued
compound — canonical molecule, ionic formula unit (`[Ca+2].[Cl-].[Cl-]`), or
representative stand-in for a class-label entry like `organic-acid-chelate` or
`potassium-fatty-acids`.

Consumed by the chemistry console's CDK-based SVG renderer.

---

## 11. Reaction Sub-package

`ReactionProfile` is a `NamedEntity<ReactionName>` but currently has **no repository,
no query, no collection** in the api. It carries `List<CompoundName> reactants` and
`List<CompoundName> products` — cross-domain references by slug.

```
name           : ReactionName
title          : String
equation       : String
reactants      : List<CompoundName>
products       : List<CompoundName>
type           : ReactionType
conditions     : ReactionConditions
significance   : String
```

Behavior: `involves(CompoundName)`, `isBiologicallyCatalyzed()`,
`isTemperatureDependent()`.

`ReactionType` is **package-private**: DISSOLUTION, ACID_BASE, OXIDATION,
CATION_EXCHANGE, CHELATION, VOLATILIZATION, BIOLOGICAL, PRECIPITATION. It does not
appear on the public api surface today; promoting it to public is a deliberate change.

### Known gap — `ReactionProfile.invariants()`

Currently validates only `name` and `title`. The other six components
(`equation`, `reactants`, `products`, `type`, `conditions`, `significance`) are not
enforced at the boundary. Real gap, not a design choice — flag it if the user is
editing this file.

### `ReactionConditions`

Optional temperature bounds (min/max/optimal as `@Nullable TemperatureFahrenheit`),
moisture/aerobic/biological-catalyst booleans, optional `biologicalCatalyst` String
(accessor: `biologicalCatalystOptional()`, not `biologicalCatalyst()` — see
domains/CLAUDE.md rule on Optional-returning queries vs component accessors), optional
pH bounds. `relativeRateAt(temp)` is a linear interpolation between min, optimal, and
max temperature endpoints. `invariants()` is intentionally empty — every component is
nullable or boolean.

### `CationExchangeProfile`

ValueObject for cation-exchange reactions. `displacingCation` and `displacedCation`
are `ElementName` (cross-entity name reference). `selectivityCoefficient`,
`exchangeCapacityCmolKg`, plus narrative fields. Owned through reactions data, not
through `Compound`.

---

## 12. NumericNamedValue Conventions

Four `NumericNamedValue` records in chemistry-api. All implement
`com.naturalist.ddd.NumericNamedValue`, all carry a single `BigDecimal value`, all
declare `@JsonCreator public static of(BigDecimal)`, all use `RoundingMode.HALF_UP`.

| Type                    | Scale | Sign rule      | Notes                                                                                  |
|-------------------------|-------|----------------|----------------------------------------------------------------------------------------|
| `TemperatureFahrenheit` | 1     | any sign valid | Helpers: `isBelow`, `isAbove`, `isWithin(delta, other)`                                |
| `Solubility`            | 2     | `value >= 0`   | `isHighlySoluble()` = `> 100 g/L`                                                      |
| `MolecularWeight`       | 4     | `value > 0`    | g/mol, IUPAC sig-fig convention                                                        |
| `AtomicWeight`          | 4     | `value > 0`    | g/mol, IUPAC sig-fig; for synthetic elements carries longest-lived isotope mass number |

`TemperatureFahrenheit` lives at the chemistry-api root (not under any subpackage) so
all sub-packages can use it without an upward-looking import.

---

## 13. Cross-domain References — by slug only

A consumer of chemistry-api references its entities by name:

```java
CompoundName.of("calcium-sulfate-dihydrate")
ElementName.of("Ca")            // element names use chemical symbols, not slugs
ProductName.

of("apiguard-vetopharma")
ReactionName.of("gypsum-dissolution")
```

`PeriodicElement` is the only chemistry type a consumer can carry by *value* — it is
an enum, not a `NamedEntity`. Anything else that crosses a domain boundary crosses by
`EntityName`.

`Compound`, `Product`, and `ReactionProfile` are never referenced cross-domain by
value; consumers fetch by name through their own port and let the chemistry side
resolve. `CompoundDepiction` is an `Entity` and never crosses a domain boundary by
value either.

---

## 14. JSON Catalog Locations (chemistry-repository-test, NOT chemistry-api)

The api ships no JSON. Catalog files live in repository-test resources:

```
chemistry-repository-test/src/main/resources/chemistry/
  compound/compounds.json
  compound/depictions.json
  element/elements.json
  product/products-base.json
  reaction/reactions.json
  reaction/reactionProfiles.json
```

Catalog conventions (per chemistry CLAUDE.md):

- `"name": "<slug>"` is the EntityName. No `id` field on `NamedEntity` JSON —
  surrogate keys are adapter-internal only.
- `CompoundDepiction` JSON carries `"name"` as a UUIDv7 string (Entity branch).
- `compoundInfo`, `solubility`, `bioavailability`, `volatilization`, `safety`,
  `omriListed`, `cdfaRegistered`, `properties` keys mirror Compound's record
  component names exactly.
- `volatilization` and `safety` may be `null`.
- `compoundInfo.structuralType` is a discriminated record with a `"kind"` field
  (e.g. `{"kind": "MONOTERPENE"}`, `{"kind": "INORGANIC"}`). Always populated;
  never `null`.
- `compoundInfo.functionalRoles` is a non-empty array of discriminated records,
  e.g. `[{"kind": "FUMIGANT"}, {"kind": "ACARICIDE"}]`.
- `PeriodicElement` serializes as chemical symbol (`"Ca"`, `"Mg"`).
- Other enums serialize by constant name (`"INORGANIC"`, `"ROOT_MASS_FLOW"`,
  `"HIGHLY_SOLUBLE"`).

Do not place catalog data in the api module. Java in api defines schema; JSON in
repository-test defines instances.

---

## 15. Current State — What's Built, What's Not

**Built and stable.**

- `Compound` aggregate with all four profiles plus `omriListed`/`cdfaRegistered`
  flags and the four-axis classification (`ChemicalNature`, `PhysicalForm`,
  `StructuralType`, `Set<FunctionalRole>`).
- `CompoundQuery` namespace + `CompoundEntityQuery` + `DepictionQuery`.
- `CompoundEntityCollections` with `CompoundCollection` (carries
  `withChemicalNature` / `withFunctionalRole` filters) and `DepictionCollection`.
- `CompoundRepository` namespace with nested `CompoundEntityRepository` and
  `DepictionRepository`.
- `CompoundDepiction` entity (the Entity-branch reference for chemistry).
- `Product` entity + `ProductQuery` + `ProductCollection` + package-private
  `ProductRepository`. Top-level (no namespace) per the N=1 collapse rule.
- `Element` entity + `ElementCollection` + `ElementQuery` + package-private
  `ElementRepository` (top-level, no longer experimental, no longer nested).
- `PeriodicElement` enum (118 elements).
- `StructuralType` sealed family (28 permits) with Jackson polymorphic wiring and
  exhaustive `category()` switch.
- `FunctionalRole` sealed family (5 permits) with Jackson polymorphic wiring.
- `CompoundCategory` enum (14 values) aligned with
  `plants.phytochemistry.PhytochemicalCategory`.
- `ReactionProfile` record with `ReactionConditions` + `CationExchangeProfile`.
- All four `NumericNamedValue` types.
- `ChemistryDomain` registered with the catalog kernel.

**Missing on the api surface.**

- No `ReactionRepository`, `ReactionQuery`, or `ReactionCollection`. Reactions
  exist as records but cannot be queried through chemistry-api today.
- `ReactionType` is package-private; not part of the cross-module surface.
- No `with*` mutators on `Compound` other than `withSafety(SafetyProfile)`. Updates
  flow through full record reconstruction at the boundary.

**Known invariant gap.**

- `ReactionProfile.invariants()` enforces only `name` and `title`. Six other
  components are unguarded.

These are the points where invented APIs are most likely to creep in. If chat is
asked to "query reactions" or "set the molecular weight via withMolecularWeight",
the honest answer is "the surface isn't built — confirm the scope before generating
code".

---

## 16. Anti-patterns Specific to chemistry-api

- **Do not invent a `CompoundId` / `ElementId` / `ProductId` / `ReactionId`.**
  Identity for the four NamedEntity types is `CompoundName`, `ElementName`,
  `ProductName`, `ReactionName`. The only `Entity`-branch identifier in chemistry
  is `DepictionId` on `CompoundDepiction`.
- **Do not put profiles in their own packages.** `SolubilityProfile`,
  `BioavailabilityProfile`, `VolatilizationProfile`, `SafetyProfile` all live next to
  `Compound` in `compound/`. They are aggregate-owned value objects, not standalone
  domains.
- **Do not re-introduce `CompoundProperty` as a `NamedEntity`.** See §4. It's a
  `Map<String,String>` value collection on the aggregate. Promote individual keys to
  typed fields on `Compound` (as `omriListed` and `cdfaRegistered` were) when they
  earn it.
- **Do not use `Element` where `PeriodicElement` is called for** (and vice versa).
  `CompoundInfo.constituentElements` is `Set<PeriodicElement>` — symbol enum. The
  `Element` entity carries domain enrichment (ionic state) loaded from JSON.
- **Do not rename `BioavailabilityProfile.isChelateEnhanced`.** Component name = JSON
  field name; the `is` prefix is part of the wire contract.
- **Do not move `TemperatureFahrenheit` into `compound/` or `reaction/`.** It is
  shared across sub-packages; the api root is the correct package.
- **Do not encode the inorganic case as `null` on `CompoundInfo.structuralType`.**
  Use the explicit `StructuralType.Inorganic` or `StructuralType.Element` permit.
  Every compound carries a positive answer on the structural axis.
- **Do not collapse `CompoundCategory` and
  `plants.phytochemistry.PhytochemicalCategory`.** They describe different axes
  (structural vs. ecological); the deliberate name overlap is for naturalist
  legibility, not type unification.
- **Do not assume a `Reaction` query exists.** It does not. If a task implies one,
  surface that gap before generating code that depends on it.
- **Do not nest `ProductRepository` / `ProductQuery` inside a namespace class.** The
  N=1 collapse applies — they are top-level.
- **Do not assume `ElementRepository` is incubating.** The earlier
  nested-interface experiment is gone. It is now a top-level package-private
  interface and follows the standard single-entity collapse.
