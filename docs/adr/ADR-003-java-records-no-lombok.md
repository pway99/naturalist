# ADR-003: Java Records, No Lombok

**Status:** Accepted
**Full rationale:** [rationale/ADR-003-java-records-no-lombok.md](rationale/ADR-003-java-records-no-lombok.md)

## Decision

- All domain classes (Entity, Aggregate, ValueObject) are Java records. Lombok is not used.
- `EntityName` and `PersistenceId` are abstract classes (parameterized base types with
  concrete subclasses), so they declare explicit `equals`/`hashCode` using `getClass()` for
  the type check — **never `instanceof`** — so `ElementName.of("carbon")` is never equal to
  `CompoundName.of("carbon")`.
- Entity records needing identity-based equality (id-only) declare custom `equals`/`hashCode`
  in the record body.

## Consequences

- No annotation processing at build time
- `equals`/`hashCode`/`toString` always visible in source — no hidden generation
- Removing a dependency cannot silently break equality semantics
- `with*` methods are explicit per record — more verbose, but mutation points visible
