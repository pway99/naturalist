# Numeric identity kind for high-volume log tables — problem & context brief

**Date:** 2026-08-27
**Status:** Problem brief for a FUTURE session — not yet designed, not scheduled.
**Why written now:** captured from the session that built the `usage` event-log (the
first concrete "ever-growing append-only log table" in the codebase), so a fresh
session inherits the context without re-deriving it. This is a handoff, not a design.

## Problem

Every `Entity` surrogate key in the codebase is a UUIDv7 (16 bytes) via `EntityId`.
For **ever-growing append-only log tables**, 16 bytes vs an 8-byte `Long` is a
material, recurring cost — multiplied across every row, its primary-key index, any
FK/reference to it, JSON/network serialization (a 36-char UUID string vs a short
number), and RDBMS storage + index density.

**Motivating case: sensor readings.** An array of sensors each emitting a reading
per minute is an unbounded, monotonically growing table; at that scale the per-row
id-width difference dominates storage and network cost. `UsageEvent` (the append-only
identification log in `domains/usage`) has the same *shape* but is low-volume
(budget-capped by design), so it is the ideal **prototype adopter** — not itself the
payoff.

**Goal:** a reusable numeric surrogate-identity **kind** for entities that are
(a) high-volume append-only, (b) never referenced cross-domain by value, and
(c) do not need a UUID's global uniqueness or client-unguessability.

## Essential context / constraints (read before designing)

- **Identity model (ADR-022; root `CLAUDE.md` + `domains/CLAUDE.md`).** Two branches
  today: `NamedEntity<NAME extends EntityName>` (kebab slug) and
  `Entity<ID extends EntityId>` (UUIDv7). Both sit on one generic data-layer port
  `Named<KEY>` (`kernels/framework/.../ddd/Named.java`, `KEY key()`). **That generic
  seam is what makes a third, numeric branch feasible without disturbing the other two.**
- **Construction-time identity is load-bearing.** From `Entity`'s javadoc: the UUIDv7
  is "generated at record construction, so the entire identity lifecycle begins in
  Java and no round-trip to the database is required at insert time." ADR-022
  explicitly **removed `PersistenceId`** ("it does not exist in Java"), superseding
  ADR-021. An entity is a fully-formed value the instant it is `new`'d.
- **THE fork.** A DB-**sequenced** `Long` (the literal first framing — "sequenced by
  the rdbms") is assigned **on insert**, so the id is unknown until persist. That
  **reopens the assignment-timing problem ADR-022 deliberately closed**
  (nullable-id-before-persist, two-phase construct-then-fetch-id). Weigh this as a
  partial reversal of a hard-won decision, not a free change.
- **Likely sweet spot: client-generated 64-bit time-ordered id (Snowflake/flake).**
  Fits in a `Long` (8 bytes), generated in-app at construction (preserves the whole
  architecture, no DB round-trip), and time-sortable so B-tree indexes stay dense
  exactly like UUIDv7 — which was chosen for that density (`EntityId.newUUID()` →
  `UuidCreator.getTimeOrderedEpoch()`). Gets the byte savings **without** the ADR-022
  reversal. A store-assigned sequence yields smaller/"nicer" numbers (1, 2, 3…) but
  pays the assignment-timing cost.
- **No real RDBMS exists yet.** Persistence is the house in-memory placeholder
  (`<domain>-repository-rdms` subclasses the in-memory mock over a shared store). So
  "sequenced by the rdbms" cannot be literally built today: a store-assigned variant
  would sequence via an in-memory `AtomicLong` now and a real DB sequence when the
  project-wide real-store milestone lands. A client-gen variant behaves identically
  now and later.
- **Cross-domain rule still applies.** A numeric surrogate must **never cross a domain
  boundary by value** (a bare `Long` is even more collision-prone across domains than
  a typed UUID). Keep it a distinct per-entity concrete type with equality qualified
  by `getClass()`, mirroring `EntityId`.
- **Enforcement to update.** The `UUID.randomUUID()` ban, the
  `EnforceArchitecture`/`EnforceQueryHygiene` OpenRewrite recipes, and any ArchUnit
  rules that assume `Entity ⇒ EntityId` must be taught the new branch.

## Possible solution (sketch — for the next session to design, not final)

Add a third kernel identity branch, e.g. `SequencedEntity<ID extends SequenceId>`
(names TBD) on the existing `Named<KEY>` port. `SequenceId` wraps a `long` with a
`@JsonValue long value()` and per-entity concrete subtypes (equality by `getClass()`),
paralleling `EntityId`. Provide a kernel generator entry point (the analog of
`EntityId.newUUID()`). **Migrate `UsageEventId` → the numeric kind as the prototype**;
target sensor readings as the first genuine high-volume adopter when that domain is
built.

## Open decisions for the next session

1. **Generation model** — client-generated 64-bit time-ordered (preserves ADR-022,
   recommended) vs store-assigned sequence (reopens assignment-timing). The pivotal call.
2. **Naming** of the branch, the id base type, and the marker interface.
3. **Whether to amend ADR-022** (or write a new ADR) documenting the third branch and
   its rationale.
4. **Scope of the first cut** — just the kernel kind + `UsageEvent` migration, or wait
   for the sensor domain to supply a real high-volume consumer.
5. **If client-gen:** flake bit-layout, and whether a node/worker component is needed
   (likely not for a single-writer app) — plus monotonicity guarantees under
   concurrent construction.

## Pointers

- `kernels/framework/src/main/java/com/naturalist/ddd/`: `Named.java`, `Entity.java`,
  `EntityId.java`, `NamedEntity.java`, `EntityName.java`.
- ADR-022 (identity model); `domains/CLAUDE.md` (identity + record conventions).
- Prototype adopter: `domains/usage/` — `UsageEvent` / `UsageEventId` (append-only,
  never cross-domain), merged to local `main` at commit `9ec2ea1f`.
- Real-store / durability milestone this rides: see the "Accepted durability trade"
  section of `docs/plans/2026-08-26-usage-event-log-redesign-design.md`.
