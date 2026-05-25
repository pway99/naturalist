# Taxonomic-Scope Breadcrumb Primitive — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify two separate clade/taxonomy rendering paths into a single breadcrumb primitive that renders the full taxonomic lineage trail on all insect console pages.

**Architecture:** A `BreadcrumbSegment` record carries `(label, url, currentPage)` tuples. A `breadcrumb.jte` template renders them as a `<nav>` trail. Controller helper methods assemble segments from entities already fetched. Two existing templates delegate their lineage rendering to the new primitive: `cladeIntro.jte` (listing pages) and `nav.jte` (detail pages).

**Tech Stack:** Java 21 records, JTE templates, CSS, Spring MVC controller

**Spec:** [`docs/superpowers/specs/2026-05-25-breadcrumb-primitive-design.md`](../superpowers/specs/2026-05-25-breadcrumb-primitive-design.md)

---

### Context for the implementer

Two templates currently render the clade lineage (`Animalia › Arthropoda › Insecta`) independently:

1. **`cladeIntro.jte`** — used by listing pages (`orders.jte`, `families.jte`, `genera.jte`, `list.jte`). Renders clade lineage + a collapsible description block. Does NOT contain the subnav tabs — those are inlined in each listing template.
2. **`nav.jte`** — used by detail pages (`order.jte`, `family.jte`, `genus.jte`, `detail.jte`, `life-stages.jte`). Renders clade lineage + the subnav tabs. Does NOT contain a description block.

Detail pages additionally show ad-hoc taxonomy links/text in their own templates (e.g. `<p class="taxonomy">order · family</p>`), separate from `nav.jte`.

After this plan:
- Both `cladeIntro.jte` and `nav.jte` delegate their lineage rendering to `breadcrumb.jte`
- Detail pages get an extended breadcrumb (clade + rank segments) and their ad-hoc taxonomy is removed
- Listing pages get the same clade-only breadcrumb they had before, just rendered by the new template

**Out of scope:** `life-stages.jte` — it has a distinct "plate" layout with a different header structure. The breadcrumb can be added there in a follow-up if wanted.

---

### Task 1: Foundation — BreadcrumbSegment record + breadcrumb.jte + CSS

**Files:**
- Create: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/BreadcrumbSegment.java`
- Create: `domains/insects/insects-console/src/main/jte/insects/breadcrumb.jte`
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css:1226-1231`

No tests — this is a rendering DTO with no invariants and a JTE template. Both are exercised by the pages that consume them in Tasks 2 and 3.

- [ ] **Step 1: Create BreadcrumbSegment record**

Create `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/BreadcrumbSegment.java`:

```java
package com.naturalist.insects.console;

public record BreadcrumbSegment(String label, String url, boolean currentPage) {

    public static BreadcrumbSegment link(String label, String url) {
        return new BreadcrumbSegment(label, url, false);
    }

    public static BreadcrumbSegment text(String label) {
        return new BreadcrumbSegment(label, null, false);
    }

    public static BreadcrumbSegment current(String label) {
        return new BreadcrumbSegment(label, null, true);
    }
}
```

- [ ] **Step 2: Create breadcrumb.jte template**

Create `domains/insects/insects-console/src/main/jte/insects/breadcrumb.jte`:

```html
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> segments

<nav class="breadcrumb-trail" aria-label="Taxonomic placement">
    @for(int i = 0; i < segments.size(); i++)
        @if(i > 0)
            <span aria-hidden="true"> › </span>
        @endif
        !{var s = segments.get(i);}
        @if(s.currentPage())
            <span class="breadcrumb-current"><strong>${s.label()}</strong></span>
        @elseif(s.url() != null)
            <a href="${s.url()}">${s.label()}</a>
        @else
            <span>${s.label()}</span>
        @endif
    @endfor
</nav>
```

- [ ] **Step 3: Add .breadcrumb-trail CSS rules**

In `apps/management-console/src/main/resources/static/css/naturalist.css`, add the new rules immediately after the existing `.clade-lineage` block (after line 1231). Do NOT remove `.clade-lineage` yet — it is still referenced by `cladeIntro.jte` and `nav.jte` until Tasks 2 and 3 migrate them.

```css
.breadcrumb-trail {
    font-size: 0.85rem;
    color: var(--pico-muted-color);
    margin: 0 0 0.25rem;
    text-shadow: var(--text-shadow-subtle);
}

.breadcrumb-trail a {
    color: var(--pico-muted-color);
    text-decoration: none;
}

.breadcrumb-trail a:hover {
    text-decoration: underline;
}
```

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/BreadcrumbSegment.java
git add domains/insects/insects-console/src/main/jte/insects/breadcrumb.jte
git add apps/management-console/src/main/resources/static/css/naturalist.css
git commit -m "Add BreadcrumbSegment record, breadcrumb.jte template, and CSS"
```

---

### Task 2: Listing pages — delegate clade lineage to breadcrumb

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/cladeIntro.jte`

- [ ] **Step 1: Add cladePrefix() helper and update addCladeDescription()**

In `InsectsController.java`, add a new import at the top:

```java
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
```

Add a `cladePrefix()` helper method (place it near the existing `addCladeDescription` method, around line 70):

```java
private List<BreadcrumbSegment> cladePrefix() {
    return CladeTraversal.ancestry(new Insecta()).reversed().stream()
            .filter(c -> !(c instanceof Eukaryota))
            .map(c -> BreadcrumbSegment.text(c.displayName()))
            .toList();
}
```

Then modify `addCladeDescription(Model model)` (line 64) to also add the breadcrumb:

```java
private void addCladeDescription(Model model) {
    var d = new Insecta().description();
    model.addAttribute("cladePreschool", descriptionRenderer.render(d.preschool()));
    model.addAttribute("cladeElementary", descriptionRenderer.render(d.elementary()));
    model.addAttribute("cladeSecondary", descriptionRenderer.render(d.secondary()));
    model.addAttribute("cladeUniversity", descriptionRenderer.render(d.university()));
    model.addAttribute("breadcrumb", cladePrefix());
}
```

The four listing handlers (`list`, `families`, `orders`, `genera`) already call `addCladeDescription(model)`, so they all pick up the breadcrumb attribute automatically. No handler changes needed.

- [ ] **Step 2: Update cladeIntro.jte to delegate lineage to breadcrumb.jte**

Replace the entire content of `domains/insects/insects-console/src/main/jte/insects/cladeIntro.jte` with:

```html
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb
@param String cladePreschool = ""
@param String cladeElementary = ""
@param String cladeSecondary = ""
@param String cladeUniversity = ""

@template.insects.breadcrumb(segments = breadcrumb)
<details class="clade-introduction" id="clade-intro">
    <summary><h2>Class Insecta</h2></summary>
    @template.components.description(
        preschool = cladePreschool,
        elementary = cladeElementary,
        secondary = cladeSecondary,
        university = cladeUniversity)
</details>
<script>
    (function () {
        var el = document.getElementById('clade-intro');
        if (!el) return;
        var stored = localStorage.getItem('clade-intro-open');
        if (stored !== 'false') el.open = true;
        el.addEventListener('toggle', function () {
            localStorage.setItem('clade-intro-open', el.open);
        });
    }());
</script>
```

Changes from the original:
- Removed `CladeTraversal`, `Eukaryota`, `Insecta` imports
- Removed the inline `!{var ancestry = ...}` and `!{var display = ...}` logic (lines 10-11)
- Removed the `<div class="clade-lineage">` loop (lines 12-19)
- Added `@param List<BreadcrumbSegment> breadcrumb`
- Added `@template.insects.breadcrumb(segments = breadcrumb)` call
- Everything else (description block, script) unchanged

- [ ] **Step 3: Update listing template cladeIntro calls to pass breadcrumb**

Each of the four listing templates calls `@template.insects.cladeIntro(...)`. Add the `breadcrumb` parameter to each call.

In `orders.jte` (line 13), change:
```
@template.insects.cladeIntro(
    cladePreschool = cladePreschool,
    cladeElementary = cladeElementary,
    cladeSecondary = cladeSecondary,
    cladeUniversity = cladeUniversity)
```
to:
```
@template.insects.cladeIntro(
    breadcrumb = breadcrumb,
    cladePreschool = cladePreschool,
    cladeElementary = cladeElementary,
    cladeSecondary = cladeSecondary,
    cladeUniversity = cladeUniversity)
```

Repeat the same change in `families.jte` (line 17), `genera.jte` (line 17), and `list.jte` (line 25). Each one adds `breadcrumb = breadcrumb,` as the first argument.

- [ ] **Step 4: Verify listing pages compile**

The user runs `mvn verify` from the repo root. Expected: all tests pass, no compilation errors. The listing pages now render the breadcrumb via the new primitive; the visual output is identical (same text, same separator, same styling class will match once `.clade-lineage` is removed and `.breadcrumb-trail` takes over — but the new CSS is already present).

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/cladeIntro.jte
git add domains/insects/insects-console/src/main/jte/insects/orders.jte
git add domains/insects/insects-console/src/main/jte/insects/families.jte
git add domains/insects/insects-console/src/main/jte/insects/genera.jte
git add domains/insects/insects-console/src/main/jte/insects/list.jte
git commit -m "Listing pages: delegate clade lineage to breadcrumb primitive"
```

---

### Task 3: Detail pages — replace ad-hoc taxonomy with breadcrumb

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/nav.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/order.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/family.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/genus.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte`
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css`

- [ ] **Step 1: Add breadcrumb assembly helpers to InsectsController**

Add four private methods after `cladePrefix()` in `InsectsController.java`:

```java
private List<BreadcrumbSegment> breadcrumbToOrder(InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.current(order.order().value()));
    return segments;
}

private List<BreadcrumbSegment> breadcrumbToFamily(InsectFamily family, InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.link(order.order().value(),
            "/insects/orders/" + order.name().value()));
    segments.add(BreadcrumbSegment.current(family.family().value()));
    return segments;
}

private List<BreadcrumbSegment> breadcrumbToGenus(InsectGenus genus,
                                                  InsectFamily family,
                                                  InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.link(order.order().value(),
            "/insects/orders/" + order.name().value()));
    segments.add(BreadcrumbSegment.link(family.family().value(),
            "/insects/families/" + family.name().value()));
    segments.add(BreadcrumbSegment.current(genus.genus().value()));
    return segments;
}

private List<BreadcrumbSegment> breadcrumbToSpecies(InsectSpecies species,
                                                    InsectGenus genus,
                                                    InsectFamily family,
                                                    InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.link(order.order().value(),
            "/insects/orders/" + order.name().value()));
    segments.add(BreadcrumbSegment.link(family.family().value(),
            "/insects/families/" + family.name().value()));
    segments.add(BreadcrumbSegment.link(genus.genus().value(),
            "/insects/genera/" + genus.name().value()));
    segments.add(BreadcrumbSegment.current(
            genus.genus().value() + " " + species.epithet().value()));
    return segments;
}
```

- [ ] **Step 2: Add breadcrumb model attributes to detail handlers**

In `orderDetail()` (line 167), add after the gallery attribute (line 183):

```java
model.addAttribute("breadcrumb", breadcrumbToOrder(order.get()));
```

In `familyDetail()` (line 124), add after the gallery attribute (line 140):

```java
model.addAttribute("breadcrumb", breadcrumbToFamily(family.get(), order));
```

In `genusDetail()` (line 214), add after the gallery attribute (line 234):

```java
model.addAttribute("breadcrumb", breadcrumbToGenus(genus.get(), family, order));
```

In `detail()` (line 264), add after the images attribute (line 281):

```java
model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));
```

- [ ] **Step 3: Update nav.jte to render breadcrumb instead of inline clade lineage**

Replace the entire content of `domains/insects/insects-console/src/main/jte/insects/nav.jte` with:

```html
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param String active = ""
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()

@if(!breadcrumb.isEmpty())
    @template.insects.breadcrumb(segments = breadcrumb)
@endif
<nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
    <a href="/insects/orders" aria-current="${"order".equals(active) ? "page" : "false"}">Order</a>
    <a href="/insects/families" aria-current="${"family".equals(active) ? "page" : "false"}">Family</a>
    <a href="/insects/genera" aria-current="${"genus".equals(active) ? "page" : "false"}">Genus</a>
    <a href="/insects/species" aria-current="${"species".equals(active) ? "page" : "false"}">Species</a>
</nav>
```

Changes from the original:
- Removed `Clade`, `CladeTraversal`, `Eukaryota`, `Insecta` imports
- Removed the inline `!{var ancestry = ...}` and `!{var display = ...}` logic
- Removed the `<div class="clade-lineage">` loop
- Added `@param List<BreadcrumbSegment> breadcrumb` with empty-list default (so `life-stages.jte` and any other caller that doesn't pass breadcrumb still works)
- Added conditional `@template.insects.breadcrumb(...)` call

- [ ] **Step 4: Update detail template nav calls to pass breadcrumb**

In `order.jte` (line 15), change:
```
@template.insects.nav(active = "order")
```
to:
```
@template.insects.nav(active = "order", breadcrumb = breadcrumb)
```

In `family.jte` (line 17), change:
```
@template.insects.nav(active = "family")
```
to:
```
@template.insects.nav(active = "family", breadcrumb = breadcrumb)
```

In `genus.jte` (line 19), change:
```
@template.insects.nav(active = "genus")
```
to:
```
@template.insects.nav(active = "genus", breadcrumb = breadcrumb)
```

In `detail.jte` (line 23), change:
```
@template.insects.nav(active = "species")
```
to:
```
@template.insects.nav(active = "species", breadcrumb = breadcrumb)
```

- [ ] **Step 5: Remove ad-hoc taxonomy from detail templates**

In `family.jte`, delete line 19:
```html
    <p class="taxonomy"><a href="/insects/orders/${order.name().value()}">${order.order().value()}</a></p>
```

In `genus.jte`, delete lines 21-24:
```html
    <p class="taxonomy">
        <a href="/insects/orders/${order.name().value()}">${order.order().value()}</a> &middot;
        <a href="/insects/families/${genus.familyName().value()}">${family.family().value()}</a>
    </p>
```

In `detail.jte`, delete lines 34-37:
```html
    <p class="taxonomy">
        ${order.order().value()} &middot; ${family.family().value()}
        &middot; <em>${genus.genus().value()} ${species.epithet().value()}</em>
    </p>
```

`order.jte` has no ad-hoc taxonomy to remove — it only had an `<h1>`.

- [ ] **Step 6: Remove old .clade-lineage CSS rule**

In `apps/management-console/src/main/resources/static/css/naturalist.css`, delete the `.clade-lineage` block (lines 1226-1231):

```css
.clade-lineage {
    font-size: 0.85rem;
    color: var(--pico-muted-color);
    margin: 0 0 0.25rem;
    text-shadow: var(--text-shadow-subtle);
}
```

No template references `.clade-lineage` anymore — `cladeIntro.jte` and `nav.jte` both delegate to `breadcrumb.jte` which uses `.breadcrumb-trail`.

- [ ] **Step 7: Verify everything compiles**

The user runs `mvn verify` from the repo root. Expected: all tests pass, no compilation errors.

- [ ] **Step 8: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/nav.jte
git add domains/insects/insects-console/src/main/jte/insects/order.jte
git add domains/insects/insects-console/src/main/jte/insects/family.jte
git add domains/insects/insects-console/src/main/jte/insects/genus.jte
git add domains/insects/insects-console/src/main/jte/insects/detail.jte
git add apps/management-console/src/main/resources/static/css/naturalist.css
git commit -m "Detail pages: replace ad-hoc taxonomy with breadcrumb trail"
```

---

### Task 4: Visual verification

- [ ] **Step 1: Start the dev server**

Run the management-console Spring Boot app.

- [ ] **Step 2: Check listing pages**

Visit each listing page and verify the breadcrumb trail renders correctly:

- `/insects/orders` — `Animalia › Arthropoda › Insecta` (non-clickable text)
- `/insects/families` — same
- `/insects/genera` — same
- `/insects/species` — same

Verify: trail appears above the collapsible "Class Insecta" description. Subnav tabs appear below description. Visual style matches the old clade lineage (small, muted text).

- [ ] **Step 3: Check detail pages**

Visit each detail page type and verify the breadcrumb trail renders correctly:

- `/insects/orders/lepidoptera` — `Animalia › Arthropoda › Insecta › **Lepidoptera**` (last segment bold, not linked)
- `/insects/families/papilionidae` — `Animalia › Arthropoda › Insecta › Lepidoptera › **Papilionidae**` (Lepidoptera is a link to `/insects/orders/lepidoptera`)
- `/insects/genera/battus` — `... › Lepidoptera › Papilionidae › **Battus**` (Lepidoptera and Papilionidae are links)
- `/insects/species/battus-philenor` (or another species) — `... › Lepidoptera › Papilionidae › Battus › **Battus philenor**` (Lepidoptera, Papilionidae, and Battus are links)

Verify: no duplicate taxonomy information below the `<h1>` (the old `order · family` text is gone). Links navigate correctly on click. Hover shows underline.

- [ ] **Step 4: Check life-stages page**

Visit a life-stages page (e.g., `/insects/battus-philenor/life-stages`). Verify it still works — it should show no breadcrumb (out of scope for this slice) and the existing plate header is unchanged. No regressions from the `nav.jte` change (the breadcrumb param defaults to empty list).

- [ ] **Step 5: Update work tracker**

Add the breadcrumb primitive to the "Recently completed" table in `docs/work-tracker.md` and clear the candidate-next-slice bullet. Update "Current slice" and "Candidate next slices" as appropriate.
