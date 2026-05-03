# chemistry-api — Chat Briefing

**Purpose.** Domain vocabulary and current shape of the `chemistry-api` module, sized
for a chat Claude session. Pair with `docs/briefings/framework.md` (framework / structural
glue). This briefing covers chemistry-api **only** — not chemistry-core, not
chemistry-repository-test, not any future chemistry-repository-rdms.

**Primary rule.** Names, packages, components, and visibility below are observed from
the source tree at briefing time, not extrapolated. If you need a type not listed here,
ask before inventing one.

---

## 1. Module Scope and DAG

```
chemistry-api  →  framework, identifiers, field-notes
```

No taxonomy, no habitat (chemistry is not an organism domain). Cross-domain consumers
(soil, insects, plants) reference chemistry **only** through the api — by `CompoundName`,
`ElementName`, `ReactionName` slug.

---

## 2. Package Map

```
com.naturalist.chemistry/
  TemperatureFahrenheit            — top-level NumericNamedValue, shared across profiles

  compound/
    Compound                       — @AggregateRoot, NamedEntity<CompoundName>
    CompoundInfo                   — ValueObject (classification facts)
    CompoundType                   — enum
    PhCharacter                    — enum
    MolecularWeight                — NumericNamedValue (g/mol, scale 4)
    Solubility                     — NumericNamedValue (g/L @ 20°C, scale 2)
    SolubilityProfile              — ValueObject (+ nested SolubilityCategory enum)
    BioavailabilityProfile         — ValueObject (+ nested AbsorptionPathway enum)
    VolatilizationProfile          — ValueObject (+ nested TemperatureAssessment enum)
    SafetyProfile                  — ValueObject (+ nested HazardLevel enum)

  element/
    Element                        — NamedEntity<ElementName>
    AtomicWeight                   — NumericNamedValue (g/mol, scale 4)
    PeriodicElement                — enum, all 118 IUPAC elements (symbol-keyed)
    ElementCollection              — final class, BehavioralCollection<Element>
    ElementQuery                   — public interface (extends EntityQuery)
    ElementRepository              — package-private CLASS, namespace for nested
                                     ElementEntityRepository (incubating, see §6)

  reaction/
    ReactionProfile                — NamedEntity<ReactionName>
    ReactionConditions             — ValueObject
    CationExchangeProfile          — ValueObject
    ReactionType                   — package-private enum

  safety/                          — exists but EMPTY (no public surface yet)
```

### Identifier locations (in the `identifiers` module, not in chemistry-api)

| Type           | Package                             |
|----------------|-------------------------------------|
| `CompoundName` | `com.naturalist.chemistry.compound` |
| `ElementName`  | `com.naturalist.chemistry.element`  |
| `ReactionName` | `com.naturalist.chemistry.reaction` |

There is no `com.naturalist.identifiers.*`. The *module* is `identifiers`; the *package*
mirrors the home domain.

---

## 3. The Three NamedEntity Branches

Chemistry has three `NamedEntity` types. Only one is an aggregate.

| Entity            | Identity       | Aggregate?                 | Owns                                                           |
|-------------------|----------------|----------------------------|----------------------------------------------------------------|
| `Element`         | `ElementName`  | No                         | `AtomicWeight`, symbol, ionic form/charge                      |
| `Compound`        | `CompoundName` | **Yes** (`@AggregateRoot`) | `CompoundInfo`, four profiles, `Map<String,String> properties` |
| `ReactionProfile` | `ReactionName` | No                         | `ReactionConditions`, reactant/product `List<CompoundName>`    |

Compound is the consistency boundary; `Element` and `ReactionProfile` stand alone. There
is no `Reaction` aggregate — `ReactionProfile` carries the data directly.

`Element.equals/hashCode` is overridden to compare by `name` only — symbol, atomic
weight, and ionic form are not part of identity.

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
    Map<String, String> properties
) implements NamedEntity<CompoundName>
```

### Required vs nullable profiles

- `solubility`, `bioavailability` — every compound has them; access directly.
- `volatilization`, `safety` — nullable; access via `volatilizationOptional()` /
  `safetyOptional()`. The optional accessor name carries the `Optional` suffix to avoid
  colliding with the record component (per domains/CLAUDE.md).

### Mutators

- `withSafety(SafetyProfile)` is the only `with*` method. Other fields have no mutator
  on the api today.

### Behavioral queries on Compound (delegate to children)

`formula()`, `molecularWeight()`, `type()`, `phCharacter()`, `isPHNeutral()`,
`isAcidic()`, `isHighlySoluble()`, `ecContributionPerGramPerLiter()`, `isSafe()`,
`isVolatile()`, `isFumigant()`, `isChelate()`, `isSafeAtTemperature(...)`,
`isEffectiveAtTemperature(...)`, `property(String)`.

### `properties` is NOT an entity collection

`Map<String, String>` open-ended attributes (e.g. `omriListed`,
`safeRateLbsPer1000Sqft`, `biologicalCatalyst`). It was previously modeled as a
standalone `CompoundProperty` `NamedEntity` and that was wrong on two counts:

1. The key (e.g. `"omriListed"`) is unique only within a compound, not globally —
   violated `NamedTestEntitySource` uniqueness.
2. A property has no independent lifecycle and is never referenced cross-domain.

Do not re-introduce `CompoundProperty`. Promote a key to a typed field on `Compound`
or `CompoundInfo` only when it gains typed behavior or cross-domain visibility.

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
profiles use `valueObject` (must be non-null and structurally valid).

---

## 5. Profile Value Objects

All four are `ValueObject` records owned by `Compound`. None has identity of its own;
none has a repository.

### `CompoundInfo`

Classification facts grouped together — what the compound *is*, not who it is.

```
formula              : String
molecularWeight      : @Nullable MolecularWeight    (null for biologicals/chelates)
type                 : CompoundType
phCharacter          : PhCharacter
constituentElements  : Set<PeriodicElement>          (uses chemical symbols: "Ca", "Mg")
```

### `SolubilityProfile`

```
gramsPerLiterAt20C       : Solubility (NumericNamedValue, scale 2)
category                 : SolubilityCategory  (INSOLUBLE, SPARINGLY_SOLUBLE,
                                                SLIGHTLY_SOLUBLE, SOLUBLE,
                                                HIGHLY_SOLUBLE, MISCIBLE)
ecContributionFactor     : BigDecimal
notes                    : String
```

`HIGHLY_SOLUBLE` threshold is **> 100 g/L** (constant on `Solubility`).

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
side. The doc comment on `HazardLevel.EXTREME` still says "not currently in Oak Vista
catalog" — the comment is residue from earlier project naming, not a current
constraint.

---

## 6. Element Sub-package

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

### Element repository / query — INCUBATING

`ElementRepository` is annotated `@Incubating("Investigating a pattern where
EntityRepositories are nested within a single interface")`:

```java
interface ElementRepository {
    interface ElementEntityRepository extends EntityRepository<ElementName, Element> {}
}
```

Note: `ElementRepository` is declared as an `interface` here, not a `class` (which
would be the standard ADR-020 namespace shape). This is **intentional and
experimental** — do not "fix" it to match the namespace pattern without checking with
the user first. `ElementQuery extends EntityQuery<ElementName, Element,
ElementCollection>` is the public consumer surface.

### `ElementCollection`

`final class extends BehavioralCollection<Element>` (ADR-011). Constructor is
package-private; `public static of(Collection<Element>)` and `public static empty()`
are the only entry points.

---

## 7. Reaction Sub-package

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

`ReactionType` is **package-private**: DISSOLUTION, ACID_BASE, OXIDATION,
CATION_EXCHANGE, CHELATION, VOLATILIZATION, BIOLOGICAL, PRECIPITATION. It does not
appear on the public api surface today; promoting it to public is a deliberate change.

### Known gap — `ReactionProfile.invariants()`

Currently validates only `name` and `title`. The other six components
(`equation`, `reactants`, `products`, `type`, `conditions`, `significance`) are not
enforced at the boundary. This is a real gap, not a design choice — flag it if the
user is editing this file.

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

## 8. NumericNamedValue Conventions

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
all three sub-packages can use it without an upward-looking import.

---

## 9. Cross-domain References — by slug only

A consumer of chemistry-api references its entities by name:

```java
CompoundName.of("calcium-sulfate-dihydrate")
ElementName.of("Ca")            // element names use chemical symbols, not slugs
ReactionName.of("gypsum-dissolution")
```

`PeriodicElement` is the only chemistry type a consumer can carry by *value* — it is
an enum, not a `NamedEntity`. Anything else that crosses a domain boundary crosses by
`EntityName`.

`Compound` is never referenced cross-domain by value; consumers fetch by
`CompoundName` through their own port and let the chemistry side resolve.

---

## 10. JSON Catalog Locations (chemistry-repository-test, NOT chemistry-api)

The api ships no JSON. Catalog files live in repository-test resources:

```
chemistry-repository-test/src/main/resources/chemistry/
  compound/compounds.json
  element/elements.json
  reaction/reactions.json
  reaction/reactionProfiles.json
```

Catalog conventions (per chemistry CLAUDE.md):

- `"name": "<slug>"` is the EntityName. No `id` field (ADR-022).
- `compoundInfo`, `solubility`, `bioavailability`, `volatilization`, `safety`,
  `properties` keys mirror Compound's record component names exactly.
- `volatilization` and `safety` may be `null`.
- `PeriodicElement` serializes as chemical symbol (`"Ca"`, `"Mg"`).
- Other enums serialize by constant name (`"INORGANIC_SALT"`, `"ROOT_MASS_FLOW"`,
  `"HIGHLY_SOLUBLE"`).

Do not place catalog data in the api module. Java in api defines schema; JSON in
repository-test defines instances.

---

## 11. Current State — What's Built, What's Not

**Built and stable.**

- `Compound` aggregate with all four profiles.
- `Element` entity + `ElementCollection` + `ElementQuery`.
- `PeriodicElement` enum (118 elements).
- `ReactionProfile` record with `ReactionConditions` + `CationExchangeProfile`.
- All four `NumericNamedValue` types.

**Built but explicitly experimental.**

- `ElementRepository` — `@Incubating`, deviates from ADR-020 namespace shape (declared
  as an `interface`, not a `class`). Do not normalize it without confirmation.

**Missing on the api surface.**

- No `CompoundRepository`, `CompoundQuery`, or `CompoundCollection` exist yet. A
  consumer cannot fetch a compound through chemistry-api today; only the repository-test
  catalog provides instances.
- No `ReactionRepository`, `ReactionQuery`, or `ReactionCollection`.
- The `safety/` subpackage exists but is empty — no public types.
- `ReactionType` is package-private; not part of the cross-module surface.

**Known invariant gap.**

- `ReactionProfile.invariants()` enforces only `name` and `title`. Six other
  components are unguarded.

These are the points where invented APIs are most likely to creep in. If chat is
asked to "look up a compound" or "query reactions", the honest answer is "the query
surface is not built yet — confirm the scope before generating code".

---

## 12. Anti-patterns Specific to chemistry-api

- **Do not invent a `CompoundId`.** Identity is `CompoundName` (slug). Same for
  `ElementId`/`ReactionId` — they don't exist; `ElementName` and `ReactionName` do.
- **Do not put profiles in their own packages.** `SolubilityProfile`,
  `BioavailabilityProfile`, `VolatilizationProfile`, `SafetyProfile` all live next to
  `Compound` in `compound/`. They are aggregate-owned value objects, not standalone
  domains.
- **Do not re-introduce `CompoundProperty` as a `NamedEntity`.** See §4. It's a
  `Map<String,String>` value collection on the aggregate.
- **Do not use `Element` where `PeriodicElement` is called for** (and vice versa).
  `CompoundInfo.constituentElements` is `Set<PeriodicElement>` — symbol enum. The
  `Element` entity carries domain enrichment (ionic state) loaded from JSON.
- **Do not rename `BioavailabilityProfile.isChelateEnhanced`.** Component name = JSON
  field name; the `is` prefix is part of the wire contract.
- **Do not move `TemperatureFahrenheit` into `compound/` or `reaction/`.** It is
  shared across both sub-packages; the api root is the correct package.
- **Do not "fix" `ElementRepository` to be a class** without confirming the
  experiment is over.
- **Do not assume a `Compound` query exists.** It does not. If a task implies one,
  surface that gap before generating code that depends on it.