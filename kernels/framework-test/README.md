# framework-test

The in-memory analog of the production data layer, and the harness that proves an
adapter obeys its contract. It carries no domain knowledge — only the machinery
every `<domain>-repository-test` module builds its fixtures and contract tests on.
It is never a compile-scope dependency.

---

## What it provides

**`TestEntitySource` — an in-memory table.** A `TestEntitySource<KEY, ENTITY>` is
the in-memory equivalent of one RDBMS table. It keys entities on their `key()`
(an `EntityName` for `NamedEntity`, an `EntityId` for `Entity`) and enforces, on
every insert: primary-key uniqueness (automatic), secondary `UniqueConstraint`s,
and intra-domain `ForeignKeyConstraint`s. Instances are seeded from JSON catalog
files, so schema is defined in Java and data in JSON.

**`NaturalistDatabase` — the single database.** One in-memory registry stands in
for the single production database. It is the only object permitted to construct a
`TestEntitySource`: a source is acquired through `NaturalistDatabase#getNamed(SourceClass.class)`,
which reflects on the source's `(NaturalistDatabase)` constructor, builds it once,
and caches it — so a whole test (or an unwired composition root) shares one
catalog. Main-wired code obtains a database via `NaturalistDatabase.create()` and
carries no JUnit coupling.

**`NaturalistTestExtension` — the JUnit lifecycle.** A subclass of
`NaturalistDatabase` that also implements `BeforeEachCallback`. Registered as a
static `@RegisterExtension` field, it resets the registry before each test, so
fixtures do not leak across tests.

**`EntityRepositoryTest` — the behavioral contract.** A `@Test default` interface
that defines, once, the behavior every repository adapter must exhibit —
argument validation, not-found, and expected-result cases for each read and write
method (ADR-002). A concrete domain contract supplies only identity constants and
entity-construction hooks; both the in-memory adapter and the RDBMS adapter
implement the same interface, which is what guarantees they behave identically.

**The N+1 select-count gate.** `com.naturalist.test.query.nofanout` holds an
AspectJ load-time-woven `SelectCountAspect` → `SelectCountRecorder` → `SelectGate`.
Wired into `NaturalistTestExtension`, it arms before each test and, after each,
throws `RepeatedSelectException` when a single head-of-DAG query invocation
repeats a repository select — the runtime signature of an N+1. Gating is per
outermost invocation, so a test that legitimately calls a query N times is not
flagged. A genuinely un-batchable repeat is whitelisted per-site with
`@AllowRepeatedSelect`; the gate is never disabled to make a test pass.

**Supporting types.** `RandomValue` (constrained random field values for
fixtures), `TestDataHelper`, `UniqueConstraint`, and `ForeignKeyConstraint`.

---

## Why it looks the way it does

The point of a single in-memory database that every adapter shares is that the
swap to a real RDBMS is invisible to consumers: the same behavioral contract
holds both adapters to the same behavior by construction, and the N+1 gate keeps
the composition batched rather than fanning out per element. Because the data
layer is in-memory, the full contract suite runs in seconds, which is what makes
running the enforcement on every build affordable.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — the `framework-test` conventions in detail,
  including the "no test infra for test infra" rule.
- [ADR-001 — Repository architecture](../../docs/adr/ADR-001-repository-architecture.md)
- [ADR-002 — Repository behavioral contract via test interface](../../docs/adr/ADR-002-repository-behavioral-contract.md)
- [N+1 select-count gate — design](../../docs/plans/2026-08-21-n-plus-one-select-gate-design.md)
  and [plan](../../docs/plans/2026-08-21-n-plus-one-select-gate-plan.md)
