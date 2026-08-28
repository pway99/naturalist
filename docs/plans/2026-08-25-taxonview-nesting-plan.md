# TaxonView Permit-Nesting Refactor Plan (insects + plants)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse each domain's five-file `…TaxonView` sealed family into a single file by nesting the four rank
permits inside the interface as `public record`s, keeping the `View` role suffix and dropping only the redundant
domain-noun prefix (`InsectSpeciesView` → `InsectTaxonView.SpeciesView`; `PlantSpeciesView` →
`PlantTaxonView.SpeciesView`).

**Architecture:** Pure structural refactor — no behaviour change. The four permit records move from standalone top-level
files into their sealed interface as nested records. Because all permits then live in the interface's own compilation
unit, the explicit `permits` clause is dropped (Java infers it). Per ADR-020, nesting drops the **domain-noun prefix**
(`Insect`/`Plant`) because the outer namespace already carries it — but the **`View` role suffix is retained**: it marks
the type as the read-model projection, distinguishing `…TaxonView.SpeciesView` (the view) from `InsectSpecies`/
`PlantSpecies` (the entity it wraps, returned by `species()`). The two `of(...)` static factories on each record are
preserved verbatim.

**Tech Stack:** Java 21 (records, sealed interfaces, pattern-matching `switch`/`instanceof`), JTE templates, Maven
reactor, OpenRewrite `EnforceArchitecture` gate.

## Global Constraints

- **Two independent atomic commits, insects first.** Insects is the reference domain (per repo convention); land and
  verify Part A, then mirror it in Part B for plants. Do not interleave.
- **Each part is atomic within itself.** A nested-type relocation does not compile in intermediate states (the moment
  the standalone files are deleted, every call site in that domain is broken until updated). Execute each Part as one
  commit; the "tasks" inside a Part are ordered work-phases, not independently shippable units.
- **No behaviour change.** No new fields, no changed invariants, no changed `of(...)` semantics. The existing test
  suites are the safety net; if any existing test needs a *logic* change (not just a type-reference rename), stop — that
  means the refactor altered behaviour and is wrong.
- **Nested names keep `View`, drop the domain prefix:** `SpeciesView`, `GenusView`, `FamilyView`, `OrderView`. Never
  `.Species` (loses the view/entity distinction) and never `.InsectSpeciesView` (redundant prefix under an
  already-prefixed namespace).
- **Do not static-import the nested permits.** Call sites read `InsectTaxonView.SpeciesView` (qualified). The one
  exception is inside the interface file itself, where the simple name is already in scope.
- **The outer names `InsectTaxonView` / `PlantTaxonView` are unchanged**, so external `{@code InsectTaxonView}` javadoc
  mentions (e.g. `PlantTaxonView.java:13`, `SoilProfileFactory.java:41`) stay valid and are out of scope.
- **Verification gate (run at the end of each Part):** `mvn verify` from repo root, then
  `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.

## Name-mapping reference

| Standalone (old)    | Nested (new)                  |
|---------------------|-------------------------------|
| `InsectSpeciesView` | `InsectTaxonView.SpeciesView` |
| `InsectGenusView`   | `InsectTaxonView.GenusView`   |
| `InsectFamilyView`  | `InsectTaxonView.FamilyView`  |
| `InsectOrderView`   | `InsectTaxonView.OrderView`   |
| `PlantSpeciesView`  | `PlantTaxonView.SpeciesView`  |
| `PlantGenusView`    | `PlantTaxonView.GenusView`    |
| `PlantFamilyView`   | `PlantTaxonView.FamilyView`   |
| `PlantOrderView`    | `PlantTaxonView.OrderView`    |

---

# Part A — Insects (reference domain, land first)

## File Inventory

**Rewritten:** `domains/insects/insects-api/.../InsectTaxonView.java` — gains the four nested records; loses its
`permits` clause; javadoc `@link`s repointed to nested names.

**Deleted (content moves into the interface):** `InsectSpeciesView.java`, `InsectGenusView.java`,
`InsectFamilyView.java`, `InsectOrderView.java` (all in `domains/insects/insects-api/.../`).

**Java call sites (references only):**

- `domains/insects/insects-api/.../Insect.java` — four `@Nullable` component types + four `with*` param types.
- `domains/insects/insects-api/src/test/java/.../InsectTaxonViewTest.java`
- `domains/insects/insects-api/src/test/java/.../InsectTest.java`
- `domains/insects/insects-core/.../InsectFactory.java` — `.of(...)` construction + the `(InsectTaxonView)` casts.
- `domains/insects/insects-core/src/test/java/.../InsectFactoryTest.java`

**JTE templates + template tests (manual — IDE rename will NOT touch these):**

- `insects-console/src/main/jte/insects/genus.jte` (`@import` L10, `instanceof` L77)
- `insects-console/src/main/jte/insects/family.jte` (`@import` L4, `instanceof` L75)
- `insects-console/src/main/jte/insects/order.jte` (`@import` L3, `instanceof` L73)
- `insects-console/src/test/java/.../Insects{Genera,Families,Orders}TemplateTest.java`

**Docs:** `domains/insects/CLAUDE.md` — the `**InsectTaxonView**` vocabulary entry.

## Task A1: Nest the four permit records into `InsectTaxonView`

**Files:** Modify `InsectTaxonView.java`; delete the four standalone `Insect*View.java`.

**Interfaces:**

- Produces: `InsectTaxonView.SpeciesView`, `.GenusView`, `.FamilyView`, `.OrderView` — each a `public record`
  implementing `InsectTaxonView`, each retaining both `of(entity, images)` and `of(entity)` factories and its `name()`
  override returning the concrete `InsectRankName` subtype. `SpeciesView` additionally retains `genusName()` and
  `belongsToGenus(@Nullable InsectTaxonView.GenusView)`.

- [ ] **Step 1: Move each permit's body into the interface, dropping only the `Insect` prefix.** The species permit
  becomes:

```java
/**
 * Species-rank permit — the canonical "we know exactly what species this is"
 * view. Composes the species record with the photographic field record. Root
 * identity is the species's {@link InsectSpeciesName}.
 */
public record SpeciesView(
        InsectSpecies species,
        ImageCollection images
) implements InsectTaxonView {

    public static SpeciesView of(InsectSpecies species, ImageCollection images) {
        return new SpeciesView(species, images);
    }

    public static SpeciesView of(InsectSpecies species) {
        return new SpeciesView(species, ImageCollection.empty());
    }

    @Override
    public InsectSpeciesName name() {
        return species.name();
    }

    /** The genus this species belongs to, exposed as a typed FK delegate. */
    public InsectGenusName genusName() {
        return species.genusName();
    }

    /**
     * True iff this species's genus FK equals the given genus's name, OR the
     * given genus is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(species, ...)} block without firing a
     * redundant {@code isTrue} violation when the genus is missing.
     */
    public boolean belongsToGenus(@Nullable GenusView genus) {
        return genus == null || species.belongsToGenus(genus.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(species, "species")
                .behavioralCollection(images, "images");
    }
}
```

`GenusView`, `FamilyView`, and `OrderView` follow identically (own rank entity, own `name()` return type, and any
`belongsTo…`/FK-delegate helpers currently on the standalone record — copy them verbatim; only the type name changes,
`Insect` prefix dropped). Hoist each record's imports (`org.jspecify.annotations.Nullable`,
`java.util.function.Consumer`, `com.naturalist.observability.Constraints`, `InsectEntityCollections.ImageCollection`)
into the interface's import block, de-duplicated.

- [ ] **Step 2: Drop the `permits` clause.** Change:

```java
public sealed interface InsectTaxonView extends ReadModel
        permits InsectOrderView, InsectFamilyView, InsectGenusView, InsectSpeciesView {
```

to:

```java
public sealed interface InsectTaxonView extends ReadModel {
```

- [ ] **Step 3: Repoint the interface's own javadoc.** In the class-level javadoc, change the switch example and the
  permit bullet `{@link …}`s to the nested names:

```java
 * switch (view) {
 *     case InsectTaxonView.SpeciesView sv -> ...sv.species()...;
 *     case InsectTaxonView.GenusView   gv -> ...gv.genus()...;
 *     case InsectTaxonView.FamilyView  fv -> ...fv.family()...;
 *     case InsectTaxonView.OrderView   ov -> ...ov.order()...;
 * }
```

and `{@link InsectSpeciesView}` → `{@link SpeciesView}` (nested `@link` resolves against the enclosing type). Update the
trailing note about the future subspecies permit to `SubspeciesView`.

- [ ] **Step 4: Delete the four standalone files.**

```bash
git rm domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpeciesView.java \
       domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenusView.java \
       domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamilyView.java \
       domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrderView.java
```

- [ ] **Step 5: Sanity-compile the api module.** Run: `mvn -q -pl domains/insects/insects-api -am compile`. Expected:
  FAIL citing only `Insect.java` cannot resolve `InsectOrderView` etc. (the contained, expected breakage Task A2 fixes).
  If it fails on anything *inside* `InsectTaxonView.java`, the nesting is malformed — fix before proceeding.

## Task A2: Update insects Java call sites

**Files:** Modify `Insect.java`, `InsectFactory.java`, `InsectTaxonViewTest.java`, `InsectTest.java`,
`InsectFactoryTest.java`.

- [ ] **Step 1: `Insect.java` — retype the four rank components** to `@Nullable InsectTaxonView.OrderView order`,
  `…FamilyView family`, `…GenusView genus`, `…SpeciesView species`, and the matching four `with*` parameter types.
  `List<InsectTaxonView> children` and body lines referencing fields by name are unchanged.

- [ ] **Step 2: `InsectFactory.java`** — `InsectSpeciesView.of(...)` → `InsectTaxonView.SpeciesView.of(...)` (and
  Genus/Family/Order). The stream casts `(InsectTaxonView) InsectFamilyView.of(...)` become
  `(InsectTaxonView) InsectTaxonView.FamilyView.of(...)`. Do not restructure beyond the name substitution.

- [ ] **Step 3: Update the three test files** with the same substitution, including
  `new InsectSpeciesView(null, null)` → `new InsectTaxonView.SpeciesView(null, null)`. Drop now-defunct single-type
  imports; ensure `InsectTaxonView` is imported.

- [ ] **Step 4: Compile api + core with tests.** Run: `mvn -q -pl domains/insects/insects-core -am test-compile`.
  Expected: PASS.

## Task A3: Update insects JTE templates + template tests

**Files:** Modify `genus.jte`, `family.jte`, `order.jte`, and the three
`Insects{Genera,Families,Orders}TemplateTest.java`.

- [ ] **Step 1: `genus.jte`** — remove `@import com.naturalist.insects.InsectSpeciesView` (keep the `InsectTaxonView`
  import); change `@if(child instanceof InsectSpeciesView sv)` → `@if(child instanceof InsectTaxonView.SpeciesView sv)`.
- [ ] **Step 2: `family.jte`** — remove the `InsectGenusView` import; `instanceof InsectGenusView gv` →
  `instanceof InsectTaxonView.GenusView gv`.
- [ ] **Step 3: `order.jte`** — remove the `InsectFamilyView` import; `instanceof InsectFamilyView fv` →
  `instanceof InsectTaxonView.FamilyView fv`.
- [ ] **Step 4: Template tests** — substitute any permit construction (`InsectSpeciesView.of(...)`) with the nested form
  and fix imports.
- [ ] **Step 5: Compile + test the console module.** Run: `mvn -q -pl domains/insects/insects-console -am test`.
  Expected: PASS.

## Task A4: Docs, full gate, commit (insects)

- [ ] **Step 1:** In `domains/insects/CLAUDE.md`, update the `**InsectTaxonView**` entry: permit list → nested names
  (`InsectTaxonView.SpeciesView`, `.GenusView`, `.FamilyView`, `.OrderView`); note they are nested records inside the
  sealed interface (no `permits` clause).
- [ ] **Step 2:** `mvn verify` → BUILD SUCCESS.
- [ ] **Step 3:** `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` → no pending fixes.
- [ ] **Step 4: Commit.**

```bash
git add -A
git commit -m "refactor(insects): nest InsectTaxonView rank permits as nested records

Collapse InsectSpeciesView/GenusView/FamilyView/OrderView into
InsectTaxonView as nested .SpeciesView/.GenusView/.FamilyView/.OrderView;
drop the permits clause (inferred from the compilation unit). Domain
prefix dropped per ADR-020; View role suffix retained. No behaviour change.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

# Part B — Plants (mirror of Part A; land after insects is green)

Plants mirrors insects exactly — same four ranks, same `…TaxonView`/`…Factory`/`…Test` shape. Apply Part A's mechanics
with the plant name-mappings from the reference table. Note the console template paths differ (nested subdirectories,
`detail.jte` per rank).

## File Inventory

**Rewritten:** `domains/plants/plants-api/.../PlantTaxonView.java`.

**Deleted:** `PlantSpeciesView.java`, `PlantGenusView.java`, `PlantFamilyView.java`, `PlantOrderView.java` (in
`domains/plants/plants-api/.../`).

**Java call sites:** `Plant.java`, `PlantTaxonViewTest.java`, `PlantTest.java` (all `plants-api`); `PlantFactory.java`,
`PlantFactoryTest.java` (`plants-core`).

**JTE templates + tests (manual):**

- `plants-console/src/main/jte/plants/genera/detail.jte` (`@import` L3 `PlantSpeciesView`, `instanceof` L79)
- `plants-console/src/main/jte/plants/families/detail.jte` (`@import` L2 `PlantGenusView`, `instanceof` L68)
- `plants-console/src/main/jte/plants/orders/detail.jte` (`@import` L1 `PlantFamilyView`, `instanceof` L65)
- `plants-console/src/test/java/.../Plants{Genus,Family,Order}DetailTemplateTest.java`
- **Ignore** `plants-console/jte-classes/gg/jte/generated/ondemand/plants/orders/JtedetailGenerated.java` — a generated
  build artifact, regenerated on the next template compile; do not hand-edit.

**Docs:** `domains/plants/CLAUDE.md` if it carries a `PlantTaxonView` vocabulary entry (check; update to match insects
if present).

## Task B1: Nest the four permit records into `PlantTaxonView`

**Files:** Modify `PlantTaxonView.java`; delete the four standalone `Plant*View.java`.

**Interfaces:** Produces `PlantTaxonView.SpeciesView`, `.GenusView`, `.FamilyView`, `.OrderView` — same contract as
insects. Preserve **whatever helper methods the plant permits currently carry** (they may differ from insects — e.g.
plants' `Cultivar` axis; copy verbatim, only renaming the type and dropping the `Plant` prefix). Read each standalone
file before moving it; do not assume insects' method set.

- [ ] **Step 1:** Move each permit body into `PlantTaxonView`, renaming to bare `SpeciesView`/`GenusView`/`FamilyView`/
  `OrderView` (drop `Plant` prefix, keep `View`). Hoist and de-duplicate imports.
- [ ] **Step 2:** Drop the `permits` clause from `PlantTaxonView`.
- [ ] **Step 3:** Repoint `PlantTaxonView`'s own javadoc `@link`s / switch example to the nested names.
- [ ] **Step 4:** `git rm` the four standalone `Plant*View.java` files.
- [ ] **Step 5:** Sanity-compile: `mvn -q -pl domains/plants/plants-api -am compile`. Expected: FAIL citing only
  `Plant.java`. If it fails inside `PlantTaxonView.java`, fix the nesting.

## Task B2: Update plants Java call sites

- [ ] **Step 1:** `Plant.java` — retype the four `@Nullable` rank components and matching `with*` params to
  `PlantTaxonView.{Order,Family,Genus,Species}View`.
- [ ] **Step 2:** `PlantFactory.java` — `PlantSpeciesView.of(...)` → `PlantTaxonView.SpeciesView.of(...)` (and the other
  ranks); update `(PlantTaxonView)` casts.
- [ ] **Step 3:** `PlantTaxonViewTest.java`, `PlantTest.java`, `PlantFactoryTest.java` — same substitution, including
  `new PlantSpeciesView(...)` forms; fix imports.
- [ ] **Step 4:** `mvn -q -pl domains/plants/plants-core -am test-compile`. Expected: PASS.

## Task B3: Update plants JTE templates + template tests

- [ ] **Step 1:** `plants/genera/detail.jte` — remove `@import …PlantSpeciesView`; `instanceof PlantSpeciesView sv` →
  `instanceof PlantTaxonView.SpeciesView sv`.
- [ ] **Step 2:** `plants/families/detail.jte` — remove `@import …PlantGenusView`; `instanceof PlantGenusView gv` →
  `instanceof PlantTaxonView.GenusView gv`.
- [ ] **Step 3:** `plants/orders/detail.jte` — remove `@import …PlantFamilyView`; `instanceof PlantFamilyView fv` →
  `instanceof PlantTaxonView.FamilyView fv`.
- [ ] **Step 4:** `Plants{Genus,Family,Order}DetailTemplateTest.java` — substitute permit construction + imports.
- [ ] **Step 5:** `mvn -q -pl domains/plants/plants-console -am test`. Expected: PASS.

## Task B4: Docs, full gate, commit (plants)

- [ ] **Step 1:** Update `domains/plants/CLAUDE.md` `PlantTaxonView` entry if present.
- [ ] **Step 2:** `mvn verify` → BUILD SUCCESS.
- [ ] **Step 3:** `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` → clean.
- [ ] **Step 4: Commit** (message mirrors A4, s/insects/plants/, s/Insect/Plant/).

---

## Follow-up (out of scope, note only)

- **`Insect`/`Plant` `with*` sprawl:** ten mutation-shaped withers per read model, only ever called by the same-package
  factory and tests. Deferred by decision — leave alone for now.

## Self-Review

- **Spec coverage:** every referencing file from the insects and plants greps has a task; the standalone `*View.java`
  files are deleted in Task N1; the generated `JtedetailGenerated.java` is explicitly excluded; outer-name javadoc
  mentions are out of scope (name unchanged).
- **Placeholder scan:** the `SpeciesView` body, `permits`-clause edit, and JTE `instanceof` edits are shown as literal
  code. Part B intentionally instructs "read each standalone file before moving it" rather than repeating insects' code,
  because the plant permits' helper-method set may legitimately differ (e.g. `Cultivar`) — copying insects' body blindly
  would be the bug.
- **Type consistency:** nested names `SpeciesView/GenusView/FamilyView/OrderView` (View retained, prefix dropped) used
  identically across definition (Task N1), Java sites (N2), JTE (N3), docs (N4), and the name-mapping table.
  `belongsToGenus` param retyped to nested `GenusView` matches `Insect.java`'s `species.belongsToGenus(genus)` call
  where `genus` is now `InsectTaxonView.GenusView`.
