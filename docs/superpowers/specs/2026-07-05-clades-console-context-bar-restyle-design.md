# Clades console — context-bar restyle

**Date:** 2026-07-05
**Scope:** `domains/library/library-console` templates + shared `naturalist.css`.
Self-contained; no Java, no query/view changes.

## Problem

The library `/clades` pages render their lineage with the old `.dual-breadcrumb`
— a bare-cream, two-row aligned CSS grid captioned "Phylogenetic lineage /
Linnaean rank ladder". Next to the insects console (which the user praised) it
looks awkward. The jar is visible when linking
`/insects/orders/lepidoptera → /clades/holometabola`.

The insects console wraps its navigation in a warm-tan `.insects-context-bar`
panel holding two stacked rows: a Linnaean `.breadcrumb-trail` (uppercase rank
eyebrow above each name) and, beneath it, a `.clade-trail` (led by a "TREE OF
LIFE" eyebrow). Two rows because an insect page shows two *different* lineages.

## Key design decision — one row, not two

A `/clades/{slug}` page has only **one** lineage: the clade ancestry
(Eukaryota → … → subject), where each `CladeStep` carries an optional
`LinealRank`. That is exactly the shape of the insects **primary breadcrumb**
— name on the main line, rank as the uppercase eyebrow above it. Reusing
`.breadcrumb-trail` renders the same data the dual-grid was aligning by column,
but in the polished field-guide idiom. A second row would duplicate the same
clade+rank data, so the two-row split is not carried over.

## Changes

1. **CSS (`naturalist.css`)**
   - Generalize `.insects-context-bar` → shared `.context-bar` (rename the two
     selectors: the panel and `.context-bar .breadcrumb-trail`).
   - Delete the now-dead `.dual-breadcrumb*` block (used only by the two clades
     templates being rewritten).

2. **Insects `nav.jte`** — swap the wrapper class `insects-context-bar` →
   `context-bar`. No visual change; no test pins the old name.

3. **`clades/detail.jte`** — replace the `.dual-breadcrumb` grid with a
   `.context-bar` panel wrapping a `.breadcrumb-trail`. Each ancestry `CladeStep`
   becomes a `.breadcrumb-segment`: rank (title-cased, e.g. "Order") as a
   `.breadcrumb-rank` eyebrow linking to `/concepts/{rank}` when present, clade
   name linking via `CladeUrls.of(slug)`; `›` separators between segments; the
   subject is the bold `.breadcrumb-current`. Preserves the `insecta →
   /insects/orders` bridge and the learnable rank→concept affordance. Concept
   cross-links, description, and children list below are unchanged.

4. **`clades/tree.jte`** — replace the single-root `.dual-breadcrumb` with the
   same `.context-bar` + `.breadcrumb-trail` panel showing the root as the bold
   current node (only when `rootView != null`). Tree list unchanged.

## Out of scope

Concept pages (`/concepts/*`) already use `.breadcrumb-trail`; untouched. No
changes to `CladeView`, `CladeQuery`, or the clades kernel.
