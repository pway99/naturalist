# Plants Console Clade Read-Model Sourcing — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Source the plants-console Tree-of-Life trail from the shared `library.CladeQuery`/`CladeView` read model instead of calling `kernels/clades` `CladeTraversal` directly in the controller.

**Architecture:** A single private `stepsFor(Clade)` helper on `PlantsController` queries `cladeQuery.getBySlug(slug)` and flattens `ancestry ++ subject` into `List<CladeStep>`. The three trail producers (`cladeTrailFor`, `catalogCladeRoot`, `cladeDetail`) call it; the two clade-trail templates and four template tests migrate from `List<Clade>` to `List<CladeStep>`. Insects is untouched (it is the reference and already works this way).

**Tech Stack:** Java 21, Spring Boot (constructor injection), JTE templates, JUnit 5 + AssertJ, Maven multi-module.

## Global Constraints

- **Insects holds still.** No file under `domains/insects/` changes.
- **No new UX.** Do not add the gap affordance or ancestor-intro panels.
- **No new module, no kernel change.** Only add a dependency on the existing `library-api`.
- **Never weaken a gate to make code pass.** The N+1 gate, ArchUnit, and `EnforceArchitecture`/`EnforceQueryHygiene` OpenRewrite recipes must pass by fixing code, never by suppression.
- **Typed identifiers only;** `CladeStep.cladeSlug()` is a `String` slug carried by the library read model — that is the read model's contract, not a domain-boundary reference.
- **Spec:** `docs/plans/2026-08-24-plants-console-clade-read-model-design.md`.

---

## File Structure

**Modify:**
- `domains/plants/plants-console/pom.xml` — add `library-api` dependency.
- `domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantsController.java` — add `CladeQuery` field + constructor param, add `stepsFor` helper, rewrite `cladeTrailFor`/`catalogCladeRoot`/`cladeDetail`.
- `domains/plants/plants-console/src/main/jte/plants/cladeTrail.jte` — param `List<Clade>` → `List<CladeStep>`.
- `domains/plants/plants-console/src/main/jte/plants/nav.jte` — param `List<Clade>` → `List<CladeStep>`.
- `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsCladeTrailTemplateTest.java` — build `List<CladeStep>`; add root→subject slug-order assertion.
- `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsCladeDetailTemplateTest.java` — build `List<CladeStep>`.
- `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsOrderListTemplateTest.java` — build `List<CladeStep>`.
- `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantBreadcrumbTest.java` — remove the static `cladeTrailFor` test (coverage moves to the template test).
- `domains/plants/CLAUDE.md` — document the read-model sourcing + the three justified per-domain differences.

**Reference (read, do not modify):**
- `domains/insects/insects-console/.../InsectsController.java` (`cladeTrailAt`, `cladeTrail`) — the sourcing pattern.
- `domains/insects/insects-console/.../InsectsCladeTrailTemplateTest.java` — the hand-built-`CladeStep` test pattern.
- `domains/library/library-api/.../CladeQuery.java`, `CladeView.java`, `CladeStep.java` — the read-model contract.

---

## Task 1: Migrate the clade trail templates and their tests to `CladeStep`

Templates first: change the two clade-trail templates and **all** their template tests to consume `List<CladeStep>`, and add the `library-api` dependency the tests need. This task leaves the controller still passing `List<Clade>` at runtime — a momentary mismatch resolved in Task 2 — but every plants-console **test** (template tests supply their own model; `PlantBreadcrumbTest` is untouched and its static `cladeTrailFor` call still compiles because the controller is unchanged) compiles and passes. The two tasks land in one branch; do not ship Task 1 alone.

**Files:**
- Modify: `domains/plants/plants-console/pom.xml` — add `library-api` dependency.
- Modify: `domains/plants/plants-console/src/main/jte/plants/cladeTrail.jte`
- Modify: `domains/plants/plants-console/src/main/jte/plants/nav.jte`
- Test: `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsCladeTrailTemplateTest.java`
- Test: `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsCladeDetailTemplateTest.java`
- Test: `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsOrderListTemplateTest.java`

**Interfaces:**
- Consumes: `com.naturalist.library.CladeStep(String cladeSlug, String displayName, Optional<LinealRank> rank)` — from `library-api`, added to the module pom in this task's Step 0.
- Produces: `cladeTrail.jte` and `nav.jte` accepting `@param List<CladeStep> cladeTrail`; all three cladeTrail-feeding template tests supply `List<CladeStep>`.

- [ ] **Step 0: Add the `library-api` dependency**

In `domains/plants/plants-console/pom.xml`, add (alphabetically, after `catalog-inmem` and before `plants-api`):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
```

No `<version>` — it is inherited from the root `dependencyManagement` (insects-console already depends on it). The controller does not use it yet (Task 2 does); the template tests below do.

- [ ] **Step 1: Rewrite the cladeTrail template test to feed `List<CladeStep>`**

Replace the whole file with:

```java
package com.naturalist.plants;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Magnoliids;
import com.naturalist.library.CladeStep;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/cladeTrail.jte} — the phylogenetic tree-of-life row,
 * now fed the shared library read model's {@link CladeStep}s (root→subject).
 */
class PlantsCladeTrailTemplateTest {

    /** Real lineage as {@link CladeStep}s, mirroring what the controller reads off CladeQuery. */
    private static List<CladeStep> steps(Clade tip) {
        return CladeTraversal.ancestry(tip).reversed().stream()
                .map(c -> new CladeStep(c.slug(), c.displayName(), Optional.empty()))
                .toList();
    }

    // Piperales → magnoliids; the trail runs root→subject, Eukaryota → … → Magnoliids.
    private static final List<CladeStep> MAGNOLIID_TRAIL = steps(new Magnoliids());

    @Test
    void trailRunsRootToSubjectEndingAtMagnoliids() {
        assertThat(MAGNOLIID_TRAIL).extracting(CladeStep::cladeSlug)
                .startsWith("eukaryota", "plantae")
                .endsWith("magnoliids");
    }

    @Test
    void rendersInConsoleLineageWithHoverDropdownsAndTheEukaryotaCrossover() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", MAGNOLIID_TRAIL), output);

        String html = output.toString();
        assertThat(html).contains("Tree of life");
        // Every node lands on an in-console clade page — even Eukaryota, the shared root.
        assertThat(html).contains("href=\"/plants/clades/eukaryota\"");
        assertThat(html).contains("href=\"/plants/clades/plantae\"");
        assertThat(html).contains("href=\"/plants/clades/magnoliids\"");
        // Hover dropdowns present each node's narrower clades — Angiosperms → Monocots …
        assertThat(html).contains("clade-menu");
        assertThat(html).contains("href=\"/plants/clades/monocots\"");
        // … and Eukaryota's dropdown reaches Animalia, crossing into the insects console.
        assertThat(html).contains("href=\"/insects/clades/animalia\"");
        // Never a link into the shared /clades tree-of-life browser.
        assertThat(html).doesNotContain("href=\"/clades/");
    }

    @Test
    void surfacesTheMissingClassAsALearningLink() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", MAGNOLIID_TRAIL), output);

        assertThat(output.toString()).contains("href=\"/concepts/class\"");
    }

    @Test
    void emptyTrailRendersNothing() {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(
                "plants/cladeTrail.jte", Map.of("cladeTrail", List.<CladeStep>of()), output);

        assertThat(output.toString()).doesNotContain("Tree of life");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q test -pl domains/plants/plants-console -am -Dtest=PlantsCladeTrailTemplateTest`
Expected: FAIL — `cladeTrail.jte` still declares `@param List<Clade> cladeTrail`, so JTE rejects the `List<CladeStep>` model (type mismatch at render), and/or the new slug-order assertion has no template yet.

- [ ] **Step 3: Migrate `cladeTrail.jte` to `CladeStep`**

Rewrite the header and node loop. New header:

```jte
@import com.naturalist.clades.Clade
@import com.naturalist.library.CladeStep
@import com.naturalist.plants.PlantCladeTree
@import java.util.List

@param List<CladeStep> cladeTrail = java.util.List.of()
@param Clade current = null
```

Inside the `@for` loop, change the node binding and the two `Clade`-typed uses (the dropdown children and the links re-resolve a `Clade` from the slug, exactly as insects' template does):

```jte
            !{var node = cladeTrail.get(i);}
            !{var narrower = PlantCladeTree.narrower(Clade.of(node.cladeSlug()));}
            <span class="clade-node">
                @if(current != null && node.cladeSlug().equals(current.slug()))
                    <span class="clade-trail-node clade-trail-current"><strong>${node.displayName()}</strong></span>
                @else
                    <a class="clade-trail-node" href="${PlantCladeTree.pageUrl(Clade.of(node.cladeSlug()))}">${node.displayName()}</a>
                @endif
                @if(!narrower.isEmpty())
                    <span class="clade-node-caret" aria-hidden="true">▾</span>
                    <span class="clade-menu" role="menu">
                        @for(var child : narrower)
                            <a role="menuitem" href="${PlantCladeTree.pageUrl(child)}">${child.displayName()}</a>
                        @endfor
                    </span>
                @endif
            </span>
```

Everything else in the file (the `Tree of life` label, separators, the `infoPopover` block) is unchanged. `child` remains a `Clade` (from `narrower`), so its `pageUrl`/`displayName` calls are unchanged.

- [ ] **Step 4: Migrate `nav.jte`'s param type**

In `plants/nav.jte`, add the import and retype the param; leave `current` as `Clade`:

```jte
@import com.naturalist.plants.BreadcrumbSegment
@import com.naturalist.clades.Clade
@import com.naturalist.library.CladeStep
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<CladeStep> cladeTrail = java.util.List.of()
@param Clade current = null
```

The `@template.plants.cladeTrail(cladeTrail = cladeTrail, current = current)` call is unchanged.

- [ ] **Step 4b: Retype the `cladeTrail` param in the eight page templates that forward it to `nav.jte`**

Eight page templates each declare `@param List<Clade> cladeTrail = java.util.List.of()` and forward it straight to `@template.plants.nav(...)`. Because `nav.jte`'s param is now `List<CladeStep>`, every one of these must retype too or the module fails to compile:

- `plants/detail.jte`, `plants/list.jte`, `plants/genera/detail.jte`, `plants/orders/detail.jte`, `plants/orders/list.jte`, `plants/families/detail.jte`, `plants/families/list.jte` — in each, `Clade` is used **only** on its `@import com.naturalist.clades.Clade` line and the `cladeTrail` param line. So make two edits: replace `@import com.naturalist.clades.Clade` with `@import com.naturalist.library.CladeStep`, and change `@param List<Clade> cladeTrail = java.util.List.of()` to `@param List<CladeStep> cladeTrail = java.util.List.of()`.
- `plants/clades/detail.jte` — this one uses `Clade` throughout (clade cards, `current`, child clades). **Keep** its `@import com.naturalist.clades.Clade`, **add** `@import com.naturalist.library.CladeStep`, and change only `@param List<Clade> cladeTrail = java.util.List.of()` to `@param List<CladeStep> cladeTrail = java.util.List.of()`.

General rule if you touch any other template: retype the `cladeTrail` param to `List<CladeStep>`, ensure `com.naturalist.library.CladeStep` is imported, and drop the `com.naturalist.clades.Clade` import only if no other `Clade` reference remains in that file.

- [ ] **Step 5: Update `PlantsCladeDetailTemplateTest` to build `List<CladeStep>`**

Add imports `com.naturalist.library.CladeStep` and `java.util.Optional`, and replace the trail construction (currently line ~33):

```java
        List<CladeStep> trail = CladeTraversal.ancestry(new Superasterids()).reversed().stream()
                .map(c -> new CladeStep(c.slug(), c.displayName(), Optional.empty()))
                .toList();
```

(Keep the existing `Clade`, `CladeTraversal`, `Superasterids`, `Asterids` imports — they build the source lineage. The `Map.of("cladeTrail", trail)` call is unchanged.)

- [ ] **Step 6: Update `PlantsOrderListTemplateTest` to build `List<CladeStep>`**

Add imports `com.naturalist.library.CladeStep` and `java.util.Optional`, and replace the inline `cladeTrail` model value (currently `List.of(new Plantae(), new Angiosperms())`) with:

```java
                        "cladeTrail", java.util.List.of(new Plantae(), new Angiosperms()).stream()
                                .map(c -> new CladeStep(c.slug(), c.displayName(), Optional.empty()))
                                .toList()),
```

- [ ] **Step 7: Run the full plants-console suite to verify it passes**

Run: `mvn -q verify -pl domains/plants/plants-console -am`
Expected: PASS. All template tests consume `List<CladeStep>`; `PlantBreadcrumbTest` still passes (controller unchanged, its static `cladeTrailFor` call still returns `List<Clade>`).

- [ ] **Step 8: Commit**

```bash
git add domains/plants/plants-console/pom.xml \
        domains/plants/plants-console/src/main/jte/plants/cladeTrail.jte \
        domains/plants/plants-console/src/main/jte/plants/nav.jte \
        domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsCladeTrailTemplateTest.java \
        domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsCladeDetailTemplateTest.java \
        domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsOrderListTemplateTest.java
git commit -m "refactor(plants): clade-trail templates consume library CladeStep"
```

---

## Task 2: Source the controller trail from `CladeQuery`

**Files:**
- Modify: `domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantsController.java`
- Test: `domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantBreadcrumbTest.java`

**Interfaces:**
- Consumes: `CladeQuery.getBySlug(String) : Optional<CladeView>`; `CladeView.subject() : CladeStep`, `CladeView.ancestry() : List<CladeStep>`. `library-api` is already a module dependency (added in Task 1, Step 0).
- Produces: `PlantsController` model attribute `"cladeTrail"` is now `List<CladeStep>` on every rank, list, and clade page; private `stepsFor(Clade) : List<CladeStep>`; `cladeTrailFor` and `catalogCladeRoot` return `List<CladeStep>` (both private **instance** methods now).

> **Prerequisite from Task 1:** the pom `library-api` dependency and the `List<CladeStep>` migrations of `PlantsCladeTrailTemplateTest`, `PlantsCladeDetailTemplateTest`, and `PlantsOrderListTemplateTest` are already done. This task touches only `PlantsController.java` and `PlantBreadcrumbTest.java`.

- [ ] **Step 1: Remove the static `cladeTrailFor` test**

In `PlantBreadcrumbTest.java`, delete the `cladeTrail_forSpecies_walksUpToTheOrdersPlacement` test method (it called `PlantsController.cladeTrailFor(plant)` statically; that method becomes a private instance method in Step 2, and its rendering is now covered by `PlantsCladeTrailTemplateTest`'s root→subject slug assertion). The two `breadcrumbFor` tests stay.

- [ ] **Step 2: Wire `CladeQuery` into the controller and rewrite the three producers**

In `PlantsController.java`:

Add imports:

```java
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeStep;
import com.naturalist.library.CladeView;
```

Add the field (beside the other query fields) and constructor param + assignment:

```java
    private final CladeQuery cladeQuery;
```

```java
    PlantsController(PlantQuery plantQuery,
                     CultivarQuery cultivarQuery,
                     SeedLineageQuery seedLineageQuery,
                     PlantProgramQuery plantProgramQuery,
                     PhytochemicalConstituentQuery phytochemicalConstituentQuery,
                     CladeQuery cladeQuery,
                     Resilience resilience) {
        this.plantQuery = plantQuery;
        this.cultivarQuery = cultivarQuery;
        this.seedLineageQuery = seedLineageQuery;
        this.plantProgramQuery = plantProgramQuery;
        this.phytochemicalConstituentQuery = phytochemicalConstituentQuery;
        this.cladeQuery = cladeQuery;
        this.descriptionRenderer = new DescriptionRenderer(PlantsParagraphCues.CUES);
        this.resilience = resilience;
    }
```

Add the helper (near the other clade helpers):

```java
    /** Root→subject clade steps for a clade, sourced from the shared library read model. */
    private List<CladeStep> stepsFor(Clade clade) {
        CladeView view = cladeQuery.getBySlug(clade.slug()).orElseThrow(
                () -> new IllegalStateException("no clade view for slug " + clade.slug()));
        var steps = new ArrayList<CladeStep>(view.ancestry());
        steps.add(view.subject());
        return steps;
    }
```

Rewrite `catalogCladeRoot()` (keep `lowestCommonAncestor`, drop the direct `CladeTraversal.ancestry` return):

```java
    private List<CladeStep> catalogCladeRoot() {
        List<Clade> orderClades = Pages.stream(1000, plantQuery.orders()::findPage)
                .map(PlantOrder::placedIn)
                .filter(clade -> clade != null)
                .toList();
        Clade sharedAncestor = lowestCommonAncestor(orderClades);
        return stepsFor(sharedAncestor == null ? new Plantae() : sharedAncestor);
    }
```

Rewrite `cladeTrailFor(Plant)` — change `static List<Clade>` to a private **instance** `List<CladeStep>` and delegate to `stepsFor`:

```java
    private List<CladeStep> cladeTrailFor(Plant plant) {
        if (plant.order() == null) {
            return List.of();
        }
        Clade placedIn = plant.order().order().placedIn();
        if (placedIn == null) {
            return List.of();
        }
        return stepsFor(placedIn);
    }
```

In the `cladeDetail` handler, replace the clade-trail line:

```java
        model.addAttribute("cladeTrail", stepsFor(clade));
```

(was `model.addAttribute("cladeTrail", CladeTraversal.ancestry(clade).reversed());`). The `Clade.of(slug)` guard, `isAnimal` redirect, `ordersPlacedAt`, and `PlantCladeTree.narrower` for `childClades` are all unchanged. `CladeTraversal` remains imported — `lowestCommonAncestor` still uses it.

- [ ] **Step 3: Build and run the full plants-console suite**

Run: `mvn -q verify -pl domains/plants/plants-console -am`
Expected: PASS. All template tests (`PlantsCladeTrailTemplateTest`, `PlantsCladeDetailTemplateTest`, `PlantsOrderListTemplateTest`, `PlantsFamily/Genus/Order*`, `PlantsDetailTemplateTest`), `PlantBreadcrumbTest` (two methods), and `PlantDetailGraphTest` compile and pass.

- [ ] **Step 4: Run the architectural-enforcement gate**

Run:
```bash
mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true
```
Expected: BUILD SUCCESS with no pending rewrite results (no N+1 / query-hygiene / architecture markers introduced). If the app-context boot test in `apps/management-console` runs here, it must still pass — the `CladeQuery` bean already exists in that context (insects consumes it), so `PlantsController`'s new param resolves.

- [ ] **Step 5: Commit**

```bash
git add domains/plants/plants-console/pom.xml \
        domains/plants/plants-console/src/main/java/com/naturalist/plants/PlantsController.java \
        domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsCladeDetailTemplateTest.java \
        domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantsOrderListTemplateTest.java \
        domains/plants/plants-console/src/test/java/com/naturalist/plants/PlantBreadcrumbTest.java
git commit -m "refactor(plants): source console clade trail from library CladeQuery"
```

---

## Task 3: Document the sourcing and the justified per-domain differences

**Files:**
- Modify: `domains/plants/CLAUDE.md`

- [ ] **Step 1: Add a clade-navigation note to `plants/CLAUDE.md`**

Under the clade-axis discussion (near the `placedIn` note), add a short subsection:

```markdown
### Console clade trail (Tree-of-Life row)

`plants-console` sources the Tree-of-Life trail from the shared library read
model — `library.CladeQuery.getBySlug(slug)` → `CladeView` → `List<CladeStep>`,
flattened `ancestry ++ subject` (root→subject) by `PlantsController.stepsFor` —
the same read model `insects-console` uses. Plants needs no anchor/override map
(insects' `InsectCladeAnchors`): the trail slug comes straight off the order's
`placedIn`, because plant clades are supra-ordinal.

Three differences from the insects clade UX are **intentional**, not drift, and
follow from order-only clade placement — do not "reconcile" them:

1. **Order-only placement** — every rank resolves its clade by walking up to its
   order; insects places at every rank.
2. **Kingdom-only anchor** — the rank breadcrumb is hand-anchored to a single
   `Plantae` "Kingdom" segment; plants has no Class rank, so there is no
   Kingdom/Phylum/Class derivation.
3. **Orders-only clade groupings** — the clade detail page groups Orders only
   (nothing is placed below Order); insects groups Orders/Families/Genera/Species.

Not adopted from insects (deferred, out of scope): the "not yet placed" gap
affordance and the ancestor-intro panels.
```

- [ ] **Step 2: Commit**

```bash
git add domains/plants/CLAUDE.md
git commit -m "docs(plants): record console clade-trail sourcing and justified UX differences"
```

---

## Self-Review

**Spec coverage:**
- Controller re-sourcing via `stepsFor` → Task 2, Step 4. ✓
- `cladeTrailFor` / `catalogCladeRoot` / `cladeDetail` rewrites → Task 2, Step 4. ✓
- Template param migration (`cladeTrail.jte`, `nav.jte`) → Task 1, Steps 3–4. ✓
- `PlantCladeTree` unchanged → not touched by any task. ✓
- `library-api` pom dependency → Task 2, Step 1. ✓
- Construction-site ripple: no `new PlantsController` exists (Spring-injected); template tests supply their own model, so only the four cladeTrail-referencing tests change → Task 1 Step 1, Task 2 Steps 2–3. ✓
- `orElseThrow` error handling mirroring insects → `stepsFor`, Task 2, Step 4. ✓
- Test asserting root→subject ordered slugs → Task 1, Step 1 (`trailRunsRootToSubjectEndingAtMagnoliids`). ✓
- Completeness gate (`mvn verify` + `rewrite:dryRun`) → Task 2, Steps 5–6. ✓
- `plants/CLAUDE.md` documentation of sourcing + three justified differences → Task 3. ✓

**Placeholder scan:** No TBD/TODO; every code step carries exact code. ✓

**Type consistency:** `stepsFor(Clade) : List<CladeStep>` used identically in `cladeTrailFor`, `catalogCladeRoot`, `cladeDetail`; `CladeStep(cladeSlug, displayName, rank)` constructed the same way in all three tests and the helper; `nav.jte`/`cladeTrail.jte` both declare `List<CladeStep> cladeTrail`; `current` stays `Clade` in both templates. ✓
```
