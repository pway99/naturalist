# Naturalist Insect Collection — Design

**Date:** 2026-07-12
**Status:** Design approved; implementation plan pending.
**Builds on:** the naturalist auth slice (`docs/plans/2026-07-06-naturalist-auth-design.md`) —
this is the first real consumer of the `CurrentNaturalist` seam.

## Problem

Each naturalist should have their own collection of insects. The naturally-occurring act
is **observing** an insect; capturing a photo is one optional kind of evidence, not the
price of admission. So the collection's primitive is the **field observation** — a
naturalist recording "I encountered this insect" — with an image as optional evidence.

A naturalist can then browse the entire shared catalog, or filter to the insects they have
collected (the distinct species across their own observations).

## Scope

**In scope**
- A `FieldObservation` entity (the collection unit) in the insects domain, with its read
  and write stack.
- `InsectImage` gains a nullable link to the observation it evidences.
- The current naturalist reaches the insects UI via a request attribute published by the
  app interceptor.
- Two entry paths: record an observation (no photo required); capture a photo (creates and
  links an observation).
- A browse toggle: entire catalog vs. "my collection" (species I've observed).

**Out of scope (explicit YAGNI)**
- Generalizing observations across other organisms (plants, chemistry) — a later
  parking-lot item; `FieldObservation` stays insect-specific for now.
- Editing / deleting observations; a multi-photo-per-observation management UI (v1 links at
  most one photo per capture).
- The admin/naturalist name-collision startup guard (tracked separately from the auth slice).
- Making the domain-console controllers Spring-managed beans (the standing
  `InsectsTestContext.create(...)` TODO) — this slice works within that pattern.

## Key decisions

1. **The field observation is the unit.** `FieldObservation(observedBy, subject, observedOn,
   notes?)` is the collection primitive. An image is optional evidence attached to it.
   "Insects I've collected" = distinct `subject`s across my observations.
2. **`FieldObservation` lives in the insects domain** (`insects-api`/`-core`/
   `-repository-test`), referencing `observedBy: NaturalistName` and `subject: InsectRankName`.
   `NaturalistName` is already on the `insects-api` classpath via `identifiers`.
3. **Images attach via a nullable `observationId`** on `InsectImage`. Owner-less images
   (`observationId == null`) remain shared catalog/reference images — today's behavior.
4. **The current naturalist reaches the insects UI via a request attribute** published by
   the app's existing header interceptor — no app/security dependency leaks into
   `insects-console`, consistent with the header pattern the auth slice shipped.
5. **"My collection" is a toggle** on the existing insects browse (`?mine=true`), not a
   separate page. Default (no param) is the full catalog, unchanged.
6. **Multiple observations of the same species are allowed** (distinct sightings, each with
   its own date/notes). "Collected" dedups by `subject`.

## Design

### FieldObservation entity (insects domain)

`FieldObservationId` — an `EntityId` (UUIDv7) subclass, in `domains/identifiers`.

```
public record FieldObservation(
    FieldObservationId id,
    NaturalistName observedBy,
    InsectRankName subject,
    Instant observedOn,
    @Nullable String notes
) implements Entity<FieldObservationId>
```

Immutable (record; equality by value), like `InsectImage`. Invariants: `entityId(id)`,
`identifier(observedBy)`, `identifier(subject)`, `notNull(observedOn)`.

### Image → observation link

`InsectImage` gains a trailing nullable component `@Nullable FieldObservationId
observationId`. A captured photo attaches to the observation it evidences (so it is
implicitly the observer's); a `null` link is a shared catalog image. Ripple (from
exploration): 5 `new InsectImage(...)` sites (1 controller, 4 test fixtures) add the
argument, and 11 `insect-images.json` entries get an optional field (absent/`null`). The
`identify-insect` skill seeds images through the test source, not the web POST, so those
remain catalog images unaffected.

### Read / write stack (mirrors existing insect patterns)

- `InsectQuery.fieldObservations()` → `FieldObservationQuery extends EntityQuery<
  FieldObservationId, FieldObservation, FieldObservationCollection>` with:
  - `forNaturalist(NaturalistName)` — all of a naturalist's observations (species-list "mine" filter);
  - `forNaturalistAndSubjects(NaturalistName, Set<InsectRankName>)` — a bounded read port that
    restricts to the given ranks, so the rank pages (order/family/genus/species detail) can render
    a per-entity "collected" indicator by passing the ranks they display.
- `InsectCommand.observations()` → `ObservationCommand extends EntityCommand<
  FieldObservationId, FieldObservation>` (insert).
- `InsectRepository.ObservationRepository extends EntityRepository<FieldObservationId,
  FieldObservation>` with `List<FieldObservation> getByNaturalist(NaturalistName)`; in-memory
  mock (`@DomainService`, argument-validating), behavioral contract test, mock test.
- `FieldObservationTestEntitySource` + `insects/field-observations.json` seeded with a few
  observations for `patrick-way` and `delia-durrell`; test ids in `identifiers-test`.
- New `ObservationCollection` in `InsectEntityCollections`.

### Current naturalist → insects UI (request attribute)

The app's `NaturalistHeaderInterceptor` is extended to also publish the current
`NaturalistName` **slug** as a request attribute under a documented key (shared by
convention between the app interceptor and `insects-console`; no shared code dependency).
Insects handlers read it — `HttpServletRequest.getAttribute(key)` → `NaturalistName.of(slug)`
— exactly as handlers already read the CSRF attribute. When the session is the bare admin or
anonymous, no naturalist slug is present: observe controls are hidden and "my collection" is
empty.

### Flows (extend `InsectsController`)

- **Observe (no photo):** `POST /insects/{name}/observe` (optional `notes`) → constructs a
  `FieldObservation(currentNaturalist, InsectSpeciesName.of(name), Instant.now(), notes)` and
  calls `insectCommand.observations().insert(...)`. No current naturalist → rejected /
  control not shown.
- **Capture photo:** the existing `POST /insects/{name}/images`, when a current naturalist is
  present, first inserts a `FieldObservation` for `(naturalist, species)` and then inserts the
  `InsectImage` with `observationId` set to it. With no current naturalist it keeps the legacy
  owner-less path (`observationId == null`).
- **Browse toggle:** `GET /insects/species?mine=true` filters the species page to species the
  current naturalist has observed — resolve `observations().forNaturalist(me)` to the set of
  `subject`s, then restrict. No param → full catalog (unchanged). The list/detail templates
  gain an "All / My collection" toggle and a "collected" indicator on species the current
  naturalist has observed.

## Testing

- Contract + mock-validation tests for `ObservationRepository` (domain method
  `getByNaturalist` validates + rejects null), `ObservationQuery`, and `ObservationCommand`,
  per project convention. Fixture ≥4 observations for the paging contract.
- `InsectImage`: update the image contract/command fixtures for the new nullable component;
  round-trip test with `observationId` set and with `null`.
- App: the interceptor publishes the `NaturalistName` request attribute for a naturalist
  session and omits it for the admin session.
- Console: `observe` creates an observation for the current naturalist; `?mine=true` filters
  the species list to observed species; a photo capture by a naturalist links the image to a
  newly-created observation.

## Interaction with the auth seam

This is the first cross-module consumer of session identity. Rather than force the
`CurrentNaturalist` interface into a shared module and make the insects controller a Spring
bean now (a larger refactor), we deliver identity as a request attribute — the same
mechanism the header already uses. If/when a second domain-console needs it, extracting a
shared seam module is the natural next step (parking-lot).

## Open items for the implementation plan

- Exact package placement of `FieldObservation`, `ObservationQuery`/`Command`, the
  `TestEntitySource` (mirror `InsectImage` / `ImageQuery` / `ImageCommand`).
- The documented request-attribute key constant and where each side declares/reads it.
- The `observe` form placement in the species detail template and the toggle placement in
  the list/rank templates.
- Whether the photo-capture-creates-observation step reuses an existing same-day observation
  or always creates a new one (default: always create; dedup is by subject at read time).
