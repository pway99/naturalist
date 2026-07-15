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
by `name()`. Assembled by name through `InsectTaxonViewFactory` and read via
`insectQuery.taxonView().getByName(rankName)`; `InsectSubspeciesName` is permitted
on `InsectRankName` but yields `Optional.empty()` (no subspecies entity exists yet).

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
