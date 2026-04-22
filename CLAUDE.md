# The amateur Naturalist

Modular-monolith domain application in `com.naturalist`. Architecture decisions live in
[`docs/adr/`](docs/adr/README.md) — see the index there.

Detailed conventions are split by area and loaded only when working in that area:

- [`kernels/CLAUDE.md`](kernels/CLAUDE.md) — framework, observability, testing observables,
  BehavioralCollection
- [`domains/CLAUDE.md`](domains/CLAUDE.md) — identity model, record conventions, field
  annotations, test identifiers, TestEntitySource, repository architecture, **api surface
  namespace patterns**, query implementation rules, new-module scaffolding, PR size discipline
- [`domains/<domain>/CLAUDE.md`](domains/) — per-domain vocabulary and invariants
- [`docs/measurement-standards.md`](docs/measurement-standards.md) — units reference

## Identity Model

Every domain class implements exactly one of four interfaces from `kernels/framework`:
`Entity`, `Aggregate`, `ValueObject`, or `BehavioralCollection`. All extend `Observable`
and declare `invariants()`. See [`domains/CLAUDE.md`](domains/CLAUDE.md) for signatures
and rules; see [ADR-011](docs/adr/ADR-011-behavioral-collections.md) and
[ADR-013](docs/adr/ADR-013-value-object-contract.md) for rationale.

`PersistenceId<Long>` is null in JSON catalogs — assigned by `TestEntitySource.nextId()`
or RDBMS. Concrete `PersistenceId` and `EntityName` subclasses live in
`domains/identifiers/`. `EntityName` is never null on any entity.

## Module Structure and DAG

```
kernels/
  framework/          — Entity, PersistenceId, EntityName, Aggregate, ValueObject,
                        Observable, Observer, BehavioralCollection
  framework-test/     — TestEntitySource, TestEntitySourceTest, TestDataHelper
  field-notes/        — Description (four-level Durrell description)
  taxonomy/           — TaxonomicClassification (organism domains only)

domains/
  identifiers/        — typed IDs and names only
  <domain>/<domain>-api, <domain>-core, <domain>-repository-test
```

DAG (no cycles — call out violations immediately):

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
framework                   →  (nothing — external libs only)
framework-test              →  framework
```

Hard rules:

1. **api modules may only depend on `framework`, `identifiers`, `field-notes`, and
   (organism domains) `taxonomy`.** No other inter-module dependencies in api modules.
2. **Domain modules access other domains only via their api.** A core module may import
   from a foreign domain's api; never from core, repository-test, or repository-rdms.
3. **repository modules depend only on their own api** (plus framework).
4. **bootstrap depends on everything; nothing depends on bootstrap.**
5. **Repository interfaces are package-private** in the api module — inter-domain
   interaction goes through public service/query classes.
6. **Repository behavior contracts** are defined as interfaces in repository-test,
   implemented by both in-memory and rdms adapters.

Package-private visibility is the primary enforcement mechanism. ArchUnit tests may
verify the DAG at build time.

## Sub-Context Boundaries

Within a module, sub-contexts enforce boundaries via Java visibility. `package-private`
= internal to sub-context. `public` = explicitly crosses sub-context boundary. The
per-domain CLAUDE.md files show the concrete sub-context layout for each domain.

## Non-Negotiables

- **Strongly typed identifiers.** Never raw `String` or `Long` as an entity reference
  across any boundary. Every entity has a concrete `PersistenceId<Long>` subclass.
- **Records, no Lombok.** Entity, Aggregate, ValueObject are Java records; the lone
  exception is `BehavioralCollection` (final class, see ADR-011). Full record rules live
  in [`domains/CLAUDE.md`](domains/CLAUDE.md).
- **Domain knowledge is code, not configuration.** `OptimumRanges`, `Amendments`,
  `Amendment.evaluateSafety()` are domain invariants. Observations and Events are
  immutable facts — records or final fields, no setters, equality by value.