# Command Framework

## Context

The naturalist framework today exposes only a **read** api. Each domain ships a public
`<Domain>Query` namespace interface (ADR-020) backed by package-private `*QueryImpl`
adapters extending `AbstractEntityQuery` (ADR-010). Mutations are vocabulary-only on
`EntityRepository` and reachable through the package-private `<Domain>Repository`
namespace class — there is no public write port. That is why every controller in
`apps/management-console` is read-only today: there is nothing public to call.

This plan introduces the symmetric write side: an `EntityCommand` port, an
`AbstractEntityCommand` template that mirrors `AbstractEntityRepository`, a
public `<Domain>Command` namespace interface per ADR-020, and a behavioral
contract test so command adapters share the same three-case discipline as
queries and repositories. The scope deliberately stops at the kernel + one
pilot domain (insects). No controller wiring lands in this PR.

The user has confirmed the policy for write failures: **fail-fast, each layer
throws what it knows.** A command that knows its repository call will fail
should throw; a repository that knows the RDBMS layer will fail should throw.
Errors propagate up through the layers rather than being swallowed. This
supersedes the "update for non-existent id is a silent no-op" sentence in
ADR-006 — see *Follow-ups* at the bottom.

## Design

### 1. Kernel additions (`kernels/framework`)

Two new types in `com.naturalist.data`, mirroring `EntityQuery` /
`AbstractEntityQuery` exactly:

```java
public interface EntityCommand<NAME, E extends Named<NAME>> {
    void insert(E entity);

    void update(E entity);
}

public abstract class AbstractEntityCommand<
        NAME,
        E extends Named<NAME>,
        R extends EntityRepository<NAME, E>> implements EntityCommand<NAME, E> {

    private final R repository;
    private final Observer observer = Observer.forClass(getClass());

    protected AbstractEntityCommand(R repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    protected R repository() {
        return repository;
    }

    protected Observer observer() {
        return observer;
    }

    @Override
    public final void insert(E entity) {
        observer.arguments("insert", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doInsert(entity);
    }

    @Override
    public final void update(E entity) {
        observer.arguments("update", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doUpdate(entity);
    }

    protected void doInsert(E entity) {
        repository.insert(entity);
    }

    protected void doUpdate(E entity) {
        repository.update(entity);
    }
}
```

Why this abstraction earns its keep — same three reasons `AbstractEntityRepository`
does:

1. **Public methods are `final`.** Validation cannot be skipped by an adapter, so
   every domain command boundary rejects nulls identically through the same
   `Observer.namedEntity(...)` constraint graph.
2. **Observer is scoped to the concrete adapter class.** Telemetry and metrics for
   `species.insert(...)` are filed under `SpeciesCommandImpl`, not under whatever
   repository happens to be wired underneath (mock today, RDBMS later).
3. **Hooks (`doInsert`, `doUpdate`) are validation-free.** A trivial command is a
   one-line subclass with a constructor; an orchestrating command overrides the
   hook without re-declaring validation.

The defaults delegate to the repository, so a no-orchestration command is
~10 lines total (file header + class + constructor). Errors from the
repository layer (`PrimaryKeyConstraintException` on duplicate insert,
`EntityNotFoundException` on missing update) propagate unchanged — fail-fast
per the user's policy.

### 2. Behavioral contract (`kernels/framework-test`)

New `EntityCommandContractTest<NAME, E>` in `com.naturalist.data`, mirroring
`EntityQueryContractTest`. Three cases per ADR-002 for each of `insert` and
`update`:

| Method   | Cases                                                                                                                             |
|----------|-----------------------------------------------------------------------------------------------------------------------------------|
| `insert` | null arg → `InvariantViolationException`; duplicate name → `PrimaryKeyConstraintException`; new entity → retrievable via query    |
| `update` | null arg → `InvariantViolationException`; ghost entity → `EntityNotFoundException`; modified entity → retrievable with new values |

Hooks the concrete contract supplies:

| Hook                       | Purpose                                                                                                                |
|----------------------------|------------------------------------------------------------------------------------------------------------------------|
| `command()`                | The command under test                                                                                                 |
| `query()`                  | Public query for post-state verification (NOT the package-private repository — the contract stays at the api boundary) |
| `source()`                 | The `TestEntitySource` for fixture access                                                                              |
| `notFoundName()`           | Fictitious name guaranteed absent                                                                                      |
| `knownEntityNames()`       | Two known names from fixtures                                                                                          |
| `newEntity()`              | Valid entity with unique name                                                                                          |
| `ghostEntity()`            | Entity whose name is absent from the catalog                                                                           |
| `modifiedEntity(original)` | The original with every mutable field changed                                                                          |

The last five hooks duplicate what `EntityRepositoryTest` already requires per
domain. **Do not extract a shared super-interface in this PR** — the duplication
is small and visible, and a premature shared base would entangle two contracts
that may diverge as command-only concerns appear (e.g. asynchronous-delivery
assertions). Revisit once a third writer contract appears.

### 3. api namespace (per ADR-020)

The query side uses a public namespace **interface** because nested types are
implicitly public (the consumer surface). Commands follow the same shape:

```java
public interface InsectCommand {
    SpeciesCommand species();

    ImageCommand images();

    interface SpeciesCommand extends EntityCommand<InsectSpeciesName, InsectSpecies> {
    }

    interface ImageCommand extends EntityCommand<InsectImageId, InsectImage> {
    }
}
```

For an N=1 domain, collapse to a single top-level public
`interface <Entity>Command extends EntityCommand<...>`, matching the query
collapse rule in ADR-020.

### 4. core implementations

Per-entity command adapters live in `<domain>-core` alongside the query
adapters:

```java

@DomainService
class SpeciesCommandImpl
        extends AbstractEntityCommand<InsectSpeciesName, InsectSpecies, InsectRepository.SpeciesRepository>
        implements InsectCommand.SpeciesCommand {
    SpeciesCommandImpl(InsectRepository.SpeciesRepository repository) {
        super(repository);
    }
}
```

Namespace impl mirrors `InsectQueryImpl` — package-private, validates
constructor args via `Observer.forClass(InsectCommandImpl.class)`, holds and
returns the per-entity command refs. **Not** annotated `@DomainService` (same
as `InsectQueryImpl` today) — it is constructed inside the test context.

### 5. Test context

`InsectsTestContext` gains a public `insectCommand()` accessor and the
constructor wires `InsectCommandImpl(speciesCommand, imageCommand)` after the
existing query wiring. **The "Read seam only" javadoc on the class must be
updated in the same PR** — adding command accessors directly contradicts it
and the next reader will think the change was an oversight.

## Pilot rollout

One PR. Insects only. No controller wiring.

| Module                                 | Files added / changed                                                                     |
|----------------------------------------|-------------------------------------------------------------------------------------------|
| `kernels/framework`                    | `EntityCommand.java`, `AbstractEntityCommand.java` (new)                                  |
| `kernels/framework-test`               | `EntityCommandContractTest.java` (new)                                                    |
| `domains/insects/insects-api`          | `InsectCommand.java` (new — public namespace interface)                                   |
| `domains/insects/insects-core`         | `SpeciesCommandImpl.java`, `ImageCommandImpl.java`, `InsectCommandImpl.java` (new)        |
| `domains/insects/insects-core` test    | `SpeciesCommandImplTest.java`, `ImageCommandImplTest.java` (new — implement the contract) |
| `domains/insects/insects-test-context` | `InsectsTestContext.java` (add `insectCommand()` accessor; revise class javadoc)          |

Aggregate-level commands are deferred. The query side has
`InsectAggregateQuery` because reads compose naturally; writes compose
through controller orchestration, not through a packaged "create species +
image" call. Revisit when a real use case forces it.

## Verification

1. **Unit tests** — both `*CommandImplTest` classes implement
   `EntityCommandContractTest` and run against the mock repository wired through
   the same `NaturalistDatabase` extension already used by the query and
   repository contract tests.
2. **Build** — user runs maven; type-check and contract suites must pass.
3. **End-to-end smoke (no controller route this PR)** — write a small
   `InsectCommandIntegrationTest` in `insects-core` that exercises:
   `command.species().insert(newSpecies)` → `query.species().getByName(name)`
   round-trip via `InsectsTestContext`. Confirms the public api wiring works
   identically to the read seam, without depending on Spring or HTTP.
4. **Module-DAG check** — confirm no new edges. `EntityCommand` is in
   `kernels/framework`, used by `insects-api`; `*CommandImpl` is in
   `insects-core`. Existing edges already cover all of this.

## Critical files to read before implementing

- `kernels/framework/src/main/java/com/naturalist/data/AbstractEntityRepository.java` — the canonical worth-abstracting
  template; mirror its shape exactly
- `kernels/framework/src/main/java/com/naturalist/data/AbstractEntityQuery.java` — the read-side analogue
- `kernels/framework-test/src/main/java/com/naturalist/data/EntityRepositoryTest.java` — the three-case behavioral
  contract pattern to mirror in `EntityCommandContractTest`
- `kernels/framework-test/src/main/java/com/naturalist/data/EntityQueryContractTest.java` — read-side contract template
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java` — namespace interface reference
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java` — package-private namespace
  class (commands consume nested `protected interface`s)
- `domains/insects/insects-core/src/main/java/com/naturalist/insects/SpeciesQueryImpl.java` — adapter shape to mirror in
  `SpeciesCommandImpl`
- `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java` — namespace impl shape to
  mirror in `InsectCommandImpl`
- `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java` — gains
  `insectCommand()` accessor; javadoc rewrite

## Reuse — do not re-create

- `Observer.forClass(...)` and `Constraints.namedEntity(...)` — argument validation
- `EntityRepository<NAME, E>` — write port already exists; commands wrap it
- `EntityNotFoundException`, `PrimaryKeyConstraintException`, `InvariantViolationException` — already thrown by the
  repository layer; commands let them propagate
- `NaturalistDatabaseExtension` — JUnit extension for per-test reset; both contract tests use it
- `@DomainService` (ADR-025) marker; the spring-runtime adapter discovers `*CommandImpl` automatically — no DI changes
  needed

## Index hygiene

Add a row for this plan to `docs/plans/README.md` under **Active** as part of
the implementing PR (the plans README is the index and is the only place
status is tracked).

## Follow-ups (not in this PR)

1. **Console controller wiring.** A separate PR adds the first `@PostMapping`
   route in `insects-console` calling `insectCommand.species().insert(...)`.
   That PR pulls in CSRF/security review and JTE form rendering — kept out of
   the framework PR per ADR-019.
2. **Second-domain rollout.** Replicate to chemistry once the framework PR
   merges. Chemistry is N=3 (Compound, Element, Product), proves the
   abstraction at higher cardinality.
3. **Aggregate-level commands.** Revisit when a controller route genuinely
   needs to coordinate multi-entity writes. A real shape will be obvious then;
   guessing now would prejudge the orchestration site.
