# Plants Evidence Stack — Implementation Plan

**Date:** 2026-08-18
**Goal:** Give plants the same evidence functionality insects has, culminating in a
**`PlantTaxonView`** rank-polymorphic read model. Two associations drive it (user's words):
**an image belongs to a field observation**, and **a feature attaches to a `PlantRank`**.
`PlantTaxonView` then composes a rank record with its images and features.

**Status (2026-08-18):** S1 `FieldObservation` **shipped** (commits `4dd84cd0`, `9a279f05`) —
entity + id + repository/query stack + fixtures, contract green. S2 `PlantImage`
**code-complete, pending `mvn verify`** — `PlantImageId` + `PlantImage` (parentName
`PlantRankName`, nullable `observationId`, `FileName resourceName`) + invariant/dispatch tests
+ `ImageRepository`/mock/contract + `PlantImageTestEntitySource` + `plant-images.json`
(4 fixtures) + `ImageCollection` + `PlantQuery.images()`/`forParentName`, wired through
`PlantQueryImpl`/`PlantsTestContext`. **Next: S3 `PlantFeature` + `PlantFeatureAssignment`.**

**Reference:** `domains/insects/` — this is a faithful port, adapted for plants (4 rank
permits, no subspecies). Mirror the insects types, names, and test conventions.

## Placement & naming (decisions)

- **Top-level in `plants-api`**, mirroring insects (which keeps `FieldObservation`,
  `InsectImage`, `InsectFeature`, `InsectTaxonView` at the domain root). They all reference
  `PlantRankName` and each other; a sub-context would only add visibility friction. Ids go
  in `identifiers/plants`.
- Names mirror insects: **`FieldObservation`** (unprefixed, as insects has it),
  **`PlantImage`**, **`PlantFeature`**, **`PlantFeatureAssignment`**, **`PlantTaxonView`** +
  **`PlantOrderView`/`PlantFamilyView`/`PlantGenusView`/`PlantSpeciesView`**,
  **`PlantFeatureView`**. Two `FieldObservation` types across domains is fine — different
  packages, and bean names are FQN since the M4 `DomainServiceScan` fix.

## Boundary: `FieldObservation` vs `KnownOrganism`

`FieldObservation` here is the insects concept — an **ephemeral sighting** that owns images
("I photographed a plant at this rank"). It is **not** the `KnownOrganism` (persistent
individual) decided in
[`2026-08-18-known-organisms-design.md`](../superpowers/specs/2026-08-18-known-organisms-design.md).
Both may coexist, exactly as insects has `FieldObservation` today and could gain known
organisms later. This effort delivers insects parity; `KnownOrganism` stays a separate,
unscheduled effort. Do not conflate them.

## Deferred (out of scope here)

- `FieldObservation.identification` (vision-ID payload), the add-photo command, hierarchical
  image query, and citations — those ride with the write-side / vision-ID work.
- The write side proper (`PlantCommand`/`Transaction`). This effort is read-side +
  test-fixture data, matching how the insects read models are exercised.

## Slices (dependency order; each is 1–3 PRs per ADR-019)

**S1 — `FieldObservation`.** The anchor images attach to.
`FieldObservationId` (identifiers) → `FieldObservation` entity (`observedBy` `NaturalistName`,
`subject` `PlantRankName` with the 4-permit dispatch, `observedOn` `Instant`, nullable
`notes`/`location`) → invariant test + `PlantRankNameDispatchTest` coverage → repository +
mock + contract test → `FieldObservationTestEntitySource` + JSON → `fieldObservations()` query.

**S2 — `PlantImage`.** Links to an observation.
`PlantImageId` → `PlantImage` (`parentName` `PlantRankName`, nullable `observationId`
`FieldObservationId`, filename/caption per insects) → tests → repository + mock + contract →
source + JSON → `ImageCollection` in `PlantEntityCollections` → `images()` query
(+ `forParentName`).

**S3 — `PlantFeature` + `PlantFeatureAssignment`.** Features at a rank.
`PlantFeatureId`/`PlantFeature` (`@UniqueValue String value`) and
`PlantFeatureAssignmentId`/`PlantFeatureAssignment` (`featureId`, `rank` `PlantRankName`) →
tests → repositories + mocks + contracts → sources + JSON → `FeatureCollection` →
`features()` query.

**S4 — `PlantTaxonView` (capstone).**
`PlantOrderView`/`PlantFamilyView`/`PlantGenusView`/`PlantSpeciesView` (each = rank entity +
`ImageCollection` + `FeatureCollection`), sealed `PlantTaxonView` with polymorphic `name()`,
`PlantFeatureView`, package-private `PlantTaxonViewFactory` in `plants-core` (assembles by
rank name), and `taxonView()` on `PlantQuery`. Invariant + factory tests mirror
`InsectTaxonViewFactoryTest`.

**S5 — Console.** Evidence surfaces mirroring insects: an observation/image gallery and a
features panel on the rank pages, consuming `taxonView()`. Optionally a unified rank route.

## Acceptance

Per slice: `mvn` green (user runs it), contract tests present (3 cases per select method),
invariant tests per entity, dispatch-test coverage for every new `PlantRankName` consumer.
Capstone: `PlantTaxonView` assembled by name returns a rank record with its images and
features, verified by a factory test paralleling `InsectTaxonViewFactoryTest`.
