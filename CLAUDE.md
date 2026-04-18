# The amateur Naturalist

## Architecture Decision Records

All architectural decisions live in [`docs/adr/`](docs/adr/README.md).

| ADR | Title |
|-----|-------|
| [ADR-001](docs/adr/ADR-001-repository-architecture.md) | Repository Architecture |
| [ADR-002](docs/adr/ADR-002-repository-behavioral-contract.md) | Repository Behavioral Contract via Test Interface (Amended) |
| [ADR-003](docs/adr/ADR-003-java-records-no-lombok.md) | Java Records, No Lombok |
| [ADR-004](docs/adr/ADR-004-modular-monolith.md) | Modular Monolith, No Microservices |
| [ADR-005](docs/adr/ADR-005-entity-identity-model.md) | Entity Identity Model — CatalogEntity and FactEntity (Amended ×2) |
| [ADR-006](docs/adr/ADR-006-command-query-separation.md) | Command Query Separation |
| [ADR-007](docs/adr/ADR-007-first-principles-problem-solving.md) | First Principles Problem Solving |
| [ADR-008](docs/adr/ADR-008-iteration-velocity-and-bounded-risk.md) | Iteration Velocity and Bounded Risk |
| [ADR-009](docs/adr/ADR-009-accuracy-precision-and-domain-authority.md) | Accuracy, Precision, and Domain Authority |
| [ADR-010](docs/adr/ADR-010-query-design-contract.md) | Query Design Contract |
| [ADR-011](docs/adr/ADR-011-behavioral-collections.md) | Behavioral Collections |
| [ADR-012](docs/adr/ADR-012-static-factory-construction.md) | Static Factory Construction |
| [ADR-013](docs/adr/ADR-013-value-object-contract.md) | Value Object Contract |
| [ADR-014](docs/adr/ADR-014-named-values.md) | Named Values |
| [ADR-015](docs/adr/ADR-015-bigdecimal-numeric-precision.md) | BigDecimal for Decimal Domain Values |
| [ADR-016](docs/adr/ADR-016-date-time-types.md) | Date and Time Types |
| [ADR-017](docs/adr/ADR-017-observability-monitoring-and-validation.md) | Observability, Monitoring, and Validation |
| [ADR-018](docs/adr/ADR-018-third-party-dependency-policy.md) | Third-Party Dependency Policy |
| [ADR-019](docs/adr/ADR-019-pull-request-size-and-review-fatigue.md) | Pull Request Size and Review Fatigue |

## Pull Request Size Discipline

One concern per PR. When multiple skills scaffold infrastructure for the same entity, they
produce separate PRs merged in dependency order: (1) entity + identifiers, (2) TestEntitySource
+ JSON catalog, (3) repository + mock + behavioral contract. Target ≤ 400 lines of meaningful
diff. The goal is reviewable PRs where a reviewer can hold the entire change in working memory
and catch domain model errors — not small PRs for their own sake. See ADR-019.

## Identity Model

Every domain class implements one of three interfaces from `kernels/framework`:

- **Entity\<ID extends PersistenceId\<?\>, NAME extends EntityName\<?\>\>** — stable identity, `id()`, non-nullable `name()`, `withId(ID)`,
  `invariants()`
- **Aggregate** — consistency boundary, owns child entities and value objects
- **ValueObject** — immutable, no identity, equality by value. Must satisfy all four
  constraints in ADR-013: no Entity/Aggregate members, must not uniquely identify an
  entity, cohesive domain concept in the ubiquitous language, members have collective
  meaning (not a projection of an entity's fields).
- **BehavioralCollection\<T extends Observable\>** — abstract base class in `kernels/framework`
  for multi-result query return types. Extended by `final class` per domain (e.g.
  `CompoundCollection`). Not a record. See ADR-011.

All four extend `Observable` and require `invariants()`.

### PersistenceId\<T\>

Parameterized abstract class wrapping the persistence identity type (always `Long`).
`PersistenceId` is null in JSON catalogs — assigned by `TestEntitySource.nextId()` or RDBMS.
Concrete subclasses live in `domains/identifiers/` (e.g. `CompoundId extends PersistenceId<Long>`).

### EntityName

Abstract class wrapping a `String` — the domain's human-readable natural key (slug) for
catalog entities, or a UUID string for fact entities (`FactName` subclass).
Never null on any entity. Concrete subclasses live in `domains/identifiers/` alongside
their PersistenceId counterparts. The `NAME` type parameter on `Entity<ID, NAME>` gives
call sites the concrete name type without a cast.

### Field Annotations

- `@EntityIdentifier` — marks a *secondary* `EntityName` component as a unique identifier
  within its data source. The canonical `name()` component (the `NAME` type parameter) is
  automatically enforced as unique by `TestEntitySource` — do not annotate it. Use
  `@EntityIdentifier` only on additional `EntityName` fields that must also be unique.
  Cross-domain FK
  `EntityName` references carry no annotation.
- `@UniqueValue` — marks plain value components (`String`, `int`, enums) that must be unique.
  Both `@EntityIdentifier` and `@UniqueValue` fields must be declared explicitly in the
  subclass `uniqueConstraints()` method.

## Java Record Conventions

Entity, Aggregate, and ValueObject are Java records. No Lombok.

**Exception — BehavioralCollection:** Behavioral collections are `final class`, not
records. Records require the canonical constructor to be at least as accessible as the
class itself, which prevents package-private instantiation. `final class` enables a
package-private constructor with `public static of(...)` factory methods as the only
external instantiation path. See ADR-011 and ADR-012.

### Entity record pattern

```java
public record Foo(
        FooId id,
        FooName name,
        String someField
) implements Entity<FooId, FooName> {

    @Override
    public Foo withId(FooId id) {
        return new Foo(id, name, someField);
    }

    @Override
    public Consumer<? extends Invariants> invariants() {
        return i -> i
                .entityId(id(), "id")
                .entityName(name(), "name")
                .notBlank(someField, "someField");
    }
}
```

### Aggregate record pattern

Aggregates have no `withId()` — write explicit `with*` methods for each field:

```java
public record FooAggregate(
        FooInfo fooInfo,
        List<ChildEntity> children
) implements Aggregate {

    public FooAggregate withFooInfo(FooInfo fooInfo) {
        return new FooAggregate(fooInfo, children);
    }

    public FooAggregate withChildren(List<ChildEntity> children) {
        return new FooAggregate(fooInfo, children);
    }

    @Override
    public Consumer<? extends Invariants> invariants() {
        return i -> i
                .entity(this, FooAggregate::fooInfo, "fooInfo")
                .notNull(children, "children");
    }
}
```

### ValueObject record pattern

A ValueObject must satisfy all four constraints (ADR-013): no Entity/Aggregate members,
does not uniquely identify an entity, cohesive ubiquitous-language concept, members have
collective meaning and are not a projection of an entity.

```java
public record Bar(String x, int y) implements ValueObject {
    @Override
    public Consumer<? extends Invariants> invariants() {
        return i -> i.notBlank(x, "x");
    }
}
```

### BehavioralCollection pattern

Behavioral collections are `final class`, extend `BehavioralCollection<T extends Observable>`
from `kernels/framework`, and live in `<domain>-api`. Constructor is package-private;
`public static of(...)` and `public static empty()` are the only external instantiation
paths. `List.copyOf` defensive copy is inherited from the base class constructor.
Domain-specific filtering methods return new instances via the package-private constructor.
See ADR-011 and ADR-012.

```java
public final class CompoundCollection extends BehavioralCollection<Compound> {

    CompoundCollection(Collection<Compound> compounds) {  // package-private
        super(compounds);
    }

    public static CompoundCollection of(Collection<Compound> compounds) {
        return new CompoundCollection(compounds);
    }

    public static CompoundCollection empty() {
        return new CompoundCollection(List.of());
    }

    public CompoundCollection withSolubilityAbove(double thresholdGramsPerLitre) {
        return new CompoundCollection(
                stream().filter(c -> ...).toList()
        );
    }
}
```

### Key rules

- Accessor names match component names exactly: `id()`, `name()`, `someField()` — never `getId()`, `getName()`, `getSomeField()`
- Boolean components use plain names: `active`, `beneficial` — accessors are `active()`, `beneficial()`. Predicate methods use `is*` prefix: `isActive()`, `isBeneficial()` are fine as long as they are behavior methods, not component accessors
- `withId(ID id)` is the only with method on Entity — all others are explicit on the concrete record
- Jackson 2.19.x natively deserialises records via `Class.getRecordComponents()`. No `@JsonCreator` needed on entity, aggregate, or value object records. JSON field names must match component names exactly
- `@JsonCreator` is required on the `public static of(...)` factory method of any non-record type that Jackson must deserialise — specifically `PersistenceId<Long>` subclasses, `EntityName` subclasses, and `NamedValue<T>` implementations. Without it, Jackson cannot locate the factory and deserialisation fails silently or with a misleading error
- `@EntityIdentifier` placed before the type in the component list: `@EntityIdentifier FooName name`
- Optional-returning query methods must not share a name with any component: if a component is `String biologicalCatalyst`, the Optional accessor must be `biologicalCatalystOptional()`, not `biologicalCatalyst()`
- Custom `equals`/`hashCode`/`toString` may be declared inside a record to override the auto-generated component-based implementations (e.g. `Element` uses identity-based equality on `id` only)
- Static factory methods (`of(...)`, `empty()`, `from(...)`) are the public instantiation API for all domain types — `new Foo(...)` at a call site outside the type's own class is a review flag. See ADR-012.

## Testing Observables

Every domain type (Entity, Aggregate, ValueObject) implements `Observable` and declares
`invariants()`. Unit tests verify invariants via `Observer` → `MethodObserver` →
`InvariantObservation` — never by calling `isValid()` on individual `Invariant` objects.
The `InvariantObservation` gives you the full set of failing invariant names in a single
assertion, eliminating the need to debug which constraint broke.

### Setup

One `Observer` per test class (static field). One `MethodObserver` per test method:

```java
class FooTest {
    private static final Observer observer = Observer.forClass(FooTest.class);

    @Test
    void fooIsValid() {
        var mo = observer.forMethod("fooIsValid");
        Foo f = new Foo(null, FooName.of(RandomValue.string()), RandomValue.string());

        InvariantObservation result = mo.observable(f, "f");

        assertThat(result.invalidInvariants()).isEmpty();
    }
}
```

### Valid case

Construct the object with all required fields populated using `RandomValue` helpers where field constraints permit.
`id` is null (persistence-assigned). Nullable profiles/components are null. Assert
`invalidInvariants().isEmpty()`.

### Invalid case

Construct the object with nulls for every component. Use `invalidInvariantNamesRemovingPrefix`
with `mo.observationPoint()` to strip the test-infrastructure prefix and assert the exact
set of domain-relative invariant paths:

```java
@Test
void fooIsNotValid() {
    var mo = observer.forMethod("fooIsNotValid");
    Foo f = new Foo(null, null, null);

    InvariantObservation observation = mo.entity(f, "f");

    assertThat(observation.invalidInvariantNamesRemovingPrefix(mo.observationPoint()))
            .containsExactlyInAnyOrder(".f.name", ".f.someField");
}
```

### Key rules

- `mo.forMethod(...)` must match the test method name exactly — it scopes the observation
- Use `mo.entity(e, label)` for Entity/Aggregate, `mo.observable(o, label)` for any Observable
- `RandomValue.string()`, `RandomValue.bigDecimal()` for required non-null fields
- `id` is always null in test construction (persistence-assigned)
- Invalid-case assertions use `containsExactlyInAnyOrder` — the exact set, no extras, no missing
- `observationPoint()` returns `ClassName.methodName` — the prefix before the label segment

## Test Identifiers

`domains/test-identifiers` contains one `Test<Domain>Identifiers` class per domain.
These classes are the single source of truth for EntityName constants used in repository
contract tests. They must never be inlined into test classes.

### Structure rules

- Class structure mirrors the domain object graph — ownership and composition relationships
  are reflected as nested static classes.
- **Child entities nest inside their parent species/entity class — never as a sibling
  top-level class.** An `InsectImage` owned by `PotatoLeafhopper` goes inside
  `PotatoLeafhopper.Images`, not in a parallel `InsectImages` class at the same level as
  `InsectSpecies`. A flat sibling class implies peer status in the domain graph, which is wrong.
- Every entity type must define **at least two** known `EntityName` constants, enabling both
  single-entity and set-based lookup tests. Partial-match tests for `getByEntityNameSet`
  and `getByIdSet` must include at least two known values plus the `NotFound` value — a
  single known value does not distinguish partial-match behavior from single-entity lookup.
- Each parent scope defines a **single** `NotFound` inner class containing one fictitious
  `EntityName` constant per entity type within that scope. Child scopes do not define their
  own `NotFound` — the parent's `NotFound` covers them all.
- Fictitious names must be obviously synthetic — `"unobtainium-oxide"`, `"Xx"` — so they
  can never accidentally collide with real catalog data added later.
- Element names in a compound's `Elements` inner class must reference the top-level
  `Elements` constants, not re-declare `ElementName.of(...)` inline.

```java
// Child entity identifiers nest under their parent — not as a sibling top-level class
// A single NotFound at the parent scope covers all entity types within it
public static class InsectSpecies {
    public static class NotFound {
        public static final InsectSpeciesName name = InsectSpeciesName.of("unobtainium-beetle");
        public static final InsectImageName imageName = InsectImageName.of(
                UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }
    public static class PotatoLeafhopper {
        public static final InsectSpeciesName name = InsectSpeciesName.of("potato-leafhopper");

        public static class Images {
            public static class Img9047 {
                public static final InsectImageName name = InsectImageName.of(
                        UUID.fromString("0066fe0f-a3e0-40d8-b557-c42f13e67067"));
            }
        }
    }
}

// Correct usage in a repository contract test
repository().getByName(TestChemistryIdentifiers.Compounds.PotassiumSulfate.name);
repository().getByName(TestChemistryIdentifiers.Compounds.NotFound.name); // empty-result case
repository().getByInsectSpeciesName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);
repository().getByName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.Images.Img9047.name);
```

## Test Entity Sources

Test data follows the model/data separation pattern: Java defines schema, JSON defines instances.

### Creating a new TestEntitySource

Use `/test-entity-source <EntityClassName> in <domain> module` to scaffold all three files.

The `TestEntitySource<ID, NAME, ENTITY>` enforces:

- **Primary key constraint** — duplicate `PersistenceId` on insert throws `PrimaryKeyConstraintException`
- **Name uniqueness** — the canonical `name()` is automatically checked on every insert; no
  subclass declaration required
- **Secondary unique constraints** — declared per entity via `uniqueConstraints()`, which
  defaults to `List.of()`. Override only for fields annotated `@EntityIdentifier` (secondary
  unique `EntityName` fields) or `@UniqueValue` (plain value fields).

Manual steps documented below for reference:

1. **Entity record** — implement `Entity<SomeId, SomeName>` as a Java record in the domain api module.
   Do not annotate the canonical `name` component with `@EntityIdentifier`.
2. **Identifier classes** — add `SomeId extends PersistenceId<Long>` and `SomeName extends EntityName`
   to `domains/identifiers/` in the entity's package. Both need `@JsonCreator` factory methods.
3. **JSON catalog file** — create in `<domain>-repository-test/src/main/resources/<domain>/<subpackage>/`
   (resource sub-directory mirrors the Java sub-package under the domain, e.g.
   `chemistry/compound/compounds.json` for `com.naturalist.chemistry.compound`):
    - `"id": null` — persistence ID is always null in catalog data
    - `"name": "<slug>"` — the EntityName natural key (e.g. `"calcium-sulfate-dihydrate"`)
    - Remaining fields match the record component names exactly
    - Enum values serialize by constant name (e.g. `"ROOT_MASS_FLOW"`, `"INORGANIC_SALT"`)
    - For `PeriodicElement` enum, use chemical symbols (`"Ca"`, `"Mg"`, `"K"`)
    - When extracting profiles from `chemical-science/catalog/compounds.json`, include
      a `compoundName` field with the parent compound's slug for association
4. **TestEntitySource subclass** — create in `<domain>-repository-test/src/main/java/`.
   Pass the `PersistenceId` factory as a method reference to `super()`.
   `valueFunction()` lambdas use record component method references (`Foo::name`, not `Foo::getName`).
   Do not declare a constraint for `name` — it is enforced automatically:
   ```java
   public class FooTestEntitySource extends TestEntitySource<FooId, FooName, Foo> {
       public FooTestEntitySource() {
           super(FooId::of);
           loadFile("<domain>/<subpackage>/foos.json");
       }

       // Omit uniqueConstraints() entirely if Foo has no @EntityIdentifier or @UniqueValue fields
   }
   ```
5. **Test class** — create in `<domain>-repository-test/src/test/java/`:
   ```java
   class FooTestEntitySourceTest extends TestEntitySourceTest<FooId, FooName, Foo, FooTestEntitySource> {
   }
   ```
   This verifies JSON deserialization succeeds and the source is non-empty.

### Data Extraction Pattern

When extracting child entity data from `chemical-science/catalog/compounds.json` into
standalone JSON catalog files:

1. Extract the nested object fields verbatim — do not rename or restructure
2. Add `"id": null` (persistence-assigned)
3. Add `"compoundName": "<compound-slug>"` to associate back to the parent compound
4. Place the file in `<domain>-repository-test/src/main/resources/<domain>/<subpackage>/`

## Module Structure

```
kernels/
  framework/          — Entity, PersistenceId, EntityName, Aggregate, ValueObject, Observable, Observer,
                        BehavioralCollection<T extends Observable>
  framework-test/     — TestEntitySource, TestEntitySourceTest, TestDataHelper
  field-notes/        — Description (four-level Durrell description, used by all catalog domains)
  taxonomy/           — TaxonomicClassification (Linnaean taxonomy, used by organism domains only)

domains/
  identifiers/        — typed IDs and names only (PersistenceId/EntityName subclasses, no value objects)
  chemistry/
    chemistry-api/    — domain entities, aggregates, value objects, enums
    chemistry-core/   — (future) domain services
    chemistry-repository-test/ — TestEntitySources + JSON catalogs + tests

chemical-science/
  catalog/            — legacy compound catalog (compounds.json) — source of truth for extraction
```

## Creating a New Module

When creating a new kernel or domain module:

1. **Create directory structure and pom.xml:**
   - `kernels/<module>/pom.xml` (with parent `<artifactId>kernels</artifactId>`) or `domains/<domain>/<domain>-<type>/pom.xml` (with parent `<artifactId><domain></artifactId>`)
   - Create `src/main/java/com/naturalist/<module>/` directory
   - Parent pom.xml should specify dependencies (framework, identifiers, etc. for kernels; framework, identifiers for apis; nothing for cores/tests)

2. **Add to parent module pom.xml:**
   - Kernel: Add `<module><module-name></module>` to kernels/pom.xml in `<modules>` section, alphabetically ordered
   - Domain: Add `<module><type></module>` to domains/<domain>/pom.xml in `<modules>` section (e.g., `<module>chemistry-api</module>`, `<module>chemistry-core</module>`, `<module>chemistry-repository-test</module>`)

3. **CRITICAL: Add dependency-management entry to root pom.xml:**
   - Add `<dependency>` entry to root pom.xml's `<dependencyManagement>` section
   - Kernels: Place in KERNELS section (first in file), alphabetically ordered
   - Domains: Place in appropriate DOMAIN section (e.g., CHEMISTRY, SOIL, ZONE), alphabetically ordered within that section
   - **Version must always be `${project.version}`** to sync with root version
   - This is **required** — all other modules depend on this entry for consistent versioning

4. **Update other pom.xml files that depend on the new module:**
   - Add the module as a dependency in `<dependencies>` (no version tag — inherited from dependencyManagement)
   - Use alphabetical ordering within the dependencies section

**Example:** When `kernels/measurements/` was created:
   - Created kernels/measurements/ with pom.xml depending on framework
   - Added `<module>measurements</module>` to kernels/pom.xml (between habitat and end)
   - **Added measurements to root pom.xml dependencyManagement** in KERNELS section (between identifiers-test and taxonomy) — this step was initially missed
   - Updated soil-api, sensors-api, zone-api, weather-api pom.xml files to depend on measurements

**Note:** Forgetting step 3 causes build failures when other modules try to reference the new module, as Maven cannot find a version in dependencyManagement.

## PeriodicElement Enum

`PeriodicElement` in `chemistry-api` uses IUPAC chemical symbols as enum constant names
(`Ca`, `Mg`, `K`, not `CALCIUM`). Full element name is in `elementName()` field.
All 118 elements included. Used for `Set<PeriodicElement> constituentElements` on `CompoundInfo`
(a `ValueObject` nested inside `Compound`).

## CompoundInfo and Compound

`CompoundInfo` is a `ValueObject` — chemical classification facts with no identity of their
own: formula, molecular weight, type, pH character, and constituent elements. It has no
repository and no `TestEntitySource`. It exists only as a component of `Compound`.

`Compound` is the `CatalogEntity<CompoundId, CompoundName>` and `@AggregateRoot` — the
single identity and consistency boundary. It carries `id`, `name` (slug), and
`@UniqueValue String commonName`, and owns `CompoundInfo` plus all value
object profiles (`SolubilityProfile`, `BioavailabilityProfile`, `VolatilizationProfile`,
`SafetyProfile`) and the `Map<String, String> properties`. All of these have no independent
lifecycle and no cross-domain identity; they exist only as attributes of their parent compound.
`Compound` has a `CompoundTestEntitySource`. Instances are in `compounds.json`.

`@AggregateRoot` lives on `Compound`. If code places `@AggregateRoot` on `CompoundInfo`,
that is an error — correct it.

`molecularWeight` is `@Nullable Double` on `CompoundInfo` because some compound categories
(biological complexes, chelates) do not have a defined molar mass.

## Compound Properties

`Compound` owns a `Map<String, String> properties` — an open-ended set of named attributes
(e.g. `omriListed`, `safeRateLbsPer1000Sqft`, `biologicalCatalyst`). These are
compound-scoped key-value pairs with no independent lifecycle and no cross-domain identity.
Persisted via `@ElementCollection` at the RDBMS adapter layer.

`CompoundProperty` was previously modeled as a standalone `CatalogEntity`. That model
was wrong on two counts:

1. `CatalogEntity` requires a globally unique `name()`. The property key (e.g. `"omriListed"`)
   is unique only within a compound — not globally. Multiple compounds share the same key
   names, which violated `TestEntitySource` uniqueness enforcement.
2. A compound property has no independent lifecycle. It only exists as an attribute of its
   parent compound, is never referenced cross-domain by identity, and has no repository of
   its own. This is a value collection, not an entity.

The correct analogy is `CompoundInfo#constituentElements` (`Set<PeriodicElement>`): both are
aggregate-owned collections with no entity identity. The RDBMS adapter will persist
`properties` as an `@ElementCollection` with `@MapKeyColumn` — the same structural pattern
as the elements join table, with an extra value column. The domain model is not driven by
the persistence concern.

Do not re-introduce `CompoundProperty` as an entity. If a specific property needs typed
behavior or cross-domain visibility, promote it to an explicit field on `Compound` or
`CompoundInfo` with its own accessor.

## Description ValueObject

`Description` is a ValueObject carrying the same truth at four levels of understanding.
Owned by every catalog entity — compounds, organisms, climate events, plant species.
Levels: preschool, elementary, secondary, university.
The Durrell pattern — wonder grows into science without contradiction.

## NaturalistJournal Application Module

`NaturalistJournal` (The Gift) presents the entire domain through Gerald Durrell's lens —
wonder, observation, ecological relationship — rather than management utility.
`Multi-Level Description` is the mechanism: preschool through university descriptions on every
catalog entity. The same truth at different resolutions. A child grows into the science
without ever being pushed.

## Module Dependency Rules

DAG (no cycles — call out violations immediately):

```
bootstrap                   →  application
<domain>-repository-test    →  <domain>-api
<domain>-repository-rdms    →  <domain>-api
<domain>-core               →  <domain>-api
<domain>-api                →  framework, identifiers, field-notes
<organism>-api              →  framework, identifiers, field-notes, taxonomy
identifiers                 →  framework
field-notes                 →  framework
taxonomy                    →  framework
framework                   →  (nothing — external libs only)
framework-test              →  framework
```

Hard rules:

1. **api modules may only depend on `framework`, `identifiers`, `field-notes`, and (for organism domains) `taxonomy`.** No other inter-module dependencies in api modules.
2. **Domain modules access other domains only via their api.** A core module may import from a foreign domain's api; never from core, repository-test, or repository-rdms.
3. **repository modules depend only on their own api** (plus framework).
4. **bootstrap depends on everything; nothing depends on bootstrap.**
5. **Repository interfaces are package-private** in the api module — inter-domain interaction goes through public service/query classes.
6. **Repository behavior contracts** are defined as interfaces in the repository-test module, implemented by both the in-memory and rdms adapters.

Package-private visibility is the primary enforcement mechanism. ArchUnit tests may verify the DAG at build time.

## Repository Architecture

See [ADR-001](docs/adr/ADR-001-repository-architecture.md) and [ADR-002](docs/adr/ADR-002-repository-behavioral-contract.md).

Key constraints for quick reference:
- Repository interfaces are **package-private** in `<domain>-api`
- A repository has exactly four responsibilities: entity cache, referential integrity, unique constraints, transactional consistency — no logic
- `NaturalistDatabase` is the only object permitted to instantiate `TestEntitySource` instances
- Cross-domain references use `EntityName` slug — never `PersistenceId<Long>`
- Cross-domain joins are prohibited; cross-domain FK enforcement is deferred to the RDBMS layer

### Repository Super-Interface Pattern

Each package in a domain api module that contains entities declares a single package-private
repository super-interface. Entity repository interfaces are nested within it. This reduces
api surface noise and provides a single discoverable entry point per package namespace.

```java
@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface PlantRepository {
    interface PlantEntityRepository extends EntityRepository<PlantId, PlantName, Plant> {}
}
```

Key rules:
- **One repository super-interface per package** — it groups all entity repositories for
  entities within that package. Entities in different packages (sub-contexts) get their own
  super-interface in their own package.
- **Package-private** — the super-interface and all nested interfaces are package-private.
- **Nested interfaces extend `EntityRepository<ID, NAME, ENTITY>`** — one per entity in
  the package.
- **`@Incubating`** — the pattern carries this annotation while under evaluation.

### Repository Behavioral Contract

Every `EntityRepository` in the system has a behavioral contract defined as a `@Test default`
interface in `<domain>-repository-test/src/main/java/`. Domain-specific contract interfaces
extend `EntityRepositoryContractTest<ID, NAME, ENTITY>` from `kernels/framework-test`, which
provides all 22 standard test cases. The concrete interface supplies only identity constants
and entity construction hooks — no test logic.

The contract covers all six `EntityRepository` methods. Each select method requires three
test cases per ADR-002: argument validation (null rejection via `InvariantViolationException`),
empty result (not-found), and expected result. Write methods (`insert`, `update`) follow the
same three-case pattern with constraint violations and entity-not-found replacing empty result.

Insert and update expected-result tests observe the persisted entity via the Observer
framework (ADR-017), walking its full constraint graph to catch adapter serialization drift.

#### Concrete test interface hooks

| Hook | Purpose |
|------|---------|
| `repository()` | The repository under test |
| `source()` | The `TestEntitySource` backing the test data |
| `notFoundName()` | A fictitious `NAME` guaranteed absent from the catalog |
| `knownEntityNames()` | At least two known `NAME` constants from the test data |
| `notFoundId()` | An `ID` guaranteed absent (typically `XxxId.of(Long.MAX_VALUE)`) |
| `newEntity()` | A valid entity with null id and unique name, using `RandomValue` where field constraints permit |
| `ghostEntity()` | An entity with a non-existent id, using `RandomValue` where field constraints permit |
| `modifiedEntity(original)` | The original with every mutable field changed via `RandomValue` |

`assertEntityEquals` defaults to recursive comparison ignoring `"id"` — override for
entities with custom equality semantics.

#### Set-based partial-match convention

Partial-match tests for `getByEntityNameSet` and `getByIdSet` must include **at least two
known values plus one not-found value**. A single known value does not distinguish
partial-match behavior from single-entity lookup.

#### Update expected-result convention

The update expected-result test must modify **every mutable field** to a value distinct from
the original using `RandomValue` helpers where field constraints permit. `PersistenceId` and `EntityName` are immutable
— carried forward from the original and asserted unchanged. If the entity carries a foreign
key `EntityName` referencing another entity, the referenced entity must exist in the test
data. Equality is verified via recursive structural comparison. The Observer walks the full
constraint graph of the persisted entity — no hand-written field-level assertions.

### Creating a New Entity Repository

Use `/entity-repository <EntityClassName>` to scaffold the full repository stack.

## Design Patterns

### Strongly Typed Identifiers

Every entity has a concrete `PersistenceId<Long>` subclass. Never raw `String` or `Long` as an entity reference across any boundary. Identifiers live in `domains/identifiers/`.

```java
// CORRECT
repository.findByZone(ZoneId.of("backyard-garden"));

// WRONG — compile error by design
repository.findByZone("backyard-garden");
repository.findByZone(SensorId.of("wh51-backyard")); // wrong type
```

### Package-Private Sub-Context Enforcement

Within a module, sub-contexts enforce boundaries via Java visibility.
`package-private` = internal to sub-context. `public` = explicitly crosses sub-context boundary.

```
soil/
  observation/
    SensorReading.java       ← package-private (internal)
    ObservationService.java  ← public (crosses boundary)
  event/
    AmendmentEvent.java      ← package-private (internal)
    EventService.java        ← public (crosses boundary)
```

### Observability Framework

All domain types implement `Observable`, which requires `Consumer<? extends Invariants> invariants()`. `Invariants` is a fluent builder with constraint methods in two forms:

**Direct-value form** — pass the component value directly. Use this inside `invariants()` on the record that owns the field (i.e. when `this` is the enclosing record):

- `notNull(value, name)` — general null check
- `notBlank(value, name)` — rejects null, empty, and whitespace-only Strings (`StringUtils.isNotBlank`)
- `entityId(id, name)` — validates `PersistenceId<?>`
- `entityName(e, name)` — validates `EntityName`

**By-function form** — pass the parent object and a method reference. Use this only when evaluating a child property from outside the parent (e.g. `entity(this, FooAggregate::fooInfo, "fooInfo")` to descend into a child Observable):

- `entity(o, fn, name)` — validates an `Entity<?>` child and descends into its invariants
- `valueObject(o, fn, name)` — validates a `ValueObject` child and descends into its invariants
- `notNull(o, fn, name)` — general null check on a child property
- `notBlank(o, fn, name)` — not-blank check on a child String property
- `namedValue(o, fn, name)` — validates a `NamedValue<?>` child property
- `entityId(o, fn, name)` — validates a child `PersistenceId<?>`

Do not use the by-function form when the direct-value form suffices — the functional indirection adds no value when `this` can never be null.

`Observer` validates entities at insertion points (`TestEntitySource.insert()` calls `observer.throwWhenInvalid(entity)`). Use `@Nullable` from JSpecify (`org.jspecify`) for nullable field documentation.

### Immutable Value Objects for Domain Facts

Observations and Events are immutable facts — they happened and cannot be changed. Java
record or final fields, no setters. Equality by value, not identity.

A ValueObject must satisfy all four constraints from ADR-013 — no Entity/Aggregate
members, does not uniquely identify an entity, cohesive ubiquitous-language concept,
members have collective meaning. A type that fails any constraint must be reclassified:
aggregate if it owns child entities, `EntityName` subclass if it uniquely identifies,
projection/DTO if it is a subset of an entity's fields.

### Domain Knowledge as Code

Agronomic rules are code, not configuration. `OptimumRanges`, `Amendments`, `Amendment.evaluateSafety()` — these are domain invariants, not configurable parameters.

### Real Data Test Fixtures

Test fixtures use actual measurements. `TestEntitySource` seeds repositories with real FGL data, real sensor readings, real amendment history. A failing test indicates a domain model error or a real-world change.

### Secondary Indexes in In-Memory Repositories

Each repository manages its own secondary indexes for query patterns. No cross-repository queries within a sub-context. No cross-sub-context repository access.

```java
private final Map<ZoneId, List<AmendmentEventId>> byZone = new HashMap<>();
```

## Open Design Questions

**Q0: Aggregate and the CatalogEntity/FactEntity model** — `Aggregate` sits orthogonal to the CatalogEntity/FactEntity identity classification. A `Colony` (Apiary) or `NaturalistJournal` is an Aggregate (consistency boundary) but may also be a `CatalogEntity` (stable, named, referenced). These concerns should compose cleanly, but the relationship between `Aggregate`, `CatalogEntity`, and `FactEntity` needs an explicit ADR before Aggregate roots are implemented. Defer until CatalogEntity/FactEntity implementation is complete.

**Q1: IrrigationEvent repository adapter** — PostgreSQL: standard table, TimescaleDB hypertable, or separate time-series store. Current decision: deferred; in-memory adapter sufficient.

**Q2: Elemental Sulfur amendment type** — no dedicated `ELEMENTAL_SULFUR` type yet; placeholder uses potassium sulfate. Add before writing nitrogen status computation.

**Q3: Nitrogen status computation** — needs biological amplification factor from Zone. Cross-module dependency (Soil → Zones) is valid in DAG; should be mediated through a `ZoneService` port.

**Q4: Insects vs Apiary (RESOLVED)** — Apiary is its own domain module (Colony aggregate root). Insects module covers Insecta (six legs). SHB control: H. indica only (not S. feltiae).

**Q5: Plants growth form vs category** — `GrowthForm` enum for tree/vine/etc. Proposed categories: FruitTree, FruitVine, VegetableCrop, CoverCrop, OrnamentalWoody, PollinatorPlant. `PlantRole` as many-to-many. `SeedLineage` for the Italian Pear adaptation program. Entity model sketched, no Java written.

**Q6: Custom WH51 calibration** — factory calibration is for mineral soil; Oak Vista worm casting/coco coir blend reads 3-5% high. AD values usable for custom calibration curve. Deferred pending gravimetric correlation study.

**Q7: Event-based production adapter** — append-only PostgreSQL tables for `AmendmentEvent`, `SensorReading`, `IrrigationEvent` as a middle path. Deferred; hexagonal architecture makes this a swap of adapters.

## Pending Implementation

Priority order:

1. **BehavioralCollection abstraction** — implement `BehavioralCollection<T extends Observable>`
   in `kernels/framework`. Abstract base class, `protected` constructor, `stream()`,
   `isEmpty()`, `size()`, `invariants()`. Bounded context: `kernels/framework` only.
   See ADR-011, ADR-012.
2. **ObservationService** — `latestReading(SensorId)`, `readingsBetween(SensorId, Instant, Instant)`, `latestLabAnalysis(ZoneId)`, `nutrientStatusFor(ZoneId, Nutrient)`
2. **EventService** — `recordAmendment(...)`, `recordIrrigation(...)`, `totalNitrogenApplied(ZoneId)`, `amendmentHistory(ZoneId, from, to)`
3. **IrrigationEvent** — `isLeachingIrrigation()`, correlation with sensor drainage curve
4. **TillageEvent** — `biologicalImpact()` → HIGH/MODERATE/LOW, `estimatedStructureRecovery()` → Duration. Current instance: April 6, 2026 rototill, Box 1 and Backyard.
5. **StateProjectionService** — computes current `SoilState`. Depends on ObservationService + EventService. Nitrogen status computation.
6. **NitrogenStatus** — current excess diagnosis. Must account for biological amplification factor from Zone. Critical: April 6, 2026 blood meal over-application.
7. **OakVistaZoneFixtures** — populate ZoneRepository with real property zones (substrate types, sun exposure, aspect per zone).

## Measurement Standards

| Concept         | Unit              | Notes                       |
|-----------------|-------------------|-----------------------------|
| Soil nutrients  | lbs per 1000 sqft | FGL reporting standard      |
| EC              | dS/m              | Soil salinity               |
| pH              | dimensionless 0-14| Log scale                   |
| Soil moisture   | % volumetric      | Sensor-derived              |
| Area            | square feet       | Primary unit                |
| Temperature     | Fahrenheit        | Sensor data + thresholds    |
| Molecular weight| g/mol             | Chemistry module            |
| Solubility      | g/L at 20°C       | Chemistry module            |
