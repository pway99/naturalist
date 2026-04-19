# Insects Domain

The `insects` domain is the **pilot** for [ADR-021](../../docs/adr/ADR-021-persistenceid-is-adapter-internal.md)
— persistence-id is adapter-internal; cross-entity references use `EntityName`. The
shape proven here is the template for every other domain.

## Domain Vocabulary

**InsectSpecies** — `NamedEntity<InsectSpeciesName>`. Catalog identity of a species
as recognised by the naturalist. Four-level Durrell `Description`. No `id()` component
at the domain layer; the RDBMS adapter carries a numeric primary key privately.

**InsectImage** — `NamedEntity<InsectImageName>`. A photograph of an observed individual.
Carries `InsectSpeciesName` (slug) as its parent reference. No `insectSpeciesId`; no
`withId`; no nullable id-shaped FK column.

**InsectAggregate** — Aggregate rooted at `InsectSpecies`, composing the species with
its `InsectImageCollection`. Assembled by name through `InsectAggregateFactory`.

## ADR-021 Migration Playbook — Propagating the Pattern

The insects pilot added a parallel kernel surface (`NamedEntity`, `NamedEntityRepository`,
`NamedEntityQuery`, `NamedTestEntitySource`, and supporting framework-test scaffolding)
so migration can proceed one domain at a time, each on a review-sized PR, with every
unmigrated domain remaining byte-compatible.

### Stage 1 — Migrate a domain (one PR per domain)

For each remaining domain (chemistry, plants, soil, apiary, climate, sensors, zone,
naturalists, vertebrates, microbes, arachnids, molluscs, worms):

1. **Port the entities.**
   - Change `implements Entity<FooId, FooName>` → `implements NamedEntity<FooName>`
     on every record in `<domain>-api`.
   - Drop the `id` component from the record header.
   - Drop the `withId(FooId)` override.
   - Update `invariants()` to remove `i.entityId(id, "id")`.
   - Any cross-entity component typed `<OtherFoo>Id` becomes `<OtherFoo>Name`. If the
     reference leaves the domain, it was already a slug (ADR-021 Rule 2) — flag any
     surviving cross-domain `*Id` field as a review blocker.

2. **Delete the `PersistenceId` subclass** from `domains/identifiers/com/naturalist/<domain>/`.
   The file is dead code the moment step 1 lands; a stranded `FooId.java` is a review
   flag.

3. **Port the repository port.**
   - In the `<domain>-api` namespace class, the nested `<Entity>Repository` interface
     extends `NamedEntityRepository<FooName, Foo>` instead of `EntityRepository<...>`.
   - In `<domain>-repository-test`, the mock extends `AbstractTestNamedEntityRepository`
     instead of `AbstractTestEntityRepository`. The `do*` hooks narrow from six to
     four (no `doGetById`, no `doGetByIdSet`).

4. **Port the query port.**
   - Nested `<Entity>Query` extends `NamedEntityQuery<FooName, Foo, FooCollection>`.
   - Query adapter in `<domain>-core` extends `AbstractNamedEntityQuery` instead of
     `AbstractEntityQuery`. Drop any `findByIdSet`/`getById` method.

5. **Port the TestEntitySource.**
   - `TestEntitySource<FooId, FooName, Foo>` → `NamedTestEntitySource<FooName, Foo>`.
   - Remove the `idFactory` constructor argument; no numeric id assignment remains.
   - `uniqueConstraints()` keeps its shape. `UniqueConstraint<Foo>` still compiles
     because the bound was relaxed to `Observable` during the pilot — this is the
     **deliberate temporary widening** that lets a single `UniqueConstraint` interface
     serve both `Entity` and `NamedEntity` adapters. It is tightened back to
     `NamedEntity` in Stage 2.
   - The `TestEntitySourceTest` base switches to `NamedTestEntitySourceTest`.

6. **Port the behavioral contract test.**
   - `EntityRepositoryContractTest<FooId, FooName, Foo>` →
     `NamedEntityRepositoryContractTest<FooName, Foo>`.
   - Delete `notFoundId()` and the id-based test hooks. `assertEntityEquals` no longer
     ignores `"id"` — the field is gone.

7. **Clean up fixtures.**
   - Remove every `"id": null` line from `<domain>/src/main/resources/<domain>/**/*.json`.
     The field no longer exists on the record; Jackson tolerates it, but a stranded
     key is dead weight.

8. **Audit call sites.**
   - Any `repository.getById(...)`, any `query.findByIdSet(...)`, any aggregate factory
     that threads a `FooId` between two repositories — all gone. The five signals in
     ADR-021 Applicability Signals apply as review checks.

Each domain migrates on its own PR, reviewable in isolation. The module graph, contract
test counts, and public query surface all change only for the migrating domain.

### Stage 2 — Consolidate the kernel (one PR, after every domain has migrated)

Once no code anywhere implements `Entity<ID, NAME>`:

1. **Delete the legacy id-bearing kernel surface:**
   - `kernels/framework/.../ddd/Entity.java`
   - `kernels/framework/.../ddd/PersistenceId.java`,
     `PersistenceIdentifier.java`, `CatalogEntity.java`, `FactEntity.java`,
     `EntityId.java` (whatever remains tied to the id-bearing shape)
   - `kernels/framework/.../data/EntityRepository.java`,
     `AbstractEntityRepository.java`, `EntityQuery.java`, `AbstractEntityQuery.java`,
     `Query.java`
   - `kernels/framework-test/.../data/TestEntitySource.java`,
     `TestEntitySourceTest.java`, `AbstractTestEntityRepository.java`,
     `EntityRepositoryContractTest.java`

2. **Rename the named surface to the default:**
   - `NamedEntity` → `Entity`
   - `NamedEntityRepository` → `EntityRepository`
   - `AbstractNamedEntityRepository` → `AbstractEntityRepository`
   - `NamedEntityQuery` → `EntityQuery`
   - `AbstractNamedEntityQuery` → `AbstractEntityQuery`
   - `NamedTestEntitySource` → `TestEntitySource`
   - `NamedTestEntitySourceTest` → `TestEntitySourceTest`
   - `AbstractTestNamedEntityRepository` → `AbstractTestEntityRepository`
   - `NamedEntityRepositoryContractTest` → `EntityRepositoryContractTest`
   - `NaturalistDatabase.getNamed(...)` → `get(...)` (the old `get` is gone)

3. **Tighten the Observable-bounded widenings.**
   - `UniqueConstraint<ENTITY extends Observable>` →
     `UniqueConstraint<ENTITY extends Entity<?>>`. The widening to `Observable` was
     adopted in the pilot to let a single interface serve both entity shapes. With
     the id-bearing shape gone, the tighter bound restores correctness — a
     `ValueObject` or `BehavioralCollection` could satisfy `Observable` but has no
     business being a unique-constraint subject.
   - `Constraints.namedEntity(...)` overloads merge back into `Constraints.entity(...)`;
     the `namedEntity(...)` names disappear.
   - `MethodObserver.namedEntity(...)` and `Observer.namedEntity(...)` disappear
     likewise.

4. **Tighten exception fields.**
   - `PrimaryKeyConstraintException.entity`,
     `EntityNotFoundException.entity`,
     `UniqueConstraintException.entity` — field type narrows from `Observable` back to
     `Entity<?>` (the new entity). The `NamedEntity` overload constructors collapse
     into the canonical `Entity<?>` constructor.
   - The `EntityNotFoundException(EntityName)` constructor added in the pilot for
     adapter-side slug-miss raises stays — it is orthogonal to the entity-shape
     decision.

5. **Delete the `PersistenceId` hierarchy and its constraint code.**
   - `PersistenceIdConstraints`, `Constraints.entityId(...)`,
     `Invariants.entityId(...)`, and the `@PersistenceId` annotation (if unused)
     all retire.

A single consolidation PR flips the vocabulary for the whole codebase — every domain
is already on the final shape; the rename and the bound-tightening are the only diff.

### Why the widening is safe

The `Observable` bound on `UniqueConstraint` is visibly provisional — no call site
ever exploits it. Every concrete `UniqueConstraint` in the codebase today is declared
over an `Entity` subtype; every one written during the migration will be declared over
a `NamedEntity` subtype. Tightening the bound in Stage 2 is a single-line change that
the compiler will verify exhaustively. If any rogue `UniqueConstraint<SomeValueObject>`
has crept in, the consolidation PR will surface it immediately.
