# RDBMS mapper-select fan-out gate — design

**Date:** 2026-08-29
**Status:** approved, ready for implementation plan
**Module:** `kernels/persistence-test` (package `com.naturalist.persistence.test`)
**Sibling of:** the in-memory N+1 gate in
`kernels/framework-test/.../com/naturalist/test/query/nofanout`

## Problem

The in-memory no-fan-out gate (`com.naturalist.test.query.nofanout`) fails the suite
when a single head-of-DAG `*QueryImpl` invocation resolves the same repository select
more than once. Now that every domain has a real Postgres + MyBatis `*Rdbms` adapter,
the same defect can hide one layer down: **a single repository method that fires the
same MyBatis mapper `@Select` more than once** — e.g. a `doGetByNameSet` that loops
`mapper.selectById(id)` per element instead of calling the batched
`mapper.selectByIdSet(ids)`. This passes on seed data and becomes N SQL round-trips in
production — the exact "fan-out must batch" invariant `domains/CLAUDE.md` forbids in
prose, made executable at the RDBMS boundary.

This gate makes that prose an executable invariant for the RDBMS adapters, mirroring
the in-memory gate. It is **test-only** and never on a production classpath.

## What counts as a violation

- **Head** = a repository method invocation. In the RDBMS `*IT` suites the behavioral
  contract exercises the repository directly (no `*QueryImpl` on the stack), so the head
  is the outermost public method on an `AbstractEntityRepository` subtype — the inherited
  `final` methods (`getByName`, `getByNameSet`, `getPage`, `insert`, `update`, `save`)
  and the domain-specific public methods declared on the concrete `*Rdbms` subclass
  (e.g. `findByCounterSince`).
- **Select** = one execution of a MyBatis mapper `@Select`, keyed by
  `MappedStatement.getId()` (e.g. `com.naturalist.usage.UsageEventMapper.selectById`).
- **Violation** = within one head invocation, the same mapped-statement id executes
  more than once, and no `@AllowRepeatedSelect` on the test method covers it.

Counting is **per outermost repository-method INVOCATION**, folded into the per-test
tally via `Math.max` at head exit — identical to the in-memory recorder. So a test that
calls one repository method N times (each doing one select) reads as count 1; only a
*single* method invocation looping a select is an N+1. Two *different* statements in one
method (e.g. `doGetPage` calling `selectPage` then `countInWindow`, each once) are two
distinct selects at count 1 — correctly not flagged.

## Why the mechanism differs from the in-memory gate

MyBatis mapper methods run through a JDK dynamic proxy (`MapperProxy` → `Executor.query`),
which AspectJ `execution()` cannot weave the way it weaves the concrete in-memory
`*QueryImpl` / `*Repository` classes. So the gate is a **hybrid**:

- **Head marker = AspectJ** (reuses the load-time-weaving infra already wired in the root
  `pom.xml` surefire/failsafe `argLine`). A `@Around` on
  `execution(public * com.naturalist.data.AbstractEntityRepository+.*(..))` pushes/pops
  the repository-method head. `AbstractEntityRepository` must itself be woven because the
  inherited `final` method bodies live there; the `*Rdbms` subclasses are woven for their
  own public methods. Scoping to `*Rdbms` alone would miss `getByNameSet` — precisely
  where a batched-vs-looped fan-out hides.
- **Select feeder = MyBatis `Interceptor`**. A `@Intercepts` plugin on
  `Executor.query(MappedStatement, Object, RowBounds, ResultHandler)` records one select
  against the current head, keyed by `MappedStatement.getId()`. Only fires on selects —
  inserts/updates route through `Executor.update` and are excluded for free. Proxy-proof
  and idiomatic; no weaving of the persistence layer's proxies required.

## Components

All new code lives in `kernels/persistence-test`, package
`com.naturalist.persistence.test`.

### New

1. **`MapperSelectRecorder`** — ThreadLocal head/select tally, a structural parallel of
   `SelectCountRecorder`: `arm` / `disarm` / `enterRepository` / `exitRepository` /
   `recordSelect(statementId)` / `snapshot()`. Per-outermost-invocation counting folded
   via `Math.max` at head exit. `enterRepository`/`exitRepository` maintain the head
   stack regardless of `armed` (self-clearing, cheap); only `recordSelect` checks `armed`.
   **A separate ThreadLocal from the in-memory `SelectCountRecorder`, by design** — the
   existing `*Repository+` select aspect also weaves the `*Rdbms` classes but is never
   armed in `*IT` runs, so keeping recorders separate prevents any cross-talk or
   spurious self-select counts.

2. **`RepositoryHeadAspect`** — the `@Around` head marker; feeds
   `enterRepository`/`exitRepository`. Never evaluates, never throws (mirrors
   `SelectCountAspect`'s dumb-feeder role).

3. **`MapperSelectInterceptor`** — the MyBatis `@Intercepts` plugin on `Executor.query`;
   calls `MapperSelectRecorder.recordSelect(ms.getId())` then `invocation.proceed()`.

4. **`META-INF/aop.xml`** (in `persistence-test/src/main/resources`) — declares
   `RepositoryHeadAspect` with `<include within>` for `com.naturalist.data.AbstractEntityRepository`,
   `com.naturalist..*Rdbms`, and the aspect class itself. AspectJ aggregates every
   `META-INF/aop.xml` on the classpath, so this composes with framework-test's existing
   aop.xml.

### Reused from `framework-test` (already `public`, stable)

- `SelectGate.evaluate(snapshot, allowlist)` — the pure, stateless rule (count > 1 and
  not allow-listed → throw, aggregating all violations into one message).
- `@AllowRepeatedSelect(query = …, select = …)` — per-site escape hatch, matched by exact
  FQN, `.`-suffix, or simple name. `query` matches the repository-method head FQN;
  `select` matches the mapped-statement id.
- `RepeatedSelectException` — `AssertionError` subtype that fails the provoking test.

`persistence-test` already depends on `framework-test`, so no new module dependency.

### Wiring into `RdbmsTestExtension`

- **Static init:** `FACTORY.getConfiguration().addInterceptor(new MapperSelectInterceptor())`.
  Keeps the gate test-only — **no change to production `MyBatisSupport`**.
- **`beforeEach`:** `MapperSelectRecorder.arm()` alongside the existing session open.
- **`afterEach`:** read `@AllowRepeatedSelect` from `context.getRequiredTestMethod()`,
  call `SelectGate.evaluate(recorder.snapshot(), allowlist)`, then `disarm()` in a
  `finally`. Ordered so the gate throws before the rollback/close teardown, but the
  recorder is always disarmed even when the gate throws.

Any RDBMS IT that registers `@RegisterExtension RdbmsTestExtension` (all of them do) is
gated automatically — nothing to call, exactly like the in-memory side.

## Weaving footprint & interaction with the existing gate

- Weaving `AbstractEntityRepository` at test time adds the `@Around` head advice to every
  repository call in every test across the reactor. This is consistent with the accepted
  footprint of the existing gate, which already weaves `AbstractEntityQuery`, all
  `*QueryImpl`, and all `*Repository*`. Weaving only happens under the `-javaagent` LTW
  path (test/IT), never in production.
- The existing `SelectCountAspect.beforeSelect` (`*Repository+` selects) does fire on the
  `*Rdbms` classes during ITs, but writes to framework-test's `SelectCountRecorder`, which
  is never armed in an RDBMS IT (only `RdbmsTestExtension` arms *this* gate's separate
  recorder). No cross-talk.
- In non-armed contexts (in-memory tests), `RepositoryHeadAspect` still pushes/pops the
  head stack, but `recordSelect` is never invoked — there is no MyBatis `Executor` and
  hence no `MapperSelectInterceptor` in an in-memory test — and the gate is never
  evaluated (only `RdbmsTestExtension` evaluates). Harmless.

## Testing (respects "no test infra for test infra")

- **`MapperSelectRecorderTest`** — direct unit test (stateful/lifecycle carve-out):
  per-invocation `Math.max` folding, arm/disarm reset, per-invocation isolation.
- **`MapperSelectInterceptorTest`** — construct a MyBatis `Invocation` with a stub
  `MappedStatement` (id set) and a stub `Executor` target; assert it records that id and
  calls `proceed()`. Proves id-extraction and delegation with no DB.
- **`RepositoryHeadWeavingTest`** — a minimal in-test `…RepositoryRdbms extends
  AbstractEntityRepository` whose `do*` methods call `MapperSelectRecorder.recordSelect(...)`
  directly (standing in for the interceptor). Assert: (a) the aspect wove the head so a
  looped select tallies 2 under the repo-method head and `SelectGate` throws; (b) a
  batched call stays at 1 and passes; (c) `@AllowRepeatedSelect` suppresses. Proves
  weaving + head scoping + gate integration with no DB.
- **Real end-to-end** (interceptor + aspect + Postgres) is proven transitively by the
  actual domain `*IT` suites, which need the standing seeded Postgres.

## Verification caveat

`mvn verify` on the rdbms modules runs the `*IT` suites via failsafe, which need a
standing Postgres seeded by `apps/test-db-seeder`. The three no-DB proof tests compile
and run without a database. Running the full domain ITs to confirm the gate stays green
(or surfaces a real pre-existing fan-out) requires that DB to be up; the implementer will
attempt it and report exactly what ran.

## Docs

- New `com.naturalist.persistence.test` package `CLAUDE.md`, mirroring the nofanout
  package's, including the "⛔ do not weaken this gate" root non-negotiable.
- Targeted update to `kernels/CLAUDE.md` (persistence-test section) noting the sibling gate.

## Out of scope

- No change to production `MyBatisSupport` or any `*Rdbms` adapter (unless the gate
  surfaces a real fan-out, which is then fixed at the source — never by weakening the gate).
- No static (OpenRewrite) backstop for mapper selects in this pass; the in-memory static
  gate `NoSelectInIteration` already flags loop/stream fan-out of repository/query selects
  lexically, and the mapper layer is thin delegation. Revisit only if a real miss appears.
