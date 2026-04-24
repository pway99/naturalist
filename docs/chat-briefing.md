# Naturalist — Chat Briefing

**Purpose.** This document gives a chat Claude instance the framework and
project context it can't infer from domain vocabulary alone. Upload it before
design discussions, architecture sketches, or code generation for the
`com.naturalist` codebase.

**Scope.** Framework APIs, identity model, module layout, naming conventions,
observability surface, package placement, anti-patterns. Domain vocabulary
(chemistry, soil, insects, plants, etc.) stays with chat — this briefing
covers only the structural glue.

**Primary rule.** When you don't know whether something exists (a method on
`Constraints`, a package path, a type name), say so rather than invent.
Invented APIs and invented package paths are the dominant failure mode.

---

## 1. Module Layout

```
kernels/
  framework/          NamedEntity, Entity, Aggregate, ValueObject,
                      BehavioralCollection, Observable, Observer,
                      Constraints, EntityName, EntityId, EntityNameSet,
                      NamedValue, AggregateRoot (annotation)
  framework-test/     NamedTestEntitySource, NamedTestEntitySourceTest,
                      NamedEntityRepositoryContractTest, UniqueConstraint,
                      TestDataHelper, RandomValue
  field-notes/        Description (four-level Durrell description)
  taxonomy/           TaxonomicClassification, TaxonomicOrder, TaxonomicFamily
  habitat/            HabitatProfile (structured habitat classification)
domains/
  identifiers/        EntityName + EntityId subclasses ONLY
  identifiers-test/   Test<Domain>Identifiers constants
  <domain>/<domain>-api
  <domain>/<domain>-core
  <domain>/<domain>-repository-test
  <domain>/<domain>-repository-rdms
  <domain>/<domain>-console          (JTE templates, Spring MVC)
bootstrap/
application/
```

### DAG (must be acyclic)

```
bootstrap                    →  application
<domain>-repository-test     →  <domain>-api
<domain>-repository-rdms     →  <domain>-api
<domain>-core                →  <domain>-api
<domain>-api                 →  framework, identifiers, field-notes
<organism>-api               →  framework, identifiers, field-notes, taxonomy
identifiers                  →  framework
field-notes                  →  framework
taxonomy                     →  framework
habitat                      →  framework
framework                    →  Jackson, Commons, Micrometer, JSpecify
framework-test               →  framework
```

**Hard rules.**
1. `api` modules depend only on `framework`, `identifiers`, `field-notes`, and
   (organism only) `taxonomy`, `habitat`. Nothing else.
2. `core` may import another domain's `api` only — never its core, repo-test,
   or repo-rdms.
3. Repository interfaces are package-private in `api`; cross-domain
   interaction goes through public query classes.
4. Nothing depends on `bootstrap`.
5. Cycles are errors, not warnings.

---

## 2. Identity Model (ADR-022)

**Every domain class implements exactly one of four interfaces from
`kernels/framework`:**

| Interface | Identity | Usage |
|-----------|----------|-------|
| `NamedEntity<NAME extends EntityName>` | Natural-key slug | Catalog entries, stable cross-domain references |
| `Entity<ID extends EntityId>` | Surrogate UUIDv7 | Events, observations, relationships with no natural key |
| `Aggregate` | Via aggregate root's identity | Consistency boundaries |
| `ValueObject` | None — equality by value | Immutable components |

Plus `BehavioralCollection<T extends Observable>` — `final class` (not a
record) for multi-result query return types.

All four extend `Observable` and declare `invariants()`.

### Naming rules

- `EntityName` subclasses are slug-identified: regex
  `^[a-z0-9]+(-[a-z0-9]+)*$`, kebab-case, never null.
- `EntityId` subclasses are UUIDv7, validated `version() == 7` at boundary,
  generated at record construction via the kernel's generator.
- **Do not call `UUID.randomUUID()` in domain or adapter code.** Use the
  kernel generator only.
- Cross-`NamedEntity` references are by `EntityName`.
- `Entity` records are never referenced cross-domain by value.
- Domain records carry no `PersistenceId`. It does not exist in Java (ADR-022
  supersedes ADR-021).

### Identifier placement — CRITICAL

**Identifier classes live in the `identifiers` module, under a sub-package
named after their home domain — NOT under `com.naturalist.identifiers`.**

Canonical locations:

| Type | Actual package | Module |
|------|----------------|--------|
| `InsectSpeciesName` | `com.naturalist.insects` | `identifiers` |
| `InsectImageId` | `com.naturalist.insects` | `identifiers` |
| `PlantName` | `com.naturalist.plants` | `identifiers` |
| `CultivarName` | `com.naturalist.plants.cultivar` | `identifiers` |
| `CompoundName` | `com.naturalist.chemistry.compound` | `identifiers` |
| `ElementName` | `com.naturalist.chemistry.element` | `identifiers` |
| `ZoneName` | `com.naturalist.zone` | `identifiers` |
| `SoilProfileName` | `com.naturalist.soil` | `identifiers` |
| `NaturalistName` | `com.naturalist.naturalist` | `identifiers` |

**There is no `com.naturalist.identifiers` package.** A class at that path
does not exist. The *module* is `identifiers`; the *package* tracks the home
domain.

### Framework types — canonical packages

| Type | Package | Module |
|------|---------|--------|
| `NamedEntity`, `Entity`, `Aggregate`, `ValueObject`, `AggregateRoot`, `Observable`, `EntityName`, `EntityId`, `EntityNameSet`, `NamedValue`, `BehavioralCollection` | `com.naturalist.ddd` | `framework` |
| `Constraints`, `Constraint`, `ConstraintCollection`, `Observer`, `MethodObserver`, `InvariantObservation`, `Metric` | `com.naturalist.observability` | `framework` |
| Constraint records (`NotNullConstraint`, `NotBlankConstraint`, `NotEmptyConstraint`, `ObservableConstraint`, `ValueObjectOrNullConstraint`, `ValueObjectCollectionConstraint`, `EntityNameConstraints`, `EntityIdConstraints`, `IdentifierConstraints`, `NamedValueConstraints`) | `com.naturalist.observability.constraints` | `framework` |
| `Description` | `com.naturalist.fieldnotes` | `field-notes` |
| `TaxonomicClassification`, `TaxonomicOrder`, `TaxonomicFamily` | `com.naturalist.taxonomy` | `taxonomy` |
| `HabitatProfile` | `com.naturalist.habitat` | `habitat` |
| `NamedTestEntitySource`, `NamedEntityRepositoryContractTest`, `UniqueConstraint`, `TestDataHelper`, `RandomValue` | `com.naturalist` / `com.naturalist.data` | `framework-test` |

---

## 3. Record Conventions

- `Entity`, `Aggregate`, `NamedEntity`, `ValueObject` are **Java records**.
- `BehavioralCollection` is the single exception: `final class` (ADR-011).
- No Lombok. Java 17+ — records, sealed interfaces, pattern matching, switch
  expressions.

### Accessor rules

- Accessor names match component names exactly: `name()`, `someField()` —
  never `getName()`.
- Boolean components use plain names: `active`, `beneficial`. Predicate
  methods use `is*` prefix only when they are behavior methods, not
  component accessors.
- Every mutable field on a concrete `NamedEntity` record needs an explicit
  `with*` method; `name()` is immutable.
- Optional-returning query methods must not share a name with any component:
  a component `String biologicalCatalyst` needs accessor
  `biologicalCatalystOptional()`, not `biologicalCatalyst()`.

### Construction

- Static factory methods (`of(...)`, `empty()`, `from(...)`) are the public
  instantiation API.
- `new Foo(...)` at a call site outside the type's own class is a review
  flag (ADR-012).

### Annotations

- `@EntityIdentifier` — marks a *secondary* unique `EntityName` component
  within a data source. Do NOT annotate the canonical `name()` component (it
  is enforced automatically). Placed before the type:
  `@EntityIdentifier FooName otherName`.
- `@UniqueValue` — marks plain value components (`String`, `int`, enums) that
  must be unique.
- Both require explicit declaration in `uniqueConstraints()`.
- `@Nullable` — JSpecify (`org.jspecify.annotations.Nullable`) for nullable
  field documentation.

### Jackson

- Jackson 2.19.x natively deserializes records — **no `@JsonCreator` on
  entity/aggregate/value-object records.**
- JSON field names must match component names exactly.
- **`@JsonCreator` IS required on the `public static of(...)` factory of any
  non-record Jackson must deserialize:** `EntityName` subclasses,
  `NamedValue<T>` implementations.
- Enum values serialize by constant name (`"INORGANIC_SALT"`, `"ROOT_MASS_FLOW"`).
- `PeriodicElement` uses chemical symbols (`"Ca"`, `"Mg"`).
- Sealed hierarchies Jackson must round-trip need `@JsonTypeInfo` +
  `@JsonSubTypes` or an explicit discriminator property.

---

## 4. Observability Framework — Complete `Constraints` API

`Constraints` is the fluent invariants builder. Every `Observable` implements
`Consumer<? extends Constraints> invariants()`. The graph walker descends
through `ConstraintCollection` nodes automatically.

### Method forms

**Direct-value form** — pass the component value directly. Use inside
`invariants()` on the record that owns the field.

**By-function form** — pass the parent object and a method reference. Use
when descending into a child from outside, or when the parent may be null
(safe null-short-circuit).

### Complete method list (canonical, as of the current kernel)

| Method | Form | Behavior |
|--------|------|----------|
| `namedEntity(e, name)` | direct | Non-null NamedEntity, descends into child's invariants |
| `namedEntity(o, fn, name)` | by-fn | Same, via parent |
| `valueObject(v, name)` | direct | Non-null ValueObject, descends |
| `valueObject(o, fn, name)` | by-fn | Same, via parent |
| `valueObjectOrNull(o, fn, name)` | by-fn | Null permitted; descends only when present |
| `valueObjectCollection(o, fn, name)` | by-fn | Non-null `Collection<V extends ValueObject>`, descends into each element with indexed path `[0]`, `[1]`, ... |
| `observable(o, fn, name)` | by-fn | Any non-null Observable (e.g. BehavioralCollection) |
| `entityName(e, name)` | direct | Validates EntityName |
| `entityId(f, name)` | direct | Validates EntityId (UUIDv7) |
| `identifier(v, name)` | direct | Polymorphic identifier (runtime dispatch) |
| `identifierSet(set, name)` | direct | Polymorphic identifier set |
| `entityNameCollection(c, name)` | direct | Collection of EntityName |
| `entityNameSet(set, name)` | direct | EntityNameSet wrapper |
| `entityNameSet(o, fn, name)` | by-fn | Same via parent |
| `namedValue(o, fn, name)` | by-fn | NamedValue child |
| `notBlank(value, name)` | direct | Rejects null, empty, whitespace-only |
| `notBlank(t, fn, name)` | by-fn | Same via parent |
| `notNull(value, name)` | direct | General null check |
| `notNull(t, fn, name)` | by-fn | Same via parent |
| `notEmpty(t, fn, name)` | by-fn | Rejects null + empty Collection/Map/CharSequence |

### What does NOT exist (do not call)

- `namedEntityOrNull(...)` — **not in the kernel.** For nullable `NamedEntity`
  descent, use a block lambda with a null guard:
  ```java
  Consumer<Constraints> body = i -> {
      i.entityName(name, "name")
       .valueObject(taxonomy, "taxonomy");
      if (stage != null) i.namedEntity(this, Parent::stage, "stage");
  };
  return body;
  ```
- `observableOrNull(...)` — not in the kernel.
- `notEmpty(value, name)` (direct form) — only by-function form exists.
- `notBlank(t, fn, name)` for non-String — `notBlank` is String-only.
- Raw collection-set-descent beyond `valueObjectCollection` /
  `entityNameCollection` / `entityNameSet`.

### Semantics

- **`valueObjectOrNull` vs `valueObject`.** Nullable VO fields must use
  `valueObjectOrNull` — the difference between "child is null" (valid) and
  "child is structurally invalid" (its own invariants). An empty-but-present
  VO must be rejected by the child's own invariants, not tolerated by the
  parent.
- **`valueObjectCollection` vs `notEmpty`.** `valueObjectCollection` asserts
  non-null + descends into each element. It does NOT assert non-empty.
  Pair with `notEmpty` when empty is illegal. Keeps policies composable.
- **Indexed paths** for collections: `windows.[0].onset`,
  `windows.[1].tail`. Matches the dotted-path convention in
  `ConstraintCollection`.

### Testing invariants

- Unit tests observe via `Observer` → `MethodObserver` → `InvariantObservation`.
  Never call `isValid()` on individual `Constraint` objects.
- One `Observer` per test class (static field). One `MethodObserver` per
  test method. `mo.forMethod(...)` must match the method name exactly.
- `mo.namedEntity(e, label)` for `NamedEntity` / `Aggregate`;
  `mo.observable(o, label)` for any `Observable`.
- Valid case: all required fields populated via `RandomValue`; nullables
  are null; assert `invalidInvariants().isEmpty()`.
- Invalid case: construct with nulls for every component. Use
  `invalidInvariantNamesRemovingPrefix(mo.observationPoint())` and
  `containsExactlyInAnyOrder` with exact domain-relative paths.

---

## 5. Namespace Patterns (ADR-020)

Three coordinated namespace types per domain api:

| Outer | Java type | Visibility | Nested |
|-------|-----------|------------|--------|
| `<DomainNoun>Repository` | `class` | package-private | `<EntitySubject>Repository` (`protected interface`) |
| `<DomainNoun>Query` | `interface` | public | `<EntitySubject>Query`, `<EntitySubject>AggregateQuery` |
| `<DomainNoun>EntityCollections` | `interface` | public | `<EntitySubject>Collection` (`final class`) |

`EntitySubject` drops the domain prefix: `InsectSpecies` → `Species`,
`InsectImage` → `Image`.

**Why class for repository, interface for query.** Nested types inside an
interface are implicitly `public static` — visibility cannot be restricted.
A class keeps repository contracts hidden (`protected` = package-private +
subclass access). Queries *want* their nested types public.

**N=1 collapse.** When a package has exactly one entity, skip the namespace:
top-level package-private `<Entity>Repository` interface + top-level public
`<Entity>Query` interface.

**Aggregate value-object nesting.** A `ValueObject` exclusively reachable
through a single `Entity` / `Aggregate` nests inside that entity's file as a
`static record`. Promote back to top-level when it gains a standalone
lifecycle, is referenced cross-domain by name, or appears in more than one
entity's graph.

---

## 6. Queries, Repositories, Factories

- Repository interfaces are **package-private in `<domain>-api`**.
- A repository has exactly four responsibilities: entity cache, referential
  integrity, unique constraints, transactional consistency. No logic.
- Cross-domain references use `EntityName` slug — never `PersistenceId<Long>`.
- Cross-domain joins are prohibited; cross-domain FK enforcement is deferred
  to the RDBMS layer.
- No cross-repository queries within a sub-context. No cross-sub-context
  repository access.

### Query design (ADR-010)

- **Thin.** A query adapter validates arguments via
  `observer().arguments(...)`, then forwards to the repository (or to an
  aggregate factory). No logic, no multi-step composition.
- Return types are `Optional<Entity>`, `Optional<Aggregate>`, or a
  `BehavioralCollection` subclass. **Raw `List<T>` at the port boundary is
  a review flag.**

### Aggregate factory placement — NON-NEGOTIABLE

- Aggregate factories NEVER live in `<domain>-api`.
- Single concrete class in `<domain>-core`.
- **No interface. No `Impl` suffix. Package-private.**
- A factory type surfacing in the api module is a review blocker — it leaks
  assembly concerns to consumers and makes the implementation detail
  Spring-injectable across module boundaries.

### Repository behavioral contract (ADR-002)

- Every `NamedEntityRepository` has a behavioral contract as a
  `@Test default` interface in `<domain>-repository-test/src/main/java/`.
- Extends `NamedEntityRepositoryContractTest<NAME, ENTITY>`.
- Each select method: three test cases — argument validation (null
  rejection), empty result, expected result.
- Write methods: argument validation, constraint violations / entity-not-found,
  expected result.
- Insert and update expected-result tests observe the persisted entity via
  the Observer framework — walks the full constraint graph to catch adapter
  serialization drift.

Reference implementation: `domains/insects/insects-api/` —
`InsectRepository`, `InsectQuery`, `InsectEntityCollections`,
`InsectSpecies`. Adapters in `insects-core/`: `InsectQueryImpl`,
`SpeciesQueryImpl`, `ImageQueryImpl`, `InsectAggregateQueryImpl`, and the
factory `InsectAggregateFactory` (concrete, no interface).

---

## 7. BehavioralCollection (ADR-011)

- `final class`, not a record.
- Extends `BehavioralCollection<T extends Observable>` from
  `kernels/framework`.
- Lives in the owning `<domain>-api`.
- Constructor is package-private; `public static of(...)` and
  `public static empty()` are the only external instantiation paths.
- `List.copyOf` defensive copy is inherited from the base class.
- Domain-specific filtering methods return new instances via the
  package-private constructor.

Reference: `CompoundCollection` in `chemistry-api`.

---

## 8. Test Fixtures & Test Identifiers

### Test identifiers (`domains/identifiers-test`)

- One `Test<Domain>Identifiers` class per domain — single source of truth
  for `EntityName` constants used in repository contract tests.
- **Never inline these into test classes.**
- Class structure **mirrors the domain object graph** — ownership and
  composition reflected as nested static classes.
- **Child entities nest inside their parent** — never as a sibling top-level
  class. `InsectImage` owned by `PotatoLeafhopper` goes inside
  `PotatoLeafhopper.Images`, not a parallel `InsectImages` class.
- Every entity type must define **at least two** known `EntityName`
  constants (single constant doesn't distinguish single-entity from
  partial-match lookups).
- Each parent scope defines a **single** `NotFound` inner class — child
  scopes do not redeclare their own.
- Fictitious names must be obviously synthetic: `"unobtainium-oxide"`,
  `"Xx"`.

### Test entity sources (`NamedTestEntitySource`)

- Model/data separation: Java defines schema, JSON defines instances.
- Use `/test-entity-source <EntityClassName> in <domain> module` to scaffold.
- Name uniqueness is enforced automatically by `NamedTestEntitySource`.
- Secondary unique constraints declared via `uniqueConstraints()` (defaults
  to `List.of()`). Override only for fields annotated `@EntityIdentifier` or
  `@UniqueValue`.

### JSON catalog rules

- `"name": "<slug>"` — the EntityName natural key.
- No `id` field — domain records carry no `PersistenceId`.
- Remaining fields match record component names exactly.
- Enum values serialize by constant name.
- JSON location: `<domain>-repository-test/src/main/resources/<domain>/<subpackage>/`.

Reference: `CompoundTestEntitySource` +
`chemistry/compound/compounds.json`.

### Test data is real

Fixtures use actual measurements. `TestEntitySource` seeds with real FGL
data, real sensor readings, real amendment history. A failing test
indicates a domain model error or a real-world change — not a mocking
mismatch. **Never mock the database in contract tests.**

---

## 9. Scaffolding a New Module

1. **Directory + pom.xml.** `kernels/<module>/pom.xml` (parent `kernels`) or
   `domains/<domain>/<domain>-<type>/pom.xml` (parent `<domain>`).
   Create `src/main/java/com/naturalist/<module>/`.
2. **Add to parent `pom.xml` `<modules>`.** Alphabetical.
3. **CRITICAL: add dependency-management entry to root `pom.xml`.**
   Version always `${project.version}`. Forgetting this breaks builds in
   other modules that reference the new one.
4. Update dependent `pom.xml` files to include the new module (no version
   tag — inherited from dependencyManagement).

---

## 10. Anti-Patterns — Specific Things Previous Chat Sessions Got Wrong

**Do not invent package paths.**
- ❌ `com.naturalist.identifiers.LifeStageKind`
- ✅ `com.naturalist.insects.lifestage.LifeStageKind` (if in insects-api) or
  `com.naturalist.insects.LifeStageKind` (if in identifiers module)
- **When in doubt, say "I don't know the package path for X — please
  confirm."**

**Do not invent methods on `Constraints`.**
- Check the table in §4 before calling a method. If it's not in the table,
  it doesn't exist.
- Historical invention: `.notEmpty(this, fn, name)` was called before the
  method was added. `namedEntityOrNull` is still NOT in the kernel.

**Do not create interfaces for aggregate factories.**
- Single concrete class, package-private, in `<domain>-core`. No interface,
  no `Impl` suffix.

**Do not place `PersistenceId` anywhere.**
- It does not exist in Java. Domain records carry `EntityName` (via
  `NamedEntity`) or `EntityId` (via `Entity`) — nothing else
  identity-shaped.

**Do not call `UUID.randomUUID()` in domain or adapter code.**
- Use the kernel's UUIDv7 generator only.

**Do not nest an Entity or Aggregate inside a ValueObject.**
- ADR-013 forbids it. The moment a nested type becomes an Entity, the
  wrapper must reclassify or be removed.

**Do not add `@JsonCreator` to records.**
- Jackson 2.19 handles records natively. `@JsonCreator` belongs on the
  `public static of(...)` factory of `EntityName` subclasses and
  `NamedValue<T>` implementations only.

**Do not use raw `List<T>` at query port boundaries.**
- Return `Optional<Entity>`, `Optional<Aggregate>`, or a
  `BehavioralCollection` subclass.

**Do not add caching inside adapters.**
- Caching is a last resort. Default to storage-layer resolution (RDBMS
  planner, indexed lookups). If caching is ever justified: Redis at
  infrastructure, never in-process inside an adapter.

**Do not skip the "why" in feedback.**
- Reasons behind conventions (prior incidents, architectural goals) matter
  for judgment calls at edges. The "why" is what lets chat extrapolate
  correctly.

---

## 11. Key ADRs — Quick Reference

| ADR | Subject |
|-----|---------|
| ADR-001 | Repository architecture |
| ADR-002 | Repository behavioral contract |
| ADR-004 | Module DAG |
| ADR-005 | EntityName slug format (regex, kebab-case) |
| ADR-010 | Query design contract |
| ADR-011 | BehavioralCollection as `final class` |
| ADR-012 | Static factory conventions (`of`, `empty`, `from`) |
| ADR-013 | ValueObject constraints (four-point test) |
| ADR-014 | Tier-3 identifier placement |
| ADR-017 | Observability — producer vs. consumer |
| ADR-019 | PR size discipline |
| ADR-020 | Namespace/interface pattern (Repository/Query/EntityCollections) |
| ADR-021 | Persistence-id is adapter-internal (**superseded by ADR-022**) |
| ADR-022 | UUIDv7 / two identity branches (`NamedEntity`, `Entity`); no `PersistenceId` in Java |

Full text: `docs/adr/` in the repo.

---

## 12. Communication Conventions for the Codebase Owner

- **Push back when reasoning is wrong. Do not validate.** User is a principal
  engineer (25+ years). Direct and precise, no preamble, no filler.
- Flag DAG violations, architecture violations, and domain-model errors
  immediately.
- Prose over bullet points for explanations. Short answers for simple
  questions; full depth for complex ones.
- Prefer "control flow" terminology over "data flow" — the latter is
  reserved for value-propagation analysis.
- **Cognitive complexity is the driver** behind structural patterns. The
  naturalist (domain user), not the developer, is the primary audience for
  the api surface. Reduce cognitive load across domains.

---

## 13. How to Use This Briefing in a Chat Session

1. **Before designing.** Upload the briefing, then state the problem. Ask
   for design shape, identity choice, module placement — the briefing
   provides the vocabulary for a grounded answer.
2. **When generating code.** After design agreement, ask chat to produce
   code. The briefing has enough context to avoid invented APIs and invented
   packages. Remaining gaps (exact type names, exact line counts, test
   wiring) should be flagged by chat rather than invented.
3. **When the task spans domains.** Name every domain involved. The
   briefing names canonical identifier locations in §2; if a domain isn't
   listed, ask the user before assuming.
4. **When chat proposes a kernel change.** That is out of scope unless the
   user explicitly agrees. Flag the gap in chat; let the user decide whether
   to expand scope.
5. **When chat writes a design doc.** A draft design doc is not an
   implementation plan. It may contain aspirational claims ("move X to
   identifiers module"). Before acting on any such claim, verify against the
   current codebase or ask.

---

## 14. Glossary of Terms Chat Often Confuses

- **Module** (Maven) vs. **package** (Java). The `identifiers` module
  contains multiple packages (`com.naturalist.insects`,
  `com.naturalist.plants`, etc.). "`identifiers`" alone refers to the
  Maven artifact, not a Java package.
- **Sub-context** — a Java package within a domain module. Package-private
  visibility is the enforcement mechanism. Not a separate bounded context
  in the strict DDD sense.
- **NamedEntity branch** vs. **Entity branch** (ADR-022). Two parallel
  identity disciplines. Choose per entity based on "does this thing have a
  natural key?"
- **Aggregate root** — the `NamedEntity` / `Entity` at the top of an
  aggregate's composition graph. The aggregate's identity is the root's
  identity.
- **Invariants** vs. **validation**. Invariants are structural rules the
  domain enforces always. Validation is boundary-time argument checking.
  Both flow through the Observability framework.
- **Observer** (record of observations) vs. **Observable** (thing being
  observed). An `Observer` is owned by a method; an `Observable` is a
  domain type.
