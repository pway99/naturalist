# Insects Domain

Reference implementation for [ADR-021](../../docs/adr/ADR-021-persistenceid-is-adapter-internal.md)
— persistence-id is adapter-internal; cross-entity references use `EntityName`.

## Domain Vocabulary

**InsectSpecies** — `NamedEntity<InsectSpeciesName>`. Catalog identity of a species as
recognised by the naturalist. Four-level Durrell `Description`. No `id()` at the domain
layer; the RDBMS adapter carries a numeric primary key privately.

**InsectImage** — `Entity<InsectImageId>`. A photograph of an observed individual.
Carries `InsectImageId id`, accessor `id()`. Parent reference is an
`InsectRankName parentName` (slug) — no `insectSpeciesId`, no `withId`, no nullable
id-shaped FK column.

**InsectTaxonView** — Sealed `ReadModel` over the four Linnaean ranks that carry
catalog entities: `InsectSpeciesView`, `InsectGenusView`, `InsectFamilyView`,
`InsectOrderView`. Each permit composes its rank entity with the `ImageCollection`
of photographs attached at that rank — a read-side projection, not a consistency
boundary. Identity is the root's typed `InsectRankName`, returned polymorphically
by `name()`. No standalone query — used only as the rank-chain and child-card
building blocks assembled inside the `Insect` read model, by `InsectFactory`.
Permits carry no `features()` slot; features live on `Insect` as
`InsectFeatureView`. `InsectSubspeciesName` is permitted on `InsectRankName` but
yields `Optional.empty()` (no subspecies entity exists yet).

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

**InsectIdentificationCommand** — in `insects-core`. Orchestrates vision
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

**`features.jte`** — renders `insect.features()`, an `InsectFeatureView`
(lineage-composite, ancestor-first, ordinal-ordered within a rank) composed directly
into the `Insect` read model. `InsectFactory` populates it via
`.withFeatures(featureQuery.findByRankName(name))`, symmetric with `.withCitations(...)`;
each rank handler reads it straight off the `Insect` it already resolved — no separate
per-page fetch. `InsectFeatureView.groups()` are `RankGroup`s (rank + ordinal-ordered
`InsectFeature`s); the template formats each group's rank label with `RankLabel.of(...)`.

**`observationGallery.jte`** — photo, `%` confidence, "Why this ID?" evidence
disclosure, "Also considered" alternatives, and the field-notes form. Sourced from
`FieldObservation.identification()`. The rank pages pass
`insectQuery.images().forParentName(rankName)` — **not** `forRankHierarchy` — because
the child-rank cards on the same page already show descendant photos via the `gallery`
attribute; using the hierarchy query would render every descendant image twice.

**`citations.jte`** — renders `Insect.citations()`, which resolves through
`CitationAssociation` records in the library domain. An identification that writes a
`Citation` without a matching association produces a silently citation-less page; see
`CitationAssociationJson` in `library-repository-test` for why that catalog needs a
hand-written DTO and how the flush seam keeps it round-trippable.

The notes form posts to `/insects/{name}/notes` from every rank and carries a
`returnPath` hidden field; `InsectsController.safeReturnPath` constrains it to
`/insects/` prefixes so the field cannot become an open redirect.
`InsectsController.updateNotes` also checks ownership before writing: a
package-private static `owns(FieldObservation, Optional<NaturalistName>)` compares
`obs.observedBy()` against the signed-in naturalist and refuses on mismatch,
redirecting to the same `destination` as the not-found branch so the two outcomes are
indistinguishable to the caller. Before this check, any naturalist could overwrite
another naturalist's field notes by POSTing their observation id —
`FieldObservationId` is a UUIDv7 and therefore time-ordered and partially guessable,
so the id alone was never proof of ownership.

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
- The `detail` handler calls `insectQuery.fieldObservations().forNaturalistAndSubjects`
  twice with identical arguments.
- `safeReturn` and `safeReturnPath` are two near-identical untrusted-redirect
  validators on `InsectsController`; `safeReturnPath` has no CRLF guard.
- Feature dedup by value is unimplemented — `InsectIdentificationCommand` creates a
  fresh `InsectFeature` per value per identification, so the catalog holds
  near-duplicates.
