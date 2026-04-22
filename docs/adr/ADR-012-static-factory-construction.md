# ADR-012: Static Factory Construction
> [rationale](rationale/ADR-012-static-factory-construction.md)

Static factories are the public instantiation API. Constructors are not the intended call site.

**Behavioral collections (structural enforcement)**
- `final class`, not record.
- Package-private constructor — only the query adapter (same package) calls it directly.
- `public static of(...)` and `public static empty()` are the only public paths.
- `List.copyOf()` defensive copy lives in the constructor, nowhere else.
- `with*` uses the package-private constructor.

**Entity / Aggregate / ValueObject (records, convention)**
- Canonical constructor is necessarily public (Java requirement for public records).
- Enforcement by convention: call sites use `Foo.of(...)`, not `new Foo(...)`.
- Compact constructor enforces invariants (`Objects.requireNonNull`, blank checks).

**Factory naming**

| Intent | Method |
|---|---|
| Primary construction | `of(...)` |
| Empty instance | `empty()` |
| Construction from related type | `from(...)` |
| Named domain concept | descriptive (`withSolubilityAbove(...)`) |

`new Foo(...)` outside the type's own class is a review flag.
