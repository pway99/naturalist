# The Amateur Naturalist

A modular-monolith domain application for cataloging and observing the natural
systems at Oak Vista — insects, plants, soil chemistry, climate, sensors —
built deliberately as a long-form study in Domain-Driven Design, hexagonal
architecture, observability, and resilience as first-order concerns.

> **Portfolio note.** This codebase is intentionally a demonstration of
> structural choices: how a real domain is bounded, how identity is typed,
> how cross-boundary calls are governed, and how vendor concerns are kept
> out of the core. The decisions are recorded as ADRs; the code matches.

---

## What it is

Oak Vista is a small managed landscape with diverse organisms, soil chemistry,
microclimates, and ongoing field observations. The application is the
naturalist's instrument:

- A **domain catalog** for insects, plants, chemistry compounds, fungi,
  microbes, molluscs, vertebrates, worms, sensors, soil, climate, and
  geographic zones.
- A **field-notes layer** (the *Durrell principle* — explain anything at four
  levels of understanding, from preschool to university) attached to every
  named entity.
- A **management console** (Spring Boot + JTE) that composes every active
  domain into a single navigable surface.

The system runs as one deployable today. The shape is built to split — each
domain is module-isolated and references peers only by typed names.

---

## Architecture at a glance

```
apps/                     composition roots (deployable artifacts)
  management-console/       Spring Boot fat jar — composes every active domain

adapters/                 ports-and-adapters with heavy/vendor deps
  resilience-resilience4j/  Resilience facade  → Resilience4j
  spring-runtime/           @DomainService marker → Spring bean discovery
  spring-test-data/         framework-test → Spring beans (pre-RDBMS)

kernels/                  cross-cutting foundations
  framework/                NamedEntity, Entity, ValueObject, Aggregate,
                            BehavioralCollection, Observable, Resilience facade
  framework-test/           TestEntitySource, NaturalistDatabase, contract harness
  field-notes/              Description (four-level Durrell description)
  taxonomy/                 Linnaean classification (organism domains)
  catalog/ + catalog-inmem/ cross-domain reference resolution

domains/                  bounded contexts — one module set per domain
  <domain>/<domain>-api          public surface (entities, queries, ports)
  <domain>/<domain>-core         adapters: query impls, aggregate factories
  <domain>/<domain>-repository-test    in-memory adapter + behavioral contract
  <domain>/<domain>-console      JTE templates contributed to the console
  identifiers/                   typed EntityName / EntityId subclasses only
```

The DAG is strict and acyclic, enforced by Java module visibility (and verified
by ArchUnit at build time):

```
apps/*           → adapters/*, domains/*, kernels/*
adapters/*       → kernels/*           (no domain, no app)
<domain>-core    → <domain>-api, kernels/*  (and other domains' api only)
<domain>-api     → kernels/framework, identifiers, field-notes [, taxonomy]
kernels/*        → framework + a tiny third-party allowlist
```

See [ADR-004 — Modular Monolith](docs/adr/ADR-004-modular-monolith.md) and
[`domains/CLAUDE.md`](domains/CLAUDE.md).

---

## Design pillars

### 1. Domain-Driven Design with structural enforcement

Every domain class implements exactly one of five framework supertypes —
`NamedEntity`, `Entity`, `Aggregate`, `ValueObject`, `BehavioralCollection`.
The chosen interface dictates identity rules, equality, and lifecycle. There
is no escape hatch.

Per-domain `CLAUDE.md` files capture the ubiquitous language and invariants
of each context (see [`domains/chemistry/CLAUDE.md`](domains/chemistry/CLAUDE.md),
[`domains/plants/CLAUDE.md`](domains/plants/CLAUDE.md),
[`domains/insects/CLAUDE.md`](domains/insects/CLAUDE.md)).

### 2. Typed identity, never raw strings or UUIDs

Two identity branches share a common data-layer port (ADR-022):

- `NamedEntity<NAME extends EntityName>` — kebab-case slug as natural key,
  the stable cross-domain reference (ADR-001).
- `Entity<ID extends EntityId>` — surrogate **UUIDv7** identity, validated
  `version() == 7` at construction. `UUID.randomUUID()` is forbidden in
  domain and adapter code; only the kernel generator may produce ids.

`EntityName` and `EntityId` subclasses live in their own
`domains/identifiers/` module — narrow, dependency-light, shared across the
DAG. A `String` or raw `UUID` crossing a boundary is a review blocker.

### 3. Hexagonal architecture (ports and adapters)

Core code (`kernels/`, `<domain>-api`, `<domain>-core`) is free of vendor
dependencies. Heavy or vendor-specific implementations live under
[`adapters/`](adapters/CLAUDE.md):

- **`Resilience` facade** is in the kernel; **Resilience4j** is in
  `adapters/resilience-resilience4j/`. No domain code imports Resilience4j.
- **`@DomainService` marker** is in the kernel; **Spring bean discovery**
  is in `adapters/spring-runtime/`. No domain code imports Spring
  (ADR-025).
- **In-memory data** is wired today via `framework-test`; the production
  RDBMS adapter is the next-planned member, swappable behind the same
  port without touching consumers.

### 4. Resilience as a first-order concern

Every cross-boundary call site declares its strategy ([ADR-026](docs/adr/ADR-026-resilience-first-order-concern.md)):

- `@Resilient(name = "...")` — class- or method-level, configured at the
  composition root.
- Programmatic facade — `resilience.timeout("name").execute(() -> ...)`.
- `@ResilienceExempt(reason = "...")` — only for in-process,
  side-effect-free, non-timeout-subject calls.

Production adapters throw `UnconfiguredResilienceException` on missing
config — silent fall-back is forbidden. An ArchUnit gate fails the build
when a class on a known cross-boundary path declares neither annotation.
Policy and reviewer checklist: [`docs/resilience-policy.md`](docs/resilience-policy.md).

### 5. Observability as structure, not cross-cutting

Every domain type implements `Observable` and declares `invariants()` —
a pure predicate graph the framework walks at method boundaries, on
repository inserts, on query results, on event processing
([ADR-017](docs/adr/ADR-017-observability-monitoring-and-validation.md)).

A single graph walk produces an `InvariantObservation` with the full set
of failing constraints in one pass — no iterative discovery. Two
Micrometer meters are emitted with stable, normalized names: a
high-cardinality opt-in `naturalist.observation` and a low-cardinality
always-on `naturalist.invariant.violation`. Domain code never touches
Micrometer types directly.

### 6. Command/Query Separation at the API surface

Read paths and write paths are distinct types ([ADR-006](docs/adr/ADR-006-command-query-separation.md),
[ADR-010](docs/adr/ADR-010-query-design-contract.md)). Each domain api
exposes three coordinated namespaces ([ADR-020](docs/adr/ADR-020-namespace-interface-pattern.md)):

| Outer                       | Java type   | Visibility      | Purpose                                  |
|-----------------------------|-------------|-----------------|------------------------------------------|
| `<Domain>Repository`        | `class`     | package-private | repository contracts for adapters        |
| `<Domain>Query`             | `interface` | public          | the consumer read surface                |
| `<Domain>EntityCollections` | `interface` | public          | typed multi-result return shapes         |

Queries are *thin* — observe arguments, dispatch, delegate. Aggregate
assembly is owned by package-private factories in `<domain>-core` and
never surfaces in the api module.

### 7. Records, no Lombok

Entity / Aggregate / ValueObject are Java records ([ADR-003](docs/adr/ADR-003-java-records-no-lombok.md)).
`BehavioralCollection` is the single deliberate exception — a `final class`
with package-private construction so domain-specific filtering returns
the same concrete type ([ADR-011](docs/adr/ADR-011-behavioral-collections.md)).
Static factories (`of(...)`, `empty()`, `from(...)`) are the public
instantiation API ([ADR-012](docs/adr/ADR-012-static-factory-construction.md)).

### 8. Behavioral repository contracts

Repository behavior is defined once, in `<domain>-repository-test`, as a
`@Test default` interface every adapter implements ([ADR-002](docs/adr/ADR-002-repository-behavioral-contract.md)).
Three cases per select method (argument validation, empty result, expected
result); writes add constraint and not-found cases. The in-memory adapter
and the production RDBMS adapter satisfy the same contract — by
construction, not by convention.

### 9. Cross-domain references, no cross-domain joins

Cross-domain references travel as `EntityName` slugs. Cross-domain joins
are prohibited; cross-domain FK enforcement is deferred to the RDBMS
layer (ADR-001, ADR-022). The catalog kernel
([`kernels/catalog/`](kernels/catalog/)) resolves slugs across registered
contributions and providers, validates uniqueness at startup, and fans
out reads in-process via `kernels/catalog-inmem/`.

### 10. Pull-request size discipline

One concern per PR; target ≤ 400 lines of meaningful diff ([ADR-019](docs/adr/ADR-019-pull-request-size-and-review-fatigue.md)).
The goal is reviewable changes a reviewer can hold in working memory —
not small PRs for their own sake.

---

## Tech stack

- **Java 25**, no Lombok, JSpecify nullability annotations.
- **Maven multi-module** — strict module DAG; Java visibility is the
  primary boundary, ArchUnit the build-time backstop.
- **Spring Boot 3.5** — composition root only, behind the
  `spring-runtime` adapter and the `@DomainService` marker.
- **JTE** for server-rendered templates in the management console.
- **Micrometer** for metrics — emitted only via the kernel's `Metric`
  builder; domain code never imports Micrometer types.
- **Resilience4j** behind the kernel's `Resilience` facade.
- **Jackson 2.19** for record-native serialization.

---

## Repository tour

| Where you might want to read first                                     | Why                                                       |
|------------------------------------------------------------------------|-----------------------------------------------------------|
| [`docs/adr/`](docs/adr/README.md)                                      | The recorded design decisions                             |
| [`kernels/framework/`](kernels/framework/)                             | Domain supertypes, identity, observability, resilience    |
| [`domains/chemistry/chemistry-api/`](domains/chemistry/chemistry-api/) | Reference implementation: `Compound`, `CompoundCollection`|
| [`domains/insects/insects-api/`](domains/insects/insects-api/)         | Reference implementation: namespace + aggregate factory   |
| [`apps/management-console/`](apps/management-console/README.md)       | The composing app — Spring config, resilience wiring      |
| [`adapters/resilience-resilience4j/`](adapters/resilience-resilience4j/) | Vendor adapter for the kernel `Resilience` facade        |
| [`docs/briefings/`](docs/briefings/README.md)                          | Short context briefings for each major domain             |

---

## Documentation index

- [Architecture Decision Records](docs/adr/README.md) — design rationale,
  open and accepted decisions.
- [`CLAUDE.md`](CLAUDE.md) — top-level conventions and identity model.
- [`kernels/CLAUDE.md`](kernels/CLAUDE.md) — framework, observability,
  behavioral collections.
- [`domains/CLAUDE.md`](domains/CLAUDE.md) — record conventions, query
  rules, repository architecture, namespace patterns, PR size.
- [`adapters/CLAUDE.md`](adapters/CLAUDE.md) — placement rules for
  vendor-specific implementations.
- [`apps/CLAUDE.md`](apps/CLAUDE.md) — composition-root discipline.
- [`docs/resilience-policy.md`](docs/resilience-policy.md) — cross-boundary
  resilience policy and reviewer checklist.
- [`docs/measurement-standards.md`](docs/measurement-standards.md) —
  units used across the domain catalog.
- [Management Console README](apps/management-console/README.md) —
  running the console, login, theming, layout.

---

## License

Proprietary — Copyright (c) 2026 Patrick Way. All rights reserved.
See [`LICENSE`](LICENSE).
