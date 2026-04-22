# ADR-022: Entity Identity — `NamedEntity` (slug) and `Entity` (UUIDv7), Unified `Named<KEY>` Port

**Status:** Accepted

**Supersedes:** ADR-005, ADR-021

## Context

ADR-005 split the identity model into `CatalogEntity` and `FactEntity`, with `CatalogEntity`
carrying an `EntityName` slug and `FactEntity` carrying a `FactName` GUID. Its Amendment 2
unified the repository/test-source infrastructure by adding a `NAME` type parameter to a
shared `Entity<ID, NAME>` kernel interface — both sub-interfaces kept a `PersistenceId`
component.

ADR-021 then removed `PersistenceId` from cross-entity references and piloted a parallel
`NamedEntity<NAME>` shape on the insects domain, leaving the broader rollout to a follow-on
ADR. Appendix A.7 argued — load-bearing on the project's choice of MyBatis — that
`PersistenceId` need never appear in Java at all.

Both ADRs left the kernel mid-refactor. In practice the code moved past both:

- Every domain adopted the `NamedEntity<NAME extends EntityName>` shape. No domain record
  carries a `PersistenceId` component. `Entity<ID, NAME>` and `PersistenceId` were deleted
  from the kernel.
- `FactEntity` and `FactName` became awkward type-system weight. The Catalog/Fact
  distinction was always a *domain-modelling* concern — "does this thing have a natural
  key, or does it need a surrogate?" — not a kernel concern. The infrastructure had
  already unified on a shared `Named<KEY>` supertype; the separate interfaces added no
  structural benefit and forced domain authors to reason about Catalog-vs-Fact vocabulary
  that Evans already captures in "natural key vs. surrogate key."
- A separate question opened: what surrogate-identity mechanism should replace the
  `FactName` UUID? UUIDv4 (`UUID.randomUUID()`) has the known B-tree fragmentation cost
  that made RDBMS practitioners reluctant to adopt UUID primary keys in the first place.
  UUIDv7 — RFC 9562, time-ordered, monotonic within a millisecond — removes the
  fragmentation cost while keeping UUID's offline-generation property, which ADR-005
  identified as load-bearing for field observations collected without connectivity.

This ADR retires both predecessors, collapses the kernel identity palette to two branches,
and commits the project to UUIDv7 specifically.

## Decision

### Two branches, one shared supertype

```
Named<KEY>                 ← data-layer supertype; one repository/query port
├─ NamedEntity<NAME extends EntityName>   — natural-key slug identity
└─ Entity<ID extends EntityId>            — UUIDv7 surrogate identity
```

`Named<KEY>` is the kernel's infrastructural contract: *"this record carries a typed
identity component."* `KEY` is open — the `data`-layer generics do not distinguish slug
from UUID because they do not need to. `EntityRepository`, `AbstractEntityRepository`,
`EntityQuery`, and `AbstractEntityQuery` bind once on `Named<KEY>` and serve both
branches with one implementation.

`NamedEntity<NAME>` and `Entity<ID>` are the *domain-facing* contracts. Each signals a
different identity discipline:

- A `NamedEntity` has a stable natural-key slug that exists before the record is
  persisted and survives across deployments. Cross-domain references use this slug
  (ADR-001's dual-key strategy, retained).
- An `Entity` has no natural key. Its identity is a surrogate UUIDv7 assigned at record
  construction. Cross-domain references to an `Entity` are never by value — other
  domains reason about them through service interfaces.

### UUIDv7, not "a UUID"

The kernel commits to UUIDv7 specifically. `EntityId.isValid()` enforces
`value.version() == 7`; any other UUID version fails the invariant at the boundary.
The contract does not say "a UUID" — that would leave `UUID.randomUUID()` (v4) as a
silent possibility and erode the B-tree friendliness the whole decision rests on.

UUIDv7 was chosen over a 64-bit Snowflake-style long for one load-bearing reason:
**offline field collection**. A naturalist collecting observations on a phone without
connectivity generates an `AmendmentEventId` locally and publishes later. UUIDv7 needs
no coordination — any device produces a globally unique, time-ordered id with no
shard-id allocation ceremony. Snowflake would require per-device machine-id assignment;
the deployment story for "my partner borrowed my iPad" is grim.

The B-tree friendliness (the whole selling point of v7 over v4) gets most of the
performance argument for longs anyway — inserts arrive in approximately time order,
pages fill sequentially, and cache locality is preserved. The 2× column/index width
versus `bigserial` is real but linear, not the exponential fragmentation v4 produced.

### Identity lifecycle begins in Java

An `EntityId` is generated at record construction, not at persistence. The full
identity lifecycle therefore begins in Java; the database never assigns an identifier.
Consequences:

- Insert statements ship a fully-formed row. No `RETURNING id` round-trip. No
  post-insert rebind on the domain record. Under MyBatis the mapper is a one-statement
  insert; under JDBC the same.
- `PersistenceId` does not exist in Java. Not as a kernel type, not as a domain
  component, not as a repository parameter. Appendix A.7 of ADR-021 is now the rule,
  not the stronger reading.
- `TestEntitySource` assigns no numeric id. JSON catalog fixtures carry no id field
  anywhere.

### Typed id subclasses

Each `Entity` domain declares its own concrete `EntityId` subclass —
`AmendmentEventId`, `IrrigationEventId`, `TillageEventId`, `LabAnalysisId`. They wrap
a single `UUID` value. Equality is qualified by `getClass()`, so `AmendmentEventId`
holding UUID X never compares equal to `LabAnalysisId` holding the same UUID. The
same type-safety discipline that stops `CompoundName` from masquerading as
`ElementName` applies unchanged.

Concrete id classes live in `domains/identifiers/` alongside the `EntityName`
subclasses.

### UUIDv7 generation is a kernel concern

The generator is centralized, not rolled per-domain. The kernel provides (or delegates
to, via a small support module) a monotonic-within-ms UUIDv7 generator that implements
RFC 9562's counter recommendation to avoid clock-regression collisions. The chosen
implementation is `com.github.f4b6a3:uuid-creator` — a small, focused library that
handles the counter correctly. Domain code never calls `UUID.randomUUID()` or a
hand-rolled v7 generator; it delegates to the kernel.

### The Catalog/Fact distinction retires to domain documentation

ADR-005's classification table is no longer encoded in the type system:

- "Immutable record of a singular occurrence" is a domain invariant. It is enforced
  on the record — records, final fields, no `with*` mutators that rewrite history —
  and documented in the per-domain `CLAUDE.md` for event and observation domains.
  It is not a kernel subtype.
- Domain laws and constants remain `ValueObject`s or static constants. That guidance
  was never about `FactEntity` and stands unchanged.

An author choosing `NamedEntity` vs. `Entity` is answering one question: *does this
thing have a natural key?* Slug-present → `NamedEntity`. Slug-absent → `Entity`.

## Consequences

- `FactEntity`, `FactName`, and `CatalogEntity` are deleted from the kernel.
- `Entity<ID extends EntityId>` and `EntityId` are added to `kernels/framework/ddd`.
  `EntityId` wraps a `UUID`, validates `version() == 7`, and qualifies equality by
  concrete class.
- `NamedEntity<NAME extends EntityName>` is unchanged. `EntityName` is unchanged — the
  kebab-case + `maxLength()` contract (ADR-005 Amendment 3) remains in force for all
  slug-keyed entities.
- `Named<KEY>` remains the shared data-layer supertype. `NamedEntityRepository` and
  `NamedEntityQuery` are renamed to `EntityRepository` and `EntityQuery` — the "Named"
  prefix was a misnomer for a port that serves both branches.
- `PersistenceId` and `Entity<ID, NAME>` stay deleted. They are not reintroduced
  under any framing.
- Every concrete `FactName` subclass is renamed to the corresponding `EntityId`
  subclass (`AmendmentEventName` → `AmendmentEventId`, and so on). Domain records
  that implemented `FactEntity<XxxName>` now implement `Entity<XxxId>`.
- A kernel-level UUIDv7 generator replaces any per-domain id-creation code.
  `UUID.randomUUID()` is forbidden in domain code; a build-time check or code-review
  rule should flag it.
- RDBMS adapters use native `uuid` columns for PKs and FKs. Appendix A of ADR-021 —
  name-to-id resolution in SQL via CTE/scalar subquery — stands unchanged; the only
  substitution is UUIDv7 where the sketch used `BIGSERIAL`.
- Under load, UUIDv7 indexes are 2× wider than `bigserial` indexes. This is an
  accepted cost. The fragmentation cost that historically argued against UUID PKs
  was a UUIDv4 artifact and does not apply here.

## Applicability Signals

Flag an ADR-022 violation in review when any of the following appears:

- A domain record declares a component of type `PersistenceId` or `Long` intended as
  an id.
- A domain class implements `FactEntity` or `CatalogEntity` (either should be a
  compile error now; flag the import if it appears).
- A domain class or test calls `UUID.randomUUID()`.
- A repository or query method accepts or returns an `EntityId` belonging to a
  different entity (cross-entity references to an `Entity` are through service
  interfaces, not by value).
- A JSON catalog fixture declares an `id` field with a non-null value.
- A `NamedEntity`'s `EntityName` is used for a record that is clearly event-shaped
  (ask: does this thing have a natural key? if no, it is an `Entity`, not a
  `NamedEntity` with a synthesized slug).

## Related ADRs

- ADR-001 — Repository Architecture (dual-key strategy; retained)
- ADR-005 — Entity Identity Model (superseded)
- ADR-007 — First Principles Problem Solving (the diagnostic discipline that produced
  the refactor path from ADR-005 through ADR-021 to here)
- ADR-010 — Query Design Contract
- ADR-011 — Behavioral Collections
- ADR-017 — Observability, Monitoring, and Validation
- ADR-021 — `PersistenceId` is Adapter-Internal (superseded; its SQL-translation
  appendix remains the canonical sketch, with `uuid` columns in place of `BIGSERIAL`)

## Notes on Residual Friction

UUIDv7 removes the B-tree fragmentation cost of UUIDv4 but does not make UUID
identity free. Three concerns worth being explicit about:

1. **Index width.** UUIDv7 columns are 16 bytes vs `bigserial`'s 8. On a 100M-row
   table the PK index is ~1.6GB vs ~800MB. Linear overhead, rarely decisive.
2. **Time-ordering is device-local.** UUIDv7 uses wall-clock time. Two devices
   collecting observations offline produce clean per-device monotonic streams that
   interleave by clock skew when merged. `ORDER BY id` is *not* a domain-chronological
   order across devices. Anything that needs real chronology carries its own
   `observedAt`/`recordedAt` timestamp and sorts on that.
3. **Generator correctness.** Naïve UUIDv7 implementations collide on clock-regression
   or within a sub-ms burst. Use the RFC 9562 monotonic-counter form (`uuid-creator`
   provides it). Do not hand-roll.

These are named in this ADR rather than left implicit so future readers can evaluate
the decision against its actual cost surface.
