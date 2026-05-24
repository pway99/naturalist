# Console Clade Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface the clade ancestry (Animalia › Arthropoda › Insecta) on every insects page and show the full Durrell description of Class Insecta on the landing page.

**Architecture:** The `clades` kernel already models the hierarchy (`Insecta.parent() → Arthropoda → Animalia → Eukaryota`). The `nav.jte` template computes the ancestry inline via `CladeTraversal.ancestry(new Insecta())` and renders a breadcrumb trail. The `/insects` landing page additionally shows the Insecta clade's four-level description via the existing tabbed description component. The controller passes the rendered description strings using the same pattern as family/genus detail pages.

**Tech Stack:** JTE templates, Spring MVC (model attributes), `kernels/clades` API, existing `DescriptionRenderer` + `@template.components.description`

**Spec:** `docs/plans/console-clade-context-design.md`

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `domains/insects/insects-console/src/main/jte/insects/nav.jte` | Modify | Add clade lineage trail above subnav |
| `domains/insects/insects-console/src/main/jte/insects/list.jte` | Modify | Add "Class Insecta" intro section with tabbed description |
| `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` | Modify | Pass clade description strings to landing page model |
| `apps/management-console/src/main/resources/static/css/naturalist.css` | Modify | Add `.clade-lineage` style rule |
| `apps/management-console/src/main/jte/layout/page.jte` | Modify | Bump CSS cache-bust `?v=7` → `?v=8` |

---

### Task 1: Add `.clade-lineage` CSS rule

**Files:**
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css:1234` (before `.insects-subnav`)
- Modify: `apps/management-console/src/main/jte/layout/page.jte:14`

- [ ] **Step 1: Add the `.clade-lineage` rule to `naturalist.css`**

Insert immediately before the `.insects-subnav` block (line 1234):

```css
.clade-lineage {
    font-size: 0.85rem;
    color: var(--pico-muted-color);
    margin: 0 0 0.25rem;
    text-shadow: var(--text-shadow-subtle);
}
```

- [ ] **Step 2: Bump the CSS cache-bust version in `page.jte`**

In `apps/management-console/src/main/jte/layout/page.jte` line 14, change:

```html
<link rel="stylesheet" href="/css/naturalist.css?v=7">
```

to:

```html
<link rel="stylesheet" href="/css/naturalist.css?v=8">
```

- [ ] **Step 3: Commit**

```bash
git add apps/management-console/src/main/resources/static/css/naturalist.css
git add apps/management-console/src/main/jte/layout/page.jte
git commit -m "Add .clade-lineage CSS rule for taxonomy breadcrumb trail"
```

---

### Task 2: Add lineage trail to `nav.jte`

**Files:**
- Modify: `domains/insects/insects-console/src/main/jte/insects/nav.jte`

- [ ] **Step 1: Rewrite `nav.jte` to include the clade lineage trail**

Replace the entire file with:

```jte
@import com.naturalist.clades.Clade
@import com.naturalist.clades.CladeTraversal
@import com.naturalist.clades.Eukaryota
@import com.naturalist.clades.Insecta
@import java.util.List

@param String active = ""

!{var ancestry = CladeTraversal.ancestry(new Insecta()).reversed();}
!{var display = ancestry.stream().filter(c -> !(c instanceof Eukaryota)).toList();}

<div class="clade-lineage" aria-label="Taxonomic placement">
    @for(int i = 0; i < display.size(); i++)
        @if(i > 0)
            <span aria-hidden="true"> › </span>
        @endif
        <span>${display.get(i).displayName()}</span>
    @endfor
</div>
<nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
    <a href="/insects/families" aria-current="${"family".equals(active) ? "page" : "false"}">Family</a>
    <a href="/insects/genera" aria-current="${"genus".equals(active) ? "page" : "false"}">Genus</a>
    <a href="/insects" aria-current="${"species".equals(active) ? "page" : "false"}">Species</a>
</nav>
```

Key details:
- `CladeTraversal.ancestry(new Insecta())` returns `[Insecta, Arthropoda, Animalia, Eukaryota]` (start to root).
- `.reversed()` gives root-first order: `[Eukaryota, Animalia, Arthropoda, Insecta]`.
- The `filter` skips Eukaryota — too abstract for a naturalist's browsing context.
- Result displayed: `Animalia › Arthropoda › Insecta`.

- [ ] **Step 2: Verify the template compiles**

Run: `mvn compile -pl domains/insects/insects-console -am -q`
Expected: BUILD SUCCESS (no JTE compilation errors)

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/nav.jte
git commit -m "Show clade lineage trail (Animalia › Arthropoda › Insecta) on all insects pages"
```

---

### Task 3: Add clade description to landing page

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (method `list()`)
- Modify: `domains/insects/insects-console/src/main/jte/insects/list.jte`

- [ ] **Step 1: Add clade description model attributes to the `list()` controller method**

In `InsectsController.java`, add imports at the top:

```java
import com.naturalist.clades.Insecta;
```

In the `list()` method (after the existing model attributes around line 84), add:

```java
var cladeDescription = new Insecta().description();
model.addAttribute("cladePreschool", descriptionRenderer.render(cladeDescription.preschool()));
model.addAttribute("cladeElementary", descriptionRenderer.render(cladeDescription.elementary()));
model.addAttribute("cladeSecondary", descriptionRenderer.render(cladeDescription.secondary()));
model.addAttribute("cladeUniversity", descriptionRenderer.render(cladeDescription.university()));
```

- [ ] **Step 2: Add the description section to `list.jte`**

In `list.jte`, add four new `@param` declarations after the existing params (after line 19):

```jte
@param String cladePreschool = ""
@param String cladeElementary = ""
@param String cladeSecondary = ""
@param String cladeUniversity = ""
```

Then, between `@template.insects.nav(active = "species")` and `<h1>Insect Species</h1>`, insert:

```jte
    <section class="clade-introduction">
        <h2>Class Insecta</h2>
        @template.components.description(
            preschool = cladePreschool,
            elementary = cladeElementary,
            secondary = cladeSecondary,
            university = cladeUniversity)
    </section>
```

- [ ] **Step 3: Verify the template compiles**

Run: `mvn compile -pl domains/insects/insects-console -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/list.jte
git commit -m "Show Class Insecta four-level description on insects landing page"
```

---

### Task 4: Visual verification

- [ ] **Step 1: Start the management console**

Run: `mvn spring-boot:run -pl apps/management-console`

- [ ] **Step 2: Verify the landing page (`/insects`)**

Open `http://localhost:8080/insects` in a browser. Confirm:
- The lineage trail "Animalia › Arthropoda › Insecta" appears above the subnav tabs
- Below the subnav, a "Class Insecta" section shows the tabbed description component
- The four tabs (Preschool, Elementary, Secondary, University) work and show the Insecta clade descriptions
- The species grid appears below the description section as before

- [ ] **Step 3: Verify sub-pages show the lineage trail but NOT the description**

Navigate to:
- `/insects/families` — lineage trail visible, no "Class Insecta" description section
- `/insects/genera` — lineage trail visible, no "Class Insecta" description section
- `/insects/families/papilionidae` (or any family detail) — lineage trail visible
- `/insects/potato-leafhopper` (or any species detail) — lineage trail visible

- [ ] **Step 4: Verify styling**

- The lineage trail text is smaller and muted (secondary colour) — doesn't compete with the subnav
- The trail is readable against the background
- On narrow viewport (mobile), the trail wraps naturally without overflow
