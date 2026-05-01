# Catalog Kernel — Plan Redirect (search-and-discovery reframe)

This file proposes the in-place edits to `PLAN.md` that pivot the catalog kernel
from an exact-match routing surface to a search-and-discovery surface, per the
chat decision recorded on 2026-04-29. It is structured as a set of section
replacements and insertions you can apply against the existing PLAN.md without
renumbering historical milestones.

The forward-direction routing model (M2, M4, M7) shipped successfully and
proved the contribution-and-assembly architecture works. Routing is the wrong
*query semantics* for the actual user — a young naturalist building a
collection through wonder and discovery, not a navigator following pre-laid
links. The fix is not throwing away what shipped; it is changing what the
contribution is contributing *to*. The same `CatalogContribution` shape, the
same per-app composition, the same kernel boundaries — but the assembled
`Catalog` answers `search(text)` instead of `resolveAlias(text)`, and the
contribution's job becomes feeding a token index instead of a routing map.

The inverse-direction work (M3, M5, M8) is unaffected. Back-references answer
a different question — "which of my entities reference this foreign name" —
and that *is* a routing answer, not a search answer.

---

## Replacement: "Why this exists"

The plants console renders `Description.university()` as a wall of dense
academic prose — Latin binomials, measurement units, parenthetical citations,
inline numbered enumerations — with no internal landmarks. The reader cannot
scan it. The text already carries latent structure (taxonomic header,
quantitative claims, visitor list, management note) that a renderer can
surface.

That same prose is dense with terms a young naturalist will not yet
recognise: *Aristolochia californica*, *Bombus*, *aristolochic acid*,
"pipevine swallowtail." Some of these have entries in the catalog already;
some do not yet; some never will. The reader's question is the same in every
case — *what is this thing, and is there anywhere I can go to find out
more?* — and the catalog's job is to give an honest answer: a hit if there
is one, an empty state with a path forward if there is not.

On the inverse side, the `Compound` detail page for `aristolochic-acid` has
no way to enumerate the plants or insects that reference it, because each
domain knows its own outbound references but no domain owns the inverse
index.

Both problems share a shape: cross-domain entity resolution, contributed by
each domain, assembled per app, queried by the console. The forward direction
is *search* — many possible answers, including none, with the empty state
itself being a feature (it is the catalog's growth signal). The inverse
direction is *routing* — a deterministic fan-out across domains that opt in
to answering a particular reference type. That is the catalog.

---

## Replacement: "Architectural decisions"

**Module placement.** `kernels/catalog/`, sibling to `kernels/field-notes` and
`kernels/taxonomy`. Depends only on `framework` and `identifiers`. Nothing in
the DAG depends on catalog except domain `<domain>-core` modules (which
contribute) and console / app modules (which consume). The kernel itself
contains contracts and a default in-memory assembly — no domain knowledge.

**Two SPIs, one kernel.** Search and inverse resolution are separate concepts
that share the contribution-and-assembly pattern. Both live in
`kernels/catalog/`:

- `CatalogContribution` — search direction. A domain declares the searchable
  entities it owns and the surface forms (tokens) under which each is
  findable. Used by the console's search box to answer "what does the
  catalog know about *aristolochia*?" with potentially many hits across
  potentially many domains.
- `EntityReferences<T extends EntityName>` — inverse direction. A domain
  declares it can answer "give me my entities that reference this foreign
  `EntityName`." Used by the console to render "Found in:
  california-pipevine (plants), pipevine-swallowtail (insects)" on the
  `aristolochic-acid` detail page. Unchanged from the original plan.

Each domain implements zero or more of each. Contributions are collected in
each app's composition root.

**Search returns many. Routing returns one set per domain.** The two SPIs
have intentionally different return shapes:

- `Catalog.search(String)` returns a `SearchResults` collection. Zero, one, or
  many hits, ordered by `MatchKind` (exact slug, exact token, prefix). Each
  hit carries the matched token so the UI can explain *why* this entity
  surfaced. Empty results are a first-class state — they fire an
  `UnresolvedSearchObservation` at INFO and the search-results page renders
  an empty-state copy that points the user toward adding the term to a
  collection.
- `Catalog.findReferencesTo(EntityName)` returns `Map<DomainId,
  List<EntityRef>>`. The contract is exhaustive — every domain that opted
  in to answering this reference type fans out concurrently — and the
  result is grouped by domain because that is how the console renders it.

**Mock index, not Lucene.** The kernel ships an in-memory token index:
lowercased whitespace-and-punctuation tokenisation, `Map<String,
Set<EntityRef>>`, no scoring, no stemming, no analyzers. The mock proves the
UX and the SPI shape. Production replaces the index implementation behind the
`Catalog.search` interface with Lucene; no contributing domain or consuming
console changes. The kernel never sees the implementation difference.

**Derive search tokens, do not register.** A domain's
`CatalogContribution.searchableEntities()` yields `(EntityRef, Stream<String>
tokens)` per entity, derived from the entity's own data — slug, scientific
binomial, genus, abbreviated binomial, common names. Adding an entity
automatically contributes its tokens; adding a common name is one edit in
one place (the entity's JSON record, the same file the entity is authored
in).

**Token collisions are a feature.** Under routing they were a bug — the M4
plants contribution silently dropped genus-level tokens that resolved
ambiguously (two `Trifolium`, two `Passiflora`). Under search they are
exactly the desired behaviour: a search for `Trifolium` returns both
species, the user picks. The drop-on-collision rule from the M4 contribution
goes away. `InMemoryCatalog` indexes every contributed token without rejection;
multiple `EntityRef`s per token is the normal case.

**Common names are typed, not strings.** A `CommonName` value object lives in
`kernels/field-notes` (same kernel as `Description` — both are vernacular
field-naturalist vocabulary). Components are `(String label, Locale locale)`.
Equality by value. `field-notes` continues to depend only on `framework` —
`CommonName` does not carry an `EntityName` back-pointer; the association
is held by the entity that owns both its `commonNames: Set<CommonName>` and
its own `EntityName`.

**Soft validation via Observer at INFO.** Forward-direction misses (a search
that returns nothing) are recorded as `UnresolvedSearchObservation` at INFO.
This is the catalog's growth signal — aggregated over time, the most-searched
terms with no hits are the entities most worth adding next. Inverse-direction
dangling references (a `PhytochemicalConstituent` referencing a `CompoundName`
that the chemistry catalog does not have) remain `UnresolvedReferenceObservation`
at WARN. Two observation types, two severities, same pipeline.

**Per-app composition, not a shared registry module.** Unchanged from the
original plan. The assembled `Catalog` instance lives in each app's composition
root; each app collects the contributions of the domains it includes; a
registries-parent module that depends on every domain is explicitly rejected.

---

## Replacement: Module layout

```
kernels/catalog/
  pom.xml
  PLAN.md
  src/main/java/com/naturalist/catalog/
    Catalog.java                — public-facing query interface (search + inverse)
    CatalogContribution.java      — search-direction SPI (searchable entities + tokens)
    EntityReferences.java       — inverse-direction SPI (back-references)
    SearchResults.java          — BehavioralCollection<SearchHit> (ADR-011)
    SearchHit.java              — ValueObject: target + matchedToken + matchKind
    MatchKind.java              — enum: EXACT_SLUG, EXACT_TOKEN, PREFIX
    EntityRef.java              — typed reference to an entity in some domain
    DomainId.java               — typed domain identifier
    UnresolvedSearchObservation.java        — INFO; forward-direction miss
    UnresolvedReferenceObservation.java     — WARN; inverse-direction dangle
    InMemoryCatalog.java           — in-memory token index + provider routing
  src/test/java/com/naturalist/catalog/
    InMemoryCatalogTest.java
```

`kernels/field-notes/` grows one new file:

```
kernels/field-notes/src/main/java/com/naturalist/fieldnotes/
  CommonName.java               — ValueObject: (label, locale)
```

---

## Milestone supersession table

| Original | Status | Supersedes / Replaced by |
|----------|--------|-------------------------|
| M0 — Scaffold | ✅ shipped | unchanged |
| M1 — Shared types | ✅ shipped | unchanged |
| M2 — Forward SPI: CatalogContribution + resolveAlias | ✅ shipped, **superseded** | replaced by **M2′** |
| M3 — Inverse SPI | ✅ shipped | unchanged |
| M4 — Plants CatalogContribution (derived aliases) | ✅ shipped, **superseded** | replaced by **M4′** |
| M5 — Plants EntityReferences\<CompoundName\> | ✅ shipped | unchanged |
| M6 — Description renderer (formatting only) | ✅ shipped (M6a + M6b wiring) | unchanged |
| M7 — Renderer catalog integration (forward linking) | ✅ shipped, **superseded** | replaced by **M7′** |
| M8 — Chemistry detail back-references | ✅ shipped | revised — Catalog wired as Spring `@Bean`; URL contribution moved to per-domain `EntityRefLinker` SPI composed by `CompositeEntityRefLinker` (LinkResolver removed in M7′) |
| M9 — Observer wiring and metrics | pending | revised — split into INFO and WARN observation types |
| M10 — Eager startup validation | pending | revised — coverage assertion, not dangle hunt |
| M11 — ArchUnit guard | pending | unchanged |
| M12 — Documentation and ADR | pending | unchanged in shape, content updated |
| — | ✅ shipped | **M1.5** — `CommonName` value object in field-notes |
| — | ✅ shipped | **M2′** — Catalog.search + token index |
| — | ✅ shipped | **M4′** — Plants searchable contribution (with common names) |
| — | ✅ shipped | **M7′** — Renderer search affordances |
| — | ✅ shipped | **M-Search-UI-A** — persistent search box in console layout |
| — | ✅ shipped | **M-Search-UI-B** — search results page |

Superseded milestones keep their existing text in PLAN.md as historical
record. New milestone bodies follow.

---

## New milestone bodies

### M1.5 — `CommonName` value object in field-notes ✅

**Goal.** Establish the typed common-name vocabulary that M4′ will harvest as
search tokens. Lands before the search-direction work because every
downstream milestone wants it.

**Read first.**
- `kernels/field-notes/src/main/java/com/naturalist/fieldnotes/Description.java`
  — the existing field-notes value object; new file mirrors its style.
- `kernels/framework/src/main/java/com/naturalist/ddd/ValueObject.java`
- `kernels/framework/src/main/java/com/naturalist/observability/Constraints.java`
- `chat-briefing.md` §3 (record conventions) and §4 (Constraints API).

**Build.**
- `kernels/field-notes/src/main/java/com/naturalist/fieldnotes/CommonName.java`
  — record `CommonName(String label, Locale locale)` implementing
  `ValueObject`. Static factory `CommonName.of(String label)` defaults
  locale to `Locale.ENGLISH`; `CommonName.of(String label, Locale locale)`
  takes both. `@JsonCreator` on the two-arg `of(...)` per the
  EntityName/NamedValue convention (records do not need it directly, but
  the static factory is the public construction path per ADR-012).
- Invariants: `notBlank(label, "label")`, `notNull(locale, "locale")`. No
  trim — labels are authored, whitespace is the author's choice.
- A unit test class `CommonNameTest` next to the existing
  `DescriptionTest`. Coverage: valid case via `RandomValue`-populated
  components; invalid cases for null/blank label and null locale, asserting
  the expected invariant paths via the Observer framework per
  `chat-briefing.md` §4.
- No changes to `field-notes/pom.xml` — its dependency footprint
  (`framework` + Jackson) is unchanged.

**Acceptance.** Module compiles. Tests pass. `field-notes` still depends
only on `framework`. No domain code touched.

**Notes.**
- The `Locale` choice over a custom `LanguageTag` value object is
  deliberate — `java.util.Locale` covers the regional-variant use case
  (`en-US`, `en-GB`, `es-MX`) without inventing a parallel type. Jackson
  natively serialises `Locale` as a BCP-47 tag.
- `CommonName` deliberately carries no `EntityName` back-pointer. The
  association is held by the entity that owns the common name; the value
  object stays purely textual so `field-notes` keeps its current
  dependency boundary.

---

### M2′ — Catalog.search and the token index ✅

**Supersedes.** M2 (`CatalogContribution.aliases()` + `Catalog.resolveAlias`).

**Goal.** Replace exact-match routing with token-based search. The
contribution's job becomes feeding a token index; `Catalog`'s forward-direction
method becomes `search(String) -> SearchResults`.

**Read first.**
- The shipped M2 code in `kernels/catalog/` — `CatalogContribution.java`,
  `Catalog.java`, `InMemoryCatalog.java`, `CatalogAssembly.java`. The supersession
  rewrites these in place.
- M1 outputs (`EntityRef`, `DomainId`).
- `kernels/framework/src/main/java/com/naturalist/ddd/BehavioralCollection.java`
  — `SearchResults` extends this per ADR-011.
- `chat-briefing.md` §7 (BehavioralCollection conventions).

**Build.**
- `MatchKind` enum: `EXACT_SLUG`, `EXACT_TOKEN`, `PREFIX`. Order is the
  intended display order — `EXACT_SLUG` first, `PREFIX` last. Adding a
  fourth kind later (e.g. `FUZZY` once Lucene lands) is an enum extension.
- `SearchHit` — record `(EntityRef target, String matchedToken, MatchKind
  kind)` implementing `ValueObject`. Invariants: target non-null, token
  non-blank, kind non-null.
- `SearchResults` — `final class` extending `BehavioralCollection<SearchHit>`
  per ADR-011. Constructor package-private; `public static of(List<SearchHit>)`
  and `public static empty()` are the construction API. Domain-specific
  filters (`groupedByDomain()`, `topN(int)`, `byKind(MatchKind)`) return
  new instances via the package-private constructor.
- `CatalogContribution` — replace `Stream<Alias> aliases()` with
  `Stream<SearchableEntity> searchableEntities()`. A `SearchableEntity` is
  a record `(EntityRef target, Stream<String> tokens)`. The contribution
  no longer pre-resolves surface forms to single targets; it emits each
  entity once, with the tokens that should index it.
- `Catalog` — replace `Optional<EntityRef> resolveAlias(String)` with
  `SearchResults search(String text)`. Inverse-direction methods unchanged.
- `InMemoryCatalog` — package-private constructor takes contributions and
  providers. Builds a `Map<String, Set<EntityRef>>` at assembly time,
  lowercasing each token and splitting on whitespace and ASCII punctuation
  (`[\s\p{Punct}]+`). At query time, `search(text)` lowercases and tokenises
  the input identically, looks up each token, and merges hits — slug-equal
  hits get `EXACT_SLUG`, token-equal hits get `EXACT_TOKEN`, prefix hits
  (token starts with the search input) get `PREFIX`. Ordering inside
  `SearchResults` is by `MatchKind` ordinal then by `EntityRef.name()`
  natural order for determinism.
- `CatalogAssembly` — unchanged shape; the `from(...)` builder now
  precomputes the token index instead of the alias map. The provider list
  is still indexed by `referenceType()`.

**Acceptance.** `InMemoryCatalogTest` covers:
- Exact slug match (`"california-pipevine"`) returns one hit, `EXACT_SLUG`.
- Exact token match against a derived token (`"aristolochia"`) returns one
  or more hits, `EXACT_TOKEN`.
- Genus-level ambiguity (`"trifolium"` against a synthetic two-Trifolium
  contribution) returns *both* hits — no drop-on-collision.
- Case-insensitive match (`"ARISTOLOCHIA"` resolves identically to
  `"aristolochia"`).
- Prefix match (`"arist"` returns the full set under `PREFIX`).
- Empty input returns `SearchResults.empty()`.
- Unknown token returns `SearchResults.empty()` and fires one
  `UnresolvedSearchObservation` (the observation type itself ships in
  M9-revised, but the firing site is wired here behind a no-op observer
  so the wiring is in place).
- The hit ordering inside a `SearchResults` matches `MatchKind` ordinal
  then `EntityRef.name()`.

Tests use synthetic contributions only — no domain wiring at this milestone.

**Notes.**
- Case sensitivity inverts from M2's "case-sensitive on the first character
  (genus capitalisation matters)" to "case-insensitive throughout." Genus
  capitalisation is a presentation concern, not a search concern — a young
  naturalist typing into a search box should not need to know the
  binomial-nomenclature capitalisation rule. The renderer (M7′) keeps
  italicising `Genus species` correctly; the search is liberal in what it
  accepts.
- Tokenisation deliberately splits on punctuation — "A. californica"
  indexes as `a` and `californica`, both individually findable. The
  abbreviated-binomial form is preserved as a hit because both tokens
  match the same `EntityRef`; the merge step deduplicates per
  `(EntityRef, MatchKind)`.

---

### M4′ — Plants searchable contribution (with common names) ✅

**Supersedes.** M4 (`PlantCatalogContribution` deriving aliases with
drop-on-collision).

**Goal.** Re-implement the plants contribution against the M2′ SPI. Emit all
derivable surface forms unconditionally; harvest authored common names.

**Read first.**
- The shipped M4 code in
  `domains/plants/plants-core/src/main/java/com/naturalist/plants/catalog/PlantCatalogContribution.java`
  and its test. The supersession rewrites these in place.
- M2′ outputs (`SearchableEntity`, the new `CatalogContribution` shape).
- M1.5 output (`CommonName`).
- `domains/plants/plants-api/src/main/java/com/naturalist/plants/Plant.java`
  — confirm whether the `commonNames: Set<CommonName>` field has landed
  (planned in this milestone if not).

**Build.**
- If `Plant` does not yet carry `commonNames: Set<CommonName>`, add the
  component. Update `PlantTestEntitySource` and `plants.json` so at least
  the worked-example entries (`california-pipevine`, `crimson-clover`,
  `white-clover`) carry their common names. Other entries get an empty set
  for now; backfill is a future task and is not blocking.
- Rewrite `PlantCatalogContribution` to implement the new SPI shape.
  `searchableEntities()` returns one `SearchableEntity` per plant in the
  catalog. Tokens per plant:
    - The slug (`plant.name().value()`) — feeds the `EXACT_SLUG` match path
      via the index built in M2′.
    - `genus + " " + species` when species is non-null.
    - `genus` alone.
    - `genus.charAt(0) + ". " + species` when species is non-null.
    - One token per `CommonName` in `plant.commonNames()` — the label only
      (locale is a presentation concern, not a search concern; users
      searching in any locale should hit any-locale common names).
- Drop the old "ambiguous-form filter" entirely. `Trifolium` (genus alone)
  emitted by both `Trifolium pratense` and `Trifolium repens` is the
  expected behaviour — both `EntityRef`s end up in the index under that
  token.
- Test rewrite. `PlantCatalogContributionTest` asserts:
    - `california-pipevine` is reachable via slug, `Aristolochia californica`,
      `Aristolochia`, `A. californica`, and each of its common names
      (`pipevine`, `California Dutchman's pipe`, …).
    - `Trifolium` (genus alone) returns both `Trifolium` species when fed
      through an assembled `InMemoryCatalog`.
    - A plant with no common names (empty set) contributes only its derived
      forms, no errors.
    - Adding a plant with a `null` species in test data emits genus-only
      tokens and skips the binomial forms (existing behaviour preserved).

**Acceptance.** Test passes against `PlantTestEntitySource`. Assembled
`InMemoryCatalog` returns the expected hit sets for each pinned input. The
contribution does not make the catalog depend on plants — assembled in
plants-core test scope, consumed via the catalog SPI only (unchanged from M4).

**Notes.**
- Adding `commonNames` to `Plant` is technically an api-module change, not
  a core-module change. Per `chat-briefing.md` §1 the api module's
  dependencies (`framework`, `identifiers`, `field-notes`, `taxonomy`)
  already include `field-notes`, so `CommonName` is reachable without a
  new arrow.
- The `commonNames` JSON field deserialises as a list of objects: `[{
  "label": "pipevine", "locale": "en" }, …]`. If the authoring burden of
  always writing the locale becomes a friction, a Jackson custom
  deserialiser on `CommonName` accepting either a string (defaulted to
  English) or an object is a reasonable next step — defer until annoyance
  is real.

---

### M7′ — Renderer search affordances ✅

**Supersedes.** M7 (renderer + `LinkResolver` producing direct anchor tags
to entity detail pages).

**Goal.** The description renderer no longer auto-links resolved binomials.
Every italicised binomial — and every common-name candidate the renderer
can spot — becomes a search affordance: a link to the search-results page
with the term as the query. The detail-page-anchor behaviour from M7 goes
away.

**Read first.**
- The shipped M7 code:
  `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/render/DescriptionRenderer.java`,
  `LinkResolver.java`, `DescriptionRendererCatalogTest.java`.
- M2′ outputs (the renderer no longer needs `LinkResolver`'s URL builders;
  it produces a single search URL per term).
- M-Search-UI-B output if it lands first (the search results URL contract).

**Build.**
- Remove `LinkResolver` and the `Map<Class<? extends EntityName>,
  Function<EntityName, String>>` URL builder machinery. The renderer no
  longer needs to know which domain a term belongs to — the search page
  figures that out at query time.
- The renderer's binomial-italics pass becomes a binomial-italics-and-wrap
  pass: every detected binomial gets wrapped in `<a
  href="/search?q={url-encoded-term}" class="discover">…</a>` regardless
  of whether the catalog currently knows about it. The class hook lets the
  layout style discovery affordances distinctly from "real" links (a
  dotted underline, a small magnifying-glass icon — design call,
  out-of-scope here).
- `attributeEscape` in M7 covered `"`; extend or replace with a proper URL
  encoder for the query value (`URLEncoder.encode(term,
  StandardCharsets.UTF_8)`). Spaces become `+` or `%20`; punctuation in
  binomials (the `.` in `A. californica`) round-trips correctly.
- `PlantsController` no longer assembles a `LinkResolver` with URL
  builders for `PlantName` / `CompoundName` / `InsectSpeciesName`. It still
  assembles the `Catalog` (now via M2′'s shape, which the renderer no
  longer uses directly) — the catalog is needed for the search-results
  controller, not the renderer.
- `DescriptionRenderer`'s public surface narrows: no `Catalog` collaborator.
  This is a simplification. The `DescriptionRendererCatalogTest` class is
  deleted; a single `DescriptionRendererTest` covers everything.

**Acceptance.** Visual review on `crimson-clover`'s university level:
- `Trifolium pratense` is italicised and wrapped in a search link.
- `T. pratense` (abbreviated) is italicised and wrapped in a search link
  pointing at the abbreviated form (round-trip-safe URL encoding).
- Clicking the link lands the user on `/search?q=Trifolium+pratense` and
  the search results page (M-Search-UI-B) renders the hits.
- A binomial the catalog doesn't know about (`Apis mellifera` if no insect
  contribution covers it yet) still wraps in a search link, lands on the
  search page, and the page renders an empty-state with an
  "add-to-collection" affordance — no italics-only graceful-degradation
  case.
- Catalog smoke test still passes (no exceptions, output non-blank,
  char-count grows or holds). Numbered-list lifting and taxonomic-header
  extraction are unchanged.

**Notes.**
- The "graceful degradation" tested in M7 (italics-only when the catalog
  cannot resolve) is no longer a feature — it was a workaround for the
  routing model's binary state. Under search, the empty-state *is* the
  graceful degradation, and it lives on the search results page, not in
  the renderer.
- `LinkResolver`'s removal cascades through `PlantsController`'s
  constructor and any tests that built a controller for fixture rendering.
  Both reductions in surface area.
- Common-name detection inside description prose (e.g. spotting "pipevine
  swallowtail" in a paragraph and wrapping it as a search affordance) is
  out of scope for this milestone. It would require a second pass against
  the tokens harvested by the contribution — non-trivial, easy to get
  wrong, and the search box gives the user the same access without it.
  Defer; revisit if reader behaviour suggests it is wanted.

---

### M-Search-UI-A — Persistent search box in console layout ✅

**Goal.** A search input on every page, in the layout shell, so the
discovery affordance is one keystroke away regardless of where the
naturalist is reading.

**Read first.**
- `naturalist-web/console/src/main/jte/layout.jte` (or whatever the base
  layout template is — locate during the session).
- `domains/plants/plants-console/src/main/jte/plants/detail.jte` to confirm
  the layout-include pattern.
- M-Search-UI-B output, since the form action target lives there.

**Build.**
- A new JTE include `naturalist-web/console/src/main/jte/_search-box.jte`
  rendering a `<form action="/search" method="get">` with a single
  `<input name="q" type="search">` and a submit button. No JavaScript —
  the form GET takes the user to the search page.
- Include the partial in the base layout near the persistent navigation
  / context card. If the layout currently does not have a "header" or
  "shell" region for cross-page elements, this milestone introduces one
  — minimal, semantic HTML, the styling is a follow-up.
- The layout include carries a small empty-state hint: when the user is
  on a detail page, show a "Search the catalog" placeholder; when the
  user is on a search-results page already, the box is pre-filled with
  the current query (driven by a layout parameter the search controller
  sets).

**Acceptance.** Visual review: navigate to `/plants/crimson-clover`, see
the search box; navigate to `/chemistry/aristolochic-acid`, see the search
box; submit `aristolochia` from either page, land on `/search?q=aristolochia`.

**Notes.**
- Pre-filling the search box on the search page is a layout-parameter
  pass-through, not a session-scoped concern. Each request carries `q`;
  the controller exposes it as a model attribute; the layout reads it.
- This milestone has no kernel involvement and no domain involvement.
  Pure console layout work.

---

### M-Search-UI-B — Search results page

**Goal.** A `/search?q=...` route in the console that runs `Catalog.search`,
groups hits by domain, and renders each group with the existing per-domain
URL conventions. Owns the empty-state UX for no-hit searches.

**Read first.**
- M2′ outputs (`Catalog.search`, `SearchResults`, `SearchHit`).
- The console's existing URL conventions (`PlantsController`,
  `ChemistryController`, `InsectsController` route definitions) — the
  results page needs a small per-domain URL builder to render each hit
  as a link.
- `naturalist-web/console`'s controller layout for placing
  `SearchController`.

**Build.**
- `naturalist-web/console/src/main/java/com/naturalist/console/search/SearchController.java`.
  One handler `@GetMapping("/search")` taking `@RequestParam("q") String q`.
  Body: trim and reject blank → render empty-state with no observation;
  call `catalog.search(q)`; expose the resulting `SearchResults` (and the
  query string itself) as model attributes; render
  `search/results.jte`.
- `naturalist-web/console/src/main/jte/search/results.jte` rendering:
    - The query string in a heading.
    - A `SearchResults.groupedByDomain()` iteration — one section per
      domain that produced hits, in `DomainId` declaration order.
      Domain header (e.g. "Plants"), then a `<ul>` of links.
    - Each link: anchor text is the `EntityName` slug; `href` is built
      via a small `Map<Class<? extends EntityName>, Function<EntityName,
      String>>` (the URL builder map removed from the renderer in M7′
      reappears here, where it actually belongs — generating links to
      entity detail pages from `EntityRef`s).
    - A "matched: {token}" hint per hit when the token is not the slug
      (so a user searching `pipevine` and landing on a hit whose slug is
      `california-pipevine` understands why).
    - Empty state: when `SearchResults` is empty, render copy along the
      lines of "The catalog doesn't know `{q}` yet. If this is something
      you've observed in the field, add it to your collection — a future
      naturalist will thank you." The collection-add affordance itself is
      a stub button until the `Naturalist` domain ships.
- Wire `SearchController` in the console's composition root with the
  assembled `Catalog` and the URL builder map.
- The empty-state path fires `UnresolvedSearchObservation` at INFO via
  the kernel (the firing site is in `InMemoryCatalog.search` from M2′; this
  milestone confirms it is wired through the console's observer).

**Acceptance.**
- `/search?q=aristolochia` returns one or more plant hits and renders the
  Plants domain section.
- `/search?q=trifolium` returns both Trifolium species under Plants,
  proving the de-collision behaviour from M4′.
- `/search?q=zzz-no-such-thing` renders the empty state and fires one
  INFO observation visible in logs.
- `/search` with no `q` param renders the empty form state without firing
  an observation.
- Submitting the search box from any page (M-Search-UI-A) lands here
  with the expected results.

**Notes.**
- The URL builder map's home is debatable. It currently sits in the
  console's composition root because that is where domain-to-URL
  knowledge naturally accumulates. If a second consumer ever needs the
  same map (RSS feed, JSON API), promote it to a shared
  `naturalist-web/console-shared` module — not before.
- The `matched: {token}` hint is the explainability feature that makes
  the search feel honest. A user who searches `pipevine` and gets
  `california-pipevine` understands; a user who searches `pipevine` and
  gets `pipevine-swallowtail` understands equally; a user who searches
  `pipevine` and gets results without the hint is left wondering.

---

## Revision: M8 — Chemistry detail back-references ✅

**Supersedes.** The original M8 in `PLAN.md` (which assumed the M7
`LinkResolver` was still around).

**What shipped.**
- `naturalist-web/console/src/main/java/com/naturalist/console/catalog/CatalogConfiguration.java`
  — first Spring `@Bean Catalog`, replacing the inline `CatalogAssembly.from(...)`
  call SearchController used to make. Wires `PlantCatalogContribution` (forward)
  and `PlantCompoundReferences` (inverse) using the existing
  `PlantsTestContext` until proper Spring-managed test data lands.
- `SearchController` now constructor-injects `Catalog` (its TODO comment
  resolved). Behavior unchanged.
- `chemistry-console`, `plants-console`, and `insects-console` each gain a
  single new dependency: `catalog` (interface only). No cross-domain
  coupling — every linker handles only the `EntityName` types its own
  domain owns.
- New `EntityRefLinker` SPI in `kernels/catalog` — third axis of the
  contribution model alongside `CatalogContribution` (forward) and
  `EntityReferences` (inverse). Returns `null` for any `EntityRef` whose
  name type the linker does not own.
- Per-domain `@Component` linkers
  (`PlantsLinker`, `ChemistryLinker`, `InsectsLinker`) live in their
  respective console modules under `…/console/catalog/` — the single place
  to look when a detail-page route is added or moved.
- `naturalist-web/console/.../catalog/CompositeEntityRefLinker` —
  `@Component @Primary` walking every per-domain `EntityRefLinker` Spring
  discovers, returning the first non-null URL. Self-injection guarded
  with `delegates.stream().filter(l -> l != this)`.
- `BackReferencesViewModel` in
  `domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/catalog/`
  takes the composite `EntityRefLinker` and asks it for each ref's URL.
  Refs with no URL are dropped (no dead anchors). Display names sourced
  from an exhaustive switch over the sealed `DomainId` permits.
- `ChemistryController` constructor-injects `Catalog` and `EntityRefLinker`,
  calls `catalog.findReferencesTo(compoundName)` in the detail handler, and
  passes both into `BackReferencesViewModel.from(...)`.
- `SearchController` drops its inline `urlBuilders` map in favor of the
  same composite `EntityRefLinker` — one place for both the search-results
  page and the back-references panel to ask "where does this ref link to?"
- `chemistry/backReferences.jte` partial — renders nothing when empty
  (no "no references found" banner, per the original M8 instruction); a
  "Found in" section with one sub-section per domain otherwise.
- `chemistry/detail.jte` accepts a defaulted `BackReferencesViewModel`
  param and includes the partial below the products section.

**Notes.**
- The view-model lives in `chemistry-console`, not in
  `naturalist-web/console`, even though the URL conventions span domains.
  Rationale: a future Lucene swap or RSS-feed consumer is more likely to
  want a domain-local back-references view than to want a shared
  "linkifier service." With the linker SPI now in place, the URL
  conventions are no longer hard-coded inside the view-model — each
  domain owns its routes — so the dependency graph stays simple even as
  more consumers arrive.
- The per-domain linker pattern was chosen over a fully-qualified-name
  router or a shared linker bean because it is *discoverable*: a future
  contributor adding a new entity to the plants console knows to look at
  `PlantsLinker` to wire its route. Each domain owns one class. Spring
  composes them automatically via `CompositeEntityRefLinker`; no central
  registry to keep in sync.
- No insect-side `EntityReferences` provider exists yet, so insect
  back-references will not appear on a chemistry detail page even though
  the panel and `InsectsLinker` are structurally ready for them. Adding
  an insect provider ships independently — no chemistry-side change
  needed.

**Acceptance.** Visual review (run `naturalist-web/console`):
- Navigate to `/chemistry/aristolochic-acid-i` — the "Found in" section
  shows a Plants subsection with `california-pipevine` and the
  corresponding constituent.
- Navigate to a compound with no plant references — no "Found in" section
  is rendered.

---

## Revision: M9 — Observer wiring and metrics (split into two observation types)

**Goal.** Translate both observation types into the operational surface.
Two counters, two log severities; the cardinality discipline is the same
across both.

**Read first.** M1's observation types as revised above
(`UnresolvedSearchObservation`, `UnresolvedReferenceObservation`); existing
observer infrastructure in `kernels/framework/observability/`;
`kernels/framework`'s `Metric.java`.

**Build.**
- `naturalist-web/console/src/main/java/com/naturalist/console/catalog/MicrometerSearchMissObserver.java`
  subscribing to `UnresolvedSearchObservation`. Increments counter
  `naturalist.catalog.search_miss_total` with tags `(query)` (and only
  `query`) — the search input is the only meaningful dimension. Bounded
  LRU defends `query` cardinality with `__overflow__` fold-in. Default
  cap 256, configurable.
- `naturalist-web/console/src/main/java/com/naturalist/console/catalog/MicrometerUnresolvedReferenceObserver.java`
  unchanged in shape from the original M9 plan: counter
  `naturalist.catalog.unresolved_reference_total`, tags `(source_domain,
  source_name, target_type, target_name)`, same LRU discipline.
- A `LoggingSearchMissObserver` emitting one INFO log line per search
  miss, with the query and the request path (where the search was issued
  from, if available).
- A `LoggingUnresolvedReferenceObserver` emitting one WARN log line per
  inverse-direction dangle.
- Wire all four observers in the console's composition root.

**Acceptance.** Synthesise both kinds of observation in a test app
context; verify each counter increments with the expected tags and the
log line is emitted at the expected severity. Verify the cardinality cap
on the search-miss counter by injecting 300 distinct queries and
confirming the overflow tag captures the surplus.

**Notes.**
- The search-miss counter is the catalog's growth signal. A periodic
  report sorted by counter value is the catalog's most valuable feedback
  loop. Surfacing that report inside the app (an `/admin/search-misses`
  page) is a natural follow-up but out of scope here.
- The two observation types deliberately do not share a parent type. They
  are different signals at different severities for different audiences;
  collapsing them under a common abstract type is a unification that
  saves no code and obscures the distinction.

---

## Revision: M10 — Catalog coverage assertion (was: eager startup validation)

**Goal.** At app startup (or as a scheduled task — see note), verify that
every entity in every contributing domain is reachable through the search
index by at least its slug. Misses are not crashes; they are observations
that a contribution is incomplete.

**Read first.** M2′ output (the assembled `InMemoryCatalog` and its index),
M9-revised, the contributing domains' `*TestEntitySource` classes.

**Build.**
- An `CatalogCoverageValidator` invoked from the app's composition root after
  the catalog is assembled. For each contribution, walks each contributed
  `SearchableEntity` and asserts that `catalog.search(entity.target.name())`
  returns a `SearchResults` containing the `target` under `EXACT_SLUG`.
  Misses fire a new `IncompleteContributionObservation` (WARN) — distinct
  from `UnresolvedSearchObservation` because the failure mode is "a
  contribution emitted an entity but its tokens did not include the slug,"
  which is a programming error, not a user-experience signal.
- The validator runs once at startup. Continuous monitoring is the user's
  open question — a scheduled re-run on the same code path is a one-line
  addition (`@Scheduled(fixedRate = ...)` in Spring) that would catch
  drift between the catalog data files and the running index. Defer the
  scheduling decision to deployment time; the validator class should be
  callable both ways.
- The walker requires no new SPI extension — `CatalogContribution.searchableEntities()`
  already enumerates everything. The original M10's
  `ValidatableReferences extends EntityReferences<T>` is no longer needed
  for the search-direction case; if inverse-direction startup walking is
  separately desired, that is its own (smaller) milestone.

**Acceptance.** Boot the console app with a deliberately broken
`PlantCatalogContribution` that emits an entity without including its slug
in the token stream; the WARN observation fires during startup; the app
does not fail to boot. Restore the contribution and verify zero
observations on a clean boot.

**Notes (carried forward from the user's annotation on the original M10).**
The user wanted continuous monitoring + fast startup. The split lets us
have both: the validator runs at startup *and* on a schedule, the
observations flow through the same pipeline, and a boot is never blocked
on a coverage failure. A separate health-check endpoint that surfaces the
last validator run's result is a natural follow-up.

---

## Revision: Open questions

The original plan's three open questions partially dissolve and partially
sharpen under the search reframe.

- **Domain enumeration.** Unchanged. `DomainId` constants stay closed; a
  new domain is still a one-line edit. This was settled in the original
  plan and the search reframe doesn't disturb it.
- **Provider exception policy.** Sharpened. Under search, a misbehaving
  contribution shrinks the result set; under inverse routing, a
  misbehaving provider shrinks a back-references panel. Both are graceful
  degradation. Both fire an observation. **Tentative pick:** partial
  result plus observation, in both directions. Same answer the original
  plan gave; the reframe just confirms it.
- **`commonNames` schema location.** Settled by M1.5 + M4′. `CommonName` is
  a `field-notes` value object; entities that want common names carry
  `Set<CommonName>` directly. The "coordinated change across api modules"
  framing was right — the change does land per-entity — but it is no
  longer blocking, because each entity adds the field at its own pace
  driven by the milestone that needs it.

A new open question worth flagging:

- **Common-name detection inside description prose.** M7′ wraps detected
  binomials as search affordances but does not wrap detected common names
  (a user reading "pipevine swallowtail" in a paragraph would not see the
  phrase as a search affordance unless they manually selected it). A
  second renderer pass that scans for harvested common-name tokens is
  technically feasible but easy to get wrong (false positives on
  ambiguous phrases like "clover" inside non-botanical contexts). **Open:
  defer until reader behaviour suggests it is wanted; revisit after the
  search box has been live long enough to see how often users hand-type
  common names from prose.**

---

## Out of scope (revised)

The original "out of scope" list mostly stands. One item moves:

- **Full-text search.** Was out of scope. *Is now in scope* — the entire
  reframe is full-text search, just with a deliberately simple mock
  implementation. Production substitution with Lucene is the natural
  evolution. The original plan's intent (no advanced search-engine
  features as part of the catalog effort) survives: stemming, scoring,
  fuzzy matching, query DSL — all out of scope. The kernel ships exact
  token matching and prefix matching; everything beyond that is the
  Lucene drop-in's responsibility.

Still out of scope:

- A graph-shaped query API. Routing-not-graph survives unchanged in the
  inverse direction.
- Persistence of the index. Memory-resident; rebuilt at app startup.
- Cross-app eventing. Each app's catalog reads its own contributions at
  assembly time and accepts staleness between deploys.
- The `Naturalist` domain's personal collection aggregate. Its own
  effort, not a catalog extension. The catalog needs no awareness that
  collections exist; the console's controllers compose the two surfaces
  at render time.
