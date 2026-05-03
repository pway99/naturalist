# ADR-012: Static Factory Construction

**Status:** Draft

## Context

The project originally used Lombok `@Value` and `@With` for immutable domain types.
Moving to Java records eliminated that dependency and aligned with the language's own
immutability model. Records handle entities, aggregates, and value objects well.

A complementary concern is instantiation control. Exposing a public constructor gives
callers no guidance on intent, no named entry point, and no single location to enforce
defensive invariants (such as `List.copyOf` on a collection backing store). Static
factory methods solve all three: they are named, they centralise invariant enforcement,
and they can be the only public path to a new instance when the constructor is not
accessible.

## Decision

Static factory methods are the public instantiation API for all domain types. Constructors
are not the intended call site for consumers.

### Structural Enforcement — Behavioral Collections

Behavioral collections (ADR-011) are `final class`, not `record`. This is a deliberate
exception to the records-everywhere convention and exists specifically to enable
structural enforcement: the constructor is package-private, and static factory methods
are the only public instantiation path.

```java
public final class CompoundCollection implements ValueObject {

    private final List<Compound> compounds;

    // Package-private — only the query adapter (same package) calls this directly
    CompoundCollection(List<Compound> compounds) {
        this.compounds = List.copyOf(compounds);
    }

    /** Primary factory — defensive copy is enforced in the constructor. */
    public static CompoundCollection of(Collection<Compound> compounds) {
        return new CompoundCollection(compounds);
    }

    public static CompoundCollection empty() {
        return new CompoundCollection(List.of());
    }

    // ... behavioral methods
}
```

The `List.copyOf()` defensive copy lives in the constructor and nowhere else. Every
code path that produces a `CompoundCollection` — `of(...)`, `empty()`, and all
internal `with*` transformation methods — goes through that constructor. The immutability
invariant is structurally guaranteed, not documented and hoped for.

### Convention — Entities, Aggregates, Value Objects

Java requires the canonical constructor of a `public record` to be at least as accessible
as the record itself. A `public record Foo` cannot have a package-private canonical
constructor — the compiler rejects it. This is not worth fighting.

For domain types that are records — all `Entity`, `Aggregate`, and `ValueObject`
implementations — the canonical constructor is necessarily public. Static factory
methods are still provided and are the documented instantiation API, but enforcement
is by convention rather than by the compiler.

```java
public record CompoundName(String value) implements EntityName {

    // Canonical constructor is public — Java record requirement
    // Compact constructor enforces the invariant
    public CompoundName {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("CompoundName must not be blank");
    }

    /** Intended instantiation path for all callers. */
    public static CompoundName of(String value) {
        return new CompoundName(value);
    }
}
```

Call sites use `CompoundName.of("calcium-sulfate")`, not `new CompoundName("calcium-sulfate")`.
The canonical constructor remains accessible but is not the idiomatic path.

### Factory Method Naming

| Intent                              | Method name                              |
|-------------------------------------|------------------------------------------|
| Primary construction from arguments | `of(...)`                                |
| Empty / zero-element instance       | `empty()`                                |
| Construction from a related type    | `from(...)`                              |
| Named domain concept construction   | descriptive (`withSolubilityAbove(...)`) |

`of` is the default. Deviate only when domain clarity demands it.

### Transformation Methods on Behavioral Collections

`with*` methods on behavioral collections follow the same rule — they return a new
instance via the package-private constructor:

```java
public CompoundCollection withSolubilityAbove(double thresholdGramsPerLitre) {
    return new CompoundCollection(
            compounds.stream()
                    .filter(c -> c.solubilityProfile().solubility() > thresholdGramsPerLitre)
                    .toList()
    );
}
```

This is valid because the `with*` method is defined inside `CompoundCollection` and has
access to the package-private constructor. External callers cannot replicate this path.

## Consequences

- Behavioral collections gain structural instantiation control; `List.copyOf` is
  enforced once and cannot be bypassed
- Record-based domain types gain named, intention-revealing factory methods; the
  canonical constructor is technically accessible but not the idiomatic path
- The distinction between structural and conventional enforcement is explicit and
  documented — no implicit assumption that records and final classes are interchangeable
- `new Foo(...)` at a call site outside the domain type's own class is a review flag
- The shift from Lombok `@Value`/`@With` to records is complete; this ADR closes the
  remaining instantiation gap that records do not address on their own
