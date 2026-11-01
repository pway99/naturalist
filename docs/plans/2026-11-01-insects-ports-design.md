# Insects ports — design

**Status:** design, reviewed by builder check; awaiting approval. Source: the adapter review at
[`docs/notes/2026-11-01-adapter-hexagonal-review.md`](../notes/2026-11-01-adapter-hexagonal-review.md),
shapes 1, 2, 4 and 5 for insects. The clade/placement rows (shape 3) are a
separate effort and are a non-goal here.

## Intent

`InsectsController` must become a pure delegating adapter: bind the request
into typed identifiers, make **one** api call, render what comes back. The
reason is adapter replaceability. Today the app is JTE/HTMX on Spring Boot;
tomorrow it may be a mobile app, a JavaScript front end over GraphQL, or
Micronaut/Quarkus. Any of those must get the *exact same experience* from
`insects-api` + `insects-core` without copying anything out of the console.
The test for every decision below: "would a GraphQL adapter written tomorrow
against this api get the same page for free?"

## Goals

1. Every insects write is a named use case on `InsectCommand`, implemented
   in `insects-core` and discovered by `@DomainService` scan. The console
   constructs nothing from core.
2. Every insects page is one api call returning a read model that already
   carries everything the page shows, composed and ordered: the viewer's
   collection state, the viewer's photos paired with their observations,
   ordered life stages, display names and binomials, species-list cards.
3. Image handling is reduced to sniff, cap, store, serve, behind one kernel
   port with one filesystem adapter. No server-side transcoding or caching.
4. The two copied image-storage services and the controller's HEIC path are
   deleted.

## Non-goals

- Clade placement, the `InsectCladeAnchors` override map, the `*PlacedAt`
  scans, `CLADE_RANK_LABEL`, breadcrumb/intro assembly. Those are the clade
  effort. After this work they are the only non-delegating code left in
  `InsectsController`, and the design must not make them harder to move.
- Porting the same shapes to plants. Reconcile-first: plants gets the
  identical treatment afterwards by copying what lands here.
- Registration/login in the app, catalog ranking, persistence findings.
- A cross-domain composing module. Decided against (below).

## Anti-goals

- Any stream filter, sort, dedup, `new <DomainRecord>(...)`, `Instant.now()`,
  ownership test, or multi-port join left in the controller or a template.
- A new dependency from `insects-core` on `naturalists-api`. Not needed.
- Server-side image resize/convert "for convenience". The client resizes.
- Weakening any gate, test, or enforcement to get green.

## Constraints

- Root `CLAUDE.md` DAG and non-negotiables; `domains/CLAUDE.md` (ADR-010 thin
  queries, ADR-020 namespace nesting, batched fan-out, repository contract
  tests, record conventions); `kernels/CLAUDE.md` (Observer usage).
- No `Service` suffix on new types (Command/Query/Transaction naming).
- Engine-first writes: Java changes go through Kaname `apply_change` atoms;
  hand edits only after a recorded atom gap.
- Both N+1 gates must pass for the new core code; the console's current
  fan-outs disappear rather than being whitelisted.
- `mvn verify` plus the OpenRewrite enforcement gate before each slice is
  declared done. Trunk-based: merge to local main, no GitHub PR ceremony.
- PR-size discipline (≈400 lines of meaningful diff) → the work is sliced.

## Decisions

### D1. Viewer identity crosses as `NaturalistName`, nothing more

`NaturalistName` lives in `domains/identifiers` and every insect observation
already carries it as `observedBy`. "Collected by the viewer" and "the
viewer's photos" are answered from insects' own observation and image
repositories. So the read side takes an optional viewer name and
`insects-core` gains no module dependency. A cross-domain composing service
module was considered and rejected: DAG rule 2 (core may import another
domain's **api**) is the repo's existing answer and insects-core already
uses it (library-api, usage-api). A composer earns its keep only for a page
that joins several domains' aggregates (a naturalist's profile); that would
be a new domain module of its own, recorded as a parking-lot fork, not a
generic service tier.

### D2. Read side: one call per page

- `InsectQuery.getByName(InsectRankName name, @Nullable NaturalistName viewer)`
  returns `Optional<Insect>`. The existing one-argument form stays and means
  "no viewer". `Insect` gains a collection view (my call on shape, builder
  may refine): `collected` (the viewer has at least one observation of this
  subject) and the viewer's observations **each paired with its images**, so
  no adapter ever does the image→observation join again. `Insect` also
  exposes the deepest placement later (clade effort) — leave room, do not
  build it.
- Life stages come back **ordered by the domain's developmental sequence**
  (`InsectLifeStages.stagesOf(...)` / `Metaboly.stages()`), set by
  `InsectFactory`. The life-stages page reads `getByName`, not four hops.
- Display names and binomials are domain accessors, not template
  expressions: `displayName()` on `InsectOrder/Family/Genus/Species` (first
  common name by a **deterministic** rule, else the scientific name; the
  rule is the builder's call but must be stable), and `binomial()` on the
  species view. Templates call them.
- The species list page gets a card read model built by a core factory
  (species + genus + family names, display name, binomial, role, gallery,
  `collected` for the viewer) returned as a `Page<...>` from a new
  `SpeciesQuery` method taking `(PageRequest, @Nullable viewer)`. The
  "my collection" lens becomes a **paged query over the collection**
  (`collectedBy(viewer, PageRequest)`), never a post-filter of a page.
- Order/family/genus list pages: one call each returning what the page
  renders (they already use `forRankHierarchies` for galleries; collapse to
  one port method if a second call remains). Guild page: the collection
  carries its order.

### D3. Write side: use cases hang off `InsectCommand`

Per ADR-020 the new ports nest inside the existing `InsectCommand`
namespace, keeping the api one tree a human can read. My call on grouping
(builder may merge/split, names bare per ADR-020 §5):

- `InsectCommand.IdentificationCommand identification()` —
  `identify(ImageUpload, @Nullable location, @Nullable Instant capturedAt, NaturalistName, @Nullable notes)`
  → `InsectRankName`. Wraps today's rate gate → budget → vision → enrichment →
  features → citations → transaction, unchanged in substance.
  **The port speaks api + kernel vocabulary only.** DAG rule 1 does not
  admit `kernels/vision` or `usage-api` types on an api signature. The
  photo metadata reuses the existing `kernels/vision` `ImageMetadata`
  (location, capturedAt) rather than a duplicate value type — `vision` joins
  rule 1's allow-list for `insects-api` like any other kernel. The refusal
  is a sealed `IdentificationRefused` exception in the new
  **`kernels/identification`** (decided 2026-11-01; see D6) with the cases
  `RATE_LIMITED` and `BUDGET_EXHAUSTED`, which core raises by translating
  `RateLimitExceededException` / `BudgetExceededException`. Any adapter,
  GraphQL included, learns a refusal from the api alone; the console maps
  it to its redirect message. `insects-api` adds both kernel dependencies
  and root `CLAUDE.md` rule 1 lists `vision` and `identification`.
- `InsectCommand.SightingCommand sightings()` —
  `record(subject, NaturalistName, @Nullable notes)`;
  `updateNotes(InsectObservationId, NaturalistName viewer, @Nullable notes)`.
  Ownership ("only the recording naturalist") is enforced inside
  `updateNotes`; a non-owner is refused the same way an absent observation
  is. **Re-identification is deferred** (decided 2026-11-01): it is the
  effort after the authorization work, built on this port with the
  image-by-observation lookups it needs. The half-built
  `/insects/{name}/re-identify` handler and its `SecurityConfiguration`
  rule are **deleted** in slice 2 (my call: a write path that knowingly
  breaks the image-parent invariant has no place in a delegating
  controller); the deferral is recorded in `docs/work-tracker.md`.
- `InsectCommand.PhotoCommand photos()` —
  `add(subject, ImageUpload, @Nullable NaturalistName, @Nullable notes, @Nullable location)`.
  Stores through the image port **inside** the use case, then persists, so a
  rejected or failed persist never leaves an orphan file (store last, or
  delete on failure — builder's call, must be tested).

Implementations are `@DomainService` classes in `insects-core` with
constructor injection; today's package-private `InsectIdentificationCommand`,
`InsectAddPhotoCommand` and the two transactions become (or are absorbed
by) them. Collaborators the controller builds inline today become app beans:
`FeatureSearch<InsectFeatureId>` (an `InMemoryFeatureSearch` over
`insectQuery.features().corpus()`, declared in a management-console
`@Configuration`) and `TextGenerationService` (`NoOpTextGenerationService`
bean until the Anthropic one is wired). The rate limiter is resolved from the
injected `Resilience` by strategy name inside core. Check how
`InsectCommandImpl`/`InsectQueryImpl` are discovered today and follow it; the
known trap is that a factory-backed query carrying `@DomainService` failed
context startup once (see memory) — verify before annotating.

### D4. Images: client prepares, server sniffs, caps, stores

- The client is responsible for resize and format. Phones already transcode
  HEIC→JPEG on upload for `accept="image/*"`, and both upload forms already
  run `apps/management-console/.../static/js/image-upload.js`, which
  canvas-resizes to 1024 px / JPEG 0.85 and fetch-submits. That script and
  its policy stay as they are (my call: 1024 px is the existing, proven
  size for vision identification). Two fixes ride along: a desktop browser
  handed a raw HEIC cannot decode it and the script currently hangs on
  "Identifying…" because `img.onload` never fires — it must fail fast with a
  message; and with JS off the server's rejection is a 500 today — the
  use case's `InvariantViolationException` must map to the same redirect
  message channel on **both** forms (`identify.jte` has `?limit=`;
  `detail.jte` lacks one and gains it).
- New kernel port in `kernels/observation`: `ImageStore` with
  `FileName store(String space, byte[] bytes)` and
  `Optional<byte[]> read(String space, FileName name)`; `space` is the
  domain namespace (`insects`, `plants`) so one bean serves every domain.
  A small `ImageUpload` value (bytes + declared media type) crosses the
  command port; the **use case** sniffs magic bytes against the whitelist
  (JPEG/PNG/WebP) and applies the size cap (20 MB today; keep, make it a
  named constant on the command) before touching the store.
- One implementation module `kernels/image-store-filesystem` (not
  `adapters/`: the adapters placement rule reserves that tree for
  vendor-dependency implementations; a pure-JDK filesystem store follows the
  `catalog-inmem` precedent and lives in `kernels/`) implementing it over
  a root directory (today `data/images/<space>`), UUID file names, path
  traversal guard, never trusting the client's name. Filesystem calls are
  cross-boundary under `docs/resilience-policy.md`, so the store carries a
  named strategy `image.store` (a timeout, registered in the app's
  `ResilienceConfiguration` in place of the deleted `image.conversion`;
  `AdminResilienceControllerWebMvcTest` and `ResilienceConfigurationTest`
  move to the new name). Replaces
  `insects-console/ImageStorageService` and `plants-console/PlantImageStorageService`
  (plants keeps its endpoint; it only switches to the adapter).
- The 14 insects (and 2 plants) seed photos shipped as HEIC are converted to
  JPEG **once**, committed, and the fixture JSON `fileName`s updated.
  **Slice 1 is not done until the standing `naturalist_test` Postgres is
  reseeded** with `apps/test-db-seeder`, because the stored
  `resource_name`s change and the running console reads that database.
  `GalleryLensWebMvcTest` asserts a literal `.HEIC` name and is updated. The
  `sips` conversion, `jpegCache`, `IMAGE_CONVERSION` strategy and its
  resilience config entry are deleted. Seed images remain classpath
  resources; the filesystem adapter resolves a name from the data directory
  first and falls back to the classpath `<space>/images/` resource — a
  location rule, not a domain one (my call; builder may instead copy seeds
  into the data dir at seed time if that is simpler).
- Serving: the console's two image endpoints collapse to one that calls
  `ImageStore.read` and sets the content type from the stored bytes. A
  kernel port call from an adapter is fine; it is not domain logic.

### D6. A shared identification kernel, not an identification service module

Plants identification follows insects. What the two share is the pipeline
mechanism (rate gate → budget → vision → authority enrichment → parent-rank
text enrichment → feature reuse → citations → transactional catalog write);
what they do not share is the write, which creates the organism's ranks,
images, observations and features under that catalog's own invariants in
its own core transaction. A service module owning identification for all
organisms would perform that write from outside the owning domain, which
the repo has consistently rejected. So, following the `OrganismObservation`
/ `OrganismImage` / `RankAncestry` precedent:

- **Now:** `kernels/identification` (depends on `framework` only) holds the
  one piece of shared vocabulary with no existing home, `IdentificationRefused`.
  It is deliberately **not** `kernels/vision`: vision is the driven-side
  port to the model vendor (`VisionService`, `Image`, `ImageMetadata`,
  `ToolSchema`), kept thin so the Anthropic adapter's seam stays small. A
  refusal for rate or budget is use-case vocabulary from the driving side,
  and the pipeline that follows orchestrates vision *alongside* usage,
  authority, text-generation and feature-search — it cannot live inside one
  of the kernels it orchestrates without making that kernel depend on the
  other four. Each domain keeps `identification()` on its own command
  namespace and its own transaction.
- **When plants identification is built:** the generic pipeline is
  extracted into the same kernel reconcile-first, parameterised over the
  rank-name type the way `OrganismObservation<ID, R>` is, with
  domain-supplied hooks (tool schema, system prompt, rank reconstruction,
  the persistence transaction). Not in this effort.

The split between `kernels/observation` (`ImageUpload`, `ImageStore`:
photo mechanics shared by add-photo and identification) and
`kernels/identification` (identification vocabulary) is my call.

### D5. What the controller looks like after

Every handler is: parse path/query/multipart into typed values →
`currentNaturalist(request)` → one `insectQuery.*` or `insectCommand.*`
call → `model.addAttribute`/redirect. `owns`, `observationLookup`, `reIdentify`,
the constructor's `new` calls, `jpegCache`, `convert`, `image` and the
second upload endpoint are gone. `InsectsControllerNotesOwnershipTest` moves to core
as the sighting command's test. The clade/placement methods remain
(non-goal) and are the only exception.

## Slices (my call; each ends green on `mvn verify` + rewrite gate, merged to local main)

1. **Image store.** Kernel port + value, filesystem adapter, app bean,
   seed HEIC→JPEG conversion, delete `sips`/cache/strategy and both console
   storage services, one serve endpoint per console, inline resize script.
   Independent of the other two.
2. **Write ports.** `kernels/identification` vocabulary; `InsectCommand`
   use cases + `@DomainService` impls + app beans for
   FeatureSearch/TextGeneration; controller write handlers become
   delegation; `reIdentify` handler + security rule deleted and the deferral
   recorded in the work tracker; ownership test moves to core.
3. **Read ports.** Viewer-aware `getByName`, `Insect` collection view,
   ordered life stages, `displayName()`/`binomial()`, species cards +
   `collectedBy` paging; controller read handlers become delegation; the
   nine insects templates switch to the accessors.

Slice 3 will likely exceed the 400-line guideline; split read-model
accessors (display names, binomials, life-stage order) from the viewer lens
and cards if it does.

## Testing expectations

TDD per the repo's convention. Core use cases are tested through
`InsectsTestContextInternal` with the mock repositories; new repository
methods get the three-case contract tests in `insects-repository-test` and
are implemented by both the mock and the rdbms adapter (declare any new
intra-domain FK on the mock). Controller tests stay `@WebMvcTest` slices on
`@MockDomainService` doubles and must get thinner, not thicker. The
image-store adapter gets one small test over a temp directory (it is an
adapter, not test infra).

## Left to the builder

Exact method and record names; whether `sightings()` and `photos()` merge;
the deterministic display-name rule; orphan-file strategy in `photos().add`;
seed-image resolution (classpath fallback vs copy at seed time); how
`identify` reports a budget or rate rejection to the adapter (today:
exceptions caught in the controller and mapped to a redirect — keep the
exceptions on the port, document them).

## Known traps (from project memory)

Removing/adding a record component ripples repo-wide (sibling tests,
TestEntitySource method refs, JTE templates) — search the whole repo. Kernel
signature changes need a clean install before dependants compile.
`spring-boot:run` resolves domain jars from `~/.m2`, so `mvn install` before
running the console. Repository mock methods must validate arguments with the
Observer and have a null-rejection contract test.
