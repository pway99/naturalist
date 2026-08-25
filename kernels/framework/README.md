# framework

The foundation every other module builds on. It carries no domain knowledge —
only the structural vocabulary that makes a domain type a domain type. Change
anything here and it ripples across the whole codebase, so these are treated as
stable contracts, not a convenient place to add things.

---

## What it provides

**The five domain supertypes.** Every domain class implements exactly one of
these, and the choice fixes its identity, equality, and lifecycle:

| Supertype | Identity | Role |
|-----------|----------|------|
| `NamedEntity<Name>` | natural-key slug (`EntityName`) | a catalog citizen referenced across domains by name |
| `Entity<Id>` | surrogate UUIDv7 (`EntityId`) | an observation or fact record, never referenced cross-domain by value |
| `Aggregate` | via its root | a consistency boundary owning child entities and value objects |
| `ReadModel` | optional | a read-side projection assembled from already-persisted parts |
| `ValueObject` | none (equality by value) | an immutable, cohesive concept with no identity |

`BehavioralCollection` is a sixth, deliberate exception — a `final class`
(not a record) that lets domain-specific query results filter down to the same
concrete type. All six extend `Observable`.

**Typed identity, unified behind one port.** `NamedEntity` and `Entity` share a
`Named<KEY>` supertype whose `key()` delegates to `name()` or `id()`
respectively. `EntityName` subclasses are kebab-case slugs — the stable
cross-domain reference. `EntityId` validates that its UUID is version 7 (time
ordered) and qualifies equality by concrete class, so ids of different types can
never compare equal. `UUID.randomUUID()` is forbidden in domain and adapter
code — only the kernel's generator produces ids.

**Observability as structure.** `Observable` requires every type to declare
`invariants()` — a fluent predicate graph the framework walks at method
boundaries, on repository inserts, and on query results. A single walk yields an
`InvariantObservation` carrying *every* failing constraint at once (no iterative
discovery), and emits Micrometer meters with stable names — without domain code
importing Micrometer.

**The `Resilience` facade.** `Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead`,
the `@Resilient` / `@ResilienceExempt` annotations, and a sealed
`ResilienceConfig`. Domain code references the facade only; the Resilience4j
implementation lives in [`adapters/resilience-resilience4j`](../../adapters/resilience-resilience4j/).

**The `@DomainService` marker.** A dependency-free `TYPE` annotation.
[`adapters/spring-runtime`](../../adapters/spring-runtime/) discovers annotated
classes and registers them as Spring beans, so no domain code imports Spring.

---

## Why it looks the way it does

The recurring theme is that structure is enforced by the compiler and the build,
not by reviewer vigilance. Repository contracts are hidden behind `class`
visibility; the consumer surface is exposed through `interface` namespaces; a
raw `String` or `UUID` at a boundary is a review blocker; and the invariant
graph turns "did we validate this?" from a habit into a mechanical guarantee.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — the shared-kernel conventions and the
  observability framework in detail.
- [ADR-022 — Unified entity identity](../../docs/adr/rationale/ADR-022-entity-identity-unified.md)
- [ADR-017 — Observability, monitoring, and validation](../../docs/adr/ADR-017-observability-monitoring-and-validation.md)
- [ADR-026 — Resilience as a first-order concern](../../docs/adr/ADR-026-resilience-first-order-concern.md)
- [ADR-025 — DI via marker annotations](../../docs/adr/ADR-025-di-via-marker-annotations.md)
