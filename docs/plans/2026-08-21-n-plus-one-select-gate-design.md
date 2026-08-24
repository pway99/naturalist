# N+1 Select Gate — Design

**Date:** 2026-08-21
**Status:** SHIPPED & ARMED (2026-08-23). Aspect + recorder + gate committed; wired into
`NaturalistTestExtension` (arms in `beforeEach`, evaluates in `afterEach`). Recorder gates
**per outermost query invocation** (a fix over the original per-FQN counting, which
false-flagged tests that call one query N times). Complements the static `NoSelectInIteration`
OpenRewrite recipe — the runtime gate catches composed/cross-method within-head fan-outs the
static lexical recipe cannot see, and vice-versa. The armed gate deliberately reds the genuine
N+1s it finds; those are tracked for a fixing pass (see the followups doc).
**Goal:** A build-time test gate that fails the suite when a head-of-DAG query fans out
into repeated repository selects — the N+1 pattern we hand-fixed across plant and insect
queries in a prior session. The existing convention ("Fan-out must batch — never
per-element in a loop", [domains/CLAUDE.md](../../domains/CLAUDE.md)) is prose enforced by
reviewer memory; this makes it an executable invariant that trips on the seed data during
the build, where those N+1s already manifest ("passes on seed data and fails in prod" is
the failure mode we're inverting).

**Scope of this pass:** build-time gate only. A runtime Micrometer metric / warning log in
the deployed app is explicitly deferred — same counting idea, a later consumer.

## Constraints that shaped the design

- **No Spring in core or the kernel.** The instrument is AspectJ (load-time weaving), not
  Spring AOP. Spring AOP could not proxy these anyway: repository interfaces are
  package-private and wired by hand through `*TestContext` / `@DomainService` discovery,
  not as proxied beans. The `framework` kernel keeps its current dependency budget
  (Jackson, Commons, Micrometer, JSpecify) untouched.
- **Test-only, never on the production classpath.** `aspectjweaver` lives in test/runtime
  scope of a test-support module; the aspect is applied by a `-javaagent` in surefire, so
  no production artifact carries it.
- **Domain-specific selects must be caught.** The six inherited selects funnel through
  `AbstractEntityRepository`'s `final` methods, but the domain-specific selects
  (`getByParentNames`, `forGenusName`, …) are implemented directly on the concrete
  adapters and route through no shared choke point — and that is exactly where the last
  N+1s lived. A single AspectJ pointcut over any repository subtype catches all of them;
  base-class instrumentation alone would not.

## Mechanism

Two pointcuts feed one ThreadLocal tally; the JUnit extension owns all judgment.

### The aspect — a dumb feeder

A test-only aspect with two pointcuts:

- **Query pointcut** — `execution(* com.naturalist..*QueryImpl.*(..))`. On entry, push onto
  a ThreadLocal **head stack**; on exit, pop. The outermost query (stack depth 0→1) is the
  *head of the DAG*. The stack always self-clears to empty at the outermost exit — bounded,
  can never leak across tests.
- **Repository pointcut** — `execution(* com.naturalist..*Repository+.*(..))`, excluding the
  write methods `insert`, `update`, `save`. On entry it increments the current head's
  `Map<repoSelectFQN, count>` **only when (a) the head stack is non-empty and (b) collection
  is armed** (see below).

The aspect never evaluates the rule and never throws. It only records.

**Two clauses, two reasons:**

- *Stack non-empty* — a repository call made **directly by the test** (seeding in arrange,
  verifying persisted state in assert) happens at head-stack depth 0, with no query on the
  stack, so it is **not counted**. Only selects that occur *because a query was invoked*
  land in the tally. This is the load-bearing exclusion: the repository is legitimately
  used for assertions, and those calls must not pollute the count.
- *Armed* — the aspect accumulates into the tally only when a `NaturalistTestExtension` has
  armed collection for the current test. A test that does not register the extension
  records nothing, so the tally cannot grow unbounded or bleed onto a later test that
  shares a pooled thread.

### The extension — the counter's native home

Rename `NaturalistDatabaseExtension` → `NaturalistTestExtension` (it grows past "database").
It keeps its current job (clear the `NaturalistDatabase` registry before each test) and
gains the counter, using JUnit's native lifecycle handles:

- **`beforeEach`** — reset the ThreadLocal tally, **arm** collection, and load any
  `@AllowRepeatedSelect` entries off the test method into the tally context.
- **`afterEach`** — read the tally, apply the rule, throw `RepeatedSelectException` naming
  **every** offending head + method + count in one message, then **disarm** and clear.

JUnit runs `beforeEach` → test body → `afterEach` on the **same thread** for a given test
(true even under `CONCURRENT` execution), so `afterEach` reads exactly the ThreadLocal the
test's synchronous fan-out wrote. Deferring evaluation to `afterEach` (rather than throwing
inline from the aspect) keeps all judgment in readable Java and lets one failure aggregate
every violation in the test.

### "Which query is under test" dissolves

We do not identify a single query under test. *Every* top-of-stack query invocation —
arrange, act, or assert — is an independently gated head. Nested sub-queries
(`forRankHierarchy` → `speciesQuery.forGenusNames` → repo) roll up to their outer head,
which is the correct N+1 semantics: the whole subtree resolution gets one budget.

## The rule

**Rule 1 (strict):** within a single head-of-DAG query, any repository select method
invoked more than once is a violation.

This directly encodes "fan-out must batch": a second call to the same select in one head
means the code should have called the batched sibling (`getByEntityNameSet` /
`getByParentNames` / `findByNameSet`) once instead.

**Escape hatch:** `@AllowRepeatedSelect(query = "<HeadQueryFQN or simple>", select =
"<repoMethodFQN or simple>")` on a test method (repeatable) whitelists a known-legitimate
repeat. The extension reads it in `beforeEach`; the aspect consults it before counting a
repeat as a violation. Every use is a visible, greppable admission in the test source.

**Failure message shape:**

```
RepeatedSelectException: N+1 select detected
  head  InsectImageQueryImpl.forRankHierarchy
  select InsectImageRepositoryMock.getByName  called 14×  (expected ≤ 1)
```

## ThreadLocal safety

The user has been burned by ThreadLocal under Maven/parallel test execution. This design is
safe by construction, and the current environment adds margin:

- **Environment today:** surefire is stock — no `parallel`, no `forkCount`, no
  `reuseForks`, no `junit-platform.properties`, no `@Execution`. Tests run single-threaded
  in one JVM. No core code does cross-thread fan-out (no `parallelStream`,
  `CompletableFuture`, `new Thread`, `commonPool`, `@Async`), and resilience is `noOp()`
  under test (inline, no bulkhead pool). The head-query → repo-select chain is fully
  synchronous on one thread.
- **If parallel-by-test is enabled later** — each worker thread has its own isolated head
  stack and tally; ThreadLocal is the correct tool for that isolation, not a hazard.
- **If `forkCount > 1`** — separate JVMs, separate statics, nothing shared.
- **Pooled-thread reuse** — the head stack self-clears at outermost exit, and the tally is
  reset in `beforeEach` and disarmed in `afterEach`, so a reused thread always starts clean.
- **Residual limitation (documented, fails safe):** a select on a thread *spawned inside a
  query* would not see the head stack and would be **under-counted, never miscounted** — the
  gate can miss an N+1 there but can never false-fail. No such code exists today; if it ever
  arises we add context propagation then (YAGNI until).

## Placement & wiring

- **New test-support module** (or an addition to `kernels/framework-test`, its natural home
  — it already owns the extension and is every test module's dependency). Holds the aspect,
  `NaturalistTestExtension`, `@AllowRepeatedSelect`, and `RepeatedSelectException`.
  `aspectjweaver` is a test/runtime-scope dependency there only.
- **`META-INF/aop.xml`** declares the aspect and the weave scope (`com.naturalist..*`).
- **Root `pom.xml` surefire** gains `-javaagent:<aspectjweaver>` once (via
  `dependency:properties` or a pinned path), so load-time weaving applies across every
  module's test run without per-module compile-time weaving config.

## Coverage & non-goals

- **In scope:** in-query fan-out (rule 1) in the ~49 test classes that register
  `NaturalistTestExtension` — precisely the repository/query tests where this fan-out lives.
- **Out of scope (this pass):**
  - Controller-loop N+1 (a controller calling one query per child across a request). That is
    a per-request-scope rule over query-method counts, not a per-head rule over select
    counts; the WebMvc `@SpringBootTest` tests do not register the extension. Add later by
    registering the extension there and adding a second rule.
  - Runtime production metric/log.

## Proving the gate bites

Rather than adding "test infra for test infra" (unit tests around the aspect internals), we
prove the instrument end-to-end with a deliberate fixture:

- A throwaway query that loops `getByName` per element → a test asserting `afterEach` raises
  `RepeatedSelectException`.
- Its batched sibling (one `getByEntityNameSet`) → a test asserting the gate stays silent.
- One `@AllowRepeatedSelect` test → asserting a whitelisted repeat does not fail.

If those three behave, the gate works in the shape that matters, and every real
repository/query test inherits it for free.

## Open questions / decisions to confirm in the plan

1. **Module home:** fold into `kernels/framework-test`, or a dedicated
   `kernels/select-count-test` module to keep aspectjweaver out of the universal test dep?
2. **`aop.xml` weave scope:** weave all of `com.naturalist..*` (simplest) vs restrict to
   `*QueryImpl` + `*Repository+` to minimise weave surface and test-startup cost.
3. **Rename timing:** the `NaturalistDatabaseExtension` → `NaturalistTestExtension` rename
   touches 49 files; land it as its own commit before the counter, or together.
