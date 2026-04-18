# ADR-010: Query Design Contract

**Status:** Draft

## Context

ADR-006 established Command Query Separation as a foundational principle: queries read
state, return a result, and never mutate. That ADR defined the CQS boundary at the method
level. It did not define the structural contract for Query objects — how they are declared,
where their implementations live, what they return, or how they validate their arguments.

As the domain grows, query logic accumulates. Without a structural contract, queries
drift toward ad-hoc service methods, return raw `List<T>` to every consumer, and scatter
argument validation inconsistently across the codebase. This ADR closes that gap.

## Decision

### Port Declaration

A Query is a public Java interface declared in the `<domain>-api` module. It is a port
in the hexagonal sense — the domain defines the contract, the core module provides the
adapter. Nothing outside the domain depends on the adapter; everything outside the domain
depends only on the port.

```java
// chemistry-api — public port
public interface CompoundQuery {
    // ...
}
```

### Single Adapter

Each Query port has exactly one adapter implementation, located in the `<domain>-core`
module. There is no multi-adapter strategy for queries — a query is not a persistence
boundary, it is a read model over the domain's repositories. The in-memory and RDBMS
repository adapters sit beneath the query; the query itself does not change.

```
chemistry-api     CompoundQuery          (public port)
chemistry-core    CompoundQueryAdapter   (single implementation)
```

### Idempotency

All query methods are idempotent. Calling the same query with the same arguments any
number of times returns the same result. Queries never cause side effects, never mutate
state, and never produce observable changes. This is a strengthening of the CQS rule
from ADR-006 — it is a structural guarantee, not an advisory.

### Argument Validation

Query implementations validate all arguments before accessing any repository. Null
arguments and structurally invalid inputs are programming errors — they are reported
via `observer().arguments(...)`, which collects all violations in a single pass and
throws `InvalidVariantException`. This mirrors the validation contract established
for repositories and services.

Whether a logically valid argument that yields no result (an unknown `EntityName`, a
date range with no matching facts) throws or returns an empty result is determined
per-method. The default is to return an empty result — the caller decides what an
empty result means. Throwing for a not-found argument is permitted only when the
method contract explicitly documents it.

### Aggregate Construction via Factory

When a query method returns a complex aggregate — one whose construction requires
coordinating multiple repositories or performing non-trivial assembly — the query
delegates that construction to a dedicated factory. The factory is package-private
within the `<domain>-core` module. The query adapter calls the factory; it does not
assemble aggregates inline.

This keeps query methods focused on retrieval and routing, and keeps assembly logic
testable in isolation.

```java
// Inside CompoundQueryAdapter
public Optional<CompoundAggregate> getAggregate(CompoundName name) {
    observer().arguments(a -> a.notNull(name, "name"));
    return compoundRepository.getByName(name)
            .map(compoundAggregateFactory::build);
}
```

### Behavioral Collections

Query methods that return multiple results return a behavioral collection rather than a
raw `List<T>`. The full structural contract for behavioral collections — naming, method
categories, placement, and the prohibition on raw collection return types at public
boundaries — is defined in [ADR-011](ADR-011-behavioral-collections.md).

```java
// Wrong — forces every consumer to re-implement filtering
List<Compound> getByType(CompoundType type);

// Correct — operations live on the collection
CompoundCollection getByType(CompoundType type);
```

## Consequences

- Query ports are public API; query adapters are module-private implementation details
- Consumers depend only on the port — swapping or extending the adapter requires no
  caller changes
- Argument validation is uniform across all query implementations
- Aggregate construction complexity is isolated in factories, not scattered across query methods
- Behavioral collections eliminate redundant stream processing at every call site and
  surface domain query vocabulary in one place
- Raw `List<T>` return types on query methods are a code smell and a review flag
- The module DAG is unchanged: `<domain>-core` depends on `<domain>-api`;
  `<domain>-api` gains the behavioral collection types alongside the query port
