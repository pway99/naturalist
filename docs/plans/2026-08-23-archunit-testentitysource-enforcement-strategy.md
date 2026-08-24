# Enforcing "acquire TestEntitySources only via NaturalistDatabase" — strategy for a fresh session

**Date:** 2026-08-23
**Status:** Analysis + recommendation. Not yet implemented. Pick up in a fresh session.

## The invariant to protect

ADR-001: a `TestEntitySource` is the in-memory analog of an RDBMS table, and **only
`NaturalistDatabase` may construct one** — acquire via `NaturalistDatabase#getNamed(X.class)`,
never `new X(db)`. Direct construction creates a source outside the shared registry, so
cross-source FK/unique constraints don't resolve against the same catalog. Tests get their
`NaturalistDatabase` from a **registered JUnit extension** — a non-static instance field
`@RegisterExtension NaturalistTestExtension db = NaturalistTestExtension.create();` — which is
the ephemeral, reset-per-test "container" (clears the registry `@BeforeEach`). The suite is
transitioning fully onto `NaturalistTestExtension`.

Two distinct rules fall out:
- **R1 (structural, ADR-001):** never `new *TestEntitySource(...)` anywhere — main or test.
- **R2 (transition convention):** in tests, obtain the DB from a `@RegisterExtension
  NaturalistTestExtension` field, not a bare `NaturalistDatabase.create()` local.

## Current state (facts, verified 2026-08-23)

- **Main code is already locked:** `apps/management-console/src/test/.../architecture/DataForkComplianceTest`
  (`@AnalyzeClasses(packages="com.naturalist", DoNotIncludeTests)`) has two green rules —
  no `NaturalistDatabase.create()` outside `{TestEntitySourceTest, TestDataConfiguration}`, and
  no `new *TestEntitySource(...)`. It sees every module's **main** classes (their JARs are deps).
- **Test code is NOT centrally reachable.** ArchUnit imports the *running module's* classpath:
  its own main+test classes + dependencies' **main** classes only. Other modules' **test**
  classes are never on the classpath (test-jars aren't deps). So a single central test cannot
  police test code repo-wide.
- The 14 fixture-building tests that used `new *TestEntitySource(NaturalistDatabase.create())`
  were migrated to the extension idiom (commit `1a7b4230`); ~42 other test files already used it.
  Repo-wide `git grep "new [A-Za-z]*TestEntitySource(" -- '*/src/test/*.java'` is now **empty**.
- **22 modules** reference `TestEntitySource`/`NaturalistDatabase`/`NaturalistTestExtension` in
  test code (the `*-console`, `*-core`, `*-repository-test`, `*-test-context` families, plus
  `framework-test`, `eol-client-mock`).
- **43** concrete `*TestEntitySource` subclasses currently declare a **`public`** ctor
  `(NaturalistDatabase)`.
- `NaturalistDatabase#getNamed` (kernels/framework-test/.../NaturalistDatabase.java) builds a
  source via `sourceClass.getDeclaredConstructor(NaturalistDatabase.class).newInstance(this)` —
  **no `setAccessible(true)` today**, so it currently relies on the ctor being `public`.

## Strategy A — per-module ArchUnit (rejected as primary)

One `DataForkArchTest` (`@AnalyzeClasses(packagesOf = <marker in this module>)`, tests included)
per module you want guarded, referencing a shared `ArchRule`.

**Why it's weak (this was the user's objection):** it guards only where placed. You must
*remember* to add the class to each of ~22 modules, and **any new module is silently unprotected**
until someone remembers. The guard's presence is itself unenforceable without more meta-machinery.
DRY still requires deciding where the shared `ArchRule` constant + the archunit dependency live
(`framework-test` = kernel edit + archunit on its classpath; or a new tiny test-support module;
or copy-paste). High standing cost, permanent gap.

## Strategy B — compiler enforcement + one central rule (RECOMMENDED for R1)

Make the anti-pattern **fail to compile**, so no per-module test is needed and future/forgotten
modules are covered by construction.

1. **Make every `*TestEntitySource` ctor non-`public`.** `private` is airtight (blocks `new`
   even from the same package — closes the split-package test case); package-private blocks only
   cross-package. Recommend **`private`**. Then `new X(db)` won't compile anywhere.
2. **One-line kernel change** so `getNamed` still builds them: add `constructor.setAccessible(true)`
   before `newInstance` in `NaturalistDatabase#getNamed`. `getDeclaredConstructor` already finds
   non-public ctors; `setAccessible` bypasses the access check (plain classpath app, no
   JPMS/SecurityManager). **This is a `framework-test` (kernel) change → needs user sign-off.**
3. **Guard the convention centrally** — add one rule to the existing `DataForkComplianceTest`
   (main-code, already sees all `TestEntitySource` subclasses): *"no subclass of `TestEntitySource`
   declares a public constructor."* A new source shipping a public ctor fails this **one** rule.
   No per-module ceremony; covers modules that don't exist yet.

**Net:** R1 becomes compiler-guaranteed at every call site + a single global rule keeps new source
classes honest. Zero per-module tests.

**Costs / caveats:**
- 43 ctors change visibility — mechanical, one-time, across the `-repository-test` modules. **Safe
  now:** post-migration nothing calls `new *TestEntitySource` in main or test (grep-clean), so the
  change breaks no current call site. Do a full `mvn verify` to confirm `getNamed`+`setAccessible`
  constructs every source (some sources may have logic in their ctor — verify none rely on public
  access beyond reflection).
- The `getNamed` `setAccessible` edit is the only kernel touch — get sign-off first.

## R2 (bare `create()` → registered extension) — not compiler-enforceable

A compiler can't tell `NaturalistDatabase.create()` from `NaturalistTestExtension.create()` (both
yield a `NaturalistDatabase`). Options, in order of preference:
- Leave it to reviewer discipline + the fact that R1 already forces every source through a
  `NaturalistDatabase` and the extension is the obvious, house-standard way to get one.
- OR a central-ish ArchUnit rule "test classes should not call `NaturalistDatabase.create()`"
  **with a whitelist** for the legitimate callers (`framework-test`'s `TestEntitySourceTest`; and
  `NaturalistTestExtension.create()` itself is a different method so it isn't caught). This still
  hits the per-module test-classpath limitation, so it inherits Strategy A's weakness — only worth
  it if R2 regressions actually appear.

## Recommended execution outline (fresh session)

1. Confirm current state (`git grep "new [A-Za-z]*TestEntitySource(" -- '*/src/test/*.java'` empty;
   `DataForkComplianceTest` green).
2. **Sign-off gate:** propose the `getNamed` `setAccessible(true)` one-liner (kernel) — wait for OK.
3. Add `setAccessible(true)` in `NaturalistDatabase#getNamed`.
4. Mechanical sweep: change all 43 `public <X>TestEntitySource(NaturalistDatabase ...)` ctors to
   `private` (one commit per domain, or one sweep — reviewer's call).
5. Add the central rule to `DataForkComplianceTest`: no `TestEntitySource` subclass has a public
   (or non-private) ctor. It should go green immediately after step 4.
6. `mvn verify` — every source still constructs via `getNamed`; whole reactor green.
7. Decide on R2 (likely: reviewer discipline; revisit only if regressions appear). Drop the
   per-module `DataForkArchTest` idea entirely.

## Pointers
- Central rule + main-code enforcement: `apps/management-console/src/test/java/com/naturalist/console/architecture/DataForkComplianceTest.java`
- `getNamed`: `kernels/framework-test/src/main/java/com/naturalist/data/NaturalistDatabase.java`
- Extension: `kernels/framework-test/src/main/java/com/naturalist/data/NaturalistTestExtension.java`
- ADR-001 (repository architecture), domains/CLAUDE.md §"Test Entity Sources" / §"Repository Architecture".
- Related session work: controller de-fork + `DataForkComplianceTest` (`docs/plans/2026-08-22-controller-defork-archunit-plan.md`), the 14-test extension migration (commit `1a7b4230`).
