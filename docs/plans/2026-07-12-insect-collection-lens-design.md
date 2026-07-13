# Insect Collection Lens + Header Restyle — Design

**Date:** 2026-07-12
**Status:** Design approved; implementation plan pending.
**Builds on:** the naturalist insect collection feature
(`docs/plans/2026-07-12-naturalist-insect-collection-design.md`).

## Problem

The collection filter shipped as a `?mine=true` link buried on the species list, and the
image galleries always show the full shared catalog. The naturalist wants a persistent,
prominent **"My collection" lens** — flip it on and the Insects section shows only what
*they* have observed (their species, their photos), so building the collection is rewarding.
Separately, the header's `Logout` button is oversized and crowds the top bar.

## Scope

**In scope**
- A sticky, session-held **collection lens** (on/off) for the logged-in naturalist; retires
  the `?mine=true` URL param.
- Header restyle: a compact user area (avatar + name + small log-out icon) replacing the big
  `Logout` button, and a segmented `All / My collection` toggle shown only on Insects pages.
- Lens filtering on the **species list**, the **species-detail gallery**, and any photo
  gallery on a species page: when on, show only the naturalist's observed species and only
  their own photos.
- Friendly empty states when the lens is on and the collection (or a gallery) is empty.

**Out of scope (deferred, flagged)**
- **Order / family / genus rank-page list filtering** under the lens — the "what counts as
  collected under an intermediate rank" (exact vs. subtree) question needs its own pass; the
  `forNaturalistAndSubjects` port already exists to build it. Until then, rank pages render
  the full catalog even under the lens (the species-detail "collected" badge already ships).
- Per-naturalist server-side persistence of the lens beyond the session; a "my collection"
  count/summary; multi-user sharing.

## Key decisions

1. **The lens is a session preference**, not a URL param. It persists across Insects
   navigation until toggled off. Source of truth: an `HttpSession` attribute. The `?mine`
   param is removed.
2. **The toggle lives in the top header, near the user area**, shown only when the path is
   under `/insects` and the session is a logged-in naturalist. Admin/anonymous never see it.
3. **The shared header (`page.jte`) stays type-clean** — it reads plain request attributes
   published by `NaturalistHeaderInterceptor` (no Spring-Security / servlet / app types),
   the same rule that governs the existing name/CSRF attributes.
4. **Galleries filter by photo ownership** — an image is "mine" when its `observationId`
   belongs to one of my observations. No new query: reuse `fieldObservations().forNaturalist(me)`
   → set of observation ids → filter the rank's images.

## Design

### Lens state + toggle endpoint

- Session attribute `insectCollectionLens` (boolean; absent = off). Read per request by the
  insects controllers to decide filtering, and by the interceptor to render the toggle state.
- `POST /insects/collection-lens` (CSRF-protected) with the desired state and a `return`
  field (the path to go back to). Sets/clears the session attribute, redirects to `return`.
  No current naturalist → no-op redirect. This is the only writer of the flag.
- The insects controllers replace their `@RequestParam mine` reads with the session flag via
  a small `collectionLensOn(request)` helper (mirrors the existing `currentNaturalist(request)`
  helper). The `?mine` param and its plumbing are removed.

### Header (interceptor + `page.jte` + CSS)

- `NaturalistHeaderInterceptor` publishes two new request attributes: `insectSection`
  (`request.getRequestURI().startsWith("/insects")`) and `collectionLens` (the session flag).
- `page.jte` user area:
  - **Compact user area:** an avatar circle (first initial) + given name + a small
    `ti-logout`-style icon link that submits the existing logout form. Replaces the full-width
    `Logout` button. Styled in `naturalist.css` (`.site-user`, `.site-logout`).
  - **Toggle:** when `insectSection && naturalistAuthenticated`, render a segmented control —
    two states, the inactive one a CSRF-protected submit button posting to
    `/insects/collection-lens` with the flipped state and `return` = current URI. The active
    state is styled (accent fill); the inactive is a plain button.
- The header markup references only `spring-web` `RequestAttributes` (as today) — the return
  URI is read from a request attribute the interceptor sets (`requestUri`), not from a
  servlet type.

### Lens filtering (this slice)

- **Species list (`GET /insects/species`):** when the lens is on, filter to species the
  naturalist has observed (reuse `fieldObservations().forNaturalist(me)` → subjects → filter
  the page). Off → full catalog. (Same logic as today's `mine`, now driven by the session
  flag.)
- **Species detail + its gallery (`GET /insects/{name}`):** when the lens is on, the image
  gallery shows only the naturalist's photos — images whose `observationId` is in the
  naturalist's observation-id set. Off → all catalog images for the rank (today's behavior).
  The "✓ In your collection" badge already ships.

### Empty states

- Species list, lens on, no observed species → a nudge card: *"No insects in your collection
  yet — go make some observations."* with a link to `/insects/collection-lens` off (or
  `/insects/species`).
- Species detail gallery, lens on, none of my photos → a small inline note: *"No photos of
  yours here yet."* rather than an empty gallery.

### Logout restyle

Markup + `naturalist.css` only. The logout stays a POST form (CSRF) — just rendered as a
small icon/link instead of a padded button. Behavior unchanged (`AdminSecurityWebMvcTest`,
`NaturalistLoginWebMvcTest`, `HeaderWebMvcTest` must stay green; update the header test's
assertion from the `Logout` button text to the new control if needed).

## Testing

- **Lens state:** `POST /insects/collection-lens` with state=on as a naturalist sets the
  session flag and redirects to `return`; state=off clears it; admin/anonymous → no-op.
- **Species list under lens:** lens on → only observed species; lens off → full catalog
  (driven by session, not `?mine`). Confirm `?mine` no longer has any effect.
- **Gallery under lens:** species detail with lens on shows only the naturalist's photos;
  off shows all catalog images.
- **Header:** toggle renders only on `/insects/*` for a naturalist (not on `/chemistry`, not
  for admin); the slim logout control still logs out.
- **Empty states:** nudge renders when a naturalist with no observations views the species
  list under the lens.

## Scope / YAGNI

Insects-only; the toggle hides elsewhere and for admin. State is per-session (one naturalist
per session). Rank-page (order/family/genus) list filtering is deferred with a noted
semantic question. Reuses the observation queries already shipped; the only new read path is
a controller-side image filter by `observationId` membership.

## Open items for the implementation plan

- Where `collectionLensOn(request)` and the lens session-attribute constant live in
  `InsectsController`, and the shared attribute-key literals between the interceptor and the
  controller (documented, same convention as `"naturalist.currentNaturalistName"`).
- The exact segmented-toggle markup in `page.jte` (two CSRF forms vs. one) and the
  `naturalist.css` rules for `.site-user` / `.collection-toggle`.
- The `naturalist.css?v=` cache-bust bump when the header ships visible changes.
