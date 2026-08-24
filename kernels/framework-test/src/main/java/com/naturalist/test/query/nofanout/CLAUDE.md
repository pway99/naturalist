# `com.naturalist.test.query.nofanout` — the runtime N+1 (no-fan-out) select gate

A **test-time gate** that fails the suite when a single head-of-DAG query invocation
resolves the same repository select more than once — the N+1 fan-out that
`domains/CLAUDE.md` ("Fan-out must batch — never per-element in a loop") forbids in prose.
This package makes that prose an executable invariant that trips on the seed data during
the build. **It is test-only** (`aspectjweaver` is test/runtime scope); it is never on a
production classpath.

## How it works — three collaborators + the extension

```
SelectCountAspect  ──feeds──▶  SelectCountRecorder  ──snapshot──▶  SelectGate
   (AspectJ LTW)                 (ThreadLocal, per-invocation)        (evaluate/throw)
        ▲                                                                  ▲
   woven per test run                                    NaturalistTestExtension
   via META-INF/aop.xml                                  arms (beforeEach) / evaluates (afterEach)
```

- **`SelectCountAspect`** (load-time woven, dumb feeder) — an `@Around` on every
  `*QueryImpl` / `AbstractEntityQuery` method maintains a head-of-DAG **query stack**; a
  `@Before` on every `*Repository+` method (except writes `insert`/`update`/`save`) records
  one **select** against the current outermost head. Never evaluates, never throws.
- **`SelectCountRecorder`** (ThreadLocal) — counts selects **per outermost query
  INVOCATION** and folds them into the per-test tally via `Math.max` at outermost exit.
  This is load-bearing: keying by query FQN and *summing across the whole test* (the
  original bug) false-flagged a test that legitimately calls one query N times, each doing
  one select. Per-invocation gating means only a *single* invocation looping a select is an
  N+1.
- **`SelectGate`** — pure rule: any `(head, select)` with count > 1 that no
  `@AllowRepeatedSelect` covers is a violation; throws `RepeatedSelectException` naming them
  all.
- **`NaturalistTestExtension`** (in the sibling `com.naturalist.data` package) hosts it:
  `beforeEach` arms the recorder, `afterEach` evaluates and disarms. **A test class is gated
  simply by registering `@RegisterExtension NaturalistTestExtension`** — most repository/
  query/core tests already do.

## Using it

- Nothing to call — register `NaturalistTestExtension` and the gate is live.
- **Escape hatch:** whitelist a genuinely un-batchable repeat with
  `@AllowRepeatedSelect(query = "<HeadQueryFQN or simple>", select = "<repoSelectFQN or simple>")`
  on the specific test method (repeatable), with a one-line comment saying why it cannot
  batch. Use sparingly — most repeats are real N+1s to fix, not to allow.

## ⛔ Do NOT weaken this gate to make a test pass

This is a root-`CLAUDE.md` non-negotiable. A `RepeatedSelectException` is the gate reporting
a real N+1 in the code under test — it is never the gate's fault. When you see one, the ONLY
acceptable response is to fix the production fan-out. **Do not**, to clear a red build:

- edit `SelectCountRecorder` / `SelectGate` / `SelectCountAspect` to stop counting or throwing;
- remove or no-op `NaturalistTestExtension`'s `beforeEach`/`afterEach` arming;
- delete a failing test's `@RegisterExtension NaturalistTestExtension`, or move the query call
  out of the gated body;
- narrow the `aop.xml` weave scope or an aspect pointcut to dodge a class;
- blanket-`@AllowRepeatedSelect` across methods to silence findings.

`@AllowRepeatedSelect` is a **per-site last resort** for a single, genuinely un-batchable
repeat, with a written justification — never a tool to make a failing suite green. If you
believe the gate itself is wrong (a false positive), stop and raise it with the human; do not
quietly disable it.

## When a test fails with `RepeatedSelectException`

The message names the `head query` + `repository select` + count. That is a real N+1:
inside one query invocation, a select was resolved once per element instead of batched.
**Fix the production fan-out (see above — do not weaken the gate):**

1. Read the flagged query/factory; find the loop / `stream().map` / recursive descent that
   calls a single-key select per element.
2. Add or use a batched sibling — `getByXNames(Set<NAME>)` / `findByNameSet(Set)` /
   `getByXIds(Set)` — and resolve the whole set in one call. Reference:
   `domains/insects/insects-core/.../InsectImageQueryImpl.forRankHierarchy`.
3. Re-run the module's tests; the exception clears.

## Current state (2026-08-23)

**Armed on `main` (commit `53e6dc87`), and GREEN.** The 4 genuine N+1 heads / 14 tests this
gate deliberately red have all been batched at the source (commits `55a0e9c5`, `96a5941a`,
`e948054e`, `5f268944`) — insects `InsectQueryImpl.getByName` + `InsectCitationQueryImpl.findByRankName`,
plants `PlantQueryImpl.getByName`, soil `SoilProfileQueryImpl.getBySoilProfileName`. Full
`mvn verify` passes with zero `RepeatedSelectException`. The remediation record (what each fix
did) is in `docs/plans/2026-08-23-n-plus-one-runtime-gate-findings.md` (§ Resolution). The
gate now guards against regressions rather than flagging a known backlog.

## Complement — the static gate

The static OpenRewrite recipe `com.naturalist.rewrite.NoSelectInIteration` (composite
`EnforceQueryHygiene`, in `tooling/naturalist-rewrite`) flags loop/stream fan-out
**lexically** in main source. This runtime gate catches **composed / cross-method
within-head** fan-outs the lexical recipe cannot see (3 of its 4 findings were missed by
the static gate); the static gate catches controller loops and untested queries this one
cannot. They are complementary — keep both.

## Constraints / gotchas

- **Never on a production classpath.** `aspectjweaver`/`aspectjrt` stay test scope; the
  `-javaagent` is wired once in the root `pom.xml` surefire `argLine`. `aop.xml` (in
  `META-INF/`) declares the aspect FQN and weave scope — **if you move/rename the aspect,
  update `aop.xml` and the root pom, and reinstall `framework-test`** (downstream modules
  weave from the installed jar).
- **Single-threaded safe.** Surefire is not parallelised and no core code fans out across
  threads in test paths. The ThreadLocal chain is synchronous; if that ever changes, a
  select on a spawned thread is **under-counted, never miscounted** — the gate can miss an
  N+1 there but never false-fail.
- **No "test infra for test infra" beyond what's here.** The recorder/gate carry direct
  unit tests (stateful/lifecycle carve-out); the aspect is proven end-to-end by
  `SelectGateWeavingTest` + `AllowRepeatedSelectWeavingTest`. Don't add more scaffolding.
