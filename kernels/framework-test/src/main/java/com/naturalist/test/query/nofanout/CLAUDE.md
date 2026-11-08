# `com.naturalist.test.query.nofanout` — the runtime N+1 (no-fan-out) select gate

A **test-time gate** that fails a test when one call from it reads the same repository select more
than once — the N+1 fan-out that `domains/CLAUDE.md` ("Fan-out must batch — never per-element in a
loop") forbids in prose. It needs no agent, weaving or wrapper.

## How it works

`TestEntitySource` calls `SelectTrace.record` on every read (`entityStream`, `getByName`,
`getByEntityNameSet`, and `pageOf` for page 0). `SelectTrace` walks the stack to the `@Test` method
and counts per **(source, repository method)**. `NaturalistTestExtension` arms it in `beforeEach`
and evaluates it in `afterEach`, so **a test class is gated by registering
`@RegisterExtension NaturalistTestExtension`**.

**What one call means.** A stack walk runs only on a read, so it cannot see a call return. The count
starts again when a read arrives from a different test position: the chain of test frames, from the
`@Test` method inward, with their bytecode indexes. Lines are not used, so a stream or `forEach` in
a test still separates its calls.

**Not counted:** reads under a write (foreign key checks), reads outside a test method (`@BeforeEach`
included), a test calling a repository directly, and later pages of a paged read.

**Commands are covered too**, not only Queries. Writes may repeat freely.

**Repository methods that read before writing are not reported.** `TestEntitySource.insert`, `update` and
`save` mark the repository method making the write, and repeats of that method's reads are dropped when the
test ends. A stack walk at the read cannot see a write coming, so this is decided at the end of the test, per
test. A method that only writes sometimes still counts in a test where it never wrote: a loop of `addIfNew`
over rows that all exist fails, and that is a real repeated read.

## Test contexts wrap what they hand out

- **Two meanings of "port".** The *graph's* port is any non-sealed `com.naturalist` interface: whatever the
  graph hands out under such a type, built or context-provided, is a `SelectTrace.traced` JDK proxy, one per
  type. *Return wrapping* is narrower: a traced proxy wraps what a method returns only when the return type
  passes `SelectTrace.isPort`, a `*Query` or `*Command` interface.
- **Only classes built through a constructor become `Traced` subclasses.** A non-final concrete class the
  graph builds reflectively through its constructor is built, once, as a ByteBuddy subclass that implements
  `SelectTrace.Traced` and enters each of its overridable methods through `SelectTrace.TraceAdvice`. A static
  `create` / `of` result and anything a context provides are plain objects (still proxied when handed out
  under a graph port). Either wrapper makes a call from the test start a new count, so tests can loop over
  context commands, queries and transactions with plain calls. Before building a class the graph checks
  that a proxy can reach every `@jakarta.transaction.Transactional` method (a class-level one inherited from
  a superclass included), and fails with `TestGraphException` naming the class and, for an inherited
  method, the class that declares it, when one is final, private or static, or the class is final.
- **Metric names stay the production class's.** `getClass()` of a graph-built object is a synthetic
  `…$ByteBuddy$…` subclass, and `Observer` names a synthetic class for the class it extends, so
  `Observer.forClass(getClass())` metric tags and observation points read as they do in production.
  `getClass()` itself (in a stack trace, say) still shows the subclass.
- **`final` classes are not traced.** They are built plain, and a loop over one in a test fails like a
  loop in the unit under test. So does an object a test builds itself or a context provides from its own
  factory under a concrete type, and anything Spring wires in a console test: batch the call or take the
  object from the graph. **Fakes are handed out untraced** as well: that can only cause a false failure,
  never hide a fan-out.
- **A wrapper resets the count only when the test is the direct caller.** A call made from inside the unit
  under test, a self-call on a `Traced` object, or a test callback the unit runs never resets, so a loop
  in the unit still fails. Ports a traced call returns are wrapped too, and an object already traced (a
  `Traced` instance or one of `SelectTrace`'s own proxies) is returned as it is.

## ⛔ Do NOT weaken this gate to make a test pass

This is a root-`CLAUDE.md` non-negotiable. A `RepeatedSelectException` is the gate reporting a repeat
in the code under test. Fix the production fan-out. Do not, to clear a red build:

- edit `SelectTrace` to stop counting or throwing, or remove a `record` call from `TestEntitySource`;
- remove `NaturalistTestExtension`'s arming, or a failing test's `@RegisterExtension`;
- use `@SuppressRepeatedSelect` to silence a finding.

`@SuppressRepeatedSelect(reason = ...)` (in `kernels/framework`, `com.naturalist.data`) goes on the production
method doing the repeat, so the decision sits next to the code. The gate does not count any read made
while that method is on the stack, including reads in everything it calls, so annotate the narrowest
method you can. It is a **last resort** for a repeat that genuinely cannot be batched, and it has no effect
on test classes. If you believe the gate is wrong, raise it with the human.

## When a test fails with `RepeatedSelectException`

The message names the source, select and count, then a clickable stack trace for each path that made
the read.

1. Read the flagged code; find the loop or the second read.
2. Use or add a batched sibling — `getByXNames(Set<NAME>)` / `findByNameSet(Set)` — or pass along what
   the first read already returned. Reference:
   `domains/insects/insects-core/.../InsectImageQueryImpl.forRankHierarchy`.
3. Re-run the module's tests.

## Complement — the static gate

The OpenRewrite recipe `com.naturalist.rewrite.NoSelectInIteration` (composite `EnforceQueryHygiene`)
flags loop and stream fan-out **lexically** in main source, including controllers and untested queries
this gate cannot see. This gate catches fan-out composed across methods. Keep both.

## Limits

- **In-memory only.** It traces `TestEntitySource`, so rdbms repositories and their mapper selects are
  not watched: `<domain>-core` is the layer that matters, and adapters stay thin by the hexagonal rule.
  The AspectJ gate and its rdbms sibling were retired for this on 2026-11-05.
- **Tested code only.** A path no test exercises is not traced; the static gate covers some of that.
- **Known widening.** Every intercepted method on a traced object resets the count when the test calls it
  directly, so a lazy per-element select inside a test loop that also calls a traced object is not caught.
  The same was true for proxied ports.
- **Single thread.** A read on a spawned thread is not counted, so the gate can miss an N+1 there but
  never fails falsely.
- `SelectTraceTest` tests the tracer directly (stateful/lifecycle carve-out). Its fixtures are top-level
  because the tracer tells the unit from the test by outer class.
