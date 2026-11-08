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

The framework also hosts the cross-boundary **ports** whose vendor adapters live
under `adapters/`, each with a zero-dependency default beside it so dev and CI run
with nothing external: `com.naturalist.notification.EmailSender`
(`LoggingEmailSender`), `com.naturalist.secrets.SecretProvider`
(`EnvironmentSecretProvider`, `FileSecretProvider`; ADR-028), and
`com.naturalist.challenge.ChallengeVerifier` (`LogOnlyChallengeVerifier`; ADR-027 D6:
a `ChallengeSubmission` in, a `ChallengeVerdict` of `PASSED | FAILED | UNAVAILABLE`
out; the fail-open/closed decision belongs to the caller, never the port). Every
default is an explicit composition-root opt-in, never a silent fallback.

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

An N+1 select gate lives alongside in `com.naturalist.test.query.nofanout` (`SelectTrace`). It
is **wired and ARMED** in `NaturalistTestExtension`: each `TestEntitySource` read walks the stack
to the `@Test` method and counts per (source, repository method), and `afterEach` throws
`RepeatedSelectException` when one call from the test read a select twice. It sees Queries and
Commands alike; `<domain>-core` is what it watches, and rdbms adapters are not traced. Test contexts
hand out traced objects, so a test may loop over their commands and queries.

The `com.naturalist.test.context` package builds those contexts: `TestContext`, `Provisions`,
`ObjectGraph`, `PackageScan`, and the `@Wired` / `@Fake` field annotations that
`NaturalistTestExtension` fills before each test (`get` and `context` serve per-method fakes). Ports are
handed out as `SelectTrace` JDK proxies, non-final classes the graph builds through a constructor as ByteBuddy
`SelectTrace.Traced` subclasses.
An outside-world port, a type whose package has no context, has one provider per graph: the test's own
context, else the only context that provides it. ByteBuddy is excluded from `management-console`'s compile-scope
`framework-test`, which ships for `TestEntitySource`: the app never builds a graph. Its tests do, through
test-scoped domain `-test-context` facades, with ByteBuddy declared again at test scope; `PackagedJarIT`
keeps it out of the jar.
Contexts are discovered through `META-INF/services/com.naturalist.test.context.TestContext`. A fake stays in
the graph's memory, and only an outside-world port takes one: faking a type whose package has a test context
(a domain service) throws. See `domains/CLAUDE.md` "Test Contexts". Allow a genuinely
un-batchable repeat with `@SuppressRepeatedSelect(reason=…)` on the production method doing it.
A static backstop `NoSelectInIteration` (composite `com.naturalist.EnforceQueryHygiene`)
lives in `tooling/naturalist-rewrite`, flagging loop/stream fan-out of repository/query
selects in main source; it is **ARMED** (2026-08-23) — folded into the `EnforceArchitecture`
composite that the completeness gate runs, after all of the reckoning's findings were batched.

### field-notes  (`com.naturalist.fieldnotes`)

The Durrell principle in code. `Description` carries the same truth at four levels of
understanding — preschool, elementary, secondary, university — for any entity the system
can describe. Chemistry compounds, climate thresholds, plants, organisms: all of them.

**Every domain api depends on this.** If you are building a new named entity and it does
not have a `Description`, that is a deliberate omission worth questioning.

```java
import com.naturalist.fieldnotes.Description;
```

### identification  (`com.naturalist.identification`)

Shared identification vocabulary, framework-only. Today that is `IdentificationRefused`
(`Reason` = `RATE_LIMITED` | `BUDGET_EXHAUSTED` | `BUSY`, plus a `detail` string carrying the budget's
limit-kind name and an optional `resetAt`): the port-level refusal any adapter can catch
without depending on `usage-api` or the resilience kernel. The identification *pipeline*
joins this kernel later, when plants identification is built. It is not part of `vision`:
`vision` is the driven-side vendor port, whereas the pipeline orchestrates it alongside
usage, authority, text-generation and feature-search.

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

Inverse-direction fan-out (`domainsReferencing`/`findReferencesTo`) is
storage-independent and lives in `ReferenceRouting` (kernel `catalog`), which
wraps each provider invocation with `resilience.circuitBreaker(...)` over
`resilience.timeout(...)` under the `catalog.fanout` name, so a wedged or
failing provider degrades only that domain's slice of the response
(ADR-026). Both `InMemoryCatalog` and `RdbmsCatalog` delegate to the same
`ReferenceRouting` instance and each keeps `@Resilient(name =
"catalog.fanout")` on its own `findReferencesTo` override.

### observation `ImageStore` / image-store-filesystem  (`com.naturalist.observation`, `com.naturalist.observation.filesystem`)

`observation` also owns the `ImageStore` port (`store(space, ImageUpload)` /
`read(space, FileName)`), the `ImageUpload` value (sniffs JPEG/PNG/WebP by magic
bytes, capped at `MAX_BYTES`) and the `StoredImage` read result. `store` mints the
file name itself (UUIDv7 + sniffed extension) and never trusts a client name.
`kernels/image-store-filesystem` is the reference implementation, `FilesystemImageStore`,
over a directory root with a classpath fallback (`<space>/images/<name>`) for seed
photos. It has no vendor dependency, so it lives in `kernels/` like `catalog-inmem`;
every `Files.*` call runs under `resilience.timeout("image.store")`.

### catalog-rdbms  (`com.naturalist.catalog.rdbms`)

Postgres/MyBatis adapter for `Catalog`: per-domain `<domain>_catalog_token`
SQL views unioned into `catalog_search_token`, queried by `RdbmsCatalog` with
`pg_trgm` fuzzy/type-ahead search. `RdbmsCatalogAssembly` is the
composition-root factory (mirrors `CatalogAssembly` in `catalog-inmem`) and
validates that every `(domain, entity_type)` the view can emit resolves
against the registered `DomainId`s and `EntityName` reconstructors. Its
matching semantics differ deliberately from the in-memory reference adapter
— see the adapter-defined section of `Catalog#search`'s Javadoc
(`kernels/catalog`).

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

**Predicate constraints** — `isTrue(boolean, name)` / `isFalse(boolean, name)` — carry a complex condition under the
name of the method that computes it (`isTrue(family.belongsToOrder(order), "familyBelongsToOrder")`,
`isFalse(token.isConsumed(), "isConsumed")`), so the metric tag names something a reviewer can open. Use them for
cross-field invariants and for an operation's *decisions*: observe the decision at `WARN` via
`observer.forMethod(m).state(...).observe(Level.WARN)` before throwing the exception the operation already throws.

**Arguments vs state.** `arguments(...)` is for values the *caller passed* — the method's boundary contract, always
`throwWhenInvalid()`. `state(...)` takes the same constraint lambda for values the method *acquired or computed during
its own run* — an aggregate fetched from another api, a count, a looked-up mapping, a decision — and the terminal
operation is the consumer's choice. The two are told apart by the `kind` tag (`argument` | `state` | `observable`) on
`naturalist.invariant.check`, so a violation points at the caller or at what the method consumed. A value that is not
one of the method's parameters (or an element of one) never goes through `arguments(...)`. To walk an acquired
object's *own* `invariants()` rather than the facts this method depends on, use `observable(...)` / `namedEntity(...)`.
Security-critical classes (`accounts-core`, console authentication, registration) construct their `Observer` in
`MonitoringMode.ALWAYS` so valid decisions are counted too (as the `INFO` baseline); everything else stays `ON_FAILURE`.

**One `MethodObserver` per method.** A method that observes more than once — its arguments, then a decision, then the
entity it wrote — obtains `MethodObserver m = observer.forMethod("name")` once and routes every observation through it
(`m.arguments(...)`, `m.state(...)`, `m.namedEntity(...)`, `m.observable(...)`); helpers that observe on the caller's behalf take the
`MethodObserver`, never the method name. Spelling the name as a string at each site (`observer.arguments("x", …)` here,
`observer.forMethod("x")` there, `helper("x", …)`) is an anti-pattern: the sites drift apart on rename and the metric
ends up scoped to a method that no longer exists. Reference: `AccountCommandImpl`.

`Observer` validates entities at insertion points — `TestEntitySource.insert()` calls
`observer.arguments("insert", i -> i.namedEntity(entity, "entity")).throwWhenInvalid()`. Insertion is an
argument-validation site: the producer refuses bad input at its boundary. For method-body observation of *produced*
state, the producer uses `.observe()` (metrics only) and hands the value to the consumer, which chooses the terminal
operation. See ADR-017's *Producer vs. consumer* section. Repository reads are the canonical producer:
`AbstractEntityRepository`'s inherited reads observe their result via `forMethod(...).state(...)` at `WARN` and return
it, scoped to the concrete adapter (`forClass(getClass())`), and each domain-specific read does the same through its
own `MethodObserver` (`domains/CLAUDE.md`, Repository Architecture). Use `@Nullable` from JSpecify (`org.jspecify`) for nullable
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
(commit `150009d`); `InsectFamilyInfo.invariants()` was the
first consumer, and its tests cover both null and non-null paths through the
constraint. Adding a kernel-level test would re-verify edges that
`InsectFamilyInfoTest` already covers.

**Scope of this convention.** Applies to all kernels: `framework`, `framework-test`,
`field-notes`, `identification`, `taxonomy`, `clades`, `biogeography`, `catalog`, `catalog-inmem`,
`catalog-rdbms`, `habitat`, `image-store-filesystem`, `measurements`, `observation`. The `framework-test` kernel additionally carries the
"no test infra for test infra" rule — don't add unit tests for code that exists
only to support other tests; rely on manual smoke + the consuming `*-repository-test`
modules.
