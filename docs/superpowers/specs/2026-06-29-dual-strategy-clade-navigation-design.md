# Dual-strategy clade navigation — design

**Date:** 2026-06-29
**Status:** Design approved; Slice 1 specced in detail, Slices 2–3 are roadmap.

## Problem

The Tree-of-Life clade pages read as if they *mix* Linnaean ranks and clades. They
do not — the `clades` kernel is a pure clade tree, and every node
(`Eukaryota`, `Animalia`, `Arthropoda`, `Insecta`, `Holometabola`, `Lepidoptera`…)
is a monophyletic group. The confusion is **perceptual**: most clade nodes happen
to coincide with a famous Linnaean rank (Animalia = Kingdom, Arthropoda = Phylum,
Insecta = Class, Lepidoptera = Order), which trains the eye to expect *all* of them
to. The lone rank-less node sitting between ranked ones — `Holometabola` — is what
reads as "mixing."

Clades and ranks are two different **organizing strategies** over the same organisms:
a clade tree is the shape of evolutionary *descent*; the Linnaean ladder is a fixed
set of *categories*. The fix is not to hide the difference but to make it explicit
and learnable.

## Vision (four capabilities)

1. **Dual-strategy breadcrumb.** On a clade page, show two parallel breadcrumb rows,
   each labelled as its own strategy:

   ```
   Phylogenetic lineage →  Eukaryota › Animalia › Arthropoda › Insecta › Holometabola › Lepidoptera
   Linnaean rank ladder →   —          Kingdom    Phylum         Class      —             Order
   ```

   The rows are **never combined into one trail**. A reader scanning down sees
   *similarity in name* (Animalia, Arthropoda, Insecta, Lepidoptera appear in both)
   and *difference in strategy* (Eukaryota = domain, Holometabola = no rank,
   Papilionoidea = superfamily have no place on the Linnaean ladder). The gaps are
   the lesson.

2. **Learnable nodes.** Every node on either row links to an internal four-level
   Durrell description page. Clade nodes already have `/clades/{slug}`; rank nodes
   get new per-rank teaching pages.

3. **Source-of-truth links.** Each description page carries a link out to its
   external authority (Encyclopedia of Life, etc.) via `AuthorityReference`. Nothing
   on the breadcrumb jumps straight to an external site — you always land on our
   summary first, then choose to dig into the source of truth.

4. **Collection lens.** Each node page lists the naturalist's own catalogued insects
   that fall under that clade/rank — browsing your specimens by either organizing
   strategy.

## Architectural commitments (settled)

- **Keep the kernel pure.** The `clades` kernel stays "pure structure: who's whose
  parent, plus descriptive metadata." It gains **no** rank knowledge and **no**
  authority knowledge.
- **The library domain owns the clade↔rank correspondence and clade source-of-truth.**
  These are curated reference data, owned by the library domain, not the kernel.
- **`library-console` does nothing but render models and call query contracts
  presented by `library-api`.** Today's `CladesController` violates this — it
  imports the `clades` kernel directly and assembles ancestry/children itself. This
  work moves that assembly into `library-api`/`library-core` and makes the console
  thin, fixing the leak in the same pass.

## Slicing (dependency-ordered, each its own spec → plan)

| Slice | Scope | Risk |
|-------|-------|------|
| **1 — Dual-strategy breadcrumb** | Two-row component + curated `Clade→LinealRank` map + per-rank `Concept` pages. View + content + library read model. No kernel change. | Low |
| **2 — Source-of-truth links** | `Optional<AuthorityReference>` on `Concept` + curated clade→authority map; render "Source of truth →" on the node pages. | Low–med |
| **3 — Collection lens** | Cross-domain clade/rank → catalogued insect species via the catalog kernel + clade-descendant resolution. | Med–high |

Slice 1 is specced below. Slices 2–3 are captured as roadmap and get their own specs
when reached.

---

## Slice 1 — Dual-strategy breadcrumb (detailed)

### Goal

On `/clades` and `/clades/{slug}`, render the two-row breadcrumb. Clade nodes link to
their existing detail pages; rank nodes link to new per-rank `Concept` pages. The
assembly logic lives in the library domain; the console only renders.

### `library-api` (read surface)

Library-api already depends on `authority` and `catalog`; this slice adds `clades`
and `taxonomy` kernel dependencies — reasonable for the domain whose job is teaching
the tree of life.

- **`CladeView`** — a `ReadModel` for one clade, carrying the dual-trail data:
  - the subject clade (slug + display name),
  - its **clade ancestry** (root → subject), as an ordered list of steps,
  - the aligned **Linnaean rank ladder**: for each ancestry step, an
    `Optional<LinealRank>` (empty where the clade has no rank — Eukaryota,
    Holometabola, Papilionoidea),
  - the subject's **direct children** (for the tree / descendants list),
  - per-node link targets (clade node → `/clades/{slug}`; rank node →
    `/concepts/{rankSlug}`).

  Shape sketch (names provisional, to be settled in the plan):

  ```
  record CladeView(
      CladeStep subject,
      List<CladeStep> ancestry,     // root → subject, subject excluded
      List<CladeStep> children
  ) implements ReadModel { … }

  record CladeStep(
      String cladeSlug,
      String displayName,
      Optional<LinealRank> rank     // empty == rank-less clade (the gap)
  ) { … }
  ```

  `CladeView` exposes kernel types (`Clade` slugs, `LinealRank`) — shared vocabulary,
  acceptable in an api read model. It is a `ReadModel`, not an `Aggregate`: it owns
  nothing and is never mutated.

- **`CladeQuery`** — a public query interface. **Does not extend `EntityQuery`**:
  there is no repository and no persisted entity. Methods:
  - `Optional<CladeView> getBySlug(String slug)`
  - a tree accessor for `/clades` (the whole catalog from Eukaryota down) — either
    `CladeView` rooted at Eukaryota or a dedicated tree read model. Settle in plan.

  Backed by a factory in `library-core`, not a database. Follows ADR-010 (thin:
  observe, dispatch, delegate to factory).

### `library-core` (logic + curated data — kept out of the console)

- **`CladeRanks`** (package-private) — the curated `Clade → Optional<LinealRank>`
  correspondence. One reviewed literal per ranked clade; rank-less clades simply
  absent (→ `Optional.empty()`). This is the library domain's bridge between the two
  kernels; it lives here, never in the kernel and never in the console.

  | Clade | LinealRank |
  |-------|-----------|
  | Animalia | KINGDOM |
  | Arthropoda | PHYLUM |
  | Insecta | CLASS |
  | Blattodea, Hemiptera, Lepidoptera, … (orders) | ORDER |
  | Papilionidae, Termitoidae, … (families) | FAMILY |
  | Eukaryota (domain), Holometabola, Apoidea, Papilionoidea, Anthophila, … | *(absent — no LinealRank)* |

  Note `LinealRank` is `KINGDOM…SUBSPECIES` — it has **no DOMAIN and no SUPERFAMILY**,
  so Eukaryota and Papilionoidea correctly map to empty. That is the intended
  divergence, not a gap to fill.

- **`CladeViewFactory`** (package-private, concrete, no interface, no `Impl` suffix —
  per ADR-020/ADR-010) — walks `CladeTraversal.ancestry(...)` and
  `CladeCatalog.childrenOf(...)`, applies `CladeRanks`, assembles `CladeView`. This is
  the assembly logic that moves **out** of `CladesController`.

- **`CladeQueryImpl`** — thin adapter: validate, delegate to the factory.

### `library-console` (thin renderer)

- **`CladesController`** rewritten to call `libraryQuery.clades().getBySlug(slug)` /
  the tree accessor. No `clades`-kernel imports, no ancestry/child assembly. Mirrors
  `ConceptsController`'s shape (via `LibraryTestContext` until Spring-managed).
- **JTE** — a two-row breadcrumb component renders the clade row and the rank row,
  each row labelled and each row header linking to its strategy concept page
  (`/concepts/clade`, `/concepts/taxonomic-rank`). Rank cells align under their clade;
  gap cells render as an em-dash placeholder. Reuse existing `breadcrumb-trail`
  styling where possible.

### Content (new `Concept` entries)

Add per-rank teaching pages to `concepts.json` so a rank node is as learnable as a
clade node: `kingdom`, `phylum`, `class`, `order`, `family`, `genus`, `species`
(and `subspecies` if surfaced). Each gets a four-level Durrell description. The
existing `taxonomic-rank` concept becomes the rank-row strategy header target; the
existing `clade` concept is the clade-row strategy header target.

Rank node link target: `/concepts/{rankSlug}` (e.g. Class → `/concepts/class`).

### Testing

- **`CladeViewFactory` / `CladeQueryImpl`** — library-core tests assert ancestry
  order (root → subject), correct rank alignment, and that rank-less clades yield
  `Optional.empty()` at the right positions (Eukaryota, Holometabola, Papilionoidea).
- **`CladeView`** — `invariants()` for structural well-formedness (non-null steps,
  non-blank slugs), tested via the Observer framework per kernel convention.
- **Concept content** — covered by the existing concept repository/source tests once
  the new entries are added.
- Manual smoke: load `/clades/lepidoptera`, confirm both rows render with Holometabola
  present in the clade row and absent from the rank row.

### Out of scope for Slice 1

- Source-of-truth / authority links (Slice 2).
- Collection lens / cross-domain insect listing (Slice 3).
- Applying the dual-row breadcrumb to the insect taxon pages in `insects-console`
  (fast-follow after clade pages land; reuses the same component).

---

## Slice 2 — Source-of-truth links (roadmap)

- Add `Optional<AuthorityReference>` to `Concept` (library already depends on the
  `authority` kernel via `Citation`).
- Add a curated `Clade → Optional<AuthorityReference>` map in `library-core`
  (symmetric with `CladeRanks`), surfaced through `CladeView`.
- Render "Source of truth: *View on …* →" on `/concepts/{slug}` and `/clades/{slug}`.
- Per-subject deep-links (e.g. Lepidoptera → its EOL page) can reuse the existing
  `external-authorities/eol/` provider.

## Slice 3 — Collection lens (roadmap)

- Each clade/rank node page lists the naturalist's catalogued insects under it.
- Join: a species' `TaxonomicClassification` order/family corresponds by name to a
  clade; "species under clade X" = species whose order-or-family clade is X **or a
  descendant of X**.
- Mechanism: the `catalog` kernel inverse back-reference (insects already ships
  `InsectsCatalogContribution`); add a clade→insects provider, resolved with
  clade-descendant expansion. Use the `/domain-catalog` skill to wire it.
- Highest risk: cross-domain, requires descendant resolution and catalog wiring.

## Open questions deferred to the plan

- Exact `CladeView` vs. a separate tree read model for `/clades`.
- Final names for `CladeView` / `CladeStep` / `CladeRanks` / `CladeQuery`.
- Whether `subspecies` rank gets a concept page in Slice 1.
