# Dynamic Test Contexts — Implementation Plan (framework + library + insects pilot)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A core test asks `nte.get(SomeType.class)` and gets a fully wired, fully traced object graph assembled by
reflection under the guidance of each package's test context; insects and library are converted as the pilot.

**Architecture:** `kernels/framework-test` gains `com.naturalist.test.context`: a `TestContext` base, an
`ObjectGraph` that resolves types by the design's resolution order, a package-only scan for `@DomainService` /
`@MockDomainService`, and tracing of everything it builds (JDK proxies for ports, ByteBuddy subclasses for concrete
classes). Each package's one context lives in `<domain>-core/src/test` and ships to other modules in the core's
test-jar; `<domain>-test-context` keeps its public API as a thin facade over the graph for Spring adapter tests.

**Tech Stack:** Java 25, JUnit 5, AssertJ, Maven reactor, ByteBuddy (new), Kaname Engine atoms, OpenRewrite gate.

**Spec:** `docs/plans/2026-11-07-dynamic-test-context-design.md`

## Global Constraints

- No Mockito, no generated mocks, no stubbed return values. ByteBuddy is used only to build tracing subclasses.
- ByteBuddy's version lives only in root `pom.xml` `<properties>` as `byte-buddy.version`, pinned to the version
  `spring-boot-dependencies:${spring-boot.version}` manages, marked `<!-- Boot-managed -->`.
- Never weaken the N+1 gate: nothing in this plan may stop a `TestEntitySource` read from being counted.
- `kernels/framework-test` carries "no test infra for test infra": no new unit tests or fixtures for the graph
  builder. The converted insects and library suites prove it; manual smoke steps cover failure paths.
- A context scans only its own Java package (no subpackages, no classpath-wide scan). Another package's types come
  from that package's context.
- The only advice annotation is `jakarta.transaction.Transactional`. Spring's
  `org.springframework.transaction.annotation.Transactional` in core is a warning, never a failure.
- Java writes go through Kaname atoms wherever an atom exists (hooks enforce it). Markdown and new files may be
  written directly.
- Stage only. Nothing is committed until Pat says "commit".

## Review Focus

1. **Asking for a type whose package has no registered context** must fail naming the package and the chain of
   requesters, not with a `NullPointerException`. Task 1 smoke step.
2. **Identity.** A port fetched with `nte.get` and the same port reached through an aggregator accessor
   (`insectQuery.species()`) must be the same instance. Pinned by `InsectQueryImplTest.accessors_*` in Task 2.
3. **A fake handed over after its type was built** must throw, naming the type; silently ignoring it would make a
   test pass against the wrong object. Task 1 smoke step.
4. **A test looping over a concrete transaction** must not false-fail the gate, while a loop inside the unit under
   test still fails. Task 3 test plus the existing `SelectTraceTest`.
5. **Package-private classes split across jars** (a mock in `insects-repository-test`, impls in `insects-core`,
   same package) must build and proxy. Exercised by every converted test in Task 2.

---

### Task 1: The graph builder, with library as its first consumer

**Files:**
- Create in `kernels/framework-test/src/main/java/com/naturalist/test/context/`: `TestContext.java`,
  `Provisions.java`, `ObjectGraph.java`, `PackageScan.java`, `TestGraphException.java`, `RegisteredFake.java`,
  `Wired.java`, `Fake.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/NaturalistTestExtension.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/test/query/nofanout/SelectTrace.java` (`invoke`)
- Create: `domains/library/library-core/src/test/java/com/naturalist/library/LibraryTestContextInternal.java`
- Create: `domains/library/library-core/src/test/resources/META-INF/services/com.naturalist.test.context.TestContext`
- Modify: `domains/library/library-core/pom.xml` (test-jar)
- Modify: `domains/library/library-core/src/test/java/com/naturalist/library/CitationAttributionTransactionTest.java`,
  `CitationQueryImplTest.java`

**Interfaces:**
- Produces:
  - `public abstract class TestContext` with `protected TestContext()`, `protected void provisions(Provisions p)`
    (default empty), `public final <T> void fake(Class<T> type, T fake)`. Its package is
    `getClass().getPackageName()`. Discovered with `ServiceLoader.load(TestContext.class)`; needs a public no-arg
    constructor.
  - `public final class Provisions` with `public <T> void provide(Class<T> type, Function<ObjectGraph, T> factory)`.
  - `public final class ObjectGraph` with `public static ObjectGraph create(NaturalistDatabase db)`,
    `public <T> T get(Class<T> type)`, `public <C extends TestContext> C context(Class<C> type)`,
    `public List<RegisteredFake> fakes()`.
  - `public record RegisteredFake(String context, Class<?> type, Class<?> fakeClass)`.
  - `public final class TestGraphException extends RuntimeException`.
  - `@Wired` and `@Fake`, two runtime field annotations in `com.naturalist.test.context`.
  - `NaturalistTestExtension`: `public <T> T get(Class<T> type)`,
    `public <C extends TestContext> C context(Class<C> type)`. The graph is created lazily on first use and
    dropped in `beforeEach` alongside `clear()`.

- [ ] **Step 1: Implement `PackageScan.candidates(String packageName): List<Class<?>>`.** Enumerate
  `ClassLoader.getResources(packageName.replace('.', '/'))`. For a `file:` URL, list the `*.class` files directly in
  that directory; for a `jar:` URL, list entries whose name has the package prefix and no further `/`. Load each
  with `Class.forName(name, false, loader)`. Keep classes annotated `com.naturalist.infrastructure.DomainService` or
  `com.naturalist.data.MockDomainService`. Cache per package in a static `ConcurrentHashMap`.

- [ ] **Step 2: Implement `ObjectGraph.get` with this resolution order** for type `T` requested by context `R`.
  The test itself has no `R`.
  1. A fake for `T` → that fake.
  2. `R`'s provisions contain `T` → build once, cache by `(R, T)`.
  3. The owner `O` is the registered context whose package equals `T.getPackageName()`. No owner →
     `TestGraphException("no test context for package <p>; <T> requested by <chain>")`. Two contexts registered
     for one package → `TestGraphException` naming both.
  4. `O`'s provisions contain `T` → build once, cache by `(O, T)`.
  5. `T` is concrete and in `O`'s candidates → build it. `T` is an interface or abstract → exactly one candidate
     in `O`'s package assignable to `T`, else `TestGraphException` naming `O` and the candidates found.
  6. Build by the class's single declared constructor (`setAccessible(true)`). With none or several, use its
     single static method named `create` or `of` that returns the class. Otherwise throw
     `TestGraphException("<C>: no single constructor; provide it in <O>")`. Parameters are resolved with `O` as
     the requester. `NaturalistDatabase` always resolves to the graph's database. A cycle throws, naming the chain.
  7. One instance per class per graph: cache the built object by class, so a diamond builds it once.
  8. Wrapping: when `T` is a non-sealed `com.naturalist.` interface, hand out `SelectTrace.traced(T, obj)` and
     cache that proxy by `T`, so every requester of `T` gets the same instance. Concrete classes are handed out
     plain until Task 3.

  `TestContext.fake` registers into the graph that owns the context instance. A fake for a type already built
  throws `TestGraphException("<T> was already built; hand the fake over before the first get")`. Every fake is
  recorded as a `RegisteredFake`.

- [ ] **Step 3: Extension.** Add `get` and `context`, delegating to a lazily created `ObjectGraph.create(this)`.
  In `afterEach`, before `SelectTrace.evaluate`, append one line per `graph.fakes()` entry to
  `target/test-context-fakes.txt`, creating the file if needed:
  `<TestClass>#<method>\t<context simple name>\t<type FQN>\t<fake class FQN>`.

- [ ] **Step 3b: Field injection, in `beforeEach` after `clear()` and dropping the graph.** Read
  `context.getRequiredTestInstance()` and walk its class and superclasses.
  1. For every `@Fake` field, read its value, which a field initialiser sets. Hand it to the context registered for
     the test class's own package, as a fake for the field's declared type. A `null` value throws
     `TestGraphException` naming the field.
  2. For every `@Wired` field, set it to `graph.get(field.getType())`. A `final` or `static` field throws
     `TestGraphException` naming the field.

  JUnit runs extension `beforeEach` callbacks before the test's own `@BeforeEach` methods. Those methods can
  therefore already use the fields.

- [ ] **Step 4: `SelectTrace.invoke` must not re-wrap an object that is already traced.** If the result is
  already a proxy (`Proxy.isProxyClass(result.getClass())`), return it unchanged. Otherwise wrap as today. This
  keeps identity for graph-built ports.

- [ ] **Step 5: Library context.** `public class LibraryTestContextInternal extends TestContext` has no
  provisions: every library class has one constructor, and `CladeQueryImpl` takes no arguments. Register it in
  the services file. Add a `maven-jar-plugin` `test-jar` execution to `library-core/pom.xml`. Plugin versions come
  from root `pluginManagement`; add a property there if none exists.

- [ ] **Step 6: Convert the two library tests.** Replace their hand-built repositories, queries, transaction and
  any setup method that only builds objects with `@Wired` fields: `@Wired CitationQuery citationQuery;`,
  `@Wired CitationAttributionTransaction transaction;` and so on. Assertions do not change.

- [ ] **Step 7: Run.**

  Run: `mvn -q -pl domains/library/library-core -am verify -Dtest='CitationAttributionTransactionTest,CitationQueryImplTest' -Dsurefire.failIfNoSpecifiedTests=false`
  Expected: BUILD SUCCESS, both classes green with unchanged assertion counts.

- [ ] **Step 8: Smoke the failure paths by hand, then revert.** In `CitationQueryImplTest`:
  - add `nte.get(com.naturalist.ddd.EntityName.class)`, a kernel type whose package has no context. Expected:
    `TestGraphException` naming package `com.naturalist.ddd`.
  - call `nte.get(CitationQuery.class)`, then `nte.context(LibraryTestContextInternal.class).fake(CitationQuery.class, …)`.
    Expected: the "already built" `TestGraphException`.

  Revert both edits.

- [ ] **Step 9: Stage.**

### Task 2: Insects contexts and the tests that used the old internal context

**Files:**
- Rewrite: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsTestContextInternal.java`
- Create: `domains/insects/insects-core/src/test/java/com/naturalist/insects/lifestage/InsectLifeStageTestContextInternal.java`
- Create: `domains/insects/insects-core/src/test/resources/META-INF/services/com.naturalist.test.context.TestContext`
- Modify: `domains/insects/insects-core/pom.xml`: a test-jar execution, plus a test-scope dependency on
  `library-core` with `<type>test-jar</type>`
- Modify in `insects-core/src/test/java/com/naturalist/insects/`: `InsectQueryImplTest`,
  `ImageQueryImplHierarchyTest`, `InsectCatalogIdentificationTransactionTest`, `InsectPhotoCommandImplTest`,
  `InsectSightingCommandImplTest`, `InsectViewQueryImplTest`, `InsectIdentificationCommandImplTest` (only its
  uses of the old context; its `new InsectIdentificationCommandImpl(...)` sites are Task 5)

**Interfaces:**
- Consumes: Task 1's `TestContext`, `Provisions`, `nte.get`, `nte.context(...).fake(...)`.
- Produces: `public class InsectsTestContextInternal extends TestContext` and
  `public class InsectLifeStageTestContextInternal extends TestContext`. Their old static `create(...)` methods and
  accessors are removed.

- [ ] **Step 1: Write `InsectsTestContextInternal.provisions`.** It provides exactly these:
  - `ImageStore` → `new FilesystemImageStore(Files.createTempDirectory("insects-images"), Resilience.noOp())`
  - `Resilience` → `Resilience.noOp()`
  - `IdentificationBudget` → `IdentificationBudget.noOp()`
  - `VisionService` → `new NoOpVisionService()`
  - `TextGenerationService` → `new NoOpTextGenerationService()`
  - `ExternalAuthority` → the existing no-op authority, kept as a private constant
  - `FeatureSearch` → `new InMemoryFeatureSearch<>(() -> g.get(InsectQuery.class).features().corpus().map(f -> new FeatureCorpus.Indexed<>(f.id(), f.value())))`

  The hand-written `noOpLifeStageQuery` and the repository wiring are deleted. `InsectLifeStageTestContextInternal`
  has no provisions. Register both contexts in the services file.

- [ ] **Step 2: Convert `InsectQueryImplTest`.** Its 24-field wiring block becomes `@Wired` fields, and the
  anonymous library and life-stage stubs are deleted:
  - `@Wired InsectQuery insectQuery;`
  - `@Wired InsectQuery.SpeciesQuery speciesQuery;`, and the same for the image, family, genus, functional-role,
    order and observation queries
  - `@Wired InsectCitationQueryImpl citationQuery;`, `@Wired InsectFeatureQueryImpl featureQuery;`,
    `@Wired InsectLifeStageQuery insectLifeStageQuery;`

  Every existing test keeps its assertions. The
  `accessors_*` tests are now the identity pin from Review Focus 2.

- [ ] **Step 3: Convert the other six tests.** Replace `InsectsTestContextInternal.create(nte)`, its accessors and
  any setup method that only builds objects with `@Wired` fields. `InsectPhotoCommandImplTest`'s
  `create(nte, store)` becomes a `@Fake ImageStore store = ...` field. `context.entityCommands()` becomes
  `@Wired InsectEntityCommands entityCommands;`.

- [ ] **Step 4: Run.**

  Run: `mvn -q -pl domains/insects/insects-core -am verify`
  Expected: BUILD SUCCESS. Test counts per class are unchanged from before the task: record them first with the
  same command on the unmodified tree. `target/test-context-fakes.txt` lists the `ImageStore` fake.

- [ ] **Step 5: Stage.**

### Task 3: Trace concrete classes and check proxy reachability

**Files:**
- Modify: root `pom.xml`: the `byte-buddy.version` property and its `dependencyManagement` entry
- Modify: `kernels/framework-test/pom.xml`: a compile dependency on `net.bytebuddy:byte-buddy`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/test/query/nofanout/SelectTrace.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/test/context/ObjectGraph.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/test/query/nofanout/CLAUDE.md`
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCatalogIdentificationTransactionTest.java`

**Interfaces:**
- Produces in `SelectTrace`:
  - `public interface Traced {}`, a marker every generated subclass implements
  - `public static void enterTraced()`, which runs `nextInvocation()` when the scope is armed and `calledByTest()`
  - `public static final class TraceAdvice` with `@Advice.OnMethodEnter static void enter()` calling `enterTraced()`
  - `isWrapperFrame` also returns true for `Traced.class.isAssignableFrom(declaring)`

- [ ] **Step 1: Write the failing test** `InsectCatalogIdentificationTransactionTest.twoCallsFromTheTestAreTwoCounts`.
  Take the transaction from `nte.get(InsectCatalogIdentificationTransaction.class)` and call its public entry point
  in a `for` loop over two different valid inputs, built as the class's existing tests build one. Assert the
  second call's result, as the existing tests assert one.

- [ ] **Step 2: Run it and see it fail.**

  Run: `mvn -q -pl domains/insects/insects-core -am verify -Dtest=InsectCatalogIdentificationTransactionTest -Dsurefire.failIfNoSpecifiedTests=false`
  Expected: FAIL with `RepeatedSelectException`, because the plain concrete object gives the gate no call
  boundary.

- [ ] **Step 3: Add ByteBuddy.** Read the version from
  `~/.m2/repository/org/springframework/boot/spring-boot-dependencies/<spring-boot.version>/spring-boot-dependencies-<spring-boot.version>.pom`
  and pin it as `byte-buddy.version` with `<!-- Boot-managed -->`.

- [ ] **Step 4: Build concrete classes as tracing subclasses in `ObjectGraph`.** For a non-final concrete class `C`:
  - Generate `new ByteBuddy().subclass(C, ConstructorStrategy.Default.IMITATE_SUPER_CLASS).implement(SelectTrace.Traced.class)`.
  - Intercept every non-private, non-static, non-final instance method declared by `C` or a `com.naturalist.`
    superclass with `Advice.to(SelectTrace.TraceAdvice.class).wrap(SuperMethodCall.INSTANCE)`.
  - Load it with `ClassLoadingStrategy.UsingLookup.of(MethodHandles.privateLookupIn(C, MethodHandles.lookup()))`.
  - Invoke the imitated constructor with the resolved arguments. The object is constructed once, as the subclass.

  Cache the generated class per `C` statically. A `final` class is built plain and untraced.

- [ ] **Step 5: The reachability check, run before building any class `C`.** Walk `C` and its `com.naturalist.`
  superclasses. Match annotations by type name, so no Spring dependency is needed.
  - For every method annotated `jakarta.transaction.Transactional`, or every method when the class itself is
    annotated: `C` must not be final, and the method must not be final, private or static. Otherwise throw
    `TestGraphException("<C>#<method> carries @jakarta.transaction.Transactional but a proxy cannot reach it: <reason>")`.
  - For `org.springframework.transaction.annotation.Transactional`, print one line to `System.err`:
    `WARN test-context: Spring @Transactional on <C>#<method>; core uses jakarta.transaction.Transactional`.

- [ ] **Step 6: Run the test again and see it pass.** Same command as Step 2. Expected: PASS.

- [ ] **Step 7: Run the gate's own suite.**

  Run: `mvn -q -pl kernels/framework-test -am verify -Dtest=SelectTraceTest -Dsurefire.failIfNoSpecifiedTests=false`
  Expected: all pass, with the same count as before.

- [ ] **Step 8: Smoke the reachability check by hand, then revert.** Make `Transaction.execute` in
  `kernels/framework` `final` and rerun Step 2's command. Expected: `TestGraphException` naming
  `InsectCatalogIdentificationTransaction#execute`. Revert.

- [ ] **Step 9: Document it.** In the nofanout `CLAUDE.md`, replace the section "Test contexts wrap what they hand
  out" with three points:
  - the graph wraps everything it builds: ports with JDK proxies, non-final concrete classes as `Traced` subclasses
  - `final` classes are not traced
  - a wrapper resets the count only when the test is the direct caller

- [ ] **Step 10: Stage.**

### Task 4: External contexts become facades over the graph

**Files:**
- Modify: `domains/insects/insects-test-context/pom.xml`: depend on `insects-core` and `library-core` with
  `<type>test-jar</type>`, compile scope
- Modify: `domains/library/library-test-context/pom.xml`: depend on `library-core` with `<type>test-jar</type>`
- Rewrite internals of: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`,
  `.../insects/lifestage/InsectLifeStageTestContext.java`,
  `domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java`

**Interfaces:**
- Consumes: `ObjectGraph.create`, `ObjectGraph.get`, `ObjectGraph.context(...).fake(...)`.
- Produces: unchanged public signatures. Every `create(...)` overload and every accessor keeps its name, parameters
  and return type.

- [ ] **Step 1: Re-implement the facades.** Each `create(...)` builds `ObjectGraph.create(db)` and hands each
  overload argument to `InsectsTestContextInternal` as a fake for its type: `ImageStore`, `Resilience`,
  `IdentificationBudget`, `VisionService`, `ExternalAuthority`. Each accessor returns `graph.get(<type>)`. The
  `noOpAuthority()` static stays. `LibraryTestContext` and `InsectLifeStageTestContext` do the same with no fakes.

- [ ] **Step 2: Run the consumers.** These are the Spring console tests, which this effort does not change.

  Run: `mvn -q -pl domains/insects/insects-console,domains/plants/plants-console,domains/soil/soil-console -am verify`
  Expected: BUILD SUCCESS with test counts unchanged from before the task.

- [ ] **Step 3: Stage.**

### Task 5: Convert the remaining equivalent hand-wiring and survey what is left

**Files:**
- Modify: any test in `domains/insects/insects-core/src/test` or `domains/library/library-core/src/test` that
  still contains `new <X>Impl(`, `new <X>RepositoryMock(`, `new <X>Transaction(` or `new <X>Resolver(`
- Create: `docs/notes/2026-11-07-hand-built-test-graphs.md`

- [ ] **Step 1: List the sites.**

  Run: `grep -rnE 'new [A-Z][A-Za-z]*(Impl|RepositoryMock|Transaction|Resolver|Commands)\(' domains/insects/insects-core/src/test domains/library/library-core/src/test`

- [ ] **Step 2: Convert every site that builds what the context would build.** Use `@Wired` fields, plus `@Fake`
  fields when the test substitutes a dependency. Delete setup methods that only build objects. Use
  `nte.context(...).fake(...)` with `nte.get(...)` only where the fake differs per test method. For example, `InsectIdentificationCommandImplTest`'s stub
  vision service becomes `nte.context(InsectsTestContextInternal.class).fake(VisionService.class, stub)` followed by
  `nte.get(InsectCommand.IdentificationCommand.class)`. That is a refactor, not a finding.
  - **Keep:** constructor null-rejection tests. They build with `null` arguments and take the other arguments from
    `nte.get`.
  - **Keep:** a site that builds something no context would. An example is an anonymous subclass overriding a
    method, as in `InsectPhotoCommandImplTest`'s throwing `InsectAddPhotoTransaction`.

- [ ] **Step 3: Write the survey** in `docs/notes/2026-11-07-hand-built-test-graphs.md`. Make one row per kept site,
  with its file and line, what it builds, and why the context cannot build it. End with one paragraph saying
  whether the remaining sites argue for a `com.naturalist.Enforce*` recipe. That decision is Pat's.

- [ ] **Step 4: Run.**

  Run: `mvn -q -pl domains/insects/insects-core,domains/library/library-core -am verify`
  Expected: BUILD SUCCESS. Test counts are unchanged, and no new entries appear in the `@SuppressRepeatedSelect`
  inventory: `grep -rn SuppressRepeatedSelect --include=*.java domains` shows the same two production sites.

- [ ] **Step 5: Stage.**

### Task 6: Docs and the completeness gate

**Files:**
- Modify: `kernels/CLAUDE.md` (framework-test section), `domains/CLAUDE.md` (the TestEntitySource / test-context
  guidance), `docs/plans/README.md` (a row for this plan), `docs/work-tracker.md` (a row pointing at the design)

- [ ] **Step 1: Document the pattern.**
  - The `domains/CLAUDE.md` guidance on test contexts says: a core test declares what it uses as `@Wired`
    fields and its substitutions as `@Fake` fields. `nte.get` and `nte.context(...).fake(...)` are for fakes that
    vary per test method.
  - Each package has one context, in `<domain>-core/src/test`, registered in
    `META-INF/services/com.naturalist.test.context.TestContext` and shipped in the core's test-jar.
  - `<domain>-test-context` is a facade for adapter tests.
  - Domains not yet converted keep their hand-wired contexts until their own rollout plan.

- [ ] **Step 2: Full build.** Run: `mvn verify`. Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Architectural gate.** Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.
  Expected: `BUILD SUCCESS`, no pending fixes or markers.

- [ ] **Step 4: The two greps from root `CLAUDE.md`.** Both must print nothing, and the version grep may print
  nothing outside the root pom:

  ```bash
  git ls-files -co --exclude-standard | xargs grep -nIiE '[a-z0-9]+[-_]api[-_]?key|sk-ant[-]'
  ```

  ```bash
  grep -rn "<version>" --include=pom.xml . | grep -v 1.0.0-SNAPSHOT | grep -v '\${'
  ```

- [ ] **Step 5: Close out.** Stage everything and print the commit commands for Pat. Do not commit.

## Not in this plan

- Rolling out to chemistry, garden, plants, soil, usage, accounts and naturalists. Each domain gets its own short
  plan repeating Tasks 2, 4 and 5 once this pilot lands.
- An enforcement recipe. It is decided by Pat from Task 5's survey.
- Reusing one graph across tests. The graph is per test; package scans and generated classes are cached per JVM.
  Measure before going further.
