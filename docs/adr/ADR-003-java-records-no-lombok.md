# ADR-003: Java Records for Domain Types

> [rationale](rationale/ADR-003-java-records-no-lombok.md)

- Entity, Aggregate, ValueObject are Java records.
- `EntityName` and `PersistenceId` are abstract classes (parameterized base, concrete
  subclasses) and declare explicit `equals`/`hashCode` using `getClass()` — **never
  `instanceof`** — so `ElementName.of("carbon")` is never equal to `CompoundName.of("carbon")`.
- Entity records needing identity-based equality (id-only) declare custom `equals`/`hashCode`
  in the record body.
- `with*` methods are explicit per record.
