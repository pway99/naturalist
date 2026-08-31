# Naturalists

The observers themselves — the people who work Oak Vista and to whom every field
observation, photograph, and management decision is attributed. This is where
identity, authentication, and attribution live.

---

## Core concepts

**A naturalist is an ecological actor, not an account.** `Naturalist` is a
`NamedEntity<NaturalistName>` — a person described by a `NaturalistRole`
(visitor, caretaker, student, keeper, teacher) and an `EcologicalStage` drawn
from the Durrell learning progression (wonder → curious → practitioner →
naturalist). The domain models people in relation to the ecosystem, not to a
software system: there is deliberately no "user" or "profile" entity.

**Authentication lives in a separate domain.** `Account` (in `accounts`) is
keyed by the opaque `AccountName` and carries the login `email` and
bcrypt-encoded `passwordHash`; `Naturalist.account` links to it. The
ecological record never carries a password, so the account can be rotated,
suspended, or have its access level changed without touching the person's
ecological data.

**Attribution travels by slug.** Other domains reference a naturalist only by
`NaturalistName` — the insects domain's `FieldObservation.observedBy` is the
primary consumer. Naturalists has no outbound api dependency on any other
domain; the coupling is identity alone.

**The ecological stage selects how much to explain.** A stage maps to one of the
four Durrell `Description` levels, so the application can surface catalog
knowledge at the depth the reader is working at. Stage is a learning position,
not a rank — the wonder of `WONDER` is not superseded by `NATURALIST`.

**Personal protective equipment is a small safety sub-context.**
`ProtectiveEquipment` (in the `safety` sub-package) catalogs the gear a
naturalist wears when handling hazardous compounds. It belongs here because PPE
is about the person wearing it; chemistry compounds carry only a boolean flag,
not a reference into this catalog.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance). The api depends
only on `framework` and `identifiers` — no taxonomy, catalog, or authority.
Domain-specific notes:

- **`naturalists-api`** — the `Naturalist` record (with its `AccountName`
  link and renameable `publicHandle`), its `NaturalistRole` /
  `EcologicalStage` enums, and the read port.
- **`naturalists-core`** — thin observe-dispatch-delegate query adapters.
- The read surface is query-only. There is no write command beyond the console
  login flow; sign-in shipped in July 2026, moved to the `accounts` domain and
  email login in August 2026.

**Session identity stays out of the domain.** Login username is the account
`email`, and the signed-in identity reaches the console through the
`CurrentNaturalist` seam in `management-console` via a request-attribute
convention. No domain code depends on Spring Security.

---

## Planned direction (not yet built)

A broader authentication/authorization redesign is designed — see
[`docs/plans/2026-08-30-accounts-auth-redesign-design.md`](../../docs/plans/2026-08-30-accounts-auth-redesign-design.md).
The core of it (the `accounts` domain, email login, `AccountName` link,
renameable `publicHandle`) is now built. One part remains:

- **Young naturalists — POSTPONED.** The design carves out teacher-provisioned
  student accounts: an admin grants an account a *teacher* capability; teachers
  create a `Classroom` with a join code; middle-school students **self-join with
  no email** (the teacher's code and sponsorship are the trust anchor) and get
  vision identification funded by a teacher-owned classroom budget pool. This maps
  onto the existing `NaturalistRole.STUDENT`/`TEACHER` and `EcologicalStage`
  vocabulary. **It is intentionally deferred** — captured here and in the design
  doc's "Young naturalists" section, but not on the near-term build path, and
  gated on a COPPA/child-privacy review before any minor is onboarded.

---

## Learn more

- [`CLAUDE.md`](CLAUDE.md) — the identity/account split and its invariants.
- [`docs/briefings/naturalists-domain.md`](../../docs/briefings/naturalists-domain.md) —
  a full type-by-type tour, the test personas, and the role/stage vocabulary.
- [`docs/plans/2026-07-06-naturalist-auth-design.md`](../../docs/plans/2026-07-06-naturalist-auth-design.md) —
  the login and session-identity design.
