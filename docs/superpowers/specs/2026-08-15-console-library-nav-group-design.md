# Console navigation — group the library items

**Date:** 2026-08-15
**Status:** Implemented, pending `mvn verify` and a browser check
**Scope:** `apps/management-console` templates and static assets only

## Problem

The management console's primary nav row carries ten links:

```
Home  Chemistry  Garden  Insects  Plants  Soil  Tree of Life  Concepts  Glossary  Citations
```

Four of those — Tree of Life (`/clades`), Concepts (`/concepts`), Glossary
(`/glossary`), Citations (`/citations`) — are all served by the library domain
(`domains/library/library-console`). They are reference surfaces, not domain
catalogs, and they take up 40% of the bar.

A second defect surfaced while reading the nav. `naturalist.css` is written for a
`<details>` / `<summary>` hamburger, but `page.jte` renders a plain `<nav
class="primary-nav">`. Below 720px the rule

```css
.primary-nav:not([open]) .primary-nav-links { display: none !important; }
```

matches unconditionally — a `<nav>` never carries `[open]` — so the entire nav
disappears on narrow viewports with no toggle to open it. The fix is a few lines
in the same element this change already rewrites, so it lands here.

## Design

### Final bar

```
Home  Garden  Soil  Plants  Insects  Chemistry  Library ▾
```

Seven items instead of ten. The domain order is a scale walk rather than
alphabetical: the garden, the soil under it, what grows, what lives on it, the
chemistry underneath. Library sits last as the reference shelf.

The dropdown holds, in the same broad-to-specific spirit:

```
Tree of Life
Concepts
Glossary
Citations
```

### Markup — `apps/management-console/src/main/jte/layout/page.jte`

Replace the `<nav class="primary-nav">` block with a `<details>` inside a
`<nav>`, and nest a second `<details>` for the library group:

```html
<nav aria-label="Primary navigation">
  <details class="primary-nav">
    <summary class="primary-nav-toggle">
      <span class="primary-nav-toggle-icon"></span> Menu
    </summary>
    <ul class="primary-nav-links">
      <li><a href="/">Home</a></li>
      <li><a href="/garden">Garden</a></li>
      <li><a href="/soil">Soil</a></li>
      <li><a href="/plants">Plants</a></li>
      <li><a href="/insects">Insects</a></li>
      <li><a href="/chemistry">Chemistry</a></li>
      <li>
        <details class="nav-menu">
          <summary>Library <span class="nav-menu-caret">▾</span></summary>
          <ul class="nav-menu-panel">
            <li><a href="/clades">Tree of Life</a></li>
            <li><a href="/concepts">Concepts</a></li>
            <li><a href="/glossary">Glossary</a></li>
            <li><a href="/citations">Citations</a></li>
          </ul>
        </details>
      </li>
    </ul>
  </details>
</nav>
```

Constraints this shape respects:

- **No new Java types, request attributes, routes, or controllers.** `page.jte`
  is compiled by every domain-console template test against a classpath lacking
  Spring Security, the `console.auth` package, and the jakarta servlet API. The
  change is template-only, so those tests stay green.
- **The `<nav>` landmark survives** as the outer wrapper; the `<details>` that
  the CSS drives is inside it.
- **Every existing URL is unchanged.** All four library routes keep working and
  remain one click away.
- **`<details>` is keyboard-accessible and announces expanded state natively.**
  It is not an ARIA menu, and that is deliberate — it matches the pattern the
  stylesheet already assumes and needs no JavaScript to function.

### CSS — `apps/management-console/src/main/resources/static/css/naturalist.css`

Desktop (the file's default layer):

- **`.primary-nav::details-content { content-visibility: visible; display: contents; }`.**
  The desktop layout needs the link list visible while the outer `<details>` is
  closed. The existing `display: flex !important` on `.primary-nav-links` was
  written for that, but it only defeats the older `display: none` slotting —
  Chrome 131+, Safari 18.4+ and Firefox 139+ instead put the contents behind a
  `::details-content` pseudo with `content-visibility: hidden`, which no
  `display` value on the `<ul>` can override. Both paths need handling, and
  neither has ever run before: until this change `.primary-nav` was a `<nav>`,
  so the hack was dead code. The mobile block hides the list by targeting
  `.primary-nav-links` directly, which still works under both paths.
- `.nav-menu { position: relative; }`
- `.nav-menu > summary` reuses the `.primary-nav-links a` treatment — italic
  serif, same padding, same hover — so the group control does not read as a
  foreign widget. Marker suppressed the way `.primary-nav-toggle` already is
  (`::marker` and `::-webkit-details-marker`, plus `list-style: none`, which
  Safari needs).
- `.nav-menu-panel` is absolutely positioned under the summary: parchment
  background, `--sepia-rule` border, `0.25rem` radius, small shadow, `min-width`
  around `12rem`, `z-index` above page content, `list-style: none`.
- Nested `<li>` elements need their own zero-margin rule. The existing
  `.primary-nav-links > li` is direct-child only and will not reach into the
  panel.

#### Pico defaults that must be overridden

Pico styles bare `nav` and `details` aggressively and those rules reach into
custom markup nested inside them. Every one of these was observed, not
anticipated:

| Pico rule | Symptom | Override |
|---|---|---|
| `nav,nav ul{display:flex}` | The four panel items lay out in a **row**, not a column — it reaches every `ul` in the `<nav>`, not just the bar | `.nav-menu-panel{display:block}` |
| `nav li{display:inline-block;padding:…}` | Panel items stay inline and pad apart even inside a block list | `.nav-menu-panel>li{display:block;padding:0}` |
| `nav li :where(a){margin:-0.5rem -0.5rem}` | Negative margins overlap the stacked rows | `.nav-menu-panel a{margin:0}` |
| `details{margin-bottom:var(--pico-spacing)}` | Library box is taller than its `<a>` siblings; `align-items:center` then lifts the label above the bar | `.nav-menu{margin:0}`, `.primary-nav{margin:0}` |
| `details summary::after` | Stray floated `›` chevron detached from the label | `.nav-menu>summary::after{display:none;content:none}` |
| `details[open]>summary{margin-bottom:var(--pico-spacing)}` | Large gap between label and panel once open | `.nav-menu[open]>summary{margin-bottom:0}` |
| `details summary:not([role])` (0,1,2) | Outranks `.nav-menu > summary`, so the label takes Pico's accordion colour | `color: … !important` |

The general lesson: a custom class does not automatically beat a framework
default, and `display` is never inherited from a sibling rule. Declare `display`
and `margin` explicitly on every new nav list, item, link, and summary. Note the
mobile `.primary-nav-toggle` must **not** have its `display` touched by these
resets, or the hamburger vanishes the moment it opens.

Mobile (`@media (max-width: 719.98px)`):

- `.nav-menu-panel` becomes `position: static` with a left indent, so the group
  stacks inside the hamburger instead of floating over the page.

Bump the cache-buster in `page.jte` from `?v=27` to `?v=28`.

### Outside-click close — `static/js/nav-menu.js`

A bare `<details>` does not close when the user clicks elsewhere. Add a small
script beside the existing `card-image-rotate.js`, loaded `defer`: on a
`document` click outside an open `details.nav-menu`, and on `Escape`, remove the
`open` attribute.

This is a progressive enhancement. Without JavaScript the menu still opens and
closes — it just takes a second click on the summary.

## Out of scope

- **Active-section highlighting.** No nav item has an active state today.
  Adding one requires either a new request attribute in
  `NaturalistHeaderInterceptor` (absent from the domain-console test classpaths)
  or URI sniffing in the template. Separate concern, separate change.
- **A `/library` landing page.** Considered and rejected: it costs a controller,
  a template, and tests, and adds a navigation hop to reach the Glossary. The
  dropdown achieves the decluttering without either cost.
- **Any change to the library domain itself.** This is presentation only.

## Verification

1. `mvn verify` from the repo root. All six console modules' templates go
   through `@template.layout.page`, and the five that render templates in tests
   — chemistry, garden, insects, plants, soil — compile `page.jte` as a result.
   A classpath or JTE syntax error reds them immediately. (`library-console` has
   no template-rendering test; its two tests cover the linker and clade rank
   links.)
2. Run the console and check the dropdown at desktop width: opens, all four
   links navigate, closes on outside click and on Escape.
3. Check at 375px. This is the first time the mobile nav has rendered at all, so
   confirm the hamburger appears, opens, and that the Library group stacks
   inside it rather than floating.
