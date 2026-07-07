# Naturalist Auth / Login — Design

**Date:** 2026-07-06
**Status:** Design approved; implementation plan pending.
**Enables:** The later "naturalist's collection" feature (per-naturalist insect image
ownership + browse/filter). That feature is a **separate** spec and is out of scope here.

## Problem

Each naturalist should eventually have their own collection of organisms and data
(starting with insects). The collection feature needs one thing from the platform that
does not exist yet: a **session-bound "current naturalist"** — the request must know
which `NaturalistName` it is acting as.

The management console already ships Spring Security, but its only principal is a single
hardcoded **admin** from config (`naturalist.admin`, credentials `naturalist`/`durrell`),
served by an in-memory `UserDetailsService`. Naturalists exist only as test fixtures:
no credentials, no read stack (no query/repository), no way to log in.

This spec turns "one hardcoded admin" into **admin + credentialed naturalist logins**,
and establishes that the authenticated session carries a `NaturalistName`.

## Scope

**In scope**
- Naturalists can log in with a password.
- A separate credential store in the naturalists domain (hashes off the `Naturalist`
  record).
- The naturalists domain's first read stack (`NaturalistQuery`), needed to resolve the
  naturalist behind a credential.
- An explicit auth principal type carrying the `NaturalistName`, and a single
  `CurrentNaturalist` resolver that is the only seam consumers use.
- Header shows "logged in as … · Logout".

**Out of scope (explicit YAGNI)**
- Self-service registration; password change / reset UI.
- Email addresses or any new username field on `Naturalist`.
- Per-role route enforcement beyond the one admin route that already exists.
- The collection feature itself (image ownership, browse/filter). Separate spec.

## Key decisions

1. **Admin stays; naturalists log in alongside it.** The config admin keeps its `ADMIN`
   authority and continues to guard the existing admin-only route. Naturalist login is a
   second principal kind, granted a `NATURALIST` authority.
2. **Login identity = the `NaturalistName` slug as username** (e.g. `patrick-way`). No new
   email/username field on the `Naturalist` record. The existing `login.jte` form is
   reused unchanged. *This is the one deliberately-simple shortcut; see Future friction.*
3. **Credentials are a separate record**, `NaturalistCredential`, keyed by `NaturalistName`
   — never fields on the ecological `Naturalist` record.
4. **Credentials persist as domain data** (JSON `TestEntitySource` pattern), not config.
5. **Bcrypt** password hashing (Spring Security `PasswordEncoder`).
6. **One seam.** Consumers only ever obtain a `NaturalistName` through `CurrentNaturalist`;
   nothing outside the `UserDetailsService` and the resolver touches `SecurityContext` or
   usernames. (Hardening — see below.)
7. **Explicit principal type** `NaturalistPrincipal` carries the `NaturalistName`, rather
   than a bare Spring `User` whose username string is re-parsed. (Hardening — see below.)

## Design

### Credential store (naturalists domain)

`NaturalistCredential` — `NamedEntity<NaturalistName>`, 1:1 with a naturalist, keyed by the
same slug. Carries a bcrypt password hash and nothing from the ecological record.

- JSON store: `naturalists/naturalist-credentials.json`, loaded via a
  `NaturalistCredentialTestEntitySource` (same pattern as `NaturalistTestEntitySource`).
- Minimal read-by-name port: `NaturalistCredentialQuery` with
  `Optional<NaturalistCredential> getByName(NaturalistName)`. Repository super-interface
  (package-private in api), in-memory mock with argument validation
  (`observer().arguments(...).throwWhenInvalid()`), behavioral contract test, mock test —
  per project convention.
- Seeded credential for `patrick-way` (bcrypt hash of a documented default password,
  mirroring the admin's `durrell` convention). The seed hash is generated with the same
  `PasswordEncoder` the app uses.

### Naturalist read stack

The naturalists domain gains its first query: `NaturalistQuery` with
`Optional<Naturalist> getByName(NaturalistName)` (repository + mock + contract test). The
`UserDetailsService` uses it to load the naturalist behind a credential; the header/seam
uses it to display the current naturalist.

### Authentication wiring (management-console app)

- **Composite `UserDetailsService`:** check the admin username first (unchanged
  `AdminProperties` path → `ADMIN` authority); otherwise look up a `NaturalistCredential`
  by name and, if present, build a `NaturalistPrincipal` (username = slug, bcrypt hash,
  `NATURALIST` authority) carrying the `NaturalistName`.
- **`NaturalistPrincipal`** — an explicit `UserDetails` implementation holding the
  `NaturalistName`. This is what makes the eventual account/naturalist decoupling a change
  to *how the principal is built*, not to every reader.
- **Bcrypt `PasswordEncoder`** bean.
- The existing `SecurityFilterChain` is preserved: form login at `/login`, CSRF on,
  `anyRequest` authenticated, admin route requires `ADMIN`. A logged-in naturalist reaches
  the normal console but gets 403 on the admin route.

### `CurrentNaturalist` seam

- A single resolver bean maps the authenticated principal → `Optional<Naturalist>`
  (empty when logged in as the bare admin, whose principal is not a `NaturalistPrincipal`).
  It is the **only** place, besides the `UserDetailsService`, that reads `SecurityContext`.
- Surfaced to the shared header via an app-level `HandlerInterceptor` that reads identity
  through `CurrentNaturalistView` and publishes plain request attributes (display name,
  authenticated flag, CSRF param/token); `layout/page.jte` reads those attributes using only
  `spring-web` and gains a "logged in as _givenName_ · Logout" element. (An interceptor, not a
  `@ControllerAdvice` model attribute, because `page.jte` is compiled by every domain-console
  module against a classpath without Spring-Security or app packages — so the layout must not
  reference those types directly.)
- For this slice the resolver is app-local. Its cross-module shape (so domain consoles can
  consume it) is deferred to the collection spec, which is its first real consumer.

### Seam discipline (rule)

Nothing outside the `UserDetailsService` and the `CurrentNaturalist` resolver touches
`SecurityContext`, the raw `Authentication`, or usernames. Consumers receive a
`NaturalistName` (or a `Naturalist`) from the seam and nothing else. This rule is what
bounds the future refactor to a handful of spots.

## Testing

- Contract + mock-validation tests for `NaturalistCredentialQuery` and `NaturalistQuery`
  (domain-specific methods validate args and reject null, per convention).
- Extend the existing `@SpringBootTest` security test:
  - admin still reaches the admin route;
  - a seeded naturalist logs in, reaches a normal route, gets 403 on the admin route;
  - bad credentials redirect to `/login?error`;
  - the `CurrentNaturalist` resolver returns the naturalist for a naturalist session and
    empty for the admin session.

## Future friction — refactor toward a full identity module

A full identity module will own accounts (an auth principal distinct from the ecological
actor), credentials, registration, reset, roles/permissions, sessions/tokens. Measured
against that target:

- **Low / reused:** separate credential record, the read-stack scaffolding, composite
  `UserDetailsService`, flat authorities, seeded JSON — all either move mechanically into
  the new module or are additive.
- **The one real coupling:** login identity *being* the `NaturalistName` slug. A full
  module gives accounts their own identity (email/username) decoupled from the domain
  natural key, so this assumption must eventually break (email login; an account not yet
  linked to a naturalist; a naturalist with no login).
- **Why it stays cheap:** its blast radius is bounded to three spots — the credential
  store, the `UserDetailsService`, and the login form — *because* the explicit principal
  type + the seam discipline keep every other consumer insulated from usernames and
  `SecurityContext`. Reversal is localized, not codebase-wide.

## Open items for the implementation plan

- Exact module/package placement of `NaturalistCredential`, its query, and the
  `TestEntitySource` (mirror `naturalists-api` / `naturalists-repository-test`).
- Whether `naturalists-core` (currently empty) hosts the query impls (expected: yes,
  matching the insects-core adapter pattern).
- Where `NaturalistPrincipal`, the composite `UserDetailsService`, and the
  `CurrentNaturalist` resolver live within the management-console app package.
- Default seed password value + how the seed hash is produced and documented.
