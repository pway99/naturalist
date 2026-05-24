# Console Clade Context: Insecta taxonomic placement

Date: 2026-05-24

## Problem

The management console's insects section presents species, genera, and families
without establishing that insects are Class Insecta within Phylum Arthropoda.
The nav header says "Insects" and pages dive straight into catalog data. A
naturalist browsing the console has no contextual awareness of where insects sit
in the tree of life.

The `clades` kernel already models this hierarchy — `Insecta.parent()` returns
`Arthropoda`, which returns `Animalia`, which returns `Eukaryota` — and each
clade carries a four-level Durrell `Description`. The console simply doesn't
surface any of it.

## Design

### Lineage trail (every insects page)

The `nav.jte` template gains a clade breadcrumb rendered above the existing
subnav links (Family | Genus | Species):

```
Animalia › Arthropoda › Insecta
─────────────────────────────────────────
Family   Genus   Species
```

Implementation:

- Import `CladeTraversal` and `Insecta` at the top of `nav.jte`.
- Compute `CladeTraversal.ancestry(new Insecta())` inline, reverse it
  (root-first), skip `Eukaryota` (too abstract — trail starts at kingdom).
- Render each clade's `displayName()` separated by ` › `. The final node
  (Insecta) has no link since we're already in that domain. Intermediate
  nodes (Animalia, Arthropoda) are plain text — they don't have console
  pages yet. They become links if/when those domains get landing pages.
- Wrapped in `<div class="clade-lineage">` with subdued styling (smaller
  text, secondary colour).

### Landing page description (`/insects` only)

Between the lineage trail/subnav and the "Insect Species" heading, a new
section introduces Class Insecta:

- Heading: "Class Insecta" (from the clade's `displayName()`).
- Body: the existing `@template.components.description(...)` tabbed component
  showing all four Durrell levels of the `Insecta` clade description.
- The `InsectsController.list()` method instantiates `new Insecta()`, gets its
  `description()`, renders each level through `descriptionRenderer.render(...)`,
  and adds the four strings to the model — same pattern as `familyDetail()` and
  `genusDetail()`.
- Only appears on `list.jte`. Other insects pages (families, genera, detail)
  show the lineage trail via `nav.jte` but not the introductory description.

### Styling

Minimal addition to `naturalist.css`:

- `.clade-lineage` — `font-size: 0.85rem`, secondary/muted colour, slight
  bottom margin separating it from the subnav. Same font stack as the nav.
- `›` separator is a plain text character, not an icon.
- Wraps naturally on narrow viewports — no responsive override needed.
- Bump CSS cache-bust `?v=7` → `?v=8` in `page.jte`.

## Touch points

| File | Change |
|------|--------|
| `domains/insects/insects-console/src/main/jte/insects/nav.jte` | Add clade lineage trail above subnav |
| `domains/insects/insects-console/src/main/jte/insects/list.jte` | Add "Class Insecta" heading + description component |
| `domains/insects/insects-console/src/main/java/.../InsectsController.java` | Pass 4 rendered description strings in `list()` method |
| `apps/management-console/src/main/resources/static/css/naturalist.css` | Add `.clade-lineage` rule (~5 lines) |
| `apps/management-console/src/main/jte/layout/page.jte` | Bump CSS version `?v=7` → `?v=8` |

## Concurrency

The other active session is implementing `InsectOrder` as an entity in
`insects-api` (refactoring `InsectGenus`/`InsectFamily` to drop local epithet
copies and add typed FK to a new `InsectOrder` entity). That work lives in the
domain model layer. This work lives entirely in `insects-console` (templates +
controller). No file overlap expected.

## Non-goals

- Clade-based navigation pages (e.g. `/arthropoda` landing page) — future work.
- Linking intermediate clade nodes in the trail — becomes possible when those
  domains get console pages.
- Showing clade context on chemistry or plants pages — separate effort per
  domain when activated.
