# Plants — Clade Modeling (as-built)

**Date:** 2026-08-16
**Status:** implemented (written, IDE-clean, awaiting `mvn verify`). Not committed.
**Source design:** the `2026-08-16-plants-clades-design.md` brief handed to this session.
Its decisions D1–D4 and the *Out of scope* list stand as written; this file records the
**C1 resolution** the brief required and the places the as-built work departed from the
brief's file list (the brief itself instructed: *grep the repo, do not trust this list*).

---

## C1 resolution — the "plants-owned trait function" unknown

**Branch: deterministic resolver. No trait inference. C1 collapses into the read.**

`kernels/CLAUDE.md` and `docs/plans/clades-kernel.md` (Phase 6) settle it. The clade kernel
holds no traits; a consuming domain "declares its own trait function over its own clade
nodes" only when it *has* a trait to declare (insects declares `Holometabola →
MetabolyTrait`). The kernel plan states plainly: *"Plant-specific traits (e.g.
photosynthesis) declared at the appropriate clade level when a consumer demands them. None
demanded by the current roadmap."*

So plants declares **no trait function** in this pass — there is no `PlantClades` class. The
brief's phrase *"PlantClades permit set mirroring InsectClades"* conflated two things:
`InsectClades` is a trait-function class, whereas the clade nodes are individual `Clade`
record permits. Plants gets the record permits and nothing else. "Clade of a taxon" is a
pure read: `PlantOrder.placedIn()`, with lower ranks resolving by walking up to their order
(deferred — see below). Morphology-based clade *inference* remains out of scope; it belongs
with the vision-ID effort.

---

## As-built (deviations from the brief's letter, not its intent)

- **Kingdom bridge node = `Plantae`** (user decision, 2026-08-16). The brief drew
  `angiosperms` as a rootless top; the clade kernel is single-rooted at `Eukaryota` via
  `parent()`. `Plantae` bridges `angiosperms → Plantae → Eukaryota`, joining the shared
  animal tree at Eukaryota exactly as `clades-kernel.md` Phase 6 specified. Matches the
  console breadcrumb's existing "Plantae" kingdom root.
- **14 clade nodes, not the brief's implied ~13.** Added `Plantae` (bridge) + `Angiosperms,
  Magnoliids, Monocots, Commelinids, Eudicots, Superrosids, Rosids, Fabids, Malvids,
  Superasterids, Asterids, Lamiids, Campanulids`. The brief's `ericales` node was **dropped**
  — no order attaches to it (see next point).
- **13 catalog orders, not the brief's 15-row table.** `Myrtales` (Feijoa) and `Ericales`
  (persimmon) are **absent** from `plant-orders.json`, and the FK-closed rank chain means
  their families/genera/species cannot exist either. Per fixture discipline the two rows
  were treated as aspirational and **not** invented. A strong cross-check: each present
  order's own `university` description already names its clade in prose ("Asterid clade,
  Campanulid lineage", "magnoliid clade", …) and **every one agreed** with the APG IV
  mapping applied.
- **No `ForeignKeyConstraint` on clades.** The brief's C2 FK bullet was dropped: clade
  integrity is type-level — `placedIn` is a `@Nullable Clade` loaded via `Clade.of(slug)`,
  whose `@JsonCreator` throws on an unknown slug. Insects adds no FK; plants matches it.
- **No new ArchUnit suite.** No module-layering ArchUnit harness exists in the repo to
  extend (only `ResilienceComplianceTest`). The `plants-api → insects-api` non-dependency is
  guaranteed structurally: `plants-api` declares only a `clades` (kernel) dependency and
  `PlantOrder` imports only `com.naturalist.clades.Clade`. Consistent with the repo's stated
  "package-private is the primary enforcement."
- **C5 has no new query.** With placement at Order only (D2), "clade of this order" is
  `order.placedIn()` — already on the read surface. A transitive `Species → … → Order →
  placedIn` walk is deferred with the lower detail pages (a port for zero present callers).
- **C6 = one line, zero controller change.** The order-detail template already receives the
  `PlantOrder`; the clade line reads `placedIn()` directly and links to `/clades/{slug}`
  (the same route insects uses). The 14 new nodes also gain `/clades/{slug}` detail pages
  and appear in the `/clades` tree for free — `CladeViewFactory` is reflection-based over the
  sealed permits.

## Milestone checklist (as-built)

- [x] **C1** — resolver branch recorded above.
- [x] **C2** — 14 `Clade` permits + `Clade.permits`/`Clade.of` wiring. No trait class, no FK.
- [x] **C3** — `@Nullable Clade placedIn` + `withPlacedIn` on `PlantOrder`; `invariants()`
      unchanged; `clades` dep added to `plants-api`. Rippled the 6 `new PlantOrder(...)`
      sites (`PlantOrderTest` ×3 + a new `withPlacedIn` test; `OrderRepositoryTest` ×3).
- [x] **C4** — `placedIn` set on all 13 orders in `plant-orders.json` (APG IV).
- [x] **C5** — `order.placedIn()` is the read; transitive walk deferred.
- [x] **C6** — clade line on the order-detail page, linking `/clades/{slug}`.
- [x] **C6+ (post-review, user-requested)** — replaced the single clade line with a
      full **phylogenetic breadcrumb row** (`plants/cladeTrail.jte`), a second context-bar
      row mirroring insects: `Plantae › Angiosperms › … › <order clade>`, each node linking
      `/clades/{slug}`. The row opens at **Plantae** (matching the taxonomic breadcrumb's
      root) — the shared `Eukaryota` root above it is filtered out to keep the row within
      the plant world (one click up from the Plantae node). Built from
      `CladeTraversal.ancestry(order.placedIn())`
      in `PlantsController` — no library dependency, no `PlantTaxonView` (the order page
      carries `placedIn` directly, so it is not entangled with the deferred read model).
      Its infoPopover turns the **supra-ordinal gap into a teaching moment**: it explains
      that plant branches between Kingdom and Order are rank-free clades sitting where
      Phylum/Class would be, and links to the existing four-level `/concepts/class`
      description — closing the "a plant order has no Class, so a learner never meets one"
      gap without adding a `PlantClass` (D1 upheld). Only the **order** detail page carries
      the row so far; family/genus/species would resolve it by walking up to the order
      (the deferred transitive walk).

## Still out of scope (unchanged from the brief)

Clade browse/pedagogy surface and the `PlantTaxonView` read model; the write side; trait
inference; sub-ordinal placement; and — permanently, per D1 — a `PlantClass` rank.
