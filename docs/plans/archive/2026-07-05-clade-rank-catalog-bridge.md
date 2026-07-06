# Clade rank eyebrows bridge into the catalog

**Date:** 2026-07-05
**Status:** approved, implementing
**Surface:** `kernels/catalog`, `kernels/catalog-inmem`, `domains/insects/insects-console`,
`domains/library/library-console`.

## Problem

On a `/clades/{slug}` page each ancestor shows a rank eyebrow (KINGDOM, CLASS,
FAMILY …) above the clade name. The eyebrow currently links to `/concepts/{rank}`
— an abstract "what is a family?" definition. But the eyebrow's real meaning is
"this clade **is** the Linnaean family Papilionidae", so the intuitive target is
that taxon's catalog page (`/insects/families/papilionidae`), not a rank
definition.

## Decisions (settled with the user)

- **Two-axis links.** Clade **name** stays on the phylogenetic axis
  (`/clades/{slug}`); the **rank eyebrow** becomes the bridge into the Linnaean
  catalog (`/insects/...`).
- **Catalog-kernel resolution**, not a hand-curated map — so clades with no
  catalog entity (Termitoidae, Animalia, Arthropoda) fall back cleanly with no
  404s, and the bridge is self-maintaining as the catalog grows.
- **Library never depends on insects.** Resolution goes through the existing
  catalog seam.

## Seam: reuse `EntityRefLinker` + one generic catalog addition

The catalog already indexes every entity's globally-unique canonical slug (it
reports `MatchKind.EXACT_SLUG`) and already turns an `EntityRef` into a console
URL via `EntityRefLinker`. So resolution is:

```
catalog.findBySlug("papilionidae")  -> EntityRef(insects, InsectFamilyName)
compositeLinker.linkFor(thatRef)    -> "/insects/families/papilionidae"
```

No clade/rank vocabulary enters the kernel; no new module.

## Changes

### 1. `kernels/catalog` — `Catalog`
Add `Optional<EntityRef> findBySlug(String slug)`: exact, case-insensitive,
**side-effect-free** (fires no `UnresolvedSearchObservation`, unlike `search`).
Returns the unique owner of that canonical slug, or empty.

### 2. `kernels/catalog-inmem` — `InMemoryCatalog`
Build a `Map<String,EntityRef> refBySlug` in the constructor keyed on the
canonical slug (`target.name().value().toLowerCase()`), alongside the existing
token index. `findBySlug` trims+lowercases and looks up that map. Matching is on
the **canonical slug only** — not on any token that merely equals the slug
string.

### 3. `domains/insects/insects-console` — `InsectsLinker`
Add the missing order case:
`case InsectOrderName n -> "/insects/orders/" + n.value()`. Today the linker
handles species/genus/family only; orders are emitted by the catalog
contribution but were unlinkable.

### 4. `domains/library/library-console`
- **`CladeUrls.of`** collapses to `"/clades/" + slug` (drop the `insecta`
  special-case). The name/children/tree axis is now uniformly phylogenetic. The
  `insecta → /insects/orders` continuity is preserved by the existing
  `CladesController` redirect on `/clades/insecta`.
- **New `CladeRankLinks`** (`final` util, static): given `(Catalog, EntityRefLinker,
  slug, LinealRank)` returns the eyebrow URL — `/insects/orders` for `insecta`
  (the class has no self-entity), else the catalog-resolved URL, else the
  `/concepts/{rank}` fallback. Package/static shape mirrors `CladeUrls`.
- **`CladesController`** injects the `Catalog` and the `@Primary`
  `EntityRefLinker` composite (both already app beans; library-console already
  depends on the catalog kernel). In `detail(...)` it builds a
  `Map<String,String> rankLinks` (clade slug → eyebrow URL) for the subject plus
  every ranked ancestry step and adds it to the model.
- **`clades/detail.jte`** rank eyebrows link to `${rankLinks.get(slug)}` instead
  of `/concepts/{rank}`. Name/children links are unchanged (they use the now-pure
  `CladeUrls.of`). Add a `Map<String,String> rankLinks` param.

## What stays put

- **`CladeRanks`** (library-owned slug→rank map) is unchanged — it still decides
  *whether* an eyebrow shows and its label; only the *link* moves to the catalog.
- The individual rank-definition concepts (`/concepts/family`, …) lose their
  inbound link from clade pages but remain reachable from the `/concepts` index
  and the "Rank ladder" cross-link under the heading.

## Tests

- `InMemoryCatalogTest`: `findBySlug` — exact hit returns the ref; miss →
  empty; null/blank → empty; case-insensitive; and it matches the canonical slug,
  not a coincidental token.
- `InsectsLinker` test (add if absent): order ref → `/insects/orders/{slug}`.
- `CladeRankLinks` test (library-console, pure Java with stub `Catalog` +
  stub `EntityRefLinker`): `papilionidae`→family URL, `lepidoptera`→order URL,
  `insecta`→`/insects/orders`, `termitoidae`→`/concepts/family` fallback,
  `animalia`→`/concepts/kingdom` fallback.

## Commit boundaries (one coherent feature, ~150–200 lines)

1. catalog `findBySlug` (kernel interface + in-mem impl + test).
2. insects `InsectsLinker` order case (+ test).
3. library two-axis rendering (`CladeUrls`, `CladeRankLinks`, controller,
   template, test).
