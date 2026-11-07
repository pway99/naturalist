# Dynamic Test Contexts — Design

**Status:** approved by Pat 2026-11-07; next step is the implementation plan.

## Why

Every `<domain>-core` test that needs more than one adapter builds the object graph by hand. In insects the
same ~60-line graph is written three times — `InsectsTestContextInternal` (core test sources),
`InsectsTestContext` (`insects-test-context`), and inline in `InsectQueryImplTest`, which wires 9 repository mocks
and 8 query implementations and hand-writes ~60 lines of anonymous library and life-stage stubs before its first
assertion. The copies have already drifted: the internal context substitutes an empty life-stage query because of a
Maven-cycle workaround, so insects-core tests never read real life stages.

`SelectTrace` (the StackWalker N+1 gate, commits `5742d3da`, `0ccedbdd`) needs every object a test calls to be
wrapped by `SelectTrace.traced`. Today ~45 accessors are wrapped by hand, with no enforcement (review follow-up D3),
and anything a test constructs itself is not wrapped at all. A loop in such a test over an unwrapped port
false-fails the gate.

The goal is that the development process itself scales: adding a domain, a port or a test should not add wiring.

## What it is

The `NaturalistTestExtension` becomes the single point of access for anything under test in a `*-core` module,
the way `getNamed` already is for `TestEntitySource`s. A test asks for a class:

```java
@RegisterExtension
NaturalistTestExtension nte = NaturalistTestExtension.create();

InsectQuery query = nte.get(InsectQuery.class);
```

Most tests should not even call `get`. Units under test, and anything else the test uses, are declared as fields
that the extension manages, and fakes are declared the same way:

```java
@RegisterExtension
NaturalistTestExtension nte = NaturalistTestExtension.create();

@Fake VisionService vision = new ScriptedVisionService(SAMPLE_RESULT_JSON);
@Wired InsectCommand.IdentificationCommand identification;
@Wired InsectQuery insectQuery;
```

Before each test the extension hands every `@Fake` field to the test's own context first. It then fills every
`@Wired` field from the graph. Setup methods that only build objects disappear, and every fake is visible as a
declared field. A test that needs a different fake per test method uses `nte.context(...).fake(...)` and then
`nte.get(...)` in the method, because `@Wired` fields are filled before the method runs.

The graph is assembled by reflection, guided by the package's test context. Think of a Spring application context
that scans the classpath, but keep only its rules: no Spring, no classpath scan, no container start-up cost.

## Rules

1. **Domain services run on the repository mocks.** A core test exercises the `@DomainService` classes from
   `<domain>-core` (queries, commands, transactions, factories). Their repositories are the `*RepositoryMock`s
   from `<domain>-repository-test` (marked `@MockDomainService`), reading the extension's `NaturalistDatabase`.
   That is the intended fit, not a compromise: the hexagonal pattern puts an in-memory repository adapter under
   the core so that rdbms adapters and their dependencies stay out of it. The only things that never get a
   stand-in are other domain services: the empty life-stage query and the anonymous library stubs in
   `InsectQueryImplTest` disappear, replaced by the real life-stage and library services on their own mocks.
2. **One instance per type.** Each type is built once and shared across the graph (the graph is a diamond:
   `FamilyQuery` feeds genus, species, image and the ancestry resolver). The extension already resets every
   `TestEntitySource` before each test; adapters hold no state of their own.
3. **A context scans only its own package.** It finds the `@DomainService` and `@MockDomainService` classes in its
   own Java package, once per test JVM. It never looks outside that package, and there is no classpath-wide scan.
   For a requested interface it supplies the one candidate in its package. Two candidates, or none, is a loud
   failure naming the context to fix. Where a package genuinely has two, the concrete context decides which to
   provide.
4. **Constructor or static factory.** Reflection uses the one way the class offers to build it. A type that needs
   special handling (the `InMemoryFeatureSearch` whose supplier closes over the finished `InsectQuery`) gets one
   hand-written line in its context.
5. **Other packages go to their own context.** When an object is needed from another package, that package's
   context provides it. Each context is publicly accessible and is responsible for providing implementations from
   its own bounded context (`library` → `LibraryTestContext`). No context reaches into another's package.
6. **Fakes go through the context.** A test that needs its own fake hands it to the package's test context, never
   to the extension directly and never by constructing the graph itself. Every substitution then has one entry
   point that can be found, counted and reported.
7. **Everything handed out is traced.** Each object is wrapped by `SelectTrace` as it is built, so the gate sees
   the whole graph without hand-wrapping. Interfaces get a JDK proxy; concrete classes get a subclass proxy
   (ByteBuddy). Because the current trace counts only calls the test itself starts, wrapping deep inside the
   graph is safe.

## Internal and external contexts

Each package has two test contexts:

- **Internal** — in `<domain>-core/src/test`. Serves the domain's own tests and can reach package-private
  implementations.
- **External** — in `<domain>-test-context`. Serves other domains' core tests and external adapters.

The two must not each carry a copy of the wiring; the knowledge lives once and both contexts use it. Which
context provides what — including the outside-world ports — is the context's decision:

- `VisionService`, `ExternalAuthority`, `TextGenerationService`, `Resilience`, `IdentificationBudget` and
  `ImageStore` are provided by the test context (today's no-ops and temp-directory store are its defaults).
- Internal versus external decides what to provide; a core test and a consuming domain may get different
  defaults.

## Defects the builder catches

The builder knows every class it constructs, so it enforces what production proxying needs. Any class or method
that carries advice (`@jakarta.transaction.Transactional`) must be reachable by a subclass proxy: not `final`, not `private`,
not on a `final` class. This would have caught the `Transaction.execute` bug (final + `@Transactional`, silently
unproxied by CGLIB, only found by `TransactionProxyReachesDoExecuteTest` in management-console) at unit-test time
in the owning module. The violation names the class and method.

`InsectEntityCommands` is `final` but carries no advice, so production Spring never proxies it and it is not a
bug. Under rule 7 it does not need its own proxy: the command ports it holds are already traced.

## Scope

- **In:** `*-core` module tests; the `NaturalistTestExtension`; every internal and external test context
  (21 context classes across chemistry, garden, insects, library, plants, soil, usage).
- **External adapters decide for themselves.** Each has a different concern:
  - Spring REST / console adapters use a test context inside a Spring `@TestConfiguration` to define beans (as
    `InsectsControllerTestConfig` and `PlantsControllerTestConfig` do today).
  - An rdbms repository may need only the `NaturalistDatabase`.
  The extension does not try to serve them.

## Goals

- One access point for everything under test in core.
- No wiring written twice; adding an implementation adds no wiring unless it is unusual (rule 4).
- The whole graph traced, closing D3 from the SelectTrace follow-ups.
- Every fake registered and reportable.
- Proxy-reachability defects caught in the owning module.

## Non-goals

- Serving external adapter tests (Spring, rdbms).
- Replacing Spring in production composition.
- A general dependency-injection library.

## Anti-goals

- **No Mockito, no generated mocks, no stubbed return values.** Mocks drift from the code they stand for and give
  a false sense of security. ByteBuddy is used only as the proxy library for tracing.
- **No hidden substitution.** A stand-in that does not pass through a context's registration is a defect.
- **No weakening of the gate** (root `CLAUDE.md` non-negotiable). Automatic wrapping must not stop any read from
  being counted.

## Constraints

- ByteBuddy's version goes in the root `pom.xml` `<properties>` only; if Spring Boot manages it, pin to Boot's
  version and mark it `<!-- Boot-managed -->`.
- `kernels/framework-test` follows "no test infra for test infra": no new unit-test suite or fixtures for the
  builder itself; the consuming domains' suites prove it. (`SelectTraceTest` exists under the stateful-lifecycle
  carve-out.)
- The DAG holds: a core module's test classpath cannot depend on its own `-test-context` (Maven cycle).
- Java writes go through Kaname atoms; nothing is committed until Pat says "commit".

## Claude's calls (Pat to check)

These came out of the builder check. They follow from the conversation but Pat did not state them.

- **One graph and one fake registry per test, shared across contexts.** This is the Spring analogy: one
  application context. When plants and insects both reach library, they share one `LibraryCommand`. A fake
  replaces its type everywhere in the graph. "Reportable" means one list per test, printed at the end of the run.
- **Resolution order for a parameter:** (1) a fake the test handed to a context; (2) what the requesting context
  chooses to provide; (3) the owning package's context; (4) reflection over the owning package's candidates. The
  requesting context therefore decides outside-world ports, as Pat said. Kernel ports with no context of their
  own (`Resilience`, `VisionService`, `ImageStore`, `FeatureSearch`, `TextGenerationService`) are always provided
  by the requesting context. `IdentificationBudget` is the insects context's choice: today's `noOp()` stays its
  default, so insects core tests write no usage rows. Choosing the real budget later is a one-line change there.
- **All `*-core` modules are in scope**, accounts and naturalists included. `AccountCommandImpl` carries `@Transactional`
  on its multi-entity writes and is a direct target of the reachability check.
- **External contexts keep their public API.** `InsectsTestContext.create(...)` and its typed accessors stay as a
  thin layer over the shared wiring, so console `@TestConfiguration` classes do not change. Those contexts keep
  returning JDK-proxied ports as today, and only the extension path uses subclass proxies, so Spring never
  receives a ByteBuddy subclass to proxy again.
- **The reachability check knows one advice annotation: `jakarta.transaction.Transactional`.** The domain and the
  kernel `Transaction` base use only that one (verified 2026-11-07; `AccountCommandImpl` and the console's
  `RegistrationTransaction` both import it). A violation throws when the object is first built, naming the class
  and method. Spring's `org.springframework.transaction.annotation.Transactional` should not appear in a core
  module: it suggests adapter concerns leaking into the domain. It may be legitimate where one entity decomposes
  into several schema objects, so the builder reports it as a warning naming the class rather than failing.
  Nothing in the codebase uses it today.
- **Trace boundaries.** Any object the test receives directly is wrapped. A wrapper resets the count only when
  the test frame is the direct caller, as `SelectTrace` does today. A loop inside the unit under test is
  therefore still caught, and the gate is not weakened. `SelectTrace.isWrapperFrame` must learn to recognise the
  subclass proxies; its counting rules do not change.

## Decided: enforcement comes after the refactor

No new gate in this effort. Do the refactor first, then survey the core tests that still build implementations or
repository mocks by hand (`new *Impl(`, `new *RepositoryMock(`). Those sites are easy to detect. Constructor
null-rejection tests are the expected exception. Whether the survey turns into a `com.naturalist.Enforce*` recipe
is decided from what it finds.

A test that builds an implementation equivalent to what its context would build is not doing anything unique.
Converting it is part of the refactor, not a finding. Only the hand-built cases left after the refactor, the
ones that build something the context would not, are signals worth investigating.

## Left to the builder

- The API shape for asking (`nte.get(...)`) and for handing a fake to a context.
- How the knowledge is shared between a package's internal and external context without a Maven cycle.
- How the extension maps a package to its context (e.g. `ServiceLoader` registration, internal over external).
- Rollout order: insects first (the reference domain), then the other domains, deleting hand wiring as each lands.
