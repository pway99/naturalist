# Repository-RDMS Intermediate Layer — Design

**Date:** 2026-08-22
**Status:** Draft

## Goal

Give the management-console a **production-shaped repository layer** now, while the
real SQL adapters do not yet exist. Introduce `<domain>-repository-rdms` modules whose
repository beans the Spring context discovers and injects, temporarily backed by the
existing in-memory mocks. When the real SQL adapters are written, the swap is "fill in
the repository body," not "rewire the application."

This is the first, deliberately-narrow slice of a larger clean-up. It establishes the
module structure and gets real repository beans into a production-named module. It does
**not** touch the console controllers — that is a separate, enforced step (see *Deferred*).

## Background — current state

The pre-RDBMS data layer is an in-memory `NaturalistDatabase` registry (ADR-001): each
`TestEntitySource` is the analog of an RDBMS table, and `NaturalistDatabase#getNamed`
lazily builds and caches one shared instance per source. A single shared
`NaturalistDatabase` bean already exists in the app context, published by
`adapters/spring-test-data/TestDataConfiguration`.

On top of that registry the wiring has drifted three ways:

1. **The Spring path is live and correct.** All `*QueryImpl`s and the insects
   `*CommandImpl`s are `@DomainService`; the repository **mocks** are `@DomainService`
   too. `adapters/spring-runtime`'s `DomainServiceScan` discovers them, so every domain
   `Query`/`Command` is already a bean wired to a repository backed by the shared
   `NaturalistDatabase`. Catalog/search and admin run on this.
2. **Dead workaround beans.** Each `*-console` `*DataConfiguration` publishes
   `@Bean <Entity>TestEntitySource` via `new <Entity>TestEntitySource(database)`. These
   are not consumed by any main-code injection point, and — being built with `new` rather
   than `getNamed` — they are duplicate instances outside the shared registry.
3. **Controllers fork private graphs.** All 9 domain controllers call
   `XTestContext.create(NaturalistDatabase.create())`, building a *fresh, private*
   `NaturalistDatabase` per controller and bypassing Spring entirely. This is a known
   interim workaround (each controller carries a `//TODO: eventually a Spring bean`).

The ADR rule *"`NaturalistDatabase` is the only object permitted to instantiate a
`TestEntitySource`"* is not enforced, which is why (2) and the console tests' direct
`new` drifted in.

## Scope of this pass

1. **Add `<domain>-repository-rdms` modules** — one per domain that has repositories.
   Each holds the domain's production repository classes, discovered as the app's
   repository beans, temporarily backed by the mock.
2. **Make the rdms impl the single repository bean.** Remove `@DomainService` from the
   mocks so exactly one bean implements each repository interface.
3. **Remove the dead `*DataConfiguration` `@Bean` source classes** (the obsolete
   workaround from Background #2).

Everything else is deferred (below). This slice deliberately **leaves the controller
forking in place** — fixing it now, without the enforcement that locks it, would spend
the one clean opportunity to enforce the solution structurally.

## Mechanism

### The module

`<domain>-repository-rdms`:

- Maven parent `<domain>`; added to `<domain>/pom.xml` `<modules>` and to root
  `<dependencyManagement>` at `${project.version}` (new-module checklist, domains/CLAUDE.md).
- Package `com.naturalist.<domain>` — the **same package** as the api's repository
  namespace and the mock. This is required: the repository interfaces are package-private
  in `<domain>-api` and the mock class is package-private, so a class implementing or
  extending them must share the package (the split-package pattern already used by
  `-repository-test` and `-test-context`).
- Depends on `<domain>-repository-test` (compile) + `<domain>-api` + `framework`.

### The intermediate repository impl

For each repository, a production repository class that — **for now** — extends the
existing mock:

```java
package com.naturalist.<domain>;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class <Entity>RepositoryRdms extends <Entity>RepositoryMock {
    <Entity>RepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

- `@DomainService` → discovered by `DomainServiceScan` as the repository bean.
- The injected `NaturalistDatabase` is the shared `spring-test-data` bean; the inherited
  `AbstractTestEntityRepository` resolves the source via `getNamed`, so the rdms bean
  reads and writes the one shared catalog — the ADR-blessed path.
- Extending the mock (rather than forwarding method-by-method) reuses every
  domain-specific select with no boilerplate, and defines the repository's behavior once.
- These classes are **permanent**: they are the natural home for the real SQL adapter.
  The SQL swap replaces `extends <Entity>RepositoryMock` with a JDBC/JPA body and drops
  the `-repository-test` dependency — no change anywhere else.

### Single bean per interface

The mock loses its `@DomainService` marker. This is required — with both the mock and the
rdms impl annotated, Spring would see two beans for one repository interface and fail with
`NoUniqueBeanDefinitionException`. Removing it from the mock is safe: the contract tests
construct the mock directly (`new <Entity>RepositoryMock(db)`), never via Spring, so they
are unaffected.

### Console config cleanup

Delete the `*DataConfiguration` classes whose only members are `@Bean <Entity>TestEntitySource`
methods (chemistry, insects, library, plants consoles). They are unconsumed and superseded
by the rdms repository beans on the shared `NaturalistDatabase`.

## DAG note — a bounded, named inversion

`<domain>-repository-rdms` depending on `<domain>-repository-test` inverts the usual rule
("repository modules depend only on their own api + framework"): here one repository module
depends on another. This is **intentional and temporary** — it exists only so the rdms
impl can extend the mock while there is no real SQL. It is severed the moment the SQL body
lands. The dependency is compile-scope and lives entirely on the pre-RDBMS classpath, which
`spring-test-data` already gates.

## Deferred (each a later, separate effort)

- **Controller de-forking + ArchUnit enforcement, together.** Convert the 9 controllers
  from `XTestContext.create(NaturalistDatabase.create())` to injecting the discovered
  `Query`/`Command` beans, and land — in the same step — an ArchUnit rule forbidding
  `NaturalistDatabase.create()` outside its sanctioned homes (`spring-test-data`,
  `NaturalistTestExtension`). Pairing the fix with the guard is the whole point of leaving
  the forking untouched now.
- **Console-test de-fork (~39 sites).** The `new <Entity>TestEntitySource(NaturalistDatabase.create())`
  in console template tests move to `NaturalistTestExtension` + `getNamed`, so a companion
  ArchUnit rule forbidding `new *TestEntitySource(...)` can go green.
- **`@DomainRepository` marker.** A repository-specific discovery marker distinct from
  `@DomainService`. Parked: it adds a second marker for `DomainServiceScan` to learn with
  no behavioral gain today, and the real SQL adapter will likely use Spring Data's own
  `@Repository`. Revisit only if repositories need distinct handling (separate scan phase,
  transaction advice).
- **`<domain>-repository-mock` extraction.** Splitting the shippable mock + `TestEntitySource`
  + JSON out of `-repository-test` (leaving it contract-tests-only) would remove the
  "prod depends on a `*-test` module" smell outright, replacing the DAG inversion above
  with a clean dependency. Parked as a larger rename; the intermediate depends on
  `-repository-test` as-is.

## Rollout

Do **insects first** as the template (reference domain), landing the module scaffolding
conventions, then replicate per domain (plants, chemistry, garden, library, soil, …). One
domain per PR keeps each change reviewable (ADR-019, ≤400 lines).

## The eventual SQL swap (what this sets up)

When a domain's real adapter is written: give each `<Entity>RepositoryRdms` a JDBC/JPA
body, stop extending the mock, drop the module's `-repository-test` dependency. The bean
name, interface, injection sites, and every consumer stay put. Removing `spring-test-data`
from the deployment then removes the `NaturalistDatabase` bean, and the app runs on real
persistence with no wiring change.

## Decisions locked

1. **Impl class name:** `<Entity>RepositoryRdms` (parallels `<Entity>RepositoryMock`).
2. **Rollout:** insects first, as the template. Other domains follow as fast-follow PRs
   after the insects slice lands and proves the pattern; they are **out of scope** for
   this plan.

## Verify during the plan

- Confirm each `*DataConfiguration` slated for deletion defines *only* `@Bean` source
  methods (no other beans) before removing it. (The insects slice removes only
  `InsectDataConfiguration`; other consoles' configs are removed with their own domain PR.)
- Confirm no main-code injection point depends on the concrete mock **type**
  (`@Autowired <Entity>RepositoryMock`) — repositories are injected by interface, so
  swapping the concrete bean from mock to rdms must be transparent.
