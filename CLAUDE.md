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

Every domain class implements exactly one of `NamedEntity`, `Aggregate`, `ValueObject`,
`BehavioralCollection` (all from `kernels/framework`; all extend `Observable` and declare
`invariants()`). Signatures and rules in [`domains/CLAUDE.md`](domains/CLAUDE.md).

Domain identity is `EntityName` — never null, the stable natural key.
`EntityName` subclasses live in `domains/identifiers/`. `PersistenceId<Long>` is an
adapter-internal concern (ADR-021) — no domain record carries an `id()` component.

## Module layout

```
kernels/
  framework/          — NamedEntity, EntityName, Aggregate, ValueObject,
                        Observable, Observer, BehavioralCollection
  framework-test/     — NamedTestEntitySource, NamedTestEntitySourceTest, TestDataHelper
  field-notes/        — Description (four-level Durrell description)
  taxonomy/           — TaxonomicClassification (organism domains only)
domains/
  identifiers/        — typed IDs and names only
  <domain>/<domain>-api, <domain>-core, <domain>-repository-test
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

- **Typed identifiers.** Never raw `String`/`Long` as an entity reference across any
  boundary. Every entity has a concrete `EntityName` subclass; cross-entity references
  are by `EntityName` only (ADR-021).
- **Records for Entity/Aggregate/ValueObject.** `BehavioralCollection` is a `final class`
  (see [ADR-011](docs/adr/ADR-011-behavioral-collections.md)). Record rules in
  [`domains/CLAUDE.md`](domains/CLAUDE.md).
- **Observations and Events are immutable** — records or final fields, no setters,
  equality by value.
