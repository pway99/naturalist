# ADR-012: Static Factory Construction

**Status:** Draft
**Full rationale:** [rationale/ADR-012-static-factory-construction.md](rationale/ADR-012-static-factory-construction.md)

## Decision

Static factory methods are the public instantiation API for all domain types. Constructors
are not the intended call site.

### Structural enforcement — behavioral collections
- `final class`, not record (deliberate exception to records-everywhere convention)
- **Package-private constructor** — only the query adapter (same package) calls it directly
- `public static of(...)` and `public static empty()` are the only public paths
- `List.copyOf()` defensive copy lives in the constructor and nowhere else
- `with*` transformation methods use the package-private constructor (valid — same class)

### Convention — Entity, Aggregate, ValueObject (records)
- Java requires a public record's canonical constructor to be at least as accessible as the
  record — package-private canonical constructors are compiler-rejected
- Canonical constructor necessarily public; **enforcement is by convention**
- Static factory methods still provided as the documented instantiation API
- Compact constructor enforces invariants (`Objects.requireNonNull`, blank checks, etc.)
- Call sites use `Foo.of(...)`, not `new Foo(...)`

### Factory naming

| Intent | Method |
|---|---|
| Primary construction | `of(...)` |
| Empty instance | `empty()` |
| Construction from related type | `from(...)` |
| Named domain concept | descriptive (`withSolubilityAbove(...)`) |

`of` is the default. Deviate only when domain clarity demands.

## Consequences

- Behavioral collections: structural instantiation control; `List.copyOf` enforced once
- Records: named intention-revealing factories; canonical constructor technically accessible
  but not idiomatic
- Distinction between structural and conventional enforcement is explicit
- `new Foo(...)` at a call site outside the type's own class is a review flag
- Shift from Lombok `@Value`/`@With` to records is complete
