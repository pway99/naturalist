# Catalog Kernel — Effort Plan

A naturalist's catalog is a cross-referenced body of knowledge that helps you
navigate between species, ranges, and ecological relationships. The `catalog`
kernel plays the same role inside this codebase: a cross-domain navigation
surface the management console renders against, and a routing layer through
which one domain discovers another's references to its entities.

This document is the durable plan. It is structured so any one milestone can
be picked up in a fresh session without reading prior chat history — the
architectural decisions are stated declaratively at the top, and each
milestone lists the exact files to read first before doing the work.

> **Plan history.** The original plan (M0–M12) framed the forward direction
> as exact-match routing (`Catalog.resolveAlias`). On 2026-04-29 it pivoted
> to search-and-discovery: the same contribution-and-assembly architecture,
> but the assembled `Catalog` answers `search(text)` instead. Routing
> milestones M2, M4, M7 shipped successfully and were superseded by M2′,
> M4′, M7′. The inverse-direction work (M3, M5, M8) was unaffected. This
> document is the post-merge canonical plan; the redirect was folded in
> 2026-05-03.

---

## Why this exists

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
each domain, assembled per app, queried by the console. The forward
direction is *search* — many possible answers, including none, with the
empty state itself being a feature (it is the catalog's growth signal). The
inverse direction is *routing* — a deterministic fan-out across domains
that opt in to answering a particular reference type.

---

## Architectural decisions (declarative, do not re-derive)

**Module placement.** `kernels/catalog/`, sibling to `kernels/field-notes` and
`kernels/taxonomy`. Depends only on `framework` and `identifiers`. Nothing in
the DAG depends on catalog except domain `<domain>-core` modules (which
contribute) and console / app modules (which consume). The kernel itself
contains contracts and a default in-memory assembly — no domain knowledge.

**Three SPIs, one kernel.** Search, inverse routing, and link resolution are
separate concepts that share the contribution-and-assembly pattern. All three
live in `kernels/catalog/`:

- `CatalogContribution` — search direction. A domain declares the searchable
  entities it owns and the surface forms (tokens) under which each is
  findable. Used by the console's search box to answer "what does the
  catalog know about *aristolochia*?" with potentially many hits across
  potentially many domains.
- `EntityReferences<T extends EntityName>` — inverse direction. A domain
  declares it can answer "give me my entities that reference this foreign
  `EntityName`." Used by the console to render "Found in:
  california-pipevine (plants), pipevine-swallowtail (insects)" on the
  `aristolochic-acid` detail page.
- `EntityRefLinker` — URL composition. Each console module ships one linker
  that knows the route conventions for its own domain; a composite walks
  every linker and returns the first non-null URL. Returns `null` for any
  `EntityRef` whose name type the linker does not own.

Each domain implements zero or more of each. Contributions are collected in
each app's composition root.

**Search returns many. Routing returns one set per domain.** The two SPIs
have intentionally different return shapes:

- `Catalog.search(String)` returns a `SearchResults` collection. Zero, one,
  or many hits, ordered by `MatchKind` (exact slug, exact token, prefix).
  Each hit carries the matched token so the UI can explain *why* this
  entity surfaced. Empty results are a first-class state — they fire an
  `UnresolvedSearchObservation` at INFO and the search-results page renders
  an empty-state copy that points the user toward adding the term to a
  collection.
- `Catalog.findReferencesTo(EntityName)` returns `Map<DomainId,
  List<EntityRef>>`. The contract is exhaustive — every domain that opted
  in to answering this reference type fans out concurrently — and the
  result is grouped by domain because that is how the console renders it.

**Mock index, not Lucene.** The kernel ships an in-memory token index:
lowercased whitespace-and-punctuation tokenisation, `Map<String,
Set<EntityRef>>`, no scoring, no stemming, no analyzers. The mock proves
the UX and the SPI shape. Production replaces the index implementation
behind the `Catalog.search` interface with Lucene; no contributing domain
or consuming console changes. The kernel never sees the implementation
difference.

**Derive search tokens, do not register.** A domain's
`CatalogContribution.searchableEntities()` yields `(EntityRef, Stream<String>
tokens)` per entity, derived from the entity's own data — slug, scientific
binomial, genus, abbreviated binomial, common names. Adding an entity
automatically contributes its tokens; adding a common name is one edit in
one place (the entity's JSON record, the same file the entity is authored
in).

**Token collisions are a feature.** A search for `Trifolium` returns both
species, the user picks. `InMemoryCatalog` indexes every contributed token
without rejection; multiple `EntityRef`s per token is the normal case.

**Common names are typed, not strings.** A `CommonName` value object lives in
`kernels/field-notes` (same kernel as `Description` — both are vernacular
field-naturalist vocabulary). Components are `(String label, Locale locale)`.
Equality by value. `field-notes` continues to depend only on `framework`.

**Soft validation via Observer at INFO/WARN.** Forward-direction misses (a
search that returns nothing) are recorded as `UnresolvedSearchObservation`
at INFO. This is the catalog's growth signal — aggregated over time, the
most-searched terms with no hits are the entities most worth adding next.
Inverse-direction dangling references (a `PhytochemicalConstituent`
referencing a `CompoundName` the chemistry catalog does not have) are
`UnresolvedReferenceObservation` at WARN. Two observation types, two
severities, same pipeline.

**`DomainId` is open** (per ADR-023). Each domain's `*-api` ships its own
`DomainId` subtype carrying its slug; the catalog assembly validates slug
uniqueness across registered subtypes at startup. The kernel knows the
names of no domains.

**Per-app composition, not a shared registry module.** The assembled
`Catalog` instance lives in each app's composition root. Each app collects
the contributions of the domains it includes; a registries-parent module
that depends on every domain is explicitly rejected.

---

## Module layout

```
kernels/catalog/
  pom.xml
  src/main/java/com/naturalist/catalog/
    Catalog.java                          — public-facing query interface (search + inverse)
    CatalogContribution.java              — search-direction SPI (searchable entities + tokens)
    EntityReferences.java                 — inverse-direction SPI (back-references)
    EntityRefLinker.java                  — URL-composition SPI
    SearchResults.java                    — BehavioralCollection<SearchHit> (ADR-011)
    SearchHit.java                        — ValueObject: target + matchedToken + matchKind
    MatchKind.java                        — enum: EXACT_SLUG, EXACT_TOKEN, PREFIX
    EntityRef.java                        — typed reference to an entity in some domain
    DomainId.java                         — open interface (ADR-023)
    UnresolvedSearchObservation.java      — INFO; forward-direction miss
    UnresolvedReferenceObservation.java   — WARN; inverse-direction dangle
    InMemoryCatalog.java                  — token index + provider routing
  src/test/java/com/naturalist/catalog/
    InMemoryCatalogTest.java
    ...

kernels/field-notes/src/main/java/com/naturalist/fieldnotes/
  CommonName.java                         — ValueObject: (label, locale)
```

Each domain that participates ships its providers in `<domain>-core` (where
repository queries live) and its linker in `<domain>-console`.

---

## DAG impact

```
catalog            →  framework, identifiers
<domain>-core      →  ..., catalog      (when the domain contributes providers)
<domain>-console   →  ..., catalog      (linker only — no api dependency)
console / app      →  ..., catalog      (consumes the assembled Catalog)
```

No new arrows from `<domain>-api`. No arrows from catalog to any domain.
Provider implementations that need domain repository access live in `-core`.

---

## Milestone status

Shipped milestones below carry compressed "what shipped" notes; the full
implementation is in code and git history. Pending milestones carry their
full bodies — that is the working brief for the next session.

| Milestone                                         | Status    | Notes                                                         |
|---------------------------------------------------|-----------|---------------------------------------------------------------|
| M0 — Scaffold the module                          | ✅ shipped |                                                               |
| M1 — Shared types (`EntityRef`, `DomainId`, …)    | ✅ shipped |                                                               |
| M1.5 — `CommonName` value object                  | ✅ shipped |                                                               |
| M2′ — `Catalog.search` and the token index        | ✅ shipped | supersedes the routing-era M2                                 |
| M3 — Inverse SPI                                  | ✅ shipped |                                                               |
| M4′ — Plants searchable contribution              | ✅ shipped | supersedes M4; emits all derivable tokens unconditionally     |
| M5 — Plants `EntityReferences<CompoundName>`      | ✅ shipped |                                                               |
| M6 — Description renderer (formatting only)       | ✅ shipped | M6a + M6b                                                     |
| M7′ — Renderer search affordances                 | ✅ shipped | supersedes M7; binomials wrap as `/search?q=…` links          |
| M-Search-UI-A — Persistent search box             | ✅ shipped |                                                               |
| M-Search-UI-B — Search results page               | ✅ shipped |                                                               |
| M-Insects-Catalog — Insects forward contribution  | ✅ shipped |                                                               |
| M-Chemistry-Catalog — Chemistry forward + inverse | ✅ shipped | compound + product searchable; compound → product inverse     |
| M8 — Chemistry detail back-references             | ✅ shipped | introduces `EntityRefLinker` SPI + `CompositeEntityRefLinker` |
| M9a — Kernel `Level` tag on emitted metrics       | ✅ shipped |                                                               |
| M9b — Typed observation types + console observers | ⏳ pending | full body below                                               |
| M10 — Catalog coverage assertion                  | ⏳ pending | revised; full body below                                      |
| M11 — ArchUnit guard for missing contributions    | ⏳ pending | full body below                                               |
| M12 — Documentation and ADR                       | ⏳ pending | full body below                                               |

### Shipped milestone notes (compressed)

**M0** — `kernels/catalog/` registered in `kernels/pom.xml` and root pom's
`dependencyManagement`; empty `src/main/java` and `src/test/java` trees.

**M1** — `EntityRef` (`(DomainId, EntityName)`), `DomainId` (originally
sealed; opened in ADR-023), `UnresolvedReferenceObservation` (later split;
see M9a/M9b). Each with valid + invalid invariant unit tests.

**M1.5** — `CommonName(String label, Locale locale)` in
`kernels/field-notes/`. `@JsonCreator` on the two-arg `of(...)`. Invariants:
`notBlank(label)`, `notNull(locale)`. `Locale` chosen over a custom
`LanguageTag` value object so Jackson serialises BCP-47 natively.

**M2′** — `Catalog.search(String) → SearchResults`. `MatchKind` enum
(`EXACT_SLUG`, `EXACT_TOKEN`, `PREFIX`); `SearchHit` ValueObject;
`SearchResults` `BehavioralCollection<SearchHit>` (ADR-011).
`CatalogContribution.searchableEntities() → Stream<SearchableEntity>` where
`SearchableEntity` is `(EntityRef target, Stream<String> tokens)`.
`InMemoryCatalog` builds `Map<String, Set<EntityRef>>` at assembly,
lowercased and split on `[\s\p{Punct}]+`. Case-insensitive throughout —
genus capitalisation is a presentation concern.

**M3** — `EntityReferences<T extends EntityName>` with `domain()`,
`referenceType()`, `referencesTo(T) → Stream<EntityRef>`. `Catalog` extended
with `domainsReferencing(Class<? extends EntityName>)` and
`findReferencesTo(EntityName) → Map<DomainId, List<EntityRef>>`.
`InMemoryCatalog` indexes providers by `referenceType()`; fan-out runs live,
no caching at the catalog layer.

**M4′** — `PlantsCatalogContribution` rewritten against the M2′ SPI. Tokens
per plant: slug, `genus + " " + species`, `genus`, `genus.charAt(0) + ". "

+ species`, plus one token per `CommonName` (label only). `Plant` carries
`Set<CommonName>`; `plants.json` populated for the worked-example entries
(`california-pipevine`, `crimson-clover`, `white-clover`).

**M5** — `PlantsCompoundReferences` at
`plants-core/.../plants/catalog/`, takes
`PhytochemicalConstituentQuery.PhytochemicalConstituentEntityQuery` as its
collaborator. Each match emits two `EntityRef`s — one `PlantName`-typed
(navigation), one `PhytochemicalConstituentName`-typed (detail) —
distinguishable by the runtime class of `EntityRef.name()`. Plants
deduplicated via `LinkedHashSet`; constituents are unique by construction.

**M6** — `DescriptionRenderer` at
`plants-console/.../console/render/DescriptionRenderer.java`. Pipeline:
HTML escape → header extraction (leading binomial + em-dash + Family
lifted into `<header class="description-taxonomy">`) → paragraph splitting
on a documented closed-list of cue phrases → numbered-list lifting →
binomial italics. Two-lens test coverage: catalog smoke (every plant ×
every Durrell level) plus pinned worked-example assertions on
`crimson-clover` and `white-clover`. Wired into `PlantsController.detail`
and `plants/detail.jte`.

**M7′** — `LinkResolver` removed. Every detected binomial wraps as
`<a href="/search?q={url-encoded}" class="discover">` regardless of catalog
membership. `URLEncoder.encode(term, UTF_8)` round-trips abbreviated forms
(`A. californica`). `DescriptionRenderer` no longer takes a `Catalog`
collaborator; `DescriptionRendererCatalogTest` folded into the single
`DescriptionRendererTest`.

**M-Search-UI-A** — `_search-box.jte` partial (form GET, no JS) included in
the base layout. Pre-fills from a `q` model attribute on the search results
page.

**M-Search-UI-B** — `SearchController` at `apps/management-console`, single
`@GetMapping("/search")` handler. Renders `search/results.jte`: heading
echoes the query, sections per `DomainId` from
`SearchResults.groupedByDomain()`, `matched: {token}` hint per hit when the
matched token is not the slug, empty state with a collection-add stub when
no hits. Wires `UnresolvedSearchObservation` through the console's observer
pipeline.

**M-Insects-Catalog** — `InsectsCatalogContribution` at
`insects-core/.../insects/catalog/`. Tokens per `InsectSpecies`: slug,
genus, full binomial, abbreviated binomial. Skips binomial/genus tokens
when species is catalogued at family level (genus null). No common-name
tokens yet — `InsectSpecies` does not carry `Set<CommonName>`.

**M-Chemistry-Catalog** — `ChemistryCatalogContribution` concatenates
compound + product `SearchableEntity` streams. Compound tokens: slug,
`commonName`, chemical `formula`. Product tokens: slug, `displayName`.
`ChemistryCompoundReferences` provides intra-domain compound → product
back-references (same SPI as cross-domain providers; the kernel does not
distinguish). Carries `@Resilient(name = "catalog.fanout")` per ADR-026.

**M8** — `Catalog` becomes a Spring `@Bean` in `CatalogConfiguration`. New
`EntityRefLinker` SPI in `kernels/catalog`: third axis alongside
`CatalogContribution` and `EntityReferences`. Per-domain `@DomainService`
linkers (`PlantsLinker`, `ChemistryLinker`, `InsectsLinker`) live in their
respective console modules under `…/console/catalog/`.
`CompositeEntityRefLinker` (`@Component @Primary`) walks every discovered
linker, returning the first non-null URL; self-injection guarded with
`delegates.stream().filter(l -> l != this)`. `BackReferencesViewModel` in
`chemistry-console` renders the "Found in" panel; refs with no URL are
dropped (no dead anchors). Empty results render nothing.

**M9a** — `kernels/framework/.../observability/Level.java` — `enum
Level { INFO, WARN, ERROR }` with `tagValue()` (lowercased constant name
for Micrometer). `InvariantObservation.observe()` becomes
`observe(Level level)`. `throwWhenInvalid()` always tags violation
counters at `Level.ERROR`. Both metric counters
(`naturalist.observation`, `naturalist.invariant.violation`) gained a
`level` tag. `InMemoryCatalog.search` miss → `Level.INFO`;
`InsectAggregateFactory` aggregate self-observation → `Level.WARN`.

---

## Pending milestones

### M9b — Typed observation types and console observers

**Goal.** Translate both catalog observation kinds into the operational
surface. Two counters, two log severities; the cardinality discipline is
the same across both.

**Read first.** M1's observation types as revised in M9a; the shipped
`Level` machinery; existing observer infrastructure in
`kernels/framework/observability/`; `kernels/framework`'s `Metric.java`.

**State of inputs.**

- `UnresolvedSearchObservation` exists and is fired from
  `InMemoryCatalog.search` (M2′ wired the firing site behind the observer
  pipeline; M9a tagged the emission `Level.INFO`).
- `UnresolvedReferenceObservation` does **not** yet exist. Firing site for
  the inverse direction is still open (kernel-side in
  `InMemoryCatalog.findReferencesTo` after fan-out vs. inside each
  `EntityReferences` provider vs. a separate validator). Current lean:
  kernel-side, after the fan-out — providers stay logic-free, one firing
  site, one place to test.

**Build.**

- `apps/management-console/.../console/catalog/MicrometerSearchMissObserver.java`
  subscribing to `UnresolvedSearchObservation`. Increments counter
  `naturalist.catalog.search_miss_total` with tags `(query)` (and only
  `query`) — the search input is the only meaningful dimension. Bounded
  LRU defends `query` cardinality with `__overflow__` fold-in. Default
  cap 256, configurable.
- `apps/management-console/.../console/catalog/MicrometerUnresolvedReferenceObserver.java`:
  counter `naturalist.catalog.unresolved_reference_total`, tags
  `(source_domain, source_name, target_type, target_name)`, same LRU
  discipline.
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

### M10 — Catalog coverage assertion

**Goal.** At app startup (or as a scheduled task), verify that every
entity in every contributing domain is reachable through the search index
by at least its slug. Misses are not crashes; they are observations that a
contribution is incomplete.

**Read first.** M2′ output (the assembled `InMemoryCatalog` and its
index), M9a/M9b, the contributing domains' `*TestEntitySource` classes.

**Build.**

- A `CatalogCoverageValidator` invoked from the app's composition root
  after the catalog is assembled. For each contribution, walks each
  contributed `SearchableEntity` and asserts that
  `catalog.search(entity.target.name())` returns a `SearchResults`
  containing the `target` under `EXACT_SLUG`. Misses fire a new
  `IncompleteContributionObservation` (WARN) — distinct from
  `UnresolvedSearchObservation` because the failure mode is "a
  contribution emitted an entity but its tokens did not include the
  slug," which is a programming error, not a user-experience signal.
- The validator runs once at startup. Continuous monitoring is the user's
  open question — a scheduled re-run on the same code path is a one-line
  addition (`@Scheduled(fixedRate = ...)` in Spring) that would catch
  drift between the catalog data files and the running index. The
  validator class should be callable both ways.
- The walker requires no new SPI extension —
  `CatalogContribution.searchableEntities()` already enumerates
  everything.

**Acceptance.** Boot the console app with a deliberately broken
`PlantsCatalogContribution` that emits an entity without including its
slug in the token stream; the WARN observation fires during startup; the
app does not fail to boot. Restore the contribution and verify zero
observations on a clean boot.

**Note.** The user wanted continuous monitoring + fast startup. The split
(validator runs at startup *and* on a schedule) gives both: the
observations flow through the same pipeline, and a boot is never blocked
on a coverage failure. A separate health-check endpoint that surfaces the
last validator run's result is a natural follow-up.

### M11 — ArchUnit guard for missing contributions

**Goal.** Build-time safety net for the registration step. A new
`EntityName` subclass without an associated catalog contribution somewhere
is a build error.

**Read first.** Existing ArchUnit tests in the project (search for usages
of `com.tngtech.archunit`); `domains/identifiers/`; the post-M9
`ResilienceComplianceTest` in `apps/management-console` as the structural
template.

**Build.**

- `CatalogCoverageTest` in a test-scope module the build can run.
  Discovers all concrete `EntityName` subclasses in `domains/identifiers/`
  and asserts that for each, at least one `CatalogContribution` or
  `EntityReferences<T>` implementation exists in some `<domain>-core`
  module. Failure message names the missing class.
- Runs in app composition test scope (where everything is on the
  classpath together), not in the kernel module.

**Acceptance.** Add a synthetic `EntityName` subclass with no
contribution; the build fails with the expected message. Remove the
synthetic class; the build passes.

### M12 — Documentation and ADR

**Goal.** Capture the architectural decisions in a permanent ADR so future
contributors find them in `docs/adr/` rather than re-deriving them from
this plan.

**Build.**

- New ADR (next sequence number under `docs/adr/`). Records: the catalog
  kernel, the three SPIs (`CatalogContribution`, `EntityReferences`,
  `EntityRefLinker`), the search-not-routing decision, the
  soft-validation-via-observer decision, the per-app composition decision.
- Cross-references ADR-023 (open `DomainId`) and ADR-026 (resilience on
  the catalog fan-out).
- Update `kernels/CLAUDE.md`'s catalog section to reference the new ADR.

**Acceptance.** ADR merged; `kernels/CLAUDE.md` references it.

---

## Open question — slug-shaped multi-token search input

Surfaced while writing `InsectsCatalogContributionTest`. The M2′
tokenisation rule (`[\s\p{Punct}]+`) splits hyphenated input the same way
it splits whitespace, so a slug-shaped query like
`not-an-insect-anywhere` becomes `["not", "an", "insect", "anywhere"]` and
the per-token union returns spurious prefix hits.

**Decision (provisional).** Do not introduce a slug-shape detector or
conjunctive multi-token semantics yet. The catalog-inmem implementation is
bare-minimum dev-time scaffolding, not a search engine; piling heuristics
on it is the slippery slope toward re-implementing Lucene in Java. The
negative test case `unknownTokenReturnsEmptyResults` was narrowed to a
single nonsense token (`"zzzzzzz"`) and the slug-shaped assertion was
removed.

**Revisit when.** The Lucene drop-in lands (search semantics move out of
the kernel entirely), or user behaviour shows real confusion from
slug-shaped queries.

---

## How to resume across sessions

A fresh session picking up this work should:

1. Read this PLAN in full (it is sized to fit easily).
2. Run `git log --oneline kernels/catalog/` to see which milestones have
   shipped.
3. Look at the `Read first` list of the next pending milestone and read
   only those files.
4. Do the milestone. Update its row in the **Milestone status** table
   from ⏳ pending to ✅ shipped, and append a compressed "what shipped"
   note under **Shipped milestone notes**, as part of the same commit. Do
   not edit milestones beyond the one being executed — if a discovery
   during work changes a later milestone, append a note under it, do not
   rewrite it.

If a milestone proves bigger than expected mid-session, split it: leave
the original milestone partially done, add a new milestone immediately
after for the leftover, and surface the split in the commit message.

---

## Open questions

Worth deciding before M9b, but not blocking earlier work.

- **`commonNames` schema location.** Settled by M1.5 + M4′. `CommonName` is
  a `field-notes` value object; entities that want common names carry
  `Set<CommonName>` directly. Each entity adds the field at its own pace
  driven by the milestone that needs it.
- **Provider exception policy.** Under search, a misbehaving contribution
  shrinks the result set; under inverse routing, a misbehaving provider
  shrinks a back-references panel. Both are graceful degradation. Both
  fire an observation. Resolved in code: partial result plus observation,
  both directions.
- **Common-name detection inside description prose.** M7′ wraps detected
  binomials as search affordances but does not wrap detected common
  names. A second renderer pass that scans for harvested common-name
  tokens is technically feasible but easy to get wrong (false positives
  on ambiguous phrases like "clover" inside non-botanical contexts).
  Defer until reader behaviour suggests it is wanted; revisit after the
  search box has been live long enough.

---

## Out of scope

These are explicitly *not* part of this effort. Each is a worthy
follow-up:

- **Lucene-backed `Catalog` adapter.** The mock kernel index ships exact
  token + prefix matching; everything beyond that (stemming, scoring,
  fuzzy matching, query DSL) is the production adapter's responsibility.
  The kernel SPI shape is stable enough that the swap is mechanical; the
  adapter lives under `adapters/catalog-lucene/` (or
  `adapters/catalog-solr/`) when it lands.
- A graph-shaped query API (`catalog.shortestPath(a, b)` or similar). The
  routing model does not preclude this but does not deliver it.
- Persistence of the index. Memory-resident; rebuilt at app startup.
- Cross-app eventing. Each app's catalog reads its own contributions at
  assembly time and accepts staleness between deploys.
- The `Naturalist` domain's personal collection aggregate. Its own
  effort, not a catalog extension. The catalog needs no awareness that
  collections exist; the console's controllers compose the two surfaces
  at render time.
