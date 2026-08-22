# Domains

Per-domain CLAUDE.md files under each domain directory cover domain-specific vocabulary
(chemistry, soil, plants, apiary, etc.). This file covers conventions shared across all
domains: identity model, record conventions, field annotations, test fixtures,
test contexts, repositories, and new-module scaffolding.

## Identity Model

Every domain class implements exactly one of six `kernels/framework` markers; all
extend `Observable` and require `invariants()`. The two identity-bearing branches —
`NamedEntity<NAME>` (natural-key slug) and `Entity<ID>` (surrogate UUIDv7) — and their
`EntityName`/`EntityId` rules live in the root [`CLAUDE.md`](../CLAUDE.md#identity)
(ADR-022; note it supersedes the older ADR-021 adapter-key framing). Observations and
events are `Entity` records: immutable, equality by value. The other four markers:

- **Aggregate** — consistency boundary, owns child entities and value objects.
  Declares explicit `with*` methods per field.
- **ReadModel** — read-side composition assembled from already-persisted parts.
  Immutable, identity optional, NOT a consistency boundary; `invariants()` assert
  the projection's structural well-formedness, not owned cross-entity consistency.
  Use instead of `Aggregate` when the type owns nothing and is never mutated as a
  unit (e.g. the insect `*View` read models).
- **ValueObject** — immutable, no identity, equality by value. Must satisfy all four
  ADR-013 constraints: no Entity/Aggregate members, does not uniquely identify an entity,
  cohesive ubiquitous-language concept, members have collective meaning (not a projection
  of an entity's fields).
- **BehavioralCollection\<T extends Observable\>** — abstract base class in
  `kernels/framework` for multi-result query return types. Extended by `final class` per
  domain (e.g. `CompoundCollection`). Not a record. See ADR-011.

## Field Annotations

- `@EntityIdentifier` — marks a *secondary* `EntityName` component as unique within its
  data source. The canonical `name()` component is automatically enforced — do not
  annotate it. Cross-domain FK `EntityName` references carry no annotation.
- `@UniqueValue` — marks plain value components (`String`, `int`, enums) that must be
  unique. Both annotations require explicit declaration in `uniqueConstraints()`.

## Java Record Conventions

Entity, Aggregate, and ValueObject are Java records. No Lombok. `BehavioralCollection` is
the single exception — `final class` to enable package-private construction (see ADR-011).

Reference implementations: `Compound` (NamedEntity), `InsectSpecies` (NamedEntity),
`CompoundInfo` (ValueObject), `CompoundCollection` (BehavioralCollection). Reading the
source is faster than a spec.

Rules:

- Accessor names match component names exactly: `name()`, `someField()` — never
  `getName()`
- Boolean components use plain names: `active`, `beneficial`. Predicate methods use `is*`
  prefix only when they are behavior methods, not component accessors
- Every mutable field on a `NamedEntity` needs an explicit `with*` method on the concrete
  record; `name()` is immutable
- Jackson 2.19.x natively deserializes records. No `@JsonCreator` on entity/aggregate/
  value object records. JSON field names must match component names exactly
- `@JsonCreator` **is** required on the `public static of(...)` factory of any non-record
  Jackson must deserialize: `EntityName` subclasses, `NamedValue<T>` implementations.
  Without it, deserialization fails silently or with a misleading error
- `@EntityIdentifier` placed before the type: `@EntityIdentifier FooName name`
- Optional-returning query methods must not share a name with any component: a component
  `String biologicalCatalyst` needs accessor `biologicalCatalystOptional()`, not
  `biologicalCatalyst()`
- Static factory methods (`of(...)`, `empty()`, `from(...)`) are the public instantiation
  API for all domain types. `new Foo(...)` at a call site outside the type's own class is
  a review flag. See ADR-012

## Test Identifiers

`domains/identifiers-test` contains one `Test<Domain>Identifiers` class per domain —
the single source of truth for `EntityName` constants used in repository contract tests.
Never inline these into test classes.

Structure rules:

- Class structure mirrors the domain object graph — ownership and composition reflected
  as nested static classes.
- **Child entities nest inside their parent species/entity class — never as a sibling
  top-level class.** An `InsectImage` owned by `PotatoLeafhopper` goes inside
  `PotatoLeafhopper.Images`, not a parallel `InsectImages` class at the same level as
  `InsectSpecies`. A flat sibling class implies peer status in the domain graph, which is wrong.
- **Promote flat constants to parent classes the moment a child collection is added.**
  If a parent is currently held as a top-level `EntityName` constant
  (e.g. `Plants.CaliforniaPipevine = PlantSpeciesName.of("aristolochia-californica")`) and a child
  entity collection (programs, images, life stages, …) is introduced, replace the
  constant with a static class carrying `name` plus the child inner classes —
  `Plants.CaliforniaPipevine { name; Programs { … } }`. Declaring the child collection
  as a sibling of the constant (`Plants.Programs` next to `Plants.CaliforniaPipevine`)
  flattens the ownership graph and forces every consumer to re-cross-reference plant
  ↔ program by hand. Update existing call sites to `Parent.name` in the same change.
- Every entity type must define **at least two** known `EntityName` constants, enabling both
  single-entity and set-based lookup tests. Partial-match tests for `getByEntityNameSet`
  must include at least two known values plus the `NotFound` value — a single known value
  does not distinguish partial-match behavior from single-entity lookup.
- Each parent scope defines a **single** `NotFound` inner class with one fictitious
  `EntityName` constant per entity type within that scope. Child scopes do not define their
  own `NotFound` — the parent's covers them all.
- Fictitious names must be obviously synthetic — `"unobtainium-oxide"`, `"Xx"` — so they
  can never accidentally collide with real catalog data added later.
- Element names in a compound's `Elements` inner class must reference the top-level
  `Elements` constants, not re-declare `ElementName.of(...)` inline.

Reference implementation: `TestInsectsIdentifiers`, `TestChemistryIdentifiers`.

## Test Entity Sources

Test data follows model/data separation: Java defines schema, JSON defines instances.

Use `/test-entity-source <EntityClassName> in <domain> module` to scaffold all three files.

`TestEntitySource<KEY, ENTITY extends Named<KEY>>` is the in-memory analog of an RDBMS
table. It keys on the entity's `key()` (an `EntityName` for `NamedEntity`, an `EntityId`
for `Entity`) and enforces:

- **Key uniqueness** — the canonical `key()` is automatically checked on every insert;
  no subclass declaration required
- **Secondary unique constraints** — declared per entity via `uniqueConstraints()`, which
  defaults to `List.of()`. Override only for fields annotated `@EntityIdentifier` (secondary
  unique `EntityName` fields) or `@UniqueValue` (plain value fields).
- **Intra-domain foreign keys** — declared per entity via `foreignKeyConstraints()`, which
  defaults to `List.of()`. Override with one `ForeignKeyConstraint.of(field, accessor,
  ForeignSourceClass.class)` per reference so a fixture whose parent is missing fails at
  load. Cross-*domain* references declare none (the DAG forbids in-memory resolution).

Every source declares a `(NaturalistDatabase)` constructor and is acquired through
`NaturalistDatabase#getNamed(...)`, which builds and caches the single shared instance —
`NaturalistDatabase` is the only object that constructs a source (see Repository
Architecture below).

Conventions for JSON catalog files:

- `"name": "<slug>"` — the `EntityName` natural key of a `NamedEntity`
  (e.g. `"calcium-sulfate-dihydrate"`); no `id` field (ADR-022)
- `"id": "<uuidv7>"` — the `EntityId` of an `Entity` record (observations, images, fact
  records); these carry an id and no `name`
- Remaining fields match the record component names exactly
- Enum values serialize by constant name (`"ROOT_MASS_FLOW"`, `"INORGANIC_SALT"`)
- `PeriodicElement` uses chemical symbols (`"Ca"`, `"Mg"`, `"K"`)
- JSON location: `<domain>-repository-test/src/main/resources/<domain>/<subpackage>/`
  (resource sub-directory mirrors the Java sub-package)

Reference implementation: `InsectSpeciesTestEntitySource` + `insects/species/insect-species.json` in the insects module.

## Test Contexts

Each domain has a `<Domain>sTestContext` class in `<domain>-test-context/src/main/java/`
that simulates DI by constructing the full object graph — mock repositories, `*QueryImpl`
adapters, `*CommandImpl` adapters, and `Transaction` subclasses — then exposing the
assembled result through public methods (`insectQuery()`, `insectCommand()`,
`catalogIdentificationTransaction()`, etc.).

The pattern works because the test-context class lives in the **same package** as the
`*-core` implementations (split-package across Maven modules). Package-private `*Impl`
constructors are visible to it. Consumers — controller bootstraps, integration tests —
see only the public contract and never wire dependencies directly.

### TestContextInternal — breaking the Maven cycle

`<domain>-test-context` depends on `<domain>-core` (compile scope) to access the
package-private implementations. This means `<domain>-core` **cannot** depend on
`<domain>-test-context` (even at test scope) — Maven rejects the direct cycle.

When a `<domain>-core` test needs the full wired object graph (e.g. testing a
`Transaction` or `Command` that touches multiple repositories), it uses a
**`<Domain>sTestContextInternal`** class in `<domain>-core/src/test/java/`. This
class follows the identical pattern — same package, same split-package access to
package-private impls, same public contract — but lives in core's own test classpath
instead of a separate module.

Rules:

- **Naming:** `<Domain>sTestContextInternal` — the `Internal` suffix distinguishes it
  from the module-level `<Domain>sTestContext`.
- **Scope:** package-private class, visible only to tests in `<domain>-core`.
- **No-op stubs for cross-domain queries:** the internal context may stub out
  cross-domain dependencies (citations, life stages) that the test doesn't exercise,
  rather than pulling in their full test contexts.
- **Not a replacement.** `<Domain>sTestContext` in `<domain>-test-context/` remains
  the canonical wiring used by console controllers and integration tests in other
  modules. The internal variant exists only to serve `<domain>-core`'s own tests.
- **Both go away** when Spring DI replaces the manual composition.

Tests in `<domain>-core` that only need a single repository or query (e.g.
`SpeciesQueryImplTest`) continue to wire their dependencies directly — the internal
test context is for tests that need the **full assembled graph**.

Reference implementation: `InsectsTestContext` + `InsectsTestContextInternal` in the
insects domain.

## Repository Architecture

See [ADR-001](../docs/adr/ADR-001-repository-architecture.md) and
[ADR-002](../docs/adr/ADR-002-repository-behavioral-contract.md).

Quick-reference constraints:

- Repository interfaces are **package-private** in `<domain>-api`
- A repository has exactly four responsibilities: entity cache, referential integrity,
  unique constraints, transactional consistency — no logic
- `NaturalistDatabase` is the only object permitted to instantiate `TestEntitySource` instances;
  acquire a source via `NaturalistDatabase#getNamed(...)`
- Cross-domain references use `EntityName` slug; `Entity` records are never referenced
  cross-domain by value (ADR-022, superseding the ADR-021 `PersistenceId` framing)
- Cross-domain joins are prohibited; cross-domain FK enforcement is deferred to the RDBMS layer
- Each repository manages its own secondary indexes. No cross-repository queries within a
  sub-context. No cross-sub-context repository access.
- **Fan-out must batch — never per-element in a loop.** Calling a repository or query once
  per element of a prior result (`for` / `stream().map` / `computeIfAbsent` / recursive
  descent) is an N+1 that scales with catalog size and degrades production; it passes on
  seed data and fails in prod. Add a batched sibling — `getByXNames(Set<NAME>)` /
  `forXNames(Set<NAME>)` — and resolve the whole set in one call, or use the inherited
  `EntityQuery.findByNameSet(Set<NAME>)` for by-name lookups. Validate the set argument with
  `observableCollection(...)` for `RankName` sets and `entityNameCollection(...)` for concrete
  `EntityName` sets. Reference: `InsectImageQueryImpl.forRankHierarchy` (subtree resolved in a
  handful of batched calls, not a per-node walk) and the `getByRankNames`/`getByParentNames`
  repository methods.

### Repository Behavioral Contract

Every `NamedEntityRepository` has a behavioral contract defined as a `@Test default`
interface in `<domain>-repository-test/src/main/java/`. Domain-specific contract
interfaces extend `EntityRepositoryTest<NAME, ENTITY>` from
`kernels/framework-test`. The concrete interface supplies only identity constants and
entity construction hooks — no test logic.

Each select method requires three test cases per ADR-002: argument validation (null
rejection via `InvariantViolationException`), empty result (not-found), and expected
result. Write methods (`insert`, `update`) follow the same three-case pattern with
constraint violations and entity-not-found replacing empty result.

Insert and update expected-result tests observe the persisted entity via the Observer
framework (ADR-017), walking its full constraint graph to catch adapter serialization drift.

Concrete test interface hooks:

| Hook                       | Purpose                                                                                           |
|----------------------------|---------------------------------------------------------------------------------------------------|
| `repository()`             | The repository under test                                                                         |
| `source()`                 | The `TestEntitySource` backing the test data                                                      |
| `notFoundName()`           | A fictitious `NAME` guaranteed absent from the catalog                                            |
| `knownEntityNames()`       | At least two known `NAME` constants from the test data                                            |
| `newEntity()`              | A valid entity with a unique name, using `RandomValue` where field constraints permit             |
| `ghostEntity()`            | An entity with a name absent from the catalog, using `RandomValue` where field constraints permit |
| `modifiedEntity(original)` | The original with every mutable field changed via `RandomValue`                                   |

`assertEntityEquals` defaults to recursive comparison — override for entities with custom
equality semantics. `NamedEntity` records carry no surrogate id to ignore (ADR-022);
`Entity` records carry an `id` component that is part of their identity and included
in the comparison.

**Update expected-result convention:** modify **every mutable field** to a value distinct
from the original using `RandomValue` helpers where field constraints permit. `EntityName`
is immutable — carried forward from the original and asserted unchanged. If the entity
carries a foreign key `EntityName` referencing another entity, the referenced entity must
exist in the test data. Equality is verified via recursive structural comparison; the
Observer walks the full constraint graph.

Use `/entity-repository <EntityClassName>` to scaffold the full repository stack.

## API Surface: Namespace Patterns

The driving concern behind these patterns is cognitive complexity. A naturalist
navigates many domains; reducing noise in the object graph — both in the IDE tree
and at the fluent, discoverable api surface — is what keeps the ecological nature
of a given domain legible. Structural organization exists to serve that end. The
specific shapes below (class for repositories, interface for queries, nested
value-object graphs) are the mechanical consequences of honest visibility rules
applied to that goal.

See [ADR-020](../docs/adr/ADR-020-namespace-interface-pattern.md) for the full
rationale.

Three coordinated namespace types organize a domain api around discoverability and
visibility:

| Outer                           | Java type   | Visibility      | Nested types                                            |
|---------------------------------|-------------|-----------------|---------------------------------------------------------|
| `<DomainNoun>Repository`        | `class`     | package-private | `<EntitySubject>Repository` (`protected interface`)     |
| `<DomainNoun>Query`             | `interface` | public          | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public          | `<EntitySubject>Collection` (`final class`)             |

`EntitySubject` drops the domain prefix — `InsectSpecies` → `Species`, `InsectImage`
→ `Image`. The outer namespace carries the prefix.

This bare-`EntitySubject` rule is for **nested** namespace types (and the N=1-collapsed
ports that stand in for one). **Standalone concrete classes invert it**: query/command
impls, repository mocks, test-entity-sources, contract tests, and their concrete unit
tests carry `<DomainNoun><EntitySubject>` — `InsectSpeciesQueryImpl`,
`PlantImageTestEntitySource`, `InsectSpeciesRepositoryMockTest` — because a loose class
is navigated by its simple name alone and a bare name collides across domains. Ports and
outer namespace types stay bare (or already-prefixed, like `InsectQuery`). See ADR-020 §5.

Why repositories use a `class` and queries use an `interface`: nested types inside
an `interface` are implicitly `public static` — visibility cannot be restricted. A
`class` keeps repository contracts hidden (`protected` = package-private + subclass
access) at both source and bytecode level. Queries and collections *want* their
nested types public; they are the consumer surface.

**N=1 collapse rule.** When a package contains exactly one entity, skip the
namespace: declare a top-level package-private `<Entity>Repository` interface and
a top-level public `<Entity>Query` interface. The namespace adds no value when
there is nothing to group.

**Aggregate value-object nesting.** A `ValueObject` exclusively reachable through
a single `Entity` or `Aggregate` nests inside that entity's file as a `static
record`. Promote it back to top-level the moment any of these becomes true: it
acquires a standalone lifecycle, it is referenced cross-domain by name, or it
appears in more than one entity's component graph.

### Query Implementation

See [ADR-010](../docs/adr/ADR-010-query-design-contract.md) for the full contract.
Short version:

- **Thin — observe, dispatch, delegate.** A query adapter validates arguments via
  `observer().arguments(...)`, then forwards to the repository (or to an aggregate
  factory). No logic, no multi-step composition.
- **Aggregate queries delegate to a package-private factory** in `<domain>-core`.
  The query never assembles an aggregate inline. The factory is a single
  package-private concrete class — no interface, no `Impl` suffix, and no
  declaration in `<domain>-api` under any circumstance. A factory type surfacing
  in the api module is a review blocker: it leaks assembly concerns to consumers
  and makes the implementation detail Spring-injectable across module boundaries.
- **Return types are `Optional<Entity>`, `Optional<Aggregate>`, `Optional<ReadModel>`,
  or a `BehavioralCollection` subclass.** Raw `List<T>` at the port boundary is a
  review flag.

**Reference implementation:** `domains/insects/insects-api/` — `InsectRepository`
(namespace class), `InsectQuery` (namespace interface), `InsectEntityCollections`
(collection namespace), `InsectSpecies` (nested value-object graph). Adapters
live in `insects-core/`: `InsectQueryImpl`, `SpeciesQueryImpl`, `ImageQueryImpl`,
and the read-model factory `InsectFactory` (concrete, no interface — the
template for factory placement).

## Test Fixtures Use Real Data

Test fixtures use actual measurements. `TestEntitySource` seeds repositories with real
FGL data, real sensor readings, real amendment history. A failing test indicates a domain
model error or a real-world change.

## Organism Domains: Rank, Identification, Clade

Domains that catalogue organisms — insects, plants, arachnids, fungi, molluscs,
vertebrates, worms, microbes — share a design the insects domain worked out and the
others should inherit rather than re-derive. Load
[`docs/plans/organism-domain-blueprint.md`](../docs/plans/organism-domain-blueprint.md)
before starting rank, identification, or clade work in any of them.

The three load-bearing ideas, so you know whether the blueprint applies:

- **Every rank a domain catalogues is its own `NamedEntity`** with a typed upward FK to
  its parent, and a sealed `<Domain>RankName` over those names so photos, observations
  and roles can attach at whichever rank was resolved.
- **An organism is catalogued at the most specific rank the evidence supports, and that
  record is permanent** — not a placeholder for a later species identification. The
  confidence bound is expressed in the identifier's elicitation contract, not applied
  afterwards.
- **Dimensions that are not ranks are separate axes**, carried as their own component
  over the same records — never as extra permits on `<Domain>RankName`. Clade is one
  (`@Nullable Clade placedIn`); plants' `Cultivar` is another.

**The ladder is per-domain, and the ladder is Linnaean.** `LinealRank` is shared, but
which rungs get entities is a domain decision — four for plants, five for insects. Copy
the mechanism, never insects' rank list.

**Before adding a permit to a rank name, check it is a rank.** If it has no `LinealRank`
value, or admitting it forces `rank()` to become nullable, it is an axis and belongs in
its own component. The blueprint's section D carries the test and two worked examples.

Reference implementation: `domains/insects/`.

## Creating a New Module

When creating a new domain or kernel module:

1. **Directory structure and pom.xml:**
    - `kernels/<module>/pom.xml` (parent `<artifactId>kernels</artifactId>`) or
      `domains/<domain>/<domain>-<type>/pom.xml` (parent `<artifactId><domain></artifactId>`)
    - Create `src/main/java/com/naturalist/<module>/`
    - Specify dependencies (framework, identifiers, etc. for kernels; framework, identifiers
      for apis; nothing for cores/tests)

2. **Add to parent module pom.xml:**
    - Kernel: add `<module><name></module>` to `kernels/pom.xml` in `<modules>`, alphabetical
    - Domain: add `<module><type></module>` to `domains/<domain>/pom.xml` in `<modules>`

3. **CRITICAL — add dependency-management entry to root pom.xml:**
    - Add a `<dependency>` entry to root `<dependencyManagement>`
    - Kernels: KERNELS section (first in file), alphabetical
    - Domains: appropriate DOMAIN section, alphabetical within that section
    - **Version must always be `${project.version}`**
    - Required — all other modules depend on this entry for consistent versioning.
      Forgetting this step causes build failures when other modules reference the new module.

4. **Update pom.xml files that depend on the new module:**
    - Add as a dependency in `<dependencies>` (no version tag — inherited from
      dependencyManagement), alphabetical within the section

## Pull Request Size Discipline

One concern per PR. When multiple skills scaffold infrastructure for the same entity, they
produce separate PRs merged in dependency order: (1) entity + identifiers,
(2) TestEntitySource + JSON catalog, (3) repository + mock + behavioral contract. Target
≤ 400 lines of meaningful diff. The goal is reviewable PRs where a reviewer can hold the
entire change in working memory and catch domain model errors — not small PRs for their
own sake. See ADR-019.
