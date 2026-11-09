# Insects Domain

Reference implementation for [ADR-021](../../docs/adr/ADR-021-persistenceid-is-adapter-internal.md)
— persistence-id is adapter-internal; cross-entity references use `EntityName`.

## Domain Vocabulary

**InsectSpeciesInfo** (and `InsectGenusInfo`, `InsectFamilyInfo`, `InsectOrderInfo`) —
`NamedEntity<InsectSpeciesName>` etc. The stored catalog record of a rank as recognised by
the naturalist. Four-level Durrell `Description`. No `id()` at the domain layer; the RDBMS
adapter carries a numeric primary key privately. The `Info` suffix follows the Info pattern
in [`domains/CLAUDE.md`](../CLAUDE.md): the bare name belongs to the composed read model
below. The persistence stack keeps the rank subject (`InsectSpeciesQueryImpl`,
`InsectSpeciesTestEntitySource`, `SpeciesQuery`).

**InsectViews / InsectViewQuery** — presentation read models (`RankPage`, `SpeciesCard`,
`RankCard`, `Viewer`) that `InsectViewQuery` assembles for the console from the biology
`InsectQuery`; nothing in the biology refers to them. The console's read handlers bind,
call one `InsectViewQuery` method and render.

**area / location** — *area* is the `GridCell` (kernel `observation`), the only place the domain keeps
for an observation; `FieldObservation.area` is nullable. The command resolves it with
`GridCell.resolve` from the client's cell or the upload's EXIF GPS, before the upload is stripped, and
hands it to identification as range context. *location* is the naturalist's own typed words,
untouched. The controller only translates the posted `areaLat` / `areaLon` cell indices; it never
reads EXIF and never sees exact coordinates.

**InsectImage** — `Entity<InsectImageId>`. A photograph of an observed individual.
Carries `InsectImageId id`, accessor `id()`. Parent reference is an
`InsectRankName parentName` (slug) — no `insectSpeciesId`, no `withId`, no nullable
id-shaped FK column.

**InsectTaxon** — Sealed `ReadModel` over the four Linnaean ranks that carry
catalog entities. Its four permits are **top-level records** — `InsectSpecies`,
`InsectGenus`, `InsectFamily`, `InsectOrder` — named in an explicit `permits` clause.
Each composes its stored `*Info` record (component `info`) with the `ImageCollection`
of photographs attached at that rank — a read-side projection, not a consistency
boundary. `InsectSpecies` is the species with its photographs; `InsectSpeciesInfo` is
the record it wraps. Identity is the root's typed
`InsectRankName`, returned polymorphically by `name()`. No standalone query — used
only as the rank-chain and child-card building blocks assembled inside the `Insect`
read model, by `InsectFactory`. Permits carry no `features()` slot; features live on
`Insect` as an `OrganismFeatureView<InsectRankName, InsectFeature>`.
`InsectSubspeciesName` is permitted on `InsectRankName` but yields `Optional.empty()`
(no subspecies entity exists yet).

## The Naturalist's Collection

Design source: `docs/plans/2026-07-12-naturalist-insect-collection-design.md`.

**FieldObservation** — `Entity<FieldObservationId>`. The collection unit: a
naturalist's claim to have observed a subject. Carries `observedBy` (`NaturalistName`)
and `subject` (`InsectRankName`) plus `observedOn`/`notes`. A naturalist "has
collected" a species/genus/family/order iff a `FieldObservation` exists with that
`subject` — membership dedups by `subject`, so repeated sightings of the same
subject by the same naturalist do not multiply collection entries. Read via
`insectQuery.fieldObservations()`, written via `insectCommand.fieldObservations()`.

**InsectImage.observationId** — nullable `FieldObservationId` link from a photo to
the `FieldObservation` it was captured under. `null` means a shared catalog image
with no owning naturalist (the pre-collection default). `POST /insects/{name}/images`
(`InsectsController.addImage`) creates a `FieldObservation` and sets this link when a
naturalist is signed in; with no naturalist the image stays owner-less, exactly as
before this feature. `POST /insects/{name}/observe` records a sighting (no photo)
via the same `FieldObservation` insert, without touching `InsectImage`.

**CatalogIdentification** — `Aggregate`. Write-side consistency boundary for insect
catalog identification. Carries four entities the transaction persists (species,
taxonomy, image, observation) with cross-entity FK invariants: image parent must
match species, observation subject must match species, image observation ID must
match observation. Each invariant is a named private method that returns true
vacuously when null (Observer validates nullity separately via `namedEntity`/
`valueObject`).

**InsectCatalogIdentificationTransaction** — `Transaction<CatalogIdentification>`
in `insects-core`. Persists a `CatalogIdentification` atomically: resolves parent
ranks (order → family → genus) idempotently, inserts species if new, then inserts
image and field observation.

**InsectIdentificationCommandImpl** — in `insects-core`. Orchestrates vision
identification → aggregate construction → transactional persistence. Takes
`VisionService` and `InsectCatalogIdentificationTransaction`. Returns
`InsectSpeciesName` (pragmatic CQS exception for controller redirect).

**Current naturalist in insects-console** — `InsectsController` resolves the signed-in
naturalist by reading the `"naturalist.currentNaturalistName"` request attribute
(`currentNaturalist(HttpServletRequest)`), written upstream by the app's
`NaturalistHeaderInterceptor`. The console module never depends on the security/auth
types directly — only on this request-attribute convention — which is why the
attribute key is duplicated as a same-literal constant on both sides rather than
shared through a type.

## Identification Evidence in the Console

Rank-polymorphic identification means most vision identifications land at ORDER or
FAMILY, not SPECIES. The four rank pages therefore carry the same evidence surfaces:

**`features.jte`** — renders `insect.features()`, an
`OrganismFeatureView<InsectRankName, InsectFeature>`
(lineage-composite, ancestor-first, ordinal-ordered within a rank) composed directly
into the `Insect` read model. `InsectFactory` populates it via
`.withFeatures(featureQuery.findByRankName(name))`, symmetric with `.withCitations(...)`;
each rank handler reads it straight off the `Insect` it already resolved — no separate
per-page fetch. `OrganismFeatureView.groups()` are `RankGroup`s (rank + ordinal-ordered
`InsectFeature`s); the template formats each group's rank label with `RankLabel.of(...)`.

**`observationGallery.jte`** — photo, `%` confidence, "Why this ID?" evidence
disclosure, "Also considered" alternatives, and the field-notes form. Sourced from
`FieldObservation.identification()`. The rank pages render `Insect.observations()`,
which `InsectFactory` fills from `insectQuery.images().forParentName(rankName)` — **not**
`forRankHierarchy` — because the child-rank cards on the same page already show
descendant photos via the `gallery` attribute; using the hierarchy query would render
every descendant image twice.

**`citations.jte`** — renders `Insect.citations()`, which resolves through
`CitationAssociation` records in the library domain. An identification that writes a
`Citation` without a matching association produces a silently citation-less page; see
`CitationAssociationJson` in `library-repository-test` for why that catalog needs a
hand-written DTO and how the flush seam keeps it round-trippable.

The notes form posts to `/insects/{name}/notes` from every rank and carries a
`returnPath` hidden field; `InsectsController.safeReturnPath` constrains it to
`/insects/` prefixes so the field cannot become an open redirect.
`InsectsController.updateNotes` delegates to `insectCommand.sightings().updateNotes(id, viewer, notes)`,
which owns the ownership rule: it compares `obs.observedBy()` against the signed-in naturalist and
silently changes nothing on mismatch or a missing id, so the two outcomes are indistinguishable to
the caller (the controller redirects to the same `destination` either way). Before this check, any
naturalist could overwrite another naturalist's field notes by POSTing their observation id —
the id is a UUIDv7 and therefore time-ordered and partially guessable, so the id alone was never
proof of ownership.

**CSRF on the rank pages.** `family.jte`, `genus.jte`, and `order.jte` do **not** take
a `@param CsrfToken _csrf`. They read the CSRF param name and token as plain request
attributes — `naturalistCsrfParam` and `naturalistCsrfToken`, published on every
request by `NaturalistHeaderInterceptor` — the same way
`apps/management-console/src/main/jte/layout/page.jte:34-39` does. `observationGallery.jte`
therefore takes two `String` params, `csrfParam` and `csrfToken`, not a `CsrfToken`.
`detail.jte` still has its own `@param CsrfToken _csrf` (pre-existing, out of scope
for this slice) and adapts to `observationGallery.jte`'s `String` params at the call
site.

This is deliberate, not an oversight: `page.jte` is compiled by every domain-console
module's template tests against classpaths that deliberately lack Spring Security, and
that invariant is enforced *only* by `spring-security-web` being absent from the
console module poms. An earlier attempt at this slice added that jar to
`insects-console/pom.xml` so the rank pages could type their CSRF param as `CsrfToken`
directly, and it was reverted in review. Reach for the request-attribute pattern
instead of the dependency — the next domain-console template with a form should do
the same.

**Known gaps** (surfaced by this slice, not fixed by it):

- `detail.jte` and `identify.jte` still import `CsrfToken` directly; neither is
  rendered by any test, so the missing-dependency risk above is latent rather than
  broken.
- `safeReturn` and `safeReturnPath` are two near-identical untrusted-redirect
  validators on `InsectsController`; `safeReturnPath` has no CRLF guard.
- Feature dedup by value is unimplemented — `InsectIdentificationCommandImpl` creates a
  fresh `InsectFeature` per value per identification, so the catalog holds
  near-duplicates.
