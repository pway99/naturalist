# Insect-Console Navigation Streamline — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the insect console's rank tab bar (tree-only navigation) and add a phylogenetic clade trail below the Linnaean-rank breadcrumb, with a "not yet placed" research affordance where the tree of life runs out.

**Architecture:** Presentation-only. PR 1 deletes the tab bar from the JTE chrome. PR 2 adds a second breadcrumb row driven by `library-api`'s existing `CladeQuery`; a console-side resolver (`InsectCladeAnchors`) maps each insect catalog entity to its deepest tree-of-life clade, and unmapped entities render a ghost node linking to a new `/concepts/placing-clades` guidance page.

**Tech Stack:** Java 21 records, JTE templates, CSS (single stylesheet), JUnit 5 + AssertJ, the `clades`/`taxonomy` kernels, and the `library` domain read surface (`CladeQuery`/`CladeView`/`CladeStep`).

**Design source:** [`docs/superpowers/specs/2026-07-02-insect-console-navigation-design.md`](../superpowers/specs/2026-07-02-insect-console-navigation-design.md)

## Global Constraints

- No changes to insect domain records, queries, repositories, or the `clades`/`taxonomy` kernels.
- No changes to `library-api` or `library-core` — the existing `CladeQuery.getBySlug` suffices.
- Clades are compile-time Java records; the gap affordance surfaces a research task, **not** a data-entry form. No persistence/contribution machinery.
- Flat `/families`, `/genera`, `/species` routes and templates stay working — only unlinked.
- `/insects` continues to redirect to `/insects/orders`.
- Builds run from the repo root as `mvn verify`. In this project **the user runs the build** — request it at each verify step rather than assuming a local invocation.
- Keep the standard `Co-Authored-By: Claude Fable 5` footer on commits.
- The clade slug convention is lowercase kebab and matches an insect entity's `name().value()` (e.g. order `coleoptera`, `lepidoptera`). `Clade.of(slug)` throws `IllegalArgumentException` for a non-clade slug; that is how "no clade" is detected.

---

# PR 1 — Tree-only navigation

## Task 1: Remove the rank tab bar

**Files:**
- Modify: `domains/insects/insects-console/src/main/jte/insects/nav.jte`
- Modify (10 callers): `insects/order.jte`, `family.jte`, `genus.jte`, `detail.jte`, `life-stages.jte`, `guild.jte`, `orders.jte`, `families.jte`, `genera.jte`, `list.jte` (all under `domains/insects/insects-console/src/main/jte/`)
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css`

**Interfaces:**
- Produces: `nav.jte` with signature `@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()` only (the `active` parameter is removed). PR 2, Task 5 adds a `cladeTrail` parameter to this same template.

- [ ] **Step 1: Rewrite `nav.jte` to drop the tab strip and the `active` param**

Replace the entire contents of `domains/insects/insects-console/src/main/jte/insects/nav.jte` with:

```jte
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()

<div class="insects-context-bar">
    @if(!breadcrumb.isEmpty())
        @template.insects.breadcrumb(segments = breadcrumb)
    @endif
</div>
```

- [ ] **Step 2: Remove `active = "..."` from every `nav` call site**

In each file below, change the `@template.insects.nav(...)` call from the old form to the new form. The only change is deleting the `active = "..."` argument.

| File (`domains/insects/insects-console/src/main/jte/`) | Old call | New call |
|---|---|---|
| `insects/order.jte` | `@template.insects.nav(active = "order", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/family.jte` | `@template.insects.nav(active = "family", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/genus.jte` | `@template.insects.nav(active = "genus", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/detail.jte` | `@template.insects.nav(active = "species", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/life-stages.jte` | `@template.insects.nav(active = "species", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/guild.jte` | `@template.insects.nav(active = "species", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/orders.jte` | `@template.insects.nav(active = "order", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/families.jte` | `@template.insects.nav(active = "family", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/genera.jte` | `@template.insects.nav(active = "genus", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |
| `insects/list.jte` | `@template.insects.nav(active = "species", breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb)` |

- [ ] **Step 3: Delete the orphaned tab CSS and the tab/breadcrumb divider**

In `apps/management-console/src/main/resources/static/css/naturalist.css`:

(a) Delete these four rule blocks entirely (currently around lines 1301–1334): `.insects-subnav`, `.insects-subnav a`, `.insects-subnav a:hover`, `.insects-subnav a[aria-current="page"]`. Also delete the two comment blocks that describe the sub-navigation (the `/* Insects sub-navigation: ... */` comment above `.insects-subnav`).

(b) Remove the tab/breadcrumb divider rule. Delete this block (currently ~lines 1247–1251) **and** its preceding comment (currently ~lines 1241–1246 describing "bottom row of the bar … separated from the rank tabs above by a thin sepia rule"):

```css
.insects-context-bar .breadcrumb-trail {
    padding: 0.4rem 0.75rem;
    border-top: 1px solid rgba(139, 107, 61, 0.35);
    margin: 0;
}
```

Replace it with the tab-free version (no top border, since nothing sits above the breadcrumb now):

```css
.insects-context-bar .breadcrumb-trail {
    padding: 0.4rem 0.75rem;
    margin: 0;
}
```

(c) Update the `.insects-context-bar` comment (currently ~lines 1283–1288) so it no longer claims to group a "rank tab strip". Replace that comment with:

```css
/* Insects context bar: warm-tan panel holding the taxonomic breadcrumb
   (and, below it, the phylogenetic clade trail). Renders only when a
   breadcrumb is present. */
```

Leave the `.insects-context-bar` rule body itself unchanged.

- [ ] **Step 4: Verify the build**

Run: `mvn verify` (from repo root; request the user run it).
Expected: BUILD SUCCESS. JTE precompilation of the 11 edited templates passes, and the existing `Insects*TemplateTest` classes still render (they assert non-blank output and pass no `active`, so removing the param cannot break them).

- [ ] **Step 5: Manual smoke check**

Start the console (request the user run it, or use the project `run` skill). Visit:
- `/insects` → redirects to `/insects/orders`; the four-tab strip (Orders/Families/Genera/Species) is **gone**; the breadcrumb (Animalia › Arthropoda › Insecta) remains.
- `/insects/orders/coleoptera` → no tab strip; breadcrumb ends at `Order Coleoptera`; "Families in this order" cards still navigate down.
- `/insects/families` → still reachable by direct URL, renders normally (just no inbound link).

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/nav.jte \
        domains/insects/insects-console/src/main/jte/insects/*.jte \
        apps/management-console/src/main/resources/static/css/naturalist.css
git commit -m "feat(insects-console): remove rank tab bar for tree-only navigation

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

# PR 2 — Phylogenetic clade trail

## Task 2: Declare the library read-surface dependencies

The `library-api` and `library-test-context` artifacts already reach `insects-console`
transitively through `insects-test-context`, but this task consumes their types
(`CladeQuery`, `CladeView`, `CladeStep`, `LibraryTestContext`) directly, so declare
them as first-class dependencies. Both are already in the root `<dependencyManagement>`,
so no version tag is needed.

**Files:**
- Modify: `domains/insects/insects-console/pom.xml`

- [ ] **Step 1: Add the two dependencies**

In `domains/insects/insects-console/pom.xml`, inside `<dependencies>`, add after the
existing `insects-api` dependency:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-test-context</artifactId>
        </dependency>
```

- [ ] **Step 2: Verify it resolves**

Run: `mvn -q -pl domains/insects/insects-console -am dependency:resolve` (request the user run it), or simply proceed — the next task's compile will confirm. Expected: no missing-artifact errors.

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-console/pom.xml
git commit -m "build(insects-console): declare library-api and library-test-context deps

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 3: Clade-trail view model and anchor resolver

Produces the two types the controller and template need, with a resolver that maps a
catalog lineage to its deepest tree-of-life clade and detects the gap. TDD.

**Files:**
- Create: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/CladeTrail.java`
- Create: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectCladeAnchors.java`
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectCladeAnchorsTest.java`

**Interfaces:**
- Consumes: `com.naturalist.clades.Clade` (kernel, already on classpath via `insects-api`); `com.naturalist.library.CladeStep` (for `CladeTrail`).
- Produces:
  - `CladeTrail(List<CladeStep> steps, String gapLabel)` with `boolean hasGap()`.
  - `InsectCladeAnchors.LineageEntry(String slug, String displayName)` — one catalog entity.
  - `InsectCladeAnchors.Anchor(String cladeSlug, String gapLabel)`.
  - `static Anchor InsectCladeAnchors.resolve(List<LineageEntry> lineage)` — `lineage` is **current entity first** (deepest), ancestors after; empty for rank-list pages. `gapLabel` is `null` when the current entity itself maps to a clade.
  - Task 4 calls `resolve(...)`; Task 5 renders `CladeTrail`.

- [ ] **Step 1: Write the failing resolver test**

Create `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectCladeAnchorsTest.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.insects.console.InsectCladeAnchors.Anchor;
import com.naturalist.insects.console.InsectCladeAnchors.LineageEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsectCladeAnchorsTest {

    @Test
    void emptyLineageAnchorsAtInsectaWithNoGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of());
        assertThat(anchor.cladeSlug()).isEqualTo("insecta");
        assertThat(anchor.gapLabel()).isNull();
    }

    @Test
    void mappedOrderResolvesToItsCladeWithNoGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("lepidoptera", "Lepidoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("lepidoptera");
        assertThat(anchor.gapLabel()).isNull();
    }

    @Test
    void unmappedOrderFallsToInsectaWithGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("coleoptera", "Coleoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("insecta");
        assertThat(anchor.gapLabel()).isEqualTo("Coleoptera");
    }

    @Test
    void curatedOverrideResolvesCurrentEntityWithNoGap() {
        // Genus battus has no "battus" clade, but is curated to tribe Troidini.
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("battus", "Battus")));
        assertThat(anchor.cladeSlug()).isEqualTo("troidini");
        assertThat(anchor.gapLabel()).isNull();
    }

    @Test
    void deepGapAnchorsAtNearestMappedAncestor() {
        // Species/genus/family unmapped; order lepidoptera is the mapped ancestor.
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("vanessa-cardui", "Vanessa cardui"),
                new LineageEntry("vanessa", "Vanessa"),
                new LineageEntry("nymphalidae", "Nymphalidae"),
                new LineageEntry("lepidoptera", "Lepidoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("lepidoptera");
        assertThat(anchor.gapLabel()).isEqualTo("Vanessa cardui");
    }

    @Test
    void fullyUnmappedLineageFallsToInsectaWithCurrentEntityGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("harpalus-affinis", "Harpalus affinis"),
                new LineageEntry("harpalus", "Harpalus"),
                new LineageEntry("carabidae", "Carabidae"),
                new LineageEntry("coleoptera", "Coleoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("insecta");
        assertThat(anchor.gapLabel()).isEqualTo("Harpalus affinis");
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `mvn -q -pl domains/insects/insects-console test -Dtest=InsectCladeAnchorsTest` (request the user run it).
Expected: COMPILE FAILURE — `InsectCladeAnchors` and `CladeTrail` do not exist yet.

- [ ] **Step 3: Create `CladeTrail`**

Create `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/CladeTrail.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.library.CladeStep;

import java.util.List;

/**
 * The phylogenetic breadcrumb row for one insect page: the real clade nodes
 * (root → subject) plus an optional gap label naming the current catalog entity
 * when it has no clade of its own.
 */
public record CladeTrail(List<CladeStep> steps, String gapLabel) {

    public boolean hasGap() {
        return gapLabel != null && !gapLabel.isBlank();
    }
}
```

- [ ] **Step 4: Create `InsectCladeAnchors`**

Create `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectCladeAnchors.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.clades.Clade;

import java.util.List;
import java.util.Map;

/**
 * Resolves the deepest tree-of-life clade for an insect catalog lineage.
 *
 * <p>The catalog (Linnaean ranks) and the clade tree (evolutionary lineages)
 * diverge: many catalogued taxa have no clade node (e.g. order Coleoptera),
 * and some map to a clade under a different slug (e.g. genus {@code battus} →
 * tribe {@code troidini}). Slug-equality resolves the common case; the curated
 * {@link #OVERRIDE} map covers the divergent ones. A catalog entity with no
 * clade of its own is a genuine gap — a research invitation, not an error.
 */
final class InsectCladeAnchors {

    /**
     * Catalog slug → clade slug, for entities whose clade lives under a
     * different name. Extend as the naturalist maps more lineages.
     */
    private static final Map<String, String> OVERRIDE = Map.of(
            "battus", "troidini"
    );

    /** Every catalogued insect sits at least within class Insecta. */
    private static final String FLOOR = "insecta";

    private InsectCladeAnchors() {
    }

    /**
     * @param lineage current entity first (deepest), then each ancestor; empty
     *                for rank-list pages.
     * @return the anchor clade slug plus a gap label (the current entity's
     *         display name) when the current entity itself is unmapped.
     */
    static Anchor resolve(List<LineageEntry> lineage) {
        for (int i = 0; i < lineage.size(); i++) {
            String cladeSlug = OVERRIDE.getOrDefault(lineage.get(i).slug(), lineage.get(i).slug());
            if (isClade(cladeSlug)) {
                String gapLabel = (i == 0) ? null : lineage.get(0).displayName();
                return new Anchor(cladeSlug, gapLabel);
            }
        }
        String gapLabel = lineage.isEmpty() ? null : lineage.get(0).displayName();
        return new Anchor(FLOOR, gapLabel);
    }

    private static boolean isClade(String slug) {
        try {
            Clade.of(slug);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    record LineageEntry(String slug, String displayName) {
    }

    record Anchor(String cladeSlug, String gapLabel) {
    }
}
```

- [ ] **Step 5: Run the test to confirm it passes**

Run: `mvn -q -pl domains/insects/insects-console test -Dtest=InsectCladeAnchorsTest` (request the user run it).
Expected: PASS (6 tests). If `Clade.of` rejects a slug you expected to map (e.g. `nymphalidae` is not a clade — that is correct, it is meant to fall through), the deep-gap test still anchors at `lepidoptera`.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/CladeTrail.java \
        domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectCladeAnchors.java \
        domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectCladeAnchorsTest.java
git commit -m "feat(insects-console): add clade-trail resolver and view model

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 4: Wire `CladeQuery` and clade-trail attributes into the controller

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

**Interfaces:**
- Consumes: `InsectCladeAnchors.resolve(...)`, `CladeTrail`, `com.naturalist.library.CladeQuery`, `com.naturalist.library.CladeView`, `com.naturalist.library.CladeStep`, `com.naturalist.library.LibraryTestContext`.
- Produces: a `"cladeTrail"` model attribute of type `CladeTrail` on every view-rendering route. Task 5's templates read it.

- [ ] **Step 1: Add imports**

In `InsectsController.java`, add to the import block:

```java
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeStep;
import com.naturalist.library.CladeView;
import com.naturalist.library.LibraryTestContext;
```

- [ ] **Step 2: Add the `cladeQuery` field and construct it**

Add a field alongside the other query fields:

```java
    private final CladeQuery cladeQuery;
```

In the constructor, after the existing `InsectsTestContext` wiring, add:

```java
        // TODO:: This will eventually be a spring managed bean
        this.cladeQuery = LibraryTestContext.create(NaturalistDatabase.create()).cladeQuery();
```

- [ ] **Step 3: Add the trail-building helpers**

Add these private methods (near the existing `breadcrumbTo*` / `intro*` helpers):

```java
    private CladeTrail cladeTrail(List<InsectCladeAnchors.LineageEntry> lineage) {
        InsectCladeAnchors.Anchor anchor = InsectCladeAnchors.resolve(lineage);
        CladeView view = cladeQuery.getBySlug(anchor.cladeSlug()).orElseThrow(
                () -> new IllegalStateException("anchor clade not found: " + anchor.cladeSlug()));
        List<CladeStep> steps = new ArrayList<>(view.ancestry());
        steps.add(view.subject());
        return new CladeTrail(steps, anchor.gapLabel());
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToOrder(InsectOrder order) {
        return List.of(new InsectCladeAnchors.LineageEntry(
                order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToFamily(InsectFamily family, InsectOrder order) {
        return List.of(
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToGenus(InsectGenus genus,
                                                                 InsectFamily family,
                                                                 InsectOrder order) {
        return List.of(
                new InsectCladeAnchors.LineageEntry(genus.name().value(), genus.genus().value()),
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToSpecies(InsectSpecies species,
                                                                   InsectGenus genus,
                                                                   InsectFamily family,
                                                                   InsectOrder order) {
        String binomial = genus.genus().value() + " " + species.epithet().value();
        return List.of(
                new InsectCladeAnchors.LineageEntry(species.name().value(), binomial),
                new InsectCladeAnchors.LineageEntry(genus.name().value(), genus.genus().value()),
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }
```

- [ ] **Step 4: Populate `cladeTrail` on every view route**

For each route, add a `model.addAttribute("cladeTrail", ...)` line immediately after the
existing `model.addAttribute("breadcrumb", ...)` line. Rank-list pages and the guild page
use an empty lineage (anchor = Insecta, no gap); detail pages reuse the same entities they
already pass to `breadcrumbTo*`.

| Route (method) | Existing breadcrumb line | Add immediately after |
|---|---|---|
| `GET /orders` (list) | `model.addAttribute("breadcrumb", cladePrefix());` | `model.addAttribute("cladeTrail", cladeTrail(List.of()));` |
| `GET /families` (list) | `model.addAttribute("breadcrumb", cladePrefix());` | `model.addAttribute("cladeTrail", cladeTrail(List.of()));` |
| `GET /genera` (list) | `model.addAttribute("breadcrumb", cladePrefix());` | `model.addAttribute("cladeTrail", cladeTrail(List.of()));` |
| `GET /species` (list) | `model.addAttribute("breadcrumb", cladePrefix());` | `model.addAttribute("cladeTrail", cladeTrail(List.of()));` |
| `GET /guild/{guild}` | `model.addAttribute("breadcrumb", cladePrefix());` | `model.addAttribute("cladeTrail", cladeTrail(List.of()));` |
| `GET /orders/{name}` | `model.addAttribute("breadcrumb", breadcrumbToOrder(order));` | `model.addAttribute("cladeTrail", cladeTrail(lineageToOrder(order)));` |
| `GET /families/{name}` | `model.addAttribute("breadcrumb", breadcrumbToFamily(family, order));` | `model.addAttribute("cladeTrail", cladeTrail(lineageToFamily(family, order)));` |
| `GET /genera/{name}` | `model.addAttribute("breadcrumb", breadcrumbToGenus(genus, family, order));` | `model.addAttribute("cladeTrail", cladeTrail(lineageToGenus(genus, family, order)));` |
| `GET /{name}` (species detail) | `model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));` | `model.addAttribute("cladeTrail", cladeTrail(lineageToSpecies(s, genus, family, order)));` |
| `GET /{name}/life-stages` | `model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));` | `model.addAttribute("cladeTrail", cladeTrail(lineageToSpecies(s, genus, family, order)));` |

Note: the two species routes bind the species variable as `s` (per the existing code). Match the local variable names actually in scope in each method; the entities are the same ones already handed to `breadcrumbToSpecies`.

- [ ] **Step 5: Verify compilation**

Run: `mvn -q -pl domains/insects/insects-console -am compile` (request the user run it).
Expected: BUILD SUCCESS. (Templates do not yet read `cladeTrail`; that is Task 5. The attribute is simply present in the model.)

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "feat(insects-console): supply phylogenetic clade trail per route

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 5: Render the clade trail beneath the breadcrumb

**Files:**
- Create: `domains/insects/insects-console/src/main/jte/insects/cladeTrail.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/nav.jte`
- Modify (10 callers): the same page templates as Task 1 (they must declare and forward the new param)
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css`
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsCladeTrailTemplateTest.java`

**Interfaces:**
- Consumes: `CladeTrail` (Task 3), `com.naturalist.library.CladeStep`.
- Produces: `nav.jte` gains `@param CladeTrail cladeTrail = null`.

- [ ] **Step 1: Create `cladeTrail.jte`**

Create `domains/insects/insects-console/src/main/jte/insects/cladeTrail.jte`:

```jte
@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.library.CladeStep

@param CladeTrail trail

<nav class="clade-trail" aria-label="Phylogenetic lineage">
    @for(int i = 0; i < trail.steps().size(); i++)
        @if(i > 0)
            <span class="clade-trail-sep" aria-hidden="true">›</span>
        @endif
        !{var step = trail.steps().get(i);}
        <a class="clade-trail-node" href="/clades/${step.cladeSlug()}">${step.displayName()}</a>
    @endfor
    @if(trail.hasGap())
        <span class="clade-trail-sep" aria-hidden="true">·</span>
        <a class="clade-trail-gap" href="/concepts/placing-clades"
           title="This lineage isn't placed in the tree of life yet — research and help map it">
            ${trail.gapLabel()} — not yet placed
        </a>
    @endif
</nav>
```

- [ ] **Step 2: Update `nav.jte` to accept and render the trail**

Replace the contents of `domains/insects/insects-console/src/main/jte/insects/nav.jte`
(the Task 1 version) with:

```jte
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param CladeTrail cladeTrail = null

<div class="insects-context-bar">
    @if(!breadcrumb.isEmpty())
        @template.insects.breadcrumb(segments = breadcrumb)
    @endif
    @if(cladeTrail != null)
        @template.insects.cladeTrail(trail = cladeTrail)
    @endif
</div>
```

- [ ] **Step 3: Forward `cladeTrail` from every page template**

In each of the 10 caller templates, (a) add the import `@import com.naturalist.insects.console.CladeTrail`, (b) add the param declaration `@param CladeTrail cladeTrail = null` alongside the existing `@param List<BreadcrumbSegment> breadcrumb = List.of()`, and (c) change the nav call to forward it.

The nav-call change is identical in every file:

| File (`domains/insects/insects-console/src/main/jte/`) | From (PR 1 state) | To |
|---|---|---|
| `insects/order.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/family.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/genus.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/detail.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/life-stages.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/guild.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/orders.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/families.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/genera.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |
| `insects/list.jte` | `@template.insects.nav(breadcrumb = breadcrumb)` | `@template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)` |

The default `= null` on each page param keeps the existing `Insects*TemplateTest` classes
(which pass no `cladeTrail`) rendering without change — `nav.jte` guards on `!= null`.

- [ ] **Step 4: Add the clade-trail CSS**

In `apps/management-console/src/main/resources/static/css/naturalist.css`, add (adjacent to the
`.breadcrumb-trail` rules) a single-row sepia trail with a dashed, muted ghost node:

```css
/* Phylogenetic clade trail — a single field-guide row beneath the Linnaean
   breadcrumb. Nodes link into /clades; the ghost node marks a lineage the
   tree of life has not mapped yet and links to research guidance. */
.clade-trail {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    justify-content: flex-start;
    gap: 0.3rem 0.5rem;
    font-size: 0.9rem;
    font-style: italic;
    color: var(--sepia-ink-soft);
    padding: 0.35rem 0.75rem 0.5rem;
    margin: 0;
}

.clade-trail-node {
    color: var(--sepia-ink-soft);
    text-decoration: none;
    border-bottom: 1px dotted var(--sepia-rule);
}

.clade-trail-node:hover {
    color: var(--pico-primary);
    border-bottom-color: var(--pico-primary);
}

.clade-trail-sep {
    color: rgba(58, 42, 24, 0.35);
}

.clade-trail-gap {
    color: rgba(58, 42, 24, 0.55);
    font-style: italic;
    text-decoration: none;
    border-bottom: 1px dashed rgba(58, 42, 24, 0.4);
}

.clade-trail-gap:hover {
    color: var(--pico-primary);
    border-bottom-color: var(--pico-primary);
}
```

- [ ] **Step 5: Write a rendering test for the clade trail**

Create `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsCladeTrailTemplateTest.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.library.CladeStep;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsCladeTrailTemplateTest {

    @Test
    void rendersCladeNodesLinkingIntoClades() {
        CladeTrail trail = new CladeTrail(List.of(
                new CladeStep("eukaryota", "Eukaryota", Optional.empty()),
                new CladeStep("insecta", "Insecta", Optional.empty())), null);
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/cladeTrail.jte", Map.of("trail", trail), output);

        String html = output.toString();
        assertThat(html).contains("href=\"/clades/eukaryota\"");
        assertThat(html).contains("href=\"/clades/insecta\"");
        assertThat(html).doesNotContain("not yet placed");
    }

    @Test
    void rendersGhostNodeForUnmappedEntity() {
        CladeTrail trail = new CladeTrail(List.of(
                new CladeStep("insecta", "Insecta", Optional.empty())), "Coleoptera");
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/cladeTrail.jte", Map.of("trail", trail), output);

        String html = output.toString();
        assertThat(html).contains("Coleoptera — not yet placed");
        assertThat(html).contains("href=\"/concepts/placing-clades\"");
    }
}
```

- [ ] **Step 6: Run the template tests**

Run: `mvn -q -pl domains/insects/insects-console test -Dtest=InsectsCladeTrailTemplateTest` (request the user run it).
Expected: PASS (2 tests).

- [ ] **Step 7: Full build**

Run: `mvn verify` (from repo root; request the user run it).
Expected: BUILD SUCCESS — all JTE templates (including the 10 callers now forwarding `cladeTrail`) precompile, and every existing template test still passes.

- [ ] **Step 8: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/cladeTrail.jte \
        domains/insects/insects-console/src/main/jte/insects/nav.jte \
        domains/insects/insects-console/src/main/jte/insects/*.jte \
        apps/management-console/src/main/resources/static/css/naturalist.css \
        domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsCladeTrailTemplateTest.java
git commit -m "feat(insects-console): render phylogenetic clade trail with gap affordance

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

## Task 6: Add the `placing-clades` guidance concept

The ghost node links to `/concepts/placing-clades`. The `/concepts/{slug}` route already
serves any entry in the library concept catalog, so this task only adds catalog content.

**Files:**
- Modify: `domains/library/library-repository-test/src/main/resources/library/concepts.json`

- [ ] **Step 1: Append the concept entry**

In `domains/library/library-repository-test/src/main/resources/library/concepts.json`
(a top-level JSON array), add this object as a new element (keep the file valid JSON —
add a comma after the previous last element). Preserve UTF-8 punctuation literally; do not
escape the em-dashes.

```json
  {
    "name": "placing-clades",
    "title": "When the tree of life runs out",
    "description": {
      "preschool": "Scientists have not drawn every branch of the tree of life yet. When a bug's branch is missing, that is a job waiting for a naturalist — you can go find out where it belongs and help draw it in.",
      "elementary": "The tree of life is still being built. For some insects nobody has worked out exactly which branch they grow from, so their place on the tree is blank. A blank is not a mistake — it is an invitation. A naturalist can read about the animal, compare it to its relatives, and figure out where its branch should join. Every filled-in branch makes the whole tree better.",
      "secondary": "A gap in the phylogeny means no clade has been curated for that taxon yet — its evolutionary placement is unresolved in our catalogue, even though its Linnaean rank (order, family, genus) is known. Resolving a gap is real scientific work: gather the taxon's diagnostic traits, find the shared derived characters (synapomorphies) that unite it with a known clade, check the current literature, and propose the branch. The Linnaean shelf tells you where a specimen is filed; the clade tells you where it came from, and the two do not always line up.",
      "university": "An unmapped taxon marks the boundary of the curated phylogeny: a node present in the Linnaean classification for which no monophyletic hypothesis has been entered. Closing the gap is a systematics task — assemble character-state data, evaluate competing topologies against the published phylogenetic evidence, and place the taxon on the most strongly supported branch. Because clade membership is inferred rather than stipulated, a proposed placement is a testable hypothesis, revised as evidence accrues. Surfacing these gaps in the field guide turns passive browsing into an agenda for research."
    }
  }
```

- [ ] **Step 2: Verify the JSON parses and the page renders**

Run: `mvn verify` (from repo root; request the user run it). Expected: BUILD SUCCESS
(the concept `TestEntitySource` loads the catalog at startup; malformed JSON fails the build).

Then smoke-check (request the user run the console): visit `/insects/orders/coleoptera` →
the clade trail shows `… › Insecta · Coleoptera — not yet placed`; clicking the ghost node
opens `/concepts/placing-clades` and renders the four reading levels. Visit
`/insects/orders/lepidoptera` → the trail descends to `Lepidoptera` with **no** ghost node.

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-repository-test/src/main/resources/library/concepts.json
git commit -m "content(library): add placing-clades research-gap guidance concept

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review Notes (author)

- **Spec coverage:** tab-bar removal (Task 1), flat routes kept-but-unlinked (Task 1, no route edits), single clade row (Task 5), curated correspondence with slug-equality + override (Task 3), gap = ghost node + guidance link (Tasks 5–6), library `CladeQuery` reuse with no library changes (Tasks 2, 4), Insecta floor (Task 3). All present.
- **Type consistency:** `CladeTrail(List<CladeStep>, String)` / `hasGap()`; `InsectCladeAnchors.resolve(List<LineageEntry>) → Anchor(cladeSlug, gapLabel)`; `LineageEntry(slug, displayName)` — used identically across Tasks 3–5.
- **Known non-blocking wart:** `nav.jte` keeps its filename though it is now breadcrumb+trail chrome, not navigation (per design). Rename deferred.
- **Deferred (out of scope, per spec):** deep-linking the gap to external-authority (EOL) search; retiring row 1's `cladePrefix()` in favour of `CladeQuery`.
