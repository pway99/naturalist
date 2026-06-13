# ReadModel kernel type + retype/rename insect read-models — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-class `ReadModel` marker to the kernel, retype the insect read-side compositions (`Insect` + the sealed `InsectAggregate` family) onto it, rename that family to `*View`/`InsectTaxonView`, and update the identity-model docs.

**Architecture:** Three sequential tasks, each leaving a compilable + green tree: (1) introduce `ReadModel` + `Constraints.readModel(...)` and retype `Insect`/`InsectAggregate` to use them; (2) the mechanical rename of the `*Aggregate` family → `*View` with a repo-wide call-site sweep; (3) doc updates. `Aggregate` stays for genuine aggregates (`Zone`, `SoilProfile`).

**Tech Stack:** Java 21 records + sealed interfaces, Maven multi-module, the kernel Observer/Constraints framework, JUnit 5 / AssertJ.

**Design doc:** [`docs/plans/2026-06-07-readmodel-kernel-type-design.md`](2026-06-07-readmodel-kernel-type-design.md).

---

## Conventions for this plan (project-specific — read first)

- **The user runs Maven.** Do **not** invoke `mvn`. Where a step says "Verify build", surface the exact command for the user/controller and wait for confirmation.
- **Subagents stage only — never commit.** Each task ends by staging (`git add`) and reporting; the user reviews the diff and commits.
- **Trunk-based** on `main`; the user pushes.
- This is a **rename + type change with no behavior change.** No new business logic; the existing `InsectTest` / view tests / factory tests are the regression gate.

---

## Task 1: Add `ReadModel` + `Constraints.readModel(...)`, retype `Insect` and `InsectAggregate`

Introduce the kernel type and its descent method, and immediately adopt them on the two read-side types. No rename yet — the `*Aggregate` names are unchanged in this task. Build stays green; the new kernel API has an immediate consumer (`Insect.invariants()`).

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/ddd/ReadModel.java`
- Modify: `kernels/framework/src/main/java/com/naturalist/observability/Constraints.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectAggregate.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java`

- [ ] **Step 1: Create the `ReadModel` marker**

Create `kernels/framework/src/main/java/com/naturalist/ddd/ReadModel.java`:

```java
package com.naturalist.ddd;

import com.naturalist.observability.Observable;

/**
 * A read-side composition assembled from already-persisted parts — a projection,
 * not a transactional consistency boundary.
 * <p>
 * A {@code ReadModel} is built at read time from entities that live in their own
 * repositories (e.g. a rank record plus its photographs). It is <b>immutable</b>,
 * its identity is <b>optional</b> (a read model may have none), and it is
 * <b>never the unit of a write or transaction</b>.
 * <p>
 * Its {@link #invariants()} assert the <i>structural well-formedness of the
 * projection</i> — required parts non-null, monotonic-fill, agreement among the
 * foreign keys of the assembled parts — <b>not</b> cross-entity consistency that
 * this type owns and mutates.
 * <p>
 * Contrast with {@link Aggregate}: an aggregate is a consistency boundary that
 * owns its child entities and value objects and is mutated as a unit (e.g.
 * {@code Zone}, {@code SoilProfile}). When a type is assembled for reading and
 * owns nothing, it is a {@code ReadModel}, not an {@code Aggregate}.
 *
 * @see Aggregate
 */
public interface ReadModel extends Observable {
}
```

- [ ] **Step 2: Add the `readModel(...)` descent method to `Constraints`**

In `Constraints.java`, add the import (alphabetically with the other `com.naturalist.ddd` imports):

```java
import com.naturalist.ddd.ReadModel;
```

Then insert this block immediately after the `aggregateOrNull(...)` overloads (after the method ending at line 68, before the `valueObject(...)` block at line 70):

```java
    /**
     * Validate a non-null {@link ReadModel} child and descend into its invariants.
     * Type-specific counterpart to {@link #observable} for read-model children —
     * assembled projections such as {@code Insect} or the rank views.
     */
    public <R extends ReadModel> Constraints readModel(R readModel, String name) {
        return readModel(readModel, Function.identity(), name);
    }

    public <O, R extends ReadModel> Constraints readModel(O o, Function<O, R> valueFunction, String name) {
        return add(new ObservableConstraint<>(o, valueFunction, name));
    }
```

(Body is identical to `aggregate(...)` / `observable(...)` — they all wrap `ObservableConstraint`. The distinct method name is the self-documenting value.)

- [ ] **Step 3: Retype the sealed `InsectAggregate` to `ReadModel`**

In `InsectAggregate.java`:
- Line 3: change `import com.naturalist.ddd.Aggregate;` → `import com.naturalist.ddd.ReadModel;`
- Line 47: change `public sealed interface InsectAggregate extends Aggregate` → `public sealed interface InsectAggregate extends ReadModel`
- In the class javadoc (line 7–9), change "assembled into a single consistency boundary" → "assembled into a single read-side view" (it is not a consistency boundary). Leave the rest of the javadoc; it is rewritten in Task 2's rename.

(The four permits implement `InsectAggregate`, so they become `ReadModel`s transitively — no per-permit edit in this task.)

- [ ] **Step 4: Retype `Insect` and swap its descent calls**

In `Insect.java`:
- Line 3: change `import com.naturalist.ddd.Aggregate;` → `import com.naturalist.ddd.ReadModel;`
- Line 88: change `) implements Aggregate {` → `) implements ReadModel {`
- In `invariants()`, change the four rank descents from `.aggregate(...)` to `.readModel(...)`:
  - Line 175: `.aggregate(order, "order")` → `.readModel(order, "order")`
  - Line 179: `.aggregate(family, "family")` → `.readModel(family, "family")`
  - Line 183: `.aggregate(genus, "genus")` → `.readModel(genus, "genus")`
  - Line 190: `.aggregate(species, "species")` → `.readModel(species, "species")`

  (The `.notNull(...)` presence checks and `.isTrue(...belongsTo...)` FK checks are unchanged.)

- [ ] **Step 5: Verify build (user runs)**

Ask the user to run from the repo root:

```
mvn -q -pl domains/insects/insects-api -am test
```

Expected: PASS. `InsectTest` (which observes `Insect` via `mo.observable(...)` and walks the constraint graph) is the consumer test for both the retype and the new `readModel(...)` method — it must stay green. The view tests (`InsectAggregateTest`) and the factory/query tests also stay green (they already observe via `mo.observable(...)`). Do not proceed until the user confirms PASS.

- [ ] **Step 6: Stage (do NOT commit — controller checkpoints for user review)**

```bash
git add \
  kernels/framework/src/main/java/com/naturalist/ddd/ReadModel.java \
  kernels/framework/src/main/java/com/naturalist/observability/Constraints.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectAggregate.java \
  domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java
```

Suggested commit message (user commits): `Add ReadModel kernel type; retype Insect + InsectAggregate onto it`.

---

## Task 2: Rename the `*Aggregate` family → `*View` / `InsectTaxonView`

A mechanical, repo-wide rename. No type or behavior change (Task 1 already retyped). The risk is missing a call site, so a repo-wide sweep gates the task.

**The rename map** (apply to every occurrence — class names, file names, javadoc `{@link}`s, variables where noted):

| From | To |
|---|---|
| `InsectAggregate` (the sealed type) | `InsectTaxonView` |
| `InsectSpeciesAggregate` | `InsectSpeciesView` |
| `InsectGenusAggregate` | `InsectGenusView` |
| `InsectFamilyAggregate` | `InsectFamilyView` |
| `InsectOrderAggregate` | `InsectOrderView` |
| `InsectAggregateFactory` | `InsectTaxonViewFactory` |
| `InsectAggregateQueryImpl` | `TaxonViewQueryImpl` |
| `InsectAggregateQuery` (nested in `InsectQuery`) | `TaxonViewQuery` |
| `insect()` (the `InsectQuery` accessor + override) | `taxonView()` |
| `InsectAggregateTest` | `InsectTaxonViewTest` |
| `InsectAggregateFactoryTest` | `InsectTaxonViewFactoryTest` |
| `InsectAggregateQueryImplTest` | `TaxonViewQueryImplTest` |

**Ordering note:** rename `InsectAggregate` → `InsectTaxonView` with a whole-word match, but be careful it does not also rewrite the longer names (`InsectAggregateFactory`, `InsectAggregateQuery*`). Do the **longer/compound names first**, then the bare `InsectAggregate`. The permit names (`Insect*Aggregate`) do not contain the substring `InsectAggregate`, so they are independent.

**Files to rename on disk** (`git mv`), then edit contents:
- `insects-api/.../InsectAggregate.java` → `InsectTaxonView.java`
- `insects-api/.../InsectSpeciesAggregate.java` → `InsectSpeciesView.java`
- `insects-api/.../InsectGenusAggregate.java` → `InsectGenusView.java`
- `insects-api/.../InsectFamilyAggregate.java` → `InsectFamilyView.java`
- `insects-api/.../InsectOrderAggregate.java` → `InsectOrderView.java`
- `insects-core/.../InsectAggregateFactory.java` → `InsectTaxonViewFactory.java`
- `insects-core/.../InsectAggregateQueryImpl.java` → `TaxonViewQueryImpl.java`
- `insects-api/.../test/.../InsectAggregateTest.java` → `InsectTaxonViewTest.java`
- `insects-core/.../test/.../InsectAggregateFactoryTest.java` → `InsectTaxonViewFactoryTest.java`
- `insects-core/.../test/.../InsectAggregateQueryImplTest.java` → `TaxonViewQueryImplTest.java`

**Files that reference the renamed identifiers and must be edited (no file rename):**
- `insects-api/.../InsectQuery.java` — nested `InsectAggregateQuery` interface (line 62), the `insect()` accessor (line 48), `getByName` return type (line 71), and the javadoc/usage block (lines 19, 35–37, 65, 67).
- `insects-core/.../InsectQueryImpl.java` — field type, factory construction, and the `insect()` override (lines 13, 35–37, 41).
- `insects-api/.../Insect.java` — the rank-slot component types and `withOrder/withFamily/withGenus/withSpecies` signatures (lines 22–24, 43, 82–85, 140–152) use `Insect*Aggregate` → `Insect*View`.
- `insects-api/.../test/.../InsectTest.java` — field types + `.of(...)` factory call sites (lines 98, 110, 123, 137, 299, 319, 340, 379–409).
- The four permit records themselves — class names (from the `git mv`), `implements InsectAggregate` → `implements InsectTaxonView`, the `belongsTo*(@Nullable Insect*Aggregate ...)` parameter types → `Insect*View`, and javadoc `{@link InsectAggregate}` → `{@link InsectTaxonView}`.
- `insects-core/.../InsectAggregateFactory.java` (→ `InsectTaxonViewFactory.java`) — `new Insect*Aggregate(...)` (lines 64–74), `<A extends InsectAggregate>` (line 79), javadoc.
- The three renamed test files — class names, constructor calls (`new InsectAggregateFactory(...)`, `new InsectAggregateQueryImpl(...)`), `Optional<InsectAggregate>` types, `insectQuery.insect()` calls.
- `insects-core/.../InsectsTestContext`-adjacent wiring: confirm via grep — `InsectsTestContext` constructs queries but not the aggregate factory directly; the factory is built inside `InsectQueryImpl`. Verify no other `*TestContext` references the renamed types.

**Optional polish (do it for honesty):** in the factory, rename the `observe(A aggregate)` parameter and the `"insectAggregate"` observation label to `taxonView` / `"taxonView"`; rename local `aggregate` variables in the tests to `view`. These are cosmetic and may be skipped if they balloon the diff — note in the report if skipped.

**One reference to leave alone:** `Constraints.java:59` javadoc mentions `InsectSpeciesAggregate` as an *example* — this is kernel code that must not depend on a domain. Update the example text to a generic phrasing (e.g. "a nullable child read model") rather than the domain name, OR leave it; do **not** introduce a kernel→domain reference. (It is a javadoc comment, not a real dependency.)

- [ ] **Step 1: Rename files and update all references**

Apply the rename map across the files listed above. Use `git mv` for the 10 file renames so history is preserved, then edit contents. Recommended approach: for each identifier in the map (longest first), run a scoped replace across `domains/insects` and `kernels`. After editing, the bare `InsectAggregate` token must survive **only** inside the renamed `InsectTaxonView.java` as the new name (i.e. it should no longer appear anywhere).

- [ ] **Step 2: Repo-wide completeness sweep**

Run and confirm each returns no live-code hits (excluding the design/plan docs and cold-storage docs):

```
grep -rn "InsectAggregate\|Insect[A-Za-z]*Aggregate" --include=*.java domains kernels apps | grep -v worktrees
grep -rn "\.insect()" --include=*.java domains kernels apps | grep -v worktrees
```

Expected: **zero** matches in `.java` (every old name is gone; `InsectTaxonView` / `InsectSpeciesView` / `taxonView()` etc. are the only forms). If anything remains, fix it before proceeding.

- [ ] **Step 3: Verify build (user runs)**

Ask the user to run from the repo root:

```
mvn -q verify
```

Expected: PASS across all modules. The renamed tests (`InsectTaxonViewTest`, `InsectTaxonViewFactoryTest`, `TaxonViewQueryImplTest`) compile and pass; `InsectQueryImplTest` / `InsectTest` / `InsectsTestContext` compile against `taxonView()` / `InsectTaxonView`. The console module compiles unchanged (it never referenced these). Do not proceed until the user confirms PASS.

- [ ] **Step 4: Stage (do NOT commit)**

Stage all renamed + edited files (use `git add -A domains/insects kernels/framework/src/main/java/com/naturalist/observability/Constraints.java` after verifying `git status` shows only the intended renames/edits and nothing stray). Report the full `git status --short` to the controller.

Suggested commit message: `Rename insect *Aggregate read-models to *View / InsectTaxonView`.

---

## Task 3: Update the identity-model docs

Reflect the sixth marker and the rename in the authoritative `CLAUDE.md` files. Cold-storage docs (ADRs, `docs/briefings/`, archived plans, `docs/work-tracker.md` historical rows) are **intentionally left** — they are point-in-time records; per project convention we don't retro-edit them.

**Files:**
- Modify: `CLAUDE.md` (top-level)
- Modify: `domains/CLAUDE.md`
- Modify: `kernels/CLAUDE.md`
- Modify: `domains/insects/CLAUDE.md`

- [ ] **Step 1: Top-level `CLAUDE.md`**

In the `framework/` module-layout block (lines 62–63), add `ReadModel` to the type list:

```
  framework/          — NamedEntity, Entity, EntityName, EntityId, Aggregate,
                        ReadModel, ValueObject, Observable, Observer,
                        BehavioralCollection, Resilience facade
```

In the "Identity" section, change the enumeration "implements exactly one of `NamedEntity`, `Entity`, `Aggregate`, `ValueObject`, `BehavioralCollection`" to include `ReadModel` (after `Aggregate`).

In the "Records for NamedEntity/Entity/Aggregate/ValueObject" non-negotiable (line 127), add `ReadModel`: "**Records for NamedEntity/Entity/Aggregate/ReadModel/ValueObject.**"

- [ ] **Step 2: `domains/CLAUDE.md`**

In the "Identity Model" section, add a `ReadModel` bullet after the `Aggregate` bullet, and add the currently-missing `Entity` bullet (the list presently omits it — correct it while here):

```
- **Entity\<ID extends EntityId\>** — surrogate UUIDv7 identity. Observations and
  events are Entity records: immutable, equality by value.
- **ReadModel** — read-side composition assembled from already-persisted parts.
  Immutable, identity optional, NOT a consistency boundary; `invariants()` assert
  the projection's structural well-formedness, not owned cross-entity consistency.
  Use instead of `Aggregate` when the type owns nothing and is never mutated as a
  unit (e.g. the insect `*View` read models).
```

Change the opening "implements one of four interfaces" to "implements one of six interfaces."

At line 247 (the "Reference implementation" paragraph), update the renamed identifiers: `InsectAggregateQueryImpl` → `TaxonViewQueryImpl`, `InsectAggregateFactory` → `InsectTaxonViewFactory`.

- [ ] **Step 3: `kernels/CLAUDE.md`**

In the `framework` kernel description (lines 10–11), add `ReadModel` to the building-blocks list (after `Aggregate`).

In "Observability Framework", add a `readModel(...)` entry to the by-function descent list (after the `aggregate`-related entries, near line 190): "`readModel(o, fn, name)` — validates a `ReadModel` child and descends into its invariants."

In "Testing Observables" (lines 207–208), add `ReadModel` to the enumerated marker types.

At line 227, update the observation-helper rule to cover read models:

```
- `mo.namedEntity(e, label)` for `NamedEntity`/`Aggregate`; `mo.observable(o, label)` for any other `Observable` (`ReadModel`, `BehavioralCollection`)
```

- [ ] **Step 4: `domains/insects/CLAUDE.md`**

Replace the stale `InsectAggregate` vocabulary entry (lines 16–23 — it says "three Linnaean ranks"/lists three permits, but there are four) with:

```
**InsectTaxonView** — Sealed `ReadModel` over the four Linnaean ranks that carry
catalog entities: `InsectSpeciesView`, `InsectGenusView`, `InsectFamilyView`,
`InsectOrderView`. Each permit composes its rank entity with the `ImageCollection`
of photographs attached at that rank — a read-side projection, not a consistency
boundary. Identity is the root's typed `InsectRankName`, returned polymorphically
by `name()`. Assembled by name through `InsectTaxonViewFactory` and read via
`insectQuery.taxonView().getByName(rankName)`; `InsectSubspeciesName` is permitted
on `InsectRankName` but yields `Optional.empty()` (no subspecies entity exists yet).
```

- [ ] **Step 5: Verify (user runs)**

Docs only — no build impact. Ask the user to skim the four edited sections for accuracy. (Optionally `mvn -q -pl ... -am test` is unaffected; a docs-only change needs no build, but a final full `mvn verify` was already green at end of Task 2.)

- [ ] **Step 6: Stage (do NOT commit)**

```bash
git add CLAUDE.md domains/CLAUDE.md kernels/CLAUDE.md domains/insects/CLAUDE.md
```

Suggested commit message: `Docs: add ReadModel to the identity model; InsectAggregate → InsectTaxonView`.

---

## Out of scope (do not touch)

- **R4/R5** — `Insect`'s `with*` mutators and the observations-vs-taxon conflation. `Insect` is only retyped/edited for the rename; not reshaped.
- **`Zone`, `SoilProfile`** — genuine `Aggregate`s; unchanged.
- **`@AggregateRoot` annotation** (on `InsectSpecies`, `Compound`, `SoilProfileInfo`) — orthogonal; unchanged.
- **`aggregate(...)` / `aggregateOrNull(...)` in `Constraints`** — left in place (kernel vocabulary; `aggregateOrNull` having no consumer is the separate parked R3).
- **ADR-020, `docs/briefings/insects-domain.md`, archived plans, work-tracker historical rows** — historical/illustrative; left as-is.
