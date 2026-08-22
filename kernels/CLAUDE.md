# Shared Kernels

The kernels are the foundation every other module builds on. Changes here ripple across
the entire codebase — treat them as stable contracts, not convenient places to add things.

## Core Kernels

### framework

DDD building blocks. `NamedEntity`, `Entity`, `Aggregate`, `ReadModel`, `ValueObject`,
`BehavioralCollection`, `Observable`, `Constraints`, `EntityName`, `EntityId`, `Observer`.
No domain knowledge — pure structural vocabulary. Everything else depends on this.

Two entity branches sharing a `Named<KEY>` supertype (ADR-022). The shared port
method is `key()`; each branch declares its own semantic accessor and defaults
`key()` to it:

- `NamedEntity<NAME extends EntityName>` — natural-key slug identity. Declares
  `name()`; `key()` delegates to `name()`.
- `Entity<ID extends EntityId>` — surrogate UUIDv7 identity, generated at record
  construction. Declares `id()`; `key()` delegates to `id()`. `EntityId` validates
  `UUID.version() == 7` and qualifies equality by concrete class.

The framework also ships the **`Resilience` facade** (`com.naturalist.resilience`):
`Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead`, plus `@Resilient` and
`@ResilienceExempt` annotations and a sealed `ResilienceConfig` record. Domain
`*-core` code references the facade only — never Resilience4j directly. The
production implementation lives in `adapters/resilience-resilience4j/`. Default
`Resilience.noOp()` is used in unit tests and unwired composition roots. See
ADR-026 and `docs/resilience-policy.md` for the cross-boundary policy and the
M9 build-time compliance gate.

The `@DomainService` marker (`com.naturalist.infrastructure.DomainService`)
also lives here — a runtime-retention `TYPE` marker with no third-party
meta-annotations. The `adapters/spring-runtime/` module discovers annotated
classes and registers them as Spring beans without domain code importing
Spring (ADR-025).

### framework-test

Test infrastructure. `TestEntitySource`, `TestEntitySourceTest`,
`EntityRepositoryTest`, `UniqueConstraint`, `ForeignKeyConstraint`, and the
`NaturalistDatabase` source registry with its JUnit lifecycle wrapper
`NaturalistTestExtension`. Used by all `<domain>-repository-test` modules. Never a
compile-scope dependency.

`NaturalistDatabase` is the in-memory analog of the single production database
(ADR-001): it owns every `TestEntitySource` and is the only object that constructs
them. Acquire a source through `NaturalistDatabase#getNamed(SourceClass.class)`, which
lazily builds and caches one instance per class so the whole test (or unwired
composition root) shares one catalog. Each source subclass declares a
`(NaturalistDatabase)` constructor — the signature `getNamed` reflects on — and enforces
primary-key uniqueness, secondary `UniqueConstraint`s, and intra-domain
`ForeignKeyConstraint`s. `NaturalistTestExtension extends NaturalistDatabase implements
BeforeEachCallback`: registered as a static `@RegisterExtension` field, it resets the
registry before each test; main-wired code uses `NaturalistDatabase.create()` and carries
no JUnit coupling.

An in-progress N+1 select-count gate lives alongside in `com.naturalist.data.count`
(recorder, aspect, `@AllowRepeatedSelect`); it is not yet wired into
`NaturalistTestExtension`. See `docs/plans/2026-08-21-n-plus-one-select-gate-plan.md`.

### field-notes  (`com.naturalist.fieldnotes`)

The Durrell principle in code. `Description` carries the same truth at four levels of
understanding — preschool, elementary, secondary, university — for any entity the system
can describe. Chemistry compounds, climate thresholds, plants, organisms: all of them.

**Every domain api depends on this.** If you are building a new named entity and it does
not have a `Description`, that is a deliberate omission worth questioning.

```java
import com.naturalist.fieldnotes.Description;
```

### taxonomy  (`com.naturalist.taxonomy`)

Linnaean classification. `TaxonomicClassification` holds order, family, genus, and species
with `binomialName()` and `isSpeciesLevel()` convenience methods. Nullable genus and species
accommodate family-level field identifications where species cannot be confirmed.

**Organism domain apis only** — insects, arachnids, worms, microbes, molluscs, vertebrates,
plants. Chemistry, climate, soil, zone, and sensors have no use for Linnaean taxonomy.

```java
import com.naturalist.taxonomy.TaxonomicClassification;
```

### clades  (`com.naturalist.clades`)

The evolutionary tree of life as a curated, controlled vocabulary. `Clade` is a
**sealed interface** with one stateless `record` permit per recognised clade
(`Eukaryota`, `Animalia`, `Arthropoda`, `Insecta`, `Holometabola`, `Lepidoptera`,
`Papilionidae`, expanding as needed). Each permit carries its slug, display name,
four-level Durrell `Description`, and an `Optional<Clade>` parent reference
(`Optional.empty()` at Eukaryota). The shape mirrors `biogeography.Bioregion`
— adding a clade is a deliberate kernel PR, not free-text data entry.

```java
import com.naturalist.clades.Clade;
import com.naturalist.clades.Holometabola;
import com.naturalist.clades.CladeTraversal;
```

**Why sealed types, not entities.** The clade catalog is small (low hundreds at
the project's upper bound) and curated. It needs single-tree identity —
**value-equal references to the same logical node** across every domain, via
record `equals`/`hashCode`, not actual singleton instances — so trait
inheritance via traversal works. But it does not need entity machinery
(repository, write paths, JSON seed). Sealed records give us all of that with
compile-time discoverability: open `Holometabola.java` to see what Holometabola
*is*. Importing a taxonomic backbone (Catalogue of Life, ChecklistBank) is
explicitly out of scope and would be revisited separately if ever activated.

**Traits are domain-owned.** The kernel holds no trait declarations. `Trait` is a
marker interface; `CladeTraversal#findTrait` takes a
`Function<Clade, Set<Trait>>` supplied by the consumer. The recommended shape
for that function is a pattern-matching `switch` over the sealed permits the
domain cares about, with `default -> Set.of()` for the rest — pure, stateless,
and reviewable. No registry, no startup wiring:

```java
public static Set<Trait> insectTraits(Clade c) {
    return switch (c) {
        case Holometabola _ -> Set.of(new MetabolyTrait(HOLOMETABOLOUS));
        default             -> Set.of();
    };
}
```

A domain that wants a compile-error nudge when new permits are added enumerates
every permit instead of using `default`. Either is fine; the rule is "pure
function in the consuming domain," not "exhaustive everywhere."

Plants, when activated, declares its own trait function over its own clade
nodes. The two domains coexist because the *clade values themselves* are
value-equal across both — no shared registry needed.

See [`docs/plans/clades-kernel.md`](../docs/plans/clades-kernel.md) for the
multi-phase plan.

### catalog  (`com.naturalist.catalog`)

Cross-domain reference resolution. `Catalog`, `CatalogContribution`,
`CatalogAssembly`, `EntityRef`, `EntityReferences`, and the open `DomainId`
interface (ADR-023). Each domain ships its own `DomainId` subtype from its
`*-api` module; the kernel knows the name of no domain. `CatalogAssembly`
validates slug uniqueness across registered contributions and providers at
startup and fails fast on collision. The catalog kernel was renamed from
`atlas` in the runtime architecture refactor; the historical name appears
only in narrative records.

### catalog-inmem  (`com.naturalist.catalog.inmem`)

Reference adapter for `Catalog` that fans out across registered
`EntityReferences` providers in-process. Light dependencies (only the
framework's existing third-party set) — that is why it lives in `kernels/`
rather than in `adapters/`. Production catalog backends with heavy
infrastructure dependencies (Solr) belong under `adapters/` per ADR-024.

`InMemoryCatalog` carries `@Resilient(name = "catalog.fanout")` and wraps
each provider invocation with `resilience.circuitBreaker(...)` over
`resilience.timeout(...)`, so a wedged or failing provider degrades only
that domain's slice of the response (ADR-026).

## DAG Position

```
field-notes  →  framework
taxonomy     →  framework
clades       →  framework, field-notes

<any>-api         →  framework, identifiers, field-notes
<organism>-api    →  framework, identifiers, field-notes, taxonomy
```

## Hard Rules

- `identifiers` contains typed identifiers only — `EntityName` subclasses (slug) and
  `EntityId` subclasses (UUIDv7). No value objects. No behavior. If it is not an
  identifier, it does not belong there.
- `PersistenceId` does not exist in Java. Domain records carry either an `EntityName`
  (via `NamedEntity`) or an `EntityId` (via `Entity`), and nothing else identity-shaped
  (ADR-022, superseding ADR-021).
- Do not call `UUID.randomUUID()` from domain or kernel code. The kernel's UUIDv7
  generator is the only source of `EntityId` values.
- `field-notes`, `taxonomy`, and `clades` contain shared value objects and
  controlled vocabularies only — no entity definitions, no per-domain logic.
  `clades` is the first kernel to use a `sealed interface + record permits`
  shape for a vocabulary; the older value-object kernels remain plain.
- Do not add a new class to any kernel without considering whether it is truly
  cross-cutting. Kingdom-specific concerns belong in the domain module, not here.

## Observability Framework

All domain types implement `Observable`, which requires `Consumer<? extends Constraints> invariants()`. `Constraints` is
a fluent builder with constraint methods in two forms:

**Direct-value form** — pass the component value directly. Use this inside `invariants()` on the record that owns the
field (when `this` is the enclosing record):

- `notNull(value, name)` — general null check
- `notBlank(value, name)` — rejects null, empty, and whitespace-only Strings (`StringUtils.isNotBlank`)
- `entityName(e, name)` — validates `EntityName`

**By-function form** — pass the parent object and a method reference. Use this only when evaluating a child property
from outside the parent (e.g. `namedEntity(this, FooAggregate::fooInfo, "fooInfo")` to descend into a child Observable):

- `namedEntity(o, fn, name)` — validates a `NamedEntity` child and descends into its invariants
- `valueObject(o, fn, name)` — validates a `ValueObject` child and descends into its invariants
- `observable(o, fn, name)` — validates any `Observable` child (e.g. a `BehavioralCollection`)
- `readModel(o, fn, name)` — validates a `ReadModel` child and descends into its invariants
- `notNull(o, fn, name)` — general null check on a child property
- `notBlank(o, fn, name)` — not-blank check on a child String property
- `namedValue(o, fn, name)` — validates a `NamedValue<?>` child property

Do not use the by-function form when the direct-value form suffices — the functional indirection adds no value when
`this` can never be null.

`Observer` validates entities at insertion points — `TestEntitySource.insert()` calls
`observer.arguments("insert", i -> i.namedEntity(entity, "entity")).throwWhenInvalid()`. Insertion is an
argument-validation site: the producer refuses bad input at its boundary. For method-body observation of *produced*
state, the producer uses `.observe()` (metrics only) and hands the value to the consumer, which chooses the terminal
operation. See ADR-017's *Producer vs. consumer* section. Use `@Nullable` from JSpecify (`org.jspecify`) for nullable
field documentation.

## Testing Observables

Every domain type (`NamedEntity`, `Entity`, `Aggregate`, `ReadModel`, `ValueObject`,
`BehavioralCollection`) implements `Observable` and declares `invariants()`. Unit tests verify invariants via
`Observer` → `MethodObserver` → `InvariantObservation` — never by calling `isValid()` on
individual `Constraint` objects. The `InvariantObservation` gives the full set of failing
invariant names in a single assertion, eliminating the need to debug which constraint broke.

One `Observer` per test class (static field). One `MethodObserver` per test method. See
any `*Test.java` in a `*-api` module for the canonical pattern.

**Valid case:** construct with all required fields populated via `RandomValue` helpers;
nullable components are null; assert `invalidInvariants().isEmpty()`.

**Invalid case:** construct with nulls for every component. Use
`invalidInvariantNamesRemovingPrefix` with `mo.observationPoint()` to strip the
test-infrastructure prefix, then assert the exact set of domain-relative invariant paths
with `containsExactlyInAnyOrder`.

Key rules:

- `mo.forMethod(...)` must match the test method name exactly — it scopes the observation
- `mo.namedEntity(e, label)` for `NamedEntity`/`Aggregate`; `mo.observable(o, label)` for any other `Observable` (`ReadModel`, `BehavioralCollection`)
- Invalid-case assertions use `containsExactlyInAnyOrder` — exact set, no extras, no missing
- `observationPoint()` returns `ClassName.methodName` — the prefix before the label segment

## BehavioralCollection

Behavioral collections are `final class`, not records. They extend
`BehavioralCollection<T extends Observable>` from `kernels/framework` and live in the
owning `<domain>-api`. Constructor is package-private;
`public static of(...)` and `public static empty()` are the only external instantiation
paths. `List.copyOf` defensive copy is inherited from the base class constructor.
Domain-specific filtering methods return new instances via the package-private constructor.
See ADR-011 and ADR-012. Reference implementation: `InsectEntityCollections` in `insects-api`.

## Kernel Testing Convention

**Kernel code is tested through its first consumer**, not via dedicated unit tests in
`kernels/<kernel>/src/test/`. Most kernel primitives (`NamedEntity`, `Constraints`,
`Observer`, `ObservableConstraint`, etc.) are exercised transitively by every domain
test — a subtle bug in the framework turns half the suite red on the next
`mvn verify`. Direct kernel tests would duplicate that coverage.

**Default: no direct kernel test.** The first consumer's existing tests prove the
primitive works in the shape that actually matters.

**Write a direct kernel test when one of these holds:**

- **No immediate consumer.** A new primitive lands ahead of the consumer it's
  designed for. Write a small placeholder test that documents the semantics; fold
  into the consumer's tests once a real consumer arrives.
- **Subtle non-orthogonal semantics.** The primitive has edges the first consumer
  doesn't exercise — e.g. a constraint that behaves differently when both parent
  and value are null, but no consumer constructs that shape. Test the uncombined
  edge directly.
- **Stateful or lifecycle behaviour.** Registries, thread-locals, clock-bound
  helpers. Direct tests pin lifecycle cleanly in a way consumer tests don't.

**Recent example.** `NamedEntityOrNullConstraint` shipped without a kernel test
(commit `150009d`); `InsectFamily.invariants()` and `InsectFamily.withEgg` are the
first consumers, and their tests cover both null and non-null paths through the
constraint. Adding a kernel-level test would re-verify edges that
`InsectFamilyTest` already covers.

**Scope of this convention.** Applies to all kernels: `framework`, `framework-test`,
`field-notes`, `taxonomy`, `clades`, `biogeography`, `catalog`, `catalog-inmem`,
`habitat`, `measurements`. The `framework-test` kernel additionally carries the
"no test infra for test infra" rule — don't add unit tests for code that exists
only to support other tests; rely on manual smoke + the consuming `*-repository-test`
modules.
