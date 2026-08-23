# N+1 Select Gate — Static OpenRewrite Backstop — Design

**Date:** 2026-08-23
**Status:** recipe shipped; arming deferred pending N+1 remediation (see followups doc)
**Goal:** A source-level CI gate that fails the build when a repository select or query
invocation appears lexically inside an iteration construct — the N+1 fan-out pattern. It is
authored as an OpenRewrite **search recipe** in `tooling/naturalist-rewrite` that plants a
`SearchResult` marker, discovered by the already-wired `rewrite:dryRun` +
`failOnDryRunResults=true` gate.

## Relationship to the AspectJ runtime gate (the complement)

This recipe **complements**, and does not replace, the paused AspectJ select-count gate
([2026-08-21-n-plus-one-select-gate-design.md](2026-08-21-n-plus-one-select-gate-design.md)).
The two divide the work by what each can actually see:

| | AspectJ runtime gate | This static recipe |
|---|---|---|
| Mechanism | Counts real selects per head-of-DAG query at runtime | Detects select sites in iteration constructs, statically |
| Sees | Only paths a test exercises, and only when the seed DAG fans out >1 child at a level | **All** source — untested queries, thin seed data included |
| Catches uniquely | Cross-method fan-out; **recursion** (`forRankHierarchy`); "same select twice, no loop" | N+1 in untested queries; author-time, no seed data or test needed |
| Cost | javaagent + LTW + ThreadLocal + per-test arming | Reuses the shipped, already-gated recipe module — pure static, no runtime |

The AspectJ gate is the fast in-test-loop signal (cheap when an agent runs tests); this
recipe is the final backstop for anything the runtime path can't reach. The division is
crisp: **this recipe owns the lexical, same-method case** (the select sits inside an
iteration construct right there in the source); **cross-method and recursive fan-out are
delegated to AspectJ**, which is the only gate that can observe them.

## Detection semantics

### Select site

A `J.MethodInvocation` **or** `J.MemberReference` whose method's declaring type is assignable
to `com.naturalist.data.EntityRepository` or `com.naturalist.data.EntityQuery`, **excluding**
the three `EntityRepository` write methods `insert` / `update` / `save`.

- **Type-based, not name-based.** Domain-specific selects (`getByParentNames`, `forGenusName`,
  …) are declared on concrete sub-interfaces that extend the two base ports, so an
  assignability check on the declaring type catches them without enumerating method names.
- **Method references count.** `repo::getByName` passed to `.map(...)` is the canonical real
  case; `J.MemberReference` carries a `getMethodType()` with the same declaring type, so it is
  matched identically to a direct call.
- **Writes excluded** because a bulk `insert`/`save` loop (e.g. seeding) is legitimate and is
  not a read fan-out. Mirrors the AspectJ pointcut's exclusion set.

### Iteration construct

A select site is a **violation** when, walking up the cursor **within the enclosing method
body**, it is contained by either:

1. a classic loop body — `J.ForLoop` / `J.ForEachLoop` / `J.WhileLoop` / `J.DoWhileLoop`; or
2. a `J.Lambda` or `J.MemberReference` that is the functional argument to a **per-element
   operation** — any `java.util.stream.Stream` / `BaseStream` operation (`map`, `filter`,
   `flatMap`, `peek`, `forEach`, `mapToObj`, `anyMatch`/`allMatch`/`noneMatch`, …), or
   `Iterable.forEach` / `Map.forEach`. Each of these applies its argument once per element,
   which is the fan-out. Matched by `MethodMatcher`.

Both `filter` and `map` (and the rest of the family) are in scope: a select inside a
`.filter(...)` predicate fans out exactly as one inside `.map(...)`.

### Lexical, same-method scope

The upward walk **stops at the enclosing `J.MethodDeclaration`** (and does not treat a lambda
that is *not* a per-element fan-out argument — e.g. `orElseGet`, `Comparator`, `Runnable` — as
an iteration construct). Consequences, by design:

- A select in a helper method that a loop calls is **not** flagged here — it produces no
  lexical loop in this method. That cross-method fan-out is AspectJ's job.
- Recursive fan-out (a tree walk with no explicit loop) is **not** flagged here — AspectJ's job.

This boundary is what keeps the recipe a precise structural check with a low false-positive
rate, rather than an unreliable attempt at whole-program dataflow.

## The rule and the marker

Any select site inside an iteration construct is a violation — **no escape hatch**, matching
the three existing `EnforceArchitecture` recipes as absolute invariants. On a hit:

```java
SearchResult.found(site, message);
```

where `message` names the offending call and points at the batched sibling to use instead
(`getByEntityNameSet` / `findByNameSet` / a domain `getBy…Names`). `rewrite:dryRun` reports the
marker; `failOnDryRunResults=true` fails CI.

### Findings summary via a data table

In addition to the per-site `SearchResult` marker, the recipe emits one row per finding to an
OpenRewrite **data table**, so a run produces a single scannable, machine-readable summary
(`rewrite:dryRun` writes it as CSV under `target/rewrite/datatables/…`) rather than only
inline dry-run diffs.

- `com.naturalist.rewrite.NoSelectInIteration.Findings extends org.openrewrite.DataTable<Row>`,
  constructed in the recipe's constructor with a display name and description.
- `Row` is a record with `@Column(displayName, description)` fields:
  `sourcePath`, `enclosingType`, `enclosingMethod`, `select` (the repository/query call —
  simple method name), `iterationKind` (`LOOP` vs `STREAM_OP`, and which op / loop keyword),
  and `suggestedBatchedSibling`.
- The visitor calls `findings.insertRow(ctx, new Row(...))` at each hit, immediately before
  planting the `SearchResult`. The marker is what fails the gate; the data table is the report.

**Escape hatch (deferred, YAGNI):** if the full-repo reckoning surfaces a genuinely
un-batchable loop-bound select, add a `@Option List<String>` of exempt call-sites to the
recipe and list every exemption centrally in `naturalist.yml`, so all exclusions are visible
in one declarative place (preferred over scattered annotations). Not built until a real case
demands it.

## Recipe shape & wiring

- **One imperative recipe:** `com.naturalist.rewrite.NoSelectInIteration`, in the style of the
  existing three — `Preconditions.check(new UsesType<>(EntityRepository) || new
  UsesType<>(EntityQuery), visitor)` over a `JavaIsoVisitor`.
- **Main-source-only — a `JavaSourceSet == "main"` guard (the inverse of `NoCachedTestEntitySourceField`'s
  test filter).** The N+1s live in `-core` query implementations and `management-console` controllers,
  both main source. Test code legitimately loops repository selects for arrange/assert (e.g.
  `for (x : items) assertThat(repo.getByName(x))…`); scanning test source would false-positive on
  exactly that. Short-circuit any source file whose `JavaSourceSet` marker name is not `"main"`. The
  `UsesType` precondition additionally self-scopes to files that reference a repository/query port.
- **Separate declarative composite:** `com.naturalist.EnforceQueryHygiene` in
  `tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml`, kept distinct
  from `EnforceArchitecture` (which is the ADR-001 test-data-graph concern). N+1 is a distinct
  query-hygiene concern, and a separate composite keeps CI failures self-describing.
- **Activation:** add `com.naturalist.EnforceQueryHygiene` to the root `pom.xml`
  `<activeRecipes>`, alongside `com.naturalist.EnforceArchitecture`.

## Proving the gate bites

TDD with the `rewrite-test` harness, following the existing `*Test` fixture-table pattern.
Assert `SearchResult` presence/absence on:

- classic `for` loop calling `repo.getByName(...)` → **flagged**
- `.stream().map(repo::getByName)` (method-ref) → **flagged** (the real case)
- `.stream().forEach(x -> repo.getByName(x))` (lambda) → **flagged**
- `.stream().filter(x -> repo.getByName(x).isPresent())` (select in a predicate) → **flagged**
- a query invocation inside a loop → **flagged**
- batched sibling `repo.getByEntityNameSet(set)` **not** in a loop → **clean**
- `repo.insert(x)` inside a loop (bulk seed) → **clean** (write exclusion)
- a repo call in a helper method invoked from a loop → **clean** (delegated to AspectJ; pins the boundary)
- the same `for`-loop select in **test source** (`srcTestJava`) → **clean** (main-source-only guard)

At least one fixture additionally asserts the emitted **data-table row** (via
`RewriteTest`'s `dataTable(...)` / `dataTableAsCsv(...)` assertion) so the `Findings` columns
are pinned, not just the marker.

Stub the two port types in the test source (the existing `NaturalistTypeStubs` pattern) so the
recipe module needs no dependency on the framework.

## Rollout

1. Author + unit-test the recipe (TDD, green fixtures).
2. Add it to a **new** `EnforceQueryHygiene` composite; run `mvn rewrite:dryRun` across the repo
   with the composite active but **not** yet armed in CI.
3. Triage from the emitted `Findings` data table (one CSV, all sites): real N+1 → fix to the batched call (follow `InsectImageQueryImpl` and the
   `getByParentNames` pattern in [domains/CLAUDE.md](../../domains/CLAUDE.md)); genuinely
   un-batchable → the deferred `naturalist.yml` exclusion option.
4. Arm it — add the composite to `<activeRecipes>` so `failOnDryRunResults=true` fails CI on any
   future N+1.

Independent of the AspectJ effort, which stays parked.

## Non-goals

- Cross-method and recursive fan-out (AspectJ's exclusive territory).
- Any runtime metric or production instrumentation.
- Rewriting the N+1 away automatically — this is a **search** recipe (detect + fail), not a
  transformation. Fixes are authored by hand against the correct batched sibling.
