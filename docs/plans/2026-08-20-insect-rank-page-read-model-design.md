# Insect rank-page read model — completing `Insect`, retiring the dead `TaxonView`

Date: 2026-08-20
Status: **DESIGN — awaiting review**

## 1. Problem

Every insects rank page (order / family / genus / species) is assembled by
**hand in the controller**: each handler resolves the `Insect` read model *and*
fires a pile of separate queries — features, images-at-rank, child-rank member
cards + galleries, functional role — then stitches them together. The read model
that was meant to be the rank-page projection sits half-used, and one piece
(features) is collated entirely in the console.

Two concrete facts drove this design:

- **`Insect` already *is* the rank-page read model.** It is a `ReadModel`
  composing the rank chain (the `InsectTaxonView` permits `InsectOrderView` …
  `InsectSpeciesView`), `lifeStages`, and **`InsectCitationView citations`** — the
  citation *lineage composite*. `InsectFactory.buildByName` assembles it by rank
  name; all four handlers consume it via `insectQuery.getByName(rank)`. Its
  `with*`/`empty()` "identification workflow" use case described in the class doc
  **does not exist in code** — only `InsectFactory` uses those mutators. So `Insect`
  is *only* the rank-page read model; enriching it pollutes no other use case.

- **Features are the one thing bolted on the side.** `InsectFeatureView` and
  `InsectCitationView` are the *same* lineage-composite shape (`subject` +
  provenance-tagged list). Citations flow through `Insect`
  (`.withCitations(citationQuery.findByRankName(name))`); features do **not** —
  the console calls `featureGroups(rank)` → `insectQuery.features().findByRankName()`
  → `FeatureGroup.of(view)`, which **groups by contributing rank in the console**.
  That console-side grouping is the collation to remove.

- **The standalone `taxonView()` is dead.** Repo-wide, the only references to
  `insectQuery.taxonView()` are its javadoc, its port declaration, and its impl.
  Nothing consumes it. `InsectTaxonView`'s own `features()` slot is not just
  always-empty but the **wrong shape** — a flat `FeatureCollection` with no
  provenance, structurally unable to carry the lineage composite. `Insect` composes
  the same permits and more, so `taxonView()` / `InsectTaxonViewFactory` /
  `TaxonViewQueryImpl` are a dead duplicate.

## 2. Design

Make `Insect` the **complete** rank-page read model, assembled solely by
`InsectFactory`; collapse the four handlers to "get `Insect` + presentation";
retire the dead duplicate.

### 2.1 The read-model boundary

`Insect` carries everything that is a **pure function of the rank name**. Two
categories stay in the controller:

- **Viewer / request-dependent** — collection-lens toggle, current naturalist,
  `observationLookup`, lens-filtering of the gallery. Keyed on the signed-in
  naturalist, not the rank; cannot live in a rank-name-keyed read model.
- **Presentation** — rendered Durrell descriptions (`descriptionRenderer`),
  breadcrumb, clade trail, ancestor intros, glossary linking. View concerns.

### 2.2 Enrich `Insect` (api)

Add three components beside `citations` / `lifeStages`:

- `@Nullable InsectFeatureView features` — the feature lineage composite,
  **exactly symmetric with `citations`**.
- `@Nullable InsectFunctionalRole role` — the functional role at the identified
  rank (`Optional<InsectFunctionalRole>` today; populated where one exists).
- `children` — the sub-taxa cards one rank below the identified rank (empty for a
  species page, which shows `lifeStages` instead). **Shape is an open stage-2
  decision — see §4.**

Each new component gets a `with*`, an `invariants()` descent, and updates to
`empty()` + the canonical constructor.

> ⚠️ **Record-arity ripple.** Adding components to the `Insect` record changes
> every `with*`, `empty()`, `new Insect(...)`, and test constructor. Grep the whole
> repo — do not trust a plan's file list (see the `feedback_record_arity_ripple`
> lesson).

### 2.3 `InsectFactory` becomes the sole assembler

It already resolves the rank chain + images (`observations`) + lifeStages +
citations. Add, per rank branch:

- `.withFeatures(featureQuery.findByRankName(name))` — one line, mirroring
  citations. The feature view is assembled **display-ready** (grouped by
  contributing rank, ordinal-ordered within a rank) so **no grouping remains in the
  console**. This moves `FeatureGroup`'s logic into `InsectFeatureQueryImpl` /
  `InsectFeatureView`.
- `.withRole(roleQuery.getByParentName(name))`.
- `.withChildren(...)` — fetch child entities + their galleries, preserving today's
  semantics: `forRankHierarchy` for genera/families (subtree images),
  `forParentName` for species (leaf).

### 2.4 Handlers collapse (console)

Each handler becomes: `insect = getByName(rank)`, then only presentation + viewer
concerns. Deleted: the `featureGroups()` helper, the `FeatureGroup` class, and the
separate image / child-card / role / gallery queries. Rank images come from
`insect.observations()` (already fetched by the factory) instead of a re-query.
`features.jte` renders `insect.features()` directly.

### 2.5 Retire the dead duplicate

Remove `taxonView()` from `InsectQuery`, plus `TaxonViewQueryImpl` and
`InsectTaxonViewFactory`. **Keep** the sealed `InsectTaxonView` + permits — they are
now the rank-chain *and* child-card building blocks — but drop the permits'
wrong-shaped `features()` slot (features live on `Insect` as the lineage composite).

## 3. Staging (ADR-019 — too big for one PR)

1. **Features → `Insect`.** Add `features` to `Insect` symmetric with `citations`;
   make `InsectFeatureView` display-ready (grouping moves out of the console);
   `features.jte` renders `insect.features()`; delete `FeatureGroup` +
   `featureGroups()`. **This stage alone fully fixes the original complaint.**
2. **Role + children → `Insect`.** Add `role` and `children`; collapse the four
   handlers; child-card galleries via the chosen children shape (§4).
3. **Retire** dead `taxonView()` / `InsectTaxonViewFactory` / `TaxonViewQueryImpl`;
   drop the permits' `features()` slot.

## 4. Open decision (stage 2): children modeling

Reuse the `InsectTaxonView` permits as child cards (`List<InsectTaxonView>`, each
carrying its own gallery images), **or** a dedicated `ChildTaxon` value object
(child entity + gallery).

**Performance is not a differentiator** — confirmed:
- Breadcrumb (up-chain) permits are built `.of(entity)` with empty images; the only
  cost is the rank entity, already fetched to render the breadcrumb. ≤4 tiny gets.
- A View permit is a flat record; `.images()` returns stored data, fetches nothing —
  no lazy object graph.
- Only children (one rank down) fetch galleries — the same queries the handlers run
  today. Zero new fetching versus current behavior.

The only real tradeoff is **semantic clarity**: permit-reuse revives the permits'
dead `images()` slot and adds no type, but the same type then means "images empty"
in a breadcrumb slot and "images populated" in a child slot; a `ChildTaxon` type
makes that explicit. Decided when stage 2 starts (stage 1 touches none of it).

## 5. Out of scope

- **Feature test data.** `misumessus-oblongus` renders no features because its
  lineage has no `InsectFeatureAssignment` records (all test assignments sit at
  insect orders). A data gap, tracked separately — not this design.
- **Citations staying flat.** Citations already render flat with an inline
  `(attachedAt)` tag; only features need the display-ready grouping. Harmonizing the
  two is not in scope.

## 6. Acceptance (per stage)

- Stage 1: `insect.features()` is a populated, display-ready lineage composite;
  `features.jte` renders it with no console-side grouping; `FeatureGroup` deleted;
  `mvn` green (user runs it); feature-view + factory + template tests updated.
- Stage 2: each handler consumes `Insect` alone for domain data (plus presentation +
  viewer concerns); role + children present; galleries preserve today's semantics.
- Stage 3: no non-test reference to `taxonView()`; permits carry no `features()`;
  `mvn` green.

## 7. Downstream

The enriched `Insect` is the reference shape the plants rank-page read model
(`Plant`, not yet built) copies, and the input to the later "extract to a kernel"
question — a rank-page read model whose features and citations are symmetric
lineage composites. See `project_taxonviews_brainstorm`.
