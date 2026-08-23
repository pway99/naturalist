# `naturalist-rewrite` — OpenRewrite enforcement module (design)

**Date:** 2026-08-23
**Status:** Implemented (this commit — the `failOnDryRunResults` arm, 2026-08-23). R1/R2/R3
are live via `com.naturalist.EnforceArchitecture`; R2/R3 are scoped to the test source set
(see the CI wiring section below); the whole-reactor `rewrite:dryRun` gate is armed and
green on `main`.
**Companion:** `docs/plans/2026-08-23-archunit-testentitysource-enforcement-strategy.md`
(the invariant this module polices, and why ArchUnit alone can't).

## Why OpenRewrite, not more ArchUnit

The invariant (ADR-001): a `TestEntitySource` is acquired via
`NaturalistDatabase#getNamed(X.class)`, never `new X(db)`; and in tests the
`NaturalistDatabase` comes from a registered `@RegisterExtension NaturalistTestExtension`
field, not a bare `NaturalistDatabase.create()`.

ArchUnit imports the *running module's* classpath — its own main+test classes plus
dependencies' **main** classes only. Other modules' **test** classes are never on the
classpath (test-jars aren't deps), so no single central ArchUnit test can police test
code repo-wide. That is exactly where these violations live.

OpenRewrite parses **source across the whole reactor** — main and test, every module —
via the `rewrite-maven-plugin`. It sidesteps the classpath limitation entirely, and it
can **auto-fix**, not just detect. This module is the general-purpose home for
architectural-enforcement recipes; the TestEntitySource invariant (R1 + R2 + R3) is the pilot.

The existing ArchUnit `DataForkComplianceTest` (main-code, bytecode lens) is **kept** as a
complementary, independent check. The rejected Strategy A (`DataForkArchTest` copied into
every module) is **dropped** — this module subsumes its purpose without the
"remember to add it to every new module" gap.

## Posture: fix everything possible, mark what can't be fixed safely

Per the brainstorming decision, recipes auto-fix wherever the transformation is well-defined.
A fix recipe is also its own detector: `rewrite:dryRun` with `failOnDryRunResults=true`
fails CI whenever a fix *would* apply; `rewrite:run` applies it. The one hard floor: any
shape a recipe cannot confidently transform emits a `SearchResult` **marker** rather than a
wrong rewrite. A marker is still a pending change, so dryRun still fails and a human fixes
that one site. Silent-corrupt is the only outcome worse than a manual fix.

## Module placement & shape

New **top-level `tooling/` tree** (a 6th alongside `adapters/`, `apps/`, `domains/`,
`external-authorities/`, `kernels/`), with one member: `tooling/naturalist-rewrite`.
It is neither a domain, app, adapter, nor a runtime kernel — it is build/dev tooling that
pulls in `rewrite-java`, too heavy for the light-dependency `kernels/` tree. It ships **no
runtime code** and nothing depends on it.

```
tooling/
  naturalist-rewrite/
    src/main/java/com/naturalist/rewrite/
      NoDirectTestEntitySourceConstruction.java   # R1
      AcquireDatabaseViaExtension.java            # R2
      NoCachedTestEntitySourceField.java          # R3
    src/main/resources/META-INF/rewrite/
      naturalist.yml                              # composite: com.naturalist.EnforceArchitecture
    src/test/java/com/naturalist/rewrite/
      *Test.java                                  # RewriteTest before/after pairs
```

The `RewriteTest` unit tests are the one place recipes get verified. This is **not**
"test infra for test infra" — it is testing transformation logic that will rewrite the
entire repository; a wrong recipe is a repo-wide hazard.

## CI wiring & the dryRun gate

`rewrite-maven-plugin` configured **once in the root pom**: `naturalist-rewrite` as a
plugin dependency, `com.naturalist.EnforceArchitecture` as the single active recipe.

Because the recipe lives in the same reactor, the recipe jar must be resolvable before the
plugin runs — the standard same-repo custom-recipe pattern is two passes. **This is the
CI invocation actually wired and proven (Task 7):**

```bash
# CI gate
mvn install -DskipTests                            # whole reactor -> ~/.m2, incl. the recipe jar
mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true   # fail on any pending change

# Developer auto-fix
mvn rewrite:run
```

`failOnDryRunResults=true` (armed in the root pom's `rewrite-maven-plugin` `<configuration>`
as of this commit) fails the build if any recipe would change any file, in any module, main
or test — with one carve-out: **R2 (`AcquireDatabaseViaExtension`) and R3
(`NoCachedTestEntitySourceField`) are scoped to the `test` source set.** Both recipes'
visitors short-circuit on any `JavaSourceFile` whose `JavaSourceSet` marker name is not
`"test"` (a missing marker is treated as non-test), so they never touch the three sanctioned
`src/main` `NaturalistDatabase.create()` callers: `TestDataConfiguration`'s Spring `@Bean`,
the `TestEntitySourceTest` contract base, and a Javadoc `@link` in `NaturalistTestExtension`
(all ADR-001-sanctioned). R1 (`NoDirectTestEntitySourceConstruction`) is deliberately left
unscoped — `new *TestEntitySource(...)` is forbidden everywhere, main or test.

Verified: with the gate armed, `mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` on a
clean `main` is `BUILD SUCCESS`; introducing a bare `NaturalistDatabase.create()` in an
existing `src/test` file reproducibly fails the build, citing the R2 marker on the exact
offending line.

## Pilot recipe 1 — `NoDirectTestEntitySourceConstruction` (R1)

**Rule:** never `new *TestEntitySource(db)`.
**Fix:** `new XTestEntitySource(db)` → `db.getNamed(XTestEntitySource.class)`.
**Match:** a constructor invocation whose target type simple-name ends with
`TestEntitySource` and is not the abstract base `TestEntitySource` itself (mirrors the
ArchUnit predicate). The `super(database)` call inside each concrete subclass's own ctor is
a constructor call on `TestEntitySource` (the base), so it is naturally excluded.
**Current state:** 0 violations repo-wide — this recipe is a **regression gate** that
demonstrates a clean auto-fix when a violation is introduced.
**Sequencing:** build R1 first (simple, showcases the mechanism end-to-end), then R3
(also a 0-violation gate, similar inline-and-delete shape), then R2 (the heavy structural
fixer with 43 live sites).

## Pilot recipe 2 — `AcquireDatabaseViaExtension` (R2)

**Rule:** in **test** source, do not obtain a `NaturalistDatabase` from a bare
`NaturalistDatabase.create()`; obtain it from a `@RegisterExtension NaturalistTestExtension`
field. `NaturalistTestExtension extends NaturalistDatabase`, so retyping keeps every
`db.getNamed(...)` call compiling (inherited), and `NaturalistTestExtension.create()` is a
*different* declared static method, so the sanctioned path is never matched.

**Current state:** 43 live call-sites in test code, in four shapes (increasing difficulty):

1. **Field initializer** — `private final NaturalistDatabase db = NaturalistDatabase.create();`
   Fix: retype to `NaturalistTestExtension`, use `NaturalistTestExtension.create()`, add
   `@RegisterExtension`, add imports.
2. **Method-local** — `NaturalistDatabase db = NaturalistDatabase.create();`
   Fix: hoist a `@RegisterExtension NaturalistTestExtension` field, delete the local.
3. **Nested as an argument** — `SomeTestContext.create(NaturalistDatabase.create())`
   Fix: hoist a field, replace the argument expression with a reference to it.
4. **Inline / unstored** — `NaturalistDatabase.create().findAll(...)`
   Fix: hoist a field, replace the `create()` expression with a reference to it.

**Field synthesis:** when hoisting, generate a field name (default `db`), collision-checked
against existing fields; add `@RegisterExtension` + the `NaturalistTestExtension` import.

**Guardrail:** any shape where a field can't be confidently synthesized/placed emits a
`SearchResult` marker instead of a rewrite. dryRun still fails; a human fixes that site.

**Scope exclusions:** test source only; `NaturalistTestExtension.create()` and main-wired
`NaturalistDatabase.create()` (console bootstraps, `TestDataConfiguration`) are excluded.

## Pilot recipe 3 — `NoCachedTestEntitySourceField` (R3)

**Why:** the extension clears the source registry in `@BeforeEach`, which runs *after* a test
instance's field initializers. A `TestEntitySource`-typed **field** is therefore populated at
construction (pre-reset) and then silently detached at the next reset — a stale reference that
no longer belongs to the live, reset registry. Every test must re-fetch its sources from the
`NaturalistDatabase` *after* reset so the whole set of `TestEntitySource` instances is reset
together.

**Rule (the local-vs-field distinction is the whole point):**
- **Forbid** — an instance or static **field** in a test class whose declared type is a
  `*TestEntitySource` subclass.
- **Allow** — method-**local** variables typed as `*TestEntitySource` and assigned from
  `db.getNamed(...)` inside a test method (fetched fresh, post-reset), and helper methods that
  `return db.getNamed(X.class)`. This is the sanctioned house idiom (e.g. insects console/
  repository-test suites, `ElementCatalogDataTest#source()`). A recipe that flagged locals
  would break the correct pattern repo-wide — it must target fields only.

**Fix:** inline the field into its usages — replace each field reference with
`db.getNamed(XTestEntitySource.class)` and delete the field — when the field's initializer is a
plain `getNamed` call.
**Guardrail:** if the field's initializer is not a plain `getNamed` call, or usages can't be
safely inlined (e.g. `db` not in scope at a usage site, non-final field reassigned), emit a
`SearchResult` marker instead of rewriting.
**Current state:** 0 field violations repo-wide — ships as a **regression gate** (like R1).

## Relationship to Strategy B (from the companion doc)

Strategy B (make `*TestEntitySource` ctors `private` + `setAccessible` in `getNamed`) makes
R1 compiler-guaranteed and is orthogonal to this module — it can still be adopted later for
defense-in-depth. This module does **not** depend on or require it, and does not change ctor
visibility or touch the kernel.

## Out of scope (YAGNI)

- No additional architectural rules yet — R1/R2 is the pilot; the module is *structured* to
  grow (composite `naturalist.yml`), but no speculative recipes are authored now.
- No deletion of existing ArchUnit enforcement.
- No kernel changes.
