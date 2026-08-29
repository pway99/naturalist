# `com.naturalist.persistence.test.nofanout` — the RDBMS mapper-select fan-out gate

A **test-time gate** that fails an RDBMS `*IT` when a single repository method resolves the
same MyBatis mapper `@Select` more than once — the SQL-layer N+1 (`domains/CLAUDE.md`:
"Fan-out must batch — never per-element in a loop"). It is the sibling of the in-memory gate
in `com.naturalist.test.query.nofanout` (framework-test): that one keys on `*QueryImpl` heads
and in-memory repository selects; this one keys on **repository-method heads** and **MyBatis
mapped-statement executions**. **Test-only** — never on a production classpath.

## How it works — hybrid feeder, reused rule

```
RepositoryHeadAspect  ──enter/exit──▶  MapperSelectRecorder  ──snapshot──▶  SelectGate
  (AspectJ LTW head)                     (ThreadLocal, per-invocation)      (framework-test rule)
MapperSelectInterceptor ──recordSelect──▶       ▲                                 ▲
  (MyBatis Executor.query)                RdbmsTestExtension arms (beforeEach) / evaluates (afterEach)
```

- **`RepositoryHeadAspect`** (load-time woven) — `@Around` on every public method of an
  `AbstractEntityRepository` subtype, pushing/popping the repository-method head. Mapper
  methods run through a MyBatis JDK proxy that AspectJ `execution()` cannot weave, so the
  select side is a MyBatis plugin, not an aspect.
- **`MapperSelectInterceptor`** (MyBatis `@Intercepts` on `Executor.query`) — records one
  select against the current head, keyed by `MappedStatement.getId()`. Only selects — writes
  route through `Executor.update`. Registered by `RdbmsTestExtension`, never by production
  `MyBatisSupport`.
- **`MapperSelectRecorder`** (ThreadLocal) — counts selects **per repository-method
  INVOCATION**, folding via `Math.max` at head exit, so a test calling one method N times
  (each one select) reads as 1; only a single invocation looping a select is an N+1. A
  **separate** ThreadLocal from the in-memory recorder, so the in-memory select aspect (which
  also weaves the `*Rdbms` classes but is never armed here) cannot cross-talk.
- **Rule reused from framework-test** — `SelectGate` (count > 1 → throw), `@AllowRepeatedSelect`
  (per-site escape hatch; `query` matches the repository-method head, `select` the statement id),
  `RepeatedSelectException`.

## Using it

- Nothing to call — register `@RegisterExtension RdbmsTestExtension` (every rdbms `*IT` already
  does) and the gate is live.
- **Escape hatch:** `@AllowRepeatedSelect(query = "<repo method>", select = "<mapper statement>")`
  on the specific test method (repeatable), with a one-line comment saying why it cannot batch.
  Match by exact FQN, `.`-suffix, or simple name.

## ⛔ Do NOT weaken this gate to make a test pass

Root-`CLAUDE.md` non-negotiable. A `RepeatedSelectException` reports a real N+1 in the code
under test — fix the fan-out (add/use a batched mapper method: `selectByIdSet`,
`findByNameSet`, …). Do **not** edit the recorder/aspect/interceptor/gate to stop counting,
drop a test's `@RegisterExtension RdbmsTestExtension`, narrow the `aop.xml` weave scope, or
blanket-`@AllowRepeatedSelect`. If you believe the gate is wrong, raise it with the human.

## Constraints / gotchas

- **Never on a production classpath.** `aspectjweaver` is a root test-scope dep; `aspectjrt` is
  compile-scope only for the `@Aspect`. The interceptor is registered solely by
  `RdbmsTestExtension`. If you move/rename the aspect, update `META-INF/aop.xml` and reinstall
  `persistence-test` (downstream rdbms modules weave from the installed jar).
- **Proof tests** (no DB): `MapperSelectRecorderTest` (stateful), `MapperSelectInterceptorTest`
  (id extraction), `RepositoryHeadWeavingTest` (weaving + head scoping). Real end-to-end
  coverage is the domain `*IT` suites against the standing Postgres. Per the framework-test
  "no test infra for test infra" rule, do not add more scaffolding.
- **Known blind spots** (shared with the in-memory gate, both by-design): a select is counted
  only inside a repository-method head, so scaffolding that drives a raw mapper via
  `RdbmsTestExtension.mapper(...)` is (correctly) invisible; cross-thread fan-out
  (`parallelStream`/executor) is **under-counted, never miscounted** because the head stack is
  ThreadLocal; and only `Executor.query` is intercepted, not `Executor.queryCursor`. No current
  adapter uses a cursor or fans out across threads.
