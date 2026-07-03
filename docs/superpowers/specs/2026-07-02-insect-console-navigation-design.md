# Insect-Console Navigation Streamline — Design

**Status:** Approved (brainstorm), ready for implementation plan
**Date:** 2026-07-02
**Related:** [`2026-06-29-dual-strategy-clade-navigation-design`](2026-06-29-dual-strategy-clade-navigation-design.md),
[`2026-06-29-dual-strategy-breadcrumb`](../../plans/2026-06-29-dual-strategy-breadcrumb.md) (library dual-breadcrumb effort this reuses)

## Goal

Make the insects console intuitive for a young naturalist by giving it **one
coherent way to move through the catalog** — walk *down* the tree via child
cards, *up* via the breadcrumb — and by surfacing the **tree of life** alongside
the Linnaean catalog, treating gaps in that tree as research invitations rather
than dead ends.

Two parts, shipped as two PRs:

1. **Tree-only navigation** — remove the top rank tab bar.
2. **Phylogenetic clade trail** — add a second breadcrumb row showing the
   evolutionary lineage, with a "not yet placed" affordance where the clade tree
   runs out.

## Motivation / current state

The order page (e.g. Coleoptera) currently carries two stacked controls, both in
[`insects/nav.jte`](../../../domains/insects/insects-console/src/main/jte/insects/nav.jte):

- A **rank tab bar** (Orders / Families / Genera / Species) linking to flat,
  cross-cutting browse lists.
- A **Linnaean-rank breadcrumb** (Kingdom Animalia › Phylum Arthropoda › Class
  Insecta › Order Coleoptera).

The tab bar is not redundant with the breadcrumb — they do different jobs — but
its job is the *disorienting* one: from the beetle page, "Families" jumps to
**every** family in the catalog, yanking a learner out of the beetle context.
That flat, database-style browse competes with the nested-tree mental model the
console should teach. Down-navigation already exists (the child-card grids), and
up-navigation already exists (the breadcrumb), so the tab bar can go.

Separately, the [`clades` kernel](../../../kernels/clades) models the evolutionary
tree of life, and the library dual-strategy effort built a read surface for it
(`CladeQuery` → `CladeView`, rendered as a two-row `dual-breadcrumb` on
`/clades/{slug}`). That phylogenetic lineage is absent from the insect pages
today; the naturalist can see *where a beetle sits on the Linnaean shelf* but not
*where it came from evolutionarily*.

---

## Part 1 — Tree-only navigation

**Change**

- Delete the `<nav class="insects-subnav">` tab block from `insects/nav.jte`.
  The template becomes a thin context-bar wrapper around the breadcrumb (and,
  after Part 2, the clade trail).
- Drop the now-dead `active` parameter from `nav.jte` and from all **10** caller
  templates (`order`, `family`, `genus`, `detail`, `life-stages`, `guild`,
  `orders`, `families`, `genera`, `list`). Each call becomes
  `@template.insects.nav(breadcrumb = breadcrumb)`.
- Remove the orphaned `.insects-subnav` CSS rules; adjust `.insects-context-bar`
  so a single breadcrumb row (two rows after Part 2) sits correctly without the
  tab row above it.

**Deliberately kept**

- `/insects` → `/insects/orders` redirect. The Orders list is the top of the
  tree and the catalog landing.
- The Linnaean-rank breadcrumb (row 1) on every page — it is the *catalog*
  navigator: its links go to `/insects/orders/{slug}`, `/insects/families/{slug}`,
  etc.
- Child-card grids ("Families in this order", …) — the down-navigation.
- The flat `/families`, `/genera`, `/species` routes **and templates stay
  working**; they simply lose their only inbound links (the tab bar). They remain
  reachable by direct URL and by global search. No controller/query/domain code
  changes.
- `nav.jte` keeps its filename despite now being breadcrumb-only, to avoid a
  10-caller rename churn for no functional gain. Noted wart.

**Confirmed:** the four tab links are the *sole* linkers to the flat list pages,
so removing them cleanly unlinks those pages with no other dangling references.

---

## Part 2 — Phylogenetic clade trail

A second breadcrumb row appears **below** the Linnaean-rank breadcrumb on insect
pages: a **single phylogenetic clade row**. It need not horizontally align with
the rank row — each node's link-relation to its parent supplies the context.

Example (Coleoptera order page):

```
Row 1 (catalog nav):  Animalia › Arthropoda › Insecta › Coleoptera
Row 2 (tree of life): Eukaryota › Animalia › Arthropoda › Insecta · Coleoptera (not yet placed)
```

Each real clade node links to `/clades/{slug}`.

### Correspondence is partial and curated

The tree of life diverges from the Linnaean ladder. Of the seven catalogued
insect orders:

- **Mapped** (order slug = clade slug): `blattodea`, `hemiptera`, `lepidoptera`.
- **Unmapped orders** (no order-level clade): `coleoptera`, `diptera`,
  `hymenoptera`, `neuroptera`.

And it is not a clean prefix: `diptera` has no order clade yet reappears deeper
as `Drosophilinae` / `Sophophora` under the fly genus; `hymenoptera` reappears as
`Apoidea` / `Anthophila`. So:

- Slug-equality resolves the easy cases (`lepidoptera` ↔ `Lepidoptera`).
- A **small curated map** (same shape as library's `CladeRanks`) supplies the
  entity→clade correspondence where slugs diverge (e.g. genus `drosophila` →
  `sophophora`). "No clade" is an **expected, first-class result**, not a lookup
  failure.

Because "which insect entity maps to which clade" is insect-domain knowledge whose
only consumer is this breadcrumb, the map lives in `insects-console`
(e.g. `InsectCladeAnchors`) — not in the library or the kernel. This keeps the
kernel pure and avoids a cross-domain port for a single caller.

### Resolving the trail

The controller already assembles the current entity's full lineage for row 1
(`breadcrumbToSpecies` has species → genus → family → order). Reuse it:

1. Walk the lineage slugs from the current entity **upward** (deepest first).
2. Resolve each through `InsectCladeAnchors` (curated map, else slug-equality) to
   find the **deepest slug that maps to a clade** — the *anchor*.
3. Call `cladeQuery.getBySlug(anchorSlug)` (library-api) → `CladeView`. Its
   `ancestry` + `subject` become the real clade nodes of row 2.
4. If the anchor is **shallower than the current page entity** (i.e. the entity's
   own slug did not resolve to a clade), the entity is **unmapped** → emit a gap.

On flat list pages the anchor is `Insecta` and there is no page entity below it,
so row 2 is `Eukaryota › … › Insecta` with no gap.

### Gap affordance (Option A — ghost node + research link)

When the current entity is unmapped, row 2 runs to the deepest real clade and
then renders the entity as a visually distinct **ghost node**: dashed / muted,
labelled `<Entity> · not yet placed`, linking to a short guidance page on
researching and proposing a clade.

- Always in place; teaches by showing *exactly* where the tree of life runs out.
- Because unmapped is common (4 of 7 orders), the research surface is large by
  design — that is the intent: many discovery invitations.

**Constraint that shapes this:** clades are compile-time Java records in the
`clades` kernel. A naturalist **cannot** add one through the console. So the
affordance *surfaces and motivates a research task* — it does not offer a
data-entry form. Building a contribution/persistence subsystem for a compile-time
type would be exactly the kind of over-built machinery to avoid.

The guidance link points to a **new concept page** `/concepts/placing-clades`
(a library `Concept` entry) explaining what a clade is, how the tree of life
differs from Linnaean ranks, and how to research a taxon's placement and propose
adding it. Later it could deep-link to the external-authority (EOL) search for the
taxon — **out of scope** for this work.

### Architecture / dependency

- New dependency: `insects-console` → `library-api` (an api-level cross-domain
  dependency — the allowed kind). Row 2 consumes `CladeQuery` / `CladeView`
  directly; **no changes to `library-api` or `library-core`** (existing
  `getBySlug` suffices).
- Row 1's existing `cladePrefix()` (which uses the `clades` kernel directly) is
  **left as-is**. This means the top three nodes (Animalia / Arthropoda / Insecta)
  appear in both rows — row 1 as rank-labelled catalog context, row 2 as clade
  nodes with `/clades/{slug}` links plus Eukaryota, deeper clades, and the gap.
  This overlap is intentional and reinforces the clade↔rank correspondence.
  Unifying row 1 onto `CladeQuery` and slimming its content is a possible future
  cleanup, not part of this work.

### Files (Part 2)

- New: `InsectCladeAnchors` (curated map + trail resolution) in `insects-console`.
- New: `insects/cladeTrail.jte` — renders the single clade row + ghost node;
  included by `nav.jte` beneath the rank breadcrumb.
- Modify: `nav.jte` to accept the resolved clade-trail view and include the new
  template.
- Modify: `InsectsController` — populate the clade-trail model attribute on each
  entity/list route (using the lineage it already builds).
- New: `placing-clades` concept entry (library `concepts.json` + content) and,
  if needed, its route already covered by the existing `/concepts/{slug}`
  controller.
- CSS: clade-row + ghost-node styles (reuse `dual-breadcrumb` conventions where
  sensible; the ghost node is new).

---

## Non-goals

- No changes to insect domain records, queries, repositories, or the `clades` /
  `taxonomy` kernels.
- No live clade contribution / data-entry flow.
- No external-authority (EOL) integration.
- No removal of the flat list routes/templates (kept, just unlinked).
- No restructuring of row 1 (the Linnaean-rank breadcrumb) beyond dropping the
  dead `active` param.

## Testing

- **Resolver** (`InsectCladeAnchors`): plain unit tests over representative
  lineages — `lepidoptera` (mapped, no gap), `coleoptera` (gap, anchor Insecta),
  a `drosophila` species (mapped via curated override), a butterfly species
  (deep anchor, gap at species). Assert anchor slug + gap flag + gap label.
- **Templates**: rely on `mvn verify` compiling JTE + manual smoke of an
  affected page per case (mapped order, unmapped order, deep species, flat list).
- **Controller**: extend existing tests to assert the clade-trail attribute is
  populated on entity and list routes.
- Run the full build from the repo root (`mvn verify`) — console / Spring Boot /
  JTE modules are the usual failure concentration.

## PR sequencing

1. **PR 1 — Tree-only navigation.** Remove tab bar, drop `active`, CSS cleanup.
   Presentation-only, small diff. Independently shippable.
2. **PR 2 — Phylogenetic clade trail + gap affordance.** `library-api`
   dependency, `InsectCladeAnchors`, `cladeTrail.jte`, controller wiring,
   `placing-clades` concept page, styles. Larger; may split the concept-page
   content into its own tiny content PR if it pushes the diff past the ~400-line
   guideline.

## Open items / future

- Deep-link the gap's research link to external-authority (EOL) search once the
  real client lands (external-authority Phase 4).
- Optionally retire row 1's `cladePrefix()` in favour of `CladeQuery` and slim
  row 1 to catalog entities only (Insecta as root), letting row 2 own everything
  above Insecta. Deferred — out of scope here.
- Rename `nav.jte` → `contextBar.jte` if the misnomer becomes bothersome.
