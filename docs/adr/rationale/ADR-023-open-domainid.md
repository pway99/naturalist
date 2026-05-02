# ADR-023: Open `DomainId` — Slug Uniqueness via Assembly Validation

**Status:** Accepted

**Date:** 2026-05-02

## Context

The catalog kernel (formerly `kernels/atlas`, renamed in the runtime
architecture refactor) ships `DomainId` as the stable identifier participating
domains carry into every `EntityRef`, into the `source_domain` tag on
observability metrics, and into the fan-out grouping on the inverse SPI.

The original shape was a sealed kernel interface with nested `Plants`,
`Chemistry`, and `Insects` records and an `of(String)` factory enumerating
every slug. The closed set was chosen to bound the metric tag's cardinality —
a sealed type guarantees no consumer can mint a fresh slug at runtime.

That shape made the kernel know the name of every domain. Eight domains beyond
the original three were already scaffolded (apiary, arachnids, climate, fungi,
microbes, molluscs, naturalists, plus active work elsewhere). Adding any of
them required a kernel edit, in violation of the project's own *"no domain
knowledge in the kernel"* rule. The bounded-cardinality property is worth
preserving; the mechanism that preserves it is not.

## Decision

`DomainId` is an open interface. Each domain's `*-api` module ships its own
subtype.

### Kernel shape

```java
public interface DomainId extends ValueObject {
    String value();

    @Override
    default Consumer<? extends Constraints> invariants() {
        return i -> {};
    }
}
```

No `permits` clause, no nested records, no `of(String)` factory. The kernel
knows the name of no domain.

### Per-domain subtypes

Each domain api module contributes a record:

```java
// domains/plants/plants-api
public record PlantsDomain() implements DomainId {
    @Override public String value() { return "plants"; }
}
```

Domain subtypes carry the `@DomainService` marker (ADR-025) so they are
auto-discovered by the Spring runtime adapter alongside the domain's
`CatalogContribution` and `EntityReferences` providers. A composition root
that wants only a subset wires explicitly.

A domain creates its `DomainId` subtype when it ships its first catalog
contribution. Modules that scaffold without participating in catalog fan-out
do not need a subtype yet.

### Slug uniqueness via assembly validation

`CatalogAssembly.from(...)` walks every registered contribution and
provider, collects each `DomainId.value()`, and throws
`IllegalArgumentException` at startup when two distinct `DomainId` instances
share the same slug. Same-instance reuse across multiple contributions and
providers is the normal case and is allowed.

Two domains accidentally registering the slug `"plants"` would have produced
ambiguous metric attribution, ambiguous fan-out routing, and an unbounded
metric tag set. The assembly check makes that a deployment defect that
surfaces before the first request.

## Consequences

- The kernel's domain vocabulary is empty. Adding a domain is a change in
  that domain's `*-api` module plus the composition root that imports it,
  not a kernel edit.
- The bounded-cardinality property of the `source_domain` metric tag is
  preserved by composition-root discipline: a slug appears in the
  `source_domain` set only if a `DomainId` subtype contributing that slug
  is on the classpath of the assembled app. The cardinality is bounded by
  what the app composes, not by what the type system permits.
- Slug collisions fail fast at startup with a named offender, not at
  query time with surprising attribution.
- The exhaustive-switch pattern that the sealed shape encouraged is gone.
  No production code relied on it; tests that asserted `switch` exhaustion
  on `DomainId` were removed when the shape opened.
- A `DomainId` instance is cheap to construct (a record with no
  components). Multiple call sites in the same domain instantiating
  `new PlantsDomain()` is fine; the assembly's slug-uniqueness check
  compares values, not identity.

## Applicability Signals

Flag an ADR-023 violation in review when any of the following appears:

- A `DomainId` subtype is added to a kernel module instead of a domain
  `*-api` module.
- A nested `DomainId` record reappears inside the kernel's `DomainId`
  interface (the shape must remain open and component-free).
- A slug is hard-coded as a String literal in catalog code instead of
  flowing from a `DomainId` instance's `value()`.
- Two domain api modules ship subtypes returning the same slug.
- A test asserts an exhaustive switch on `DomainId` (it cannot be
  exhaustive).

## Related ADRs

- ADR-018 — Third-Party Dependency Policy (the kernel keeps its narrow
  third-party surface; adding domain knowledge would have been the same
  category of leak).
- ADR-025 — DI via Marker Annotations (the `@DomainService` marker that
  carries each per-domain subtype into the assembled context).
- ADR-024 — Apps and Adapters Trees (the composition root that runs the
  slug-uniqueness check is the app, not the kernel).

## Reference Implementation

Post-M3 of the runtime architecture refactor:

- `kernels/catalog/.../DomainId.java` — open interface, no nested types,
  no factory.
- `domains/plants/plants-api/.../PlantsDomain.java`,
  `domains/chemistry/chemistry-api/.../ChemistryDomain.java`,
  `domains/insects/insects-api/.../InsectsDomain.java` — concrete
  per-domain records, each carrying `@DomainService`.
- `kernels/catalog/.../CatalogAssembly.java` — `from(...)` overloads
  walk contributions and providers, accumulate `DomainId` instances by
  `value()`, and throw `IllegalArgumentException` on collision.
- `kernels/catalog-inmem/.../InMemoryCatalogTest.java` — three
  slug-uniqueness tests covering the duplicate-via-contribution,
  duplicate-via-provider, and same-instance-reuse-allowed cases.
