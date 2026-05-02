# The amateur Naturalist

Modular-monolith domain application in `com.naturalist`. ADRs: [`docs/adr/`](docs/adr/README.md).

Area-specific conventions (load when working in that area):

- [`kernels/CLAUDE.md`](kernels/CLAUDE.md) — framework, observability, BehavioralCollection
- [`domains/CLAUDE.md`](domains/CLAUDE.md) — identity model, record conventions, field
  annotations, TestEntitySource, repository architecture, **api surface namespace
  patterns**, query rules, new-module scaffolding, PR size
- [`domains/<domain>/CLAUDE.md`](domains/) — per-domain vocabulary and invariants
- [`docs/measurement-standards.md`](docs/measurement-standards.md) — units

## Identity

Every domain class implements exactly one of `NamedEntity`, `Entity`, `Aggregate`,
`ValueObject`, `BehavioralCollection` (all from `kernels/framework`; all extend
`Observable` and declare `invariants()`). Signatures and rules in
[`domains/CLAUDE.md`](domains/CLAUDE.md).

Two identity branches share a common `Named<KEY>` data-layer port (ADR-022):

- **`NamedEntity<NAME extends EntityName>`** — natural-key slug identity.
  `EntityName` is never null, kebab-case, and the stable cross-domain reference
  (ADR-001). `EntityName` subclasses live in `domains/identifiers/`.
- **`Entity<ID extends EntityId>`** — surrogate UUIDv7 identity. `EntityId` is
  generated at record construction, validated `version() == 7` at the boundary,
  and never crosses a domain boundary by value. `EntityId` subclasses live in
  `domains/identifiers/`. Use a kernel-provided generator; `UUID.randomUUID()`
  is forbidden.

No domain record carries a `PersistenceId` component — it does not exist in
Java (ADR-022, superseding ADR-021).

## Module layout

```
kernels/
  framework/          — NamedEntity, Entity, EntityName, EntityId, Aggregate,
                        ValueObject, Observable, Observer, BehavioralCollection,
                        Resilience facade
  framework-test/     — NamedTestEntitySource, NamedTestEntitySourceTest, TestDataHelper
  field-notes/        — Description (four-level Durrell description)
  taxonomy/           — TaxonomicClassification (organism domains only)
  catalog/            — cross-domain reference resolution (DomainId, Catalog)
  catalog-inmem/      — in-memory reference adapter for catalog
domains/
  identifiers/        — typed names (EntityName subclasses) and typed ids
                        (EntityId subclasses) only
  <domain>/<domain>-api, <domain>-core, <domain>-repository-test
adapters/
  resilience-resilience4j/ — bridges kernel Resilience facade to Resilience4j
apps/
  management-console/ — composition-root deployment artifact (Spring Boot)
```

## DAG (no cycles)

```
bootstrap                   →  application
<domain>-repository-test    →  <domain>-api
<domain>-repository-rdms    →  <domain>-api
<domain>-core               →  <domain>-api
<domain>-api                →  framework, identifiers, field-notes
<organism>-api              →  framework, identifiers, field-notes, taxonomy
identifiers                 →  framework
field-notes                 →  framework
taxonomy                    →  framework
framework                   →  Jackson, Commons, Micrometer, JSpecify only
framework-test              →  framework
```

Rules:

1. api modules depend only on `framework`, `identifiers`, `field-notes`, (organism only)
   `taxonomy`. Nothing else.
2. core may import another domain's **api** only — never its core, repository-test, or
   repository-rdms.
3. repository modules depend only on their own api (+ framework).
4. bootstrap depends on everything; nothing depends on bootstrap.
5. Repository interfaces are package-private in api; cross-domain interaction goes
   through public query/service classes.
6. Repository behavior contracts live in repository-test; both in-memory and rdms
   adapters implement them.

Package-private visibility is the primary enforcement; ArchUnit tests may verify at build time.

## Sub-contexts

Within a module, `package-private` = internal to sub-context, `public` = crosses
boundary. Per-domain `CLAUDE.md` files show each domain's sub-context layout.

## Non-negotiables

- **Typed identifiers.** Never raw `String`, `Long`, or `UUID` as an entity reference
  across any boundary. `NamedEntity` records carry a concrete `EntityName` subclass;
  `Entity` records carry a concrete `EntityId` subclass. Cross-`NamedEntity` references
  are by `EntityName`; `Entity` records are never referenced cross-domain by value
  (ADR-022).
- **UUIDv7 only.** `EntityId.isValid()` enforces version 7. Do not call
  `UUID.randomUUID()` in domain or adapter code — use the kernel's generator.
- **Records for NamedEntity/Entity/Aggregate/ValueObject.** `BehavioralCollection` is
  a `final class` (see [ADR-011](docs/adr/ADR-011-behavioral-collections.md)). Record
  rules in [`domains/CLAUDE.md`](domains/CLAUDE.md).
- **Observations and Events are immutable** — records or final fields, no setters,
  equality by value. This is a domain invariant for `Entity` records and is documented
  in each event/observation domain's own `CLAUDE.md`; it is no longer encoded in a
  separate kernel subtype.
