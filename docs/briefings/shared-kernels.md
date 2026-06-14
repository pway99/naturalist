# Shared Kernels — Chat Briefing

**Purpose.** Covers all shared kernels *except* the framework kernel
(see `framework-core.md` for identity model, `Constraints` API,
namespace patterns; `framework-reference.md` for data-layer ports,
Observer ceremony, and full Constraints table). Upload this alongside
`framework-core.md` and one or more domain briefings when a chat
session needs the full structural vocabulary.

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time (2026-06-14), not
extrapolated. If you need a type not listed here, ask before inventing
one.

---

## 1. field-notes — `com.naturalist.fieldnotes`

Every domain api depends on this kernel. Two types.

### `Description` — ValueObject

```java
public record Description(
    String preschool,
    String elementary,
    String secondary,
    String university
) implements ValueObject
```

The Durrell principle: the same truth at four levels of understanding
(preschool <6, elementary 6–11, secondary 12–18, university 18+). All
four levels are required — never null. Every catalog entity should carry
one unless deliberately omitted.

### `CommonName` — ValueObject

```java
public record CommonName(String label, Locale locale) implements ValueObject
```

Vernacular name + BCP-47 locale. `label` must be non-blank; `locale`
must be non-null. Factories: `CommonName.of(label)` (defaults to
`Locale.ENGLISH`), `CommonName.of(label, locale)` (`@JsonCreator`).
Entities carry `Set<CommonName>`.

---

## 2. taxonomy — `com.naturalist.taxonomy`

Organism domain apis only (insects, plants, etc.). Chemistry, climate,
soil, zone, and sensor domains do not use it.

### Rank value types — `NamedValue<String>` records

| Class                | Convention   | Example         | Validity                            |
|----------------------|-------------|-----------------|-------------------------------------|
| `TaxonomicOrder`     | Capitalised | `"Coleoptera"`  | non-null, non-blank, uppercase first |
| `TaxonomicFamily`    | Capitalised | `"Carabidae"`   | non-null, non-blank, uppercase first |
| `TaxonomicGenus`     | Capitalised | `"Hippodamia"`  | non-null, non-blank, uppercase first |
| `TaxonomicSpecies`   | Lowercase   | `"convergens"`  | non-null, non-blank, lowercase first |
| `TaxonomicSubspecies`| Lowercase   | `"orientis"`    | non-null, non-blank, lowercase first |

All have `@JsonCreator static of(String value)` factory and `@JsonValue`
on `value()`.

### `TaxonomicClassification` — ValueObject

```java
public record TaxonomicClassification(
    TaxonomicOrder order,
    TaxonomicFamily family,
    @Nullable TaxonomicGenus genus,
    @Nullable TaxonomicSpecies species
) implements ValueObject
```

`order` and `family` are required. `genus` and `species` are nullable
to support family-level identifications. Key methods:

- `binomialName()` — genus + species if both present; genus + "sp." if
  genus only; family + "sp." otherwise.
- `isSpeciesLevel()` — `genus != null && species != null`.
- `belongsToGenus(TaxonomicGenus)` — null-safe genus comparison.

### Linnaean role interfaces

Implemented by per-domain rank entity records. Each provides a
`*Slug()` default method for deriving the entity's slug `EntityName`
from the proper-cased epithet via `TaxonomicSlugs`.

| Interface                                  | Parent FK type param     | Key methods                                          |
|--------------------------------------------|--------------------------|------------------------------------------------------|
| `LinnaeanOrder`                            | —                        | `order()`, default `orderSlug()`                     |
| `LinnaeanFamily<ORDER_NAME>`               | parent order name        | `orderName()`, `family()`, default `familySlug()`    |
| `LinnaeanGenus<FAMILY_NAME>`               | parent family name       | `familyName()`, `genus()`, default `genusSlug()`     |
| `LinnaeanSpecies<GENUS_NAME>`              | parent genus name        | `genusName()`, `genus()`, `species()`, default `binomialSlug()` |
| `LinnaeanSubspecies<PARENT extends Named>` | parent species           | `parentSpecies()`, `genus()`, `species()`, `subspeciesEpithet()`, default `trinomialSlug()` |

`LinnaeanSubspecies` has no current implementing entity.

### `LinealRank` — enum

```
KINGDOM, PHYLUM, CLASS, ORDER, FAMILY, GENUS, SPECIES, SUBSPECIES
```

Ordinal = increasing specificity. Organism-agnostic position marker.

### `TaxonomicSlugs` — package-private utility

Static slug derivation: `orderSlug`, `familySlug`, `genusSlug`,
`binomial(genus, species)`, `trinomial(genus, species, subspecies)`.
Lowercases, collapses whitespace/underscores to hyphens.

---

## 3. clades — `com.naturalist.clades`

Depends on `framework` and `field-notes`. Organism domains use it.

### `Clade` — sealed interface (17 permits)

```java
public sealed interface Clade
    permits Eukaryota, Animalia, Anthophila, Apoidea, Arthropoda,
            Blattodea, DrosophilaSensuStricto, Drosophilinae, Hemiptera,
            Holometabola, Insecta, Lepidoptera, Papilionidae,
            Papilionoidea, Sophophora, Termitoidae, Troidini
```

Interface methods:
- `String slug()` — `@JsonValue`
- `String displayName()`
- `Description description()` — four-level Durrell
- `Optional<Clade> parent()` — empty only at `Eukaryota`

`@JsonCreator static Clade of(String slug)` — throws on unknown slugs.

All permits are **stateless no-arg records**. Value equality is
structural (`record equals`/`hashCode`), not singleton identity. Adding
a clade is a deliberate kernel PR, not free-text data entry.

**Parent tree (current):**

```
Eukaryota (root)
└── Animalia
    └── Arthropoda
        └── Insecta
            ├── Holometabola
            │   ├── Lepidoptera
            │   │   └── Papilionoidea
            │   │       └── Papilionidae
            │   │           └── Troidini
            │   ├── Apoidea
            │   │   └── Anthophila
            │   └── Drosophilinae
            │       ├── DrosophilaSensuStricto
            │       └── Sophophora
            ├── Blattodea
            │   └── Termitoidae
            └── Hemiptera
```

### `Trait` — marker interface (open, not sealed)

```java
public interface Trait { }
```

The kernel declares no trait types. Owning domains declare them (e.g.
insects declares `MetabolyTrait`). The recommended shape for the
trait-declaration function is a pattern-matching `switch` over sealed
permits:

```java
public static Set<Trait> insectTraits(Clade c) {
    return switch (c) {
        case Holometabola _ -> Set.of(new MetabolyTrait(HOLOMETABOLOUS));
        case Blattodea _    -> Set.of(new MetabolyTrait(HEMIMETABOLOUS));
        case Hemiptera _    -> Set.of(new MetabolyTrait(HEMIMETABOLOUS));
        default             -> Set.of();
    };
}
```

### `CladeTraversal` — static helpers

- `findTrait(Clade start, Class<T> traitType, Function<Clade, Set<Trait>> traitsFor)`
  — walks parent chain upward, returns nearest ancestor's matching trait.
- `ancestry(Clade start)` — chain from start to root inclusive.

### `CladeCatalog` — static helpers

- `all()` — enumerates all permits via reflection. Automatically tracks
  new permits.
- `childrenOf(Clade parent)` — direct descendants sorted by
  `displayName()`.

---

## 4. habitat — `com.naturalist.habitat`

Depends on `framework` only. Organism domains use it for structured
habitat classification.

### `HabitatProfile` — ValueObject

```java
public record HabitatProfile(
    Set<HabitatZone> zones,
    @Nullable MoistureRegime moisture,
    @Nullable LightRegime light,
    @Nullable Set<VerticalLayer> layers
) implements ValueObject
```

`zones` is required (non-null, non-empty by design intent). All other
axes are nullable — characterisation is incremental. Convenience
predicates: `occupies(HabitatZone)`, `operatesIn(VerticalLayer)`,
`hasMoistureRegime(MoistureRegime)`, `hasLightRegime(LightRegime)`.

### Enum vocabularies

| Enum             | Constants                                                                                        |
|------------------|--------------------------------------------------------------------------------------------------|
| `HabitatZone`    | `MEADOW`, `BARE_GROUND`, `CULTIVATED`, `HEDGEROW`, `WOODLAND_EDGE`, `WOODLAND`, `CHAPARRAL`, `WETLAND`, `RIPARIAN`, `COMPOST_HEAP` |
| `MoistureRegime` | `XERIC`, `MESIC`, `HYDRIC`, `SEASONALLY_XERIC`                                                  |
| `LightRegime`    | `FULL_SUN` (≥6h), `PARTIAL_SUN` (3–6h), `DAPPLED` (filtered canopy), `FULL_SHADE` (<3h)         |
| `VerticalLayer`  | `CANOPY`, `UNDERSTORY`, `SHRUB_LAYER`, `HERBACEOUS_LAYER`, `GROUND_SURFACE`, `SUBTERRANEAN`      |

---

## 5. biogeography — `com.naturalist.biogeography`

Depends on `framework` and `field-notes`. Same sealed-record shape as
`clades`.

### `Bioregion` — sealed interface (6 permits)

```java
public sealed interface Bioregion
    permits SacramentoValley, SouthernCascades, KlamathMountains,
            CoastRanges, SierraNevada, ModocPlateau
```

Interface methods: `String slug()` (`@JsonValue`),
`String displayName()`, `Description description()`.
`@JsonCreator static Bioregion of(String slug)` — throws on unknown.

Flat hierarchy (no `parent()`). All permits are stateless no-arg
records. Names track EPA Level III ecoregion boundaries where they match
vernacular usage.

---

## 6. measurements — `com.naturalist.measurements`

Depends on `framework` only. All types implement `NumericNamedValue`
(a `NamedValue<BigDecimal>` extension with `scale()` and
`roundingMode()`). All use `RoundingMode.HALF_UP`.

| Class                    | Unit    | Scale | Validity                 |
|--------------------------|---------|-------|--------------------------|
| `AreaSquareFeet`         | ft²     | 1 dp  | non-null, > 0            |
| `DepthInches`            | inches  | 2 dp  | non-null, > 0            |
| `ElectricalConductivity` | dS/m    | 2 dp  | non-null, ≥ 0            |
| `MoisturePercent`        | % 0–100 | 1 dp  | non-null, ≥ 0            |
| `PrecipitationInches`    | inches  | 2 dp  | non-null, ≥ 0            |
| `SlopeDegrees`           | degrees | 1 dp  | non-null, ≥ 0, ≤ 90     |

All follow the pattern:
`record Foo(BigDecimal value) implements NumericNamedValue` with
`@JsonCreator static Foo of(BigDecimal value)`.

---

## 7. catalog — `com.naturalist.catalog`

Depends on `framework` only. The kernel knows no domain by name —
domains contribute themselves.

### `DomainId` — open interface

```java
public interface DomainId extends ValueObject {
    String value();   // kebab-case slug, unique per assembly
}
```

Each domain ships its own concrete record in its `-api` module.
`CatalogAssembly` enforces slug uniqueness at startup.

### `EntityRef` — ValueObject

```java
public record EntityRef(DomainId domain, EntityName name) implements ValueObject
```

Domain-agnostic entity reference. Both components required.
`displayLabel()` returns `name.value()`.

### Contribution SPI (implemented in `-core` modules)

| Interface                          | Role                              | Key methods                                |
|------------------------------------|-----------------------------------|--------------------------------------------|
| `CatalogContribution`             | Forward search (entity → tokens)  | `domain()`, `searchableEntities()`         |
| `EntityReferences<T>`             | Inverse lookup (entity ← refs)    | `domain()`, `referenceType()`, `referencesTo(target)` |
| `EntityRefLinker`                  | URL rendering                     | `linkFor(EntityRef)` → nullable String     |

`CatalogContribution.SearchableEntity` is a nested `ValueObject` record
carrying `EntityRef target` and `Stream<String> tokens`.

### `Catalog` — query surface

```java
public interface Catalog {
    SearchResults search(String text);
    Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType);
    Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target);
}
```

Search is case-insensitive, prefix-matching, deduplicated per
`(EntityRef, MatchKind)`.

### `MatchKind` — enum (display priority order)

`EXACT_SLUG`, `EXACT_TOKEN`, `PREFIX`.

### `SearchResults` — BehavioralCollection

`BehavioralCollection<SearchHit>`. Ordered by `MatchKind` ordinal then
slug. Methods: `groupedByDomain()`, `byKind(MatchKind)`, `topN(int n)`.

---

## 8. catalog-inmem — `com.naturalist.catalog.inmem`

Depends on `catalog` and `framework`. In-kernel because its only
dependencies are already in-kernel (no heavy infrastructure).

### `CatalogAssembly` — composition-root factory

```java
CatalogAssembly.from(contributions, providers, resilience)  // → InMemoryCatalog
```

Multiple overloads for convenience. All validate slug uniqueness across
contributions, providers, and an optional explicit domain list.

### `InMemoryCatalog` — package-private, implements `Catalog`

Builds a frozen token index at construction (tokens ≥2 chars,
lowercased, split on whitespace/punctuation). `findReferencesTo` is
`@Resilient(name = "catalog.fanout")` — each provider invocation is
circuit-breaker + timeout wrapped so a wedged provider degrades only
that domain's slice.

---

## 9. authority — `com.naturalist.authority`

Depends on `framework` only. External-authority seam for citation
management.

### `Citation` — sealed NamedEntity (1 permit)

```java
@JsonTypeInfo(use = Id.NAME, property = "kind")
@JsonSubTypes({
    @Type(value = OnlineSource.class, name = "ONLINE_SOURCE")
})
public sealed interface Citation extends NamedEntity<CitationName>
    permits OnlineSource
```

Declares `authorityReference()` and `title()` on the interface.

### `CitationName` — EntityName (max 200)

Lives in `kernels/authority` (`com.naturalist.authority`), not in
`domains/identifiers`. The kernel owns its names.

### `OnlineSource` — the sole permit

```java
public record OnlineSource(
    CitationName name,
    AuthorityReference authorityReference,
    String title,
    @Nullable String author,
    @Nullable Integer year,
    @Nullable Instant lastModified
) implements Citation
```

### `AuthorityReference` — ValueObject

```java
public record AuthorityReference(AuthoritySource source, URI url)
    implements ValueObject
```

Self-describing deep-link into an external authority's catalogue.
`url` is a fully precomputed resolvable URI.

### `AuthoritySource` — ValueObject

```java
public record AuthoritySource(String id, String displayName)
    implements ValueObject
```

Open provider metadata. No concrete sources named in the kernel.

### `ExternalAuthority` — SPI

```java
public interface ExternalAuthority {
    AuthoritySource source();
    Set<AuthorityReference> lookup(EntityName subject);
}
```

Network-backed implementations must be Resilience-wrapped (ADR-026).
In-memory implementations carry `@ResilienceExempt`. Consumers choose
which authority to call — no fan-out across providers.

---

## 10. Cross-Kernel Dependency DAG

```
framework            (no kernel deps)
field-notes          → framework
taxonomy             → framework
habitat              → framework
measurements         → framework
authority            → framework
catalog              → framework
clades               → framework, field-notes
biogeography         → framework, field-notes
catalog-inmem        → framework, catalog
```

Domain api dependency rules:

- **All domain apis** depend on `framework`, `identifiers`, `field-notes`.
- **Organism apis** (insects, plants, etc.) additionally depend on
  `taxonomy`, `habitat`, `clades`, `biogeography`.
- **Domain cores** that contribute to the catalog depend on `catalog`.
- `catalog-inmem` is wired at the composition-root layer only.
- `authority` is consumed by the library domain; other domains reach
  citations through `CitationAssociationQuery`, not through the
  authority kernel directly.

---

## 11. Anti-patterns

- **Do not add domain-specific logic to a kernel.** If it is not truly
  cross-cutting, it belongs in the domain module.
- **Do not declare trait types in the clades kernel.** Traits are
  domain-owned — declared via pattern-matching `switch` in the
  consuming domain.
- **Do not add free-text clade or bioregion permits.** Each permit is a
  deliberate kernel PR. The set is curated, not data-driven.
- **Do not import `catalog-inmem` from domain code.** It is a
  composition-root dependency only.
- **Do not add new `Citation` permits in domain code.** `Citation` is
  sealed in `kernels/authority`; new permits belong there.
- **Do not put `CitationName` in `domains/identifiers`.** Kernel-level
  `EntityName` subclasses stay alongside their entity in the kernel.
- **Do not use raw `double`/`float` for decimal domain values.** Use
  `NumericNamedValue` (backed by `BigDecimal`) from the measurements
  kernel.
- **Do not confuse `TaxonomicClassification` with the Linnaean role
  interfaces.** `TaxonomicClassification` is a value object carrying
  proper-cased epithets; the `Linnaean*` interfaces are implemented by
  per-domain rank entity records and provide slug derivation.
