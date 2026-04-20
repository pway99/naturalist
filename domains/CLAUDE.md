# Domains

Per-domain CLAUDE.md files under each domain directory cover domain-specific vocabulary
(chemistry, soil, plants, apiary, etc.). This file covers conventions shared across all
domains: test fixtures, repositories, and new-module scaffolding.

## Test Identifiers

`domains/test-identifiers` contains one `Test<Domain>Identifiers` class per domain —
the single source of truth for `EntityName` constants used in repository contract tests.
Never inline these into test classes.

Structure rules:

- Class structure mirrors the domain object graph — ownership and composition reflected
  as nested static classes.
- **Child entities nest inside their parent species/entity class — never as a sibling
  top-level class.** An `InsectImage` owned by `PotatoLeafhopper` goes inside
  `PotatoLeafhopper.Images`, not a parallel `InsectImages` class at the same level as
  `InsectSpecies`. A flat sibling class implies peer status in the domain graph, which is wrong.
- Every entity type must define **at least two** known `EntityName` constants, enabling both
  single-entity and set-based lookup tests. Partial-match tests for `getByEntityNameSet`
  and `getByIdSet` must include at least two known values plus the `NotFound` value — a
  single known value does not distinguish partial-match behavior from single-entity lookup.
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

`TestEntitySource<ID, NAME, ENTITY>` enforces:

- **Primary key constraint** — duplicate `PersistenceId` on insert throws `PrimaryKeyConstraintException`
- **Name uniqueness** — the canonical `name()` is automatically checked on every insert;
  no subclass declaration required
- **Secondary unique constraints** — declared per entity via `uniqueConstraints()`, which
  defaults to `List.of()`. Override only for fields annotated `@EntityIdentifier` (secondary
  unique `EntityName` fields) or `@UniqueValue` (plain value fields).

Conventions for JSON catalog files:
- `"id": null` — persistence ID is always null in catalog data
- `"name": "<slug>"` — the `EntityName` natural key (e.g. `"calcium-sulfate-dihydrate"`)
- Remaining fields match the record component names exactly
- Enum values serialize by constant name (`"ROOT_MASS_FLOW"`, `"INORGANIC_SALT"`)
- `PeriodicElement` uses chemical symbols (`"Ca"`, `"Mg"`, `"K"`)
- JSON location: `<domain>-repository-test/src/main/resources/<domain>/<subpackage>/`
  (resource sub-directory mirrors the Java sub-package)

Reference implementation: `CompoundTestEntitySource` + `chemistry/compound/compounds.json`.

## Repository Architecture

See [ADR-001](../docs/adr/ADR-001-repository-architecture.md) and
[ADR-002](../docs/adr/ADR-002-repository-behavioral-contract.md).

Quick-reference constraints:
- Repository interfaces are **package-private** in `<domain>-api`
- A repository has exactly four responsibilities: entity cache, referential integrity,
  unique constraints, transactional consistency — no logic
- `NaturalistDatabase` is the only object permitted to instantiate `TestEntitySource` instances
- Cross-domain references use `EntityName` slug — never `PersistenceId<Long>`
- Cross-domain joins are prohibited; cross-domain FK enforcement is deferred to the RDBMS layer
- Each repository manages its own secondary indexes. No cross-repository queries within a
  sub-context. No cross-sub-context repository access.

### Repository Behavioral Contract

Every `EntityRepository` has a behavioral contract defined as a `@Test default` interface
in `<domain>-repository-test/src/main/java/`. Domain-specific contract interfaces extend
`EntityRepositoryContractTest<ID, NAME, ENTITY>` from `kernels/framework-test`, which
provides all 22 standard test cases. The concrete interface supplies only identity
constants and entity construction hooks — no test logic.

The contract covers all six `EntityRepository` methods. Each select method requires three
test cases per ADR-002: argument validation (null rejection via `InvariantViolationException`),
empty result (not-found), and expected result. Write methods (`insert`, `update`) follow
the same three-case pattern with constraint violations and entity-not-found replacing
empty result.

Insert and update expected-result tests observe the persisted entity via the Observer
framework (ADR-017), walking its full constraint graph to catch adapter serialization drift.

Concrete test interface hooks:

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

**Update expected-result convention:** modify **every mutable field** to a value distinct
from the original using `RandomValue` helpers where field constraints permit.
`PersistenceId` and `EntityName` are immutable — carried forward from the original and
asserted unchanged. If the entity carries a foreign key `EntityName` referencing another
entity, the referenced entity must exist in the test data. Equality is verified via
recursive structural comparison; the Observer walks the full constraint graph.

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

| Outer | Java type | Visibility | Nested types |
|-------|-----------|------------|--------------|
| `<DomainNoun>Repository` | `class` | package-private | `<EntitySubject>Repository` (`protected interface`) |
| `<DomainNoun>Query` | `interface` | public | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public | `<EntitySubject>Collection` (`final class`) |

`EntitySubject` drops the domain prefix — `InsectSpecies` → `Species`, `InsectImage`
→ `Image`. The outer namespace carries the prefix.

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
- **Return types are `Optional<Entity>`, `Optional<Aggregate>`, or a
  `BehavioralCollection` subclass.** Raw `List<T>` at the port boundary is a
  review flag.

**Reference implementation:** `domains/insects/insects-api/` — `InsectRepository`
(namespace class), `InsectQuery` (namespace interface), `InsectEntityCollections`
(collection namespace), `InsectSpecies` (nested value-object graph). Adapters
live in `insects-core/`: `InsectQueryImpl`, `SpeciesQueryImpl`, `ImageQueryImpl`,
`InsectAggregateQueryImpl`, and the aggregate factory `InsectAggregateFactory`
(concrete, no interface — the template for factory placement).

## Test Fixtures Use Real Data

Test fixtures use actual measurements. `TestEntitySource` seeds repositories with real
FGL data, real sensor readings, real amendment history. A failing test indicates a domain
model error or a real-world change.

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
