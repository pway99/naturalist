# ADR-005: Entity Identity Model — CatalogEntity and FactEntity

**Status:** Accepted (Amended ×2, Amendment 3 Draft)

## Context

Eric Evans defines an Entity as an object defined not by its attributes but by a thread of
continuity and its identity. Identity is what distinguishes one Entity from another of the
same type, even when all observable attributes are identical.

The original version of this ADR split the identity model into two incompatible branches:
`CatalogEntity` carried a non-nullable `EntityName` (slug), while `FactEntity` had no name
at all — `PersistenceId` was its sole identity. This forced the framework to duplicate
its infrastructure:

- `CatalogEntityRepository` vs `FactEntityRepository` vs `EntityRepository`
- `TestEntitySource` vs `TestEntitySource`
- `AbstractCatalogTestEntityRepository` vs `AbstractTestEntityRepository`

The duplication existed to solve a single problem: catalog entities had name-based lookups
and fact entities did not. But the real question was never "does this entity have a name?"
— it was "what *kind* of name does this entity have?" A compound has a human-readable
slug. An amendment event has no slug, but it still needs a globally unique identifier for
deduplication, especially when observations are collected offline and published later.

The original decision to strip `name()` from `FactEntity` also created a practical
problem: the synthetic slugs that were removed ("blood-meal-backyard-2026-04-06") were
ugly and wrong, but the underlying need — a unique, non-PersistenceId identifier on every
entity — was real. Stripping the name removed the symptom and the signal.

### What we learned

The Catalog/Fact distinction is a **domain** concern about what kind of thing an entity
represents. It is not an **infrastructure** concern about whether the entity carries a
name. Every entity needs a name. The difference is the kind of name:

- A catalog entity's name is a **human-readable slug** — stable, meaningful, known to the
  domain before the entity is persisted. "potassium-sulfate", "Ca", "backyard-garden".
- A fact entity's name is a **globally unique identifier (GUID)** — machine-generated,
  opaque, assigned at creation time (not at persistence time). It provides deduplication
  and identity without encoding mutable state.

This means the framework does not need to bifurcate its infrastructure. A single
`EntityRepository`, a single `TestEntitySource`, and a single `AbstractTestEntityRepository`
can serve both entity types uniformly — because both carry a non-nullable `name()`.

## Decision

### Every Entity has a non-nullable name

`Entity.name()` is non-nullable for all entities. The `@Nullable default` returning `null`
is removed. All domain records — catalog and fact — declare a `name` component.

```java
public interface Entity<ID extends PersistenceId<?>, NAME extends EntityName<?>> extends Observable {
    ID id();
    NAME name(); // non-nullable for all entities, covariant per subtype
    <E extends Entity<ID, NAME>> E withId(ID id);
}
```

The `NAME` type parameter was added in Amendment 2 (see below). It makes the name type
visible at the call site without a cast, and it enables `TestEntitySource` to enforce
name uniqueness automatically without subclass-declared constraints.

### CatalogEntity\<ID, NAME\> — slug identity

```java
public interface CatalogEntity<ID extends PersistenceId<?>, NAME extends CatalogName>
        extends Entity<ID, NAME> {
}
```

A `CatalogEntity` is a stable, named classification that exists independently of
observation. Its `EntityName` is a human-readable slug that the domain already knows.
Cross-domain references use this slug (per ADR-001).

The `name()` on a catalog entity record returns a concrete `EntityName` subclass:
`CompoundName`, `ElementName`, `ZoneName`, etc.

Examples: `Element`, `Compound`, `InsectSpecies`, `Plant`, `ZoneInfo`, `SoilProfileInfo`

### FactEntity\<ID, NAME\> — GUID identity

```java
public interface FactEntity<ID extends PersistenceId<?>, NAME extends FactName>
        extends Entity<ID, NAME> {
}
```

A `FactEntity` is a record of something that happened or was observed. Its `name()` returns
a concrete `FactName` subclass — a GUID assigned at creation time, not at persistence time.

The GUID solves three problems the original design left open:

1. **Deduplication** — when a naturalist collects observations offline (in the field, on a
   phone, without connectivity) and publishes them later, the GUID prevents duplicate
   insertion. The existing unique constraint on `name` handles this with no special sync
   logic.
2. **Uniform infrastructure** — every entity has a name, so `getByName()`,
   `getByEntityNameSet()`, `TestEntitySource`, and `EntityRepository` work identically for
   both catalog and fact entities. No infrastructure duplication.
3. **Updatability** — facts are records of what happened, but a naturalist may need to
   correct a typo or update a published observation. The GUID provides stable identity for
   updates without encoding mutable state in the name.

Within `FactEntity` two sub-kinds naturally emerge, though both share the same identity
model:

- **Events** — things done by the naturalist: `AmendmentEvent`, `IrrigationEvent`,
  `TillageEvent`. Human-initiated, discrete, causally significant.
- **Observations** — things measured or noticed: `LabAnalysis`, sensor readings,
  species sightings, phenological records. Passive records of state in nature.

Examples: `AmendmentEvent`, `IrrigationEvent`, `TillageEvent`, `LabAnalysis`

### FactName — abstract GUID wrapper

```java
public abstract class FactName extends EntityName {
    protected FactName(UUID value) {
        super(value.toString());
    }
}
```

`FactName` extends `EntityName`, wrapping a UUID as a String. Since `EntityName.equals()`
checks `getClass()`, a `FactName` subclass will never accidentally equal an `EntityName`
subclass (or another `FactName` subclass) even if the string values coincide.

Each fact entity domain defines its own concrete subclass for compile-time type safety:

```java
public final class AmendmentEventName extends FactName { ... }
public final class IrrigationEventName extends FactName { ... }
public final class LabAnalysisName extends FactName { ... }
```

This prevents accidentally passing an `AmendmentEventName` where an `IrrigationEventName`
is expected — the same type safety that `CompoundName` vs `ElementName` provides for
catalog entities.

### Domain Laws and Constants

A third category of domain knowledge does not map to either sub-interface and requires
no persistence at all: **domain laws and constants**.

Scientific laws (Newtonian mechanics, gas laws, thermodynamic relationships), physical
constants (gravitational acceleration, Avogadro's number), and agronomic rules
(optimal nitrogen ranges, amendment safety thresholds) are domain knowledge encoded in
pure Java. They are not entities under Evans' definition — they have no identity thread
through time, no surrogate key, and no natural slug. They did not *happen*, and they are
not named catalog entries. They are generalisations extracted from observation and held
as true until the model is corrected.

These belong as:

- **Value objects** — when the law or constant needs to travel with other data
  (e.g. a `GravitationalField` carrying both magnitude and direction as a value).
- **Static constants** — when the value is universal and dimensionless within the domain
  (e.g. `PhysicsConstants.GRAVITATIONAL_ACCELERATION_MS2`).
- **Domain service methods** — when the rule is a computation (e.g.
  `NitrogenCycle.biologicalAmplificationFactor(SoilType)`).

None of these require an RDBMS table. The fact that a law *was derived* from observations
(a `FactEntity`) does not make the law itself a `FactEntity`. The Cavendish measurement
(1798) is a `FactEntity`; the gravitational constant it produced is a value.

### Classification

| Category | Type | Name kind | Identity | Persistent |
|---|---|---|---|---|
| Named stable classification | `CatalogEntity` | `EntityName` (slug) | `PersistenceId` + slug | Yes |
| Singular occurrence | `FactEntity` | `FactName` (GUID) | `PersistenceId` + GUID | Yes |
| Domain law, rule, or constant | `ValueObject` / constant | None | None | No |

### Unified infrastructure

Because every entity now carries a non-nullable `name()`, the framework infrastructure is
unified:

- `EntityRepository` carries `getByName()` and `getByEntityNameSet()` directly — no
  `CatalogEntityRepository` or `FactEntityRepository` subinterfaces needed
- `TestEntitySource` carries `getByName()` and `getByEntityNameSet()` directly — no
  `TestEntitySource` needed
- `AbstractTestEntityRepository` carries `doGetByName()` and `doGetByNameSet()` directly —
  no `AbstractCatalogTestEntityRepository` needed

The Catalog/Fact distinction remains as a domain semantic marker on entity records. It does
not bifurcate the infrastructure.

## Consequences

- `FactName` (abstract, extends `EntityName`) is added to `kernels/framework`
- Concrete `XxxName extends FactName` classes are restored to `domains/identifiers` for
  each fact entity type: `AmendmentEventName`, `IrrigationEventName`, `TillageEventName`,
  `LabAnalysisName`
- `Entity.name()` becomes non-nullable — the `@Nullable default` returning `null` is removed
- `CatalogEntity` and `FactEntity` remain as semantic markers but no longer affect the
  repository or test infrastructure hierarchy
- `CatalogEntityRepository`, `FactEntityRepository`, `TestEntitySource`, and
  `AbstractCatalogTestEntityRepository` are deleted — their methods are absorbed into the
  base classes
- `getByName()` and `getByEntityNameSet()` are available on every repository, not just
  catalog repositories
- Fact entity records gain a `name` component of their concrete `FactName` subclass —
  the GUID is assigned at creation time, not at persistence time
- The dual-key strategy (ADR-001) now applies uniformly to all entities — slug for catalog,
  GUID for fact. `PersistenceId` never crosses a domain boundary; `EntityName` (slug or
  GUID) is the cross-boundary reference
- Offline-collected observations are deduplicated by the existing unique constraint on
  `name` — no special sync logic required
- The `Invariants` builder validates both `entityId` and `entityName` for all entities

---

## Amendment 2 — `Entity<ID, NAME>` and Automatic Name Uniqueness

### Context

After Amendment 1 established that every entity carries a non-nullable `name()`, the
`Entity` interface still typed that method as `EntityName` (the abstract base). At call
sites, retrieving a `CompoundName` from a `Compound` required either an unchecked cast
or a redundant accessor override. More significantly, `TestEntitySource` had to redeclare
the name uniqueness constraint in every subclass, even though the base class already called
`entity.name()` in its `getByName` and `getByEntityNameSet` queries.

### Decision

#### `Entity<ID, NAME extends EntityName<?>>` — name type parameter

`Entity` gains a second type parameter `NAME`, bounded by `EntityName<?>`. The `name()`
method returns `NAME`, giving call sites the concrete name type without a cast.

```java
public interface Entity<ID extends PersistenceId<?>, NAME extends EntityName<?>> extends Observable {
    ID id();
    NAME name();
    <E extends Entity<ID, NAME>> E withId(ID id);
}
```

`CatalogEntity` and `FactEntity` propagate the parameter with appropriate bounds:

```java
public interface CatalogEntity<ID extends PersistenceId<?>, NAME extends CatalogName>
        extends Entity<ID, NAME> { }

public interface FactEntity<ID extends PersistenceId<?>, NAME extends FactName>
        extends Entity<ID, NAME> { }
```

#### `TestEntitySource` enforces name uniqueness automatically

Because `TestEntitySource<ID, NAME, ENTITY>` holds `NAME` as a type parameter, it can call
`entity.name()` without casting and compare it directly. Name uniqueness is now enforced in
`preSaveChecks` before iterating `uniqueConstraints()`:

```java
void preSaveChecks(ENTITY entity) {
    NAME name = entity.name();
    if (name != null && entityMap.values().stream().anyMatch(e -> e.name().equals(name))) {
        throw new UniqueConstraintException(entity, "name", name);
    }
    for (UniqueConstraint<ENTITY> uc : uniqueConstraints()) {
        // additional constraints declared by subclass
    }
}
```

`uniqueConstraints()` gains a default implementation returning `List.of()`. Subclasses
override it only when the entity has constraints beyond the canonical name — for example,
`Compound` also constrains `commonName` and `formula` (via `compoundInfo().formula()`).

#### `@EntityIdentifier` scope narrowed

`@EntityIdentifier` previously marked the entity's own canonical name field to signal that
it produces a unique constraint in `TestEntitySource`. That role is now served by the
`NAME` type parameter and the automatic enforcement above.

Going forward, `@EntityIdentifier` is used only on *secondary* `EntityName` fields — fields
that are not the entity's canonical `name()` but that must nonetheless be unique within the
data source. For plain value fields (`String`, `int`, enums) that must be unique but are
not `EntityName` subclasses, use `@UniqueValue` instead (e.g. `@UniqueValue String commonName`
on `Compound`). Both must be declared explicitly in `uniqueConstraints()`, as before.

The annotation is never placed on the canonical `name` component of any entity record.

### Consequences

- `Entity`, `CatalogEntity`, and `FactEntity` gain a `NAME` type parameter
- All entity records are updated to propagate the parameter:
  `Compound implements CatalogEntity<CompoundId, CompoundName>`, etc.
- `TestEntitySource<ID, NAME, ENTITY>` enforces name uniqueness automatically
- `uniqueConstraints()` is no longer abstract — default returns `List.of()`
- The `name` `UniqueConstraint` is removed from every `TestEntitySource` subclass;
  subclasses with no other constraints need not override `uniqueConstraints()` at all
- `@EntityIdentifier` is removed from all canonical `name` components in entity records
- `@EntityIdentifier` is retained on secondary unique `EntityName` fields
- `@UniqueValue` is used for secondary unique plain value fields (e.g. `String commonName`
  on `Compound`)
- The `@EntityIdentifier` javadoc is updated to document the narrowed scope

---

## Amendment 3 (Draft) — CatalogName Canonical Form and Name–Display Separation

### Context

After Amendment 2 established the `NAME` type parameter, early catalog data still used
human-readable strings as entity names: `"Ca"` for an element, `"Gypsum Dissolution in
Soil Water"` for a reaction. These values were fragile keys — case-sensitive, whitespace-
sensitive, and ambiguous about whether they were machine identifiers or display labels.

The problem became visible when two independent concerns collided in a single field:

1. **Machine identity** — `name()` is the cross-domain reference key (ADR-001), the unique
   constraint enforced by `TestEntitySource`, and the future `VARCHAR` column in RDBMS DDL.
   It must be normalised, predictable, and safe for URLs, filenames, and case-insensitive
   lookups.
2. **Human readability** — titles, common names, and IUPAC symbols are how humans recognise
   entities. They may contain spaces, mixed case, special characters, and are not unique
   in the same way (e.g. multiple compounds share the formula pattern `CaSO₄`).

Overloading `name()` with both concerns meant that enforcing format constraints on the key
would destroy the display value, and preserving the display value would leave the key
unreliable. The solution is separation: `name()` is strictly the machine key; human-readable
values are explicit, separate fields on the entity record.

### Decision

#### CatalogName enforces lower-kebab-case

`CatalogName` (the abstract base for all `EntityName<String>` subclasses used by catalog
entities) enforces a canonical format via `isValid()`:

```java
public abstract class CatalogName implements EntityName<String> {
    private static final Pattern LOWER_KEBAB = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    protected abstract int maxLength();

    @Override
    public boolean isValid() {
        return value != null
                && value.length() <= maxLength()
                && LOWER_KEBAB.matcher(value).matches();
    }
}
```

The regex `^[a-z0-9]+(-[a-z0-9]+)*$` permits only lowercase alphanumeric segments separated
by single hyphens. No leading or trailing hyphens, no consecutive hyphens, no uppercase, no
whitespace, no special characters. This format is safe for URLs, filenames, SQL identifiers,
and case-insensitive collation.

#### Each concrete subclass declares its own `maxLength()`

The maximum length is not a property of all catalog names — it varies by domain. An element
name (`"calcium"`) is inherently shorter than a reaction name
(`"calcium-displacing-potassium-from-coir-exchange-sites"`). Each concrete `CatalogName`
subclass declares its own `maxLength()`, which maps directly to `VARCHAR(n)` in the RDBMS
DDL:

```java
public final class ElementName extends CatalogName {
    @Override protected int maxLength() { return 64; }
}

public final class ReactionName extends CatalogName {
    @Override protected int maxLength() { return 128; }
}
```

This keeps the length constraint close to the domain knowledge — the domain knows how long
its slugs can reasonably be — and gives the RDBMS adapter an authoritative column width
without relying on a global constant or `VARCHAR(MAX)`.

#### FactName is unchanged

`FactName` wraps a `UUID` whose string representation is always 36 characters. No format
or length enforcement is added — the UUID constructor guarantees validity.

#### Name is the machine key; display values are separate fields

The principle established by this amendment:

- `name()` returns the **machine key** — a lower-kebab-case slug for catalog entities, a
  UUID string for fact entities. It is the cross-domain reference, the unique constraint,
  and the `VARCHAR` column value.
- **Human-readable values** — titles, common names, IUPAC symbols — are explicit, separate
  fields on the entity record. They are not constrained to kebab format and may contain
  spaces, mixed case, and special characters.

This separation is analogous to the `PersistenceId` / `EntityName` split (ADR-001): just
as `PersistenceId` is the RDBMS key and `EntityName` is the domain key, `name()` is the
machine key and display fields are the human key. Neither role should be overloaded onto
the other.

### Consequences

- `CatalogName.isValid()` enforces `^[a-z0-9]+(-[a-z0-9]+)*$` and `value.length() <= maxLength()`
- Every `CatalogName` subclass implements `protected abstract int maxLength()`
- All catalog JSON data uses lower-kebab-case slugs for `name` fields
- `Element` — `name` is the kebab slug (`"calcium"`), `symbol` carries the IUPAC symbol
  (`"Ca"`). The former `commonName` field was dropped; it duplicated information already
  available from the slug and provided no additional domain value.
- `ReactionProfile` — gained `String title` for the human-readable reaction name
  (`"Gypsum Dissolution in Soil Water"`). `name` is the kebab slug
  (`"gypsum-dissolution-in-soil-water"`).
- `Compound` — `commonName` changed from `@EntityIdentifier CompoundCommonName` (an
  `EntityName` subclass) to `@UniqueValue String`. The common name is a display label, not
  a cross-domain reference key, and does not need the `EntityName` type machinery.
  `CompoundCommonName` class deleted.
- `FactName` subclasses are unaffected — UUID format needs no additional constraints.
- The `@EntityIdentifier` annotation is no longer used on any field in the current codebase;
  its specification (Amendment 2) remains valid for future secondary `EntityName` fields.
