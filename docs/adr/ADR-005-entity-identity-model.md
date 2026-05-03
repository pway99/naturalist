# ADR-005: Entity Identity Model — CatalogEntity and FactEntity

> [rationale](rationale/ADR-005-entity-identity-model.md)

- Every entity has a non-nullable `name()`. No `@Nullable` default returning null.
- `Entity<ID, NAME>`:
  ```java
  public interface Entity<ID extends PersistenceId<?>, NAME extends EntityName<?>> extends Observable {
      ID id();
      NAME name();
      <E extends Entity<ID, NAME>> E withId(ID id);
  }
  ```
- `CatalogEntity<ID, NAME extends CatalogName>` — slug identity, stable named classification
  (`Element`, `Compound`, `InsectSpecies`, `Plant`, `ZoneInfo`).
- `FactEntity<ID, NAME extends FactName>` — GUID identity, singular occurrence.
  Informal sub-kinds: **Events** (human-initiated: `AmendmentEvent`, `IrrigationEvent`,
  `TillageEvent`) and **Observations** (passive: `LabAnalysis`, sensor readings).
- `FactName` — abstract `EntityName` wrapping a UUID. Concrete subclass per fact entity
  (`AmendmentEventName`, `LabAnalysisName`). Assigned at creation, not at persistence —
  enables offline deduplication via existing name unique constraint.
- Domain laws and constants are ValueObjects, static constants, or domain service methods —
  not entities. No table, no `name()`.
- Single `EntityRepository`, `TestEntitySource`, `AbstractTestEntityRepository` —
  Catalog/Fact are semantic markers, not separate infrastructure.

| Category                    | Type                             | Name kind |
|-----------------------------|----------------------------------|-----------|
| Named stable classification | `CatalogEntity`                  | slug      |
| Singular occurrence         | `FactEntity`                     | UUID      |
| Domain law / constant       | `ValueObject` / static / service | none      |

## Name uniqueness & annotations (Amendment 2)

- `Entity` has `NAME` type parameter bounded by `EntityName<?>`.
- `TestEntitySource<ID, NAME, ENTITY>` enforces name uniqueness in `preSaveChecks` before
  iterating `uniqueConstraints()`.
- `uniqueConstraints()` default returns `List.of()`; override for additional constraints
  beyond canonical name.
- `@EntityIdentifier` only on *secondary* `EntityName` fields — never on canonical `name`.
- `@UniqueValue` for secondary unique plain-value fields (e.g. `String commonName`).

## CatalogName canonical form (Amendment 3 — Draft)

- `CatalogName` enforces `^[a-z0-9]+(-[a-z0-9]+)*$` via `isValid()`.
- Each concrete `CatalogName` declares `protected abstract int maxLength()` → `VARCHAR(n)` in DDL.
- `FactName` unchanged — UUID self-validates.
- `name()` is the machine key; human-readable labels live in separate unconstrained fields
  (`Element.symbol = "Ca"`, `ReactionProfile.title`, etc.).
- `Compound.commonName`: `@UniqueValue String` (was `@EntityIdentifier CompoundCommonName`).
